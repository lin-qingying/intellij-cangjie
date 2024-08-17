package com.huawei.cangjie.descriptors.library

import com.huawei.cangjie.descriptors.PackageFragmentDescriptor
import com.huawei.cangjie.descriptors.PackageFragmentProviderOptimized
import com.huawei.cangjie.name.FqName
import com.huawei.cangjie.name.Name

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
