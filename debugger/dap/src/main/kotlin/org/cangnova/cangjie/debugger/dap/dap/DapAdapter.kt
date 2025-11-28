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

package org.cangnova.cangjie.debugger.dap.dap

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.cangnova.cangjie.debugger.dap.core.*
import org.cangnova.cangjie.debugger.dap.core.Capabilities
import org.cangnova.cangjie.debugger.dap.core.Variable
import org.cangnova.cangjie.debugger.dap.exception.DapAdapterException
import org.eclipse.lsp4j.debug.*
import org.eclipse.lsp4j.debug.services.IDebugProtocolServer
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CopyOnWriteArrayList
import org.eclipse.lsp4j.debug.Scope as DapScope

/**
 * DAP协议适配器实现
 *
 * 实现Debug Adapter Protocol，提供统一的调试接口
 */
class DapAdapter(
    private val project: Project
) : DebugAdapter {

    companion object {
        private val LOG = Logger.getInstance(DapAdapter::class.java)
    }

    override val type = AdapterType.DAP

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private lateinit var connection: DapConnection
    private lateinit var client: DapClient
    private lateinit var server: IDebugProtocolServer

    private val eventHandlers = CopyOnWriteArrayList<(AdapterEvent) -> Unit>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override suspend fun initialize(config: AdapterConfig): Result<Capabilities> {
        return withContext(Dispatchers.IO) {
            try {
                LOG.info("Initializing DAP adapter")
                _connectionState.value = ConnectionState.Connecting

                // 创建连接
                connection = DapConnection(
                    host = config.host,
                    port = config.port,
                    config = config.connectionConfig
                )

                // 创建客户端
                client = DapClient(
                    eventDispatcher = ::dispatchEvent
                )

                // 连接到服务器
                server = connection.connect(client).getOrThrow()

                // 发送初始化请求
                val capabilities = sendInitializeRequest().getOrThrow()



                _connectionState.value = ConnectionState.Connected
                LOG.info("DAP adapter initialized successfully")

                Result.success(capabilities)
            } catch (e: Exception) {
                LOG.error("Failed to initialize DAP adapter", e)
                _connectionState.value = ConnectionState.Failed(e)
                Result.failure(DapAdapterException("Initialization failed", e))
            }
        }
    }

    private suspend fun sendInitializeRequest(): Result<Capabilities> {
        return withContext(Dispatchers.IO) {
            try {
                val initializeRequest = InitializeRequestArguments().apply {
                    clientID = "intellij-cangjie-dap"
                    clientName = "IntelliJ CangJie Debugger"
                    adapterID = "cangjie-debug"
                    linesStartAt1 = true
                    columnsStartAt1 = true
                    supportsVariableType = true
                    supportsVariablePaging = true
                }

                val response = server.initialize(initializeRequest).await()

                val capabilities = Capabilities(
                    supportsConfigurationDoneRequest = response.supportsConfigurationDoneRequest ?: false,
                    supportsFunctionBreakpoints = response.supportsFunctionBreakpoints ?: false,
                    supportsConditionalBreakpoints = response.supportsConditionalBreakpoints ?: false,
                    supportsHitConditionalBreakpoints = response.supportsHitConditionalBreakpoints ?: false,
                    supportsEvaluateForHovers = response.supportsEvaluateForHovers ?: false,
                    supportsStepBack = response.supportsStepBack ?: false,
                    supportsSetVariable = response.supportsSetVariable ?: false,
                    supportsRestartFrame = response.supportsRestartFrame ?: false,
                    supportsGotoTargetsRequest = response.supportsGotoTargetsRequest ?: false,
                    supportsStepInTargetsRequest = response.supportsStepInTargetsRequest ?: false,
                    supportsCompletionsRequest = response.supportsCompletionsRequest ?: false,
                    supportsModulesRequest = response.supportsModulesRequest ?: false,
                    supportsRestartRequest = response.supportsRestartRequest ?: false,
                    supportsExceptionOptions = response.supportsExceptionOptions ?: false,
                    supportsValueFormattingOptions = response.supportsValueFormattingOptions ?: false,
                    supportsExceptionInfoRequest = response.supportsExceptionInfoRequest ?: false,
                    supportTerminateDebuggee = response.supportTerminateDebuggee ?: false,
                    supportSuspendDebuggee = response.supportSuspendDebuggee ?: false,
                    supportsDelayedStackTraceLoading = response.supportsDelayedStackTraceLoading ?: false,
                    supportsLoadedSourcesRequest = response.supportsLoadedSourcesRequest ?: false,
                    supportsLogPoints = response.supportsLogPoints ?: false,
                    supportsTerminateThreadsRequest = response.supportsTerminateThreadsRequest ?: false,
                    supportsSetExpression = response.supportsSetExpression ?: false,
                    supportsTerminateRequest = response.supportsTerminateRequest ?: false,
                    supportsDataBreakpoints = response.supportsDataBreakpoints ?: false,
                    supportsReadMemoryRequest = response.supportsReadMemoryRequest ?: false,
                    supportsWriteMemoryRequest = response.supportsWriteMemoryRequest ?: false,
                    supportsDisassembleRequest = response.supportsDisassembleRequest ?: false,
                    supportsCancelRequest = response.supportsCancelRequest ?: false,
                    supportsBreakpointLocationsRequest = response.supportsBreakpointLocationsRequest ?: false,
                    supportsClipboardContext = response.supportsClipboardContext ?: false,
                    supportsSteppingGranularity = response.supportsSteppingGranularity ?: false,
                    supportsInstructionBreakpoints = response.supportsInstructionBreakpoints ?: false,
                    supportsExceptionFilterOptions = response.supportsExceptionFilterOptions ?: false
                )

                Result.success(capabilities)
            } catch (e: Exception) {
                LOG.error("Failed to send initialize request", e)
                Result.failure(DapAdapterException("Initialize request failed", e))
            }
        }
    }

    override suspend fun configurationDone(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                LOG.info("Sending configuration done request")

                val args = ConfigurationDoneArguments()
                server.configurationDone(args).await()

                LOG.info("Configuration done successfully")
                Result.success(Unit)
            } catch (e: Exception) {
                LOG.error("Failed to send configuration done", e)
                Result.failure(DapAdapterException("Configuration done failed", e))
            }
        }
    }

    override suspend fun launch(args: LaunchArguments): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                LOG.info("Launching debug target: ${args.program}")

                val launchArgs = mapOf(
                    "type" to "cangjie",
                    "request" to "launch",
                    "program" to args.program,
                    "args" to args.arguments,
                    "cwd" to args.workingDirectory,
                    "env" to args.environment
                )

                server.launch(launchArgs).await()

                LOG.info("Debug target launched successfully")
                Result.success(Unit)
            } catch (e: Exception) {
                LOG.error("Failed to launch debug target", e)
                Result.failure(DapAdapterException("Launch failed", e))
            }
        }
    }

    override suspend fun attach(args: AttachArguments): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                LOG.info("Attaching to process: ${args.processId}")

                val attachArgs = mapOf(
                    "type" to "cangjie",
                    "request" to "attach",
                    "processId" to args.processId
                )

                server.attach(attachArgs).await()

                LOG.info("Attached to process successfully")
                Result.success(Unit)
            } catch (e: Exception) {
                LOG.error("Failed to attach to process", e)
                Result.failure(DapAdapterException("Attach failed", e))
            }
        }
    }

    override suspend fun disconnect(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                LOG.info("Disconnecting from DAP server")

                server.disconnect(DisconnectArguments().apply {
                    terminateDebuggee = true
                }).await()

                _connectionState.value = ConnectionState.Disconnected
                LOG.info("Disconnected successfully")

                Result.success(Unit)
            } catch (e: Exception) {
                LOG.error("Failed to disconnect", e)
                Result.failure(DapAdapterException("Disconnect failed", e))
            }
        }
    }

    override suspend fun setBreakpoints(
        source: SourceFile,
        breakpoints: List<BreakpointSpec>
    ): Result<List<BreakpointResult>> {
        return withContext(Dispatchers.IO) {
            try {
                val args = SetBreakpointsArguments().apply {
                    this.source = Source().apply {
                        path = source.path
                        name = source.name
                    }
                    this.breakpoints = breakpoints.map { spec ->
                        SourceBreakpoint().apply {
                            line = spec.line
                            column = spec.column
                            condition = spec.condition
                            logMessage = spec.logMessage
                        }
                    }.toTypedArray()
                }

                val response = server.setBreakpoints(args).await()

                val results = response.breakpoints.map { bp ->
                    BreakpointResult(
                        id = bp.id,
                        verified = bp.isVerified,
                        line = bp.line,
                        message = bp.message
                    )
                }

                LOG.debug("Set ${results.size} breakpoints in ${source.path}")
                Result.success(results)
            } catch (e: Exception) {
                LOG.error("Failed to set breakpoints", e)
                Result.failure(DapAdapterException("Set breakpoints failed", e))
            }
        }
    }

    override suspend fun evaluate(
        expression: String,
        frameId: Long,
        context: EvaluationType
    ): Result<EvaluationResult> {
        return withContext(Dispatchers.IO) {
            try {
                val args = EvaluateArguments().apply {
                    this.expression = expression
                    this.frameId = frameId.toInt()
                    this.context = when (context) {
                        EvaluationType.WATCH -> EvaluateArgumentsContext.WATCH
                        EvaluationType.REPL -> EvaluateArgumentsContext.REPL
                        EvaluationType.HOVER -> EvaluateArgumentsContext.HOVER
                        EvaluationType.CONDITIONAL -> EvaluateArgumentsContext.CLIPBOARD
                    }
                }

                val response = server.evaluate(args).await()

                val result = EvaluationResult(
                    value = response.result,
                    type = response.type,
                    variablesReference = response.variablesReference.toLong(),
                    presentationHint = response.presentationHint?.let {
                        PresentationHint(
                            kind = it.kind,
                            attributes = it.attributes?.toList()
                        )
                    }
                )

                LOG.debug("Evaluated expression: $expression = ${result.value}")
                Result.success(result)
            } catch (e: Exception) {
                LOG.error("Failed to evaluate expression: $expression", e)
                Result.failure(DapAdapterException("Evaluation failed", e))
            }
        }
    }

    override suspend fun getVariables(variablesReference: Long): Result<List<Variable>> {
        return withContext(Dispatchers.IO) {
            try {
                val args = VariablesArguments().apply {
                    this.variablesReference = variablesReference.toInt()
                }

                val response = server.variables(args).await()

                val variables = response.variables.map { v ->
                    Variable(
                        name = v.name,
                        value = v.value,
                        type = v.type ?: "",
                        variablesReference = v.variablesReference.toLong(),
                        presentationHint = v.presentationHint?.kind
                    )
                }

                LOG.debug("Got ${variables.size} variables for reference $variablesReference")
                Result.success(variables)
            } catch (e: Exception) {
                LOG.error("Failed to get variables", e)
                Result.failure(DapAdapterException("Get variables failed", e))
            }
        }
    }

    override suspend fun getStackTrace(threadId: Long): Result<List<StackFrameInfo>> {
        return withContext(Dispatchers.IO) {
            try {
                LOG.debug("Getting stack trace for thread $threadId")

                val args = StackTraceArguments().apply {
                    this.threadId = threadId.toInt()
                    // 获取所有堆栈帧
                    startFrame = 0
                    levels = 0 // 0表示获取所有帧
                }

                val response = server.stackTrace(args).await()

                val frames = response.stackFrames.mapIndexed { index, frame ->
                    StackFrameInfo(
                        id = frame.id.toLong(),
                        name = frame.name,
                        source = frame.source?.let { source ->
                            SourceInfo(
                                path = source.path,
                                name = source.name
                            )
                        },
                        line = frame.line,
                        column = frame.column ?: 0
                    )
                }

                LOG.debug("Retrieved ${frames.size} stack frames for thread $threadId")
                Result.success(frames)
            } catch (e: Exception) {
                LOG.error("Failed to get stack trace for thread $threadId", e)
                Result.failure(DapAdapterException("Get stack trace failed", e))
            }
        }
    }

    override suspend fun getThreads(): Result<List<ThreadInfo>> {
        return withContext(Dispatchers.IO) {
            try {
                LOG.debug("Getting thread list")

                val response = server.threads().await()

                val threads = response.threads.map { thread ->
                    ThreadInfo(
                        id = thread.id.toLong(),
                        name = thread.name
                    )
                }

                LOG.debug("Retrieved ${threads.size} threads")
                Result.success(threads)
            } catch (e: Exception) {
                LOG.error("Failed to get threads", e)
                Result.failure(DapAdapterException("Get threads failed", e))
            }
        }
    }

    override suspend fun continueExecution(threadId: Long): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                LOG.debug("Continuing execution for thread $threadId")

                val args = ContinueArguments().apply {
                    this.threadId = threadId.toInt()
                }

                server.continue_(args).await()

                LOG.debug("Continue command sent for thread $threadId")
                Result.success(Unit)
            } catch (e: Exception) {
                LOG.error("Failed to continue execution for thread $threadId", e)
                Result.failure(DapAdapterException("Continue execution failed", e))
            }
        }
    }

    override suspend fun pause(threadId: Long): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                LOG.debug("Pausing execution for thread $threadId")

                val args = PauseArguments().apply {
                    this.threadId = threadId.toInt()
                }

                server.pause(args).await()

                LOG.debug("Pause command sent for thread $threadId")
                Result.success(Unit)
            } catch (e: Exception) {
                LOG.error("Failed to pause execution for thread $threadId", e)
                Result.failure(DapAdapterException("Pause execution failed", e))
            }
        }
    }

    override suspend fun next(threadId: Long): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                LOG.debug("Step over for thread $threadId")

                val args = NextArguments().apply {
                    this.threadId = threadId.toInt()
                }

                server.next(args).await()

                LOG.debug("Step over command sent for thread $threadId")
                Result.success(Unit)
            } catch (e: Exception) {
                LOG.error("Failed to step over for thread $threadId", e)
                Result.failure(DapAdapterException("Step over failed", e))
            }
        }
    }

    override suspend fun stepIn(threadId: Long): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                LOG.debug("Step in for thread $threadId")

                val args = StepInArguments().apply {
                    this.threadId = threadId.toInt()
                }

                server.stepIn(args).await()

                LOG.debug("Step in command sent for thread $threadId")
                Result.success(Unit)
            } catch (e: Exception) {
                LOG.error("Failed to step in for thread $threadId", e)
                Result.failure(DapAdapterException("Step in failed", e))
            }
        }
    }

    override suspend fun stepOut(threadId: Long): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                LOG.debug("Step out for thread $threadId")

                val args = StepOutArguments().apply {
                    this.threadId = threadId.toInt()
                }

                server.stepOut(args).await()

                LOG.debug("Step out command sent for thread $threadId")
                Result.success(Unit)
            } catch (e: Exception) {
                LOG.error("Failed to step out for thread $threadId", e)
                Result.failure(DapAdapterException("Step out failed", e))
            }
        }
    }

    override suspend fun getScopes(frameId: Long): Result<List<org.cangnova.cangjie.debugger.dap.core.Scope>> {
        return withContext(Dispatchers.IO) {
            try {
                LOG.debug("Getting scopes for frame $frameId")

                val args = ScopesArguments().apply {
                    this.frameId = frameId.toInt()
                }

                val response = server.scopes(args).await()

                val scopes: List<org.cangnova.cangjie.debugger.dap.core.Scope> =
                    response.scopes.map { dapScope: DapScope ->
                        org.cangnova.cangjie.debugger.dap.core.Scope(
                            name = dapScope.name,
                            variablesReference = dapScope.variablesReference.toLong(),
                            expensive = false, // DAP 的 expensive 字段可能不可用，默认为 false
                            presentationHint = dapScope.presentationHint
                        )
                    }

                LOG.debug("Retrieved ${scopes.size} scopes for frame $frameId")
                Result.success(scopes)
            } catch (e: Exception) {
                LOG.error("Failed to get scopes for frame $frameId", e)
                Result.failure(DapAdapterException("Get scopes failed", e))
            }
        }
    }

    override suspend fun setVariable(
        variablesReference: Long,
        name: String,
        value: String
    ): Result<Variable> {
        return withContext(Dispatchers.IO) {
            try {
                LOG.debug("Setting variable: $name = $value (reference: $variablesReference)")

                val args = SetVariableArguments().apply {
                    this.variablesReference = variablesReference.toInt()
                    this.name = name
                    this.value = value
                }

                val response = server.setVariable(args).await()

                val variable = Variable(
                    name = name, // 使用输入的 name
                    value = response.value,
                    type = response.type ?: "",
                    variablesReference = response.variablesReference.toLong(),
                    presentationHint = null // 暂时设为 null，后续可以扩展
                )

                LOG.debug("Variable set successfully: $variable")
                Result.success(variable)
            } catch (e: Exception) {
                LOG.error("Failed to set variable: $name = $value", e)
                Result.failure(DapAdapterException("Set variable failed", e))
            }
        }
    }

    override suspend fun terminate(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                LOG.debug("Sending terminate request to DAP server")

                val args = TerminateArguments().apply {
                    restart = false // 不重启，只是终止
                }

                server.terminate(args).await()

                LOG.debug("Terminate request sent successfully")
                Result.success(Unit)
            } catch (e: Exception) {
                LOG.warn("Failed to send terminate request: ${e.message}")
                // terminate 请求失败不是致命错误，继续执行断开连接
                Result.success(Unit)
            }
        }
    }

    override fun subscribeEvents(handler: (AdapterEvent) -> Unit): Subscription {
        eventHandlers.add(handler)
        return object : Subscription {
            override fun unsubscribe() {
                eventHandlers.remove(handler)
            }
        }
    }

    private fun dispatchEvent(event: AdapterEvent) {
        eventHandlers.forEach { handler ->
            try {
                handler(event)
            } catch (e: Exception) {
                LOG.error("Error dispatching event", e)
            }
        }
    }

    override fun dispose() {
        scope.cancel()
        if (::client.isInitialized) {
            client.dispose()
        }
        if (::connection.isInitialized) {
            connection.dispose()
        }
    }
}

/**
 * CompletableFuture 扩展函数，用于等待结果
 */
private suspend fun <T> CompletableFuture<T>.await(): T {
    return withContext(Dispatchers.IO) {
        get()
    }
}