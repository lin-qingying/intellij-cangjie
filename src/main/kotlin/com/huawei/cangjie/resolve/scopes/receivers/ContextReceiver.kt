package com.huawei.cangjie.resolve.scopes.receivers

import com.huawei.cangjie.descriptors.CallableDescriptor
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.types.CangJieType


class ContextReceiver(
    override val declarationDescriptor: CallableDescriptor,
    receiverType: CangJieType,
    override val customLabelName: Name?,
    original: ReceiverValue?
) : AbstractReceiverValue(receiverType, original), ImplicitContextReceiver {
    override fun replaceType(newType: CangJieType): ReceiverValue = ContextReceiver(declarationDescriptor, newType, customLabelName, original)

    override fun toString(): String = "Cxt { $declarationDescriptor }"
}