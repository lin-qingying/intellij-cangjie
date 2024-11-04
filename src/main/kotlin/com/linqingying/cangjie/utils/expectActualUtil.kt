package com.linqingying.cangjie.utils

import com.linqingying.cangjie.descriptors.CallableDescriptor
import com.linqingying.cangjie.descriptors.DeclarationDescriptor
import com.linqingying.cangjie.descriptors.MemberDescriptor
import com.linqingying.cangjie.descriptors.ValueParameterDescriptor

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
