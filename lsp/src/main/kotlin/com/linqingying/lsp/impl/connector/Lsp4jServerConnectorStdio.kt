package com.linqingying.lsp.impl.connector

import com.linqingying.lsp.api.LspServer
import com.intellij.execution.impl.ExecutionManagerImpl
import com.intellij.execution.process.OSProcessHandler
import java.io.InputStream
import java.io.OutputStream

class Lsp4jServerConnectorStdio(lspServer: com.linqingying.lsp.api.LspServer) : Lsp4jServerConnector(lspServer) {

    private val processHandler: OSProcessHandler = lspServer.descriptor.startServerProcess()
    private val processListener: LspServerProcessListener = LspServerProcessListener(processHandler)

    init {
        processHandler.addProcessListener(processListener)

    }

    override fun disconnect() {
        LOG.debug("Stopping process: " + this.processHandler)
        ExecutionManagerImpl.stopProcess(this.processHandler)
    }

    override fun startNotify() {
        this.processHandler.startNotify()
    }

    override fun getServerOutputStream(): OutputStream {
        return processHandler.processInput
    }

    override fun getServerInputStream(): InputStream {
        return processListener.pipedInputStream
    }

}
