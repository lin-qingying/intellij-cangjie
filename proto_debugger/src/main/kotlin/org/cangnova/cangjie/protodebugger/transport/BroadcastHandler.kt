package org.cangnova.cangjie.protodebugger.transport

import com.google.protobuf.Message
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.text.StringUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import org.cangnova.cangjie.protodebugger.core.DebuggerStateManager
import org.cangnova.cangjie.protodebugger.core.Handler
import org.cangnova.cangjie.protodebugger.data.LLBreakpoint
import org.cangnova.cangjie.protodebugger.data.LLBreakpointLocation
import org.cangnova.cangjie.protodebugger.data.LLFrame
import org.cangnova.cangjie.protodebugger.data.LLThread
import org.cangnova.cangjie.protodebugger.data.convertBreakpointLocation
import org.cangnova.cangjie.protodebugger.data.makeBreakpoint
import org.cangnova.cangjie.protodebugger.data.newLLFrame
import org.cangnova.cangjie.protodebugger.data.newLLThread
import org.cangnova.cangjie.protodebugger.execution.ExitStatus
import org.cangnova.cangjie.protodebugger.execution.TargetState
import org.cangnova.cangjie.protodebugger.util.DebuggerSourceFileHash
import proto.Broadcasts
import proto.Model

/**
 * 广播事件处理器
 *
 * 负责接收调试器的广播事件，并将其分发到相应的处理器。
 * 职责：
 * - 监听 MessageBus 的广播消息
 * - 解析不同类型的广播事件
 * - 更新 DebuggerStateManager 的状态
 * - 调用 Handler 接口通知 UI 层
 */
class BroadcastHandler(
    private val messageBus: MessageBus,
    private val stateManager: DebuggerStateManager,
    private val handler: Handler,
    private val scope: CoroutineScope
) {
    companion object {
        private val LOG = Logger.getInstance(BroadcastHandler::class.java)
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
                is Broadcasts.InitializedEvent -> handleInitialized(message)
                is Broadcasts.ReadyForCommandsEvent -> handleReadyForCommands(message)
                is Broadcasts.PromptChangedEvent -> handlePromptChanged(message)
                is Broadcasts.CommandInterpreterMessageEvent -> handleInterpreterMessage(message)
                is Broadcasts.LogMessageEvent -> handleLogMessage(message)

                // 进程事件
                is Broadcasts.ProcessInterruptedEvent -> handleProcessInterrupted(message)
                is Broadcasts.ProcessRunningEvent -> handleProcessRunning(message)
                is Broadcasts.ProcessExitedEvent -> handleProcessExited(message)
                is Broadcasts.ProcessOutputEvent -> handleProcessOutput(message)

                // 断点事件
                is Broadcasts.BreakpointAddedEvent -> handleBreakpointAdded(message)
                is Broadcasts.BreakpointRemovedEvent -> handleBreakpointRemoved(message)
                is Broadcasts.BreakpointChangedEvent -> handleBreakpointChanged(message)
                is Broadcasts.BreakpointLocationsAddedEvent -> handleBreakpointLocationsAdded(message)
                is Broadcasts.BreakpointLocationsRemovedEvent -> handleBreakpointLocationsRemoved(message)
                is Broadcasts.BreakpointLocationsResolvedEvent -> handleBreakpointLocationsResolved(message)

                // 帧选择事件
                is Broadcasts.SelectedFrameChangedEvent -> handleSelectedFrameChanged(message)

                // 模块事件
                is Broadcasts.ModulesLoadedEvent -> handleModulesLoaded(message)
                is Broadcasts.ModulesUnloadedEvent -> handleModulesUnloaded(message)

                // 符号下载事件
                is Broadcasts.SymbolsDownloadStartedEvent -> handleSymbolsDownloadStarted(message)
                is Broadcasts.SymbolsDownloadProgressEvent -> handleSymbolsDownloadProgress(message)
                is Broadcasts.SymbolsDownloadFinishedEvent -> handleSymbolsDownloadFinished(message)

                else -> {
                    LOG.debug("Unhandled broadcast message: ${message.javaClass.simpleName}")
                }
            }
        } catch (e: Exception) {
            LOG.error("Error handling broadcast message", e)
        }
    }

    // 系统事件处理
    private fun handleInitialized(event: Broadcasts.InitializedEvent) {
        stateManager.updateCapabilitiesAndVersion(event.capabilities, event.version)
        stateManager.markInitialized()
        LOG.info("Debugger initialized: version=${event.version}, capabilities=${event.capabilities}")
    }

    private fun handleReadyForCommands(event: Broadcasts.ReadyForCommandsEvent) {
        val newState = if (event.isReady) TargetState.SUSPENDED else TargetState.NOT_READY
        stateManager.updateState(newState)
        LOG.debug("Ready for commands: ${event.isReady}, state: $newState")
    }

    private fun handlePromptChanged(event: Broadcasts.PromptChangedEvent) {
        LOG.debug("Prompt changed: ${event.newPrompt}")
        handler.handlePrompt(event.newPrompt)
    }

    private fun handleInterpreterMessage(event: Broadcasts.CommandInterpreterMessageEvent) {
        LOG.debug("Interpreter message: ${event.message}")
    }

    private fun handleLogMessage(event: Broadcasts.LogMessageEvent) {
        val level = event.level
        val message = event.message
        when (level) {
            proto.Model.LogLevel.LOG_LEVEL_ERROR -> LOG.error(message)
            proto.Model.LogLevel.LOG_LEVEL_INFO -> LOG.info(message)
            proto.Model.LogLevel.LOG_LEVEL_DEBUG -> LOG.debug(message)
            else -> LOG.trace(message)
        }
    }

    // 进程事件处理
    private fun handleProcessInterrupted(event: Broadcasts.ProcessInterruptedEvent) {
        stateManager.updateState(TargetState.SUSPENDED)
        val thread = event.interruptedThread?.let { newLLThread(it) }
        stateManager.updateStoppedThread(thread)
        LOG.info("Process interrupted on thread: ${thread?.id}")

        thread?.let { t ->
            event.currentFrame?.let { frame ->
                val llFrame = newLLFrame(frame)
                handler.handleInterrupted(org.cangnova.cangjie.protodebugger.breakpoint.StopPlace(t, llFrame))
            }
        }
    }

    private fun handleProcessRunning(event: Broadcasts.ProcessRunningEvent) {
        stateManager.updateState(TargetState.RUNNING)
        stateManager.updateStoppedThread(null)
        LOG.debug("Process running")
        handler.handleRunning()
    }

    private fun handleProcessExited(event: Broadcasts.ProcessExitedEvent) {
        stateManager.updateState(TargetState.FINISHED)
        LOG.info("Process exited with code: ${event.exitCode}")
        val exitCode: Int = event.exitCode
        val exitDescription: String? =
            if (event.hasExitDescription()) event.getExitDescription() else null
        var exitStatus = ExitStatus(exitCode, exitDescription)
        if (SystemInfo.isMac && exitCode == 0 && exitDescription != null && exitDescription.startsWith("Terminated due to signal ")) {
            val signal = StringUtil.parseInt(
                exitDescription.substring("Terminated due to signal ".length).trim { it <= ' ' }, -1
            )
            if (signal > 0) {
                exitStatus = ExitStatus.fromSignal(signal)
            }
        }
        handler.handleTargetTerminated(exitStatus)
    }

    private fun handleProcessOutput(event: Broadcasts.ProcessOutputEvent) {
        val outputKey = when (event.outputType) {
            proto.Model.OutputType.OUTPUT_TYPE_STDOUT -> com.intellij.execution.process.ProcessOutputTypes.STDOUT
            proto.Model.OutputType.OUTPUT_TYPE_STDERR -> com.intellij.execution.process.ProcessOutputTypes.STDERR
            else -> com.intellij.execution.process.ProcessOutputTypes.SYSTEM
        }
        handler.handleTargetOutput(event.text, outputKey)
    }

    // 断点事件处理
    private fun handleBreakpointAdded(event: Broadcasts.BreakpointAddedEvent) {
        LOG.debug("Breakpoint added: id=${event.breakpoint.id}")
        // 从locations中获取第一个位置信息
        val bRes = makeBreakpoint(event.breakpoint, event.locationsList)


        handler.handleBreakpointAdded(bRes.breakpoint)
        handler.handleBreakpointLocationsUpdated(bRes.breakpoint.id, bRes.breakpointLocations)

    }

    private fun handleBreakpointRemoved(event: Broadcasts.BreakpointRemovedEvent) {
        LOG.debug("Breakpoint removed: id=${event.breakpointId}")
        handler.handleBreakpointRemoved(event.breakpointId)
    }

    private fun handleBreakpointChanged(event: Broadcasts.BreakpointChangedEvent) {
        LOG.debug("Breakpoint changed: id=${event.breakpoint.id}, type=${event.eventType}")

        val bRes = makeBreakpoint(event.breakpoint, event.locationsList)




        handler.handleBreakpointUpdated(bRes.breakpoint)
        handler.handleBreakpointLocationsReplaced(bRes.breakpoint.id, bRes.breakpointLocations)

    }

    private fun handleBreakpointLocationsAdded(event: Broadcasts.BreakpointLocationsAddedEvent) {
        LOG.debug("Breakpoint locations added: id=${event.breakpointId}, count=${event.locationsList.size}")
        val locations = event.locationsList.map { convertBreakpointLocation(it) }
        handler.handleBreakpointLocationsUpdated(event.breakpointId, locations)
    }

    private fun handleBreakpointLocationsRemoved(event: Broadcasts.BreakpointLocationsRemovedEvent) {
        LOG.debug("Breakpoint locations removed: id=${event.breakpointId}, count=${event.locationIdsList.size}")
        handler.handleBreakpointLocationsRemoved(event.breakpointId, event.locationIdsList.map { it.toString() })
    }

    private fun handleBreakpointLocationsResolved(event: Broadcasts.BreakpointLocationsResolvedEvent) {
        LOG.debug("Breakpoint locations resolved: id=${event.breakpointId}, count=${event.locationsList.size}")
        val locations = event.locationsList.map { convertBreakpointLocation(it) }
        handler.handleBreakpointLocationsReplaced(event.breakpointId, locations)
    }

    // 帧选择事件处理
    private fun handleSelectedFrameChanged(event: Broadcasts.SelectedFrameChangedEvent) {
        LOG.debug("Selected frame changed: thread=${event.thread?.index}, frame=${event.frame?.index}")
        event.thread?.let { thread ->
            event.frame?.let { frame ->
                handler.handleSelectedFrameChanged(newLLThread(thread), newLLFrame(frame))
            }
        }
    }

    // 模块事件处理
    private fun handleModulesLoaded(event: Broadcasts.ModulesLoadedEvent) {
        LOG.debug("Modules loaded: ${event.moduleNamesList.joinToString()}")
        // Handler接口期望LLModule列表，但proto只提供名称
        // 暂时不调用handler方法
    }

    private fun handleModulesUnloaded(event: Broadcasts.ModulesUnloadedEvent) {
        LOG.debug("Modules unloaded: ${event.moduleNamesList.joinToString()}")
        // Handler接口期望LLModule列表，但proto只提供名称
        // 暂时不调用handler方法
    }

    // 符号下载事件处理
    private fun handleSymbolsDownloadStarted(event: Broadcasts.SymbolsDownloadStartedEvent) {
        LOG.debug("Symbols download started: ${event.title}")
        handler.handleSymbolsDownloadStarted(event.title, event.details)
    }

    private fun handleSymbolsDownloadProgress(event: Broadcasts.SymbolsDownloadProgressEvent) {
        LOG.debug("Symbols download progress: ${event.progressPercent}%")
        handler.handleSymbolsDownloadProgress(event.progressPercent.toInt())
    }

    private fun handleSymbolsDownloadFinished(event: Broadcasts.SymbolsDownloadFinishedEvent) {
        LOG.debug("Symbols download finished")
        handler.handleSymbolsDownloadFinished()
    }

    suspend fun waitForInitialization() {
        stateManager.waitForInitialization()
    }
}
