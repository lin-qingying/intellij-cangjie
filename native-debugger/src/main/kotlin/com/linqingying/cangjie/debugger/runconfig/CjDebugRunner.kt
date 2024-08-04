package com.linqingying.cangjie.debugger.runconfig


import com.linqingying.cangjie.ide.run.cjpm.BuildResult
import com.linqingying.cangjie.ide.run.cjpm.CjpmCommandConfiguration
import com.linqingying.cangjie.ide.run.cjpm.CjpmRunStateBase

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.openapi.project.Project
import com.intellij.xdebugger.XDebugProcess
import com.intellij.xdebugger.XDebugProcessStarter
import com.intellij.xdebugger.XDebugSession
import com.intellij.xdebugger.XDebuggerManager

class CjDebugRunner: CjDebugRunnerBase() {
    override fun canRun(executorId: String, profile: RunProfile): Boolean =
        super.canRun(executorId, profile) &&
                profile is CjpmCommandConfiguration






    override fun checkToolchainSupported(project: Project, host: String): BuildResult.ToolchainError? =
        CjDebugRunnerUtils.checkToolchainSupported(project, host)

    override fun checkToolchainConfigured(project: Project): Boolean =
        CjDebugRunnerUtils.checkToolchainConfigured(project)
}
