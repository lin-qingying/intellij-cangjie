package com.huawei.cangjie.ide.util

import com.huawei.cangjie.descriptors.CallableDescriptor
import com.huawei.cangjie.types.CangJieType
import com.huawei.cangjie.types.TypeSubstitutor
import com.huawei.cangjie.types.nullability
import com.huawei.cangjie.types.util.TypeNullability
import com.huawei.cangjie.types.util.makeNotNullable
import com.huawei.cangjie.types.util.nullability
import com.huawei.cangjie.utils.CallType


fun <TCallable : CallableDescriptor> TCallable.substituteExtensionIfCallable(
    receiverTypes: Collection<CangJieType>,
    callType: CallType<*>,
    ignoreTypeParameters: Boolean = false,
): Collection<TCallable> {
    if (!callType.descriptorKindFilter.accepts(this)) return listOf()

    var types = receiverTypes.asSequence()
    if (callType == CallType.SAFE) {
        types = types.map { it.makeNotNullable() }
    }

    val extensionReceiverType = fuzzyExtensionReceiverType()!!
    val substitutors = types.mapNotNull {
        // NOTE: this creates a fuzzy type for `it` without type parameters
        var substitutor = extensionReceiverType.checkIsSuperTypeOf(it)

        // If enabled, we can ignore type parameters in the receiver type, and only check whether the constructors match
        if (ignoreTypeParameters && substitutor == null && it.constructor == extensionReceiverType.type.constructor) {
            substitutor = TypeSubstitutor.EMPTY
        }

        // check if we may fail due to receiver expression being nullable
        if (substitutor == null && it.nullability() == TypeNullability.NULLABLE && extensionReceiverType.nullability() == TypeNullability.NOT_NULL) {
            substitutor = extensionReceiverType.checkIsSuperTypeOf(it.makeNotNullable())
        }
        substitutor
    }

    return if (typeParameters.isEmpty()) { // optimization for non-generic callables
        if (substitutors.any()) listOf(this) else listOf()
    } else {
        substitutors
            .mapNotNull { @Suppress("UNCHECKED_CAST") (substitute(it) as TCallable?) }
            .toList()
    }
}
