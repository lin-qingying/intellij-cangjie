package com.debugger.runconfig

import com.debugger.CangJieDebuggerPluginService
import com.debugger.backend.CjBreakpoint
import com.debugger.runconfig.breakpoint.CangJieBreakpointHandler
import com.debugger.runconfig.message.MessageHandler
import com.debugger.runconfig.views.CjdbPanel
import com.huawei.cangjie.idea.run.cjpm.CjpmRunStateBase
import com.huawei.cangjie.lang.sdk.CangJieSdkManager
import com.huawei.cangjie.psi.CjElement
import com.huawei.cangjie.psi.psiUtil.ancestorOrSelf
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessListener
import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.ExecutionConsole
import com.intellij.execution.ui.RunnerLayoutUi
import com.intellij.execution.ui.layout.PlaceInGrid
import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.*
import com.intellij.ui.ColoredTextContainer
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.content.Content
import com.intellij.util.DocumentUtil
import com.intellij.util.ThreeState
import com.intellij.xdebugger.*
import com.intellij.xdebugger.breakpoints.XBreakpointHandler
import com.intellij.xdebugger.evaluation.XDebuggerEditorsProvider
import com.intellij.xdebugger.evaluation.XDebuggerEvaluator
import com.intellij.xdebugger.frame.*
import com.intellij.xdebugger.frame.presentation.XValuePresentation
import com.intellij.xdebugger.impl.ui.ExecutionPointHighlighter
import com.intellij.xdebugger.impl.ui.tree.nodes.XValueNodePresentationConfigurator
import com.intellij.xdebugger.ui.XDebugTabLayouter
import com.linqingying.lsp.api.LspProcessHandler
import dap.event.*
import dap.request.RunInTerminalRequest
import dap.response.*
import dap.type.*
import java.nio.file.Paths
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicReference
import javax.swing.Icon


class CangJieDebugProcess(session: XDebugSession, val state: CjpmRunStateBase) : XDebugProcess(session),
    MessageHandler {


    //    调试器的socket
    var myDriver: DebugDriver = DebugDriver(session, this)


    private val myEditorsProvider = CangJieDebuggerEditorsProvider()


    private val cjdbpanpel = CjdbPanel(session.project)

    private val myBreakpointHandlers: Array<XBreakpointHandler<*>>
    private val myBreakpointHandler: CangJieBreakpointHandler


//    val command = arrayOf("cmd.exe") //

    val command = GeneralCommandLine().apply {

        val sdk = CangJieSdkManager.getProjectSdk()

        exePath = "C:\\Windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe"

        environment.put("CANGJIE_HOME", sdk?.homePath?.toSystemPath())
        environment.put(
            "PATH",
            "${sdk?.homePath}/bin;${sdk?.homePath}/tools/bin;".toSystemPath() + System.getenv("PATH")
        )
        withWorkDirectory(session.project.basePath)
    }
    private val shellProcessHandler = LspProcessHandler(
        command
    ).apply {
        addProcessListener(
            object : ProcessListener {
                override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
                    super.onTextAvailable(event, outputType)
                }
            }
        )

    }

    protected val myConsole: ConsoleView = state.consoleBuilder.getConsole().apply {
        shellProcessHandler.startNotify()
        attachToProcess(shellProcessHandler)
    }

    private val myProcessDisposable: Disposable
    val myUiDisposable: Disposable = Disposer.newDisposable();

    private val project get() = session.project

    init {

        this.myBreakpointHandler = this.createBreakpointHandler()

        val handlersList: List<XBreakpointHandler<*>> = listOfNotNull(myBreakpointHandler)
        myBreakpointHandlers = handlersList.toTypedArray()

        val gutterIconManager = MySuspensionGutterIconManager(this)
        val debuggerPluginService = project.getService(CangJieDebuggerPluginService::class.java)
        myProcessDisposable = Disposer.newDisposable(debuggerPluginService, "CangJieDebugProcess");
        Disposer.register(this.myProcessDisposable, gutterIconManager);

    }


    override fun doGetProcessHandler(): ProcessHandler? {
        return myDriver.debugProcessHandler
    }

    fun isDetachDefault(): Boolean {
        return false
    }

    override fun isLibraryFrameFilterSupported(): Boolean {
        return true
    }

    override fun getEditorsProvider(): XDebuggerEditorsProvider = myEditorsProvider


    override fun createConsole(): ExecutionConsole {


        return myConsole

//        return super.createConsole()
    }

    //    protected fun waitForTermination(): Boolean {
//        return this.myDriver.getShellProcessHandler().waitFor()
//    }
    private fun createBreakpointHandler(): CangJieBreakpointHandler {
        return CangJieBreakpointHandler(this)
    }


//    private class MySuspensionGutterIconManager implements XDebugSessionListener, XDebuggerManagerListener, Disposable

    private class MySuspensionGutterIconManager(private val process: CangJieDebugProcess) : XDebugSessionListener,
        XDebuggerManagerListener, Disposable {

        val mySession = process.session
        val myExecutionPointHighlighter = ExecutionPointHighlighter(mySession.project, process.myUiDisposable)


        init {
            mySession.addSessionListener(this, this)
            mySession.project.messageBus.connect(this).subscribe(XDebuggerManager.TOPIC, this);
        }

        override fun dispose() {
            this.hide();
        }

//        override fun sessionPaused() {
//            this.update()
//        }
//
//        override fun sessionResumed() {
//            this.update()
//        }
//        override fun sessionStopped() {
//            this.update()
//        }
//
//
//        override fun settingsChanged() {
//            this.update()
//        }
//
//        override fun currentSessionChanged(previousSession: XDebugSession?, currentSession: XDebugSession?) {
//            this.update()
//        }
//


        override fun stackFrameChanged() {

            if (mySession.currentStackFrame is CangJieStackFrame) {
//            val position = mySession.currentStackFrame?.sourcePosition
//            if (position != null) {
//                this.myExecutionPointHighlighter.show(position, true, null, false);
//            }
                process.currentThread.set((mySession.currentStackFrame as CangJieStackFrame).thread)
                process.currentStackFrame.set(mySession.currentStackFrame as CangJieStackFrame)
                process.variablesReference.set((mySession.currentStackFrame as CangJieStackFrame).frame.id)
            }

            update()
        }

        private fun update() {
            this.hide()

//            if (XDebuggerManager.getInstance(this.mySession.project).currentSession == this.mySession) {
//                val suspendContext = mySession.suspendContext
//                if (suspendContext != null) {
//                    val suspendExecutionStack = suspendContext.activeExecutionStack
//                    if (suspendExecutionStack is CangJieExecutionStack) {
//
////                        if (true) {
//                        val suspendFrame = suspendExecutionStack.getTopFrame()
////                        && this.mySession.currentStackFrame == suspendFrame
//                        if (suspendFrame != null ) {
//                            val position: XSourcePosition? =   suspendFrame.sourcePosition
//
//
//
//
//                            position?.let { this.myExecutionPointHighlighter.show(it, true, null, false) };
//                        }
////                        }
//                    }
//                }
//            }
        }

        private fun hide() {
            myExecutionPointHighlighter.hide()
        }

    }

    override fun getBreakpointHandlers(): Array<XBreakpointHandler<*>> = this.myBreakpointHandlers

    override fun stop() {
        this.myDriver.disconnect()
        session.stop()
    }

    fun createSourcePosition(file: String, line: Int): XSourcePosition? {


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
        this.variablesReference.set(variablesReference)
        isVariablesLoaded[variablesReference] = CompletableFuture<Boolean>()
        myDriver.sendVariables(variablesReference, count, start, filter)


//        等待变量列表加载完成
        isVariablesLoaded[variablesReference]?.get()

        return variables[variablesReference] ?: mutableListOf()


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
            this.myDriver.sendBreakpointsToDebugger(this.breakpoints)

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
            this.myDriver.sendBreakpointsToDebugger(this.breakpoints)
        }

    }

    //    继续
    override fun resume(context: XSuspendContext?) {

        this.myDriver.sendContinue(getCurrentThreadId())
    }

    val currentSuspendContext = AtomicReference<CangJieSuspendContext?>(null)

    //步过
    override fun startStepOver(context: XSuspendContext?) {

//        if (context is CangJieSuspendContext) {
//            currentSuspendContext.set(context)
//            context.activeThread.threadid?.let { this.myDriver.sendNext(it) }
//        }
        if (currentThread.get() == threads[0]) {
            this.myDriver.sendNext(currentThread.get()?.id ?: -1)

        } else {
            currentThread.get()?.let { this.myDriver.sendContinue(it.id) }
        }
    }

    //步入
    override fun startStepInto(context: XSuspendContext?) {
        if (currentThread.get() == threads[0]) {
            this.myDriver.sendStepIn(currentThread.get()?.id ?: -1)

        } else {
            currentThread.get()?.let { this.myDriver.sendContinue(it.id) }

        }

    }

    //步出
    override fun startStepOut(context: XSuspendContext?) {
        if (currentThread.get() == threads[0]) {
            this.myDriver.sendStepOut(currentThread.get()?.id ?: -1)

        } else {
            currentThread.get()?.let { this.myDriver.sendContinue(it.id) }

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
    override fun handleRunInTerminalRequest(message: RunInTerminalRequest) {
        this.myDriver.sendShellProcessId(shellProcessHandler.process.pid(), message.seq)
    }

    override fun handleOutputEvent(message: OutputEvent) {
        message.body?.output?.let { cjdbpanpel.print(it) }
    }


    val threads: MutableList<Thread> = mutableListOf()
    val currentThread = AtomicReference<Thread?>(null)

    //    线程数据是否已经加载
    var isThreadsLoaded = CompletableFuture<Boolean>()

    override fun handleThreadsResponse(message: ThreadsResponse) {


        if (message.success) {
            this.threads.clear()
            this.threads.addAll(message.body.threads)


            if (this.threads.isEmpty()) {
                return
            }
            isThreadsLoaded.complete(true)
//            this.currentThread.set(this.threads[0])
////            this.isStackTraceLoaded[getCurrentThreadId()] = CompletableFuture<Boolean>()
//            this.myDriver.sendStacktrace(getCurrentThreadId())

        }
    }

    //    val stackTraces: MutableMap<Int,List<StackFrame>> = mutableMapOf()
    val stackTraces: MutableMap<Long, MutableList<StackFrame>> = mutableMapOf()
    var isStackTraceLoaded: MutableMap<Long, CompletableFuture<Boolean>> = mutableMapOf()
    val currentStackFrame = AtomicReference<CangJieStackFrame?>(null)


    override fun handleStackTraceResponse(message: StackTraceResponse) {

        if (message.success) {
//            message.body.let { it?.let { it1 -> stackTraces.addAll(it1.stackFrames) } }
            stackTraces[getCurrentThreadId()] = message.body?.stackFrames?.toMutableList() ?: mutableListOf()
            isStackTraceLoaded[getCurrentThreadId()]?.complete(true)

//            this.myDriver.sendScopes(stackTraces[getCurrentThreadId()]!![0].id)

            if (message.body?.stackFrames?.isEmpty() == true) {
                return
            }


            if (currentThread.get() == threads[0]) {
                if (message.body?.stackFrames?.get(0)?.source?.path == null) {
                    this.myDriver.sendNext(getCurrentThreadId())
                    return
                }
            }


        }
    }


    val scopes: MutableList<Scope> = mutableListOf()
    var isScopesLoaded = CompletableFuture<Boolean>()
    override fun handleScopesResponse(message: ScopesResponse) {

        isScopesLoaded.complete(true)
        if (message.success) {
            scopes.clear()
            message.body?.scopes?.let { scopes.addAll(it) }

            this.myDriver.sendVariables(scopes[0].variablesReference)
        }
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


    //    程序暂停并且断点命中
    private fun pauseAndHitBreakpoint(id: Int? = null) {

//        等待threads和stackTrace数据加载完成, isStackTraceLoaded, isVariablesLoaded
        CompletableFuture.allOf(isThreadsLoaded).thenAccept {

            val suspendContext = CangJieSuspendContext(
                this,
                threads,
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

//            刷新ui
//            this.session.rebuildViews()


        }


    }


    /**
     * 重置数据
     */
    fun reset() {
//        this.threads.clear()
        this.stackTraces.clear()
//        this.scopes.clear()
        this.variables.clear()

        isThreadsLoaded = CompletableFuture<Boolean>()
        isStackTraceLoaded.clear()
        isScopesLoaded = CompletableFuture<Boolean>()
        isVariablesLoaded.clear()
    }

    override fun handleStoppedEvent(message: StoppedEvent) {

        reset()
        this.myDriver.sendThreads()


        when (message.body?.reason) {
            StoppedEventReason.Breakpoint -> {
                for (id in message.body.hitBreakpointIds!!) {

                    pauseAndHitBreakpoint(id)
//
//                val breakpoint = this.myBreakpointHandler.getXBreakpoint(id)
//
//
//                val executionStack = CangJieExecutionStack(
//                    this,
//
//                    )
//
//                val suspendContext = CangJieSuspendContext(
//                    executionStack
//                )
//
//                session.breakpointReached(breakpoint!!, null, suspendContext)
//
//                val stack = CangJieExecutionStack(this, CjThread(1, "", "", "", ""), null, CjValue())
//
//                val suspendContext = CangJieSuspendContext(
//                    this,
//                    stack,
//                    CjThread(1, "", "", "", ""),
//                    CjFrame(1, "", "", 1, false, false, "")
//                )
//                val shouldSuspend = breakpoint?.let { session.breakpointReached(it, null, suspendContext) }
                }

            }

            StoppedEventReason.Step -> {
                pauseAndHitBreakpoint()
            }

            else -> {
                println()
            }
        }

//        if (message.body?.reason is StoppedEventReason.Breakpoint) {
////            断点停止
//
//
//        }

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
//            if (cjBreakpoint?.isRunToCursor?.get() == true) {
//                this.myDriver.sendContinue(getCurrentThreadId())
//                continue
//            }


            if (cjBreakpoint != null) {
                if (cjBreakpoint.isRunToCursor[breakpoint.line] == true) {
                    this.myDriver.sendContinue(getCurrentThreadId())

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

//        val breakpoint = getCjBreakpoint(message.body?.breakpoint?.source?.path)
//
//        if (breakpoint != null) {
//            val line = message.body?.breakpoint?.line
//            if (breakpoint.isRunToCursor[line] == true) {
//                this.myDriver.sendContinue(getCurrentThreadId())
//                breakpoint.isRunToCursor.remove(line)
//                line?.let { removeBreakpoint(breakpoint.filepath, it) }
//                return
//            }
//        }

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
            this.myDriver.sendBreakpointsToDebugger(this.breakpoints)

        }

        this.myDriver.sendFunctionBreakpointsToDebugger()
        this.myDriver.sendDataBreakpointsToDebugger()
        this.myDriver.sendInstructionBreakpointsToDebugger()

        myDriver.sendConfigurationDone()
    }

    override fun handleInitializeResponse(response: InitializeResponse) {
        if (response.success) {
            this.myDriver.sendLaunch()
        }
    }


    override fun handleConfigurationDoneResponse(response: ConfigurationDoneResponse) {
        if (response.success) {
            this.myDriver.sendDebugInConsole()
        }
    }

    override fun handleTerminatedEvent(event: TerminatedEvent) {


//        断开连接
//        this.myDriver.disconnect()
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
    ): CompletableFuture<EvaluateResponse> {

        isEvaluateLoaded = CompletableFuture()
        myDriver.sendEvaluate(expression, frameId, context)
        return isEvaluateLoaded
    }

      var isSetVariableLoaded = CompletableFuture<SetVariableResponse>()
    fun sendSetVariable(value: Variable,expression:String,parentScope:Int) {
        isSetVariableLoaded = CompletableFuture()
        myDriver.sendSetVariable(value,expression,parentScope)

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
    val id = frame.id

//    class RightAlignedTextContainer : ColoredTextContainer {
//        private val panel = JPanel(BorderLayout())
//        private val label = JLabel()
//
//        init {
//            panel.add(label, BorderLayout.EAST)
//        }
//
//        override fun append(text: String, attributes: SimpleTextAttributes) {
//            label.text = text
//            label.foreground = attributes.fgColor
//        }
//
//        fun getComponent(): JComponent = panel
//
//
//    }

    override fun customizePresentation(component: ColoredTextContainer) {
        val position = getSourcePosition()
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


    val variables: MutableList<Variable> get() = process.variables[frame.id] ?: mutableListOf()

    //表达式求值接口
    override fun getEvaluator(): XDebuggerEvaluator = CangJieDebuggerLanguageSupportManager.createEvaluator(this)


    val scopesList = mutableListOf<Scope>()
    override fun computeChildren(node: XCompositeNode) {
        val children = XValueChildrenList()


//        process.variables.map {
//            children.add(
//                it.name,
//                CangJieValue(
//                    it
//                )
//            )
//        }
        process.currentStackFrame.set(this)
        process.variablesReference.set(frame.id)
        process.isScopesLoaded = CompletableFuture<Boolean>()
        process.isVariablesLoaded[frame.id] = CompletableFuture<Boolean>()
        process.myDriver.sendScopes(frame.id)

        process.isVariablesLoaded[frame.id]?.thenAccept {
            scopesList.addAll(process.scopes)
            variables.map {
                children.add(
                    it.name,
                    CangJieValue(
                        process,
                        it,
                        process.scopes[0].variablesReference,
                        mySourcePosition,

                    )
                )
            }
            node.addChildren(children, true)


        }


//        node.addChildren(children, true)

    }


    private val mySourcePosition: XSourcePosition?

    init {

        val file = frame.source?.path
        mySourcePosition = if (file == null) null else process.createSourcePosition(
            file,

            frame.line - 1
        )


    }

    override fun getSourcePosition(): XSourcePosition? = this.mySourcePosition;


    val threadId get() = thread.id


//    val frameIndex get() = frame.index
}


class CangJieValue(
    val process: CangJieDebugProcess,
    val value: Variable,
    private val parentScope:Int,
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
//    override fun computeSourcePosition(navigatable: XNavigatable) {
//        val a = position?.let { XDebuggerUtil.getInstance().createPositionByOffset(position.file, it.offset) }
//        navigatable.setSourcePosition(a)
//    }

    override fun computeInlineDebuggerData(callback: XInlineDebuggerDataCallback): ThreeState {
        this.doComputeInlineDebuggerDataAsync(callback::computed);
//        callback.computed(object : XInlineDebuggerData() {
//            fun computePresentation( node: XValueNodeImpl,  place: XValuePlace?) {
//                node.setPresentation(null, myValue, false)
//            }
//        })

        return ThreeState.YES
    }

    //    fun getContextElement(sourcePosition: XSourcePosition): PsiElement? {
//        val document = FileDocumentManager.getInstance().getDocument(sourcePosition.file);
//        if (document != null) {
//            val psiFile = PsiDocumentManager.getInstance(process.session.project).getPsiFile(document);
//            return psiFile?.let { findContextElement(sourcePosition, document, it) };
//        }
//        return null
//
//    }
//
//    private fun findContextElement(
//        sourcePosition: XSourcePosition,
//        document: Document,
//        psiFile: PsiFile
//    ): PsiElement? {
//        val positionOffset = sourcePosition.offset
//        return if (!DocumentUtil.isValidOffset(positionOffset, document)) {
//            null
//        } else {
//            val lineEndOffset = document.getLineEndOffset(document.getLineNumber(positionOffset))
//            var offset = positionOffset
//
//
//            do {
//                val element: PsiElement = psiFile.findElementAt(offset) ?: break
//
//                if ((element !is PsiWhiteSpace && element !is PsiComment)) {
//                    return element
//                }
//                offset = element.textRange.endOffset + 1
//            } while (offset < lineEndOffset)
//
//            psiFile.findElementAt(positionOffset)
//        }
//    }
//    private fun resolveToDeclaration(ctx: PsiElement?, name: String): PsiElement? {
//        val composite = ctx?.ancestorOrSelf<CjElement>() ?: return null
//      return null
//    }
//
//      fun resolveToDeclaration(position: XSourcePosition?, `var`: String): PsiElement? {
//        val context = position?.let { getContextElement(it) }
//        return resolveToDeclaration(context, `var`)
//    }
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

    override fun computeTypeSourcePosition(navigatable: XNavigatable) {
        super.computeTypeSourcePosition(navigatable)
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


                process.sendSetVariable(value,expression.expression,parentScope)

                process.isSetVariableLoaded.thenAccept {


                    if (it.success){

                        value.value = it.body?.value.toString()
                        value.type = it.body?.type

                        callback.valueModified()

                    }else{
                        callback.errorOccurred(it.message.toString())

                    }
                }

            }
        }
    }

    override fun getEvaluationExpression(): String = value.name
}


class CangJieExecutionStack(
    val process: CangJieDebugProcess,
    val thread: Thread? = null,

    val frame: StackFrame? = null,
//    val returnValue: CjValue
) : XExecutionStack(thread?.name) {

    private val threadid: Long? = thread?.id


    override fun getTopFrame(): XStackFrame? {
        return myTopFrame
    }

    protected fun newFrame(frame: StackFrame?): CangJieStackFrame? {
        if (frame == null) return null

        return this.thread?.let { CangJieStackFrame(this.process, it, frame) }
    }

    private val stackFrames: MutableList<CangJieStackFrame?>
        get() {
//            if (process.stackTraces.isEmpty()) {
//
//                   requestStackFrames()
//
//            }
            return process.stackTraces[threadid]
//        .filter {
//
//
//        it.source?.path != null
//
//
//    }
                ?.map {
                    newFrame(it)
                }
                ?.toMutableList() ?: mutableListOf()
        }


    private val myTopFrame: CangJieStackFrame?
        get() {

            return if (stackFrames.isEmpty()) null
            else stackFrames.first()


        }


    private fun requestStackFrames(): MutableList<StackFrame>? {
        process.currentThread.set(thread)

        if (process.isStackTraceLoaded.containsKey(threadid!!)) {
            if (process.isStackTraceLoaded[threadid]?.isDone == true) {
                process.isStackTraceLoaded[threadid] = CompletableFuture<Boolean>()

                process.myDriver.sendStacktrace(threadid)
            }
        } else {
            process.isStackTraceLoaded[threadid] = CompletableFuture<Boolean>()

            process.myDriver.sendStacktrace(threadid)
        }


// 等待返回
        process.isStackTraceLoaded[threadid]?.get()
        return process.stackTraces[threadid]
    }

    override fun computeStackFrames(firstFrameIndex: Int, container: XStackFrameContainer?) {

        if (stackFrames.isEmpty()) {

            requestStackFrames()

//            process.currentThread.set(thread)
//            process.isStackTraceLoaded = CompletableFuture<Boolean>()
//            process.myDriver.sendStacktrace(threadid!!)
//            process.isStackTraceLoaded.thenAccept {
//                container?.addStackFrames(stackFrames, true)
//            }
            container?.addStackFrames(stackFrames, true)
        } else {
            if (firstFrameIndex < stackFrames.size) {
                container?.addStackFrames(stackFrames, true)
            }
        }


    }
}

/**
 * 暂停上下文
 */
class CangJieSuspendContext(
    val process: CangJieDebugProcess,
//    val currentStack: CangJieExecutionStack,

    val threads: List<Thread> = listOf(),
//    val activeThread: CjThread,
//    val preferableFrameToSelect: CjFrame
) : XSuspendContext() {

    val executionStacks: MutableList<CangJieExecutionStack> = mutableListOf()


    var activeIndex = 0

    val activeThread get() = executionStacks[activeIndex]

    init {

        executionStacks.addAll(threads.map { CangJieExecutionStack(process, it) })

    }


    override fun computeExecutionStacks(container: XExecutionStackContainer?) {

        container?.addExecutionStack(executionStacks, true)
    }

    override fun getActiveExecutionStack(): XExecutionStack {
        return executionStacks[activeIndex]
    }
}


//open class CjThread
//    (
//    val id: Long,
//    val name: String?,
//    val state: String?,
//    val workQueue: String?,
//
//    val tid: String?,
//    val frozen: Boolean = false
//) {
//
//    @get:NlsSafe
//    open val displayName: String
//        get() {
//            val builder = StringBuilder()
//            builder.append("Thread-").append(this.id)
//            this.workQueue?.let { builder.append("-<").append(it).append(">") }
//            this.name?.let { builder.append("-[").append(it).append("]") }
//            this.tid?.let { builder.append(" (").append(it).append(")") }
//            return builder.toString()
//        }
//}
//
//class CjFrame(
//    val index: Int,
//    val function: String?,
//    val file: String?,
//    val line: Int,
//    val optimized: Boolean,
//    val inlined: Boolean,
//    val module: String?
//)
//
//class CjValue : UserDataHolderBase()
//
//class LLMissingThread :
//    CjThread(-1L, null, null, null, null) {
//    override val displayName: String
//        get() = "Thread is not available"
//}


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