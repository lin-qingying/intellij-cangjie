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

package org.cangnova.cangjie.debugger.ui

import com.intellij.openapi.diagnostic.Logger
import com.intellij.xdebugger.XDebugProcess
import com.intellij.xdebugger.XDebugSession
import com.intellij.xdebugger.XSourcePosition
import com.intellij.xdebugger.breakpoints.XBreakpointHandler
import com.intellij.xdebugger.evaluation.XDebuggerEditorsProvider
import com.intellij.xdebugger.frame.XSuspendContext
import kotlinx.coroutines.runBlocking
import org.cangnova.cangjie.debugger.config.ServerConfig
import org.cangnova.cangjie.debugger.core.AdapterConfig
import org.cangnova.cangjie.debugger.core.ConnectionConfig
import org.cangnova.cangjie.debugger.core.LaunchConfig
import org.cangnova.cangjie.debugger.dap.DapAdapter
import org.cangnova.cangjie.debugger.process.ProcessManager
import org.cangnova.cangjie.debugger.service.BreakpointService
import org.cangnova.cangjie.debugger.service.DebugSessionService
import org.cangnova.cangjie.debugger.service.EvaluationService
import org.cangnova.cangjie.debugger.service.VariableService

/**
 * 仓颉调试进程
 *
 * IntelliJ平台的调试进程实现，集成所有服务
 */
class CangJieDebugProcess(
    private val runConfig: CangJieRunConfiguration,
    session: XDebugSession
) : XDebugProcess(session) {

    companion object {
        private val LOG = Logger.getInstance(CangJieDebugProcess::class.java)
    }

    // 服务组件
    private lateinit var processManager: ProcessManager
    private lateinit var debugSession: DebugSessionService
    private lateinit var breakpointService: BreakpointService
    private lateinit var evaluationService: EvaluationService
    private lateinit var variableService: VariableService

    // UI组件
    private val editorsProvider = CangJieDebuggerEditorsProvider()
    private lateinit var breakpointHandler: CangJieBreakpointHandler

    init {
        initialize()
    }

    private fun initialize() {
        try {
            LOG.info("Initializing CangJie debug process")

            // 创建进程管理器
            processManager = ProcessManager(session.project)

            // 启动调试服务器
            val serverInfo = runBlocking {
                processManager.startServer(ServerConfig()).getOrThrow()
            }

            // 创建适配器
            val adapter = DapAdapter(session.project)

            // 创建服务
            debugSession = DebugSessionService(session.project, session, this)
            debugSession.initialize(adapter)

            breakpointService = BreakpointService(session.project, adapter)
            evaluationService = EvaluationService(adapter)
            variableService = VariableService(adapter)

            // 创建断点处理器
            breakpointHandler = CangJieBreakpointHandler(this)

            // 启动调试会话
            val launchConfig = createLaunchConfig(serverInfo)
            runBlocking {
                debugSession.start(launchConfig).getOrThrow()
            }

            // 同步断点
            runBlocking {
                breakpointService.synchronize()
            }

            LOG.info("CangJie debug process initialized successfully")
        } catch (e: Exception) {
            LOG.error("Failed to initialize debug process", e)
            session.reportError("Failed to start debugger: ${e.message}")
            throw e
        }
    }

    private fun createLaunchConfig(serverInfo: org.cangnova.cangjie.debugger.process.ServerInfo): LaunchConfig {
        return LaunchConfig(
            program = runConfig.programPath,
            arguments = runConfig.programArguments,
            workingDirectory = runConfig.workingDirectory,
            environment = runConfig.environmentVariables,
            adapterConfig = AdapterConfig(
                host = serverInfo.host,
                port = serverInfo.port,
                connectionConfig = ConnectionConfig()
            )
        )
    }

    override fun getEditorsProvider(): XDebuggerEditorsProvider {
        return editorsProvider
    }

    override fun getBreakpointHandlers(): Array<XBreakpointHandler<*>> {
        return arrayOf(breakpointHandler)
    }

    override fun resume(context: XSuspendContext?) {
        runBlocking {
            val threadId = (context as? CangJieSuspendContext)?.activeThreadId
            debugSession.resume(threadId).onFailure { error ->
                session.reportError("Failed to resume: ${error.message}")
            }
        }
    }

    override fun startStepOver(context: XSuspendContext?) {
        runBlocking {
            val threadId = (context as? CangJieSuspendContext)?.activeThreadId ?: return@runBlocking
            debugSession.stepOver(threadId).onFailure { error ->
                session.reportError("Failed to step over: ${error.message}")
            }
        }
    }

    override fun startStepInto(context: XSuspendContext?) {
        runBlocking {
            val threadId = (context as? CangJieSuspendContext)?.activeThreadId ?: return@runBlocking
            debugSession.stepInto(threadId).onFailure { error ->
                session.reportError("Failed to step into: ${error.message}")
            }
        }
    }

    override fun startStepOut(context: XSuspendContext?) {
        runBlocking {
            val threadId = (context as? CangJieSuspendContext)?.activeThreadId ?: return@runBlocking
            debugSession.stepOut(threadId).onFailure { error ->
                session.reportError("Failed to step out: ${error.message}")
            }
        }
    }

    override fun stop() {
        try {
            LOG.info("Stopping debug process")

            runBlocking {
                debugSession.stop()
            }

            LOG.info("Debug process stopped")
        } catch (e: Exception) {
            LOG.error("Error stopping debug process", e)
        }
    }

    override fun runToPosition(position: XSourcePosition, context: XSuspendContext?) {
        // TODO: 实现运行到光标位置
        // 1. 设置临时断点
        // 2. 继续执行
        runBlocking {
            // 实现逻辑
        }
    }

    // 提供给其他组件使用的方法
    fun getBreakpointService(): BreakpointService = breakpointService
    fun getEvaluationService(): EvaluationService = evaluationService
    fun getVariableService(): VariableService = variableService
    fun getDebugSession(): DebugSessionService = debugSession
}

/**
 * 仓颉运行配置
 *
 * 临时数据类，实际应该从运行配置中获取
 */
data class CangJieRunConfiguration(
    val programPath: String,
    val programArguments: List<String> = emptyList(),
    val workingDirectory: String,
    val environmentVariables: Map<String, String> = emptyMap()
)