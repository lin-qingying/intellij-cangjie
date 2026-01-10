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

package org.cangnova.cangjie.descriptors.impl

import org.cangnova.cangjie.descriptors.ModuleDescriptor
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.resolve.scopes.MemberScope

/**
 * 可变包片段描述符 - 提供空成员作用域的简单包片段实现
 *
 * MutablePackageFragmentDescriptor 是 [PackageFragmentDescriptorImpl] 的一个最小实现，
 * 专门用于表示一个空的或占位符的包片段。它总是返回 [MemberScope.Empty]，表示该包片段
 * 当前不包含任何成员（类、函数、变量等）。
 *
 * ## 核心功能
 *
 * ### 空成员作用域
 *
 * 覆盖 [getMemberScope] 方法，始终返回空作用域：
 * - 不包含任何类、接口、类型别名
 * - 不包含任何函数或变量
 * - 所有符号查找都返回空结果
 *
 * ### 极简实现
 *
 * 这是一个"骨架"实现：
 * - 仅提供必需的包片段基础结构
 * - 没有延迟初始化逻辑
 * - 没有成员管理能力
 *
 * ## 使用场景
 *
 * ### 1. 占位符包片段
 *
 * 在构建模块结构时，为尚未加载的包创建占位符：
 *
 * ```kotlin
 * // 模块初始化阶段
 * class ModuleBuilder {
 *     fun createPackagePlaceholder(fqName: FqName): PackageFragmentDescriptor {
 *         // 创建一个空的包片段占位符
 *         return MutablePackageFragmentDescriptor(module, fqName)
 *     }
 * }
 *
 * // 使用
 * val placeholder = builder.createPackagePlaceholder(FqName("com.example"))
 * // placeholder.getMemberScope() 返回空作用域
 * // 后续可能会替换为实际的包片段
 * ```
 *
 * ### 2. 空包的表示
 *
 * 表示一个确实为空的包（不包含任何声明）：
 *
 * ```kotlin
 * // 项目结构：
 * // src/
 * //   com/
 * //     example/
 * //       empty/    <- 空目录，没有 .cj 文件
 * //       util/
 * //         Utils.cj
 *
 * // 为 com.example.empty 创建包片段
 * val emptyPackage = MutablePackageFragmentDescriptor(
 *     module,
 *     FqName("com.example.empty")
 * )
 *
 * // isEmpty() 检查
 * if (emptyPackage.getMemberScope() == MemberScope.Empty) {
 *     println("Package is empty")
 * }
 * ```
 *
 * ### 3. 测试和模拟
 *
 * 在单元测试中创建简单的包片段模拟对象：
 *
 * ```kotlin
 * class PackageResolutionTest {
 *     @Test
 *     fun testEmptyPackage() {
 *         val module = createTestModule()
 *         val emptyPackage = MutablePackageFragmentDescriptor(
 *             module,
 *             FqName("test.empty")
 *         )
 *
 *         // 验证空包行为
 *         val members = emptyPackage.getMemberScope().getContributedDescriptors()
 *         assertTrue(members.isEmpty())
 *     }
 * }
 * ```
 *
 * ### 4. 临时包片段创建
 *
 * 在某些构建阶段需要临时的包片段引用：
 *
 * ```kotlin
 * // 反序列化过程中的临时包引用
 * fun deserializePackageReference(fqName: FqName): PackageFragmentDescriptor {
 *     // 首先创建一个空的占位符
 *     val tempFragment = MutablePackageFragmentDescriptor(module, fqName)
 *
 *     // 后续可能会填充实际内容或替换
 *     return tempFragment
 * }
 * ```
 *
 * ## 实现细节
 *
 * ### 继承层次
 *
 * ```
 * DeclarationDescriptor (interface)
 *   ↑
 * PackageFragmentDescriptor (interface)
 *   ↑
 * DeclarationDescriptorNonRootImpl (abstract class)
 *   ↑
 * PackageFragmentDescriptorImpl (abstract class)
 *   ↑
 * MutablePackageFragmentDescriptor (concrete class)
 * ```
 *
 * ### 成员作用域实现
 *
 * ```kotlin
 * override fun getMemberScope(): MemberScope {
 *     return MemberScope.Empty
 * }
 * ```
 *
 * - **MemberScope.Empty**: 单例空作用域对象
 * - 所有查询方法返回空集合
 * - 零内存开销（共享单例）
 * - 无延迟初始化逻辑
 *
 * ### 从父类继承的属性
 *
 * 从 [PackageFragmentDescriptorImpl] 继承：
 * - `fqName`: 包的完全限定名
 * - `containingDeclaration`: 所属模块（ModuleDescriptor）
 * - `source`: 源代码元素（默认为 NO_SOURCE）
 * - `accept`: Visitor 模式支持
 *
 * ## 与其他包片段实现的对比
 *
 * | 特性 | MutablePackageFragmentDescriptor | LazyPackageFragmentDescriptor | SourcePackageFragmentDescriptor |
 * |------|----------------------------------|-------------------------------|----------------------------------|
 * | 成员作用域 | 始终为空 | 延迟加载 | 从源码解析 |
 * | 初始化成本 | 极低（无逻辑） | 低（延迟） | 高（需解析） |
 * | 内存占用 | 极低 | 中等 | 高 |
 * | 可变性 | 不可变（虽然名字叫 Mutable） | 不可变 | 不可变 |
 * | 使用场景 | 占位符、空包 | 按需加载 | 实际源码 |
 *
 * ## 名称说明
 *
 * ### "Mutable" 的含义
 *
 * 尽管类名包含 "Mutable"，但这个类实际上：
 * - **不是真正可变的**：没有提供修改成员的方法
 * - **始终返回空作用域**：不会随时间改变
 *
 * 可能的命名原因：
 * 1. **历史遗留**：最初设计可能计划支持可变性
 * 2. **意图表达**：表示这是一个"可以被替换"的占位符
 * 3. **区分用途**：与只读的包片段区分开来
 *
 * 更准确的名称可能是：
 * - `EmptyPackageFragmentDescriptor`
 * - `PlaceholderPackageFragmentDescriptor`
 * - `StubPackageFragmentDescriptor`
 *
 * ## 性能特性
 *
 * ### 极低开销
 *
 * - **创建成本**: O(1)，仅调用父类构造器
 * - **内存占用**: 最小（仅存储 fqName 和 module 引用）
 * - **查询成本**: O(1)，直接返回空作用域单例
 *
 * ### 适合大量创建
 *
 * 由于开销极低，适合在以下场景大量创建：
 * - 模块初始化时为所有可能的包创建占位符
 * - 包层次树的构建
 * - 缓存失效后的临时替换
 *
 * ## 注意事项
 *
 * 1. **不支持成员添加**：没有提供添加类、函数等成员的方法
 * 2. **始终为空**：无论何时查询，都返回空作用域
 * 3. **不可替换成员**：一旦创建，成员作用域固定为空
 * 4. **非真正可变**：尽管名字叫 Mutable，但实际上是不可变的
 *
 * ## 典型使用模式
 *
 * ```kotlin
 * // 模式 1: 创建包层次占位符
 * fun createPackageHierarchy(
 *     module: ModuleDescriptor,
 *     rootPackage: FqName
 * ): Map<FqName, PackageFragmentDescriptor> {
 *     val packages = mutableMapOf<FqName, PackageFragmentDescriptor>()
 *
 *     // 为包层次中的每个级别创建占位符
 *     var current = rootPackage
 *     while (!current.isRoot) {
 *         packages[current] = MutablePackageFragmentDescriptor(module, current)
 *         current = current.parent()
 *     }
 *
 *     return packages
 * }
 *
 * // 模式 2: 临时包引用
 * fun getOrCreatePackageFragment(
 *     module: ModuleDescriptor,
 *     fqName: FqName,
 *     cache: MutableMap<FqName, PackageFragmentDescriptor>
 * ): PackageFragmentDescriptor {
 *     return cache.getOrPut(fqName) {
 *         // 创建临时占位符
 *         MutablePackageFragmentDescriptor(module, fqName)
 *     }
 * }
 *
 * // 模式 3: 空包检测辅助
 * fun isEmptyPackage(fragment: PackageFragmentDescriptor): Boolean {
 *     return fragment is MutablePackageFragmentDescriptor ||
 *            fragment.getMemberScope() == MemberScope.Empty
 * }
 *
 * // 模式 4: 测试中的模拟对象
 * @Test
 * fun testPackageResolution() {
 *     val module = mock<ModuleDescriptor>()
 *     val packageFqName = FqName("test.package")
 *
 *     // 创建简单的空包片段用于测试
 *     val fragment = MutablePackageFragmentDescriptor(module, packageFqName)
 *
 *     // 验证行为
 *     assertEquals(packageFqName, fragment.fqName)
 *     assertEquals(module, fragment.containingDeclaration)
 *     assertTrue(fragment.getMemberScope() == MemberScope.Empty)
 * }
 * ```
 *
 * ## 改进方向
 *
 * ### 潜在增强
 *
 * 如果需要真正的可变性，可以考虑：
 *
 * ```kotlin
 * // 示例：真正可变的包片段（假设的实现）
 * class TrulyMutablePackageFragmentDescriptor(
 *     module: ModuleDescriptor,
 *     fqName: FqName
 * ) : PackageFragmentDescriptorImpl(module, fqName) {
 *     private val _members = mutableListOf<DeclarationDescriptor>()
 *     private var _memberScope: MemberScope? = null
 *
 *     fun addMember(descriptor: DeclarationDescriptor) {
 *         _members.add(descriptor)
 *         _memberScope = null  // 失效缓存
 *     }
 *
 *     override fun getMemberScope(): MemberScope {
 *         return _memberScope ?: createMemberScope().also { _memberScope = it }
 *     }
 *
 *     private fun createMemberScope(): MemberScope {
 *         // 基于 _members 创建作用域
 *     }
 * }
 * ```
 *
 * 但当前的简单实现已足够满足占位符和空包的需求。
 *
 * ## 示例：完整的包层次构建
 *
 * ```kotlin
 * // 场景：为模块创建完整的包层次结构
 *
 * class PackageHierarchyBuilder(private val module: ModuleDescriptor) {
 *     private val fragments = mutableMapOf<FqName, PackageFragmentDescriptor>()
 *
 *     fun ensurePackageExists(fqName: FqName): PackageFragmentDescriptor {
 *         // 1. 检查缓存
 *         fragments[fqName]?.let { return it }
 *
 *         // 2. 确保父包存在
 *         if (!fqName.isRoot) {
 *             ensurePackageExists(fqName.parent())
 *         }
 *
 *         // 3. 创建当前包的占位符
 *         val fragment = MutablePackageFragmentDescriptor(module, fqName)
 *         fragments[fqName] = fragment
 *
 *         return fragment
 *     }
 *
 *     fun getPackageHierarchy(): Map<FqName, PackageFragmentDescriptor> {
 *         return fragments.toMap()
 *     }
 * }
 *
 * // 使用
 * val builder = PackageHierarchyBuilder(module)
 * builder.ensurePackageExists(FqName("com.example.util"))
 *
 * // 结果：创建了以下占位符包片段
 * // - com
 * // - com.example
 * // - com.example.util
 *
 * val hierarchy = builder.getPackageHierarchy()
 * hierarchy.forEach { (fqName, fragment) ->
 *     println("$fqName: ${fragment.getMemberScope()}")
 *     // 输出: com: Empty
 *     //      com.example: Empty
 *     //      com.example.util: Empty
 * }
 * ```
 *
 * @param module 所属的模块描述符
 * @param fqName 包的完全限定名
 *
 * @property module 所属模块（继承自父类）
 * @property fqName 包的完全限定名（继承自父类）
 *
 * @see PackageFragmentDescriptorImpl 父类，提供基础实现
 * @see MemberScope.Empty 空成员作用域单例
 * @see PackageFragmentDescriptor 实现的接口
 */
class MutablePackageFragmentDescriptor(
    module: ModuleDescriptor,
    fqName: FqName
) : PackageFragmentDescriptorImpl(module, fqName) {

    override fun getMemberScope(): MemberScope {
        return MemberScope.Empty
    }
}
