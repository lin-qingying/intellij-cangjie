package com.huawei.cangjie.dapDebugger.runconfig


import com.google.protobuf.Message
import com.intellij.execution.ExecutionFinishedException
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Conditions
import com.intellij.openapi.util.Pair
import com.intellij.util.Consumer
import com.intellij.util.concurrency.QueueProcessor

import com.huawei.cangjie.dapDebugger.protocol.ProtocolMessage
import com.huawei.cangjie.dapDebugger.protocol.request.InitializeRequest
import com.huawei.cangjie.dapDebugger.protocol.response.InitializeResponse
import com.huawei.cangjie.dapDebugger.protocol.type.adapter.moshi
import com.huawei.cangjie.dapDebugger.protocol.type.serializer.format
import kotlinx.serialization.encodeToString
import org.jetbrains.annotations.TestOnly
import java.io.IOException
import java.io.Writer
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.SocketChannel
import java.util.*
import java.util.concurrent.ExecutionException
import java.util.concurrent.Future
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Supplier

const val TWO_CRLF = "\r\n\r\n"
const val ONE_CRLF = "\r\n"

fun <T> Consumer<T>.accept(v: T) {
    this.consume(v)

}
inline fun <reified T : ProtocolMessage> Writer.write(message: T) {
    val json = format.encodeToString(message)
    val size = json.toByteArray().size
    this.write("Content-Length: ${size}$TWO_CRLF${json}")
    this.flush()

}
open class DapClent<T : ProtocolMessage>(
    private val prot: Int,
    inboxConsumer: Consumer<ProtocolMessage>,
    val initData: InitializeRequest
) {
    companion object {

        private val ourCount: AtomicInteger = AtomicInteger(0)
        private fun alloc(size: Int): ByteBuffer {
//            val length = "Content-Length: ".length + TWO_CRLF.length + size
            val buffer = ByteBuffer.allocate(size)
            buffer.order(ByteOrder.LITTLE_ENDIAN)
            return buffer
        }
    }

//    private val responseHandlers: Deque<Pair<Consumer<Message>, Class<out Message>>> = ArrayDeque(100)


    private val defaultTimeout: Long = 30000L
    private val socketLock: Any = Any()

    private val toRelease = mutableListOf<Semaphore>()
    private val uid = ourCount.incrementAndGet()

    private val initializationTime: Long = System.currentTimeMillis()

    private val responseHandlers: Deque<Pair<Consumer<ProtocolMessage>, Class<out ProtocolMessage>>> = ArrayDeque(100)

//    private val socketCient: Socket = Socket("127.0.0.1", prot)

    private val inboxProcessor = QueueProcessor<ProtocolMessage>(
        { generatedMessage ->

            synchronized(responseHandlers) {
                val responseHandler =
                    if (responseHandlers.isEmpty()) null else responseHandlers.peekFirst()

//                val responseHandler = responseHandlers.peekFirst() as Pair<Consumer<Message>, *>
                if (responseHandler != null && responseHandler.second == generatedMessage.javaClass) {
                    responseHandler.first.accept(generatedMessage)
                    responseHandlers.removeFirst()
                    return@synchronized
                }
            }

            inboxConsumer.accept(generatedMessage)
        }, Conditions.alwaysFalse<Message>()
    )

    private val outboxProcessor: QueueProcessor<Pair<ProtocolMessage, Consumer<ProtocolMessage>>> =
        QueueProcessor({ pair ->
            val result: Boolean = this.doSendMessage(pair.first)
            if (pair.second != null) {
                pair.second.consume(if (result) pair.first else null)
            }
        }, Conditions.alwaysFalse<ProtocolMessage>())
    private var socketChannel: SocketChannel? = null

    private var readerThreadFuture: Future<*>? = null


    inline fun <reified T : ProtocolMessage> toByteArray(message: T): ByteArray {
        return format.encodeToString<T>(message).toByteArray()
    }

    private inline fun <reified T : ProtocolMessage> doSendMessage(generatedMessage: T): Boolean {
        synchronized(socketLock) {
            if (socketChannel == null) {
                return false
            } else {
                val bytes = toByteArray(generatedMessage)

                val buf = alloc(bytes.size + 4 + "Content-Length: ".length + TWO_CRLF.length)
                buf.put("Content-Length: ".toByteArray())
                buf.put("${bytes.size}".toByteArray())
                buf.put(TWO_CRLF.toByteArray())
                buf.put(bytes)
                buf.rewind()
                debug { "req(${bytes.size + 4}): $generatedMessage" }
//                writeStringToFile(generatedMessage.toString(), "发送消息")
                try {
                    socketChannel!!.write(buf)
                } catch (ioEx: IOException) {
                    return false
                }
                return true
            }
        }
    }


    init {
        connectToServer()

    }

    private fun debug(message: Supplier<String>) {
//        if (LOG.isDebugEnabled) {
//            val time: Long = System.currentTimeMillis() - initializationTime
//            LOG.debug(("[protobuf client " + this.uid + "] " + time + " " + message.get()).trim { it <= ' ' })
//        }
    }


    private var cancelAcceptAttempts = false


    private fun sendInit() {

        this.sendMessage(initData, InitializeResponse::class.java, null)
    }

    private fun connectToServer() {
        synchronized(socketLock) {
            readerThreadFuture = ApplicationManager.getApplication().executeOnPooledThread {
                try {
                    var socketChannel: SocketChannel
                    synchronized(socketLock) {
                        while (true) {
                            if (this.socketChannel != null) {
                                socketChannel = this.socketChannel!!
                                sendInit()
                                break
                            }
                            if (cancelAcceptAttempts) {
                                return@executeOnPooledThread
                            }
                            try {
                                this.socketChannel = SocketChannel.open(InetSocketAddress(prot))
                                Thread.sleep(5L)
                            } catch (_: IOException) {
                            } catch (ex: InterruptedException) {
                                return@executeOnPooledThread
                            }
                        }
                    }
                    readerThread(socketChannel)
                } catch (ex: IOException) {
                    handleIOException(ex)
                }
            }
        }
    }


    protected open fun handleIOException(ex: IOException?) {


    }

    @Throws(ProtobufTimedOutException::class)
    fun <T : ProtocolMessage> sendMessageAndWaitUntilSent(
        request: ProtocolMessage,
        responseClass: Class<T>?,
        responseHandler: Consumer<T>?
    ) {
        sendMessageAndWaitUntilSent(request, responseClass, responseHandler, defaultTimeout)
    }

    @Throws(ProtobufTimedOutException::class, ExecutionFinishedException::class)
    fun <ResponseType : ProtocolMessage> sendMessage(
        message: ProtocolMessage,
        responseClass: Class<ResponseType>?,
        responseHandler: Consumer<in ResponseType>?,
        sendHandler: Consumer<ProtocolMessage>? = null
    ) {

        if (responseClass != null && responseHandler != null) {
            synchronized(responseHandlers) {
                responseHandlers.addLast(Pair(responseHandler as Consumer<ProtocolMessage>, responseClass))
            }
        }

        outboxProcessor.add(Pair.create(message, sendHandler))
    }


    @Throws(ProtobufTimedOutException::class)
    fun <T : ProtocolMessage> sendMessageAndWaitUntilSent(
        request: ProtocolMessage,
        responseClass: Class<T>?,
        responseHandler: Consumer<T>?,
        msTimeout: Long
    ) {
        val semaphore = Semaphore(0)
        synchronized(toRelease) {
            toRelease.add(semaphore)
        }

        sendMessage(request, responseClass, responseHandler) { generatedMessage ->
            semaphore.release()
        }

        val acquired: Boolean

        try {
            acquired = semaphore.tryAcquire(msTimeout, TimeUnit.MILLISECONDS)
            if (!acquired) {
                throw ProtobufTimedOutException()
            }
        } catch (e: InterruptedException) {
            // Handle interruption
        } finally {
            synchronized(toRelease) {
                toRelease.remove(semaphore)
            }
        }
    }

    fun <ResponseType : ProtocolMessage> sendMessage(
        message: ProtocolMessage,
        responseClass: Class<ResponseType>?,
        responseHandler: Consumer<in ResponseType>?
    ) {

        this.sendMessage(message, responseClass, responseHandler, null)
    }


    var length = 0
    val strBuffer = StringBuilder()

    fun handleMessage(message: String) {
        var messageTemp = message

//
        if (!message.startsWith("Content-Length")) {
            if (message.length < length) {
                strBuffer.append(message.substring(length - strBuffer.length))
                return
            } else {
                strBuffer.append(message.substring(length - strBuffer.length))



//                writeStringToFile(strBuffer.toString(), "接收到的消息")
                val protocolMessage = moshi.adapter(ProtocolMessage::class.java).fromJson(strBuffer.toString())

                inboxProcessor.add(protocolMessage!!)



                messageTemp = messageTemp.substring(0, length - strBuffer.length)

            }

        }

        val regex = Regex("Content-Length: (\\d+)")
        val matches = regex.findAll(message).map {
            messageTemp = messageTemp.replace(it.groupValues[0], "")

            messageTemp = messageTemp.replace(ONE_CRLF, "")
            it.groupValues[1]
        }


        val list = matches.toList()


        var position = 0
        list.forEach {
            length = it.toInt()

            if (messageTemp.length < position + length) {
                strBuffer.append(messageTemp.substring(position, messageTemp.length))
                return
            }

            val content = messageTemp.substring(position, position + length)


//            writeStringToFile(content, "接收到的消息")
            val protocolMessage = moshi.adapter(ProtocolMessage::class.java).fromJson(content)

            inboxProcessor.add(protocolMessage!!)


            position += length
        }


    }

    @Throws(IOException::class)
    private fun readerThread(stream: SocketChannel) {
        try {
            val buffer = alloc(66560)

            while (true) {
                val read: Int
                try {
//                    buffer.position(
//                        0
//                    )
//                    buffer.limit(buffer.capacity())
//                    buffer.mark()
//                    buffer.reset()
//                    buffer.clear()

                    read = stream.read(buffer)
                } catch (ioEx: IOException) {
                    break
                }

                if (read == -1) {
                    break
                }

                if (read == 0) {
                    continue
                }
//                writeStringToFile(String(buffer.array(), 0, read))




                handleMessage(String(buffer.array(), 0, read))
                buffer.clear()
//                while (buffer.position() > 0) {
//                    buffer.flip()
//                    buffer.compact()
//                    var size = 0
//
//
//
//                    if (buffer.get(0) == 'C'.code.toByte() && buffer.get(1) == 'o'.code.toByte()) {
//                        buffer.get(ByteArray("Content-Length: ".length))
//                        buffer.compact()  // 将剩余数据移到开头
//                        buffer.flip()
//
//
//                        val buf = alloc(100)
//                        var byte = buffer.get()
//
//                        while (byte != 123.toByte()) {
//
////                        buffer.flip()
//                            buf.put(byte)
//
//                            buffer.compact()  // 将剩余数据移到开头
//                            buffer.flip()
//                            byte = buffer.get()
//                        }
////                        buffer.flip()
////                        buffer.get(ByteArray(buf.position()))
////                        buffer.compact()
//
//                        size = String(buf.array(), 0, buf.position() - 4).toInt()
//                        buffer.rewind()
//
//                        while (buffer.remaining() >= size && buffer.hasRemaining() && size > 0) {
//                            val byteArray = ByteArray(size)
//
//                            buffer.get(byteArray, buffer.position(), size)
//                            val message = String(byteArray, 0, size)
//                            // 根据需要处理消息
//                            writeStringToFile(message, "接收到的消息")
//                            val protocolMessage = moshi.adapter(ProtocolMessage::class.java).fromJson(message)
//
//                            inboxProcessor.add(protocolMessage!!)
//
//                            size = 0
//                        }
//
//                        println(size)
//
//
//                    } else {
////                        TODO 处理连续数据
//                        print("不带Content-Length的消息")
//                    }
//                }


//                while (buffer.position() > 0) {
//
//
//                    var size = 0
//
//                    buffer.flip()  // 将限制设置为当前位置，并将位置设置为0
////                    val contentLength = buffer.int
//
//
//                    if (buffer.get(0) == 'C'.code.toByte() && buffer.get(1) == 'o'.code.toByte()) {
//                        buffer.get(ByteArray("Content-Length: ".length))
//                        buffer.compact()  // 将剩余数据移到开头
//                        buffer.flip()
//
//
//                        val buf = alloc(100)
//                        var byte = buffer.get()
//
//                        while (byte != 123.toByte()) {
//
////                        buffer.flip()
//                            buf.put(byte)
//
//                            buffer.compact()  // 将剩余数据移到开头
//                            buffer.flip()
//                            byte = buffer.get()
//                        }
////                        buffer.flip()
////                        buffer.get(ByteArray(buf.position()))
////                        buffer.compact()
//
//                        size = String(buf.array(), 0, buf.position() - 4).toInt()
//
//                    } else {
//                        print("不带Content-Length的消息")
//                    }
//
//
//                    val messageBytes = ByteArray(size)
//                    buffer.rewind()
//                    while (buffer.remaining() >= size && buffer.hasRemaining() && size > 0) {
//
//
//                        buffer.get(messageBytes, 0, size)
//
//
//                        val message = String(messageBytes, 0, size)
//                        // 根据需要处理消息
//                        writeStringToFile(message, "接收到的消息")
//                        val protocolMessage = moshi.adapter(ProtocolMessage::class.java).fromJson(message)
//
//                        inboxProcessor.add(protocolMessage!!)
//
//                        size = 0
//                    }
//
//                }

            }
        } catch (ex: IOException) {
            // Handle exception as needed
        }
    }

    fun tearDown() {
        releaseAll()
        synchronized(socketLock) {
            try {
                if (socketChannel != null) {
                    socketChannel!!.close()
                }
            } catch (_: IOException) {
            } finally {
                socketChannel = null
            }
        }
    }

    private fun releaseAll() {
        this.inboxProcessor.clear()

        this.outboxProcessor.clear()

        synchronized(toRelease) {
            for (release in toRelease) {
                release.release()
            }
        }
    }

    @TestOnly
    fun waitFor() {

        synchronized(socketLock) {
            if (readerThreadFuture != null && socketChannel == null) {
                try {
                    readerThreadFuture!!.get()
                } catch (_: InterruptedException) {
                } catch (ex: ExecutionException) {
                    throw RuntimeException(ex)
                }
            }
        }
        inboxProcessor.waitFor()
        this.outboxProcessor.waitFor()
    }

    @Throws(ProtobufTimedOutException::class, ExecutionFinishedException::class)
    fun <T : ProtocolMessage> sendMessageAndWaitForReply(
        message: ProtocolMessage,
        responseClass: Class<T>,
        responseHandler: Consumer<T>
    ) {

        this.sendMessageAndWaitForReply(message, responseClass, responseHandler, 0L)
    }

    @Throws(ProtobufTimedOutException::class, ExecutionFinishedException::class)
    open fun <ResponseType : ProtocolMessage> sendMessageAndWaitForReply(
        message: ProtocolMessage,
        responseClass: Class<ResponseType>,
        responseHandler: Consumer<ResponseType>,
        msTimeout: Long = 0L
    ) {

        val semaphore = Semaphore(0)
        synchronized(toRelease) {
            toRelease.add(semaphore)
        }

        val abandoned = booleanArrayOf(false)
        val failedToSend = booleanArrayOf(false)
        val done = booleanArrayOf(false)

        synchronized(responseHandlers) {
            responseHandlers.addLast(Pair.create(Consumer {
                if (!abandoned[0]) {
                    responseHandler.consume(it as ResponseType)
                    done[0] = true
                }
                semaphore.release()
            }, responseClass))
        }

        outboxProcessor.add(Pair(message, Consumer { generatedMessage ->
            if (generatedMessage == null) {
                failedToSend[0] = true
                semaphore.release()
            }
        }))

        var acquired = false

        try {
            if (msTimeout > 0L) {
                acquired = semaphore.tryAcquire(msTimeout, TimeUnit.MILLISECONDS)
                if (!acquired) {
                    abandoned[0] = true
                    throw ProtobufTimedOutException()
                }
            } else {
                semaphore.acquire()
            }
        } catch (e: InterruptedException) {
            abandoned[0] = true
        } finally {
            synchronized(toRelease) {
                toRelease.remove(semaphore)
            }
        }

        if (failedToSend[0] || abandoned[0] || !done[0]) {
            throw ExecutionFinishedException()
        }
    }
}

class ProtobufTimedOutException : Exception()
