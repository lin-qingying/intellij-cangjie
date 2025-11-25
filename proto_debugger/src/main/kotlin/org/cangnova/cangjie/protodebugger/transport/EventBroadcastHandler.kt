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
import org.cangnova.cangjie.protodebugger.core.DebuggerStateManager
import org.cangnova.cangjie.protodebugger.core.DebuggerHandler
import org.cangnova.cangjie.protodebugger.data.newLLDBFrame
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
//                is EventOuterClass.ReadyForCommandsEvent -> handleReadyForCommands(message)
//                is EventOuterClass.PromptChangedEvent -> handlePromptChanged(message)
//                is EventOuterClass.CommandInterpreterMessageEvent -> handleInterpreterMessage(message)
//                is EventOuterClass.LogMessageEvent -> handleLogMessage(message)

                // 进程事件
                is EventOuterClass.ProcessStopped -> handleProcessStopped(message)
//                is EventOuterClass.ProcessRunningEvent -> handleProcessRunning(message)
                is EventOuterClass.ProcessExited -> handleProcessExited(message)
//                is EventOuterClass.OutputEvent -> handleProcessOutput(message)

                // 断点事件
//                is EventOuterClass.BreakpointAddedEvent -> handleBreakpointAdded(message)
//                is EventOuterClass.BreakpointRemovedEvent -> handleBreakpointRemoved(message)
//                is EventOuterClass.BreakpointChangedEvent -> handleBreakpointChanged(message)
//                is EventOuterClass.BreakpointLocationsAddedEvent -> handleBreakpointLocationsAdded(message)
//                is EventOuterClass.BreakpointLocationsRemovedEvent -> handleBreakpointLocationsRemoved(message)
//                is EventOuterClass.BreakpointLocationsResolvedEvent -> handleBreakpointLocationsResolved(message)

                // 帧选择事件
//                is EventOuterClass.SelectedFrameChangedEvent -> handleSelectedFrameChanged(message)

                // 模块事件
//                is EventOuterClass.ModulesLoadedEvent -> handleModulesLoaded(message)
//                is EventOuterClass.ModulesUnloadedEvent -> handleModulesUnloaded(message)

                // 符号下载事件
//                is EventOuterClass.SymbolsDownloadStartedEvent -> handleSymbolsDownloadStarted(message)
//                is EventOuterClass.SymbolsDownloadProgressEvent -> handleSymbolsDownloadProgress(message)
//                is EventOuterClass.SymbolsDownloadFinishedEvent -> handleSymbolsDownloadFinished(message)

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
        val frame = newLLDBFrame(event.currentFrame)
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
//    private fun handleProcessRunning(event: Broadcasts.ProcessRunningEvent) {
//        stateManager.updateState(TargetState.RUNNING)
//        stateManager.updateStoppedThread(null)
//        LOG.debug("Process running")
//        debuggerHandler.handleRunning()
//    }

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

//    private fun handleProcessOutput(event:EventOuterClass.OutputEvent ) {
//        val outputKey = when (event.outputType) {
//            proto.Model.OutputType.OUTPUT_TYPE_STDOUT -> com.intellij.execution.process.ProcessOutputTypes.STDOUT
//            proto.Model.OutputType.OUTPUT_TYPE_STDERR -> com.intellij.execution.process.ProcessOutputTypes.STDERR
//            else -> com.intellij.execution.process.ProcessOutputTypes.SYSTEM
//        }
//        debuggerHandler.handleTargetOutput(event.text, outputKey)
//    }

    // 断点事件处理
//    private fun handleBreakpointAdded(event: Broadcasts.BreakpointAddedEvent) {
//        LOG.debug("Breakpoint added: id=${event.breakpoint.id}")
//        // 从locations中获取第一个位置信息
//        val bRes = makeBreakpoint(event.breakpoint, event.locationsList)
//
//
//        debuggerHandler.handleBreakpointAdded(bRes.breakpoint)
//        debuggerHandler.handleBreakpointLocationsUpdated(bRes.breakpoint.id, bRes.breakpointLocations)
//
//    }
//
//    private fun handleBreakpointRemoved(event: Broadcasts.BreakpointRemovedEvent) {
//        LOG.debug("Breakpoint removed: id=${event.breakpointId}")
//        debuggerHandler.handleBreakpointRemoved(event.breakpointId)
//    }
//
//    private fun handleBreakpointChanged(event: Broadcasts.BreakpointChangedEvent) {
//        LOG.debug("Breakpoint changed: id=${event.breakpoint.id}, type=${event.eventType}")
//
//        val bRes = makeBreakpoint(event.breakpoint, event.locationsList)
//
//
//
//
//        debuggerHandler.handleBreakpointUpdated(bRes.breakpoint)
//        debuggerHandler.handleBreakpointLocationsReplaced(bRes.breakpoint.id, bRes.breakpointLocations)
//
//    }
//
//    private fun handleBreakpointLocationsAdded(event: Broadcasts.BreakpointLocationsAddedEvent) {
//        LOG.debug("Breakpoint locations added: id=${event.breakpointId}, count=${event.locationsList.size}")
//        val locations = event.locationsList.map { convertBreakpointLocation(it) }
//        debuggerHandler.handleBreakpointLocationsUpdated(event.breakpointId, locations)
//    }
//
//    private fun handleBreakpointLocationsRemoved(event: Broadcasts.BreakpointLocationsRemovedEvent) {
//        LOG.debug("Breakpoint locations removed: id=${event.breakpointId}, count=${event.locationIdsList.size}")
//        debuggerHandler.handleBreakpointLocationsRemoved(event.breakpointId, event.locationIdsList.map { it.toString() })
//    }
//
//    private fun handleBreakpointLocationsResolved(event: Broadcasts.BreakpointLocationsResolvedEvent) {
//        LOG.debug("Breakpoint locations resolved: id=${event.breakpointId}, count=${event.locationsList.size}")
//        val locations = event.locationsList.map { convertBreakpointLocation(it) }
//        debuggerHandler.handleBreakpointLocationsReplaced(event.breakpointId, locations)
//    }
//
//    // 帧选择事件处理
//    private fun handleSelectedFrameChanged(event: Broadcasts.SelectedFrameChangedEvent) {
//        LOG.debug("Selected frame changed: thread=${event.thread?.index}, frame=${event.frame?.index}")
//        event.thread?.let { thread ->
//            event.frame?.let { frame ->
//                debuggerHandler.handleSelectedFrameChanged(newLLThread(thread), newLLDBFrame(frame))
//            }
//        }
//    }
//
//    // 模块事件处理
//    private fun handleModulesLoaded(event: Broadcasts.ModulesLoadedEvent) {
//        LOG.debug("Modules loaded: ${event.moduleNamesList.joinToString()}")
//        // Handler接口期望LLModule列表，但proto只提供名称
//        // 暂时不调用handler方法
//    }
//
//    private fun handleModulesUnloaded(event: Broadcasts.ModulesUnloadedEvent) {
//        LOG.debug("Modules unloaded: ${event.moduleNamesList.joinToString()}")
//        // Handler接口期望LLModule列表，但proto只提供名称
//        // 暂时不调用handler方法
//    }
//
//    // 符号下载事件处理
//    private fun handleSymbolsDownloadStarted(event: Broadcasts.SymbolsDownloadStartedEvent) {
//        LOG.debug("Symbols download started: ${event.title}")
//        debuggerHandler.handleSymbolsDownloadStarted(event.title, event.details)
//    }
//
//    private fun handleSymbolsDownloadProgress(event: Broadcasts.SymbolsDownloadProgressEvent) {
//        LOG.debug("Symbols download progress: ${event.progressPercent}%")
//        debuggerHandler.handleSymbolsDownloadProgress(event.progressPercent.toInt())
//    }
//
//    private fun handleSymbolsDownloadFinished(event: Broadcasts.SymbolsDownloadFinishedEvent) {
//        LOG.debug("Symbols download finished")
//        debuggerHandler.handleSymbolsDownloadFinished()
//    }

    suspend fun waitForInitialization() {
        stateManager.waitForInitialization()
    }
}
