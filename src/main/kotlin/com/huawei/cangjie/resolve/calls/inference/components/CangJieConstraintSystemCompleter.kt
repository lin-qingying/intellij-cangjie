package com.huawei.cangjie.resolve.calls.inference.components

import com.huawei.cangjie.resolve.calls.components.transformToResolvedLambda
import com.huawei.cangjie.resolve.calls.inference.model.*
import com.huawei.cangjie.resolve.calls.model.*
import com.huawei.cangjie.types.CangJieType
import com.huawei.cangjie.types.ErrorUtils
import com.huawei.cangjie.types.TypeConstructor
import com.huawei.cangjie.types.UnwrappedType
import com.huawei.cangjie.types.error.ErrorTypeKind
import com.huawei.cangjie.types.error.MultipleSupertypeTypeInferenceFailure
import com.huawei.cangjie.types.model.CangJieTypeMarker
import com.huawei.cangjie.types.model.TypeConstructorMarker
import com.huawei.cangjie.types.model.TypeVariableMarker
import com.huawei.cangjie.types.model.TypeVariableTypeConstructorMarker
import com.intellij.util.containers.addIfNotNull

class CangJieConstraintSystemCompleter(
    private val resultTypeResolver: ResultTypeResolver,
    val variableFixationFinder: VariableFixationFinder,
    private val postponedArgumentsInputTypesResolver: PostponedArgumentInputTypesResolver,
//    private val languageVersionSettings: LanguageVersionSettings
) {
    fun completeConstraintSystem(
        c: ConstraintSystemCompletionContext,
        topLevelType: UnwrappedType,
        topLevelAtoms: List<ResolvedAtom>,
        completionMode: ConstraintSystemCompletionMode,
        diagnosticsHolder: CangJieDiagnosticsHolder
    ) {
        c.runCompletion(
            completionMode,
            topLevelAtoms,
            topLevelType,
            diagnosticsHolder,
            collectVariablesFromContext = true,
        ) {
            error("Shouldn't be called in complete constraint system mode")
        }
    }
    fun runCompletion(
        c: ConstraintSystemCompletionContext,
        completionMode: ConstraintSystemCompletionMode,
        topLevelAtoms: List<ResolvedAtom>,
        topLevelType: UnwrappedType,
        diagnosticsHolder: CangJieDiagnosticsHolder,
        analyze: (PostponedResolvedAtom) -> Unit
    ) {
        c.runCompletion(
            completionMode,
            topLevelAtoms,
            topLevelType,
            diagnosticsHolder,
            collectVariablesFromContext = false,
            analyze = analyze
        )
    }

    private fun ConstraintSystemCompletionContext.runCompletion(
        completionMode: ConstraintSystemCompletionMode,
        topLevelAtoms: List<ResolvedAtom>,
        topLevelType: UnwrappedType,
        diagnosticsHolder: CangJieDiagnosticsHolder,
        collectVariablesFromContext: Boolean,
        analyze: (PostponedResolvedAtom) -> Unit
    ) {
        val topLevelTypeVariables = topLevelType.extractTypeVariables()

        completion@ while (true) {
            // TODO: This is very slow
            val postponedArguments = getOrderedNotAnalyzedPostponedArguments(topLevelAtoms)

            if (completionMode == ConstraintSystemCompletionMode.UNTIL_FIRST_LAMBDA && hasLambdaToAnalyze(
//                    languageVersionSettings,
                    postponedArguments
                )
            ) return

            // Stage 1: analyze postponed arguments with fixed parameter types
            if (analyzeArgumentWithFixedParameterTypes(/*languageVersionSettings,*/ postponedArguments, analyze))
                continue

            val isThereAnyReadyForFixationVariable = variableFixationFinder.findFirstVariableForFixation(
                this,
                getOrderedAllTypeVariables(collectVariablesFromContext, topLevelAtoms),
                postponedArguments,
                completionMode,
                topLevelType
            ) != null

            // If there aren't any postponed arguments and ready for fixation variables, then completion isn't needed: nothing to do
            if (postponedArguments.isEmpty() && !isThereAnyReadyForFixationVariable)
                break

            val postponedArgumentsWithRevisableType = postponedArguments
                .filterIsInstance<PostponedAtomWithRevisableExpectedType>()
            val dependencyProvider =
                TypeVariableDependencyInformationProvider(notFixedTypeVariables, postponedArguments, topLevelType, this)

            // Stage 2: collect parameter types for postponed arguments
            val wasBuiltNewExpectedTypeForSomeArgument =
                postponedArgumentsInputTypesResolver.collectParameterTypesAndBuildNewExpectedTypes(
                    this,
                    postponedArgumentsWithRevisableType,
                    completionMode,
                    dependencyProvider,
                    topLevelTypeVariables
                )

            if (wasBuiltNewExpectedTypeForSomeArgument)
                continue

            if (completionMode == ConstraintSystemCompletionMode.FULL) {
                // Stage 3: fix variables for parameter types of all postponed arguments
                for (argument in postponedArguments) {
                    val variableWasFixed =
                        postponedArgumentsInputTypesResolver.fixNextReadyVariableForParameterTypeIfNeeded(
                            this,
                            argument,
                            postponedArguments,
                            topLevelType,
                            dependencyProvider,
                        ) {
                            findResolvedAtomBy(it, topLevelAtoms) ?: topLevelAtoms.firstOrNull()
                        }

                    if (variableWasFixed)
                        continue@completion
                }

                // Stage 4: create atoms with revised expected types if needed
                for (argument in postponedArgumentsWithRevisableType) {
                    val argumentWasTransformed = transformToAtomWithNewFunctionalExpectedType(
                        this, argument, diagnosticsHolder
                    )

                    if (argumentWasTransformed)
                        continue@completion
                }
            }

            // Stage 5: analyze the next ready postponed argument
            if (analyzeNextReadyPostponedArgument(/*languageVersionSettings,*/ postponedArguments,
                    completionMode,
                    analyze
                )
            )
                continue

            // Stage 6: fix next ready type variable with proper constraints
            if (
                fixNextReadyVariable(
                    completionMode,
                    topLevelAtoms,
                    topLevelType,
                    collectVariablesFromContext,
                    postponedArguments,
                    diagnosticsHolder
                )
            ) continue

            // Stage 7: try to complete call with the builder inference if there are uninferred type variables
            val areThereAppearedProperConstraintsForSomeVariable = tryToCompleteWithBuilderInference(
                completionMode,
                topLevelAtoms,
                topLevelType,
                postponedArguments,
                collectVariablesFromContext,
                diagnosticsHolder,
                analyze
            )

            if (areThereAppearedProperConstraintsForSomeVariable)
                continue

            // Stage 8: report "not enough information" for uninferred type variables
            reportNotEnoughTypeInformation(
                completionMode,
                topLevelAtoms,
                topLevelType,
                collectVariablesFromContext,
                postponedArguments,
                diagnosticsHolder
            )

            // Stage 9: force analysis of remaining not analyzed postponed arguments and rerun stages if there are
            if (completionMode == ConstraintSystemCompletionMode.FULL) {
                if (analyzeRemainingNotAnalyzedPostponedArgument(postponedArguments, analyze))
                    continue
            }

            break
        }
    }

    private fun ConstraintSystemCompletionContext.tryToCompleteWithBuilderInference(
        completionMode: ConstraintSystemCompletionMode,
        topLevelAtoms: List<ResolvedAtom>,
        topLevelType: UnwrappedType,
        postponedArguments: List<PostponedResolvedAtom>,
        collectVariablesFromContext: Boolean,
        diagnosticsHolder: CangJieDiagnosticsHolder,
        analyze: (PostponedResolvedAtom) -> Unit
    ): Boolean {
        if (completionMode != ConstraintSystemCompletionMode.FULL) return false


        val lambdaArguments =
            postponedArguments.filterIsInstance<ResolvedLambdaAtom>().takeIf { it.isNotEmpty() } ?: return false

        fun ResolvedLambdaAtom.notFixedInputTypeVariables(): List<TypeVariableTypeConstructorMarker> =
            inputTypes.flatMap { it.extractTypeVariables() }.filter { it !in fixedTypeVariables }


        val dangerousBuilderInferenceWithoutAnnotation =
            lambdaArguments.size >= 2 && lambdaArguments.count { it.notFixedInputTypeVariables().isNotEmpty() } >= 2

        val builder = getBuilder()
        for (argument in lambdaArguments) {
            val reallyHasBuilderInferenceAnnotation = argument.atom.hasBuilderInferenceAnnotation


            // Imitate having builder inference annotation. TODO: Remove after getting rid of @BuilderInference
            if (!reallyHasBuilderInferenceAnnotation) {
                argument.atom.hasBuilderInferenceAnnotation = true
            }

            val notFixedInputTypeVariables = argument.notFixedInputTypeVariables()

            // lambda is subject to builder inference past this point
            if (notFixedInputTypeVariables.isEmpty()) continue


            for (variable in notFixedInputTypeVariables) {
                builder.markPostponedVariable(notFixedTypeVariables.getValue(variable).typeVariable)
            }

            analyze(argument)
        }

        val variableForFixation = variableFixationFinder.findFirstVariableForFixation(
            this,
            getOrderedAllTypeVariables(collectVariablesFromContext, topLevelAtoms),
            postponedArguments,
            completionMode,
            topLevelType
        )

        // continue completion (rerun stages) only if ready for fixation variables with proper constraints have appeared
        // (after analysing a lambda with the builder inference)
        // otherwise we don't continue and report "not enough type information" error
        return variableForFixation?.isReady == true
    }

    private fun transformToAtomWithNewFunctionalExpectedType(
        c: ConstraintSystemCompletionContext,
        argument: PostponedAtomWithRevisableExpectedType,
        diagnosticsHolder: CangJieDiagnosticsHolder
    ): Boolean = with(c) {
        val revisedExpectedType: UnwrappedType = argument.revisedExpectedType
            ?.takeIf { it.isFunctionWithAny() } as UnwrappedType? ?: return false

        when (argument) {
            is PostponedCallableReferenceAtom ->
                CallableReferenceWithRevisedExpectedTypeAtom(argument.atom, revisedExpectedType).also {
                    argument.setAnalyzedResults(null, listOf(it))
                }

            is LambdaWithTypeVariableAsExpectedTypeAtom ->
                argument.transformToResolvedLambda(c.getBuilder(), diagnosticsHolder, revisedExpectedType)

            else -> throw IllegalStateException("Unsupported postponed argument type of $argument")
        }

        return true
    }

    private fun ConstraintSystemCompletionContext.fixNextReadyVariable(
        completionMode: ConstraintSystemCompletionMode,
        topLevelAtoms: List<ResolvedAtom>,
        topLevelType: UnwrappedType,
        collectVariablesFromContext: Boolean,
        postponedArguments: List<PostponedResolvedAtom>,
        diagnosticsHolder: CangJieDiagnosticsHolder
    ): Boolean {
        val variableForFixation = variableFixationFinder.findFirstVariableForFixation(
            this,
            getOrderedAllTypeVariables(collectVariablesFromContext, topLevelAtoms),
            postponedArguments,
            completionMode,
            topLevelType
        ) ?: return false

        if (!variableForFixation.isReady) return false

        fixVariable(
            this,
            notFixedTypeVariables.getValue(variableForFixation.variable),
            topLevelAtoms,
            diagnosticsHolder
        )

        return true
    }

    private fun fixVariable(
        c: ConstraintSystemCompletionContext,
        variableWithConstraints: VariableWithConstraints,
        topLevelAtoms: List<ResolvedAtom>,
        diagnosticsHolder: CangJieDiagnosticsHolder
    ) {
        fixVariable(
            c,
            variableWithConstraints,
            TypeVariableDirectionCalculator.ResolveDirection.UNKNOWN,
            topLevelAtoms,
            diagnosticsHolder
        )
    }

    private fun reportWarningIfFixedIntoDeclaredUpperBounds(
        diagnosticsHolder: CangJieDiagnosticsHolder,
        variableWithConstraints: VariableWithConstraints,
        resultType: CangJieTypeMarker
    ) {
        var upperBoundType: CangJieTypeMarker? = null
        val constraintFromDeclaredUpperBoundExists = variableWithConstraints.constraints.any { constraint ->
            val position = constraint.position.from.let { basicPosition ->
                if (basicPosition is IncorporationConstraintPosition) basicPosition.from else basicPosition
            }
            if (position is BuilderInferenceSubstitutionConstraintPosition<*> && position.isFromNotSubstitutedDeclaredUpperBound) {
                upperBoundType = constraint.type
                true
            } else {
                false
            }
        }
        if (resultType is MultipleSupertypeTypeInferenceFailure) {
            diagnosticsHolder.addDiagnostic(
                CangJieConstraintSystemDiagnostic(
                    MultipleMinimalCommonSupertypes(
                        variableWithConstraints.typeVariable,
                        resultType.intersectedTypes
                    )
                )
            )

        }
        if (constraintFromDeclaredUpperBoundExists && upperBoundType == resultType) {
            diagnosticsHolder.addDiagnostic(
                CangJieConstraintSystemDiagnostic(InferredIntoDeclaredUpperBounds(variableWithConstraints.typeVariable))
            )
        }
    }

    private fun fixVariable(
        c: ConstraintSystemCompletionContext,
        variableWithConstraints: VariableWithConstraints,
        direction: TypeVariableDirectionCalculator.ResolveDirection,
        topLevelAtoms: List<ResolvedAtom>,
        diagnosticsHolder: CangJieDiagnosticsHolder
    ) {
        val resultType = resultTypeResolver.findResultType(c, variableWithConstraints, direction)

        val variable = variableWithConstraints.typeVariable
        val resolvedAtom = findResolvedAtomBy(variable, topLevelAtoms) ?: topLevelAtoms.firstOrNull()

        reportWarningIfFixedIntoDeclaredUpperBounds(diagnosticsHolder, variableWithConstraints, resultType)

        c.fixVariable(variable, resultType, FixVariableConstraintPositionImpl(variable, resolvedAtom))
    }

    private fun ConstraintSystemCompletionContext.reportNotEnoughTypeInformation(
        completionMode: ConstraintSystemCompletionMode,
        topLevelAtoms: List<ResolvedAtom>,
        topLevelType: UnwrappedType,
        collectVariablesFromContext: Boolean,
        postponedArguments: List<PostponedResolvedAtom>,
        diagnosticsHolder: CangJieDiagnosticsHolder
    ) {
        while (true) {
            val variableForFixation = variableFixationFinder.findFirstVariableForFixation(
                this, getOrderedAllTypeVariables(collectVariablesFromContext, topLevelAtoms),
                postponedArguments, completionMode, topLevelType,
            ) ?: break

            assert(!variableForFixation.isReady) {
                "At this stage there should be no remaining variables with proper constraints"
            }

            if (completionMode == ConstraintSystemCompletionMode.PARTIAL) break

            val variableWithConstraints = notFixedTypeVariables.getValue(variableForFixation.variable)
            processVariableWhenNotEnoughInformation(variableWithConstraints, topLevelAtoms, diagnosticsHolder)
        }
    }

    private fun ConstraintSystemCompletionContext.processVariableWhenNotEnoughInformation(
        variableWithConstraints: VariableWithConstraints,
        topLevelAtoms: List<ResolvedAtom>,
        diagnosticsHolder: CangJieDiagnosticsHolder
    ) {
        val typeVariable = variableWithConstraints.typeVariable
        val resolvedAtom = findResolvedAtomBy(typeVariable, topLevelAtoms) ?: topLevelAtoms.firstOrNull()

        if (resolvedAtom != null) {
            addError(
                NotEnoughInformationForTypeParameterImpl(
                    typeVariable,
                    resolvedAtom,
                    couldBeResolvedWithUnrestrictedBuilderInference()
                )
            )
        }

        val resultErrorType = when {
            typeVariable is TypeVariableFromCallableDescriptor -> {
                ErrorUtils.createErrorType(
                    ErrorTypeKind.UNINFERRED_TYPE_VARIABLE,
                    typeVariable.originalTypeParameter.name.asString()
                )
            }

            typeVariable is TypeVariableForLambdaParameterType && typeVariable.atom is LambdaCangJieCallArgument -> {
                diagnosticsHolder.addDiagnostic(
                    NotEnoughInformationForLambdaParameter(typeVariable.atom, typeVariable.index)
                )
                ErrorUtils.createErrorType(ErrorTypeKind.UNINFERRED_LAMBDA_PARAMETER_TYPE)
            }

            else -> ErrorUtils.createErrorType(ErrorTypeKind.UNINFERRED_TYPE_VARIABLE, typeVariable.toString())
        }

        fixVariable(typeVariable, resultErrorType, FixVariableConstraintPositionImpl(typeVariable, resolvedAtom))

    }

    private fun ConstraintSystemCompletionContext.getOrderedAllTypeVariables(
        collectVariablesFromContext: Boolean,
        topLevelAtoms: List<ResolvedAtom>
    ): List<TypeConstructorMarker> {
        if (collectVariablesFromContext)
            return notFixedTypeVariables.keys.toList()

        fun getVariablesFromRevisedExpectedType(revisedExpectedType: CangJieType?) =
            revisedExpectedType?.arguments?.map { it.type.constructor }?.filterIsInstance<TypeVariableTypeConstructor>()

        // Note that it's important to use Set here, because several atoms can share the same type variable
        val result = linkedSetOf<TypeConstructor>()

        fun ResolvedAtom.collectAllTypeVariables() {
            val typeVariables = when (this) {
                is ResolvedLambdaAtom -> {
                    listOfNotNull(typeVariableForLambdaReturnType?.freshTypeConstructor)
                }
//                is LambdaWithTypeVariableAsExpectedTypeAtom -> {
//                    getVariablesFromRevisedExpectedType(revisedExpectedType).orEmpty()
//                }
                is PostponedCallableReferenceAtom -> {
                    getVariablesFromRevisedExpectedType(revisedExpectedType).orEmpty() +
                            candidate?.freshVariablesSubstitutor?.freshVariables?.map { it.freshTypeConstructor }
                                .orEmpty()
                }

                is ResolvedCallAtom -> freshVariablesSubstitutor.freshVariables.map { it.freshTypeConstructor }
                is ResolvedCallableReferenceArgumentAtom -> candidate?.freshVariablesSubstitutor?.freshVariables?.map { it.freshTypeConstructor }
                    .orEmpty()

                else -> emptyList()
            }


            typeVariables.mapNotNullTo(result) {

                it.takeIf { notFixedTypeVariables.containsKey(it) }
            }

            /*
             * Hack for completing error candidates in delegate resolve
             */
            if (this is StubResolvedAtom && typeVariable in notFixedTypeVariables) {
                result += typeVariable
            }

            if (analyzed) {
                subResolvedAtoms?.forEach { it.collectAllTypeVariables() }
            }
        }

        for (topLevelAtom in topLevelAtoms) {
            topLevelAtom.collectAllTypeVariables()
        }

        require(result.size == notFixedTypeVariables.size) {
            val notFoundTypeVariables = notFixedTypeVariables.keys.toMutableSet().apply { removeAll(result) }
            "Not all type variables found: $notFoundTypeVariables"
        }

        return result.toList()
    }

    companion object {
        fun findResolvedAtomBy(typeVariable: TypeVariableMarker, topLevelAtoms: List<ResolvedAtom>): ResolvedAtom? {
            fun ResolvedAtom.check(): ResolvedAtom? {
                val suitableCall = when (this) {
                    is ResolvedCallAtom -> typeVariable in freshVariablesSubstitutor.freshVariables
                    is ResolvedCallableReferenceArgumentAtom -> candidate?.freshVariablesSubstitutor?.freshVariables?.let { typeVariable in it }
                        ?: false

                    is ResolvedLambdaAtom -> typeVariable == typeVariableForLambdaReturnType
                    else -> false
                }

                if (suitableCall) {
                    return this
                }

                subResolvedAtoms?.forEach { subResolvedAtom ->
                    subResolvedAtom.check()?.let { result -> return@check result }
                }

                return null
            }

            for (topLevelAtom in topLevelAtoms) {
                topLevelAtom.check()?.let { return it }
            }

            return null
        }

        fun getOrderedNotAnalyzedPostponedArguments(topLevelAtoms: List<ResolvedAtom>): List<PostponedResolvedAtom> {
            fun ResolvedAtom.process(to: MutableList<PostponedResolvedAtom>) {
                to.addIfNotNull((this as? PostponedResolvedAtom)?.takeUnless { it.analyzed })

                if (analyzed) {
                    subResolvedAtoms?.forEach { it.process(to) }
                }
            }

            val notAnalyzedArguments = arrayListOf<PostponedResolvedAtom>()
            for (primitive in topLevelAtoms) {
                primitive.process(notAnalyzedArguments)
            }

            return notAnalyzedArguments
        }
    }
}
