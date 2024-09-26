package com.huawei.cangjie.resolve.calls.inference.components

import com.huawei.cangjie.resolve.calls.inference.ConstraintSystemBuilder
import com.huawei.cangjie.resolve.calls.inference.model.*
import com.huawei.cangjie.resolve.calls.model.PostponedAtomWithRevisableExpectedType
import com.huawei.cangjie.resolve.calls.model.PostponedResolvedAtomMarker
import com.huawei.cangjie.types.model.COnly
import com.huawei.cangjie.types.model.CangJieTypeMarker
import com.huawei.cangjie.types.model.TypeConstructorMarker
import com.huawei.cangjie.types.model.TypeVariableMarker





abstract class ConstraintSystemCompletionContext : VariableFixationFinder.Context, ResultTypeResolver.Context {
    abstract override val notFixedTypeVariables: Map<TypeConstructorMarker, VariableWithConstraints>
    abstract override val fixedTypeVariables: Map<TypeConstructorMarker, CangJieTypeMarker>
    abstract override val postponedTypeVariables: List<TypeVariableMarker>

    abstract fun getBuilder(): ConstraintSystemBuilder

    // type can be proper if it not contains not fixed type variables
    abstract fun canBeProper(type: CangJieTypeMarker): Boolean

    abstract fun containsOnlyFixedOrPostponedVariables(type: CangJieTypeMarker): Boolean
    abstract fun containsOnlyFixedVariables(type: CangJieTypeMarker): Boolean

    // mutable operations
    abstract fun addError(error: ConstraintSystemError)

    abstract fun fixVariable(
        variable: TypeVariableMarker,
        resultType: CangJieTypeMarker,
        position: FixVariableConstraintPosition<*>,
        resultTypeForOnlyInputTypes: CangJieTypeMarker = resultType
    )

    abstract fun couldBeResolvedWithUnrestrictedBuilderInference(): Boolean
    abstract fun resolveForkPointsConstraints()

    fun <A : PostponedResolvedAtomMarker> analyzeArgumentWithFixedParameterTypes(
//        languageVersionSettings: LanguageVersionSettings,
        postponedArguments: List<A>,
        analyze: (A) -> Unit
    ): Boolean {
//        val useBuilderInferenceOnlyIfNeeded =
//            languageVersionSettings.supportsFeature(LanguageFeature.UseBuilderInferenceOnlyIfNeeded)
        val argumentToAnalyze =
        //        if (useBuilderInferenceOnlyIfNeeded) {
            findPostponedArgumentWithFixedInputTypes(postponedArguments)
//        } else {
//            findPostponedArgumentWithFixedOrPostponedInputTypes(postponedArguments)
//        }

        if (argumentToAnalyze != null) {
            analyze(argumentToAnalyze)
            return true
        }

        return false
    }

    fun <A : PostponedResolvedAtomMarker> analyzeNextReadyPostponedArgument(
//        languageVersionSettings: LanguageVersionSettings,
        postponedArguments: List<A>,
        completionMode: ConstraintSystemCompletionMode,
        analyze: (A) -> Unit
    ): Boolean {
        if (completionMode.allLambdasShouldBeAnalyzed) {
            val argumentWithTypeVariableAsExpectedType =
                findPostponedArgumentWithRevisableExpectedType(postponedArguments)

            if (argumentWithTypeVariableAsExpectedType != null) {
                analyze(argumentWithTypeVariableAsExpectedType)
                return true
            }
        }

        return analyzeArgumentWithFixedParameterTypes(/*languageVersionSettings, */postponedArguments, analyze)
    }

    fun <A : PostponedResolvedAtomMarker> analyzeRemainingNotAnalyzedPostponedArgument(
        postponedArguments: List<A>,
        analyze: (A) -> Unit
    ): Boolean {
        val remainingNotAnalyzedPostponedArgument = postponedArguments.firstOrNull { !it.analyzed }

        if (remainingNotAnalyzedPostponedArgument != null) {
            analyze(remainingNotAnalyzedPostponedArgument)
            return true
        }

        return false
    }

    fun <A : PostponedResolvedAtomMarker> hasLambdaToAnalyze(
//        languageVersionSettings: LanguageVersionSettings,
        postponedArguments: List<A>
    ): Boolean {
        return analyzeArgumentWithFixedParameterTypes(/*languageVersionSettings,*/ postponedArguments) {}
    }

    // Avoiding smart cast from filterIsInstanceOrNull looks dirty
    private fun <A : PostponedResolvedAtomMarker> findPostponedArgumentWithRevisableExpectedType(postponedArguments: List<A>): A? =
        postponedArguments.firstOrNull { argument -> argument is PostponedAtomWithRevisableExpectedType }

    private fun <T : PostponedResolvedAtomMarker> findPostponedArgumentWithFixedOrPostponedInputTypes(
        postponedArguments: List<T>
    ) =
        postponedArguments.firstOrNull { argument -> argument.inputTypes.all { containsOnlyFixedOrPostponedVariables(it) } }

    private fun <T : PostponedResolvedAtomMarker> findPostponedArgumentWithFixedInputTypes(
        postponedArguments: List<T>
    ) = postponedArguments.firstOrNull { argument -> argument.inputTypes.all { containsOnlyFixedVariables(it) } }

    fun List<Constraint>.extractUpperTypesToCheckIntersectionEmptiness(): List<CangJieTypeMarker> =
        filter { constraint ->
            constraint.kind == ConstraintKind.UPPER && !constraint.type.contains {
                !it.typeConstructor().isClassTypeConstructor() && !it.typeConstructor().isTypeParameterTypeConstructor()
            }
        }.map { it.type }

    /**
     * @see [com.huawei.cangjie.resolve.calls.inference.components.VariableFixationFinder.Context.typeVariablesThatAreNotCountedAsProperTypes]
     * @see [com.huawei.cangjie.fir.resolve.transformers.body.resolve.FirDeclarationsResolveTransformer.fixInnerVariablesForProvideDelegateIfNeeded]
     */
    @COnly
    abstract fun <R> withTypeVariablesThatAreCountedAsProperTypes(
        typeVariables: Set<TypeConstructorMarker>,
        block: () -> R
    ): R
}
