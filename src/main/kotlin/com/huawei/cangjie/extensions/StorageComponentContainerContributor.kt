package com.huawei.cangjie.extensions

import com.huawei.cangjie.container.StorageComponentContainer
import com.intellij.execution.target.TargetPlatform

@JvmDefaultWithCompatibility
interface StorageComponentContainerContributor {
    companion object : ProjectExtensionDescriptor<StorageComponentContainerContributor>(
        "com.huawei.cangjie.storageComponentContainerContributor", StorageComponentContainerContributor::class.java
    )

    fun registerModuleComponents(container: StorageComponentContainer, platform: TargetPlatform ) {}
}
