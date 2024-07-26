package com.huawei.cangjie.cjpm.toolchain.tools


import com.huawei.cangjie.cjpm.toolchain.CjToolchainBase
import com.huawei.cangjie.cjpm.toolchain.impl.CjcVersion
import com.huawei.cangjie.cjpm.toolchain.impl.parseCjcVersion
import com.huawei.cangjie.idea.project.tools.projectWizard.wizard.CjProcessResult
import com.huawei.cangjie.idea.run.cjpm.isUnitTestMode
import com.huawei.cangjie.idea.run.cjpm.runconfig.CjCapturingProcessHandler
import com.huawei.cangjie.idea.run.cjpm.runconfig.CjProcessExecutionException
import com.huawei.cangjie.idea.run.cjpm.runconfig.unwrapOrElse
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.execution.process.ProcessListener
import com.intellij.execution.process.ProcessOutput
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import java.nio.file.Path

//fun CjToolchainBase.cjc(): Cjc = Cjc(this)
//val CjToolchainBase.cjc :Cjc = toolchain::cjc

class Cjc(toolchain: CjToolchainBase) : CangJieComponent(NAME, toolchain) {
    var version: CjcVersion? = null

    init {
        toolchain.cjc = this
    }

    fun queryVersion(workingDirectory: Path? = null): CjcVersion? {
        try {
            if (!isUnitTestMode) {
                checkIsBackgroundThread()
            }
            val lines = createBaseCommandLine(
                "-v",
                workingDirectory = workingDirectory
            ).execute(toolchain.executionTimeoutInMilliseconds)?.stdoutLines
            version = lines?.let { parseCjcVersion(it) }
            return version
        } catch (e: CjProcessExecutionException) {

            return null
        }
    }

    fun queryVersion(
        workingDirectory: Path, owner: Disposable, listener: ProcessListener
    ): CjProcessResult<CjcVersion?> {
        if (!isUnitTestMode) {
            checkIsBackgroundThread()
        }
        return createBaseCommandLine("-v", workingDirectory = workingDirectory).execute(owner, listener = listener)
            .map {
                parseCjcVersion(it.stdoutLines)

//            version
            }
    }


    fun getSysroot(projectDirectory: Path): String? {
        if (!isUnitTestMode) {
            checkIsBackgroundThread()
        }
        val timeoutMs = 10000
        val output = createBaseCommandLine(
            "--print", "sysroot", workingDirectory = projectDirectory
        ).execute(timeoutMs)

        if (output?.isSuccess != true) return null
        return toolchain.toLocalPath(output.stdout.trim())
    }


    companion object {
        const val NAME: String = "bin/cjc"
    }
}

fun checkIsBackgroundThread() {
    check(!ApplicationManager.getApplication().isDispatchThread) {
        "Long running operation invoked on UI thread"
    }
}


private val LOG: Logger = Logger.getInstance("CommandLineExt")

fun CapturingProcessHandler.runProcessWithGlobalProgress(timeoutInMilliseconds: Int? = null): ProcessOutput {
    return runProcess(ProgressManager.getGlobalProgressIndicator(), timeoutInMilliseconds)
}

fun CapturingProcessHandler.runProcess(
    indicator: ProgressIndicator?, timeoutInMilliseconds: Int? = null
): ProcessOutput {
    return when {
        indicator != null && timeoutInMilliseconds != null -> runProcessWithProgressIndicator(
            indicator,
            timeoutInMilliseconds
        )

        indicator != null -> runProcessWithProgressIndicator(indicator)
        timeoutInMilliseconds != null -> runProcess(timeoutInMilliseconds)
        else -> runProcess()
    }
}

val ProcessOutput.isSuccess: Boolean get() = !isTimeout && !isCancelled && exitCode == 0

fun GeneralCommandLine.execute(timeoutInMilliseconds: Int?): ProcessOutput? {
    LOG.info("Executing `$commandLineString`")
    val handler = CjCapturingProcessHandler.startProcess(this).unwrapOrElse {
        LOG.warn("Failed to run executable", it)
        return null
    }
    val output = handler.runProcessWithGlobalProgress(timeoutInMilliseconds)

    if (!output.isSuccess) {
        LOG.warn(CjProcessExecutionException.errorMessage(commandLineString, output))
    }

    return output
}
