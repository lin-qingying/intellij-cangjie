/*
 * Copyright 2024 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

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
