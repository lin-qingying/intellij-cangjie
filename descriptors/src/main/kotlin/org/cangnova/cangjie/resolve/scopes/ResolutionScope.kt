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

/**
 * 解析作用域接口 - 仓颉语言符号解析系统的核心抽象
 *
 * 该接口定义了符号解析的基础能力，是仓颉语言类型系统和名称解析机制的关键组件。
 * 作用域（Scope）表示一个可以查找声明符号（如类、函数、变量等）的命名空间环境。
 *
 * ## 核心职责
 *
 * 1. **符号查找**: 根据名称查找各类符号描述符（分类器、函数、变量、属性等）
 * 2. **包管理**: 支持包视图和完全限定名的查找
 * 3. **序列化支持**: 提供基于索引和导出ID的反序列化查找
 * 4. **弃用处理**: 区分普通符号和弃用符号的查找
 * 5. **查找记录**: 支持增量编译的查找位置记录
 *
 * ## 架构层次
 *
 * ```
 * ResolutionScope (基础接口)
 *   ↓
 * MemberScope (成员作用域 - 添加名称集合属性)
 *   ↓
 * MemberScopeImpl (抽象基类 - 提供默认实现)
 *   ↓
 * 各种具体作用域实现:
 *   - ChainedMemberScope (链式组合作用域)
 *   - SubstitutingScope (类型替换作用域)
 *   - StaticMemberScope (静态成员作用域)
 *   - InstanceMemberScope (实例成员作用域)
 *   - BuiltInsMemberScope (内置类型作用域)
 *   - ErrorScope (错误恢复作用域)
 *   等等...
 * ```
 *
 * ## 使用场景
 *
 * 1. **类型解析**: 在类型检查时解析类型引用
 * 2. **名称解析**: 在代码分析时解析标识符引用
 * 3. **代码补全**: 在IDE中提供符号补全建议
 * 4. **重构支持**: 在重命名等重构操作中查找符号使用
 * 5. **导航功能**: 在跳转定义、查找引用等功能中定位符号
 *
 * ## 设计原则
 *
 * - **懒加载**: 查找操作应尽可能延迟计算，避免不必要的性能开销
 * - **缓存友好**: 实现应考虑与 IntelliJ 的缓存系统（如 CachedValuesManager）集成
 * - **不可变性**: 作用域应是不可变的，其内容由其包含的描述符决定
 * - **增量支持**: 通过 [recordLookup] 支持增量编译的依赖追踪
 *
 * ## 查找语义
 *
 * ### 单一符号查找
 * - [getContributedClassifier]: 返回第一个匹配的非弃用分类器
 * - [getContributedPackageView]: 返回第一个匹配的包视图
 *
 * ### 多符号收集
 * - [getContributedFunctions]: 收集所有同名函数（支持重载）
 * - [getContributedVariables]: 收集所有同名变量
 * - [getContributedPropertys]: 收集所有同名属性
 * - [getContributedDescriptors]: 根据类型和名称过滤器收集所有符号
 *
 * ### 弃用处理
 * - 默认查找方法返回非弃用符号
 * - `IncludeDeprecated` 后缀的方法返回包括弃用符号的结果
 * - 弃用信息通过 [DescriptorWithDeprecation] 包装
 *
 * ## 反序列化支持
 *
 * 编译后的元数据通过索引或导出ID引用符号，相关方法支持从元数据反序列化：
 * - [getContributedClassifierByIndex]: 通过数字索引查找（用于紧凑的二进制格式）
 * - [getContributedClassifierByExportId]: 通过导出ID查找（用于跨模块引用）
 *
 * ## 增量编译支持
 *
 * - [recordLookup]: 记录查找操作，用于增量编译的依赖分析
 * - [LookupLocation]: 标记查找发生的代码位置，用于精确的失效检测
 *
 * ## 性能优化建议
 *
 * 1. **使用 Stub 索引**: 对于文件级符号，通过 Stub 索引加速查找
 * 2. **惰性计算**: 延迟构建作用域内容，直到真正需要时
 * 3. **避免全量遍历**: 实现 [definitelyDoesNotContainName] 快速排除不存在的名称
 * 4. **缓存策略**: 对于重复查找，使用 CachedValuesManager 缓存结果
 *
 * ## 示例
 *
 * ```kotlin
 * // 在作用域中查找类
 * val classDescriptor = scope.getContributedClassifier(
 *     Name.identifier("MyClass"),
 *     location
 * )
 *
 * // 查找所有同名函数（支持重载）
 * val functions = scope.getContributedFunctions(
 *     Name.identifier("processData"),
 *     location
 * )
 *
 * // 过滤查找所有可调用符号
 * val callables = scope.getContributedDescriptors(
 *     kindFilter = DescriptorKindFilter.CALLABLES,
 *     nameFilter = { it.asString().startsWith("get") }
 * )
 * ```
 *
 * @see MemberScope 扩展接口，添加名称集合访问能力
 * @see LookupLocation 查找位置标记，用于增量编译
 * @see DescriptorKindFilter 描述符类型过滤器
 * @see DescriptorWithDeprecation 包含弃用信息的描述符包装
 */
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
