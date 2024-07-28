package com.huawei.cangjie.dapDebugger.runconfig

import com.huawei.cangjie.CangJieBundle

import com.huawei.cangjie.ide.run.cjpm.CjpmRunStateBase
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.xdebugger.XDebugProcess
import com.intellij.xdebugger.XDebugProcessStarter
import com.intellij.xdebugger.XDebugSession
import com.intellij.xdebugger.XDebuggerManager
import org.jetbrains.annotations.Nls


object CjDebugRunnerUtils {


    val ERROR_MESSAGE_TITLE: String = CangJieBundle.message("unable.to.run.debugger")


    fun showRunContent(
        state: CjpmRunStateBase,
        environment: ExecutionEnvironment,
        runExecutable: GeneralCommandLine
    ): RunContentDescriptor {
        val runParameters = RunParameters(

            CangJieDebuggerServerManager.getCommandLine(state.project),
            CangJieDebuggerServerManager.DEBUGPORT,
            runExecutable.commandLineString

        )



        return XDebuggerManager.getInstance(environment.project).startSession(
            environment, object : XDebugProcessStarter() {
                override fun start(session: XDebugSession): XDebugProcess =
                    CangJieDebugProcess(runParameters, session).apply {
                        start()
                    }

            }
        ).runContentDescriptor
    }
}
