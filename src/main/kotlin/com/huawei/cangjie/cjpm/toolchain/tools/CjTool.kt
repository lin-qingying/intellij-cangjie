package com.huawei.cangjie.cjpm.toolchain.tools

import com.huawei.cangjie.cjpm.toolchain.CjToolchainBase
import com.huawei.cangjie.ide.run.cjpm.runconfig.ExecuteResult
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.ElevationService
import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.io.systemIndependentPath
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.util.*
import java.util.concurrent.Callable


abstract class CjTool(toolName: String, val toolchain: CjToolchainBase) {
    open val executable: Path = toolchain.pathToExecutable(toolName)

    companion object {
        val LOG = Logger.getInstance(CjTool::class.java)
    }

    private class StreamCallable(private val stream: InputStream) : Callable<String> {
        override fun call(): String {
            val retString = StringBuilder()

            try {
                val brInputStream = BufferedReader(InputStreamReader(this.stream, StandardCharsets.UTF_8))

                val str: String =
                    try {
                        var line: String?
                        while ((brInputStream.readLine().also { line = it }) != null) {
                            retString.append(line)
                            retString.append(System.lineSeparator())
                        }

                        retString.toString()
                    } catch (e: Throwable) {
                        try {
                            brInputStream.close()
                        } catch (var5: Throwable) {
                            e.addSuppressed(var5)
                        }

                        throw e
                    }

                brInputStream.close()
                return str
            } catch (ioe: IOException) {
                LOG.warn("Error reading next line!!")
                return ""
            }
        }
    }

    /**
     * 执行命令
     */
    fun executeCommand(vararg args: String): Optional<ExecuteResult> {

        try {

            val processOutput =
                createBaseCommandLine(parameters = args).execute(toolchain.executionTimeoutInMilliseconds)
                    ?: return Optional.empty()



            return Optional.of(ExecuteResult(processOutput.exitCode, processOutput.stdout))


        } catch (e: Exception) {
            return Optional.empty()
        }



    }

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
//        override fun createProcess(): Process = if (withSudo) {
//            ElevationService.getInstance().createProcess(this)
//        } else {
//            super.createProcess()
//        }
    }

fun GeneralCommandLine.withWorkDirectory(path: Path?) = withWorkDirectory(path?.systemIndependentPath)

abstract class CangJieComponent(componentName: String, toolchain: CjToolchainBase) : CjTool(componentName, toolchain) {

    val executionPath: String = executable.systemIndependentPath


}
