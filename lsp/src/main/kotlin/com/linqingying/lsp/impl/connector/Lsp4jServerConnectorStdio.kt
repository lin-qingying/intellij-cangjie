package com.linqingying.lsp.impl.connector

import com.intellij.execution.impl.ExecutionManagerImpl.Companion.stopProcess
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.process.ProcessHandler
import com.linqingying.lsp.impl.LspServerImpl
import java.io.InputStream

internal class Lsp4jServerConnectorStdio(     lspServer: LspServerImpl) :
    Lsp4jServerConnector(lspServer) {


    private val processHandler: OSProcessHandler = lspServer.descriptor.startServerProcess()

    private val processListener: LspServerProcessListener = LspServerProcessListener( lspServer)
    init {
        processHandler.addProcessListener(processListener)

    }
    override val serverToIdeStream: InputStream = this.processListener.pipedInputStream
    override val ideToServerStream = processHandler.processInput

    override fun disconnect() {
        if (!processHandler.isProcessTerminated) {
            lspServer.logInfo("Stopping LSP server process: " + processHandler.commandLine)
            stopProcess(processHandler as ProcessHandler)
        }
    }

    override fun isConnectionAlive(): Boolean {
        return processHandler.isStartNotified && !processHandler.isProcessTerminated

    }

    override fun prepareConnect() {
    }

    override fun startNotify() {
        processHandler.startNotify()

    }
}
