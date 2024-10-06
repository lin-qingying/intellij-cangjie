package com.huawei.cangjie.resolve.calls.tower

//import com.huawei.cangjie.resolve.calls.smartcasts.SmartCastManager

import com.huawei.cangjie.builtins.CangJieBuiltIns
import com.huawei.cangjie.builtins.toFunctionType
import com.huawei.cangjie.descriptors.*
import com.huawei.cangjie.diagnostics.Errors
import com.huawei.cangjie.extensions.internal.CandidateInterceptor
import com.huawei.cangjie.psi.*
import com.huawei.cangjie.resolve.BindingContext
import com.huawei.cangjie.resolve.BindingContextUtils.updateRecordedType
import com.huawei.cangjie.resolve.MissingSupertypesResolver
import com.huawei.cangjie.resolve.StatementFilter
import com.huawei.cangjie.resolve.calls.ArgumentTypeResolver
import com.huawei.cangjie.resolve.calls.DiagnosticReporterByTrackingStrategy
import com.huawei.cangjie.resolve.calls.checkers.AdditionalTypeChecker
import com.huawei.cangjie.resolve.calls.checkers.CallChecker
import com.huawei.cangjie.resolve.calls.checkers.CallCheckerContext
import com.huawei.cangjie.resolve.calls.components.AdditionalDiagnosticReporter
import com.huawei.cangjie.resolve.calls.components.isArrayType
import com.huawei.cangjie.resolve.calls.components.isVararg
import com.huawei.cangjie.resolve.calls.context.BasicCallResolutionContext
import com.huawei.cangjie.resolve.calls.context.CallPosition
import com.huawei.cangjie.resolve.calls.inference.buildResultingSubstitutor
import com.huawei.cangjie.resolve.calls.inference.components.NewTypeSubstitutor
import com.huawei.cangjie.resolve.calls.model.*
import com.huawei.cangjie.resolve.calls.smartcasts.DataFlowValueFactory
import com.huawei.cangjie.resolve.calls.smartcasts.SmartCastManager
import com.huawei.cangjie.resolve.calls.tasks.TracingStrategy
import com.huawei.cangjie.resolve.calls.util.getEffectiveExpectedType
import com.huawei.cangjie.resolve.calls.util.getResolvedCall
import com.huawei.cangjie.resolve.calls.util.isFakeElement
import com.huawei.cangjie.resolve.constants.CompileTimeConstant
import com.huawei.cangjie.resolve.constants.IntegerLiteralTypeConstructor
import com.huawei.cangjie.resolve.constants.evaluate.ConstantExpressionEvaluator
import com.huawei.cangjie.resolve.deprecation.DeprecationResolver
import com.huawei.cangjie.resolve.getLastStatementInABlock
import com.huawei.cangjie.types.*
import com.huawei.cangjie.types.error.ErrorScopeKind
import com.huawei.cangjie.types.expressions.DataFlowAnalyzer
import com.huawei.cangjie.types.expressions.ExpressionTypingServices
import com.huawei.cangjie.types.expressions.ExpressionTypingUtils
import com.huawei.cangjie.types.model.TypeSystemInferenceExtensionContextDelegate
import com.huawei.cangjie.types.util.TypeUtils
import com.huawei.cangjie.types.util.TypeUtils.NO_EXPECTED_TYPE

class CangJieToResolvedCallTransformer(


    private val callCheckers: Iterable<CallChecker>,
    private val additionalTypeCheckers: Iterable<AdditionalTypeChecker>,
    private val dataFlowAnalyzer: DataFlowAnalyzer,
    private val argumentTypeResolver: ArgumentTypeResolver,
    private val constantExpressionEvaluator: ConstantExpressionEvaluator,
    private val deprecationResolver: DeprecationResolver,
    private val expressionTypingServices: ExpressionTypingServices,

    private val additionalDiagnosticReporter: AdditionalDiagnosticReporter,
    private val moduleDescriptor: ModuleDescriptor,
    private val dataFlowValueFactory: DataFlowValueFactory,
    private val builtIns: CangJieBuiltIns,
    private val typeSystemContext: TypeSystemInferenceExtensionContextDelegate,
    private val smartCastManager: SmartCastManager,
    private val typeApproximator: TypeApproximator,
    private val missingSupertypesResolver: MissingSupertypesResolver,
    private val candidateInterceptor: CandidateInterceptor,
    private val callComponents: CangJieCallComponents,
) {

    companion object {
        private val REPORT_MISSING_NEW_INFERENCE_DIAGNOSTIC
            get() = false

        fun keyForPartiallyResolvedCall(resolvedCallAtom: ResolvedCallAtom): Call {
            val psiCangJieCall = resolvedCallAtom.atom.psiCangJieCall
            return if (psiCangJieCall is PSICangJieCallForInvoke)
                psiCangJieCall.baseCall.psiCall
            else
                psiCangJieCall.psiCall
        }
    }

    // todo very beginning code
    fun runArgumentsChecks(context: BasicCallResolutionContext, resolvedCall: NewAbstractResolvedCall<*>) {
        if (resolvedCall !is NewResolvedCallImpl<*>) return

        for (valueArgument in resolvedCall.call.valueArguments) {
            val argumentMapping = resolvedCall.getArgumentMapping(valueArgument!!)
            val parameter: ValueParameterDescriptor?
            val (expectedType, callPosition) = when (argumentMapping) {
                is ArgumentMatch -> {
                    parameter = argumentMapping.valueParameter

                    // We should take expected type from the last used conversion
                    // TODO: move this logic into ParameterTypeConversion
                    val expectedType =
                        resolvedCall.getExpectedTypeForUnitConvertedArgument(valueArgument)
                            ?: resolvedCall.getExpectedTypeForSuspendConvertedArgument(valueArgument)
                            ?: resolvedCall.getExpectedTypeForSamConvertedArgument(valueArgument)
                            ?: getEffectiveExpectedType(argumentMapping.valueParameter, valueArgument, context)
                    Pair(
                        expectedType,
                        CallPosition.ValueArgumentPosition(resolvedCall, argumentMapping.valueParameter, valueArgument),
                    )
                }

                else -> {
                    parameter = null
                    Pair(NO_EXPECTED_TYPE, CallPosition.Unknown)
                }
            }
            val newContext =
                context.replaceDataFlowInfo(resolvedCall.dataFlowInfoForArguments.getInfo(valueArgument))

                    .replaceCallPosition(callPosition)
                    .replaceExpectedType(
                        if (context.expectedType != NO_EXPECTED_TYPE) {
                            if (CangJieBuiltIns.isArray(context.expectedType)) {
                                context.expectedType.arguments[0].type

                            } else {
                                expectedType
                            }
                        } else

                        if (parameter?.isVararg == true &&   !resolvedCall.cangjieCall.argumentsInParenthesis.any { it.isArrayType() }) {
                                parameter.varargElementType

                            } else {
                                expectedType
                            }
                    )


            val constantConvertedArgument = resolvedCall.getArgumentTypeForConstantConvertedArgument(valueArgument)
            val argumentExpression = valueArgument.getArgumentExpression() ?: continue

            if (constantConvertedArgument != null) {
                context.trace.record(BindingContext.COMPILE_TIME_VALUE, argumentExpression, constantConvertedArgument)
                updateRecordedType(
                    constantConvertedArgument.unknownIntegerType, argumentExpression, context.trace, false
                )
            }


            if (!valueArgument.isExternal()) {
                updateRecordedType(
                    argumentExpression,
                    parameter,
                    newContext,
                    constantConvertedArgument?.unknownIntegerType?.unwrap(),
                    resolvedCall.isReallySuccess()
                )
            }
        }
    }

    fun getResolvedCallForArgumentExpression(expression: CjExpression, context: BasicCallResolutionContext) =
        if (!ExpressionTypingUtils.dependsOnExpectedType(expression))
            null
        else
            expression.getResolvedCall(context.trace.bindingContext) as? NewAbstractResolvedCall<*>

    fun updateRecordedType(
        expression: CjExpression,
        parameter: ValueParameterDescriptor?,
        context: BasicCallResolutionContext,
        convertedArgumentType: UnwrappedType?,
        reportErrorForTypeMismatch: Boolean,
    ): CangJieType? {
        val deparenthesized = expression.let {
            CjPsiUtil.getLastElementDeparenthesized(it, context.statementFilter)
        } ?: return null

        val recordedType = context.trace.getType(deparenthesized)
        val recordedTypeForParenthesized = context.trace.getType(expression)

        var updatedType = convertedArgumentType ?: getResolvedCallForArgumentExpression(deparenthesized, context)?.run {
            when {
                resultingDescriptor is FunctionDescriptor
                        && deparenthesized is CjNameReferenceExpression -> (resultingDescriptor as FunctionDescriptor).toFunctionType()

                else -> resultingDescriptor.returnType
            }

//            resultingDescriptor.returnType
        }

        // For the cases like 'foo(1)' the type of '1' depends on expected type (it can be Int, Byte, etc.),
        // so while the expected type is not known, it's IntegerValueType(1), and should be updated when the expected type is known.
        if (recordedType != null && !recordedType.constructor.isDenotable) {
            updatedType =
                argumentTypeResolver.updateResultArgumentTypeIfNotDenotable(context, deparenthesized) ?: updatedType
        }

        var reportErrorDuringTypeCheck = reportErrorForTypeMismatch

        if (parameter != null /*&& isEnabledFor(parameter, context.languageVersionSettings)*/) {
            val argumentCompileTimeValue = context.trace[BindingContext.COMPILE_TIME_VALUE, deparenthesized]
            if (argumentCompileTimeValue != null && argumentCompileTimeValue.parameters.isConvertableConstVal) {
                val generalNumberType = createTypeForConvertableConstant(argumentCompileTimeValue)
                if (generalNumberType != null) {
                    updatedType = argumentTypeResolver.updateResultArgumentTypeIfNotDenotable(
                        context.trace, context.statementFilter, context.expectedType, generalNumberType, expression,
                    )
                    reportErrorDuringTypeCheck = true
                }

            }
        } else if (convertedArgumentType != null) {
            context.trace.report(Errors.SIGNED_CONSTANT_CONVERTED_TO_UNSIGNED.on(deparenthesized))
        }

        updatedType =
            updateRecordedTypeForArgument(updatedType, recordedType, recordedTypeForParenthesized, expression, context)

        dataFlowAnalyzer.checkType(updatedType, deparenthesized, context, reportErrorDuringTypeCheck)

        return updatedType
    }


    // See CallCompleter#updateRecordedTypeForArgument
    private fun updateRecordedTypeForArgument(
        updatedType: CangJieType?,
        recordedType: CangJieType?,
        recordedTypeForParenthesized: CangJieType?,
        argumentExpression: CjExpression,
        context: BasicCallResolutionContext,
    ): CangJieType? {
        if ((!ErrorUtils.containsErrorType(recordedType) && recordedType == updatedType && recordedType == recordedTypeForParenthesized) || updatedType == null)
            return updatedType

        val expressions = ArrayList<CjExpression>().also { expressions ->
            var expression: CjExpression? = argumentExpression
            while (expression != null) {
                expressions.add(expression)
                expression = deparenthesizeOrGetSelector(expression, context.statementFilter)
            }
            expressions.reverse()
        }

        var shouldBeMadeNullable: Boolean = false
        for (expression in expressions) {
            if (!(expression is CjParenthesizedExpression)) {
                shouldBeMadeNullable = hasNecessarySafeCall(expression, context.trace)
            }
            updateRecordedType(updatedType, expression, context.trace, shouldBeMadeNullable)
        }

        return context.trace.getType(argumentExpression)
    }

    private fun hasNecessarySafeCall(expression: CjExpression, trace: BindingTrace): Boolean {
        // We are interested in type of the last call:
        // 'a.b?.foo()' is safe call, but 'a?.b.foo()' is not.
        // Since receiver is 'a.b' and selector is 'foo()',
        // we can only check if an expression is safe call.
        if (expression !is CjSafeQualifiedExpression) return false

        //If a receiver type is not null, then this safe expression is useless, and we don't need to make the result type nullable.
        val expressionType = trace.getType(expression.receiverExpression)
        return expressionType != null && TypeUtils.isNullableType(expressionType)
    }

    private fun deparenthesizeOrGetSelector(expression: CjExpression, statementFilter: StatementFilter): CjExpression? {
        val deparenthesized = CjPsiUtil.deparenthesizeOnce(expression)
        if (deparenthesized != expression) return deparenthesized

        return when (expression) {
            is CjBlockExpression -> statementFilter.getLastStatementInABlock(expression)
            is CjQualifiedExpression -> expression.selectorExpression
            else -> null
        }
    }

    private fun createTypeForConvertableConstant(constant: CompileTimeConstant<*>): SimpleType? {
        val value = (constant.getValue(NO_EXPECTED_TYPE) as? Number)?.toLong() ?: return null
        val typeConstructor = IntegerLiteralTypeConstructor(value, moduleDescriptor, constant.parameters)
        return CangJieTypeFactory.simpleTypeWithNonTrivialMemberScope(
            TypeAttributes.Empty, typeConstructor, emptyList(), false,
            ErrorUtils.createErrorScope(
                ErrorScopeKind.INTEGER_LITERAL_TYPE_SCOPE,
                throwExceptions = true,
                typeConstructor.toString()
            ),
        )
    }

    fun reportCallDiagnostic(
        context: BasicCallResolutionContext,
        trace: BindingTrace,
        resolvedCall: NewAbstractResolvedCall<*>,
        resultingDescriptor: CallableDescriptor,
        diagnostics: Collection<CangJieCallDiagnostic>,
    ) {
        val trackingTrace = TrackingBindingTrace(trace)
        val newContext = context.replaceBindingTrace(trackingTrace)

        val diagnosticHolder = CangJieDiagnosticsHolder.SimpleHolder()
        val resolvedCallAtom = resolvedCall.resolvedCallAtom

        if (resolvedCallAtom != null) {
            additionalDiagnosticReporter.reportAdditionalDiagnostics(
                resolvedCallAtom,
                resultingDescriptor,
                diagnosticHolder,
                diagnostics
            )
        }

        val allDiagnostics = diagnostics + diagnosticHolder.getDiagnostics()

        val diagnosticReporter = DiagnosticReporterByTrackingStrategy(
            constantExpressionEvaluator,
            newContext,
            resolvedCall.psiCangJieCall,
            context.dataFlowValueFactory,
            allDiagnostics,
            smartCastManager,
            typeSystemContext
        )

        for (diagnostic in allDiagnostics) {
            trackingTrace.reported = false
            diagnostic.report(diagnosticReporter)

            if (diagnostic is ResolvedUsingDeprecatedVisibility) {
                reportResolvedUsingDeprecatedVisibility(
                    resolvedCall.psiCangJieCall.psiCall,
                    resolvedCall.candidateDescriptor,
                    resultingDescriptor,
                    diagnostic,
                    trace,
                )
            }

            val dontRecordToTraceAsIs = diagnostic is ResolutionDiagnostic && diagnostic !is VisibilityError
            val shouldReportMissingDiagnostic = !trackingTrace.reported && !dontRecordToTraceAsIs
            if (shouldReportMissingDiagnostic && REPORT_MISSING_NEW_INFERENCE_DIAGNOSTIC) {
                val factory =
                    if (diagnostic.candidateApplicability.isSuccess) Errors.NEW_INFERENCE_DIAGNOSTIC else Errors.NEW_INFERENCE_ERROR
                trace.report(
                    factory.on(
                        diagnosticReporter.psiCangJieCall.psiCall.callElement,
                        "Missing diagnostic: $diagnostic"
                    )
                )
            }
        }
    }

    fun reportDiagnostics(
        context: BasicCallResolutionContext,
        trace: BindingTrace,
        resolvedCall: NewAbstractResolvedCall<*>,
        diagnostics: Collection<CangJieCallDiagnostic>,
    ) {
        when (resolvedCall) {
            is NewVariableAsFunctionResolvedCallImpl -> {
                val variableCall = resolvedCall.variableCall
                val functionCall = resolvedCall.functionCall

                reportCallDiagnostic(context, trace, variableCall, variableCall.resultingDescriptor, diagnostics)
                reportCallDiagnostic(context, trace, functionCall, functionCall.resultingDescriptor, emptyList())
            }

            else -> {
                val resolvedCallAtom = resolvedCall.resolvedCallAtom
                if (resolvedCallAtom != null) {
                    reportCallDiagnostic(context, trace, resolvedCall, resolvedCall.resultingDescriptor, diagnostics)
                }
            }
        }
    }

    fun <D : CallableDescriptor> onlyTransform(
        resolvedCallAtom: ResolvedCallAtom,
        diagnostics: Collection<CangJieCallDiagnostic>,
    ): NewAbstractResolvedCall<D> = transformToResolvedCall(resolvedCallAtom, null, null, diagnostics)

    private fun bind(trace: BindingTrace, simpleResolvedCall: NewAbstractResolvedCall<*>) {
        val tracing = simpleResolvedCall.psiCangJieCall.tracingStrategy

        tracing.bindReference(trace, simpleResolvedCall)
        tracing.bindResolvedCall(trace, simpleResolvedCall)
    }

    private fun bind(trace: BindingTrace, variableAsFunction: NewVariableAsFunctionResolvedCallImpl) {
        val outerTracingStrategy = variableAsFunction.baseCall.tracingStrategy
        val variableCall = variableAsFunction.variableCall
        val functionCall = variableAsFunction.functionCall

        outerTracingStrategy.bindReference(trace, variableCall)
        outerTracingStrategy.bindResolvedCall(trace, variableAsFunction)
        functionCall.psiCangJieCall.tracingStrategy.bindReference(trace, functionCall)
    }

    internal fun bind(trace: BindingTrace, resolvedCall: ResolvedCall<*>) {
        (resolvedCall as? NewAbstractResolvedCall<*>)?.let { bind(trace, it) }
        (resolvedCall as? NewVariableAsFunctionResolvedCallImpl)?.let { bind(trace, it) }
    }


    fun runCallCheckers(resolvedCall: ResolvedCall<*>, callCheckerContext: CallCheckerContext) {
        val calleeExpression = if (resolvedCall is VariableAsFunctionResolvedCall)
            resolvedCall.variableCall.call.calleeExpression
        else
            resolvedCall.call.calleeExpression
        val reportOn =
            if (calleeExpression != null && !calleeExpression.isFakeElement) calleeExpression
            else resolvedCall.call.callElement

        for (callChecker in callCheckers) {
            callChecker.check(resolvedCall, reportOn, callCheckerContext)

            if (resolvedCall is VariableAsFunctionResolvedCall) {
                callChecker.check(resolvedCall.variableCall, reportOn, callCheckerContext)
            }
        }
    }

    fun <D : CallableDescriptor> transformToResolvedCall(
        completedCallAtom: ResolvedCallAtom,
        trace: BindingTrace?,
        resultSubstitutor: NewTypeSubstitutor? = null, // if substitutor is not null, it means that this call is completed
        diagnostics: Collection<CangJieCallDiagnostic>,
    ): NewAbstractResolvedCall<D> {
        val psiCangJieCall = completedCallAtom.atom.psiCangJieCall

        completedCallAtom.setCandidateDescriptor(
            candidateInterceptor.interceptResolvedCallAtomCandidate(
                completedCallAtom.candidateDescriptor,
                completedCallAtom,
                trace,
                resultSubstitutor,
                diagnostics
            )
        )

        return if (psiCangJieCall is PSICangJieCallForInvoke) {
            val diagnosticsForVariableCall =
                if (completedCallAtom.candidateDescriptor is FunctionDescriptor) emptyList() else diagnostics
            val diagnosticsForFunctionCall =
                if (completedCallAtom.candidateDescriptor is FunctionDescriptor) diagnostics else emptyList()

            @Suppress("UNCHECKED_CAST")
            NewVariableAsFunctionResolvedCallImpl(
                createOrGet(
                    psiCangJieCall.variableCall.resolvedCall,
                    trace,
                    resultSubstitutor,
                    diagnosticsForVariableCall
                ),
                createOrGet(completedCallAtom, trace, resultSubstitutor, diagnosticsForFunctionCall),
            ) as NewAbstractResolvedCall<D>
        } else {
            createOrGet(completedCallAtom, trace, resultSubstitutor, diagnostics)
        }
    }

    private fun <D : CallableDescriptor> createOrGet(
        completedSimpleAtom: ResolvedCallAtom,
        trace: BindingTrace?,
        resultSubstitutor: NewTypeSubstitutor?,
        diagnostics: Collection<CangJieCallDiagnostic>,
    ): NewAbstractResolvedCall<D> {
        if (trace != null) {
            val storedResolvedCall = completedSimpleAtom.atom.psiCangJieCall.getResolvedPsiCangJieCall<D>(trace)
            if (storedResolvedCall != null) {
                storedResolvedCall.setResultingSubstitutor(resultSubstitutor)
                storedResolvedCall.updateDiagnostics(diagnostics)
                return storedResolvedCall
            }
        }
        return NewResolvedCallImpl(
            completedSimpleAtom, resultSubstitutor, diagnostics,
            typeApproximator, expressionTypingServices.languageVersionSettings
        )
//        return if (completedSimpleAtom.atom.callKind == CangJieCallKind.CALLABLE_REFERENCE) {
//            NewCallableReferenceResolvedCall(
//                completedSimpleAtom as ResolvedCallableReferenceCallAtom,
////                typeApproximator,
////                expressionTypingServices.languageVersionSettings,
//                resultSubstitutor
//            )
//        } else {
//            NewResolvedCallImpl(
//                completedSimpleAtom, resultSubstitutor, diagnostics,
////                typeApproximator, expressionTypingServices.languageVersionSettings
//            )
//        }
    }

    fun <D : CallableDescriptor> createStubResolvedCallAndWriteItToTrace(
        candidate: ResolvedCallAtom,
        trace: BindingTrace,
        diagnostics: Collection<CangJieCallDiagnostic>,
        substitutor: NewTypeSubstitutor?,
    ): NewAbstractResolvedCall<D> {
        val result = transformToResolvedCall<D>(candidate, trace, substitutor, diagnostics)
        val psiCangJieCall = candidate.atom.psiCangJieCall
        val tracing =
            (psiCangJieCall as? PSICangJieCallForInvoke)?.baseCall?.tracingStrategy ?: psiCangJieCall.tracingStrategy

        tracing.bindReference(trace, result)
        tracing.bindResolvedCall(trace, result)

        return result
    }

    private fun forwardCallToInferenceSession(
        baseResolvedCall: CallResolutionResult,
        context: BasicCallResolutionContext,
        resolvedCall: NewAbstractResolvedCall<*>,
        tracingStrategy: TracingStrategy,
    ) {
        if (baseResolvedCall is CompletedCallResolutionResult) {
            context.inferenceSession.addCompletedCallInfo(
                PSICompletedCallInfo(
                    baseResolvedCall,
                    context,
                    resolvedCall,
                    tracingStrategy
                )
            )
        }
    }

    fun <D : CallableDescriptor> transformAndReport(
        baseResolvedCall: CallResolutionResult,
        context: BasicCallResolutionContext,
        tracingStrategy: TracingStrategy,
    ): NewAbstractResolvedCall<D> {
        return when (baseResolvedCall) {
            is PartialCallResolutionResult -> {
                val candidate = baseResolvedCall.resultCallAtom

                val psiCall = keyForPartiallyResolvedCall(candidate)

                context.trace.record(BindingContext.ONLY_RESOLVED_CALL, psiCall, PartialCallContainer(baseResolvedCall))
                context.trace.record(BindingContext.PARTIAL_CALL_RESOLUTION_CONTEXT, psiCall, context)

                if (baseResolvedCall.forwardToInferenceSession) {
                    context.inferenceSession.addPartialCallInfo(
                        PSIPartialCallInfo(baseResolvedCall, context, tracingStrategy),
                    )
                }

                createStubResolvedCallAndWriteItToTrace<D>(
                    candidate,
                    context.trace,
                    baseResolvedCall.diagnostics,
                    substitutor = null
                )
            }

            is CompletedCallResolutionResult, is ErrorCallResolutionResult -> {

                val candidate = (baseResolvedCall as SingleCallResolutionResult).resultCallAtom

                val resultSubstitutor =
                    baseResolvedCall.constraintSystem.getBuilder().currentStorage()
                        .buildResultingSubstitutor(typeSystemContext)
                if (context.inferenceSession.writeOnlyStubs(baseResolvedCall)) {
                    val stub = createStubResolvedCallAndWriteItToTrace<CallableDescriptor>(
                        candidate,
                        context.trace,
                        baseResolvedCall.diagnostics,
                        substitutor = resultSubstitutor,
                    )

                    forwardCallToInferenceSession(baseResolvedCall, context, stub, tracingStrategy)

                    @Suppress("UNCHECKED_CAST")
                    return stub as NewAbstractResolvedCall<D>
                }

                val cjPrimitiveCompleter = ResolvedAtomCompleter(
                    resultSubstitutor,
                    context,
                    this,
                    expressionTypingServices,
                    argumentTypeResolver,

                    builtIns,
                    deprecationResolver,
                    moduleDescriptor,
                    dataFlowValueFactory,
                    typeApproximator,
                    missingSupertypesResolver,
                    callComponents,
                )

                if (context.inferenceSession.shouldCompleteResolvedSubAtomsOf(candidate)) {
                    candidate.subResolvedAtoms?.forEach { subCjPrimitive ->
                        cjPrimitiveCompleter.completeAll(subCjPrimitive)
                    }
                }

                @Suppress("UNCHECKED_CAST")
                val resolvedCall = cjPrimitiveCompleter.completeResolvedCall(
                    candidate, baseResolvedCall.completedDiagnostic(resultSubstitutor),
                ) as NewAbstractResolvedCall<D>

                forwardCallToInferenceSession(baseResolvedCall, context, resolvedCall, tracingStrategy)

                resolvedCall
            }

            is SingleCallResolutionResult -> error("Call resolution result for one candidate didn't transformed: $baseResolvedCall")
            is AllCandidatesResolutionResult -> error("Cannot transform result for ALL_CANDIDATES mode")
        }
    }
}
