package com.huawei.cangjie.debugger

import com.huawei.cangjie.lang.CangJieFileType.INSTANCE
import com.jetbrains.cidr.execution.debugger.breakpoints.CidrLineBreakpointFileTypesProvider

class CjLineBreakpointFileTypesProvider : CidrLineBreakpointFileTypesProvider {
    override fun getFileTypes() = setOf(CangJieFileType.INSTANCE)
}
