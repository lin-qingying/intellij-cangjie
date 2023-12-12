package com.debugger.runconfig


import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.filters.Filter
import com.intellij.execution.filters.TextConsoleBuilder
import com.intellij.openapi.project.Project
import com.intellij.xdebugger.XDebugSession
//
//class CjLocalDebugProcess(
//    runParameters: CjDebugRunParameters,
//    debugSession: XDebugSession,
//    consoleBuilder: TextConsoleBuilder
//) : CidrLocalDebugProcess(
//    runParameters,
//    debugSession,
//    consoleBuilder,
//    { Filter.EMPTY_ARRAY },
//    runParameters.emulateTerminal
//) {
//    override fun isLibraryFrameFilterSupported() = false
//}
//
//
//class CjDebugRunParameters(
//    val project: Project,
//    private val cmd: GeneralCommandLine,
//
//
//    val emulateTerminal: Boolean = false
//) : RunParameters() {
//    override fun getInstaller(): Installer {
//        TODO("Not yet implemented")
//    }
//
//    override fun getDebuggerDriverConfiguration(): DebuggerDriverConfiguration {
//        TODO("Not yet implemented")
//    }
//
//    override fun getArchitectureId(): String? {
//        TODO("Not yet implemented")
//    }
//
//}
