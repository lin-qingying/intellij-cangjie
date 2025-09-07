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

package org.cangnova.cangjie.resolve.scopes

import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.descriptors.macro.MacroDescriptor
import org.cangnova.cangjie.incremental.components.LookupLocation
import org.cangnova.cangjie.incremental.components.NoLookupLocation
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

interface ResolutionScope {
    /**
     * Returns only non-deprecated classifiers.
     *
     * See [getContributedClassifierIncludeDeprecated] to get all classifiers.
     */
    fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor?
    fun getContributedPackageView(name: Name, location: LookupLocation): PackageViewDescriptor? = null

    fun getContributedClassifiers(name: Name, location: LookupLocation): List<ClassifierDescriptor> = emptyList()
    fun getContributedEnumEntrys(name: Name, location: LookupLocation): List<ClassifierDescriptor> = emptyList()


    /**
     * Returns contributed classifier, but discriminates deprecated
     *
     * This method can return some classifier where [getContributedClassifier] haven't returned any,
     * but it should never return different one, even if it is deprecated.
     * Note that implementors are encouraged to provide non-deprecated classifier if it doesn't contradict
     * contract above.
     */
    fun getContributedClassifierIncludeDeprecated(
        name: Name,
        location: LookupLocation
    ): DescriptorWithDeprecation<ClassifierDescriptor>? =
        getContributedClassifier(name, location)?.let { DescriptorWithDeprecation.createNonDeprecated(it) }

    fun getContributedClassifierIncludeDeprecateds(
        name: Name,
        location: LookupLocation
    ): List<DescriptorWithDeprecation<ClassifierDescriptor>>? =
        getContributedClassifiers(name, location).map { DescriptorWithDeprecation.createNonDeprecated(it) }

    fun getContributedVariables(name: Name, location: LookupLocation): Collection<@JvmWildcard VariableDescriptor>
    fun getContributedPropertys(name: Name, location: LookupLocation): Collection<@JvmWildcard PropertyDescriptor>
    fun getContributedMacros(name: Name, location: LookupLocation): Collection<@JvmWildcard MacroDescriptor>
    {
        return emptyList()
    }
    fun getContributedFunctions(name: Name, location: LookupLocation): Collection<@JvmWildcard FunctionDescriptor>
    fun getContributedPackages(
        name: Name,
        location: LookupLocation
    ): Collection<@JvmWildcard PackageFragmentDescriptor> = emptyList()

    /**
     * All visible descriptors from current scope possibly filtered by the given name and kind filters
     * (that means that the implementation is not obliged to use the filters but may do so when it gives any performance advantage).
     */
    fun getContributedDescriptors(
        kindFilter: DescriptorKindFilter = DescriptorKindFilter.ALL,
        nameFilter: (Name) -> Boolean = MemberScope.ALL_NAME_FILTER
    ): Collection<DeclarationDescriptor>

    fun definitelyDoesNotContainName(name: Name): Boolean = false

    /**
     * 记录指定名称的查找操作
     *
     * 该函数旨在记录在特定位置对指定名称进行的查找操作它通过调用[getContributedFunctions]
     * 来获取与该名称相关的函数信息
     *
     * @param name 要查找的名称，通常是一个函数或变量名
     * @param location 查找操作发生的地点，用于提供上下文信息
     */
    fun recordLookup(name: Name, location: LookupLocation) {
        getContributedFunctions(name, location)
    }


    /**
     * 通过   [org.cangnova.cangjie.name.Name] 查找文件中导入的完限定包名
     * return [org.cangnova.cangjie.name.FqName]
     */
    fun getContributedPackageFqName(name: Name/*, location: LookupLocation*/): List<FqName>? {
        return null
    }

    /**
     * 仅适用与反序列化
     */
    fun getContributedClassifierByIndex(
        index: Int,
        location: LookupLocation = NoLookupLocation.FROM_DESERIALIZATION
    ): ClassifierDescriptor? {
        return null
    }
    fun getContributedClassifierByExportId(
        exportId: String,
        location: LookupLocation = NoLookupLocation.FROM_DESERIALIZATION
    ): ClassifierDescriptor? {
        return null
    }

}
