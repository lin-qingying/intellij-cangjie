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
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.io.systemIndependentPath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.cangnova.cangjie.macro.service.*
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
 * 输出目录与正常编译输出路径一致，从 `cjpm.toml` 的 `target-dir` 配置读取，
 * 默认为 `target`。完整输出路径为 `<project>/<target-dir>/release/<targetPlatform>/`。
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

        val cjcPath = getCjcPath(sdk)
        return cjcPath.toFile().exists()
    }

    /**
     * 编译项目中的所有宏声明
     *
     * 扫描项目中所有包含 `macro package` 声明的源文件，
     * 确定各宏包的源码目录，然后逐个编译。
     */
    suspend fun compileAllMacrosInProject(
        options: MacroCompilationOptions
    ): CjResult<MacroCompilationResult, MacroCompilationError> {
        val sdk = CjProjectSdkConfig.getInstance(project).getProjectSdk()
            ?: return CjResult.Err(MacroCompilationError.SdkNotConfigured())

        if (!sdk.isValid) {
            return CjResult.Err(MacroCompilationError.CompilerUnavailable("SDK 无效"))
        }

        val projectDir = project.guessProjectDir()
            ?: return CjResult.Err(MacroCompilationError.InternalError("无法获取项目目录"))

        // 查找宏包目录（每个宏包的源码目录）
        val macroPackageDirs = findMacroPackageDirectories(projectDir)
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
            CjResult.Err(MacroCompilationError.InternalError(e.message ?: "未知错误", e))
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
            return CjResult.Err(MacroCompilationError.CompilerUnavailable("SDK 无效"))
        }

        // 宏包目录 = 宏源文件的父目录
        val packageDir = file.parent?.path
            ?: return CjResult.Err(MacroCompilationError.InternalError("无法确定宏包目录"))

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
            CjResult.Err(MacroCompilationError.InternalError(e.message ?: "未知错误", e))
        }
    }

    /**
     * 获取宏编译输出目录
     *
     * 输出到标准构建输出目录 `<project>/<target-dir>/release/<targetPlatform>/`，
     * 其中 `<target-dir>` 从 `cjpm.toml` 的 `target-dir` 配置读取，默认为 `target`。
     */
    fun getOutputDirectory(file: VirtualFile): Path? {
        val sdk = CjProjectSdkConfig.getInstance(project).getProjectSdk() ?: return null
        val targetPlatform = sdk.version?.targetPlatform ?: return null
        val projectDir = project.guessProjectDir() ?: return null
        val targetDir = resolveTargetDir(projectDir)
        return Path.of(projectDir.path, "$targetDir/release/$targetPlatform")
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
     * 输出目录使用标准构建输出目录 `<target-dir>/release/<targetPlatform>/`，
     * 其中 `<target-dir>` 从 `cjpm.toml` 的 `target-dir` 配置读取。
     */
    private suspend fun compileAllMacroPackages(
        sdk: CjSdk,
        packageDirs: List<String>,
        options: MacroCompilationOptions
    ): CjResult<MacroCompilationResult, MacroCompilationError> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val cjcPath = getCjcPath(sdk)
        val allOutputFiles = mutableListOf<Path>()
        val allCompiledOutputDirs = mutableListOf<String>()
        val allOutput = StringBuilder()

        // 确定输出目录：<project_dir>/<target-dir>/release/<targetPlatform>/
        val projectDir = project.guessProjectDir()
        val targetPlatform = sdk.version?.targetPlatform
        val targetDir = if (projectDir != null) resolveTargetDir(projectDir) else "target"
        val outputDir = if (projectDir != null && targetPlatform != null) {
            File(projectDir.path, "$targetDir/release/$targetPlatform")
        } else if (projectDir != null) {
            File(projectDir.path, targetDir)
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
                    exePath = cjcPath.systemIndependentPath
                    addParameter("-p")
                    addParameter(packageDir)
                    addParameter("--compile-macro")
                    addParameter("-o")
                    addParameter(outputDir.absolutePath)

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
                    allOutput.appendLine("编译失败 ($packageDir): ${output.stderr}")
                    continue
                }

                allCompiledOutputDirs.add(outputDir.absolutePath)
                allOutput.appendLine(output.stdout)
                allOutput.appendLine(output.stderr)
            } catch (e: Exception) {
                logger.warn("编译宏包失败 ($packageDir): ${e.message}")
                allOutput.appendLine("编译异常 ($packageDir): ${e.message}")
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
     * 扫描项目中包含 `macro package` 声明的 `.cj` 文件，
     * 返回这些文件所在的目录路径（去重）。
     *
     * 每个目录对应一个宏包，作为 `cjc -p <dir>` 的参数。
     */
    private fun findMacroPackageDirectories(projectDir: VirtualFile): List<String> {
        val macroFiles = findMacroPackageFiles(projectDir)
        return macroFiles
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
     */
    private fun findSourceFiles(projectDir: VirtualFile): List<VirtualFile> {
        val result = mutableListOf<VirtualFile>()

        fun collectFiles(dir: VirtualFile) {
            for (child in dir.children) {
                when {
                    child.isDirectory && !child.name.startsWith(".") && child.name != "target" && child.name != "build" -> {
                        collectFiles(child)
                    }
                    child.extension == "cj" -> {
                        result.add(child)
                    }
                }
            }
        }

        // 优先搜索 src 目录
        val srcDir = projectDir.findChild("src")
        if (srcDir != null && srcDir.isDirectory) {
            collectFiles(srcDir)
        } else {
            collectFiles(projectDir)
        }

        return result
    }

    /**
     * 查找目录中的宏动态库文件
     *
     * 查找匹配 `lib-macro_*` 模式的动态库文件（.dll/.so/.dylib）
     */
    private fun findMacroLibsInDir(dir: File): List<File> {
        if (!dir.exists() || !dir.isDirectory) {
            return emptyList()
        }

        val macroLibExtension = getMacroLibExtension()
        return dir.listFiles()?.filter { file ->
            file.name.startsWith("lib-macro_") && file.name.endsWith(".$macroLibExtension")
        } ?: emptyList()
    }

    /**
     * 获取宏动态库文件扩展名
     */
    private fun getMacroLibExtension(): String {
        val os = System.getProperty("os.name").lowercase()
        return when {
            os.contains("win") -> "dll"
            os.contains("mac") || os.contains("darwin") -> "dylib"
            else -> "so"
        }
    }

    /**
     * 从 cjpm.toml 解析构建输出目录名
     *
     * 读取 `cjpm.toml` 中 `[package]` 的 `target-dir` 配置，
     * 默认返回 `"target"`，与 `CjpmBuildConfigurationImpl.outputDir` 逻辑一致。
     */
    private fun resolveTargetDir(projectDir: VirtualFile): String {
        val tomlFile = projectDir.findChild("cjpm.toml") ?: return "target"
        return try {
            val content = String(tomlFile.contentsToByteArray(), Charsets.UTF_8)
            // 匹配 [package] 段中的 target-dir = "xxx"
            val targetDirRegex = Regex("""target-dir\s*=\s*"([^"]+)"""")
            targetDirRegex.find(content)?.groupValues?.getOrNull(1)?.takeIf { it.isNotEmpty() } ?: "target"
        } catch (_: Exception) {
            "target"
        }
    }

    /**
     * 获取 cjc 编译器路径
     *
     * 宏编译使用 `cjc`（完整编译器），而非 `cjc-frontend`。
     * 参照 CJLint 实现：`cjc -p <dir> --compile-macro -o <dir>`
     */
    private fun getCjcPath(sdk: CjSdk): Path {
        val os = System.getProperty("os.name").lowercase()
        val compilerName = if (os.contains("win")) "cjc.exe" else "cjc"
        return sdk.binPath.resolve(compilerName)
    }
}
