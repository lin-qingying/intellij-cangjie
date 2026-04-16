/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.cangnova.cangjie.macro.server.protocol

import java.io.EOFException
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * 管道传输层
 *
 * 实现 LSPMacroServer 通信的 length-prefixed 帧协议：
 * ```
 * [ 8字节 size_t (uint64_le)：payload 长度 ][ N字节 payload ]
 * ```
 *
 * C++ 端 `ReadMsgFromClient` / `SendMsgToClient` 均使用
 * `sizeof(size_t) = 8` 字节读写长度前缀（64 位系统），此处与之对齐。
 *
 * 管道缓冲区限制为 4096 字节，写入时自动分片；读取时按帧长度完整接收。
 */
class PipeTransport(
    private val inputStream: InputStream,
    private val outputStream: OutputStream,
) {
    companion object {
        /** 管道每次写入的分片大小（受 OS 管道缓冲区限制） */
        private const val CHUNK_SIZE = 4096

        /** C++ sizeof(size_t) on 64-bit = 8 */
        private const val SIZE_PREFIX_BYTES = 8
    }

    /**
     * 发送一条消息
     *
     * @param payload FlatBuffers 编码的消息字节数组
     * @throws java.io.IOException 管道写入失败
     */
    fun send(payload: ByteArray) {
        // 写 8 字节小端长度前缀（与 C++ sizeof(size_t) 对齐）
        val lenBuf = ByteBuffer.allocate(SIZE_PREFIX_BYTES).order(ByteOrder.LITTLE_ENDIAN)
        lenBuf.putLong(payload.size.toLong())
        outputStream.write(lenBuf.array())


        // 分片写入（管道缓冲区有限）
        var offset = 0
        while (offset < payload.size) {
            val len = minOf(CHUNK_SIZE, payload.size - offset)
            outputStream.write(payload, offset, len)
            offset += len
        }
        outputStream.flush()
    }

    /**
     * 接收一条消息（阻塞直到收到完整帧）
     *
     * @return FlatBuffers 编码的消息字节数组
     * @throws EOFException 管道已关闭
     * @throws java.io.IOException 读取失败
     */
    fun receive(): ByteArray {
        // 读 8 字节长度前缀（C++ sizeof(size_t) = 8 on 64-bit）
        val lenBuf = ByteArray(SIZE_PREFIX_BYTES)
        readFully(lenBuf)
        val len = ByteBuffer.wrap(lenBuf).order(ByteOrder.LITTLE_ENDIAN).getLong().toInt()

        // 读取 payload
        val payload = ByteArray(len)
        readFully(payload)
        return payload
    }

    /**
     * 完整读取指定字节数（处理短读）
     */
    private fun readFully(buf: ByteArray) {
        var read = 0
        while (read < buf.size) {
            val n = inputStream.read(buf, read, buf.size - read)
            if (n < 0) throw EOFException("管道已关闭（已读 $read/${buf.size} 字节）")
            read += n
        }
    }

    /** 关闭传输层（关闭底层流） */
    fun close() {
        runCatching { outputStream.close() }
        runCatching { inputStream.close() }
    }
}
