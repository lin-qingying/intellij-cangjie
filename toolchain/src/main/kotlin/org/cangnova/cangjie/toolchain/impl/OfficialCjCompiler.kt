/*
 * Copyright 2025 LinQingYing. and contributors.
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

package org.cangnova.cangjie.toolchain.impl

import org.cangnova.cangjie.toolchain.CangJieVersion
import org.cangnova.cangjie.toolchain.api.*
import org.cangnova.cangjie.toolchain.api.CjCompiler.Companion.NAME
import com.intellij.util.text.SemVer
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

/**
 * 官方CangJie编译器实现
 */
class OfficialCjCompiler(override val toolchain: CjToolchain) : CjCompiler {

    override val executable: Path = toolchain.pathToExecutable(NAME)

    override fun getVersion(): CangJieVersion? {
        val process = ProcessBuilder(executable.toString(), "--version")
            .redirectErrorStream(true)
            .start()

        val lines = process.inputStream.bufferedReader().readLines()
        process.waitFor(toolchain.executionTimeoutInMilliseconds.toLong(), TimeUnit.MILLISECONDS)


        val cangjieComiler = """Cangjie Compiler: (\d+\.\d+\.\d+.*)""".toRegex()


        val find = { re: Regex -> lines.firstNotNullOfOrNull { re.matchEntire(it) } }
        val releaseMatch = find(cangjieComiler) ?: return null

        val hostRe = "Target:(.*)".toRegex()
        val hostText = find(hostRe)?.groups?.get(1)?.value?.trim() ?: return null
        var versionText = releaseMatch.groups[1]?.value ?: return null


//    分割
        var type: String? = null
        return try {

//去掉括号
            val typeRegex = Regex("\\(([^()]*)\\)")

            type = typeRegex.find(versionText.split(" ")[1])?.groups?.get(1)?.value
            versionText = versionText.split(" ")[0]

            val semVer = SemVer.parseFromText(versionText) ?: return null
            CangJieVersion(semVer, hostText, type)

        } catch (iex: IndexOutOfBoundsException) {
            null
        }

    }

    override fun compile(sourcePath: Path, outputPath: Path, options: CjCompileOptions): CjCompileResult {
        return compileFiles(listOf(sourcePath), outputPath, options)
    }

    override fun compileFiles(sourcePaths: List<Path>, outputPath: Path, options: CjCompileOptions): CjCompileResult {
        // 确保输出目录存在
        Files.createDirectories(outputPath)

        val command = mutableListOf(executable.toString())

        // 添加源文件路径
        sourcePaths.forEach { command.add(it.toString()) }

        // 添加输出路径
        command.add("-o")
        command.add(outputPath.toString())

        // 添加编译选项
        addCompileOptions(command, options)

        return executeCompileCommand(command, outputPath)
    }

    override fun compileProject(projectPath: Path, options: CjCompileOptions): CjCompileResult {
        // 确保项目目录存在
        if (!Files.exists(projectPath) || !Files.isDirectory(projectPath)) {
            return createErrorResult("Project directory does not exist: $projectPath")
        }

        // 查找项目配置文件
//        val configFile = projectPath.resolve("cangjie.json")
//        if (!Files.exists(configFile)) {
//            return createErrorResult("Project configuration file not found: $configFile")
//        }

        val command = mutableListOf(executable.toString(), "build")

        // 添加项目路径
        command.add("--project")
        command.add(projectPath.toString())

        // 添加编译选项
        addCompileOptions(command, options)

        // 项目构建通常会输出到项目目录下的build或dist目录
        val outputPath = projectPath.resolve("build")
        Files.createDirectories(outputPath)

        return executeCompileCommand(command, outputPath)
    }

    private fun executeCompileCommand(command: List<String>, outputPath: Path): CjCompileResult {
        try {
            val process = ProcessBuilder(command)
                .redirectErrorStream(true)
                .start()

            val output = process.inputStream.bufferedReader().readText()
            val completed = process.waitFor(toolchain.executionTimeoutInMilliseconds.toLong(), TimeUnit.MILLISECONDS)

            if (!completed) {
                process.destroyForcibly()
                return createTimeoutResult(output)
            }

            val exitCode = process.exitValue()

            // 解析编译器输出，获取诊断信息
            val diagnostics = parseDiagnostics(output)

            // 查找生成的输出文件
            val outputFiles = findOutputFiles(outputPath)

            return when {
                exitCode == 0 && diagnostics.none { it.level == CjDiagnostic.Level.ERROR } -> {
                    if (diagnostics.any { it.level == CjDiagnostic.Level.WARNING }) {
                        createSuccessResultWithWarnings(outputFiles, diagnostics, output)
                    } else {
                        createSuccessResult(outputFiles, diagnostics, output)
                    }
                }

                else -> createFailureResult(diagnostics, output)
            }
        } catch (e: Exception) {
            return createErrorResult("Compilation failed: ${e.message}")
        }
    }

    private fun parseDiagnostics(output: String): List<CjDiagnostic> {
        val diagnostics = mutableListOf<CjDiagnostic>()

        // 假设编译器输出格式为：[ERROR|WARNING|INFO] file:line:column: message
        val pattern = Pattern.compile("\\[(ERROR|WARNING|INFO|HINT)\\] ([^:]+):(\\d+):(\\d+): (.*)")

        output.lines().forEach { line ->
            val matcher = pattern.matcher(line)
            if (matcher.find()) {
                val level = when (matcher.group(1)) {
                    "ERROR" -> CjDiagnostic.Level.ERROR
                    "WARNING" -> CjDiagnostic.Level.WARNING
                    "INFO" -> CjDiagnostic.Level.INFO
                    "HINT" -> CjDiagnostic.Level.HINT
                    else -> CjDiagnostic.Level.INFO
                }

                val filePath = matcher.group(2)
                val lineNum = matcher.group(3).toIntOrNull()
                val column = matcher.group(4).toIntOrNull()
                val message = matcher.group(5)

                diagnostics.add(
                    OfficialCjDiagnostic(
                        level = level,
                        message = message,
                        filePath = Path.of(filePath),
                        line = lineNum,
                        column = column,
                        type = determineType(message)
                    )
                )
            }
        }

        return diagnostics
    }

    private fun determineType(message: String): CjDiagnostic.DiagnosticType {
        return when {
            message.contains("syntax") -> CjDiagnostic.DiagnosticType.SYNTAX
            message.contains("type") -> CjDiagnostic.DiagnosticType.TYPE
            message.contains("undefined") || message.contains("not found") -> CjDiagnostic.DiagnosticType.NAME_RESOLUTION
            message.contains("internal") -> CjDiagnostic.DiagnosticType.INTERNAL
            else -> CjDiagnostic.DiagnosticType.OTHER
        }
    }

    private fun findOutputFiles(outputPath: Path): List<Path> {
        return Files.walk(outputPath)
            .filter { Files.isRegularFile(it) }
            .filter { it.toString().endsWith(".cj.out") || it.toString().endsWith(".cjm") }
            .toList()
    }

    private fun createSuccessResult(
        outputFiles: List<Path>,
        diagnostics: List<CjDiagnostic>,
        output: String
    ): CjCompileResult {
        return OfficialCjCompileResult(
            success = true,
            outputFiles = outputFiles,
            diagnostics = diagnostics,
            output = output,
            status = CjCompileResult.CompilationStatus.SUCCESS
        )
    }

    private fun createSuccessResultWithWarnings(
        outputFiles: List<Path>,
        diagnostics: List<CjDiagnostic>,
        output: String
    ): CjCompileResult {
        return OfficialCjCompileResult(
            success = true,
            outputFiles = outputFiles,
            diagnostics = diagnostics,
            output = output,
            status = CjCompileResult.CompilationStatus.WARNING
        )
    }

    private fun createFailureResult(diagnostics: List<CjDiagnostic>, output: String): CjCompileResult {
        return OfficialCjCompileResult(
            success = false,
            outputFiles = emptyList(),
            diagnostics = diagnostics,
            output = output,
            status = CjCompileResult.CompilationStatus.FAILURE
        )
    }

    private fun createErrorResult(message: String): CjCompileResult {
        val diagnostic = OfficialCjDiagnostic(
            level = CjDiagnostic.Level.ERROR,
            message = message,
            filePath = null,
            line = null,
            column = null,
            type = CjDiagnostic.DiagnosticType.OTHER
        )

        return OfficialCjCompileResult(
            success = false,
            outputFiles = emptyList(),
            diagnostics = listOf(diagnostic),
            output = message,
            status = CjCompileResult.CompilationStatus.FAILURE
        )
    }

    private fun createTimeoutResult(output: String): CjCompileResult {
        val diagnostic = OfficialCjDiagnostic(
            level = CjDiagnostic.Level.ERROR,
            message = "Compilation timed out after ${toolchain.executionTimeoutInMilliseconds} ms",
            filePath = null,
            line = null,
            column = null,
            type = CjDiagnostic.DiagnosticType.OTHER
        )

        return OfficialCjCompileResult(
            success = false,
            outputFiles = emptyList(),
            diagnostics = listOf(diagnostic),
            output = output,
            status = CjCompileResult.CompilationStatus.TIMEOUT
        )
    }

    /**
     * 添加编译选项到命令行
     */
    private fun addCompileOptions(command: MutableList<String>, options: CjCompileOptions) {
        // 调试选项
        if (options.debug) {
            command.add("--debug")
        }

        // 优化级别
        when (options.optimizationLevel) {
            CjCompileOptions.OptimizationLevel.NONE -> command.add("-O0")
            CjCompileOptions.OptimizationLevel.BASIC -> command.add("-O1")
            CjCompileOptions.OptimizationLevel.MEDIUM -> command.add("-O2")
            CjCompileOptions.OptimizationLevel.FULL -> command.add("-O3")
        }

        // 警告级别
        when (options.warningLevel) {
            CjCompileOptions.WarningLevel.NONE -> command.add("-w")
            CjCompileOptions.WarningLevel.NORMAL -> {} // 默认级别，不需要额外参数
            CjCompileOptions.WarningLevel.ALL -> command.add("-Wall")
            CjCompileOptions.WarningLevel.ERROR -> command.add("-Werror")
        }

        // 目标平台
        options.targetPlatform?.let {
            when (it) {
                CjCompileOptions.TargetPlatform.JVM -> command.add("--target=jvm")
                CjCompileOptions.TargetPlatform.NATIVE -> command.add("--target=native")
                CjCompileOptions.TargetPlatform.WASM -> command.add("--target=wasm")
                CjCompileOptions.TargetPlatform.JS -> command.add("--target=js")
            }
        }

        // 实验性功能
        if (options.experimental) {
            command.add("--experimental")
        }

        // 增量编译（需要实验性功能支持）
        if (options.incrementalCompile) {
            command.add("--incremental-compile")
        }

        // 链接器选项
        if (options.linkOptions.isNotEmpty()) {
            options.linkOptions.forEach { option ->
                command.add("--link-options")
                command.add(option)
            }
        }

        // 代码覆盖率选项
        options.sanitizerCoverage?.let { coverage ->
            if (coverage.basicBlockCoverage) {
                command.add("--sanitizer-coverage=bb")
            }

            if (coverage.edgeCoverage) {
                command.add("--sanitizer-coverage=edge")
            }

            if (coverage.eightBitCounters) {
                command.add("--sanitizer-coverage=8bit-counters")
            }

            if (coverage.tracePcGuard) {
                command.add("--sanitizer-coverage=trace-pc-guard")
            }

            if (coverage.functionEntryCoverage) {
                command.add("--sanitizer-coverage=function")
            }

            if (coverage.stackDepthCoverage) {
                command.add("--sanitizer-coverage-stack-depth")
            }

            if (coverage.traceCompares) {
                command.add("--sanitizer-coverage-trace-compares")
            }

            if (coverage.traceMemcmp) {
                command.add("--sanitizer-coverage-trace-memcmp")
            }
        }

        // 添加额外参数
        command.addAll(options.extraArgs)
    }
} 