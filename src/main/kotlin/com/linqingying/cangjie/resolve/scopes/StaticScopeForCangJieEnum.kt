package com.linqingying.cangjie.resolve.scopes

import com.linqingying.cangjie.descriptors.*
import com.linqingying.cangjie.incremental.components.LookupLocation
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.resolve.source.MemberScopeImpl
import com.linqingying.cangjie.storage.StorageManager
import com.linqingying.cangjie.utils.Printer
import com.intellij.util.SmartList


// We don't need to track lookups here since this scope used only for introduce special Enum class members
class StaticScopeForCangJieEnum(
    storageManager: StorageManager,
    private val containingClass: ClassDescriptor,
    private val enumEntriesCanBeUsed: Boolean,
) : MemberScopeImpl() {
    init {
        assert(containingClass.kind == ClassKind.ENUM) { "Class should be an enum: $containingClass" }
    }

    override fun getContributedClassifier(name: Name, location: LookupLocation) = null // TODO

    //    private val functions: List<SimpleFunctionDescriptor> by storageManager.createLazyValue {
//        listOf(createEnumValueOfMethod(containingClass), createEnumValuesMethod(containingClass))
//    }
//
//    private val properties: List<PropertyDescriptor> by storageManager.createLazyValue {
//        if (enumEntriesCanBeUsed) {
//            // It still might be filtered out later in tower resolve if feature disabled
//            listOfNotNull(createEnumEntriesProperty(containingClass))
//        } else {
//            emptyList()
//        }
//    }
    private val functions: List<SimpleFunctionDescriptor> = emptyList()
    private val properties: List<PropertyDescriptor> = emptyList()
    private val variables: List<VariableDescriptor> = emptyList()

    override fun getContributedDescriptors(kindFilter: DescriptorKindFilter, nameFilter: (Name) -> Boolean) =
        functions + properties

    override fun getContributedFunctions(name: Name, location: LookupLocation) =
        functions.filterTo(SmartList()) { it.name == name }

    override fun getContributedPropertys(name: Name, location: LookupLocation): Collection<PropertyDescriptor> =
        properties.filterTo(SmartList()) { it.name == name }


    override fun getContributedVariables(name: Name, location: LookupLocation): Collection<@JvmWildcard VariableDescriptor> =
        variables.filterTo(SmartList()) { it.name == name }

    override fun printScopeStructure(p: Printer) {
        p.println("Static scope for $containingClass")
    }
}
