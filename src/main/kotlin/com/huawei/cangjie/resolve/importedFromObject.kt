package com.huawei.cangjie.resolve

import com.huawei.cangjie.descriptors.CallableDescriptor
import com.huawei.cangjie.descriptors.CallableMemberDescriptor
import com.huawei.cangjie.descriptors.ClassDescriptor

abstract class ImportedFromObjectCallableDescriptor<out TCallable : CallableMemberDescriptor>(
    val callableFromObject: TCallable,
    private val originalOrNull: TCallable?
) : CallableDescriptor {

    val containingObject = callableFromObject.containingDeclaration as ClassDescriptor

    protected val _original
        get() = originalOrNull ?: this
}
