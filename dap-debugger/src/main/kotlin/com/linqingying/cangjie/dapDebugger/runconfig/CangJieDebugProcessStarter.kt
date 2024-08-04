package com.linqingying.cangjie.dapDebugger.runconfig

import com.linqingying.cangjie.ide.run.cjpm.CjpmRunStateBase
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.xdebugger.XDebugProcess
import com.intellij.xdebugger.XDebugProcessStarter
import com.intellij.xdebugger.XDebugSession

//class CangJieDebugProcessStarter(val state: CjpmRunStateBase) : XDebugProcessStarter() {
//    override fun start(session: XDebugSession): XDebugProcess =
//        CangJieDebugProcess(session, state).apply {
//            ProcessTerminatedListener.attach(processHandler, session.project)
//
//        }
//
//}
