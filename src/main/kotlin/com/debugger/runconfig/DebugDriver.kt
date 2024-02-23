package com.debugger.runconfig

import com.debugger.backend.CjBreakpoint
import com.debugger.runconfig.message.MessageHandler
import com.huawei.cangjie.cjpm.project.settings.cangjieSettings
import com.huawei.cangjie.cjpm.toolchain.tools.cjc
//import com.huawei.cangjie.idea.project.CangJieProjectManager


import com.intellij.execution.ExecutionException
import com.intellij.execution.process.BaseProcessHandler
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.NlsContexts
import com.intellij.openapi.util.Ref
import com.intellij.util.Consumer
import com.intellij.util.concurrency.QueueProcessor
import com.intellij.util.text.SemVer
import com.intellij.xdebugger.XDebugSession
import dap.event.*
import dap.protocol.ProtocolMessage
import dap.request.*
import dap.response.*
import dap.type.*
import dap.type.arguments.*
import dap.type.body.RunInTerminalResponseBody
import dap.type.serializer.format
import kotlinx.serialization.encodeToString
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.IOException
import java.io.Writer
import java.net.Socket
import java.net.SocketException
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicBoolean

const val TWO_CRLF = "\r\n\r\n"
const val ONE_CRLF = "\r\n"
inline fun <reified T : ProtocolMessage> Writer.write(message: T) {
    val json = format.encodeToString(message)
    val size = json.toByteArray().size
    this.write("Content-Length: ${size}${TWO_CRLF}${json}")
    this.flush()

}


class DebugDriver(
    val session: XDebugSession, val handler: MessageHandler
) : Consumer<ProtocolMessage> {

    val debugProcessHandler = CangJieDebuggerServerManager.getDebugServerProcess()


    private var seq = 1

    private var requestSeq = 0

    private var socket: Socket? = null


//    private var socketChannel : SocketChannel

    var reader: BufferedReader? = null

    val myStateTransition: TargetStateTransition = TargetStateTransition(TargetState.NOT_READY, TargetState.NOT_READY)
    private val myHandlerProcessor: QueueProcessor<Runnable> = QueueProcessor.createRunnableQueueProcessor()

    private val myMessageHandler: MessageHandler = MessageHandler.createQueuedHandler(handler, myHandlerProcessor)

    private var writer: BufferedWriter? = null


//    val processInput: OutputStream

    val runState: Boolean
        get() = !debugProcessHandler.isProcessTerminated && !debugProcessHandler.isProcessTerminating && debugProcessHandler.isStartNotified
    private var socketContentState = AtomicBoolean(false)

    private val MAX_RETRY_COUNT = 10
    private val RETRY_DELAY_MS = 20L
    private val myConnectedClient: CompletableFuture<DapClent<Response>> = CompletableFuture()
    private var myDapClient: DapClent<Response>? = null

//    val thread = Thread {
//        val isWhile = AtomicBoolean(true)
//        while (isWhile.get()) {
//
//
//            if (socket?.isConnected == true) {
//                if (socketContentState.get()) {
//                    val message = read()
//
//                    this.myMessageHandler.handleMessage(message!!)
//                }
//
//            } else {
//                isWhile.set(false)
//            }
//
//
//        }
//    }

    val sessionid = UUID.randomUUID().toString()

//    private val latch = CountDownLatch(1)

    init {
//        if (runState) {
//            this.content()
//            this.initialize()
//
//        } else {
//            print("调试器启动失败")
//        }

        try {
            myDapClient = DapClent(CangJieDebuggerServerManager.DEBUGPORT, this@DebugDriver, getInitializeRequest())


//            myDapClient!!.waitFor()
        } catch (ioEx: IOException) {

            throw ExecutionException(ioEx)
        }


    }


    //    private fun unblockContent() {
//        latch.countDown() // This will unblock the content method
//    }


    /**
     * 发送配置完成
     */
    fun sendConfigurationDone() {
        val request = ConfigurationDoneRequest(
            seq = ++seq,

            )
        send(request)
    }


    fun sendScopes(frameId: Int): ScopesResponse {
        val request = ScopesRequest(
            seq = ++seq,
            arguments = ScopesArguments(
                frameId = frameId
            )
        )

        return sendMessageAndWaitForReply(request, ScopesResponse::class.java)
    }

    fun sendStacktrace(threadId: Long, levels: Int = 20, startFrame: Int = 0): StackTraceResponse {


        val request = StackTraceRequest(
            seq = ++seq,
            arguments = StackTraceArguments(
                threadId = threadId,
                levels = levels,
                startFrame = startFrame
            )
        )

        return sendMessageAndWaitForReply(request, StackTraceResponse::class.java)
//        send(request)
    }

//    /**
//     * 开启消息进程
//     */
//    private fun startReadMessageThread() {
////        启动一个kotlin线程
//
//        thread.start()
//    }

    //    @Synchronized
//    fun read(): ProtocolMessage? {
//        if (reader == null) return null
//        val str = reader!!.readLine()
//        val arr = str.split(":")
//
//        if (arr.size < 2) return null
//
//
//        val contentLength = arr[1].trim().toInt() + ONE_CRLF.length
////        读2位 把\r\n读掉
////        val charArray = CharArray(2)
////        reader.read(CharArray(2), 0, 2)
//
//        val charArray = CharArray(contentLength)
//        reader?.read(charArray, 0, contentLength)
//
//        val jsonstr = String(charArray).trim().trimEnd('\r', '\n', ' ', '\u0000')
//        val response = moshi.adapter(ProtocolMessage::class.java).fromJson(jsonstr)
//
//
////        seq = response?.seq ?: seq
//
//        if (response is Response) {
//            requestSeq = response.request_seq
//        }
//        LOG.info("接收消息：$response")
//
//        return response
//    }
    @Throws(ExecutionException::class)
    protected fun getDapClient(): DapClent<Response> {

        return ExecutionResult.get(myConnectedClient)
    }

    //    @Synchronized
    private inline fun <reified T> send(message: T): Response? where T : ProtocolMessage {
//        this.socket.write(`Content-Length: ${Buffer.byteLength(e, "utf8")}${d.TWO_CRLF}${e}`, "utf8")

//        writer?.write("Content-Length: ${message.toByteArray().size}$TWO_CRLF$message")
//        if (socketContentState.get()) {
//
//            LOG.info("发送消息：$message")
//
//
//            writer?.write(message)
//            writer?.flush()
//
//        }

        getDapClient().sendMessage<T>(message, null, null)


//        return read() as Response
        return null

    }


    fun sendLaunch() {

        val toolchain = session.project.cangjieSettings.toolchain
        val sdkVersion = toolchain?.cjc()?.version?.semver


        val mainexe = if (sdkVersion!! < SemVer.parseFromText("0.45.2")) {
            ((session.project.basePath + "/build/bin/main").toSystemIndependentPath())
        } else {
            ((session.project.basePath + "/build/debug/bin/main").toSystemIndependentPath())
        }
        val request = LaunchRequest(
            seq = ++seq, arguments = LaunchRequestArguments(
                buildBeforeLaunch = true,
                externalConsole = true,
                name = "Cangjie Debug (cjdb): launch",
                program = mainexe,
                request = MessageCommand.launch,
                type = "cangjieDebug",
                __sessionId = sessionid,
                __configurationTarget = 6
            )
        )

        send(request)
    }

    private fun sendInitializeData() {
        val init = InitializeRequest(
            InitializeRequestArguments(
                adapterID = "cangjieDebug",
                clientId = "idea",
                clientName = "Intellij IDEA",
                columnsStartAt1 = true,
                linesStartAt1 = true,
                locale = "zh-cn",
                pathFormat = PathFormat.Path,
                supportsInvalidatedEvent = true,
                supportsMemoryEvent = true,
                supportsArgsCanBeInterpretedByShell = true,
                supportsMemoryReferences = true,
                supportsProgressReporting = true,
                supportsRunInTerminalRequest = true,
                supportsStartDebuggingRequest = true,
                supportsVariablePaging = true,
                supportsVariableType = true
            )
        )
        send(init)
    }


    private fun sendDisconnect() {
        val request = DisconnectRequest(
            seq = ++seq,
            arguments = DisconnectArguments(
                restart = false,
                terminateDebuggee = true,
//                suspendDebuggee = false
            )
        )
        send(request)
    }

    fun disconnect() {

        try {
            sendDisconnect()
        } catch (e: SocketException) {
            LOG.info("调试器已经断开连接")
        }


        dispose()

    }

    /**
     * 释放资源
     */
    fun dispose() {

        socket?.close()
        reader?.close()
        writer?.close()
        socketContentState.set(false)

////        debugProcessHandler.destroyProcess()
////        如果debugProcessHandler没有被销毁，那么就销毁
//        XDebugSession会关闭该进程
//        if (!debugProcessHandler.isProcessTerminated) {
//            debugProcessHandler.destroyProcess()
//
//        }

    }


    @Synchronized
    fun sendShellProcessId(id: Long, seq: Int) {
//      获取调试控制台的进程id


        val response = RunInTerminalResponse(
            seq = ++this.seq,
            request_seq = seq,
            success = true,
            body = RunInTerminalResponseBody(
                shellProcessId = id
            )
        )

        send(response)

    }

    fun sendFunctionBreakpointsToDebugger() {
        val request = SetFunctionBreakpointsRequest(
            seq = ++seq,
            arguments = SetFunctionBreakpointsArguments(
                breakpoints = listOf()
            )
        )
        send(request)
    }

    fun sendDataBreakpointsToDebugger() {
        val request = SetDataBreakpointsRequest(
            seq = ++seq,
            arguments = SetDataBreakpointsArguments(
                breakpoints = listOf()
            )
        )
        send(request)
    }

    fun sendInstructionBreakpointsToDebugger() {
        val request = SetInstructionBreakpointsRequest(
            seq = ++seq,
            arguments = SetInstructionBreakpointsArguments(
                breakpoints = listOf()
            )
        )
        send(request)
    }

    fun sendBreakpointsToDebugger(breakpoints: List<CjBreakpoint>) {


        for (breakpoint in breakpoints) {
            val source = Source(
                name = breakpoint.filename, path = breakpoint.filepath
            )


            val response = SetBreakpointsRequest(
                seq = ++seq,
                arguments = SetBreakpointsArguments(
                    source = source,
                    breakpoints = breakpoint.lines.map { line ->
                        SourceBreakpoint(
//                            line = line.key + 1,
                            line = line.key,
                        )

                    },
                    sourceModified = false,
                    lines = breakpoint.lines.map { it.key },
//                    lines = breakpoint.lines.map { it.key + 1 },

                )
            )

            send(response)
        }


    }


//    fun content() {
////        latch.await() // This will block the current thread
//
//        var retryCount = 0
//        while (retryCount < MAX_RETRY_COUNT) {
//            try {
//                socket = Socket("127.0.0.1", CangJieDebuggerServerManager.DEBUGPORT)
//                if (socket!!.isConnected) {
//                    reader = socket!!.getInputStream().bufferedReader()
//                    writer = socket!!.getOutputStream().bufferedWriter()
//                    socketContentState.set(true)
//                    startReadMessageThread()
////                        unblockContent()
//                    break
//                }
//            } catch (e: SocketException) {
//
//                retryCount++
//                sleep(RETRY_DELAY_MS)
//            }
//        }
//
////        ApplicationManager.getApplication().executeOnPooledThread {
////            var retryCount = 0
////            while (retryCount < MAX_RETRY_COUNT) {
////                try {
////                    socket = Socket("127.0.0.1", CangJieDebuggerServerManager.DEBUGPORT)
////                    if (socket!!.isConnected) {
////                        reader = socket!!.getInputStream().bufferedReader()
////                        writer = socket!!.getOutputStream().bufferedWriter()
////                        socketContentState.set(true)
////                        startReadMessageThread()
//////                        unblockContent()
////                        break
////                    }
////                } catch (e: SocketException) {
////
////                    retryCount++
////                    sleep(RETRY_DELAY_MS)
////                }
////            }
////            return@executeOnPooledThread
////        }
//    }


//    fun initialize() {
////        进行调试器配置
//        sendInitializeData()
//
//
//    }


    fun getProcessHandler(): BaseProcessHandler<*> {
        return debugProcessHandler
    }

    fun sendDebugInConsole() {

        val request = DebugInConsoleRequest(
            seq = ++seq,
            arguments = DebugInConsoleRequestArguments(
                debugCommand = "process handle -p true -s false -n false SIGSEGV"
            )
        )
        send(request)
    }

    fun sendThreads(): ThreadsResponse {
        val request = ThreadsRequest(
            seq = ++seq
        )
        return sendMessageAndWaitForReply(request, ThreadsResponse::class.java)
    }

    fun sendVariables(
        variablesReference: Int,
        count: Int = 1000,
        start: Int = 0,
        filter: VariablesArgumentsFilter? = null
    ): VariablesResponse {
        val request = VariablesRequest(
            seq = ++seq,
            arguments = VariablesArguments(
                variablesReference = variablesReference,
                count = count,
                filter = filter,
                start = start
            )
        )
        return sendMessageAndWaitForReply(request, VariablesResponse::class.java)

    }

    fun sendStepIn(currentThreadId: Long) {

        val request = StepInRequest(
            seq = ++seq,
            arguments = StepInArguments(
                threadId = currentThreadId
            )
        )
        send(request)


    }

    fun sendNext(currentThreadId: Long) {

        val request = NextRequest(
            seq = ++seq,
            arguments = NextArguments(
                threadId = currentThreadId
            )
        )
        send(request)


    }

    fun sendStepOut(currentThreadId: Long) {

        val request = StepOutRequest(
            seq = ++seq,
            arguments = StepOutArguments(
                threadId = currentThreadId
            )
        )
        send(request)

    }

    fun sendContinue(currentThreadId: Long) {

        val request = ContinueRequest(
            seq = ++seq,
            arguments = ContinueArguments(
                threadId = currentThreadId.toInt()
            )
        )
        send(request)

    }

    fun sendEvaluate(expression: String, frameId: Int, context: EvaluateArgumentsContext): EvaluateResponse {
        val request = EvaluateRequest(
            seq = ++seq,
            arguments = EvaluateArguments(
                expression = expression,
                frameId = frameId,
                context = context,


                )
        )

        return sendMessageAndWaitForReply(request, EvaluateResponse::class.java)
    }


    fun sendSetVariable(
        value: Variable,
        expression: String,
        parentScope: Int,
        errorHandler: ResponseMessageConsumer<SetVariableResponse, DriverException>? = null
    ): SetVariableResponse {
        val request = SetVariableRequest(
            seq = ++seq,
            arguments = SetVariableArguments(
                variablesReference = parentScope,
                name = value.name,
                value = expression
            )
        )
        if (errorHandler == null) {
            return sendMessageAndWaitForReply(request, SetVariableResponse::class.java)

        }
        return sendMessageAndWaitForReply(request, SetVariableResponse::class.java, errorHandler, 0)

    }

    data class TargetStateTransition(
        val previousState: TargetState, val currentState: TargetState
    )


    companion object {
        val LOG = Logger.getInstance(DebugDriver::class.java)
        fun getInitializeRequest(): InitializeRequest {
            return InitializeRequest(
                InitializeRequestArguments(
                    adapterID = "cangjieDebug",
                    clientId = "idea",
                    clientName = "Intellij IDEA",
                    columnsStartAt1 = true,
                    linesStartAt1 = true,
                    locale = "zh-cn",
                    pathFormat = PathFormat.Path,
                    supportsInvalidatedEvent = true,
                    supportsMemoryEvent = true,
                    supportsArgsCanBeInterpretedByShell = true,
                    supportsMemoryReferences = true,
                    supportsProgressReporting = true,
                    supportsRunInTerminalRequest = true,
                    supportsStartDebuggingRequest = true,
                    supportsVariablePaging = true,
                    supportsVariableType = true
                )
            )
        }

        enum class TargetState {
            NOT_READY, RUNNING, SUSPENDED, FINISHING, FINISHED
        }

    }

    override fun consume(t: ProtocolMessage) {


        try {

            this.handleMessage(t)
            this.myMessageHandler.handleMessage(t)


        } catch (t: Throwable) {
            try {

            } catch (tt: Throwable) {
                t.addSuppressed(tt)
            }
            throw t
        }


    }

    private fun handleLoadedSourceEvent(loadedSourceEvent: LoadedSourceEvent) {
        TODO("Not yet implemented")
    }

    private fun handleTerminatedEvent(terminatedEvent: TerminatedEvent) {
        TODO("Not yet implemented")
    }

    private fun handleExitedEvent(exitedEvent: ExitedEvent) {
        TODO("Not yet implemented")
    }

    private fun handleThreadEvent(event: ThreadEvent) {
        TODO("Not yet implemented")
    }

    private fun handleInitializedEvent(event: InitializedEvent) {
//        TODO("Not yet implemented")
    }

    private fun handleMessageByEvent(event: Event) {
        when (event) {
            is InitializedEvent -> {
                handleInitializedEvent(event)
            }

            is ThreadEvent -> {
                handleThreadEvent(event)
            }

            is StoppedEvent -> {
                handleStoppedEvent(event)
            }

            is OutputEvent -> {
                handleOutputEvent(event)
            }

            is BreakpointEvent -> {
                handleBreakpointEvent(event)
            }

            is LoadedSourceEvent -> {
                handleLoadedSourceEvent(event)
            }

            is ExitedEvent -> {
                handleExitedEvent(event)
            }

            is TerminatedEvent -> {
                handleTerminatedEvent(event)
            }

        }

    }

    private fun handleBreakpointEvent(event: BreakpointEvent) {
        TODO("Not yet implemented")
    }

    private fun handleStoppedEvent(event: StoppedEvent) {
        TODO("Not yet implemented")
    }

    private fun handleOutputEvent(outputEvent: OutputEvent) {
//        TODO("Not yet implemented")
        println(outputEvent)
    }

    private fun handleMessage(message: ProtocolMessage) {
        when (message) {
//            is Event -> {
//                handleMessageByEvent(message)
//            }

            is Response -> {
                handleMessageByResponse(message)
            }

//            is Request -> {
//                handleMessageByRequest(message)
//            }
//
//            else -> {
//                MessageHandler.LOG.info("空消息")
//            }

        }

    }

    private fun haveConnection(initializeResponse: InitializeResponse) {


//        TODO 校验服务器权能

        try {
            myConnectedClient.complete(myDapClient)

            if (initializeResponse.success) {
                sendLaunch()
            }

        } catch (e: Exception) {
            myConnectedClient.completeExceptionally(e)


        }
    }

    private fun handleMessageByResponse(response: Response) {
        when (response) {
            is InitializeResponse -> {
                haveConnection(response)
            }

//            is ErrorResponse -> {
//                handleErrorResponse(response)
//            }
//
//            is CancelResponse -> {
//                handleCancelResponse(response)
//            }
//
//            is LaunchResponse -> {
//                handleLaunchResponse(response)
//            }
//
//            is SetBreakpointsResponse -> {
//                handleSetBreakpointsResponse(response)
//            }
//
//            is ConfigurationDoneResponse -> {
//                handleConfigurationDoneResponse(response)
//            }
//
//            is DebugInConsoleResponse -> {
//                handleDebugInConsoleResponse(response)
//            }
//
//            is ThreadsResponse -> {
//                handleThreadsResponse(response)
//            }
//
//            is StackTraceResponse -> {
//                handleStackTraceResponse(response)
//            }
//
//            is ScopesResponse -> {
//                handleScopesResponse(response)
//            }
//
//            is VariablesResponse -> {
//                handleVariablesResponse(response)
//            }
//
//            is EvaluateResponse -> {
//                handleEvaluateResponse(response)
//            }
//
//            is SetVariableResponse -> {
//                handleSetVariableResponse(response)
//            }
        }


    }

    private fun handleConfigurationDoneResponse(response: ConfigurationDoneResponse) {
        TODO("Not yet implemented")
    }

    private fun handleSetBreakpointsResponse(response: SetBreakpointsResponse) {
        TODO("Not yet implemented")
    }

    private fun handleCancelResponse(response: CancelResponse) {
        TODO("Not yet implemented")
    }

    private fun handleDebugInConsoleResponse(response: DebugInConsoleResponse) {
        TODO("Not yet implemented")
    }

    private fun handleThreadsResponse(response: ThreadsResponse) {
        TODO("Not yet implemented")
    }

    private fun handleStackTraceResponse(response: StackTraceResponse) {
        TODO("Not yet implemented")
    }

    private fun handleLaunchResponse(response: LaunchResponse) {
        TODO("Not yet implemented")
    }

    private fun handleEvaluateResponse(response: EvaluateResponse) {
        TODO("Not yet implemented")
    }

    private fun handleScopesResponse(response: ScopesResponse) {
        TODO("Not yet implemented")
    }

    private fun handleVariablesResponse(response: VariablesResponse) {
        TODO("Not yet implemented")
    }

    private fun handleSetVariableResponse(response: SetVariableResponse) {
        TODO("Not yet implemented")
    }

    private fun handleErrorResponse(response: ErrorResponse) {
        TODO("Not yet implemented")
    }

    private fun handleMessageByRequest(request: Request) {

        when (request) {
            is RunInTerminalRequest -> {
                handleRunInTerminalRequest(request)

            }
        }
    }

    private fun handleRunInTerminalRequest(request: RunInTerminalRequest) {
        TODO("Not yet implemented")
    }

    @Throws(ExecutionException::class)
    fun <R : ProtocolMessage> sendMessageAndWaitForReply(
        message: ProtocolMessage,
        responseClass: Class<R>,
        msTimeout: Long = 0L
    ): R {


        val errorHandler: ResponseMessageConsumer<in R, DriverException> =
            ThrowIfNotValid("Invalid response")

        val result: ProtocolMessage = sendMessageAndWaitForReply(message, responseClass, errorHandler, msTimeout)

        @Suppress("UNCHECKED_CAST")
        return result as R
    }

    @Throws(ExecutionException::class)
    fun <R : ProtocolMessage, E : Exception> sendMessageAndWaitForReply(
        message: ProtocolMessage,
        responseClass: Class<R>,
        errorHandler: ResponseMessageConsumer<in R, E>,
        msTimeout: Long
    ): R {


        val responseRef = Ref.create<R>()
        val responseHandler = Consumer { responseMessage: R ->
            errorHandler.consume(responseMessage)
//            if (errorHandler.success()) {
            responseRef.set(responseMessage)
//            }
        }

        getDapClient().sendMessageAndWaitForReply(message, responseClass, responseHandler, msTimeout)
        errorHandler.throwIfNeeded()

        return responseRef.get()
            ?: throw ExecutionException("Null response to message $message")
    }

    fun sendSourceFile(name: String, sourceReference: Int): SourceResponse {
        val req = SourceRequest(
            ++seq,
            SourceArguments(
                source = Source(
                    name = name,
                    sourceReference = sourceReference
                ),
                sourceReference = sourceReference
            )
        )
        return sendMessageAndWaitForReply(req, SourceResponse::class.java)

    }

    abstract class ResponseMessageConsumer<T : ProtocolMessage, E : Exception>(private var myMessage: String) :
        Consumer<T> {
        private var mySuccess = false

        private var type: String? = null

        var data: T? = null

        fun getMessage(): String {
            return myMessage
        }

        fun success(): Boolean {
            return mySuccess
        }

        override fun consume(message: T) {
//            val allFields = message.allFields
//            allFields.values.forEach {
//                if (it is ProtocolResponses.CommonResponse) {
//                    myIsValid = it.isValid
//                    if (!myIsValid && it.hasErrorMessage()) {
//                        val errorMessage = it.errorMessage
//                        if (!StringUtil.isEmptyOrSpaces(errorMessage)) {
//                            myMessage = errorMessage
//                        }
//                    }
//                }
//            }

            data = message

            if (message is Response) {
                myMessage = message.message.toString()
                mySuccess = message.success
            } else if (message is Event) {

                type = message.type.toString()
                mySuccess = true
            }
        }


        open fun throwIfNeeded() {
            if (!mySuccess) {
                throwError()
            }
        }

        open fun throwError() {
            throw ExecutionException(getMessage())
        }
    }


    protected open class ThrowIfNotValid<T : ProtocolMessage>(message: String) :
        ResponseMessageConsumer<T, DriverException>(message) {

        @Throws(DriverException::class)
        override fun throwIfNeeded() {
            super.throwIfNeeded()
        }

        @Throws(DriverException::class)
        override fun throwError() {
            throw DriverException(getMessage())
        }
    }

}

class DriverException(s: @NlsContexts.DialogMessage String?) : ExecutionException(s)


fun String.toDapPath(): String {
//  去除file前缀 并且把windows路径转换为unix路径
    return this.substringAfter("file://").replace("\\", "/")
}

