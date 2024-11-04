package com.linqingying.cangjie.resolve.scopes

import com.linqingying.cangjie.descriptors.FunctionDescriptor
import com.linqingying.cangjie.descriptors.impl.TupleClassDescriptor
import com.linqingying.cangjie.storage.StorageManager


class TupleClassScope(
    storageManager: StorageManager,
    containingClass: TupleClassDescriptor
) : GivenFunctionsMemberScope(storageManager, containingClass) {
    override fun computeDeclaredFunctions(): List<FunctionDescriptor> = emptyList()
}
