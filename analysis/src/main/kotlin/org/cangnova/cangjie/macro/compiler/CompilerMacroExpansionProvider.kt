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
 * 编译器宏展开提供者
 *
 * 通过调用 `cjc-frontend --debug-macro` 命令来展开宏。
 * 支持在展开前自动编译宏声明（通过 `--compile-macro`）。
 *
 * ## 工作流程
 *
 * 1. 如果启用了自动编译（`autoCompileMacros = true`）：
 *    - 先执行 `cjc-frontend --compile-macro` 编译宏声明
 *    - 生成 `.cjo` 文件到 `target/release/<platform>/` 目录
 *
 * 2. 执行 `cjc-frontend --debug-macro` 展开宏：
 *    - 使用编译后的 `.cjo` 文件解析 import
 *    - 生成 `<filename>.macro.cj` 文件
 *
 * ## 命令格式
 *
 * ```
 * cjc-frontend --compile-macro [options] <source-files>
 * cjc-frontend --debug-macro [options] <source-file>
 * ```
 *
 * **注意**: 这些选项属于 `GROUP(FRONTEND)`，只能通过 `cjc-frontend` 使用。
 */
class CompilerMacroExpansionProvider(private val project: Project) : MacroExpansionProvider {

    private val logger = Logger.getInstance(CompilerMacroExpansionProvider::class.java)

    private val compilationProvider = CompilerMacroCompilationProvider(project)

    override val name: String = "Compiler"

    override val source: ExpansionSource = ExpansionSource.COMPILER

    override fun isAvailable(project: Project): Boolean {
        val sdk = CjProjectSdkConfig.getInstance(project).getProjectSdk()
        if (sdk == null || !sdk.isValid) {
            return false
        }

        // 检查编译器是否存在
        val compilerPath = getCompilerPath(sdk)
        return compilerPath.toFile().exists()
    }

    override suspend fun expandMacroAtOffset(
        project: Project,
        file: VirtualFile,
        offset: Int,
        options: MacroExpansionOptions
    ): CjResult<MacroExpansionResult, MacroExpansionError> {
        // 编译器 --debug-macro 是文件级别的操作，需要先展开所有宏然后筛选
        val allResults = expandAllMacrosInFile(project, file, options)

        return when (allResults) {
            is CjResult.Ok -> {
                // 查找包含指定偏移量的宏
                val matchingResult = allResults.ok.find { result ->
                    offset >= result.startOffset && offset < result.endOffset
                }

                if (matchingResult != null) {
                    CjResult.Ok(matchingResult)
                } else {
                    CjResult.Err(MacroExpansionError.MacroNotFound("offset=$offset"))
                }
            }
            is CjResult.Err -> allResults
        }
    }

    override suspend fun expandAllMacrosInFile(
        project: Project,
        file: VirtualFile,
        options: MacroExpansionOptions
    ): CjResult<List<MacroExpansionResult>, MacroExpansionError> {
        val sdk = CjProjectSdkConfig.getInstance(project).getProjectSdk()
            ?: return CjResult.Err(MacroExpansionError.SdkNotConfigured())

        if (!sdk.isValid) {
            return CjResult.Err(MacroExpansionError.CompilerUnavailable("SDK 无效"))
        }

        return try {
            // Step 1: 如果启用了自动编译，先编译宏声明
            if (options.autoCompileMacros) {
                val compileResult = compileMacrosIfNeeded(file, options)
                if (compileResult is CjResult.Err) {
                    logger.warn("宏编译失败: ${compileResult.err.message}")
                    // 编译失败不阻止展开尝试，可能是文件中没有宏声明
                }
            }

            // Step 2: 执行宏展开
            if (options.timeoutMs > 0) {
                withTimeout(options.timeoutMs) {
                    executeCompilerDebugMacro(sdk, file, options)
                }
            } else {
                executeCompilerDebugMacro(sdk, file, options)
            }
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            CjResult.Err(MacroExpansionError.Timeout(options.timeoutMs))
        } catch (e: Exception) {
            CjResult.Err(MacroExpansionError.InternalError(e.message ?: "未知错误", e))
        }
    }

    override suspend fun expandMacrosInRange(
        project: Project,
        file: VirtualFile,
        startOffset: Int,
        endOffset: Int,
        options: MacroExpansionOptions
    ): CjResult<List<MacroExpansionResult>, MacroExpansionError> {
        val allResults = expandAllMacrosInFile(project, file, options)

        return when (allResults) {
            is CjResult.Ok -> {
                // 筛选范围内的宏
                val filteredResults = allResults.ok.filter { result ->
                    result.startOffset >= startOffset && result.endOffset <= endOffset
                }
                CjResult.Ok(filteredResults)
            }
            is CjResult.Err -> allResults
        }
    }

    /**
     * 如果需要，编译宏声明
     *
     * @return 编译结果
     */
    private suspend fun compileMacrosIfNeeded(
        file: VirtualFile,
        options: MacroExpansionOptions
    ): CjResult<MacroCompilationResult, MacroCompilationError> {
        // 检查是否需要重新编译
        val needsRecompile = options.forceRecompile || compilationProvider.needsRecompilation(file)

        if (!needsRecompile) {
            logger.debug("跳过宏编译：文件未修改或已有最新的编译结果")
            return CjResult.Ok(
                MacroCompilationResult(
                    compiledFiles = listOf(file.path),
                    outputFiles = emptyList(),
                    outputDirectory = compilationProvider.getOutputDirectory(file) ?: Path.of(""),
                    compilationTimeMs = 0,
                    compilerOutput = "使用缓存"
                )
            )
        }

        logger.info("开始编译宏声明: ${file.path}")

        val compileOptions = MacroCompilationOptions(
            forceRecompile = options.forceRecompile,
            timeoutMs = options.timeoutMs / 2,  // 使用一半的超时时间用于编译
            parallel = true
        )

        return compilationProvider.compileMacros(file, compileOptions)
    }

    /**
     * 执行编译器 --debug-macro 命令
     */
    private suspend fun executeCompilerDebugMacro(
        sdk: CjSdk,
        file: VirtualFile,
        options: MacroExpansionOptions
    ): CjResult<List<MacroExpansionResult>, MacroExpansionError> = withContext(Dispatchers.IO) {
        val compilerPath = getCompilerPath(sdk)
        val filePath = file.path
        val sourceFile = File(filePath)

        // 创建临时输出目录
        val tempDir = Files.createTempDirectory("cj-macro-expansion").toFile()

        try {
            // 构建命令行
            val commandLine = GeneralCommandLine().apply {
                exePath = compilerPath.systemIndependentPath
                addParameter("--debug-macro")

                // 指定输出目录
                addParameter("--output-dir")
                addParameter(tempDir.absolutePath)

                // 添加项目相关的导入路径
                addImportPaths(this, file, sdk)

                // 添加并行宏展开选项（如果支持）
                if (options.recursive) {
                    addParameter("--parallel-macro-expansion")
                }

                // 添加源文件路径
                addParameter(filePath)

                charset = Charset.forName("UTF-8")

                // 配置环境变量
                environment.putAll(sdk.getEnvironment())
            }

            logger.debug("执行宏展开命令: ${commandLine.commandLineString}")

            val handler = CapturingProcessHandler(commandLine)
            val output = handler.runProcess(
                (options.timeoutMs.takeIf { it > 0 } ?: 60000).toInt()
            )

            if (output.exitCode != 0) {
                val stderr = output.stderr
                val errorMessage = when {
                    // 检测 "can not find package" 错误并提供更有帮助的提示
                    stderr.contains("can not find package") -> {
                        val packageMatch = Regex("can not find package '([^']+)'").find(stderr)
                        val packageName = packageMatch?.groupValues?.getOrNull(1) ?: "未知"
                        "找不到包 '$packageName'。请确保：\n" +
                            "1. 已执行 'cjpm build' 构建项目（或启用自动编译选项）\n" +
                            "2. 依赖包的 .cjo 文件存在于 target/release/<platform>/ 目录下\n" +
                            "原始错误: $stderr"
                    }
                    else -> stderr
                }

                return@withContext CjResult.Err(
                    MacroExpansionError.ProcessExecutionError(
                        commandLine = commandLine.commandLineString,
                        exitCode = output.exitCode,
                        stderr = errorMessage
                    )
                )
            }

            // 读取生成的宏展开文件
            // 文件名格式为 <原文件名>.macro.cj
            val macroOutputFile = findMacroOutputFile(tempDir, sourceFile.nameWithoutExtension)

            if (macroOutputFile == null || !macroOutputFile.exists()) {
                // 没有生成宏展开文件，可能文件中没有宏
                return@withContext CjResult.Ok(emptyList())
            }

            val expandedCode = macroOutputFile.readText(Charsets.UTF_8)
            val result = MacroExpansionResult(
                filePath = filePath,
                startOffset = 0,
                endOffset = 0,
                expandedText = expandedCode,
                source = ExpansionSource.COMPILER
            )

            CjResult.Ok(listOf(result))
        } catch (e: Exception) {
            CjResult.Err(
                MacroExpansionError.InternalError(
                    "执行编译器命令失败: ${e.message}",
                    e
                )
            )
        } finally {
            // 清理临时目录
            tempDir.deleteRecursively()
        }
    }

    /**
     * 添加导入路径参数
     *
     * 根据文件所在项目添加必要的 --import-path 参数
     *
     * ## 导入路径优先级
     * 1. 目标平台构建输出目录 (`target/release/<targetPlatform>` 和 `target/debug/<targetPlatform>`)
     * 2. 通用输出目录 (`target`, `build`, `out`, `.cjpm/target`)
     * 3. 项目根目录
     * 4. SDK 标准库路径
     * 5. 环境变量中的路径 (CANGJIE_HOME, CANGJIE_PATH)
     */
    private fun addImportPaths(commandLine: GeneralCommandLine, file: VirtualFile, sdk: CjSdk) {
        val addedPaths = mutableSetOf<String>()

        fun addPath(path: String) {
            if (path !in addedPaths && File(path).exists()) {
                addedPaths.add(path)
                commandLine.addParameter("--import-path")
                commandLine.addParameter(path)
            }
        }

        // 1. 尝试获取文件所在的项目目录
        val sourceFile = File(file.path)
        val projectDir = findProjectRoot(sourceFile)

        if (projectDir != null) {
            // 获取目标平台用于定位构建输出目录
            val targetPlatform = sdk.version?.targetPlatform

            // 1.1 添加目标平台特定的构建输出目录
            if (targetPlatform != null) {
                // release 模式输出
                val releaseDir = File(projectDir, "target/release/$targetPlatform")
                if (releaseDir.exists() && releaseDir.isDirectory) {
                    addPath(releaseDir.absolutePath)
                }

                // debug 模式输出
                val debugDir = File(projectDir, "target/debug/$targetPlatform")
                if (debugDir.exists() && debugDir.isDirectory) {
                    addPath(debugDir.absolutePath)
                }

                // .cjpm 目录下的输出
                val cjpmReleaseDir = File(projectDir, ".cjpm/target/release/$targetPlatform")
                if (cjpmReleaseDir.exists() && cjpmReleaseDir.isDirectory) {
                    addPath(cjpmReleaseDir.absolutePath)
                }
            }

            // 1.2 添加通用输出目录
            val possibleOutputDirs = listOf(
                File(projectDir, "target"),
                File(projectDir, "build"),
                File(projectDir, "out"),
                File(projectDir, ".cjpm/target"),
                projectDir // 项目根目录本身
            )

            for (outputDir in possibleOutputDirs) {
                if (outputDir.exists() && outputDir.isDirectory) {
                    addPath(outputDir.absolutePath)
                }
            }

            // 1.3 添加 src 目录的父目录（用于解析同项目包）
            val srcDir = findSrcDir(sourceFile)
            if (srcDir != null && srcDir.parentFile != null) {
                addPath(srcDir.parentFile.absolutePath)
            }
        }

        // 2. 添加 SDK 的标准库路径
        val sdkLibPath = sdk.homePath.resolve("lib")
        if (sdkLibPath.toFile().exists()) {
            addPath(sdkLibPath.toString())
        }

        // 3. 从环境变量获取额外的导入路径
        val cangjieHome = System.getenv("CANGJIE_HOME")
        if (!cangjieHome.isNullOrBlank()) {
            val homeLibPath = File(cangjieHome, "lib")
            if (homeLibPath.exists()) {
                addPath(homeLibPath.absolutePath)
            }
        }

        val cangjiePath = System.getenv("CANGJIE_PATH")
        if (!cangjiePath.isNullOrBlank()) {
            cangjiePath.split(File.pathSeparator).forEach { path ->
                if (path.isNotBlank()) {
                    addPath(path)
                }
            }
        }
    }

    /**
     * 查找项目根目录
     *
     * 通过查找 cjpm.toml 或其他项目标识文件来定位项目根目录
     */
    private fun findProjectRoot(file: File): File? {
        var current = file.parentFile
        val projectMarkers = listOf("cjpm.toml", ".git", ".idea", "build.gradle.kts", "build.gradle")

        while (current != null) {
            for (marker in projectMarkers) {
                if (File(current, marker).exists()) {
                    return current
                }
            }
            current = current.parentFile
        }
        return null
    }

    /**
     * 查找 src 目录
     */
    private fun findSrcDir(file: File): File? {
        var current = file.parentFile
        while (current != null) {
            if (current.name == "src") {
                return current
            }
            current = current.parentFile
        }
        return null
    }

    /**
     * 查找宏展开输出文件
     */
    private fun findMacroOutputFile(outputDir: File, baseName: String): File? {
        // 尝试查找 .macro.cj 后缀的文件
        val macroFile = File(outputDir, "$baseName.macro.cj")
        if (macroFile.exists()) {
            return macroFile
        }

        // 如果没有找到，尝试匹配其他可能的命名模式
        return outputDir.listFiles()?.firstOrNull { file ->
            file.name.contains(baseName) && file.extension == "cj"
        }
    }

    /**
     * 获取 cjc-frontend 路径
     *
     * 注意：`--debug-macro` 和 `--compile-macro` 选项属于 FRONTEND 组，
     * 必须使用 `cjc-frontend` 而不是 `cjc`
     */
    private fun getCompilerPath(sdk: CjSdk): Path {
        val os = System.getProperty("os.name").lowercase()
        val compilerName = if (os.contains("win")) "cjc-frontend.exe" else "cjc-frontend"
        return sdk.binPath.resolve(compilerName)
    }
}
