package com.huawei.cangjie.resolve.scopes

import com.huawei.cangjie.descriptors.ClassifierDescriptor
import com.huawei.cangjie.descriptors.SimpleFunctionDescriptor
import com.huawei.cangjie.descriptors.VariableDescriptor
import com.huawei.cangjie.incremental.components.LookupLocation
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.utils.Printer
import com.huawei.cangjie.utils.SmartList


class ChainedMemberScope private constructor(
    private val debugName: String,
    private val scopes: Array<out MemberScope>
) : MemberScope {

    override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? =
        getFirstClassifierDiscriminateHeaders(scopes) { it.getContributedClassifier(name, location) }

    override fun getContributedVariables(name: Name, location: LookupLocation): Collection<VariableDescriptor> =
        getFromAllScopes(scopes) { it.getContributedVariables(name, location) }

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
