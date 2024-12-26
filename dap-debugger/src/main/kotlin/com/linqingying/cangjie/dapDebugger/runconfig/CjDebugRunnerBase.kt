package com.linqingying.cangjie.dapDebugger.runconfig

import com.linqingying.cangjie.dapDebugger.runconfig.CjDebugRunnerUtils.ERROR_MESSAGE_TITLE
import com.linqingying.cangjie.ide.run.cjpm.CjExecutableRunner
import com.linqingying.cangjie.ide.run.cjpm.CjpmRunStateBase
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.ui.RunContentDescriptor

abstract class CjDebugRunnerBase : CjExecutableRunner(DefaultDebugExecutor.EXECUTOR_ID, ERROR_MESSAGE_TITLE) {

    override fun getRunnerId(): String  = RUNNER_ID
    override fun doExecute(state: RunProfileState, environment: ExecutionEnvironment): RunContentDescriptor? {
        return super.doExecute(state, environment)
    }

    override fun showRunContent(
        state: CjpmRunStateBase,
        environment: ExecutionEnvironment,
        runExecutable: GeneralCommandLine
    ): RunContentDescriptor?  = CjDebugRunnerUtils.showRunContent(state, environment, runExecutable)

    companion object {
        const val RUNNER_ID: String = "CjDebugRunner"
    }
}
