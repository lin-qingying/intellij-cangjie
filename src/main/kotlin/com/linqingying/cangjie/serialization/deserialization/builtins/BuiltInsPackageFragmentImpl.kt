package com.linqingying.cangjie.serialization.deserialization.builtins

import com.linqingying.cangjie.builtins.BuiltInsPackageFragment
import com.linqingying.cangjie.descriptors.DeclarationDescriptor
import com.linqingying.cangjie.descriptors.ModuleDescriptor
import com.linqingying.cangjie.metadata.ProtoBuf
import com.linqingying.cangjie.metadata.builtins.BuiltInsBinaryVersion
import com.linqingying.cangjie.metadata.builtins.readBuiltinsPackageFragment
import com.linqingying.cangjie.name.FqName
import com.linqingying.cangjie.resolve.DescriptorUtils
import com.linqingying.cangjie.serialization.deserialization.DeserializedPackageFragmentImpl
import com.linqingying.cangjie.storage.StorageManager
import java.io.InputStream

class BuiltInsPackageFragmentImpl private constructor(
    fqName: FqName,
    storageManager: StorageManager,
    module: ModuleDescriptor,
    proto: ProtoBuf.PackageFragment,
    metadataVersion: BuiltInsBinaryVersion,
    override val isFallback: Boolean
) : BuiltInsPackageFragment, DeserializedPackageFragmentImpl(
    fqName, storageManager, module, proto, metadataVersion, containerSource = null
) {
    companion object {
        fun create(
            fqName: FqName,
            storageManager: StorageManager,
            module: ModuleDescriptor,
            inputStream: InputStream,
            isFallback: Boolean
        ): BuiltInsPackageFragmentImpl {
            val (proto, version) = inputStream.readBuiltinsPackageFragment()

            if (proto == null) {
                // TODO: report a proper diagnostic
                throw UnsupportedOperationException(
                    "CangJie built-in definition format version is not supported: " +
                            "expected ${BuiltInsBinaryVersion.INSTANCE}, actual $version. " +
                            "Please update CangJie"
                )
            }

            return BuiltInsPackageFragmentImpl(fqName, storageManager, module, proto, version, isFallback)
        }
    }

    override fun toString(): String = "builtins package fragment for $fqName from $module"
}

val DeclarationDescriptor.module: ModuleDescriptor
    get() = DescriptorUtils.getContainingModule(this)
