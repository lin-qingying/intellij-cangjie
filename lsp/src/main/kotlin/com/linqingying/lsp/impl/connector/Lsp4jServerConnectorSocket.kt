package com.linqingying.lsp.impl.connector

import com.intellij.execution.impl.ExecutionManagerImpl
import com.intellij.execution.process.OSProcessHandler
import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import com.intellij.util.concurrency.annotations.RequiresReadLockAbsence
import com.intellij.util.io.IOUtil
import com.linqingying.lsp.api.LspCommunicationChannel
import com.linqingying.lsp.impl.LspServerImpl
import java.io.*
import java.net.InetAddress
import java.net.Socket


internal class Lsp4jServerConnectorSocket(    lspServer: LspServerImpl) :
    Lsp4jServerConnector(lspServer) {
    override val ideToServerStream: OutputStream
        get() {
            return outputStream

        }

    private lateinit var inputStream: DataInputStream

    private lateinit var outputStream: DataOutputStream

    private lateinit var processHandler: OSProcessHandler

    override val serverToIdeStream: InputStream
        get() {
            return inputStream
        }

    private lateinit var socket: Socket

    private val socketInfo: LspCommunicationChannel.Socket =
        this.lspServer.descriptor.lspCommunicationChannel as LspCommunicationChannel.Socket


    private fun connect() {
        socket = Socket(InetAddress.getLoopbackAddress().hostAddress, socketInfo.port).apply {
            this@Lsp4jServerConnectorSocket.outputStream = DataOutputStream(BufferedOutputStream(getOutputStream()))
            this@Lsp4jServerConnectorSocket.inputStream = DataInputStream(BufferedInputStream(getInputStream()))
        }
    }

    override fun disconnect() {
        socket.let {
            val logger = Logger.getInstance(Lsp4jServerConnectorSocket::class.java)
            val closeables = arrayOf(
                outputStream,
                inputStream,
                it // socket
            )
            IOUtil.closeSafe(logger, *closeables)
        }

        processHandler.let { handler ->
            if (!handler.isProcessTerminated) {
                lspServer.logInfo("Stopping LSP server process: $handler")
                ExecutionManagerImpl.stopProcess(handler)
            }
        }
    }

    override fun isConnectionAlive(): Boolean {

        if (!socket.isClosed) {
            return if (socketInfo.startProcess) {
                processHandler.let { handler ->
                    handler.isStartNotified && !handler.isProcessTerminated
                }
            } else {
                true
            }
        }

        return false
    }

    @RequiresBackgroundThread
    @RequiresReadLockAbsence
    override fun prepareConnect() {
        var throwable: Throwable? = null

        if (socketInfo.startProcess) {
            for (attempt in 1..10) {
                if (::processHandler.isInitialized && !processHandler.process.isAlive || ::socket.isInitialized) {
                    break
                }

                try {
                    connect()
                    throwable = null
                    break
                } catch (e: Throwable) {
                    throwable = e
                    lspServer.logDebug("Attempt $attempt: failed to create a socket connection (port=${socketInfo.port}): ${e.message}")
                    Thread.sleep(1000)
                }
            }
        } else {
            connect()
            throwable = null
        }

        throwable?.let {
            lspServer.logWarn("All attempts to create a socket connection failed (port=${socketInfo.port})", it)
            throw it
        }
    }

    override fun startNotify() {
        if (socketInfo.startProcess) {

            this.processHandler.startNotify()
        }

    }
}

