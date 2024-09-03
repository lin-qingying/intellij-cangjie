package com.huawei.cangjie.resolve.source

import com.huawei.cangjie.descriptors.*
import com.huawei.cangjie.descriptors.impl.FunctionClassDescriptor
import com.huawei.cangjie.incremental.components.LookupLocation
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.resolve.lazy.descriptors.LazyExtendClassDescriptor
import com.huawei.cangjie.resolve.scopes.DescriptorKindFilter
import com.huawei.cangjie.resolve.scopes.MemberScope
import com.huawei.cangjie.utils.Printer
import com.huawei.cangjie.utils.alwaysTrue
import com.huawei.cangjie.utils.filterIsInstanceMapTo


abstract class MemberScopeImpl : MemberScope {

    override fun getContributedVariables(
        name: Name,
        location: LookupLocation
    ): Collection<@JvmWildcard VariableDescriptor> =
        emptyList()

    override fun getContributedPropertys(name: Name, location: LookupLocation): Collection<PropertyDescriptor> {
        return emptyList()
    }

    override fun getContributedFunctions(name: Name, location: LookupLocation): Collection<SimpleFunctionDescriptor> {
        return emptyList()
    }
    override fun getExtendClass(name: Name): List<LazyExtendClassDescriptor> {
return emptyList()
    }

    abstract override fun printScopeStructure(p: Printer)

//    override fun getFunctionClassDescriptor(parameterCount: Int): FunctionClassDescriptor?  = null
    override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? = null
    override fun getFunctionNames(): Set<Name> =
        getContributedDescriptors(
            DescriptorKindFilter.FUNCTIONS, alwaysTrue()
        ).filterIsInstanceMapTo<SimpleFunctionDescriptor, Name, MutableSet<Name>>(mutableSetOf()) { it.name }

    override fun getVariableNames(): Set<Name> =
        getContributedDescriptors(
            DescriptorKindFilter.VARIABLES, alwaysTrue()
        ).filterIsInstanceMapTo<SimpleFunctionDescriptor, Name, MutableSet<Name>>(mutableSetOf()) { it.name }

    override fun getClassifierNames(): Set<Name>? = null
    override fun getPropertyNames(): Set<Name> {
        return getContributedDescriptors(
            DescriptorKindFilter.PROPERTYS, alwaysTrue()
        ).filterIsInstanceMapTo<SimpleFunctionDescriptor, Name, MutableSet<Name>>(mutableSetOf()) { it.name }
    }

    override fun getContributedDescriptors(
        kindFilter: DescriptorKindFilter,
        nameFilter: (Name) -> Boolean
    ): Collection<DeclarationDescriptor> = emptyList()
}
