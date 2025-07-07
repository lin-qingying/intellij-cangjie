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

package cn.cangnova.cangjie.toolchain.impl

import cn.cangnova.cangjie.toolchain.api.*
import cn.cangnova.cangjie.toolchain.api.CjPackageManager.Companion.NAME
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

/**
 * 官方CangJie包管理器实现
 */
class OfficialCjPackageManager(override val toolchain: CjToolchain) : CjPackageManager {

    override val executable: Path = toolchain.pathToExecutable(NAME)

    override fun getVersion(): String {
        val process = ProcessBuilder(executable.toString(), "--version")
            .redirectErrorStream(true)
            .start()

        val output = process.inputStream.bufferedReader().readText()
        process.waitFor(toolchain.executionTimeoutInMilliseconds.toLong(), TimeUnit.MILLISECONDS)

        // 解析版本号，假设输出格式为 "CangJie Package Manager vX.Y.Z"
        val versionPattern = Pattern.compile("CangJie Package Manager v(\\d+\\.\\d+\\.\\d+)")
        val matcher = versionPattern.matcher(output)
        return if (matcher.find()) {
            matcher.group(1)
        } else {
            "unknown"
        }
    }

    override fun init(
        name: String,
        path: Path?,
        outputType: CjPackageManager.OutputType?,
        options: CjPackageOptions
    ): CjPackageResult {
        val command = mutableListOf(executable.toString(), "init", "--name", name)

        // 添加路径参数
        if (path != null) {
            command.add("--path")
            command.add(path.toString())
        }

        // 添加输出类型参数
        if (outputType != null) {
            command.add("--type")
            command.add(
                when (outputType) {
                    CjPackageManager.OutputType.EXECUTABLE -> "executable"
                    CjPackageManager.OutputType.STATIC -> "static"
                    CjPackageManager.OutputType.DYNAMIC -> "dynamic"
                }
            )
        }

        // 添加其他选项
        addPackageOptions(command, options)

        return executePackageCommand(command, CjPackageResult.OperationType.INIT)
    }

    override fun build(projectPath: Path, target: String?, options: CjPackageOptions): CjPackageResult {
        val command = mutableListOf(executable.toString(), "build")

        // 添加工作目录
        command.add("--cwd")
        command.add(projectPath.toString())

        // 添加目标平台
        if (target != null) {
            command.add("--target")
            command.add(target)
        }

        // 添加其他选项
        addPackageOptions(command, options)

        return executePackageCommand(command, CjPackageResult.OperationType.BUILD)
    }

    override fun run(projectPath: Path, args: List<String>, options: CjPackageOptions): CjPackageResult {
        val command = mutableListOf(executable.toString(), "run")

        // 添加工作目录
        command.add("--cwd")
        command.add(projectPath.toString())

        // 添加其他选项
        addPackageOptions(command, options)

        // 添加运行参数
        if (args.isNotEmpty()) {
            command.add("--")
            command.addAll(args)
        }

        return executePackageCommand(command, CjPackageResult.OperationType.RUN)
    }

    override fun test(
        projectPath: Path,
        testPaths: List<Path>?,
        target: String?,
        options: CjPackageOptions
    ): CjPackageResult {
        val command = mutableListOf(executable.toString(), "test")

        // 添加工作目录
        command.add("--cwd")
        command.add(projectPath.toString())

        // 添加目标平台
        if (target != null) {
            command.add("--target")
            command.add(target)
        }

        // 添加测试路径
        if (testPaths != null && testPaths.isNotEmpty()) {
            testPaths.forEach { path ->
                command.add(path.toString())
            }
        }

        // 添加其他选项
        addPackageOptions(command, options)

        return executePackageCommand(command, CjPackageResult.OperationType.TEST)
    }

    override fun clean(projectPath: Path, options: CjPackageOptions): CjPackageResult {
        val command = mutableListOf(executable.toString(), "clean")

        // 添加工作目录
        command.add("--cwd")
        command.add(projectPath.toString())

        // 添加其他选项
        addPackageOptions(command, options)

        return executePackageCommand(command, CjPackageResult.OperationType.CLEAN)
    }

    override fun install(packageName: String, version: String?, options: CjPackageOptions): CjPackageResult {
        val command = mutableListOf(executable.toString(), "install", packageName)

        // 添加版本要求
        if (version != null) {
            command.add("@$version")
        }

        // 添加安装选项
        addPackageOptions(command, options)

        return executePackageCommand(command, CjPackageResult.OperationType.INSTALL)
    }

    override fun installDependencies(projectPath: Path, options: CjPackageOptions): CjPackageResult {
        val command = mutableListOf(executable.toString(), "install")

        // 添加项目路径
        command.add("--cwd")
        command.add(projectPath.toString())

        // 添加安装选项
        addPackageOptions(command, options)

        return executePackageCommand(command, CjPackageResult.OperationType.INSTALL)
    }

    override fun update(packageName: String?, options: CjPackageOptions): CjPackageResult {
        val command = mutableListOf(executable.toString(), "update")

        // 添加包名称（如果指定）
        if (packageName != null) {
            command.add(packageName)
        }

        // 添加更新选项
        addPackageOptions(command, options)

        return executePackageCommand(command, CjPackageResult.OperationType.UPDATE)
    }

    override fun uninstall(packageName: String, options: CjPackageOptions): CjPackageResult {
        val command = mutableListOf(executable.toString(), "uninstall", packageName)

        // 添加卸载选项
        addPackageOptions(command, options)

        return executePackageCommand(command, CjPackageResult.OperationType.UNINSTALL)
    }

    private fun addPackageOptions(command: MutableList<String>, options: CjPackageOptions) {
        // 添加安装范围
        when (options.scope) {
            CjPackageOptions.InstallScope.GLOBAL -> command.add("--global")
            CjPackageOptions.InstallScope.PROJECT -> {} // 默认为项目级安装，不需要额外参数
        }

        // 添加依赖类型
        when (options.dependencyType) {
            CjPackageOptions.DependencyType.PRODUCTION -> {} // 默认为生产依赖，不需要额外参数
            CjPackageOptions.DependencyType.DEVELOPMENT -> command.add("--dev")
            CjPackageOptions.DependencyType.OPTIONAL -> command.add("--optional")
            CjPackageOptions.DependencyType.PEER -> command.add("--peer")
        }

        // 添加保存模式
        when (options.saveMode) {
            CjPackageOptions.SaveMode.NONE -> command.add("--no-save")
            CjPackageOptions.SaveMode.EXACT -> command.add("--save-exact")
            CjPackageOptions.SaveMode.COMPATIBLE -> command.add("--save")
        }

        // 添加目标目录
        if (options.targetDir != null) {
            command.add("--target-dir")
            command.add(options.targetDir!!)
        }

        // 添加目标平台
        if (options.target != null) {
            command.add("--target")
            command.add(options.target!!)
        }

        // 添加发布/调试模式
        if (options.release) {
            command.add("--release")
        } else {
            command.add("--debug")
        }

        // 添加工作目录
        if (options.workingDirectory != null) {
            command.add("--cwd")
            command.add(options.workingDirectory!!)
        }

        // 添加额外参数
        command.addAll(options.extraArgs)
    }

    private fun executePackageCommand(
        command: List<String>,
        operationType: CjPackageResult.OperationType
    ): CjPackageResult {
        try {
            val processBuilder = ProcessBuilder(command)
                .redirectErrorStream(true)

            val process = processBuilder.start()

            val output = process.inputStream.bufferedReader().readText()
            val completed = process.waitFor(toolchain.executionTimeoutInMilliseconds.toLong(), TimeUnit.MILLISECONDS)

            if (!completed) {
                process.destroyForcibly()
                return createErrorResult(
                    "Operation timed out after ${toolchain.executionTimeoutInMilliseconds} ms",
                    operationType
                )
            }

            val exitCode = process.exitValue()

            // 解析包管理器输出，获取包信息
            val packages = parsePackageInfo(output, operationType)

            return when (exitCode) {
                0 -> {
                    if (packages.isEmpty()) {
                        createNoChangeResult(output, operationType)
                    } else {
                        createSuccessResult(packages, output, operationType)
                    }
                }

                else -> {
                    if (packages.isNotEmpty()) {
                        createPartialSuccessResult(packages, output, operationType)
                    } else {
                        createFailureResult(output, operationType)
                    }
                }
            }
        } catch (e: Exception) {
            return createErrorResult("Operation failed: ${e.message}", operationType)
        }
    }

    private fun parsePackageInfo(output: String, operationType: CjPackageResult.OperationType): List<CjPackageInfo> {
        val packages = mutableListOf<CjPackageInfo>()

        // 根据操作类型选择不同的解析模式
        when (operationType) {
            CjPackageResult.OperationType.INSTALL -> {
                // 假设安装输出格式为：+ package@version
                val pattern = Pattern.compile("\\+ ([^@]+)@(\\d+\\.\\d+\\.\\d+)")
                output.lines().forEach { line ->
                    val matcher = pattern.matcher(line)
                    if (matcher.find()) {
                        packages.add(
                            OfficialCjPackageInfo(
                                name = matcher.group(1),
                                version = matcher.group(2),
                                dependencyType = CjPackageOptions.DependencyType.PRODUCTION, // 默认为生产依赖
                                status = CjPackageInfo.PackageStatus.INSTALLED
                            )
                        )
                    }
                }
            }

            CjPackageResult.OperationType.UPDATE -> {
                // 假设更新输出格式为：package@oldVersion -> package@newVersion
                val pattern = Pattern.compile("([^@]+)@(\\d+\\.\\d+\\.\\d+) -> \\1@(\\d+\\.\\d+\\.\\d+)")
                output.lines().forEach { line ->
                    val matcher = pattern.matcher(line)
                    if (matcher.find()) {
                        packages.add(
                            OfficialCjPackageInfo(
                                name = matcher.group(1),
                                version = matcher.group(3),
                                dependencyType = CjPackageOptions.DependencyType.PRODUCTION, // 默认为生产依赖
                                status = CjPackageInfo.PackageStatus.UPDATED
                            )
                        )
                    }
                }
            }

            CjPackageResult.OperationType.UNINSTALL -> {
                // 假设卸载输出格式为：- package@version
                val pattern = Pattern.compile("- ([^@]+)@(\\d+\\.\\d+\\.\\d+)")
                output.lines().forEach { line ->
                    val matcher = pattern.matcher(line)
                    if (matcher.find()) {
                        packages.add(
                            OfficialCjPackageInfo(
                                name = matcher.group(1),
                                version = matcher.group(2),
                                dependencyType = CjPackageOptions.DependencyType.PRODUCTION, // 默认为生产依赖
                                status = CjPackageInfo.PackageStatus.REMOVED
                            )
                        )
                    }
                }
            }

            else -> {} // 其他操作类型不解析包信息
        }

        return packages
    }

    private fun createSuccessResult(
        packages: List<CjPackageInfo>,
        output: String,
        operationType: CjPackageResult.OperationType
    ): CjPackageResult {
        return OfficialCjPackageResult(
            success = true,
            packages = packages,
            output = output,
            operationType = operationType,
            status = CjPackageResult.OperationStatus.SUCCESS
        )
    }

    private fun createPartialSuccessResult(
        packages: List<CjPackageInfo>,
        output: String,
        operationType: CjPackageResult.OperationType
    ): CjPackageResult {
        return OfficialCjPackageResult(
            success = true,
            packages = packages,
            output = output,
            operationType = operationType,
            status = CjPackageResult.OperationStatus.PARTIAL_SUCCESS
        )
    }

    private fun createFailureResult(output: String, operationType: CjPackageResult.OperationType): CjPackageResult {
        return OfficialCjPackageResult(
            success = false,
            packages = emptyList(),
            output = output,
            operationType = operationType,
            status = CjPackageResult.OperationStatus.FAILURE
        )
    }

    private fun createNoChangeResult(output: String, operationType: CjPackageResult.OperationType): CjPackageResult {
        return OfficialCjPackageResult(
            success = true,
            packages = emptyList(),
            output = output,
            operationType = operationType,
            status = CjPackageResult.OperationStatus.NO_CHANGE
        )
    }

    private fun createErrorResult(message: String, operationType: CjPackageResult.OperationType): CjPackageResult {
        return OfficialCjPackageResult(
            success = false,
            packages = emptyList(),
            output = message,
            operationType = operationType,
            status = CjPackageResult.OperationStatus.FAILURE
        )
    }
} 