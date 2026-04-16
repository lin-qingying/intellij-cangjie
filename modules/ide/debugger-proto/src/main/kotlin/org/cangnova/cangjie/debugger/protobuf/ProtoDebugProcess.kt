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

package org.cangnova.cangjie.debugger.protobuf.core

import com.intellij.execution.console.ConsoleViewWrapperBase
import com.intellij.execution.console.LanguageConsoleBuilder
import com.intellij.execution.console.LanguageConsoleImpl
import com.intellij.execution.console.LanguageConsoleView
import com.intellij.execution.filters.TextConsoleBuilder
import com.intellij.execution.impl.ConsoleViewImpl
import com.intellij.execution.process.*
import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.execution.ui.ExecutionConsole
import com.intellij.execution.ui.RunnerLayoutUi
import com.intellij.execution.ui.layout.PlaceInGrid
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.util.UserDataHolderEx
import com.intellij.openapi.vfs.encoding.EncodingProjectManager
import com.intellij.terminal.ProcessHandlerTtyConnector
import com.intellij.terminal.TerminalExecutionConsole
import com.intellij.ui.content.Content
import com.intellij.util.ModalityUiUtil
import com.intellij.xdebugger.XAlternativeSourceHandler
import com.intellij.xdebugger.XDebugProcess
import com.intellij.xdebugger.XDebugSession
import com.intellij.xdebugger.XSourcePosition
import com.intellij.xdebugger.breakpoints.SuspendPolicy
import com.intellij.xdebugger.breakpoints.XBreakpoint
import com.intellij.xdebugger.breakpoints.XBreakpointHandler
import com.intellij.xdebugger.evaluation.XDebuggerEditorsProvider
import com.intellij.xdebugger.evaluation.XDebuggerEvaluator
import com.intellij.xdebugger.frame.*
import com.intellij.xdebugger.frame.presentation.XValuePresentation
import com.intellij.xdebugger.ui.XDebugTabLayouter
import com.jediterm.core.util.TermSize
import org.cangnova.cangjie.debugger.RunParameters
import org.cangnova.cangjie.debugger.protobuf.breakpoint.*
import org.cangnova.cangjie.debugger.protobuf.console.CJDB_EXECUTOR_KEY
import org.cangnova.cangjie.debugger.protobuf.console.CangJieDebugProtoLLDBLanguage
import org.cangnova.cangjie.debugger.protobuf.console.CjdbConsoleExecutor
import org.cangnova.cangjie.debugger.protobuf.execution.exit.ExitStatus
import org.cangnova.cangjie.debugger.protobuf.services.DisasmService
import org.cangnova.cangjie.debugger.protobuf.settings.ArchitectureType

import org.cangnova.cangjie.debugger.protobuf.settings.ProtoDebuggerService
import org.cangnova.cangjie.debugger.protobuf.transport.isTest
import org.jetbrains.annotations.NonNls
import java.io.OutputStream
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicReference
import javax.swing.Icon

/**
 * 仓颉语言调试进程核心类
 *
 * 负责管理整个调试会话的生命周期，协调各个子模块完成调试任务。
 * 采用组合模式将功能分散到专门的管理器中，保持职责边界清晰。
 *
 * 主要功能模块：
 * - 状态管理：跟踪调试会话的各个状态
 * - 生命周期管理：处理调试会话的启动、运行、停止
 * - 断点管理：管理各种类型的断点
 * - 控制台管理：处理输出和终端交互
 *
 * @property parameters 运行配置参数，包含可执行文件路径等信息
 * @property session 调试会话对象，提供调试API
 * @property consoleBuilder 控制台构建器，用于创建输出视图
 */
class ProtoDebugProcess(
    private val parameters: RunParameters,
    session: XDebugSession,
    consoleBuilder: TextConsoleBuilder
) : XDebugProcess(session), UserDataHolderEx {

    // ==================== 核心组件 ====================

    /** 用户数据存储器，用于存储与调试会话相关的临时数据 */
    private val userDataHolder = UserDataHolderBase()

    /** 控制台视图，展示程序输出和调试信息 */
    private val consoleView: ConsoleView = consoleBuilder.console

    /** 状态管理器，维护调试会话的状态机 */
    private val stateManager = DebugStateManager()

    /** UI管理器，管理调试相关的所有UI面板 */
    private val uiManager = DebugUIManager(this)

    /** 生命周期管理器，控制调试会话的启动和停止 */
    private val lifecycleManager = DebugLifecycleManager()

    /** 断点管理器，统一管理所有类型的断点处理器 */
    private val breakpointManager = BreakpointManager()


    /** 项目引用 */
    val project: Project get() = session.project


    /** 反汇编服务，用于查看底层汇编代码 */
    val disasmService: DisasmService get() = facade.disasmService

    // ==================== 调试器事件处理器 ====================

    /**
     * 调试器事件处理器实现
     *
     * 处理来自调试器后端的各种事件，包括：
     * - 目标程序输出
     * - 调试器诊断信息
     * - 程序终止事件
     * - 断点命中事件
     */
    val debuggerHandler = object : DebuggerHandler {

        /** 处理目标程序的标准输出和错误输出 */
        override fun handleTargetOutput(text: String, type: Key<*>) {
            consoleManager.notifyTextAvailable(text, type)
        }

        /**
         * 根据 suspendPolicy 处理断点暂停
         */
        private fun processSuspendPolicy(
            breakpoint: XBreakpoint<*>,
            evaluatedLogExpression: String?,
            suspendContext: CangJieSuspendContext
        ) {
            // 如果策略是 NONE，直接恢复执行
            if (breakpoint.suspendPolicy == SuspendPolicy.NONE) {
                executeCommand { facade.steppingService.resume() }
                return
            }

            // 其他策略（THREAD/ALL）：通知会话到达断点
            val shouldSuspend = session.breakpointReached(breakpoint, evaluatedLogExpression, suspendContext)
            if (!shouldSuspend) {
                // 条件不满足或其他原因不需要暂停
                executeCommand { facade.steppingService.resume() }
            }
            // shouldSuspend == true 时，调试器会自动暂停，无需额外处理
        }

        override fun handleBreakpoint(debugPausePoint: DebugPausePoint) {
            val breakpointInfo = debugPausePoint.getBreakpointInfo()
                ?: return handleStopped(debugPausePoint)

            // 根据断点 ID 和类型查找对应断点
            val breakpoint = breakpointManager.findBreakpointAt(
                breakpointInfo.breakpointId,
                breakpointInfo.type
            )

            val suspendContext = debugPausePoint.toCangJieSuspendContext(facade)

            if (breakpoint == null) {
                LOG.warn("Breakpoint not found for ID: ${breakpointInfo.breakpointId}, type: ${breakpointInfo.type}")
                handleStopped(debugPausePoint)
                return
            }

            val logExpr = breakpoint.logExpressionObject

            // ============================================================
            // 1. 日志表达式求值
            // ============================================================
            if (logExpr != null) {
                val activeExecutionStack = suspendContext.activeExecutionStack

                val frame = activeExecutionStack.topFrame
                val sourcePosition = frame?.sourcePosition
                val evaluator = frame?.evaluator
                executeCommand {


                    if (evaluator == null) {
                        // 求值器不可用，直接处理暂停逻辑
                        executeCommand {
                            session.reportMessage("[Log Expression Error] Evaluator not available", MessageType.ERROR)
                        }.whenComplete { _, _ ->
                            processSuspendPolicy(breakpoint, null, suspendContext)
                        }
                    } else {
                        evaluator.evaluate(
                            logExpr,
                            object : XDebuggerEvaluator.XEvaluationCallback {
                                override fun evaluated(result: XValue) {
                                    result.computePresentation(
                                        object : XValueNode {

                                            override fun setPresentation(
                                                icon: Icon?,
                                                type: @NonNls String?,
                                                value: @NonNls String,
                                                hasChildren: Boolean
                                            ) {
                                                session.reportMessage("[Log] $value", MessageType.INFO)

                                                // ✅ 在这里，日志输出完成，传递求值结果
                                                processSuspendPolicy(breakpoint, value, suspendContext)

                                            }

                                            override fun setPresentation(
                                                icon: Icon?,
                                                presentation: XValuePresentation,
                                                hasChildren: Boolean
                                            ) {

                                            }

                                            override fun setFullValueEvaluator(fullValueEvaluator: XFullValueEvaluator) {

                                            }


                                        },
                                        XValuePlace.TOOLTIP


                                    )
                                }

                                override fun errorOccurred(errorMessage: String) {
                                    session.reportMessage("[Log Expression Error] $errorMessage", MessageType.ERROR)
                                    // ✅ 求值出错，传递 null 或错误信息
                                    processSuspendPolicy(breakpoint, null, suspendContext)

                                }
                            },
                            breakpoint.sourcePosition
                        )
                    }
                }
            } else {
                // ============================================================
                // 2. 普通断点处理
                // ============================================================

                processSuspendPolicy(breakpoint, null, suspendContext)
            }


        }

        /** 处理调试器自身的诊断输出 */
        override fun handleDebuggerOutput(text: String, type: Key<*>) {
            consoleManager.printToDebugConsole(text, type)
        }

        /** 处理目标程序终止事件 */
        override fun handleTargetTerminated(exitStatus: ExitStatus) {
            consoleManager.handleTermination(exitStatus)
            session.stop()
        }


        /** 处理调试器退出事件 */
        override fun handleExited(code: Int) {
            session.stop()
        }

        /** 处理程序暂停事件（断点命中、单步等） */
        override fun handleStopped(debugPausePoint: DebugPausePoint) {
            session.positionReached(
                debugPausePoint.toCangJieSuspendContext(facade)
            )
        }

        /** 处理模块加载事件 */
        override fun handleModulesLoaded(modules: List<lldbprotobuf.Model.Module>) {
            LOG.info("Modules loaded: ${modules.size} modules")
            modules.forEach { module ->
                LOG.debug("  - ${module.name} at 0x${module.baseAddress.toString(16)}")
            }
            // 模块加载后可能需要重新解析断点
            // 这里可以触发断点管理器的更新逻辑
        }

        /** 处理模块卸载事件 */
        override fun handleModulesUnloaded(modules: List<lldbprotobuf.Model.Module>) {
            LOG.info("Modules unloaded: ${modules.size} modules")
            modules.forEach { module ->
                LOG.debug("  - ${module.name}")
            }
        }

        /** 处理断点状态变更事件 */
        override fun handleBreakpointChanged(
            breakpoint: lldbprotobuf.Model.Breakpoint,
            changeType: lldbprotobuf.Model.BreakpointEventType,
            description: String?
        ) {
            LOG.debug("Breakpoint changed: id=${breakpoint.id.id}, type=$changeType${description?.let { ", $it" } ?: ""}")

            // 根据变更类型执行相应操作
            when (changeType) {
                lldbprotobuf.Model.BreakpointEventType.BREAKPOINT_EVENT_TYPE_ADDED -> {
                    LOG.info("Breakpoint added: ${breakpoint.id.id}")
                }

                lldbprotobuf.Model.BreakpointEventType.BREAKPOINT_EVENT_TYPE_REMOVED -> {
                    LOG.info("Breakpoint removed: ${breakpoint.id.id}")
                }

                lldbprotobuf.Model.BreakpointEventType.BREAKPOINT_EVENT_TYPE_ENABLED -> {
                    LOG.info("Breakpoint enabled: ${breakpoint.id.id}")
                }

                lldbprotobuf.Model.BreakpointEventType.BREAKPOINT_EVENT_TYPE_DISABLED -> {
                    LOG.info("Breakpoint disabled: ${breakpoint.id.id}")
                }

                lldbprotobuf.Model.BreakpointEventType.BREAKPOINT_EVENT_TYPE_LOCATIONS_RESOLVED -> {
                    LOG.info("Breakpoint resolved: ${breakpoint.id.id}")
                    // 断点解析后更新 IDE 中的断点验证状态
                    updateBreakpointVerificationStatus(breakpoint.id.id, true)
                }

                lldbprotobuf.Model.BreakpointEventType.BREAKPOINT_EVENT_TYPE_LOCATIONS_ADDED -> {
                    LOG.info("Breakpoint locations added: ${breakpoint.id.id}")
                    // 新增位置时也标记为已验证
                    updateBreakpointVerificationStatus(breakpoint.id.id, true)
                }

                lldbprotobuf.Model.BreakpointEventType.BREAKPOINT_EVENT_TYPE_LOCATIONS_REMOVED -> {
                    LOG.info("Breakpoint locations removed: ${breakpoint.id.id}")
                    // 如果所有位置都被移除，可能需要标记为未验证
                    // 这里暂时不处理，因为需要检查是否还有其他位置
                }

                else -> {
                    LOG.debug("Unknown breakpoint change type: $changeType")
                }
            }
        }

        /**
         * 更新断点验证状态
         *
         * @param breakpointId 后端断点 ID
         * @param verified 是否已验证
         */
        private fun updateBreakpointVerificationStatus(breakpointId: Long, verified: Boolean) {
            // 根据断点 ID 查找对应的 IDE 断点
            val ideBreakpoint = breakpointManager.findBreakpointById(breakpointId)

            if (ideBreakpoint == null) {
                LOG.warn("Cannot update breakpoint verification status: breakpoint not found for ID $breakpointId")
                return
            }

            // 委托给对应的 Handler 更新断点验证状态
            breakpointManager.getAllHandlers().forEach { handler ->
                if (handler is BaseBreakpointHandler<*, *, *>) {
                    handler.updateVerificationStatus(breakpointId, verified)
                }
            }
        }

        /** 处理线程状态变更事件 */
        override fun handleThreadStateChanged(
            thread: lldbprotobuf.Model.Thread,
            changeType: lldbprotobuf.Model.ThreadStateChangeType,
            description: String?
        ) {
            LOG.debug("Thread state changed: id=${thread.threadId}, type=$changeType${description?.let { ", $it" } ?: ""}")

            when (changeType) {
                lldbprotobuf.Model.ThreadStateChangeType.THREAD_STATE_CHANGE_TYPE_THREAD_SUSPENDED -> {
                    LOG.debug("Thread suspended: ${thread.threadId}")
                }

                lldbprotobuf.Model.ThreadStateChangeType.THREAD_STATE_CHANGE_TYPE_THREAD_RESUMED -> {
                    LOG.debug("Thread resumed: ${thread.threadId}")
                }

                lldbprotobuf.Model.ThreadStateChangeType.THREAD_STATE_CHANGE_TYPE_THREAD_SELECTED -> {
                    LOG.debug("Thread selected: ${thread.threadId}")
                }

                lldbprotobuf.Model.ThreadStateChangeType.THREAD_STATE_CHANGE_TYPE_STACK_CHANGED -> {
                    LOG.debug("Thread stack changed: ${thread.threadId}")
                }

                lldbprotobuf.Model.ThreadStateChangeType.THREAD_STATE_CHANGE_TYPE_SELECTED_FRAME_CHANGED -> {
                    LOG.debug("Thread selected frame changed: ${thread.threadId}")
                }

                else -> {
                    LOG.debug("Unknown thread state change type: $changeType")
                }
            }
        }

        /** 处理符号加载事件 */
        override fun handleSymbolsLoaded(
            module: lldbprotobuf.Model.Module,
            symbolCount: Int,
            symbolFilePath: String?
        ) {
            val pathInfo = symbolFilePath?.let { " from $it" } ?: ""
            LOG.info("Symbols loaded for ${module.name}: $symbolCount symbols$pathInfo")

            // 符号加载后可以：
            // 1. 通知用户（可选，通常只在手动加载符号时）
            // 2. 触发断点重新解析
            // 3. 刷新调用栈视图（现在可能有更多符号信息）

            if (symbolCount > 0) {
                session.reportMessage(
                    "Loaded $symbolCount symbols for ${module.name}",
                    MessageType.INFO
                )
            }
        }
    }

    /** 调试器驱动门面，提供统一的调试器操作接口 */
    val facade: DebuggerDriverFacade = createFacade()

    /** 控制台管理器，处理输出重定向和终端模拟 */
    val consoleManager = ConsoleManager(this, consoleView)

    // ==================== 初始化方法 ====================

    /**
     * 创建调试器驱动门面
     *
     * 配置并初始化调试器驱动，设置架构类型和事件处理器
     *
     * @return 配置好的调试器驱动门面实例
     */
    private fun createFacade(): DebuggerDriverFacade {

        return DebuggerDriverFacade(
            session = session,
            debuggerHandler = debuggerHandler,

            architectureType = ArchitectureType.X86_64,
            debuggerProvider = parameters.debuggerProvider
            )
    }

    // ==================== XDebugProcess 核心方法 ====================

    /**
     * 创建额外的调试器UI标签页
     *
     * 注册调试器相关的附加UI面板，如调试控制台、内存视图等
     *
     * @return UI内容数组，每个元素代表一个标签页
     */
    override fun createTabLayouter(): XDebugTabLayouter {
        return uiManager.createTabLayouter()
    }

    /**
     * 提供编辑器支持，用于表达式求值等功能
     */
    override fun getEditorsProvider(): XDebuggerEditorsProvider {
        return CangJieDebuggerEditorsProvider()
    }

    /**
     * 注册断点处理器
     *
     * @return 所有支持的断点类型处理器数组
     */
    override fun getBreakpointHandlers(): Array<XBreakpointHandler<*>> {
        return breakpointManager.getAllHandlers()
    }


    /**
     * 获取进程处理器，用于控制台集成
     */
    override fun doGetProcessHandler(): ProcessHandler {
        return consoleManager.processHandler
    }

    /**
     * 创建控制台视图
     */
    override fun createConsole(): ConsoleView {
        return consoleManager.createConsole()
    }

    // ==================== 用户数据管理 ====================

    override fun <T : Any?> getUserData(key: Key<T?>): T? = userDataHolder.getUserData(key)

    override fun <T : Any?> putUserData(key: Key<T?>, value: T?) = userDataHolder.putUserData(key, value)

    override fun <T : Any?> putUserDataIfAbsent(key: Key<T?>, value: T & Any): T & Any =
        userDataHolder.putUserDataIfAbsent(key, value)

    override fun <T : Any?> replace(key: Key<T?>, oldValue: T?, newValue: T?): Boolean =
        userDataHolder.replace(key, oldValue, newValue)

    // ==================== 生命周期控制 ====================

    /**
     * 启动调试会话
     *
     * 加载目标程序并准备调试环境
     */
    fun start() = lifecycleManager.start()

    /**
     * 会话初始化完成回调
     *
     * 在调试环境准备好后启动目标程序
     */
    override fun sessionInitialized() = lifecycleManager.onSessionInitialized()

    /**
     * 停止调试会话
     *
     * 清理资源并终止目标程序
     */
    override fun stop() = lifecycleManager.stop()


    // ==================== 步进操作 ====================

    /**
     * 单步跳过（Step Over）
     *
     * 执行当前行，如果是函数调用则不进入函数内部
     */
    override fun startStepOver(context: XSuspendContext?) {
        if (context is CangJieSuspendContext) {
            executeCommand {
                facade.sessionService.getStoppedThread()?.let { thread ->
                    facade.steppingService.stepOver(thread)
                }
            }
        }
    }

    /**
     * 单步进入（Step Into）
     *
     * 执行当前行，如果是函数调用则进入函数内部
     */
    override fun startStepInto(context: XSuspendContext?) {
        if (context is CangJieSuspendContext) {
            executeCommand {
                facade.sessionService.getStoppedThread()?.let { thread ->
                    facade.steppingService.stepInto(thread)
                }
            }
        }
    }

    /**
     * 单步跳出（Step Out）
     *
     * 执行完当前函数并返回到调用处
     */
    override fun startStepOut(context: XSuspendContext?) {
        if (context is CangJieSuspendContext) {
            executeCommand {
                facade.sessionService.getStoppedThread()?.let { thread ->
                    facade.steppingService.stepOut(thread)
                }
            }
        }
    }

    /**
     * 暂停程序执行
     */
    override fun startPausing() {
        executeCommand {
            facade.steppingService.suspend()
        }
    }

    /**
     * 恢复程序执行
     */
    override fun resume(context: XSuspendContext?) {
        executeCommand {
            facade.steppingService.resume()
        }.handleResult("Resume")
    }

    override fun runToPosition(position: XSourcePosition, context: XSuspendContext?) {
        executeCommand {
            val address = facade.memoryViewFacade.getDisasmStore().getAddressAtSourcePosition(position)
            if (address != null) {
                facade.steppingService.runToAddress(address)
            } else {
                facade.steppingService.runToLine(position.file.path, position.line - 1)
            }

        }.handleResult("Run to Position")

    }


    override fun getAlternativeSourceHandler(): XAlternativeSourceHandler {
        return CangJieAlternativeSourceHandler(facade)
    }
    // ==================== 辅助方法 ====================

    /**
     * 在调试器上下文中异步执行命令
     *
     * @param block 要执行的挂起函数
     * @return CompletableFuture，用于跟踪执行结果
     */
    fun <T> executeCommand(block: suspend () -> T): CompletableFuture<T> {
        return facade.executeCommand { block() }
    }

    /**
     * 处理异步操作结果，记录日志
     */
    private fun <T> CompletableFuture<T>.handleResult(operationName: String) =
        whenComplete { _, error ->
            when {
                error != null -> LOG.error("$operationName failed", error)
                else -> LOG.info("$operationName completed successfully")
            }
        }

    // ==================== 内部类：状态管理器 ====================

    /**
     * 调试状态管理器
     *
     * 维护调试会话的状态机，确保状态转换的有效性。
     * 状态流转：INITIALIZED -> LOADING -> LOADED -> RUNNING <-> STOPPED -> TERMINATED
     */
    private class DebugStateManager {

        @Volatile
        private var currentState: State = State.INITIALIZED

        /**
         * 调试会话状态枚举
         */
        enum class State {
            /** 已初始化，尚未开始加载 */
            INITIALIZED,

            /** 正在加载目标程序 */
            LOADING,

            /** 目标程序已加载，尚未运行 */
            LOADED,

            /** 目标程序正在运行 */
            RUNNING,

            /** 目标程序已暂停（断点/单步） */
            STOPPED,

            /** 调试会话已终止 */
            TERMINATED
        }

        /**
         * 尝试转换到新状态
         *
         * @param newState 目标状态
         * @return 转换是否成功
         */
        fun transitionTo(newState: State): Boolean {
            val oldState = currentState
            if (!isValidTransition(oldState, newState)) {
                LOG.warn("Invalid state transition: $oldState -> $newState")
                return false
            }
            currentState = newState
            LOG.info("State transition: $oldState -> $newState")
            return true
        }

        /**
         * 获取当前状态
         */
        fun getCurrent(): State = currentState

        /**
         * 检查状态转换是否合法
         */
        private fun isValidTransition(from: State, to: State): Boolean {
            return when (from) {
                State.INITIALIZED -> to in setOf(State.LOADING, State.TERMINATED)
                State.LOADING -> to in setOf(State.LOADED, State.TERMINATED)
                State.LOADED -> to in setOf(State.RUNNING, State.TERMINATED)
                State.RUNNING -> to in setOf(State.STOPPED, State.TERMINATED)
                State.STOPPED -> to in setOf(State.RUNNING, State.TERMINATED)
                State.TERMINATED -> false // 终止状态是最终状态
            }
        }
    }

    // ==================== 内部类：生命周期管理器 ====================

    /**
     * 调试生命周期管理器
     *
     * 负责协调调试会话的启动、运行和停止流程。
     * 使用 CompletableFuture 处理异步加载和启动过程。
     */
    private inner class DebugLifecycleManager {

        /** 目标程序加载的 Future */
        private val loadFuture = AtomicReference<CompletableFuture<Unit>?>()

        /**
         * 启动调试会话
         *
         * 加载目标可执行文件并准备调试环境
         */
        fun start() {
            if (!stateManager.transitionTo(DebugStateManager.State.LOADING)) {
                return
            }

            val executable = parameters.runExecutable

            val future = executeCommand {
                facade.sessionService.loadForLaunch(
                    executable,
                    ArchitectureType.X86_64.getId()
                )
            }.thenApply {
                stateManager.transitionTo(DebugStateManager.State.LOADED)
                LOG.info("Target loaded successfully")
            }.exceptionally { error ->
                stateManager.transitionTo(DebugStateManager.State.TERMINATED)

            }

            loadFuture.set(future)
        }

        /**
         * 会话初始化完成后启动目标程序
         */
        fun onSessionInitialized() {
            loadFuture.get()?.thenCompose {
                if (stateManager.getCurrent() == DebugStateManager.State.LOADED) {
                    executeCommand {
                        facade.sessionService.startTarget()
                        stateManager.transitionTo(DebugStateManager.State.RUNNING)
                    }.thenApply {
                        session.rebuildViews()
                        LOG.info("Target started successfully")
                    }
                } else {
                    CompletableFuture.completedFuture(Unit)
                }
            }
        }

        /**
         * 停止调试会话并清理资源
         */
        fun stop() {
            try {
                // 在 UI 线程中清理断点
                ModalityUiUtil.invokeLaterIfNeeded(ModalityState.defaultModalityState()) {
                    if (!project.isDisposed) {
                        breakpointManager.cleanup()
                    }
                }

                // 终止进程
                processHandler.destroyProcess()

                // 测试模式下立即停止会话
                if (isTest) {
                    session.stop()
                }

                stateManager.transitionTo(DebugStateManager.State.TERMINATED)
            } catch (e: Exception) {
                LOG.error("Error stopping debugger", e)
            }
        }
    }

    // ==================== 内部类：断点管理器 ====================

    /**
     * 断点管理器
     *
     * 统一管理所有类型的断点处理器：
     * - 行断点：在源代码特定行设置断点
     * - 符号断点：在函数名处设置断点
     * - 地址断点：在内存地址处设置断点
     * - 监视点：监视内存位置的读写
     */
    private inner class BreakpointManager {

        private val handlers = listOf(
            LineBreakpointHandler(this@ProtoDebugProcess),
            AddressBreakpointHandler(this@ProtoDebugProcess),
        )

        /**
         * 清理所有断点
         */
        fun cleanup() {
            handlers.forEach { it.clear() }
        }

        /**
         * 获取所有断点处理器
         */
        fun getAllHandlers(): Array<XBreakpointHandler<*>> = handlers.toTypedArray()

        /**
         * 在指定位置查找断点
         *
         * @param filePosition 文件位置信息
         * @return 找到的断点信息，如果没有找到则返回 null
         */
        fun findBreakpointAt(filePosition: XSourcePosition): XBreakpoint<*>? {
            return handlers.firstNotNullOfOrNull { handler ->
                when (handler) {
                    is LineBreakpointHandler -> handler.findBreakpointAt(filePosition)
                    is AddressBreakpointHandler -> handler.findBreakpointAt(filePosition)
                    else -> null
                }
            }
        }

        /**
         * 根据断点ID和类型查找断点
         *
         * @param breakpointId 断点ID
         * @param breakpointType 断点类型
         * @return 找到的断点信息，如果没有找到则返回 null
         */
        fun findBreakpointAt(breakpointId: Long, breakpointType: lldbprotobuf.Model.BreakpointType): XBreakpoint<*>? {
            return handlers.firstNotNullOfOrNull { handler ->
                when (breakpointType) {
                    lldbprotobuf.Model.BreakpointType.BREAKPOINT_TYPE_LINE ->
                        if (handler is LineBreakpointHandler) handler.findBreakpointById(breakpointId) else null


                    lldbprotobuf.Model.BreakpointType.BREAKPOINT_TYPE_ADDRESS ->
                        if (handler is AddressBreakpointHandler) handler.findBreakpointById(breakpointId) else null


                    else -> null
                }
            }
        }

        /**
         * 根据断点ID查找断点（遍历所有处理器）
         *
         * @param breakpointId 断点ID
         * @return 找到的断点信息，如果没有找到则返回 null
         */
        fun findBreakpointById(breakpointId: Long): XBreakpoint<*>? {
            return handlers.firstNotNullOfOrNull { handler ->
                when (handler) {
                    is LineBreakpointHandler -> handler.findBreakpointById(breakpointId)
                    is AddressBreakpointHandler -> handler.findBreakpointById(breakpointId)
                    else -> null
                }
            }
        }
    }

    companion object {
        private val LOG = Logger.getInstance(ProtoDebugProcess::class.java)
    }
}

// ==================== 控制台管理器 ====================

/**
 * 控制台管理器
 *
 * 负责管理调试会话的输出和交互，支持两种模式：
 * 1. 标准控制台模式：简单的文本输出
 * 2. 终端模拟模式：完整的终端仿真，支持颜色和控制序列
 *
 * @property debuggerProcess 调试进程引用
 * @property consoleView 控制台视图
 */
class ConsoleManager(
    val debuggerProcess: ProtoDebugProcess,
    val consoleView: ConsoleView
) {

    /** 进程处理器，用于与 IntelliJ 平台集成 */
    val processHandler = ConsoleProcessHandler()

    /** 调试控制台，用于显示调试器日志信息 */
    val debugConsole: ConsoleView by lazy {
        ConsoleViewImpl(
            debuggerProcess.project,
            true
        )
    }
    val cjdbConsole: LanguageConsoleView by lazy {
        // 使用 LLDB 语言,支持命令补全
        val console = LanguageConsoleImpl(
            debuggerProcess.project,
            "Cjdb",
            CangJieDebugProtoLLDBLanguage
        ).apply {
            // 设置提示符
            prompt = "cjdb> "
        }


        // 创建执行器
        val executor = CjdbConsoleExecutor(debuggerProcess, console)

        // 将 executor 存储到 PsiFile 的 UserData 中,供补全使用
        console.file.putUserData(CJDB_EXECUTOR_KEY, executor)

        // 注册执行 Action
        LanguageConsoleBuilder.registerExecuteAction(
            console,
            { text -> executor.execute(text) },
            "CjdbConsole",           // historyType: 历史记录类型
            "CjdbConsole",           // historyPersistenceId: 历史记录持久化ID
            null                     // enabledCondition: 启用条件(null表示总是启用)
        )

        console
    }

    /**
     * 创建并配置控制台视图
     *
     * 根据设置选择标准控制台或终端模拟模式
     */
    fun createConsole(): ConsoleView {
        if (ProtoDebuggerService.getInstance().emulateTerminal) {
            setupTerminalConsole()
        } else {
            consoleView.attachToProcess(processHandler)
        }
        return consoleView
    }

    /**
     * 配置终端模拟控制台
     *
     * 提供完整的终端仿真功能，支持 PTY 和窗口大小调整
     */
    private fun setupTerminalConsole() {
        val terminal = findTerminalConsole(consoleView) ?: run {
            LOG.error("Cannot retrieve TerminalExecutionConsole")
            consoleView.attachToProcess(processHandler)
            return
        }

        val connector = createTtyConnector()
        terminal.attachToProcess(processHandler, connector, true)
    }

    /**
     * 递归查找终端控制台实例
     */
    private fun findTerminalConsole(view: ConsoleView): TerminalExecutionConsole? {
        return when (view) {
            is TerminalExecutionConsole -> view
            is ConsoleViewWrapperBase -> findTerminalConsole(view.delegate)
            else -> null
        }
    }

    /**
     * 创建 TTY 连接器，支持窗口大小调整
     */
    private fun createTtyConnector(): ProcessHandlerTtyConnector {
        return object : ProcessHandlerTtyConnector(
            processHandler,
            EncodingProjectManager.getInstance(debuggerProcess.project).defaultCharset
        ) {
            override fun resize(termSize: TermSize) {
                this@ConsoleManager.resize(termSize.columns, termSize.rows)
            }
        }
    }

    /**
     * 调整终端窗口大小
     */
    fun resize(columns: Int, rows: Int) {
        debuggerProcess.executeCommand {
            debuggerProcess.facade.resize(columns, rows)
        }
    }

    /**
     * 输出调试器诊断信息到调试控制台
     */
    fun printToDebugConsole(text: String, type: Key<*>) {
        val contentType = when {
            ProcessOutputType.isStdout(type) -> ConsoleViewContentType.NORMAL_OUTPUT
            ProcessOutputType.isStderr(type) -> ConsoleViewContentType.ERROR_OUTPUT
            else -> ConsoleViewContentType.SYSTEM_OUTPUT
        }

        ModalityUiUtil.invokeLaterIfNeeded(ModalityState.any()) {
            debugConsole.print(text, contentType)
        }
    }

    /**
     * 处理目标程序终止事件
     */
    fun handleTermination(exitStatus: ExitStatus) {
        processHandler.handleTermination(exitStatus)
    }

    /**
     * 转发目标程序输出到控制台
     */
    fun notifyTextAvailable(text: String, type: Key<*>) {
        processHandler.notifyTextAvailable(text, type)
    }

    // ==================== 内部类：进程处理器 ====================

    /**
     * 控制台进程处理器
     *
     * 实现 ProcessHandler 接口，用于与 IntelliJ 平台的控制台系统集成。
     * 处理进程生命周期、输出重定向和 ANSI 转义序列解码。
     */
    inner class ConsoleProcessHandler : ProcessHandler() {

        private val exitCodeRef = AtomicReference<Int?>()
        private val ansiDecoder = AnsiEscapeDecoder()

        init {
            // 监听进程终止事件，关闭调试器连接
            addProcessListener(object : ProcessAdapter() {
                override fun processWillTerminate(event: ProcessEvent, willBeDestroyed: Boolean) {
                    debuggerProcess.facade.close()

                }
            }, debuggerProcess.facade.facadeDisposable)
        }

        /**
         * 进程退出码
         */
        var exitCode: Int
            get() = exitCodeRef.get() ?: 0
            set(value) = exitCodeRef.set(value)

        override fun getExitCode(): Int = exitCode

        override fun destroyProcessImpl() = terminateProcess(detach = false)

        override fun detachProcessImpl() = terminateProcess(detach = true)

        override fun detachIsDefault(): Boolean = false

        override fun getProcessInput(): OutputStream? = null

        /**
         * 处理文本输出
         *
         * 在终端模拟模式下，解码 ANSI 转义序列
         */
        override fun notifyTextAvailable(text: String, outputType: Key<*>) {
            if (ProtoDebuggerService.getInstance().emulateTerminal) {
                super.notifyTextAvailable(text, outputType)
            } else {
                ansiDecoder.escapeText(text, outputType) { decodedText, decodedType ->
                    super.notifyTextAvailable(decodedText, decodedType)
                }
            }
        }

        /**
         * 处理目标程序终止
         */
        fun handleTermination(exitStatus: ExitStatus) {
            when (exitStatus) {
                is ExitStatus.Normal -> {
                    exitCode = exitStatus.code
                    exitStatus.description?.let { description ->
                        notifyTextAvailable("$description\n", ProcessOutputTypes.SYSTEM)
                    }
                }

                is ExitStatus.Signal -> {
                    exitCode = exitStatus.exitCode
                    notifyTextAvailable(
                        "进程被信号终止: ${exitStatus.signalName ?: exitStatus.signalNumber}\n",
                        ProcessOutputTypes.SYSTEM
                    )
                }

                ExitStatus.Unknown -> {
                    exitCode = -1
                }
            }
        }

        /**
         * 终止进程
         *
         * @param detach 是否分离（detach）而非杀死进程
         */
        private fun terminateProcess(detach: Boolean) {
            if (isTest) return

            ProcessIOExecutorService.INSTANCE.execute {
                if (waitForDebuggerTermination()) {
                    if (detach) {
                        notifyProcessDetached()
                    } else {
                        notifyProcessTerminated(exitCode)
                    }
                }
            }
        }

        /**
         * 等待调试器完全终止
         */
        private fun waitForDebuggerTermination(): Boolean {
            if (isTest) return true
            return debuggerProcess.facade.frontendHandler.waitFor()
        }
    }

    companion object {
        private val LOG = Logger.getInstance(ConsoleManager::class.java)


    }
}


// ==================== UI管理器 ====================

/**
 * 调试UI管理器
 *
 * 负责管理调试会话中的所有UI面板，包括：
 * 1. 调试控制台（Debug Console）：显示调试器适配器的输出
 * 2. 内存视图（Memory View）：查看和编辑内存内容
 * 3. 反汇编视图（Disassembly View）：查看汇编代码
 * 4. 寄存器视图（Registers View）：查看CPU寄存器状态
 *
 * @property debuggerProcess 调试进程引用
 */
class DebugUIManager(
    private val debuggerProcess: ProtoDebugProcess
) {

    /** 内存视图实例（延迟初始化） */
//    private var memoryView: MemoryViewConsole? = null

    /**
     * 创建调试标签页布局器
     *
     * 定义调试器窗口中各个标签页的布局和内容
     */
    fun createTabLayouter(): XDebugTabLayouter {
        return object : XDebugTabLayouter() {

            /**
             * 注册额外的调试标签页
             *
             * @param info 布局信息对象，用于注册标签页
             */
            override fun registerAdditionalContent(ui: RunnerLayoutUi) {
                registerCjdbConsoleTab(ui)
                // 注册调试控制台标签页
                registerDebugLogConsoleTab(ui)

                // 注册内存视图标签页
//                registerMemoryViewTab(ui)

                // 可以继续添加其他标签页
                // registerDisassemblyManagerTab(info)
                // registerRegistersViewTab(info)
            }

            override fun registerConsoleContent(ui: RunnerLayoutUi, console: ExecutionConsole): Content {

                val content = super.registerConsoleContent(ui, console)

                return content
            }

            /**
             * 注册调试控制台标签页
             *
             * 显示调试器适配器（LLDB）的原始输出，用于：
             * - 查看调试器命令和响应
             * - 诊断调试器问题
             * - 手动输入调试器命令（高级用户）
             */
            private fun registerDebugLogConsoleTab(ui: RunnerLayoutUi) {
                val debugConsole = debuggerProcess.consoleManager.debugConsole

                val content = ui.createContent(
                    DEBUG_LLDB_CONSOLE_LOG_ID,
                    debugConsole.component,
                    "Debug Log",
                    null, // 可以使用 AllIcons.Debugger.Console
                    null
                )
                content.isCloseable = false

                ui.addContent(content, 1, PlaceInGrid.bottom, false)

                LOG.info("Registered Debug Console tab")

            }

            private fun registerCjdbConsoleTab(ui: RunnerLayoutUi) {
                val debugConsole = debuggerProcess.consoleManager.cjdbConsole

                val content = ui.createContent(
                    DEBUG_CJDB_CONSOLE_ID,
                    debugConsole.component,
                    "Cjdb",
                    null, // 可以使用 AllIcons.Debugger.Console
                    null
                )
                content.isCloseable = false

                ui.addContent(content, 0, PlaceInGrid.center, false)

                LOG.info("Registered Cjdb Console tab")
            }


        }
    }

    /**
     * 清理资源
     */
    fun dispose() {
//        memoryView?.dispose()
//        memoryView = null
    }

    companion object {
        private val LOG = Logger.getInstance(DebugUIManager::class.java)

        /** 调试控制台内容ID */
        private const val DEBUG_LLDB_CONSOLE_LOG_ID = "CangJieLLDBLog"
        private const val DEBUG_CJDB_CONSOLE_ID = "CangJieCjdbConsole"

        /** 内存视图内容ID */
        private const val MEMORY_VIEW_CONTENT_ID = "CangJieMemoryView"

        /** 反汇编视图内容ID */
        private const val DISASSEMBLY_VIEW_CONTENT_ID = "CangJieDisassemblyManager"

        /** 寄存器视图内容ID */
        private const val REGISTERS_VIEW_CONTENT_ID = "CangJieRegistersView"
    }
}

