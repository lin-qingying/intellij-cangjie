package com.huawei.cangjie.idea.debugger

import com.huawei.cangjie.idea.debugger.dap.DapInitializeRequest
import com.huawei.cangjie.idea.debugger.socket.DebugSocket
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.ui.ExecutionConsole
import com.intellij.xdebugger.XDebugProcess
import com.intellij.xdebugger.XDebugSession
import com.intellij.xdebugger.evaluation.XDebuggerEditorsProvider
import com.intellij.xdebugger.frame.XSuspendContext
import org.jetbrains.annotations.Debug

class CangJieDebugProcess(session: XDebugSession, val state: RunProfileState) : XDebugProcess(session) {

    val processHandler = CangJieDebuggerServerManager.getDebugServerProcess()

    //    调试器的socket
    var debuggerSocket: DebugSocket? = null


    val myEditorsProvider = CangJieDebuggerEditorsProvider()

    override fun getEditorsProvider(): XDebuggerEditorsProvider = myEditorsProvider

    override fun stop() {
        println()
    }

    override fun sessionInitialized() {
//        将DapInitializeData发送给服务器
        debuggerSocket = CangJieDebuggerServerManager.getDebugServerSocket()
        debuggerSocket!!.sendInitializeData()


    }

    override fun doGetProcessHandler(): ProcessHandler {
        return processHandler
    }

}
