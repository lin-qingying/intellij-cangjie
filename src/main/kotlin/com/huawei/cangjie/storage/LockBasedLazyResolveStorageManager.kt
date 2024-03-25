package com.huawei.cangjie.storage

import com.huawei.cangjie.descriptors.BindingTrace


class LockBasedLazyResolveStorageManager(private val storageManager: StorageManager) : StorageManager by storageManager,
    LazyResolveStorageManager {
    override fun <K, V : Any> createSoftlyRetainedMemoizedFunction(compute: (K) -> V): MemoizedFunctionToNotNull<K, V> {
        TODO("Not yet implemented")
    }

    override fun <K, V : Any> createSoftlyRetainedMemoizedFunctionWithNullableValues(compute: (K) -> V): MemoizedFunctionToNullable<K, V> {
        TODO("Not yet implemented")
    }

    override fun createSafeTrace(originalTrace: BindingTrace): BindingTrace {
        TODO("Not yet implemented")
    }


}