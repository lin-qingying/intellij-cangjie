//package cn.cangnova.cangjie.dapDebugger.runconfig.breakpoint
//
//import com.intellij.xdebugger.breakpoints.XBreakpoint
//import org.eclipse.lsp4j.debug.DataBreakpoint
//
//class CangJiePropertyBreakpoint(
//    private val properties: CangJiePropertyBreakpointType.Properties
//) {
////    fun toDataBreakpoint(breakpoint: XBreakpoint<CangJiePropertyBreakpointType.Properties>): DataBreakpoint {
////        return DataBreakpoint().apply {
////            dataId = properties.propertyName
////            accessType = when (properties.accessType) {
////                CangJiePropertyBreakpointType.AccessType.READ -> "read"
////                CangJiePropertyBreakpointType.AccessType.WRITE -> "write"
////                CangJiePropertyBreakpointType.AccessType.READ_WRITE -> "readWrite"
////            }
////            condition = breakpoint.conditionExpression?.expression
////            hitCondition = breakpoint.conditionExpression?.expression
////        }
////    }
//}
