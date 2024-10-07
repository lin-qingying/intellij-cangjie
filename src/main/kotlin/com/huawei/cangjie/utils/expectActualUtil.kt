package com.huawei.cangjie.utils

import com.huawei.cangjie.descriptors.CallableDescriptor
import com.huawei.cangjie.descriptors.DeclarationDescriptor
import com.huawei.cangjie.descriptors.MemberDescriptor
import com.huawei.cangjie.descriptors.ValueParameterDescriptor

fun DeclarationDescriptor.liftToExpected(): DeclarationDescriptor? {
    if (this is MemberDescriptor) {
        return when {
            isExpect -> this

            else -> null
        }
    }

    if (this is ValueParameterDescriptor) {
        val containingExpectedDescriptor = containingDeclaration.liftToExpected() as? CallableDescriptor ?: return null
        return containingExpectedDescriptor.valueParameters.getOrNull(index)
    }

    return null
}
