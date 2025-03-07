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

package cn.cangnova.cangjie.extensions.internal

import cn.cangnova.cangjie.descriptors.BindingTrace
import cn.cangnova.cangjie.descriptors.CallableDescriptor
import cn.cangnova.cangjie.descriptors.FunctionDescriptor
import cn.cangnova.cangjie.descriptors.VariableDescriptor
import cn.cangnova.cangjie.extensions.ProjectExtensionDescriptor
import cn.cangnova.cangjie.incremental.components.LookupLocation
import cn.cangnova.cangjie.name.Name
import cn.cangnova.cangjie.resolve.calls.CallResolver
import cn.cangnova.cangjie.resolve.calls.CandidateResolver
import cn.cangnova.cangjie.resolve.calls.context.BasicCallResolutionContext
import cn.cangnova.cangjie.resolve.calls.inference.components.NewTypeSubstitutor
import cn.cangnova.cangjie.resolve.calls.model.CangJieCallDiagnostic
import cn.cangnova.cangjie.resolve.calls.model.ResolvedCallAtom
import cn.cangnova.cangjie.resolve.calls.tasks.TracingStrategy
import cn.cangnova.cangjie.resolve.calls.tower.ImplicitScopeTower
import cn.cangnova.cangjie.resolve.calls.tower.NewResolutionOldInference
import cn.cangnova.cangjie.resolve.calls.tower.PSICallResolver
import cn.cangnova.cangjie.resolve.scopes.ResolutionScope
import cn.cangnova.cangjie.resolve.scopes.receivers.ReceiverValueWithSmartCastInfo
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
        "cn.cangnova.cangjie.extensions.internal.callResolutionInterceptorExtension",
        CallResolutionInterceptorExtension::class.java
    )
}
