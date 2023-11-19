package com.huawei.cangjie.idea.debugger

import com.huawei.cangjie.idea.debugger.dap.DapInitializeRequest
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.ui.ExecutionConsole
import com.intellij.xdebugger.XDebugProcess
import com.intellij.xdebugger.XDebugSession
import com.intellij.xdebugger.evaluation.XDebuggerEditorsProvider
import com.intellij.xdebugger.frame.XSuspendContext

class CangJieDebugProcess(session: XDebugSession, val state: RunProfileState) : XDebugProcess(session) {

    val processHandler = CangJieDebuggerServerManager.getDebugServerProcess()


    val myEditorsProvider = CangJieDebuggerEditorsProvider()

    override fun getEditorsProvider(): XDebuggerEditorsProvider = myEditorsProvider

    override fun stop() {
println()
    }

    override fun sessionInitialized() {

//        将DapInitializeData发送给服务器
        val initializeRequest = DapInitializeRequest()
        processHandler.processInput.write(initializeRequest.toString().toByteArray())
    }

    override fun doGetProcessHandler(): ProcessHandler {
        return processHandler
    }

}
