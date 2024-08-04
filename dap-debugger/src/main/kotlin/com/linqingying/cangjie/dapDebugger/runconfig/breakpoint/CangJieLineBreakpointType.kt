package com.linqingying.cangjie.dapDebugger.runconfig.breakpoint//package com.linqingying.cangjie.ide.debugger

import com.linqingying.cangjie.dapDebugger.runconfig.CangJieDebuggerCoreBundle
import com.linqingying.cangjie.lang.CangJieFileType
import com.intellij.openapi.components.BaseState
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.xdebugger.breakpoints.XBreakpointProperties
import com.intellij.xdebugger.breakpoints.XLineBreakpointType

//class CangJieLineBreakpointType : CangJieLineBreakpointTypeBase<CangJieLineBreakpointProperties>(
//     ID,
//    CangJieDebuggerCoreBundle.message("line.breakpoint.tab.title")
//) {
//    override fun createBreakpointProperties(file: VirtualFile, line: Int): CangJieLineBreakpointProperties {
//        return CangJieLineBreakpointProperties()
//    }
//
//companion object{
//    val ID = "cangjie-line"
//}
//
//
//    override fun canPutAt(file: VirtualFile, line: Int, project: Project): Boolean {
//
//
//        return file.extension == "cj" && file.fileType == CangJieFileType
//    }
//
//}
class CangJieLineBreakpointType :
    XLineBreakpointType<Properties>(ID, CangJieDebuggerCoreBundle.message("line.breakpoint.tab.title")) {
    override fun createBreakpointProperties(file: VirtualFile, line: Int): Properties {
        return Properties()
    }

    override fun canPutAt(file: VirtualFile, line: Int, project: com.intellij.openapi.project.Project): Boolean {
        return file.extension == "cj" && file.fileType == CangJieFileType
    }

    companion object {
        val ID = "cangjie-line"
    }

}

class Properties :
    XBreakpointProperties<Properties.State>() {


    class State : BaseState()

    var myState = State()

    override fun getState(): State = myState

    override fun loadState(state: State) {
        this.myState = state
    }
}
