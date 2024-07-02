package com.huawei.cangjie.resolve.scopes.receivers

import com.huawei.cangjie.descriptors.ClassDescriptor
import com.huawei.cangjie.descriptors.DeclarationDescriptor
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.types.CangJieType

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