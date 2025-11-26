/*
 * Copyright 2025 LinQingYing. and contributors.
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

package org.cangnova.cangjie.debugger.dap

import com.intellij.openapi.diagnostic.Logger
import org.cangnova.cangjie.debugger.core.AdapterEvent
import org.cangnova.cangjie.debugger.core.BreakpointInfo
import org.cangnova.cangjie.debugger.core.StopReason
import org.cangnova.cangjie.debugger.exception.DapClientException
import org.eclipse.lsp4j.debug.*
import org.eclipse.lsp4j.debug.services.IDebugProtocolClient
import java.io.File
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * DAP客户端实现
 *
 * 处理来自DAP服务器的事件和请求
 */
class DapClient(

    private val eventDispatcher: (AdapterEvent) -> Unit
) : IDebugProtocolClient {

    companion object {
        private val LOG = Logger.getInstance(DapClient::class.java)
    }

    // 管理启动的进程
    private val managedProcesses = ConcurrentHashMap<Int, Process>()

    override fun initialized() {
        LOG.info("DAP server initialized")
        eventDispatcher(AdapterEvent.Initialized)


    }

    override fun stopped(args: StoppedEventArguments) {
        LOG.info("Stopped: reason=${args.reason}, threadId=${args.threadId}, hitBreakpointIds=${args.hitBreakpointIds?.joinToString()}")

        val event = AdapterEvent.Stopped(
            reason = mapStopReason(args.reason),
            threadId = args.threadId?.toLong() ?: 0,
            allThreadsStopped = args.allThreadsStopped ?: false,
            hitBreakpointIds = args.hitBreakpointIds?.map { it.toInt() } ?: emptyList(),
            description = args.description,
            text = args.text
        )

        eventDispatcher(event)
    }

    override fun continued(args: ContinuedEventArguments) {
        LOG.info("Continued: threadId=${args.threadId}")

        val event = AdapterEvent.Continued(
            threadId = args.threadId.toLong(),
            allThreadsContinued = args.allThreadsContinued ?: false
        )

        eventDispatcher(event)
    }

    override fun exited(args: ExitedEventArguments) {
        LOG.info("Exited: exitCode=${args.exitCode}")
        eventDispatcher(AdapterEvent.Exited(args.exitCode))
    }

    override fun terminated(args: TerminatedEventArguments?) {
        LOG.info("Terminated")
        cleanupProcesses()
        eventDispatcher(AdapterEvent.Terminated)
    }

    override fun thread(args: ThreadEventArguments) {
        LOG.debug("Thread event: reason=${args.reason}, threadId=${args.threadId}")

        val event = when (args.reason) {
            "started" -> AdapterEvent.ThreadStarted(args.threadId.toLong())
            "exited" -> AdapterEvent.ThreadExited(args.threadId.toLong())
            else -> return
        }

        eventDispatcher(event)
    }

    override fun output(args: OutputEventArguments) {
        val content = args.output ?: return

        LOG.debug("Output: category=${args.category}, length=${content.length}")

        val event = AdapterEvent.Output(
            category = args.category ?: "console",
            output = content,
            source = args.source?.path,
            line = args.line,
            column = args.column
        )

        eventDispatcher(event)
    }

    override fun breakpoint(args: BreakpointEventArguments) {
        LOG.info("Breakpoint event: reason=${args.reason}, id=${args.breakpoint.id}")

        val event = AdapterEvent.BreakpointChanged(
            reason = args.reason,
            breakpoint = mapBreakpoint(args.breakpoint)
        )

        eventDispatcher(event)
    }

    override fun module(args: ModuleEventArguments) {
        LOG.debug("Module event: reason=${args.reason}")
        // 可以在这里处理模块加载事件
    }

    override fun loadedSource(args: LoadedSourceEventArguments) {
        LOG.debug("Loaded source: reason=${args.reason}")
        // 可以在这里处理源文件加载事件
    }

    override fun process(args: ProcessEventArguments) {
        LOG.info("Process event: name=${args.name}, startMethod=${args.startMethod}")
        // 可以在这里处理进程事件
    }

    override fun capabilities(args: CapabilitiesEventArguments) {
        LOG.debug("Capabilities changed")
        // 可以在这里处理能力变化事件
    }

    override fun progressStart(args: ProgressStartEventArguments) {
        LOG.debug("Progress start: ${args.title}")
    }

    override fun progressUpdate(args: ProgressUpdateEventArguments) {
        LOG.debug("Progress update: ${args.message}")
    }

    override fun progressEnd(args: ProgressEndEventArguments) {
        LOG.debug("Progress end")
    }

    override fun invalidated(args: InvalidatedEventArguments) {
        LOG.info("Invalidated: areas=${args.areas?.joinToString()}")
        eventDispatcher(AdapterEvent.Invalidated(args.areas?.toList() ?: emptyList()))
    }

    override fun memory(args: MemoryEventArguments) {
        LOG.debug("Memory event: offset=${args.offset}, count=${args.count}")
    }

    override fun runInTerminal(
        args: RunInTerminalRequestArguments
    ): CompletableFuture<RunInTerminalResponse> {
        return CompletableFuture.supplyAsync {
            try {
                LOG.info("Running in terminal: ${args.args.joinToString(" ")}")

                val processBuilder = ProcessBuilder(*args.args)

                // 设置工作目录
                args.cwd?.let { processBuilder.directory(File(it)) }

                // 设置环境变量
                args.env?.forEach { (key, value) ->
                    processBuilder.environment()[key] = value
                }

                // 启动进程
                val process = processBuilder.start()
                val pid = process.pid().toInt()

                // 管理进程生命周期
                managedProcesses[pid] = process

                LOG.info("Process started: pid=$pid")

                RunInTerminalResponse().apply {
                    processId = pid
                    shellProcessId = null
                }
            } catch (e: Exception) {
                LOG.error("Failed to run in terminal", e)
                throw DapClientException("Failed to run in terminal", e)
            }
        }
    }

    private fun mapStopReason(reason: String): StopReason {
        return when (reason) {
            "step" -> StopReason.STEP
            "breakpoint" -> StopReason.BREAKPOINT
            "exception" -> StopReason.EXCEPTION
            "pause" -> StopReason.PAUSE
            "entry" -> StopReason.ENTRY
            else -> StopReason.UNKNOWN
        }
    }

    private fun mapBreakpoint(bp: Breakpoint): BreakpointInfo {
        return BreakpointInfo(
            id = bp.id,
            verified = bp.isVerified,
            line = bp.line,
            column = bp.column,
            message = bp.message,
            source = bp.source?.path
        )
    }

    private fun cleanupProcesses() {
        managedProcesses.values.forEach { process ->
            if (process.isAlive) {
                LOG.info("Terminating process: pid=${process.pid()}")
                process.destroy()

                // 等待一段时间后强制终止
                if (!process.waitFor(5, TimeUnit.SECONDS)) {
                    LOG.warn("Force killing process: pid=${process.pid()}")
                    process.destroyForcibly()
                }
            }
        }
        managedProcesses.clear()
    }

    /**
     * 清理所有资源
     */
    fun dispose() {
        cleanupProcesses()
    }
}