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

import com.intellij.util.SmartList
import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.descriptors.macro.MacroDescriptor
import org.cangnova.cangjie.incremental.components.LookupLocation
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.utils.Printer

/**
 * 链式成员作用域实现，用于组合多个 [MemberScope] 形成统一的查找视图
 *
 * 该类实现了组合模式（Composite Pattern），将多个作用域链接成一个逻辑作用域。
 * 在符号解析时，会按顺序遍历所有内部作用域，收集或返回匹配的符号。
 *
 * ## 使用场景
 *
 * 1. **类继承作用域组合**: 将类自身成员作用域与父类成员作用域链接，实现继承语义
 * 2. **嵌套作用域查找**: 组合局部作用域、类作用域、包作用域，实现从内向外的符号查找
 * 3. **多包导入**: 合并多个导入语句引入的符号作用域
 *
 * ## 实现特点
 *
 * - **自动扁平化**: 通过 [create] 工厂方法创建时，会自动展开嵌套的 ChainedMemberScope，避免深层嵌套
 * - **过滤空作用域**: 创建时会自动移除 [MemberScope.Empty]，提高查找效率
 * - **分类器去重**: 对于类型查找（classifier），使用 `getFirstClassifierDiscriminateHeaders` 进行去重，
 *   避免头文件和实现文件的重复符号
 * - **惰性计算名称集合**: 各类名称集合（functionNames, variableNames等）采用惰性计算，
 *   仅在需要时才遍历所有作用域
 *
 * ## 查找策略
 *
 * - **单一符号查找**: 对于包视图和分类器，返回第一个找到的符号（短路求值）
 * - **多符号收集**: 对于函数、变量、属性等，收集所有作用域中的匹配符号（支持重载）
 *
 * ## 示例
 *
 * ```kotlin
 * // 组合类作用域和父类作用域
 * val combinedScope = ChainedMemberScope.create(
 *     "MyClass with Parent",
 *     myClassScope,
 *     parentClassScope
 * )
 *
 * // 查找函数时会从两个作用域中收集所有同名函数（支持重载）
 * val functions = combinedScope.getContributedFunctions(name, location)
 * ```
 *
 * @property debugName 调试名称，用于日志输出和作用域结构打印
 * @property scopes 内部作用域数组，查找时按顺序遍历
 *
 * @see MemberScope
 * @see create 推荐使用的工厂方法，自动优化作用域结构
 */
class ChainedMemberScope private constructor(
    private val debugName: String,
    private val scopes: Array<out MemberScope>
) : MemberScope {
    override val functionNames: Set<Name>
        get() = scopes.flatMapTo(mutableSetOf()) { it.functionNames }
    override val variableNames: Set<Name>
        get() = scopes.flatMapTo(mutableSetOf()) { it.variableNames }
    override val propertyNames: Set<Name>
        get() = scopes.flatMapTo(mutableSetOf()) { it.propertyNames }
    override val classifierNames: Set<Name>?
        get() = scopes.asIterable().flatMapClassifierNamesOrNull()

    override fun getContributedPackageView(name: Name, location: LookupLocation): PackageViewDescriptor? =
        getFirstFromAllScopes(scopes) { it.getContributedPackageView(name, location) }

    override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? =
        getFirstClassifierDiscriminateHeaders(scopes) { it.getContributedClassifier(name, location) }

    override fun getContributedClassifiers(name: Name, location: LookupLocation): List<ClassifierDescriptor> =
        getListClassifierDiscriminateHeaders(scopes) { it.getContributedClassifiers(name, location) }


    override fun getContributedClassifierByIndex(index: Int, location: LookupLocation): ClassifierDescriptor? =
        getFirstClassifierDiscriminateHeaders(scopes) { it.getContributedClassifierByIndex(index, location) }

    override fun getContributedClassifierByExportId(exportId: String, location: LookupLocation): ClassifierDescriptor? =
        getFirstClassifierDiscriminateHeaders(scopes) { it.getContributedClassifierByExportId(exportId, location) }

    override fun getContributedVariables(
        name: Name,
        location: LookupLocation
    ): Collection<@JvmWildcard VariableDescriptor> =
        getFromAllScopes(scopes) { it.getContributedVariables(name, location) }

    override fun getContributedPropertys(name: Name, location: LookupLocation): Collection<PropertyDescriptor> =
        getFromAllScopes(scopes) { it.getContributedPropertys(name, location) }

    override fun getContributedMacros(name: Name, location: LookupLocation): Collection<MacroDescriptor> =
        getFromAllScopes(scopes) { it.getContributedMacros(name, location) }

    override fun getContributedFunctions(name: Name, location: LookupLocation): Collection<SimpleFunctionDescriptor> =
        getFromAllScopes(scopes) { it.getContributedFunctions(name, location) }

    override fun getContributedDescriptors(kindFilter: DescriptorKindFilter, nameFilter: (Name) -> Boolean) =
        getFromAllScopes(scopes) { it.getContributedDescriptors(kindFilter, nameFilter) }

//    override fun getFunctionNames() = scopes.flatMapTo(mutableSetOf()) { it.getFunctionNames() }
//    override fun getVariableNames() = scopes.flatMapTo(mutableSetOf()) { it.getVariableNames() }
//    override fun classifierNames(): Set<Name>? = scopes.asIterable().flatMapClassifierNamesOrNull()

    override fun recordLookup(name: Name, location: LookupLocation) {
        scopes.forEach { it.recordLookup(name, location) }
    }

    override fun toString() = debugName

    //
    override fun printScopeStructure(p: Printer) {
        p.println(this::class.java.simpleName, ": ", debugName, " {")
        p.pushIndent()

        for (scope in scopes) {
            scope.printScopeStructure(p)
        }

        p.popIndent()
        p.println("}")
    }

    companion object {
        /**
         * 创建链式作用域的工厂方法（可变参数版本）
         *
         * @param debugName 调试名称，建议使用描述性字符串如 "MyClass with Parents"
         * @param scopes 要组合的作用域（可变参数）
         * @return 优化后的作用域实例：
         *   - 如果所有作用域都为空，返回 [MemberScope.Empty]
         *   - 如果只有一个非空作用域，直接返回该作用域（避免不必要的包装）
         *   - 否则返回 ChainedMemberScope 实例
         */
        fun create(debugName: String, vararg scopes: MemberScope): MemberScope = create(debugName, scopes.asIterable())

        /**
         * 创建链式作用域的工厂方法（集合版本）
         *
         * 该方法会自动优化作用域结构：
         * 1. **过滤空作用域**: 移除所有 [MemberScope.Empty] 实例
         * 2. **扁平化嵌套链**: 展开内部的 ChainedMemberScope，避免链式调用的深层嵌套
         * 3. **优化单例情况**: 如果最终只有一个作用域，直接返回该作用域而非包装
         *
         * 这些优化能够显著提升符号解析性能，避免不必要的遍历层级。
         *
         * @param debugName 调试名称，用于作用域结构打印和日志输出
         * @param scopes 要组合的作用域集合
         * @return 优化后的作用域实例
         *
         * @see createOrSingle 内部使用的优化方法
         */
        fun create(debugName: String, scopes: Iterable<MemberScope>): MemberScope {
            val flattenedNonEmptyScopes = SmartList<MemberScope>()
            for (scope in scopes) {
                when {
                    scope === MemberScope.Empty -> {}
                    scope is ChainedMemberScope -> flattenedNonEmptyScopes.addAll(scope.scopes)
                    else -> flattenedNonEmptyScopes.add(scope)
                }
            }
            return createOrSingle(debugName, flattenedNonEmptyScopes)
        }

        /**
         * 根据作用域列表大小选择最优返回值的内部方法
         *
         * 该方法实现了以下优化策略：
         * - 空列表 → 返回 [MemberScope.Empty] 单例，避免创建无意义的对象
         * - 单元素列表 → 直接返回该元素，避免单层包装的性能开销
         * - 多元素列表 → 创建 ChainedMemberScope 实例
         *
         * @param debugName 调试名称
         * @param scopes 已优化的作用域列表（不含空作用域和嵌套链）
         * @return 最优的作用域实例
         */
        internal fun createOrSingle(debugName: String, scopes: List<MemberScope>): MemberScope =
            when (scopes.size) {
                0 -> MemberScope.Empty
                1 -> scopes[0]
                else -> ChainedMemberScope(debugName, scopes.toTypedArray())
            }
    }
}
