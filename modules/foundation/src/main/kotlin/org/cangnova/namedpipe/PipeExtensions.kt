package org.cangnova.namedpipe

import okio.ByteString

// ─────────────────────────────────────────────────────────────────────────────
// 扩展函数 —— 基于 Okio BufferedSource/Sink API
// 替代原来手写 PrintWriter / BufferedReader 包装
// ─────────────────────────────────────────────────────────────────────────────

/** 写入 UTF-8 文本行（自动追加 \n） */
fun NamedPipe.writeLine(line: String) {
    sink.writeUtf8(line).writeUtf8("\n").emit()
}

/** 读取一行 UTF-8 文本，EOF 时返回 null */
fun NamedPipe.readLine(): String? = source.readUtf8Line()

/** 写入字节数组 */
fun NamedPipe.writeBytes(data: ByteArray) {
    sink.write(data).emit()
}

/** 读取最多 maxLen 字节 */
fun NamedPipe.readBytes(maxLen: Long = 65536): ByteArray {
    source.request(maxLen)
    val n = source.buffer.size.coerceAtMost(maxLen)
    return if (n <= 0) ByteArray(0) else source.readByteArray(n)
}

/** 写入 Okio ByteString */
fun NamedPipe.write(data: ByteString) {
    sink.write(data).emit()
}

// ─────────────────────────────────────────────────────────────────────────────
// DSL helpers
// ─────────────────────────────────────────────────────────────────────────────

/** 同步服务端 use-DSL */
fun ServerNamedPipe.serve(block: (ServerNamedPipe) -> Unit) = use { s ->
    s.waitForConnection(); block(s)
}

/** 同步客户端 use-DSL */
fun ClientNamedPipe.call(block: (ClientNamedPipe) -> Unit) = use { c ->
    c.connect(); block(c)
}

/** 协程服务端 DSL */
suspend fun ServerNamedPipe.serveAsync(block: suspend (ServerNamedPipe) -> Unit) = use { s ->
    s.waitForConnectionAsync(); block(s)
}

/** 协程客户端 DSL */
suspend fun ClientNamedPipe.callAsync(block: suspend (ClientNamedPipe) -> Unit) = use { c ->
    c.connectAsync(); block(c)
}

/** 持续服务循环 */
fun ServerNamedPipe.loop(shouldStop: () -> Boolean = { false }, handler: (ServerNamedPipe) -> Unit) {
    while (!shouldStop() && !Thread.currentThread().isInterrupted) {
        try {
            waitForConnection()
            try { handler(this) } finally { disconnect() }
        } catch (e: PipeException) {
            if (shouldStop()) break else throw e
        }
    }
}
