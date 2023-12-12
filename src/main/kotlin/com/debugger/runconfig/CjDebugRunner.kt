package com.debugger.runconfig

import com.huawei.cangjie.idea.run.cjpm.CjpmCommandConfiguration
import com.huawei.cangjie.idea.run.cjpm.CjpmRunStateBase
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.GenericProgramRunner
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.xdebugger.XDebuggerManager
import java.io.File

//class CjDebugRunner : CjDebugRunnerBase() {
//
//
//    override fun canRun(executorId: String, profile: RunProfile): Boolean =
//        super.canRun(executorId, profile) &&
//                profile is CjpmCommandConfiguration
//
//}

class CjDebugRunner : GenericProgramRunner<RunnerSettings>() {
    override fun getRunnerId(): String {
        return "CjDebugRunner"
    }


    override fun doExecute(state: RunProfileState, environment: ExecutionEnvironment): RunContentDescriptor? {
        if (state !is CjpmRunStateBase) return null
        val runExecutable = GeneralCommandLine().apply {
            exePath = state.project.basePath + "/build/bin/main.exe"
            workDirectory = state.project.basePath?.let { File(it) }
        }
        return CjDebugRunnerUtils.showRunContent(state, environment, runExecutable)
    }

    override fun canRun(executorId: String, profile: RunProfile): Boolean {
        return profile is CjpmCommandConfiguration
    }
}
