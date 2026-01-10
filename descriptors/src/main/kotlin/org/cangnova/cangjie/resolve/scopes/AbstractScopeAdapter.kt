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


/**
 * 抽象作用域适配器 - 提供作用域委托的基础实现
 *
 * AbstractScopeAdapter 是一个抽象基类，实现了适配器（Adapter）和委托（Delegate）模式。
 * 它将所有 [MemberScope] 接口方法的调用委托给内部的 [workerScope]，为子类提供一个
 * 简单的扩展点来包装或修改现有作用域的行为。
 *
 * ## 核心设计
 *
 * ### 完全委托模式
 *
 * 所有接口方法都直接委托给 [workerScope]：
 * - 查询方法：`getContributedXxx` → `workerScope.getContributedXxx`
 * - 名称集合：`functionNames` → `workerScope.functionNames`
 * - 其他方法：`recordLookup` → `workerScope.recordLookup`
 *
 * ### 适配器展开
 *
 * [getActualScope] 方法递归展开嵌套的适配器，获取真正的底层作用域：
 *
 * ```kotlin
 * fun getActualScope(): MemberScope =
 *     if (workerScope is AbstractScopeAdapter)
 *         (workerScope as AbstractScopeAdapter).getActualScope()
 *     else
 *         workerScope
 * ```
 *
 * 这避免了多层适配器嵌套导致的性能问题。
 *
 * ## 使用场景
 *
 * ### 1. 延迟初始化作用域
 *
 * [LazyScopeAdapter] 使用此基类实现延迟加载：
 *
 * ```kotlin
 * class LazyScopeAdapter(
 *     storageManager: StorageManager,
 *     getScope: () -> MemberScope
 * ) : AbstractScopeAdapter() {
 *     private val lazyScope = storageManager.createLazyValue { getScope() }
 *     override val workerScope: MemberScope get() = lazyScope()
 * }
 * ```
 *
 * ### 2. 作用域装饰器
 *
 * 为现有作用域添加额外功能（如日志、监控）：
 *
 * ```kotlin
 * class LoggingScope(
 *     private val delegate: MemberScope
 * ) : AbstractScopeAdapter() {
 *     override val workerScope = delegate
 *
 *     override fun getContributedFunctions(...) {
 *         log("Looking up functions: $name")
 *         return super.getContributedFunctions(name, location)
 *     }
 * }
 * ```
 *
 * ### 3. 作用域转换
 *
 * 在保持接口不变的情况下转换底层实现：
 *
 * ```kotlin
 * class CachingAdapter(
 *     private val original: MemberScope
 * ) : AbstractScopeAdapter() {
 *     override val workerScope = CachedScope(original)
 * }
 * ```
 *
 * ## 子类实现指南
 *
 * ### 最小实现
 *
 * 子类只需实现一个属性：
 *
 * ```kotlin
 * class MyAdapter(private val scope: MemberScope) : AbstractScopeAdapter() {
 *     override val workerScope: MemberScope = scope
 * }
 * ```
 *
 * ### 可选覆盖
 *
 * 如果需要修改特定行为，可以覆盖相应方法：
 *
 * ```kotlin
 * override fun getContributedFunctions(...) {
 *     // 自定义逻辑
 *     val functions = super.getContributedFunctions(name, location)
 *     // 后处理
 *     return functions.filter { customFilter(it) }
 * }
 * ```
 *
 * ## 实现细节
 *
 * ### 属性委托
 *
 * 名称集合属性使用 Kotlin 的自定义 getter 委托：
 *
 * ```kotlin
 * override val functionNames: Set<Name>
 *     get() = workerScope.functionNames
 * ```
 *
 * 每次访问都会查询 workerScope，支持动态作用域。
 *
 * ### 方法委托
 *
 * 所有方法都是简单的转发调用：
 *
 * ```kotlin
 * override fun getContributedFunctions(name: Name, location: LookupLocation) =
 *     workerScope.getContributedFunctions(name, location)
 * ```
 *
 * ### 递归展开
 *
 * [getActualScope] 递归处理嵌套适配器：
 *
 * ```
 * Adapter1 (AbstractScopeAdapter)
 *   → Adapter2 (AbstractScopeAdapter)
 *       → Adapter3 (AbstractScopeAdapter)
 *           → RealScope (实际作用域)
 *
 * getActualScope() 直接返回 RealScope
 * ```
 *
 * ## 性能考虑
 *
 * ### 零开销抽象
 *
 * 由于是简单的委托调用，JVM 的即时编译器（JIT）可以内联这些方法，
 * 使得适配器在运行时几乎没有性能开销。
 *
 * ### 避免深层嵌套
 *
 * [getActualScope] 帮助避免多层适配器嵌套：
 *
 * ```kotlin
 * // 在 LazyScopeAdapter 中使用
 * private val lazyScope = storageManager.createLazyValue {
 *     getScope().let {
 *         if (it is AbstractScopeAdapter) it.getActualScope() else it
 *     }
 * }
 * ```
 *
 * ## 与其他模式的对比
 *
 * | 模式 | AbstractScopeAdapter | SubstitutingScope | ChainedMemberScope |
 * |------|----------------------|-------------------|---------------------|
 * | 设计模式 | 适配器/委托 | 装饰器 | 组合 |
 * | 底层作用域数量 | 1个 | 1个 | 多个 |
 * | 修改行为 | 可选 | 必须（类型替换） | 无（纯组合） |
 * | 典型用途 | 包装、延迟加载 | 泛型实例化 | 继承、多作用域 |
 *
 * ## 调试支持
 *
 * ### printScopeStructure
 *
 * 提供递归的作用域结构打印：
 *
 * ```kotlin
 * override fun printScopeStructure(p: Printer) {
 *     p.println(this::class.java.simpleName, " {")
 *     p.pushIndent()
 *     p.print("worker =")
 *     workerScope.printScopeStructure(p.withholdIndentOnce())
 *     p.popIndent()
 *     p.println("}")
 * }
 * ```
 *
 * 输出示例：
 * ```
 * LazyScopeAdapter {
 *   worker = ChainedMemberScope: MyClass {
 *     ...
 *   }
 * }
 * ```
 *
 * ## 注意事项
 *
 * 1. **workerScope 必须提供**: 子类必须实现此抽象属性
 * 2. **避免循环引用**: workerScope 不应直接或间接引用自身
 * 3. **线程安全**: 如果 workerScope 是延迟初始化的，需确保线程安全
 * 4. **透明性**: 适配器应该对外部透明，不改变语义
 *
 * ## 典型子类
 *
 * - [LazyScopeAdapter]: 延迟初始化作用域
 * - 其他自定义适配器实现
 *
 * ## 示例：完整的适配器实现
 *
 * ```kotlin
 * // 示例：为作用域添加访问计数
 * class CountingScope(
 *     private val delegate: MemberScope
 * ) : AbstractScopeAdapter() {
 *     override val workerScope = delegate
 *
 *     private var lookupCount = 0
 *
 *     override fun getContributedFunctions(name: Name, location: LookupLocation) {
 *         lookupCount++
 *         return super.getContributedFunctions(name, location)
 *     }
 *
 *     fun getStats(): String = "Total lookups: $lookupCount"
 * }
 *
 * // 使用
 * val countingScope = CountingScope(originalScope)
 * val functions = countingScope.getContributedFunctions(name, location)
 * println(countingScope.getStats())
 * ```
 *
 * @property workerScope 被委托的底层作用域，所有调用都转发给它
 *
 * @see LazyScopeAdapter 延迟加载的适配器实现
 * @see MemberScope 实现的接口
 */
abstract class AbstractScopeAdapter : MemberScope {
    protected abstract val workerScope: MemberScope


    override val functionNames: Set<Name>
        get() = workerScope.functionNames
    override val variableNames: Set<Name>
        get() = workerScope.variableNames
    override val classifierNames
        get() = workerScope.classifierNames
    override val propertyNames: Set<Name>
        get() = workerScope.propertyNames
    fun getActualScope(): MemberScope =
        if (workerScope is AbstractScopeAdapter)
            (workerScope as AbstractScopeAdapter).getActualScope()
        else
            workerScope

    override fun getContributedFunctions(name: Name, location: LookupLocation): Collection<SimpleFunctionDescriptor> {
        return workerScope.getContributedFunctions(name, location)
    }

    override fun getContributedMacros(name: Name, location: LookupLocation): Collection<MacroDescriptor> {
        return workerScope.getContributedMacros(name, location)

    }

    override fun getContributedClassifiers(name: Name, location: LookupLocation): List<ClassifierDescriptor> {
        return workerScope.getContributedClassifiers(name, location)
    }
    override fun getContributedPackageView(name: Name, location: LookupLocation): PackageViewDescriptor? {
        return workerScope.getContributedPackageView(name, location)
    }
    override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? {
        return workerScope.getContributedClassifier(name, location)
    }
    override fun getContributedClassifierByExportId(exportId: String, location: LookupLocation): ClassifierDescriptor? {
        return workerScope.getContributedClassifierByExportId(exportId, location)
    }
    override fun getContributedClassifierByIndex(index: Int, location: LookupLocation): ClassifierDescriptor? {
        return workerScope.getContributedClassifierByIndex(index, location)
    }
    override fun getContributedVariables(name: Name, location: LookupLocation): Collection<@JvmWildcard VariableDescriptor> {
        return workerScope.getContributedVariables(name, location)
    }
    override fun getContributedPropertys(name: Name, location: LookupLocation): Collection<@JvmWildcard PropertyDescriptor> {
        return workerScope.getContributedPropertys(name, location)
    }
    override fun getContributedDescriptors(kindFilter: DescriptorKindFilter,
                                           nameFilter: (Name) -> Boolean): Collection<DeclarationDescriptor> {
        return workerScope.getContributedDescriptors(kindFilter, nameFilter)
    }

/*
    override fun getFunctionNames() = workerScope.getFunctionNames()
    override fun getVariableNames() = workerScope.getVariableNames()
    override fun classifierNames() = workerScope.classifierNames()
*/

    override fun recordLookup(name: Name, location: LookupLocation) {
        workerScope.recordLookup(name, location)
    }

    override fun printScopeStructure(p: Printer) {
        p.println(this::class.java.simpleName, " {")
        p.pushIndent()

        p.print("worker =")
        workerScope.printScopeStructure(p.withholdIndentOnce())

        p.popIndent()
        p.println("}")
    }
}
