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

package org.cangnova.cangjie.descriptors

import org.cangnova.cangjie.incremental.components.NoLookupLocation
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.resolve.getResolutionAnchorIfAny

/**
 * 跨模块依赖查找分类器 - 在模块及其所有依赖中查找指定的分类器
 *
 * 该函数提供了在模块及其依赖中查找分类器（类、接口、类型别名等）的核心功能。
 * 它支持嵌套类的查找，并考虑了解析锚点（resolution anchor）的情况。
 *
 * ## 核心功能
 *
 * ### 分层查找
 *
 * 1. **顶层分类器查找**：在包的成员作用域中查找第一级名称
 * 2. **嵌套分类器查找**：逐级在类的未替换成员作用域中查找
 *
 * ### 解析锚点支持
 *
 * 使用 [withResolutionAnchor] 优先在解析锚点模块中查找：
 * - 如果存在解析锚点，先在锚点模块中查找
 * - 如果锚点模块查找失败，回退到当前模块查找
 *
 * ## 使用场景
 *
 * ### 1. 反序列化时的类型解析
 *
 * 从编译后的元数据中恢复类型引用：
 *
 * ```kotlin
 * // 反序列化器读取到类型引用: "com.example.MyClass.InnerClass"
 * val classId = ClassId.fromString("com/example/MyClass.InnerClass")
 *
 * // 在模块依赖中查找该类
 * val classifier = module.findClassifierAcrossModuleDependencies(classId)
 *
 * if (classifier is ClassDescriptor) {
 *     // 成功找到类描述符
 *     val type = classifier.defaultType
 * } else {
 *     // 未找到或不是类
 *     reportError("Cannot resolve class: $classId")
 * }
 * ```
 *
 * ### 2. import 语句的符号解析
 *
 * 解析导入的类或类型别名：
 *
 * ```kotlin
 * // 源代码: import com.example.utils.StringUtils
 *
 * val classId = ClassId(
 *     packageFqName = FqName("com.example.utils"),
 *     relativeClassName = FqName("StringUtils")
 * )
 *
 * val classifier = module.findClassifierAcrossModuleDependencies(classId)
 * // 可能是类、接口或类型别名
 * ```
 *
 * ### 3. 嵌套类的查找
 *
 * 查找内部类或嵌套类：
 *
 * ```kotlin
 * // 源代码结构:
 * // class Outer {
 * //     class Inner {
 * //         class DeepNested {}
 * //     }
 * // }
 *
 * val classId = ClassId.fromString("com/example/Outer.Inner.DeepNested")
 *
 * // 查找过程：
 * // 1. 在 com.example 包中查找 Outer
 * // 2. 在 Outer.unsubstitutedMemberScope 中查找 Inner
 * // 3. 在 Inner.unsubstitutedMemberScope 中查找 DeepNested
 * val deepNestedClass = module.findClassifierAcrossModuleDependencies(classId)
 * ```
 *
 * ### 4. 跨依赖库的类型查找
 *
 * 查找依赖库中的类型：
 *
 * ```kotlin
 * // 项目依赖: stdlib.cjpm
 * // 查找标准库中的类型
 *
 * val classId = ClassId(
 *     packageFqName = FqName("std.collection"),
 *     relativeClassName = FqName("ArrayList")
 * )
 *
 * val arrayListClass = module.findClassifierAcrossModuleDependencies(classId)
 * // 在依赖的 stdlib 模块中查找
 * ```
 *
 * ## 实现细节
 *
 * ### 查找算法
 *
 * ```kotlin
 * // 1. 获取包视图描述符
 * val packageView = getPackage(classId.packageFqName)
 * // 例如: classId = "com.example.Outer.Inner"
 * //      packageFqName = "com.example"
 *
 * // 2. 分割相对类名路径
 * val segments = classId.relativeClassName.pathSegments()
 * // 例如: segments = ["Outer", "Inner"]
 *
 * // 3. 查找顶层分类器
 * val topLevelClass = packageView.memberScope.getContributedClassifier(
 *     segments.first(),  // "Outer"
 *     NoLookupLocation.FROM_DESERIALIZATION
 * ) ?: return null
 *
 * // 4. 逐级查找嵌套分类器
 * var result = topLevelClass
 * for (name in segments.subList(1, segments.size)) {  // ["Inner"]
 *     if (result !is ClassDescriptor) return null
 *     result = result.unsubstitutedMemberScope.getContributedClassifier(
 *         name,
 *         NoLookupLocation.FROM_DESERIALIZATION
 *     ) as? ClassDescriptor ?: return null
 * }
 * ```
 *
 * ### NoLookupLocation.FROM_DESERIALIZATION
 *
 * 使用 `FROM_DESERIALIZATION` 位置表示：
 * - 查找来自反序列化过程，不是用户代码
 * - 不需要记录查找位置用于增量编译
 * - 性能优化：跳过不必要的查找跟踪
 *
 * ### 未替换成员作用域
 *
 * 使用 `unsubstitutedMemberScope` 而非 `memberScope`：
 * - 获取原始的类成员，不进行类型参数替换
 * - 例如: `List<T>` 的 `unsubstitutedMemberScope` 包含 `T` 作为类型参数
 * - 而 `List<Int>` 的 `memberScope` 会将 `T` 替换为 `Int`
 *
 * ### 解析锚点机制
 *
 * 通过 [withResolutionAnchor] 支持模块解析锚点：
 * - 某些模块可能有一个"解析锚点"模块
 * - 例如：平台特定模块可能将公共模块设为锚点
 * - 优先在锚点模块中查找，提供更精确的类型解析
 *
 * ## 性能特性
 *
 * ### 时间复杂度
 *
 * - **包查找**: O(1)，通过哈希表查找
 * - **顶层分类器查找**: O(1)，通过名称索引（Stub Index）
 * - **嵌套分类器查找**: O(n)，n 是嵌套深度
 * - **总复杂度**: O(n)，n 通常很小（1-3）
 *
 * ### 优化策略
 *
 * - 使用 Stub Index 加速顶层符号查找
 * - 使用 `NoLookupLocation.FROM_DESERIALIZATION` 跳过查找跟踪
 * - 短路返回：找不到时立即返回 null
 *
 * ## 返回值
 *
 * - **成功**: 返回找到的 [ClassifierDescriptor]
 *   - 可能是 [ClassDescriptor]（类、接口）
 *   - 可能是 [TypeAliasDescriptor]（类型别名）
 * - **失败**: 返回 `null`
 *   - 包不存在
 *   - 顶层分类器不存在
 *   - 中间嵌套类不存在
 *   - 类型不匹配（期望类但找到其他类型）
 *
 * ## 典型使用模式
 *
 * ### 模式 1: 带类型检查的查找
 *
 * ```kotlin
 * val classId = ClassId.fromString("com/example/MyClass")
 * val classifier = module.findClassifierAcrossModuleDependencies(classId)
 *
 * when (classifier) {
 *     is ClassDescriptor -> {
 *         // 找到类或接口
 *         println("Found class: ${classifier.name}")
 *     }
 *     is TypeAliasDescriptor -> {
 *         // 找到类型别名
 *         println("Found type alias: ${classifier.name}")
 *     }
 *     null -> {
 *         // 未找到
 *         println("Classifier not found: $classId")
 *     }
 * }
 * ```
 *
 * ### 模式 2: 安全的嵌套类访问
 *
 * ```kotlin
 * fun findNestedClass(
 *     module: ModuleDescriptor,
 *     packageName: String,
 *     vararg classNames: String
 * ): ClassDescriptor? {
 *     val relativeClassName = FqName.fromSegments(classNames.asList())
 *     val classId = ClassId(FqName(packageName), relativeClassName)
 *
 *     return module.findClassifierAcrossModuleDependencies(classId) as? ClassDescriptor
 * }
 *
 * // 使用
 * val innerClass = findNestedClass(
 *     module,
 *     "com.example",
 *     "Outer", "Middle", "Inner"
 * )
 * ```
 *
 * ### 模式 3: 批量查找多个类
 *
 * ```kotlin
 * fun findClasses(
 *     module: ModuleDescriptor,
 *     classIds: List<ClassId>
 * ): Map<ClassId, ClassifierDescriptor?> {
 *     return classIds.associateWith { classId ->
 *         module.findClassifierAcrossModuleDependencies(classId)
 *     }
 * }
 * ```
 *
 * ## 注意事项
 *
 * 1. **仅查找已声明的分类器**：不会创建虚拟类或错误类
 * 2. **区分大小写**：类名查找区分大小写
 * 3. **需要完整路径**：必须提供从包到目标类的完整路径
 * 4. **不处理泛型参数**：返回原始类型描述符，不包含类型参数替换
 * 5. **线程安全**：依赖于底层作用域和索引的线程安全性
 *
 * ## 常见错误
 *
 * ### 1. 路径分隔符错误
 *
 * ```kotlin
 * // 错误：使用了错误的分隔符
 * val classId = ClassId.fromString("com.example.MyClass")  // 应该用 '/'
 *
 * // 正确
 * val classId = ClassId.fromString("com/example/MyClass")
 * ```
 *
 * ### 2. 混淆包名和类名
 *
 * ```kotlin
 * // 错误：将嵌套类当作包的一部分
 * ClassId(
 *     packageFqName = FqName("com.example.Outer.Inner"),
 *     relativeClassName = FqName("Method")
 * )
 *
 * // 正确
 * ClassId(
 *     packageFqName = FqName("com.example"),
 *     relativeClassName = FqName("Outer.Inner")
 * )
 * ```
 *
 * ## 相关函数
 *
 * @param classId 要查找的分类器的唯一标识符
 * @return 找到的分类器描述符，如果未找到则返回 null
 *
 * @see ClassId 类的唯一标识符
 * @see ClassifierDescriptor 分类器描述符基类
 * @see withResolutionAnchor 解析锚点支持
 * @see findClassAcrossModuleDependencies 仅查找类（不包括类型别名）
 * @see findTypeAliasAcrossModuleDependencies 仅查找类型别名
 * @see findNonGenericClassAcrossDependencies 查找或创建虚拟类
 */
fun ModuleDescriptor.findClassifierAcrossModuleDependencies(classId: ClassId): ClassifierDescriptor? =
    withResolutionAnchor {
        val packageViewDescriptor = getPackage(classId.packageFqName)
        val segments = classId.relativeClassName.pathSegments()
        val topLevelClass = packageViewDescriptor.memberScope.getContributedClassifier(
            segments.first(),
            NoLookupLocation.FROM_DESERIALIZATION
        ) ?: return@withResolutionAnchor null
        var result = topLevelClass
        for (name in segments.subList(1, segments.size)) {
            if (result !is ClassDescriptor) return@withResolutionAnchor null
            result = result.unsubstitutedMemberScope
                .getContributedClassifier(name, NoLookupLocation.FROM_DESERIALIZATION) as? ClassDescriptor
                ?: return@withResolutionAnchor null
        }
        return@withResolutionAnchor result
    }

/**
 * 跨模块依赖查找类 - 在模块及其所有依赖中查找指定的类
 *
 * 这是 [findClassifierAcrossModuleDependencies] 的类型安全变体，仅返回类描述符，
 * 排除类型别名等其他类型的分类器。
 *
 * ## 核心功能
 *
 * - 调用 [findClassifierAcrossModuleDependencies] 查找分类器
 * - 使用类型转换 `as? ClassDescriptor` 过滤结果
 * - 仅返回类或接口，不返回类型别名
 *
 * ## 使用场景
 *
 * ### 1. 反序列化类引用
 *
 * ```kotlin
 * // 元数据中存储的类引用
 * val classId = ClassId.fromString("com/example/MyClass")
 *
 * // 确保找到的是类，而不是类型别名
 * val classDescriptor = module.findClassAcrossModuleDependencies(classId)
 *
 * if (classDescriptor != null) {
 *     // 安全使用类特有的功能
 *     val constructors = classDescriptor.constructors
 *     val superTypes = classDescriptor.typeConstructor.supertypes
 * }
 * ```
 *
 * ### 2. 继承关系分析
 *
 * ```kotlin
 * // 查找父类
 * fun getParentClass(classId: ClassId, module: ModuleDescriptor): ClassDescriptor? {
 *     val cls = module.findClassAcrossModuleDependencies(classId) ?: return null
 *
 *     // 获取第一个父类（排除接口）
 *     return cls.typeConstructor.supertypes
 *         .firstOrNull { !it.constructor.declarationDescriptor.isInterface }
 *         ?.constructor?.declarationDescriptor as? ClassDescriptor
 * }
 * ```
 *
 * ### 3. 实例化检查
 *
 * ```kotlin
 * // 检查类是否可以实例化
 * val classDescriptor = module.findClassAcrossModuleDependencies(classId)
 *
 * if (classDescriptor != null && !classDescriptor.isAbstract && !classDescriptor.isInterface) {
 *     // 类可以实例化
 *     val instance = createInstance(classDescriptor)
 * }
 * ```
 *
 * ## 与 findClassifierAcrossModuleDependencies 的区别
 *
 * | 特性 | findClassAcrossModuleDependencies | findClassifierAcrossModuleDependencies |
 * |------|----------------------------------|--------------------------------------|
 * | 返回类型 | ClassDescriptor? | ClassifierDescriptor? |
 * | 包含类型别名 | 否 | 是 |
 * | 使用场景 | 需要类特有功能 | 通用分类器查找 |
 * | 类型安全 | 更高 | 较低（需要类型检查） |
 *
 * ## 返回值
 *
 * - **成功**: 返回找到的 [ClassDescriptor]（类或接口）
 * - **失败**: 返回 `null`
 *   - 分类器不存在
 *   - 找到的是类型别名而非类
 *
 * ## 典型使用模式
 *
 * ```kotlin
 * // 模式 1: 带默认值的查找
 * val classDescriptor = module.findClassAcrossModuleDependencies(classId)
 *     ?: builtIns.anyClass  // 找不到时使用 Any
 *
 * // 模式 2: 安全调用链
 * val memberScope = module.findClassAcrossModuleDependencies(classId)
 *     ?.unsubstitutedMemberScope
 *     ?: MemberScope.Empty
 *
 * // 模式 3: 检查并报告错误
 * val classDescriptor = module.findClassAcrossModuleDependencies(classId)
 * if (classDescriptor == null) {
 *     trace.report(Errors.CLASS_NOT_FOUND.on(element, classId))
 *     return
 * }
 * ```
 *
 * @param classId 要查找的类的唯一标识符
 * @return 找到的类描述符，如果未找到或找到的不是类则返回 null
 *
 * @see findClassifierAcrossModuleDependencies 查找任意分类器
 * @see findTypeAliasAcrossModuleDependencies 查找类型别名
 * @see ClassDescriptor 类描述符接口
 */
fun ModuleDescriptor.findClassAcrossModuleDependencies(classId: ClassId): ClassDescriptor? =
    findClassifierAcrossModuleDependencies(classId) as? ClassDescriptor

/**
 * 带解析锚点的查找辅助函数 - 优先在解析锚点模块中查找，失败后回退到当前模块
 *
 * 这是一个内联辅助函数，实现了"解析锚点"模式，用于在多个模块间查找符号。
 * 解析锚点通常用于平台特定代码和公共代码的关联。
 *
 * ## 核心功能
 *
 * ### 双层查找策略
 *
 * 1. **优先锚点模块**: 如果存在解析锚点，先在锚点模块中查找
 * 2. **回退当前模块**: 如果锚点模块查找失败，回退到当前模块查找
 * 3. **无锚点情况**: 如果没有锚点，直接在当前模块查找
 *
 * ## 使用场景
 *
 * ### 1. 平台特定模块的符号解析
 *
 * 在多平台项目中，平台特定模块可能引用公共模块中的符号：
 *
 * ```kotlin
 * // 项目结构：
 * // - commonMain (公共代码)
 * //   - class CommonClass
 * // - jvmMain (JVM 平台代码，解析锚点指向 commonMain)
 * //   - fun useCommonClass()
 *
 * // 在 jvmMain 模块中查找 CommonClass
 * val classId = ClassId.fromString("com/example/CommonClass")
 *
 * // withResolutionAnchor 会：
 * // 1. 先在 commonMain 模块中查找
 * // 2. 如果找不到，回退到 jvmMain 模块查找
 * val commonClass = jvmModule.withResolutionAnchor {
 *     findClassifierAcrossModuleDependencies(classId)
 * }
 * ```
 *
 * ### 2. 期望声明和实际声明的关联
 *
 * ```kotlin
 * // 在 Kotlin 多平台中：
 * // commonMain: expect class Platform
 * // jvmMain: actual class Platform
 *
 * // 查找 Platform 类时，优先在公共模块中查找 expect 声明
 * val platformClass = actualModule.withResolutionAnchor {
 *     findClassAcrossModuleDependencies(platformClassId)
 * }
 * ```
 *
 * ### 3. 依赖解析优化
 *
 * ```kotlin
 * // 在有解析锚点的情况下，可以优先解析更权威的符号定义
 * fun resolveSymbol(module: ModuleDescriptor, symbolId: ClassId): ClassifierDescriptor? {
 *     return module.withResolutionAnchor {
 *         // 优先在锚点模块（可能是公共模块）中查找
 *         // 这样可以获得最原始的定义
 *         findClassifierAcrossModuleDependencies(symbolId)
 *     }
 * }
 * ```
 *
 * ## 实现细节
 *
 * ### 内联优化
 *
 * 使用 `inline` 和 `crossinline` 关键字：
 *
 * ```kotlin
 * inline fun ModuleDescriptor.withResolutionAnchor(
 *     crossinline doSearch: ModuleDescriptor.() -> ClassifierDescriptor?
 * ): ClassifierDescriptor?
 * ```
 *
 * - `inline`: 函数调用会被内联，避免 lambda 对象创建
 * - `crossinline`: 允许在非内联上下文中使用 lambda
 * - 性能优化：零运行时开销
 *
 * ### 查找逻辑
 *
 * ```kotlin
 * val anchor = getResolutionAnchorIfAny()
 * return if (anchor == null) {
 *     doSearch()  // 没有锚点，直接查找
 * } else {
 *     anchor.doSearch() ?: doSearch()  // 先锚点，后当前
 * }
 * ```
 *
 * ### getResolutionAnchorIfAny
 *
 * 获取解析锚点模块：
 * - 如果模块有关联的公共模块，返回公共模块
 * - 如果是独立模块，返回 null
 * - 配置通常在模块创建时设置
 *
 * ## 解析锚点的典型配置
 *
 * ```kotlin
 * // 创建平台特定模块时设置锚点
 * val jvmModule = ModuleDescriptorImpl(
 *     name = Name.identifier("myProject-jvm"),
 *     storageManager = storageManager,
 *     builtIns = builtIns
 * )
 *
 * val commonModule = ModuleDescriptorImpl(
 *     name = Name.identifier("myProject-common"),
 *     storageManager = storageManager,
 *     builtIns = builtIns
 * )
 *
 * // 设置 jvmModule 的解析锚点为 commonModule
 * jvmModule.setResolutionAnchor(commonModule)
 * ```
 *
 * ## 性能特性
 *
 * - **内联优化**: 零运行时开销，无 lambda 对象分配
 * - **短路求值**: 锚点模块找到结果后立即返回
 * - **缓存友好**: 锚点模块通常是公共模块，缓存命中率高
 *
 * ## 注意事项
 *
 * 1. **循环锚点**: 避免两个模块互为锚点（会导致无限递归）
 * 2. **锚点链**: 不支持多级锚点链（锚点的锚点）
 * 3. **null 语义**: `anchor.doSearch() ?: doSearch()` 只在锚点返回 null 时回退
 *
 * ## 典型使用模式
 *
 * ```kotlin
 * // 模式 1: 在扩展函数中使用
 * fun ModuleDescriptor.findCommonOrPlatformClass(classId: ClassId): ClassDescriptor? {
 *     return withResolutionAnchor {
 *         findClassAcrossModuleDependencies(classId)
 *     }
 * }
 *
 * // 模式 2: 批量查找
 * fun ModuleDescriptor.findMultipleClasses(
 *     classIds: List<ClassId>
 * ): List<ClassDescriptor> {
 *     return classIds.mapNotNull { classId ->
 *         withResolutionAnchor {
 *             findClassAcrossModuleDependencies(classId)
 *         }
 *     }
 * }
 *
 * // 模式 3: 带类型转换
 * inline fun <reified T : ClassifierDescriptor> ModuleDescriptor.findWithAnchor(
 *     classId: ClassId
 * ): T? {
 *     return withResolutionAnchor {
 *         findClassifierAcrossModuleDependencies(classId)
 *     } as? T
 * }
 * ```
 *
 * @param doSearch 查找操作的 lambda，在模块上下文中执行
 * @return 找到的分类器描述符，如果未找到则返回 null
 *
 * @see getResolutionAnchorIfAny 获取解析锚点模块
 * @see findClassifierAcrossModuleDependencies 使用此函数的主要查找方法
 */
inline fun ModuleDescriptor.withResolutionAnchor(
    crossinline doSearch: ModuleDescriptor.() -> ClassifierDescriptor?
): ClassifierDescriptor? {
    val anchor = getResolutionAnchorIfAny()
    return if (anchor == null) doSearch() else anchor.doSearch() ?: doSearch()
}

/**
 * 跨模块依赖查找类型别名 - 在模块及其所有依赖中查找指定的类型别名
 *
 * 这是 [findClassifierAcrossModuleDependencies] 的类型安全变体，仅返回类型别名描述符，
 * 排除类、接口等其他类型的分类器。
 *
 * ## 核心功能
 *
 * - 调用 [findClassifierAcrossModuleDependencies] 查找分类器
 * - 使用类型转换 `as? TypeAliasDescriptor` 过滤结果
 * - 仅返回类型别名，不返回类或接口
 *
 * ## 使用场景
 *
 * ### 1. 反序列化类型别名引用
 *
 * ```kotlin
 * // 源代码: typealias StringMap<V> = HashMap<String, V>
 * // 元数据中存储的类型别名引用
 *
 * val classId = ClassId.fromString("com/example/StringMap")
 * val typeAlias = module.findTypeAliasAcrossModuleDependencies(classId)
 *
 * if (typeAlias != null) {
 *     // 获取展开后的类型
 *     val expandedType = typeAlias.expandedType
 *     // HashMap<String, V>
 *
 *     // 获取类型参数
 *     val typeParameters = typeAlias.declaredTypeParameters
 *     // [V]
 * }
 * ```
 *
 * ### 2. import 语句的类型别名解析
 *
 * ```kotlin
 * // 源代码: import com.example.StringList
 * // StringList 可能是类型别名
 *
 * val classId = ClassId(
 *     packageFqName = FqName("com.example"),
 *     relativeClassName = FqName("StringList")
 * )
 *
 * val typeAlias = module.findTypeAliasAcrossModuleDependencies(classId)
 * if (typeAlias != null) {
 *     // 这是一个类型别名，使用展开后的类型
 *     val actualType = typeAlias.expandedType
 * } else {
 *     // 可能是类或不存在
 *     val classifier = module.findClassifierAcrossModuleDependencies(classId)
 * }
 * ```
 *
 * ### 3. 类型别名展开
 *
 * ```kotlin
 * // 递归展开嵌套的类型别名
 * fun expandTypeAlias(
 *     module: ModuleDescriptor,
 *     classId: ClassId
 * ): CangJieType? {
 *     var currentId = classId
 *     val visited = mutableSetOf<ClassId>()
 *
 *     while (true) {
 *         if (!visited.add(currentId)) {
 *             // 检测到循环类型别名
 *             return ErrorUtils.createErrorType()
 *         }
 *
 *         val typeAlias = module.findTypeAliasAcrossModuleDependencies(currentId)
 *             ?: break  // 不是类型别名，停止展开
 *
 *         val expandedType = typeAlias.expandedType
 *         val expandedClassId = expandedType.constructor.declarationDescriptor?.classId
 *             ?: break  // 无法继续展开
 *
 *         currentId = expandedClassId
 *     }
 *
 *     // 返回最终展开的类型
 *     return module.findClassifierAcrossModuleDependencies(currentId)?.defaultType
 * }
 * ```
 *
 * ## 类型别名 vs 类
 *
 * | 特性 | 类型别名 | 类 |
 * |------|---------|-----|
 * | 定义方式 | `typealias Name = Type` | `class Name { }` |
 * | 是否创建新类型 | 否（仅别名） | 是 |
 * | 可以有构造器 | 否 | 是 |
 * | 可以有成员 | 否 | 是 |
 * | 类型检查 | 等价于展开类型 | 独立类型 |
 * | 使用场景 | 简化复杂类型 | 定义新实体 |
 *
 * ## 注意事项
 *
 * ### TODO: 类型别名变成类/接口的情况
 *
 * 代码中有一个 TODO 注释：
 * ```kotlin
 * // TODO what if typealias becomes a class / interface?
 * ```
 *
 * 这提示了一个潜在问题：
 * - 在增量编译或多模块场景中
 * - 类型别名可能在某个版本中变成类或接口
 * - 当前实现可能无法处理这种情况
 * - 需要考虑版本兼容性和迁移策略
 *
 * ## 返回值
 *
 * - **成功**: 返回找到的 [TypeAliasDescriptor]
 * - **失败**: 返回 `null`
 *   - 分类器不存在
 *   - 找到的是类或接口而非类型别名
 *
 * ## 典型使用模式
 *
 * ```kotlin
 * // 模式 1: 检查是类还是类型别名
 * val classId = ClassId.fromString("com/example/MyType")
 *
 * val typeAlias = module.findTypeAliasAcrossModuleDependencies(classId)
 * if (typeAlias != null) {
 *     println("This is a type alias to: ${typeAlias.expandedType}")
 * } else {
 *     val classDescriptor = module.findClassAcrossModuleDependencies(classId)
 *     if (classDescriptor != null) {
 *         println("This is a class")
 *     }
 * }
 *
 * // 模式 2: 获取最终类型
 * fun getFinalType(module: ModuleDescriptor, classId: ClassId): CangJieType? {
 *     val typeAlias = module.findTypeAliasAcrossModuleDependencies(classId)
 *     return typeAlias?.expandedType
 *         ?: module.findClassAcrossModuleDependencies(classId)?.defaultType
 * }
 *
 * // 模式 3: 类型别名链展开
 * fun fullyExpandTypeAlias(
 *     module: ModuleDescriptor,
 *     initialType: CangJieType
 * ): CangJieType {
 *     var currentType = initialType
 *     var typeAlias = currentType.constructor.declarationDescriptor as? TypeAliasDescriptor
 *
 *     while (typeAlias != null) {
 *         currentType = typeAlias.expandedType
 *         typeAlias = currentType.constructor.declarationDescriptor as? TypeAliasDescriptor
 *     }
 *
 *     return currentType
 * }
 * ```
 *
 * @param classId 要查找的类型别名的唯一标识符
 * @return 找到的类型别名描述符，如果未找到或找到的不是类型别名则返回 null
 *
 * @see findClassifierAcrossModuleDependencies 查找任意分类器
 * @see findClassAcrossModuleDependencies 查找类
 * @see TypeAliasDescriptor 类型别名描述符接口
 */
fun ModuleDescriptor.findTypeAliasAcrossModuleDependencies(classId: ClassId): TypeAliasDescriptor? {
    // TODO what if typealias becomes a class / interface?
    return findClassifierAcrossModuleDependencies(classId) as? TypeAliasDescriptor
}

/**
 * 查找非泛型类或创建虚拟类 - 在模块依赖中查找类，如果找不到则创建一个无类型参数的虚拟类
 *
 * 这是一个容错查找函数，保证总是返回一个类描述符。如果在模块依赖中找不到指定的类，
 * 它会通过 [NotFoundClasses] 创建一个虚拟的错误类描述符，以允许编译继续进行。
 *
 * ## 核心功能
 *
 * ### 双策略处理
 *
 * 1. **优先真实类**: 尝试在模块依赖中查找真实的类
 * 2. **回退虚拟类**: 如果找不到，创建虚拟类描述符
 *
 * ### 虚拟类特性
 *
 * - **无类型参数**: 虚拟类不支持泛型，所有嵌套类也标记为无类型参数
 * - **错误标记**: 虚拟类被标记为错误类，类型检查会识别并报告
 * - **成员作用域**: 虚拟类有基本的错误恢复作用域
 *
 * ## 使用场景
 *
 * ### 1. 反序列化缺失的类引用
 *
 * 从元数据中恢复类引用时，类可能已被删除或移动：
 *
 * ```kotlin
 * // 元数据中引用了一个类，但该类在当前依赖中不存在
 * val classId = ClassId.fromString("com/example/DeletedClass")
 *
 * // 使用 findNonGenericClassAcrossDependencies 确保返回非 null
 * val classDescriptor = module.findNonGenericClassAcrossDependencies(
 *     classId,
 *     notFoundClasses
 * )
 *
 * // classDescriptor 总是非 null，可能是真实类或虚拟类
 * if (classDescriptor.isError) {
 *     // 这是一个虚拟类，类不存在
 *     trace.report(Errors.UNRESOLVED_REFERENCE.on(element, classId))
 * } else {
 *     // 找到真实的类
 *     val type = classDescriptor.defaultType
 * }
 * ```
 *
 * ### 2. 处理损坏的依赖
 *
 * 当依赖库不完整或版本不匹配时：
 *
 * ```kotlin
 * // 项目依赖 library-v1.0.jar
 * // 代码引用了 library-v2.0 中新增的类
 *
 * val classId = ClassId.fromString("com/library/NewClassInV2")
 *
 * // 创建虚拟类以避免编译器崩溃
 * val classDescriptor = module.findNonGenericClassAcrossDependencies(
 *     classId,
 *     notFoundClasses
 * )
 *
 * // 编译继续，但会报告错误
 * ```
 *
 * ### 3. IDE 中的错误恢复
 *
 * 在 IDE 中，用户代码可能引用尚未定义的类：
 *
 * ```kotlin
 * // 用户正在输入：
 * // val obj: NotYetDefinedClass = ...
 *
 * // IDE 需要为 NotYetDefinedClass 创建临时描述符
 * val classId = ClassId.fromString("com/example/NotYetDefinedClass")
 * val placeholder = module.findNonGenericClassAcrossDependencies(
 *     classId,
 *     notFoundClasses
 * )
 *
 * // IDE 功能（补全、导航等）仍然可以工作
 * ```
 *
 * ## 实现细节
 *
 * ### 类型参数计数
 *
 * 计算嵌套层级并为每一层分配零个类型参数：
 *
 * ```kotlin
 * // 例如: com.example.Outer.Middle.Inner
 * // classId = ClassId("com.example", "Outer.Middle.Inner")
 *
 * val typeParametersCount = generateSequence(classId, ClassId::outerClassId)
 *     .map { 0 }
 *     .toList()
 *
 * // 生成序列：
 * // - ClassId("com.example", "Outer.Middle.Inner") -> 0
 * // - ClassId("com.example", "Outer.Middle") -> 0
 * // - ClassId("com.example", "Outer") -> 0
 * // - null (停止)
 *
 * // typeParametersCount = [0, 0, 0]
 * // 表示 Outer、Middle、Inner 都没有类型参数
 * ```
 *
 * ### NotFoundClasses.getClass
 *
 * 创建虚拟类描述符：
 *
 * ```kotlin
 * // NotFoundClasses 内部实现（简化）
 * class NotFoundClasses {
 *     private val cache = mutableMapOf<ClassId, ClassDescriptor>()
 *
 *     fun getClass(
 *         classId: ClassId,
 *         typeParametersCount: List<Int>
 *     ): ClassDescriptor {
 *         return cache.getOrPut(classId) {
 *             createMockClassDescriptor(classId, typeParametersCount)
 *         }
 *     }
 *
 *     private fun createMockClassDescriptor(...): ClassDescriptor {
 *         // 创建标记为错误的类描述符
 *         // 设置错误作用域
 *         // 标记为 final 和非抽象
 *     }
 * }
 * ```
 *
 * ## 虚拟类的限制
 *
 * ### 重要限制
 *
 * 如代码注释所述：
 * ```
 * // NB: the returned class has no type parameters and thus cannot be given arguments in types
 * ```
 *
 * 虚拟类的限制：
 * - **无类型参数**: 不能写 `VirtualClass<Int>`
 * - **无构造器**: 不能实例化
 * - **无真实成员**: 成员作用域是错误作用域
 * - **不能继承**: 不能作为父类
 *
 * ### 使用示例
 *
 * ```kotlin
 * // 假设 List<T> 不存在，创建虚拟类
 * val virtualList = notFoundClasses.getClass(
 *     ClassId.fromString("std/collection/List"),
 *     listOf(0)  // 原本应该有 1 个类型参数，但虚拟类强制为 0
 * )
 *
 * // 错误：虚拟类不支持类型参数
 * // val listOfInt = virtualList.defaultType.replace(
 * //     newArguments = listOf(intType.asTypeProjection())
 * // )
 *
 * // 正确：使用原始类型
 * val rawListType = virtualList.defaultType  // List（无类型参数）
 * ```
 *
 * ## 与其他查找函数的区别
 *
 * | 特性 | findNonGenericClassAcrossDependencies | findClassAcrossModuleDependencies |
 * |------|--------------------------------------|----------------------------------|
 * | 返回值 | 总是非 null | 可能返回 null |
 * | 找不到时 | 创建虚拟类 | 返回 null |
 * | 类型参数 | 虚拟类无类型参数 | 保留真实类型参数 |
 * | 使用场景 | 错误恢复、反序列化 | 正常查找 |
 * | 性能 | 略慢（需要计算层级） | 较快 |
 *
 * ## 性能考虑
 *
 * ### 层级计算开销
 *
 * ```kotlin
 * // 计算嵌套层级
 * generateSequence(classId, ClassId::outerClassId).map { 0 }.toList()
 *
 * // 时间复杂度: O(n)，n 是嵌套深度（通常很小，1-3）
 * // 空间复杂度: O(n)，存储类型参数计数列表
 * ```
 *
 * ### NotFoundClasses 缓存
 *
 * - NotFoundClasses 内部维护缓存
 * - 相同 ClassId 的虚拟类只创建一次
 * - 后续访问直接返回缓存实例
 *
 * ## 注意事项
 *
 * 1. **仅用于错误恢复**: 不应在正常代码路径中依赖虚拟类
 * 2. **报告错误**: 使用虚拟类时应该向用户报告错误
 * 3. **不支持泛型**: 虚拟类无法处理泛型类型
 * 4. **缓存管理**: NotFoundClasses 实例应该与模块生命周期一致
 *
 * ## 典型使用模式
 *
 * ```kotlin
 * // 模式 1: 反序列化容错
 * fun deserializeClassReference(
 *     module: ModuleDescriptor,
 *     classIdString: String,
 *     notFoundClasses: NotFoundClasses
 * ): ClassDescriptor {
 *     val classId = ClassId.fromString(classIdString)
 *
 *     val classDescriptor = module.findNonGenericClassAcrossDependencies(
 *         classId,
 *         notFoundClasses
 *     )
 *
 *     if (classDescriptor.isError) {
 *         logger.warn("Class not found: $classId, using mock")
 *     }
 *
 *     return classDescriptor
 * }
 *
 * // 模式 2: 批量加载带错误恢复
 * fun loadClassReferences(
 *     module: ModuleDescriptor,
 *     classIds: List<ClassId>,
 *     notFoundClasses: NotFoundClasses
 * ): List<ClassDescriptor> {
 *     return classIds.map { classId ->
 *         module.findNonGenericClassAcrossDependencies(classId, notFoundClasses)
 *     }
 * }
 *
 * // 模式 3: 区分真实类和虚拟类
 * fun processClassReference(
 *     module: ModuleDescriptor,
 *     classId: ClassId,
 *     notFoundClasses: NotFoundClasses
 * ) {
 *     val cls = module.findNonGenericClassAcrossDependencies(classId, notFoundClasses)
 *
 *     if (cls.isError) {
 *         // 处理虚拟类
 *         trace.report(Errors.UNRESOLVED_REFERENCE.on(element, classId))
 *         // 使用降级策略
 *         useFallbackStrategy()
 *     } else {
 *         // 处理真实类
 *         processRealClass(cls)
 *     }
 * }
 * ```
 *
 * ## 示例：完整的错误恢复流程
 *
 * ```kotlin
 * // 场景：反序列化依赖库的元数据
 *
 * class MetadataDeserializer(
 *     private val module: ModuleDescriptor,
 *     private val notFoundClasses: NotFoundClasses
 * ) {
 *     fun deserializeType(proto: TypeProto): CangJieType {
 *         val classId = readClassId(proto)
 *
 *         // 1. 尝试查找真实的类
 *         //    如果找不到，创建虚拟类（保证非 null）
 *         val classDescriptor = module.findNonGenericClassAcrossDependencies(
 *             classId,
 *             notFoundClasses
 *         )
 *
 *         // 2. 检查是否是虚拟类
 *         if (classDescriptor.isError) {
 *             // 记录诊断信息
 *             logger.warn("Cannot resolve class: $classId")
 *
 *             // 返回错误类型
 *             return ErrorUtils.createErrorType("Unresolved class: $classId")
 *         }
 *
 *         // 3. 使用真实类创建类型
 *         val typeArguments = deserializeTypeArguments(proto)
 *
 *         // 注意：虚拟类不支持类型参数，所以上面已经检查并返回
 *         return classDescriptor.defaultType.replace(
 *             newArguments = typeArguments
 *         )
 *     }
 * }
 * ```
 *
 * @param classId 要查找的类的唯一标识符
 * @param notFoundClasses 虚拟类工厂，用于创建不存在的类的占位符
 * @return 找到的类描述符或新创建的虚拟类描述符（总是非 null）
 *
 * @see findClassAcrossModuleDependencies 可能返回 null 的查找方法
 * @see NotFoundClasses 虚拟类工厂
 * @see ClassDescriptor.isError 检查是否是虚拟类
 */
fun ModuleDescriptor.findNonGenericClassAcrossDependencies(
    classId: ClassId,
    notFoundClasses: NotFoundClasses
): ClassDescriptor {
    val existingClass = findClassAcrossModuleDependencies(classId)
    if (existingClass != null) return existingClass

    // Take a list of N zeros, where N is the number of class names in the given ClassId
    val typeParametersCount = generateSequence(classId, ClassId::outerClassId).map { 0 }.toList()

    return notFoundClasses.getClass(classId, typeParametersCount)
}
