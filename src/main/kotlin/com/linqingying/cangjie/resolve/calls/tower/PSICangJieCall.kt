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

package com.linqingying.cangjie.resolve.calls.tower

import com.linqingying.cangjie.descriptors.BindingTrace
import com.linqingying.cangjie.descriptors.CallableDescriptor
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.psi.Call
import com.linqingying.cangjie.resolve.calls.CallTransformer
import com.linqingying.cangjie.resolve.calls.components.candidate.ResolutionCandidate
import com.linqingying.cangjie.resolve.calls.model.*
import com.linqingying.cangjie.resolve.calls.smartcasts.DataFlowInfo
import com.linqingying.cangjie.resolve.calls.tasks.TracingStrategy
import com.linqingying.cangjie.resolve.calls.tasks.TracingStrategyForInvoke
import com.linqingying.cangjie.resolve.calls.util.getResolvedCall
import com.linqingying.cangjie.resolve.scopes.receivers.ExpressionReceiver
import com.linqingying.cangjie.resolve.scopes.receivers.ReceiverValue
import com.linqingying.cangjie.utils.OperatorNameConventions

val CangJieCall.psiCangJieCall: PSICangJieCall
    get() {
        assert(this is PSICangJieCall) {
            "Incorrect ASTCAll: $this. Java class: ${javaClass.canonicalName}"
        }
        return this as PSICangJieCall
    }

abstract class PSICangJieCall : CangJieCall {
    abstract val psiCall: Call
    abstract val startingDataFlowInfo: DataFlowInfo
    abstract val resultDataFlowInfo: DataFlowInfo
    abstract val dataFlowInfoForArguments: DataFlowInfoForArguments
    abstract val tracingStrategy: TracingStrategy

    override fun toString() = "$psiCall"
}


@Suppress("UNCHECKED_CAST")
fun <D : CallableDescriptor> CangJieCall.getResolvedPsiCangJieCall(trace: BindingTrace): NewResolvedCallImpl<D>? =
    psiCangJieCall.psiCall.getResolvedCall(trace.bindingContext) as? NewResolvedCallImpl<D>


class PSICangJieCallForInvoke(
    val baseCall: PSICangJieCallImpl,
    val variableCall: ResolutionCandidate,
    override val explicitReceiver: ReceiverCangJieCallArgument,
    override val dispatchReceiverForInvokeExtension: SimpleCangJieCallArgument?
) : PSICangJieCall() {
    override val callKind: CangJieCallKind get() = CangJieCallKind.FUNCTION
    override val name: Name get() = OperatorNameConventions.INVOKE
    override val typeArguments: List<TypeArgument> get() = baseCall.typeArguments
    override val topTypeArguments: List<TypeArgument> = baseCall.topTypeArguments
    override val argumentsInParenthesis: List<CangJieCallArgument> get() = baseCall.argumentsInParenthesis
    override val externalArgument: CangJieCallArgument? get() = baseCall.externalArgument

    override val startingDataFlowInfo: DataFlowInfo get() = baseCall.startingDataFlowInfo
    override val resultDataFlowInfo: DataFlowInfo get() = baseCall.resultDataFlowInfo
    override val dataFlowInfoForArguments: DataFlowInfoForArguments get() = baseCall.dataFlowInfoForArguments
    override val psiCall: Call
    override val tracingStrategy: TracingStrategy
    override val isForImplicitInvoke: Boolean = true

    init {
        val variableReceiver = dispatchReceiverForInvokeExtension ?: explicitReceiver
        val explicitExtensionReceiver = if (dispatchReceiverForInvokeExtension == null) null else explicitReceiver
        val calleeExpression = baseCall.psiCall.calleeExpression!!

        psiCall = CallTransformer.CallForImplicitInvoke(
            explicitExtensionReceiver?.receiverValue,
            variableReceiver.receiverValue as ExpressionReceiver, baseCall.psiCall, true
        )
        tracingStrategy =
            TracingStrategyForInvoke(
                calleeExpression,
                psiCall,
                variableReceiver.receiverValue!!.type
            ) // check for type parameters
    }
}

val ReceiverCangJieCallArgument.receiverValue: ReceiverValue?
    get() = when (this) {
        is SimpleCangJieCallArgument -> this.receiver.receiverValue
//        is QualifierReceiverCangJieCallArgument -> this.receiver.classValueReceiver
        else -> null
    }

class PSICangJieCallForVariable(
    val baseCall: PSICangJieCallImpl,
    override val explicitReceiver: ReceiverCangJieCallArgument?,
    override val name: Name
) : PSICangJieCall() {
    override val callKind: CangJieCallKind get() = CangJieCallKind.VARIABLE
    override val typeArguments: List<TypeArgument> get() = emptyList()
    override val topTypeArguments: List<TypeArgument> = emptyList()
    override val argumentsInParenthesis: List<CangJieCallArgument> get() = emptyList()
    override val externalArgument: CangJieCallArgument? get() = null

    override val startingDataFlowInfo: DataFlowInfo get() = baseCall.startingDataFlowInfo
    override val resultDataFlowInfo: DataFlowInfo get() = baseCall.startingDataFlowInfo
    override val dataFlowInfoForArguments: DataFlowInfoForArguments get() = baseCall.dataFlowInfoForArguments

    override val tracingStrategy: TracingStrategy get() = baseCall.tracingStrategy
    override val psiCall: Call = CallTransformer.stripCallArguments(baseCall.psiCall).let {
        if (explicitReceiver == null) CallTransformer.stripReceiver(it) else it
    }

    override val isForImplicitInvoke: Boolean get() = false
}
class PSICangJieCallImpl(
    override val callKind: CangJieCallKind,
    override val psiCall: Call,
    override val tracingStrategy: TracingStrategy,
    override val explicitReceiver: ReceiverCangJieCallArgument?,
    override val dispatchReceiverForInvokeExtension: ReceiverCangJieCallArgument?,
    override val name: Name,
    override val typeArguments: List<TypeArgument>,
    override val topTypeArguments: List<TypeArgument>,
    override val argumentsInParenthesis: List<CangJieCallArgument>,
    override val externalArgument: CangJieCallArgument?,
    override val startingDataFlowInfo: DataFlowInfo,
    override val resultDataFlowInfo: DataFlowInfo,
    override val dataFlowInfoForArguments: DataFlowInfoForArguments,
    override val isForImplicitInvoke: Boolean
) : PSICangJieCall()
