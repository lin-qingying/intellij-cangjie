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
 */

package org.cangnova.cangjie.resolve.calls.model

import org.cangnova.cangjie.descriptors.TypeParameterDescriptor
import org.cangnova.cangjie.descriptors.ValueParameterDescriptor
import org.cangnova.cangjie.descriptors.macro.MacroDescriptor
import org.cangnova.cangjie.psi.Call
import org.cangnova.cangjie.psi.ValueArgument
import org.cangnova.cangjie.resolve.calls.inference.model.ResolvedValueArgument
import org.cangnova.cangjie.resolve.calls.results.ResolutionStatus
import org.cangnova.cangjie.resolve.calls.smartcasts.DataFlowInfo
import org.cangnova.cangjie.resolve.calls.tasks.ExplicitReceiverKind
import org.cangnova.cangjie.resolve.scopes.receivers.ReceiverValue
import org.cangnova.cangjie.types.CangJieType

/**
 * 宏调用的轻量级 ResolvedCall 实现
 *
 * 宏不需要类型参数推导、接收者、lambda 参数等复杂机制，
 * 因此本类仅包装 MacroDescriptor、Call 和解析状态。
 */
class ResolvedMacroCall(
    override val candidateDescriptor: MacroDescriptor,
    override val call: Call,
    override val status: ResolutionStatus
) : ResolvedCall<MacroDescriptor> {

    override val resultingDescriptor: MacroDescriptor get() = candidateDescriptor

    override val typeArguments: Map<TypeParameterDescriptor, CangJieType> get() = emptyMap()

    override val valueArgumentsByIndex: List<ResolvedValueArgument>? get() = null

    override val valueArguments: Map<ValueParameterDescriptor, ResolvedValueArgument> get() = emptyMap()

    override val dispatchReceiver: ReceiverValue? get() = null

    override val explicitReceiverKind: ExplicitReceiverKind get() = ExplicitReceiverKind.NO_EXPLICIT_RECEIVER

    override val smartCastDispatchReceiverType: CangJieType? get() = null

    override val dataFlowInfoForArguments: DataFlowInfoForArguments
        get() = object : DataFlowInfoForArguments {
            override fun getInfo(valueArgument: ValueArgument): DataFlowInfo = DataFlowInfo.EMPTY
            override val resultInfo: DataFlowInfo get() = DataFlowInfo.EMPTY
        }

    override fun getArgumentMapping(valueArgument: ValueArgument): ArgumentMapping = ArgumentUnmapped
}
