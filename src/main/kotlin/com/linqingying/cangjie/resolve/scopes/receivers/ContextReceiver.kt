package com.linqingying.cangjie.resolve.scopes.receivers

import com.linqingying.cangjie.descriptors.CallableDescriptor
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.types.CangJieType


class ContextReceiver(
    override val declarationDescriptor: CallableDescriptor,
    receiverType: CangJieType,
    override val customLabelName: Name?,
    original: ReceiverValue?
) : AbstractReceiverValue(receiverType, original), ImplicitContextReceiver {
    override fun replaceType(newType: CangJieType): ReceiverValue = ContextReceiver(declarationDescriptor, newType, customLabelName, original)

    override fun toString(): String = "Cxt { $declarationDescriptor }"
}
