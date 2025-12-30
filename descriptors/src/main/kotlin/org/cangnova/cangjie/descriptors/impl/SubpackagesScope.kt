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

package org.cangnova.cangjie.descriptors.impl

import com.intellij.util.containers.addIfNotNull
import org.cangnova.cangjie.descriptors.DeclarationDescriptor
import org.cangnova.cangjie.descriptors.ModuleDescriptor
import org.cangnova.cangjie.descriptors.PackageViewDescriptor
import org.cangnova.cangjie.incremental.components.LookupLocation
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.resolve.scopes.DescriptorKindExclude
import org.cangnova.cangjie.resolve.scopes.DescriptorKindFilter
import org.cangnova.cangjie.resolve.scopes.MemberScopeImpl
import org.cangnova.cangjie.utils.Printer

/**
 * 子包作用域 - 用于访问指定包下的所有子包的作用域实现
 *
 * SubpackagesScope 是一个专门的作用域实现，用于在包层次结构中导航和查找子包。
 * 该类仅提供对包（Package）的访问，不包含类、函数、变量等其他符号。
 *
 * ## 核心功能
 *
 * ### 子包查找
 *
 * - [getContributedDescriptors]: 返回当前包的所有非空子包
 * - 支持通过名称过滤子包
 * - 支持通过 [DescriptorKindFilter] 限定为包类型
 * - 自动过滤空包（不包含任何声明的包）
 *
 * ### 顶级包排除
 *
 * 可选地排除顶级包（根包的直接子包），通过 `DescriptorKindExclude.Module` 控制。
 *
 * ## 使用场景
 *
 * ### 1. 包层次导航
 *
 * 在 IDE 中浏览项目结构时，展示包的层次关系：
 *
 * ```kotlin
 * // 项目包结构：
 * // com.example
 * //   ├── util
 * //   ├── model
 * //   └── service
 *
 * val moduleDescriptor = getModuleDescriptor()
 * val comExampleScope = SubpackagesScope(
 *     moduleDescriptor,
 *     FqName("com.example")
 * )
 *
 * // 获取所有子包
 * val subpackages = comExampleScope.getContributedDescriptors(
 *     DescriptorKindFilter.PACKAGES
 * )
 * // 返回: [util, model, service] 的 PackageViewDescriptor
 * ```
 *
 * ### 2. import 语句的包补全
 *
 * 当用户输入 `import com.example.` 时，提示可用的子包：
 *
 * ```kotlin
 * // 用户输入: import com.example.
 * //                               ^ 光标位置
 *
 * val scope = SubpackagesScope(module, FqName("com.example"))
 * val suggestions = scope.getContributedDescriptors()
 *     .map { it.name.asString() }
 * // 建议: ["util", "model", "service"]
 * ```
 *
 * ### 3. 包视图的成员作用域
 *
 * 包描述符的成员作用域通常包含子包作用域的组合：
 *
 * ```kotlin
 * // 包的完整成员作用域 = 直接成员 + 子包
 * val packageMembers = ChainedMemberScope.create(
 *     "Package com.example",
 *     packageDirectMembersScope,
 *     SubpackagesScope(module, FqName("com.example"))
 * )
 * ```
 *
 * ## 实现细节
 *
 * ### 包查找机制
 *
 * 通过 [ModuleDescriptor] 的 API 获取子包：
 *
 * ```kotlin
 * // 1. 获取所有子包的全限定名
 * val subFqNames = moduleDescriptor.getSubPackagesOf(fqName, nameFilter)
 *
 * // 2. 对每个子包创建 PackageViewDescriptor
 * for (subFqName in subFqNames) {
 *     val shortName = subFqName.shortName()
 *     val packageView = getPackage(shortName)
 *     if (!packageView.isEmpty()) {
 *         result.add(packageView)
 *     }
 * }
 * ```
 *
 * ### 空包过滤
 *
 * 通过 `packageViewDescriptor.isEmpty()` 判断包是否为空：
 * - 空包：不包含任何声明（类、函数、变量）且没有非空子包
 * - 非空包：至少包含一个声明或有非空子包
 *
 * 这确保了 IDE 中只显示有实际内容的包。
 *
 * ### 特殊名称处理
 *
 * 排除特殊名称（如编译器内部使用的名称）：
 *
 * ```kotlin
 * if (name.isSpecial) {
 *     return null // 不返回特殊名称的包
 * }
 * ```
 *
 * ## 与其他作用域的集成
 *
 * ### 包的完整作用域
 *
 * 包的成员作用域通常是多个作用域的链式组合：
 *
 * ```kotlin
 * // PackageViewDescriptor.memberScope =
 * ChainedMemberScope.create(
 *     "Package $fqName",
 *     packageFragmentScope1,  // 来自源码的声明
 *     packageFragmentScope2,  // 来自依赖库的声明
 *     SubpackagesScope(module, fqName)  // 子包
 * )
 * ```
 *
 * ## 特性与限制
 *
 * ### classifierNames
 *
 * - 返回空集：子包作用域不包含分类器（类、接口等）
 * - 仅用于包级别的导航
 *
 * ### definitelyDoesNotContainName
 *
 * - 默认实现有误：当前返回 `classifierNames.contains(name)`（总是 false）
 * - 应该检查是否存在对应名称的子包
 *
 * ### 顶级包过滤
 *
 * 当 `fqName.isRoot` 且过滤器包含 `Module` 排除时：
 * - 不返回顶级包（如 `com`, `org`, `java` 等）
 * - 用于某些场景下隐藏顶级包结构
 *
 * ## 性能考虑
 *
 * ### 惰性查找
 *
 * 子包列表通过 `getSubPackagesOf` 在每次调用时动态获取：
 * - 不缓存结果
 * - 适合包结构可能变化的场景（如 IDE 开发模式）
 *
 * ### 过滤优化
 *
 * 在 `getContributedDescriptors` 中先检查过滤器：
 *
 * ```kotlin
 * if (!kindFilter.acceptsKinds(DescriptorKindFilter.PACKAGES_MASK)) {
 *     return emptyList() // 快速返回
 * }
 * ```
 *
 * ## 典型使用示例
 *
 * ### 示例 1: 递归浏览包结构
 *
 * ```kotlin
 * fun printPackageTree(module: ModuleDescriptor, fqName: FqName, indent: Int = 0) {
 *     val scope = SubpackagesScope(module, fqName)
 *     val subpackages = scope.getContributedDescriptors(DescriptorKindFilter.PACKAGES)
 *
 *     for (pkg in subpackages) {
 *         println("  ".repeat(indent) + pkg.name)
 *         printPackageTree(module, pkg.fqName, indent + 1)
 *     }
 * }
 *
 * // 输出：
 * // com
 * //   example
 * //     util
 * //     model
 * //     service
 * ```
 *
 * ### 示例 2: 查找特定子包
 *
 * ```kotlin
 * val scope = SubpackagesScope(module, FqName("com.example"))
 * val utilPackage = scope.getPackage(Name.identifier("util"))
 *
 * if (utilPackage != null && !utilPackage.isEmpty()) {
 *     // 使用 util 包
 *     val utilMembers = utilPackage.memberScope
 * }
 * ```
 *
 * ### 示例 3: IDE 包补全
 *
 * ```kotlin
 * fun providePackageCompletion(currentPackage: FqName): List<String> {
 *     val scope = SubpackagesScope(module, currentPackage)
 *     return scope.getContributedDescriptors(
 *         kindFilter = DescriptorKindFilter.PACKAGES,
 *         nameFilter = { name ->
 *             // 可以添加前缀过滤
 *             name.asString().startsWith(userInput)
 *         }
 *     ).map { it.name.asString() }
 * }
 * ```
 *
 * ## 与 PackageViewDescriptor 的关系
 *
 * - SubpackagesScope 返回 [PackageViewDescriptor] 实例
 * - PackageViewDescriptor 表示包的视图（可能来自多个源）
 * - PackageViewDescriptor.memberScope 又可能包含 SubpackagesScope（递归结构）
 *
 * ```
 * PackageViewDescriptor
 *   ├── fqName: FqName
 *   ├── module: ModuleDescriptor
 *   └── memberScope: MemberScope
 *         └── (包含) SubpackagesScope
 *               └── (返回) PackageViewDescriptor (子包)
 * ```
 *
 * ## 注意事项
 *
 * 1. **仅用于包**: 不要尝试在此作用域中查找类、函数等其他符号
 * 2. **空包过滤**: 空包不会出现在结果中，这是预期行为
 * 3. **模块依赖**: 依赖于 ModuleDescriptor 的正确实现
 * 4. **特殊名称**: 特殊名称的包（编译器内部使用）会被自动排除
 *
 * ## 改进方向
 *
 * 1. **修正 definitelyDoesNotContainName**: 应检查子包存在性
 * 2. **缓存支持**: 对于不变的包结构，可以添加缓存
 * 3. **批量查询优化**: 减少对 ModuleDescriptor 的重复调用
 *
 * @property moduleDescriptor 所属的模块描述符，用于查询子包
 * @property fqName 当前包的完全限定名
 *
 * @see ModuleDescriptor.getSubPackagesOf 获取子包列表的方法
 * @see PackageViewDescriptor 包视图描述符
 * @see MemberScopeImpl 父类，提供默认实现
 * @see DescriptorKindFilter.PACKAGES 包类型过滤器
 */
open class SubpackagesScope(private val moduleDescriptor: ModuleDescriptor, private val fqName: FqName) :
    MemberScopeImpl() {

    protected fun getPackage(name: Name): PackageViewDescriptor? {
        if (name.isSpecial) {
            return null
        }
        val packageViewDescriptor = moduleDescriptor.getPackage(fqName.child(name))
        if (packageViewDescriptor.isEmpty()) {
            return null
        }
        return packageViewDescriptor
    }



    override val classifierNames: Set<Name>
        get() =  emptySet()

    /**
     * 获取指定名称的子包视图描述符
     *
     * 此方法是解决 `import std.core` 后可以使用 `core.String` 的关键。
     * 当导入一个包（如 `std.core`）后，包名 `core` 应该可以作为命名空间使用。
     *
     * 调用链：
     * ```
     * LazyExplicitImportScope.getContributedPackage("core")
     *   → std.memberScope.getContributedPackageView("core", ...)
     *   → ChainedMemberScope 遍历所有子作用域
     *   → SubpackagesScope.getContributedPackageView("core", ...)
     *   → getPackage("core") → 返回 std.core 包
     * ```
     *
     * @param name 子包名称
     * @param location 查找位置（用于增量编译追踪）
     * @return 包视图描述符，如果不存在则返回 null
     */
    override fun getContributedPackageView(name: Name, location: LookupLocation): PackageViewDescriptor? {
        return getPackage(name)
    }

    override fun definitelyDoesNotContainName(name: Name): Boolean {
        return classifierNames.contains(name)

    }

    override fun getContributedDescriptors(
        kindFilter: DescriptorKindFilter,
        nameFilter: (Name) -> Boolean
    ): Collection<DeclarationDescriptor> {
        if (!kindFilter.acceptsKinds(DescriptorKindFilter.PACKAGES_MASK)) return listOf()
        if (fqName.isRoot && kindFilter.excludes.contains(DescriptorKindExclude.Module)) return listOf()

        val subFqNames = moduleDescriptor.getSubPackagesOf(fqName, nameFilter)
        val result = ArrayList<DeclarationDescriptor>(subFqNames.size)
        for (subFqName in subFqNames) {
            val shortName = subFqName.shortName()
            if (nameFilter(shortName)) {
                result.addIfNotNull(getPackage(shortName))
            }
        }
        return result
    }


    override fun printScopeStructure(p: Printer) {
        p.println(this::class.java.simpleName, " {")
        p.pushIndent()

        p.popIndent()
        p.println("}")
    }

    override fun toString(): String = "subpackages of $fqName from $moduleDescriptor"
}
