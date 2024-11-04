package com.linqingying.cangjie.resolve.calls.inference.components

import com.linqingying.cangjie.resolve.calls.inference.model.Constraint
import com.linqingying.cangjie.resolve.calls.inference.model.ConstraintKind
import com.linqingying.cangjie.types.model.CangJieTypeMarker
import com.linqingying.cangjie.types.model.SimpleTypeMarker
import com.linqingying.cangjie.types.model.TypeSystemInferenceExtensionContext
import com.linqingying.cangjie.types.model.TypeSystemInferenceExtensionContextDelegate

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
    private fun CangJieTypeMarker.isNothingOrNullableNothing(): Boolean =
        typeConstructor().isNothingConstructor()


    // It's possible to generate Nothing-like constraints inside incorporation mechanism:
    // For instance, when two type variables are in subtyping relation `T <: K`, after incorporation
    // there will be constraint `approximation(out K) <: K` => `Nothing <: K`, which is innocent
    // but can change result of the constraint system.
    // Therefore, here we avoid adding such trivial constraints to have stable constraint system
    fun isGeneratedConstraintTrivial(
        baseConstraint: Constraint,
        otherConstraint: Constraint,
        generatedConstraintType: CangJieTypeMarker,
        isSubtype: Boolean
    ): Boolean {
        if (isSubtype && (generatedConstraintType.isNothing() || generatedConstraintType.isFlexibleNothing())) return true
        if (!isSubtype && generatedConstraintType.isNullableAny()) return true

        // If types from constraints that will be used to generate new constraint already contains `Nothing(?)`,
        // then we can't decide that resulting constraint will be useless
        if (baseConstraint.type.contains { it.isNothingOrNullableNothing() }) return false
        if (otherConstraint.type.contains { it.isNothingOrNullableNothing() }) return false

        // It's important to preserve constraints with nullable Nothing: `Nothing? <: T` (see implicitNothingConstraintFromReturn.kt test)
        if (generatedConstraintType.containsOnlyNonNullableNothing()) return true

        return false
    }


    private fun CangJieTypeMarker.containsOnlyNonNullableNothing(): Boolean =
        contains {
            (it.isNothing() || it.isFlexibleNothing()) &&
                    !(it is SimpleTypeMarker && it.typeConstructor().isNothingConstructor() && it.isMarkedNullable())
        }
}
