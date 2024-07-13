package com.huawei.cangjie.resolve.calls.tower

import com.huawei.cangjie.builtins.CangJieBuiltIns
import com.huawei.cangjie.descriptors.BindingTrace
import com.huawei.cangjie.descriptors.CallableDescriptor
import com.huawei.cangjie.descriptors.FunctionDescriptor
import com.huawei.cangjie.descriptors.ModuleDescriptor
import com.huawei.cangjie.extensions.internal.CandidateInterceptor
import com.huawei.cangjie.psi.Call
import com.huawei.cangjie.resolve.BindingContext
import com.huawei.cangjie.resolve.MissingSupertypesResolver
import com.huawei.cangjie.resolve.calls.ArgumentTypeResolver
import com.huawei.cangjie.resolve.calls.checkers.AdditionalTypeChecker
import com.huawei.cangjie.resolve.calls.checkers.CallChecker
import com.huawei.cangjie.resolve.calls.checkers.CallCheckerContext
import com.huawei.cangjie.resolve.calls.components.AdditionalDiagnosticReporter
import com.huawei.cangjie.resolve.calls.context.BasicCallResolutionContext
import com.huawei.cangjie.resolve.calls.inference.buildResultingSubstitutor
import com.huawei.cangjie.resolve.calls.inference.components.NewTypeSubstitutor
import com.huawei.cangjie.resolve.calls.model.*
import com.huawei.cangjie.resolve.calls.smartcasts.DataFlowValueFactory
import com.huawei.cangjie.resolve.calls.smartcasts.SmartCastManager
//import com.huawei.cangjie.resolve.calls.smartcasts.SmartCastManager
import com.huawei.cangjie.resolve.calls.tasks.TracingStrategy
import com.huawei.cangjie.resolve.calls.util.isFakeElement
import com.huawei.cangjie.resolve.constants.evaluate.ConstantExpressionEvaluator
import com.huawei.cangjie.resolve.deprecation.DeprecationResolver
import com.huawei.cangjie.types.TypeApproximator
import com.huawei.cangjie.types.expressions.DataFlowAnalyzer
import com.huawei.cangjie.types.expressions.DoubleColonExpressionResolver
import com.huawei.cangjie.types.expressions.ExpressionTypingServices
import com.huawei.cangjie.types.model.TypeSystemInferenceExtensionContextDelegate

class CangJieToResolvedCallTransformer(


    private val callCheckers: Iterable<CallChecker>,
    private val additionalTypeCheckers: Iterable<AdditionalTypeChecker>,
    private val dataFlowAnalyzer: DataFlowAnalyzer,
    private val argumentTypeResolver: ArgumentTypeResolver,
    private val constantExpressionEvaluator: ConstantExpressionEvaluator,
    private val deprecationResolver: DeprecationResolver,
    private val expressionTypingServices: ExpressionTypingServices,
    private val doubleColonExpressionResolver: DoubleColonExpressionResolver,
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
        fun keyForPartiallyResolvedCall(resolvedCallAtom: ResolvedCallAtom): Call {
            val psiCangJieCall = resolvedCallAtom.atom.psiCangJieCall
            return if (psiCangJieCall is PSICangJieCallForInvoke)
                psiCangJieCall.baseCall.psiCall
            else
                psiCangJieCall.psiCall
        }
    }

    fun <D : CallableDescriptor> onlyTransform(
        resolvedCallAtom: ResolvedCallAtom,
        diagnostics: Collection<CangJieCallDiagnostic>,
    ): NewAbstractResolvedCall<D> = transformToResolvedCall(resolvedCallAtom, null, null, diagnostics)

    private fun bind(trace: BindingTrace, simpleResolvedCall: NewAbstractResolvedCall<*>) {
        val tracing = simpleResolvedCall.psiCangJieCall.tracingStrategy

        tracing.bindReference(trace, simpleResolvedCall)
//        tracing.bindResolvedCall(trace, simpleResolvedCall)
    }

    internal fun bind(trace: BindingTrace, resolvedCall: ResolvedCall<*>) {
        (resolvedCall as? NewAbstractResolvedCall<*>)?.let { bind(trace, it) }
//        (resolvedCall as? NewVariableAsFunctionResolvedCallImpl)?.let { bind(trace, it) }
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
                @Suppress("USELESS_CAST") // K2 warning suppression, TODO: KT-62472
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
                    doubleColonExpressionResolver,
                    builtIns,
                    deprecationResolver,
                    moduleDescriptor,
                    dataFlowValueFactory,
//                    typeApproximator,
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
