package cn.cangnova.cangjie.debugger

import com.jetbrains.cidr.execution.debugger.breakpoints.CidrLineBreakpointFileTypesProvider
import cn.cangnova.cangjie.lang.CangJieFileType

class CjLineBreakpointFileTypesProvider : CidrLineBreakpointFileTypesProvider {
    override fun getFileTypes() =        setOf(CangJieFileType.INSTANCE)

}
