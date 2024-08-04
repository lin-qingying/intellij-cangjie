package com.linqingying.cangjie.dapDebugger.runconfig

import com.linqingying.cangjie.dapDebugger.runconfig.CjDebugRunnerUtils.ERROR_MESSAGE_TITLE
import com.linqingying.cangjie.ide.run.cjpm.CjExecutableRunner
import com.linqingying.cangjie.ide.run.cjpm.CjpmCommandConfiguration
import com.linqingying.cangjie.ide.run.cjpm.CjpmRunStateBase
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.ui.RunContentDescriptor


class CjDebugRunner : CjDebugRunnerBase() {

    override fun canRun(executorId: String, profile: RunProfile): Boolean {


        return super.canRun(executorId, profile) &&
                profile is CjpmCommandConfiguration
    }



}
