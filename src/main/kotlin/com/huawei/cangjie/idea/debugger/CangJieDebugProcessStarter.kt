package com.huawei.cangjie.idea.debugger

import com.intellij.execution.configurations.RunProfileState
import com.intellij.xdebugger.XDebugProcess
import com.intellij.xdebugger.XDebugProcessStarter
import com.intellij.xdebugger.XDebugSession

class CangJieDebugProcessStarter(val state: RunProfileState) : XDebugProcessStarter() {
    override fun start(session: XDebugSession): XDebugProcess {
        return CangJieDebugProcess(session, state)
    }
}
