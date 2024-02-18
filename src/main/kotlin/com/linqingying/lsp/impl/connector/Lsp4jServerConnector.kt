package com.linqingying.lsp.impl.connector

import com.google.gson.JsonParseException
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.Ref
import com.intellij.openapi.util.text.StringUtil
import com.intellij.util.ConcurrencyUtil
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import com.intellij.util.concurrency.annotations.RequiresReadLockAbsence
import com.linqingying.lsp.api.Lsp4jClient
import com.linqingying.lsp.api.LspServer
import com.linqingying.lsp.api.LspServerDescriptor
import org.eclipse.lsp4j.InitializeResult
import org.eclipse.lsp4j.InitializedParams
import org.eclipse.lsp4j.ServerCapabilities
import org.eclipse.lsp4j.jsonrpc.MessageIssueException
import org.eclipse.lsp4j.jsonrpc.RemoteEndpoint
import org.eclipse.lsp4j.jsonrpc.json.MessageJsonHandler
import org.eclipse.lsp4j.jsonrpc.json.StreamMessageConsumer
import org.eclipse.lsp4j.jsonrpc.json.StreamMessageProducer
import org.eclipse.lsp4j.jsonrpc.messages.Message
import org.eclipse.lsp4j.jsonrpc.services.ServiceEndpoints
import org.eclipse.lsp4j.services.LanguageServer
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit


class MyStreamMessageProducer(
    input: InputStream, jsonHandler: MessageJsonHandler
) : StreamMessageProducer(input, jsonHandler){
    override fun fireError(error: Throwable?) {
//        super.fireError(error)
    }
}

abstract class Lsp4jServerConnector(lspServer: LspServer) {
    private val serverDescriptor: LspServerDescriptor = lspServer.descriptor
    private val lsp4jClient: Lsp4jClient = serverDescriptor.createLsp4jClient(lspServer.serverNotificationsHandler)
    private var initializeResult: InitializeResult? = null
    var lsp4jServer: LanguageServer? = null

    companion object {
        val LOG = Logger.getInstance(Lsp4jServerConnector::class.java)
    }

    protected abstract fun getServerInputStream(): InputStream

    protected abstract fun getServerOutputStream(): OutputStream

    /**
     * 获取服务器的能力
     */
    fun getServerCapabilities(): ServerCapabilities? {
        return initializeResult?.capabilities
    }

    @RequiresBackgroundThread
    @RequiresReadLockAbsence
    fun connect() {
        ApplicationManager.getApplication().assertReadAccessNotAllowed()
        val messageHandler: MessageJsonHandler = messageParseHandler()
        val remoteEndpoint = RemoteEndpoint(
            StreamMessageConsumer(getServerOutputStream(), messageHandler), ServiceEndpoints.toEndpoint(lsp4jClient)
        )
        messageHandler.methodProvider = remoteEndpoint
        lsp4jServer =
            ServiceEndpoints.toServiceObject(remoteEndpoint, serverDescriptor.lsp4jServerClass) as LanguageServer
        ApplicationManager.getApplication().executeOnPooledThread {
            ConcurrencyUtil.runUnderThreadName("LSP Listener: $serverDescriptor") {
                LOG.debug("$serverDescriptor: LSP Listener thread started")
                try {
                    val messageProducer = MyStreamMessageProducer(getServerInputStream(), messageHandler)
                    try {
                        messageProducer.listen(remoteEndpoint)
                    } catch (e: Throwable) {
                        try {
                            messageProducer.close()
                        } catch (closeException: Throwable) {
                            e.addSuppressed(closeException)
                        }
                        throw e
                    } catch (e: IllegalStateException) {
                        println()
                    } catch (e: Exception) {
                        println()
                    }
                    messageProducer.close()
                } catch (e: Throwable) {
                    LOG.error(serverDescriptor.toString(), e)
                }
                LOG.debug("$serverDescriptor: LSP Listener thread finished")
            }
        }
        startNotify()
        initialize()
    }

    protected abstract fun disconnect()

    @RequiresBackgroundThread
    @RequiresReadLockAbsence
    fun shutdownExitDisconnect() {
        ApplicationManager.getApplication().assertReadAccessNotAllowed()
        try {
            lsp4jServer?.shutdown()?.get(10L, TimeUnit.SECONDS)
        } catch (e: Exception) {
            LOG.warn("$serverDescriptor: `shutdown` request failed: $e")
        } finally {
            try {
                lsp4jServer?.exit()
            } finally {
                disconnect()
            }
        }
    }

    private fun messageParseHandler(): MessageJsonHandler {
        val serverClass = serverDescriptor.lsp4jServerClass
        val supportedMethods = LinkedHashMap(ServiceEndpoints.getSupportedMethods(serverClass))
        supportedMethods.putAll(ServiceEndpoints.getSupportedMethods(lsp4jClient::class.java))
        return object : MessageJsonHandler(supportedMethods) {
            override fun serialize(message: Message): String {
                val serialized = super.serialize(message)
                LOG.debug("--> $serverDescriptor: ${StringUtil.shortenTextWithEllipsis(serialized, 3000, 500)}")
                return serialized
            }

            @Throws(JsonParseException::class)
            override fun parseMessage(input: CharSequence): Message {
                LOG.debug("<-- $serverDescriptor: ${StringUtil.shortenTextWithEllipsis(input.toString(), 3000, 500)}")


                try {
                    return super.parseMessage(input)
                } catch (me: MessageIssueException) {


                    if (input.contains("\"codeLensProvider\":true")) {
                        val inputstr: CharSequence = input.toString()
                            .replace("\"codeLensProvider\":true", "\"codeLensProvider\":{\"resolveProvider\": true}")
                        return super.parseMessage(inputstr)
                    }

                    LOG.error(me.message)
                } catch (ie: IllegalStateException) {

                    LOG.error(ie.message)
                }
                return super.parseMessage(input)
            }
        }
    }

    protected open fun startNotify() {}

    /**
     * 初始化lsp服务器
     *
     */
    @RequiresBackgroundThread
    @RequiresReadLockAbsence
    private fun initialize() {
        ApplicationManager.getApplication().assertReadAccessNotAllowed()
        LOG.debug("$serverDescriptor: initializing LSP server")
        val initializeParams = serverDescriptor.createInitializeParams()
        val exceptionRef = Ref.create<Throwable>()
        val latch = CountDownLatch(1)
        lsp4jServer?.initialize(initializeParams)?.whenComplete { result, exception ->
            if (result != null) {
                initializeResult = result
                lsp4jServer?.initialized(InitializedParams())
            } else {
                exceptionRef.set(exception)
            }
            latch.countDown()
        }
        try {
            val received = latch.await(100L, TimeUnit.SECONDS)
            if (!received) {
                throw RuntimeException("'initialized' response not received from the server")
            }
            if (initializeResult == null) {
                throw RuntimeException("LSP server failed to initialize", exceptionRef.get())
            }
        } catch (e: InterruptedException) {
            throw RuntimeException(e)
        }
        val serverInfo = initializeResult!!.serverInfo
        if (serverInfo != null) {
            LOG.info("$serverDescriptor: server initialized, name = ${serverInfo.name}, version = ${serverInfo.version}")
        } else {
            LOG.info("$serverDescriptor: server initialized")
        }
        val listener = serverDescriptor.lspServerListener
        listener?.serverInitialized(initializeResult!!)
    }
}
