package com.linqingying.cangjie.extensions.internal

import com.linqingying.cangjie.descriptors.BindingTrace
import com.linqingying.cangjie.descriptors.CallableDescriptor
import com.linqingying.cangjie.descriptors.FunctionDescriptor
import com.linqingying.cangjie.descriptors.VariableDescriptor
import com.linqingying.cangjie.extensions.ProjectExtensionDescriptor
import com.linqingying.cangjie.incremental.components.LookupLocation
import com.linqingying.cangjie.name.Name
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
import com.intellij.openapi.project.Project

@OptIn(InternalNonStableExtensionPoints::class)
class CandidateInterceptor(project: Project) {
    private val extensions = getInstances(project)

    fun interceptResolvedCallAtomCandidate(
        candidateDescriptor: CallableDescriptor,
        completedCallAtom: ResolvedCallAtom,
        trace: BindingTrace?,
        resultSubstitutor: NewTypeSubstitutor?,
        diagnostics: Collection<CangJieCallDiagnostic>
    ): CallableDescriptor = extensions.fold(candidateDescriptor) { it, extension ->
        extension.interceptResolvedCallAtomCandidate(it, completedCallAtom, trace, resultSubstitutor, diagnostics)
    }

    fun interceptResolvedCandidates(
        candidates: Collection<NewResolutionOldInference.MyCandidate>,
        context: BasicCallResolutionContext,
        candidateResolver: CandidateResolver,
        callResolver: CallResolver,
        name: Name,
        kind: NewResolutionOldInference.ResolutionKind,
        tracing: TracingStrategy
    ): Collection<NewResolutionOldInference.MyCandidate> = extensions.fold(candidates) { it, extension ->
        extension.interceptCandidates(it, context, candidateResolver, callResolver, name, kind, tracing)
    }

    fun interceptFunctionCandidates(
        candidates: Collection<FunctionDescriptor>,
        scopeTower: ImplicitScopeTower,
        resolutionContext: BasicCallResolutionContext,
        resolutionScope: ResolutionScope,
        callResolver: CallResolver,
        name: Name,
        location: LookupLocation
    ): Collection<FunctionDescriptor> = extensions.fold(candidates) { it, extension ->
        extension.interceptFunctionCandidates(it, scopeTower, resolutionContext, resolutionScope, callResolver, name, location)
    }

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
    ): Collection<FunctionDescriptor> = extensions.fold(candidates) { it, extension ->
        extension.interceptFunctionCandidates(
            it, scopeTower, resolutionContext, resolutionScope, callResolver, name, location, dispatchReceiver, extensionReceiver
        )
    }

    fun interceptVariableCandidates(
        candidates: Collection<VariableDescriptor>,
        scopeTower: ImplicitScopeTower,
        resolutionContext: BasicCallResolutionContext,
        resolutionScope: ResolutionScope,
        callResolver: CallResolver,
        name: Name,
        location: LookupLocation
    ): Collection<VariableDescriptor> = extensions.fold(candidates) { it, extension ->
        extension.interceptVariableCandidates(it, scopeTower, resolutionContext, resolutionScope, callResolver, name, location)
    }

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
    ): Collection<VariableDescriptor> = extensions.fold(candidates) { it, extension ->
        extension.interceptVariableCandidates(it, scopeTower, resolutionContext, resolutionScope, callResolver, name, location, dispatchReceiver, extensionReceiver)
    }

    companion object : ProjectExtensionDescriptor<CallResolutionInterceptorExtension>(
        "com.linqingying.cangjie.extensions.internal.callResolutionInterceptorExtension",
        CallResolutionInterceptorExtension::class.java
    )
}
