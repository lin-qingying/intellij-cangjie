package cn.cangnova.cangjie.lsp.core.server

import cn.cangnova.cangjie.lsp.core.client.Lsp4jClient
import com.fasterxml.jackson.core.JsonParseException
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.Ref
import com.intellij.openapi.util.text.StringUtil
import com.intellij.util.ConcurrencyUtil
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import com.intellij.util.concurrency.annotations.RequiresReadLockAbsence
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
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class MyStreamMessageProducer(
    input: InputStream, jsonHandler: MessageJsonHandler
) : StreamMessageProducer(input, jsonHandler) {
    override fun fireError(error: Throwable?) {
//        super.fireError(error)
    }
}

class Lsp4jServerConnector(val lspServer: LspServer) {

    private val lsp4jClient: Lsp4jClient = Lsp4jClient()
    private var initializeResult: InitializeResult? = null
    var lsp4jServer: LanguageServer? = null

    companion object {
        val LOG = Logger.getInstance(Lsp4jServerConnector::class.java)
    }


    /**
     * 获取服务器的能力
     */
    fun getServerCapabilities(): ServerCapabilities? {
        return initializeResult?.capabilities
    }

    @RequiresBackgroundThread
    @RequiresReadLockAbsence
    fun connect(success: () -> Unit) {
        ApplicationManager.getApplication().assertReadAccessNotAllowed()
        val messageHandler: MessageJsonHandler = messageParseHandler()
        val remoteEndpoint = RemoteEndpoint(
            StreamMessageConsumer(lspServer.serverOutputStream, messageHandler),
            ServiceEndpoints.toEndpoint(lsp4jClient)
        )
        messageHandler.methodProvider = remoteEndpoint
        lsp4jServer =
            ServiceEndpoints.toServiceObject(remoteEndpoint, lspServer.lsp4jServerClass) as LanguageServer
        ApplicationManager.getApplication().executeOnPooledThread {
            ConcurrencyUtil.runUnderThreadName("LSP Listener: $lspServer") {
                LOG.debug("$lspServer: LSP Listener thread started")
                try {
                    val messageProducer = MyStreamMessageProducer(lspServer.serverInputStream, messageHandler)
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
                    LOG.error(lspServer.toString(), e)
                }
                LOG.debug("$lspServer: LSP Listener thread finished")
            }
        }
//        startNotify()
        initialize(success)
    }


    @RequiresBackgroundThread
    // 确保在后台线程中执行
    @RequiresReadLockAbsence
    // 确保没有读锁
    fun shutdownExitDisconnect() {
        // 断言没有读锁
        ApplicationManager.getApplication().assertReadAccessNotAllowed()
        try {
            // 关闭LSP服务器
            lsp4jServer?.shutdown()?.get(10L, TimeUnit.SECONDS)
        } catch (e: Exception) {
            // 如果关闭失败，记录警告日志
            LOG.warn("$lspServer: `shutdown` request failed: $e")
        } finally {
            try {
                // 退出LSP服务器
                lsp4jServer?.exit()
            } finally {
                // 断开连接
                disconnect()
            }
        }
    }

    fun disconnect() {
        lspServer.stop()
    }

    private fun messageParseHandler(): MessageJsonHandler {
        val serverClass = lspServer.lsp4jServerClass
        val supportedMethods = LinkedHashMap(ServiceEndpoints.getSupportedMethods(serverClass))
        supportedMethods.putAll(ServiceEndpoints.getSupportedMethods(lsp4jClient::class.java))
        return object : MessageJsonHandler(supportedMethods) {
            override fun serialize(message: Message): String {
                val serialized = super.serialize(message)
                LOG.debug("--> $lspServer: ${StringUtil.shortenTextWithEllipsis(serialized, 3000, 500)}")
                return serialized
            }

            @Throws(JsonParseException::class)
            override fun parseMessage(input: CharSequence): Message {
                LOG.debug("<-- $lspServer: ${StringUtil.shortenTextWithEllipsis(input.toString(), 3000, 500)}")


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

        fun startNotify() {
            lspServer.start()
        }

    /**
     * 初始化lsp服务器
     *
     */
    @RequiresBackgroundThread
    @RequiresReadLockAbsence
    private fun initialize(success: () -> Unit) {
        ApplicationManager.getApplication().assertReadAccessNotAllowed()
        LOG.debug("$lspServer: initializing LSP server")
        val initializeParams = lspServer.createInitializeParams()
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
            LOG.info("$lspServer: server initialized, name = ${serverInfo.name}, version = ${serverInfo.version}")
        } else {
            LOG.info("$lspServer: server initialized")
        }
//        val listener = lspServer.lspServerListener
//        listener?.serverInitialized(initializeResult!!)
        success()
    }
}