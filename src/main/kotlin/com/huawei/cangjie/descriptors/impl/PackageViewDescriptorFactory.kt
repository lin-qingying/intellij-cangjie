package com.huawei.cangjie.descriptors.impl

import com.huawei.cangjie.descriptors.ModuleCapability
import com.huawei.cangjie.descriptors.PackageViewDescriptor
import com.huawei.cangjie.name.FqName
import com.huawei.cangjie.storage.StorageManager

interface PackageViewDescriptorFactory {
    fun compute(
        module: ModuleDescriptorImpl,
        fqName: FqName,
        storageManager: StorageManager
    ): PackageViewDescriptor

    object Default: PackageViewDescriptorFactory {
        override fun compute(module: ModuleDescriptorImpl, fqName: FqName, storageManager: StorageManager): PackageViewDescriptor {
            return LazyPackageViewDescriptorImpl(module, fqName, storageManager)
        }
    }

    companion object {
        val CAPABILITY = ModuleCapability<PackageViewDescriptorFactory>("PackageViewDescriptorFactory")
    }
}
