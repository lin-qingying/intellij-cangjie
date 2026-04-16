package org.cangnova.namedpipe

import okio.BufferedSink
import okio.BufferedSource
import java.io.Closeable

enum class PipeAccess { READ_ONLY, WRITE_ONLY, READ_WRITE }

enum class PipeState { CLOSED, CONNECTING, CONNECTED, DISCONNECTED, ERROR }

data class PipeConfig(
    val name: String,
    val access: PipeAccess = PipeAccess.READ_WRITE,
    val bufferSize: Int = 65536,
    val connectTimeoutMs: Long = 5000,
    /** 最大并发实例数（仅 Windows 服务端有效） */
    val maxInstances: Int = 10
)

sealed class PipeException(message: String, cause: Throwable? = null) :
    Exception(message, cause) {
    class AlreadyExists(name: String)  : PipeException("Named pipe '$name' already exists")
    class NotFound(name: String)       : PipeException("Named pipe '$name' not found")
    class ConnectionTimeout(name: String, ms: Long) :
        PipeException("Connection to '$name' timed out after ${ms}ms")
    class AccessDenied(name: String)   : PipeException("Access denied to named pipe '$name'")
    class BrokenPipe(name: String)     : PipeException("Named pipe '$name' connection broken")
    class IoError(name: String, cause: Throwable) :
        PipeException("I/O error on '$name': ${cause.message}", cause)
    class UnsupportedPlatform(platform: String) :
        PipeException("Named pipe not supported on platform: $platform")
}

/**
 * 命名管道核心接口
 *
 * source / sink 使用 Okio [BufferedSource] / [BufferedSink]，
 * 替代原始 InputStream/OutputStream 包装，直接支持：
 *   source.readUtf8Line()  sink.writeUtf8("hello\n").emit()
 */
interface NamedPipe : Closeable {
    val config: PipeConfig
    val state: PipeState
    val pipePath: String

    /** Okio 读取端 */
    val source: BufferedSource

    /** Okio 写入端 */
    val sink: BufferedSink

    val isConnected: Boolean get() = state == PipeState.CONNECTED

    fun disconnect()
}

/**
 * 服务端管道接口 —— 异步方法为 suspend，直接在协程中使用，不再需要回调。
 */
interface ServerNamedPipe : NamedPipe {
    /** 阻塞等待客户端连接 */
    fun waitForConnection()

    /** 协程挂起等待客户端连接（在 IO Dispatcher 上执行，不阻塞调用线程） */
    suspend fun waitForConnectionAsync()
}
/**
 * Windows 专用：暴露底层 HANDLE 数值，供传参给子进程
 */
interface WindowsPipeHandle  : ServerNamedPipe {
    /** HANDLE 的整数值，用于作为命令行参数传给 LSPMacroServer */
    val handleValue: Long
}
/**
 * 客户端管道接口
 */
interface ClientNamedPipe : NamedPipe {
    /** 同步连接 */
    fun connect()

    /** 协程挂起连接 */
    suspend fun connectAsync()
}
