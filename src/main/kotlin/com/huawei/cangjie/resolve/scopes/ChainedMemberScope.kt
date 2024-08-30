package com.huawei.cangjie.resolve.scopes

import com.huawei.cangjie.descriptors.*
import com.huawei.cangjie.incremental.components.LookupLocation
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.utils.Printer
import com.intellij.util.SmartList


class ChainedMemberScope private constructor(
    private val debugName: String,
    private val scopes: Array<out MemberScope>
) : MemberScope {
    override fun getFunctionNames() = scopes.flatMapTo(mutableSetOf()) { it.getFunctionNames() }
    override fun getVariableNames() = scopes.flatMapTo(mutableSetOf()) { it.getVariableNames() }
    override fun getPropertyNames()  = scopes.flatMapTo(mutableSetOf()) { it.getPropertyNames() }
    override fun getClassifierNames(): Set<Name>? = scopes.asIterable().flatMapClassifierNamesOrNull()
    override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? =
        getFirstClassifierDiscriminateHeaders(scopes) { it.getContributedClassifier(name, location) }

    override fun getExtendClass(name: Name): List<ClassDescriptor> =
        getListClassifierDiscriminateHeaders(scopes) { it.getExtendClass(name ) }

    override fun getContributedVariables(
        name: Name,
        location: LookupLocation
    ): Collection<@JvmWildcard VariableDescriptor> =
        getFromAllScopes(scopes) { it.getContributedVariables(name, location) }

    override fun getContributedPropertys(name: Name, location: LookupLocation): Collection<PropertyDescriptor> =
        getFromAllScopes(scopes) { it.getContributedPropertys(name, location) }

    override fun getContributedFunctions(name: Name, location: LookupLocation): Collection<SimpleFunctionDescriptor> =
        getFromAllScopes(scopes) { it.getContributedFunctions(name, location) }

    override fun getContributedDescriptors(kindFilter: DescriptorKindFilter, nameFilter: (Name) -> Boolean) =
        getFromAllScopes(scopes) { it.getContributedDescriptors(kindFilter, nameFilter) }

//    override fun getFunctionNames() = scopes.flatMapTo(mutableSetOf()) { it.getFunctionNames() }
//    override fun getVariableNames() = scopes.flatMapTo(mutableSetOf()) { it.getVariableNames() }
//    override fun getClassifierNames(): Set<Name>? = scopes.asIterable().flatMapClassifierNamesOrNull()

    override fun recordLookup(name: Name, location: LookupLocation) {
        scopes.forEach { it.recordLookup(name, location) }
    }

    override fun toString() = debugName

    //
    override fun printScopeStructure(p: Printer) {
        p.println(this::class.java.simpleName, ": ", debugName, " {")
        p.pushIndent()

        for (scope in scopes) {
            scope.printScopeStructure(p)
        }

        p.popIndent()
        p.println("}")
    }

    companion object {
        fun create(debugName: String, vararg scopes: MemberScope): MemberScope = create(debugName, scopes.asIterable())

        fun create(debugName: String, scopes: Iterable<MemberScope>): MemberScope {
            val flattenedNonEmptyScopes = SmartList<MemberScope>()
            for (scope in scopes) {
                when {
                    scope === MemberScope.Empty -> {}
                    scope is ChainedMemberScope -> flattenedNonEmptyScopes.addAll(scope.scopes)
                    else -> flattenedNonEmptyScopes.add(scope)
                }
            }
            return createOrSingle(debugName, flattenedNonEmptyScopes)
        }

        internal fun createOrSingle(debugName: String, scopes: List<MemberScope>): MemberScope =
            when (scopes.size) {
                0 -> MemberScope.Empty
                1 -> scopes[0]
                else -> ChainedMemberScope(debugName, scopes.toTypedArray())
            }
    }
}
