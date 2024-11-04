package com.linqingying.cangjie.resolve.scopes

import com.linqingying.cangjie.descriptors.ClassDescriptor
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.storage.LockBasedStorageManager
import com.linqingying.cangjie.storage.StorageManager

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
