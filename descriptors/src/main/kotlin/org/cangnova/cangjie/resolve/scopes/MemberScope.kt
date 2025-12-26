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


/**
 * 描述符类型排除抽象类
 * 用于定义哪些类型的描述符应该被排除
 */
abstract class DescriptorKindExclude {
    /**
     * 判断指定的描述符是否应该被排除
     */
    abstract fun excludes(descriptor: DeclarationDescriptor): Boolean

    /**
     * 排除扩展函数的对象
     */
    object Extensions : DescriptorKindExclude() {
        override fun excludes(descriptor: DeclarationDescriptor) =
            descriptor is CallableDescriptor && descriptor.extensionReceiverParameter != null

        override val fullyExcludedDescriptorKinds: Int get() = 0
    }

    override fun toString() = this::class.java.simpleName

    /**
     * 排除非扩展函数的对象
     */
    object NonExtensions : DescriptorKindExclude() {
        override fun excludes(descriptor: DeclarationDescriptor) =
            descriptor !is CallableDescriptor || descriptor.extensionReceiverParameter == null

        override val fullyExcludedDescriptorKinds =
            DescriptorKindFilter.ALL_KINDS_MASK and (DescriptorKindFilter.FUNCTIONS_MASK or DescriptorKindFilter.VARIABLES_MASK).inv()
    }

    /**
     * 排除顶级包的对象
     */
    object TopLevelPackages : DescriptorKindExclude() {
        override fun excludes(descriptor: DeclarationDescriptor): Boolean {
            val fqName = when (descriptor) {
                is PackageFragmentDescriptor -> descriptor.fqName
                is PackageViewDescriptor -> descriptor.fqName
                else -> return false
            }
            return fqName.parent().isRoot
        }

        override val fullyExcludedDescriptorKinds: Int get() = 0
    }



    /**
     * 被此 [DescriptorKindExclude] 完全排除的描述符类型的位掩码
     * 即，[excludes] 对这些类型的所有描述符都返回 true
     * Bit-mask of descriptor kind's that are fully excluded by this [DescriptorKindExclude].
     * That is, [excludes] returns true for all descriptor of these kinds.
     */
    abstract val fullyExcludedDescriptorKinds: Int
}

/**
 * 描述符类型过滤器
 * 用于根据类型掩码和排除规则过滤描述符
 */
class DescriptorKindFilter(
    kindMask: Int,
    val excludes: List<DescriptorKindExclude> = listOf()
) {
    companion object {
        // 掩码到名称的映射类
        private class MaskToName(val mask: Int, val name: String)


        // 用于生成下一个掩码值的变量和函数
        private var nextMaskValue: Int = 0x01
        private fun nextMask() = nextMaskValue.apply { nextMaskValue = nextMaskValue shl 1 }

        // 各种描述符类型的掩码常量
        val NON_SINGLETON_CLASSIFIERS_MASK: Int = nextMask()  // 非单例分类器掩码


        val TYPE_ALIASES_MASK: Int = nextMask()               // 类型别名掩码
        val PACKAGES_MASK: Int = nextMask()                   // 包掩码
        val FUNCTIONS_MASK: Int = nextMask()                  // 函数掩码
        val VARIABLES_MASK: Int = nextMask()                  // 变量掩码
        val PROPERTYS_MASK: Int = nextMask()                  // 属性掩码
        val EXTENDS_MASK: Int = nextMask()                    // 扩展掩码

        val REEXPORT_MASK: Int = nextMask()                   // 重导出掩码

        // 组合掩码常量
        val ALL_KINDS_MASK: Int = nextMask() - 1              // 所有类型掩码
        val CLASSIFIERS_MASK: Int =
            NON_SINGLETON_CLASSIFIERS_MASK  or TYPE_ALIASES_MASK  // 分类器掩码

        val VALUES_MASK: Int   =FUNCTIONS_MASK or VARIABLES_MASK or PROPERTYS_MASK  // 值掩码
        val CALLABLES_MASK: Int = FUNCTIONS_MASK or VARIABLES_MASK or PROPERTYS_MASK  // 可调用对象掩码

        // 预定义的过滤器实例
        @JvmField
        val ALL: DescriptorKindFilter = DescriptorKindFilter(ALL_KINDS_MASK)  // 所有类型过滤器

        @JvmField
        val CALLABLES: DescriptorKindFilter = DescriptorKindFilter(CALLABLES_MASK)  // 可调用对象过滤器

        @JvmField
        val NON_SINGLETON_CLASSIFIERS: DescriptorKindFilter =
            DescriptorKindFilter(NON_SINGLETON_CLASSIFIERS_MASK)  // 非单例分类器过滤器


        @JvmField
        val TYPE_ALIASES: DescriptorKindFilter = DescriptorKindFilter(TYPE_ALIASES_MASK)  // 类型别名过滤器

        @JvmField
        val CLASSIFIERS: DescriptorKindFilter = DescriptorKindFilter(CLASSIFIERS_MASK)  // 分类器过滤器

        @JvmField
        val PACKAGES: DescriptorKindFilter = DescriptorKindFilter(PACKAGES_MASK)  // 包过滤器

        @JvmField
        val REEXPORT: DescriptorKindFilter = DescriptorKindFilter(REEXPORT_MASK)  // 重导出过滤器

        @JvmField
        val FUNCTIONS: DescriptorKindFilter = DescriptorKindFilter(FUNCTIONS_MASK)  // 函数过滤器

        @JvmField
        val VARIABLES: DescriptorKindFilter = DescriptorKindFilter(VARIABLES_MASK)  // 变量过滤器

        @JvmField
        val PROPERTYS: DescriptorKindFilter = DescriptorKindFilter(PROPERTYS_MASK)  // 属性过滤器

        @JvmField
        val EXTENDS: DescriptorKindFilter = DescriptorKindFilter(EXTENDS_MASK)  // 扩展过滤器

        @JvmField
        val VALUES: DescriptorKindFilter = DescriptorKindFilter(VALUES_MASK)  // 值过滤器


        // 调试用的掩码位名称映射
        private val DEBUG_MASK_BIT_NAMES = staticFields<DescriptorKindFilter>()
            .filter { it.type == Integer.TYPE }
            .mapNotNull { field ->
                val mask = field.get(null) as Int
                val isOneBitMask = mask == (mask and (-mask))
                if (isOneBitMask) MaskToName(mask, field.name) else null
            }

        // 调试用的预定义过滤器掩码名称映射
        private val DEBUG_PREDEFINED_FILTERS_MASK_NAMES = staticFields<DescriptorKindFilter>()
            .mapNotNull { field ->
                val filter = field.get(null) as? DescriptorKindFilter
                if (filter != null) MaskToName(filter.kindMask, field.name) else null
            }

        // 获取静态字段的内联函数
        private inline fun <reified T : Any> staticFields() =
            T::class.java.fields.filter { Modifier.isStatic(it.modifiers) }

    }

    val kindMask: Int  // 类型掩码

    init {
        // 应用排除规则，从掩码中移除被完全排除的类型
        var mask = kindMask
        excludes.forEach { mask = mask and it.fullyExcludedDescriptorKinds.inv() }
        this.kindMask = mask
    }

    /**
     * 与另一个过滤器求交集
     */
    fun intersect(other: DescriptorKindFilter) =
        DescriptorKindFilter(kindMask and other.kindMask, excludes + other.excludes)

    /**
     * 判断是否接受指定类型
     */
    fun acceptsKinds(kinds: Int): Boolean = kindMask and kinds != 0

    /**
     * 创建排除指定类型的新过滤器
     */
    fun withoutKinds(kinds: Int): DescriptorKindFilter = DescriptorKindFilter(kindMask and kinds.inv(), excludes)

    /**
     * 创建仅限于指定类型的过滤器，如果没有匹配的类型则返回 null
     */
    fun restrictedToKindsOrNull(kinds: Int): DescriptorKindFilter? {
        val mask = kindMask and kinds
        if (mask == 0) return null
        return DescriptorKindFilter(mask, excludes)
    }

    /**
     * 获取声明描述符对应的类型掩码
     */
    private fun DeclarationDescriptor.kind(): Int {
        return when (this) {
            is ClassDescriptor ,is ClassifierDescriptor->   NON_SINGLETON_CLASSIFIERS_MASK
            is TypeAliasDescriptor -> TYPE_ALIASES_MASK
            is PackageFragmentDescriptor, is PackageViewDescriptor -> PACKAGES_MASK
            is FunctionDescriptor -> FUNCTIONS_MASK
            is PropertyDescriptor -> PROPERTYS_MASK
            is VariableDescriptor -> VARIABLES_MASK
            is ExtendDescriptor -> EXTENDS_MASK

            else -> 0
        }
    }

    /**
     * 判断是否接受指定的声明描述符
     */
    fun accepts(descriptor: DeclarationDescriptor): Boolean =
        kindMask and descriptor.kind() != 0 && excludes.all { !it.excludes(descriptor) }

    /**
     * 添加排除规则并返回新的过滤器
     */
    infix fun exclude(exclude: DescriptorKindExclude): DescriptorKindFilter =
        DescriptorKindFilter(kindMask, excludes + listOf(exclude))

    /**
     * 添加指定类型并返回新的过滤器
     */
    fun withKinds(kinds: Int): DescriptorKindFilter = DescriptorKindFilter(kindMask or kinds, excludes)
    override fun toString(): String {
        val predefinedFilterName = DEBUG_PREDEFINED_FILTERS_MASK_NAMES.firstOrNull { it.mask == kindMask }?.name
        val kindString = predefinedFilterName ?: DEBUG_MASK_BIT_NAMES
            .mapNotNull { if (acceptsKinds(it.mask)) it.name else null }
            .joinToString(separator = " | ")

        return "DescriptorKindFilter($kindString, $excludes)"
    }


}
