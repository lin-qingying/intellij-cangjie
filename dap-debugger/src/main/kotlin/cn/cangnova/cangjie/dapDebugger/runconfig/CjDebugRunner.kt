package cn.cangnova.cangjie.dapDebugger.runconfig

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.ui.RunContentDescriptor

import cn.cangnova.cangjie.ide.run.cjpm.CjExecutableRunner
import cn.cangnova.cangjie.ide.run.cjpm.CjpmCommandConfiguration
import cn.cangnova.cangjie.ide.run.cjpm.CjpmRunStateBase
private const val RUNNER_ID = "CjDebugRunner"

class CjDebugRunner : CjExecutableRunner(DefaultDebugExecutor.EXECUTOR_ID,CjDebugRunnerUtils. ERROR_MESSAGE_TITLE) {
    override fun getRunnerId(): String = RUNNER_ID
    override fun canRun(executorId: String, profile: RunProfile): Boolean {


        return super.canRun(executorId, profile) &&
                profile is CjpmCommandConfiguration 
    }

    override fun showRunContent(
        state: CjpmRunStateBase,
        environment: ExecutionEnvironment,
        runExecutable: GeneralCommandLine
    ): RunContentDescriptor = CjDebugRunnerUtils.showRunContent(state, environment, runExecutable)

}

