/*
 * Copyright 2026 LinQingYing. and contributors.
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

package org.cangnova.cangjie.extensions.internal

import org.cangnova.cangjie.descriptors.CallableDescriptor
import org.cangnova.cangjie.descriptors.FunctionDescriptor
import org.cangnova.cangjie.descriptors.VariableDescriptor
import org.cangnova.cangjie.descriptors.impl.AnonymousFunctionDescriptor
import org.cangnova.cangjie.incremental.components.LookupLocation
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjLambdaExpression
import org.cangnova.cangjie.resolve.binding.BindingTrace
import org.cangnova.cangjie.resolve.calls.CallResolver
import org.cangnova.cangjie.resolve.calls.CandidateResolver
import org.cangnova.cangjie.resolve.calls.context.BasicCallResolutionContext
import org.cangnova.cangjie.resolve.calls.model.CangJieCallDiagnostic
import org.cangnova.cangjie.resolve.calls.model.ResolvedCallAtom
import org.cangnova.cangjie.resolve.calls.tasks.TracingStrategy
import org.cangnova.cangjie.resolve.calls.tower.ImplicitScopeTower
import org.cangnova.cangjie.resolve.calls.tower.ResolutionOldInference
import org.cangnova.cangjie.resolve.calls.tower.PSICallResolver
import org.cangnova.cangjie.resolve.scopes.ResolutionScope
import org.cangnova.cangjie.resolve.scopes.receivers.ReceiverValueWithSmartCastInfo
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.ComposableTypeSubstitutor
import org.cangnova.cangjie.types.expressions.ExpressionTypingContext

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
        resultSubstitutor: ComposableTypeSubstitutor?,
        diagnostics: Collection<CangJieCallDiagnostic>
    ): CallableDescriptor = candidateDescriptor

    fun interceptCandidates(
        candidates: Collection<ResolutionOldInference.MyCandidate>,
        context: BasicCallResolutionContext,
        candidateResolver: CandidateResolver,
        callResolver: CallResolver,
        name: Name,
        kind: ResolutionOldInference.ResolutionKind,
        tracing: TracingStrategy
    ): Collection<ResolutionOldInference.MyCandidate> = candidates

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

    ): Collection<VariableDescriptor> = candidates
}
