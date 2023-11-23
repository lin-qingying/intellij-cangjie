package com.huawei.cangjie.idea.debugger.socket

import com.huawei.cangjie.idea.debugger.dap.DapInitializeRequest
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.Writer
import java.net.Socket

class DebugSocket(port: Int) {

    val TWO_CRLF = "\r\n\r\n"
    private val socket: Socket = Socket("127.0.0.1", port)

    private var reader: BufferedReader? = null
    private var writer: Writer? = null

    init {
        if (socket.isConnected) {

            reader = BufferedReader(socket.getInputStream().reader())
            writer = socket.getOutputStream().writer()


        } else {
            throw Exception("socket connect failed")
        }
    }


    /**
     * 向调试器发送初始化数据
     */
    fun sendInitializeData() {
        val DapInitializeRequest = DapInitializeRequest()
        send(DapInitializeRequest.toString())

    }


    private fun send(message: String) {
//        this.socket.write(`Content-Length: ${Buffer.byteLength(e, "utf8")}${d.TWO_CRLF}${e}`, "utf8")

        writer?.write("Content-Length: ${message.toByteArray().size}$TWO_CRLF$message")
        writer?.flush()


//        读取调试器的响应
        val data = reader?.let { DapBody(it) }


    }


    fun stop() {
        socket.close()
    }


}


class DapBody(reader: BufferedReader) {
    private var contentLength: Int = 0
    val body: Json = Json

    private var charArray: CharArray? = null

    init {


//        读第一行
        readHeader(reader)

//        读2位 把\r\n读掉
//        val charArray = CharArray(2)
        reader.read(CharArray(2), 0, 2)


        readBody(reader)


        println(charArray?.let { String(it) })

    }


    //    读取头部
    private fun readHeader(reader: BufferedReader) {
        val header = reader.readLine()
        contentLength = header.split(":")[1].trim().toInt()

        charArray = CharArray(contentLength)
    }

    //    读取body
    private fun readBody(reader: BufferedReader) {
        reader.read(charArray!!, 0, contentLength)

//        将body转换为json
        val json = String(charArray!!)
        val jsonBody = body.parseToJsonElement(json)
        println(jsonBody)


    }

}
