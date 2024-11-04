package com.linqingying.cangjie.types.error

import com.linqingying.cangjie.descriptors.*
import com.linqingying.cangjie.incremental.components.LookupLocation
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.resolve.lazy.descriptors.LazyExtendClassDescriptor
import com.linqingying.cangjie.resolve.scopes.DescriptorKindFilter
import com.linqingying.cangjie.resolve.scopes.MemberScope
import com.linqingying.cangjie.types.ErrorUtils
import com.linqingying.cangjie.utils.Printer


open class ErrorScope(val kind: ErrorScopeKind, vararg formatParams: String) : MemberScope {
    protected val debugMessage = kind.debugMessage.format(*formatParams)

    override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor =
        ErrorClassDescriptor(Name.special(ErrorEntity.ERROR_CLASS.debugText.format(name)))

    override fun getExtendClass(name: Name): List<LazyExtendClassDescriptor> {
        return emptyList()
    }

    override fun getContributedClassifierIncludeDeprecated(
        name: Name, location: LookupLocation
    ): DescriptorWithDeprecation<ClassifierDescriptor>? = null

    override fun getContributedVariables(name: Name, location: LookupLocation): Collection<@JvmWildcard VariableDescriptor> =
        ErrorUtils.errorVariableGroup

    override fun getContributedPropertys (name: Name, location: LookupLocation): Set<PropertyDescriptor> =
        ErrorUtils.errorPropertyGroup

    override fun getContributedFunctions(name: Name, location: LookupLocation): Set<SimpleFunctionDescriptor> =
        setOf(ErrorFunctionDescriptor(ErrorUtils.errorClass))

    override fun getContributedDescriptors(
        kindFilter: DescriptorKindFilter, nameFilter: Function1<Name, Boolean>
    ): Collection<DeclarationDescriptor> = emptyList()

    override fun getFunctionNames(): Set<Name> = emptySet()
    override fun getVariableNames(): Set<Name> = emptySet()
    override fun getClassifierNames(): Set<Name> = emptySet()
    override fun getPropertyNames(): Set<Name> = emptySet()
    override fun recordLookup(name: Name, location: LookupLocation) {}
    override fun definitelyDoesNotContainName(name: Name): Boolean = false

    override fun toString(): String = "ErrorScope{$debugMessage}"

    override fun printScopeStructure(p: Printer) {
        p.println(javaClass.simpleName, ": ", debugMessage)
    }
}
