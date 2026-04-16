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

    /** 服务端当前已加载的宏库路径集合，用于避免重复发送 DefLib */
    private var loadedLibPaths: Set<String> = emptySet()

    /** 客户端是否已连接且可用 */
    val isConnected: Boolean get() = serverProcess?.isAlive == true && transport != null

    /**
     * 启动 LSPMacroServer 进程并建立管道连接
     *
     * 内部使用 [LspMacroServerProcess.unifiedInputStream] /
     * [LspMacroServerProcess.unifiedOutputStream]，自动适配 Windows / Unix。
     *
     * @throws IllegalStateException 已连接
     * @throws java.io.IOException 进程启动失败
     */
    fun start() = lock.withLock {
        check(!isConnected) { "LSPMacroServer 客户端已连接" }

        val proc = LspMacroServerProcess(sdk, enableParallel)
        proc.start()

        // unifiedInputStream / unifiedOutputStream 在 Windows 下使用 JNA HANDLE 包装流，
        // 在 Unix 下直接使用子进程 stdin/stdout，PipeTransport 无需感知平台差异
        transport = PipeTransport(proc.unifiedInputStream, proc.unifiedOutputStream)
        serverProcess = proc
        logger.info("LSPMacroServer 客户端已就绪")
    }

    /**
     * 向服务端发送 DefLib 消息，加载宏动态库
     *
     * 仅在库路径集合发生变化时才发送，避免重复加载导致句柄堆积。
     * 必须在 [start] 之后、[expandMacros] 之前调用。
     *
     * @param libPaths 宏动态库绝对路径列表（`.dll`/`.so`/`.dylib`）
     */
    fun loadLibs(libPaths: List<String>) = lock.withLock {
        checkConnected()
        if (libPaths.isEmpty()) return@withLock
        val newPaths = libPaths.toSet()
        if (newPaths == loadedLibPaths) {
            logger.debug("宏动态库路径未变化，跳过 DefLib 发送")
            return@withLock
        }
        logger.debug("加载宏动态库: $libPaths")
        transport!!.send(MacroMsgCodec.buildDefLib(libPaths))
        transport!!.receive()
        loadedLibPaths = newPaths
    }

    /**
     * 发送 ExitTask(flag=false)，重置服务端宏展开状态
     *
     * 清空服务端的宏声明、宏调用、诊断信息，但进程继续运行。
     * 每次 [expandMacros] 完成后应调用此方法，与编译器的 SendExitStgTask 行为一致。
     */
    fun resetStage() = lock.withLock {
        if (!isConnected) return@withLock
        runCatching {
            transport!!.send(MacroMsgCodec.buildResetStageTask())
        }.onFailure { e ->
            logger.warn("发送 ResetStage 失败: ${e.message}")
        }
    }

    /**
     * 请求展开一批宏调用，返回每个调用的展开结果
     *
     * LSPMacroServer 协议要求**每次只发送一条 MacroCall 并接收一条 MacroResult**：
     * - C++ 的 `EvalMacroCallsAndWaitResult` 每次调用仅处理并返回一条结果，
     *   处理完后服务端返回 `for(;;)` 等待下一条消息
     * - 串行模式下，`FindMacroDefMethod` 失败会直接退出进程而不发任何响应；
     *   并行模式下，失败时始终发送 `STATUS_FAIL` 响应，因此服务端需以并行模式启动
     *
     * @param calls 宏调用信息列表，每条将单独发送
     * @return 展开结果列表（顺序与请求对应）
     * @throws LspMacroServerException 通信错误或展开失败
     */
    fun expandMacros(
        calls: List<MacroMsgCodec.MacroCallInfo>,
    ): List<MacroMsgCodec.ParsedMacroResult> = lock.withLock {
        checkConnected()
        try {
            calls.map { callInfo ->
                // 每条宏调用单独发送，等待一条 MacroResult 响应
                val payload = MacroMsgCodec.buildMultiMacroCalls(listOf(callInfo))
                transport!!.send(payload)

                val response = transport!!.receive()
                val msgType = MacroMsgCodec.getMsgType(response)
                if (msgType != MacroMsgCodec.TYPE_MACRO_RESULT) {
                    throw LspMacroServerException(
                        CangJieMacroBundle.message("macro.lsp.error.unexpected.message.type", msgType)
                    )
                }
                MacroMsgCodec.parseMacroResult(response)
                    ?: throw LspMacroServerException(
                        CangJieMacroBundle.message("macro.lsp.error.macro.result.parse.failed")
                    )
            }
        } catch (e: LspMacroServerException) {
            throw e
        } catch (e: java.io.IOException) {
            throw LspMacroServerException(
                e.message ?: CangJieMacroBundle.message("macro.lsp.error.process.exited"), e
            )
        }
    }

    /**
     * 优雅关闭：发送 ExitTask(flag=true)，等待进程退出
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
            loadedLibPaths = emptySet()
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
            loadedLibPaths = emptySet()
        }
        serverProcess?.forceStop()
        serverProcess = null
    }

    private fun checkConnected() {
        if (!isConnected) {
            if (serverProcess?.isAlive == false) {
                throw LspMacroServerException(
                    CangJieMacroBundle.message("macro.lsp.error.process.exited")
                )
            }
            throw LspMacroServerException("LSPMacroServer 客户端未连接，请先调用 start()")
        }
    }
}

/** LSPMacroServer 通信异常 */
class LspMacroServerException(message: String, cause: Throwable? = null) :
    Exception(message, cause)