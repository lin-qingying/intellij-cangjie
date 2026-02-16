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
import org.cangnova.cangjie.macro.service.*
import org.cangnova.cangjie.result.CjResult
import org.cangnova.cangjie.toolchain.api.CjProjectSdkConfig
import org.cangnova.cangjie.toolchain.api.CjSdk
import java.io.File
import java.nio.charset.Charset
import java.nio.file.Path

/**
 * 编译器宏展开提供者
 *
 * 通过调用 `cjc-frontend --debug-macro` 命令来展开宏。
 * 支持在展开前自动编译宏声明（通过 `cjc --compile-macro`）。
 *
 * ## 工作流程
 *
 * 1. 如果启用了自动编译（`autoCompileMacros = true`）：
 *    - 先执行 `cjc -p <pkg_dir> --compile-macro -o <pkg_dir>` 编译宏声明
 *    - 在宏包源码目录下生成动态库 `lib-macro_<name>.dll/.so/.dylib`
 *
 * 2. 执行 `cjc-frontend --debug-macro` 展开宏：
 *    - 通过 `--import-path` 指向宏包目录，让编译器加载动态库
 *    - 生成 `<filename>.macro.cj` 文件
 *
 * ## 命令格式
 *
 * ```
 * cjc -p <pkg_dir> --compile-macro -o <pkg_dir>    (编译宏包)
 * cjc-frontend --debug-macro [options] <source-file> (展开宏)
 * ```
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
        // 编译器 --debug-macro 是文件级别的操作，生成整个文件的展开结果
        val allResults = expandAllMacrosInFile(project, file, options)

        return when (allResults) {
            is CjResult.Ok -> {
                if (allResults.ok.isEmpty()) {
                    return CjResult.Err(MacroExpansionError.MacroNotFound("offset=$offset"))
                }

                // 优先查找包含指定偏移量的结果
                val matchingResult = allResults.ok.find { result ->
                    result.startOffset != result.endOffset &&
                        offset >= result.startOffset && offset < result.endOffset
                }

                // --debug-macro 生成的是整个文件的展开结果，没有单个宏的偏移量，
                // 所以如果没有精确匹配，直接返回第一个结果
                CjResult.Ok(matchingResult ?: allResults.ok.first())
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
     * 编译项目中的宏定义包
     *
     * 查找项目中包含 `macro package` 的源文件并编译，
     * 而不是编译调用宏的文件本身。
     *
     * @return 编译结果
     */
    private suspend fun compileMacrosIfNeeded(
        file: VirtualFile,
        options: MacroExpansionOptions
    ): CjResult<MacroCompilationResult, MacroCompilationError> {
        val compileOptions = MacroCompilationOptions(
            forceRecompile = options.forceRecompile,
            timeoutMs = options.timeoutMs / 2,
            parallel = true
        )

        // 编译项目中所有的宏定义包（而非调用宏的文件）
        return compilationProvider.compileAllMacrosInProject(compileOptions)
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

        // .macrocall 文件由编译器生成在源文件旁边（file.cj -> file.cj.macrocall）
        // 这是编译器的固定行为，无法通过参数改变
        val macroCallFile = File("$filePath.macrocall")

        try {
            // 构建命令行
            val commandLine = GeneralCommandLine().apply {
                exePath = compilerPath.systemIndependentPath
                addParameter("--debug-macro")

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
                (options.timeoutMs.takeIf { it > 0 }
                    ?: Registry.intValue("cangjie.macro.expansion.timeout.ms").toLong()).toInt()
            )

            // 收集编译器错误信息（即使失败，.macrocall 文件可能已生成）
            val compilerDiagnostics = mutableListOf<MacroDiagnostic>()
            if (output.exitCode != 0) {
                val stderr = output.stderr
                val errorMessage = when {
                    stderr.contains("can not find package") -> {
                        val packageMatch = Regex("can not find package '([^']+)'").find(stderr)
                        val packageName = packageMatch?.groupValues?.getOrNull(1) ?: "未知"
                        "找不到包 '$packageName'。请确保：\n" +
                            "1. 已执行 'cjpm build' 构建项目（或启用自动编译选项）\n" +
                            "2. 依赖包的 .cjo 文件存在于构建输出目录下\n" +
                            "原始错误: $stderr"
                    }
                    else -> stderr
                }

                // 即使编译器返回错误，只要 .macrocall 文件已生成，仍视为展开成功
                if (!macroCallFile.exists()) {
                    return@withContext CjResult.Err(
                        MacroExpansionError.ProcessExecutionError(
                            commandLine = commandLine.commandLineString,
                            exitCode = output.exitCode,
                            stderr = errorMessage
                        )
                    )
                }

                // .macrocall 文件存在，将错误信息记录到诊断中
                logger.warn("宏展开命令返回非零退出码 (${output.exitCode})，但 .macrocall 文件已生成: $stderr")
                compilerDiagnostics.add(
                    MacroDiagnostic(
                        level = DiagnosticLevel.ERROR,
                        message = errorMessage
                    )
                )
            }

            // 读取编译器生成的 .macrocall 文件
            // 编译器将展开结果写入 <source_file>.macrocall（在源文件旁边）
            if (!macroCallFile.exists()) {
                // 没有生成 .macrocall 文件，可能文件中没有宏
                return@withContext CjResult.Ok(emptyList())
            }

            // 规范化换行符：IntelliJ DocumentImpl 不接受 \r\n
            val expandedCode = macroCallFile.readText(Charsets.UTF_8).replace("\r\n", "\n").replace("\r", "\n")

            // 解析编译器输出中的宏展开标记
            val parsedBlocks = MacroCallFileParser.parse(expandedCode)

            if (parsedBlocks.isEmpty()) {
                // 无标记格式，回退到原始行为（向后兼容）
                val result = MacroExpansionResult(
                    filePath = filePath,
                    startOffset = 0,
                    endOffset = 0,
                    expandedText = expandedCode,
                    diagnostics = compilerDiagnostics,
                    source = ExpansionSource.COMPILER
                )
                CjResult.Ok(listOf(result))
            } else {
                // 读取源文件内容用于计算偏移量
                val sourceContent = sourceFile.readText(Charsets.UTF_8)

                val results = parsedBlocks.map { block ->
                    val (startOff, endOff) = calculateOffsets(sourceContent, block.line, block.column)
                    MacroExpansionResult(
                        filePath = filePath,
                        startOffset = startOff,
                        endOffset = endOff,
                        expandedText = block.expandedText,
                        macroName = block.macroName,
                        sourceFileName = block.sourceFileName,
                        diagnostics = compilerDiagnostics,
                        source = ExpansionSource.COMPILER
                    )
                }
                CjResult.Ok(results)
            }
        } catch (e: Exception) {
            CjResult.Err(
                MacroExpansionError.InternalError(
                    "执行编译器命令失败: ${e.message}",
                    e
                )
            )
        } finally {
            // 清理编译器生成的 .macrocall 文件，避免污染源码目录
            if (macroCallFile.exists()) {
                macroCallFile.delete()
            }
        }
    }

    /**
     * 将行列号转换为字符偏移量
     *
     * @param content 源文件内容
     * @param line 行号（1-based）
     * @param column 列号（1-based）
     * @return (startOffset, endOffset)，endOffset 指向该行末尾
     */
    private fun calculateOffsets(content: String, line: Int, column: Int): Pair<Int, Int> {
        if (line <= 0) return Pair(0, 0)

        val lines = content.lines()
        if (line > lines.size) return Pair(content.length, content.length)

        // 计算目标行的起始偏移量（累加前面所有行的长度 + 换行符）
        var lineStartOffset = 0
        for (i in 0 until line - 1) {
            lineStartOffset += lines[i].length + 1 // +1 for '\n'
        }

        val targetLine = lines[line - 1]
        val colOffset = (column - 1).coerceIn(0, targetLine.length)
        val startOffset = lineStartOffset + colOffset
        val endOffset = lineStartOffset + targetLine.length

        return Pair(startOffset, endOffset)
    }

    /**
     * 添加导入路径参数
     *
     * 根据文件所在项目添加必要的 --import-path 参数
     *
     * ## 导入路径优先级
     * 1. 编译后的宏包目录（包含 `lib-macro_*.dll/.so` 的目录）
     * 2. 目标平台构建输出目录 (`<target-dir>/release/<targetPlatform>` 和 `<target-dir>/debug/<targetPlatform>`)
     * 3. 通用输出目录 (`<target-dir>`, `build`, `out`, `.cjpm/<target-dir>`)
     * 4. 项目根目录
     * 5. SDK 标准库路径
     * 6. 环境变量中的路径 (CANGJIE_HOME, CANGJIE_PATH)
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

        // 0. 添加编译后的宏包目录（最高优先级）
        // 这些目录包含 lib-macro_*.dll/.so 动态库
        for (macroDir in compilationProvider.compiledMacroPackageDirs) {
            addPath(macroDir)
        }

        // 1. 尝试获取文件所在的项目目录
        val sourceFile = File(file.path)
        val projectDir = findProjectRoot(sourceFile)

        if (projectDir != null) {
            // 获取目标平台用于定位构建输出目录
            val targetPlatform = sdk.version?.targetPlatform
            // 从 cjpm.toml 读取 target-dir 配置
            val targetDir = resolveTargetDir(projectDir)

            // 1.1 添加目标平台特定的构建输出目录
            if (targetPlatform != null) {
                // release 模式输出
                val releaseDir = File(projectDir, "$targetDir/release/$targetPlatform")
                if (releaseDir.exists() && releaseDir.isDirectory) {
                    addPath(releaseDir.absolutePath)
                }

                // debug 模式输出
                val debugDir = File(projectDir, "$targetDir/debug/$targetPlatform")
                if (debugDir.exists() && debugDir.isDirectory) {
                    addPath(debugDir.absolutePath)
                }

                // .cjpm 目录下的输出
                val cjpmReleaseDir = File(projectDir, ".cjpm/$targetDir/release/$targetPlatform")
                if (cjpmReleaseDir.exists() && cjpmReleaseDir.isDirectory) {
                    addPath(cjpmReleaseDir.absolutePath)
                }
            }

            // 1.2 添加通用输出目录
            val possibleOutputDirs = listOf(
                File(projectDir, targetDir),
                File(projectDir, "build"),
                File(projectDir, "out"),
                File(projectDir, ".cjpm/$targetDir"),
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
     * 从 cjpm.toml 解析构建输出目录名
     *
     * 读取 `cjpm.toml` 中 `[package]` 的 `target-dir` 配置，
     * 默认返回 `"target"`，与 `CjpmBuildConfigurationImpl.outputDir` 逻辑一致。
     */
    private fun resolveTargetDir(projectDir: File): String {
        val tomlFile = File(projectDir, "cjpm.toml")
        if (!tomlFile.exists()) return "target"
        return try {
            val content = tomlFile.readText(Charsets.UTF_8)
            val targetDirRegex = Regex("""target-dir\s*=\s*"([^"]+)"""")
            targetDirRegex.find(content)?.groupValues?.getOrNull(1)?.takeIf { it.isNotEmpty() } ?: "target"
        } catch (_: Exception) {
            "target"
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
