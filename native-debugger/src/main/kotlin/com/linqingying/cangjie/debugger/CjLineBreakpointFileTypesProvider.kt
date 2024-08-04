package com.linqingying.cangjie.debugger

import com.linqingying.cangjie.lang.CangJieFileType
import com.jetbrains.cidr.execution.debugger.breakpoints.CidrLineBreakpointFileTypesProvider

class CjLineBreakpointFileTypesProvider : CidrLineBreakpointFileTypesProvider {
    override fun getFileTypes() = setOf(CangJieFileType)
}
