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
