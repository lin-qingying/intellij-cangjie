package com.linqingying.cangjie.dapDebugger1.runconfig

import com.intellij.xdebugger.XDebugSession
import com.linqingying.cangjie.dapDebugger1.runconfig.breakpoint.CangJieBreakpointHandler
import org.eclipse.lsp4j.debug.launch.DSPLauncher
import org.eclipse.lsp4j.debug.services.IDebugProtocolServer
import org.eclipse.lsp4j.jsonrpc.Launcher
import java.net.Socket
import java.util.concurrent.Future

/**
 * DAP客户端连接管理
 */
class DapConnection(
    private val debugSession: XDebugSession,
    private val parameters: RunParameters,
    private val breakpointHandler: CangJieBreakpointHandler,
    private val host: String = "localhost"
) {
    val port = parameters.port
    private lateinit var server: IDebugProtocolServer
    private lateinit var socket: Socket
    private lateinit var listenFuture: Future<Void>
    private lateinit var client: DapClient
    private lateinit var launcher: Launcher<IDebugProtocolServer>

    /**
     * 连接到调试适配器服务器
     */
    fun connect() {
        var retryCount = 0
        val maxRetries = 10 // 最大重试次数
        val retryDelayMs = 500L // 重试间隔（毫秒）

        while (retryCount < maxRetries) {
            try {
                // 尝试建立连接
                socket = Socket(host, port)
                if (socket.isConnected) {
                    println("连接成功")

                    // 如果连接成功，继续初始化
                    client = DapClient(debugSession, this, parameters, breakpointHandler)

                    launcher = DSPLauncher.createClientLauncher(
                        client,
                        socket.inputStream,
                        socket.outputStream
                    )
                    server = launcher.remoteProxy

                    listenFuture = launcher.startListening()

                    sendInitializeRequest()
                    return
                } else {
                    println("连接失败")
                    throw RuntimeException("Failed to connect to DAP server at $host:$port")
                }


            } catch (e: Exception) {
                retryCount++
                if (retryCount >= maxRetries) {
                    throw RuntimeException(
                        "Failed to connect to DAP server at $host:$port after $maxRetries attempts",
                        e
                    )
                }

                // 等待一段时间后重试
                Thread.sleep(retryDelayMs)
            }
        }
    }

    /**
     * 发送初始化请求
     */
    private fun sendInitializeRequest() {
        val initializeRequest = org.eclipse.lsp4j.debug.InitializeRequestArguments()
        initializeRequest.apply {
            clientID = "intellij-cangjie-dap"
            clientName = "Intellij CangJie Debugger"
            adapterID = "intellij-cangjie-debug"
            linesStartAt1 = true
            columnsStartAt1 = true
            supportsVariableType = true
            supportsVariablePaging = true
        }

        server.initialize(initializeRequest)
            .thenAccept { response ->
            // 初始化完成
//            client.initialized()
                val launchArgs = mapOf(
                    "type" to "cangjieDebug",
                    "request" to "launch",
                    "name" to "Intellij Cangjie Debug",
                    // 如果需要其他参数，可以在这里添加
                    "program" to parameters.program
                )

               server.launch(launchArgs).thenAccept {
                    // launch 请求完成
                    println("Launch request completed")
                }
        }
    }

    /**
     * 获取调试协议服务器代理
     */
    fun getServer(): IDebugProtocolServer {
        if (!::server.isInitialized) {
            throw IllegalStateException("Not connected to DAP server. Call connect() first.")
        }
        return server
    }

    /**
     * 断开连接
     */
    fun disconnect() {
        if (::listenFuture.isInitialized) {
            listenFuture.cancel(true)
        }
        if (::socket.isInitialized && !socket.isClosed) {
            socket.close()
        }
    }
}
