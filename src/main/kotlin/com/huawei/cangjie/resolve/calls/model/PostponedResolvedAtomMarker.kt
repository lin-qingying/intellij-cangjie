package com.huawei.cangjie.resolve.calls.model

import com.huawei.cangjie.types.model.CangJieTypeMarker

interface PostponedResolvedAtomMarker {
    val inputTypes: Collection<CangJieTypeMarker>
    val outputType: CangJieTypeMarker?
    val expectedType: CangJieTypeMarker?
    val analyzed: Boolean
}
