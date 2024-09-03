package com.huawei.cangjie.resolve.scopes

import com.huawei.cangjie.descriptors.FunctionDescriptor
import com.huawei.cangjie.descriptors.impl.TupleClassDescriptor
import com.huawei.cangjie.storage.StorageManager


class TupleClassScope(
    storageManager: StorageManager,
    containingClass: TupleClassDescriptor
) : GivenFunctionsMemberScope(storageManager, containingClass) {
    override fun computeDeclaredFunctions(): List<FunctionDescriptor> = emptyList()
}
