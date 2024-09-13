package com.huawei.cangjie.resolve.calls.inference.components

import com.huawei.cangjie.resolve.calls.model.PostponedResolvedAtomMarker
import com.huawei.cangjie.types.model.CangJieTypeMarker

class TypeVariableDirectionCalculator(
    private val c: VariableFixationFinder.Context,
    private val postponedCjPrimitives: List<PostponedResolvedAtomMarker>,
    topLevelType: CangJieTypeMarker
) {
    enum class ResolveDirection {
        TO_SUBTYPE,
        TO_SUPERTYPE,
        UNKNOWN
    }
}
