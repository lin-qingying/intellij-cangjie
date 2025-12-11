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

import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.descriptors.annotations.Annotations
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.resolve.scopes.ChainedMemberScope
import org.cangnova.cangjie.resolve.scopes.LazyScopeAdapter
import org.cangnova.cangjie.resolve.scopes.MemberScope
import org.cangnova.cangjie.storage.StorageManager
import org.cangnova.cangjie.storage.getValue

/**
 * 延迟包视图描述符实现 - 提供包描述符的延迟初始化和线程安全访问
 *
 * LazyPackageViewDescriptorImpl 是 [PackageViewDescriptor] 的惰性实现，用于表示模块中的包视图。
 * 它利用 [StorageManager] 提供线程安全的延迟求值，确保包的成员作用域、片段列表等资源
 * 只在首次访问时才初始化，这在处理大型项目或避免循环依赖时特别有用。
 *
 * ## 核心功能
 *
 * ### 延迟初始化
 *
 * 所有重量级资源通过 [StorageManager] 延迟加载：
 * - [empty]: 惰性判断包是否为空
 * - [memberScope]: 惰性构建包的成员作用域
 * - [fragments]: 惰性加载包片段列表
 *
 * ### 包视图组合
 *
 * 包的成员作用域由多个部分组成：
 * - 所有 [PackageFragmentDescriptor] 的成员作用域（来自不同源或库）
 * - [SubpackagesScope] 提供子包访问
 * - 通过 [ChainedMemberScope] 统一组合
 *
 * ### 空包优化
 *
 * 如果包为空（无成员、无子包），返回 [MemberScope.Empty]：
 * - 避免创建不必要的作用域对象
 * - 提升查找性能
 * - 减少内存占用
 *
 * ## 使用场景
 *
 * ### 1. 模块的包管理
 *
 * [ModuleDescriptorImpl] 使用此类表示所有包：
 *
 * ```kotlin
 * class ModuleDescriptorImpl(...) {
 *     override fun getPackage(fqName: FqName): PackageViewDescriptor {
 *         // 延迟创建包视图
 *         return LazyPackageViewDescriptorImpl(
 *             module = this,
 *             fqName = fqName,
 *             storageManager = storageManager
 *         )
 *     }
 * }
 *
 * // 使用
 * val myPackage = module.getPackage(FqName("com.example"))
 * // 此时包视图已创建，但成员作用域尚未初始化
 *
 * val members = myPackage.memberScope.getContributedFunctions(name, location)
 * // 首次访问 memberScope，触发延迟初始化
 * ```
 *
 * ### 2. 跨源码和库的包合并
 *
 * 同一个包可能在多个位置定义（源码、依赖库、JDK 等）：
 *
 * ```kotlin
 * // 项目包结构：
 * // 源码: src/com/example/MyClass.cj
 * // 库 A: lib-a.jar!/com/example/UtilA.cj
 * // 库 B: lib-b.jar!/com/example/UtilB.cj
 *
 * val packageView = module.getPackage(FqName("com.example"))
 *
 * // packageView.fragments 包含：
 * // - SourcePackageFragment (来自 src/)
 * // - LibraryPackageFragment (来自 lib-a.jar)
 * // - LibraryPackageFragment (来自 lib-b.jar)
 *
 * // packageView.memberScope 合并所有片段的成员：
 * // - MyClass (源码)
 * // - UtilA (库 A)
 * // - UtilB (库 B)
 * // - 子包: com.example.subpackage
 * ```
 *
 * ### 3. import 语句的包解析
 *
 * 编译器在处理 import 语句时查找包：
 *
 * ```kotlin
 * // 源代码: import com.example.MyClass
 *
 * // 1. 解析包
 * val packageView = module.getPackage(FqName("com.example"))
 *
 * // 2. 在包的成员作用域中查找 MyClass
 * val classDescriptor = packageView.memberScope.getContributedClassifier(
 *     Name.identifier("MyClass"),
 *     location
 * )
 * ```
 *
 * ### 4. IDE 中的包浏览
 *
 * 在项目视图中展示包层次结构：
 *
 * ```kotlin
 * // 用户点击包节点: com.example
 *
 * val packageView = module.getPackage(FqName("com.example"))
 *
 * if (!packageView.isEmpty()) {
 *     // 显示包内容
 *     val classes = packageView.memberScope.getContributedDescriptors(
 *         DescriptorKindFilter.CLASSIFIERS
 *     )
 *
 *     // 显示子包
 *     val subpackages = packageView.memberScope.getContributedDescriptors(
 *         DescriptorKindFilter.PACKAGES
 *     )
 * }
 * ```
 *
 * ## 实现细节
 *
 * ### 惰性 empty 判断
 *
 * 通过 StorageManager 委托延迟计算：
 *
 * ```kotlin
 * val empty: Boolean by storageManager.createLazyValue {
 *     module.packageFragmentProvider.isEmpty(fqName)
 * }
 * ```
 *
 * - 首次访问时调用 `packageFragmentProvider.isEmpty()`
 * - 结果被缓存，后续访问直接返回
 * - 线程安全（取决于 StorageManager 配置）
 *
 * ### 成员作用域构建
 *
 * 使用 [LazyScopeAdapter] 延迟构建作用域：
 *
 * ```kotlin
 * override val memberScope: MemberScope = LazyScopeAdapter(storageManager) {
 *     if (isEmpty()) {
 *         MemberScope.Empty  // 空包优化
 *     } else {
 *         // 1. 获取所有片段的成员作用域
 *         val fragmentScopes = fragments.map { it.getMemberScope() }
 *
 *         // 2. 添加子包作用域
 *         val subpackageScope = SubpackagesScope(module, fqName)
 *
 *         // 3. 链式组合
 *         val allScopes = fragmentScopes + subpackageScope
 *         ChainedMemberScope.create(
 *             "package view scope for $fqName in ${module.name}",
 *             allScopes
 *         )
 *     }
 * }
 * ```
 *
 * ### 片段列表加载
 *
 * 通过 `by` 委托实现惰性加载：
 *
 * ```kotlin
 * override val fragments: List<PackageFragmentDescriptor> by storageManager.createLazyValue {
 *     module.packageFragmentProvider.packageFragments(fqName)
 * }
 * ```
 *
 * - 使用 Kotlin 的属性委托语法
 * - 等价于定义一个 lazy val
 * - StorageManager 确保线程安全
 *
 * ### 包含关系
 *
 * 获取父包视图：
 *
 * ```kotlin
 * override val containingDeclaration: PackageViewDescriptor?
 *     get() = if (fqName.isRoot) null else module.getPackage(fqName.parent())
 * ```
 *
 * - 根包（fqName.isRoot）无父包，返回 null
 * - 其他包返回父包的 PackageViewDescriptor
 * - 形成包层次树结构
 *
 * ## 与其他 PackageViewDescriptor 实现的对比
 *
 * | 特性 | LazyPackageViewDescriptorImpl | 即时加载实现 |
 * |------|-------------------------------|-------------|
 * | 初始化成本 | 极低（仅存储引用） | 高（立即构建作用域） |
 * | 内存占用 | 低（未访问时不占用） | 高（始终占用） |
 * | 首次访问延迟 | 有（需计算） | 无（已就绪） |
 * | 线程安全 | 可配置（通过 StorageManager） | 需手动实现 |
 * | 循环依赖 | 易于处理（延迟打破循环） | 困难（可能死锁） |
 * | 适用场景 | 大型项目、IDE | 小型项目、批量编译 |
 *
 * ## 性能特性
 *
 * ### 初始化成本
 *
 * - **创建 LazyPackageViewDescriptorImpl**: O(1)，仅存储引用
 * - **首次访问 empty**: 查询 PackageFragmentProvider，成本取决于实现
 * - **首次访问 memberScope**: 构建所有片段的作用域并组合，成本较高
 * - **首次访问 fragments**: 查询 PackageFragmentProvider
 * - **后续访问**: O(1)，直接返回缓存值
 *
 * ### 内存占用
 *
 * - **未初始化状态**: 仅存储 module、fqName、storageManager 引用
 * - **部分初始化**: 仅已访问的字段占用内存
 * - **完全初始化**: 存储 fragments、memberScope 和 empty 标志
 *
 * ### 空包优化
 *
 * ```kotlin
 * // 空包直接返回 MemberScope.Empty（单例）
 * if (isEmpty()) {
 *     MemberScope.Empty
 * } else {
 *     // 复杂的作用域构建...
 * }
 * ```
 *
 * - 避免为空包创建 ChainedMemberScope
 * - 大型项目中可能有大量空包（中间层级包）
 * - 显著减少内存占用和 GC 压力
 *
 * ## Visitor 模式支持
 *
 * 实现 [DeclarationDescriptor.accept] 方法：
 *
 * ```kotlin
 * override fun <R, D> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D?): R =
 *     visitor.visitPackageViewDescriptor(this, data!!)
 * ```
 *
 * 允许通过访问者模式遍历描述符树：
 *
 * ```kotlin
 * val visitor = object : DeclarationDescriptorVisitor<Unit, Unit> {
 *     override fun visitPackageViewDescriptor(descriptor: PackageViewDescriptor, data: Unit) {
 *         println("访问包: ${descriptor.fqName}")
 *         descriptor.fragments.forEach { fragment ->
 *             // 访问片段内容
 *         }
 *     }
 * }
 *
 * packageView.accept(visitor, Unit)
 * ```
 *
 * ## 可见性
 *
 * 包始终是公开的：
 *
 * ```kotlin
 * override val visibility: DescriptorVisibility
 *     get() = DescriptorVisibilities.PUBLIC
 * ```
 *
 * - 仓颉语言中没有私有包的概念
 * - 所有包都可以被任何模块访问
 * - 访问控制在类/函数级别实现
 *
 * ## 典型使用模式
 *
 * ### 模式 1: 模块中创建包视图
 *
 * ```kotlin
 * class ModuleDescriptorImpl(...) : ModuleDescriptor {
 *     private val packageCache = mutableMapOf<FqName, LazyPackageViewDescriptorImpl>()
 *
 *     override fun getPackage(fqName: FqName): PackageViewDescriptor {
 *         return packageCache.getOrPut(fqName) {
 *             LazyPackageViewDescriptorImpl(
 *                 module = this,
 *                 fqName = fqName,
 *                 storageManager = storageManager
 *             )
 *         }
 *     }
 * }
 * ```
 *
 * ### 模式 2: 查找包中的符号
 *
 * ```kotlin
 * fun resolveImport(importPath: String): ClassifierDescriptor? {
 *     // import com.example.MyClass
 *     val parts = importPath.split(".")
 *     val className = parts.last()
 *     val packageName = parts.dropLast(1).joinToString(".")
 *
 *     // 1. 获取包视图（延迟初始化）
 *     val packageView = module.getPackage(FqName(packageName))
 *
 *     if (packageView.isEmpty()) {
 *         return null // 包不存在
 *     }
 *
 *     // 2. 在包的成员作用域中查找类（首次访问触发作用域构建）
 *     return packageView.memberScope.getContributedClassifier(
 *         Name.identifier(className),
 *         location
 *     )
 * }
 * ```
 *
 * ### 模式 3: 递归浏览包层次
 *
 * ```kotlin
 * fun printPackageTree(packageView: PackageViewDescriptor, indent: Int = 0) {
 *     val prefix = "  ".repeat(indent)
 *     println("$prefix${packageView.fqName}")
 *
 *     if (!packageView.isEmpty()) {
 *         // 获取成员（触发 memberScope 初始化）
 *         val members = packageView.memberScope.getContributedDescriptors(
 *             DescriptorKindFilter.CLASSIFIERS
 *         )
 *         members.forEach { member ->
 *             println("$prefix  - ${member.name}")
 *         }
 *
 *         // 递归访问子包
 *         val subpackages = packageView.memberScope.getContributedDescriptors(
 *             DescriptorKindFilter.PACKAGES
 *         )
 *         subpackages.filterIsInstance<PackageViewDescriptor>().forEach { subpackage ->
 *             printPackageTree(subpackage, indent + 1)
 *         }
 *     }
 * }
 * ```
 *
 * ## 常见陷阱
 *
 * ### 1. 过早访问 memberScope
 *
 * ```kotlin
 * // 错误示例：在初始化阶段访问 memberScope
 * class BadDescriptor(...) {
 *     init {
 *         val packageView = module.getPackage(fqName)
 *         // 可能导致循环初始化或 NPE
 *         val members = packageView.memberScope.getContributedDescriptors()
 *     }
 * }
 *
 * // 正确做法：延迟到实际需要时访问
 * class GoodDescriptor(...) {
 *     private val members by lazy {
 *         val packageView = module.getPackage(fqName)
 *         packageView.memberScope.getContributedDescriptors()
 *     }
 * }
 * ```
 *
 * ### 2. 忽略 isEmpty 检查
 *
 * ```kotlin
 * // 低效示例：总是访问 memberScope
 * val packageView = module.getPackage(fqName)
 * val members = packageView.memberScope.getContributedDescriptors()
 * if (members.isEmpty()) {
 *     // 对于空包，已经浪费了作用域构建成本
 * }
 *
 * // 高效做法：先检查 isEmpty
 * val packageView = module.getPackage(fqName)
 * if (!packageView.isEmpty()) {
 *     val members = packageView.memberScope.getContributedDescriptors()
 *     // 处理成员
 * }
 * ```
 *
 * ### 3. 假设包总是存在
 *
 * ```kotlin
 * // 危险示例：未检查包是否为空
 * val packageView = module.getPackage(FqName("nonexistent.package"))
 * val className = packageView.memberScope.getContributedClassifier(name, location)
 * // packageView 不为 null，但 isEmpty() 为 true
 * // memberScope 返回 MemberScope.Empty，className 为 null
 *
 * // 安全做法
 * val packageView = module.getPackage(FqName("com.example"))
 * if (!packageView.isEmpty()) {
 *     val className = packageView.memberScope.getContributedClassifier(name, location)
 *     // 处理 className
 * }
 * ```
 *
 * ## 线程安全性
 *
 * 线程安全性完全取决于传入的 [StorageManager]：
 *
 * ### 无锁模式（单线程）
 *
 * ```kotlin
 * val packageView = LazyPackageViewDescriptorImpl(
 *     module = module,
 *     fqName = fqName,
 *     storageManager = LockBasedStorageManager.NO_LOCKS
 * )
 * // 仅适用于单线程场景，多线程可能导致重复初始化
 * ```
 *
 * ### 带锁模式（多线程）
 *
 * ```kotlin
 * val packageView = LazyPackageViewDescriptorImpl(
 *     module = module,
 *     fqName = fqName,
 *     storageManager = LockBasedStorageManager("PackageView:${fqName}")
 * )
 * // 线程安全，保证只初始化一次，适用于 IDE 多线程环境
 * ```
 *
 * ## 调试技巧
 *
 * ### 检查包是否已初始化
 *
 * 由于字段是私有的，无法直接检查。可以在工厂函数中添加日志：
 *
 * ```kotlin
 * val packageView = LazyPackageViewDescriptorImpl(
 *     module = module,
 *     fqName = fqName,
 *     storageManager = object : StorageManager by originalStorageManager {
 *         override fun <T> createLazyValue(computable: () -> T): NotNullLazyValue<T> {
 *             println("Creating lazy value for $fqName")
 *             return originalStorageManager.createLazyValue {
 *                 println("Computing lazy value for $fqName")
 *                 computable()
 *             }
 *         }
 *     }
 * )
 * ```
 *
 * ### 追踪初始化顺序
 *
 * 在 memberScope 工厂函数中添加堆栈跟踪：
 *
 * ```kotlin
 * override val memberScope: MemberScope = LazyScopeAdapter(storageManager) {
 *     println("Initializing memberScope for $fqName")
 *     Thread.currentThread().stackTrace.take(10).forEach { println(it) }
 *     // ... 正常逻辑
 * }
 * ```
 *
 * ## 示例：完整的包解析流程
 *
 * ```kotlin
 * // 1. 模块初始化
 * val module = ModuleDescriptorImpl(
 *     name = Name.identifier("myModule"),
 *     storageManager = LockBasedStorageManager("Module"),
 *     packageFragmentProvider = myProvider
 * )
 *
 * // 2. 获取包视图（创建 LazyPackageViewDescriptorImpl）
 * val packageView = module.getPackage(FqName("com.example"))
 * // 此时仅创建了对象，未加载任何内容
 *
 * // 3. 检查包是否为空（触发 empty 惰性计算）
 * if (packageView.isEmpty()) {
 *     println("Package is empty")
 *     return
 * }
 * // 首次访问 empty，调用 packageFragmentProvider.isEmpty()
 *
 * // 4. 访问成员作用域（触发 memberScope 惰性构建）
 * val memberScope = packageView.memberScope
 * // 首次访问：
 * // - 加载 fragments（触发 fragments 惰性加载）
 * // - 为每个 fragment 获取 getMemberScope()
 * // - 创建 SubpackagesScope
 * // - 通过 ChainedMemberScope.create 组合所有作用域
 *
 * // 5. 查找符号
 * val myClass = memberScope.getContributedClassifier(
 *     Name.identifier("MyClass"),
 *     location
 * )
 * // 在组合作用域中查找（遍历所有 fragment scopes 和 subpackage scope）
 *
 * // 6. 后续访问（所有字段已缓存）
 * val anotherClass = memberScope.getContributedClassifier(
 *     Name.identifier("AnotherClass"),
 *     location
 * )
 * // 直接使用缓存的 memberScope，无延迟
 * ```
 *
 * @property module 所属的模块描述符，提供 PackageFragmentProvider
 * @property fqName 包的完全限定名
 * @property storageManager 存储管理器，控制延迟值的线程安全和失效策略
 * @property empty 惰性判断包是否为空（无成员、无子包）
 * @property fragments 惰性加载的包片段列表（来自不同源或库）
 * @property memberScope 惰性构建的成员作用域（组合所有片段和子包）
 * @property containingDeclaration 父包视图（根包返回 null）
 * @property visibility 包的可见性（始终为 PUBLIC）
 *
 * @see PackageViewDescriptor 实现的接口
 * @see LazyScopeAdapter 用于延迟构建 memberScope
 * @see ChainedMemberScope 用于组合多个作用域
 * @see SubpackagesScope 提供子包访问
 * @see StorageManager 延迟值管理器
 * @see PackageFragmentDescriptor 包片段描述符
 */
class LazyPackageViewDescriptorImpl(
    override val module: ModuleDescriptorImpl,
    override val fqName: FqName,
    val storageManager: StorageManager
) : DeclarationDescriptorImpl(Annotations.EMPTY, fqName.shortNameOrSpecial()), PackageViewDescriptor {
    override fun <R, D> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D): R =
        visitor.visitPackageViewDescriptor(this, data!!)


    val empty: Boolean by storageManager.createLazyValue {
        module.packageFragmentProvider.isEmpty(fqName)
    }

    override fun isEmpty(): Boolean = empty


    override val visibility: DescriptorVisibility
        get() {
            return DescriptorVisibilities.PUBLIC
        }


    override val containingDeclaration: PackageViewDescriptor?
        get() = if (fqName.isRoot) null else module.getPackage(fqName.parent())
    override val memberScope: MemberScope = LazyScopeAdapter(storageManager) {
        if (isEmpty()) {
            MemberScope.Empty
        } else {
            val scopes = fragments.map { it.getMemberScope() } + SubpackagesScope(module, fqName)
            ChainedMemberScope.create("package view scope for $fqName in ${module.name}", scopes)
        }
    }
    override val fragments: List<PackageFragmentDescriptor> by storageManager.createLazyValue {
        module.packageFragmentProvider.packageFragments(fqName)
    }

}




