package org.cangnova.cangjie.dapDebugger.runconfig



import org.cangnova.cangjie.messages.CangJieBundle
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.xdebugger.XDebugProcess
import com.intellij.xdebugger.XDebugProcessStarter
import com.intellij.xdebugger.XDebugSession
import com.intellij.xdebugger.XDebuggerManager
import org.cangnova.cangjie.run.CangJieProgramRunState
import org.cangnova.cangjie.run.CangJieRunConfigurationBase
import org.cangnova.cangjie.run.CangJieRunState
import org.jetbrains.annotations.Nls

data class RunParameters(
    val command: GeneralCommandLine, val port: Int,

    val program: String, val runExecutable: GeneralCommandLine? = null, val state: RunProfileState? = null
)

object CjDebugRunnerUtils {


    val ERROR_MESSAGE_TITLE: String = CangJieBundle.message("unable.to.run.debugger")


    fun <T : CangJieRunConfigurationBase> showRunContent(
        state: CangJieRunState<T>,
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

                    }

            }
        ).runContentDescriptor
    }
}
