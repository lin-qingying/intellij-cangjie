package com.huawei.cangjie.resolve

import com.huawei.cangjie.descriptors.ModuleDescriptor
import com.huawei.cangjie.storage.StorageManager

class MissingSupertypesResolver(
    storageManager: StorageManager,
    private val moduleDescriptor: ModuleDescriptor
)
