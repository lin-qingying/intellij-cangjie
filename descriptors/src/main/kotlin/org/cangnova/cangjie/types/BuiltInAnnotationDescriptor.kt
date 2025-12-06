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

package org.cangnova.cangjie.types

import org.cangnova.cangjie.builtins.CangJieBuiltIns
import org.cangnova.cangjie.descriptors.SourceElement
import org.cangnova.cangjie.descriptors.annotations.AnnotationDescriptor
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.resolve.constants.ConstantValue

/**
 * 内置注解描述符
 *
 * 表示编译器内置的注解（如 @Deprecated、@Override 等），这些注解由编译器直接提供，
 * 不需要从源代码或库中加载。
 *
 * ## 什么是内置注解？
 *
 * 内置注解是编译器预定义的特殊注解，具有以下特点：
 * - **无源代码**: 不对应实际的源文件，由编译器直接创建
 * - **性能优化**: 避免解析和加载注解类的开销
 * - **核心功能**: 提供编译器必需的元数据（如废弃标记、覆盖检查等）
 *
 * ## 常见的内置注解示例
 *
 * ```kotlin
 * @Deprecated("Use newMethod instead")  // 标记废弃的 API
 * fun oldMethod() { }
 *
 * @Override  // 标记覆盖方法
 * fun toString(): String = "..."
 *
 * @SinceVersion("1.2.0")  // 标记引入版本
 * fun newFeature() { }
 * ```
 *
 * ## 与普通注解的区别
 *
 * | 特性 | 内置注解 | 普通注解 |
 * |------|---------|---------|
 * | 来源 | 编译器内置 | 源代码/库 |
 * | 源元素 | NO_SOURCE | 有具体位置 |
 * | 加载方式 | 直接创建 | 解析加载 |
 * | 性能 | 高效 | 相对慢 |
 *
 * ## 使用场景
 *
 * 1. **编译器特殊处理的注解**: 需要编译器识别并执行特定逻辑
 * 2. **标准库注解**: 语言标准定义的注解
 * 3. **性能敏感场景**: 频繁使用的注解（如 @Override）
 *
 * @property builtIns 内置类型系统，用于查找注解类
 * @property fqName 注解的完全限定名（如 "kotlin.Deprecated"）
 * @property allValueArguments 注解的所有参数值映射
 * @property forcePropagationDeprecationToOverrides 是否强制将废弃信息传播到覆盖方法
 *
 * @see AnnotationDescriptor 注解描述符接口
 * @see CangJieBuiltIns 内置类型系统
 */
class BuiltInAnnotationDescriptor(
    /**
     * 内置类型系统
     *
     * 提供对编译器内置类型的访问，用于查找注解类的类型信息。
     *
     * ## 作用
     *
     * 通过 `builtIns.getBuiltInClassByFqName()` 可以获取注解类的描述符，
     * 进而得到注解的类型信息。
     */
    private val builtIns: CangJieBuiltIns,

    /**
     * 注解的完全限定名
     *
     * 唯一标识一个注解类型的全路径名称。
     *
     * ## 示例
     *
     * - `a.Deprecated` - 废弃注解
     *
     * ## 用途
     *
     * - 在类型系统中查找对应的注解类
     * - 在代码生成时识别特定注解
     * - 在诊断消息中显示注解名称
     */
    override val fqName: FqName,

    /**
     * 注解的所有参数值
     *
     * 存储注解的所有命名参数及其对应的常量值。
     *
     * ## 结构
     *
     * Map 的键是参数名（Name），值是对应的常量值（ConstantValue）。
     *
     * ## 示例
     *
     * 对于注解使用：
     * ```kotlin
     * @Deprecated(message = "Use newApi instead", level = DeprecationLevel.ERROR)
     * ```
     *
     * 对应的 map 为：
     * ```kotlin
     * {
     *   "message" -> ConstantValue("Use newApi instead"),
     *   "level" -> ConstantValue(DeprecationLevel.ERROR)
     * }
     * ```
     *
     * ## 空 Map 的情况
     *
     * 如果注解没有参数，则为空 map：
     * ```kotlin
     * @Override  // allValueArguments = emptyMap()
     * ```
     */
    override val allValueArguments: Map<Name, ConstantValue<*>>,

    /**
     * 是否强制传播废弃信息到覆盖方法
     *
     * 控制当一个方法被标记为 @Deprecated 时，其所有覆盖方法是否也应该
     * 被视为废弃。
     *
     * ## 使用场景
     *
     * ### 1. 强制传播（true）
     *
     * ```kotlin
     * interface Api {
     *     @Deprecated("Old API")
     *     fun oldMethod()  // 标记为废弃
     * }
     *
     * class Impl : Api {
     *     override fun oldMethod() { }  // 也被视为废弃
     * }
     * ```
     *
     * 调用 `Impl().oldMethod()` 会触发废弃警告。
     *
     * ### 2. 不强制传播（false，默认）
     *
     * ```kotlin
     * interface Api {
     *     @Deprecated("Old API")
     *     fun oldMethod()
     * }
     *
     * class Impl : Api {
     *     override fun oldMethod() { }  // 不自动视为废弃
     * }
     * ```
     *
     * 调用 `Impl().oldMethod()` 不会触发废弃警告（除非显式标记）。
     *
     * ## 设计考虑
     *
     * - **API 演进**: 允许实现类提供新的非废弃版本
     * - **向后兼容**: 保持现有代码的行为
     * - **灵活性**: 让开发者决定传播策略
     *
     * 默认值为 `false`，因为大多数情况下不需要强制传播。
     */
    val forcePropagationDeprecationToOverrides: Boolean = false
) : AnnotationDescriptor {

    /**
     * 注解的类型
     *
     * 表示这个注解实例的类型，即对应注解类的默认类型。
     *
     * ## 延迟初始化
     *
     * 使用 `lazy` 延迟计算，原因：
     * 1. **避免循环依赖**: 在构造时可能还未完全初始化类型系统
     * 2. **性能优化**: 只在需要时才查找和创建类型
     * 3. **线程安全**: `LazyThreadSafetyMode.PUBLICATION` 确保多线程安全
     *
     * ## 计算过程
     *
     * ```
     * 1. 通过 fqName 在 builtIns 中查找注解类描述符
     * 2. 获取该类的默认类型（通常是无参数的类型）
     * 3. 缓存结果，后续访问直接返回
     * ```
     *
     * ## 线程安全模式说明
     *
     * `LazyThreadSafetyMode.PUBLICATION`:
     * - 允许多个线程同时计算初始值
     * - 第一个完成的结果会被发布并使用
     * - 其他计算结果被丢弃
     * - 适合计算成本不高且结果相同的场景
     *
     * ## 示例
     *
     * ```kotlin
     * val deprecatedAnnotation = BuiltInAnnotationDescriptor(
     *     builtIns = builtIns,
     *     fqName = FqName("kotlin.Deprecated"),
     *     allValueArguments = mapOf(...)
     * )
     *
     * // 第一次访问时计算
     * val type = deprecatedAnnotation.type  // 查找并缓存 Deprecated 类型
     * // 后续访问直接返回缓存的类型
     * val sameType = deprecatedAnnotation.type  // 直接返回
     * ```
     */
    override val type: CangJieType by lazy(LazyThreadSafetyMode.PUBLICATION) {
        builtIns.getBuiltInClassByFqName(fqName).defaultType
    }

    /**
     * 源代码位置
     *
     * 内置注解没有对应的源代码位置，因为它们是由编译器直接创建的。
     *
     * ## 返回 NO_SOURCE 的原因
     *
     * 1. **无实际源文件**: 内置注解不是从源代码解析得到的
     * 2. **避免空指针**: 提供一个明确的"无源"标记
     * 3. **统一接口**: 满足 AnnotationDescriptor 接口要求
     *
     * ## 与普通注解的对比
     *
     * ```kotlin
     * // 内置注解
     * val builtIn = BuiltInAnnotationDescriptor(...)
     * builtIn.source  // NO_SOURCE
     *
     * // 普通注解（从源码解析）
     * val normal = parsedAnnotation
     * normal.source  // 指向实际的源文件位置（文件名、行号等）
     * ```
     *
     * ## 影响
     *
     * - **错误报告**: 涉及内置注解的错误不会显示源位置
     * - **IDE 导航**: 无法跳转到注解定义（因为没有源文件）
     * - **调试**: 无法在注解定义处设置断点
     *
     * @return 总是返回 `SourceElement.NO_SOURCE`
     */
    override val source: SourceElement
        get() = SourceElement.NO_SOURCE
}