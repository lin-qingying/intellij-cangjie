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
import org.cangnova.cangjie.descriptors.extend.ExtendDescriptor
import org.cangnova.cangjie.descriptors.macro.MacroDescriptor
import org.cangnova.cangjie.incremental.components.LookupLocation
import org.cangnova.cangjie.incremental.components.NoLookupLocation
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

interface ResolutionScope {
    /**
     * 获取非弃用的分类器
     *
     * 参见 [getContributedClassifierIncludeDeprecated] 获取所有分类器
     */
    fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor?

    /**
     * 获取贡献的包视图
     */
    fun getContributedPackageView(name: Name, location: LookupLocation): PackageViewDescriptor? = null

    /**
     * 获取指定名称的所有分类器列表
     */
    fun getContributedClassifiers(name: Name, location: LookupLocation): List<ClassifierDescriptor> = emptyList()

    /**
     * 根据扩展ID获取扩展描述符
     */
    fun getContributedExtend(extendId: String, location: LookupLocation): ExtendDescriptor? = null


    /**
     * 获取贡献的分类器，包括弃用的分类器
     *
     * 此方法可能返回 [getContributedClassifier] 未返回的分类器，
     * 但不应返回不同的分类器，即使它已被弃用。
     * 建议实现者在不违反上述约定的情况下提供非弃用的分类器。
     */
    fun getContributedClassifierIncludeDeprecated(
        name: Name,
        location: LookupLocation
    ): DescriptorWithDeprecation<ClassifierDescriptor>? =
        getContributedClassifier(name, location)?.let { DescriptorWithDeprecation.createNonDeprecated(it) }

    /**
     * 获取指定名称的所有分类器列表，包括弃用的分类器
     */
    fun getContributedClassifierIncludeDeprecateds(
        name: Name,
        location: LookupLocation
    ): List<DescriptorWithDeprecation<ClassifierDescriptor>>? =
        getContributedClassifiers(name, location).map { DescriptorWithDeprecation.createNonDeprecated(it) }

    /**
     * 获取指定名称的变量描述符集合
     */
    fun getContributedVariables(name: Name, location: LookupLocation): Collection<@JvmWildcard VariableDescriptor>

    /**
     * 获取指定名称的属性描述符集合
     */
    fun getContributedPropertys(name: Name, location: LookupLocation): Collection<@JvmWildcard PropertyDescriptor>

    /**
     * 获取指定名称的宏描述符集合
     */
    fun getContributedMacros(name: Name, location: LookupLocation): Collection<@JvmWildcard MacroDescriptor>
    {
        return emptyList()
    }

    /**
     * 获取指定名称的函数描述符集合
     */
    fun getContributedFunctions(name: Name, location: LookupLocation): Collection<@JvmWildcard FunctionDescriptor>


    /**
     * 从当前作用域获取所有可见的描述符，可能通过给定的名称和类型过滤器进行过滤
     * （这意味着实现不一定要使用过滤器，但在能够提供性能优势时可以使用）
     */
    fun getContributedDescriptors(
        kindFilter: DescriptorKindFilter = DescriptorKindFilter.ALL,
        nameFilter: (Name) -> Boolean = MemberScope.ALL_NAME_FILTER
    ): Collection<DeclarationDescriptor>

    /**
     * 确定是否绝对不包含指定名称
     */
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
     * 根据索引获取分类器描述符
     * 仅适用于反序列化
     */
    fun getContributedClassifierByIndex(
        index: Int,
        location: LookupLocation = NoLookupLocation.FROM_DESERIALIZATION
    ): ClassifierDescriptor? {
        return null
    }

    /**
     * 根据导出ID获取分类器描述符
     * 仅适用于反序列化
     */
    fun getContributedClassifierByExportId(
        exportId: String,
        location: LookupLocation = NoLookupLocation.FROM_DESERIALIZATION
    ): ClassifierDescriptor? {
        return null
    }

}
