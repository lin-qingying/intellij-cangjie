package com.linqingying.cangjie.resolve.scopes

import com.linqingying.cangjie.descriptors.*
import com.linqingying.cangjie.incremental.components.LookupLocation
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.resolve.lazy.descriptors.LazyExtendClassDescriptor
import com.intellij.util.SmartList

interface LocalRedeclarationChecker {
    fun checkBeforeAddingToScope(scope: LexicalScope, newDescriptor: DeclarationDescriptor)

    object DO_NOTHING : LocalRedeclarationChecker {
        override fun checkBeforeAddingToScope(scope: LexicalScope, newDescriptor: DeclarationDescriptor) {}
    }
}

abstract class LexicalScopeStorage(
    parent: HierarchicalScope,
    val redeclarationChecker: LocalRedeclarationChecker
) : LexicalScope {
    private class IntList(val last: Int, val prev: IntList?)


    override val parent = parent.takeSnapshot()

    protected val addedDescriptors: MutableList<DeclarationDescriptor> = SmartList()

    private var functionsByName: MutableMap<Name, IntList>? = null
    private var variablesAndClassifiersByName: MutableMap<Name, IntList>? = null

    override fun getContributedClassifier(name: Name, location: LookupLocation) =
        variableOrClassDescriptorByName(name) as? ClassifierDescriptor


    override fun getExtendClass(name: Name): List<LazyExtendClassDescriptor> {

     return  listOfNotNull(variableOrClassDescriptorByName(name) as? LazyExtendClassDescriptor)
    }


    override fun getContributedVariables(name: Name, location: LookupLocation): Collection<@JvmWildcard VariableDescriptor> =
        listOfNotNull(variableOrClassDescriptorByName(name) as? VariableDescriptor)


    override fun getContributedPropertys(name: Name, location: LookupLocation) =
        listOfNotNull(variableOrClassDescriptorByName(name) as? PropertyDescriptor)

    override fun getContributedFunctions(name: Name, location: LookupLocation) = functionsByName(name)

    override fun getContributedDescriptors(kindFilter: DescriptorKindFilter, nameFilter: (Name) -> Boolean) =
        addedDescriptors

    protected fun addVariableOrClassDescriptor(descriptor: DeclarationDescriptor) {
        val name = descriptor.name
        if (name.isSpecial) return
        val descriptorIndex = addDescriptor(descriptor)

        if (variablesAndClassifiersByName == null) {
            variablesAndClassifiersByName = HashMap()
        }

        variablesAndClassifiersByName!![name] = variablesAndClassifiersByName!![name] + descriptorIndex

    }

    protected fun addFunctionDescriptorInternal(functionDescriptor: FunctionDescriptor) {
        val name = functionDescriptor.name
        val descriptorIndex = addDescriptor(functionDescriptor)
        if (functionsByName == null) {
            functionsByName = HashMap(1)
        }

        functionsByName!![name] = functionsByName!![name] + descriptorIndex
    }

    protected fun variableOrClassDescriptorByName(
        name: Name,
        descriptorLimit: Int = addedDescriptors.size
    ): DeclarationDescriptor? {
       if (descriptorLimit == 0) return null

        var list = variablesAndClassifiersByName?.get(name)
        while (list != null) {
            val descriptorIndex = list.last
            if (descriptorIndex < descriptorLimit) {
                return descriptorIndex.descriptorByIndex()
            }
            list = list.prev
        }
        return null
    }

    protected fun functionsByName(name: Name, descriptorLimit: Int = addedDescriptors.size): List<FunctionDescriptor> {
        if (descriptorLimit == 0) return emptyList()

        var list = functionsByName?.get(name)
        while (list != null) {
            if (list.last < descriptorLimit) {
                return list.toDescriptors<FunctionDescriptor>()
            }
            list = list.prev
        }
        return emptyList()
    }

    private fun addDescriptor(descriptor: DeclarationDescriptor): Int {
        redeclarationChecker.checkBeforeAddingToScope(this, descriptor)
        addedDescriptors.add(descriptor)
        return addedDescriptors.size - 1
    }

    private fun Int.descriptorByIndex() = addedDescriptors[this]

    private operator fun IntList?.plus(value: Int) = IntList(value, this)

    private fun <TDescriptor : DeclarationDescriptor> IntList.toDescriptors(): List<TDescriptor> {
        val result = ArrayList<TDescriptor>(1)
        var rest: IntList? = this
        do {
            @Suppress("UNCHECKED_CAST")
            result.add(rest!!.last.descriptorByIndex() as TDescriptor)
            rest = rest.prev
        } while (rest != null)
        return result
    }

    override fun definitelyDoesNotContainName(name: Name) =
        functionsByName?.get(name) == null && variablesAndClassifiersByName?.get(name) == null
}
