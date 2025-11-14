package org.cangnova.cangjie.protodebugger.process


import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.*
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressIndicatorProvider
import com.intellij.util.containers.ContainerUtil


object RunProcessUtil {
    const val READ_VERSION_TIMEOUT = 10000

    @JvmStatic
    fun runProcess(handler: BaseProcessHandler<*>, indicator: ProgressIndicator?, timeout: Int): ProcessOutput {
        return runProcess(handler as ProcessHandler, indicator, timeout)
    }

    @JvmStatic
    fun runProcess(handler: ProcessHandler, indicator: ProgressIndicator?, timeout: Int): ProcessOutput {
        val processOutput = if (indicator == null) {
            CapturingProcessRunner(handler).runProcess(timeout)
        } else {
            CapturingProcessRunner(handler).runProcess(indicator, timeout)
        }
        return processOutput
    }

    @JvmStatic
    fun getProgressIndicator(): ProgressIndicator? {
        return ProgressIndicatorProvider.getGlobalProgressIndicator()
    }

    @JvmStatic
    fun runWithProgress(handler: CapturingProcessHandler): ProcessOutput {
        return runWithProgress(handler, 0)
    }

    @JvmStatic
    fun runWithProgress(handler: BaseProcessHandler<*>, timeout: Int): ProcessOutput {
        return runWithProgress(handler as ProcessHandler, timeout)
    }

    @JvmStatic
    fun runWithProgress(handler: ProcessHandler, timeout: Int): ProcessOutput {
        val indicator = getProgressIndicator()
        val result = runProcess(handler, indicator, timeout)
        if (result.isCancelled) {
            throw ProcessCanceledException()
        }
        return result
    }

    @JvmStatic
    fun readOneLineWithProgress(commandLine: GeneralCommandLine, timeout: Int): String? {
        return try {
            val output = runWithProgress(CapturingProcessHandler(commandLine), timeout)
            ContainerUtil.getFirstItem(output.stdout.lines())
        } catch (e: ExecutionException) {
            null
        }
    }

    @JvmStatic
    fun setHasPty(processHandler: OSProcessHandler, usePty: Boolean) {
        if (usePty) {
            processHandler.setHasPty(true)
            processHandler.setShouldDestroyProcessRecursively(false)
        }
    }
}
