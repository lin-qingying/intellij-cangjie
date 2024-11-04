package com.linqingying.cangjie.resolve.scopes.receivers

import com.linqingying.cangjie.descriptors.DeclarationDescriptor
import com.linqingying.cangjie.types.checker.prepareArgumentTypeRegardingCaptureTypes
import com.linqingying.cangjie.resolve.scopes.MemberScope
import com.linqingying.cangjie.types.CangJieType

// this receiver used only for resolution. see subtypes
interface DetailedReceiver
interface QualifierReceiver : Receiver, DetailedReceiver {
    val descriptor: DeclarationDescriptor

    val staticScope: MemberScope

    val classValueReceiver: ReceiverValue?

    // for qualifiers smart cast is impossible
    val classValueReceiverWithSmartCastInfo: ReceiverValueWithSmartCastInfo?
        get() = classValueReceiver?.let { ReceiverValueWithSmartCastInfo(it, emptySet(), true) }
}


class ReceiverValueWithSmartCastInfo(
    val receiverValue: ReceiverValue,
    /*
     * It doesn't include receiver.type and is used only to special marking such types (e.g. for IDE green highlighting)
     * but not to construct the resulting type
     */
    val typesFromSmartCasts: Set<CangJieType>,
    val isStable: Boolean,
    originalBaseType: CangJieType = receiverValue.type
) : DetailedReceiver {
    // It's used to construct the resulting type
    val allOriginalTypes = typesFromSmartCasts + originalBaseType

    fun hasTypesFromSmartCasts() = typesFromSmartCasts.isNotEmpty()

    override fun toString() = receiverValue.toString()
}

fun ReceiverValueWithSmartCastInfo.prepareReceiverRegardingCaptureTypes(): ReceiverValueWithSmartCastInfo {
    val preparedBaseType = prepareArgumentTypeRegardingCaptureTypes(receiverValue.type.unwrap()) ?: return this

    return ReceiverValueWithSmartCastInfo(receiverValue.replaceType(preparedBaseType), typesFromSmartCasts, isStable, receiverValue.type)
}
