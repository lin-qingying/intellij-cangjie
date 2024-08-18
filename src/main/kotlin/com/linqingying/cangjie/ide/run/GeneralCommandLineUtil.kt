package com.linqingying.cangjie.ide.run

import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.PtyCommandLine
import com.intellij.execution.process.*
import com.intellij.execution.target.TargetEnvironmentConfiguration
import com.intellij.execution.target.TargetedCommandLine
import com.intellij.execution.target.TargetedCommandLineBuilder
import com.intellij.execution.target.value.TargetValue
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.util.io.BaseOutputReader
import com.linqingying.cangjie.CangJieBundle
import com.linqingying.cangjie.cjpm.project.CjToolchainPathChoosingComboBox
import com.linqingying.cangjie.ide.run.cjpm.runconfig.*
import com.pty4j.PtyProcess
import java.nio.charset.Charset


@Throws(ExecutionException::class)
fun GeneralCommandLine.createProcessHandler(): OSProcessHandler {
    return KillableColoredProcessHandler.Silent(this).apply {
        addProcessListener(
            object : ProcessListener {
                override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {

//                    val _text = event.text.toByteArray(charset("gb2312"))
//                    val correctString = String(_text, charset("UTF-8")) // 转换为 UTF-8

                    super.onTextAvailable(event, outputType)
                }
            }
        )
    }
//    return CjProcessHandler(this)
}


fun GeneralCommandLine.startProcess(
    project: Project,
    config: TargetEnvironmentConfiguration?,
    processColors: Boolean,
    uploadExecutable: Boolean
): ProcessHandler {
    if (config == null) {
        val handler = createProcessHandler()
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

class CjProcessHandler : KillableProcessHandler, AnsiEscapeDecoder.ColoredTextAcceptor {
    private val decoder: AnsiEscapeDecoder?

    constructor(commandLine: GeneralCommandLine, processColors: Boolean = true) : super(commandLine) {
        setHasPty(commandLine is PtyCommandLine)
        setShouldDestroyProcessRecursively(!hasPty())
        decoder = if (processColors && !hasPty()) CjAnsiEscapeDecoder() else null
    }

    constructor(
        process: Process,
        commandRepresentation: String,
        charset: Charset,
        processColors: Boolean = true
    ) : super(process, commandRepresentation, charset) {
        setHasPty(process is PtyProcess)
        setShouldDestroyProcessRecursively(!hasPty())
        decoder = if (processColors && !hasPty()) CjAnsiEscapeDecoder() else null
    }

    override fun notifyTextAvailable(text: String, outputType: Key<*>) {
        var textN = text

        if (!textN.contains("\r\n")) {
            textN = textN.replace("\n", "\r\n")
        }

        when (outputType) {
            ProcessOutputType.STDOUT -> {
                decoder?.escapeText(textN, outputType, this) ?: super.notifyTextAvailable(textN, outputType)
            }

            ProcessOutputType.SYSTEM -> {

            }

            ProcessOutputType.STDERR -> {

            }
        }
    }

    override fun coloredTextAvailable(text: String, attributes: Key<*>) {
        super.notifyTextAvailable(text, attributes)
    }

    override fun readerOptions(): BaseOutputReader.Options =
        if (hasPty()) {
            BaseOutputReader.Options.forTerminalPtyProcess()
        } else {
            BaseOutputReader.Options.forMostlySilentProcess()
        }
}
