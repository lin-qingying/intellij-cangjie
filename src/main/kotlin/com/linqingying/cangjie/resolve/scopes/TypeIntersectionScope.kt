package com.linqingying.cangjie.resolve.scopes

import com.linqingying.cangjie.descriptors.CallableDescriptor
import com.linqingying.cangjie.descriptors.DeclarationDescriptor
import com.linqingying.cangjie.descriptors.VariableDescriptor
import com.linqingying.cangjie.incremental.components.LookupLocation
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.resolve.selectMostSpecificInEachOverridableGroup
import com.linqingying.cangjie.types.CangJieType
import com.linqingying.cangjie.utils.Printer


class TypeIntersectionScope private constructor(private val debugName: String, override val workerScope: MemberScope) : AbstractScopeAdapter() {
    override fun getContributedFunctions(name: Name, location: LookupLocation) =
        super.getContributedFunctions(name, location).selectMostSpecificInEachOverridableGroup { this }

    override fun getContributedVariables(name: Name, location: LookupLocation): Collection<@JvmWildcard VariableDescriptor> =
        super.getContributedVariables(name, location).selectMostSpecificInEachOverridableGroup { this }

    override fun getContributedDescriptors(kindFilter: DescriptorKindFilter, nameFilter: (Name) -> Boolean): Collection<DeclarationDescriptor> {
        val (callables, other) = super.getContributedDescriptors(kindFilter, nameFilter).partition { it is CallableDescriptor }

        @Suppress("UNCHECKED_CAST")
        return (callables as Collection<CallableDescriptor>).selectMostSpecificInEachOverridableGroup { this } + other
    }

    override fun printScopeStructure(p: Printer) {
        p.print("TypeIntersectionScope for: " + debugName)
        super.printScopeStructure(p)
    }

    companion object {
        @JvmStatic
        fun create(message: String, types: Collection<CangJieType>): MemberScope {
            val nonEmptyScopes = listOfNonEmptyScopes(types.map { it.memberScope })
            val chainedOrSingle = ChainedMemberScope.createOrSingle(message, nonEmptyScopes)

            if (nonEmptyScopes.size <= 1) return chainedOrSingle

            return TypeIntersectionScope(message, chainedOrSingle)
        }
    }
}
