package com.huawei.cangjie.idea.debugger

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.xdebugger.breakpoints.XLineBreakpointType

abstract class CangJieLineBreakpointTypeBase<P : CangJieLineBreakpointProperties>(id: String, title: String) :
    XLineBreakpointType<P>(id, title), CangJieBreakpointType {


}
