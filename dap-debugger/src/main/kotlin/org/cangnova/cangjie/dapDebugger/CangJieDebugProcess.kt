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

package org.cangnova.cangjie.dapDebugger

import com.intellij.xdebugger.XDebugProcess
import com.intellij.xdebugger.XDebugSession
import com.intellij.xdebugger.XSourcePosition
import com.intellij.xdebugger.breakpoints.XBreakpointHandler
import com.intellij.xdebugger.breakpoints.XLineBreakpoint
import com.intellij.xdebugger.evaluation.XDebuggerEditorsProvider
import com.intellij.xdebugger.frame.XSuspendContext
import org.cangnova.cangjie.dapDebugger.breakpoint.CangJieBreakpointHandler
import org.cangnova.cangjie.dapDebugger.breakpoint.CangJieLineBreakpointType
import org.cangnova.cangjie.dapDebugger.stack.CangJieSuspendContext
import org.eclipse.lsp4j.debug.*

class CangJieDebugProcess(val parameters: RunParameters, session: XDebugSession) : XDebugProcess(session) {

    private val breakpointHandler = CangJieBreakpointHandler(this)
    private val dapConnection = DapConnection(
        session,
        parameters = parameters,
        breakpointHandler = breakpointHandler
    )
    private lateinit var adapterProcess: DapProcessHandler
    private val editorsProvider = CangJieDebuggerEditorsProvider()

    init {
        startDebugAdapter()
        connectToDebugAdapter()
    }

    /**
     * 启动调试适配器
     */
    private fun startDebugAdapter() {
        try {
            // 使用GeneralCommandLine启动调试适配器
            adapterProcess = DapProcessHandler(parameters.command).apply {
                startNotify()
            }

        } catch (e: Exception) {
            session.reportError("Failed to start debug adapter: ${e.message}")
            throw e
        }
    }

    /**
     * 连接到调试适配器
     */
    private fun connectToDebugAdapter() {
        try {
            dapConnection.connect()
        } catch (e: Exception) {
            session.reportError("Failed to connect to debug adapter: ${e.message}")
        }
    }

    override fun stop() {
        try {
            // 发送终止请求
            dapConnection.getServer().terminate(TerminateArguments())

            // 通知断点处理器会话已停止
            breakpointHandler.sessionStopped()

            // 断开连接并清理
            dapConnection.disconnect()
            if (::adapterProcess.isInitialized) {
                adapterProcess.destroyProcess()
            }
        } catch (e: Exception) {
            // 如果发送请求失败，直接关闭连接并销毁进程
            dapConnection.disconnect()
            if (::adapterProcess.isInitialized) {
                adapterProcess.destroyProcess()
            }
            session.reportError("Failed to stop debug adapter: ${e.message}")
        }
    }

    override fun getEditorsProvider(): XDebuggerEditorsProvider = editorsProvider

    override fun getBreakpointHandlers(): Array<XBreakpointHandler<*>> {
        return arrayOf(breakpointHandler)
    }

    // 提供给 BreakpointHandler 使用的方法
    fun getConnection(): DapConnection = dapConnection

    fun disableAllBreakpoints() {
        breakpointHandler.disableAllBreakpoints()
    }

    fun enableAllBreakpoints() {
        breakpointHandler.enableAllBreakpoints()
    }

    fun toggleBreakpoint(breakpoint: XLineBreakpoint<CangJieLineBreakpointType.Properties>) {
        breakpointHandler.toggleBreakpoint(breakpoint)
    }

    // 继续执行
    override fun resume(context: XSuspendContext?) {
        if (context is CangJieSuspendContext) {
            getConnection().getServer().continue_(ContinueArguments().apply {
                threadId = context.activeThreadId.toInt()
            })
        }
    }

    // 步过
    override fun startStepOver(context: XSuspendContext?) {
        if (context is CangJieSuspendContext) {
            dapConnection.getServer().next(NextArguments().apply {
                threadId = context.activeThreadId.toInt()
            })
        }
    }

    // 步入
    override fun startStepInto(context: XSuspendContext?) {
        if (context is CangJieSuspendContext) {
            dapConnection.getServer().stepIn(StepInArguments().apply {
                threadId = context.activeThreadId.toInt()
            })
        }
    }

    // 步出
    override fun startStepOut(context: XSuspendContext?) {
        if (context is CangJieSuspendContext) {
            dapConnection.getServer().stepOut(StepOutArguments().apply {
                threadId = context.activeThreadId.toInt()
            })
        }
    }



    // 运行到光标处
    override fun runToPosition(position: XSourcePosition, context: XSuspendContext?) {
        if (context is CangJieSuspendContext) {
            // 设置一个一次性断点
            dapConnection.getServer().setBreakpoints(SetBreakpointsArguments().apply {
                source = Source().apply {
                    path = position.file.path
                }
                breakpoints = arrayOf(SourceBreakpoint().apply {
                    line = position.line + 1  // DAP使用1-based行号
                })
                sourceModified = false
            }).thenAccept { _ ->
                // 断点设置成功后继续执行
                resume(context)
            }
        }
    }
}
