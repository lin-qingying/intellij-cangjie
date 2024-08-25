package com.huawei.cangjie.descriptors

import com.huawei.cangjie.name.FqName
import com.huawei.cangjie.name.Name
import java.util.ArrayList
import java.util.HashSet


interface PackageFragmentProviderOptimized : PackageFragmentProvider {
    fun collectPackageFragments(fqName: FqName, packageFragments: MutableCollection<PackageFragmentDescriptor>)
    fun isEmpty(fqName: FqName): Boolean
}

interface PackageFragmentProvider {
    @Deprecated("for usages use #packageFragments(FqName) at final point, for impl use #collectPackageFragments(FqName, MutableCollection<PackageFragmentDescriptor>)")
    fun getPackageFragments(fqName: FqName): List<PackageFragmentDescriptor>

    fun getSubPackagesOf(fqName: FqName, nameFilter: (Name) -> Boolean): Collection<FqName>

    object Empty : PackageFragmentProvider {
        @Deprecated("for usages use #packageFragments(FqName) at final point, for impl use #collectPackageFragments(FqName, MutableCollection<PackageFragmentDescriptor>)",
            ReplaceWith("emptyList<PackageFragmentDescriptor>()")
        )
        override fun getPackageFragments(fqName: FqName) = emptyList<PackageFragmentDescriptor>()

        override fun getSubPackagesOf(fqName: FqName, nameFilter: (Name) -> Boolean) = emptySet<FqName>()
    }
}

fun PackageFragmentProvider.collectPackageFragmentsOptimizedIfPossible(
    fqName: FqName,
    packageFragments: MutableCollection<PackageFragmentDescriptor>
) {
    when (this) {
        is PackageFragmentProviderOptimized -> collectPackageFragments(fqName, packageFragments)
        else -> packageFragments.addAll(@Suppress("DEPRECATION") getPackageFragments(fqName))
    }
}

class CompositePackageFragmentProvider(// can be modified from outside
    private val providers: List<PackageFragmentProvider>,
    private val debugName: String
) : PackageFragmentProviderOptimized {
    override fun collectPackageFragments(
        fqName: FqName,
        packageFragments: MutableCollection<PackageFragmentDescriptor>
    ) {
        for (provider in providers) {
            provider.collectPackageFragmentsOptimizedIfPossible(fqName, packageFragments)
        }
    }

    override fun isEmpty(fqName: FqName): Boolean {

        return providers.all { it.isEmpty(fqName) }
    }

    @Deprecated("for usages use #packageFragments(FqName) at final point, for impl use #collectPackageFragments(FqName, MutableCollection<PackageFragmentDescriptor>)")
    override fun getPackageFragments(fqName: FqName): List<PackageFragmentDescriptor> {
        val result = ArrayList<PackageFragmentDescriptor>()
        for (provider in providers) {
            provider.collectPackageFragmentsOptimizedIfPossible(fqName, result)
        }
        return result.toList()
    }

    override fun getSubPackagesOf(fqName: FqName, nameFilter: (Name) -> Boolean): Collection<FqName> {
        val result = HashSet<FqName>()
        for (provider in providers) {
            result.addAll(provider.getSubPackagesOf(fqName, nameFilter))
        }
        return result
    }
    override fun toString(): String = debugName
}

fun PackageFragmentProvider.packageFragments(fqName: FqName): List<PackageFragmentDescriptor> {
    val packageFragments = mutableListOf<PackageFragmentDescriptor>()
    collectPackageFragmentsOptimizedIfPossible(fqName, packageFragments)
    return packageFragments
}
fun PackageFragmentProvider.isEmpty(fqName: FqName): Boolean {
    return when (this) {
        is PackageFragmentProviderOptimized -> isEmpty(fqName)
        else -> packageFragments(fqName).isEmpty()
    }
}
