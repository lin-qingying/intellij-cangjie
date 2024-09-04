package com.huawei.cangjie.resolve.calls.inference.components

import com.huawei.cangjie.resolve.calls.inference.ForkPointData
import com.huawei.cangjie.resolve.calls.inference.model.IncorporationConstraintPosition
import com.huawei.cangjie.resolve.calls.inference.model.VariableWithConstraints
import com.huawei.cangjie.resolve.calls.model.PostponedResolvedAtomMarker
import com.huawei.cangjie.types.model.*
/**
 * Returns `false` for fixed type variables types even if `isProper(type) == true`
 * Thus allowing only non-TVs types to be used for fixation on top level.
 * While this limitation is important, it doesn't really limit final results because when we have a constraint like T <: E or E <: T
 * and we're going to fix T into E, we assume that if E has some other constraints, they are being incorporated to T, so we would choose
 * them instead of E itself.
 */
inline fun TypeSystemInferenceExtensionContext.isProperTypeForFixation(
    type: CangJieTypeMarker,
    notFixedTypeVariables: Set<TypeConstructorMarker>,
    isProper: (CangJieTypeMarker) -> Boolean
): Boolean {
    // We don't allow fixing T into any top-level TV type, like T := F or T := F & Any
    // Even if F is considered as a proper by `isProper` (e.g., it belongs to an outer CS)
    // But at the same time, we don't forbid fixing into T := MutableList<F>
    if (type.typeConstructor() in notFixedTypeVariables) return false

    return isProper(type) && extractProjectionsForAllCapturedTypes(type).all(isProper)
}
class VariableFixationFinder(
    private val trivialConstraintTypeInferenceOracle: TrivialConstraintTypeInferenceOracle,
//    private val languageVersionSettings: LanguageVersionSettings,
) {
    interface Context : TypeSystemInferenceExtensionContext {
        val notFixedTypeVariables: Map<TypeConstructorMarker, VariableWithConstraints>
        val fixedTypeVariables: Map<TypeConstructorMarker, CangJieTypeMarker>
        val postponedTypeVariables: List<TypeVariableMarker>
        val constraintsFromAllForkPoints: MutableList<Pair<IncorporationConstraintPosition, ForkPointData>>
        val allTypeVariables: Map<TypeConstructorMarker, TypeVariableMarker>

        /**
         * See [org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintStorage.outerSystemVariablesPrefixSize]
         */
        val outerSystemVariablesPrefixSize: Int

        val outerTypeVariables: Set<TypeConstructorMarker>?
            get() =
                when {
                    outerSystemVariablesPrefixSize > 0 -> allTypeVariables.keys.take(outerSystemVariablesPrefixSize)
                        .toSet()

                    else -> null
                }

        /**
         * If not null, that property means that we should assume temporary them all as proper types when fixating some variables.
         *
         * By default, if that property is null, we assume all `allTypeVariables` as not proper.
         *
         * Currently, that is only used for `provideDelegate` resolution, see
         * [org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirDeclarationsResolveTransformer.fixInnerVariablesForProvideDelegateIfNeeded]
         */
        val typeVariablesThatAreCountedAsProperTypes: Set<TypeConstructorMarker>?

//        fun isReified(variable: TypeVariableMarker): Boolean
    }


    class VariableForFixation(
        val variable: TypeConstructorMarker,
        private val hasProperConstraint: Boolean,
        private val hasDependencyOnOuterTypeVariable: Boolean = false,
    ) {
        val isReady: Boolean get() = hasProperConstraint && !hasDependencyOnOuterTypeVariable
    }
    fun findFirstVariableForFixation(
        c: Context,
        allTypeVariables: List<TypeConstructorMarker>,
        postponedKtPrimitives: List<PostponedResolvedAtomMarker>,
        completionMode: ConstraintSystemCompletionMode,
        topLevelType: CangJieTypeMarker,
    ): VariableForFixation? =
        c.findTypeVariableForFixation(allTypeVariables, postponedKtPrimitives, completionMode, topLevelType)

    enum class TypeVariableFixationReadiness {
        FORBIDDEN,
        WITHOUT_PROPER_ARGUMENT_CONSTRAINT, // proper constraint from arguments -- not from upper bound for type parameters
        OUTER_TYPE_VARIABLE_DEPENDENCY,
        READY_FOR_FIXATION_DECLARED_UPPER_BOUND_WITH_SELF_TYPES,
        WITH_COMPLEX_DEPENDENCY, // if type variable T has constraint with non fixed type variable inside (non-top-level): T <: Foo<S>
        ALL_CONSTRAINTS_TRIVIAL_OR_NON_PROPER, // proper trivial constraint from arguments, Nothing <: T
        RELATED_TO_ANY_OUTPUT_TYPE,
        FROM_INCORPORATION_OF_DECLARED_UPPER_BOUND,
        READY_FOR_FIXATION_UPPER,
        READY_FOR_FIXATION_LOWER,
        READY_FOR_FIXATION,
        READY_FOR_FIXATION_REIFIED,
    }

    private fun Context.getTypeVariableReadiness(
        variable: TypeConstructorMarker,
        dependencyProvider: TypeVariableDependencyInformationProvider,
    ): TypeVariableFixationReadiness = when {
//        !notFixedTypeVariables.contains(variable) || dependencyProvider.isVariableRelatedToTopLevelType(variable) ||
//                variableHasUnprocessedConstraintsInForks(variable) ->
//            TypeVariableFixationReadiness.FORBIDDEN
//        isTypeInferenceForSelfTypesSupported && areAllProperConstraintsSelfTypeBased(variable) ->
//            TypeVariableFixationReadiness.READY_FOR_FIXATION_DECLARED_UPPER_BOUND_WITH_SELF_TYPES
//        !variableHasProperArgumentConstraints(variable) -> TypeVariableFixationReadiness.WITHOUT_PROPER_ARGUMENT_CONSTRAINT
//        dependencyProvider.isRelatedToOuterTypeVariable(variable) -> TypeVariableFixationReadiness.OUTER_TYPE_VARIABLE_DEPENDENCY
//        hasDependencyToOtherTypeVariables(variable) -> TypeVariableFixationReadiness.WITH_COMPLEX_DEPENDENCY
//        // TODO: Consider removing this kind of readiness, see KT-63032
//        allConstraintsTrivialOrNonProper(variable) -> TypeVariableFixationReadiness.ALL_CONSTRAINTS_TRIVIAL_OR_NON_PROPER
//        dependencyProvider.isVariableRelatedToAnyOutputType(variable) -> TypeVariableFixationReadiness.RELATED_TO_ANY_OUTPUT_TYPE
//        variableHasOnlyIncorporatedConstraintsFromDeclaredUpperBound(variable) ->
//            TypeVariableFixationReadiness.FROM_INCORPORATION_OF_DECLARED_UPPER_BOUND
//        isReified(variable) -> TypeVariableFixationReadiness.READY_FOR_FIXATION_REIFIED
//        inferenceCompatibilityModeEnabled -> {
//            when {
//                variableHasLowerNonNothingProperConstraint(variable) -> TypeVariableFixationReadiness.READY_FOR_FIXATION_LOWER
//                else -> TypeVariableFixationReadiness.READY_FOR_FIXATION_UPPER
//            }
//        }
        else -> TypeVariableFixationReadiness.READY_FOR_FIXATION
    }

    private fun Context.findTypeVariableForFixation(
        allTypeVariables: List<TypeConstructorMarker>,
        postponedArguments: List<PostponedResolvedAtomMarker>,
        completionMode: ConstraintSystemCompletionMode,
        topLevelType: CangJieTypeMarker,
    ): VariableForFixation? {
        if (allTypeVariables.isEmpty()) return null

        val dependencyProvider = TypeVariableDependencyInformationProvider(
            notFixedTypeVariables, postponedArguments, topLevelType.takeIf { completionMode == ConstraintSystemCompletionMode.PARTIAL }, this,
        )

        val candidate =
            allTypeVariables.maxByOrNull { getTypeVariableReadiness(it, dependencyProvider) } ?: return null

        return when (getTypeVariableReadiness(candidate, dependencyProvider)) {
            TypeVariableFixationReadiness.FORBIDDEN -> null
            TypeVariableFixationReadiness.WITHOUT_PROPER_ARGUMENT_CONSTRAINT -> VariableForFixation(candidate, false)
            TypeVariableFixationReadiness.OUTER_TYPE_VARIABLE_DEPENDENCY ->
                VariableForFixation(candidate, hasProperConstraint = true, hasDependencyOnOuterTypeVariable = true)

            else -> VariableForFixation(candidate, true)
        }
    }

}
fun TypeSystemInferenceExtensionContext.extractProjectionsForAllCapturedTypes(baseType: CangJieTypeMarker): Set<CangJieTypeMarker> {
//    if (baseType.isFlexible()) {
//        val flexibleType = baseType.asFlexibleType()!!
//        return buildSet {
//            addAll(extractProjectionsForAllCapturedTypes(flexibleType.lowerBound()))
//            addAll(extractProjectionsForAllCapturedTypes(flexibleType.upperBound()))
//        }
//    }
//    val simpleBaseType = baseType.asSimpleType()?.originalIfDefinitelyNotNullable()
//
//    return buildSet {
//        val projectionType = if (simpleBaseType is CapturedTypeMarker) {
//            val typeArgument = simpleBaseType.typeConstructorProjection().takeIf { !it.isStarProjection() } ?: return@buildSet
//            typeArgument.getType().also(::add)
//        } else baseType
//        val argumentsCount = projectionType.argumentsCount().takeIf { it != 0 } ?: return@buildSet
//
//        for (i in 0 until argumentsCount) {
//            val typeArgument = projectionType.getArgument(i).takeIf { !it.isStarProjection() } ?: continue
//            addAll(extractProjectionsForAllCapturedTypes(typeArgument.getType()))
//        }
//    }
    return emptySet()
}
fun TypeSystemInferenceExtensionContext.containsTypeVariable(type: CangJieTypeMarker, typeVariable: TypeConstructorMarker): Boolean {
    if (type.contains { it.typeConstructor().unwrapStubTypeVariableConstructor() == typeVariable }) return true

    val typeProjections = extractProjectionsForAllCapturedTypes(type)

    return typeProjections.any { typeProjectionsType ->
        typeProjectionsType.contains { it.typeConstructor().unwrapStubTypeVariableConstructor() == typeVariable }
    }
}
