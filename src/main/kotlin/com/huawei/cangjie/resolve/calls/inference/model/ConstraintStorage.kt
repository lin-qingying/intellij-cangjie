package com.huawei.cangjie.resolve.calls.inference.model

import com.huawei.cangjie.resolve.calls.inference.ForkPointData
import com.huawei.cangjie.types.AbstractTypeChecker
import com.huawei.cangjie.types.model.CangJieTypeMarker
import com.huawei.cangjie.types.model.TypeCheckerProviderContext
import com.huawei.cangjie.types.model.TypeConstructorMarker
import com.huawei.cangjie.types.model.TypeVariableMarker


/**
 * Every type variable can be in the following states:
 *  - not fixed => there is several constraints for this type variable(possible no one).
 *      for this type variable we have VariableWithConstraints in map notFixedTypeVariables
 *  - fixed to proper type or not proper type. For such type variable there is no VariableWithConstraints in notFixedTypeVariables.
 *      Also we should guaranty that there is no other constraints in other VariableWithConstraints which depends on this fixed type variable.
 *
 *  Note: fixedTypeVariables can contains a proper and not proper type.
 *
 *  Fixing procedure(to proper types). First of all we should determinate fixing order.
 *  After it, for every type variable we do the following:
 *  - determinate result proper type
 *  - add equality constraint, for example: T = Int
 *  - run incorporation and generate all new constraints
 *  - after is we remove VariableWithConstraints for type variable T from map notFixedTypeVariables
 *  - also we remove all constraint in other variable which contains T
 *  - add result type to fixedTypeVariables.
 *
 *  Note fixing procedure to not proper type the same. The only difference in determination result type.
 *
 */

interface ConstraintStorage {
    val allTypeVariables: Map<TypeConstructorMarker, TypeVariableMarker>
    val notFixedTypeVariables: Map<TypeConstructorMarker, VariableWithConstraints>
    val missedConstraints: List<Pair<IncorporationConstraintPosition, List<Pair<TypeVariableMarker, Constraint>>>>
    val initialConstraints: List<InitialConstraint>
    val maxTypeDepthFromInitialConstraints: Int
    val errors: List<ConstraintSystemError>
    val hasContradiction: Boolean
    val fixedTypeVariables: Map<TypeConstructorMarker, CangJieTypeMarker>
    val postponedTypeVariables: List<TypeVariableMarker>
    val builtFunctionalTypesForPostponedArgumentsByTopLevelTypeVariables: Map<Pair<TypeConstructorMarker, List<Pair<TypeConstructorMarker, Int>>>, CangJieTypeMarker>
    val builtFunctionalTypesForPostponedArgumentsByExpectedTypeVariables: Map<TypeConstructorMarker, CangJieTypeMarker>
    val constraintsFromAllForkPoints: List<Pair<IncorporationConstraintPosition, ForkPointData>>

    /**
     *  Outer system for a call means some set of variables defined beside it/its arguments
     *
     *  In case some candidate's CS is built in the context of some outer CS, first [outerSystemVariablesPrefixSize] in the list
     *  of [allTypeVariables] belong to the outer CS.
     *
     *  That information is very limitedly used in a couple of cases when we need to separate those kinds of variables
     *   - When completing `provideDelegate` calls, we assume outer variables as proper types
     *   (see fixInnerVariablesForProvideDelegateIfNeeded).
     *   - When checking consistency of collected variables for the inner candidate
     *   (see checkNotFixedTypeVariablesCountConsistency).
     *
     *  Also, see docs/fir/delegated_property_inference.md
     */
    val outerSystemVariablesPrefixSize: Int

    val usesOuterCs: Boolean

    object Empty : ConstraintStorage {
        override val allTypeVariables: Map<TypeConstructorMarker, TypeVariableMarker> get() = emptyMap()
        override val notFixedTypeVariables: Map<TypeConstructorMarker, VariableWithConstraints> get() = emptyMap()
        override val missedConstraints: List<Pair<IncorporationConstraintPosition, List<Pair<TypeVariableMarker, Constraint>>>> get() = emptyList()
        override val initialConstraints: List<InitialConstraint> get() = emptyList()
        override val maxTypeDepthFromInitialConstraints: Int get() = 1
        override val errors: List<ConstraintSystemError> get() = emptyList()
        override val hasContradiction: Boolean get() = false
        override val fixedTypeVariables: Map<TypeConstructorMarker, CangJieTypeMarker> get() = emptyMap()
        override val postponedTypeVariables: List<TypeVariableMarker> get() = emptyList()
        override val builtFunctionalTypesForPostponedArgumentsByTopLevelTypeVariables: Map<Pair<TypeConstructorMarker, List<Pair<TypeConstructorMarker, Int>>>, CangJieTypeMarker> = emptyMap()
        override val builtFunctionalTypesForPostponedArgumentsByExpectedTypeVariables: Map<TypeConstructorMarker, CangJieTypeMarker> = emptyMap()
        override val constraintsFromAllForkPoints: List<Pair<IncorporationConstraintPosition, ForkPointData>> = emptyList()

        override val outerSystemVariablesPrefixSize: Int get() = 0

        override val usesOuterCs: Boolean get() = false
    }
}

enum class ConstraintKind {
    LOWER,
    UPPER,
    EQUALITY;

    fun isLower(): Boolean = this == LOWER
    fun isUpper(): Boolean = this == UPPER
    fun isEqual(): Boolean = this == EQUALITY

    fun opposite() = when (this) {
        LOWER -> UPPER
        UPPER -> LOWER
        EQUALITY -> EQUALITY
    }
}

class Constraint(
    val kind: ConstraintKind,
    val type: CangJieTypeMarker, // flexible types here is allowed
    val position: IncorporationConstraintPosition,
    val typeHashCode: Int = type.hashCode(),
    val derivedFrom: Set<TypeVariableMarker>,
    // This value is true for constraints of the form `Nothing? <: Tv`
    // that have been created during incorporation phase of the constraint of the form `Kv? <: Tv` (where `Kv` another type variable).
    // The main idea behind that parameter is that we don't consider such constraints as proper (signifying that variable is ready for completion).
    // And also, there is additional logic in K1 that doesn't allow to fix variable into `Nothing?` if we had only that kind of lower constraints
    val isNullabilityConstraint: Boolean,
    val inputTypePositionBeforeIncorporation: OnlyInputTypeConstraintPosition? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false

        other as Constraint

        if (typeHashCode != other.typeHashCode) return false
        if (kind != other.kind) return false
        if (position != other.position) return false
        if (type != other.type) return false

        return true
    }

    override fun hashCode() = typeHashCode

    override fun toString() = "$kind($type) from $position"
}

interface VariableWithConstraints {
    val typeVariable: TypeVariableMarker
    val constraints: List<Constraint>
}

class InitialConstraint(
    val a: CangJieTypeMarker,
    val b: CangJieTypeMarker,
    val constraintKind: ConstraintKind, // see [checkConstraint]
    val position: ConstraintPosition
) {
    override fun toString(): String = "${asStringWithoutPosition()} from $position"

    fun asStringWithoutPosition(): String {
        val sign =
            when (constraintKind) {
                ConstraintKind.EQUALITY -> "=="
                ConstraintKind.LOWER -> ":>"
                ConstraintKind.UPPER -> "<:"
            }
        return "$a $sign $b"
    }
}

//fun InitialConstraint.checkConstraint(substitutor: TypeSubstitutor): Boolean {
//    val newA = substitutor.substitute(a)
//    val newB = substitutor.substitute(b)
//    return checkConstraint(newB as CangJieTypeMarker, constraintKind, newA as CangJieTypeMarker)
//}

fun checkConstraint(
    context: TypeCheckerProviderContext,
    constraintType: CangJieTypeMarker,
    constraintKind: ConstraintKind,
    resultType: CangJieTypeMarker
): Boolean {


    val typeChecker = AbstractTypeChecker
    return when (constraintKind) {
        ConstraintKind.EQUALITY -> typeChecker.equalTypes(context, constraintType, resultType)
        ConstraintKind.LOWER -> typeChecker.isSubtypeOf(context, constraintType, resultType)
        ConstraintKind.UPPER -> typeChecker.isSubtypeOf(context, resultType, constraintType)
    }
}

fun Constraint.replaceType(newType: CangJieTypeMarker) =
    Constraint(kind, newType, position, typeHashCode, derivedFrom, isNullabilityConstraint, inputTypePositionBeforeIncorporation)
