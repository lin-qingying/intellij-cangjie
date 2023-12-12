package com.debugger.runconfig.breakpoint//package com.huawei.cangjie.idea.debugger
//
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.xdebugger.breakpoints.XBreakpointProperties
import com.intellij.xdebugger.breakpoints.XLineBreakpointType

abstract class CangJieLineBreakpointTypeBase<P : XBreakpointProperties<*>>(id: String, title: String) :
    XLineBreakpointType<P>(id, title) {


}
