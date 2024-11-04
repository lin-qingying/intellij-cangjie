package com.linqingying.cangjie.cjpm.toolchain.tools

import com.linqingying.cangjie.cjpm.toolchain.CjToolchainBase
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.ElevationService
import com.intellij.util.io.systemIndependentPath
import java.nio.file.Path


abstract class CjTool(toolName: String, val toolchain: CjToolchainBase) {
    open val executable: Path = toolchain.pathToExecutable(toolName)




    protected fun createBaseCommandLine(
        vararg parameters: String,
        workingDirectory: Path? = null,
        environment: Map<String, String> = emptyMap()
    ): GeneralCommandLine = createBaseCommandLine(
        parameters.toList(),
        workingDirectory = workingDirectory,
        environment = environment
    )

    protected open fun createBaseCommandLine(
        parameters: List<String>,
        workingDirectory: Path? = null,
        environment: Map<String, String> = emptyMap()
    ): GeneralCommandLine = GeneralCommandLine(executable)
        .withWorkDirectory(workingDirectory)
        .withParameters(parameters)
        .withEnvironment(environment)
        .withCharset(Charsets.UTF_8)
        .also { toolchain.patchCommandLine(it) }
}

@Suppress("FunctionName", "UnstableApiUsage")
fun GeneralCommandLine(path: Path, withSudo: Boolean = false, vararg args: String) =
    object : GeneralCommandLine(path.systemIndependentPath, *args) {
        override fun createProcess(): Process = if (withSudo) {
            ElevationService.getInstance().createProcess(this)
        } else {
            super.createProcess()
        }
    }

fun GeneralCommandLine.withWorkDirectory(path: Path?) = withWorkDirectory(path?.systemIndependentPath)

abstract class CangJieComponent(componentName: String, toolchain: CjToolchainBase) : CjTool(componentName, toolchain) {

    val executionPath: String = executable.systemIndependentPath



}
