package org.cangnova.namedpipe

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.*
import okio.Path.Companion.toPath
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.PosixFilePermissions

private val log = KotlinLogging.logger {}

// ─────────────────────────────────────────────────────────────────────────────
// Unix Factory
// ─────────────────────────────────────────────────────────────────────────────

internal class UnixNamedPipeFactory : NamedPipeFactory {

    companion object {
        val PIPE_DIR: Path by lazy {
            val xdg = System.getenv("XDG_RUNTIME_DIR")
            val base = if (!xdg.isNullOrBlank()) Paths.get(xdg) else Paths.get("/tmp")
            base.resolve(".namedpipes").also { dir ->
                if (!Files.exists(dir)) {
                    Files.createDirectories(dir)
                    runCatching {
                        Files.setPosixFilePermissions(dir, PosixFilePermissions.fromString("rwxr-xr-x"))
                    }
                }
            }
        }
    }

    override fun resolvePipePath(name: String): String =
        PIPE_DIR.resolve(name).toAbsolutePath().toString()

    override fun createServer(config: PipeConfig): ServerNamedPipe =
        UnixServerNamedPipe(config, this)

    override fun createClient(config: PipeConfig): ClientNamedPipe =
        UnixClientNamedPipe(config, this)
}

// ─────────────────────────────────────────────────────────────────────────────
// Unix Base — 使用 Okio FileSystem 统一处理 I/O
// ─────────────────────────────────────────────────────────────────────────────

internal abstract class UnixBaseNamedPipe(
    override val config: PipeConfig,
    protected val factory: UnixNamedPipeFactory
) : NamedPipe {

    override val pipePath: String = factory.resolvePipePath(config.name)

    @Volatile
    override var state: PipeState = PipeState.CLOSED
        protected set

    // Okio 替代原始 InputStream/OutputStream 包装
    protected var _source: BufferedSource? = null
    protected var _sink: BufferedSink? = null

    override val source: BufferedSource
        get() {
            check(state == PipeState.CONNECTED) { "Pipe '${config.name}' is not connected" }
            check(config.access != PipeAccess.WRITE_ONLY) { "Pipe is WRITE_ONLY" }
            return _source ?: error("Source not initialized")
        }

    override val sink: BufferedSink
        get() {
            check(state == PipeState.CONNECTED) { "Pipe '${config.name}' is not connected" }
            check(config.access != PipeAccess.READ_ONLY) { "Pipe is READ_ONLY" }
            return _sink ?: error("Sink not initialized")
        }

    override fun disconnect() {
        runCatching { _sink?.close() }
        runCatching { _source?.close() }
        _source = null
        _sink = null
        state = PipeState.DISCONNECTED
        log.debug { "Pipe '${config.name}' disconnected" }
    }

    override fun close() {
        disconnect()
        state = PipeState.CLOSED
    }

    // ── Okio source / sink 工厂 ──────────────────────────────────────────────

    /** 用 Okio FileSystem 打开 FIFO，返回 BufferedSource */
    protected fun openSource(path: String): BufferedSource =
        FileSystem.SYSTEM.source(path.toPath()).buffer()

    /** 用 Okio FileSystem 打开 FIFO，返回 BufferedSink */
    protected fun openSink(path: String): BufferedSink =
        FileSystem.SYSTEM.sink(path.toPath(), mustCreate = false).buffer()

    protected fun mkfifo(path: String) {
        val process = ProcessBuilder("mkfifo", path).redirectErrorStream(true).start()
        val exit = process.waitFor()
        if (exit != 0 && !File(path).exists()) {
            val msg = process.inputStream.bufferedReader().readText()
            throw PipeException.IoError(config.name, IOException("mkfifo failed: $msg"))
        }
        log.debug { "mkfifo created: $path" }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Unix Server
// ─────────────────────────────────────────────────────────────────────────────

internal class UnixServerNamedPipe(
    config: PipeConfig,
    factory: UnixNamedPipeFactory
) : UnixBaseNamedPipe(config, factory), ServerNamedPipe {

    init {
        state = PipeState.CONNECTING
        createFifoFiles()
    }

    private fun createFifoFiles() {
        when (config.access) {
            PipeAccess.READ_WRITE -> {
                listOf("$pipePath.r", "$pipePath.w").forEach { p ->
                    File(p).delete()
                    mkfifo(p)
                }
            }
            else -> { File(pipePath).delete(); mkfifo(pipePath) }
        }
    }

    override fun waitForConnection() {
        try {
            when (config.access) {
                PipeAccess.READ_ONLY  -> _source = openSource(pipePath)
                PipeAccess.WRITE_ONLY -> _sink   = openSink(pipePath)
                PipeAccess.READ_WRITE -> {
                    // 服务端先开 sink（.w），再开 source（.r），客户端顺序相反，避免死锁
                    _sink   = openSink("$pipePath.w")
                    _source = openSource("$pipePath.r")
                }
            }
            state = PipeState.CONNECTED
            log.info { "Server pipe '${config.name}' client connected" }
        } catch (e: IOException) {
            state = PipeState.ERROR
            throw PipeException.IoError(config.name, e)
        }
    }

    /**
     * 协程版本 —— 在 IO Dispatcher 执行阻塞打开，不占用调用线程
     * 替代原来的 Executors + 回调 ~15 行
     */
    override suspend fun waitForConnectionAsync() =
        withContext(Dispatchers.IO) { waitForConnection() }

    override fun close() {
        super.close()
        when (config.access) {
            PipeAccess.READ_WRITE -> { File("$pipePath.r").delete(); File("$pipePath.w").delete() }
            else -> File(pipePath).delete()
        }
        log.debug { "Server pipe '${config.name}' closed & FIFO removed" }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Unix Client
// ─────────────────────────────────────────────────────────────────────────────

internal class UnixClientNamedPipe(
    config: PipeConfig,
    factory: UnixNamedPipeFactory
) : UnixBaseNamedPipe(config, factory), ClientNamedPipe {

    override fun connect() {
        state = PipeState.CONNECTING
        val deadline = if (config.connectTimeoutMs > 0)
            System.currentTimeMillis() + config.connectTimeoutMs else Long.MAX_VALUE

        fun fifoReady() = when (config.access) {
            PipeAccess.READ_WRITE -> File("$pipePath.r").exists() && File("$pipePath.w").exists()
            else                  -> File(pipePath).exists()
        }

        try {
            while (!fifoReady()) {
                if (System.currentTimeMillis() > deadline)
                    throw PipeException.ConnectionTimeout(config.name, config.connectTimeoutMs)
                Thread.sleep(50)
            }
            when (config.access) {
                PipeAccess.READ_ONLY  -> _source = openSource(pipePath)
                PipeAccess.WRITE_ONLY -> _sink   = openSink(pipePath)
                PipeAccess.READ_WRITE -> {
                    // 客户端先开 source（.w 端），再开 sink（.r 端）——与服务端相反
                    _source = openSource("$pipePath.w")
                    _sink   = openSink("$pipePath.r")
                }
            }
            state = PipeState.CONNECTED
            log.info { "Client pipe '${config.name}' connected" }
        } catch (e: PipeException) {
            state = PipeState.ERROR; throw e
        } catch (e: IOException) {
            state = PipeState.ERROR
            throw when {
                e.message?.contains("No such file") == true -> PipeException.NotFound(config.name)
                e.message?.contains("Permission denied") == true -> PipeException.AccessDenied(config.name)
                else -> PipeException.IoError(config.name, e)
            }
        }
    }

    /** 协程版本，替代原来的 Executor + 回调 */
    override suspend fun connectAsync() =
        withContext(Dispatchers.IO) { connect() }
}
