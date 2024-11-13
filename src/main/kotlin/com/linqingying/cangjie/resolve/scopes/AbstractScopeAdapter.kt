package com.linqingying.cangjie.resolve.scopes

import com.linqingying.cangjie.descriptors.*
import com.linqingying.cangjie.descriptors.macro.MacroDescriptor
import com.linqingying.cangjie.incremental.components.LookupLocation
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.resolve.lazy.descriptors.LazyExtendClassDescriptor
import com.linqingying.cangjie.utils.Printer


/**
 * Introduces a simple wrapper for internal scope.
 */
abstract class AbstractScopeAdapter : MemberScope {
    protected abstract val workerScope: MemberScope
    override fun getFunctionNames() = workerScope.getFunctionNames()
    override fun getVariableNames() = workerScope.getVariableNames()
    override fun getClassifierNames() = workerScope.getClassifierNames()
    override fun getPropertyNames() = workerScope.getPropertyNames()
    fun getActualScope(): MemberScope =
        if (workerScope is AbstractScopeAdapter)
            (workerScope as AbstractScopeAdapter).getActualScope()
        else
            workerScope

    override fun getContributedFunctions(name: Name, location: LookupLocation): Collection<SimpleFunctionDescriptor> {
        return workerScope.getContributedFunctions(name, location)
    }

    override fun getContributedMacros(name: Name, location: LookupLocation): Collection<MacroDescriptor> {
        return workerScope.getContributedMacros(name, location)

    }
    override fun getContributedClassifiers(name: Name, location: LookupLocation): List<ClassifierDescriptor> {
        return workerScope.getContributedClassifiers(name, location)
    }
    override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? {
        return workerScope.getContributedClassifier(name, location)
    }
    override fun getExtendClass(name: Name): List<LazyExtendClassDescriptor> {
        return workerScope.getExtendClass(name)

    }
    override fun getContributedVariables(name: Name, location: LookupLocation): Collection<@JvmWildcard VariableDescriptor> {
        return workerScope.getContributedVariables(name, location)
    }
    override fun getContributedPropertys(name: Name, location: LookupLocation): Collection<@JvmWildcard PropertyDescriptor> {
        return workerScope.getContributedPropertys(name, location)
    }
    override fun getContributedDescriptors(kindFilter: DescriptorKindFilter,
                                           nameFilter: (Name) -> Boolean): Collection<DeclarationDescriptor> {
        return workerScope.getContributedDescriptors(kindFilter, nameFilter)
    }

/*
    override fun getFunctionNames() = workerScope.getFunctionNames()
    override fun getVariableNames() = workerScope.getVariableNames()
    override fun getClassifierNames() = workerScope.getClassifierNames()
*/

    override fun recordLookup(name: Name, location: LookupLocation) {
        workerScope.recordLookup(name, location)
    }

    override fun printScopeStructure(p: Printer) {
        p.println(this::class.java.simpleName, " {")
        p.pushIndent()

        p.print("worker =")
        workerScope.printScopeStructure(p.withholdIndentOnce())

        p.popIndent()
        p.println("}")
    }
}
