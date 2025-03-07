/*
 * Copyright 2024 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package cn.cangnova.cangjie.resolve.calls.inference.components

import cn.cangnova.cangjie.config.LanguageFeature
import cn.cangnova.cangjie.config.LanguageVersionSettings
import cn.cangnova.cangjie.resolve.calls.inference.ForkPointData
import cn.cangnova.cangjie.resolve.calls.inference.hasRecursiveTypeParametersWithGivenSelfType
import cn.cangnova.cangjie.resolve.calls.inference.isRecursiveTypeParameter
import cn.cangnova.cangjie.resolve.calls.inference.model.Constraint
import cn.cangnova.cangjie.resolve.calls.inference.model.DeclaredUpperBoundConstraintPosition
import cn.cangnova.cangjie.resolve.calls.inference.model.IncorporationConstraintPosition
import cn.cangnova.cangjie.resolve.calls.inference.model.VariableWithConstraints
import cn.cangnova.cangjie.resolve.calls.model.PostponedResolvedAtomMarker
import cn.cangnova.cangjie.types.model.*
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
    private val languageVersionSettings: LanguageVersionSettings,
) {
    interface Context : TypeSystemInferenceExtensionContext {
        val notFixedTypeVariables: Map<TypeConstructorMarker, VariableWithConstraints>
        val fixedTypeVariables: Map<TypeConstructorMarker, CangJieTypeMarker>
        val postponedTypeVariables: List<TypeVariableMarker>
        val constraintsFromAllForkPoints: MutableList<Pair<IncorporationConstraintPosition, ForkPointData>>
        val allTypeVariables: Map<TypeConstructorMarker, TypeVariableMarker>

        /**
         * See [cn.cangnova.cangjie.resolve.calls.inference.model.ConstraintStorage.outerSystemVariablesPrefixSize]
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
         * [cn.cangnova.cangjie.fir.resolve.transformers.body.resolve.FirDeclarationsResolveTransformer.fixInnerVariablesForProvideDelegateIfNeeded]
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
        postponedCjPrimitives: List<PostponedResolvedAtomMarker>,
        completionMode: ConstraintSystemCompletionMode,
        topLevelType: CangJieTypeMarker,
    ): VariableForFixation? =
        c.findTypeVariableForFixation(allTypeVariables, postponedCjPrimitives, completionMode, topLevelType)

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
    private fun Context.variableHasProperArgumentConstraints(variable: TypeConstructorMarker): Boolean {
        val constraints = notFixedTypeVariables[variable]?.constraints ?: return false
        // temporary hack to fail calls which contain callable references resolved though OI with uninferred type parameters
        val areThereConstraintsWithUninferredTypeParameter = constraints.any { c -> c.type.contains { it.isUninferredParameter() } }
        return constraints.any { isProperArgumentConstraint(it) } && !areThereConstraintsWithUninferredTypeParameter
    }
    private fun Context.isProperType(type: CangJieTypeMarker): Boolean =
        isProperTypeForFixation(type, notFixedTypeVariables.keys) { t -> !t.contains { isNotFixedRelevantVariable(it) } }
    private fun Context.isNotFixedRelevantVariable(it: CangJieTypeMarker): Boolean {
        val key = it.typeConstructor()
        if (!notFixedTypeVariables.containsKey(key)) return false
        if (typeVariablesThatAreCountedAsProperTypes?.contains(key) == true) return false
        return true
    }
    private fun Context.isProperArgumentConstraint(c: Constraint) =
        isProperType(c.type)
                && c.position.initialConstraint.position !is DeclaredUpperBoundConstraintPosition<*>
                && !c.isNullabilityConstraint
    private val inferenceCompatibilityModeEnabled: Boolean
        get() = languageVersionSettings.supportsFeature(LanguageFeature.InferenceCompatibility)
    private val isTypeInferenceForSelfTypesSupported: Boolean
        get() = languageVersionSettings.supportsFeature(LanguageFeature.TypeInferenceOnCallsWithSelfTypes)
    private fun Context.isSelfTypeConstraint(constraint: Constraint): Boolean {
        val typeConstructor = constraint.type.typeConstructor()
        return constraint.position.from is DeclaredUpperBoundConstraintPosition<*>
                && (hasRecursiveTypeParametersWithGivenSelfType(typeConstructor) || isRecursiveTypeParameter(typeConstructor))
    }
    private fun Context.areAllProperConstraintsSelfTypeBased(variable: TypeConstructorMarker): Boolean {
        val constraints = notFixedTypeVariables[variable]?.constraints?.takeIf { it.isNotEmpty() } ?: return false

        var hasSelfTypeConstraint = false
        var hasOtherProperConstraint = false

        for (constraint in constraints) {
            if (isSelfTypeConstraint(constraint)) {
                hasSelfTypeConstraint = true
            }
            if (isProperArgumentConstraint(constraint)) {
                hasOtherProperConstraint = true
            }
            if (hasSelfTypeConstraint && hasOtherProperConstraint) break
        }

        return hasSelfTypeConstraint && !hasOtherProperConstraint
    }
    private fun Context.getTypeVariableReadiness(
        variable: TypeConstructorMarker,
        dependencyProvider: TypeVariableDependencyInformationProvider,
    ): TypeVariableFixationReadiness = when {
        !notFixedTypeVariables.contains(variable) || dependencyProvider.isVariableRelatedToTopLevelType(variable) ||
                variableHasUnprocessedConstraintsInForks(variable) ->
            TypeVariableFixationReadiness.FORBIDDEN
        isTypeInferenceForSelfTypesSupported && areAllProperConstraintsSelfTypeBased(variable) ->
            TypeVariableFixationReadiness.READY_FOR_FIXATION_DECLARED_UPPER_BOUND_WITH_SELF_TYPES
        !variableHasProperArgumentConstraints(variable) -> TypeVariableFixationReadiness.WITHOUT_PROPER_ARGUMENT_CONSTRAINT
        dependencyProvider.isRelatedToOuterTypeVariable(variable) -> TypeVariableFixationReadiness.OUTER_TYPE_VARIABLE_DEPENDENCY
        hasDependencyToOtherTypeVariables(variable) -> TypeVariableFixationReadiness.WITH_COMPLEX_DEPENDENCY
//        // TODO: Consider removing this kind of readiness
        allConstraintsTrivialOrNonProper(variable) -> TypeVariableFixationReadiness.ALL_CONSTRAINTS_TRIVIAL_OR_NON_PROPER
        dependencyProvider.isVariableRelatedToAnyOutputType(variable) -> TypeVariableFixationReadiness.RELATED_TO_ANY_OUTPUT_TYPE
        variableHasOnlyIncorporatedConstraintsFromDeclaredUpperBound(variable) ->
            TypeVariableFixationReadiness.FROM_INCORPORATION_OF_DECLARED_UPPER_BOUND
//        isReified(variable) -> TypeVariableFixationReadiness.READY_FOR_FIXATION_REIFIED
        inferenceCompatibilityModeEnabled -> {
            when {
                variableHasLowerNonNothingProperConstraint(variable) -> TypeVariableFixationReadiness.READY_FOR_FIXATION_LOWER
                else -> TypeVariableFixationReadiness.READY_FOR_FIXATION_UPPER
            }
        }
        else -> TypeVariableFixationReadiness.READY_FOR_FIXATION
    }
    private fun Context.hasDependencyToOtherTypeVariables(typeVariable: TypeConstructorMarker): Boolean {
        for (constraint in notFixedTypeVariables[typeVariable]?.constraints ?: return false) {
            val dependencyPresenceCondition = { type: CangJieTypeMarker ->
                type.typeConstructor() != typeVariable && notFixedTypeVariables.containsKey(type.typeConstructor())
            }
            if (constraint.type.lowerBoundIfFlexible().argumentsCount() != 0 && constraint.type.contains(dependencyPresenceCondition))
                return true
        }
        return false
    }
    private fun Context.allConstraintsTrivialOrNonProper(variable: TypeConstructorMarker): Boolean {
        return notFixedTypeVariables[variable]?.constraints?.all { constraint ->
            trivialConstraintTypeInferenceOracle.isNotInterestingConstraint(constraint) || !isProperArgumentConstraint(constraint)
        } ?: false
    }
    private fun Context.variableHasOnlyIncorporatedConstraintsFromDeclaredUpperBound(variable: TypeConstructorMarker): Boolean {
        val constraints = notFixedTypeVariables[variable]?.constraints ?: return false

        return constraints.filter { isProperArgumentConstraint(it) }.all { it.position.isFromDeclaredUpperBound }
    }
    private fun Context.variableHasLowerNonNothingProperConstraint(variable: TypeConstructorMarker): Boolean {
        val constraints = notFixedTypeVariables[variable]?.constraints ?: return false

        return constraints.any {
            it.kind.isLower() && isProperArgumentConstraint(it) && !it.type.typeConstructor().isNothingConstructor()
        }
    }

    private fun Context.variableHasUnprocessedConstraintsInForks(variableConstructor: TypeConstructorMarker): Boolean {
        if (constraintsFromAllForkPoints.isEmpty()) return false

        for ((_, forkPointData) in constraintsFromAllForkPoints) {
            for (constraints in forkPointData) {
                for ((typeVariableFromConstraint, constraint) in constraints) {
                    if (typeVariableFromConstraint.freshTypeConstructor() == variableConstructor) return true
                    if (containsTypeVariable(constraint.type, variableConstructor)) return true
                }
            }
        }

        return false
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
