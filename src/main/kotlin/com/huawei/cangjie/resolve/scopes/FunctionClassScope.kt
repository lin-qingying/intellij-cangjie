package com.huawei.cangjie.resolve.scopes

import com.huawei.cangjie.builtins.functions.FunctionTypeKind
import com.huawei.cangjie.descriptors.FunctionDescriptor
import com.huawei.cangjie.descriptors.impl.FunctionClassDescriptor
import com.huawei.cangjie.descriptors.impl.FunctionInvokeDescriptor
import com.huawei.cangjie.storage.StorageManager


class FunctionClassScope(
    storageManager: StorageManager,
    containingClass: FunctionClassDescriptor
) : GivenFunctionsMemberScope(storageManager, containingClass) {
    override fun computeDeclaredFunctions(): List<FunctionDescriptor> =
        when ((containingClass as FunctionClassDescriptor).functionTypeKind) {
            FunctionTypeKind.Function -> listOf(FunctionInvokeDescriptor.create(containingClass))
//            FunctionTypeKind.SuspendFunction -> listOf(FunctionInvokeDescriptor.create(containingClass, isSuspend = true))
            else -> emptyList()
        }
}
