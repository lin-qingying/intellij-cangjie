package com.linqingying.cangjie.resolve.calls.components

import com.linqingying.cangjie.builtins.*
import com.linqingying.cangjie.config.LanguageFeature
import com.linqingying.cangjie.config.LanguageVersionSettings
import com.linqingying.cangjie.descriptors.annotations.Annotations
import com.linqingying.cangjie.descriptors.annotations.FilteredAnnotations
import com.linqingying.cangjie.resolve.calls.inference.addSubsystemFromArgument
import com.linqingying.cangjie.resolve.calls.inference.components.ConstraintSystemCompletionMode
import com.linqingying.cangjie.resolve.calls.inference.components.freshTypeConstructor
import com.linqingying.cangjie.resolve.calls.inference.model.BuilderInferencePosition
import com.linqingying.cangjie.resolve.calls.inference.model.LambdaArgumentConstraintPositionImpl
import com.linqingying.cangjie.resolve.calls.inference.model.NewTypeVariable
import com.linqingying.cangjie.resolve.calls.model.*
import com.linqingying.cangjie.types.CangJieType
import com.linqingying.cangjie.types.StubTypeForBuilderInference
import com.linqingying.cangjie.types.UnwrappedType
import com.linqingying.cangjie.types.model.StubTypeMarker
import com.linqingying.cangjie.types.model.TypeVariableMarker
import com.linqingying.cangjie.types.model.defaultType
import com.linqingying.cangjie.types.model.safeSubstitute
import com.linqingying.cangjie.types.util.builtIns

class PostponedArgumentsAnalyzer(
    private val callableReferenceArgumentResolver: CallableReferenceArgumentResolver,
    private val languageVersionSettings: LanguageVersionSettings
) {
    data class SubstitutorAndStubsForLambdaAnalysis(
        val stubsForPostponedVariables: Map<TypeVariableMarker, StubTypeMarker>,
        val substitute: (CangJieType) -> UnwrappedType
    )

    fun PostponedArgumentsAnalyzerContext.createSubstituteFunctorForLambdaAnalysis(): SubstitutorAndStubsForLambdaAnalysis {
        val stubsForPostponedVariables = bindingStubsForPostponedVariables()
        val currentSubstitutor =
            buildCurrentSubstitutor(stubsForPostponedVariables.mapKeys { it.key.freshTypeConstructor(this) })
        return SubstitutorAndStubsForLambdaAnalysis(stubsForPostponedVariables) {
            currentSubstitutor.safeSubstitute(this, it) as UnwrappedType
        }
    }

    private fun UnwrappedType?.receiver(): UnwrappedType? {
        return forFunctionalType { getReceiverTypeFromFunctionType()?.unwrap() }
    }

    private inline fun <T> UnwrappedType?.forFunctionalType(f: UnwrappedType.() -> T?): T? {
        return if (this?.isBuiltinFunctionalType == true) f(this) else null
    }

    private fun UnwrappedType?.contextReceivers(): List<UnwrappedType>? {
        return forFunctionalType { getContextReceiverTypesFromFunctionType().map { it.unwrap() } }
    }

    private fun UnwrappedType?.valueParameters(): List<UnwrappedType>? {
        return forFunctionalType { getValueParameterTypesFromFunctionType().map { it.type.unwrap() } }
    }

    fun analyzeLambda(
        c: PostponedArgumentsAnalyzerContext,
        resolutionCallbacks: CangJieResolutionCallbacks,
        lambda: ResolvedLambdaAtom,
        completionMode: ConstraintSystemCompletionMode,
        diagnosticHolder: CangJieDiagnosticsHolder,
    ): ReturnArgumentsAnalysisResult {
        val substitutorAndStubsForLambdaAnalysis = c.createSubstituteFunctorForLambdaAnalysis()
        val substitute = substitutorAndStubsForLambdaAnalysis.substitute

        // Expected type has a higher priority against which lambda should be analyzed
        // Mostly, this is needed to report more specific diagnostics on lambda parameters
        fun expectedOrActualType(expected: UnwrappedType?, actual: UnwrappedType?): UnwrappedType? {
            val expectedSubstituted = expected?.let(substitute)
            return if (expectedSubstituted != null && c.canBeProper(expectedSubstituted)) expectedSubstituted else actual?.let(
                substitute
            )
        }

        val builtIns = c.getBuilder().builtIns

        val expectedParameters = lambda.expectedType.valueParameters()
        val expectedReceiver = lambda.expectedType.receiver()
        val expectedContextReceivers = lambda.expectedType.contextReceivers()

        val receiver = lambda.receiver?.let {
            expectedOrActualType(expectedReceiver ?: expectedParameters?.getOrNull(0), lambda.receiver)
        }
        val contextReceivers = lambda.contextReceivers.mapIndexedNotNull { i, contextReceiver ->
            expectedOrActualType(expectedContextReceivers?.getOrNull(i), contextReceiver)
        }

        val expectedParametersToMatchAgainst = when {
            receiver == null && expectedReceiver != null && expectedParameters != null -> listOf(expectedReceiver) + expectedParameters
            receiver == null && expectedReceiver != null -> listOf(expectedReceiver)
            receiver != null && expectedReceiver == null -> expectedParameters?.drop(1)
            else -> expectedParameters
        }

        val parameters =
            expectedParametersToMatchAgainst?.mapIndexed { index, expected ->
                expectedOrActualType(expected, lambda.parameters.getOrNull(index)) ?: builtIns.nothingType
            } ?: lambda.parameters.map(substitute)

        val rawReturnType = lambda.returnType

        val expectedTypeForReturnArguments = when {
            c.canBeProper(rawReturnType) -> substitute(rawReturnType)

            // For Unit-coercion
            !rawReturnType.isMarkedOption && c.hasUpperOrEqualUnitConstraint(rawReturnType) -> builtIns.unitType

            else -> null
        }

        val convertedAnnotations = lambda.expectedType?.annotations?.let { annotations ->
            if (receiver != null || expectedReceiver == null) annotations
            else FilteredAnnotations(annotations, true) { it != StandardNames.FqNames.extensionFunctionType }
        }

        @Suppress("UNCHECKED_CAST")
        val returnArgumentsAnalysisResult = resolutionCallbacks.analyzeAndGetLambdaReturnArguments(
            lambda.atom,

            receiver,
            contextReceivers,
            parameters,
            expectedTypeForReturnArguments,
            convertedAnnotations ?: Annotations.EMPTY,
            substitutorAndStubsForLambdaAnalysis.stubsForPostponedVariables as Map<NewTypeVariable, StubTypeForBuilderInference>,
        )
        applyResultsOfAnalyzedLambdaToCandidateSystem(
            c,
            lambda,
            returnArgumentsAnalysisResult,
            completionMode,
            diagnosticHolder,
            substitute
        )
        return returnArgumentsAnalysisResult
    }

    fun applyResultsOfAnalyzedLambdaToCandidateSystem(
        c: PostponedArgumentsAnalyzerContext,
        lambda: ResolvedLambdaAtom,
        returnArgumentsAnalysisResult: ReturnArgumentsAnalysisResult,
        completionMode: ConstraintSystemCompletionMode,
        diagnosticHolder: CangJieDiagnosticsHolder,
        substitute: (CangJieType) -> UnwrappedType = c.createSubstituteFunctorForLambdaAnalysis().substitute
    ) {
        val (returnArgumentsInfo, inferenceSession, hasInapplicableCallForBuilderInference) =
            returnArgumentsAnalysisResult

        if (hasInapplicableCallForBuilderInference) {
            inferenceSession?.initializeLambda(lambda)
            c.getBuilder().markCouldBeResolvedWithUnrestrictedBuilderInference()
            c.getBuilder().removePostponedVariables()
            return
        }

        val returnArguments = returnArgumentsInfo.nonErrorArguments
        returnArguments.forEach { c.addSubsystemFromArgument(it) }

        val lastExpression = returnArgumentsInfo.lastExpression
        val allReturnArguments =
            if (lastExpression != null && returnArgumentsInfo.lastExpressionCoercedToUnit && c.addSubsystemFromArgument(
                    lastExpression
                )
            ) {
                returnArguments + lastExpression
            } else {
                returnArguments
            }

        val subResolvedKtPrimitives = allReturnArguments.map {
            resolveCjPrimitive(
                c.getBuilder(), it, lambda.returnType.let(substitute),
                diagnosticHolder, ReceiverInfo.notReceiver, convertedType = null,
                inferenceSession
            )
        }

        if (!returnArgumentsInfo.returnArgumentsExist) {
            val unitType = lambda.returnType.builtIns.unitType
            val lambdaReturnType = lambda.returnType.let(substitute)
            c.getBuilder()
                .addSubtypeConstraint(unitType, lambdaReturnType, LambdaArgumentConstraintPositionImpl(lambda))
        }

        lambda.setAnalyzedResults(returnArgumentsInfo, subResolvedKtPrimitives)

        val shouldUseBuilderInference = lambda.atom.hasBuilderInferenceAnnotation
                || languageVersionSettings.supportsFeature(LanguageFeature.UseBuilderInferenceWithoutAnnotation)

        if (inferenceSession != null && shouldUseBuilderInference) {
            val constraintSystemBuilder = c.getBuilder()

            val postponedVariables = inferenceSession.inferPostponedVariables(
                lambda,
                constraintSystemBuilder,
                completionMode,
                diagnosticHolder
            )
            if (postponedVariables == null) {
                c.getBuilder().removePostponedVariables()
                return
            }

            // WARN: Following type constraint system unification algorithm is incorrect,
            // To perform constraint unification properly, original constraints should be
            // unified instead of simple result type based constraint
            // Other possible solution is to add equality constraint, but it will be too strict
            // and will limit usability
            // Nevertheless, proper design should be done before fixing this
            for ((constructor, resultType) in postponedVariables) {
                val variableWithConstraints =
                    constraintSystemBuilder.currentStorage().notFixedTypeVariables[constructor] ?: continue
                val variable = variableWithConstraints.typeVariable

                c.getBuilder().unmarkPostponedVariable(variable)

                // We add <inferred type> <: TypeVariable(T) to be able to contribute type info from several builder inference lambdas
                c.getBuilder().addSubtypeConstraint(resultType, variable.defaultType(c), BuilderInferencePosition)
            }

            c.removePostponedTypeVariablesFromConstraints(postponedVariables.keys)
        }
    }

    fun analyze(
        c: PostponedArgumentsAnalyzerContext,
        resolutionCallbacks: CangJieResolutionCallbacks,
        argument: ResolvedAtom,
        completionMode: ConstraintSystemCompletionMode,
        diagnosticsHolder: CangJieDiagnosticsHolder
    ) {
        when (argument) {
            is ResolvedLambdaAtom ->
                analyzeLambda(c, resolutionCallbacks, argument, completionMode, diagnosticsHolder)

            is LambdaWithTypeVariableAsExpectedTypeAtom ->
                analyzeLambda(
                    c,
                    resolutionCallbacks,
                    argument.transformToResolvedLambda(c.getBuilder(), diagnosticsHolder),
                    completionMode,
                    diagnosticsHolder
                )

            is ResolvedCallableReferenceArgumentAtom ->
                callableReferenceArgumentResolver.processCallableReferenceArgument(
                    c.getBuilder(), argument, diagnosticsHolder, resolutionCallbacks
                )

            is ResolvedCollectionLiteralAtom -> TODO("Not supported")

            else -> error("Unexpected resolved primitive: ${argument.javaClass.canonicalName}")
        }
    }
}
