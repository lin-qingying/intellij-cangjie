package com.linqingying.cangjie.resolve.scopes

import com.linqingying.cangjie.builtins.functions.FunctionTypeKind
import com.linqingying.cangjie.descriptors.FunctionDescriptor
import com.linqingying.cangjie.descriptors.impl.FunctionClassDescriptor
import com.linqingying.cangjie.descriptors.impl.FunctionInvokeDescriptor
import com.linqingying.cangjie.storage.StorageManager


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
