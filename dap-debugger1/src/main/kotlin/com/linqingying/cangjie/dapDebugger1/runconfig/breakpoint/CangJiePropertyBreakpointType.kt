//package com.linqingying.cangjie.dapDebugger1.runconfig.breakpoint
//
//import com.intellij.xdebugger.breakpoints.XBreakpointProperties
//import com.intellij.xdebugger.breakpoints.XBreakpointType
//import com.intellij.xdebugger.evaluation.XDebuggerEditorsProvider
//import com.linqingying.cangjie.dapDebugger1.runconfig.CangJieDebuggerEditorsProvider
//
//class CangJiePropertyBreakpointType : XBreakpointType<CangJiePropertyBreakpoint, CangJiePropertyBreakpointType.Properties>(
//    ID, "CangJie Property Breakpoint"
//) {
//    class Properties : XBreakpointProperties<Properties>() {
//        var propertyName: String = ""
//        var accessType: AccessType = AccessType.READ_WRITE
//
//        override fun getState(): Properties = this
//        override fun loadState(state: Properties) {
//            propertyName = state.propertyName
//            accessType = state.accessType
//        }
//    }
//
//    enum class AccessType {
//        READ, WRITE, READ_WRITE
//    }
//
//    override fun createBreakpoint(properties: Properties): CangJiePropertyBreakpoint {
//        return CangJiePropertyBreakpoint(properties)
//    }
//
//    override fun createProperties(): Properties = Properties()
//
//    override fun getEditorsProvider(): XDebuggerEditorsProvider {
//        return CangJieDebuggerEditorsProvider()
//    }
//
//    companion object {
//        const val ID = "cangjie-property"
//    }
//}
