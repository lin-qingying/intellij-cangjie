package org.cangnova.namedpipe

import com.sun.jna.Pointer
import com.sun.jna.platform.win32.Kernel32        // jna-platform 官方封装，替代手写 Kernel32Ex
import com.sun.jna.platform.win32.Kernel32Util     // 提供 closeHandle 等工具方法
import com.sun.jna.platform.win32.WinBase
import com.sun.jna.platform.win32.WinDef
import com.sun.jna.platform.win32.WinError
import com.sun.jna.platform.win32.WinNT
import com.sun.jna.ptr.IntByReference
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.*
import java.io.IOException

private val log = KotlinLogging.logger {}

// ─────────────────────────────────────────────────────────────────────────────
// Windows Factory
// ─────────────────────────────────────────────────────────────────────────────

internal class WindowsNamedPipeFactory : NamedPipeFactory {
    override fun resolvePipePath(name: String): String = "\\\\.\\pipe\\$name"
    override fun createServer(config: PipeConfig): ServerNamedPipe =
        WindowsServerNamedPipe(config, this)
    override fun createClient(config: PipeConfig): ClientNamedPipe =
        WindowsClientNamedPipe(config, this)
}

// ─────────────────────────────────────────────────────────────────────────────
// WinAPI constants（原 Kernel32Ex 里的手写常量，直接用 jna-platform 官方类）
// ─────────────────────────────────────────────────────────────────────────────

private object Pipe {
    // dwOpenMode
    const val ACCESS_DUPLEX   = 0x00000003
    const val ACCESS_INBOUND  = 0x00000001
    const val ACCESS_OUTBOUND = 0x00000002
    // dwPipeMode
    const val TYPE_BYTE       = 0x00000000
    const val READMODE_BYTE   = 0x00000000
    const val WAIT            = 0x00000000
    // ConnectNamedPipe error that means "already connected"
    const val ERROR_PIPE_CONNECTED = 535
}

// ─────────────────────────────────────────────────────────────────────────────
// Okio Source/Sink backed by a Windows HANDLE
// 替代原先手写 HandleInputStream / HandleOutputStream，并接入 Okio 缓冲层
// ─────────────────────────────────────────────────────────────────────────────

private class HandleSource(
    private val handle: WinNT.HANDLE,
    private val pipeName: String
) : Source {
    private val k32 = Kernel32.INSTANCE

    override fun read(sink: Buffer, byteCount: Long): Long {
        val buf = ByteArray(byteCount.coerceAtMost(8192).toInt())
        val read = IntByReference()
        val ok = k32.ReadFile(handle, buf, buf.size, read, null)
        if (!ok) {
            val err = k32.GetLastError()
            if (err == WinError.ERROR_BROKEN_PIPE || err == 109) return -1L
            throw IOException("ReadFile failed on '$pipeName', error=$err")
        }
        val n = read.value
        if (n == 0) return -1L
        sink.write(buf, 0, n)
        return n.toLong()
    }

    override fun timeout(): Timeout = Timeout.NONE
    override fun close() { /* handle closed by pipe */ }
}

private class HandleSink(
    private val handle: WinNT.HANDLE,
    private val pipeName: String
) : Sink {
    private val k32 = Kernel32.INSTANCE

    override fun write(source: Buffer, byteCount: Long) {
        val buf = source.readByteArray(byteCount)
        val written = IntByReference()
        val ok = k32.WriteFile(handle, buf, buf.size, written, null)
        if (!ok) throw IOException("WriteFile failed on '$pipeName', error=${k32.GetLastError()}")
    }

    override fun flush() { Kernel32.INSTANCE.FlushFileBuffers(handle) }
    override fun timeout(): Timeout = Timeout.NONE
    override fun close() { /* handle closed by pipe */ }
}

// ─────────────────────────────────────────────────────────────────────────────
// Windows Base
// ─────────────────────────────────────────────────────────────────────────────

internal abstract class WindowsBaseNamedPipe(
    override val config: PipeConfig,
    protected val factory: WindowsNamedPipeFactory
) : NamedPipe {

    override val pipePath: String = factory.resolvePipePath(config.name)

    @Volatile
    override var state: PipeState = PipeState.CLOSED
        protected set

    protected var handle: WinNT.HANDLE = WinBase.INVALID_HANDLE_VALUE
    protected val k32: Kernel32 = Kernel32.INSTANCE

    protected var _source: BufferedSource? = null
    protected var _sink: BufferedSink? = null

    override val source: BufferedSource
        get() {
            check(state == PipeState.CONNECTED) { "Pipe '${config.name}' not connected" }
            check(config.access != PipeAccess.WRITE_ONLY) { "Pipe is WRITE_ONLY" }
            return _source ?: error("Source not initialized")
        }

    override val sink: BufferedSink
        get() {
            check(state == PipeState.CONNECTED) { "Pipe '${config.name}' not connected" }
            check(config.access != PipeAccess.READ_ONLY) { "Pipe is READ_ONLY" }
            return _sink ?: error("Sink not initialized")
        }

    /** 从 HANDLE 构建 Okio source/sink，替代手写 HandleInputStream/OutputStream */
    protected fun initOkioStreams() {
        when (config.access) {
            PipeAccess.READ_ONLY  -> _source = HandleSource(handle, config.name).buffer()
            PipeAccess.WRITE_ONLY -> _sink   = HandleSink(handle, config.name).buffer()
            PipeAccess.READ_WRITE -> {
                _source = HandleSource(handle, config.name).buffer()
                _sink   = HandleSink(handle, config.name).buffer()
            }
        }
    }

    override fun disconnect() {
        runCatching { _sink?.close() }
        runCatching { _source?.close() }
        _source = null; _sink = null
        if (handle != WinBase.INVALID_HANDLE_VALUE) k32.DisconnectNamedPipe(handle)
        state = PipeState.DISCONNECTED
        log.debug { "Pipe '${config.name}' disconnected" }
    }

    override fun close() {
        disconnect()
        if (handle != WinBase.INVALID_HANDLE_VALUE) {
            Kernel32Util.closeHandle(handle)   // jna-platform 工具方法，替代手写 CloseHandle
            handle = WinBase.INVALID_HANDLE_VALUE
        }
        state = PipeState.CLOSED
    }
}


/**
 * Windows 可继承匿名管道
 *
 * 封装一对可被子进程继承的 Windows 匿名管道句柄。
 *
 * ## 使用场景
 *
 * 当父进程需要通过命令行参数将管道句柄传递给子进程时，
 * 普通句柄在子进程中无效。通过设置 [WinBase.SECURITY_ATTRIBUTES.bInheritHandle] = true，
 * 子进程可以直接使用与父进程相同的句柄整数值访问同一管道。
 *
 * ## 管道方向
 *
 * 一个 [WindowsInheritablePipe] 实例代表单向数据流：
 * - 写入方持有 [writeHandle]，调用 WriteFile 写入数据
 * - 读取方持有 [readHandle]，调用 ReadFile 读取数据
 *
 * ## 典型用法
 *
 * ```kotlin
 * val toChild   = WindowsInheritablePipe()  // 父写 → 子读
 * val fromChild = WindowsInheritablePipe()  // 子写 → 父读
 *
 * // 传给子进程的句柄整数值（子进程通过命令行参数接收）
 * argv[1] = toChild.readHandleValue      // 子进程读端
 * argv[2] = fromChild.writeHandleValue   // 子进程写端
 *
 * // 子进程启动后，关闭父进程不再需要的那端
 * Kernel32Util.closeHandle(toChild.readHandle)
 * Kernel32Util.closeHandle(fromChild.writeHandle)
 *
 * // 父进程保留自己使用的那端
 * // toChild.writeHandle   → 父进程写端，用 WriteFile 向子进程发数据
 * // fromChild.readHandle  → 父进程读端，用 ReadFile 从子进程收数据
 * ```
 *
 * ## 注意事项
 *
 * - 子进程启动后，**必须关闭父进程持有的子进程那端句柄**，
 *   否则管道引用计数不归零，ReadFile/WriteFile 永远不会返回 EOF。
 * - [close] 会关闭读写两端，请在所有 IO 完成后调用。
 */
class WindowsInheritablePipe : Closeable {

    private val k32 = Kernel32.INSTANCE

    /**
     * 管道读端句柄（HANDLE）
     *
     * 用于 ReadFile，从管道中读取数据。
     * 若此管道作为"父写子读"通道，此句柄应传给子进程使用。
     */
    val readHandle: WinNT.HANDLE

    /**
     * 管道写端句柄（HANDLE）
     *
     * 用于 WriteFile，向管道写入数据。
     * 若此管道作为"子写父读"通道，此句柄应传给子进程使用。
     */
    val writeHandle: WinNT.HANDLE

    /**
     * 读端句柄的整数值
     *
     * HANDLE 指针的原始数值，可直接作为命令行参数传递给子进程。
     * 子进程侧：`reinterpret_cast<HANDLE>(atoi(argv[N]))`
     */
    val readHandleValue: Long

    /**
     * 写端句柄的整数值
     *
     * 同 [readHandleValue]，用于将写端传递给子进程。
     */
    val writeHandleValue: Long

    init {
        // 配置安全属性：允许子进程继承句柄
        // bInheritHandle = true 是跨进程共享匿名管道句柄的关键
        val sa = WinBase.SECURITY_ATTRIBUTES().apply {
            bInheritHandle = true
            lpSecurityDescriptor = null
            dwLength = WinDef.DWORD(size().toLong())
        }

        val hReadRef  = WinNT.HANDLEByReference()
        val hWriteRef = WinNT.HANDLEByReference()

        // 创建匿名管道，bufferSize=0 使用系统默认缓冲区大小
        if (!k32.CreatePipe(hReadRef, hWriteRef, sa, 0)) {
            throw IOException("CreatePipe 失败，错误码=${k32.GetLastError()}")
        }

        readHandle  = hReadRef.value
        writeHandle = hWriteRef.value

        // 将 HANDLE 指针转换为 Long，供命令行参数使用
        readHandleValue  = Pointer.nativeValue(readHandle.pointer)
        writeHandleValue = Pointer.nativeValue(writeHandle.pointer)
    }

    /**
     * 关闭读写两端句柄，释放系统资源
     *
     * 若已在子进程启动后提前关闭了某端（推荐做法），
     * 此处重复关闭不会报错（jna-platform closeHandle 内部有保护）。
     */
    override fun close() {
        runCatching { Kernel32Util.closeHandle(readHandle) }
        runCatching { Kernel32Util.closeHandle(writeHandle) }
    }
}
// ─────────────────────────────────────────────────────────────────────────────
// Windows Server
// ─────────────────────────────────────────────────────────────────────────────

internal class WindowsServerNamedPipe(
    config: PipeConfig,
    factory: WindowsNamedPipeFactory
) : WindowsBaseNamedPipe(config, factory), ServerNamedPipe ,WindowsPipeHandle {

    init { createPipe() }
    override val handleValue: Long
        get() = Pointer.nativeValue(handle.pointer)

    private fun openMode() = when (config.access) {
        PipeAccess.READ_ONLY  -> Pipe.ACCESS_INBOUND
        PipeAccess.WRITE_ONLY -> Pipe.ACCESS_OUTBOUND
        PipeAccess.READ_WRITE -> Pipe.ACCESS_DUPLEX
    }

    private fun createPipe() {
        handle = k32.CreateNamedPipe(
            pipePath,
            openMode(),
            Pipe.TYPE_BYTE or Pipe.READMODE_BYTE or Pipe.WAIT,
            config.maxInstances,
            config.bufferSize,
            config.bufferSize,
            0,
            null
        )
        if (handle == WinBase.INVALID_HANDLE_VALUE) {
            state = PipeState.ERROR
            throw PipeException.IoError(
                config.name, IOException("CreateNamedPipe failed, error=${k32.GetLastError()}")
            )
        }
        state = PipeState.CONNECTING
        log.debug { "Server pipe '${config.name}' created at $pipePath" }
    }

    override fun waitForConnection() {
        val ok = k32.ConnectNamedPipe(handle, null)
        if (!ok && k32.GetLastError() != Pipe.ERROR_PIPE_CONNECTED) {
            state = PipeState.ERROR
            throw PipeException.IoError(
                config.name, IOException("ConnectNamedPipe failed, error=${k32.GetLastError()}")
            )
        }
        initOkioStreams()
        state = PipeState.CONNECTED
        log.info { "Server pipe '${config.name}' client connected" }
    }

    override suspend fun waitForConnectionAsync() =
        withContext(Dispatchers.IO) { waitForConnection() }

    override fun disconnect() {
        super.disconnect()
        createPipe()   // 重置，准备接受下一个客户端
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Windows Client
// ─────────────────────────────────────────────────────────────────────────────

internal class WindowsClientNamedPipe(
    config: PipeConfig,
    factory: WindowsNamedPipeFactory
) : WindowsBaseNamedPipe(config, factory), ClientNamedPipe {

    private fun desiredAccess() = when (config.access) {
        PipeAccess.READ_ONLY  -> WinNT.GENERIC_READ
        PipeAccess.WRITE_ONLY -> WinNT.GENERIC_WRITE
        PipeAccess.READ_WRITE -> WinNT.GENERIC_READ or WinNT.GENERIC_WRITE
    }

    override fun connect() {
        state = PipeState.CONNECTING
        val timeoutMs = config.connectTimeoutMs.coerceAtLeast(1L).toInt()

        try {
            // WaitNamedPipe — jna-platform Kernel32 直接支持，无需手写 WString 转换
            if (!k32.WaitNamedPipe(pipePath, timeoutMs)) {
                val err = k32.GetLastError()
                throw if (err == WinError.ERROR_FILE_NOT_FOUND)
                    PipeException.NotFound(config.name)
                else
                    PipeException.ConnectionTimeout(config.name, config.connectTimeoutMs)
            }

            handle = k32.CreateFile(
                pipePath, desiredAccess(), 0, null,
                WinNT.OPEN_EXISTING, 0, null
            )
            if (handle == WinBase.INVALID_HANDLE_VALUE) {
                val err = k32.GetLastError()
                throw when (err) {
                    WinError.ERROR_FILE_NOT_FOUND -> PipeException.NotFound(config.name)
                    WinError.ERROR_ACCESS_DENIED  -> PipeException.AccessDenied(config.name)
                    else -> PipeException.IoError(config.name, IOException("CreateFile failed, error=$err"))
                }
            }
            initOkioStreams()
            state = PipeState.CONNECTED
            log.info { "Client pipe '${config.name}' connected" }
        } catch (e: PipeException) {
            state = PipeState.ERROR; throw e
        }
    }

    override suspend fun connectAsync() =
        withContext(Dispatchers.IO) { connect() }
}
