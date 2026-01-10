/*
 * Copyright 2026 LinQingYing. and contributors.
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

package org.cangnova.cangjie.debugger.dap.protocol

import com.intellij.openapi.diagnostic.Logger
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.cangnova.cangjie.debugger.dap.core.*
import org.cangnova.cangjie.debugger.dap.exception.DapAdapterException
import org.eclipse.lsp4j.debug.services.IDebugProtocolServer
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CopyOnWriteArrayList

/**
 * DAP 协议客户端
 *
 * 职责：
 * - 封装 DAP 协议操作
 * - 使用已建立的 IDebugProtocolServer
 * - 不负责创建连接
 * - 事件订阅与分发
 */
class DapProtocolClient(
    private val server: IDebugProtocolServer
) : DebugAdapter {

    companion object {
        private val LOG = Logger.getInstance(DapProtocolClient::class.java)
    }

    override val type = AdapterType.DAP

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Connected)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val eventHandlers = CopyOnWriteArrayList<(AdapterEvent) -> Unit>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * 初始化适配器
     *
     * 注意：此方法不再创建连接，只发送初始化请求
     */
    override suspend fun initialize( ): Result<Capabilities> {
        return withContext(Dispatchers.IO) {
            try {
                LOG.info("Initializing DAP protocol client")

                // 直接发送初始化请求，不再创建连接
                val capabilities = sendInitializeRequest().getOrThrow()

                LOG.info("DAP protocol client initialized successfully")
                Result.success(capabilities)
            } catch (e: Exception) {
//                LOG.error("Failed to initialize DAP protocol client", e)
                _connectionState.value = ConnectionState.Failed(e)
                Result.failure(DapAdapterException("Initialization failed", e))
            }
        }
    }

    /**
     * 发送初始化请求
     */
    private suspend fun sendInitializeRequest(): Result<Capabilities> {
        return withContext(Dispatchers.IO) {
            try {
                val initializeRequest = org.eclipse.lsp4j.debug.InitializeRequestArguments().apply {
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
//                LOG.error("Failed to send initialize request", e)
                Result.failure(DapAdapterException("Initialize request failed", e))
            }
        }
    }

    override suspend fun configurationDone(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                LOG.info("Sending configuration done request")
                val args = org.eclipse.lsp4j.debug.ConfigurationDoneArguments()
                server.configurationDone(args).await()
                LOG.info("Configuration done successfully")
                Result.success(Unit)
            } catch (e: Exception) {
//                LOG.error("Failed to send configuration done", e)
                Result.failure(DapAdapterException("Configuration done failed", e))
            }
        }
    }

    override suspend fun launch(args: LaunchArguments): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                LOG.info("Launching debug targetPlatform: ${args.program}")

                val launchArgs = mapOf(
                    "type" to "cangjie",
                    "request" to "launch",
                    "program" to args.program,
                    "args" to args.arguments,
                    "cwd" to args.workingDirectory,
                    "env" to args.environment
                )

                server.launch(launchArgs).await()

                LOG.info("Debug targetPlatform launched successfully")
                Result.success(Unit)
            } catch (e: Exception) {
//                LOG.error("Failed to launch debug targetPlatform", e)
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
//                LOG.error("Failed to attach to process", e)
                Result.failure(DapAdapterException("Attach failed", e))
            }
        }
    }

    override suspend fun disconnect(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                LOG.info("Disconnecting from DAP server")

                server.disconnect(org.eclipse.lsp4j.debug.DisconnectArguments().apply {
                    terminateDebuggee = true
                }).await()

                _connectionState.value = ConnectionState.Disconnected
                LOG.info("Disconnected successfully")

                Result.success(Unit)
            } catch (e: Exception) {
//                LOG.error("Failed to disconnect", e)
                Result.failure(DapAdapterException("Disconnect failed", e))
            }
        }
    }

    override suspend fun terminate(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                LOG.debug("Sending terminate request to DAP server")

                val args = org.eclipse.lsp4j.debug.TerminateArguments().apply {
                    restart = false
                }

                server.terminate(args).await()

                LOG.debug("Terminate request sent successfully")
                Result.success(Unit)
            } catch (e: Exception) {
                LOG.warn("Failed to send terminate request: ${e.message}")
                // terminate 请求失败不是致命错误
                Result.success(Unit)
            }
        }
    }

    override suspend fun setBreakpoints(
        source: SourceFile,
        breakpoints: List<BreakpointSpec>
    ): Result<List<BreakpointResult>> {
        return withContext(Dispatchers.IO) {
            try {
                val args = org.eclipse.lsp4j.debug.SetBreakpointsArguments().apply {
                    this.source = org.eclipse.lsp4j.debug.Source().apply {
                        path = source.path
                        name = source.name
                    }
                    this.breakpoints = breakpoints.map { spec ->
                        org.eclipse.lsp4j.debug.SourceBreakpoint().apply {
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
//                LOG.error("Failed to set breakpoints", e)
                Result.failure(DapAdapterException("Set breakpoints failed", e))
            }
        }
    }

    override suspend fun continueExecution(threadId: Long): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                LOG.debug("Continuing execution for thread $threadId")

                val args = org.eclipse.lsp4j.debug.ContinueArguments().apply {
                    this.threadId = threadId.toInt()
                }

                server.continue_(args).await()

                LOG.debug("Continue command sent for thread $threadId")
                Result.success(Unit)
            } catch (e: Exception) {
//                LOG.error("Failed to continue execution for thread $threadId", e)
                Result.failure(DapAdapterException("Continue execution failed", e))
            }
        }
    }

    override suspend fun pause(threadId: Long): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                LOG.debug("Pausing execution for thread $threadId")

                val args = org.eclipse.lsp4j.debug.PauseArguments().apply {
                    this.threadId = threadId.toInt()
                }

                server.pause(args).await()

                LOG.debug("Pause command sent for thread $threadId")
                Result.success(Unit)
            } catch (e: Exception) {
//                LOG.error("Failed to pause execution for thread $threadId", e)
                Result.failure(DapAdapterException("Pause execution failed", e))
            }
        }
    }

    override suspend fun next(threadId: Long): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                LOG.debug("Step over for thread $threadId")

                val args = org.eclipse.lsp4j.debug.NextArguments().apply {
                    this.threadId = threadId.toInt()
                }

                server.next(args).await()

                LOG.debug("Step over command sent for thread $threadId")
                Result.success(Unit)
            } catch (e: Exception) {
//                LOG.error("Failed to step over for thread $threadId", e)
                Result.failure(DapAdapterException("Step over failed", e))
            }
        }
    }

    override suspend fun stepIn(threadId: Long): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                LOG.debug("Step in for thread $threadId")

                val args = org.eclipse.lsp4j.debug.StepInArguments().apply {
                    this.threadId = threadId.toInt()
                }

                server.stepIn(args).await()

                LOG.debug("Step in command sent for thread $threadId")
                Result.success(Unit)
            } catch (e: Exception) {
//                LOG.error("Failed to step in for thread $threadId", e)
                Result.failure(DapAdapterException("Step in failed", e))
            }
        }
    }

    override suspend fun stepOut(threadId: Long): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                LOG.debug("Step out for thread $threadId")

                val args = org.eclipse.lsp4j.debug.StepOutArguments().apply {
                    this.threadId = threadId.toInt()
                }

                server.stepOut(args).await()

                LOG.debug("Step out command sent for thread $threadId")
                Result.success(Unit)
            } catch (e: Exception) {
//                LOG.error("Failed to step out for thread $threadId", e)
                Result.failure(DapAdapterException("Step out failed", e))
            }
        }
    }

    override suspend fun getStackTrace(threadId: Long): Result<List<StackFrameInfo>> {
        return withContext(Dispatchers.IO) {
            try {
                LOG.debug("Getting stack trace for thread $threadId")

                val args = org.eclipse.lsp4j.debug.StackTraceArguments().apply {
                    this.threadId = threadId.toInt()
                    startFrame = 0
                    levels = 0 // 0表示获取所有帧
                }

                val response = server.stackTrace(args).await()

                val frames = response.stackFrames.map { frame ->
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
                        column = frame.column
                    )
                }

                LOG.debug("Retrieved ${frames.size} stack frames for thread $threadId")
                Result.success(frames)
            } catch (e: Exception) {
//                LOG.error("Failed to get stack trace for thread $threadId", e)
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
//                LOG.error("Failed to get threads", e)
                Result.failure(DapAdapterException("Get threads failed", e))
            }
        }
    }

    override suspend fun getScopes(frameId: Long): Result<List<Scope>> {
        return withContext(Dispatchers.IO) {
            try {
                LOG.debug("Getting scopes for frame $frameId")

                val args = org.eclipse.lsp4j.debug.ScopesArguments().apply {
                    this.frameId = frameId.toInt()
                }

                val response = server.scopes(args).await()

                val scopes = response.scopes.map { dapScope ->
                    Scope(
                        name = dapScope.name,
                        variablesReference = dapScope.variablesReference.toLong(),
                        expensive = false,
                        presentationHint = dapScope.presentationHint
                    )
                }

                LOG.debug("Retrieved ${scopes.size} scopes for frame $frameId")
                Result.success(scopes)
            } catch (e: Exception) {
//                LOG.error("Failed to get scopes for frame $frameId", e)
                Result.failure(DapAdapterException("Get scopes failed", e))
            }
        }
    }

    override suspend fun getVariables(variablesReference: Long): Result<List<Variable>> {
        return withContext(Dispatchers.IO) {
            try {
                val args = org.eclipse.lsp4j.debug.VariablesArguments().apply {
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
//                LOG.error("Failed to get variables", e)
                Result.failure(DapAdapterException("Get variables failed", e))
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

                val args = org.eclipse.lsp4j.debug.SetVariableArguments().apply {
                    this.variablesReference = variablesReference.toInt()
                    this.name = name
                    this.value = value
                }

                val response = server.setVariable(args).await()

                val variable = Variable(
                    name = name,
                    value = response.value,
                    type = response.type ?: "",
                    variablesReference = response.variablesReference.toLong(),
                    presentationHint = null
                )

                LOG.debug("Variable set successfully: $variable")
                Result.success(variable)
            } catch (e: Exception) {
//                LOG.error("Failed to set variable: $name = $value", e)
                Result.failure(DapAdapterException("Set variable failed", e))
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
                val args = org.eclipse.lsp4j.debug.EvaluateArguments().apply {
                    this.expression = expression
                    this.frameId = frameId.toInt()
//                    this.context = when (context) {
//                        EvaluationType.WATCH -> org.eclipse.lsp4j.debug.EvaluateArgumentsContext.WATCH
//                        EvaluationType.REPL -> org.eclipse.lsp4j.debug.EvaluateArgumentsContext.REPL
//                        EvaluationType.HOVER -> org.eclipse.lsp4j.debug.EvaluateArgumentsContext.HOVER
//                        EvaluationType.CONDITIONAL -> org.eclipse.lsp4j.debug.EvaluateArgumentsContext.CLIPBOARD
//                    }
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
//                LOG.error("Failed to evaluate expression: $expression", e)
                Result.failure(DapAdapterException("Evaluation failed", e))
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

    /**
     * 分发事件
     */
    fun dispatchEvent(event: AdapterEvent) {
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
        eventHandlers.clear()
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