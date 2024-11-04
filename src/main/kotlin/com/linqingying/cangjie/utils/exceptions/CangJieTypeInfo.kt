package com.linqingying.cangjie.utils.exceptions

import com.linqingying.cangjie.resolve.calls.smartcasts.DataFlowInfo
import com.linqingying.cangjie.types.CangJieType

class CangJieTypeInfo @JvmOverloads constructor(
    val type: CangJieType?,
    val dataFlowInfo: DataFlowInfo,
    val jumpOutPossible: Boolean = false,
    val jumpFlowInfo: DataFlowInfo = dataFlowInfo
){
    fun clearType() = replaceType(null)
    fun replaceJumpOutPossible(jumpOutPossible: Boolean) =
        if (jumpOutPossible == this.jumpOutPossible) this else CangJieTypeInfo(type, dataFlowInfo, jumpOutPossible, jumpFlowInfo)
    fun replaceJumpFlowInfo(jumpFlowInfo: DataFlowInfo) =
        if (jumpFlowInfo == this.jumpFlowInfo) this else CangJieTypeInfo(type, dataFlowInfo, jumpOutPossible, jumpFlowInfo)

    fun replaceDataFlowInfo(dataFlowInfo: DataFlowInfo) = when (this.dataFlowInfo) {
        // Nothing changed
        dataFlowInfo -> this
        // Jump info is the same as data flow info: change both
        jumpFlowInfo -> CangJieTypeInfo(type, dataFlowInfo, jumpOutPossible, dataFlowInfo)
        // Jump info is not the same: change data flow info only
        else -> CangJieTypeInfo(type, dataFlowInfo, jumpOutPossible, jumpFlowInfo)
    }
    // NB: do not compare type with this.type because this comparison is complex and unstable
    fun replaceType(type: CangJieType?) = CangJieTypeInfo(type, dataFlowInfo, jumpOutPossible, jumpFlowInfo)

}
