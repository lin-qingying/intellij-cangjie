package com.huawei.cangjie.serialization.deserialization.builtins

import com.huawei.cangjie.builtins.BuiltInsPackageFragment
import com.huawei.cangjie.descriptors.ModuleDescriptor
import com.huawei.cangjie.name.FqName
import com.huawei.cangjie.resolve.descriptorUtil.module
import com.huawei.cangjie.serialization.deserialization.DeserializedPackageFragmentImpl
import com.huawei.cangjie.storage.StorageManager

class BuiltInsPackageFragmentImpl private constructor(
    fqName: FqName,
    storageManager: StorageManager,
    module: ModuleDescriptor,
//    proto: ProtoBuf.PackageFragment,
//    metadataVersion: BuiltInsBinaryVersion,
//    override val isFallback: Boolean
) : BuiltInsPackageFragment, DeserializedPackageFragmentImpl(
    fqName, storageManager, module/*, proto, metadataVersion, containerSource = null*/
) {


    override fun toString(): String = "builtins package fragment for $fqName from $module"

}
