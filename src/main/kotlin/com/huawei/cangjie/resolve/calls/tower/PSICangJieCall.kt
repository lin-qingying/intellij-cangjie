package com.huawei.cangjie.resolve.calls.tower

import com.huawei.cangjie.descriptors.BindingTrace
import com.huawei.cangjie.descriptors.CallableDescriptor
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.psi.Call
import com.huawei.cangjie.resolve.calls.CallTransformer
import com.huawei.cangjie.resolve.calls.components.candidate.ResolutionCandidate
import com.huawei.cangjie.resolve.calls.model.*
import com.huawei.cangjie.resolve.calls.smartcasts.DataFlowInfo
import com.huawei.cangjie.resolve.calls.tasks.TracingStrategy
import com.huawei.cangjie.resolve.calls.tasks.TracingStrategyForInvoke
import com.huawei.cangjie.resolve.calls.util.getResolvedCall
import com.huawei.cangjie.resolve.scopes.receivers.ExpressionReceiver
import com.huawei.cangjie.resolve.scopes.receivers.ReceiverValue
import com.huawei.cangjie.utils.OperatorNameConventions

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

class PSICangJieCallImpl(
    override val callKind: CangJieCallKind,
    override val psiCall: Call,
    override val tracingStrategy: TracingStrategy,
    override val explicitReceiver: ReceiverCangJieCallArgument?,
    override val dispatchReceiverForInvokeExtension: ReceiverCangJieCallArgument?,
    override val name: Name,
    override val typeArguments: List<TypeArgument>,
    override val argumentsInParenthesis: List<CangJieCallArgument>,
    override val externalArgument: CangJieCallArgument?,
    override val startingDataFlowInfo: DataFlowInfo,
    override val resultDataFlowInfo: DataFlowInfo,
    override val dataFlowInfoForArguments: DataFlowInfoForArguments,
    override val isForImplicitInvoke: Boolean
) : PSICangJieCall()
