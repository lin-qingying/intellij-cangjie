package com.huawei.cangjie.resolve.scopes

import com.huawei.cangjie.descriptors.*
import com.huawei.cangjie.descriptors.impl.SubpackagesScope
import com.huawei.cangjie.incremental.components.LookupLocation
import com.huawei.cangjie.name.FqName
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.utils.Printer


class SubpackagesImportingScope(
    override val parent: ImportingScope?,
    moduleDescriptor: ModuleDescriptor,
    fqName: FqName
) : SubpackagesScope(moduleDescriptor, fqName), ImportingScope by ImportingScope.Empty {

    override fun getContributedPackage(name: Name): PackageViewDescriptor? = getPackage(name)

    override fun printStructure(p: Printer) = printScopeStructure(p)

    override fun getContributedVariables(
        name: Name,
        location: LookupLocation
    ): Collection<@JvmWildcard VariableDescriptor> = super.getContributedVariables(name, location)

    override fun getContributedFunctions(name: Name, location: LookupLocation) =
        super.getContributedFunctions(name, location)

    override fun definitelyDoesNotContainName(name: Name): Boolean {
        return super<SubpackagesScope>.definitelyDoesNotContainName(name)
    }

    override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? {
        return super.getContributedClassifier(name, location)
    }

    override fun getContributedDescriptors(
        kindFilter: DescriptorKindFilter,
        nameFilter: (Name) -> Boolean
    ): Collection<DeclarationDescriptor> {
        return  super<SubpackagesScope>.getContributedDescriptors(kindFilter, nameFilter)
    }


    //TODO: kept old behavior, but it seems very strange (super call seems more applicable)
    override fun getContributedDescriptors(
        kindFilter: DescriptorKindFilter,
        nameFilter: (Name) -> Boolean,
        changeNamesForAliased: Boolean
    ): Collection<DeclarationDescriptor> = emptyList()

    override fun computeImportedNames() = emptySet<Name>()
}
