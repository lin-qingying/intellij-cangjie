package com.linqingying.cangjie.serialization.deserialization

import com.linqingying.cangjie.descriptors.ModuleDescriptor
import com.linqingying.cangjie.descriptors.PackageFragmentDescriptor
import com.linqingying.cangjie.descriptors.PackageFragmentProviderOptimized
import com.linqingying.cangjie.metadata.decompiler.CangJieMetadataFinder
import com.linqingying.cangjie.name.FqName
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.storage.StorageManager
import com.linqingying.cangjie.utils.addIfNotNull

abstract class AbstractDeserializedPackageFragmentProvider(
    protected val storageManager: StorageManager,
    protected val finder: CangJieMetadataFinder,
    protected val moduleDescriptor: ModuleDescriptor
) : PackageFragmentProviderOptimized {
    protected lateinit var components: DeserializationComponents

    private val fragments = storageManager.createMemoizedFunctionWithNullableValues<FqName, PackageFragmentDescriptor> { fqName ->
        findPackage(fqName)?.apply {
            initialize(components)
        }
    }

    protected abstract fun findPackage(fqName: FqName): DeserializedPackageFragment?

    override fun collectPackageFragments(fqName: FqName, packageFragments: MutableCollection<PackageFragmentDescriptor>) {
        packageFragments.addIfNotNull(fragments(fqName))
    }

    override fun isEmpty(fqName: FqName): Boolean {
        val descriptor = if (fragments.isComputed(fqName)) {
            fragments.invoke(fqName)
        } else {
            findPackage(fqName)
        }
        return descriptor == null
    }

    @Deprecated("for usages use #packageFragments(FqName) at final point, for impl use #collectPackageFragments(FqName, MutableCollection<PackageFragmentDescriptor>)")
    override fun getPackageFragments(fqName: FqName): List<PackageFragmentDescriptor> = listOfNotNull(fragments.invoke(fqName))

    override fun getSubPackagesOf(fqName: FqName, nameFilter: (Name) -> Boolean): Collection<FqName> = emptySet()
}
