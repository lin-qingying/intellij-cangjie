package com.huawei.cangjie.dapDebugger.runconfig.breakpoint//package com.huawei.cangjie.ide.debugger
//
import com.intellij.xdebugger.breakpoints.XBreakpointProperties


open class CangJieBreakpointProperties<T : CangJieBreakpointProperties<T>> : XBreakpointProperties<T>() {
    override fun getState(): T? {
        TODO("Not yet implemented")
    }

    override fun loadState(state: T) {
        TODO("Not yet implemented")
    }

}

open class CangJieLineBreakpointProperties :
    CangJieBreakpointProperties<CangJieLineBreakpointProperties>() {


}

