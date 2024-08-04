package com.linqingying.cangjie.ide.run.cjpm.runconfig

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.PtyCommandLine
import com.intellij.execution.process.AnsiEscapeDecoder
import com.intellij.execution.process.KillableProcessHandler
import com.intellij.execution.process.ProcessOutputType
import com.intellij.openapi.util.Key
import com.intellij.util.io.BaseOutputReader
import com.pty4j.PtyProcess
import java.nio.charset.Charset

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
