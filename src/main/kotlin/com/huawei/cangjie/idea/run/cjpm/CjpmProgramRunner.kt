package com.huawei.cangjie.idea.run.cjpm

import com.huawei.cangjie.idea.debugger.CangJieDebugProcessStarter
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.GenericProgramRunner
import com.intellij.xdebugger.XDebuggerManager

class CjpmProgramRunner : GenericProgramRunner<RunnerSettings>() {
    override fun canRun(executorId: String, profile: RunProfile): Boolean {
        return executorId == DefaultDebugExecutor.EXECUTOR_ID && profile is CjpmRunConfiguration
    }

    override fun execute(environment: ExecutionEnvironment, state: RunProfileState) {
        if (state is CjpmCommandLineState) {
            XDebuggerManager.getInstance(environment.project)
                .startSession(environment, CangJieDebugProcessStarter(state))
        }
    }

    override fun getRunnerId(): String = "CjpmProgramRunner"
}
