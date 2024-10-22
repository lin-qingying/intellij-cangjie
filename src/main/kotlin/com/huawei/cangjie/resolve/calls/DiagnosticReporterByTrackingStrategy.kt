package com.huawei.cangjie.resolve.calls


import com.huawei.cangjie.builtins.UnsignedTypes
import com.huawei.cangjie.builtins.isExtensionFunctionType
import com.huawei.cangjie.config.LanguageFeature
import com.huawei.cangjie.diagnostics.Diagnostic
import com.huawei.cangjie.diagnostics.DiagnosticFactory2
import com.huawei.cangjie.diagnostics.Errors.*
import com.huawei.cangjie.diagnostics.reportDiagnosticOnce
import com.huawei.cangjie.psi.*
import com.huawei.cangjie.psi.psiUtil.lastBlockStatementOrThis
import com.huawei.cangjie.resolve.BindingContext
import com.huawei.cangjie.resolve.calls.context.BasicCallResolutionContext
import com.huawei.cangjie.resolve.calls.inference.model.*
import com.huawei.cangjie.resolve.calls.model.*
import com.huawei.cangjie.resolve.calls.smartcasts.DataFlowValueFactory
import com.huawei.cangjie.resolve.calls.smartcasts.SingleSmartCast
import com.huawei.cangjie.resolve.calls.smartcasts.SmartCastManager
import com.huawei.cangjie.resolve.calls.tasks.TracingStrategy
import com.huawei.cangjie.resolve.calls.tower.*
import com.huawei.cangjie.resolve.calls.util.extractCallableReferenceExpression
import com.huawei.cangjie.resolve.calls.util.getResolvedCall
import com.huawei.cangjie.resolve.calls.util.reportTrailingLambdaErrorOr
import com.huawei.cangjie.resolve.constants.CompileTimeConstantChecker
import com.huawei.cangjie.resolve.constants.TypedCompileTimeConstant
import com.huawei.cangjie.resolve.constants.evaluate.ConstantExpressionEvaluator
import com.huawei.cangjie.resolve.descriptorUtil.module
import com.huawei.cangjie.resolve.scopes.receivers.ExpressionReceiver
import com.huawei.cangjie.types.AbstractTypeChecker
import com.huawei.cangjie.types.CangJieType
import com.huawei.cangjie.types.ErrorUtils
import com.huawei.cangjie.types.checker.SimpleClassicTypeSystemContext.isNothing
import com.huawei.cangjie.types.checker.intersectWrappedTypes
import com.huawei.cangjie.types.expressions.ControlStructureTypingUtils
import com.huawei.cangjie.types.model.TypeSystemInferenceExtensionContextDelegate
import com.huawei.cangjie.types.model.TypeVariableMarker
import com.huawei.cangjie.types.model.freshTypeConstructor
import com.huawei.cangjie.types.util.TypeUtils
import com.huawei.cangjie.types.util.contains
import com.huawei.cangjie.types.util.makeOptional
import com.huawei.cangjie.utils.shouldNotBeCalled
import io.github.classgraph.TypeArgument
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

class DiagnosticReporterByTrackingStrategy(
    val constantExpressionEvaluator: ConstantExpressionEvaluator,
    val context: BasicCallResolutionContext,
    val psiCangJieCall: PSICangJieCall,
    val dataFlowValueFactory: DataFlowValueFactory,
    private val allDiagnostics: List<CangJieCallDiagnostic>,
    private val smartCastManager: SmartCastManager,
    private val typeSystemContext: TypeSystemInferenceExtensionContextDelegate
) : DiagnosticReporter {
    private val trace = context.trace as TrackingBindingTrace
    private val tracingStrategy: TracingStrategy get() = psiCangJieCall.tracingStrategy
    private val call: Call get() = psiCangJieCall.psiCall

    override fun onExplicitReceiver(diagnostic: CangJieCallDiagnostic) {

    }

    private val reportAdditionalErrors: Boolean
        get() = !context.languageVersionSettings.supportsFeature(LanguageFeature.NoAdditionalErrorsInDiagnosticReporter)

    override fun onCall(diagnostic: CangJieCallDiagnostic) {
        when (diagnostic) {
            is NoCallOperatorFunction -> {
                trace.report(
                    NO_CALL_OPERATOR.on(
                        psiCangJieCall.psiCall.callElement as CjCallExpression,
                        diagnostic.descriptor
                    )
                )

            }

            is StaticContextAccessNonStaticMemberDiagnostic -> {
                trace.report(
                    STATIC_CONTEXT_REFERENCE_ERROR.on(
                        psiCangJieCall.psiCall.callElement,
                        diagnostic.kind,
                        diagnostic.descriptor
                    )
                )
            }

            is NonStaticContextAccessStaticMemberDiagnostic -> {
                trace.report(
                    INSTANCE_ACCESS_STATIC_MEMBER_ERROR.on(
                        psiCangJieCall.psiCall.callElement,
                        diagnostic.kind,
                        diagnostic.descriptor
                    )
                )

            }

//            is NoneOperatorCallDiagnostic -> {
//             if(diagnostic.left !is ErrorType && diagnostic.right !is ErrorType){
//                 call.calleeExpression?.let{
//                     trace.report(
//                         INVALID_BINARY_OPERATOR.on(
//                             it, InvalidBinaryData(
//                                 psiCangJieCall.name.asOperatorString(), diagnostic.left, diagnostic.right
//                             )
//                         )
//                     )
//                 }
//             }
//
//
//            }

            is VisibilityError -> tracingStrategy.invisibleMember(trace, diagnostic.invisibleMember)
            is NoValueForParameter -> tracingStrategy.noValueForParameter(trace, diagnostic.parameterDescriptor)
//            is TypeCheckerHasRanIntoRecursion -> {
//                // Note: we have two similar diagnostics here
//                // - TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM (error starting from 1.7)
//                // - TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM_IN_AUGMENTED_ASSIGNMENT (error starting from 1.9)
//                // however they have different deprecation cycle, and thus it's better to distinguish them.
//                // This 'insideAugmentedAssignment' is just a heuristics (approximate) to do it.
//                // It cannot turn red code to green or green to red; the worst thing we can get here
//                // is replacing red code with yellow, if e.g. LV is set to 1.8 explicitly,
//                // and we have chosen the second diagnostics instead of the first one.
//                val insideAugmentedAssignment = call.callElement.parents.any {
//                    it is CjBinaryExpression && it.operationToken in CjTokens.AUGMENTED_ASSIGNMENTS
//                }
//                tracingStrategy.recursiveType(trace, context.languageVersionSettings, insideAugmentedAssignment)
//            }
//            is InstantiationOfAbstractClass -> tracingStrategy.instantiationOfAbstractClass(trace)
//            is AbstractSuperCall -> {
//                val superExpression = diagnostic.receiver.psiExpression as? CjSuperExpression
//                if (context.languageVersionSettings.supportsFeature(LanguageFeature.ForbidSuperDelegationToAbstractAnyMethod) ||
//                    superExpression == null ||
//                    trace[BindingContext.SUPER_EXPRESSION_FROM_ANY_MIGRATION, superExpression] != true
//                ) {
//                    tracingStrategy.abstractSuperCall(trace)
//                } else {
//                    tracingStrategy.abstractSuperCallWarning(trace)
//                }
//            }
//            is AbstractFakeOverrideSuperCall -> {
//                if (context.languageVersionSettings.supportsFeature(LanguageFeature.ForbidSuperDelegationToAbstractFakeOverride)) {
//                    tracingStrategy.abstractSuperCall(trace)
//                } else {
//                    tracingStrategy.abstractSuperCallWarning(trace)
//                }
//            }
//            is NonApplicableCallForBuilderInferenceDiagnostic -> {
//                val reportOn = diagnostic.cangjieCall
//                trace.reportDiagnosticOnce(NON_APPLICABLE_CALL_FOR_BUILDER_INFERENCE.on(reportOn.psiCangJieCall.psiCall.callElement))
//            }
//            is CandidateChosenUsingOverloadResolutionByLambdaAnnotation -> {
//                trace.report(CANDIDATE_CHOSEN_USING_OVERLOAD_RESOLUTION_BY_LAMBDA_ANNOTATION.on(psiCangJieCall.psiCall.callElement))
//            }
//            is EnumEntryAmbiguityWarning -> {
//                val propertyDescriptor = diagnostic.property
//                val enumEntryDescriptor = diagnostic.enumEntry
//                val enumCompanionDescriptor = (enumEntryDescriptor.containingDeclaration as? ClassDescriptor)?.companionObjectDescriptor
//                if (enumCompanionDescriptor == null || propertyDescriptor.containingDeclaration != enumCompanionDescriptor) {
//                    trace.report(
//                        DEPRECATED_RESOLVE_WITH_AMBIGUOUS_ENUM_ENTRY.on(
//                            psiCangJieCall.psiCall.callElement, propertyDescriptor, enumEntryDescriptor
//                        )
//                    )
//                }
//            }
//            is CompatibilityWarning -> {
//                val callElement = psiCangJieCall.psiCall.callElement
//                trace.report(
//                    COMPATIBILITY_WARNING.on(callElement.getCalleeExpressionIfAny() ?: callElement, diagnostic.candidate)
//                )
//            }
//            is NoContextReceiver -> {
//                val callElement = psiCangJieCall.psiCall.callElement
//                trace.report(
//                    NO_CONTEXT_RECEIVER.on(
//                        callElement.getCalleeExpressionIfAny() ?: callElement,
//                        diagnostic.receiverDescriptor.value.toString()
//                    )
//                )
//            }
//            is MultipleArgumentsApplicableForContextReceiver -> {
//                val callElement = psiCangJieCall.psiCall.callElement
//                trace.report(
//                    MULTIPLE_ARGUMENTS_APPLICABLE_FOR_CONTEXT_RECEIVER.on(callElement, diagnostic.receiverDescriptor.value.toString())
//                )
//            }
//            is ContextReceiverAmbiguity -> {
//                val callElement = psiCangJieCall.psiCall.callElement
//                trace.report(AMBIGUOUS_CALL_WITH_IMPLICIT_CONTEXT_RECEIVER.on(callElement))
//            }
//            is UnsupportedContextualDeclarationCall -> {
//                val callElement = psiCangJieCall.psiCall.callElement
//                trace.report(UNSUPPORTED_CONTEXTUAL_DECLARATION_CALL.on(callElement))
//            }
//
//            is AdaptedCallableReferenceIsUsedWithReflection, is NotCallableMemberReference, is CallableReferencesDefaultArgumentUsed -> {
//                // AdaptedCallableReferenceIsUsedWithReflection -> reported in onCallArgument
//                // NotCallableMemberReference -> UNSUPPORTED reported in DoubleColonExpressionResolver
//                // CallableReferencesDefaultArgumentUsed -> possible in 1.3 and earlier versions only
//                return
//            }

            else -> {
                unknownError(diagnostic, "onCall")
            }
        }
    }

    private fun unknownError(diagnostic: CangJieCallDiagnostic, onTarget: String) {
        if (AbstractTypeChecker.RUN_SLOW_ASSERTIONS) {
            throw AssertionError("$onTarget should not be called with ${diagnostic::class.java}")
        } else if (reportAdditionalErrors) {
            trace.report(
                NEW_INFERENCE_UNKNOWN_ERROR.on(
                    psiCangJieCall.psiCall.callElement,
                    diagnostic.candidateApplicability,
                    onTarget
                )
            )
        }
    }

    override fun onTypeArguments(diagnostic: CangJieCallDiagnostic) {
        val psiCallElement = psiCangJieCall.psiCall.callElement
        val reportElement =
            if (psiCallElement is CjCallExpression)
                psiCallElement.typeArgumentList ?: psiCallElement.calleeExpression ?: psiCallElement
            else
                psiCallElement

        when (diagnostic) {
            is TypeArgumentsCompilerError -> {
                trace.report(
                    COMPILER_AFFECTED_SYNTAX_ERROR_BY_MESSAGE.on(
                        reportElement,
                        "unable to infer generic argument of this function"
                    )
                )
            }

            is TypeArgumentsAfterEnumEntry -> {
                trace.report(
                    TYPE_ARGUMENTS_NOT_AFTER_ENUMENTRY.on(
                        reportElement,
                        diagnostic.enumEntry,
                        diagnostic.enum
                    )
                )
            }

            is WrongCountOfTypeArguments -> {
                val expectedTypeArgumentsCount = diagnostic.descriptor.typeParameters.size
                trace.report(
                    WRONG_NUMBER_OF_TYPE_ARGUMENTS.on(
                        reportElement,
                        expectedTypeArgumentsCount,
                        diagnostic.descriptor
                    )
                )
            }

            else -> {
                unknownError(diagnostic, "onTypeArguments")
            }
        }
    }

    override fun onCallName(diagnostic: CangJieCallDiagnostic) {

    }

    override fun onTypeArgument(typeArgument: TypeArgument, diagnostic: CangJieCallDiagnostic) {

    }

    override fun onCallReceiver(callReceiver: SimpleCangJieCallArgument, diagnostic: CangJieCallDiagnostic) {
        when (diagnostic) {
            is UnsafeCallError -> {
                val isForImplicitInvoke = when (callReceiver) {
                    is ReceiverExpressionCangJieCallArgument -> callReceiver.isForImplicitInvoke
                    else -> diagnostic.isForImplicitInvoke
                            || callReceiver.receiver.receiverValue.type.isExtensionFunctionType
                }

                tracingStrategy.unsafeCall(trace, callReceiver.receiver.receiverValue.type, isForImplicitInvoke)
            }

//            is SuperAsExtensionReceiver -> {
//                val psiExpression = callReceiver.psiExpression
//                if (psiExpression is CjSuperExpression) {
//                    trace.report(SUPER_CANT_BE_EXTENSION_RECEIVER.on(psiExpression, psiExpression.text))
//                }
//            }
//
//            is StubBuilderInferenceReceiver -> {
//                val stubType = callReceiver.receiver.receiverValue.type as? StubTypeForBuilderInference
//                val originalTypeParameter = stubType?.originalTypeVariable?.originalTypeParameter
//
//                trace.report(
//                    BUILDER_INFERENCE_STUB_RECEIVER.on(
//                        callReceiver.psiExpression ?: call.callElement,
//                        originalTypeParameter?.name ?: SpecialNames.NO_NAME_PROVIDED,
//                        originalTypeParameter?.containingDeclaration?.name ?: SpecialNames.NO_NAME_PROVIDED
//                    )
//                )
//            }
            else -> {
                unknownError(diagnostic, "onCallReceiver")
            }
        }
    }

    private fun reportSmartCast(smartCastDiagnostic: SmartCastDiagnostic) {
        val expressionArgument = smartCastDiagnostic.argument
        val smartCastResult = when (expressionArgument) {
            is ExpressionCangJieCallArgumentImpl -> {
                trace.markAsReported()
                val context = context.replaceDataFlowInfo(expressionArgument.dataFlowInfoBeforeThisArgument)
                val argumentExpression = CjPsiUtil.getLastElementDeparenthesized(
                    expressionArgument.valueArgument.getArgumentExpression(),
                    context.statementFilter
                )
                val dataFlowValue =
                    dataFlowValueFactory.createDataFlowValue(expressionArgument.receiver.receiverValue, context)
                val call = if (call.callElement is CjBinaryExpression) null else call
                if (!expressionArgument.valueArgument.isExternal()) {
                    smartCastManager.checkAndRecordPossibleCast(
                        dataFlowValue, smartCastDiagnostic.smartCastType, argumentExpression, context, call,
                        recordExpressionType = false
                    )
                } else null
            }

            is ReceiverExpressionCangJieCallArgument -> {
                trace.markAsReported()
                val receiverValue = expressionArgument.receiver.receiverValue
                val dataFlowValue = dataFlowValueFactory.createDataFlowValue(receiverValue, context)
                smartCastManager.checkAndRecordPossibleCast(
                    dataFlowValue,
                    smartCastDiagnostic.smartCastType,
                    (receiverValue as? ExpressionReceiver)?.expression,
                    context,
                    call,
                    recordExpressionType = true
                )
            }

            else -> null
        }
        val resolvedCall =
            smartCastDiagnostic.cangjieCall?.psiCangJieCall?.psiCall?.getResolvedCall(trace.bindingContext) as? NewResolvedCallImpl<*>
        if (resolvedCall != null && smartCastResult != null) {
            if (resolvedCall.extensionReceiver == expressionArgument.receiver.receiverValue) {
                resolvedCall.updateExtensionReceiverWithSmartCastIfNeeded(smartCastResult.resultType)
            }
            if (resolvedCall.dispatchReceiver == expressionArgument.receiver.receiverValue) {
                resolvedCall.setSmartCastDispatchReceiverType(smartCastResult.resultType)
            }
        }
    }

    private fun reportUnstableSmartCast(unstableSmartCast: UnstableSmartCast) {
        val dataFlowValue =
            dataFlowValueFactory.createDataFlowValue(unstableSmartCast.argument.receiver.receiverValue, context)
        val possibleTypes = unstableSmartCast.argument.receiver.typesFromSmartCasts
        val argumentExpression = unstableSmartCast.argument.psiExpression ?: return

        require(possibleTypes.isNotEmpty()) { "Receiver for unstable smart cast without possible types" }
        val intersectWrappedTypes = intersectWrappedTypes(possibleTypes)
        trace.record(
            BindingContext.UNSTABLE_SMARTCAST,
            argumentExpression,
            SingleSmartCast(null, intersectWrappedTypes)
        )
        trace.report(
            SMARTCAST_IMPOSSIBLE.on(
                argumentExpression,
                intersectWrappedTypes,
                argumentExpression.text,
                dataFlowValue.kind.description
            )
        )
    }

    override fun onCallArgument(callArgument: CangJieCallArgument, diagnostic: CangJieCallDiagnostic) {
        when (diagnostic) {
            is SmartCastDiagnostic -> reportSmartCast(diagnostic)
            is UnstableSmartCast -> reportUnstableSmartCast(diagnostic)
            is VisibilityErrorOnArgument -> {
                val invisibleMember = diagnostic.invisibleMember
                val argumentExpression =
                    diagnostic.argument.psiCallArgument.valueArgument.getArgumentExpression()
                        ?.lastBlockStatementOrThis()

                if (argumentExpression != null) {
                    trace.report(
                        INVISIBLE_MEMBER.on(
                            argumentExpression,
                            invisibleMember,
                            invisibleMember.visibility,
                            invisibleMember
                        )
                    )
                }
            }

            is TooManyArguments -> {
                trace.reportTrailingLambdaErrorOr(callArgument.psiExpression) { expr ->
                    TOO_MANY_ARGUMENTS.on(expr, diagnostic.descriptor)
                }

                trace.markAsReported()
            }

            is VarargArgumentOutsideParentheses -> trace.reportTrailingLambdaErrorOr(callArgument.psiExpression) { expr ->
                VARARG_OUTSIDE_PARENTHESES.on(expr)
            }

            is MissingNamedArgumentPrefix -> {
                trace.report(
                    NAMED_PARAMETER_PREFIX_MISSING.on(
                        callArgument.psiCallArgument.valueArgument.asElement(),
                        diagnostic.names
                    )
                )
            }

            is PositionalAfierNamedArgument -> {
                trace.report(POSITIONAL_ARGUMENT_AFTER_NAMED_ARGUMENT.on(callArgument.psiCallArgument.valueArgument.asElement()))
            }

            is MixingNamedAndPositionArguments -> {
                trace.report(MIXING_NAMED_AND_POSITIONED_ARGUMENTS.on(callArgument.psiCallArgument.valueArgument.asElement()))
            }

            is NoneCallableReferenceCallCandidates -> {
                val argument = diagnostic.argument
                val expression = (argument as? CallableReferenceCangJieCallArgumentImpl)?.cjCallableReferenceExpression
                if (expression != null) {
                    trace.report(UNRESOLVED_REFERENCE.on(expression.callableReference, expression.callableReference))
                }
            }

            is CallableReferenceCallCandidatesAmbiguity -> {
                val expression = when (val psiExpression = diagnostic.argument.psiExpression) {
                    is CjPsiUtil.CjExpressionWrapper -> psiExpression.baseExpression
                    else -> psiExpression
                } as? CjCallableReferenceExpression

                val candidates = diagnostic.candidates.map { it.candidate }
                if (expression != null) {
                    trace.reportDiagnosticOnce(
                        CALLABLE_REFERENCE_RESOLUTION_AMBIGUITY.on(
                            expression.callableReference,
                            candidates
                        )
                    )
                    trace.record(BindingContext.AMBIGUOUS_REFERENCE_TARGET, expression.callableReference, candidates)
                }
            }

//            is ArgumentNullabilityErrorDiagnostic -> reportNullabilityMismatchDiagnostic(callArgument, diagnostic)
//
//            is ArgumentNullabilityWarningDiagnostic -> reportNullabilityMismatchDiagnostic(callArgument, diagnostic)
//
//            is CallableReferencesDefaultArgumentUsed -> {
//                val callableReferenceExpression = diagnostic.argument.call.extractCallableReferenceExpression()
//
//                require(callableReferenceExpression != null) {
//                    "A call element must be callable reference for `CallableReferencesDefaultArgumentUsed`"
//                }
//
//                trace.report(
//                    UNSUPPORTED_FEATURE.on(
//                        callableReferenceExpression,
//                        LanguageFeature.FunctionReferenceWithDefaultValueAsOtherType to context.languageVersionSettings
//                    )
//                )
//            }

//            is ResolvedToSamWithVarargDiagnostic -> {
//                trace.report(TYPE_INFERENCE_CANDIDATE_WITH_SAM_AND_VARARG.on(callArgument.psiCallArgument.valueArgument.asElement()))
//            }

            is NotEnoughInformationForLambdaParameter -> {
                val lambdaArgument = diagnostic.lambdaArgument
                val parameterIndex = diagnostic.parameterIndex

                val valueArgument = lambdaArgument.psiCallArgument.valueArgument

                val valueParameters =
                    when (val argumentExpression = CjPsiUtil.deparenthesize(valueArgument.getArgumentExpression())) {
                        is CjLambdaExpression -> argumentExpression.valueParameters
                        is CjNamedFunction -> argumentExpression.valueParameters // for anonymous functions
                        else -> return
                    }

                val parameter = valueParameters.getOrNull(parameterIndex)
                if (parameter != null) {
                    trace.report(CANNOT_INFER_PARAMETER_TYPE.on(parameter))
                }
            }

            is CompatibilityWarningOnArgument -> {
                trace.report(
                    COMPATIBILITY_WARNING.on(
                        callArgument.psiCallArgument.valueArgument.asElement(),
                        diagnostic.candidate
                    )
                )
            }

//            is AdaptedCallableReferenceIsUsedWithReflection -> {
//                trace.report(
//                    ADAPTED_CALLABLE_REFERENCE_AGAINST_REFLECTION_TYPE.on(
//                        callArgument.psiCallArgument.valueArgument.asElement()
//                    )
//                )
//            }
//
//            is MultiLambdaBuilderInferenceRestriction -> {
//                val typeParameter = diagnostic.typeParameter as? TypeParameterDescriptor
//
//                trace.reportDiagnosticOnce(
//                    BUILDER_INFERENCE_MULTI_LAMBDA_RESTRICTION.on(
//                        callArgument.psiCallArgument.valueArgument.asElement(),
//                        typeParameter?.name ?: SpecialNames.NO_NAME_PROVIDED,
//                        typeParameter?.containingDeclaration?.name ?: SpecialNames.NO_NAME_PROVIDED,
//                    )
//                )
//            }
//
//            is NotCallableMemberReference, is NotCallableExpectedType -> {
//                // NotCallableMemberReference -> UNSUPPORTED is reported in DoubleColonExpressionResolver
//                // NotCallableExpectedType -> TYPE_MISMATCH is reported in reportConstraintErrorByPosition
//                return
//            }

            else -> {
                unknownError(diagnostic, "onCallArgument")
            }
        }
    }

    override fun onCallArgumentName(callArgument: CangJieCallArgument, diagnostic: CangJieCallDiagnostic) {
        val nameReference = callArgument.psiCallArgument.valueArgument.getArgumentName()?.referenceExpression ?: return
        when (diagnostic) {
            is NamedArgumentReference -> {
                trace.record(BindingContext.REFERENCE_TARGET, nameReference, diagnostic.parameterDescriptor)
                trace.markAsReported()
            }


            is NameForAmbiguousParameter -> trace.report(NAME_FOR_AMBIGUOUS_PARAMETER.on(nameReference))
            is NameNotFound -> trace.report(NAMED_PARAMETER_NOT_FOUND.on(nameReference, nameReference))

            is NamedArgumentNotAllowed -> trace.report(
                NAMED_ARGUMENTS_NOT_ALLOWED.on(
                    nameReference,
                    when (diagnostic.descriptor) {
//                        is FunctionInvokeDescriptor -> INVOKE_ON_FUNCTION_TYPE
//                        is DeserializedCallableMemberDescriptor -> INTEROP_FUNCTION
                        else -> BadNamedArgumentsTarget.NON_CANGJIE_FUNCTION
                    }
                )
            )

            is ArgumentPassedTwice -> trace.report(ARGUMENT_PASSED_TWICE.on(nameReference))
            else -> {
                unknownError(diagnostic, "onCallArgumentName")
            }
        }
    }

    override fun onCallArgumentSpread(callArgument: CangJieCallArgument, diagnostic: CangJieCallDiagnostic) {
//        when (diagnostic) {
//            is NonVarargSpread -> {
//                val castedPsiCallArgument = callArgument as? PSICangJieCallArgument
//                val castedCallArgument = callArgument as? ExpressionCangJieCallArgumentImpl
//
//                if (castedCallArgument != null) {
//                    val spreadElement = castedCallArgument.valueArgument.getSpreadElement()
//                    if (spreadElement != null) {
//                        trace.report(NON_VARARG_SPREAD.onError(spreadElement))
//                    }
//                } else if (castedPsiCallArgument != null) {
//                    val spreadElement = castedPsiCallArgument.valueArgument.getSpreadElement()
//                    if (spreadElement != null) {
//                        trace.report(NON_VARARG_SPREAD.on(context.languageVersionSettings, spreadElement))
//                    }
//                }
//            }
//            else -> {
//                unknownError(diagnostic, "onCallArgumentSpread")
//            }
//        }

    }

    private fun reportArgumentConstraintErrorByPosition(
        error: NewConstraintMismatch,
        argument: CangJieCallArgument,
        isWarning: Boolean,
        typeMismatchDiagnostic: DiagnosticFactory2<CjExpression, CangJieType, CangJieType>,
        selectorCall: CangJieCall?,
        report: (Diagnostic) -> Unit
    ) {
        if (argument is LambdaCangJieCallArgument) {
            val parameterTypes = argument.parametersTypes?.toList()
            if (parameterTypes != null) {
                val index = parameterTypes.indexOf(error.upperCangJieType.unwrap())
                val lambdaExpression = argument.psiExpression as? CjLambdaExpression
                val parameter = lambdaExpression?.valueParameters?.getOrNull(index)
                if (parameter != null) {
                    val diagnosticFactory =
                        if (isWarning) EXPECTED_PARAMETER_TYPE_MISMATCH_WARNING else EXPECTED_PARAMETER_TYPE_MISMATCH
                    report(diagnosticFactory.on(parameter, error.lowerCangJieType))
                    return
                }
            }
        }

        val expression = argument.psiExpression ?: run {
            val psiCall = (selectorCall as? PSICangJieCall)?.psiCall ?: psiCangJieCall.psiCall

            if (context.languageVersionSettings.supportsFeature(LanguageFeature.ProperTypeInferenceConstraintsProcessing) &&
                reportAdditionalErrors
            ) {
                report(
                    RECEIVER_TYPE_MISMATCH.on(
                        psiCall.calleeExpression ?: psiCall.callElement, error.upperCangJieType, error.lowerCangJieType
                    )
                )
            }
            return
        }

        val deparenthesized = CjPsiUtil.safeDeparenthesize(expression)
        if (reportConstantTypeMismatch(error, deparenthesized)) return

        val compileTimeConstant = trace[BindingContext.COMPILE_TIME_VALUE, deparenthesized] as? TypedCompileTimeConstant
        if (compileTimeConstant != null) {
            val expressionType = trace[BindingContext.EXPRESSION_TYPE_INFO, expression]?.type
            if (expressionType != null &&
                !UnsignedTypes.isUnsignedType(compileTimeConstant.type) && UnsignedTypes.isUnsignedType(expressionType)
            ) {

                return
            }
        }
        report(typeMismatchDiagnostic.on(deparenthesized, error.upperCangJieType, error.lowerCangJieType))
    }

    private fun reportConstantTypeMismatch(constraintError: NewConstraintMismatch, expression: CjExpression): Boolean {
        if (expression is CjConstantExpression) {
            val module = context.scope.ownerDescriptor.module
            val constantValue =
                constantExpressionEvaluator.evaluateToConstantValue(expression, trace, context.expectedType)
            val hasConstantTypeError = CompileTimeConstantChecker(context, module, true)
                .checkConstantExpressionType(constantValue, expression, constraintError.upperCangJieType)
            if (hasConstantTypeError) return true
        }
        return false
    }

    private fun reportCallableReferenceConstraintError(
        error: NewConstraintMismatch,
        rhsExpression: CjSimpleNameExpression
    ) {
        trace.report(TYPE_MISMATCH.on(rhsExpression, error.lowerCangJieType, error.upperCangJieType))
    }

    private fun reportConstraintErrorByPosition(error: NewConstraintMismatch, position: ConstraintPosition) {
        if (position is CallableReferenceConstraintPositionImpl) {
            val callableReferenceExpression = position.callableReferenceCall.call.extractCallableReferenceExpression()

            require(callableReferenceExpression != null) {
                "There should be the corresponding callable reference expression for `CallableReferenceConstraintPositionImpl`"
            }

            reportCallableReferenceConstraintError(error, callableReferenceExpression.callableReference)
            return
        }

        val isWarning = error is NewConstraintWarning
        val typeMismatchDiagnostic = if (isWarning) TYPE_MISMATCH_WARNING else TYPE_MISMATCH
        val report = if (isWarning) trace::reportDiagnosticOnce else trace::report

        when (position) {
            is ArgumentConstraintPosition<*> -> {
                reportArgumentConstraintErrorByPosition(
                    error, position.argument as CangJieCallArgument,
                    isWarning, typeMismatchDiagnostic,
                    selectorCall = null, report
                )
            }

            is ReceiverConstraintPosition<*> -> {
                reportArgumentConstraintErrorByPosition(
                    error, position.argument as CangJieCallArgument,
                    isWarning, typeMismatchDiagnostic,
                    selectorCall = (position as ReceiverConstraintPositionImpl).selectorCall, report
                )
            }

            is LambdaArgumentConstraintPosition<*> -> {
                reportArgumentConstraintErrorByPosition(
                    error, (position.lambda as ResolvedLambdaAtom).atom,
                    isWarning, typeMismatchDiagnostic,
                    selectorCall = null, report
                )
            }
//            is BuilderInferenceExpectedTypeConstraintPosition -> {
//                val inferredType =
//                    if (!error.lowerCangJieType.isNullableNothing()) error.lowerCangJieType
//                    else error.upperCangJieType.makeOptional()
//                trace.report(TYPE_MISMATCH.on(position.topLevelCall, error.upperCangJieType, inferredType))
//            }
            is ExpectedTypeConstraintPosition<*> -> {
                val call =
                    (position.topLevelCall as? CangJieCall)?.psiCangJieCall?.psiCall?.callElement as? CjExpression
                val inferredType =
                    if (!error.lowerCangJieType.isNothing()) error.lowerCangJieType
                    else error.upperCangJieType.makeOptional()
                if (call != null) {
                    report(typeMismatchDiagnostic.on(call, error.upperCangJieType, inferredType))
                }
            }

            is BuilderInferenceSubstitutionConstraintPosition<*> -> {
                reportConstraintErrorByPosition(error, position.initialConstraint.position)
            }

            is ExplicitTypeParameterConstraintPosition<*> -> {
                val typeArgumentReference =
                    (position.typeArgument as SimpleTypeArgumentImpl).typeProjection.typeReference ?: return
                val diagnosticFactory = if (isWarning) UPPER_BOUND_VIOLATED_WARNING else UPPER_BOUND_VIOLATED
                report(diagnosticFactory.on(typeArgumentReference, error.upperCangJieType, error.lowerCangJieType))
            }

            is FixVariableConstraintPosition<*> -> {
                val morePreciseDiagnosticExists = allDiagnostics.any { other ->
                    val otherError = other.constraintSystemError ?: return@any false
                    otherError is NewConstraintError && otherError.position.from !is FixVariableConstraintPositionImpl
                }
                if (morePreciseDiagnosticExists) return

                val call = ((position.resolvedAtom as? ResolvedAtom)?.atom as? PSICangJieCall)?.psiCall ?: call
                val expression = call.calleeExpression ?: return

                trace.reportDiagnosticOnce(
                    typeMismatchDiagnostic.on(
                        expression,
                        error.upperCangJieType,
                        error.lowerCangJieType
                    )
                )
            }

            BuilderInferencePosition -> {
                // some error reported later?
            }

            is DeclaredUpperBoundConstraintPosition<*> -> {
                val originalCall = (position as DeclaredUpperBoundConstraintPositionImpl).cangjieCall
                val typeParameterDescriptor = position.typeParameter
                val ownerDescriptor = typeParameterDescriptor.containingDeclaration
                if (reportAdditionalErrors) {
                    trace.reportDiagnosticOnce(
                        UPPER_BOUND_VIOLATION_IN_CONSTRAINT.on(
                            (originalCall as PSICangJieCall).psiCall.callElement,
                            typeParameterDescriptor.name,
                            ownerDescriptor.name,
                            error.upperCangJieType,
                            error.lowerCangJieType
                        )
                    )
                }
            }
//            is DelegatedPropertyConstraintPosition<*> -> {
//                // DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE, reported later
//            }
            is KnownTypeParameterConstraintPosition<*> -> {
                // UPPER_BOUND_VIOLATED, reported later?
            }

            is CallableReferenceConstraintPosition<*>,
            is IncorporationConstraintPosition,
            is InjectedAnotherStubTypeConstraintPosition<*>,
            is /*LHSArgumentConstraintPosition<*, *>,*/ SimpleConstraintSystemConstraintPosition/*, ProvideDelegateFixationPosition*/
                -> {
                if (AbstractTypeChecker.RUN_SLOW_ASSERTIONS) {
                    throw AssertionError("Constraint error in unexpected position: $position")
                } else if (reportAdditionalErrors) {
                    report(
                        TYPE_MISMATCH_IN_CONSTRAINT.on(
                            psiCangJieCall.psiCall.callElement,
                            error.upperCangJieType,
                            error.lowerCangJieType,
                            position
                        )
                    )
                }
            }


        }
    }

    private fun CangJieType.containsUninferredTypeParameter(uninferredTypeVariable: TypeVariableMarker) = contains {
        ErrorUtils.isUninferredTypeVariable(it) || it == TypeUtils.DONT_CARE
                || it.constructor == uninferredTypeVariable.freshTypeConstructor(typeSystemContext)
    }

    private fun getSubResolvedAtomsOfSpecialCallToReportUninferredTypeParameter(
        resolvedAtom: ResolvedAtom,
        uninferredTypeVariable: TypeVariableMarker
    ): Set<ResolvedAtom> =
        buildSet {
            for (subResolvedAtom in resolvedAtom.subResolvedAtoms ?: return@buildSet) {
                val atom = subResolvedAtom.atom
                val typeToCheck = when {
                    subResolvedAtom is PostponedResolvedAtom -> subResolvedAtom.expectedType ?: return@buildSet
                    atom is SimpleCangJieCallArgument -> atom.receiver.receiverValue.type
                    else -> return@buildSet
                }

                if (typeToCheck.containsUninferredTypeParameter(uninferredTypeVariable)) {
                    add(subResolvedAtom)
                }

                if (!subResolvedAtom.subResolvedAtoms.isNullOrEmpty()) {
                    addAll(
                        getSubResolvedAtomsOfSpecialCallToReportUninferredTypeParameter(
                            subResolvedAtom,
                            uninferredTypeVariable
                        )
                    )
                }
            }
        }


    private fun getArgumentsExpressionOrLastExpressionInBlock(atom: PSICangJieCallArgument): CjExpression? {
        val valueArgumentExpression = atom.valueArgument.getArgumentExpression()

        return if (valueArgumentExpression is CjBlockExpression) valueArgumentExpression.statements.lastOrNull() else valueArgumentExpression
    }

    private fun reportNotEnoughInformationForTypeParameterForSpecialCall(
        resolvedAtom: ResolvedCallAtom,
        error: NotEnoughInformationForTypeParameterImpl
    ) {
        val subResolvedAtomsToReportError =
            getSubResolvedAtomsOfSpecialCallToReportUninferredTypeParameter(resolvedAtom, error.typeVariable)

        if (subResolvedAtomsToReportError.isEmpty()) return

        for (subResolvedAtom in subResolvedAtomsToReportError) {
            val atom = subResolvedAtom.atom as? PSICangJieCallArgument ?: continue
            val argumentsExpression = getArgumentsExpressionOrLastExpressionInBlock(atom)

            if (argumentsExpression != null) {
                val specialFunctionName = requireNotNull(
                    ControlStructureTypingUtils.ResolveConstruct.entries.find { specialFunction ->
                        specialFunction.specialFunctionName == resolvedAtom.candidateDescriptor.name
                    }
                ) { "Unsupported special construct: ${resolvedAtom.candidateDescriptor.name} not found in special construct names" }

                trace.reportDiagnosticOnce(
                    NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER.on(
                        argumentsExpression, " for subcalls of ${specialFunctionName.name} expression", null
                    )
                )
            }
        }
    }

    @OptIn(ExperimentalContracts::class)
    private fun isSpecialFunction(atom: ResolvedAtom): Boolean {
        contract {
            returns(true) implies (atom is ResolvedCallAtom)
        }
        if (atom !is ResolvedCallAtom) return false

        return ControlStructureTypingUtils.ResolveConstruct.entries.any { specialFunction ->
            specialFunction.specialFunctionName == atom.candidateDescriptor.name
        }
    }

    override fun constraintError(error: ConstraintSystemError) {
        when (error) {
            is NewConstraintMismatch -> reportConstraintErrorByPosition(error, error.position.from)

            is CapturedTypeFromSubtyping -> {
                val position = error.position
                val argumentPosition: ArgumentConstraintPositionImpl? =
                    position as? ArgumentConstraintPositionImpl
                        ?: (position as? IncorporationConstraintPosition)?.from as? ArgumentConstraintPositionImpl

                argumentPosition?.let {
                    val expression = it.argument.psiExpression ?: return
                    trace.reportDiagnosticOnce(
                        NEW_INFERENCE_ERROR.on(
                            expression,
                            "Capture type from subtyping ${error.constraintType} for variable ${error.typeVariable}"
                        )
                    )
                }
            }


            is MultipleMinimalCommonSupertypes -> {
                val psiCall = psiCangJieCall.psiCall
                val expression = if (psiCall is CallTransformer.CallForImplicitInvoke) {
                    psiCall.outerCall.calleeExpression
                } else {
                    psiCall.calleeExpression?.takeIf { it.isPhysical } ?: psiCall.callElement
                } ?: return

                trace.reportDiagnosticOnce(
                    TYPE_MISMATCH_MULTIPLE_SUPERTYPES.on(
                        expression,
                        error.candidates
                    )
                )
            }

            is InferredIntoDeclaredUpperBounds -> {
                val psiCall = psiCangJieCall.psiCall
                val expression = if (psiCall is CallTransformer.CallForImplicitInvoke) {
                    psiCall.outerCall.calleeExpression
                } else {
                    psiCall.calleeExpression?.takeIf { it.isPhysical } ?: psiCall.callElement
                } ?: return
                val typeVariable = error.typeVariable as? TypeVariableFromCallableDescriptor ?: return

                trace.reportDiagnosticOnce(
                    INFERRED_INTO_DECLARED_UPPER_BOUNDS.on(
                        expression,
                        typeVariable.originalTypeParameter.name.asString()
                    )
                )
            }

            is NotEnoughInformationForTypeParameterImpl -> {
                val resolvedAtom = error.resolvedAtom
                val isDiagnosticRedundant = !isSpecialFunction(resolvedAtom) && allDiagnostics.any {
                    when (it) {
                        is WrongCountOfTypeArguments -> true
                        is CangJieConstraintSystemDiagnostic -> {
                            val otherError = it.error
                            (otherError is ConstrainingTypeIsError && otherError.typeVariable == error.typeVariable)
                                    || otherError is NewConstraintError
                        }

                        else -> false
                    }
                }

                if (isDiagnosticRedundant) return
                val expression = when (val atom = error.resolvedAtom.atom) {
                    is PSICangJieCall -> {
                        val psiCall = atom.psiCall
                        if (psiCall is CallTransformer.CallForImplicitInvoke) {
                            psiCall.outerCall.calleeExpression
                        } else {
                            psiCall.calleeExpression
                        }
                    }

                    is PSICangJieCallArgument -> atom.valueArgument.getArgumentExpression()
                    else -> call.calleeExpression
                } ?: return

                if (isSpecialFunction(resolvedAtom)) {
                    // We locally report errors on some arguments of special calls, on which the error may not be reported directly
                    reportNotEnoughInformationForTypeParameterForSpecialCall(resolvedAtom, error)
                } else {

                    val (typeVariable, typeVariableName) = when (val typeVariable = error.typeVariable) {
                        is TypeVariableFromCallableDescriptor -> typeVariable.originalTypeParameter to typeVariable.originalTypeParameter.name.asString()
                        is TypeVariableForLambdaReturnType -> null to "return type of lambda"
                        else -> error("Unsupported type variable: $typeVariable")
                    }
                    val unwrappedExpression = if (expression is CjBlockExpression) {
                        expression.statements.lastOrNull() ?: expression
                    } else expression

//                    val diagnostic = if (error.couldBeResolvedWithUnrestrictedBuilderInference) {
//                        COULD_BE_INFERRED_ONLY_WITH_UNRESTRICTED_BUILDER_INFERENCE
//                    } else {
//                        NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER
//                    }
                    val diagnostic = NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER
                    if (unwrappedExpression is CjCollectionLiteralExpression && diagnostic == NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER) {
//                        数组字面量 替换为ARRAY_LITERAL_TYPE_INFERENCE_FAILED
                        trace.reportDiagnosticOnce(
                            ARRAY_LITERAL_TYPE_INFERENCE_FAILED.on(
                                unwrappedExpression
                            )
                        )
                    } else {

                        trace.reportDiagnosticOnce(diagnostic.on(unwrappedExpression, typeVariableName,typeVariable?.containingDeclaration))

                    }
                }
            }

            is OnlyInputTypesDiagnostic -> {
                val typeVariable = error.typeVariable as? TypeVariableFromCallableDescriptor ?: return
                psiCangJieCall.psiCall.calleeExpression?.let {
                    trace.report(
                        TYPE_INFERENCE_ONLY_INPUT_TYPES.on(
                            context.languageVersionSettings,
                            it,
                            typeVariable.originalTypeParameter
                        )
                    )
                }
            }
//
            is InferredEmptyIntersectionError, is InferredEmptyIntersectionWarning -> {
                val typeVariable = (error as InferredEmptyIntersection).typeVariable
                psiCangJieCall.psiCall.calleeExpression?.let { expression ->
                    val typeVariableText =
                        (typeVariable as? TypeVariableFromCallableDescriptor)?.originalTypeParameter?.name?.asString()
                            ?: typeVariable.toString()

                    @Suppress("UNCHECKED_CAST")
                    val incompatibleTypes = error.incompatibleTypes as List<CangJieType>

                    @Suppress("UNCHECKED_CAST")
                    val causingTypes = error.causingTypes as List<CangJieType>
                    val causingTypesText =
                        if (incompatibleTypes == causingTypes) "" else ": ${causingTypes.joinToString()}"
                    val diagnostic = if (error.kind.isDefinitelyEmpty) {
                        INFERRED_TYPE_VARIABLE_INTO_EMPTY_INTERSECTION.on(
                            context.languageVersionSettings, expression, typeVariableText,
                            incompatibleTypes, error.kind.description, causingTypesText
                        )
                    } else {
                        INFERRED_TYPE_VARIABLE_INTO_POSSIBLE_EMPTY_INTERSECTION.on(
                            expression, typeVariableText,
                            incompatibleTypes, error.kind.description, causingTypesText
                        )
                    }

                    trace.reportDiagnosticOnce(diagnostic)
                }
            }
            // ConstrainingTypeIsError means that some type isError, so it's reported somewhere else
            is ConstrainingTypeIsError -> {}
//            // LowerPriorityToPreserveCompatibility is not expected to report something
            is LowerPriorityToPreserveCompatibility -> {}

            is NoSuccessfulFork -> shouldNotBeCalled()

//            is  MultiLambdaBuilderInferenceRestriction<*> -> shouldNotBeCalled()
//            // NotEnoughInformationForTypeParameterImpl is already considered above
            is NotEnoughInformationForTypeParameter<*> -> {
                throw AssertionError("constraintError should not be called with ${error::class.java}")
            }


        }

    }
}

val NewConstraintMismatch.upperCangJieType get() = upperType as CangJieType
val NewConstraintMismatch.lowerCangJieType get() = lowerType as CangJieType
