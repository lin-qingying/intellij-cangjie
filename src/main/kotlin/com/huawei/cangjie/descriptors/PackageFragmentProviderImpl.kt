package com.huawei.cangjie.descriptors

import com.huawei.cangjie.name.FqName
import com.huawei.cangjie.name.Name

class PackageFragmentProviderImpl(
    private val packageFragments: Collection<PackageFragmentDescriptor> = emptyList()
) : PackageFragmentProviderOptimized {
    override fun collectPackageFragments(
        fqName: FqName,
        packageFragments: MutableCollection<PackageFragmentDescriptor>
    ) {
        this.packageFragments.filterTo(packageFragments) { it.fqName == fqName }

    }

    override fun isEmpty(fqName: FqName): Boolean {
        return this.packageFragments.none { it.fqName == fqName }

    }

    @Deprecated("for usages use #packageFragments(FqName) at final point, for impl use #collectPackageFragments(FqName, MutableCollection<PackageFragmentDescriptor>)")
    override fun getPackageFragments(fqName: FqName): List<PackageFragmentDescriptor> {
        return packageFragments.filter { it.fqName == fqName }

    }

    override fun getSubPackagesOf(fqName: FqName, nameFilter: (Name) -> Boolean): Collection<FqName> {
        return packageFragments.asSequence()
            .map { it.fqName }
            .filter { !it.isRoot && it.parent() == fqName }
            .toList()
    }
}
