package dap


import dap.protocol.ProtocolMessage
import dap.request.InitializeRequest
import dap.type.PathFormat
import dap.type.arguments.InitializeRequestArguments
import dap.type.serializer.format
import kotlinx.serialization.encodeToString

import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.ClosedChannelException
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicInteger


class DapServer {
    companion object {
        private val ourCount: AtomicInteger = AtomicInteger(0)
        private fun alloc(size: Int): ByteBuffer {
            val length = "Content-Length: ".length + TWO_CRLF.length + size
            val buffer = ByteBuffer.allocate(length)
            buffer.order(ByteOrder.LITTLE_ENDIAN)
            return buffer
        }
    }

    private var serverSocket: ServerSocketChannel? = null
    private var readerThreadFuture: Future<*>? = null
    private var socketChannel: SocketChannel? = null
    private var cancelAcceptAttempts = false

    private val socketLock: Any = Any()

    val port = startReaderThread()


    init {
        connectSocket()
    }

    fun connectSocket() {
        synchronized(socketLock) {
            socketChannel = SocketChannel.open()
            try {
                socketChannel!!.connect(InetSocketAddress("127.0.0.1", port))
            } catch (ioEx: IOException) {
//                handleIOException(ioEx)
            }

//            readerThread(socketChannel!!)
        }
    }

    private fun startReaderThread(): Int {
        synchronized(socketLock) {
            serverSocket = ServerSocketChannel.open()
            serverSocket!!.configureBlocking(false)
            val inetAddress = InetAddress.getLoopbackAddress()
            serverSocket!!.socket().bind(InetSocketAddress(inetAddress, 0))
            val port = serverSocket!!.socket().localPort


            println(port)
//            readerThreadFuture = ApplicationManager.getApplication().executeOnPooledThread {
            try {
                var socketChannel: SocketChannel? = null
                synchronized(socketLock) {
                    while (true) {
                        if (this.socketChannel != null) {
                            socketChannel = this.socketChannel!!
                            break
                        }

                        if (cancelAcceptAttempts) {
//                                return@executeOnPooledThread

                            break
                        }

                        this.socketChannel = serverSocket!!.accept()

                        try {
                            Thread.sleep(5L)
                        } catch (ex: InterruptedException) {
                            break
                        }
                    }
                }

                readerThread(socketChannel!!)
            } catch (ex: IOException) {
                handleIOException(ex)
            }
//            }
            return port
        }
    }

    protected open fun handleIOException(ex: IOException?) {

    }

    @Throws(IOException::class)
    private fun readerThread(stream: SocketChannel) {
        try {
            var buffer = alloc(66560)

            while (true) {
                val read: Int
                try {
                    read = stream.read(buffer)
                } catch (ioEx: IOException) {
                    break
                }

                if (read == -1) {
                    break
                }

                if (buffer.position() != 0) {
                    var begin = 0
                    val end = buffer.position()
                    buffer.rewind()
                    var size = 0

                    while (true) {
                        if (buffer.position() < end && end - buffer.position() >= 4) {
                            size = buffer.int
                            if (end - buffer.position() >= size) {
                                val array = ByteArray(size)
                                buffer.get(array)
//                                val compositeResponse = responseParser.parse(array)
//                                debug { "res:$compositeResponse" }
//                                val message =
//                                    ProtobufUtils.unpackComposite(compositeResponse, responseParser::decompose)


//                                inboxProcessor.add(message)
                                begin = buffer.position()
                                continue
                            }
                        }

                        val remaining = end - begin
                        buffer.position(begin)
                        val remainingBytes = ByteArray(remaining)
                        buffer.get(remainingBytes)
                        if (size + 4 > buffer.capacity()) {
                            buffer = alloc(size + 4)
                        }

                        buffer.rewind()
                        buffer.put(remainingBytes)
                        break
                    }
                }
            }
        } catch (ex: ClosedChannelException) {
            // Handle exception as needed
        }
    }


    private fun doSendMessage(generatedMessage: InitializeRequest): Boolean {

        synchronized(socketLock) {
            if (socketChannel == null) {
                return false
            } else {

                val bytes = format.encodeToString(generatedMessage).toByteArray()

                val buf = alloc(bytes.size + 4)
                buf.put("Content-Length: ".toByteArray())
                buf.put("${bytes.size}".toByteArray())
                buf.put(TWO_CRLF.toByteArray())
                buf.put(bytes)
                buf.rewind()


                try {
                    socketChannel!!.write(buf)
                } catch (ioEx: IOException) {
                    return false
                }

                return true
            }
        }
    }

    fun sendMessage(message: InitializeRequest) {

        doSendMessage(message)
    }

}

fun main() {
    val server = DapServer()
    println("ss")

    val init = InitializeRequest(
        InitializeRequestArguments(
            adapterID = "cangjieDebug",
            clientId = "idea",
            clientName = "Intellij IDEA",
            columnsStartAt1 = true,
            linesStartAt1 = true,
            locale = "zh-cn",
            pathFormat = PathFormat.Path,
            supportsInvalidatedEvent = true,
            supportsMemoryEvent = true,
            supportsArgsCanBeInterpretedByShell = true,
            supportsMemoryReferences = true,
            supportsProgressReporting = true,
            supportsRunInTerminalRequest = true,
            supportsStartDebuggingRequest = true,
            supportsVariablePaging = true,
            supportsVariableType = true
        )
    )
    server.sendMessage(init)
}