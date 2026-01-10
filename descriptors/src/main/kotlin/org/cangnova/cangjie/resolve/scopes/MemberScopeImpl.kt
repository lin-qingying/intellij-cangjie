/*
 * Copyright 2026 LinQingYing. and contributors.
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
import org.cangnova.cangjie.utils.alwaysTrue
import org.cangnova.cangjie.utils.filterIsInstanceMapTo

/**
 * MemberScope 的抽象基类实现 - 提供默认实现和模板方法模式支持
 *
 * 该类为 [MemberScope] 接口提供了便利的默认实现，使得具体作用域类只需覆盖
 * 它们实际支持的方法，而其他方法将返回空结果。这遵循了模板方法设计模式，
 * 大大简化了新作用域实现的编写。
 *
 * ## 默认实现策略
 *
 * ### 1. 符号查找方法 - 默认返回空
 *
 * - [getContributedVariables]: 返回空列表
 * - [getContributedPropertys]: 返回空列表
 * - [getContributedMacros]: 返回空列表
 * - [getContributedFunctions]: 返回空列表
 * - [getContributedClassifier]: 返回 null
 * - [getContributedClassifiers]: 返回空列表
 * - [getContributedDescriptors]: 返回空列表
 *
 * ### 2. 名称集合属性 - 通过 getContributedDescriptors 计算
 *
 * 默认实现通过调用 [getContributedDescriptors] 并过滤特定类型来计算名称集合：
 * - [functionNames]: 从所有描述符中过滤出函数名称
 * - [variableNames]: 从所有描述符中过滤出变量名称
 * - [propertyNames]: 从所有描述符中过滤出属性名称
 * - [classifierNames]: 返回 null（表示不支持分类器）
 *
 * **性能注意**: 这种默认实现可能效率较低，具体子类应覆盖这些属性以提供更高效的实现。
 *
 * ### 3. 调试方法 - 必须覆盖
 *
 * - [printScopeStructure]: 抽象方法，子类必须实现
 *
 * ## 使用场景
 *
 * ### 场景 1: 只读空作用域
 *
 * ```kotlin
 * class MyEmptyScope : MemberScopeImpl() {
 *     override fun printScopeStructure(p: Printer) {
 *         p.println("MyEmptyScope")
 *     }
 * }
 * // 所有查找方法自动返回空结果
 * ```
 *
 * ### 场景 2: 仅支持函数的作用域
 *
 * ```kotlin
 * class FunctionOnlyScope(private val functions: List<SimpleFunctionDescriptor>) : MemberScopeImpl() {
 *     override fun getContributedFunctions(name: Name, location: LookupLocation) =
 *         functions.filter { it.name == name }
 *
 *     override val functionNames: Set<Name>
 *         get() = functions.mapTo(mutableSetOf()) { it.name }
 *
 *     override fun printScopeStructure(p: Printer) {
 *         p.println("FunctionOnlyScope with ${functions.size} functions")
 *     }
 * }
 * // 变量、属性等查找自动返回空结果
 * ```
 *
 * ### 场景 3: 基于描述符列表的作用域
 *
 * ```kotlin
 * class DescriptorListScope(private val descriptors: List<DeclarationDescriptor>) : MemberScopeImpl() {
 *     override fun getContributedDescriptors(kindFilter: DescriptorKindFilter, nameFilter: (Name) -> Boolean) =
 *         descriptors.filter { kindFilter.accepts(it) && nameFilter(it.name) }
 *
 *     override fun printScopeStructure(p: Printer) {
 *         p.println("DescriptorListScope with ${descriptors.size} descriptors")
 *     }
 * }
 * // 名称集合属性自动通过 getContributedDescriptors 计算
 * ```
 *
 * ## 性能优化建议
 *
 * 虽然默认实现提供了便利，但对于性能敏感的作用域，建议覆盖以下内容：
 *
 * ### 1. 覆盖名称集合属性
 *
 * 默认实现通过扫描所有描述符计算名称集合，效率较低：
 *
 * ```kotlin
 * // 默认实现（低效）
 * override val functionNames: Set<Name>
 *     get() = getContributedDescriptors(DescriptorKindFilter.FUNCTIONS, alwaysTrue())
 *         .filterIsInstanceMapTo<SimpleFunctionDescriptor, Name, MutableSet<Name>>(mutableSetOf()) { it.name }
 *
 * // 推荐覆盖（高效）
 * override val functionNames: Set<Name>
 *     get() = cachedFunctionNames // 使用缓存或直接访问内部数据结构
 * ```
 *
 * ### 2. 实现 definitelyDoesNotContainName
 *
 * 对于大型作用域，提供快速排除方法可以避免不必要的查找：
 *
 * ```kotlin
 * override fun definitelyDoesNotContainName(name: Name): Boolean {
 *     // 使用 Bloom Filter 或名称集合快速判断
 *     return !allPossibleNames.contains(name)
 * }
 * ```
 *
 * ### 3. 使用惰性计算和缓存
 *
 * ```kotlin
 * private val _functionNames: Set<Name> by lazy {
 *     // 惰性计算并缓存
 *     computeFunctionNames()
 * }
 *
 * override val functionNames: Set<Name>
 *     get() = _functionNames
 * ```
 *
 * ## 继承层次
 *
 * ```
 * ResolutionScope (接口)
 *   ↓
 * MemberScope (接口)
 *   ↓
 * MemberScopeImpl (抽象基类) ← 你在这里
 *   ↓
 * 具体实现类:
 *   - ChainedMemberScope
 *   - SubstitutingScope
 *   - StaticMemberScope
 *   - InstanceMemberScope
 *   - BuiltInsMemberScope
 *   - ErrorScope
 *   - MemberScope.Empty
 *   等等...
 * ```
 *
 * ## 实现checklist
 *
 * 当创建新的 MemberScopeImpl 子类时，考虑以下内容：
 *
 * - [ ] **必须**: 实现 [printScopeStructure] 用于调试
 * - [ ] **推荐**: 覆盖实际支持的查找方法（`getContributedXxx`）
 * - [ ] **推荐**: 覆盖对应的名称集合属性以提高性能
 * - [ ] **可选**: 实现 [definitelyDoesNotContainName] 优化查找
 * - [ ] **可选**: 覆盖 [getContributedDescriptors] 提供完整的描述符列表
 *
 * ## 与其他基类的比较
 *
 * - **MemberScopeImpl**: 通用基类，适合大多数场景
 * - **直接实现 MemberScope**: 适合需要完全自定义行为的场景
 * - **继承专用基类**: 如 SubstitutingScope、StaticMemberScope 等，适合特定模式
 *
 * @see MemberScope 父接口，定义完整的成员作用域契约
 * @see ResolutionScope 祖先接口，定义基础符号解析能力
 * @see ChainedMemberScope 组合多个作用域的实现示例
 * @see MemberScope.Empty 空作用域的单例实现示例
 */
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


    abstract override fun printScopeStructure(p: Printer)
    override fun getContributedClassifiers(name: Name, location: LookupLocation): List<ClassifierDescriptor> =
        emptyList()

    //    override fun getFunctionClassDescriptor(parameterCount: Int): FunctionClassDescriptor?  = null
    override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? = null
    override val functionNames: Set<Name>
        get() =
            getContributedDescriptors(
                DescriptorKindFilter.FUNCTIONS, alwaysTrue()
            ).filterIsInstanceMapTo<SimpleFunctionDescriptor, Name, MutableSet<Name>>(mutableSetOf()) { it.name }


    override val classifierNames : Set<Name>? = null
    override val variableNames: Set<Name>
        get() =  getContributedDescriptors(
            DescriptorKindFilter.VARIABLES, alwaysTrue()
        ).filterIsInstanceMapTo<SimpleFunctionDescriptor, Name, MutableSet<Name>>(mutableSetOf()) { it.name }


    override val propertyNames: Set<Name>
        get() = getContributedDescriptors(
            DescriptorKindFilter.PROPERTYS, alwaysTrue()
        ).filterIsInstanceMapTo<SimpleFunctionDescriptor, Name, MutableSet<Name>>(mutableSetOf()) { it.name }


    override fun getContributedDescriptors(
        kindFilter: DescriptorKindFilter,
        nameFilter: (Name) -> Boolean
    ): Collection<DeclarationDescriptor> = emptyList()
}
