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

import org.cangnova.cangjie.descriptors.DeclarationDescriptorVisitor
import org.cangnova.cangjie.descriptors.ModuleDescriptor
import org.cangnova.cangjie.descriptors.PackageFragmentDescriptor
import org.cangnova.cangjie.descriptors.SourceElement
import org.cangnova.cangjie.descriptors.annotations.Annotations
import org.cangnova.cangjie.name.FqName

/**
 * 包片段描述符抽象基类 - 为包的片段提供统一的基础实现
 *
 * PackageFragmentDescriptorImpl 是 [PackageFragmentDescriptor] 接口的抽象实现，表示一个包的片段。
 * 在模块化系统中，同一个包可能分散在多个源（源码、不同的库、JDK 等），每个源贡献一个包片段。
 * 此类提供了包片段的通用基础设施，包括模块关联、完全限定名管理和访问者模式支持。
 *
 * ## 核心概念
 *
 * ### 包片段（Package Fragment）
 *
 * **包片段**是包的一部分，来自特定的源：
 * - 源码包片段：从项目源码中解析的声明
 * - 库包片段：从编译后的库（.jar、.cjpm）加载的声明
 * - 内置包片段：语言内置类型和函数
 * - 虚拟包片段：IDE 或工具生成的临时声明
 *
 * 多个片段组合形成完整的包视图。
 *
 * ### 为什么需要包片段？
 *
 * 1. **模块化**：不同模块可能贡献同一包的不同部分
 * 2. **源码隔离**：区分来自源码、库、JDK 的声明
 * 3. **增量编译**：仅重新编译变化的片段
 * 4. **延迟加载**：按需加载库的包片段
 *
 * ## 核心功能
 *
 * ### 模块关联
 *
 * [containingDeclaration] 收窄为 [ModuleDescriptor]：
 * - 每个包片段属于特定的模块
 * - 提供类型安全的模块访问
 * - 覆盖父类的通用 `DeclarationDescriptor` 类型
 *
 * ### 完全限定名管理
 *
 * [fqName] 属性存储包的完全限定名：
 * - 例如：`org.cangnova.cangjie.psi`
 * - 用于包的唯一标识
 * - 从 fqName 派生短名称（name 属性）
 *
 * ### 访问者模式支持
 *
 * [accept] 方法调用访问者的 [DeclarationDescriptorVisitor.visitPackageFragmentDescriptor]：
 * - 支持遍历包片段树
 * - 区分包片段和其他声明类型
 *
 * ### 源码位置
 *
 * [source] 固定为 [SourceElement.NO_SOURCE]：
 * - 包片段本身没有直接的源码位置
 * - 包中的声明（类、函数）有自己的源码位置
 * - 覆盖父类以提供固定值
 *
 * ### 调试字符串
 *
 * [toString] 优化避免捕获模块引用：
 * - 防止内存泄漏
 * - 提供清晰的调试信息：`package com.example of module myModule`
 *
 * ## 使用场景
 *
 * ### 1. 源码包片段
 *
 * 从项目源码中解析的包片段：
 *
 * ```kotlin
 * // 源码结构:
 * // src/
 * //   com/
 * //     example/
 * //       MyClass.cj
 * //       Utils.cj
 *
 * class SourcePackageFragment(
 *     module: ModuleDescriptor,
 *     fqName: FqName,
 *     private val sourceFiles: List<CjFile>
 * ) : PackageFragmentDescriptorImpl(module, fqName) {
 *     override fun getMemberScope(): MemberScope {
 *         // 从源文件构建成员作用域
 *         return SourcePackageFragmentScope(this, sourceFiles)
 *     }
 * }
 *
 * // 使用
 * val module: ModuleDescriptor = ...
 * val packageFqName = FqName("com.example")
 * val sourceFiles = listOf(myClassFile, utilsFile)
 *
 * val sourceFragment = SourcePackageFragment(
 *     module = module,
 *     fqName = packageFqName,
 *     sourceFiles = sourceFiles
 * )
 *
 * // 获取包中的类
 * val myClass = sourceFragment.getMemberScope()
 *     .getContributedClassifier(Name.identifier("MyClass"), location)
 * ```
 *
 * ### 2. 库包片段
 *
 * 从编译后的库加载的包片段：
 *
 * ```kotlin
 * class LibraryPackageFragment(
 *     module: ModuleDescriptor,
 *     fqName: FqName,
 *     private val libraryFile: VirtualFile
 * ) : PackageFragmentDescriptorImpl(module, fqName) {
 *     // 延迟加载成员作用域
 *     private val memberScope by lazy {
 *         deserializePackageFromLibrary(libraryFile, fqName)
 *     }
 *
 *     override fun getMemberScope(): MemberScope = memberScope
 * }
 *
 * // 使用示例
 * // 项目依赖: stdlib.cjpm
 * val stdlibModule: ModuleDescriptor = ...
 * val stdlibFile = VirtualFileManager.getInstance()
 *     .findFileByUrl("jar://stdlib.cjpm!/")
 *
 * val collectionPackage = LibraryPackageFragment(
 *     module = stdlibModule,
 *     fqName = FqName("std.collection"),
 *     libraryFile = stdlibFile
 * )
 *
 * // 查找 ArrayList 类
 * val arrayListClass = collectionPackage.getMemberScope()
 *     .getContributedClassifier(Name.identifier("ArrayList"), location)
 * ```
 *
 * ### 3. 空包片段（占位符）
 *
 * 使用 [MutablePackageFragmentDescriptor] 表示空包或占位符：
 *
 * ```kotlin
 * // MutablePackageFragmentDescriptor 继承自此类
 * class MutablePackageFragmentDescriptor(
 *     module: ModuleDescriptor,
 *     fqName: FqName
 * ) : PackageFragmentDescriptorImpl(module, fqName) {
 *     override fun getMemberScope(): MemberScope {
 *         return MemberScope.Empty
 *     }
 * }
 *
 * // 使用：创建包层次占位符
 * fun createPackageHierarchy(
 *     module: ModuleDescriptor,
 *     targetPackage: FqName
 * ): List<PackageFragmentDescriptor> {
 *     val fragments = mutableListOf<PackageFragmentDescriptor>()
 *     var current = targetPackage
 *
 *     while (!current.isRoot) {
 *         fragments.add(
 *             MutablePackageFragmentDescriptor(module, current)
 *         )
 *         current = current.parent()
 *     }
 *
 *     return fragments.reversed()
 * }
 * ```
 *
 * ### 4. 包片段的组合（包视图）
 *
 * 多个包片段组合为完整的包视图：
 *
 * ```kotlin
 * class PackageViewDescriptorImpl(
 *     override val module: ModuleDescriptor,
 *     override val fqName: FqName
 * ) : PackageViewDescriptor {
 *     // 收集所有片段
 *     override val fragments: List<PackageFragmentDescriptor> by lazy {
 *         module.packageFragmentProvider.packageFragments(fqName)
 *     }
 *
 *     // 组合所有片段的成员作用域
 *     override val memberScope: MemberScope by lazy {
 *         val scopes = fragments.map { it.getMemberScope() }
 *         ChainedMemberScope.create("package $fqName", scopes)
 *     }
 * }
 *
 * // 使用示例
 * // 项目结构:
 * // - src/com/example/MyClass.cj      (源码片段)
 * // - lib-a.jar!/com/example/UtilA.cj (库片段 A)
 * // - lib-b.jar!/com/example/UtilB.cj (库片段 B)
 *
 * val packageView = PackageViewDescriptorImpl(
 *     module = myModule,
 *     fqName = FqName("com.example")
 * )
 *
 * // packageView.fragments 包含 3 个片段
 * // - SourcePackageFragment (MyClass)
 * // - LibraryPackageFragment (UtilA)
 * // - LibraryPackageFragment (UtilB)
 *
 * // packageView.memberScope 包含所有符号
 * val allClasses = packageView.memberScope.getContributedDescriptors(
 *     DescriptorKindFilter.CLASSIFIERS
 * )
 * // 返回: MyClass, UtilA, UtilB
 * ```
 *
 * ## 实现细节
 *
 * ### 继承层次
 *
 * ```
 * DeclarationDescriptorImpl (基础：名称、注解、original)
 *   ↑
 * DeclarationDescriptorNonRootImpl (添加：containingDeclaration、source)
 *   ↑
 * PackageFragmentDescriptorImpl (特化为包片段)
 *   ↑
 * 具体实现（SourcePackageFragment、LibraryPackageFragment、MutablePackageFragmentDescriptor）
 * ```
 *
 * ### 构造器参数
 *
 * ```kotlin
 * abstract class PackageFragmentDescriptorImpl(
 *     module: ModuleDescriptor,        // 所属模块
 *     final override val fqName: FqName  // 包的完全限定名
 * ) : DeclarationDescriptorNonRootImpl(
 *     module,                           // containingDeclaration
 *     Annotations.EMPTY,                // 包片段无注解
 *     fqName.shortNameOrSpecial(),      // 短名称
 *     SourceElement.NO_SOURCE           // 包片段无源码位置
 * ), PackageFragmentDescriptor
 * ```
 *
 * **设计要点**:
 * - **fqName**: 声明为 `final override`，确保不被子类修改
 * - **Annotations.EMPTY**: 包片段本身不支持注解
 * - **SourceElement.NO_SOURCE**: 包片段无对应的源码元素
 * - **shortNameOrSpecial**: 从 fqName 提取最后一段作为 name
 *
 * ### debugString 字段
 *
 * ```kotlin
 * private val debugString: String = "package $fqName of $module"
 * ```
 *
 * **为什么不内联？**
 *
 * 注释中解释：*"Not inlined in order to not capture ref on 'module'"*
 *
 * - 避免在 toString 中捕获 module 引用
 * - 防止通过调试字符串意外延长模块的生命周期
 * - 在对象创建时计算一次，存储结果
 *
 * ### containingDeclaration 类型收窄
 *
 * ```kotlin
 * override val containingDeclaration: ModuleDescriptor
 *     get() = super.containingDeclaration as ModuleDescriptor
 * ```
 *
 * - 将返回类型从 `DeclarationDescriptor` 收窄为 `ModuleDescriptor`
 * - 提供类型安全的模块访问
 * - 安全转换（因为构造器保证传入的是 ModuleDescriptor）
 *
 * ### accept 实现
 *
 * ```kotlin
 * override fun <R, D> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D?): R {
 *     return visitor.visitPackageFragmentDescriptor(this, data!!)
 * }
 * ```
 *
 * - 调用访问者的专门方法处理包片段
 * - 强制 data 非空（使用 `data!!`）
 *
 * ### source 覆盖
 *
 * ```kotlin
 * override val source: SourceElement = SourceElement.NO_SOURCE
 * ```
 *
 * - 显式覆盖父类属性
 * - 包片段本身没有源码位置
 * - 包中的声明有各自的源码位置
 *
 * ## 与其他描述符的对比
 *
 * | 特性 | PackageFragmentDescriptorImpl | PackageViewDescriptor | MutablePackageFragmentDescriptor |
 * |------|-------------------------------|----------------------|----------------------------------|
 * | 表示 | 包的一个片段 | 包的完整视图 | 空包/占位符片段 |
 * | containingDeclaration | ModuleDescriptor | ModuleDescriptor | ModuleDescriptor |
 * | fragments | 不适用（自身是片段） | 多个片段列表 | 不适用 |
 * | memberScope | 抽象（子类实现） | 组合所有片段的作用域 | 固定返回 Empty |
 * | 使用场景 | 基类，不直接实例化 | 提供包的统一视图 | 占位符 |
 *
 * ## 包片段的生命周期
 *
 * ### 创建
 *
 * ```kotlin
 * // 模块初始化时创建
 * class PackageFragmentProviderImpl(
 *     private val module: ModuleDescriptor
 * ) : PackageFragmentProvider {
 *     override fun packageFragments(fqName: FqName): List<PackageFragmentDescriptor> {
 *         val fragments = mutableListOf<PackageFragmentDescriptor>()
 *
 *         // 添加源码片段
 *         sourceFiles[fqName]?.let {
 *             fragments.add(SourcePackageFragment(module, fqName, it))
 *         }
 *
 *         // 添加库片段
 *         libraryFiles[fqName]?.forEach { libFile ->
 *             fragments.add(LibraryPackageFragment(module, fqName, libFile))
 *         }
 *
 *         return fragments
 *     }
 * }
 * ```
 *
 * ### 使用
 *
 * ```kotlin
 * // 通过 PackageViewDescriptor 统一访问
 * val packageView = module.getPackage(FqName("com.example"))
 * val fragments = packageView.fragments  // 获取所有片段
 * val memberScope = packageView.memberScope  // 组合的作用域
 * ```
 *
 * ### 销毁
 *
 * 随模块一起销毁，通常在：
 * - 项目关闭
 * - 模块卸载
 * - IDE 缓存失效
 *
 * ## 性能特性
 *
 * ### 内存占用
 *
 * 基础开销（继承自父类）+ 以下字段：
 * - **fqName**: 引用（8 字节）
 * - **debugString**: 字符串引用（8 字节）
 * - **总计**: 约 64 字节 + fqName 和 debugString 的实际大小
 *
 * ### 延迟加载优化
 *
 * 子类通常延迟加载 memberScope：
 *
 * ```kotlin
 * class LazyLibraryPackageFragment(...) : PackageFragmentDescriptorImpl(...) {
 *     private val memberScope by lazy {
 *         // 仅在首次访问时反序列化
 *         deserializePackage()
 *     }
 *
 *     override fun getMemberScope(): MemberScope = memberScope
 * }
 * ```
 *
 * ## 典型使用模式
 *
 * ### 模式 1: 实现源码包片段
 *
 * ```kotlin
 * class SourcePackageFragmentImpl(
 *     module: ModuleDescriptor,
 *     fqName: FqName,
 *     private val files: List<CjFile>
 * ) : PackageFragmentDescriptorImpl(module, fqName) {
 *
 *     override fun getMemberScope(): MemberScope {
 *         // 收集所有文件中的顶层声明
 *         val descriptors = mutableListOf<DeclarationDescriptor>()
 *
 *         for (file in files) {
 *             file.declarations.forEach { declaration ->
 *                 descriptors.add(createDescriptor(declaration, this))
 *             }
 *         }
 *
 *         return SimpleMemberScope(descriptors)
 *     }
 * }
 * ```
 *
 * ### 模式 2: 查找包中的特定符号
 *
 * ```kotlin
 * fun findClassInPackageFragment(
 *     fragment: PackageFragmentDescriptor,
 *     className: Name
 * ): ClassDescriptor? {
 *     return fragment.getMemberScope()
 *         .getContributedClassifier(className, NoLookupLocation.FROM_BACKEND) as? ClassDescriptor
 * }
 * ```
 *
 * ### 模式 3: 收集包的所有片段
 *
 * ```kotlin
 * fun collectAllFragments(
 *     module: ModuleDescriptor,
 *     packageFqName: FqName
 * ): List<PackageFragmentDescriptor> {
 *     return module.packageFragmentProvider.packageFragments(packageFqName)
 * }
 * ```
 *
 * ### 模式 4: 遍历包片段树
 *
 * ```kotlin
 * val visitor = object : DeclarationDescriptorVisitor<Unit, Int> {
 *     override fun visitPackageFragmentDescriptor(
 *         descriptor: PackageFragmentDescriptor,
 *         depth: Int
 *     ) {
 *         println("${"  ".repeat(depth)}Package fragment: ${descriptor.fqName}")
 *         println("${"  ".repeat(depth)}Module: ${descriptor.containingDeclaration.name}")
 *
 *         // 遍历成员
 *         descriptor.getMemberScope().getContributedDescriptors().forEach {
 *             it.accept(this, depth + 1)
 *         }
 *     }
 *
 *     // 其他 visit 方法...
 * }
 *
 * packageFragment.accept(visitor, 0)
 * ```
 *
 * ## 常见陷阱
 *
 * ### 1. 混淆包片段和包视图
 *
 * ```kotlin
 * // 错误示例：期望获取完整的包内容
 * val fragment = module.packageFragmentProvider.packageFragments(fqName).first()
 * val allClasses = fragment.getMemberScope().getContributedDescriptors()
 * // 错误：仅获取了第一个片段的内容，可能遗漏其他片段
 *
 * // 正确做法：使用 PackageViewDescriptor
 * val packageView = module.getPackage(fqName)
 * val allClasses = packageView.memberScope.getContributedDescriptors()
 * // 正确：获取了所有片段的组合内容
 * ```
 *
 * ### 2. 在 toString 中捕获模块引用
 *
 * ```kotlin
 * // 错误示例（已在基类中避免）
 * override fun toString(): String {
 *     return "package $fqName of ${containingDeclaration.name}"
 *     // 问题：每次调用都访问 containingDeclaration，可能延长其生命周期
 * }
 *
 * // 正确做法（基类实现）
 * private val debugString = "package $fqName of $module"
 * override fun toString() = debugString
 * ```
 *
 * ### 3. 修改 fqName
 *
 * ```kotlin
 * // 编译错误：fqName 是 final
 * class WrongPackageFragment(
 *     module: ModuleDescriptor,
 *     fqName: FqName
 * ) : PackageFragmentDescriptorImpl(module, fqName) {
 *     override val fqName = FqName("different.package")  // 错误！
 * }
 * ```
 *
 * ## 线程安全性
 *
 * - **fqName**: 不可变，线程安全
 * - **debugString**: 不可变，线程安全
 * - **containingDeclaration**: 不可变引用，线程安全
 * - **getMemberScope**: 子类责任，通常使用延迟初始化需要考虑线程安全
 *
 * ## 调试技巧
 *
 * ### 检查片段所属模块
 *
 * ```kotlin
 * fun checkFragmentModule(fragment: PackageFragmentDescriptor) {
 *     val module = fragment.containingDeclaration
 *     println("Fragment: ${fragment.fqName}")
 *     println("Module: ${module.name}")
 *     println("Module type: ${module.javaClass.simpleName}")
 * }
 * ```
 *
 * ### 比较不同片段的内容
 *
 * ```kotlin
 * fun compareFragments(
 *     fragment1: PackageFragmentDescriptor,
 *     fragment2: PackageFragmentDescriptor
 * ) {
 *     println("Fragment 1: ${fragment1.fqName} from ${fragment1.containingDeclaration.name}")
 *     val members1 = fragment1.getMemberScope().getContributedDescriptors()
 *     println("Members: ${members1.map { it.name }}")
 *
 *     println("\nFragment 2: ${fragment2.fqName} from ${fragment2.containingDeclaration.name}")
 *     val members2 = fragment2.getMemberScope().getContributedDescriptors()
 *     println("Members: ${members2.map { it.name }}")
 * }
 * ```
 *
 * ## 示例：完整的库包片段实现
 *
 * ```kotlin
 * // 从编译后的库文件加载包片段
 * class DeserializedPackageFragment(
 *     module: ModuleDescriptor,
 *     fqName: FqName,
 *     private val storageManager: StorageManager,
 *     private val proto: ProtoBuf.Package,
 *     private val nameResolver: NameResolver
 * ) : PackageFragmentDescriptorImpl(module, fqName) {
 *
 *     // 延迟反序列化成员作用域
 *     private val memberScope by storageManager.createLazyValue {
 *         DeserializedPackageMemberScope(
 *             packageFragment = this,
 *             proto = proto,
 *             nameResolver = nameResolver,
 *             storageManager = storageManager
 *         )
 *     }
 *
 *     override fun getMemberScope(): MemberScope = memberScope()
 *
 *     override fun toString(): String {
 *         return "deserialized package $fqName from ${containingDeclaration.name}"
 *     }
 * }
 *
 * // 使用示例
 * val stdlibModule: ModuleDescriptor = ...
 * val packageProto: ProtoBuf.Package = loadPackageFromJar("stdlib.jar", "std.collection")
 * val nameResolver = NameResolver(packageProto.stringTable, packageProto.qualifiedNameTable)
 *
 * val packageFragment = DeserializedPackageFragment(
 *     module = stdlibModule,
 *     fqName = FqName("std.collection"),
 *     storageManager = LockBasedStorageManager("stdlib"),
 *     proto = packageProto,
 *     nameResolver = nameResolver
 * )
 *
 * // 查找 ArrayList
 * val arrayListClass = packageFragment.getMemberScope()
 *     .getContributedClassifier(Name.identifier("ArrayList"), NoLookupLocation.FROM_BACKEND)
 *
 * println("Found ArrayList: $arrayListClass")
 * ```
 *
 * @param module 所属的模块描述符
 * @param fqName 包的完全限定名（如 org.cangnova.cangjie.psi）
 *
 * @property fqName 包的完全限定名（final，不可被子类修改）
 * @property containingDeclaration 所属模块（类型收窄为 ModuleDescriptor）
 * @property source 源码元素（固定为 NO_SOURCE）
 *
 * @see PackageFragmentDescriptor 实现的接口
 * @see DeclarationDescriptorNonRootImpl 父类
 * @see MutablePackageFragmentDescriptor 空包片段实现
 * @see ModuleDescriptor 模块描述符
 * @see PackageViewDescriptor 包视图（组合多个片段）
 */
abstract class PackageFragmentDescriptorImpl(
    module: ModuleDescriptor,
    final override val fqName: FqName
) : DeclarationDescriptorNonRootImpl(module, Annotations.EMPTY, fqName.shortNameOrSpecial(), SourceElement.NO_SOURCE),
    PackageFragmentDescriptor {
    // Not inlined in order to not capture ref on 'module'
    private val debugString: String = "package $fqName of $module"

    override val containingDeclaration: ModuleDescriptor
        get() = super.containingDeclaration as ModuleDescriptor

    override fun <R, D> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D): R {
        return visitor.visitPackageFragmentDescriptor(this, data!!)

    }


    override val source: SourceElement = SourceElement.NO_SOURCE
    override fun toString(): String = debugString
}