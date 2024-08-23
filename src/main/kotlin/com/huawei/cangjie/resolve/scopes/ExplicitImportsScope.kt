package com.huawei.cangjie.resolve.scopes

import com.huawei.cangjie.descriptors.*
import com.huawei.cangjie.incremental.components.LookupLocation
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.utils.Printer
import com.huawei.cangjie.utils.firstIsInstanceOrNull

class ExplicitImportsScope(private val descriptors: Collection<DeclarationDescriptor>) : BaseImportingScope(null) {
    override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? {
        return descriptors.filter { it.name == name }.firstIsInstanceOrNull<ClassifierDescriptor>()
    }

    override fun getContributedPackage(name: Name): PackageViewDescriptor? {
        return descriptors.filter { it.name == name }.firstIsInstanceOrNull<PackageViewDescriptor>()
    }

    override fun getContributedVariables(name: Name, location: LookupLocation): List<VariableDescriptor> {
        return descriptors.filter { it.name == name }.filterIsInstance<VariableDescriptor>()
    }

    override fun getContributedFunctions(name: Name, location: LookupLocation): List<FunctionDescriptor> {
        return descriptors.filter { it.name == name }.filterIsInstance<FunctionDescriptor>()
    }

    override fun getContributedDescriptors(
        kindFilter: DescriptorKindFilter,
        nameFilter: (Name) -> Boolean,
        changeNamesForAliased: Boolean
    ): Collection<DeclarationDescriptor> {
        return descriptors
    }

    override fun computeImportedNames(): HashSet<Name> {
        return descriptors.mapTo(hashSetOf()) { it.name }
    }

    override fun printStructure(p: Printer) {
        p.println(this::class.java.name)
    }
}
