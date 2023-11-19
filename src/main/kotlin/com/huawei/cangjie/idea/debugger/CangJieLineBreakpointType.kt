package com.huawei.cangjie.idea.debugger

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.xdebugger.breakpoints.XBreakpointType
import com.intellij.xdebugger.breakpoints.XLineBreakpointType

class CangJieLineBreakpointType : CangJieLineBreakpointTypeBase<CangJieLineBreakpointProperties>(
    "cangjie-line",
    CangJieDebuggerCoreBundle.message("line.breakpoint.tab.title")
), CangJieBreakpointType {
    override fun createBreakpointProperties(file: VirtualFile, line: Int): CangJieLineBreakpointProperties {
        return CangJieLineBreakpointProperties()
    }

}
