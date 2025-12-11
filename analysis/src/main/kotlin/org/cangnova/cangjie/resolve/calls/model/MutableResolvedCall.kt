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

package org.cangnova.cangjie.resolve.calls.model

import org.cangnova.cangjie.descriptors.CallableDescriptor
import org.cangnova.cangjie.descriptors.ValueParameterDescriptor
import org.cangnova.cangjie.psi.ValueArgument
import org.cangnova.cangjie.resolve.binding.DelegatingBindingTrace
import org.cangnova.cangjie.resolve.calls.inference.ConstraintSystem
import org.cangnova.cangjie.resolve.calls.inference.model.ResolvedValueArgument
import org.cangnova.cangjie.resolve.calls.results.ResolutionStatus
import org.cangnova.cangjie.resolve.calls.tasks.TracingStrategy
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.TypeSubstitutor

interface MutableResolvedCall<D : CallableDescriptor> : ResolvedCall<D> {

    fun addStatus(status: ResolutionStatus)

    fun setStatusToSuccess()

    val trace: DelegatingBindingTrace
    val tracingStrategy: TracingStrategy

    fun markCallAsCompleted()

    fun addRemainingTasks(task: () -> Unit)

    fun performRemainingTasks()

    val isCompleted: Boolean

    fun recordValueArgument(valueParameter: ValueParameterDescriptor, valueArgument: ResolvedValueArgument)

    fun recordArgumentMatchStatus(valueArgument: ValueArgument, matchStatus: ArgumentMatchStatus)

    override val dataFlowInfoForArguments: DataFlowInfoForArguments

    var constraintSystem: ConstraintSystem?


    fun setSubstitutor(substitutor: TypeSubstitutor)

    val knownTypeParametersSubstitutor: TypeSubstitutor?

    // todo remove: use value to parameter map status
    val hasInferredReturnType: Boolean

    fun setSmartCastDispatchReceiverType(smartCastDispatchReceiverType: CangJieType)

    fun updateExtensionReceiverWithSmartCastIfNeeded(smartCastExtensionReceiverType: CangJieType)
}