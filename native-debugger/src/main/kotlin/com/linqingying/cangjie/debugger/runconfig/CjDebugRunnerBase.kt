package cn.cangnova.cangjie.debugger.runconfig

import cn.cangnova.cangjie.debugger.runconfig.CjDebugRunnerUtils.ERROR_MESSAGE_TITLE
import cn.cangnova.cangjie.ide.run.cjpm.CjExecutableRunner
import cn.cangnova.cangjie.ide.run.cjpm.CjpmRunStateBase
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.ui.RunContentDescriptor
 abstract class CjDebugRunnerBase : CjExecutableRunner(DefaultDebugExecutor.EXECUTOR_ID, ERROR_MESSAGE_TITLE) {
    override fun getRunnerId(): String = RUNNER_ID

    override fun showRunContent(
        state: CjpmRunStateBase,
        environment: ExecutionEnvironment,
        runExecutable: GeneralCommandLine
    ): RunContentDescriptor? = CjDebugRunnerUtils.showRunContent(state, environment, runExecutable)

    companion object {
        const val RUNNER_ID: String = "CjDebugRunner"
    }
}
