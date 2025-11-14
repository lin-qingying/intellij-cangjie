//package org.cangnova.cangjie.protodebugger.core
//
//import com.google.protobuf.ByteString
//import com.google.protobuf.Message
//import com.intellij.execution.CommandLineUtil
//import com.intellij.execution.ExecutionException
//import com.intellij.execution.ExecutionFinishedException
//import com.intellij.execution.configurations.GeneralCommandLine
//import com.intellij.execution.process.*
//import com.intellij.openapi.application.ApplicationManager
//import com.intellij.openapi.application.ApplicationNamesInfo
//import com.intellij.openapi.application.PathManager
//import com.intellij.openapi.diagnostic.Logger
//import com.intellij.openapi.util.Expirable
//import com.intellij.openapi.util.Key
//import com.intellij.openapi.util.NlsContexts.DialogMessage
//import com.intellij.openapi.util.NlsSafe
//import com.intellij.openapi.util.Ref
//import com.intellij.openapi.util.SystemInfo
//import com.intellij.openapi.util.ThrowableComputable
//import com.intellij.openapi.util.registry.Registry
//import com.intellij.openapi.util.text.StringUtil
//import com.intellij.util.Consumer
//import com.intellij.util.PathUtil
//import com.intellij.util.concurrency.QueueProcessor
//import com.intellij.util.containers.ContainerUtil
//import com.intellij.util.io.BaseOutputReader
//import com.pty4j.unix.Pty
//import org.cangnova.cangjie.messages.DebuggerBundle
//import org.cangnova.cangjie.protodebugger.breakpoint.AddBreakpointResult
//import org.cangnova.cangjie.protodebugger.breakpoint.StopPlace
//import org.cangnova.cangjie.protodebugger.breakpoint.SymbolicBreakpoint
//import org.cangnova.cangjie.protodebugger.core.DebuggerDriverConfiguration.Companion.parseVersion
//import org.cangnova.cangjie.protodebugger.data.*
//import org.cangnova.cangjie.protodebugger.event.EventSpan
//import org.cangnova.cangjie.protodebugger.exception.DebuggerCommandException
//import org.cangnova.cangjie.protodebugger.exception.DebuggerEvaluationTimedOutException
//import org.cangnova.cangjie.protodebugger.exception.DriverException
//import org.cangnova.cangjie.protodebugger.exception.JumpToLineOutsideCurrentFunctionException
//import org.cangnova.cangjie.protodebugger.execution.ExecutionResult
//import org.cangnova.cangjie.protodebugger.execution.ExitStatus
//import org.cangnova.cangjie.protodebugger.execution.TargetState
//import org.cangnova.cangjie.protodebugger.execution.TargetStateTransition
//import org.cangnova.cangjie.protodebugger.ipc.DebuggerFatalException
//import org.cangnova.cangjie.protodebugger.ipc.ProtobufServer
//import org.cangnova.cangjie.protodebugger.ipc.ProtobufTimedOutException
//import org.cangnova.cangjie.protodebugger.ipc.ProtobufUtils
//import org.cangnova.cangjie.protodebugger.ipc.WinPipe
//import org.cangnova.cangjie.protodebugger.memory.Address
//import org.cangnova.cangjie.protodebugger.memory.AddressRange
//import org.cangnova.cangjie.protodebugger.memory.endCoerced
//import org.cangnova.cangjie.protodebugger.memory.rangeToExclusive
//import org.cangnova.cangjie.protodebugger.output.GLogOutputReaders
//import org.cangnova.cangjie.protodebugger.output.ResultList
//import org.cangnova.cangjie.protodebugger.path.PathMapping
//import org.cangnova.cangjie.protodebugger.process.HostMachine
//import org.cangnova.cangjie.protodebugger.process.LocalHost
//import org.cangnova.cangjie.protodebugger.process.ProcessOutputReaders
//import org.cangnova.cangjie.protodebugger.protocol.ProtobufMessageFactory
//import org.cangnova.cangjie.protodebugger.settings.ArchitectureType
//import org.cangnova.cangjie.protodebugger.settings.DebuggerSettings
//import org.cangnova.cangjie.protodebugger.settings.DisasmFlavor
//import org.cangnova.cangjie.protodebugger.settings.DisasmOptions
//import org.cangnova.cangjie.protodebugger.settings.RichValueDescriptionSupport
//import org.cangnova.cangjie.protodebugger.util.DebuggerSourceFileHash
//import org.cangnova.cangjie.protodebugger.util.Installer
//import org.cangnova.cangjie.protodebugger.util.ToolVersion
//import org.intellij.lang.annotations.PrintFormat
//import org.jetbrains.annotations.Contract
//import org.jetbrains.annotations.Nls
//import org.jetbrains.annotations.NonNls
//import org.jetbrains.annotations.TestOnly
//import proto.Broadcasts
//import proto.Model
//import proto.Protocol
//import proto.ProtocolResponses
//import java.io.ByteArrayOutputStream
//import java.io.File
//import java.io.FileNotFoundException
//import java.io.IOException
//import java.io.InputStream
//import java.io.OutputStream
//import java.nio.charset.StandardCharsets
//import java.util.*
//import java.util.concurrent.BlockingQueue
//import java.util.concurrent.CompletableFuture
//import java.util.concurrent.Future
//import java.util.concurrent.LinkedBlockingQueue
//import java.util.concurrent.TimeUnit
//import java.util.concurrent.atomic.AtomicBoolean
//import java.util.concurrent.atomic.AtomicReference
//import java.util.regex.Pattern
//import kotlin.collections.set
//import kotlin.text.toInt
//
///**
// * 调试器驱动类
// *
// * 该类是调试器的核心驱动器，负责管理与LLDB调试引擎的通信和控制。
// * 它提供了完整的调试功能，包括进程控制、断点管理、表达式求值、内存操作等。
// *
// * 使用场景：
// * - IntelliJ IDEA中的仓颉语言调试
// * - 管理调试会话的整个生命周期
// * - 与底层调试器(LLDB)进行通信
// * - 处理调试事件和状态变化
// *
// * 主要功能：
// * - 进程的启动、附加、分离和终止
// * - 断点和观察点的管理
// * - 线程和栈帧的控制
// * - 表达式求值和变量检查
// * - 内存读写和分析
// * - 反汇编和调试信息获取
// *
// * @param handler 事件处理器，用于处理调试器事件
// * @param configuration 调试器配置信息
// * @param architectureType 目标架构类型
// */
//class DebuggerDriver(
//    handler: Handler,
//    val configuration: DebuggerDriverConfiguration,
//    architectureType: ArchitectureType
//) : Consumer<Message>, RichValueDescriptionSupport {
//    private var capabilities: Long = 0
//    private var valuesFilteringEnabled = false
//    private var pty: Pty? = null
//    private val cachedArchInfo: AtomicReference<ArchInfo> = AtomicReference()
//    @Volatile
//    private var stoppedThread: LLThread? = null
//
//    @Volatile
//    private var processInput: OutputStream? = null
//    @Volatile
//    private var disasmFlavor: DisasmFlavor? = null
//
//    private val asyncFatalException: AtomicReference<DebuggerFatalException> = AtomicReference()
//
//    private var version: ToolVersion? = null
//
//    private val connectedClient: CompletableFuture<ProtobufServer<ProtocolResponses.CompositeResponse>> =
//        CompletableFuture()
//
//
//
//    private var commandLine: GeneralCommandLine  = this.configuration.createDriverCommandLine(this, architectureType)
//    private var frontendHandler: BaseProcessHandler<*> = createDebugProcessHandler(commandLine, this.configuration)
//
//
//    private val gLogOutputReaders: GLogOutputReaders = object : GLogOutputReaders(getLogDir(), "CangJieLLDBFrontend") {
//        override fun onTextAvailable(text: String, type: LogType) {
//
//            if (LOG.isTraceEnabled) {
//                LOG.trace(text.trim { it <= ' ' })
//            }
//        }
//    }
//
//    private var protobufServer: ProtobufServer<ProtocolResponses.CompositeResponse>  =  object : ProtobufServer<ProtocolResponses.CompositeResponse>(
//        this@DebuggerDriver,
//        object : ProtobufParser<ProtocolResponses.CompositeResponse> {
//
//
//            @Throws(IOException::class)
//
//            override fun parse(data: ByteArray): ProtocolResponses.CompositeResponse {
//
//                return ProtocolResponses.CompositeResponse.parseFrom(data)
//
//            }
//
//            override fun decompose(message: Message): Boolean {
//                return message is ProtocolResponses.CompositeResponse || message is Broadcasts.CompositeBroadcast
//
//            }
//        }) {
//        private fun decomposeRequest(request: Message): Boolean {
//
//            return request is Protocol.CompositeRequest
//        }
//
//        @Throws(ProtobufTimedOutException::class, ExecutionFinishedException::class)
//        override fun <ResponseType : Message> sendMessageAndWaitForReply(
//            message: Message,
//            responseClass: Class<ResponseType>,
//            responseHandler: Consumer<in ResponseType>,
//            msTimeout: Long
//        ) {
//            var msTimeout = msTimeout
//
//
//            val ignored = EventSpan("debug", {
//                ("sendMessageAndWaitForReply (" + ProtobufUtils.unpackComposite(message) { request ->
//                    decomposeRequest(
//                        request
//                    )
//                }::class.java.getSimpleName()) + ")"
//            }, { message.toString() })
//            try {
//                if (msTimeout == 0L && ApplicationManager.getApplication().isUnitTestMode) {
//                    msTimeout = getTimeoutMs().toLong()
//                }
//                super.sendMessageAndWaitForReply(message, responseClass, responseHandler, msTimeout)
//            } catch (t: Throwable) {
//                try {
//                    ignored.close()
//                } catch (tt: Throwable) {
//                    t.addSuppressed(tt)
//                }
//                throw t
//            }
//            ignored.close()
//        }
//
//        override fun handleIOException(ex: IOException?) {
//            ex?.let {
//                LOG.warn(it)
//                if (!connectedClient.completeExceptionally(it)) {
//                    this@DebuggerDriver.storeAsyncFatalException(DebuggerFatalException(it))
//                }
//            }
//        }
//    }
//
//
//    private var stateSpan: EventSpan? = null
//
//    private var readers: ProcessOutputReaders? = null
//    private var stateTransition: TargetStateTransition =
//        TargetStateTransition(TargetState.NOT_READY, TargetState.NOT_READY)
//
//    protected val handlerProcessor: QueueProcessor<Runnable> = QueueProcessor.createRunnableQueueProcessor()
//    protected var toRedirect = false
//    protected val handler: Handler = Handler.createQueuedHandler(handler, this.handlerProcessor)
//
//    private val temporaryBreakpoints: BlockingQueue<Int> = LinkedBlockingQueue()
//    val hostMachine: HostMachine get() = this.configuration.hostMachine
//
//
//    init {
//
//
//
//        val environment = commandLine .environment
//        environment["GLOG_log_dir"] = this.gLogOutputReaders.getLogDir().path
//        if (LOG.isTraceEnabled) {
//            environment["GLOG_minloglevel"] = "0"
//            environment["GLOG_logbufsecs"] = "0"
//            environment["GLOG_v"] = "1"
//            this.gLogOutputReaders.init()
//        } else {
//            environment["GLOG_minloglevel"] = "2"
//        }
//
//
//        frontendHandler.addProcessListener(object : ProcessAdapter() {
//            override fun processTerminated(event: ProcessEvent) {
//
//                connectedClient.completeExceptionally(ExecutionFinishedException())
//                val processInput = processInput
//                if (processInput != null) {
//                    try {
//                        processInput.close()
//                    } catch (ioex: IOException) {
//                        LOG.warn(ioex)
//                    }
//                }
//                this@DebuggerDriver.processInput = null
//                gLogOutputReaders.close()
//
//                    protobufServer.tearDown()
//
//                this@DebuggerDriver.handleExited(event.exitCode)
//            }
//
//            override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
//
//                val text = event.text
//                if (text != null) {
//                    if (ProcessOutputType.isStderr(outputType) || ProcessOutputType.isStdout(outputType)) {
//                        if (LOG.isDebugEnabled) {
//                            LOG.debug(PathUtil.getFileName(commandLine.exePath) + " [" + outputType + "]: " + text)
//                        }
//                        handleDebuggerOutput(text, outputType)
//                    }
//                }
//            }
//        })
//    }
//
//    val port get() = protobufServer.port
//
//    @Throws(ExecutionException::class)
//    protected fun getProtobufClient(): ProtobufServer<ProtocolResponses.CompositeResponse> {
//        checkErrors()
//        return ExecutionResult.get(connectedClient)
//    }
//
//    @Throws(ExecutionException::class, DebuggerCommandException::class)
//    fun getValueAddress(value: LLValue): Long {
//
//        val request: Protocol.CompositeRequest = ProtobufMessageFactory.getValueAddress(
//            valId(
//                value
//            )
//        )
//        val errorMessage: Ref<String?> = Ref()
//        val result = Ref.create<Long>()
//        getProtobufClient().sendMessageAndWaitForReply(
//            request,
//            ProtocolResponses.GetValueAddressResponse::class.java,
//            { res ->
//                val isValid: Boolean = res.status.success
//                if (!isValid) {
//                    errorMessage.set(res.status.getErrorMessage())
//                } else {
//                    result.set(res.address)
//                }
//            },
//            0L
//        )
//        return if (!errorMessage.isNull) {
//            val message = errorMessage.get()
//            throw DebuggerCommandException(message ?: "Unknown error occurred")
//        } else {
//            result.get() as Long
//        }
//    }
//
//    private fun storeAsyncFatalException(e: DebuggerFatalException) {
//        asyncFatalException.compareAndSet(null, e)
//    }
//
//
//    protected fun handleTargetOutput(text: @Nls String, type: Key<*>) {
//        logOutputSummary("Target", text, type)
//        this.handler.handleTargetOutput(text, type)
//    }
//
//    protected fun handlePrompt(inMultiLineCommand: Boolean) {
//        this.handlePrompt(if (inMultiLineCommand) 1 else 0)
//    }
//
//    @TestOnly
//    fun waitHandlerProcessed() {
//        handlerProcessor.waitFor()
//    }
//
//    protected fun handlePrompt() {
//        this.handlePrompt(false)
//    }
//
//    abstract class ResponseMessageConsumer<T : Message, E : Exception>(private var myMessage: String) :
//        Consumer<T> {
//        private var isValid = false
//
//        fun getMessage(): String {
//            return myMessage
//        }
//
//        fun isValid(): Boolean {
//            return isValid
//        }
//
//        override fun consume(message: T) {
//            val allFields = message.allFields
//            allFields.values.forEach {
//                if (it is ProtocolResponses.ResponseStatus) {
//                    isValid = it.success
//                    if (!isValid) {
//                        val errorMessage = it.errorMessage
//                        if (!StringUtil.isEmptyOrSpaces(errorMessage)) {
//                            myMessage = errorMessage
//                        }
//                    }
//                }
//            }
//        }
//
//
//        open fun throwIfNeeded() {
//            if (!isValid) {
//                throwError()
//            }
//        }
//
//        abstract fun throwError()
//    }
//
//    protected open class ThrowIfNotValid<T : Message>(message: String) :
//        ResponseMessageConsumer<T, DriverException>(message) {
//
//        @Throws(DriverException::class)
//        override fun throwIfNeeded() {
//            super.throwIfNeeded()
//        }
//
//        @Throws(DriverException::class)
//        override fun throwError() {
//            throw DriverException(getMessage())
//        }
//    }
//
//
//    @Throws(ExecutionException::class)
//    fun <R : Message, E : Exception> sendMessageAndWaitForReply(
//        message: Message,
//        responseClass: Class<R>,
//        errorHandler: ResponseMessageConsumer<in R, E>,
//        msTimeout: Long
//    ): R {
//        val responseRef = Ref.create<R>()
//        val responseHandler = Consumer { responseMessage: R ->
//            errorHandler.consume(responseMessage)
//            if (errorHandler.isValid()) {
//                responseRef.set(responseMessage)
//            }
//        }
//
//        getProtobufClient().sendMessageAndWaitForReply(message, responseClass, responseHandler, msTimeout)
//        errorHandler.throwIfNeeded()
//
//        return responseRef.get()
//            ?: throw ExecutionException(DebuggerBundle.message("error.null.response.to.message", message))
//    }
//
//    @Throws(ExecutionException::class)
//    fun <R : Message> sendMessageAndWaitForReply(message: Message, responseClass: Class<R>): R {
//        val errorHandler: ResponseMessageConsumer<in R, DriverException> =
//            ThrowIfNotValid(DebuggerBundle.message("error.invalid.response", arrayOfNulls<Any>(0)))
//        val result: Message = sendMessageAndWaitForReply(message, responseClass, errorHandler, 0L)
//        @Suppress("UNCHECKED_CAST")
//        return result as R
//    }
//
//    @Throws(ExecutionException::class)
//    protected fun addBreakpoint(address: Address): AddBreakpointResult {
//
//        return this.addBreakpoint(address, null as String?)
//    }
//
//    @Throws(ExecutionException::class)
//    protected fun addBreakpoint(address: Address, condition: String?): AddBreakpointResult {
//
//        val req: Protocol.CompositeRequest =
//            ProtobufMessageFactory.addBreakpoint(address.unsignedLongValue, condition)
//        val res: ProtocolResponses.AddBreakpointResponse = this.sendMessageAndWaitForReply(
//            req,
//            ProtocolResponses.AddBreakpointResponse::class.java
//        )
//        return makeBreakpoint(
//            res.breakpoint,
//            res.locationsList
//        )
//    }
//
//    @Throws(ExecutionException::class)
//    fun interrupt(): Boolean {
//        val result: Ref<Boolean> = Ref()
//        val req: Protocol.CompositeRequest = ProtobufMessageFactory.suspend()
//        getProtobufClient().sendMessageAndWaitForReply(
//            req,
//            ProtocolResponses.SuspendResponse::class.java
//        ) { suspendRes -> result.set(suspendRes.status.success) }
//        return !result.isNull && result.get() == true
//    }
//
//    @Throws(ExecutionException::class, DebuggerCommandException::class)
//    fun addBreakpoint(path: String, line: Int): AddBreakpointResult {
//        return addBreakpoint(path, line, null, false)
//    }
//
//    @Throws(ExecutionException::class)
//    fun runTo(address: Address) {
//        val breakpoint = addBreakpoint(address)
//        temporaryBreakpoints.add(breakpoint.breakpoint.id)
//        if (!resume()) {
//            throw ExecutionException(DebuggerBundle.message("error.cannot.resume.program", arrayOfNulls<Any>(0)))
//        }
//    }
//
//    @Throws(ExecutionException::class)
//    fun runTo(path: String, line: Int) {
//        try {
//            val breakpoint = this.addBreakpoint(path, line)
//            temporaryBreakpoints.add(breakpoint.breakpoint.id)
//        } catch (var4: DebuggerCommandException) {
//            throw ExecutionException(DebuggerBundle.message("error.cannot.set.breakpoint", arrayOfNulls<Any>(0)), var4)
//        }
//        if (!resume()) {
//            throw ExecutionException(DebuggerBundle.message("error.cannot.resume.program", arrayOfNulls<Any>(0)))
//        }
//    }
//
//
//    protected fun handlePrompt(promptLevel: Int) {
//        this.handlePrompt(
//            if (promptLevel == 0) "(" + this.getPromptText() + ") " else StringUtil.repeat(
//                "> ",
//                promptLevel
//            )
//        )
//    }
//
//    fun supportsJumpToLine(): Boolean {
//        return false
//    }
//
//    @Throws(ExecutionException::class, DebuggerCommandException::class)
//    fun jumpToAddress(thread: LLThread, address: Address, canLeaveFunction: Boolean): StopPlace {
//
//        val req = ProtobufMessageFactory.jumpToAddress(thread.id, address.unsignedLongValue, canLeaveFunction)
//        return sendAndHandleJumpRequest<ProtocolResponses.JumpToAddressResponse, DebuggerCommandException>(
//            thread,
//            req,
//            ProtocolResponses.JumpToAddressResponse::class.java
//        ) {
//            it.currentFrame
//        }
//    }
//
//    protected class ThrowDebuggerCommandExceptionIfNotValid<T : Message>(message: @DialogMessage String?) :
//        ResponseMessageConsumer<T, DebuggerCommandException>(message!!) {
//        @Throws(DebuggerCommandException::class)
//        override fun throwError() {
//            throw DebuggerCommandException(getMessage())
//        }
//    }
//
//    private fun newLLFrame(frame: Model.StackFrame): LLFrame {
//
//        return LLFrame(
//            frame.index,
//            frame.functionName,
//            this.configuration.convertToLocalPath(frame.location.filePath),
//            createSourceFileHash(
//                frame.location.hashAlgorithm,
//                frame.location.hashValue
//            ),
//            frame.location.line - 1,
//            frame.programCounter,
//
//            frame.isOptimized,
//            frame.isInlined,
//            frame.moduleName
//        )
//    }
//
//    @Throws(ExecutionException::class)
//    private inline fun <R : Message, E : Exception> sendAndHandleJumpRequest(
//        thread: LLThread,
//        message: Message,
//        responseClass: Class<R>,
//        getNewFrame: (R) -> Model.StackFrame
//    ): StopPlace {
//
//        val errorHandler =
//            ThrowDebuggerCommandExceptionIfNotValid<R>(
//                DebuggerBundle.message(
//                    "error.invalid.response",
//                    *arrayOf<Any>()
//                )
//            )
//
//        try {
//            val response = sendMessageAndWaitForReply(message, responseClass, errorHandler, 0L)
//            val newFrame = getNewFrame(response)
//            return StopPlace(thread, newLLFrame(newFrame))
//        } catch (dex: DebuggerCommandException) {
//            val errorMessage = dex.message
//            if (errorMessage != null && errorMessage.contains("is outside the current function")) {
//                throw JumpToLineOutsideCurrentFunctionException(errorMessage, dex)
//            } else {
//                throw dex
//            }
//        }
//    }
//
//    @Throws(ExecutionException::class, DebuggerCommandException::class)
//    fun jumpToLine(thread: LLThread, path: String, line: Int, canLeaveFunction: Boolean): StopPlace {
//        val convertedPath: String = this.configuration.convertToProjectModelPath(path)
//        val req: Protocol.CompositeRequest =
//            ProtobufMessageFactory.jumpToLine(thread.id, convertedPath, line + 1, canLeaveFunction)
//        return sendAndHandleJumpRequest<ProtocolResponses.JumpToLineResponse, DebuggerCommandException>(
//            thread, req,
//            ProtocolResponses.JumpToLineResponse::class.java,
//        ) {
//            it.currentFrame
//        }
//    }
//
//    protected fun handleDebuggerOutput(text: @Nls String, type: Key<*>) {
//
//        logOutputSummary("Debugger", text, type)
//        handler.handleDebuggerOutput(text, type)
//    }
//
//    protected fun handleInterrupted(stopPlace: StopPlace) {
//
//        setState(TargetState.SUSPENDED)
//        handler.handleInterrupted(stopPlace)
//    }
//
//    @Throws(DebuggerCommandException::class, ExecutionException::class)
//    fun resize(columns: Int, rows: Int) {
//    }
//
//    protected fun handleException(
//        stopPlace: StopPlace,
//        exceptionAddress: Address,
//        exceptionFile: String?,
//        exceptionHash: DebuggerSourceFileHash?,
//        exceptionLine: Int,
//        description: String
//    ) {
//
//        setState(TargetState.SUSPENDED)
//        handler.handleException(stopPlace, exceptionAddress, exceptionFile, exceptionHash, exceptionLine, description)
//    }
//
//    protected fun handleRunning() {
//        setState(TargetState.RUNNING)
//        handler.handleRunning()
//    }
//
//    protected fun handleSignal(stopPlace: StopPlace, signal: String, meaning: String) {
//
//        setState(TargetState.SUSPENDED)
//        handler.handleSignal(stopPlace, signal, meaning)
//    }
//
//    protected fun handleWatchpoint(stopPlace: StopPlace, watchpointNumber: Int) {
//
//        setState(TargetState.SUSPENDED)
//        handler.handleWatchpoint(stopPlace, watchpointNumber)
//    }
//
//    protected fun handleBreakpoint(stopPlace: StopPlace, breakpointNumber: Int) {
//
//        setState(TargetState.SUSPENDED)
//        handler.handleBreakpoint(stopPlace, breakpointNumber)
//    }
//
//    fun isInPromptMode(): Boolean {
//        return false
//    }
//
//    @Throws(ExecutionException::class)
//    fun completeConsoleCommand(command: String, pos: Int): ResultList<String?> {
//        val request: Protocol.CompositeRequest = ProtobufMessageFactory.handleCompletion(command, pos)
//        val reply: ProtocolResponses.HandleCompletionResponse = this.sendMessageAndWaitForReply(
//            request,
//            ProtocolResponses.HandleCompletionResponse::class.java
//        )
//
//
//        return ResultList.create(reply.completionsList, false)
//    }
//
//    protected fun handleExited(code: Int) {
//        setState(TargetState.FINISHED)
//        handler.handleExited(code)
//    }
//
//    protected fun handleAttached(pid: Int) {
//        handler.handleAttached(pid)
//    }
//
//    @Throws(ExecutionException::class)
//    fun resume(): Boolean {
//        val res = BooleanArray(1)
//        val resume: Protocol.CompositeRequest = ProtobufMessageFactory.resume()
//        getProtobufClient().sendMessageAndWaitForReply(
//            resume,
//            ProtocolResponses.ContinueResponse::class.java,
//            { continueRes -> res[0] = continueRes.status.success },
//            0L
//        )
//        return res[0]
//    }
//
//    protected fun handleSymbolsDownloadProgress(percent: Int) {
//        handler.handleSymbolsDownloadProgress(percent)
//    }
//
//    protected fun handleSymbolsDownloadFinished() {
//        handler.handleSymbolsDownloadFinished()
//    }
//
//    protected fun handleSymbolsDownloadStarted(caption: String, details: String) {
//
//        handler.handleSymbolsDownloadStarted(caption, details)
//    }
//
//
//    protected fun handleSelectedFrameChanged(thread: LLThread, frame: LLFrame) {
//
//        handler.handleSelectedFrameChanged(thread, frame)
//    }
//
//    protected fun handleBreakpointLocationsReplaced(breakpointId: Int, locations: List<LLBreakpointLocation>) {
//
//        handler.handleBreakpointLocationsReplaced(breakpointId, locations)
//    }
//
//    protected fun handleBreakpointLocationsUpdated(breakpointId: Int, locations: List<LLBreakpointLocation>) {
//
//        handler.handleBreakpointLocationsUpdated(breakpointId, locations)
//    }
//
//    protected fun handleBreakpointAdded(breakpoint: LLBreakpoint) {
//
//        handler.handleBreakpointAdded(breakpoint)
//    }
//
//    fun supportsFreezeOtherThreads(): Boolean {
//        return false
//    }
//
//    private fun newLLThread(thread: Model.Thread): LLThread {
//
//        val stopReasonInfo: Model.ThreadStopInfo = thread.stopInfo
//        val isStopped = stopReasonInfo.reason !== Model.StopReason.STOP_REASON_INVALID
//        return LLThread(
//            thread.index.toLong(),
//            if (isStopped) "STOPPED" else null,
//            thread.queueName,
//            thread.getName(),
//            java.lang.String.valueOf(thread.threadId),
//            thread.isFrozen
//        )
//    }
//
//    @Throws(ExecutionException::class, DebuggerCommandException::class)
//    fun getThreads(): MutableList<LLThread> {
//        val request: Protocol.CompositeRequest = ProtobufMessageFactory.getThreads()
//        val res: ProtocolResponses.GetThreadsResponse = this.sendMessageAndWaitForReply(
//            request,
//            ProtocolResponses.GetThreadsResponse::class.java
//        )
//        return ContainerUtil.map(res.threadsList) { thread ->
//            this.newLLThread(
//                thread
//            )
//        }
//    }
//
//    fun supportsFreezeSingleThread(): Boolean {
//        return false
//    }
//
//
//    protected fun handleBreakpointUpdated(breakpoint: LLBreakpoint) {
//
//        handler.handleBreakpointUpdated(breakpoint)
//    }
//
//    protected fun handleBreakpointLocationsRemoved(breakpointId: Int, locationIds: List<String>) {
//
//        handler.handleBreakpointLocationsRemoved(breakpointId, locationIds)
//    }
//
//    protected fun handleBreakpointRemoved(breakpointId: Int) {
//        handler.handleBreakpointRemoved(breakpointId)
//    }
//
//    protected fun handleModulesLoaded(modules: List<LLModule>) {
//
//        handler.handleModulesLoaded(modules)
//    }
//
//    protected fun handleTargetTerminated(exitStatus: ExitStatus) {
//
//        setState(TargetState.FINISHED)
//        handler.handleTargetTerminated(exitStatus)
//    }
//
//    fun getPromptText(): String {
//        return "cangjie lldb"
//    }
//
//    @Throws(ExecutionException::class, DebuggerCommandException::class)
//    fun evaluate(thread: LLThread, frame: LLFrame, expression: String): LLValue {
//
//        return evaluate(thread.id, frame.index, expression)
//    }
//
//    protected fun handlePrompt(prompt: String) {
//
//        handler.handlePrompt(prompt)
//    }
//
//
//    @Throws(ExecutionException::class, DebuggerCommandException::class)
//    fun getFrames(
//        thread: LLThread,
//        from: Int,
//        count: Int,
//        untilFirstLineWithCode: Boolean
//    ): ResultList<LLFrame> {
//        val request = ProtobufMessageFactory.getFrames(thread.id, from, count, untilFirstLineWithCode)
//
//        val res: ProtocolResponses.GetFramesResponse = try {
//            sendMessageAndWaitForReply(
//                request,
//                ProtocolResponses.GetFramesResponse::class.java
//            )
//        } catch (e: ExecutionException) {
//            if (getState() != TargetState.SUSPENDED) {
//                throw DebuggerCommandException(e)
//            }
//            throw e
//        }
//
//        val frames = res.framesList.map { newLLFrame(it) }
//
//        return ResultList(frames, res.hasMore)
//    }
//
//
//    @Throws(ExecutionException::class, DebuggerCommandException::class)
//    fun disassembleFunction(address: Address, fallbackRange: AddressRange): List<LLInstruction> {
//
//        assert(fallbackRange.contains(address))
//
//        return try {
//            val request = ProtobufMessageFactory.contextInfo(
//                address.unsignedLongValue,
//                fallbackRange.start.unsignedLongValue,
//                fallbackRange.endCoerced.unsignedLongValue
//            )
//            val res = sendMessageAndWaitForReply(request, ProtocolResponses.GetContextInfoResponse::class.java)
//
//            val contextInfo = if (res.hasContextInfo()) res.contextInfo else null
//
//            if (contextInfo == null) {
//                return disassembleRangeWithPivot(fallbackRange, address, null)
//            } else {
//                val startAddr = Address.fromUnsignedLong(contextInfo.startAddress)
//                val endAddr = Address.fromUnsignedLong(contextInfo.endAddress)
//                val range = startAddr.rangeToExclusive(endAddr)
//
//                if (!range.contains(address) || (contextInfo.isGap)) {
//                    return disassembleRange(fallbackRange, null)
//                } else if (range.size == 1L) {
//                    return disassembleRangeWithPivot(fallbackRange, address, null)
//                } else {
//                    val functionName = contextInfo.name
//                    val saneRange = if (range.size > 65536L) range.intersectWith(fallbackRange) else range
//
//                    return if (saneRange.start == startAddr) {
//                        disassembleRange(saneRange, functionName)
//                    } else {
//                        disassembleRangeWithPivot(saneRange, address, functionName)
//                    }
//                }
//            }
//        } catch (driverException: DriverException) {
//            return disassembleRangeWithPivot(fallbackRange, address, null)
//        }
//    }
//
//
//    @Throws(ExecutionException::class, DebuggerCommandException::class)
//    private fun executeLldbShowCommand(setting: String): ProtocolResponses.HandleConsoleCommandResponse {
//
//        return this.sendMessageAndWaitForReply(
//            ProtobufMessageFactory.handleConsoleCommand(
//                -1L, -1,
//                "settings show $setting"
//            ),
//            ProtocolResponses.HandleConsoleCommandResponse::class.java
//        )
//    }
//
//    private class ArchInfo(
//        private val myArchitecture: String,
//        private val myRegisterSets: List<LLRegisterSet>
//    ) {
//        val myRegisterNames: Set<String> = myRegisterSets
//            .flatMap {
//                it.registers
//            }
//            .toSet()
//
//        fun getArchitecture(): String {
//            return myArchitecture
//        }
//
//        fun getRegisterSets(): List<LLRegisterSet> {
//            return myRegisterSets
//        }
//
//        fun getRegisterNames(): Set<String> {
//            return myRegisterNames
//        }
//    }
//
//    @Throws(ExecutionException::class, DebuggerCommandException::class)
//    private fun computeArchitecture(): String {
//        val responseHandler = ThrowDebuggerCommandExceptionIfNotValid<ProtocolResponses.GetArchitectureResponse>(
//            DebuggerBundle.message("error.failed.to.get.architecture")
//        )
//        val res = sendMessageAndWaitForReply(
//            ProtobufMessageFactory.getArch(),
//            ProtocolResponses.GetArchitectureResponse::class.java,
//            responseHandler
//        )
//        return res.architecture
//    }
//
//    @Throws(ExecutionException::class, DebuggerCommandException::class)
//    private fun computeRegisterSets(): List<LLRegisterSet> {
//        val responseHandler = ThrowDebuggerCommandExceptionIfNotValid<ProtocolResponses.GetRegisterSetsResponse>(
//            DebuggerBundle.message("error.failed.to.get.register.sets")
//        )
//        val res = sendMessageAndWaitForReply(
//            ProtobufMessageFactory.getRegisterSets(),
//            ProtocolResponses.GetRegisterSetsResponse::class.java,
//            responseHandler
//        )
//
//        return res.registerSetsList.map { registerSet ->
//            LLRegisterSet(registerSet.name, ArrayList(registerSet.registerNamesList))
//        }
//    }
//
//    private fun getArchInfo(): ArchInfo {
//        val result = cachedArchInfo.get()
//        if (result != null) {
//            return result
//        } else {
//            val architecture = computeArchitecture()
//            val registerSets = computeRegisterSets()
//            val newResult = ArchInfo(architecture, registerSets)
//            cachedArchInfo.set(newResult)
//            return newResult
//        }
//    }
//
//    private fun initDisasmFlavor() {
//        if (disasmFlavor == null) {
//            val arch: ArchInfo
//            try {
//                arch = getArchInfo()
//            } catch (var9: DebuggerCommandException) {
//                LOG.debug("Failed to get architecture, assume intel syntax is not supported")
//                disasmFlavor = DisasmFlavor("default")
//                return
//            }
//
//            if (!arch.getArchitecture().contains("x86")) {
//                disasmFlavor = DisasmFlavor("default")
//            } else {
//                var flavor: String?
//                try {
//                    val result = executeLldbShowCommand("target.x86-disassembly-flavor")
//                    val output = result.standardOutput
//                    val separatorIdx = output.indexOf('=')
//                    flavor = if (separatorIdx != -1) output.substring(separatorIdx + 1).trim() else null
//                } catch (var8: DebuggerCommandException) {
//                    LOG.debug("Failed to get disassembly-flavor, assume intel syntax is not supported")
//                    disasmFlavor = DisasmFlavor(null)
//                    return
//                }
//
//                if ("default" == flavor) {
//                    flavor = "att"
//                }
//
//                val disasmFlavor = DisasmFlavor(flavor)
//                this@DebuggerDriver.disasmFlavor = disasmFlavor
//                if (DisasmFlavor.isX86DisasmFlavor(disasmFlavor)) {
//                    val useIntelSyntax = DisasmOptions.getUseIntelSyntax() ?: return
//                    val requestedFlavor = DisasmFlavor.getX86DisasmFlavor(useIntelSyntax)
//                    if (requestedFlavor == disasmFlavor) {
//                        return
//                    }
//
//                    try {
//                        setDisasmFlavor(requestedFlavor)
//                    } catch (var7: DebuggerCommandException) {
//                        LOG.warn("Failed to switch disassembly flavor", var7)
//                    }
//                }
//            }
//        }
//    }
//
//    protected fun disassembleRange(range: AddressRange, functionName: String?): List<LLInstruction> {
//
//        val start = range.start.unsignedLongValue
//        val end = range.endCoerced.unsignedLongValue
//        initDisasmFlavor()
//        val disassembleRes = sendMessageAndWaitForReply(
//            ProtobufMessageFactory.disassemble(start, end),
//            ProtocolResponses.DisassembleResponse::class.java
//        )
//        return convertInstructionList(
//            disassembleRes.instructionsList,
//            functionName,
//            start
//        )
//    }
//
//    @Throws(ExecutionException::class)
//    private fun disassembleRangeUntilPivot(
//        start: Address,
//        pivot: LLInstruction,
//        functionName: String?
//    ): MutableList<LLInstruction> {
//
//        val startAddr = start.unsignedLongValue
//        val pivotRange = pivot.range
//        val pivotAddr = pivotRange.start.unsignedLongValue
//        val pivotInstrSize = pivotRange.size.toInt()
//        initDisasmFlavor()
//        val disassembleRes = sendMessageAndWaitForReply(
//            ProtobufMessageFactory.disassembleUntilPivot(startAddr, pivotAddr, pivotInstrSize),
//            ProtocolResponses.DisassembleResponse::class.java
//        )
//
//        return convertInstructionList(
//            disassembleRes.instructionsList,
//            functionName,
//            startAddr
//        )
//    }
//
//    protected fun disassembleRangeWithPivot(
//        range: AddressRange,
//        pivot: Address,
//        functionName: String?
//    ): List<LLInstruction> {
//
//        assert(range.contains(pivot))
//
//        val rangePostPivot = AddressRange(pivot, range.endInclusive)
//        val instructionsFromPivot = disassembleRange(rangePostPivot, functionName)
//
//        return if (instructionsFromPivot.isEmpty()) {
//            instructionsFromPivot
//        } else {
//            val pivotInstruction = instructionsFromPivot[0]
//            val instructions = disassembleRangeUntilPivot(range.start, pivotInstruction, functionName)
//            instructions.addAll(instructionsFromPivot)
//            instructions
//        }
//    }
//
//    @Throws(ExecutionException::class, DebuggerCommandException::class)
//    fun dumpMemory(range: AddressRange): List<LLMemoryHunk> {
//        val result = Ref.create<ByteString>()
//        val exception = Ref.create<DebuggerCommandException>()
//
//        getProtobufClient().sendMessageAndWaitForReply(
//            ProtobufMessageFactory.dumpMemory(
//                range.start.unsignedLongValue,
//                range.endInclusive.unsignedLongValue + 1L
//            ),
//            ProtocolResponses.DumpMemoryResponse::class.java
//        ) { res ->
//            if (!res.status.success) {
//                exception.set(DebuggerCommandException(res.status.errorMessage))
//            } else if (res.data.size().toLong() != range.size) {
//                exception.set(DebuggerCommandException("Unable to read memory $range"))
//            } else {
//                result.set(res.data)
//            }
//        }
//
//        if (!exception.isNull) {
//            throw exception.get()
//        } else {
//            val hunk = LLMemoryHunk(range, result.get().toByteArray())
//            val list = mutableListOf(hunk)
//            return list
//        }
//    }
//
//    private fun createLLValue(lldbValue: Model.Value, expression: String?): LLValue {
//
//        val type: LLValue.TypeClass? = when (lldbValue.getTypeClass()) {
//            Model.ValueTypeClass.VALUE_TYPE_FUNCTION -> LLValue.TypeClass.FUNCTION
//            Model.ValueTypeClass.VALUE_TYPE_BUILTIN -> LLValue.TypeClass.BUILTIN
//            Model.ValueTypeClass.VALUE_TYPE_CLASS, Model.ValueTypeClass.VALUE_TYPE_STRUCT -> LLValue.TypeClass.CLASS_STRUCT
//
//            Model.ValueTypeClass.VALUE_TYPE_BLOCK_POINTER -> LLValue.TypeClass.POINTER
//            else -> null
//        }
//        val referenceExpression: String = lldbValue.getName()
//        val result = LLValue(
//            expression ?: lldbValue.getName(),
//            lldbValue.typeName,
//            lldbValue.getDisplayType(),
//            lldbValue.address,
//            type,
//            referenceExpression
//        )
//        result.putUserData(LLVALUE_ID, lldbValue.id)
//        result.putUserData(LLVALUE_DATA_LOADER, LLValueDataLoader())
//
//        return result
//    }
//
//    @Throws(ExecutionException::class, DebuggerCommandException::class)
//    fun evaluate(id: Long, index: Int, expression: String): LLValue {
//
//        val req = ProtobufMessageFactory.evaluateExpression(id, index, expression)
//        val result = Ref<LLValue>()
//        val errorMessage = Ref<String>()
//
//        try {
//            EventSpan("debug", "evaluate", expression).use { ignored ->
//                try {
//                    getProtobufClient().sendMessageAndWaitForReply(
//                        req,
//                        ProtocolResponses.EvaluateExpressionResponse::class.java
//                    ) { res ->
//                        if (res.status.success) {
//                            if (res.hasResult()) {
//                                val lldbValue = res.result
//                                val value = createLLValue(lldbValue, expression)
//                                result.set(value)
//                            } else {
//                                errorMessage.set("<no result>")
//                            }
//                        } else {
//                            errorMessage.set(res.status.errorMessage)
//                        }
//                    }
//                } catch (t: Throwable) {
//                    ignored.close()
//                    throw t
//                }
//
//                ignored.close()
//            }
//        } catch (_: ProtobufTimedOutException) {
//            throw DebuggerEvaluationTimedOutException(expression)
//        }
//
//        if (!errorMessage.isNull) {
//            val message = errorMessage.get()
//            if ("<no result>" == message) {
//                val resultValue = LLValue("result", "void", null, null, "")
//                resultValue.putUserData(LLVALUE_ID, 0)
//                resultValue.putUserData(LLVALUE_DATA, LLValueData("", null, false, false, false))
//                resultValue.putUserData(CHILDREN_COUNT_CACHE, 0)
//                return resultValue
//            } else {
//                val p = Pattern.compile("error: (.*)\nerror: \\d+ errors? parsing expression\n")
//                val matcher = p.matcher(message)
//                if (matcher.find()) {
//                    return LLValue("result", "void", null, null, matcher.group(1))
//                }
//                throw DebuggerCommandException(message)
//            }
//        } else if (result.isNull) {
//            throw ExecutionException(DebuggerBundle.message("error.unknown.evaluation.error"))
//        } else {
//            val value = result.get()
//            return value ?: throw ExecutionException(DebuggerBundle.message("error.unknown.evaluation.error"))
//        }
//    }
//
//    @Throws(ExecutionException::class)
//    fun <R : Message, E : java.lang.Exception> sendMessageAndWaitForReply(
//        message: Message,
//        responseClass: Class<R>,
//        errorHandler: ResponseMessageConsumer<in R, E>
//    ): R {
//
//        return sendMessageAndWaitForReply(message, responseClass, errorHandler, 0L)
//    }
//
//    @Throws(ExecutionException::class)
//    fun setValuesFilteringEnabled(enabled: Boolean) {
//        if (this.valuesFilteringEnabled != enabled) {
//            this.valuesFilteringEnabled = enabled
//            this.sendMessageAndWaitForReply(
//                ProtobufMessageFactory.setValuesFilteringEnabled(enabled),
//                ProtocolResponses.SetValueFilteringPolicyResponse::class.java,
//                ThrowIfNotValid(
//                    DebuggerBundle.message(
//                        "error.cannot.set.values.filtering.policy",
//                        arrayOfNulls<Any>(0)
//                    )
//                )
//            )
//        }
//    }
//
//    @Throws(ExecutionException::class)
//    private fun doLaunch(
//        targetCommandLine: GeneralCommandLine,
//        launchRequestSupplier: ThrowableComputable<Protocol.CompositeRequest, ExecutionException>,
//        isRemoteTarget: Boolean
//    ): Long {
//
//        val launchedPid = Ref<Long>()
//
//        val responseHandler = object : ThrowIfNotValid<ProtocolResponses.LaunchResponse>(
//            DebuggerBundle.message("error.cannot.launch.process")
//        ) {
//            override fun consume(message: ProtocolResponses.LaunchResponse) {
//                super.consume(message)
//                if (isValid()) {
//                    launchedPid.set(message.processId)
//                }
//            }
//        }
//
//        printTargetCommandLine(targetCommandLine)
//
//        val launchReq = launchRequestSupplier.compute() as Protocol.CompositeRequest
//        getProtobufClient().sendMessageAndWaitForReply(
//            launchReq,
//            ProtocolResponses.LaunchResponse::class.java,
//            responseHandler
//        )
//
//        if (isRemoteTarget && !responseHandler.isValid() && "process launch failed: Locked" == responseHandler.getMessage()) {
//            throw DriverException(
//                DebuggerBundle.message(
//                    "debug.lldb.lockedDeviceUserMessage",
//                    ApplicationNamesInfo.getInstance().productName
//                )
//            )
//        } else {
//            responseHandler.throwIfNeeded()
//            val pid = launchedPid.get()
//            return pid ?: throw ExecutionException(DebuggerBundle.message("error.process.launch.no.pid"))
//        }
//    }
//
//    @Throws(ExecutionException::class)
//    protected fun lldbSet(setting: String, value: String?) {
//
//        val settingCommand = if (value != null) "set $setting $value" else "remove $setting"
//        this.protobufServer.sendMessageAndWaitUntilSent(
//            ProtobufMessageFactory.handleConsoleCommand(
//                -1L, -1,
//                "settings $settingCommand"
//            ),
//            ProtocolResponses.HandleConsoleCommandResponse::class.java,
//            ThrowIfNotValid(DebuggerBundle.message("error.cannot.set.setting.to", arrayOf(setting, value)))
//        )
//    }
//
//    @Throws(ExecutionException::class)
//    protected fun configureTarget() {
//        this.lldbSet("target.max-string-summary-length", "256")
//    }
//
//    @Throws(ExecutionException::class)
//    protected fun sendCreateTargetRequest(createTargetRequest: Protocol.CompositeRequest) {
//
//        val responseHandler: ThrowIfNotValid<ProtocolResponses.CreateTargetResponse> =
//            ThrowIfNotValid(DebuggerBundle.message("error.cannot.target.create", arrayOfNulls<Any>(0)))
//        this.sendMessageAndWaitForReply(
//            createTargetRequest,
//            ProtocolResponses.CreateTargetResponse::class.java,
//            responseHandler
//        )
//    }
//
//    @Throws(ExecutionException::class)
//    fun loadForLaunch(installer: Installer, architecture: String?): Inferior {
//        val targetCommandLine = installer.install()
//        val lldbArchitectureId =
//            getArchitectureType(architecture)
//        val useExternalConsole = targetCommandLine.getUserData(USE_EXTERNAL_CONSOLE_KEY) == true
//        sendCreateTargetRequest(
//            ProtobufMessageFactory.createTarget(
//                installer.executableFile.path,
//                lldbArchitectureId
//            )
//        )
//        configureTarget()
//
//        return object : DebuggerDriver.Inferior() {
//            @Throws(ExecutionException::class)
//            override fun startImpl(): Long {
//                return doLaunch(targetCommandLine, {
//                    var stdoutPath: String? = null
//                    var stderrPath: String? = null
//                    val inputFile = targetCommandLine.inputFile
//                    var stdinPath: String? = null
//                    val emulateTerminal = configuration.emulateTerminal()
//
//                    try {
//                        if (inputFile != null) {
//                            if (!inputFile.isFile || !inputFile.canRead()) {
//                                throw FileNotFoundException(
//                                    DebuggerBundle.message(
//                                        "debug.driver.cannotReadInputFile",
//                                        inputFile.path
//                                    )
//                                )
//                            }
//
//                            stdinPath = inputFile.path
//                            if (emulateTerminal && !SystemInfo.isWindows) {
//                                pty = Pty()
//                                processInput = pty!!.outputStream
//                                initTerminalReader(targetCommandLine, pty!!.inputStream)
//                                stdoutPath = pty!!.slaveName
//                                stderrPath = pty!!.slaveName
//                            } else if (emulateTerminal) {
//                                val readers = initReaders(LocalHost, targetCommandLine, true, true)
//                                stdoutPath = readers.getOutFileAbsolutePath()
//                            }
//                        } else if (!useExternalConsole) {
//                            if (SystemInfo.isWindows) {
//                                val pipe = WinPipe.createOutboundPipe("stdin")
//                                processInput = pipe.outputStream
//                                stdinPath = pipe.name
//                                if (emulateTerminal) {
//                                    val readers = initReaders(LocalHost, targetCommandLine, true, true)
//                                    stdoutPath = readers.getOutFileAbsolutePath()
//                                }
//                            } else {
//                                pty = Pty()
//                                processInput = pty!!.outputStream
//                                stdinPath = pty!!.slaveName
//                                if (emulateTerminal) {
//                                    initTerminalReader(targetCommandLine, pty!!.inputStream)
//                                    stdoutPath = pty!!.slaveName
//                                    stderrPath = pty!!.slaveName
//                                }
//                            }
//                        }
//                    } catch (ioEx: IOException) {
//                        LOG.error(ioEx)
//                        throw DriverException(
//                            ioEx.message?.let {
//                                DebuggerBundle.message(
//                                    "debug.driver.cannotCreatePipe",
//                                    it
//                                )
//                            }
//                        )
//                    }
//
//                    if (stdoutPath == null && toRedirect && !useExternalConsole) {
//                        val readers = initReaders(LocalHost, targetCommandLine, !SystemInfo.isWindows, false)
//                        stdoutPath = readers.getOutFileAbsolutePath()
//                        stderrPath = readers.getErrFileAbsolutePath()
//                    }
//
//                    ProtobufMessageFactory.launch(
//                        targetCommandLine,
//                        useExternalConsole,
//                        emulateTerminal,
//                        stdinPath,
//                        stdoutPath,
//                        stderrPath
//                    )
//                }, false)
//            }
//
//            @Throws(ExecutionException::class)
//            override fun detachImpl() {
//                detach()
//            }
//
//            @Throws(ExecutionException::class)
//            override fun destroyImpl(): Boolean {
//                return abort()
//            }
//        }
//    }
//
//    fun supportsWatchpointLifetime(): Boolean {
//        return false
//    }
//
//    fun supportsWatchpoints(): Boolean {
//        return true
//    }
//
//    @Throws(ExecutionException::class, DebuggerCommandException::class)
//    fun cancelSymbolsDownload(details: String) {
//        val cancelSymbolsDownloadReq: Protocol.CompositeRequest =
//            ProtobufMessageFactory.cancelSymbolsDownload(details)
//        getProtobufClient().sendMessage(cancelSymbolsDownloadReq, null, null as Consumer<*>?)
//    }
//
//    @Throws(ExecutionException::class, DebuggerCommandException::class)
//    fun executeInterpreterCommand(threadId: Long, frameIndex: Int, command: String): String {
//        val request = ProtobufMessageFactory.handleConsoleCommand(threadId, frameIndex, command)
//        val reply = sendMessageAndWaitForReply(request, ProtocolResponses.HandleConsoleCommandResponse::class.java)
//
//        if (reply.errorOutput != null) {
//            handleDebuggerOutput(asNlsSafe(reply.errorOutput), ProcessOutputTypes.STDERR)
//        }
//
//        return if (reply.standardOutput != null) {
//            handleDebuggerOutput(asNlsSafe(reply.standardOutput), ProcessOutputTypes.STDOUT)
//            reply.standardOutput ?: throw IllegalStateException("Unexpected null value for reply.out")
//        } else {
//            ""
//        }
//    }
//
//    @Throws(ExecutionException::class)
//    protected fun initReaders(
//        host: HostMachine,
//        targetCommandLine: GeneralCommandLine,
//        usePty: Boolean,
//        emulateTerminal: Boolean
//    ): ProcessOutputReaders {
//
//        val name = CommandLineUtil.extractPresentableName(targetCommandLine.commandLineString)
//        readers = object : ProcessOutputReaders(host, name, targetCommandLine.charset, usePty, emulateTerminal) {
//            override fun onTextAvailable(text: @NlsSafe String, key: Key<*>) {
//
//                handleTargetOutput(text, key)
//            }
//        }
//
//        return readers!!
//    }
//
//    protected fun printTargetCommandLine(commandLine: GeneralCommandLine?) {
//        if (commandLine != null) {
//            handleTargetOutput(commandLine.commandLineString + "\n", ProcessOutputTypes.SYSTEM)
//        }
//    }
//
//    @Throws(ExecutionException::class, DebuggerCommandException::class)
//    fun addAddressBreakpoint(address: Address, condition: String?): AddBreakpointResult {
//
//
//        return addBreakpoint(address, condition)
//    }
//
//    @Throws(ExecutionException::class, DebuggerCommandException::class)
//    fun removeCodepoints(ids: Collection<Int>) {
//        for (id in ids) {
//            val responseHandler = ThrowIfNotValid<ProtocolResponses.RemoveBreakpointResponse>(
//                DebuggerBundle.message("error.cannot.remove.breakpoint", *emptyArray())
//            )
//            val req = createRemoveBreakpointRequest(id)
//            sendMessageAndWaitForReply(req, ProtocolResponses.RemoveBreakpointResponse::class.java, responseHandler)
//
//        }
//    }
//
//    @Throws(ExecutionException::class)
//    fun stepOut(thread: LLThread, stopInFramesWithNoDebugInfo: Boolean) {
//        stepOut(stopInFramesWithNoDebugInfo)
//    }
//
//
//
//    @Throws(ExecutionException::class)
//    fun stepOut(stopInFramesWithNoDebugInfo: Boolean) {
//        this.stepOut(this.getLastStoppedThread(), stopInFramesWithNoDebugInfo)
//
//    }
//
//    @Throws(ExecutionException::class)
//    private fun getLastStoppedThread(): LLThread {
//        return stoppedThread
//            ?: throw ExecutionException(
//                DebuggerBundle.message(
//                    "error.cannot.retrieve.stopped.thread"
//                )
//            )
//    }
//
//
//    @Throws(ExecutionException::class)
//    fun stepInto(forceStepIntoFramesWithNoDebugInfo: Boolean, stepByInstruction: Boolean) {
//        this.stepInto(getLastStoppedThread(), forceStepIntoFramesWithNoDebugInfo, stepByInstruction)
//
//    }
//
//    @Throws(ExecutionException::class)
//    fun stepInto(thread: LLThread, forceStepIntoFramesWithNoDebugInfo: Boolean, stepByInstruction: Boolean) {
//        // Implementation should use the thread parameter, currently delegating to the no-thread version
//        stepInto(forceStepIntoFramesWithNoDebugInfo, stepByInstruction)
//    }
//
//
//    @Throws(ExecutionException::class)
//    private fun stepScripted(threadId: Long, className: String, exceptionMsg: @DialogMessage String) {
//        val responseHandler: ThrowIfNotValid<ProtocolResponses.StepScriptedResponse> = ThrowIfNotValid(exceptionMsg)
//        val request: Protocol.CompositeRequest = ProtobufMessageFactory.stepScripted(threadId, className)
//        this.sendMessageAndWaitForReply(
//            request,
//            ProtocolResponses.StepScriptedResponse::class.java,
//            responseHandler,
//            0L
//        )
//    }
//
//    @Throws(ExecutionException::class)
//    fun stepOver(thread: LLThread, stepByInstruction: Boolean) {
//        val responseHandler: ThrowIfNotValid<ProtocolResponses.StepOverResponse> =
//            ThrowIfNotValid(DebuggerBundle.message("error.cannot.step.over", arrayOfNulls<Any>(0)))
//        val request: Protocol.CompositeRequest = ProtobufMessageFactory.stepOver(thread.id, stepByInstruction)
//        sendMessageAndWaitForReply(request, ProtocolResponses.StepOverResponse::class.java, responseHandler)
//    }
//
//    @Throws(ExecutionException::class, DebuggerCommandException::class)
//    fun addSymbolicBreakpoint(symbolPattern: String, module: String?, condition: String?): LLSymbolicBreakpoint? {
//
//        val symBreakpoint = SymbolicBreakpoint()
//        symBreakpoint.setPattern(symbolPattern)
//        symBreakpoint.setModule(module)
//        symBreakpoint.setCondition(condition)
//        return addSymbolicBreakpoint(symBreakpoint)
//    }
//
//    @Throws(ExecutionException::class, DebuggerCommandException::class)
//    fun removeWatchpoint(ids: List<Int?>) {
//        val num = ids[0] as Int
//        val responseHandler: ThrowIfNotValid<ProtocolResponses.RemoveWatchpointResponse> =
//            ThrowIfNotValid(DebuggerBundle.message("error.cannot.remove.watchpoint", arrayOfNulls<Any>(0)))
//        val req: Protocol.CompositeRequest = ProtobufMessageFactory.removeWatchpoint(num)
//        this.sendMessageAndWaitForReply(req, ProtocolResponses.RemoveWatchpointResponse::class.java, responseHandler)
//    }
//
//    @Throws(ExecutionException::class, DebuggerCommandException::class)
//    fun addWatchpoint(
//        threadId: Long,
//        frameIndex: Int,
//        value: LLValue,
//        expr: String,
//        lifetime: LLWatchpoint.Lifetime?,
//        accessType: LLWatchpoint.AccessType
//    ): LLWatchpoint {
//        val expression: String = value.referenceExpression
//        val request: Protocol.CompositeRequest = ProtobufMessageFactory.addWatchpoint(
//            valId(value),
//            null,
//            accessType === LLWatchpoint.AccessType.ANY || accessType === LLWatchpoint.AccessType.READ,
//            accessType === LLWatchpoint.AccessType.ANY || accessType === LLWatchpoint.AccessType.WRITE,
//            true
//        )
//        val result: Ref<LLWatchpoint> = Ref.create(null)
//        val toThrow: Ref<DebuggerCommandException> = Ref.create(null)
//        getProtobufClient().sendMessageAndWaitForReply(
//            request,
//            ProtocolResponses.AddWatchpointResponse::class.java
//        ) { res ->
//            if (!res.status.success) {
//                toThrow.set(DebuggerCommandException(res.status.getErrorMessage()))
//            } else {
//                result.set(LLWatchpoint(res.watchpointId, expression))
//            }
//        }
//        return if (result.isNull) {
//            throw (toThrow.get() as DebuggerCommandException)
//        } else {
//            result.get() as LLWatchpoint
//
//        }
//    }
//
//    @Throws(ExecutionException::class, DebuggerCommandException::class)
//    fun addSymbolicBreakpoint(symbolicBreakpoint: SymbolicBreakpoint): LLSymbolicBreakpoint? {
//        val req: Protocol.CompositeRequest = ProtobufMessageFactory.addBreakpoint(
//            symbolicBreakpoint.getPattern(),
//            symbolicBreakpoint.isRegexpPattern(),
//            symbolicBreakpoint.getModule(),
//            symbolicBreakpoint.getCondition(),
//            symbolicBreakpoint.getThreadId()
//        )
//        val result: Ref<LLSymbolicBreakpoint?> = Ref()
//        getProtobufClient().sendMessageAndWaitForReply(
//            req,
//            ProtocolResponses.AddBreakpointResponse::class.java
//        ) { res -> result.set(LLSymbolicBreakpoint(res.breakpoint.id)) }
//        return result.get()
//    }
//
//    @Throws(ExecutionException::class, DebuggerCommandException::class)
//    fun addSymbolicBreakpoint(symbolPattern: String): LLSymbolicBreakpoint? {
//
//        return this.addSymbolicBreakpoint(symbolPattern, null, null)
//    }
//
//    fun getProcessInput(): OutputStream? {
//        return null
//    }
//
//    @Volatile
//    private var myAsyncAttachingTo: Int? = null
//
//    @Throws(ExecutionException::class)
//    fun loadForAttach(processId: Int): Inferior {
//        sendCreateTargetRequest(ProtobufMessageFactory.createTarget("", ""))
//        configureTarget()
//        return object : Inferior() {
//            @Throws(ExecutionException::class)
//            override fun startImpl(): Long {
//                this@DebuggerDriver.myAsyncAttachingTo = processId
//                val responseHandler: ThrowIfNotValid<ProtocolResponses.AttachResponse> =
//                    ThrowIfNotValid(DebuggerBundle.message("error.cannot.attach", arrayOfNulls<Any>(0)))
//                this@DebuggerDriver.sendMessageAndWaitForReply(
//                    ProtobufMessageFactory.attach(
//                        processId,
//                        this@DebuggerDriver.configuration.isContinueAfterAttachNeeded()
//                    ),
//                    ProtocolResponses.AttachResponse::class.java, responseHandler
//                )
//                return processId.toLong()
//            }
//
//            @Throws(ExecutionException::class)
//            override fun detachImpl() {
//                detach()
//            }
//
//            @Throws(ExecutionException::class)
//            override fun destroyImpl(): Boolean {
//                return abort()
//            }
//        }
//    }
//
//    @Throws(ExecutionException::class)
//    private fun abort(): Boolean {
//        val toThrow = Ref.create<ExecutionException>()
//        val abort = Ref.create(false)
//        getProtobufClient().sendMessageAndWaitForReply(
//            ProtobufMessageFactory.kill(),
//            ProtocolResponses.KillResponse::class.java
//        ) { res ->
//            val commonResponse: ProtocolResponses.ResponseStatus = res.status
//            if (commonResponse.success) {
//                abort.set(true)
//            } else {
//                var errorMessage: String = commonResponse.getErrorMessage()
//                if ("process not exist" != errorMessage && "process does not exist" != errorMessage) {
//                    if (StringUtil.isEmptyOrSpaces(errorMessage)) {
//                        errorMessage = DebuggerBundle.message("error.cannot.abort.process", arrayOfNulls<Any>(0))
//                    }
//                    toThrow.set(DriverException(errorMessage))
//                } else {
//                    abort.set(false)
//                }
//            }
//        }
//        return if (!toThrow.isNull) {
//            throw (toThrow.get() as ExecutionException)
//        } else {
//            abort.get() as Boolean
//        }
//    }
//
//    @Throws(ExecutionException::class)
//    fun checkErrors() {
//        val exception = this.asyncFatalException.get()
//        if (exception != null) {
//            this.asyncFatalException.compareAndSet(exception, null)
//            throw DebuggerFatalException(exception)
//        }
//    }
//
//    fun loadCoreDump(
//        coreFile: File,
//        symbolFile: File?,
//        sysroot: File?,
//        sourcePathMappings: List<PathMapping>,
//        execSearchPaths: List<String>
//    ): Inferior {
//        if (symbolFile != null) {
//            val sysrootPath = if (sysroot != null) sysroot.absolutePath else ""
//            val platform = if (sysroot != null) getLocalPlatform() else ""
//            sendCreateTargetRequest(
//                ProtobufMessageFactory.createRemoteTarget(
//                    symbolFile.path,
//                    "",
//                    platform,
//                    sysrootPath,
//                    ""
//                )
//            )
//        }
//
//        configureTarget()
//        if (execSearchPaths.isNotEmpty()) {
//            val execSearchPathsArgs = StringBuilder()
//            val var10: Iterator<*> = execSearchPaths.iterator()
//            while (var10.hasNext()) {
//                val path = var10.next() as String
//                execSearchPathsArgs.append("\"").append(path.replace("\\", "\\\\")).append("\" ")
//            }
//            lldbSet("target.exec-search-paths", execSearchPathsArgs.toString())
//        }
//
//        this.sendMessageAndWaitForReply(
//            ProtobufMessageFactory.loadCoreDump(coreFile.path),
//            ProtocolResponses.LoadCoreResponse::class.java
//        )
//        return object : Inferior() {
//            @Throws(ExecutionException::class)
//            override fun startImpl(): Long {
//                addPathMapping(sourcePathMappings, true)
//                return 0L
//            }
//
//            @Throws(ExecutionException::class)
//            override fun detachImpl() {
//                detach()
//            }
//
//            @Throws(ExecutionException::class)
//            override fun destroyImpl(): Boolean {
//                return abort()
//            }
//        }
//    }
//
//
//    @Throws(ExecutionException::class)
//    private fun addPathMapping(mappings: List<PathMapping>) {
//
//        this.addPathMapping(mappings, false)
//    }
//
//    @Throws(ExecutionException::class)
//    fun loadForRemote(
//        connectionString: String,
//        symbolFile: File?,
//        sysroot: File?,
//        pathMappings: List<PathMapping>
//    ): Inferior {
//
//        val exePath = if (symbolFile != null) symbolFile.path else ""
//        val sysrootPath = if (sysroot != null) sysroot.absolutePath else ""
//        if (LOG.isDebugEnabled) {
//            LOG.debug(
//                StringUtil.join(
//                    *arrayOf(
//                        "attaching remote process started under debug server: url: ",
//                        connectionString,
//                        " exePath: ",
//                        exePath,
//                        " sysroot: ",
//                        sysrootPath
//                    )
//                )
//            )
//        }
//
//        sendCreateTargetRequest(ProtobufMessageFactory.createRemoteTarget(exePath, "", "", sysrootPath, ""))
//        configureTarget()
//        this.addPathMapping(pathMappings)
//        return object : Inferior() {
//            @Throws(ExecutionException::class)
//            override fun startImpl(): Long {
//                val responseHandler: ThrowIfNotValid<ProtocolResponses.ConnectProcessResponse> =
//                    ThrowIfNotValid(
//                        DebuggerBundle.message(
//                            "error.cannot.attach.remote.debug.server",
//                            arrayOfNulls<Any>(0)
//                        )
//                    )
//                this@DebuggerDriver.sendMessageAndWaitForReply(
//                    ProtobufMessageFactory.connectProcess(
//                        connectionString,
//                        "gdb-remote",
//                        this@DebuggerDriver.configuration.isContinueAfterAttachNeeded()
//                    ),
//                    ProtocolResponses.ConnectProcessResponse::class.java, responseHandler
//                )
//                this@DebuggerDriver.resume()
//                return 0L
//            }
//
//            @Throws(ExecutionException::class)
//            override fun detachImpl() {
//                detach()
//            }
//
//            @Throws(ExecutionException::class)
//            override fun destroyImpl(): Boolean {
//                return abort()
//            }
//        }
//    }
//
//
//    protected fun setState(state: TargetState) {
//
//        val previousState = stateTransition.currentState
//        if (previousState != TargetState.FINISHED) {
//            stateTransition = TargetStateTransition(previousState, state)
//            if (state == TargetState.RUNNING) {
//                traceState(EventSpan("debug", "driver running", null, "<DebuggerDriver>"))
//            } else {
//                traceState(null)
//            }
//
//            if (state == TargetState.FINISHED) {
//                closeOutputReaders()
//            }
//        }
//    }
//
//    @Throws(ExecutionException::class, DebuggerCommandException::class)
//    fun addBreakpoint(
//        path: String,
//        line: Int,
//        condition: String?,
//        ignoreSourceHash: Boolean
//    ): AddBreakpointResult {
//        val convertedPath: String = this.configuration.convertToProjectModelPath(path)
//        val req: Protocol.CompositeRequest =
//            ProtobufMessageFactory.addBreakpoint(convertedPath, line + 1, ignoreSourceHash, condition)
//        val res: ProtocolResponses.AddBreakpointResponse = this.sendMessageAndWaitForReply(
//            req,
//            ProtocolResponses.AddBreakpointResponse::class.java
//        )
//        return makeBreakpoint(
//            res.breakpoint,
//            res.locationsList
//        )
//
//    }
//
//
//
//    @Throws(ExecutionException::class, DebuggerCommandException::class)
//    fun getVariables(thread: LLThread, frame: LLFrame): List<LLValue> {
//
//        return getVariables(thread.id, frame.index)
//    }
//
//
//    fun getVariables(threadId: Long, frameIndex: Int): List<LLValue> {
//        val staticsAndGlobals: Boolean = this.configuration.isStaticVarsLoadingEnabled()
//
//
//        return getVariables(threadId, frameIndex, staticsAndGlobals, staticsAndGlobals)
//    }
//
//    @Throws(ExecutionException::class, DebuggerCommandException::class)
//    fun getVariables(threadId: Long, frameIndex: Int, statics: Boolean, globals: Boolean): List<LLValue> {
//        val request = ProtobufMessageFactory.getVars(threadId, frameIndex, statics, globals)
//        val result = ArrayList<LLValue>()
//        val errorMessage = Ref<String>()
//
//        getProtobufClient().sendMessageAndWaitForReply(
//            request,
//            ProtocolResponses.GetVariablesResponse::class.java
//        ) { res ->
//            val commonResponse = res.status
//            if (!commonResponse.success) {
//                errorMessage.set(commonResponse.errorMessage)
//            } else {
//                res.variablesList.forEach { lldbValue ->
//                    result.add(createLLValue(lldbValue, null))
//                }
//            }
//        }
//
//        if (!errorMessage.isNull && !StringUtil.isEmpty(errorMessage.get())) {
//            throw DebuggerCommandException(errorMessage.get())
//        }
//
//        return result
//    }
//
//    fun getData(value: LLValue): LLValueData {
//        return getLLValueData(value)
//    }
//
//    fun getDescription(value: LLValue, maxLength: Int): String {
//        val req: Protocol.CompositeRequest = ProtobufMessageFactory.getValueDescription(
//            valId(
//                value
//            ), maxLength
//        )
//        val description = Ref.create<String>()
//        val exception = Ref.create<DebuggerCommandException>()
//        getProtobufClient().sendMessageAndWaitForReply(
//            req,
//            ProtocolResponses.GetValueDescriptionResponse::class.java
//        ) { res ->
//            val commonResponse: ProtocolResponses.ResponseStatus = res.status
//            if (!commonResponse.success) {
//                exception.set(DebuggerCommandException(commonResponse.getErrorMessage()))
//            } else {
//                if (res.description != null) {
//                    description.set(res.getDescription())
//                }
//            }
//        }
//        return if (!exception.isNull) {
//            throw exception.get()
//        } else {
//            description.get()
//        }
//    }
//
//    private fun convertList(valuesList: List<Model.Value>, result: MutableList<LLValue>) {
//
//        for (value in valuesList) {
//            result.add(createLLValue(value, null))
//
//        }
//    }
//
//    fun getChildrenCount(value: LLValue): Int {
//        val cached = value.getUserData(CHILDREN_COUNT_CACHE)
//        return if (cached != null) {
//
//            cached
//        } else {
//            val request: Protocol.CompositeRequest = ProtobufMessageFactory.getChildrenCount(
//                valId(
//                    value
//                )
//            )
//            val errorMessage: Ref<String> = Ref()
//            val result = Ref.create(0)
//            getProtobufClient().sendMessageAndWaitForReply(
//                request,
//                ProtocolResponses.GetChildrenCountResponse::class.java,
//                { res ->
//                    val isValid: Boolean = res.status.success
//                    if (!isValid) {
//                        errorMessage.set(res.status.getErrorMessage())
//                    } else {
//                        result.set(res.count)
//                    }
//                },
//                0L
//            )
//            if (!errorMessage.isNull) {
//                throw DebuggerCommandException(errorMessage.get()!!)
//            } else {
//                value.putUserData(CHILDREN_COUNT_CACHE, result.get())
//                result.get()
//
//
//            }
//        }
//    }
//
//    fun getVariableChildren(value: LLValue, from: Int, count: Int): ResultList<LLValue> {
//
//        val childrenCount = getChildrenCount(value)
//
//        return if (count == 0) {
//            ResultList.empty()
//
//        } else {
//            val request: Protocol.CompositeRequest =
//                ProtobufMessageFactory.getValueChildren(
//                    valId(
//                        value
//                    ), from, count
//                )
//            val errorMessage: Ref<String> = Ref()
//            val result: MutableList<LLValue> = mutableListOf()
//            getProtobufClient().sendMessageAndWaitForReply(
//                request,
//                ProtocolResponses.GetValueChildrenResponse::class.java,
//                { res ->
//                    val isValid: Boolean = res.status.success
//                    if (!isValid) {
//                        errorMessage.set(res.status.getErrorMessage())
//                    } else {
//                        this.convertList(res.childrenList, result)
//                    }
//                },
//                0L
//            )
//            val message = errorMessage.get()
//            if (message != null) {
//                throw DebuggerCommandException(message)
//            } else {
//                val hasMore = from + count < childrenCount
//                return ResultList.create(result, hasMore)
//
//
//            }
//        }
//    }
//
//    protected fun initTerminalReader(targetCommandLine: GeneralCommandLine, inputStream: InputStream) {
//
//        val name = CommandLineUtil.extractPresentableName(targetCommandLine.commandLineString)
//        val baseOutputReader: BaseOutputReader =
//            object : BaseOutputReader(inputStream, targetCommandLine.charset, Options.forTerminalPtyProcess()) {
//                init {
//                    start("Reading $name")
//                }
//
//                override fun onTextAvailable(text: @NlsSafe String) {
//
//                    this@DebuggerDriver.handleTargetOutput(text, ProcessOutputType.STDOUT)
//                }
//
//                override fun executeOnPooledThread(runnable: Runnable): Future<*> {
//
//                    return ApplicationManager.getApplication().executeOnPooledThread(runnable)
//                }
//            }
//    }
//
//
//    protected fun closeOutputReaders() {
//        val readers: ProcessOutputReaders? = this.readers
//        if (readers != null) {
//            try {
//                if (!readers.waitFor(300L, TimeUnit.MILLISECONDS)) {
//                    LOG.warn("Closing inferior output readers took too long")
//                }
//            } finally {
//                readers.close()
//                this.readers = null
//            }
//        }
//    }
//
//
//    @Throws(ExecutionException::class)
//    fun doExit(): Boolean {
//        val sendExit = connectedClient.isDone
//        if (sendExit) {
//            ExecutionResult.get(connectedClient).sendMessage<ProtocolResponses.CompositeResponse>(
//                ProtobufMessageFactory.exit(),
//                null,
//                null
//            )
//        }
//
//        return sendExit
//    }
//
//    @Throws(ExecutionException::class)
//    fun addSymbolsFile(symbols: File, module: File?) {
//        var modulePath = ""
//        val command: String = if (module != null) {
//            modulePath = module.absolutePath
//            "target module add \"" + module.absolutePath + "\" -s \"" + symbols.absolutePath + "\""
//
//        } else {
//            "target symbols add \"" + symbols.absolutePath + "\""
//        }
//
//
//
//        getProtobufClient().sendMessageAndWaitForReply(
//            ProtobufMessageFactory.handleConsoleCommand(-1L, -1, command),
//            ProtocolResponses.HandleConsoleCommandResponse::class.java,
//            ThrowIfNotValid(
//                DebuggerBundle.message(
//                    "error.cannot.add.symbols",
//                    *arrayOf<Any>(symbols.absolutePath, modulePath)
//                )
//            )
//        )
//
//    }
//
//    @Throws(ExecutionException::class)
//    protected fun createDebugProcessHandler(
//        commandLine: GeneralCommandLine,
//        config: DebuggerDriverConfiguration
//    ): BaseProcessHandler<*> {
//
//
//        val handler = config.createDebugProcessHandler(commandLine)
//        val process = handler.process
//        LOG.info("[PID ${process.pid()}] Debugger started: ${commandLine.commandLineString}")
//
//        handler.addProcessListener(object : ProcessAdapter() {
//            override fun processWillTerminate(event: ProcessEvent, willBeDestroyed: Boolean) {
//
//                if (process.isAlive && willBeDestroyed) {
//                    try {
//                        if (!doExit()) {
//                            return
//                        }
//                    } catch (e: ExecutionException) {
//                        LOG.warn(e)
//                        return
//                    }
//
//                    try {
//                        process.waitFor(1500L, TimeUnit.MILLISECONDS)
//                    } catch (e: InterruptedException) {
//                    }
//                }
//            }
//
//            override fun processTerminated(event: ProcessEvent) {
//
//
//                val exitCodeString =
//                    ProcessTerminatedListener.stringifyExitCode(
//                        config.getHostMachine().getOSType().toOS(),
//                        event.exitCode
//                    )
//                LOG.info("[PID ${process.pid()}] Debugger exited with code $exitCodeString")
//
//                if (process.isAlive && OSProcessHandler.processCanBeKilledByOS(process)) {
//                    OSProcessUtil.killProcess(process)
//                }
//            }
//        })
//
//
//
//        return handler
//    }
//
//
//    private fun traceState(eventSpan: EventSpan?) {
//        if (this.stateSpan != null) {
//            this.stateSpan!!.close()
//        }
//        this.stateSpan = eventSpan
//    }
//
//    fun supportsMemoryWrite(): Boolean {
//        return false
//    }
//
//    @Throws(ExecutionException::class, DebuggerCommandException::class)
//    fun writeMemory(address: Address, bytes: ByteArray?) {
//
//        throw UnsupportedOperationException("Memory write is not supported")
//    }
//
//    fun supportsRegisters(): Boolean {
//        return false
//    }
//
//    @Throws(ExecutionException::class, DebuggerCommandException::class)
//    fun getRegisters(thread: LLThread, frame: LLFrame): List<LLValue> {
//
//        throw UnsupportedOperationException("Registers are not supported")
//    }
//
//
//    @Throws(ExecutionException::class, DebuggerCommandException::class)
//    fun getRegisters(thread: LLThread, frame: LLFrame, registerNames: Set<String>): List<LLValue> {
//        // Since registers are not supported, this method should always throw the same exception
//        throw UnsupportedOperationException("Registers are not supported")
//    }
//
//    @Throws(ExecutionException::class, DebuggerCommandException::class)
//    fun getArchitecture(): String? {
//        return null
//    }
//
//    fun getState(): TargetState {
//
//        return stateTransition.currentState
//    }
//
//    @Throws(ExecutionException::class, DebuggerCommandException::class)
//    fun getRegisterSets(): List<LLRegisterSet> {
//
//        return emptyList()
//    }
//
//    fun extractRegisterName(expression: String): String? {
//
//        return if (expression.startsWith("$")) {
//            for (i in 1 until expression.length) {
//                val c = expression[i]
//                if (!Character.isLetterOrDigit(c) && c != '_') {
//                    return null
//                }
//            }
//            expression.substring(1)
//        } else {
//            null
//        }
//    }
//
//    fun setExpirable(expirable: Expirable?) {}
//
//    @Throws(ExecutionException::class)
//    private fun addPathMapping(mappings: List<PathMapping>, useTargetSourceMap: Boolean, index: Int = -1) {
//        if (mappings.isNotEmpty()) {
//            val builder = StringBuilder()
//            if (index >= 0) {
//                builder.append(index).append(' ')
//            }
//            mappings.forEach { (from, to) ->
//                builder.append("\"").append(from).append("\" ")
//                builder.append("\"").append(to).append("\" ")
//            }
//            val command = if (useTargetSourceMap) {
//                if (index >= 0) "settings insert-before target.source-map " else "settings append target.source-map "
//            } else {
//                if (index >= 0) "target modules search-paths insert " else "target modules search-paths add "
//            }
//            protobufServer.sendMessageAndWaitForReply(
//                ProtobufMessageFactory.handleConsoleCommand(-1L, -1, command + builder),
//                ProtocolResponses.HandleConsoleCommandResponse::class.java,
//                ThrowIfNotValid(
//                    DebuggerBundle.message(
//                        "debug.command.error.cannotAddModulesSearchPaths",
//                        arrayOfNulls<Any>(0)
//                    )
//                ),
//                0L
//            )
//        }
//    }
//
//    @Throws(ExecutionException::class)
//    fun addPathMapping(index: Int, from: String, to: String) {
//
//        val convertedFrom: String = this.configuration.convertToProjectModelPath(from)
//        val convertedTo: String = this.configuration.convertToProjectModelPath(to)
//        val pathMapping = PathMapping(convertedFrom, convertedTo)
//        val list = listOf(pathMapping)
//        this.addPathMapping(list,true,  index,)
//    }
//
//    fun addForcedFileMapping(index: Int, from: String, hash: DebuggerSourceFileHash?, to: String) {
//        if ((this.capabilities and 1L) != 0L && hash != null) {
//            this.addPathMapping(
//                index, getSourceFileHashSchema(
//                    hash
//                ) + hash.hash, to
//            )
//        } else {
//            this.addPathMapping(index, from, to)
//        }
//
//    }
//
//    fun getDisasmFlavor(): DisasmFlavor? {
//        return null
//    }
//
//    @Throws(ExecutionException::class, DebuggerCommandException::class)
//    fun setDisasmFlavor(flavor: DisasmFlavor) {
//
//        throw UnsupportedOperationException("Disassembly flavor change is not supported")
//    }
//
//    fun supportsCommandCancellation(): Boolean {
//        return false
//    }
//
//    @Throws(ExecutionException::class, DebuggerCommandException::class)
//    fun freezeThread(thread: LLThread) {
//
//        throw UnsupportedOperationException("Freeze thread is not supported")
//    }
//
//    @Throws(ExecutionException::class, DebuggerCommandException::class)
//    fun unfreezeAllThreads(thread: LLThread) {
//
//        throw java.lang.UnsupportedOperationException("Unfreeze thread is not supported")
//    }
//
//    @Throws(ExecutionException::class, DebuggerCommandException::class)
//    fun unfreezeThread(thread: LLThread) {
//
//        throw UnsupportedOperationException("Unfreeze thread is not supported")
//    }
//
//    @Throws(ExecutionException::class, DebuggerCommandException::class)
//    fun freezeOtherThreads(thread: LLThread) {
//
//        throw UnsupportedOperationException("Freeze thread is not supported")
//    }
//
//    @Throws(ExecutionException::class)
//    private fun removeTemporaryBreakpoints(): Set<Int> {
//        val breakpointIds: LinkedHashSet<Int> = LinkedHashSet()
//        this.temporaryBreakpoints.drainTo(breakpointIds)
//
//
//        for (id in breakpointIds) {
//            getProtobufClient().sendMessage(
//                createRemoveBreakpointRequest(id),
//                ProtocolResponses.RemoveBreakpointResponse::class.java
//            ) {
//                if (it.status.success) {
//                    LOG.error(
//                        "Couldn't remove breakpoint. error: " + it.status.getErrorMessage()
//                    )
//                }
//
//            }
//        }
//
//        return breakpointIds
//    }
//
//    @Contract(pure = true)
//    protected fun createTypeSummaryConsoleCommand(
//        summaryString: String,
//        category: String?,
//        vararg typeNames: String
//    ): String {
//
//
//        return String.format(
//            "type summary add --skip-pointers --summary-string %s --category %s %s",
//            stringify(summaryString),
//            category ?: "default",
//
//            typeNames.joinToString(" ") {
//                stringify(it)
//            }
//
//        )
//    }
//
//    @Throws(ExecutionException::class)
//    protected fun lldbSet(setting: String, enabled: Boolean) {
//        lldbSet(setting, if (enabled) "true" else "false")
//    }
//
//    @Throws(ExecutionException::class)
//    private fun lldbSetStepIntoNoDebug(value: Boolean) {
//        lldbSet("target.process.thread.step-in-avoid-nodebug", !value)
//    }
//
//    @Throws(ExecutionException::class)
//    private fun lldbSetStepOutNoDebug(value: Boolean) {
//        this.lldbSet("target.process.thread.step-out-avoid-nodebug", !value)
//    }
//
//    @Throws(ProtobufTimedOutException::class)
//    private fun executeConsoleCommandAndHandleOutput(command: @NonNls String) {
//
//        protobufServer.sendMessageAndWaitUntilSent(
//            ProtobufMessageFactory.handleConsoleCommand(-1L, -1, command),
//            ProtocolResponses.HandleConsoleCommandResponse::class.java
//        ) { res ->
//            if (res.standardOutput != null || res.errorOutput != null) {
//                val message = DebuggerBundle.message(
//                    "error.during.data.formatters.setup.0.1",
//                    *arrayOf<Any>(
//                        if (res.standardOutput != null) """
//
//     ${res.standardOutput}
//     """.trimIndent() else "",
//                        if (res.errorOutput != null) """
//
//     ${res.errorOutput}
//     """.trimIndent() else ""
//                    )
//                )
//                handleTargetOutput(message, ProcessOutputTypes.SYSTEM)
//            }
//        }
//    }
//
//
//
//    private fun haveConnection(version: String, capabilities: Long) {
//
//        this.handlePrompt()
//        try {
//            this.version = parseVersion(version)
//            this.capabilities = capabilities
//            this.lldbSetStepIntoNoDebug(false)
//            this.lldbSetStepOutNoDebug(false)
//            if (LOG.isTraceEnabled) {
//                val logPath: String = getLogDir().toString() + "/lldb.log"
//                protobufServer.sendMessageAndWaitUntilSent(
//                    ProtobufMessageFactory.handleConsoleCommand(
//                        -1L, -1,
//                        "log enable --timestamp --sequence --pid-tid -f $logPath lldb all"
//                    ),
//                    ProtocolResponses.HandleConsoleCommandResponse::class.java
//                ) { }
//            }
//            val typeSummaryConsoleCommands = arrayOf(
//                this.createTypeSummaryConsoleCommand(
//                    "\${var%d} \${var}",
//                    "cplusplus",
//                    "char",
//                    "signed char"
//                ), this.createTypeSummaryConsoleCommand("\${var%u} \${var}", "cplusplus", "unsigned char")
//            )
//            val var6 = typeSummaryConsoleCommands.size
//            for (var7 in 0 until var6) {
//                val typeSummaryConsoleCommand = typeSummaryConsoleCommands[var7]
//                protobufServer.sendMessageAndWaitUntilSent(
//                    ProtobufMessageFactory.handleConsoleCommand(-1L, -1, typeSummaryConsoleCommand),
//                    ProtocolResponses.HandleConsoleCommandResponse::class.java
//                ) { }
//            }
//            protobufServer.sendMessageAndWaitUntilSent(
//                ProtobufMessageFactory.handleConsoleCommand(-1L, -1, "type category enable objc"),
//                ProtocolResponses.HandleConsoleCommandResponse::class.java
//            ) { }
//            protobufServer.sendMessageAndWaitUntilSent(
//                ProtobufMessageFactory.setValuesFilteringEnabled(
//                    DebuggerSettings.getInstance().isValuesFilterEnabled()
//                ),
//                ProtocolResponses.SetValueFilteringPolicyResponse::class.java,
//                ThrowIfNotValid(
//                    DebuggerBundle.message(
//                        "error.cannot.set.values.filtering.policy",
//                        arrayOfNulls<Any>(0)
//                    )
//                )
//            )
//            if (this.commandLine.getUserData(ENABLE_STL_RENDERERS) == true) {
//                this.executeConsoleCommandAndHandleOutput("command script import lldb_formatters")
//            }
//            connectedClient.complete(protobufServer)
//        } catch (e: Exception) {
//            connectedClient.completeExceptionally(e)
//        }
//    }
//
//    private fun handleMessage(generatedMessage: Message) {
//
//        if (generatedMessage is Broadcasts.InitializedEvent) {
//            this.haveConnection(generatedMessage.version, generatedMessage.capabilities)
//        } else if (generatedMessage is Broadcasts.ProcessExitedEvent) {
//            val exitCode: Int = generatedMessage.exitCode
//            val exitDescription: String? =
//                if (generatedMessage.hasExitDescription()) generatedMessage.getExitDescription() else null
//            var exitStatus = ExitStatus(exitCode, exitDescription)
//            if (SystemInfo.isMac && exitCode == 0 && exitDescription != null && exitDescription.startsWith("Terminated due to signal ")) {
//                val signal = StringUtil.parseInt(
//                    exitDescription.substring("Terminated due to signal ".length).trim { it <= ' ' }, -1
//                )
//                if (signal > 0) {
//                    exitStatus = ExitStatus.fromSignal(signal)
//                }
//            }
//            this.handleTargetTerminated(exitStatus)
//        } else if (generatedMessage is Broadcasts.ProcessRunningEvent) {
//            val attachedTo = myAsyncAttachingTo
//            myAsyncAttachingTo = null
//            if (attachedTo != null) {
//                this.handleAttached(attachedTo)
//            }
//            this.stoppedThread = null
//            this.handleRunning()
//        } else if (generatedMessage is Broadcasts.ProcessInterruptedEvent) {
//            val interruptedBroadcast: Broadcasts.ProcessInterruptedEvent =
//                generatedMessage
//            var temporaryBreakpoints: Set<Int?> = emptySet<Int>()
//            try {
//                temporaryBreakpoints = this.removeTemporaryBreakpoints()
//            } catch (ex: ExecutionException) {
//                LOG.error(ex)
//            }
//            val lldbThread: Model.Thread = interruptedBroadcast.interruptedThread
//            val thread: LLThread = this.newLLThread(lldbThread)
//            val frame = newLLFrame(interruptedBroadcast.currentFrame)
//            val stopReasonInfo: Model.ThreadStopInfo? =
//                lldbThread.stopInfo
//            val stopReason = stopReasonInfo?.reason
//            val stopPlace: StopPlace = if (stopReasonInfo != null && stopReasonInfo.hasReturnValue()) {
//                StopPlace(thread, frame, createLLValue(stopReasonInfo.returnValue, null as String?))
//            } else {
//                StopPlace(thread, frame)
//            }
//            this.stoppedThread = stopPlace.thread
//            if (stopReason === Model.StopReason.STOP_REASON_BREAKPOINT && !temporaryBreakpoints.contains(
//                    stopReasonInfo.exceptionCode
//                )
//            ) {
//                this.handleBreakpoint(stopPlace, stopReasonInfo.exceptionCode)
//            } else if (stopReason === Model.StopReason.STOP_REASON_WATCHPOINT) {
//                this.handleWatchpoint(stopPlace, stopReasonInfo.exceptionCode)
//            } else if (stopReason === Model.StopReason.STOP_REASON_SIGNAL) {
//                val signal: Int = stopReasonInfo.signalNumber
//                if (isTargetTerminationSignal(signal)) {
//                    this.handleTargetTerminated(ExitStatus.fromSignal(signal))
//                } else {
//                    val signalName = StringUtil.defaultIfEmpty(stopReasonInfo.getSignalName(), signal.toString())
//                    this.handleSignal(stopPlace, signalName, stopReasonInfo.description)
//                }
//            } else if (stopReason === Model.StopReason.STOP_REASON_EXCEPTION) {
//                this.handleException(
//                    stopPlace,
//                    Address.fromUnsignedLong(stopReasonInfo.address),
//                    stopReasonInfo.location.filePath,
//                    createSourceFileHash(
//                        stopReasonInfo.location.hashAlgorithm,
//                        stopReasonInfo.location.hashValue
//                    ),
//                    stopReasonInfo.location.line,
//                    stopReasonInfo.description
//                )
//            } else {
//                this.handleInterrupted(stopPlace)
//            }
//        } else if (generatedMessage is Broadcasts.PromptChangedEvent) {
//            this.handlePrompt(generatedMessage.newPrompt)
//        } else if (generatedMessage is Broadcasts.ReadyForCommandsEvent) {
//            this.handlePrompt(!generatedMessage.isReady)
//        } else if (generatedMessage is Broadcasts.CommandInterpreterMessageEvent) {
//            this.handleDebuggerOutput(
//                asNlsSafe(generatedMessage.getMessage()),
//                ProcessOutputTypes.STDOUT
//            )
//        } else if (generatedMessage is Broadcasts.ProcessOutputEvent) {
//            val outputBroadcast: Broadcasts.ProcessOutputEvent =
//                generatedMessage
//            handleTargetOutput(
//                asNlsSafe(outputBroadcast.getText()),
//                outputType2ProcessOutputKey(outputBroadcast.getOutputType())
//            )
//        } else if (generatedMessage is Broadcasts.LogMessageEvent) {
//            val message: String = generatedMessage.getMessage()
//            LOG.info(message)
//        } else if (generatedMessage is Broadcasts.ModulesLoadedEvent) {
//            val msg: Broadcasts.ModulesLoadedEvent = generatedMessage
//            val lst: List<LLModule> = ContainerUtil.map(msg.moduleNamesList) { module ->
//                LLModule(
//                    module.trim()
//                )
//            }
//            this.handleModulesLoaded(lst)
//        } else {
//            val locations: List<*>
//            if (generatedMessage is Broadcasts.BreakpointAddedEvent) {
//                val msg: Broadcasts.BreakpointAddedEvent = generatedMessage
//                val brk: Model.Breakpoint = msg.breakpoint
//                locations = msg.locationsList
//                val result = makeBreakpoint(brk, locations)
//                this.handleBreakpointAdded(result.breakpoint)
//                this.handleBreakpointLocationsUpdated(result.breakpoint.id, result.breakpointLocations)
//            } else {
//                val breakpointId: Int
//                if (generatedMessage is Broadcasts.BreakpointRemovedEvent) {
//                    val msg: Broadcasts.BreakpointRemovedEvent =
//                        generatedMessage
//                    breakpointId = msg.breakpointId
//                    this.handleBreakpointRemoved(breakpointId)
//                } else {
//                    val brkLocations: List<*>
//                    when (generatedMessage) {
//                        is Broadcasts.BreakpointLocationsAddedEvent -> {
//                            val msg: Broadcasts.BreakpointLocationsAddedEvent =
//                                generatedMessage
//                            breakpointId = msg.breakpointId
//                            locations = msg.locationsList
//                            brkLocations = ContainerUtil.mapNotNull(
//                                locations
//                            ) { loc ->
//                                makeLocation(
//                                    breakpointId,
//                                    loc
//                                )
//                            }
//                            this.handleBreakpointLocationsUpdated(breakpointId, brkLocations)
//                        }
//
//                        is Broadcasts.BreakpointLocationsRemovedEvent -> {
//                            val msg: Broadcasts.BreakpointLocationsRemovedEvent =
//                                generatedMessage
//                            breakpointId = msg.breakpointId
//                            locations = msg.locationIdsList
//                            brkLocations = ContainerUtil.map(
//                                locations
//                            ) { id ->
//                                makeBreakpointLocationCanonicalName(
//                                    breakpointId,
//                                    id
//                                )
//                            }
//                            this.handleBreakpointLocationsRemoved(breakpointId, brkLocations)
//                        }
//
//                        is Broadcasts.BreakpointLocationsResolvedEvent -> {
//                            val msg: Broadcasts.BreakpointLocationsResolvedEvent =
//                                generatedMessage
//                            breakpointId = msg.breakpointId
//                            locations = msg.locationsList
//                            brkLocations = ContainerUtil.mapNotNull(
//                                locations
//                            ) { loc ->
//                                makeLocation(
//                                    breakpointId,
//                                    loc
//                                )
//                            }
//                            this.handleBreakpointLocationsUpdated(breakpointId, brkLocations)
//                        }
//
//                        is Broadcasts.BreakpointChangedEvent -> {
//                            val msg: Broadcasts.BreakpointChangedEvent =
//                                generatedMessage
//                            val eventType: Model.BreakpointEventType = msg.getEventType()
//                            val brk: Model.Breakpoint = msg.breakpoint
//                            brkLocations = msg.locationsList
//                            val bRes = makeBreakpoint(
//                                brk,
//                                brkLocations
//                            )
//                            val breakpoint: LLBreakpoint = bRes.breakpoint
//                            this.handleBreakpointUpdated(breakpoint)
//                            this.handleBreakpointLocationsReplaced(breakpoint.id, bRes.breakpointLocations)
//                        }
//
//                        is Broadcasts.SelectedFrameChangedEvent -> {
//                            val msg: Broadcasts.SelectedFrameChangedEvent =
//                                generatedMessage
//                            val thread: LLThread = this.newLLThread(msg.thread)
//                            val frame = newLLFrame(msg.frame)
//                            this.handleSelectedFrameChanged(thread, frame)
//                        }
//
//                        is Broadcasts.SymbolsDownloadStartedEvent -> {
//                            val msg: Broadcasts.SymbolsDownloadStartedEvent =
//                                generatedMessage
//                            this.handleSymbolsDownloadStarted(msg.title, msg.getDetails())
//                        }
//
//                        is Broadcasts.SymbolsDownloadProgressEvent -> {
//                            val msg: Broadcasts.SymbolsDownloadProgressEvent =
//                                generatedMessage
//                            this.handleSymbolsDownloadProgress(msg.progressPercent)
//                        }
//
//                        is Broadcasts.SymbolsDownloadFinishedEvent -> {
//                            this.handleSymbolsDownloadFinished()
//                        }
//                    }
//                }
//            }
//        }
//    }
//
//    override fun consume(generatedMessage: Message) {
//
//        val ignored = EventSpan(
//            "debug",
//            { "consume (" + generatedMessage.javaClass.getSimpleName() + ")" },
//            { generatedMessage.toString() })
//
//        try {
//            this.handleMessage(generatedMessage)
//        } catch (t: Throwable) {
//            try {
//                ignored.close()
//            } catch (tt: Throwable) {
//                t.addSuppressed(tt)
//            }
//            throw t
//        }
//
//        ignored.close()
//    }
//
//
//    override fun setRichValueDescriptionEnabled(enable: Boolean) {
//        val command = String.format("jb_renderers_set_markup %d", if (enable) 1 else 0)
//        executeInterpreterCommand(-1L, -1, command)
//    }
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//    abstract inner class Inferior(private val myId: Int = 1) {
//        private val myIsRunning = AtomicBoolean()
//
//        fun getId(): Int {
//            return myId
//        }
//
//        fun getDriver(): DebuggerDriver {
//
//            return this@DebuggerDriver
//        }
//
//        @Throws(ExecutionException::class)
//        fun start(): Long {
//            if (!myIsRunning.compareAndSet(false, true)) {
//                throw IllegalStateException("started already")
//            }
//            return startImpl()
//        }
//
//        @Throws(ExecutionException::class)
//        fun detach() {
//            if (myIsRunning.compareAndSet(true, false)) {
//                detachImpl()
//            }
//        }
//
//        @Throws(ExecutionException::class)
//        fun destroy(): Boolean {
//            if (!myIsRunning.compareAndSet(true, false)) {
//                return false
//            }
//            this@DebuggerDriver.setState(TargetState.FINISHING)
//            return destroyImpl()
//        }
//
//        protected abstract fun startImpl(): Long
//
//        protected abstract fun detachImpl()
//
//        protected abstract fun destroyImpl(): Boolean
//    }
//
//
//    private inner class LLValueDataLoader {
//        @Throws(ExecutionException::class, DebuggerCommandException::class)
//        fun loadData(value: LLValue): LLValueData {
//
//            val req = ProtobufMessageFactory.getValueData(
//                valId(
//                    value
//                ), 256
//            )
//            val lldbDataRef = Ref.create<Model.ValueData>()
//            val exception = Ref.create<DebuggerCommandException>()
//            getProtobufClient().sendMessageAndWaitForReply(
//                req, ProtocolResponses.GetValueDataResponse::class.java
//            ) {
//                val commonResponse = it.status
//                if (!commonResponse.success) {
//                    exception.set(DebuggerCommandException(commonResponse.errorMessage))
//                } else {
//                    lldbDataRef.set(it.data)
//                }
//            }
//
//            if (!exception.isNull) {
//                throw exception.get()
//            } else {
//                val lldbData = lldbDataRef.get()
//                return LLValueData(
//                    lldbData.value,
//                    lldbData.summary,
//                    lldbData.hasExtendedDescription,
//                    lldbData.hasChildren,
//                    lldbData.isSynthetic
//                )
//            }
//        }
//    }
//
//    companion object {
//        val USE_EXTERNAL_CONSOLE_KEY = Key.create<Boolean>("USE_EXTERNAL_CONSOLE")
//        val LOG = Logger.getInstance("#" + DebuggerDriver::class.java.getPackage().name)
//
//        private fun convertInstructionList(
//            instructions: List<Model.Instruction>,
//            functionName: String?,
//            functionStart: Long
//        ): MutableList<LLInstruction> {
//
//            val result = ArrayList<LLInstruction>(instructions.size)
//            instructions.forEach { instruction ->
//                val addr = instruction.address
//                val functionOffset =
//                    if (functionName != null) LLSymbolOffset(functionName, addr - functionStart) else null
//                val llInstruction = LLInstruction.create(
//                    Address.fromUnsignedLong(addr),
//                    instruction.opcodeBytes,
//                    instruction.mnemonic,
//                    instruction.operands,
//                    instruction.comment,
//                    functionOffset
//                )
//                result.add(llInstruction)
//            }
//
//
//            return result
//        }
//
//        protected fun getTimeoutMs(): Int {
//            return Registry.intValue("cangjie.debugger.timeout", 30000)
//        }
//
//        private fun outputType2ProcessOutputKey(outputType: Model.OutputType): Key<*> {
//            return if (outputType === Model.OutputType.OUTPUT_TYPE_STDOUT) ProcessOutputTypes.STDOUT else ProcessOutputTypes.STDERR
//        }
//
//        private fun getLogDir(): File {
//            return File(if (ApplicationManager.getApplication().isUnitTestMode) PathManager.getSystemPath() + "/testlog" else PathManager.getLogPath())
//        }
//
//        private fun getSourceFileHashSchema(hash: DebuggerSourceFileHash): String {
//            return when (hash.type) {
//                DebuggerSourceFileHash.Type.MD5 -> "md5://"
//                DebuggerSourceFileHash.Type.SHA1 -> "sha1://"
//                DebuggerSourceFileHash.Type.SHA256 -> "sha256://"
//            }
//
//        }
//
//        @Throws(ExecutionException::class, DebuggerCommandException::class)
//        private fun getLLValueData(value: LLValue): LLValueData {
//            return synchronized(value) {
//                val loader = value.getUserData(LLVALUE_DATA_LOADER)
//                if (loader != null) {
//                    val data = loader.loadData(value)
//                    value.putUserData(LLVALUE_DATA_LOADER, null)
//                    value.putUserData(LLVALUE_DATA, data)
//                    return@synchronized data
//                }
//
//                return@synchronized value.getUserData<LLValueData?>(LLVALUE_DATA)
//                    ?: throw ExecutionException(
//                        DebuggerBundle.message(
//                            "error.variable.not.initialized",
//                            value
//                        )
//                    )
//            }
//        }
//
//        @Throws(ExecutionException::class)
//        private fun getLocalPlatform(): String {
//            return if (SystemInfo.isWindows) {
//                "remote-windows"
//            } else if (SystemInfo.isMac) {
//                "remote-macosx"
//            } else if (SystemInfo.isLinux) {
//                "remote-linux"
//            } else if (SystemInfo.isFreeBSD) {
//                "remote-freebsd"
//            } else {
//                throw ExecutionException(DebuggerBundle.message("error.unsupported.os"))
//            }
//        }
//
//
//        val ENABLE_STL_RENDERERS: Key<Boolean> = Key.create("DebuggerDriver.synthethicsEnabled")
//        private val LLVALUE_ID: Key<Int> = Key.create("DebuggerDriver.LLVALUE_ID")
//        private val LLVALUE_DATA_LOADER: Key<LLValueDataLoader> =
//            Key.create("DebuggerDriver.LLVALUE_DATA_LOADER")
//        private val LLVALUE_DATA: Key<LLValueData> = Key.create("DebuggerDriver.LLVALUE_DATA")
//        private val CHILDREN_COUNT_CACHE: Key<Int> = Key.create("DebuggerDriver.CHILDREN_COUNT_CACHE")
//        const val NO_RESULT = "<no result>"
//        private const val LOCKED_DEVICE_RESPONSE = "process launch failed: Locked"
//        private const val TERMINATED_DUE_TO_SIGNAL = "Terminated due to signal "
//        private fun getArchitectureType(architectureType: String?): String {
//            var lldbArchitecture = StringUtil.notNullize(architectureType)
//            if (lldbArchitecture == ArchitectureType.UNKNOWN.getId()) {
//                lldbArchitecture = ""
//            }
//
//            return lldbArchitecture
//        }
//
//        private fun createRemoveBreakpointRequest(num: Int): Protocol.CompositeRequest {
//            return ProtobufMessageFactory.removeBreakpoint(num)
//        }
//
//        private fun makeLocation(breakpointId: Int, loc: Model.BreakpointLocation): LLBreakpointLocation? {
//
//            return if (!loc.isResolved) {
//                null
//            } else {
//                val id: String = makeBreakpointLocationCanonicalName(breakpointId, loc.id)
//                val address = Address.fromUnsignedLong(loc.address)
//                val location = FileLocation(loc.location.filePath, loc.location.line - 1)
//                LLBreakpointLocation(id, address, location)
//            }
//        }
//
//        private fun makeBreakpointLocationCanonicalName(breakpointId: Int, locationId: Int): String {
//
//            return "$breakpointId.$locationId"
//        }
//
//        private fun makeBreakpoint(
//            breakpoint: Model.Breakpoint,
//            breakpointLocations: List<Model.BreakpointLocation>
//        ): AddBreakpointResult {
//
//            val origFilePath =
//                if (breakpoint.hasOriginalLocation()) breakpoint.originalLocation.filePath else "<address>"
//            val origLine = if (breakpoint.hasOriginalLocation()) breakpoint.originalLocation.line else 0
//            val condition: String? = breakpoint.getCondition()
//            val llBreakpoint = LLBreakpoint(breakpoint.id, origFilePath, origLine - 1, condition)
//            val locationList: List<LLBreakpointLocation> = ContainerUtil.mapNotNull(breakpointLocations) { loc ->
//                makeLocation(
//                    breakpoint.id,
//                    loc
//                )
//            }
//            return AddBreakpointResult(llBreakpoint, locationList)
//        }
//
//
//        private fun createSourceFileHash(type: Model.HashAlgorithm?, hash: String?): DebuggerSourceFileHash? {
//            return if (type != null && hash != null) {
//                val t: DebuggerSourceFileHash.Type = when (type) {
//                    Model.HashAlgorithm.HASH_ALGORITHM_MD5 -> DebuggerSourceFileHash.Type.MD5
//                    Model.HashAlgorithm.HASH_ALGORITHM_SHA1 -> DebuggerSourceFileHash.Type.SHA1
//                    Model.HashAlgorithm.HASH_ALGORITHM_SHA256 -> DebuggerSourceFileHash.Type.SHA256
//                    else -> return null
//                }
//                DebuggerSourceFileHash(t, hash)
//            } else {
//                null
//            }
//        }
//
//
//        @Throws(ExecutionException::class)
//        private fun valId(value: LLValue): Int {
//
//            return value.getUserData(LLVALUE_ID) as Int
//        }
//
//        private fun logOutputSummary(@NonNls description: String, @NonNls text: String, type: Key<*>) {
//
//            if (LOG.isTraceEnabled) {
//                val length = text.length
//                var shortText = stringify(StringUtil.shortenTextWithEllipsis(text, 80, 0))
//                if (length > 80) {
//                    shortText += " ($length chars)"
//                }
//
//                LOG.trace("$description [$type]: $shortText")
//            }
//        }
//
//
//        private fun parseEscapedByte(s: String, idx: Int, bs: ByteArrayOutputStream): Int {
//            if (idx + 4 > s.length || s[idx] != '\\') return -1
//
//            return try {
//                val code = s.substring(idx + 1, idx + 4).toInt(8)
//                bs.write(code)
//                idx + 4
//            } catch (e: NumberFormatException) {
//                -1
//            }
//        }
//
//
//        @JvmStatic
//        protected fun asNlsSafe(s: @NlsSafe String): @NlsSafe String {
//            return s
//        }
//
//
//        protected fun getLoadTimeoutMs(): Int {
//            return Registry.intValue("cangjie.debugger.timeout.load", 90000)
//        }
//
//        protected fun getEvaluationTimeoutMs(): Int {
//            return Registry.intValue("cangjie.debugger.timeout.evaluate", 30000)
//        }
//
//
//
//
//        @Throws(ExecutionException::class)
//        fun parseAddress(str: String): Address {
//
//
//            return try {
//                Address.parseHexString(str)
//            } catch (numberFormatException: NumberFormatException) {
//                throw ExecutionException(numberFormatException)
//            }
//        }
//
//        @Contract("_, !null -> !null")
//        fun parseAddress(str: String, defaultValue: Address): Address {
//
//            return try {
//                Address.parseHexString(str)
//            } catch (numberFormatException: NumberFormatException) {
//                defaultValue
//            }
//        }
//
//        fun unescapeString(s: String): String {
//
//            val buffer = StringBuilder(s.length)
//            val bs = ByteArrayOutputStream()
//            var escaped = false
//            var idx = 0
//            while (idx < s.length) {
//                val ch = s[idx]
//                if (!escaped) {
//                    if (ch == '\\') {
//                        escaped = true
//                    } else {
//                        buffer.append(ch)
//                    }
//                } else {
//                    when (ch) {
//                        '"' -> buffer.append('"')
//                        '\'' -> buffer.append('\'')
//                        '\\' -> buffer.append('\\')
//                        'a' -> buffer.append('\u0007')
//                        'b' -> buffer.append('\b')
//                        'e' -> buffer.append('\u001b')
//                        'f' -> buffer.append("\u000c")
//                        'n' -> buffer.append('\n')
//                        'r' -> buffer.append('\r')
//                        't' -> buffer.append('\t')
//                        else -> {
//                            var i = idx - 1
//                            bs.reset()
//                            while (parseEscapedByte(s, i, bs).also { i = it } != -1) {
//                                idx = i
//                            }
//                            if (bs.size() > 0) {
//                                buffer.append(String(bs.toByteArray(), StandardCharsets.UTF_8))
//                                --idx
//                            } else {
//                                buffer.append('\\')
//                                buffer.append(ch)
//                            }
//                        }
//                    }
//                    escaped = false
//                }
//                ++idx
//            }
//            if (escaped) {
//                buffer.append('\\')
//            }
//
//            return buffer.toString()
//        }
//
//        fun stringify(s: String): String {
//
//            val buffer = StringBuilder(s.length + 2).append('"')
//            StringUtil.escapeStringCharacters(s.length, s, "\"", buffer)
//
//            return buffer.append('"').toString()
//        }
//
//        protected fun format(@PrintFormat format: @NonNls String, vararg args: Any?): String {
//            return String.format(Locale.ROOT, format, *args)
//        }
//
//        @JvmStatic
//        protected fun isTargetTerminationSignal(signal: Int): Boolean {
//            return signal == 15 || signal == 9
//        }
//
//        protected fun isTargetTerminationSignal(name: String?): Boolean {
//            return "SIGTERM" == name || "SIGKILL" == name
//        }
//
//    }
//
//
//
//}
//
//
//
