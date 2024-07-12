package com.huawei.cangjie.types

import com.huawei.cangjie.types.checker.AbstractTypePreparator
import com.huawei.cangjie.types.model.*
import com.huawei.cangjie.utils.SmartSet
import java.util.*

object AbstractTypeChecker {
    @JvmField
    var RUN_SLOW_ASSERTIONS = false
    fun equalTypes(
        context: TypeCheckerProviderContext,
        a: CangJieTypeMarker,
        b: CangJieTypeMarker,
        stubTypesEqualToAnything: Boolean = true
    ): Boolean {
        return equalTypes(context.newTypeCheckerState(false, stubTypesEqualToAnything), a, b)
    }

    fun effectiveVariance(declared: TypeVariance, useSite: TypeVariance): TypeVariance? {
        if (declared == TypeVariance.INV) return useSite
        if (useSite == TypeVariance.INV) return declared

        // both not INVARIANT
        if (declared == useSite) return declared

        // composite In with Out
        return null
    }


    fun equalTypes(state: TypeCheckerState, a: CangJieTypeMarker, b: CangJieTypeMarker): Boolean =
        with(state.typeSystemContext) {
            if (a === b) return true
//
//            if (isCommonDenotableType(a) && isCommonDenotableType(b)) {
//                val refinedA = state.prepareType(state.refineType(a))
//                val refinedB = state.prepareType(state.refineType(b))
//                val simpleA = refinedA.lowerBoundIfFlexible()
//                if (!areEqualTypeConstructors(refinedA.typeConstructor(), refinedB.typeConstructor())) return false
//                if (simpleA.argumentsCount() == 0) {
//                    if (refinedA.hasFlexibleNullability() || refinedB.hasFlexibleNullability()) return true
//
//                    return simpleA.isMarkedNullable() == refinedB.lowerBoundIfFlexible().isMarkedNullable()
//                }
//            }

            return isSubtypeOf(state, a, b) && isSubtypeOf(state, b, a)
        }

    private fun completeIsSubTypeOf(
        state: TypeCheckerState,
        subType: CangJieTypeMarker,
        superType: CangJieTypeMarker,
        isFromNullabilityConstraint: Boolean
    ): Boolean = with(state.typeSystemContext) {
        val preparedSubType = state.prepareType(state.refineType(subType))
        val preparedSuperType = state.prepareType(state.refineType(superType))

//        checkSubtypeForSpecialCases(state, preparedSubType.lowerBoundIfFlexible(), preparedSuperType.upperBoundIfFlexible())?.let {
//            state.addSubtypeConstraint(preparedSubType, preparedSuperType, isFromNullabilityConstraint)
//            return it
//        }

        // we should add constraints with flexible types, otherwise we never get flexible type as answer in constraint system
        state.addSubtypeConstraint(preparedSubType, preparedSuperType, isFromNullabilityConstraint)?.let { return it }

        return isSubtypeOfForSingleClassifierType(
            state,
            preparedSubType.lowerBoundIfFlexible(),
            preparedSuperType.upperBoundIfFlexible()
        )
    }

    private fun isSubtypeOfForSingleClassifierType(
        state: TypeCheckerState,
        subType: SimpleTypeMarker,
        superType: SimpleTypeMarker
    ): Boolean = with(state.typeSystemContext) {
//        if (RUN_SLOW_ASSERTIONS) {
//            assert(subType.isSingleClassifierType() || subType.typeConstructor().isIntersection() || state.isAllowedTypeVariable(subType)) {
//                "Not singleClassifierType and not intersection subType: $subType"
//            }
//            assert(superType.isSingleClassifierType() || state .isAllowedTypeVariable(superType)) {
//                "Not singleClassifierType superType: $superType"
//            }
//        }
//
//        if (!AbstractNullabilityChecker.isPossibleSubtype(state, subType, superType)) return false
//
//        checkSubtypeForIntegerLiteralType(state, subType.lowerBoundIfFlexible(), superType.upperBoundIfFlexible())?.let {
//            state.addSubtypeConstraint(subType, superType)
//            return it
//        }
//
//        val superConstructor = superType.typeConstructor()
//
//        if (areEqualTypeConstructors(subType.typeConstructor(), superConstructor) && superConstructor.parametersCount() == 0) return true
//        if (superType.typeConstructor().isAnyConstructor()) return true
//
//        val supertypesWithSameConstructor = with(findCorrespondingSupertypes(state, subType, superConstructor)) {
//            // Note: in K1, we can have partially computed types here, like SomeType<NON COMPUTED YET>
//            // (see e.g. interClassesRecursion.kt from diagnostic tests)
//            // In this case we don't want to affect lazy computation in normal case (size <= 1), that's why we don't create a set
//            // (adding to a hash set requires hash-code calculation for each set element)
//
//            if (size > 1 && (state.typeSystemContext as? TypeSystemInferenceExtensionContext)?.isK2 == true) {
//                // Here we want to filter out equivalent types to avoid unnecessary forking
//                mapTo(mutableSetOf()) { state.prepareType(it).asSimpleType() ?: it }
//            } else {
//                // TODO: drop this branch together with K1 code
//                map { state.prepareType(it).asSimpleType() ?: it }
//            }
//        }
//        when (supertypesWithSameConstructor.size) {
//            0 -> return hasNothingSupertype(state, subType) // todo Nothing & Array<Number> <: Array<String>
//            1 -> return state.isSubtypeForSameConstructor(supertypesWithSameConstructor.first().asArgumentList(), superType)
//
//            else -> { // at least 2 supertypes with same constructors. Such case is rare
//                val newArguments = ArgumentList(superConstructor.parametersCount())
//                var anyNonOutParameter = false
//                for (index in 0 until superConstructor.parametersCount()) {
//                    anyNonOutParameter = anyNonOutParameter || superConstructor.getParameter(index).getVariance() != TypeVariance.OUT
//                    if (anyNonOutParameter) continue
//                    val allProjections = supertypesWithSameConstructor.map {
//                        it.getArgumentOrNull(index)?.takeIf { it.getVariance() == TypeVariance.INV }?.getType()
//                            ?: error("Incorrect type: $it, subType: $subType, superType: $superType")
//                    }
//
//                    // todo discuss
//                    val intersection = intersectTypes(allProjections).asTypeArgument()
//                    newArguments.add(intersection)
//                }
//
//                if (!anyNonOutParameter && state.isSubtypeForSameConstructor(newArguments, superType)) return true
//
//                return state.runForkingPoint {
//                    for (subTypeArguments in supertypesWithSameConstructor) {
//                        fork { state.isSubtypeForSameConstructor(subTypeArguments.asArgumentList(), superType) }
//                    }
//                }
//            }
//        }
        false
    }

    fun isSubtypeOf(
        context: TypeCheckerProviderContext,
        subType: CangJieTypeMarker,
        superType: CangJieTypeMarker,
        stubTypesEqualToAnything: Boolean = true
    ): Boolean {
        return isSubtypeOf(context.newTypeCheckerState(true, stubTypesEqualToAnything), subType, superType)
    }

    @JvmOverloads
    fun isSubtypeOf(
        state: TypeCheckerState,
        subType: CangJieTypeMarker,
        superType: CangJieTypeMarker,
        isFromNullabilityConstraint: Boolean = false
    ): Boolean {
        if (subType === superType) return true

        if (!state.customIsSubtypeOf(subType, superType)) return false

        return completeIsSubTypeOf(state, subType, superType, isFromNullabilityConstraint)
    }
}

/**
 * Context that defines how type-checker operates, stores type-checker state,
 * created by [TypeCheckerProviderContext.newTypeCheckerState] in most cases
 *
 * Stateful and shouldn't be reused
 *
 * Once some type-checker operation is performed using a [TypeCheckerProviderContext], for example a [AbstractTypeChecker.isSubtypeOf],
 * new instance of particular [TypeCheckerState] should be created, with properly specified type system context
 */
open class TypeCheckerState(
    val isErrorTypeEqualsToAnything: Boolean,
    val isStubTypeEqualsToAnything: Boolean,
    val allowedTypeVariable: Boolean,
    val typeSystemContext: TypeSystemContext,
    val cangjieTypePreparator: AbstractTypePreparator,
    val cangjieTypeRefiner: AbstractTypeRefiner
) {
    sealed class SupertypesPolicy {
        abstract fun transformType(state: TypeCheckerState, type: CangJieTypeMarker): SimpleTypeMarker

        object None : SupertypesPolicy() {
            override fun transformType(state: TypeCheckerState, type: CangJieTypeMarker) =
                throw UnsupportedOperationException("Should not be called")
        }

        object UpperIfFlexible : SupertypesPolicy() {
            override fun transformType(state: TypeCheckerState, type: CangJieTypeMarker) =
                with(state.typeSystemContext) { type.upperBoundIfFlexible() }
        }

        object LowerIfFlexible : SupertypesPolicy() {
            override fun transformType(state: TypeCheckerState, type: CangJieTypeMarker) =
                with(state.typeSystemContext) { type.lowerBoundIfFlexible() }
        }

        abstract class DoCustomTransform : SupertypesPolicy()
    }

    private var supertypesLocked = false
    var supertypesDeque: ArrayDeque<SimpleTypeMarker>? = null
        private set
    var supertypesSet: MutableSet<SimpleTypeMarker>? = null
        private set

    open fun addSubtypeConstraint(
        subType: CangJieTypeMarker,
        superType: CangJieTypeMarker,
        isFromNullabilityConstraint: Boolean = false
    ): Boolean? = null

    fun initialize() {
        assert(!supertypesLocked) {
            "Supertypes were locked for ${this::class}"
        }
        supertypesLocked = true

        if (supertypesDeque == null) {
            supertypesDeque = ArrayDeque(4)
        }
        if (supertypesSet == null) {
            supertypesSet = SmartSet.create()
        }
    }

    inline fun anySupertype(
        start: SimpleTypeMarker,
        predicate: (SimpleTypeMarker) -> Boolean,
        supertypesPolicy: (SimpleTypeMarker) -> SupertypesPolicy
    ): Boolean {
        if (predicate(start)) return true

        initialize()

        val deque = supertypesDeque!!
        val visitedSupertypes = supertypesSet!!

        deque.push(start)
        while (deque.isNotEmpty()) {
            if (visitedSupertypes.size > 1000) {
                error("Too many supertypes for type: $start. Supertypes = ${visitedSupertypes.joinToString()}")
            }
            val current = deque.pop()
            if (!visitedSupertypes.add(current)) continue

            val policy = supertypesPolicy(current).takeIf { it != SupertypesPolicy.None } ?: continue
            val supertypes = with(typeSystemContext) { current.typeConstructor().supertypes() }
            for (supertype in supertypes) {
                val newType = policy.transformType(this, supertype)
                if (predicate(newType)) {
                    clear()
                    return true
                }
                deque.add(newType)
            }
        }

        clear()
        return false
    }

    fun clear() {
        supertypesDeque!!.clear()
        supertypesSet!!.clear()
        supertypesLocked = false
    }

    @OptIn(TypeRefinement::class)
    fun refineType(type: CangJieTypeMarker): CangJieTypeMarker {
        return cangjieTypeRefiner.refineType(type)
    }

    open fun customIsSubtypeOf(subType: CangJieTypeMarker, superType: CangJieTypeMarker): Boolean = true
    fun prepareType(type: CangJieTypeMarker): CangJieTypeMarker {
        return cangjieTypePreparator.prepareType(type)
    }
}

object AbstractNullabilityChecker {
    fun TypeCheckerState.hasNotNullSupertype(
        type: SimpleTypeMarker, supertypesPolicy:
        TypeCheckerState.SupertypesPolicy
    ) =
        with(typeSystemContext) {
            anySupertype(type, {
                (it.isClassType() && !it.isMarkedNullable()) || it.isDefinitelyNotNullType()
            }) {
                if (it.isMarkedNullable()) TypeCheckerState.SupertypesPolicy.None else supertypesPolicy
            }
        }

}
