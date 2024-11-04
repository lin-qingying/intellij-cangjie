package com.linqingying.cangjie.extensions

import com.linqingying.cangjie.container.StorageComponentContainer

import com.linqingying.cangjie.descriptors.ModuleDescriptor
import com.linqingying.cangjie.platform.TargetPlatform

@JvmDefaultWithCompatibility
interface StorageComponentContainerContributor {
    companion object : ProjectExtensionDescriptor<StorageComponentContainerContributor>(
        "com.linqingying.cangjie.storageComponentContainerContributor", StorageComponentContainerContributor::class.java
    )
    fun registerModuleComponents(container: StorageComponentContainer, platform: TargetPlatform, moduleDescriptor: ModuleDescriptor) {}

    fun registerModuleComponents(container: StorageComponentContainer, platform: TargetPlatform ) {}
}
