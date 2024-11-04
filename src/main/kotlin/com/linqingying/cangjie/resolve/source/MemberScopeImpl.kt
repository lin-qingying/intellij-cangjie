package com.linqingying.cangjie.resolve.source

import com.linqingying.cangjie.descriptors.*
import com.linqingying.cangjie.incremental.components.LookupLocation
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.resolve.lazy.descriptors.LazyExtendClassDescriptor
import com.linqingying.cangjie.resolve.scopes.DescriptorKindFilter
import com.linqingying.cangjie.resolve.scopes.MemberScope
import com.linqingying.cangjie.utils.Printer
import com.linqingying.cangjie.utils.alwaysTrue
import com.linqingying.cangjie.utils.filterIsInstanceMapTo


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
    override fun getContributedClassifiers(name: Name, location: LookupLocation): List<ClassifierDescriptor>  = emptyList()

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
