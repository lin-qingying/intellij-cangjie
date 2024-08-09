package com.linqingying.cangjie.ide.run

import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.target.*
import com.intellij.execution.target.value.TargetValue
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.NlsContexts
import com.linqingying.cangjie.CangJieBundle
import com.linqingying.cangjie.ide.run.cjpm.isUnitTestMode
import com.linqingying.cangjie.ide.run.cjpm.runconfig.CjCommandLineSetup


private val LOG: Logger = Logger.getInstance("com.linqingying.cangjie.ide.run.cjpm.runconfig.Utils")





fun <T> Project.computeWithCancelableProgress(
    @Suppress("UnstableApiUsage") @NlsContexts.ProgressTitle title: String,
    supplier: () -> T
): T {
    if (isUnitTestMode) {
        return supplier()
    }
    return ProgressManager.getInstance().runProcessWithProgressSynchronously<T, Exception>(supplier, title, true, this)
}




fun TargetEnvironmentRequest.prepareEnvironment(
    setup: CjCommandLineSetup,
    progressIndicator: ProgressIndicator
): TargetEnvironment {
    val targetProgressIndicator = object : TargetProgressIndicator {
        override fun isCanceled(): Boolean = progressIndicator.isCanceled
        override fun stop() = progressIndicator.cancel()
        override fun isStopped(): Boolean = isCanceled
        override fun addText(text: String, key: Key<*>) {
            progressIndicator.text2 = text.trim()
        }
    }

    return try {
        val environment = prepareEnvironment(targetProgressIndicator)
        setup.provideEnvironment(environment, targetProgressIndicator)
        environment
    } catch (e: ProcessCanceledException) {
        throw e
    } catch (e: Exception) {
        throw ExecutionException(
            CangJieBundle.message(
                "dialog.message.failed.to.prepare.remote.environment",
                e.localizedMessage
            ), e
        )
    }
}
