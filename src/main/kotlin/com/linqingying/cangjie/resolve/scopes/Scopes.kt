package com.linqingying.cangjie.resolve.scopes

import com.linqingying.cangjie.descriptors.*
import com.linqingying.cangjie.descriptors.macro.MacroDescriptor
import com.linqingying.cangjie.incremental.components.LookupLocation
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.resolve.lazy.descriptors.LazyExtendClassDescriptor
import com.linqingying.cangjie.utils.Printer

interface HierarchicalScope : ResolutionScope {
    val parent: HierarchicalScope?

    fun printStructure(p: Printer)
}

enum class LexicalScopeKind(val withLocalDescriptors: Boolean) {
    EMPTY(false),
    THROWING(false),

    CLASS_HEADER(false),
    CLASS_INHERITANCE(false),
    CONSTRUCTOR_HEADER(false),
    CLASS_STATIC_SCOPE(false),
    CLASS_MEMBER_SCOPE(false),
    CLASS_INITIALIZER(true),

    DEFAULT_VALUE(true),

    PROPERTY_HEADER(false),
    VARIABLE_INITIALIZER_OR_DELEGATE(true),
    PROPERTY_ACCESSOR_BODY(true),
    PROPERTY_DELEGATE_METHOD(false),
    EXTEND_HEADER(false),

    FUNCTION_HEADER(false),
    FUNCTION_HEADER_FOR_DESTRUCTURING(false),
    FUNCTION_INNER_SCOPE(true),

    TYPE_ALIAS_HEADER(false),

    CODE_BLOCK(true),

    LEFT_BOOLEAN_EXPRESSION(true),
    RIGHT_BOOLEAN_EXPRESSION(true),
    WHILE(true),

    THEN(true),
    ELSE(true),
    DO_WHILE_BODY(true),
    CATCH(true),
    TRY(true),
    IF(true),

    FOR(true),
    WHILE_BODY(true),
    MATCH(true),
    MATCH_CASE(true),

    CALLABLE_REFERENCE(false),

    // for tests, CDoc & IDE
    SYNTHETIC(false)
}

abstract class BaseHierarchicalScope(override val parent: HierarchicalScope?) : HierarchicalScope {
    override fun getContributedDescriptors(
        kindFilter: DescriptorKindFilter,
        nameFilter: (Name) -> Boolean
    ): Collection<DeclarationDescriptor> = emptyList()

    //    override fun getFunctionClassDescriptor(parameterCount: Int): FunctionClassDescriptor? {
//        return null
//    }
    override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? = null
    override fun getContributedPackages(name: Name, location: LookupLocation): Collection<PackageFragmentDescriptor> =
        emptyList()

    override fun getExtendClass(name: Name): List<LazyExtendClassDescriptor> {
        return emptyList()
    }

    override fun getContributedVariables(
        name: Name,
        location: LookupLocation
    ): Collection<@JvmWildcard VariableDescriptor> =
        emptyList()

    override fun getContributedPropertys(name: Name, location: LookupLocation): Collection<PropertyDescriptor> =
        emptyList()

    override fun getContributedMacros(name: Name, location: LookupLocation): Collection<MacroDescriptor> {
        return emptyList()
    }
    override fun getContributedFunctions(name: Name, location: LookupLocation): Collection<FunctionDescriptor> =
        emptyList()
}

interface LexicalScope : HierarchicalScope {
    override val parent: HierarchicalScope

    val ownerDescriptor: DeclarationDescriptor
    val isOwnerDescriptorAccessibleByLabel: Boolean

    val implicitReceiver: ReceiverParameterDescriptor?
    val contextReceiversGroup: List<ReceiverParameterDescriptor>

    val kind: LexicalScopeKind
    fun addVariableDescriptor(variableDescriptor: VariableDescriptor) {

    }

    class Base(
        parent: HierarchicalScope,
        override val ownerDescriptor: DeclarationDescriptor
    ) : BaseHierarchicalScope(parent), LexicalScope {
        override val parent: HierarchicalScope
            get() = super.parent!!

        override val isOwnerDescriptorAccessibleByLabel: Boolean
            get() = false

        override val implicitReceiver: ReceiverParameterDescriptor?
            get() = null
        override val contextReceiversGroup: List<ReceiverParameterDescriptor>
            get() = emptyList()

        override val kind: LexicalScopeKind
            get() = LexicalScopeKind.EMPTY

//        override fun getContributedPackageFqName(name: Name, location: LookupLocation): List<FqName> {
//            return mutableListOf()
//        }
//        override fun definitelyDoesNotContainName(name: Name) = true

        override fun printStructure(p: Printer) {
            p.println("Base lexical scope with owner = $ownerDescriptor and parent = $parent")
        }


    }
}

interface ImportingScope : HierarchicalScope {
    override val parent: ImportingScope?

    fun getContributedPackage(name: Name): PackageViewDescriptor?


    fun getContributedDescriptors(
        kindFilter: DescriptorKindFilter = DescriptorKindFilter.ALL,
        nameFilter: (Name) -> Boolean = MemberScope.ALL_NAME_FILTER,
        changeNamesForAliased: Boolean
    ): Collection<DeclarationDescriptor>

    override fun getContributedDescriptors(
        kindFilter: DescriptorKindFilter,
        nameFilter: (Name) -> Boolean
    ): Collection<DeclarationDescriptor> {
        return getContributedDescriptors(kindFilter, nameFilter, changeNamesForAliased = false)
    }

    fun computeImportedNames(): Set<Name>?

    object Empty : BaseImportingScope(null) {
        override fun printStructure(p: Printer) {
            p.println("ImportingScope.Empty")
        }

        override fun computeImportedNames() = emptySet<Name>()

        override fun definitelyDoesNotContainName(name: Name) = true
    }
}

abstract class BaseImportingScope(parent: ImportingScope?) : BaseHierarchicalScope(parent), ImportingScope {
    override val parent: ImportingScope?
        get() = super.parent as ImportingScope?

    override fun getContributedPackage(name: Name): PackageViewDescriptor? = null

    override fun getContributedPackages(name: Name, location: LookupLocation): Collection<PackageFragmentDescriptor> {
        return emptyList()
    }


    override fun getContributedDescriptors(
        kindFilter: DescriptorKindFilter,
        nameFilter: (Name) -> Boolean
    ): Collection<DeclarationDescriptor> {
        return getContributedDescriptors(kindFilter, nameFilter, changeNamesForAliased = false)
    }

    override fun getContributedDescriptors(
        kindFilter: DescriptorKindFilter,
        nameFilter: (Name) -> Boolean,
        changeNamesForAliased: Boolean
    ): Collection<DeclarationDescriptor> = emptyList()
}

inline fun HierarchicalScope.processForMeAndParent(process: (HierarchicalScope) -> Unit) {
    var currentScope = this
    while (true) {
        process(currentScope)
        currentScope = currentScope.parent ?: break
    }
}

inline fun <T : Any> HierarchicalScope.getListFromMeAndParent(fetch: (HierarchicalScope) -> List<T>?): List<T> {
    val result = mutableListOf<T>()
    processForMeAndParent { fetch(it)?.let { result.addAll(it) } }

    return result
}

inline fun <T : Any> HierarchicalScope.findFirstFromMeAndParent(fetch: (HierarchicalScope) -> T?): T? {
    processForMeAndParent { fetch(it)?.let { return it } }
    return null
}


class CompositePrioritizedImportingScope(
    private val primaryScope: ImportingScope,
    private val secondaryScope: ImportingScope,
) : ImportingScope {
    override val parent: ImportingScope?
        get() = primaryScope.parent ?: secondaryScope.parent

    override fun getContributedPackage(name: Name): PackageViewDescriptor? {
        return primaryScope.getContributedPackage(name) ?: secondaryScope.getContributedPackage(name)
    }

    //
//    override fun getFunctionClassDescriptor(parameterCount: Int): FunctionClassDescriptor? {
//return null
//    }
    override fun getContributedDescriptors(
        kindFilter: DescriptorKindFilter,
        nameFilter: (Name) -> Boolean,
        changeNamesForAliased: Boolean
    ): Collection<DeclarationDescriptor> {
        return primaryScope.getContributedDescriptors(kindFilter, nameFilter, changeNamesForAliased).union(
            secondaryScope.getContributedDescriptors(kindFilter, nameFilter, changeNamesForAliased)
        )
    }

    override fun getExtendClass(name: Name): List<LazyExtendClassDescriptor> {
        return primaryScope.getExtendClass(name) + (secondaryScope.getExtendClass(
            name,

            ))
    }

    override fun computeImportedNames(): Set<Name>? {
        val primaryNames = primaryScope.computeImportedNames()
        val secondaryNames = secondaryScope.computeImportedNames()
        return primaryNames?.union(secondaryNames.orEmpty()) ?: secondaryNames
    }

    override fun printStructure(p: Printer) {
        p.println(primaryScope::class.java.simpleName)
    }

    override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? {
        return primaryScope.getContributedClassifier(name, location) ?: secondaryScope.getContributedClassifier(
            name,
            location
        )
    }

    override fun getContributedVariables(
        name: Name,
        location: LookupLocation
    ): Collection<@JvmWildcard VariableDescriptor> {
        return primaryScope.getContributedVariables(name, location).union(
            secondaryScope.getContributedVariables(name, location)
        )
    }

    override fun getContributedPropertys(name: Name, location: LookupLocation): Collection<PropertyDescriptor> {
        return primaryScope.getContributedPropertys(name, location).union(
            secondaryScope.getContributedPropertys(name, location)
        )
    }

    override fun getContributedMacros(name: Name, location: LookupLocation): Collection<MacroDescriptor> {
        return primaryScope.getContributedMacros(name, location).union(
            secondaryScope.getContributedMacros(name, location)
        )
    }
    override fun getContributedFunctions(name: Name, location: LookupLocation): Collection<FunctionDescriptor> {
        return primaryScope.getContributedFunctions(name, location).union(
            secondaryScope.getContributedFunctions(name, location)
        )
    }

    override fun getContributedPackages(name: Name, location: LookupLocation): Collection<PackageFragmentDescriptor> {
        TODO("Not yet implemented")
    }
}
