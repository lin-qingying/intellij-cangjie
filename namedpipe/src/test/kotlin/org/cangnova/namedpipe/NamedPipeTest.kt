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
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.namedpipe

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class NamedPipeTest {

    // ── 1. 单向写读 ────────────────────────────────────────────────────────────
    @Test @Order(1)
    fun `server writes client reads`() = runTest {
        val factory = NamedPipeFactory.createUnix()
        val config  = PipeConfig("t_oneway", PipeAccess.WRITE_ONLY)

        val serverJob = async(Dispatchers.IO) {
            factory.createServer(config).serveAsync { it.writeLine("hello") }
        }

        delay(200)
        var received: String? = null
        factory.createClient(PipeConfig("t_oneway", PipeAccess.READ_ONLY))
            .callAsync { received = it.readLine() }

        serverJob.await()
        assertEquals("hello", received)
    }

    // ── 2. 双向 echo ────────────────────────────────────────────────────────────
    @Test @Order(2)
    fun `bidirectional echo`() = runTest {
        val factory = NamedPipeFactory.createUnix()
        val config  = PipeConfig("t_echo")

        val serverJob = async(Dispatchers.IO) {
            factory.createServer(config).serveAsync { s ->
                val m = s.readLine() ?: return@serveAsync
                s.writeLine("ECHO:$m")
            }
        }

        delay(200)
        var reply: String? = null
        factory.createClient(config).callAsync { c ->
            c.writeLine("ping")
            reply = c.readLine()
        }

        serverJob.await()
        assertEquals("ECHO:ping", reply)
    }

    // ── 3. 状态转换 ────────────────────────────────────────────────────────────
    @Test @Order(3)
    fun `state transitions`() {
        val server = NamedPipeFactory.createUnix()
            .createServer(PipeConfig("t_state", PipeAccess.WRITE_ONLY))
        assertEquals(PipeState.CONNECTING, server.state)
        server.close()
        assertEquals(PipeState.CLOSED, server.state)
    }

    // ── 4. 连接超时 ────────────────────────────────────────────────────────────
    @Test @Order(4)
    fun `client times out when no server`() {
        val client = NamedPipeFactory.createUnix()
            .createClient(PipeConfig("t_timeout_xyz", connectTimeoutMs = 300))
        assertThrows<PipeException.ConnectionTimeout> { client.connect() }
    }

    // ── 5. 管道路径 ────────────────────────────────────────────────────────────
    @Test @Order(5)
    fun `unix pipe path format`() {
        val path = NamedPipeFactory.createUnix().resolvePipePath("mypipe")
        assertTrue(path.endsWith("mypipe"))
    }

    @Test @Order(6)
    fun `windows pipe path format`() {
        assertEquals(
            "\\\\.\\pipe\\mypipe",
            NamedPipeFactory.createWindows().resolvePipePath("mypipe")
        )
    }

    // ── 6. 大数据传输 ─────────────────────────────────────────────────────────
    @Test @Order(7)
    fun `transfer 64KB payload`() = runTest {
        val factory = NamedPipeFactory.createUnix()
        val config  = PipeConfig("t_large", PipeAccess.WRITE_ONLY, bufferSize = 131072)
        val payload = ByteArray(65536) { it.toByte() }

        val serverJob = async(Dispatchers.IO) {
            factory.createServer(config).serveAsync { it.writeBytes(payload) }
        }

        delay(300)
        var received = ByteArray(0)
        factory.createClient(PipeConfig("t_large", PipeAccess.READ_ONLY, bufferSize = 131072))
            .callAsync { c ->
                val chunks = mutableListOf<Byte>()
                while (true) {
                    val b = c.readBytes(8192)
                    if (b.isEmpty()) break
                    chunks.addAll(b.toList())
                }
                received = chunks.toByteArray()
            }

        serverJob.await()
        assertArrayEquals(payload, received)
    }

    // ── 7. Okio readUtf8Line 直接访问 ─────────────────────────────────────────
    @Test @Order(8)
    fun `okio source readUtf8Line works`() = runTest {
        val factory = NamedPipeFactory.createUnix()
        val config  = PipeConfig("t_okio")

        val serverJob = async(Dispatchers.IO) {
            factory.createServer(config).serveAsync { s ->
                // 直接使用 Okio sink
                s.sink.writeUtf8("okio_line\n").emit()
            }
        }

        delay(200)
        var result: String? = null
        factory.createClient(config).callAsync { c ->
            // 直接使用 Okio source
            result = c.source.readUtf8Line()
        }

        serverJob.await()
        assertEquals("okio_line", result)
    }
}