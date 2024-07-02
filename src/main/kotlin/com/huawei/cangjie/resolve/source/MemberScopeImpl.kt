package com.huawei.cangjie.resolve.source

import com.huawei.cangjie.descriptors.ClassifierDescriptor
import com.huawei.cangjie.descriptors.DeclarationDescriptor
import com.huawei.cangjie.descriptors.SimpleFunctionDescriptor
import com.huawei.cangjie.descriptors.VariableDescriptor
import com.huawei.cangjie.incremental.components.LookupLocation
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.resolve.scopes.DescriptorKindFilter
import com.huawei.cangjie.resolve.scopes.MemberScope
import com.huawei.cangjie.utils.Printer


abstract class MemberScopeImpl : MemberScope {

    override fun getContributedVariables(
        name: Name,
        location: LookupLocation
    ): Collection<@JvmWildcard VariableDescriptor> =
        emptyList()

    override fun getContributedFunctions(name: Name, location: LookupLocation): Collection<SimpleFunctionDescriptor> {
        return emptyList()
    }
    abstract override fun printScopeStructure(p: Printer)

    override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? = null

    override fun getContributedDescriptors(
        kindFilter: DescriptorKindFilter,
        nameFilter: (Name) -> Boolean
    ): Collection<DeclarationDescriptor> = emptyList()
}
