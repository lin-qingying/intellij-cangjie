package com.linqingying.cangjie.resolve.scopes.receivers

import com.linqingying.cangjie.descriptors.ClassDescriptor
import com.linqingying.cangjie.descriptors.DeclarationDescriptor
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.types.CangJieType

class ContextClassReceiver(
    val classDescriptor: ClassDescriptor,
    receiverType: CangJieType,
    override val customLabelName: Name?,
    original: ReceiverValue?
): AbstractReceiverValue(receiverType, original), ImplicitContextReceiver {
    override val declarationDescriptor: DeclarationDescriptor
        get() = classDescriptor

    override fun replaceType(newType: CangJieType): ReceiverValue = ContextClassReceiver(classDescriptor, newType, customLabelName, original)

    override fun toString(): String = "$type: Ctx { $classDescriptor }"
}
