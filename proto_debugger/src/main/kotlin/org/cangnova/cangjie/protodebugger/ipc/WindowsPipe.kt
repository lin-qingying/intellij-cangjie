package org.cangnova.cangjie.protodebugger.ipc

import com.sun.jna.platform.win32.*
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.atomic.AtomicInteger
import com.pty4j.windows.cygwin.CygwinPTYInputStream
import com.pty4j.windows.cygwin.CygwinPTYOutputStream

/**
 * Windows命名管道实现，封装JNA调用，
 * 支持异步连接与双向通信。
 */
class WinPipe private constructor(
    private val direction: PipeDirection,
    private val nameSuffix: String
) : NamedPipe {

    private val pipeName: String
    private val handle: WinNT.HANDLE
    private val namedPipe: com.pty4j.windows.winpty.NamedPipe

    init {
        pipeName = "\\\\.\\pipe\\cangjie-debugger-${Kernel32.INSTANCE.GetCurrentProcessId()}-${processCounter.getAndIncrement()}-$nameSuffix"
        val openMode = direction.flag or 0x40000000

        handle = Kernel32.INSTANCE.CreateNamedPipe(
            pipeName,
            openMode,
            0,
            1,
            0,
            0,
            0,
            WinBase.SECURITY_ATTRIBUTES()
        )

        if (handle == WinBase.INVALID_HANDLE_VALUE) {
            throw IOException("Failed to create named pipe '$pipeName'")
        }
        namedPipe = com.pty4j.windows.winpty.NamedPipe(handle, false)
    }

    override val name: String
        get() = pipeName

    override val inputStream: InputStream
        get() = CygwinPTYInputStream(namedPipe)

    override val outputStream: OutputStream
        get() = CygwinPTYOutputStream(namedPipe)

    @Throws(IOException::class)
    override fun close() {
        namedPipe.close()
    }

    /**
     * 异步等待客户端连接，支持中断。
     * 返回true表示成功连接，false表示关闭中断。
     */
    @Throws(IOException::class)
    override fun waitForConnection(): Boolean {
        val shutdownEvent = Kernel32.INSTANCE.CreateEvent(null, true, false, null)
            ?: throw IOException("Failed to create shutdown event")

        val connectEvent = Kernel32.INSTANCE.CreateEvent(null, true, false, null)
            ?: throw IOException("Failed to create connect event")

        val overlapped = WinBase.OVERLAPPED()
        overlapped.hEvent = connectEvent

        val waitHandles = arrayOf(connectEvent, shutdownEvent)
        try {
            val connected = Kernel32.INSTANCE.ConnectNamedPipe(handle, overlapped)
            if (!connected) {
                val err = Kernel32.INSTANCE.GetLastError()
                if (err != WinError.ERROR_PIPE_CONNECTED && err != WinError.ERROR_IO_PENDING) {
                    throw IOException("ConnectNamedPipe failed with error code $err")
                }
            }

            val waitResult = Kernel32.INSTANCE.WaitForMultipleObjects(
                waitHandles.size,
                waitHandles,
                false,

                WinBase.INFINITE
            )

            return when (waitResult) {
                WinBase.WAIT_OBJECT_0 -> true  // Connect event triggered
                WinBase.WAIT_OBJECT_0 + 1 -> false // Shutdown event triggered
                else -> {
                    val err = Kernel32.INSTANCE.GetLastError()
                    throw IOException("WaitForMultipleObjects failed: $err")
                }
            }
        } finally {
            Kernel32.INSTANCE.CloseHandle(connectEvent)
            Kernel32.INSTANCE.CloseHandle(shutdownEvent)
        }
    }

    companion object {
        private val processCounter = AtomicInteger()

        @Throws(IOException::class)
        fun createOutboundPipe(nameSuffix: String): WinPipe =
            WinPipe(PipeDirection.Outbound, nameSuffix)

        @Throws(IOException::class)
        fun createInboundPipe(nameSuffix: String): WinPipe =
            WinPipe(PipeDirection.Inbound, nameSuffix)
    }

    enum class PipeDirection(val flag: Int) {
        Inbound(WinNT.PIPE_ACCESS_INBOUND),
        Outbound(WinNT.PIPE_ACCESS_OUTBOUND),
        Duplex(WinNT.PIPE_ACCESS_DUPLEX)
    }
}