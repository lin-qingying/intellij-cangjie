package org.cangnova.cangjie.protodebugger.transport

import com.google.protobuf.Message
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.delay
import org.cangnova.cangjie.protodebugger.protocol.ProtobufUtils
import org.cangnova.cangjie.protodebugger.util.writeStringToFile
import org.jetbrains.annotations.TestOnly
import lldbprotobuf.EventOuterClass
import lldbprotobuf.RequestOuterClass
import lldbprotobuf.ResponseOuterClass
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ExecutionException
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicBoolean

val isTest = false
/**
 * 基于Server Socket的传输层实现
 *
 * 使用TCP Socket进行调试器通信，支持协程和结构化并发
 */
class ServerSocketTransport : Transport {
    companion object {
        private val LOG = Logger.getInstance(ServerSocketTransport::class.java)
        private const val DEFAULT_TIMEOUT_MS = 30000L
        private const val BUFFER_SIZE = 66560
        private const val MAX_MESSAGE_SIZE = 100 * 1024 * 1024 // 100MB (增大限制)

        private fun allocBuffer(size: Int): ByteBuffer {
            return ByteBuffer.allocate(size).apply {
                order(ByteOrder.BIG_ENDIAN)
            }
        }
    }

    private val responseParser: ResponseParser<ResponseOuterClass.Response> =
        object : ResponseParser<ResponseOuterClass.Response> {
            override fun parse(data: ByteArray): ResponseOuterClass.Response {
                return ResponseOuterClass.Response.parseFrom(data)
            }

            override fun decompose(message: Message): Boolean {
                return message is ResponseOuterClass.Response ||
                        message is EventOuterClass.Event
            }
        }

    private val socketLock = Mutex()
    private val threadLock = Any()  // 用于线程同步的锁

    @Volatile
    private var socketChannel: SocketChannel? = null

    private var serverSocket: ServerSocketChannel  = ServerSocketChannel.open().apply {
        configureBlocking(false)
        socket().bind(InetSocketAddress(InetAddress.getLoopbackAddress(), if (isTest) 58920 else 0))
    }

    private val broadcastChannel = Channel<Message>(Channel.UNLIMITED)
    private val responseQueue = ConcurrentLinkedQueue<ResponseWaiter<*>>()
    private val pendingResponses = mutableMapOf<Long, ResponseWaiter<*>>()
    private val readerThreadRunning = AtomicBoolean(true)
    private lateinit var readerThreadFuture: Future<*>

    @Volatile
    override var isConnected = false
        private set

    val port: Int = serverSocket!!.socket().localPort

    init {
        startAcceptorThread()
    }

    override suspend fun <T : Message> sendAndWait(
        message: Message,
        responseClass: Class<T>,
        timeoutMs: Long
    ): T {

        // 提取请求的哈希值
        val requestHash = extractRequestHash(message)

        val waiter = ResponseWaiter(responseClass, Channel<T>(1))

        // 将等待者按哈希值存储
        if (requestHash != null) {
            pendingResponses[requestHash] = waiter
        } else {
            // 如果没有哈希值，使用旧的队列方式
            responseQueue.offer(waiter)
        }

        try {
            socketLock.withLock {
                if (!doSendMessage(message)) {
                    if (requestHash != null) {
                        pendingResponses.remove(requestHash)
                    } else {
                        responseQueue.remove(waiter)
                    }
                    throw TransportException("Failed to send message")
                }
            }

            // 等待响应
            return waiter.responseChannel.receive()
        } catch (e: Exception) {
            if (requestHash != null) {
                pendingResponses.remove(requestHash)
            } else {
                responseQueue.remove(waiter)
            }
            throw e
        }
    }

    override suspend fun send(message: Message) {

        waitForConnection()
        socketLock.withLock {
            if (!doSendMessage(message)) {
                throw TransportException("Failed to send message")
            }
        }
    }

    override fun broadcasts(): Flow<Message> = broadcastChannel.receiveAsFlow()

    @TestOnly
    fun waitFor() {
        synchronized(threadLock) {
            if (this::readerThreadFuture.isInitialized && socketChannel == null) {
                try {
                    readerThreadFuture.get()
                } catch (_: InterruptedException) {
                } catch (ex: ExecutionException) {
                    throw RuntimeException(ex)
                }
            }
        }
    }

    /**
     * 等待连接建立 - 使用协程方式
     */
    override suspend fun waitForConnection() {
        while (!isConnected && readerThreadRunning.get()) {
            delay(100)
        }

        if (!isConnected) {
            throw TransportException("Transport closed while waiting for connection")
        }
    }

    private fun doSendMessage(message: Message): Boolean {
        val channel = socketChannel ?: return false
        writeStringToFile(message.toString(), "发送消息")

        try {
            val bytes = message.toByteArray()
            val buffer = allocBuffer(bytes.size + 4)
            buffer.putInt(bytes.size)
            buffer.put(bytes)
            buffer.rewind()

            LOG.debug("Sending message (${bytes.size + 4} bytes): ${message.javaClass.simpleName}")
            channel.write(buffer)
            return true
        } catch (e: IOException) {
            LOG.warn("Failed to send message", e)
            return false
        }
    }

    /**
     * 启动接受器线程 - 负责接受客户端连接
     */
    private fun startAcceptorThread() {
        synchronized(threadLock) {
            readerThreadFuture = ApplicationManager.getApplication().executeOnPooledThread {
                try {
                    LOG.info("Acceptor thread started, waiting for connection on port $port")
                    var socketChannel: SocketChannel

                    synchronized(threadLock) {
                        while (true) {
                            if (this.socketChannel != null) {
                                socketChannel = this.socketChannel!!
                                isConnected = true
                                break
                            }
                            this.socketChannel = serverSocket.accept()

                            try {
                                Thread.sleep(5)
                            } catch (e: InterruptedException) {
                                LOG.info("Acceptor thread interrupted")
                                return@executeOnPooledThread
                            } catch (e: IOException) {
                                LOG.warn("Error accepting connection", e)
                                return@executeOnPooledThread
                            }
                        }
                    }

                    startReaderThread(socketChannel)
                } catch (e: Exception) {
                    LOG.error("Acceptor thread error", e)
                }
            }
        }
    }

    /**
     * 启动读取线程 - 负责读取并处理消息
     */
    private fun startReaderThread(channel: SocketChannel) {
        synchronized(threadLock) {
            ApplicationManager.getApplication().executeOnPooledThread {
                try {
                    LOG.info("Reader thread started")
                    readerLoop(channel)
                } catch (e: Exception) {
                    LOG.error("Reader thread error", e)
                } finally {
                    isConnected = false
                    LOG.info("Reader thread stopped")
                }
            }
        }
    }

    private fun readerLoop(channel: SocketChannel) {
        var buffer = allocBuffer(BUFFER_SIZE)

        while (readerThreadRunning.get()) {
            try {
                val read = channel.read(buffer)
                if (read == -1) {
                    LOG.info("Connection closed by peer")
                    break
                }

                if (buffer.position() > 0) {
                    processBuffer(buffer)
                    buffer = allocBuffer(BUFFER_SIZE)
                }
            } catch (e: IOException) {
                if (readerThreadRunning.get()) {
                    LOG.warn("Read error", e)
                }
                break
            }
        }
    }

    private fun processBuffer(buffer: ByteBuffer) {
        var begin = 0
        val end = buffer.position()
        buffer.rewind()

        while (buffer.position() < end && end - buffer.position() >= 4) {
            val size = buffer.int

            // 验证消息大小的有效性
            if (size !in 1..MAX_MESSAGE_SIZE) {
                LOG.error("Invalid message size: $size, skipping buffer")
                break
            }

            if (end - buffer.position() < size) {
                // 消息不完整,等待更多数据
                buffer.position(begin)
                break
            }

            val array = ByteArray(size)
            buffer.get(array)

            try {
                val compositeResponse = responseParser.parse(array)
                writeStringToFile(compositeResponse.toString(), "接收到的消息")

                val message = ProtobufUtils.unpackComposite(
                    compositeResponse,
                    responseParser::decompose
                )

                LOG.debug("Received message: ${message.javaClass.simpleName}")
                handleIncomingMessage(message)

                begin = buffer.position()
            } catch (e: Exception) {
                LOG.error("Failed to parse message", e)
            }
        }
    }

    private fun handleIncomingMessage(data: Pair<Long?,Message>) {
        val message = data.second
        val responseHash = data.first
        // 检查是否是广播消息
        if (message is EventOuterClass.Event || isBroadcast(message)) {
            broadcastChannel.trySend(message)
            return
        }


        if (responseHash != null) {
            // 使用哈希值匹配响应
            val waiter = pendingResponses.remove(responseHash)
            if (waiter != null && waiter.responseClass.isInstance(message)) {
                @Suppress("UNCHECKED_CAST")
                (waiter as ResponseWaiter<Message>).responseChannel.trySend(message)
            } else {
                if (waiter != null) {
                    LOG.warn(
                        "Response type mismatch for hash $responseHash: expected ${waiter.responseClass.simpleName}, " +
                                "got ${message.javaClass.simpleName}"
                    )
                }
                broadcastChannel.trySend(message)
            }
        } else {
            // 没有哈希值，使用旧的队列方式
            val waiter = responseQueue.poll()
            if (waiter != null && waiter.responseClass.isInstance(message)) {
                @Suppress("UNCHECKED_CAST")
                (waiter as ResponseWaiter<Message>).responseChannel.trySend(message)
            } else {
                // 没有匹配的等待者,作为广播处理
                if (waiter != null) {
                    LOG.warn(
                        "Response type mismatch: expected ${waiter.responseClass.simpleName}, " +
                                "got ${message.javaClass.simpleName}"
                    )
                }
                broadcastChannel.trySend(message)
            }
        }
    }

    private fun isBroadcast(message: Message): Boolean {
        return message.javaClass.simpleName.contains("Event")
    }

    /**
     * 从请求消息中提取哈希值
     */
    private fun extractRequestHash(message: Message): Long? {
        return if (message is RequestOuterClass.Request) {
            message.hash
        } else {
            null
        }
    }



    override fun close() {
        LOG.info("Closing transport")
        readerThreadRunning.set(false)
        isConnected = false

        try {
            socketChannel?.close()
            serverSocket.close()
        } catch (e: IOException) {
            LOG.warn("Error closing transport", e)
        } finally {
            socketChannel = null

        }

        broadcastChannel.close()

        // 取消所有等待的响应
        while (true) {
            val waiter = responseQueue.poll() ?: break
            waiter.responseChannel.close()
        }

        // 取消所有基于哈希的等待响应
        for (waiter in pendingResponses.values) {
            waiter.responseChannel.close()
        }
        pendingResponses.clear()
    }

    private data class ResponseWaiter<T : Message>(
        val responseClass: Class<T>,
        val responseChannel: Channel<T>
    )

    interface ResponseParser<T : Message> {
        fun parse(data: ByteArray): T
        fun decompose(message: Message): Boolean
    }
}