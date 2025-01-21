package com.linqingying.lsp.impl.connector

import com.google.gson.JsonParseException
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.LogLevel
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.Ref
import com.intellij.openapi.util.text.StringUtil
import com.intellij.util.ConcurrencyUtil
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import com.intellij.util.concurrency.annotations.RequiresReadLockAbsence
import com.linqingying.lsp.api.Lsp4jClient
import com.linqingying.lsp.api.LspServerDescriptor
import com.linqingying.lsp.impl.LspServerImpl
import org.eclipse.lsp4j.InitializeResult
import org.eclipse.lsp4j.InitializedParams
import org.eclipse.lsp4j.jsonrpc.Endpoint
import org.eclipse.lsp4j.jsonrpc.MessageIssueException
import org.eclipse.lsp4j.jsonrpc.RemoteEndpoint
import org.eclipse.lsp4j.jsonrpc.json.MessageJsonHandler
import org.eclipse.lsp4j.jsonrpc.json.StreamMessageConsumer
import org.eclipse.lsp4j.jsonrpc.json.StreamMessageProducer
import org.eclipse.lsp4j.jsonrpc.messages.Message
import org.eclipse.lsp4j.jsonrpc.services.ServiceEndpoints
import org.eclipse.lsp4j.services.LanguageServer
import java.io.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit


internal abstract class Lsp4jServerConnector protected constructor(val lspServer: LspServerImpl) {
    companion object {
        val LOG = Logger.getInstance(Lsp4jServerConnector::class.java).apply {

        }
    }

    private val descriptor: LspServerDescriptor = lspServer.descriptor

    protected abstract val ideToServerStream: OutputStream

    private val lsp4jClient: Lsp4jClient /*get() */ =
        descriptor.createLsp4jClient(lspServer.serverNotificationsHandler)

    lateinit var lsp4jServer: LanguageServer

    private var initializeResult: InitializeResult? = null

    protected abstract val serverToIdeStream: InputStream

    fun messageDebug(message: String) {
        LOG.debug(message)

        // 获取项目根目录并构造日志路径
        val logFilePath = "${descriptor.project.basePath}/.idea/log/lsplog.log"
        val logFile = File(logFilePath)
        if (!logFile.parentFile.exists()) {
            logFile.parentFile.mkdirs() // 创建目录
        }
        if (!logFile.exists()) {
            logFile.createNewFile() // 创建日志文件
        }

        try {
            BufferedWriter(FileWriter(logFile, true)).use { writer ->
                writer.write(message)
                writer.newLine()
            }
        } catch (e: IOException) {
            LOG.error("无法写入日志文件", e)
        }
    }

    private fun messageParseHandler(): MessageJsonHandler {
        val serverClass = descriptor.lsp4jServerClass
        val supportedMethods = LinkedHashMap(ServiceEndpoints.getSupportedMethods(serverClass))
        supportedMethods.putAll(ServiceEndpoints.getSupportedMethods(lsp4jClient::class.java))
        return object : MessageJsonHandler(supportedMethods) {
            override fun serialize(message: Message): String {
                val serialized = super.serialize(message)
                messageDebug(
                    "--> $descriptor: ${
                        StringUtil.shortenTextWithEllipsis(
                            serialized,
                            3000,
                            500
                        )
                    }"
                )
                return serialized
            }

            @Throws(JsonParseException::class)
            override fun parseMessage(input: CharSequence): Message {
                messageDebug(
                    "<-- $descriptor: ${
                        StringUtil.shortenTextWithEllipsis(
                            input.toString(),
                            3000,
                            500
                        )
                    }"
                )


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

    @RequiresBackgroundThread
    @RequiresReadLockAbsence
    fun connect(onSuccess: (InitializeResult) -> Unit) {
        try {
            prepareConnect()
            val messageHandler = messageParseHandler()
            val remoteEndpoint = RemoteEndpoint(
                StreamMessageConsumer(ideToServerStream, messageHandler),
                ServiceEndpoints.toEndpoint(lsp4jClient)
            )
            messageHandler.methodProvider = remoteEndpoint
            val serviceObject =
                ServiceEndpoints.toServiceObject(remoteEndpoint as Endpoint, descriptor.lsp4jServerClass)

            this.lsp4jServer = serviceObject
            ApplicationManager.getApplication().executeOnPooledThread {
                ConcurrencyUtil.runUnderThreadName("LSP Listener: $descriptor") {
                    LOG.debug("$descriptor: LSP Listener thread started")
                    try {
                        val messageProducer = StreamMessageProducer(serverToIdeStream, messageHandler)
                        try {
                            messageProducer.listen(remoteEndpoint)
                        } catch (e: Throwable) {
                            try {
                                messageProducer.close()
                            } catch (closeException: Throwable) {
                                e.addSuppressed(closeException)
                            }
                            throw e
                        } catch (_: IllegalStateException) {

                        } catch (_: Exception) {

                        }
                        messageProducer.close()
                    } catch (e: Throwable) {
                        LOG.error(descriptor.toString(), e)
                    }
                    LOG.debug("$descriptor: LSP Listener thread finished")
                }
            }

        } finally {
            startNotify()
        }

        initialize(onSuccess)
    }

    /**
     * 初始化lsp服务器
     *
     */
    @RequiresBackgroundThread
    @RequiresReadLockAbsence
    private fun initialize(onComplete: (InitializeResult) -> Unit = {}) {
        ApplicationManager.getApplication().assertReadAccessNotAllowed()
        LOG.debug("$descriptor: initializing LSP server")
        val initializeParams = descriptor.createInitializeParams()
        val exceptionRef = Ref.create<Throwable>()
        val latch = CountDownLatch(1)
        lsp4jServer.initialize(initializeParams)?.whenComplete { result, exception ->
            if (result != null) {
                initializeResult = result
                lsp4jServer.initialized(InitializedParams())
                onComplete(result)
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
            LOG.info("$descriptor: server initialized, name = ${serverInfo.name}, version = ${serverInfo.version}")
        } else {
            LOG.info("$descriptor: server initialized")
        }
        val listener = descriptor.lspServerListener
        listener?.serverInitialized(initializeResult!!)
    }


    protected abstract fun disconnect()


    protected abstract fun isConnectionAlive(): Boolean

    protected abstract fun prepareConnect()

    @RequiresBackgroundThread
    @RequiresReadLockAbsence
    internal fun shutdownExitDisconnect() {
        ApplicationManager.getApplication().assertReadAccessNotAllowed()
        try {
            lsp4jServer.shutdown()?.get(10L, TimeUnit.SECONDS)
        } catch (e: Exception) {
            LOG.warn("$descriptor: `shutdown` request failed: $e")
        } finally {
            try {
                lsp4jServer.exit()
            } finally {
                disconnect()
            }
        }
    }

    protected abstract fun startNotify()
}

