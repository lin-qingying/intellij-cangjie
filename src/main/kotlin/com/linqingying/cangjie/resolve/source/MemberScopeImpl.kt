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

package com.linqingying.cangjie.resolve.source

import com.linqingying.cangjie.descriptors.*
import com.linqingying.cangjie.descriptors.macro.MacroDescriptor
import com.linqingying.cangjie.incremental.components.LookupLocation
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.resolve.lazy.descriptors.LazyExtendClassDescriptor
import com.linqingying.cangjie.resolve.scopes.DescriptorKindFilter
import com.linqingying.cangjie.resolve.scopes.MemberScope
import com.linqingying.cangjie.utils.Printer
import com.linqingying.cangjie.utils.alwaysTrue
import com.linqingying.cangjie.utils.filterIsInstanceMapTo


abstract class MemberScopeImpl : MemberScope {

    override fun getContributedVariables(
        name: Name,
        location: LookupLocation
    ): Collection<@JvmWildcard VariableDescriptor> =
        emptyList()

    override fun getContributedPropertys(name: Name, location: LookupLocation): Collection<PropertyDescriptor> {
        return emptyList()
    }

    override fun getContributedMacros(name: Name, location: LookupLocation): Collection<MacroDescriptor> {
        return emptyList()

    }
    override fun getContributedFunctions(name: Name, location: LookupLocation): Collection<SimpleFunctionDescriptor> {
        return emptyList()
    }
    override fun getExtendClass(name: Name): List<LazyExtendClassDescriptor> {
return emptyList()
    }

    abstract override fun printScopeStructure(p: Printer)
    override fun getContributedClassifiers(name: Name, location: LookupLocation): List<ClassifierDescriptor>  = emptyList()

//    override fun getFunctionClassDescriptor(parameterCount: Int): FunctionClassDescriptor?  = null
    override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? = null
    override fun getFunctionNames(): Set<Name> =
        getContributedDescriptors(
            DescriptorKindFilter.FUNCTIONS, alwaysTrue()
        ).filterIsInstanceMapTo<SimpleFunctionDescriptor, Name, MutableSet<Name>>(mutableSetOf()) { it.name }

    override fun getVariableNames(): Set<Name> =
        getContributedDescriptors(
            DescriptorKindFilter.VARIABLES, alwaysTrue()
        ).filterIsInstanceMapTo<SimpleFunctionDescriptor, Name, MutableSet<Name>>(mutableSetOf()) { it.name }

    override fun getClassifierNames(): Set<Name>? = null
    override fun getPropertyNames(): Set<Name> {
        return getContributedDescriptors(
            DescriptorKindFilter.PROPERTYS, alwaysTrue()
        ).filterIsInstanceMapTo<SimpleFunctionDescriptor, Name, MutableSet<Name>>(mutableSetOf()) { it.name }
    }

    override fun getContributedDescriptors(
        kindFilter: DescriptorKindFilter,
        nameFilter: (Name) -> Boolean
    ): Collection<DeclarationDescriptor> = emptyList()
}
