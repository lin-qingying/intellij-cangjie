package com.linqingying.cangjie.serialization.deserialization

import com.linqingying.cangjie.descriptors.ModuleDescriptor
import com.linqingying.cangjie.name.FqName
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.resolve.lazy.declarations.impl.PackageFragmentDescriptorImpl
import com.linqingying.cangjie.serialization.deserialization.descriptors.DeserializedMemberScope
import com.linqingying.cangjie.storage.StorageManager

abstract class DeserializedPackageFragment(
    fqName: FqName,
    protected val storageManager: StorageManager,
    module: ModuleDescriptor
) : PackageFragmentDescriptorImpl(module, fqName) {

    abstract fun initialize(components: DeserializationComponents)

    abstract val classDataFinder: ClassDataFinder

    open fun hasTopLevelClass(name: Name): Boolean {
        val scope = getMemberScope()
        return scope is DeserializedMemberScope && name in scope.classNames
    }
}
