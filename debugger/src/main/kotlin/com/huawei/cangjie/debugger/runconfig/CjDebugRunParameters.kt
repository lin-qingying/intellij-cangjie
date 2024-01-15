package com.huawei.cangjie.debugger.runconfig

import com.huawei.cangjie.debugger.CjDebuggerDriverConfigurationProvider
import com.huawei.cangjie.debugger.CjLLDBDriverConfiguration
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.project.Project
import com.jetbrains.cidr.execution.Installer
import com.jetbrains.cidr.execution.RunParameters
import com.jetbrains.cidr.execution.TrivialInstaller
import com.jetbrains.cidr.execution.debugger.backend.DebuggerDriverConfiguration
import com.jetbrains.cidr.execution.debugger.backend.lldb.LLDBDriverConfiguration

class CjDebugRunParameters(
    val project: Project,
    private val cmd: GeneralCommandLine,
    private val isElevated: Boolean,
    val emulateTerminal: Boolean
) : RunParameters() {
    override fun getInstaller(): Installer = TrivialInstaller(cmd)



    override fun getArchitectureId(): String? = null

    override fun getDebuggerDriverConfiguration(): DebuggerDriverConfiguration {
        for (provider in CjDebuggerDriverConfigurationProvider.EP_NAME.extensionList) {
            return provider.getDebuggerDriverConfiguration(project, isElevated, emulateTerminal) ?: continue
        }
        return CjLLDBDriverConfiguration(isElevated, emulateTerminal)
    }
}
