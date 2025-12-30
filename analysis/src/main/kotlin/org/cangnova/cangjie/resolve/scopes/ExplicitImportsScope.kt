/*
 * Copyright 2025 LinQingYing. and contributors.
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

package org.cangnova.cangjie.resolve.scopes

import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.descriptors.macro.MacroDescriptor
import org.cangnova.cangjie.incremental.components.LookupLocation
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.utils.Printer
import org.cangnova.cangjie.utils.firstIsInstanceOrNull

class ExplicitImportsScope(private val descriptors: Collection<DeclarationDescriptor>) : BaseImportingScope(null) {
    override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? {
        return descriptors.filter { it.name == name }.firstIsInstanceOrNull<ClassifierDescriptor>()
    }

    override fun getContributedPackage(name: Name): PackageViewDescriptor? {
        return descriptors.filter { it.name == name }.firstIsInstanceOrNull<PackageViewDescriptor>()
    }

    override fun getContributedVariables(
        name: Name,
        location: LookupLocation
    ): Collection<@JvmWildcard VariableDescriptor> {
        return descriptors.filter { it.name == name }.filterIsInstance<VariableDescriptor>()
    }

    override fun getContributedFunctions(name: Name, location: LookupLocation): List<FunctionDescriptor> {
        return descriptors.filter { it.name == name }.filterIsInstance<FunctionDescriptor>()
    }

    override fun getContributedMacros(name: Name, location: LookupLocation): Collection<MacroDescriptor> {
        return descriptors.filter { it.name == name }.filterIsInstance<MacroDescriptor>()
    }



    override fun getContributedDescriptors(
        kindFilter: DescriptorKindFilter,
        nameFilter: (Name) -> Boolean,
        changeNamesForAliased: Boolean
    ): Collection<DeclarationDescriptor> {
        return descriptors
    }

    override fun computeImportedNames(): HashSet<Name> {
        return descriptors.mapTo(hashSetOf()) { it.name }
    }

    override fun printStructure(p: Printer) {
        p.println(this::class.java.name)
    }


}
