package com.linqingying.cangjie.extensions.internal

import com.linqingying.cangjie.descriptors.BindingTrace
import com.linqingying.cangjie.descriptors.CallableDescriptor
import com.linqingying.cangjie.descriptors.FunctionDescriptor
import com.linqingying.cangjie.descriptors.VariableDescriptor
import com.linqingying.cangjie.descriptors.impl.AnonymousFunctionDescriptor
import com.linqingying.cangjie.incremental.components.LookupLocation
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.psi.CjElement
import com.linqingying.cangjie.psi.CjLambdaExpression
import com.linqingying.cangjie.resolve.calls.CallResolver
import com.linqingying.cangjie.resolve.calls.CandidateResolver
import com.linqingying.cangjie.resolve.calls.context.BasicCallResolutionContext
import com.linqingying.cangjie.resolve.calls.inference.components.NewTypeSubstitutor
import com.linqingying.cangjie.resolve.calls.model.CangJieCallDiagnostic
import com.linqingying.cangjie.resolve.calls.model.ResolvedCallAtom
import com.linqingying.cangjie.resolve.calls.tasks.TracingStrategy
import com.linqingying.cangjie.resolve.calls.tower.ImplicitScopeTower
import com.linqingying.cangjie.resolve.calls.tower.NewResolutionOldInference
import com.linqingying.cangjie.resolve.calls.tower.PSICallResolver
import com.linqingying.cangjie.resolve.scopes.ResolutionScope
import com.linqingying.cangjie.resolve.scopes.receivers.ReceiverValueWithSmartCastInfo
import com.linqingying.cangjie.types.CangJieType
import com.linqingying.cangjie.types.expressions.ExpressionTypingContext

/**
 * This is marker for non-stable experimental extension points.
 * Extension points marked with this meta-annotation will be broken in the future version.
 * Please do not use them in general code.
 */
@RequiresOptIn(level = RequiresOptIn.Level.ERROR)
@Retention(AnnotationRetention.BINARY)
annotation class InternalNonStableExtensionPoints

@InternalNonStableExtensionPoints
interface TypeResolutionInterceptorExtension {
    fun interceptFunctionLiteralDescriptor(
        expression: CjLambdaExpression,
        context: ExpressionTypingContext,
        descriptor: AnonymousFunctionDescriptor
    ): AnonymousFunctionDescriptor = descriptor

    fun interceptType(
        element: CjElement,
        context: ExpressionTypingContext,
        resultType: CangJieType
    ): CangJieType = resultType
}


@InternalNonStableExtensionPoints
interface CallResolutionInterceptorExtension {
    fun interceptResolvedCallAtomCandidate(
        candidateDescriptor: CallableDescriptor,
        completedCallAtom: ResolvedCallAtom,
        trace: BindingTrace?,
        resultSubstitutor: NewTypeSubstitutor?,
        diagnostics: Collection<CangJieCallDiagnostic>
    ): CallableDescriptor = candidateDescriptor

    fun interceptCandidates(
        candidates: Collection<NewResolutionOldInference.MyCandidate>,
        context: BasicCallResolutionContext,
        candidateResolver: CandidateResolver,
        callResolver: CallResolver,
        name: Name,
        kind: NewResolutionOldInference.ResolutionKind,
        tracing: TracingStrategy
    ): Collection<NewResolutionOldInference.MyCandidate> = candidates

    fun interceptFunctionCandidates(
        candidates: Collection<FunctionDescriptor>,
        scopeTower: ImplicitScopeTower,
        resolutionContext: BasicCallResolutionContext,
        resolutionScope: ResolutionScope,
        callResolver: CallResolver,
        name: Name,
        location: LookupLocation
    ): Collection<FunctionDescriptor> = candidates

    fun interceptFunctionCandidates(
        candidates: Collection<FunctionDescriptor>,
        scopeTower: ImplicitScopeTower,
        resolutionContext: BasicCallResolutionContext,
        resolutionScope: ResolutionScope,
        callResolver: PSICallResolver,
        name: Name,
        location: LookupLocation,
        dispatchReceiver: ReceiverValueWithSmartCastInfo?,
        extensionReceiver: ReceiverValueWithSmartCastInfo?
    ): Collection<FunctionDescriptor> = candidates

    fun interceptVariableCandidates(
        candidates: Collection<VariableDescriptor>,
        scopeTower: ImplicitScopeTower,
        resolutionContext: BasicCallResolutionContext,
        resolutionScope: ResolutionScope,
        callResolver: CallResolver,
        name: Name,
        location: LookupLocation
    ): Collection<VariableDescriptor> = candidates

    fun interceptVariableCandidates(
        candidates: Collection<VariableDescriptor>,
        scopeTower: ImplicitScopeTower,
        resolutionContext: BasicCallResolutionContext,
        resolutionScope: ResolutionScope,
        callResolver: PSICallResolver,
        name: Name,
        location: LookupLocation,
        dispatchReceiver: ReceiverValueWithSmartCastInfo?,
        extensionReceiver: ReceiverValueWithSmartCastInfo?
    ): Collection<VariableDescriptor> = candidates
}
