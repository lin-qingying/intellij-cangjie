package com.linqingying.cangjie.debugger.runconfig


import com.intellij.execution.configurations.RunProfile
import com.intellij.openapi.project.Project
import com.linqingying.cangjie.ide.run.cjpm.BuildResult
import com.linqingying.cangjie.ide.run.cjpm.CjpmCommandConfiguration

class CjDebugRunner: CjDebugRunnerBase() {
    override fun canRun(executorId: String, profile: RunProfile): Boolean =
        super.canRun(executorId, profile) &&
                profile is CjpmCommandConfiguration






    override fun checkToolchainSupported(project: Project, host: String): BuildResult.ToolchainError? =
        CjDebugRunnerUtils.checkToolchainSupported(project, host)

    override fun checkToolchainConfigured(project: Project): Boolean =
        CjDebugRunnerUtils.checkToolchainConfigured(project)
}
