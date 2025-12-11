/*
 * Copyright 2025 LinQingYing. and contributors.
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

package org.cangnova.cangjie.resolve.calls.tasks

import org.cangnova.cangjie.descriptors.CallableDescriptor
import org.cangnova.cangjie.psi.Call
import org.cangnova.cangjie.types.TypeSubstitutor

class OldResolutionCandidate<D : CallableDescriptor> private constructor(
    private val call: Call,
    private val candidateDescriptor: D,
    private var dispatchReceiver: ReceiverValue?, // receiver object of a method
    private var explicitReceiverKind: ExplicitReceiverKind,
    private val knownTypeParametersResultingSubstitutor: TypeSubstitutor?
) {

    fun setDispatchReceiver(dispatchReceiver: ReceiverValue?) {
        this.dispatchReceiver = dispatchReceiver
    }

    fun setExplicitReceiverKind(explicitReceiverKind: ExplicitReceiverKind) {
        this.explicitReceiverKind = explicitReceiverKind
    }

    fun getCall(): Call = call

    fun getDescriptor(): D = candidateDescriptor

    fun getDispatchReceiver(): ReceiverValue? = dispatchReceiver

    fun getExplicitReceiverKind(): ExplicitReceiverKind = explicitReceiverKind

    fun getKnownTypeParametersResultingSubstitutor(): TypeSubstitutor? = knownTypeParametersResultingSubstitutor

    override fun toString(): String = candidateDescriptor.toString()

    companion object {
        fun <D : CallableDescriptor> create(call: Call, descriptor: D): OldResolutionCandidate<D> =
            OldResolutionCandidate(call, descriptor, null, ExplicitReceiverKind.NO_EXPLICIT_RECEIVER, null)

        fun <D : CallableDescriptor> create(
            call: Call,
            descriptor: D,
            knownTypeParametersResultingSubstitutor: TypeSubstitutor?
        ): OldResolutionCandidate<D> =
            OldResolutionCandidate(
                call,
                descriptor,
                null,
                ExplicitReceiverKind.NO_EXPLICIT_RECEIVER,
                knownTypeParametersResultingSubstitutor
            )

        fun <D : CallableDescriptor> create(
            call: Call,
            descriptor: D,
            dispatchReceiver: ReceiverValue?,
            explicitReceiverKind: ExplicitReceiverKind,
            knownTypeParametersResultingSubstitutor: TypeSubstitutor?
        ): OldResolutionCandidate<D> =
            OldResolutionCandidate(
                call,
                descriptor,
                dispatchReceiver,
                explicitReceiverKind,
                knownTypeParametersResultingSubstitutor
            )
    }
}
