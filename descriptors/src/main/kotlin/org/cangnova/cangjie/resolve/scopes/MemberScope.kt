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
import org.cangnova.cangjie.incremental.components.LookupLocation
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.resolve.scopes.MemberScope.Companion.ALL_NAME_FILTER
import org.cangnova.cangjie.utils.Printer
import org.cangnova.cangjie.utils.flatMapToNullable
import java.lang.reflect.Modifier

/**
 * 计算所有名称的集合
 * 将函数名、变量名和分类器名合并到一个可变集合中
 */
fun MemberScope.computeAllNames() = classifierNames?.let { classifierNames ->
    functionNames.toMutableSet().also {
        it.addAll(variableNames)
        it.addAll(classifierNames)
    }
}

/**
 * 与 getDescriptors(kindFilter, nameFilter) 相同，但结果保证按类型和名称过滤
 * The same as getDescriptors(kindFilter, nameFilter) but the result is guaranteed to be filtered by kind and name.
 */
fun MemberScope.getDescriptorsFiltered(
    kindFilter: DescriptorKindFilter = DescriptorKindFilter.ALL,
    nameFilter: (Name) -> Boolean = ALL_NAME_FILTER
): Collection<DeclarationDescriptor> {
    if (kindFilter.kindMask == 0) return listOf()
    return getContributedDescriptors(kindFilter, nameFilter).filter { kindFilter.accepts(it) && nameFilter(it.name) }
}

/**
 * 将多个 MemberScope 的分类器名称平铺映射为一个可变集合，如果没有则返回 null
 */
fun Iterable<MemberScope>.flatMapClassifierNamesOrNull(): MutableSet<Name>? =
    flatMapToNullable(hashSetOf(), MemberScope::classifierNames)


/**
 * 成员作用域接口 - 提供成员符号查找和名称集合访问的完整能力
 *
 * MemberScope 是 [ResolutionScope] 的核心扩展，添加了对成员名称集合的快速访问能力。
 * 该接口是仓颉语言类型系统中最常用的作用域类型，用于表示类、包、文件等包含成员的作用域。
 *
 * ## 与 ResolutionScope 的区别
 *
 * - **ResolutionScope**: 仅提供基于名称的查找方法（`getContributedXxx(name, location)`）
 * - **MemberScope**: 额外提供名称集合属性（`functionNames`, `variableNames` 等），支持快速遍历
 *
 * ## 核心能力
 *
 * ### 1. 名称集合访问（MemberScope 特有）
 *
 * 提供快速访问各类符号名称集合的属性：
 * - [functionNames]: 所有函数名称集合
 * - [variableNames]: 所有变量名称集合
 * - [classifierNames]: 所有分类器（类、接口等）名称集合（可为 null 表示不支持）
 * - [propertyNames]: 所有属性名称集合
 *
 * **注意**: 这些集合可能返回实际名称的超集（即包含一些实际不存在的名称），
 * 用于性能优化，避免完整扫描。实际查找仍需通过 `getContributedXxx` 验证。
 *
 * ### 2. 符号查找（继承自 ResolutionScope）
 *
 * 提供精确的符号查找方法，详见 [ResolutionScope] 文档。
 *
 * ### 3. 作用域结构调试
 *
 * - [printScopeStructure]: 打印作用域的层次结构，用于调试和测试
 *
 * ## 使用场景
 *
 * 1. **类成员作用域**: 表示类的成员（字段、方法、嵌套类等）
 * 2. **包作用域**: 表示包中的顶层声明
 * 3. **文件作用域**: 表示文件中的所有声明
 * 4. **局部作用域**: 表示函数或代码块中的局部变量
 * 5. **导入作用域**: 表示通过导入语句引入的符号
 *
 * ## 特殊实现
 *
 * ### MemberScope.Empty
 *
 * 空作用域单例，表示不包含任何成员的作用域：
 * - 所有名称集合都返回空集
 * - 所有查找方法都返回空结果
 * - [definitelyDoesNotContainName] 总是返回 true
 * - 用于优化空作用域的创建和比较（单例模式）
 *
 * ## 设计模式
 *
 * ### 模板方法模式
 *
 * MemberScopeImpl 提供基础实现，子类可以选择性覆盖：
 * ```kotlin
 * abstract class MyScope : MemberScopeImpl() {
 *     // 只需覆盖特定方法
 *     override fun getContributedFunctions(...) = ...
 * }
 * ```
 *
 * ### 组合模式
 *
 * ChainedMemberScope 将多个作用域组合成一个逻辑作用域：
 * ```kotlin
 * val combinedScope = ChainedMemberScope.create(
 *     "Class with Parents",
 *     classScope,
 *     parentScope
 * )
 * ```
 *
 * ### 装饰器模式
 *
 * SubstitutingScope 在现有作用域上应用类型替换：
 * ```kotlin
 * val substitutedScope = SubstitutingScope(
 *     baseScope,
 *     typeSubstitutor
 * )
 * ```
 *
 * ## 性能优化
 *
 * ### 名称集合的超集语义
 *
 * 名称集合属性允许返回超集（包含不存在的名称），这样可以：
 * - 避免完整扫描作用域内容
 * - 使用廉价的近似计算（如从 Stub 索引快速获取）
 * - 实际验证延迟到 `getContributedXxx` 调用时
 *
 * ### 示例
 *
 * ```kotlin
 * // 可能包含不存在的函数名（性能优化）
 * val functionNames = scope.functionNames
 *
 * // 遍历时需要实际查找验证
 * for (name in functionNames) {
 *     val functions = scope.getContributedFunctions(name, location)
 *     // functions 可能为空
 * }
 * ```
 *
 * ## 辅助函数
 *
 * ### computeAllNames
 * 合并所有类型的名称为一个集合（函数、变量、分类器）
 *
 * ### getDescriptorsFiltered
 * 获取经过类型和名称过滤的描述符，保证结果符合过滤器条件
 *
 * ### flatMapClassifierNamesOrNull
 * 将多个作用域的分类器名称合并，如果任何作用域不支持分类器则返回 null
 *
 * ## 实现建议
 *
 * 1. **惰性计算名称集合**: 延迟构建，缓存结果
 * 2. **快速失败**: 实现 [definitelyDoesNotContainName] 快速排除明显不存在的名称
 * 3. **Stub 索引集成**: 对于 PSI 基础作用域，利用 Stub 索引加速
 * 4. **缓存策略**: 对于重复查找，使用 CachedValuesManager
 *
 * ## 示例
 *
 * ```kotlin
 * // 获取所有函数名
 * val allFunctionNames = scope.functionNames
 *
 * // 查找特定函数
 * val processFunctions = scope.getContributedFunctions(
 *     Name.identifier("process"),
 *     location
 * )
 *
 * // 获取所有可调用符号
 * val callables = scope.getDescriptorsFiltered(
 *     kindFilter = DescriptorKindFilter.CALLABLES
 * )
 *
 * // 组合多个作用域的分类器名称
 * val allClassifierNames = listOf(scope1, scope2, scope3)
 *     .flatMapClassifierNamesOrNull()
 * ```
 *
 * @see ResolutionScope 父接口，定义基础符号查找能力
 * @see MemberScopeImpl 抽象基类，提供默认实现
 * @see ChainedMemberScope 组合多个作用域的实现
 * @see computeAllNames 合并所有名称的辅助函数
 * @see getDescriptorsFiltered 过滤查找的辅助函数
 */
interface MemberScope : ResolutionScope {
    override fun getContributedVariables(
        name: Name,
        location: LookupLocation
    ): Collection<@JvmWildcard VariableDescriptor>

    override fun getContributedPropertys(
        name: Name,
        location: LookupLocation
    ): Collection<@JvmWildcard PropertyDescriptor>

    /**
     * 这些方法可能返回实际名称集合的超集
     * These methods may return a superset of an actual names' set
     */
    val functionNames: Set<Name>  // 函数名称集合
    val variableNames: Set<Name>  // 变量名称集合
    val classifierNames: Set<Name>?  // 分类器名称集合（可为空）
    val propertyNames: Set<Name>  // 属性名称集合

    override fun getContributedFunctions(
        name: Name,
        location: LookupLocation
    ): Collection<@JvmWildcard SimpleFunctionDescriptor>

    companion object {
        // 接受所有名称的过滤器
        val ALL_NAME_FILTER: (Name) -> Boolean = { true }
    }

    /**
     * 仅供测试和调试使用
     * Is supposed to be used in tests and debug only
     */
    fun printScopeStructure(p: Printer)

    /**
     * 空成员作用域对象，不包含任何成员
     */
    object Empty : MemberScopeImpl() {
        override fun printScopeStructure(p: Printer) {
            p.println("Empty member scope")
        }

        override fun definitelyDoesNotContainName(name: Name): Boolean = true
        override val propertyNames: Set<Name>
            get() = emptySet<Name>()
        override val functionNames: Set<Name>
            get() = emptySet<Name>()

        override val classifierNames: Set<Name>?
            get() = emptySet()
        override val variableNames = emptySet<Name>()

    }
}


