package com.huawei.cangjie.resolve.calls.inference.components

import com.huawei.cangjie.resolve.calls.inference.model.Constraint
import com.huawei.cangjie.resolve.calls.inference.model.ConstraintKind
import com.huawei.cangjie.types.model.CangJieTypeMarker
import com.huawei.cangjie.types.model.TypeSystemInferenceExtensionContext
import com.huawei.cangjie.types.model.TypeSystemInferenceExtensionContextDelegate

class TrivialConstraintTypeInferenceOracle private constructor(context: TypeSystemInferenceExtensionContext) :
    TypeSystemInferenceExtensionContext by context {

    // This constructor is used for injection only in old FE
    constructor(context: TypeSystemInferenceExtensionContextDelegate) : this(context as TypeSystemInferenceExtensionContext)
    // The idea is to add knowledge that constraint `Nothing(?) <: T` is quite useless and
    // it's totally fine to go and resolve postponed argument without fixation T to Nothing(?).
    // In other words, constraint `Nothing(?) <: T` is *not* proper
    fun isNotInterestingConstraint(constraint: Constraint): Boolean {
        return constraint.kind == ConstraintKind.LOWER && constraint.type.typeConstructor().isNothingConstructor()
    }

    // This function controls the choice between sub and super result type
    // Even that Nothing(?) is the most specific type for subtype, it doesn't bring valuable information to the user,
    // therefore it is discriminated in favor of supertype
    fun isSuitableResultedType(
        resultType: CangJieTypeMarker
    ): Boolean {
        return !resultType.typeConstructor().isNothingConstructor() || (/*isK2 &&*/ resultType.isDynamic())
    }
}
