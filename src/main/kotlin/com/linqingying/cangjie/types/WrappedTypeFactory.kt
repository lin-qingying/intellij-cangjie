package com.linqingying.cangjie.types

import com.linqingying.cangjie.descriptors.BindingTrace
import com.linqingying.cangjie.storage.StorageManager


open class WrappedTypeFactory(private val storageManager: StorageManager) {
    open fun createDeferredType(trace: BindingTrace, computation: () -> CangJieType): CangJieType =
        DeferredType.create(storageManager, trace, computation)

    open fun createRecursionIntolerantDeferredType(trace: BindingTrace, computation: () -> CangJieType): CangJieType =
        DeferredType.createRecursionIntolerant(storageManager, trace, computation)
}
