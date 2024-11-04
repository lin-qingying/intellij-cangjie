package com.linqingying.cangjie.resolve

import com.linqingying.cangjie.descriptors.CallableDescriptor
import com.linqingying.cangjie.descriptors.CallableMemberDescriptor
import com.linqingying.cangjie.descriptors.ClassDescriptor

abstract class ImportedFromObjectCallableDescriptor<out TCallable : CallableMemberDescriptor>(
    val callableFromObject: TCallable,
    private val originalOrNull: TCallable?
) : CallableDescriptor {

    val containingObject = callableFromObject.containingDeclaration as ClassDescriptor

    protected val _original
        get() = originalOrNull ?: this
}
