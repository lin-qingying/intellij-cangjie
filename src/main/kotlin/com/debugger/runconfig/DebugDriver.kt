package com.debugger.runconfig

import com.debugger.backend.CjBreakpoint
import com.debugger.runconfig.message.MessageHandler
import com.huawei.cangjie.idea.project.CangJieProjectManager
import com.intellij.execution.process.BaseProcessHandler
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessListener
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.concurrency.QueueProcessor
import com.intellij.xdebugger.XDebugSession
import dap.protocol.ProtocolMessage
import dap.request.*
import dap.response.Response
import dap.response.RunInTerminalResponse
import dap.type.*
import dap.type.adapter.moshi
import dap.type.arguments.*
import dap.type.body.RunInTerminalResponseBody
import dap.type.serializer.format
import kotlinx.serialization.encodeToString

import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.OutputStream
import java.io.Writer
import java.net.Socket
import java.net.SocketException
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

import java.lang.Thread.sleep
import java.util.concurrent.CountDownLatch

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
) {

    val debugProcessHandler = CangJieDebuggerServerManager.getDebugServerProcess()


    private var seq = 1

    private var requestSeq = 0

    private var socket: Socket? = null


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

    val thread = Thread {
        val isWhile = AtomicBoolean(true)
        while (isWhile.get()) {


            if (socket?.isConnected == true) {
                if (socketContentState.get()) {
                    val message = read()

                    this.myMessageHandler.handleMessage(message!!)
                }

            } else {
                isWhile.set(false)
            }


        }
    }

    val sessionid = UUID.randomUUID().toString()

//    private val latch = CountDownLatch(1)

    init {
        if (runState) {
            this.content()
            this.initialize()

        } else {
            print("调试器启动失败")
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


    fun sendScopes(frameId: Int) {
        val request = ScopesRequest(
            seq = ++seq,
            arguments = ScopesArguments(
                frameId = frameId
            )
        )
        send(request)
    }

    fun sendStacktrace(threadId: Long, levels: Int = 20, startFrame: Int = 0) {
        val request = StackTraceRequest(
            seq = ++seq,
            arguments = StackTraceArguments(
                threadId = threadId,
                levels = levels,
                startFrame = startFrame
            )
        )
        send(request)
    }

    /**
     * 开启消息进程
     */
    private fun startReadMessageThread() {
//        启动一个kotlin线程

        thread.start()
    }

    //    @Synchronized
    fun read(): ProtocolMessage? {
        if (reader == null) return null
        val str = reader!!.readLine()
        val arr = str.split(":")

        if (arr.size < 2) return null


        val contentLength = arr[1].trim().toInt() + ONE_CRLF.length
//        读2位 把\r\n读掉
//        val charArray = CharArray(2)
//        reader.read(CharArray(2), 0, 2)

        val charArray = CharArray(contentLength)
        reader?.read(charArray, 0, contentLength)

        val jsonstr = String(charArray).trim().trimEnd('\r', '\n', ' ', '\u0000')
        val response = moshi.adapter(ProtocolMessage::class.java).fromJson(jsonstr)


//        seq = response?.seq ?: seq

        if (response is Response) {
            requestSeq = response.request_seq
        }
        LOG.info("接收消息：$response")

        return response
    }

    //    @Synchronized
    private inline fun <reified T> send(message: T): Response? where T : ProtocolMessage {
//        this.socket.write(`Content-Length: ${Buffer.byteLength(e, "utf8")}${d.TWO_CRLF}${e}`, "utf8")

//        writer?.write("Content-Length: ${message.toByteArray().size}$TWO_CRLF$message")
        if (socketContentState.get()) {

            LOG.info("发送消息：$message")


            writer?.write(message)
            writer?.flush()

        }

//        return read() as Response
        return null

    }


    fun sendLaunch() {
        val mainexe = (CangJieProjectManager.getCurrentProject()?.basePath + "/build/bin/main.exe")
        val request = LaunchRequest(
            seq = ++seq, arguments = LaunchRequestArguments(
                buildBeforeLaunch = true,
                externalConsole = false,
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


    fun content() {
//        latch.await() // This will block the current thread

        var retryCount = 0
        while (retryCount < MAX_RETRY_COUNT) {
            try {
                socket = Socket("127.0.0.1", CangJieDebuggerServerManager.DEBUGPORT)
                if (socket!!.isConnected) {
                    reader = socket!!.getInputStream().bufferedReader()
                    writer = socket!!.getOutputStream().bufferedWriter()
                    socketContentState.set(true)
                    startReadMessageThread()
//                        unblockContent()
                    break
                }
            } catch (e: SocketException) {

                retryCount++
                sleep(RETRY_DELAY_MS)
            }
        }

//        ApplicationManager.getApplication().executeOnPooledThread {
//            var retryCount = 0
//            while (retryCount < MAX_RETRY_COUNT) {
//                try {
//                    socket = Socket("127.0.0.1", CangJieDebuggerServerManager.DEBUGPORT)
//                    if (socket!!.isConnected) {
//                        reader = socket!!.getInputStream().bufferedReader()
//                        writer = socket!!.getOutputStream().bufferedWriter()
//                        socketContentState.set(true)
//                        startReadMessageThread()
////                        unblockContent()
//                        break
//                    }
//                } catch (e: SocketException) {
//
//                    retryCount++
//                    sleep(RETRY_DELAY_MS)
//                }
//            }
//            return@executeOnPooledThread
//        }
    }


    fun initialize() {
//        进行调试器配置
        sendInitializeData()


    }


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

    fun sendThreads() {
        val request = ThreadsRequest(
            seq = ++seq
        )
        send(request)
    }

    fun sendVariables(variablesReference: Int,count:Int = 1000,start:Int = 0) {
        val request = VariablesRequest(
            seq = ++seq,
            arguments = VariablesArguments(
                variablesReference = variablesReference,
                count = count,
                start = start
            )
        )
        send(request)

    }

    data class TargetStateTransition(
        val previousState: TargetState, val currentState: TargetState
    )


    companion object {
        val LOG = Logger.getInstance(DebugDriver::class.java)

        enum class TargetState {
            NOT_READY, RUNNING, SUSPENDED, FINISHING, FINISHED
        }

    }


}

fun String.toDapPath(): String {
//  去除file前缀 并且把windows路径转换为unix路径
    return this.substringAfter("file://").replace("\\", "/")
}



