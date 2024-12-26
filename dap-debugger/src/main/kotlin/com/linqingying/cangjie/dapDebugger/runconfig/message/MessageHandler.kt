package com.linqingying.cangjie.dapDebugger.runconfig.message


import com.intellij.notification.NotificationType
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.Key
import com.intellij.util.concurrency.QueueProcessor
import com.linqingying.cangjie.dapDebugger.protocol.event.*
import com.linqingying.cangjie.dapDebugger.protocol.ProtocolMessage
import com.linqingying.cangjie.dapDebugger.protocol.request.Request
import com.linqingying.cangjie.dapDebugger.protocol.request.RunInTerminalRequest
import com.linqingying.cangjie.dapDebugger.protocol.response.*

import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Proxy
import java.util.*


interface MessageHandler : EventListener {


    fun handleMessage(message: ProtocolMessage) {
        when (message) {
            is Event -> {
                handleMessageByEvent(message)
            }

            is Response -> {
                handleMessageByResponse(message)
            }

            is Request -> {
                handleMessageByRequest(message)
            }

            else -> {
                LOG.info("空消息")
            }

        }


    }

    fun handleMessageByRequest(message: Request) {
        when (message) {
            is RunInTerminalRequest -> {
                handleRunInTerminalRequest(message)
            }
        }
    }

    fun handleRunInTerminalRequest(message: RunInTerminalRequest) {

    }

    fun handleMessageByEvent(message: Event) {
        when (message) {
            is InitializedEvent -> {
                handleInitializedEvent(message)
            }

            is ThreadEvent -> {
                handleThreadEvent(message)
            }

            is StoppedEvent -> {
                handleStoppedEvent(message)
            }

            is OutputEvent -> {
                handleOutputEvent(message)
            }
            is ModuleEvent -> {
                handleModuleEvent(message)
            }

            is BreakpointEvent -> {
                handleBreakpointEvent(message)
            }

            is LoadedSourceEvent -> {
                handleLoadedSourceEvent(message)
            }

            is ExitedEvent -> {
                handleExitedEvent(message)
            }

            is TerminatedEvent -> {
                handleTerminatedEvent(message)
            }
        }
    }

    fun handleBreakpointEvent(message: BreakpointEvent) {

    }

    fun handleLoadedSourceEvent(message: LoadedSourceEvent) {

    }
    fun handleModuleEvent(message: ModuleEvent) {

    }
    fun handleOutputEvent(message: OutputEvent) {

    }

    fun handleThreadEvent(message: ThreadEvent) {

    }

    fun handleStoppedEvent(message: StoppedEvent) {

    }

    fun handleInitializedEvent(message: InitializedEvent) {

    }

    fun handleMessageByResponse(message: Response) {
        when (message) {
            is InitializeResponse -> {
                handleInitializeResponse(message)
            }
            is ModulesResponse -> {
                handleModulesResponse(message)
            }
            is ErrorResponse -> {
                handleErrorResponse(message)
            }

            is CancelResponse -> {
                handleCancelResponse(message)
            }

            is LaunchResponse -> {
                handleLaunchResponse(message)
            }

            is SetBreakpointsResponse -> {
                handleSetBreakpointsResponse(message)
            }

            is ConfigurationDoneResponse -> {
                handleConfigurationDoneResponse(message)
            }

            is DebugInConsoleResponse -> {
                handleDebugInConsoleResponse(message)
            }

            is ThreadsResponse -> {
                handleThreadsResponse(message)
            }

            is StackTraceResponse -> {
                handleStackTraceResponse(message)
            }

            is ScopesResponse -> {
                handleScopesResponse(message)
            }

            is VariablesResponse -> {
                handleVariablesResponse(message)
            }

            is EvaluateResponse -> {
                handleEvaluateResponse(message)
            }

            is SetVariableResponse -> {
                handleSetVariableResponse(message)
            }
        }
    }

    fun handleSetVariableResponse(variableResponse: SetVariableResponse) {

    }

    fun handleEvaluateResponse(evaluateResponse: EvaluateResponse) {


    }

    fun handleVariablesResponse(message: VariablesResponse) {

    }

    fun handleScopesResponse(message: ScopesResponse) {

    }

    fun handleStackTraceResponse(message: StackTraceResponse) {

    }

    fun handleThreadsResponse(message: ThreadsResponse) {

    }

    fun handleDebugInConsoleResponse(message: DebugInConsoleResponse) {

    }


    fun handleConfigurationDoneResponse(response: ConfigurationDoneResponse) {

    }
    fun handleModulesResponse(response: ModulesResponse) {
        // Your implementation here
    }
    fun handleErrorResponse(response: ErrorResponse) {
        // Your implementation here
    }

    fun handleCancelResponse(response: CancelResponse) {
        // Your implementation here
    }

    fun handleLaunchResponse(response: LaunchResponse) {
        // Your implementation here
    }

    fun handleSetBreakpointsResponse(response: SetBreakpointsResponse) {
        // Your implementation here
    }

    fun handleExitedEvent(event: ExitedEvent) {
        // Your implementation here
    }

    fun handleTerminatedEvent(event: TerminatedEvent) {
        // Your implementation here
    }

    fun handleInitializeResponse(response: InitializeResponse) {

    }

    fun handleRunning() {}
//
//    fun handleModulesLoaded(modules: List<LLModule>) {
//
//    }
//
//    fun handleModulesUnloaded(modules: List<LLModule>) {
//
//    }
//
//    fun handleBreakpointAdded(breakpoint: LLBreakpoint) {
//
//    }

    fun handleBreakpointRemoved(breakpointId: Int) {}

//    fun handleBreakpointUpdated(breakpoint: LLBreakpoint) {
//
//    }

//    fun handleBreakpointLocationsReplaced(breakpointId: Int, locations: List<LLBreakpointLocation>) {
//
//    }
//
//    fun handleBreakpointLocationsUpdated(breakpointId: Int, locations: List<LLBreakpointLocation>) {
//
//    }

    fun handleBreakpointLocationsRemoved(breakpointId: Int, locationIds: List<String>) {

    }
//
//    fun handleInterrupted(stopPlace: StopPlace) {
//
//    }
//
//    fun handleSignal(stopPlace: StopPlace, signal: String, meaning: String) {
//
//    }

//    fun handleException(
//        stopPlace: StopPlace,
//        exceptionAddress: Address,
//        exceptionFile: String?,
//        exceptionHash: DebuggerSourceFileHash?,
//        exceptionLine: Int,
//        description: String
//    ) {
//
//    }
//
//    fun handleBreakpoint(stopPlace: StopPlace, breakpointNumber: Int) {
//
//    }
//
//    fun handleWatchpoint(stopPlace: StopPlace, watchpointNumber: Int) {
//
//    }

    fun handleWatchpointScope(watchpointNumber: Int) {}

    fun handleTargetOutput(text: String, type: Key<*>) {

    }

    fun handleDebuggerOutput(text: String, type: Key<*>) {

    }

    fun handlePrompt(prompt: String) {

    }

//    fun handleTargetTerminated(exitStatus: ExitStatus) {
//
//    }



    fun handleAttached(pid: Int) {}

    fun handleDetached() {}

    fun handleConnected(connection: String) {

    }

    fun handleDisconnected() {}


    fun handleSymbolsDownloadStarted(caption: String, details: String) {

    }

    fun handleSymbolsDownloadProgress(percent: Int) {}

    fun handleSymbolsDownloadFinished() {}

    fun handleNotification(message: String, type: NotificationType) {

    }


    companion object {

        val LOG = Logger.getInstance(MessageHandler::class.java)

        fun createQueuedHandler(handler: MessageHandler, queueProcessor: QueueProcessor<Runnable>): MessageHandler {
            val handlerProxy = Proxy.newProxyInstance(
                MessageHandler::class.java.classLoader,
                arrayOf(MessageHandler::class.java)
            ) { _, method, args ->
                if (method.declaringClass == Any::class.java) {
                    method.invoke(handler, *args.orEmpty())
                } else {
                    queueProcessor.add {
                        try {
                            method.invoke(handler, *args.orEmpty())
                        } catch (e: InvocationTargetException) {
                            LOG.error(e)
                        } catch (e: IllegalAccessException) {
                            LOG.error(e)
                        }
                    }
                    null
                }
            }
            return handlerProxy as MessageHandler
        }

    }
}
