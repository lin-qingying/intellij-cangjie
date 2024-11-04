package com.linqingying.cangjie.resolve.calls.inference.components

import com.linqingying.cangjie.resolve.calls.model.PostponedResolvedAtomMarker
import com.linqingying.cangjie.types.model.CangJieTypeMarker

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
