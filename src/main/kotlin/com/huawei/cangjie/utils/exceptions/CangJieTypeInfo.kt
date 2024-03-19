package com.huawei.cangjie.utils.exceptions

import com.huawei.cangjie.resolve.calls.smartcasts.DataFlowInfo
import com.huawei.cangjie.types.CangJieType

class CangJieTypeInfo @JvmOverloads constructor(
    val type: CangJieType?,
    val dataFlowInfo: DataFlowInfo,
    val jumpOutPossible: Boolean = false,
    val jumpFlowInfo: DataFlowInfo = dataFlowInfo
)