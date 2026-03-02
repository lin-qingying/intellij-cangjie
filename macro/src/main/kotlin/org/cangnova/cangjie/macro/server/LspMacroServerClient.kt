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

package org.cangnova.cangjie.macro.server

import com.intellij.openapi.diagnostic.Logger
import org.cangnova.cangjie.macro.messages.CangJieMacroBundle
import org.cangnova.cangjie.macro.server.protocol.MacroMsgCodec
import org.cangnova.cangjie.macro.server.protocol.PipeTransport
import org.cangnova.cangjie.toolchain.api.CjSdk
import java.io.Closeable
import java.io.EOFException
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * LSPMacroServer 高层客户端
 *
 * 封装进程管理（[LspMacroServerProcess]）和管道通信（[PipeTransport]），
 * 提供面向业务的宏展开 API。
 *
 * ## 线程安全
 *
 * 使用 [ReentrantLock] 保证单次请求-响应的原子性，支持从多线程调用。
 *
 * ## 生命周期
 *
 * ```
 * client.start()           // 启动进程
 * client.loadLibs(paths)   // 加载宏动态库（必须先调用）
 * client.expandMacros(...) // 展开宏（可多次调用）
 * client.close()           // 关闭（发送 ExitTask + 等待进程退出）
 * ```
 */
class LspMacroServerClient(
    private val sdk: CjSdk,
    private val enableParallel: Boolean = true,
) : Closeable {

    private val logger = Logger.getInstance(LspMacroServerClient::class.java)

    private val lock = ReentrantLock()
    private var serverProcess: LspMacroServerProcess? = null
    private var transport: PipeTransport? = null

    /** 客户端是否已连接且可用 */
    val isConnected: Boolean get() = serverProcess?.isAlive == true && transport != null

    /**
     * 启动 LSPMacroServer 进程并建立管道连接
     *
     * @throws IllegalStateException 已连接
     * @throws java.io.IOException 进程启动失败
     */
    fun start() = lock.withLock {
        check(!isConnected) { "LSPMacroServer 客户端已连接" }

        val proc = LspMacroServerProcess(sdk, enableParallel)
        proc.start()

        transport = PipeTransport(proc.inputStream, proc.outputStream)
        serverProcess = proc
        logger.info("LSPMacroServer 客户端已就绪")
    }

    /**
     * 向服务端发送 DefLib 消息，加载宏动态库
     *
     * 必须在 [start] 之后、[expandMacros] 之前调用。
     *
     * @param libPaths 宏动态库绝对路径列表（`.dll`/`.so`/`.dylib`）
     */
    fun loadLibs(libPaths: List<String>) = lock.withLock {
        checkConnected()
        if (libPaths.isEmpty()) return@withLock
        logger.debug("加载宏动态库: $libPaths")
        transport!!.send(MacroMsgCodec.buildDefLib(libPaths))
        // DefLib 无响应消息，服务端直接处理
    }

    /**
     * 请求展开一批宏调用，返回每个调用的展开结果
     *
     * 由于 LSPMacroServer 使用文件级展开（接收 MultiMacroCalls 后逐个返回 MacroResult），
     * 此方法按顺序发送请求并收集响应。
     *
     * **注意**：`MultiMacroCalls` 的构建需要完整的 Token 序列和位置信息，
     * 这些数据由调用者提供（通常来自 LSP 服务器的语义分析结果）。
     *
     * @param rawMultiCallsPayload 已序列化的 MultiMacroCalls FlatBuffers payload
     * @param expectedCount 预期返回的 MacroResult 数量
     * @return 展开结果列表（顺序与请求对应）
     * @throws LspMacroServerException 通信错误或展开失败
     */
    fun expandMacros(
        rawMultiCallsPayload: ByteArray,
        expectedCount: Int,
    ): List<MacroMsgCodec.ParsedMacroResult> = lock.withLock {
        checkConnected()
        transport!!.send(rawMultiCallsPayload)

        // 接收 expectedCount 个 MacroResult 响应
        val results = mutableListOf<MacroMsgCodec.ParsedMacroResult>()
        repeat(expectedCount) {
            val payload = transport!!.receive()
            val msgType = MacroMsgCodec.getMsgType(payload)
            if (msgType != MacroMsgCodec.TYPE_MACRO_RESULT) {
                throw LspMacroServerException(CangJieMacroBundle.message("macro.lsp.error.unexpected.message.type", msgType))
            }
            val result = MacroMsgCodec.parseMacroResult(payload)
                ?: throw LspMacroServerException(CangJieMacroBundle.message("macro.lsp.error.macro.result.parse.failed"))
            results.add(result)
        }
        results
    }

    /**
     * 优雅关闭：发送 ExitTask，等待进程退出
     */
    override fun close() {
        lock.withLock {
            val t = transport ?: return@withLock
            runCatching {
                t.send(MacroMsgCodec.buildExitTask())
            }.onFailure { e ->
                logger.warn("发送 ExitTask 失败: ${e.message}")
            }
            t.close()
            transport = null
        }

        serverProcess?.stopGracefully()
        serverProcess = null
        logger.info("LSPMacroServer 客户端已关闭")
    }

    /** 强制关闭（不发送 ExitTask） */
    fun forceClose() {
        lock.withLock {
            transport?.close()
            transport = null
        }
        serverProcess?.forceStop()
        serverProcess = null
    }

    private fun checkConnected() {
        if (!isConnected) {
            // 检测管道是否仍然有效（进程可能意外退出）
            if (serverProcess?.isAlive == false) {
                throw LspMacroServerException(CangJieMacroBundle.message("macro.lsp.error.process.exited"))
            }
            throw LspMacroServerException("LSPMacroServer 客户端未连接，请先调用 start()")
        }
    }
}

/** LSPMacroServer 通信异常 */
class LspMacroServerException(message: String, cause: Throwable? = null) :
    Exception(message, cause)
