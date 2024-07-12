package com.huawei.cangjie.resolve.calls.inference.model

import com.huawei.cangjie.resolve.calls.inference.ForkPointData
import com.huawei.cangjie.resolve.calls.tower.isSuccess
import com.huawei.cangjie.types.model.*
import com.huawei.cangjie.utils.SmartList
import com.huawei.cangjie.utils.trimToSize

private typealias Context = TypeSystemInferenceExtensionContext

class MutableVariableWithConstraints private constructor(
    private val context: Context,
    override val typeVariable: TypeVariableMarker,
    constraints: List<Constraint>? // assume simplified and deduplicated
) : VariableWithConstraints {
    constructor(context: Context, typeVariable: TypeVariableMarker) : this(context, typeVariable, null)

    constructor(context: Context, other: VariableWithConstraints) : this(context, other.typeVariable, other.constraints)

    private val mutableConstraints = if (constraints == null) SmartList() else SmartList(constraints)

    /**
     * The contract for mutating this list is that the only allowed mutation is appending items.
     * In any other case, it must be set to `null`, so that it will be recomputed when [constraints] is called.
     *
     * The reason is that the list might be mutated while it's being iterated.
     * For this reason, we use an index loop in
     * [org.jetbrains.kotlin.resolve.calls.inference.components.ConstraintIncorporator.forEachConstraint].
     */
    private var simplifiedConstraints: SmartList<Constraint>? = mutableConstraints
    private fun SmartList<Constraint>.simplifyConstraints(): SmartList<Constraint> =
        simplifyLowerConstraints().simplifyEqualityConstraints()

    private fun SmartList<Constraint>.simplifyEqualityConstraints(): SmartList<Constraint> {
        val equalityConstraints = filter { it.kind == ConstraintKind.EQUALITY }.groupBy { it.typeHashCode }
        return when {
            equalityConstraints.isEmpty() -> this
            else -> filterTo(SmartList()) { isUsefulConstraint(it, equalityConstraints) }
        }
    }

    private fun isUsefulConstraint(constraint: Constraint, equalityConstraints: Map<Int, List<Constraint>>): Boolean {
        if (constraint.kind == ConstraintKind.EQUALITY) return true
        return equalityConstraints[constraint.typeHashCode]?.none { it.type == constraint.type } ?: true
    }

    // Such constraint is applicable for simplification
    private fun Constraint.isLowerAndFlexibleTypeWithDefNotNullLowerBound(): Boolean {
        return with(context) {
            kind == ConstraintKind.LOWER && type.isFlexible() && type.lowerBoundIfFlexible().isDefinitelyNotNullType()
        }
    }
    // This method should be used only when constraint system has state COMPLETION
    internal fun removeConstrains(shouldRemove: (Constraint) -> Boolean) {
        mutableConstraints.removeAll(shouldRemove)
        if (simplifiedConstraints !== mutableConstraints) {
            simplifiedConstraints = null
        }
    }
    val rawConstraintsCount get() = mutableConstraints.size

    // This method should be used only for transaction in constraint system
    // shouldRemove should give true only for tail elements
    internal fun removeLastConstraints(sinceIndex: Int) {
        mutableConstraints.trimToSize(sinceIndex)
        if (simplifiedConstraints !== mutableConstraints) {
            simplifiedConstraints = null
        }
    }

    private fun SmartList<Constraint>.simplifyLowerConstraints(): SmartList<Constraint> {
        val usefulConstraints = SmartList<Constraint>()
        for (constraint in this) {
            if (!constraint.isLowerAndFlexibleTypeWithDefNotNullLowerBound()) {
                usefulConstraints.add(constraint)
                continue
            }

            // Now we have to check that some constraint T!!.T? <: K is useless or not
            // If there is constraint T..T? <: K, then the original one (T!!.T?) is useless
            // This is so because CST(T..T?, T!!..T?) == CST(T..T?)

            val thereIsStrongerConstraint =
                this.any { it.isStrongerThanLowerAndFlexibleTypeWithDefNotNullLowerBound(constraint) }

            if (!thereIsStrongerConstraint) {
                usefulConstraints.add(constraint)
            }
        }

        return usefulConstraints
    }

    private fun Constraint.isStrongerThanLowerAndFlexibleTypeWithDefNotNullLowerBound(other: Constraint): Boolean {
        if (this === other) return false

        if (typeHashCode != other.typeHashCode || kind == ConstraintKind.UPPER) return false
        with(context) {
            if (!type.isFlexible() || !other.type.isFlexible()) return false
            val otherLowerBound = other.type.lowerBoundIfFlexible()
            if (!otherLowerBound.isDefinitelyNotNullType()) return false
            require(otherLowerBound is DefinitelyNotNullTypeMarker)
            val thisLowerBound = type.lowerBoundIfFlexible()
            val thisUpperBound = type.upperBoundIfFlexible()
            val otherUpperBound = other.type.upperBoundIfFlexible()
            return thisLowerBound == otherLowerBound.original() && thisUpperBound == otherUpperBound
        }
    }

    override val constraints: List<Constraint>
        get() {
            if (simplifiedConstraints == null) {
                simplifiedConstraints = mutableConstraints.simplifyConstraints()
            }
            return simplifiedConstraints!!
        }

}

internal class MutableConstraintStorage : ConstraintStorage {
    override val allTypeVariables: MutableMap<TypeConstructorMarker, TypeVariableMarker> = LinkedHashMap()
    override val notFixedTypeVariables: MutableMap<TypeConstructorMarker, MutableVariableWithConstraints> =
        LinkedHashMap()
    override val missedConstraints: MutableList<Pair<IncorporationConstraintPosition, MutableList<Pair<TypeVariableMarker, Constraint>>>> =
        SmartList()
    override val initialConstraints: MutableList<InitialConstraint> = SmartList()
    override var maxTypeDepthFromInitialConstraints: Int = 1
    override val errors: MutableList<ConstraintSystemError> = SmartList()
    override val hasContradiction: Boolean get() = errors.any { !it.applicability.isSuccess }
    override val fixedTypeVariables: MutableMap<TypeConstructorMarker, CangJieTypeMarker> = LinkedHashMap()
    override val postponedTypeVariables: MutableList<TypeVariableMarker> = SmartList()
    override val builtFunctionalTypesForPostponedArgumentsByTopLevelTypeVariables: MutableMap<Pair<TypeConstructorMarker, List<Pair<TypeConstructorMarker, Int>>>, CangJieTypeMarker> =
        LinkedHashMap()
    override val builtFunctionalTypesForPostponedArgumentsByExpectedTypeVariables: MutableMap<TypeConstructorMarker, CangJieTypeMarker> =
        LinkedHashMap()

    override val constraintsFromAllForkPoints: MutableList<Pair<IncorporationConstraintPosition, ForkPointData>> =
        SmartList()

    override var outerSystemVariablesPrefixSize: Int = 0

    override var usesOuterCs: Boolean = false

    //    @AssertionsOnly
    internal var outerCS: ConstraintStorage? = null
}
/**
 * Annotated member is used only for assertion purposes and does not affect semantics
 */
@RequiresOptIn
annotation class AssertionsOnly
