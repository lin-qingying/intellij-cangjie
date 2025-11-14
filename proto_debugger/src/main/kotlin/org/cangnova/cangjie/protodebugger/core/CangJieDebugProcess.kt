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
 */

package org.cangnova.cangjie.protodebugger.core

import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.console.ConsoleViewWrapperBase
import com.intellij.execution.filters.TextConsoleBuilder
import com.intellij.execution.process.AnsiEscapeDecoder
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessIOExecutorService
import com.intellij.execution.process.ProcessOutputTypes
import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.util.UserDataHolderEx
import com.intellij.openapi.vfs.encoding.EncodingProjectManager
import com.intellij.terminal.ProcessHandlerTtyConnector
import com.intellij.terminal.TerminalExecutionConsole
import com.intellij.xdebugger.XDebugProcess
import com.intellij.xdebugger.XDebugSession
import com.intellij.xdebugger.XSourcePosition
import com.intellij.xdebugger.breakpoints.XBreakpointHandler
import com.intellij.xdebugger.evaluation.EvaluationMode
import com.intellij.xdebugger.evaluation.XDebuggerEditorsProvider
import com.intellij.xdebugger.frame.XSuspendContext
import com.jediterm.core.util.TermSize
import kotlinx.coroutines.*
import kotlinx.coroutines.future.asCompletableFuture
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.protodebugger.breakpoint.AddressBreakpointHandler
import org.cangnova.cangjie.protodebugger.breakpoint.CangJieDebuggerLineBreakpointHandler
import org.cangnova.cangjie.protodebugger.breakpoint.SymbolicBreakpointHandler
import org.cangnova.cangjie.protodebugger.breakpoint.WatchpointBreakpointHandler
import org.cangnova.cangjie.protodebugger.data.LLThread
import org.cangnova.cangjie.protodebugger.execution.ExitStatus
import org.cangnova.cangjie.protodebugger.services.DisasmService
import org.cangnova.cangjie.protodebugger.settings.ArchitectureType
import org.cangnova.cangjie.protodebugger.util.InstallerImpl
import org.jetbrains.concurrency.Promise
import java.io.OutputStream
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicReference
import kotlin.code

/**
 * 仓颉语言调试进程类
 *
 * 负责管理仓颉语言的调试会话，包括与调试器的通信、执行控制、断点管理等功能。
 * 实现了 IntelliJ 平台的 XDebugProcess 接口，以集成到 IntelliJ 的调试框架中。
 *
 * @property parameters 运行参数，包含启动调试会话所需的配置信息
 * @property emulateTerminal 是否模拟终端环境
 */
class CangJieDebugProcess(
    val parameters: RunParameters,
    session: XDebugSession,
    consoleBuilder: TextConsoleBuilder,
    val emulateTerminal: Boolean = false
) : XDebugProcess(session), Handler, UserDataHolderEx, Disposable {

    // ==================== 核心组件 ====================

    private val consoleView: ConsoleView = consoleBuilder.console
    private val configuration = DebuggerDriverConfiguration()

    val debuggerDriver: DebuggerDriverFacade = DebuggerDriverFacade(
        handler = this,
        configuration = configuration,
        architectureType = ArchitectureType.X86_64,
        this
    )
    private val userDataHolder: UserDataHolderBase = UserDataHolderBase()
    private val editorsProvider: XDebuggerEditorsProvider = createEditorsProvider(session.runProfile)

    // 独立的断点处理器实例
    private val lineBreakpointHandler = CangJieDebuggerLineBreakpointHandler(this)
    private val symbolicBreakpointHandler = SymbolicBreakpointHandler(this)
    private val addressBreakpointHandler = AddressBreakpointHandler(this)
    private val watchpointBreakpointHandler = WatchpointBreakpointHandler(this)


    @Volatile
    private var state: DebugState = DebugState.INITIALIZED

    // 保存 loadForLaunch 的 Future，用于在 sessionInitialized 中等待
    @Volatile
    private var loadFuture: java.util.concurrent.CompletableFuture<Unit>? = null

    // ==================== 属性访问器 ====================

    val project: Project
        get() = session.project

    // RequestExecutor 已移除，直接使用 debuggerDriver 的服务接口

    val disasmService: DisasmService
        get() = debuggerDriver.disasmService

    // ==================== 调试状态 ====================

    enum class DebugState {
        INITIALIZED,
        STARTING,
        STARTED,
        FINISHED
    }

    // ==================== 编辑器提供者 ====================

    private fun createEditorsProvider(profile: RunProfile?): XDebuggerEditorsProvider {
        return object : XDebuggerEditorsProvider() {
            override fun createDocument(
                project: Project,
                expression: com.intellij.xdebugger.XExpression,
                sourcePosition: XSourcePosition?,
                mode: EvaluationMode
            ): Document {
                return EditorFactory.getInstance().createDocument(expression.expression)
            }

            override fun getFileType(): FileType = CangJieFileType.INSTANCE
        }
    }

    override fun getEditorsProvider(): XDebuggerEditorsProvider = editorsProvider

    /**
     * 获取所有断点处理器
     */
    override fun getBreakpointHandlers(): Array<XBreakpointHandler<*>> {
        return arrayOf(
            lineBreakpointHandler,
            symbolicBreakpointHandler,
            addressBreakpointHandler,
            watchpointBreakpointHandler
        )
    }

    // ==================== 用户数据管理 ====================

    override fun <T : Any?> putUserDataIfAbsent(key: Key<T?>, value: T & Any): T & Any {
        return userDataHolder.putUserDataIfAbsent(key, value)
    }

    override fun <T : Any?> replace(key: Key<T?>, oldValue: T?, newValue: T?): Boolean {
        return userDataHolder.replace(key, oldValue, newValue)
    }

    override fun <T : Any?> getUserData(key: Key<T?>): T? {
        return userDataHolder.getUserData(key)
    }

    override fun <T : Any?> putUserData(key: Key<T?>, value: T?) {
        userDataHolder.putUserData(key, value)
    }


    // ==================== 生命周期管理 ====================

    /**
     * 启动调试进程
     */
    fun start() {
        try {
            state = DebugState.STARTING

            // executeCommand 会自动等待初始化完成
            parameters.runExecutable?.let { executable ->
                loadFuture = executeCommand {
                    debuggerDriver.sessionService.loadForLaunch(
                        InstallerImpl(executable),
                        ArchitectureType.X86_64.getId()
                    )
                    state = DebugState.STARTED
                    logInfo("Target loaded successfully")
                }.whenComplete { _, error ->
                    if (error != null) {
                        logError("Failed to load target: ${error.message}")
                        state = DebugState.FINISHED
                    }
                }
            }

        } catch (e: Exception) {
            state = DebugState.FINISHED
            logError("Failed to start debug process: ${e.message}")
            throw e
        }
    }

    override fun sessionInitialized() {
        // sessionInitialized 在 IDE 准备好后调用
        // 此时断点已注册，可以发送启动命令
        // 等待 loadForLaunch 完成后再发送启动命令
        val future = loadFuture
        if (future != null) {
            future.thenCompose { _ ->
                executeCommand {
                    if (state == DebugState.STARTED) {
                        // 发送启动命令
                        debuggerDriver.sessionService.startTarget()
                        this.session.rebuildViews()
                        logInfo("Target started successfully")
                    }
                }
            }.whenComplete { _, error ->
                if (error != null) {
                    logError("Failed to start target: ${error.message}")
                }
            }
        } else {
            // 如果没有 loadFuture，说明没有可执行文件，直接返回
            logInfo("No executable to start")
        }
    }

    /**
     * 停止调试
     */
    override fun stop() {
        try {
            debuggerDriver.close()
            state = DebugState.FINISHED
        } catch (e: Exception) {
            logError("Error stopping debugger: ${e.message}")
        }
    }

    // ==================== 暂停上下文 ====================

    /**
     * 构建暂停上下文
     */
    private fun buildSuspendContext(): XSuspendContext? {
        return debuggerDriver.getStoppedThread()?.let { thread ->
            CangJieSuspendContext(thread, debuggerDriver)
        }
    }

    // ==================== 步进操作 ====================

    /**
     * 单步跳过
     */
    override fun startStepOver(context: XSuspendContext?) {
        executeStepCommand(context, "step over") { thread ->
            debuggerDriver.steppingService.stepOver(thread, false)
        }
    }

    /**
     * 单步进入
     */
    override fun startStepInto(context: XSuspendContext?) {
        executeStepCommand(context, "step into") { thread ->
            debuggerDriver.steppingService.stepInto(thread, false, false)
        }
    }

    /**
     * 单步跳出
     */
    override fun startStepOut(context: XSuspendContext?) {
        executeStepCommand(context, "step out") { thread ->
            debuggerDriver.steppingService.stepOut(thread, false)
        }
    }

    /**
     * 继续执行
     */
    override fun resume(context: XSuspendContext?) {
        executeCommand {
            debuggerDriver.steppingService.resume()
        }.whenComplete { _, error ->
            if (error != null) {
                logError("Resume failed: ${error.message}")
            } else {
                logInfo("Resume executed")
            }
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 执行步进命令的通用方法
     */
    private fun executeStepCommand(
        context: XSuspendContext?,
        operationName: String,
        stepAction: suspend (LLThread) -> Unit
    ) {
        val thread = (context as? CangJieSuspendContext)?.thread
            ?: debuggerDriver.getStoppedThread()

        if (thread == null) {
            logError("No active thread for $operationName")
            return
        }

        executeCommand {
            stepAction(thread)
        }.whenComplete { _, error ->
            if (error != null) {
                logError("$operationName failed: ${error.message}")
            } else {
                logInfo("$operationName executed")
            }
        }
    }

    fun <T> executeCommand(
        block: suspend () -> T
    ): CompletableFuture<T> {

        return debuggerDriver.executeCommand {
            block()
        }
    }

    private val processHandler: ExeProcessHandler =
        ExeProcessHandler()


    private inner class ExeProcessHandler : ProcessHandler() {
        private val myExitCode = AtomicReference<Int>()
        private val myAnsiEscapeDecoder = AnsiEscapeDecoder()

        fun setExitCode(exitCode: Int) {
            myExitCode.set(exitCode)
        }

        override fun getExitCode(): Int {
            return myExitCode.get() ?: 0
        }

        override fun destroyProcessImpl() {
            doDestroyOrDetach(false)
        }

        override fun detachProcessImpl() {
            doDestroyOrDetach(true)
        }

        protected fun waitForTermination(): Boolean {
            return debuggerDriver.frontendHandler.waitFor()
        }

        private fun doDestroyOrDetach(detach: Boolean) {
            ProcessIOExecutorService.INSTANCE.execute {
                if (waitForTermination()) {
                    if (detach) {
                        notifyProcessDetached()
                    } else {
                        notifyProcessTerminated(exitCode)
                    }
                }
            }
        }

        fun isDetachDefault(): Boolean {
            return false
        }

        override fun detachIsDefault(): Boolean {
            return isDetachDefault()
        }

        override fun getProcessInput(): OutputStream? {
            return null
        }

        override fun notifyTextAvailable(text: String, outputType: Key<*>) {
            if (emulateTerminal) {
                super.notifyTextAvailable(text, outputType)
            } else {
                myAnsiEscapeDecoder.escapeText(text, outputType) { x0, x1 ->
                    super.notifyTextAvailable(x0, x1)
                }
            }
        }
    }

    /**
     * 输出错误日志
     */
    private fun logError(message: String) {
        consoleView.print("$message\n", ConsoleViewContentType.ERROR_OUTPUT)
    }

    fun resize(columns: Int, rows: Int) {
        executeCommand { debuggerDriver.resize(columns, rows) }
    }

    override fun createConsole(): ConsoleView {
        if (emulateTerminal) {


            val terminalExecutionConsole = getTerminalExecutionConsole(consoleView)
            terminalExecutionConsole?.attachToProcess(processHandler, object : ProcessHandlerTtyConnector(
                processHandler,
                EncodingProjectManager.getInstance(project).defaultCharset
            ) {
                override fun resize(termSize: TermSize) {
                    this@CangJieDebugProcess.resize(termSize.columns, termSize.rows)
                }
            }, true)
        } else {
            consoleView.attachToProcess(processHandler)
        }

        return consoleView
    }

    private fun getTerminalExecutionConsole(consoleView: ConsoleView): TerminalExecutionConsole? = when (consoleView) {
        is TerminalExecutionConsole -> consoleView
        is ConsoleViewWrapperBase -> getTerminalExecutionConsole(consoleView.delegate)
        else -> {
            LOG.error("Cannot retrieve TerminalExecutionConsole from ConsoleView")
            null
        }
    }

    /**
     * 输出普通日志
     */
    private fun logInfo(message: String) {
        consoleView.print("$message\n", ConsoleViewContentType.NORMAL_OUTPUT)
    }

    override fun dispose() {

    }

    override fun handleTargetTerminated(exitStatus: ExitStatus) {
        if (exitStatus !== ExitStatus.UNKNOWN) {
            this.processHandler.exitCode = exitStatus.code
            if (exitStatus.description != null) {
                this.processHandler.notifyTextAvailable(exitStatus.description + "\n", ProcessOutputTypes.SYSTEM)
            }
        } else {
            this.processHandler.exitCode = -1
        }
        this.session.stop()
    }

    override fun handleExited(code: Int) {

        this.session.stop()
    }

    companion object {
        val LOG = Logger.getInstance(CangJieDebugProcess::class.java)
    }
}