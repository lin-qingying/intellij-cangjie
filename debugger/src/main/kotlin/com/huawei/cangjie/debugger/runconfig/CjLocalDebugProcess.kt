package com.huawei.cangjie.debugger.runconfig

import com.intellij.execution.filters.Filter
import com.intellij.execution.filters.TextConsoleBuilder
import com.intellij.xdebugger.XDebugSession
import com.jetbrains.cidr.execution.Installer
import com.jetbrains.cidr.execution.RunParameters
import com.jetbrains.cidr.execution.debugger.CidrLocalDebugProcess
import com.jetbrains.cidr.execution.debugger.backend.DebuggerDriverConfiguration
import com.jetbrains.cidr.execution.debugger.backend.lldb.LLDBDriverConfiguration

class CjDebugRunParameters(    val emulateTerminal: Boolean): RunParameters() {
    override fun getInstaller(): Installer {
        TODO("Not yet implemented")
    }

    override fun getDebuggerDriverConfiguration(): DebuggerDriverConfiguration {
     return   LLDBDriverConfiguration()
    }

    override fun getArchitectureId(): String? = null
}

class CjLocalDebugProcess(val runParameters: CjDebugRunParameters,
                          debugSession: XDebugSession,
                          consoleBuilder: TextConsoleBuilder,) : CidrLocalDebugProcess(runParameters, debugSession, consoleBuilder, { Filter.EMPTY_ARRAY }, runParameters.emulateTerminal)