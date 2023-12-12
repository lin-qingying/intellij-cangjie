package dap

import dap.protocol.ProtocolMessage
import dap.request.InitializeRequest
import dap.request.LaunchRequest
import dap.request.Request
import dap.response.CancelResponse
import dap.response.ErrorResponse
import dap.response.InitializeResponse
import dap.response.Response
import dap.type.MessageCommand
import dap.type.PathFormat
import dap.type.adapter.moshi
import dap.type.arguments.InitializeRequestArguments
import dap.type.arguments.LaunchRequestArguments
import dap.type.serializer.format
import kotlinx.serialization.encodeToString
import java.io.Writer
import java.net.Socket

const val TWO_CRLF = "\r\n\r\n"
const val ONE_CRLF = "\r\n"


//val LOG =

inline fun <reified T : Request> Writer.write(message: T) {
    val json = format.encodeToString(message)
    val size = json.toByteArray().size
    this.write("Content-Length: ${size}$TWO_CRLF${json}")
    this.flush()

}

class DebugSocket {

    var seq = 1

    var request_seq = 0

    val socket = Socket("localhost", 65500)


    val reader = socket.getInputStream().bufferedReader()

    val writer = socket.getOutputStream().bufferedWriter()


    private inline fun <reified T : Request> send(message: T): Response? {
//        this.socket.write(`Content-Length: ${Buffer.byteLength(e, "utf8")}${d.TWO_CRLF}${e}`, "utf8")

//        writer?.write("Content-Length: ${message.toByteArray().size}$TWO_CRLF$message")

        println("向调试适配器发送消息:$message")


        writer.write(message)
        writer.flush()







        return null
//        return read()


    }

    val thread = Thread {
        while (true) {
            val message = read()


            when (message) {

                is Response -> {
                    when (message) {
                        is InitializeResponse -> {
                            if (message.success) {
                                sendLaunch()
                            }


                        }

                        is ErrorResponse -> {

                        }

                        is CancelResponse -> {

                        }
                    }


                }

                else -> {
                    println("空消息")
                }

            }


        }
    }

    fun start() {
//        启动一个kotlin线程

        thread.start()
    }


    fun read(): ProtocolMessage? {
        val str = reader.readLine()
        val arr = str.split(":")

        if (arr.size < 2) return null


        val contentLength = arr[1].trim().toInt() + ONE_CRLF.length
//        读2位 把\r\n读掉
//        val charArray = CharArray(2)
//        reader.read(CharArray(2), 0, 2)

        val charArray = CharArray(contentLength)
        reader.read(charArray, 0, contentLength)

        val jsonstr = String(charArray).trim().trimEnd('\r', '\n', ' ', '\u0000')
        val response = moshi.adapter(ProtocolMessage::class.java).fromJson(jsonstr)


        seq = response?.seq ?: seq

        if (response is Response) {
            request_seq = response.request_seq
        }
        println("收到来自调试适配器的消息：$response")

        return response
    }

    fun sendLaunch() {
        val request = LaunchRequest(
            seq = ++seq,
            arguments = LaunchRequestArguments(
                buildBeforeLaunch = true,
                externalConsole = true,
                name = "Cangjie Debug (cjdb): launch",
                program = "d:/Code/Cj/test22/test22/build/bin/main.exe",
                request = MessageCommand.launch,
                type = "cangjieDebug"
            )
        )

        send(request)
    }

    fun sendInit() {
        val init = InitializeRequest(
            InitializeRequestArguments(
                adapterID = "cangjieDebug",
                clientId = "intellij_dap_client[ThreadId(1)]",
                clientName = "Intellij IDEA DAP Client",
                columnsStartAt1 = true,
                linesStartAt1 = true,
                locale = "zh_CN",
                pathFormat = PathFormat.Path,
                supportsInvalidatedEvent = false,
                supportsMemoryEvent = false,
                supportsMemoryReferences = false,
                supportsProgressReporting = false,
                supportsRunInTerminalRequest = true,
                supportsVariablePaging = false,
                supportsVariableType = false
            )
        )
        send(init)
    }
}


inline fun <reified T : Request> b(m: T) {
    val a = format.encodeToString(m)
    println(a)
}

fun main() {
//    val request = LaunchRequest(
////        type =  MessageType.request,
//        seq = 2,
//        arguments = LaunchRequestArguments(
//            buildBeforeLaunch = true,
//            externalConsole = true,
//            name = "Cangjie Debug (cjdb): launch",
//            program = "d:/Code/Cj/test22/test22/build/bin/main.exe",
//            request = MessageCommand.launch,
//            type = "cangjieDebug"
//        )
//    )
//
//    println(format.encodeToString(request))
////
    val socket = DebugSocket()

    socket.sendInit()

    socket.start()


//    val str = """{
//    "body": {
//        "additionalModuleColumns": [],
//        "exceptionBreakpointFilters": [],
//        "supportTerminateDebuggee": true,
//        "supportedChecksumAlgorithms": [],
//        "supportedTimeTravelRecordPointTypes": [
//            "step",
//            "breakpoint",
//            "function breakpoint",
//            "instruction breakpoint",
//            "data breakpoint",
//            "exception"
//        ],
//        "supportsConditionalBreakpoints": true,
//        "supportsConfigurationDoneRequest": true,
//        "supportsDataBreakpoints": true,
//        "supportsDisassembleRequest": true,
//        "supportsFunctionBreakpoints": true,
//        "supportsHitConditionalBreakpoints": true,
//        "supportsInstructionBreakpoints": true,
//        "supportsReadMemoryRequest": true,
//        "supportsSetExecutionPoint": true,
//        "supportsSetVariable": true,
//        "supportsSteppingGranularity": true,
//        "supportsTimeTravelDebugging": false,
//        "supportsWriteMemoryRequest": true
//    },
//    "command": "initialize",
//    "request_seq": 1,
//    "seq": 1,
//    "success": true,
//    "type": "response"
//}"""
//
//    val response = moshi.adapter(ProtocolMessage::class.java).fromJson(str)
//    println(response)

//    val init = InitializeRequest(
//        InitializeRequestArguments(
//            adapterID = "cangjieDebug",
//            clientId = "intellij_dap_client[ThreadId(1)]",
//            clientName = "Intellij IDEA DAP Client",
//            columnsStartAt1 = true,
//            linesStartAt1 = true,
//            locale = "zh_CN",
//            pathFormat = PathFormat.Path,
//            supportsInvalidatedEvent = false,
//            supportsMemoryEvent = false,
//            supportsMemoryReferences = false,
//            supportsProgressReporting = false,
//            supportsRunInTerminalRequest = true,
//            supportsVariablePaging = false,
//            supportsVariableType = false
//        )
//    )


//
//    println(moshi.adapter(InitializeRequest::class.java).toJson(init))
//    b(init)

//    {
//    "arguments": {
//        "adapterID": "cangjieDebug",
//        "clientId": "vnext_dap_client[ThreadId(1)]",
//        "clientName": "vNext DAP Client",
//        "columnsStartAt1": true,
//        "linesStartAt1": true,
//        "locale": "en_US",
//        "pathFormat": "path",
//        "supportsInvalidatedEvent": false,
//        "supportsMemoryEvent": false,
//        "supportsMemoryReferences": false,
//        "supportsProgressReporting": false,
//        "supportsRunInTerminalRequest": true,
//        "supportsVariablePaging": false,
//        "supportsVariableType": false
//    },
//    "command": "initialize",
//    "seq": 1,
//    "type": "request"
//}


}


fun a() {


    val list = listOf(1, 2, 34)
    val a = run loop@{
        for (i in 0..list.size) {
            return@loop 1
        }
    }
}
