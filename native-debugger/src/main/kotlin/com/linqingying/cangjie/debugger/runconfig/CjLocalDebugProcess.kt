package cn.cangnova.cangjie.debugger.runconfig

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.filters.Filter
import com.intellij.execution.filters.TextConsoleBuilder
import com.intellij.openapi.project.Project
import com.intellij.xdebugger.XDebugSession
import com.jetbrains.cidr.execution.Installer
import com.jetbrains.cidr.execution.RunParameters
import com.jetbrains.cidr.execution.TrivialInstaller
import com.jetbrains.cidr.execution.debugger.CidrLocalDebugProcess
import com.jetbrains.cidr.execution.debugger.backend.DebuggerDriverConfiguration
import com.jetbrains.cidr.execution.debugger.backend.lldb.LLDBDriverConfiguration


class CjLocalDebugProcess(
    val runParameters: CjDebugRunParameters,
    debugSession: XDebugSession,
    consoleBuilder: TextConsoleBuilder,
) : CidrLocalDebugProcess(
    runParameters,
    debugSession,
    consoleBuilder,
    { Filter.EMPTY_ARRAY },
    runParameters.emulateTerminal
){
    override fun isLibraryFrameFilterSupported() = false

}
