/*
 * Copyright 2024 LinQingYing. and contributors.
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

package com.linqingying.cangjie.dapDebugger.runconfig


import com.huawei.cangjie.dapDebugger.runconfig.breakpoint.CangJieBreakpointHandler
import com.linqingying.cangjie.dapDebugger.backend.CjBreakpoint
import com.linqingying.cangjie.dapDebugger.protocol.ProtocolMessage
import com.linqingying.cangjie.dapDebugger.protocol.event.*
import com.linqingying.cangjie.dapDebugger.protocol.request.*
import com.linqingying.cangjie.dapDebugger.protocol.response.*
import com.linqingying.cangjie.dapDebugger.protocol.type.*
import com.linqingying.cangjie.dapDebugger.protocol.type.arguments.*
import com.linqingying.cangjie.dapDebugger.protocol.type.body.RunInTerminalResponseBody

import com.linqingying.cangjie.dapDebugger.runconfig.message.MessageHandler
import com.linqingying.cangjie.dapDebugger.runconfig.views.CjdbPanel
import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.ui.ExecutionConsole
import com.intellij.execution.ui.RunnerLayoutUi
import com.intellij.execution.ui.layout.PlaceInGrid
import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.NlsContexts
import com.intellij.openapi.util.Ref
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightVirtualFile
import com.intellij.ui.ColoredTextContainer
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.content.Content
import com.intellij.util.Consumer
import com.intellij.util.ThreeState
import com.intellij.util.concurrency.QueueProcessor

import com.intellij.xdebugger.*
import com.intellij.xdebugger.breakpoints.XBreakpointHandler
import com.intellij.xdebugger.evaluation.XDebuggerEditorsProvider
import com.intellij.xdebugger.evaluation.XDebuggerEvaluator
import com.intellij.xdebugger.frame.*
import com.intellij.xdebugger.frame.presentation.XValuePresentation
import com.intellij.xdebugger.impl.ui.ExecutionPointHighlighter
import com.intellij.xdebugger.ui.XDebugTabLayouter
import java.io.IOException
import java.net.SocketException
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicReference
import javax.swing.Icon

data class RunParameters(
    val command: GeneralCommandLine, val port: Int,

    val program: String, val runExecutable: GeneralCommandLine? = null, val state: RunProfileState? = null
)

class DriverException(s: @NlsContexts.DialogMessage String?) : ExecutionException(s)

//
class CangJieDebugProcess(val parameters: RunParameters, session: XDebugSession) : XDebugProcess(session),
    MessageHandler,
    Consumer<ProtocolMessage> {

    val dapProcessHandler = DapProcessHandler(parameters.command)
    var dapClent: DapClent<Response>? = null
    private val myConnectedClient: CompletableFuture<DapClent<Response>> = CompletableFuture()

    private val myHandlerProcessor: QueueProcessor<Runnable> = QueueProcessor.createRunnableQueueProcessor()

    private val myMessageHandler: MessageHandler =
        MessageHandler.createQueuedHandler(this, myHandlerProcessor)
    private var seq = 1

    private var requestSeq = 0


    val consoleView = DapTerminalConsole(project)


    private val editorsProvider = CangJieDebuggerEditorsProvider()


    private val cjdbpanpel = CjdbPanel(session.project)

    private val myBreakpointHandlers: Array<XBreakpointHandler<*>>
    private val myBreakpointHandler: CangJieBreakpointHandler


    val myUiDisposable: Disposable = Disposer.newDisposable()

    private val project get() = session.project


    companion object {
        val LOG = Logger.getInstance(CangJieDebugProcess::class.java)

    }

    init {

        this.myBreakpointHandler = this.createBreakpointHandler()

        val handlersList: List<XBreakpointHandler<*>> = listOfNotNull(myBreakpointHandler)
        myBreakpointHandlers = handlersList.toTypedArray()

        startDapServer()

    }

    /**
     * 启动dap服务器
     */
    fun startDapServer() {
        dapProcessHandler.startNotify()
    }

    //执行终端
    override fun createConsole(): ExecutionConsole {
        return consoleView

    }

    fun start() {


        try {
            dapClent = DapClent(
                parameters.port, this,

                InitializeRequest(
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
            )


//            myDapClient!!.waitFor()
        } catch (ioEx: IOException) {

            throw ExecutionException(ioEx)
        }
    }

    override fun doGetProcessHandler(): ProcessHandler {
        return dapProcessHandler
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

    override fun isLibraryFrameFilterSupported(): Boolean {
        return true
    }

    override fun getEditorsProvider(): XDebuggerEditorsProvider = editorsProvider


    private fun createBreakpointHandler(): CangJieBreakpointHandler {
        return CangJieBreakpointHandler(this)
    }


    private class MySuspensionGutterIconManager(private val process: CangJieDebugProcess) : XDebugSessionListener,
        XDebuggerManagerListener, Disposable {

        val mySession: XDebugSession = process.session
        val myExecutionPointHighlighter = ExecutionPointHighlighter(mySession.project, process.myUiDisposable)


        init {
            mySession.addSessionListener(this, this)
            mySession.project.messageBus.connect(this).subscribe(XDebuggerManager.TOPIC, this)
        }

        override fun dispose() {
            this.hide()
        }


        override fun stackFrameChanged() {


            update()
        }

        private fun update() {
            this.hide()

        }

        private fun hide() {
            myExecutionPointHighlighter.hide()
        }

    }

    private fun sendDisconnect() {
        val request = DisconnectRequest(
            seq = ++seq, arguments = DisconnectArguments(
                restart = false,
                terminateDebuggee = true,
//                suspendDebuggee = false
            )
        )
        send(request)
    }

    @Throws(ExecutionException::class)
    protected fun getDapClient(): DapClent<Response> {

        return ExecutionResult.get(myConnectedClient)
    }

    private inline fun <reified T> send(message: T): Response? where T : ProtocolMessage {

        getDapClient().sendMessage<T>(message, null, null)


        return null

    }

    /**
     * 释放资源
     */
    private fun dispose() {

//        if (dapTerminaProcessHandle.detachIsDefault()) {
//            dapTerminaProcessHandle.detachProcess()
//        } else {
//            dapTerminaProcessHandle.destroyProcess()
//        }
    }

    fun sendThreads(): ThreadsResponse {
        val request = ThreadsRequest(
            seq = ++seq
        )
        return sendMessageAndWaitForReply(request, ThreadsResponse::class.java)
    }

    override fun getBreakpointHandlers(): Array<XBreakpointHandler<*>> = this.myBreakpointHandlers
    fun disconnect() {

        try {
            sendDisconnect()
        } catch (e: SocketException) {
            LOG.info("调试器已经断开连接")
        }


        dispose()

    }

    override fun stop() {
        disconnect()
        session.stop()
    }

    fun createSourcePosition(
        file: String,
        line: Int,
        isBinary: Boolean = false,
        name: String = "temp"
    ): XSourcePosition? {
        if (isBinary) {
            val virtualFile: VirtualFile = LightVirtualFile(name, file)
            return XDebuggerUtil.getInstance().createPosition(virtualFile, line)
        }

        return try {
            val resolvedFile = resolveFile(file)
            resolvedFile?.let { XDebuggerUtil.getInstance().createPosition(it, line) }
        } catch (e: ProcessCanceledException) {
            null
        }
    }

    fun resolveFile(file: String): VirtualFile? {

        return LocalFileSystem.getInstance().findFileByPath(file)
    }

    fun registerCjdbViewPanel(ui: RunnerLayoutUi) {


        val cjdbViewContent: Content = ui.createContent("Cjdb", cjdbpanpel, "Cjdb", null, null)
        cjdbViewContent.isCloseable = false
        cjdbViewContent.setShouldDisposeContent(true)

        cjdbViewContent.icon = AllIcons.Debugger.Console

        ui.addContent(cjdbViewContent, 0, PlaceInGrid.center, false)

    }


    fun getCurrentThreadId(): Long {
//(currentSuspendContext.get()?.activeExecutionStack as CangJieExecutionStack?)?.threadid
//            ?:
        return currentThread.get()?.id ?: -1
    }

//    fun getCurrentFrameIndex(): Int {
//        val currentStackFrame: CangJieStackFrame? = this.session.currentStackFrame as CangJieStackFrame?
//        return if (currentStackFrame != null) currentStackFrame.frameIndex else -1
//    }

    override fun createTabLayouter(): XDebugTabLayouter {
        return object : XDebugTabLayouter() {
            override fun registerAdditionalContent(ui: RunnerLayoutUi) {
                registerCjdbViewPanel(ui)
            }

//            override fun registerConsoleContent(ui: RunnerLayoutUi, console: ExecutionConsole): Content {
//
//
//                ProcessTerminatedListener.attach(shellProcessHandler) // 监听进程结束事件
//
//                val newConsole = ConsoleViewImpl(session.project, true)
//                newConsole.attachToProcess(shellProcessHandler) // 将进程处理器附加到新的控制台
//
//                shellProcessHandler.startNotify() // 开始通知进程事件
//
//                val consoleContent = ui.createContent(
//                    "NewConsoleContent",
//                    newConsole.component,
//                    "Console",
//                    AllIcons.Debugger.Console,
//                    newConsole.preferredFocusableComponent
//                )
//                consoleContent.isCloseable = true
//
//
//                ui.addContent(consoleContent, 0, PlaceInGrid.bottom, false)
//
//                return consoleContent
//            }
        }
    }


    //    请求变量列表并返回
    fun sendAndReturnVariables(
        variablesReference: Int,
        count: Int = 1000,
        start: Int = 0,
        filter: VariablesArgumentsFilter? = null
    ): List<Variable> {

        val res = sendVariables(variablesReference, count, start, filter)

        return res.body.variables!!

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

    override fun sessionInitialized() {
//        session.initBreakpoints()

//        将DapInitializeData发送给服务器

//        session.consoleView.print("sessionInitialized", ConsoleViewContentType.NORMAL_OUTPUT)


        this.session.rebuildViews()

    }

    //    是否准备完成，可以发送断点
    val isReady = AtomicReference(false)


//    override fun doGetProcessHandler(): ProcessHandler {
//
//        return shellProcessHandler
//    }


    val breakpoints: MutableList<CjBreakpoint> = mutableListOf()

    fun addBreakpoint(breakpoint: CjBreakpoint) {


        addBreakpoints(listOf(breakpoint))
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

    fun addBreakpoints(breakpoints: List<CjBreakpoint>) {

        //        如果文件已经存在，那么就直接添加行号
        for (breakpoint in breakpoints) {
            val index = this.breakpoints.indexOfFirst { it.filepath == breakpoint.filepath }
            if (index != -1) {
                this.breakpoints[index].addLines(breakpoint.lines)
                this.breakpoints[index].isRunToCursor.putAll(breakpoint.isRunToCursor)
            } else {
                this.breakpoints.add(breakpoint)
            }
        }

        if (isReady.get()) {
            sendBreakpointsToDebugger(this.breakpoints)

        }

    }

    fun removeBreakpoint(sourcePath: String?, line: Int) {
        val index = this.breakpoints.indexOfFirst { it.filepath == sourcePath }
        if (index != -1) {

//                删除行号
            this.breakpoints[index].removeLine(line)


//
//
//                if (this.breakpoints[index].lines.isEmpty()) {
//                    this.breakpoints.removeAt(index)
//                }
        }
    }

    fun removeBreakpoints(cjBreakpoints: MutableList<CjBreakpoint>) {

//        删除文件中的行号，如果文件中的行号为空，那么就删除文件
        for (breakpoint in cjBreakpoints) {
            val index = this.breakpoints.indexOfFirst { it.filepath == breakpoint.filepath }
            if (index != -1) {

//                删除行号
                this.breakpoints[index].removeLines(breakpoint.lines)


//
//
//                if (this.breakpoints[index].lines.isEmpty()) {
//                    this.breakpoints.removeAt(index)
//                }
            }
        }


        if (isReady.get()) {
            sendBreakpointsToDebugger(this.breakpoints)
        }

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

    //    继续
    override fun resume(context: XSuspendContext?) {
        if (context is CangJieSuspendContext) {
            this.sendContinue(context.activeThreadId)

        }
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

    //步过
    override fun startStepOver(context: XSuspendContext?) {


        if (context is CangJieSuspendContext) {
//            currentSuspendContext.set(context)

            sendNext(context.activeThreadId)
        }

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

    //步入
    override fun startStepInto(context: XSuspendContext?) {
        if (context is CangJieSuspendContext) {
            sendStepIn(context.activeThreadId)
        }


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

    //步出
    override fun startStepOut(context: XSuspendContext?) {
        if (context is CangJieSuspendContext) {
            sendStepOut(context.activeThreadId)
        }

    }


    //运行到光标处
    override fun runToPosition(position: XSourcePosition, context: XSuspendContext?) {


//        设置断点
//        val breakpointManager = XDebuggerManager.getInstance(project).breakpointManager
//
//        // Get the file and line number from the XSourcePosition
//        val file = position.file
        val line = position.line + 1

        // Create a new breakpoint at the line of the file
//        val breakpointProperties = CangJieLineBreakpointType().createBreakpointProperties(file, line)

//        ApplicationManager.getApplication().runWriteAction {
//            val xbreakpoint = breakpointManager.addLineBreakpoint(CangJieLineBreakpointType(), file.url, line, breakpointProperties)
        val breakpoint = CjBreakpoint(
            position.file.name,
            position.file.path,
        )

        breakpoint.addLine(line, null)
        breakpoint.isRunToCursor[line] = true
        addBreakpoint(breakpoint)


//            删除该断点


//        }
    }


    /*********************************MessageHandler**************************************/
    override fun handleInitializeResponse(response: InitializeResponse) {
        haveConnection(response)
    }

    override fun handleErrorResponse(response: ErrorResponse) {
//      TODO 是否像前端报告错误，怎么报告？


        when (response.command) {
            MessageCommand.launch -> session.stop()

            else -> {

            }
        }

    }

    override fun handleOutputEvent(message: OutputEvent) {
        message.body?.output?.let { cjdbpanpel.print(it) }
    }

    override fun consume(t: ProtocolMessage) {

        this.myMessageHandler.handleMessage(t)

    }

    val threads: MutableList<Thread> = mutableListOf()
    val currentThread = AtomicReference<Thread?>(null)

    //    线程数据是否已经加载
    var isThreadsLoaded = CompletableFuture<Boolean>()
    override fun handleRunInTerminalRequest(message: RunInTerminalRequest) {

        requestSeq = message.seq


//        session.consoleView.print("测试", ConsoleViewContentType.NORMAL_OUTPUT)

        val id = consoleView.shellId
        val response = RunInTerminalResponse(
            seq = ++seq,
            request_seq = requestSeq,

            body = RunInTerminalResponseBody(
//                shellProcessId = 14980
                shellProcessId = id
            ), success = true
        )

        send(response)


    }

    override fun handleThreadsResponse(message: ThreadsResponse) {


        if (message.success) {
            this.threads.clear()
            this.threads.addAll(message.body.threads)


            if (this.threads.isEmpty()) {
                return
            }
            isThreadsLoaded.complete(true)

        }
    }


    override fun handleStackTraceResponse(message: StackTraceResponse) {
//

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

    override fun handleScopesResponse(message: ScopesResponse) {


    }

    val variables: MutableMap<Int, MutableList<Variable>> = mutableMapOf()
    var isVariablesLoaded: MutableMap<Int, CompletableFuture<Boolean>> = mutableMapOf()

    var variablesReference: AtomicReference<Int> = AtomicReference(-1)
    override fun handleVariablesResponse(message: VariablesResponse) {
        if (message.success) {
//            variables.clear()
//            message.body.variables?.let { variables.addAll(it) }


            variables[variablesReference.get()] =
                message.body.variables?.toMutableList() ?: mutableListOf()
            isVariablesLoaded[variablesReference.get()]?.complete(true)


        }

    }

    private fun pauseAndHitBreakpoint(threadId: Long, id: Int? = null) {
        val suspendContext = CangJieSuspendContext(
            this,
            threadId,
        )
        if (id == null) {
//                步进
            session.positionReached(suspendContext)
        } else {
//                断点
            val breakpoint = this.myBreakpointHandler.getXBreakpoint(id)
//                breakpoint?.let { it1 -> session.breakpointReached(it1, null, suspendContext) }
            if (breakpoint != null) {
                session.breakpointReached(breakpoint, null, suspendContext)
            } else {
                session.positionReached(suspendContext)
            }

        }

    }

    //    程序暂停并且断点命中


    override fun handleStoppedEvent(message: StoppedEvent) {
        when (message.body?.reason) {
            StoppedEventReason.Breakpoint -> {
                for (id in message.body.hitBreakpointIds!!) {
                    pauseAndHitBreakpoint(message.body.threadId!!, id)

                }
            }

            StoppedEventReason.DataBreakpoint -> TODO()
            StoppedEventReason.Entry -> TODO()
            StoppedEventReason.Exception -> TODO()
            StoppedEventReason.FunctionBreakpoint -> TODO()
            StoppedEventReason.Goto -> TODO()
            StoppedEventReason.InstructionBreakpoint -> TODO()
            is StoppedEventReason.Other -> TODO()
            StoppedEventReason.Pause -> TODO()
            StoppedEventReason.Step -> {
                pauseAndHitBreakpoint(message.body.threadId!!)
            }

            null -> TODO()
        }
    }


    fun getCjBreakpoint(sourcePath: String?): CjBreakpoint? {
        return this.breakpoints.find {
            Paths.get(it.filepath).equals(sourcePath?.let { it1 -> Paths.get(it1) })
        }
    }


    override fun handleSetBreakpointsResponse(response: SetBreakpointsResponse) {
//        将断点添加到handle列表中
        for (breakpoint in response.body?.breakpoints!!) {
            val id = breakpoint.id

            val cjBreakpoint = getCjBreakpoint(breakpoint.source?.path)


            if (cjBreakpoint != null) {
                if (cjBreakpoint.isRunToCursor[breakpoint.line] == true) {
                    sendContinue(getCurrentThreadId())

                    breakpoint.line?.let { removeBreakpoint(breakpoint.source?.path, it) }
                    continue
                }
            }

            val xbreakpoint =
                cjBreakpoint?.lines?.get(breakpoint.line)

            if (xbreakpoint != null) {
                ApplicationManager.getApplication().invokeLater {
                    id?.let { this.myBreakpointHandler.addBreakpoint(xbreakpoint, it) }
//               刷新ui
                    this.session.rebuildViews()
                }
            }

        }

    }


    override fun handleBreakpointEvent(message: BreakpointEvent) {


    }


    override fun handleExitedEvent(event: ExitedEvent) {


//        结束运行任务
        stopAsync()

    }


    override fun handleInitializedEvent(message: InitializedEvent) {
//TODO
//        设置方法断点
//        设置数据断点
//        setInstructionBreakpoints

        isReady.set(true)
        if (this.breakpoints.isNotEmpty()) {
            sendBreakpointsToDebugger(this.breakpoints)

        }

        sendFunctionBreakpointsToDebugger()
        sendDataBreakpointsToDebugger()
        sendInstructionBreakpointsToDebugger()

        sendConfigurationDone()
    }

    /**
     * 发送配置完成
     */
    fun sendConfigurationDone() {
        val request = ConfigurationDoneRequest(
            seq = ++seq,

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

    fun sendDataBreakpointsToDebugger() {
        val request = SetDataBreakpointsRequest(
            seq = ++seq,
            arguments = SetDataBreakpointsArguments(
                breakpoints = listOf()
            )
        )
        send(request)
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

    private var serverCapabilities: InitializeResponse? = null

    val sessionid = UUID.randomUUID().toString()

    private fun sendLaunch() {
        val mainexe = parameters.program

        val request =
            LaunchRequestArguments(
                buildBeforeLaunch = true,
                externalConsole = false,
                env = mapOf(
//                    "PATH" to "C:\\Users\\27439\\.sdk\\cangjie0.53.4\\runtime\\lib\\windows_x86_64_llvm"
                ),
                name = "Cangjie Debug (cjdb): launch",
                program = mainexe,
                request = MessageCommand.launch,
                type = "cangjieDebug",
                __sessionId = sessionid,
                __configurationTarget = 6

            )

        val data = LaunchRequest(
            seq = ++seq, arguments = request
        )
        send(data)

    }

    private fun haveConnection(initializeResponse: InitializeResponse) {


        serverCapabilities = initializeResponse

//        TODO 校验服务器权能

        try {
            myConnectedClient.complete(dapClent)

            if (initializeResponse.success) {
                sendLaunch()
            }
//            TODO 在返回的success字段为false时是否需要抛出异常？

        } catch (e: Exception) {
            myConnectedClient.completeExceptionally(e)


        }
    }


    override fun handleConfigurationDoneResponse(response: ConfigurationDoneResponse) {
        if (response.success) {
            sendDebugInConsole()
        }
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

    override fun handleTerminatedEvent(event: TerminatedEvent) {


    }


    //
    var isEvaluateLoaded = CompletableFuture<EvaluateResponse>()

    override fun handleEvaluateResponse(evaluateResponse: EvaluateResponse) {
//        if (evaluateResponse.success) {
//
//        }
        isEvaluateLoaded.complete(evaluateResponse)
    }

    /**
     * 执行表达式
     */
    fun evaluate(
        expression: String,
        frameId: Int,
        context: EvaluateArgumentsContext
    ): EvaluateResponse {


        return sendEvaluate(expression, frameId, context)

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

    var isSetVariableLoaded = CompletableFuture<SetVariableResponse>()
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

    override fun handleSetVariableResponse(variableResponse: SetVariableResponse) {
        isSetVariableLoaded.complete(variableResponse)
    }
}


class CangJieStackFrame(
    val process: CangJieDebugProcess,
    val thread: Thread,
    val frame: StackFrame,
    val returnValue: Variable? = null
) : XStackFrame() {
    override fun getEvaluator(): XDebuggerEvaluator = CangJieDebuggerLanguageSupportManager.createEvaluator(this)


    override fun customizePresentation(component: ColoredTextContainer) {
        val position = sourcePosition
        if (position != null) {

            frame.name.let { component.append(it, SimpleTextAttributes(SimpleTextAttributes.STYLE_BOLD, null)) }



            component.append(" ${position.file.name}", SimpleTextAttributes.REGULAR_ATTRIBUTES)
            component.append(":${(position.line + 1)}", SimpleTextAttributes.REGULAR_ATTRIBUTES)

        } else {
            component.append(frame.name, SimpleTextAttributes(SimpleTextAttributes.STYLE_BOLD, null))
            component.append(" ${frame.source?.name}", SimpleTextAttributes.REGULAR_ATTRIBUTES)
            component.append(":${frame.line}", SimpleTextAttributes.REGULAR_ATTRIBUTES)
        }
    }


    private var mySourcePosition: XSourcePosition? = null

    //    变量范围
    var scope: ScopesResponse? = null

    override fun getSourcePosition(): XSourcePosition? {
        val file = frame.source?.path

        if (mySourcePosition == null) {
            mySourcePosition = file?.let {
                process.createSourcePosition(
                    it,
                    frame.line - 1
                )
            }
        }
        return mySourcePosition
//
//        mySourcePosition = if (file == null) {
//            //            请求源文件
////TODO 请求汇编文件太卡了，就不请求了
////            val res = process.myDriver.sendSourceFile(frame.source?.name!!, frame.source.sourceReference!!)
//
////            res.body?.content?.let { process.createSourcePosition(it, frame.line - 1, true, frame.name) }
//
//     null
//        } else {
//            process.createSourcePosition(
//                file,
//                frame.line - 1
//            )
//        }
//
//        return mySourcePosition
    }

    override fun computeChildren(node: XCompositeNode) {
//        super.computeChildren(node)
        scope = process.sendScopes(frame.id)
        val children = XValueChildrenList()


        val res = scope?.body?.scopes?.first()?.variablesReference?.let { process.sendVariables(it) }


        if (res != null) {
            res.body.variables?.forEach {
                children.add(
                    it.name,
                    CangJieValue(
                        process,
                        it,
                        scope?.body?.scopes?.first()?.variablesReference!!,
                        mySourcePosition,

                        )
                )
            }

            node.addChildren(children, true)
        }


    }

}

//相当于一个线程
class CangJieExecutionStack(
    val process: CangJieDebugProcess,
    val thread: Thread? = null,

//    val frame: StackFrame? = null,
//    val returnValue: CjValue
) : XExecutionStack(thread?.name) {


    var stackFrames: MutableList<CangJieStackFrame?> = mutableListOf()
    private val myTopFrame: CangJieStackFrame?
        get() {


            return if (stackFrames.isEmpty()) null
            else stackFrames.first()


        }

    init {


    }

    protected fun newFrame(frame: StackFrame?): CangJieStackFrame? {
        if (frame == null) return null

        return this.thread?.let { CangJieStackFrame(this.process, it, frame) }
    }


    private fun getStackFrames() {
        val res = process.sendStacktrace(thread?.id!!)


        res.body?.stackFrames?.sortedBy {
            it.id
        }?.forEach {
            stackFrames.add(newFrame(it))
        }
    }

    override fun getTopFrame(): XStackFrame? {


        return myTopFrame

    }

    override fun computeStackFrames(firstFrameIndex: Int, container: XStackFrameContainer?) {

        if (stackFrames.isEmpty()) {
            getStackFrames()
        }

        if (firstFrameIndex < stackFrames.size) {
            container?.addStackFrames(stackFrames, true)
        }
    }
}

class CangJieSuspendContext(
    val process: CangJieDebugProcess,

    val activeThreadId: Long

) : XSuspendContext() {

    val threads = process.sendThreads()


    val executionStacks: MutableMap<Long, CangJieExecutionStack> = mutableMapOf()


    init {
        threads.body.threads.sortedBy { it.id }.forEach {
            executionStacks.put(it.id, CangJieExecutionStack(process, it))
        }


    }

    override fun computeExecutionStacks(container: XExecutionStackContainer) {
        container.addExecutionStack(executionStacks.values.toList(), true)
    }

    override fun getExecutionStacks(): Array<XExecutionStack> {
//        return executionStacks.toList().toArray { size ->
//            arrayOfNulls(size)
//        }

        return executionStacks.values.toArray { size ->
            arrayOfNulls(size)
        }
    }

    override fun getActiveExecutionStack(): XExecutionStack? {
        return executionStacks[activeThreadId]
    }

}




class CangJieValue(
    val process: CangJieDebugProcess,
    val value: Variable,
    private val parentScope: Int,
    private val position: XSourcePosition? = null,

    ) : XValue() {


    //    基本类型变量
    class PrimitiveValuePlace(private val value: Variable) : XValuePresentation() {
        override fun getSeparator(): String {
            return " = "
        }

        override fun getType(): String? {
            return value.type
        }

        override fun renderValue(renderer: XValueTextRenderer) {
            renderer.renderValue(value.value)
        }

    }

    class ObjectValuePlace(private val value: Variable) : XValuePresentation() {
        override fun getSeparator(): String {
            return " = "
        }

        override fun getType(): String? {
            return value.type
        }

        override fun renderValue(renderer: XValueTextRenderer) {
            if (value.memoryAddress != null) {
                renderer.renderValue("@${value.memoryAddress}")
            }

        }
    }


    class ArrayValuePlace(private val value: Variable) : XValuePresentation() {
        override fun getSeparator(): String {
            return " = "
        }

        override fun getType(): String? {
            return value.type
        }

        override fun renderValue(renderer: XValueTextRenderer) {
//          size = ${value.indexedVariables}
            renderer.renderValue("size = ${value.indexedVariables}")
        }


    }

    private val isPrimitive get() = value.variablesReference == 0

    //TODO 查看CIDR中数组数据是否可以返回
    override fun computePresentation(node: XValueNode, place: XValuePlace) {


        if (value.variablesReference > 0) {
//                    结构化变量
            if (value.indexedVariables != null) {
//                     数组  索引类型变量 显示size = 1
                node.setPresentation(null, ArrayValuePlace(value), true)


            } else if (value.namedVariables != null) {
//                    对象
                node.setPresentation(null, ObjectValuePlace(value), true)
            }
        } else {
//                    基本类型变量
            node.setPresentation(null, PrimitiveValuePlace(value), false)

        }

    }


    //    获取过滤器
    val filter: VariablesArgumentsFilter?
        get() {
            return if (isPrimitive) {
                null
            } else if (value.indexedVariables != null) {
                VariablesArgumentsFilter.indexed

            } else if (value.namedVariables != null) {
                VariablesArgumentsFilter.named
            } else {
                null
            }

        }

    val count get() = value.indexedVariables ?: 1000
    val start get() = 0

    override fun computeInlineDebuggerData(callback: XInlineDebuggerDataCallback): ThreeState {
        this.doComputeInlineDebuggerDataAsync(callback::computed)


        return ThreeState.YES
    }


    private fun doComputeInlineDebuggerDataAsync(navigatable: XNavigatable) {

//        TODO 需要根据变量名称查找变量的位置 这里先不搞了，就先这样
        navigatable.setSourcePosition(position)
//        if (position == null) {
//            navigatable.setSourcePosition(null)
//        } else {
//
//
//            val a =resolveToDeclaration(position, value.name)
//            println()
//
//        }
    }

    override fun computeChildren(node: XCompositeNode) {

        if (!isPrimitive) {
//            请求子变量

            val children = XValueChildrenList()


            val variables = process.sendAndReturnVariables(value.variablesReference, count, start, filter)


            variables.map {
                children.add(
                    it.name,
                    CangJieValue(
                        process,
                        it,
                        value.variablesReference,
                        position

                    )
                )
            }




            node.addChildren(children, true)

        }

    }

    override fun getModifier(): XValueModifier {
        return object : XValueModifier() {
            override fun setValue(expression: XExpression, callback: XModificationCallback) {

//                TODO 需要先判断是否有写的权限

                val errorHandler: CangJieDebugProcess.ResponseMessageConsumer<SetVariableResponse, DriverException> =
                    object : CangJieDebugProcess.ResponseMessageConsumer<SetVariableResponse, DriverException>("") {


                        override fun throwIfNeeded() {

                            if (success()) {
                                value.value = data?.body?.value.toString()
                                value.type = data?.body?.type
                                callback.valueModified()
                            } else {
                                callback.errorOccurred(data?.message.toString())

                            }
                        }

                    }

                process.sendSetVariable(value, expression.expression, parentScope, errorHandler)
//

            }
        }
    }

    override fun getEvaluationExpression(): String = value.name
}


class MySuspensionGutterIconRenderer : GutterIconRenderer() {
    override fun equals(other: Any?): Boolean {
        if (this === other)
            return true
        return other is MySuspensionGutterIconRenderer
    }


    override fun hashCode(): Int {
        return 0
    }

    override fun getTooltipText(): String {
        return "Suspended"
    }

    override fun getIcon(): Icon {
        return AllIcons.Debugger.Db_db_object
    }
}
