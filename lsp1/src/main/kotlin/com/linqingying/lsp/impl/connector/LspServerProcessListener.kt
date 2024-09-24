package com.linqingying.lsp.impl.connector

import com.intellij.execution.ExecutionException
import com.intellij.execution.impl.ExecutionManagerImpl
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessListener
import com.intellij.execution.process.ProcessOutputType
import com.intellij.openapi.diagnostic.Logger
import java.io.IOException
import java.io.OutputStreamWriter
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.nio.charset.StandardCharsets
import com.intellij.openapi.util.Key
import com.intellij.util.ReflectionUtil

class LspServerProcessListener(private val processHandler: OSProcessHandler) : ProcessListener {

    private val outputStreamWriter: OutputStreamWriter
    val pipedInputStream: PipedInputStream


    init {
        try {
            val pipedOutputStream = PipedOutputStream()
            outputStreamWriter = OutputStreamWriter(pipedOutputStream, StandardCharsets.UTF_8)
            pipedInputStream = PipedInputStream(pipedOutputStream)
        } catch (e: IOException) {
            throw ExecutionException(e)
        }
    }

    companion object {
        private val LOG = Logger.getInstance(
            LspServerProcessListener::class.java
        )

    }

    override fun startNotified(event: ProcessEvent) {

        LOG.info("LSP server process started: $processHandler")
    }

    override fun processTerminated(event: ProcessEvent) {


        LOG.info("LSP server process terminated, exit code = " + event.exitCode + ", command line: " + processHandler)
        try {
            outputStreamWriter.close()
            pipedInputStream.close()
        } catch (e: IOException) {
            LOG.error(e)
        }
    }

    override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
        if (ProcessOutputType.isStdout(outputType)) {
            val text = event.text

            try {
                outputStreamWriter.write(text)
                outputStreamWriter.flush()
            } catch (exception: IOException) {
                LOG.error(
                    "Problem proxying data to the listener, stopping process: ${processHandler.process}; " +
                            ReflectionUtil.dumpFields(
                                PipedInputStream::class.java,
                                pipedInputStream,
                                *arrayOf("readSide", "writeSide", "closedByReader", "closedByWriter")

                            ),
                    exception
                )
                ExecutionManagerImpl.stopProcess(processHandler)
            }
        } else if (ProcessOutputType.isStderr(outputType)) {
            LOG.info("${processHandler}\n STDERR: ${event.text}")
        }
    }


}
