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

package cn.cangnova.cangjie.resolve.calls.components.candidate


import cn.cangnova.cangjie.descriptors.CallableDescriptor
import cn.cangnova.cangjie.resolve.calls.components.CallableReceiver
import cn.cangnova.cangjie.resolve.calls.components.CallableReferenceAdaptation
import cn.cangnova.cangjie.resolve.calls.components.CangJieResolutionCallbacks
import cn.cangnova.cangjie.resolve.calls.inference.components.FreshVariableNewTypeSubstitutor
import cn.cangnova.cangjie.resolve.calls.inference.model.ConstraintStorage
import cn.cangnova.cangjie.resolve.calls.model.*
import cn.cangnova.cangjie.resolve.calls.tasks.ExplicitReceiverKind
import cn.cangnova.cangjie.resolve.calls.tower.ImplicitScopeTower
import cn.cangnova.cangjie.types.TypeSubstitutor
import cn.cangnova.cangjie.types.UnwrappedType


class CallableReferenceResolutionCandidate(
    val candidate: CallableDescriptor,
    val dispatchReceiver: CallableReceiver?,
    val extensionReceiver: CallableReceiver?,
    val explicitReceiverKind: ExplicitReceiverKind,
    val reflectionCandidateType: UnwrappedType,
    val callableReferenceAdaptation: CallableReferenceAdaptation?,
    val cangjieCall: CallableReferenceResolutionAtom,
    val expectedType: UnwrappedType?,
    override val callComponents: CangJieCallComponents,
    override val scopeTower: ImplicitScopeTower,
    override val resolutionCallbacks: CangJieResolutionCallbacks,
    override val baseSystem: ConstraintStorage?
) : ResolutionCandidate() {
    override val variableCandidateIfInvoke: ResolutionCandidate? = null

    override val knownTypeParametersResultingSubstitutor: TypeSubstitutor? = null // callable reference's rhs doesn't have type parameters

    override val resolvedCall = ResolvedCallableReferenceCallAtom(
        cangjieCall.call, candidate, explicitReceiverKind,
        if (dispatchReceiver != null) ReceiverExpressionCangJieCallArgument(dispatchReceiver.receiver) else null,
        if (extensionReceiver != null) ReceiverExpressionCangJieCallArgument(extensionReceiver.receiver) else null,
        reflectionCandidateType,
        candidate = this
    )

    override fun addResolvedCjPrimitive(resolvedAtom: ResolvedAtom) {} // there aren't nested resolved primitives for callable references


    override fun getSubResolvedAtoms(): List<ResolvedAtom> = emptyList()

    var freshVariablesSubstitutor: FreshVariableNewTypeSubstitutor? = null
        internal set

    val numDefaults get() = callableReferenceAdaptation?.defaults ?: 0
}
