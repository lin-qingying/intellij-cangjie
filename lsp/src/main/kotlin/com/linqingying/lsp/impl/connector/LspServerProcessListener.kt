package com.linqingying.lsp.impl.connector

import com.intellij.execution.impl.ExecutionManagerImpl
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessListener
import com.intellij.execution.process.ProcessOutputType
import com.intellij.openapi.application.ReadAction.compute
import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.ReflectionUtil
import com.intellij.util.io.IOUtil
import com.linqingying.lsp.impl.LspServerImpl
import com.linqingying.lsp.impl.LspServerManagerImpl
import java.io.*
import java.nio.charset.StandardCharsets

  open class LspServerProcessListenerBase(open val lspServer: LspServerImpl) :
    ProcessListener {

    override fun processTerminated(event: ProcessEvent): Unit {
        val exitCode = event.exitCode
        val commandLineInfo = "Exit code: $exitCode\nCommand line: ${event.processHandler}"
        lspServer.logInfo("LSP server process terminated. $commandLineInfo")

        val serverManager = compute<LspServerManagerImpl?, Throwable> {
            if (!lspServer.project.isDisposed) {
                LspServerManagerImpl.getInstanceImpl(lspServer.project)
            } else {
                null
            }
        }
        serverManager?.handleMaybeUnexpectedServerStop(lspServer, commandLineInfo)
    }

    override fun startNotified(event: ProcessEvent): Unit {
        lspServer.logInfo("LSP server process started: " + event.processHandler)

    }
}


  class LspServerProcessListener(override val lspServer: LspServerImpl) :
    LspServerProcessListenerBase(lspServer) {

    private val pipedOutputStream: PipedOutputStream = PipedOutputStream()

    val pipedInputStream: PipedInputStream = PipedInputStream(this.pipedOutputStream)

    private val outputStreamWriter: OutputStreamWriter =
        OutputStreamWriter(pipedOutputStream as OutputStream, StandardCharsets.UTF_8)

    override fun onTextAvailable(
        event: ProcessEvent,
        outputType: com.intellij.openapi.util.Key<*>
    ): Unit {
        if (ProcessOutputType.isStdout(outputType)) {
            try {
                outputStreamWriter.write(event.text)
                outputStreamWriter.flush()
            } catch (e: IOException) {
                val pipedInputStream = this.pipedInputStream
                val fieldsToDump = arrayOf("readSide", "writeSide", "closedByReader", "closedByWriter")
                val fieldDump = ReflectionUtil.dumpFields(PipedInputStream::class.java, pipedInputStream, *fieldsToDump)
                lspServer.logError("Problem proxying data to the listener: ${e.message}\nStopping LSP server process: ${event.processHandler}\n$fieldDump")
                ExecutionManagerImpl.stopProcess(event.processHandler)
            }
        } else if (ProcessOutputType.isStderr(outputType)) {
            val text = event.text
            requireNotNull(text) { "Text cannot be null" }
            val trimmedText = text.trimEnd()
            val nonEmptyText = if (trimmedText.isNotEmpty()) trimmedText else null

            nonEmptyText?.let {
                lspServer.logInfo("STDERR: $it")
                lspServer.appendServerErrorOutput(it)
            }
        }
    }

    override fun processTerminated(event: ProcessEvent): Unit {

        val log: Logger =
            Logger.getInstance(LspServerProcessListener::class.java)


        IOUtil.closeSafe(log, *arrayOf<Closeable>(this.outputStreamWriter, this.pipedOutputStream))
        super.processTerminated(event)
    }
}

