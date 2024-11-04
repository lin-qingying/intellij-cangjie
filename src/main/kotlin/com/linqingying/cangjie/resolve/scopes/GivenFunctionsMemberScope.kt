package com.linqingying.cangjie.resolve.scopes

import com.linqingying.cangjie.descriptors.*
import com.linqingying.cangjie.incremental.components.LookupLocation
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.resolve.NonReportingOverrideStrategy
import com.linqingying.cangjie.resolve.OverridingUtil
import com.linqingying.cangjie.resolve.source.MemberScopeImpl
import com.linqingying.cangjie.storage.StorageManager

import com.linqingying.cangjie.storage.getValue
import com.linqingying.cangjie.utils.Printer
import com.linqingying.cangjie.utils.compact
import com.linqingying.cangjie.utils.filterIsInstanceAnd

/**
 * A scope that may contain some declared functions + fake-overridden functions/properties from supertypes.
 */
abstract class GivenFunctionsMemberScope(
    storageManager: StorageManager,
    protected val containingClass: ClassDescriptor
) : MemberScopeImpl() {
    private val allDescriptors by storageManager.createLazyValue {
        val fromCurrent = computeDeclaredFunctions()
        fromCurrent + createFakeOverrides(fromCurrent)
    }

    protected abstract fun computeDeclaredFunctions(): List<FunctionDescriptor>

    override fun getContributedDescriptors(
        kindFilter: DescriptorKindFilter,
        nameFilter: (Name) -> Boolean
    ): Collection<DeclarationDescriptor> {
        if (!kindFilter.acceptsKinds(DescriptorKindFilter.CALLABLES.kindMask)) return listOf()
        return allDescriptors
    }

    override fun getContributedFunctions(name: Name, location: LookupLocation): Collection<SimpleFunctionDescriptor> {
        return allDescriptors.filterIsInstanceAnd { it.name == name }
    }

    override fun getContributedVariables(name: Name, location: LookupLocation): Collection<@JvmWildcard VariableDescriptor> {
        return allDescriptors.filterIsInstanceAnd { it.name == name }
    }

    private fun createFakeOverrides(functionsFromCurrent: List<FunctionDescriptor>): List<DeclarationDescriptor> {
        val result = ArrayList<DeclarationDescriptor>(3)
        val allSuperDescriptors = containingClass.typeConstructor.supertypes
            .flatMap { it.memberScope.getContributedDescriptors() }
            .filterIsInstance<CallableMemberDescriptor>()
        for ((name, group) in allSuperDescriptors.groupBy { it.name }) {
            for ((isFunction, descriptors) in group.groupBy { it is FunctionDescriptor }) {
                OverridingUtil.DEFAULT.generateOverridesInFunctionGroup(
                    name,
                    /* membersFromSupertypes = */
                    descriptors,
                    /* membersFromCurrent = */
                    if (isFunction) functionsFromCurrent.filter { it.name == name } else listOf(),
                    containingClass,
                    object : NonReportingOverrideStrategy() {
                        override fun addFakeOverride(fakeOverride: CallableMemberDescriptor) {
                            OverridingUtil.resolveUnknownVisibilityForMember(fakeOverride, null)
                            result.add(fakeOverride)
                        }

                        override fun conflict(
                            fromSuper: CallableMemberDescriptor,
                            fromCurrent: CallableMemberDescriptor
                        ) {
                            error("Conflict in scope of $containingClass: $fromSuper vs $fromCurrent")
                        }
                    }
                )
            }
        }

        return result.compact()
    }

    override fun printScopeStructure(p: Printer) {
        p.println("Scope of class: $containingClass")
    }
}
