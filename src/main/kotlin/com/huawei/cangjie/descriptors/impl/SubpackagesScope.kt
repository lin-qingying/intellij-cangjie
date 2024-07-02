package com.huawei.cangjie.descriptors.impl

import com.huawei.cangjie.descriptors.DeclarationDescriptor
import com.huawei.cangjie.descriptors.ModuleDescriptor
import com.huawei.cangjie.descriptors.PackageViewDescriptor
import com.huawei.cangjie.name.FqName
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.resolve.scopes.DescriptorKindExclude
import com.huawei.cangjie.resolve.scopes.DescriptorKindFilter
import com.huawei.cangjie.resolve.source.MemberScopeImpl
import com.huawei.cangjie.utils.Printer
import com.intellij.util.containers.addIfNotNull


open class SubpackagesScope(private val moduleDescriptor: ModuleDescriptor, private val fqName: FqName) : MemberScopeImpl() {

    protected fun getPackage(name: Name): PackageViewDescriptor? {
        if (name.isSpecial) {
            return null
        }
        val packageViewDescriptor = moduleDescriptor.getPackage(fqName.child(name))
        if (packageViewDescriptor.isEmpty()) {
            return null
        }
        return packageViewDescriptor
    }

    override fun getContributedDescriptors(kindFilter: DescriptorKindFilter,
                                           nameFilter: (Name) -> Boolean): Collection<DeclarationDescriptor> {
        if (!kindFilter.acceptsKinds(DescriptorKindFilter.PACKAGES_MASK)) return listOf()
        if (fqName.isRoot && kindFilter.excludes.contains(DescriptorKindExclude.TopLevelPackages)) return listOf()

        val subFqNames = moduleDescriptor.getSubPackagesOf(fqName, nameFilter)
        val result = ArrayList<DeclarationDescriptor>(subFqNames.size)
        for (subFqName in subFqNames) {
            val shortName = subFqName.shortName()
            if (nameFilter(shortName)) {
                result.addIfNotNull(getPackage(shortName))
            }
        }
        return result
    }



//    override fun getClassifierNames(): Set<Name> = emptySet()
//
    override fun printScopeStructure(p: Printer) {
        p.println(this::class.java.simpleName, " {")
        p.pushIndent()

        p.popIndent()
        p.println("}")
    }

    override fun toString(): String = "subpackages of $fqName from $moduleDescriptor"
}
