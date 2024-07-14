package com.huawei.cangjie.serialization.deserialization

import com.huawei.cangjie.descriptors.ModuleDescriptor
import com.huawei.cangjie.name.FqName
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.resolve.lazy.declarations.impl.PackageFragmentDescriptorImpl
import com.huawei.cangjie.storage.StorageManager




abstract class DeserializedPackageFragment(
    fqName: FqName,
    protected val storageManager: StorageManager,
    module: ModuleDescriptor
) : PackageFragmentDescriptorImpl(module, fqName)
{


    abstract fun initialize(components: DeserializationComponents)

//    abstract val classDataFinder: ClassDataFinder

//    open fun hasTopLevelClass(name: Name): Boolean {
//        val scope = getMemberScope()
//        return scope is DeserializedMemberScope && name in scope.class    Names
//    }
}
