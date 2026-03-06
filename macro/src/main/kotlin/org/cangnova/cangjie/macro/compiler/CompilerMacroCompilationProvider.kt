/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.cangjie.macro.compiler

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.io.systemIndependentPath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.cangnova.cangjie.macro.messages.CangJieMacroBundle
import org.cangnova.cangjie.macro.service.*
import org.cangnova.cangjie.project.service.CjProjectsService
import org.cangnova.cangjie.result.CjResult
import org.cangnova.cangjie.toolchain.api.CjProjectSdkConfig
import org.cangnova.cangjie.toolchain.api.CjSdk
import java.io.File
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path

/**
 * 编译器宏编译提供者
 *
 * 通过调用 `cjc -p <package_dir> --compile-macro -o <output_dir>` 命令来编译宏声明。
 * 参照 LSP/CJLint 实现，每个宏包独立编译，输出动态库到构建输出目录。
 *
 * ## 命令格式
 *
 * ```
 * cjc -p <package_dir> --compile-macro -o <target_dir>/release/<platform>/
 * ```
 *
 * ## 输出目录
 *
 * 输出目录通过 `cangjie-project` 模块的 `CjSourceSet.outputDirectory` 获取，
 * 与项目正常构建输出路径一致。
 * 完整输出路径为 `<outputDirectory>/release/<targetPlatform>/`。
 *
 * ## 输出说明
 *
 * `--compile-macro` 会编译宏声明并生成动态库文件：
 * - Windows: `lib-macro_<package_name>.dll`
 * - Linux: `lib-macro_<package_name>.so`
 * - macOS: `lib-macro_<package_name>.dylib`
 */
class CompilerMacroCompilationProvider(private val project: Project) {

    private val logger = Logger.getInstance(CompilerMacroCompilationProvider::class.java)

    /**
     * 最近一次编译成功的宏包目录列表
     *
     * 用于在宏展开时添加到 import path 中，让编译器能找到动态库
     */
    @Volatile
    var compiledMacroPackageDirs: List<String> = emptyList()
        private set

    /**
     * 检查编译器是否可用
     */
    fun isAvailable(): Boolean {
        val sdk = CjProjectSdkConfig.getInstance(project).getProjectSdk()
        if (sdk == null || !sdk.isValid) {
            return false
        }

        return sdk.getExecutable("cjc").toFile().exists()
    }

    /**
     * 编译项目中的所有宏声明
     *
     * 扫描项目中所有包含 `macro package` 声明的源文件，
     * 确定各宏包的源码目录，然后逐个编译。
     *
     * @param contextFile 触发宏展开的源文件，传入后将优先通过语义定位器精确查找相关宏包；
     *                    null 表示全项目范围扫描
     */
    suspend fun compileAllMacrosInProject(
        options: MacroCompilationOptions,
        contextFile: VirtualFile? = null
    ): CjResult<MacroCompilationResult, MacroCompilationError> {
        val sdk = CjProjectSdkConfig.getInstance(project).getProjectSdk()
            ?: return CjResult.Err(MacroCompilationError.SdkNotConfigured())

        if (!sdk.isValid) {
            return CjResult.Err(MacroCompilationError.CompilerUnavailable(CangJieMacroBundle.message("macro.error.sdk.invalid")))
        }

        val cjProject = CjProjectsService.getInstance(project).cjProject
        if (!cjProject.isValid) {
            return CjResult.Err(MacroCompilationError.InternalError(CangJieMacroBundle.message("macro.compilation.error.project.dir.unavailable")))
        }

        val projectDir = cjProject.rootDir

        // 查找宏包目录（每个宏包的源码目录）
        val macroPackageDirs = findMacroPackageDirectories(projectDir, contextFile)
        if (macroPackageDirs.isEmpty()) {
            return CjResult.Err(MacroCompilationError.NoMacrosFound(projectDir.path))
        }

        return try {
            if (options.timeoutMs > 0) {
                withTimeout(options.timeoutMs) {
                    compileAllMacroPackages(sdk, macroPackageDirs, options)
                }
            } else {
                compileAllMacroPackages(sdk, macroPackageDirs, options)
            }
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            CjResult.Err(MacroCompilationError.Timeout(options.timeoutMs))
        } catch (e: Exception) {
            CjResult.Err(MacroCompilationError.InternalError(e.message ?: CangJieMacroBundle.message("macro.error.unknown"), e))
        }
    }

    /**
     * 编译指定文件所在的宏包
     */
    suspend fun compileMacros(
        file: VirtualFile,
        options: MacroCompilationOptions
    ): CjResult<MacroCompilationResult, MacroCompilationError> {
        val sdk = CjProjectSdkConfig.getInstance(project).getProjectSdk()
            ?: return CjResult.Err(MacroCompilationError.SdkNotConfigured())

        if (!sdk.isValid) {
            return CjResult.Err(MacroCompilationError.CompilerUnavailable(CangJieMacroBundle.message("macro.error.sdk.invalid")))
        }

        // 宏包目录 = 宏源文件的父目录
        val packageDir = file.parent?.path
            ?: return CjResult.Err(MacroCompilationError.InternalError(CangJieMacroBundle.message("macro.compilation.error.macro.pkg.dir.unavailable")))

        return try {
            if (options.timeoutMs > 0) {
                withTimeout(options.timeoutMs) {
                    compileAllMacroPackages(sdk, listOf(packageDir), options)
                }
            } else {
                compileAllMacroPackages(sdk, listOf(packageDir), options)
            }
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            CjResult.Err(MacroCompilationError.Timeout(options.timeoutMs))
        } catch (e: Exception) {
            CjResult.Err(MacroCompilationError.InternalError(e.message ?: CangJieMacroBundle.message("macro.error.unknown"), e))
        }
    }

    /**
     * 获取宏编译输出目录
     *
     * 优先使用 cangjie-project 提供的模块输出目录（`CjSourceSet.outputDirectory`）；
     * 若模块尚未构建（输出目录不存在），回退到 null。
     */
    fun getOutputDirectory(file: VirtualFile): Path? {
        val sdk = CjProjectSdkConfig.getInstance(project).getProjectSdk() ?: return null
        val targetPlatform = sdk.version?.targetPlatform
        val projectsService = CjProjectsService.getInstance(project)
        val cjModule = projectsService.findModuleForFile(file)

        val outputDirs = (cjModule?.sourceSets
            ?: projectsService.cjProject.module?.sourceSets
            ?: emptyList())
            .flatMap { it.outputDirectory }

        val baseDir = outputDirs.firstOrNull() ?: return null
        return if (targetPlatform != null) {
            Path.of(baseDir.path, "release", targetPlatform)
        } else {
            Path.of(baseDir.path)
        }
    }

    /**
     * 检查文件是否需要重新编译
     */
    fun needsRecompilation(file: VirtualFile): Boolean {
        val outputDir = getOutputDirectory(file) ?: return true
        val macroLibs = findMacroLibsInDir(outputDir.toFile())
        if (macroLibs.isEmpty()) {
            return true
        }

        val sourceModified = File(file.path).lastModified()
        val oldestLib = macroLibs.minOf { it.lastModified() }

        return sourceModified > oldestLib
    }

    /**
     * 逐个编译宏包
     *
     * 命令：`cjc -p <package_src_dir> --compile-macro -o <output_dir>`
     *
     * 输出目录优先使用 cangjie-project 提供的模块输出目录；
     * 若输出目录尚不存在（首次构建前），回退到项目根目录。
     */
    private suspend fun compileAllMacroPackages(
        sdk: CjSdk,
        packageDirs: List<String>,
        options: MacroCompilationOptions
    ): CjResult<MacroCompilationResult, MacroCompilationError> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val allOutputFiles = mutableListOf<Path>()
        val allCompiledOutputDirs = mutableListOf<String>()
        val allOutput = StringBuilder()

        // 确定输出目录：从 cangjie-project 获取已有的模块输出目录
        val cjProject = CjProjectsService.getInstance(project).cjProject
        val allModules = buildList {
            cjProject.module?.let { add(it) }
            cjProject.workspace?.modules?.let { addAll(it) }
        }
        val targetPlatform = sdk.version?.targetPlatform
        val existingOutputDirs = allModules
            .flatMap { it.sourceSets }
            .flatMap { it.outputDirectory }

        val outputDir = if (existingOutputDirs.isNotEmpty()) {
            // 使用已存在的输出目录作为基础
            val baseDir = existingOutputDirs.first()
            if (targetPlatform != null) {
                File(baseDir.path, "release/$targetPlatform")
            } else {
                File(baseDir.path)
            }
        } else if (cjProject.isValid) {
            // 项目尚未构建：回退到项目根目录下的标准路径
            if (targetPlatform != null) {
                File(cjProject.rootDir.path, "target/release/$targetPlatform")
            } else {
                File(cjProject.rootDir.path, "target")
            }
        } else {
            Files.createTempDirectory("cj-macro-build").toFile()
        }
        Files.createDirectories(outputDir.toPath())

        for (packageDir in packageDirs) {
            val packageDirFile = File(packageDir)
            if (!packageDirFile.exists() || !packageDirFile.isDirectory) {
                logger.warn("宏包目录不存在: $packageDir")
                continue
            }

            try {
                // cjc -p <src_dir> --compile-macro -o <target_dir>
                val commandLine = GeneralCommandLine().apply {
                    exePath = sdk.getExecutable("cjc").systemIndependentPath
                    addParameter("-p")
                    addParameter(packageDir)
                    addParameter("--compile-macro")
                    addParameter("-o")
                    addParameter(outputDir.absolutePath)

                    // 设置工作目录为输出目录，避免编译器在 IDE 安装目录下生成中间文件
                    workDirectory = outputDir

                    charset = Charset.forName("UTF-8")
                    environment.putAll(sdk.getEnvironment())
                }

                logger.debug("执行宏编译命令: ${commandLine.commandLineString}")

                val handler = CapturingProcessHandler(commandLine)
                val output = handler.runProcess(
                    (options.timeoutMs.takeIf { it > 0 }
                        ?: Registry.intValue("cangjie.macro.compilation.timeout.ms").toLong()).toInt()
                )

                if (output.exitCode != 0) {
                    logger.warn("宏包编译失败 ($packageDir): ${output.stderr}")
                    allOutput.appendLine(CangJieMacroBundle.message("macro.compilation.output.failed", packageDir, output.stderr))
                    continue
                }

                allCompiledOutputDirs.add(outputDir.absolutePath)
                allOutput.appendLine(output.stdout)
                allOutput.appendLine(output.stderr)
            } catch (e: Exception) {
                logger.warn("编译宏包失败 ($packageDir): ${e.message}")
                allOutput.appendLine(CangJieMacroBundle.message("macro.compilation.output.exception", packageDir, e.message ?: ""))
            }
        }

        // 更新已编译的宏包输出目录列表（用于添加到 import path）
        compiledMacroPackageDirs = allCompiledOutputDirs

        val compilationTime = System.currentTimeMillis() - startTime

        if (allCompiledOutputDirs.isEmpty()) {
            return@withContext CjResult.Err(
                MacroCompilationError.CompilationFailed(
                    commandLine = "cjc -p <dirs> --compile-macro -o <dirs>",
                    exitCode = 1,
                    stderr = allOutput.toString()
                )
            )
        }

        CjResult.Ok(
            MacroCompilationResult(
                compiledFiles = packageDirs,
                outputFiles = allOutputFiles,
                outputDirectory = Path.of(allCompiledOutputDirs.first()),
                compilationTimeMs = compilationTime,
                compilerOutput = allOutput.toString()
            )
        )
    }

    /**
     * 查找宏包目录
     *
     * 优先使用 [MacroDeclarationLocator] 扩展点（按优先级降序尝试），
     * 若所有实现均返回 null，则回退到内置文本正则扫描逻辑。
     *
     * 每个目录对应一个宏包，作为 `cjc -p <dir>` 的参数。
     */
    private fun findMacroPackageDirectories(projectDir: VirtualFile, contextFile: VirtualFile? = null): List<String> {
        MacroDeclarationLocator.EP_NAME.extensionList
            .sortedByDescending { it.priority }
            .forEach { locator ->
                val dirs = locator.findMacroPackageDirs(project, contextFile)
                if (dirs != null) return dirs
            }
        // 兜底：内置文本正则扫描
        return findMacroPackageFiles(projectDir)
            .mapNotNull { it.parent?.path }
            .distinct()
    }

    /**
     * 查找项目中包含 `macro package` 声明的源文件
     */
    private fun findMacroPackageFiles(projectDir: VirtualFile): List<VirtualFile> {
        val allFiles = findSourceFiles(projectDir)
        return allFiles.filter { file ->
            try {
                val content = String(file.contentsToByteArray(), Charsets.UTF_8)
                // 匹配 "macro package" 声明（可能前面有 public/internal 等修饰符）
                content.contains(Regex("""(^|\n)\s*(\w+\s+)*macro\s+package\s"""))
            } catch (_: Exception) {
                false
            }
        }
    }

    /**
     * 查找项目中的所有源文件
     *
     * 通过 cangjie-project 模块获取各模块的源码根目录，
     * 并排除输出目录（避免扫描编译产物）。
     */
    private fun findSourceFiles(projectDir: VirtualFile): List<VirtualFile> {
        val result = mutableListOf<VirtualFile>()
        val projectsService = CjProjectsService.getInstance(project)
        val cjProject = projectsService.cjProject
        val allModules = buildList {
            cjProject.module?.let { add(it) }
            cjProject.workspace?.modules?.let { addAll(it) }
        }

        if (allModules.isNotEmpty()) {
            // 收集所有输出目录路径（用于排除，避免扫描编译产物）
            val excludedPaths = allModules.flatMap { module ->
                module.sourceSets.flatMap { it.outputDirectory }
            }.map { it.path }.toSet()

            // 遍历所有模块的源码根目录
            for (module in allModules) {
                for (sourceSet in module.sourceSets) {
                    for (sourceRoot in sourceSet.sourceRoots) {
                        collectCjFiles(sourceRoot, excludedPaths, result)
                    }
                }
            }
        } else {
            // 回退：扫描整个项目目录（不排除特定目录名）
            collectCjFiles(projectDir, emptySet(), result)
        }

        return result
    }

    /**
     * 递归收集目录中的 .cj 源文件
     *
     * @param dir 起始目录
     * @param excludedPaths 需要跳过的目录路径集合（输出目录）
     * @param result 收集结果列表
     */
    private fun collectCjFiles(
        dir: VirtualFile,
        excludedPaths: Set<String>,
        result: MutableList<VirtualFile>
    ) {
        for (child in dir.children) {
            when {
                child.isDirectory && child.path !in excludedPaths && !child.name.startsWith(".") -> {
                    collectCjFiles(child, excludedPaths, result)
                }
                child.extension == "cj" -> result.add(child)
            }
        }
    }

    /**
     * 检查是否存在已编译的宏动态库
     *
     * 检查项目所有输出目录中是否存在 `lib-macro_*` 文件。
     */
    fun hasCompiledMacroLibs(): Boolean {
        val cjProject = CjProjectsService.getInstance(project).cjProject
        val allModules = buildList {
            cjProject.module?.let { add(it) }
            cjProject.workspace?.modules?.let { addAll(it) }
        }
        val sdk = CjProjectSdkConfig.getInstance(project).getProjectSdk() ?: return false
        val targetPlatform = sdk.version?.targetPlatform

        val outputDirs = allModules
            .flatMap { it.sourceSets }
            .flatMap { it.outputDirectory }

        val dirsToCheck = if (outputDirs.isNotEmpty()) {
            outputDirs.map { baseDir ->
                if (targetPlatform != null) File(baseDir.path, "release/$targetPlatform")
                else File(baseDir.path)
            }
        } else if (cjProject.isValid) {
            listOf(
                if (targetPlatform != null) File(cjProject.rootDir.path, "target/release/$targetPlatform")
                else File(cjProject.rootDir.path, "target")
            )
        } else {
            return false
        }

        return dirsToCheck.any { findMacroLibsInDir(it).isNotEmpty() }
    }

    /**
     * 查找目录中的宏动态库文件
     *
     * 查找匹配 `lib-macro_*` 模式的动态库文件（.dll/.so/.dylib）
     */
    internal fun findMacroLibsInDir(dir: File): List<File> {
        if (!dir.exists() || !dir.isDirectory) {
            return emptyList()
        }

        val sdk = CjProjectSdkConfig.getInstance(project).getProjectSdk()
            ?: return emptyList()
        val ext = sdk.macroLibExtension
        return dir.listFiles()?.filter { file ->
            file.name.startsWith("lib-macro_") && file.name.endsWith(".$ext")
        } ?: emptyList()
    }
}
