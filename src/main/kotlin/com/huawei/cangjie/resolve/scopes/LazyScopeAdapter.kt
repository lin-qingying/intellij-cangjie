package com.huawei.cangjie.resolve.scopes

import com.huawei.cangjie.descriptors.ClassDescriptor
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.storage.LockBasedStorageManager
import com.huawei.cangjie.storage.StorageManager

class LazyScopeAdapter @JvmOverloads constructor(
    storageManager: StorageManager = LockBasedStorageManager.NO_LOCKS,
    getScope: () -> MemberScope
) : AbstractScopeAdapter() {

    private val lazyScope = storageManager.createLazyValue{
        getScope().let {
            if (it is AbstractScopeAdapter) it.getActualScope() else it
        }
    }

    override val workerScope: MemberScope
        get() = lazyScope()


}
