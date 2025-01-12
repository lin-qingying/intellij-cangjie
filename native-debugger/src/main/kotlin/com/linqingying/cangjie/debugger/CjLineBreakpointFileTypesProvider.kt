package com.linqingying.cangjie.debugger

import com.jetbrains.cidr.execution.debugger.breakpoints.CidrLineBreakpointFileTypesProvider
import com.linqingying.cangjie.lang.CangJieFileType

class CjLineBreakpointFileTypesProvider : CidrLineBreakpointFileTypesProvider {
    override fun getFileTypes() =        setOf(CangJieFileType.INSTANCE)

}
