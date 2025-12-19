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

package org.cangnova.cangjie.resolve

/**
 * 懒加载显式导入作用域模块
 *
 * 本文件实现了显式导入（单一导入）的作用域，用于处理如下形式的导入：
 * - `import foo.bar.Baz` - 导入单个类型
 * - `import foo.bar.baz as qux` - 导入并重命名
 * - `import foo.bar.func` - 导入顶层函数
 *
 * ## 与全通配导入的区别
 *
 * | 特性 | 显式导入 | 全通配导入 |
 * |------|----------|------------|
 * | 语法 | `import foo.Bar` | `import foo.*` |
 * | 作用域实现 | [LazyExplicitImportScope] | [AllUnderImportScope] |
 * | 支持别名 | 是 | 否 |
 * | 导入数量 | 单个 | 多个 |
 *
 * ## 工作原理
 *
 * 1. 接收导入路径的父描述符（包或类）
 * 2. 存储声明的名称和别名
 * 3. 当查询时，在父描述符的作用域中查找声明
 * 4. 如果使用了别名，只响应别名的查询
 *
 * ## 重导出机制说明
 *
 * 仓颉语言支持重导出（reexport），允许将导入的符号以指定的可见性重新导出：
 * ```cangjie
 * public import std.core.String    // 重导出为 public
 * internal import std.io.File      // 重导出为 internal
 * import std.math.sqrt             // 默认 private，不重导出
 * ```
 *
 * **重导出的实现分层**：
 *
 * | 组件 | 职责 |
 * |------|------|
 * | [LazyExplicitImportScope] | 单个导入的符号解析（不处理重导出可见性） |
 * | [PackageReexportScope] | 包级别重导出声明的聚合和可见性检查 |
 * | [CjImportDirectiveItem.isReexport] | 判断导入是否为重导出 |
 * | [CjImportDirectiveItem.importVisibility] | 获取导入的可见性修饰符 |
 *
 * @see AllUnderImportScope 全通配导入作用域
 * @see LazyImportScope 懒加载导入作用域
 * @see PackageReexportScope 包级重导出作用域
 */

import com.intellij.util.SmartList
import org.cangnova.cangjie.config.LanguageVersionSettings
import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.descriptors.macro.MacroDescriptor
import org.cangnova.cangjie.incremental.components.LookupLocation
import org.cangnova.cangjie.incremental.components.NoLookupLocation
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.resolve.lazy.descriptors.LazyClassDescriptor
import org.cangnova.cangjie.resolve.qualified.QualifierPosition
import org.cangnova.cangjie.resolve.qualified.isVisible
import org.cangnova.cangjie.resolve.scopes.BaseImportingScope
import org.cangnova.cangjie.resolve.scopes.DescriptorKindFilter
import org.cangnova.cangjie.resolve.scopes.MemberScope
import org.cangnova.cangjie.utils.CallOnceFunction
import org.cangnova.cangjie.utils.Printer
import org.cangnova.cangjie.utils.addIfNotNull

/**
 * 懒加载显式导入作用域
 *
 * 表示单个显式导入语句创建的作用域。处理如下形式的导入：
 * ```cangjie
 * import std.core.String           // declaredName = aliasName = "String"
 * import std.core.String as Str    // declaredName = "String", aliasName = "Str"
 * import std.math.sqrt             // 导入顶层函数
 * ```
 *
 * ## 核心特性
 *
 * ### 名称映射
 * - [declaredName]: 导入路径中的原始名称（最后一个部分）
 * - [aliasName]: 导入后使用的名称（可能是别名）
 * - 只有查询 [aliasName] 时才会返回结果
 *
 * ### 支持的导入目标
 * - **类型**: 类、接口、枚举、类型别名
 * - **函数**: 顶层函数、静态方法
 * - **变量**: 顶层变量、静态属性
 * - **宏**: 宏定义
 * - **包**: 子包（用于包别名）
 *
 * ### 可见性检查
 * 使用 [packageFragmentForVisibilityCheck] 进行可见性验证，
 * 确保导入的符号对当前文件可见。
 *
 * ## 使用示例
 *
 * ```kotlin
 * // 由 QualifiedExpressionResolver.processSingleImport() 创建
 * val scope = LazyExplicitImportScope(
 *     languageVersionSettings = settings,
 *     packageOrClassDescriptor = packageDescriptor,  // std.core 包
 *     packageFragmentForVisibilityCheck = currentPackage,
 *     declaredName = Name.identifier("String"),
 *     aliasName = Name.identifier("Str"),
 *     storeReferences = { descriptors -> /* 记录引用 */ }
 * )
 *
 * // 查询时只响应别名
 * scope.getContributedClassifier(Name.identifier("Str"), location)  // 返回 String
 * scope.getContributedClassifier(Name.identifier("String"), location) // 返回 null
 * ```
 *
 * @param languageVersionSettings 语言版本设置，用于可见性检查
 * @param packageOrClassDescriptor 导入路径的父描述符（包或类）
 * @param packageFragmentForVisibilityCheck 用于可见性检查的包片段
 * @param declaredName 导入路径中声明的原始名称
 * @param aliasName 导入后使用的名称（别名或原始名称）
 * @param storeReferences 存储引用的回调函数，用于记录解析结果
 *
 * @see QualifiedExpressionResolver.processSingleImport 创建此作用域的方法
 */
class LazyExplicitImportScope(
    private val languageVersionSettings: LanguageVersionSettings,
    private val packageOrClassDescriptor: DeclarationDescriptor,
    private val packageFragmentForVisibilityCheck: PackageFragmentDescriptor?,
    private val declaredName: Name,
    private val aliasName: Name,
    private val storeReferences: CallOnceFunction<Collection<DeclarationDescriptor>, Unit>,
    /**
     * 重导出作用域，用于查找包中被重导出的声明
     *
     * 当从包中导入单个声明时（如 `import pkg.a.Foo`），如果 `Foo` 不是 `pkg.a` 的直接成员，
     * 而是 `pkg.a` 重导出的声明（如 `public import other.Foo`），则需要在此作用域中查找。
     */
    private val reexportScope: MemberScope? = null
) : BaseImportingScope(null) {

    /**
     * 获取导入的分类器（类、接口、类型别名等）
     *
     * 只有当查询的名称与 [aliasName] 匹配时才返回结果。
     * 首先在父描述符（包或类）的作用域中查找 [declaredName] 对应的分类器，
     * 如果没找到，则在重导出作用域中查找。
     *
     * @param name 要查找的名称
     * @param location 查找位置
     * @return 匹配的分类器描述符，如果名称不匹配或未找到则返回 null
     */
    override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? {
        if (name != aliasName) return null

        // 首先在包/类的成员作用域中查找
        val result = when (packageOrClassDescriptor) {
            is PackageViewDescriptor -> packageOrClassDescriptor.memberScope.getContributedClassifier(
                declaredName,
                location
            )

            is ClassDescriptor -> packageOrClassDescriptor.unsubstitutedMemberScope.getContributedClassifier(
                declaredName,
                location
            )

            else -> throw IllegalStateException("Should be class or package: $packageOrClassDescriptor")
        }

        // 如果在成员作用域中没找到，尝试在重导出作用域中查找
        if (result == null && reexportScope != null) {
            return reexportScope.getContributedClassifier(declaredName, location)
        }

        return result
    }

    /**
     * 获取导入的函数
     *
     * 只有当查询的名称与 [aliasName] 匹配时才返回结果。
     * 收集父描述符作用域中 [declaredName] 对应的所有函数。
     *
     * @param name 要查找的函数名称
     * @param location 查找位置
     * @return 匹配的函数描述符集合
     */
    override fun getContributedFunctions(name: Name, location: LookupLocation): Collection<FunctionDescriptor> {
        if (name != aliasName) return emptyList()

        return collectCallableMemberDescriptors(location, MemberScope::getContributedFunctions)
    }

    /**
     * 获取导入的宏
     *
     * 只有当查询的名称与 [aliasName] 匹配时才返回结果。
     * 收集父描述符作用域中 [declaredName] 对应的所有宏定义。
     *
     * @param name 要查找的宏名称
     * @param location 查找位置
     * @return 匹配的宏描述符集合
     */
    override fun getContributedMacros(name: Name, location: LookupLocation): Collection<MacroDescriptor> {
        if (name != aliasName) return emptyList()

        return collectCallableMemberDescriptors(location, MemberScope::getContributedMacros)
    }

    /**
     * 获取导入的变量
     *
     * 只有当查询的名称与 [aliasName] 匹配时才返回结果。
     * 收集父描述符作用域中 [declaredName] 对应的所有变量。
     *
     * @param name 要查找的变量名称
     * @param location 查找位置
     * @return 匹配的变量描述符集合
     */
    override fun getContributedVariables(
        name: Name,
        location: LookupLocation
    ): Collection<@JvmWildcard VariableDescriptor> {
        if (name != aliasName) return emptyList()

        return collectCallableMemberDescriptors(location, MemberScope::getContributedVariables)
    }

    /**
     * 获取导入的属性
     *
     * 只有当查询的名称与 [aliasName] 匹配时才返回结果。
     * 收集父描述符作用域中 [declaredName] 对应的所有属性。
     *
     * @param name 要查找的属性名称
     * @param location 查找位置
     * @return 匹配的属性描述符集合
     */
    override fun getContributedPropertys(name: Name, location: LookupLocation): Collection<PropertyDescriptor> {
        if (name != aliasName) return emptyList()

        return collectCallableMemberDescriptors(location, MemberScope::getContributedPropertys)
    }

    /**
     * 获取导入的包
     *
     * 用于支持包别名导入，例如：`import std.core as c`
     * 此时查询 "c" 会返回 std.core 包。
     *
     * @param name 要查找的包名称
     * @return 匹配的包视图描述符，如果名称不匹配或未找到则返回 null
     */
    override fun getContributedPackage(name: Name): PackageViewDescriptor? {
        // 只有查询别名时才返回结果
        if (name != aliasName) return null

        return when (packageOrClassDescriptor) {
            is LazyClassDescriptor -> {
                // 类描述符不能导入为包
                null
            }

            is PackageViewDescriptor -> {
                // 从父包中查找子包
                packageOrClassDescriptor.memberScope.getContributedPackageView(
                    declaredName, NoLookupLocation.MATCH_GET_ALL_DESCRIPTORS
                )
            }

            else -> throw IllegalStateException("Should be class or package: $packageOrClassDescriptor")
        }
    }

    /**
     * 获取此导入作用域贡献的所有描述符
     *
     * 根据 [kindFilter] 收集所有匹配的描述符，包括：
     * - 分类器（类、接口、类型别名）
     * - 函数
     * - 变量
     * - 包
     *
     * 如果 [changeNamesForAliased] 为 true 且使用了别名，
     * 则返回的描述符会使用别名作为名称。
     *
     * @param kindFilter 描述符类型过滤器
     * @param nameFilter 名称过滤器
     * @param changeNamesForAliased 是否将描述符名称改为别名
     * @return 匹配的描述符集合
     */
    override fun getContributedDescriptors(
        kindFilter: DescriptorKindFilter,
        nameFilter: (Name) -> Boolean,
        changeNamesForAliased: Boolean
    ): Collection<DeclarationDescriptor> {
        val descriptors = SmartList<DeclarationDescriptor>()

        if (kindFilter.acceptsKinds(DescriptorKindFilter.CLASSIFIERS_MASK)) {
            descriptors.addIfNotNull(getContributedClassifier(aliasName, NoLookupLocation.MATCH_GET_ALL_DESCRIPTORS))
        }
        if (kindFilter.acceptsKinds(DescriptorKindFilter.FUNCTIONS_MASK)) {
            descriptors.addAll(getContributedFunctions(aliasName, NoLookupLocation.MATCH_GET_ALL_DESCRIPTORS))
        }
        if (kindFilter.acceptsKinds(DescriptorKindFilter.VARIABLES_MASK)) {
            descriptors.addAll(getContributedVariables(aliasName, NoLookupLocation.MATCH_GET_ALL_DESCRIPTORS))
        }
        // 注意：REEXPORT_MASK 在此作用域中不处理
        // 原因：LazyExplicitImportScope 只负责单个导入的符号解析，
        // 不持有导入指令的可见性修饰符信息。
        // 重导出功能由 PackageReexportScope 在包级别统一处理：
        // - 它收集包中所有带有 public/internal/protected 修饰符的导入
        // - 通过 CjImportDirectiveItem.isReexport 判断是否为重导出
        // - 根据访问位置检查可见性并提供重导出的声明
        if (kindFilter.acceptsKinds(DescriptorKindFilter.PACKAGES_MASK)) {
            getContributedPackage(aliasName)?.let {
                descriptors.add(it)
            }
        }




        if (changeNamesForAliased && aliasName != declaredName) {
            for (i in descriptors.indices) {
                val newDescriptor: DeclarationDescriptor = when (val descriptor = descriptors[i]) {
                    is ClassDescriptor -> {
                        object : ClassDescriptor by descriptor {

                            override val name: Name = aliasName
                        }
                    }

                    is TypeAliasDescriptor -> {
                        object : TypeAliasDescriptor by descriptor {
                            override val name: Name = aliasName
                        }
                    }

                    is CallableMemberDescriptor -> {
                        descriptor
                            .newCopyBuilder()
                            .setName(aliasName)
                            .setOriginal(descriptor)
                            .build()!!
                    }

                    else -> error("Unknown kind of descriptor in import alias: $descriptor")
                }
                descriptors[i] = newDescriptor
            }
        }

        return descriptors
    }

    /**
     * 计算此作用域导入的名称集合
     *
     * 对于显式导入，只返回一个名称（别名）。
     *
     * @return 包含别名的集合
     */
    override fun computeImportedNames() = setOf(aliasName)

    /**
     * 打印作用域结构（调试用）
     *
     * @param p 打印器
     */
    override fun printStructure(p: Printer) {
        p.println(this::class.java.simpleName, ": ", aliasName)
    }


    /**
     * 存储对描述符的引用
     *
     * 强制解析此导入并存储解析到的描述符引用。
     * 此方法设计为只调用一次，用于触发懒加载解析并记录结果。
     *
     * 用途：
     * - 在代码分析完成时触发，确保所有引用都被正确记录
     * - IDE 需要这些信息来支持导航、重构等功能
     *
     * @return 解析到的描述符集合
     */
    internal fun storeReferencesToDescriptors() = getContributedDescriptors().apply(storeReferences)


    /**
     * 收集可调用成员描述符
     *
     * 该函数用于收集给定作用域中所有可见的可调用成员描述符（如函数或属性描述符）
     * 它根据[packageOrClassDescriptor]的类型（包或类描述符）来决定使用哪种作用域进行查找
     * 同时也会从重导出作用域中查找被重导出的声明
     *
     * @param location 查找位置，用于调试信息
     * @param getDescriptors 一个高阶函数，用于从成员作用域中获取描述符集合
     * @return 返回收集到的可调用成员描述符集合
     */
    private fun <D : /*CallableMemberDescriptor*/CallableDescriptor> collectCallableMemberDescriptors(
        location: LookupLocation,
        getDescriptors: MemberScope.(Name, LookupLocation) -> Collection<D>
    ): Collection<D> {
        val descriptors = SmartList<D>()

        // 根据packageOrClassDescriptor的类型决定如何收集描述符
        when (packageOrClassDescriptor) {
            is PackageViewDescriptor -> {
                // 如果是包描述符，使用成员作用域收集描述符
                val packageScope = packageOrClassDescriptor.memberScope
                descriptors.addAll(packageScope.getDescriptors(declaredName, location))
            }

            is ClassDescriptor -> {
                // 如果是类描述符，使用静态作用域收集描述符
                val staticClassScope = packageOrClassDescriptor.staticScope
                descriptors.addAll(staticClassScope.getDescriptors(declaredName, location))


            }

            // 如果既不是包描述符也不是类描述符，则抛出异常
            else -> throw IllegalStateException("Should be class or package: $packageOrClassDescriptor")
        }

        // 如果在成员作用域中没找到，尝试从重导出作用域中查找
        if (descriptors.isEmpty() && reexportScope != null) {
            descriptors.addAll(reexportScope.getDescriptors(declaredName, location))
        }

        // 返回所有收集到的描述符，经过可见性过滤
        return descriptors.choseOnlyVisibleOrAll()
    }


    /**
     * 从集合中选择所有可见的元素，如果都不可见则返回原始集合
     *
     * 这是一个可见性过滤的辅助方法：
     * 1. 首先过滤出所有对当前包可见的描述符
     * 2. 如果有可见的描述符，返回过滤后的集合
     * 3. 如果没有可见的描述符，返回原始集合（用于错误诊断）
     *
     * 返回原始集合而不是空集合的设计是为了：
     * - 让错误诊断能够报告"符号不可见"而不是"符号不存在"
     * - 提供更准确的错误信息
     *
     * @param D 描述符类型
     * @return 过滤后的集合，或原始集合（如果没有可见元素）
     */
    private fun <D : DeclarationDescriptor> Collection<D>.choseOnlyVisibleOrAll(): Collection<D> =
        filter {
            isVisible(
                it,
                packageFragmentForVisibilityCheck,
                position = QualifierPosition.IMPORT,
                languageVersionSettings
            )
        }
            .takeIf { it.isNotEmpty() }
            ?: this

}
