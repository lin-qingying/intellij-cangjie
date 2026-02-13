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
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
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
import kotlin.io.path.exists
import kotlin.io.path.isDirectory

/**
 * 编译器宏编译提供者
 *
 * 通过调用 `cjc-frontend --compile-macro` 命令来编译宏声明。
 *
 * ## 命令格式
 *
 * ```
 * cjc-frontend --compile-macro [options] <source-files>
 * ```
 *
 * **注意**: `--compile-macro` 选项属于 `GROUP(FRONTEND)`，只能通过 `cjc-frontend` 使用。
 *
 * ## 输出说明
 *
 * `--compile-macro` 会编译宏声明并生成 `.cjo` 文件到指定的输出目录。
 */
class CompilerMacroCompilationProvider(private val project: Project) {

    /**
     * 检查编译器是否可用
     */
    fun isAvailable(): Boolean {
        val sdk = CjProjectSdkConfig.getInstance(project).getProjectSdk()
        if (sdk == null || !sdk.isValid) {
            return false
        }

        val compilerPath = getCompilerPath(sdk)
        return compilerPath.toFile().exists()
    }

    /**
     * 编译指定文件中的宏声明
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

        return try {
            if (options.timeoutMs > 0) {
                withTimeout(options.timeoutMs) {
                    executeCompileMacro(sdk, listOf(file), options)
                }
            } else {
                executeCompileMacro(sdk, listOf(file), options)
            }
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            CjResult.Err(MacroCompilationError.Timeout(options.timeoutMs))
        } catch (e: Exception) {
            CjResult.Err(MacroCompilationError.InternalError(e.message ?: "未知错误", e))
        }
    }

    /**
     * 编译项目中的所有宏声明
     */
    suspend fun compileAllMacrosInProject(
        options: MacroCompilationOptions
    ): CjResult<MacroCompilationResult, MacroCompilationError> {
        val sdk = CjProjectSdkConfig.getInstance(project).getProjectSdk()
            ?: return CjResult.Err(MacroCompilationError.SdkNotConfigured())

        if (!sdk.isValid) {
            return CjResult.Err(MacroCompilationError.CompilerUnavailable("SDK 无效"))
        }

        // 查找项目中所有的 .cj 文件
        val projectDir = project.guessProjectDir()
            ?: return CjResult.Err(MacroCompilationError.InternalError("无法获取项目目录"))

        val sourceFiles = findSourceFiles(projectDir)
        if (sourceFiles.isEmpty()) {
            return CjResult.Err(MacroCompilationError.NoMacrosFound(projectDir.path))
        }

        return try {
            if (options.timeoutMs > 0) {
                withTimeout(options.timeoutMs) {
                    executeCompileMacro(sdk, sourceFiles, options)
                }
            } else {
                executeCompileMacro(sdk, sourceFiles, options)
            }
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            CjResult.Err(MacroCompilationError.Timeout(options.timeoutMs))
        } catch (e: Exception) {
            CjResult.Err(MacroCompilationError.InternalError(e.message ?: "未知错误", e))
        }
    }

    /**
     * 获取文件的编译输出目录
     */
    fun getOutputDirectory(file: VirtualFile): Path? {
        val sdk = CjProjectSdkConfig.getInstance(project).getProjectSdk() ?: return null
        val targetPlatform = sdk.version?.targetPlatform ?: return null

        val sourceFile = File(file.path)
        val projectDir = findProjectRoot(sourceFile) ?: return null

        // 默认输出到 target/release/<platform>/
        val outputDir = projectDir.toPath().resolve("target/release/$targetPlatform")
        return outputDir
    }

    /**
     * 检查文件是否需要重新编译
     */
    fun needsRecompilation(file: VirtualFile): Boolean {
        val outputDir = getOutputDirectory(file) ?: return true

        if (!outputDir.exists() || !outputDir.isDirectory()) {
            return true
        }

        // 检查是否有对应的 .cjo 文件
        val sourceFile = File(file.path)
        val baseName = sourceFile.nameWithoutExtension

        // 简单的时间戳检查
        val cjoFile = outputDir.resolve("$baseName.cjo")
        if (!cjoFile.exists()) {
            return true
        }

        val sourceModified = sourceFile.lastModified()
        val cjoModified = cjoFile.toFile().lastModified()

        return sourceModified > cjoModified
    }

    /**
     * 执行编译器 --compile-macro 命令
     */
    private suspend fun executeCompileMacro(
        sdk: CjSdk,
        files: List<VirtualFile>,
        options: MacroCompilationOptions
    ): CjResult<MacroCompilationResult, MacroCompilationError> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val compilerPath = getCompilerPath(sdk)

        // 确定输出目录
        val outputDir = options.outputDir ?: run {
            val firstFile = files.firstOrNull()
                ?: return@withContext CjResult.Err(MacroCompilationError.NoMacrosFound(""))
            getOutputDirectory(firstFile)
                ?: return@withContext CjResult.Err(MacroCompilationError.InternalError("无法确定输出目录"))
        }

        // 确保输出目录存在
        Files.createDirectories(outputDir)

        try {
            // 构建命令行
            val commandLine = GeneralCommandLine().apply {
                exePath = compilerPath.systemIndependentPath
                addParameter("--compile-macro")

                // 指定输出目录
                addParameter("--output-dir")
                addParameter(outputDir.toString())

                // 添加项目相关的导入路径
                files.firstOrNull()?.let { file ->
                    addImportPaths(this, file, sdk)
                }

                // 添加并行编译选项
                if (options.parallel) {
                    addParameter("--parallel-macro-expansion")
                }

                // 添加所有源文件
                files.forEach { file ->
                    addParameter(file.path)
                }

                charset = Charset.forName("UTF-8")

                // 配置环境变量
                environment.putAll(sdk.getEnvironment())
            }

            val handler = CapturingProcessHandler(commandLine)
            val output = handler.runProcess(
                (options.timeoutMs.takeIf { it > 0 } ?: 60000).toInt()
            )

            val compilationTime = System.currentTimeMillis() - startTime

            if (output.exitCode != 0) {
                return@withContext CjResult.Err(
                    MacroCompilationError.CompilationFailed(
                        commandLine = commandLine.commandLineString,
                        exitCode = output.exitCode,
                        stderr = output.stderr
                    )
                )
            }

            // 查找生成的 .cjo 文件
            val generatedFiles = findGeneratedCjoFiles(outputDir)

            CjResult.Ok(
                MacroCompilationResult(
                    compiledFiles = files.map { it.path },
                    outputFiles = generatedFiles,
                    outputDirectory = outputDir,
                    compilationTimeMs = compilationTime,
                    compilerOutput = output.stdout + output.stderr
                )
            )
        } catch (e: Exception) {
            CjResult.Err(
                MacroCompilationError.InternalError(
                    "执行编译器命令失败: ${e.message}",
                    e
                )
            )
        }
    }

    /**
     * 添加导入路径参数
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

        val sourceFile = File(file.path)
        val projectDir = findProjectRoot(sourceFile)

        if (projectDir != null) {
            val targetPlatform = sdk.version?.targetPlatform

            // 添加目标平台特定的输出目录
            if (targetPlatform != null) {
                val releaseDir = File(projectDir, "target/release/$targetPlatform")
                if (releaseDir.exists() && releaseDir.isDirectory) {
                    addPath(releaseDir.absolutePath)
                }

                val debugDir = File(projectDir, "target/debug/$targetPlatform")
                if (debugDir.exists() && debugDir.isDirectory) {
                    addPath(debugDir.absolutePath)
                }
            }

            // 添加通用输出目录
            val possibleOutputDirs = listOf(
                File(projectDir, "target"),
                File(projectDir, "build"),
                File(projectDir, "out"),
                projectDir
            )

            for (outputDir in possibleOutputDirs) {
                if (outputDir.exists() && outputDir.isDirectory) {
                    addPath(outputDir.absolutePath)
                }
            }

            // 添加 src 目录的父目录
            val srcDir = findSrcDir(sourceFile)
            if (srcDir != null && srcDir.parentFile != null) {
                addPath(srcDir.parentFile.absolutePath)
            }
        }

        // 添加 SDK 标准库路径
        val sdkLibPath = sdk.homePath.resolve("lib")
        if (sdkLibPath.toFile().exists()) {
            addPath(sdkLibPath.toString())
        }

        // 环境变量
        System.getenv("CANGJIE_HOME")?.takeIf { it.isNotBlank() }?.let {
            val homeLibPath = File(it, "lib")
            if (homeLibPath.exists()) {
                addPath(homeLibPath.absolutePath)
            }
        }

        System.getenv("CANGJIE_PATH")?.takeIf { it.isNotBlank() }?.let { path ->
            path.split(File.pathSeparator).forEach { p ->
                if (p.isNotBlank()) {
                    addPath(p)
                }
            }
        }
    }

    /**
     * 查找项目根目录
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
     * 查找生成的 .cjo 文件
     */
    private fun findGeneratedCjoFiles(outputDir: Path): List<Path> {
        if (!outputDir.exists() || !outputDir.isDirectory()) {
            return emptyList()
        }

        return Files.list(outputDir)
            .filter { it.toString().endsWith(".cjo") }
            .toList()
    }

    /**
     * 获取 cjc-frontend 路径
     */
    private fun getCompilerPath(sdk: CjSdk): Path {
        val os = System.getProperty("os.name").lowercase()
        val compilerName = if (os.contains("win")) "cjc-frontend.exe" else "cjc-frontend"
        return sdk.binPath.resolve(compilerName)
    }
}
