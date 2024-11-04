package com.linqingying.cangjie.descriptors.impl

import com.linqingying.cangjie.descriptors.ModuleCapability
import com.linqingying.cangjie.descriptors.PackageViewDescriptor
import com.linqingying.cangjie.name.FqName
import com.linqingying.cangjie.storage.StorageManager

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
