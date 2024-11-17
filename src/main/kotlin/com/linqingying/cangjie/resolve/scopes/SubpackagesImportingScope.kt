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
import com.linqingying.cangjie.descriptors.impl.SubpackagesScope
import com.linqingying.cangjie.incremental.components.LookupLocation
import com.linqingying.cangjie.name.FqName
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.utils.Printer


class SubpackagesImportingScope(
    override val parent: ImportingScope?,
    moduleDescriptor: ModuleDescriptor,
    fqName: FqName
) : SubpackagesScope(moduleDescriptor, fqName), ImportingScope by ImportingScope.Empty {

    override fun getContributedPackage(name: Name): PackageViewDescriptor? = getPackage(name)

    override fun printStructure(p: Printer) = printScopeStructure(p)

    override fun getContributedVariables(
        name: Name,
        location: LookupLocation
    ): Collection<@JvmWildcard VariableDescriptor> = super.getContributedVariables(name, location)

    override fun getContributedFunctions(name: Name, location: LookupLocation) =
        super.getContributedFunctions(name, location)

    override fun definitelyDoesNotContainName(name: Name): Boolean {
        return super<SubpackagesScope>.definitelyDoesNotContainName(name)
    }

    override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? {
        return super.getContributedClassifier(name, location)
    }

    override fun getContributedDescriptors(
        kindFilter: DescriptorKindFilter,
        nameFilter: (Name) -> Boolean
    ): Collection<DeclarationDescriptor> {
        return  super<SubpackagesScope>.getContributedDescriptors(kindFilter, nameFilter)
    }


    //TODO: kept old behavior, but it seems very strange (super call seems more applicable)
    override fun getContributedDescriptors(
        kindFilter: DescriptorKindFilter,
        nameFilter: (Name) -> Boolean,
        changeNamesForAliased: Boolean
    ): Collection<DeclarationDescriptor> = emptyList()

    override fun computeImportedNames() = emptySet<Name>()
}
