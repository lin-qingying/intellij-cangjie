@file:Suppress("UnstableApiUsage")
package com.huawei.cangjie.ide.run.cjpm.runconfig

import com.huawei.cangjie.CangJieBundle
import com.huawei.cangjie.cjpm.project.CjToolchainPathChoosingComboBox
import com.huawei.cangjie.ide.run.cjpm.isUnitTestMode
import com.huawei.cangjie.ide.run.cjpm.languageRuntime
import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.execution.target.*
import com.intellij.execution.target.value.TargetValue
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.NlsContexts
import com.intellij.util.text.nullize
private val LOG: Logger = Logger.getInstance("com.huawei.cangjie.ide.run.cjpm.runconfig.Utils")





fun <T> Project.computeWithCancelableProgress(
    @Suppress("UnstableApiUsage") @NlsContexts.ProgressTitle title: String,
    supplier: () -> T
): T {
    if (isUnitTestMode) {
        return supplier()
    }
    return ProgressManager.getInstance().runProcessWithProgressSynchronously<T, Exception>(supplier, title, true, this)
}

fun GeneralCommandLine.startProcess(
    project: Project,
    config: TargetEnvironmentConfiguration?,
    processColors: Boolean,
    uploadExecutable: Boolean
): ProcessHandler {
    if (config == null) {
        val handler = CjProcessHandler(this)
        ProcessTerminatedListener.attach(handler)
        return handler
    }

    val request = config.createEnvironmentRequest(project)
    val setup = CjCommandLineSetup(request)
    val targetCommandLine = toTargeted(setup, uploadExecutable)
    val progressIndicator = ProgressManager.getInstance().progressIndicator ?: EmptyProgressIndicator()
    val environment =
        project.computeWithCancelableProgress(CangJieBundle.message("progress.title.preparing.remote.environment")) {
            request.prepareEnvironment(setup, progressIndicator)
        }
    val process = environment.createProcess(targetCommandLine, progressIndicator)

    val commandRepresentation = targetCommandLine.getCommandPresentation(environment)
    CjToolchainPathChoosingComboBox.LOG.debug("Executing command: `$commandRepresentation`")

    val handler = CjProcessHandler(process, commandRepresentation, targetCommandLine.charset, processColors)
    ProcessTerminatedListener.attach(handler)
    return handler
}


private fun GeneralCommandLine.toTargeted(
    setup: CjCommandLineSetup,
    uploadExecutable: Boolean
): TargetedCommandLine {
    val commandLineBuilder = TargetedCommandLineBuilder(setup.request)
    commandLineBuilder.charset = charset

    val targetedExePath = if (uploadExecutable) setup.requestUploadIntoTarget(exePath) else TargetValue.fixed(exePath)
    commandLineBuilder.exePath = targetedExePath

    val workDirectory = workDirectory
    if (workDirectory != null) {
        val targetWorkingDirectory = setup.requestUploadIntoTarget(workDirectory.absolutePath)
        commandLineBuilder.setWorkingDirectory(targetWorkingDirectory)
    }

    val inputFile = inputFile
    if (inputFile != null) {
        val targetInput = setup.requestUploadIntoTarget(inputFile.absolutePath)
        commandLineBuilder.setInputFile(targetInput)
    }

    commandLineBuilder.addParameters(parametersList.parameters)

    for ((key, value) in environment.entries) {
        commandLineBuilder.addEnvironmentVariable(key, value)
    }
//    val runtime = setup.request.configuration?.languageRuntime
//    commandLineBuilder.addEnvironmentVariable("CJC", runtime?.cjcPath?.nullize(true))
//    commandLineBuilder.addEnvironmentVariable("CJPM", runtime?.cjpmPath?.nullize(true))


    return commandLineBuilder.build()
}

private fun TargetEnvironmentRequest.prepareEnvironment(
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
