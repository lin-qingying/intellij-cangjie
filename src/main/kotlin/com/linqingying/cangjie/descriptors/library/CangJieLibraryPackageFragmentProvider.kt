package com.linqingying.cangjie.descriptors.library

import com.linqingying.cangjie.descriptors.PackageFragmentDescriptor
import com.linqingying.cangjie.descriptors.PackageFragmentProviderOptimized
import com.linqingying.cangjie.name.FqName
import com.linqingying.cangjie.name.Name

class CangJieLibraryPackageFragmentProvider: PackageFragmentProviderOptimized {
    override fun collectPackageFragments(
        fqName: FqName,
        packageFragments: MutableCollection<PackageFragmentDescriptor>
    ) {
        TODO("Not yet implemented")
    }

    override fun isEmpty(fqName: FqName): Boolean {
        TODO("Not yet implemented")
    }

    override fun getPackageFragments(fqName: FqName): List<PackageFragmentDescriptor> {
        TODO("Not yet implemented")
    }

    override fun getSubPackagesOf(fqName: FqName, nameFilter: (Name) -> Boolean): Collection<FqName> {
        TODO("Not yet implemented")
    }
}
