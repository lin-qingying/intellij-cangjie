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

package org.cangnova.cangjie.protodebugger.transport

import com.google.protobuf.Message
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.text.StringUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import lldbprotobuf.EventOuterClass
import lldbprotobuf.Model
import org.cangnova.cangjie.protodebugger.breakpoint.DebugPausePoint
import org.cangnova.cangjie.protodebugger.core.DebuggerHandler
import org.cangnova.cangjie.protodebugger.core.DebuggerStateManager
import org.cangnova.cangjie.protodebugger.data.createLLDBFrame
import org.cangnova.cangjie.protodebugger.data.newLLThread
import org.cangnova.cangjie.protodebugger.execution.exit.ExitStatus
import org.cangnova.cangjie.protodebugger.execution.state.TargetState
import org.cangnova.cangjie.protodebugger.memory.Address


/**
 * 广播事件处理器
 *
 * 负责接收调试器的广播事件，并将其分发到相应的处理器。
 * 职责：
 * - 监听 MessageBus 的广播消息
 * - 解析不同类型的广播事件
 * - 更新 DebuggerStateManager 的状态
 * - 调用 DebuggerHandler 接口通知 UI 层
 */
class EventBroadcastHandler(
    private val messageBus: MessageBus,
    private val stateManager: DebuggerStateManager,
    private val debuggerHandler: DebuggerHandler,
    private val scope: CoroutineScope
) {
    companion object {
        private val LOG = Logger.getInstance(EventBroadcastHandler::class.java)
    }

    fun start() {
        scope.launch {
            messageBus.broadcasts().collect { message ->
                handleBroadcast(message)
            }
        }
    }

    private fun handleBroadcast(message: Message) {
        try {
            when (message) {
                // 系统事件
                is EventOuterClass.Initialized -> handleInitialized(message)

                // 进程事件
                is EventOuterClass.ProcessStopped -> handleProcessStopped(message)
                is EventOuterClass.ProcessRunning -> handleProcessRunning(message)
                is EventOuterClass.ProcessExited -> handleProcessExited(message)
                is EventOuterClass.ProcessOutput -> handleProcessOutput(message)

                // 模块事件
                is EventOuterClass.ModuleEvent -> handleModuleEvent(message)

                // 断点事件
                is EventOuterClass.BreakpointChangedEvent -> handleBreakpointChanged(message)

                // 线程事件
                is EventOuterClass.ThreadStateChangedEvent -> handleThreadStateChanged(message)

                // 符号事件
                is EventOuterClass.SymbolsLoadedEvent -> handleSymbolsLoaded(message)

                else -> {
                    LOG.debug("Unhandled broadcast message: ${message.javaClass.simpleName}")
                }
            }
        } catch (e: Exception) {
            LOG.error("Error handling broadcast message", e)
        }
    }

    // 系统事件处理
    private fun handleInitialized(event: EventOuterClass.Initialized) {
        stateManager.updateCapabilitiesAndVersion(event.capabilities)
        stateManager.markInitialized()

    }

    //    private fun handleReadyForCommands(event: Broadcasts.ReadyForCommandsEvent) {
//        val newState = if (event.isReady) TargetState.SUSPENDED else TargetState.NOT_READY
//        stateManager.updateState(newState)
//        LOG.debug("Ready for commands: ${event.isReady}, state: $newState")
//    }
//
//    private fun handlePromptChanged(event: Broadcasts.PromptChangedEvent) {
//        LOG.debug("Prompt changed: ${event.newPrompt}")
//        debuggerHandler.handlePrompt(event.newPrompt)
//    }
//
//    private fun handleInterpreterMessage(event: Broadcasts.CommandInterpreterMessageEvent) {
//        LOG.debug("Interpreter message: ${event.message}")
//    }
//
//    private fun handleLogMessage(event: Broadcasts.LogMessageEvent) {
//        val level = event.level
//        val message = event.message
//        when (level) {
//            proto.Model.LogLevel.LOG_LEVEL_ERROR -> LOG.error(message)
//            proto.Model.LogLevel.LOG_LEVEL_INFO -> LOG.info(message)
//            proto.Model.LogLevel.LOG_LEVEL_DEBUG -> LOG.debug(message)
//            else -> LOG.trace(message)
//        }
//    }
//
//    // 进程事件处理
    private fun handleProcessStopped(event: EventOuterClass.ProcessStopped) {
        stateManager.updateState(TargetState.Paused)
        val stoppedThread = event.stoppedThread ?: return
        val thread = stoppedThread.let { newLLThread(it) }
        stateManager.updateStoppedThread(thread)
        LOG.info("Process interrupted on thread: ${thread.id}")
        val frame = createLLDBFrame(event.currentFrame)
        val debugPausePoint = DebugPausePoint(thread, frame, stoppedThread.stopInfo).also {
            stateManager.updateStopPlace(it)
        }

        val stopInfo = stoppedThread.stopInfo
        when (val stopReason = stopInfo.reason) {
            Model.StopReason.STOP_REASON_INVALID -> {
                LOG.warn("无效的停止原因")
                debuggerHandler.handleStopped(debugPausePoint)
            }

            Model.StopReason.STOP_REASON_NONE -> {
                LOG.debug("无停止原因")
                debuggerHandler.handleStopped(debugPausePoint)
            }

            Model.StopReason.STOP_REASON_TRACE -> {
                LOG.info("单步跟踪停止")
                stopInfo.stepInfo?.let { stepInfo ->
                    LOG.info("单步类型: ${stepInfo.stepType}, 范围: ${stepInfo.stepRange}")
                }
                debuggerHandler.handleStopped(debugPausePoint)
            }

            // 断点命中
            Model.StopReason.STOP_REASON_BREAKPOINT -> {
                stopInfo.breakpointInfo?.let { breakpointInfo ->
                    LOG.info(
                        "断点命中: ID=${breakpointInfo.breakpointId}, 类型=${breakpointInfo.type}, 地址=0x${
                            breakpointInfo.address.toString(
                                16
                            )
                        }"
                    )
                    LOG.info("命中次数: ${breakpointInfo.hitCount}, 条件: ${breakpointInfo.condition}")
                    // 调用专门的断点处理方法
                    debuggerHandler.handleBreakpoint(debugPausePoint)
                } ?: run {
                    LOG.warn("断点命中但缺少断点信息")
                    debuggerHandler.handleStopped(debugPausePoint)
                }
            }

            Model.StopReason.STOP_REASON_WATCHPOINT -> {
                stopInfo.watchpointInfo?.let { watchpointInfo ->
                    LOG.info("观察点命中: ID=${watchpointInfo.watchpointId}, 类型=${watchpointInfo.watchType}")
                    LOG.info("地址: 0x${watchpointInfo.address.toString(16)}, 大小: ${watchpointInfo.size}")
                    // 调用专门的观察点处理方法
                    debuggerHandler.handleBreakpoint(debugPausePoint)
                } ?: run {
                    LOG.warn("观察点命中但缺少观察点信息")
                    debuggerHandler.handleStopped(debugPausePoint)
                }
            }

            Model.StopReason.STOP_REASON_SIGNAL -> {
                stopInfo.signalInfo?.let { signalInfo ->
                    LOG.info("信号中断: ${signalInfo.signalName}(${signalInfo.signalNumber})")
                    val signalMeaning = when (signalInfo.signalNumber) {
                        2 -> "用户中断 (SIGINT)"
                        9 -> "强制终止 (SIGKILL)"
                        11 -> "内存访问错误 (SIGSEGV)"
                        15 -> "正常终止请求 (SIGTERM)"
                        else -> signalInfo.signalName ?: "未知信号"
                    }
                    LOG.info("信号含义: $signalMeaning")
                    // 调用专门的信号处理方法
                    debuggerHandler.handleSignal(debugPausePoint)
                } ?: run {
                    LOG.warn("信号中断但缺少信号信息")
                    debuggerHandler.handleStopped(debugPausePoint)
                }
            }

            Model.StopReason.STOP_REASON_EXCEPTION -> {
                stopInfo.exceptionStopInfo?.let { exceptionInfo ->
                    LOG.info("异常发生: ${exceptionInfo.exceptionName}(${exceptionInfo.exceptionCode})")
                    LOG.info("异常地址: 0x${exceptionInfo.exceptionAddress.toString(16)}")
                    LOG.info("异常类型: ${exceptionInfo.exceptionType}")
                    LOG.info("异常描述: ${exceptionInfo.message}")
                    exceptionInfo.location?.let { location ->
                        LOG.info("异常位置: ${location.filePath}:${location.line}")
                    }

                    // 创建异常处理所需的参数
                    val exceptionAddress = Address.Companion.Factory.fromLong(exceptionInfo.exceptionAddress)
                    val exceptionType = exceptionInfo.exceptionType ?: exceptionInfo.exceptionName
                    val exceptionMessage = exceptionInfo.message

                    // 调用专门的异常处理方法
                    debuggerHandler.handleException(
                        debugPausePoint,
                        exceptionType,
                        exceptionMessage,
                        exceptionAddress
                    )
                } ?: run {
                    LOG.warn("异常发生但缺少异常信息")
                    debuggerHandler.handleStopped(debugPausePoint)
                }
            }

            Model.StopReason.STOP_REASON_EXEC -> {
                LOG.info("进程执行 exec 系统调用 - 程序替换")
                debuggerHandler.handleStopped(debugPausePoint)
            }

            Model.StopReason.STOP_REASON_FORK -> {
                LOG.info("进程创建子进程 (fork)")
                debuggerHandler.handleStopped(debugPausePoint)
            }

            Model.StopReason.STOP_REASON_VFORK -> {
                LOG.info("进程执行 vfork 系统调用")
                debuggerHandler.handleStopped(debugPausePoint)
            }

            Model.StopReason.STOP_REASON_VFORK_DONE -> {
                LOG.info("vfork 操作完成，父进程恢复执行")
                debuggerHandler.handleStopped(debugPausePoint)
            }

            Model.StopReason.STOP_REASON_PLAN_COMPLETE -> {
                stopInfo.planCompleteInfo?.let { planInfo ->
                    LOG.info("执行计划完成: ${planInfo.planType}")
                    LOG.info("完成状态: ${planInfo.status}")
                    LOG.info("执行步骤数: ${planInfo.stepsExecuted}")
                    LOG.info("结果描述: ${planInfo.resultDescription}")
                }
                debuggerHandler.handleStopped(debugPausePoint)
            }

            Model.StopReason.STOP_REASON_THREAD_EXITING -> {
                stopInfo.threadExitInfo?.let { threadExitInfo ->
                    LOG.info("线程退出: 退出码=${threadExitInfo.exitCode}")
                    LOG.info("退出原因: ${threadExitInfo.exitReason}")
                    LOG.info("是否主线程: ${threadExitInfo.isMainThread}")
                }
                debuggerHandler.handleStopped(debugPausePoint)
            }

            Model.StopReason.STOP_REASON_INSTRUMENTATION -> {
                stopInfo.instrumentationInfo?.let { instrumentationInfo ->
                    LOG.info("工具化事件: 工具=${instrumentationInfo.toolName}")
                    LOG.info("事件类型: ${instrumentationInfo.eventType}")
                    LOG.info("事件ID: ${instrumentationInfo.eventId}")
                    LOG.info("事件数据: ${instrumentationInfo.eventData}")
                }
                debuggerHandler.handleStopped(debugPausePoint)
            }

            Model.StopReason.STOP_REASON_UNKNOWN -> {
                LOG.warn("未知停止原因: ${stopInfo.description}")
                debuggerHandler.handleStopped(debugPausePoint)
            }

            Model.StopReason.UNRECOGNIZED -> {
                LOG.warn("未识别的停止原因")
                debuggerHandler.handleStopped(debugPausePoint)
            }

            else -> {
                LOG.info("程序暂停: 原因=${stopReason}, 描述=${stopInfo.description}")
                debuggerHandler.handleStopped(debugPausePoint)
            }
        }


    }
//
private fun handleProcessRunning(event: EventOuterClass.ProcessRunning) {
    stateManager.updateState(TargetState.Running)
    stateManager.updateStoppedThread(null)
    LOG.debug("Process running")
    debuggerHandler.handleRunning()
}

    private fun handleProcessExited(event: EventOuterClass.ProcessExited) {
        stateManager.updateState(TargetState.Terminated)
        LOG.info("Process exited with code: ${event.exitCode}")
        val exitCode: Int = event.exitCode
        val exitDescription: String? =
            if (event.hasExitDescription()) event.getExitDescription() else null
        var exitStatus: ExitStatus = ExitStatus.normal(exitCode, exitDescription)
        if (SystemInfo.isMac && exitCode == 0 && exitDescription != null && exitDescription.startsWith("Terminated due to signal ")) {
            val signal = StringUtil.parseInt(
                exitDescription.substring("Terminated due to signal ".length).trim { it <= ' ' }, -1
            )
            if (signal > 0) {
                exitStatus = ExitStatus.fromSignal(signal)
            }
        }
        debuggerHandler.handleTargetTerminated(exitStatus)
    }

    /**
     * 处理进程输出事件
     *
     * 当被调试进程产生标准输出或标准错误时调��。
     */
    private fun handleProcessOutput(event: EventOuterClass.ProcessOutput) {
        val outputKey = when (event.outputType) {
            Model.OutputType.OutputTypeStdout -> com.intellij.execution.process.ProcessOutputTypes.STDOUT
            Model.OutputType.OutputTypeStderr -> com.intellij.execution.process.ProcessOutputTypes.STDERR
            Model.OutputType.UNRECOGNIZED -> com.intellij.execution.process.ProcessOutputTypes.SYSTEM
            else -> com.intellij.execution.process.ProcessOutputTypes.SYSTEM
        }

        LOG.debug("Process output (${event.outputType}): ${event.text}")
        debuggerHandler.handleTargetOutput(event.text, outputKey)
    }

    /**
     * 处理模块事件
     *
     * 当模块（可执行文件或共享库）被加载或卸载时调用。
     */
    private fun handleModuleEvent(event: EventOuterClass.ModuleEvent) {
        val eventType = event.eventType
        val modules = event.modulesList

        when (eventType) {
            Model.EventType.MODULE_LOADED -> {
                LOG.info("Modules loaded: ${modules.joinToString { it.name }}")
                debuggerHandler.handleModulesLoaded(modules)
            }

            Model.EventType.MODULE_UNLOADED -> {
                LOG.info("Modules unloaded: ${modules.joinToString { it.name }}")
                debuggerHandler.handleModulesUnloaded(modules)
            }

            else -> {
                LOG.warn("Unknown module event type: $eventType")
            }
        }
    }

    /**
     * 处理断点变更事件
     *
     * 当断点状态发生变化时调用。
     */
    private fun handleBreakpointChanged(event: EventOuterClass.BreakpointChangedEvent) {
        val breakpoint = event.breakpoint
        val changeType = event.changeType
        val description = if (event.hasDescription()) event.description else null

        LOG.debug("Breakpoint changed: id=${breakpoint.id.id}, type=$changeType${description?.let { ", desc=$it" } ?: ""}")
        debuggerHandler.handleBreakpointChanged(breakpoint, changeType, description)
    }

    /**
     * 处理线程状态变更事件
     *
     * 当线程被创建、销毁或状态改变时调用。
     */
    private fun handleThreadStateChanged(event: EventOuterClass.ThreadStateChangedEvent) {
        val thread = event.thread
        val changeType = event.changeType
        val description = if (event.hasDescription()) event.description else null

        LOG.debug("Thread state changed: id=${thread.threadId}, type=$changeType${description?.let { ", desc=$it" } ?: ""}")
        debuggerHandler.handleThreadStateChanged(thread, changeType, description)
    }

    /**
     * 处理符号加载事件
     *
     * 当调试符号被加载时调用。
     */
    private fun handleSymbolsLoaded(event: EventOuterClass.SymbolsLoadedEvent) {
        val module = event.module
        val symbolCount = event.symbolCount.toInt()
        val symbolFilePath = if (event.hasSymbolFilePath()) event.symbolFilePath else null

        LOG.info("Symbols loaded for module: ${module.name}, count=$symbolCount${symbolFilePath?.let { ", path=$it" } ?: ""}")
        debuggerHandler.handleSymbolsLoaded(module, symbolCount, symbolFilePath)
    }

    suspend fun waitForInitialization() {
        stateManager.waitForInitialization()
    }
}
