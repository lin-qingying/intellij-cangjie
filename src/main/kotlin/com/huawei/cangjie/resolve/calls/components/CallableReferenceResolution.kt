package com.huawei.cangjie.resolve.calls.components

import com.huawei.cangjie.descriptors.ValueParameterDescriptor
import com.huawei.cangjie.resolve.calls.model.ResolvedCallArgument
import com.huawei.cangjie.resolve.scopes.receivers.ReceiverValueWithSmartCastInfo
import com.huawei.cangjie.types.CangJieType
import com.huawei.cangjie.types.expressions.CoercionStrategy


sealed class CallableReceiver(val receiver: ReceiverValueWithSmartCastInfo) {
    class UnboundReference(receiver: ReceiverValueWithSmartCastInfo) : CallableReceiver(receiver)
    class BoundValueReference(receiver: ReceiverValueWithSmartCastInfo) : CallableReceiver(receiver)
    class ScopeReceiver(receiver: ReceiverValueWithSmartCastInfo) : CallableReceiver(receiver)
    class ExplicitValueReceiver(receiver: ReceiverValueWithSmartCastInfo) : CallableReceiver(receiver)
}
class CallableReferenceAdaptation(
    val argumentTypes: Array<CangJieType>,
    val coercionStrategy: CoercionStrategy,
    val defaults: Int,
    val mappedArguments: Map<ValueParameterDescriptor, ResolvedCallArgument>,
//    val suspendConversionStrategy: SuspendConversionStrategy
)
