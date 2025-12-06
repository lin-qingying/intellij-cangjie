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

import org.cangnova.cangjie.descriptors.TypeParameterDescriptor

/**
 * 类型使用场景枚举
 *
 * 标识类型在类型擦除过程中的使用位置，用于决定擦除策略。
 *
 * ## 使用场景
 *
 * - **SUPERTYPE**: 类型作为父类型使用
 *   - 如：`class MyClass : BaseClass<T>`
 *   - 擦除时需要保留类型的上界信息
 *   - 用于继承关系检查和多态调用
 *
 * - **COMMON**: 类型作为普通用途使用
 *   - 如：字段类型、方法参数类型、返回值类型
 *   - 擦除时使用标准策略
 *   - 大多数类型使用场景
 *
 * @see ErasureTypeAttributes 类型擦除属性
 */
enum class TypeUsage {
    /** 类型用作父类型 */
    SUPERTYPE,

    /** 类型用作普通场景 */
    COMMON
}

/**
 * 类型擦除属性
 *
 * 在类型擦除过程中携带的上下文信息，用于控制擦除行为并防止无限递归。
 *
 * ## 什么是类型擦除？
 *
 * 类型擦除（Type Erasure）是将泛型类型转换为非泛型类型的过程：
 * ```
 * 泛型类型:   List<String>
 * 擦除后:     List (或 List<Any>)
 * ```
 *
 * ## 为什么需要类型擦除？
 *
 * 1. **字节码兼容性**: 底层 JVM 不直接支持泛型
 * 2. **类型安全**: 在编译时检查类型，运行时擦除
 * 3. **向后兼容**: 与非泛型代码互操作
 *
 * ## 这个类的作用
 *
 * 在擦除类型时，需要跟踪：
 * - **使用场景** (`howThisTypeIsUsed`): 决定擦除策略
 * - **已访问的类型参数** (`visitedTypeParameters`): 防止循环引用导致的无限递归
 * - **默认类型** (`defaultType`): 擦除的目标类型
 *
 * ## 递归问题示例
 *
 * 考虑以下类型定义：
 * ```kotlin
 * class Node<T : Node<T>> {  // T 的上界引用了自己
 *     val next: T? = null
 * }
 * ```
 *
 * 计算上界时会发生：
 * ```
 * 计算 T 的上界 -> Node<T>
 *   -> 需要计算 T 的上界 -> Node<T>
 *     -> 需要计算 T 的上界 -> ...（无限递归！）
 * ```
 *
 * `visitedTypeParameters` 通过记录已访问的类型参数来打破这个循环。
 *
 * @property howThisTypeIsUsed 类型的使用场景，决定擦除策略
 * @property visitedTypeParameters 已访问的类型参数集合，用于防止递归
 * @property defaultType 默认的擦除目标类型
 *
 * @see TypeUsage 类型使用场景枚举
 * @see TypeParameterDescriptor 类型参数描述符
 */
open class ErasureTypeAttributes(
    /**
     * 类型使用场景
     *
     * 标识这个类型在何种上下文中使用：
     * - `SUPERTYPE`: 作为父类型，需要保留更多类型信息
     * - `COMMON`: 普通使用，使用标准擦除策略
     */
    open val howThisTypeIsUsed: TypeUsage,

    /**
     * 已访问的类型参数集合
     *
     * 记录在计算类型上界时已经访问过的类型参数，用于防止递归循环。
     *
     * ## 工作机制
     *
     * 当遇到类型参数 `T` 时：
     * 1. 检查 `T` 是否在 `visitedTypeParameters` 中
     * 2. 如果在，说明遇到了循环引用，停止递归
     * 3. 如果不在，将 `T` 加入集合，继续计算上界
     *
     * ## 示例
     *
     * ```kotlin
     * // 定义
     * class Comparable<T : Comparable<T>>
     *
     * // 计算过程
     * 计算 T 的上界:
     *   visitedTypeParameters = {}
     *   -> T : Comparable<T>
     *     visitedTypeParameters = {T}
     *     -> T : Comparable<T>  // T 已在集合中，停止
     * ```
     *
     * @see withNewVisitedTypeParameter 添加新的已访问类型参数
     */
    open val visitedTypeParameters: Set<TypeParameterDescriptor>? = null,

    /**
     * 默认擦除类型
     *
     * 指定擦除的目标类型。如果为 null，则使用标准的擦除策略。
     *
     * ## 使用场景
     *
     * - 指定特定的擦除目标（如擦除为 `Any` 或特定的上界类型）
     * - 在不同的擦除阶段传递类型信息
     * - 优化擦除结果
     */
    open val defaultType: SimpleType? = null
) {

    /**
     * 创建带有新默认类型的属性副本
     *
     * 不可变更新模式：创建新实例而不是修改当前实例。
     *
     * ## 使用示例
     *
     * ```kotlin
     * val attrs = ErasureTypeAttributes(TypeUsage.COMMON)
     * val newAttrs = attrs.withDefaultType(anyType)
     * // attrs 保持不变，newAttrs 包含新的 defaultType
     * ```
     *
     * @param type 新的默认类型
     * @return 包含新默认类型的属性实例
     */
    open fun withDefaultType(type: SimpleType?): ErasureTypeAttributes =
        ErasureTypeAttributes(howThisTypeIsUsed, visitedTypeParameters, defaultType = type)

    /**
     * 创建添加了新访问类型参数的属性副本
     *
     * 在递归计算类型上界时，每访问一个新的类型参数就调用此方法，
     * 将其添加到已访问集合中。
     *
     * ## 工作流程
     *
     * 1. 如果 `visitedTypeParameters` 已存在，将新参数添加到集合中
     * 2. 如果 `visitedTypeParameters` 为 null，创建只包含新参数的集合
     * 3. 返回包含更新后集合的新实例
     *
     * ## 使用示例
     *
     * ```kotlin
     * fun computeBound(typeParam: TypeParameterDescriptor, attrs: ErasureTypeAttributes) {
     *     // 检查是否已访问
     *     if (attrs.visitedTypeParameters?.contains(typeParam) == true) {
     *         return // 防止递归
     *     }
     *
     *     // 标记为已访问
     *     val newAttrs = attrs.withNewVisitedTypeParameter(typeParam)
     *
     *     // 继续处理类型参数的上界
     *     for (bound in typeParam.upperBounds) {
     *         processBound(bound, newAttrs)
     *     }
     * }
     * ```
     *
     * @param typeParameter 要添加到已访问集合的类型参数
     * @return 包含更新后已访问集合的属性实例
     */
    open fun withNewVisitedTypeParameter(typeParameter: TypeParameterDescriptor): ErasureTypeAttributes =
        ErasureTypeAttributes(
            howThisTypeIsUsed,
            visitedTypeParameters = visitedTypeParameters?.let { it + typeParameter } ?: setOf(typeParameter),
            defaultType
        )

    /**
     * 相等性判断
     *
     * 两个 ErasureTypeAttributes 相等当且仅当：
     * - `defaultType` 相同
     * - `howThisTypeIsUsed` 相同
     *
     * ## 注意事项
     *
     * **`visitedTypeParameters` 不参与相等性判断**。
     *
     * 这是因为：
     * - 已访问的类型参数是临时的运行状态，不是属性的本质特征
     * - 两个具有相同使用场景和默认类型的属性在语义上是等价的
     * - 包含 `visitedTypeParameters` 会导致不必要的不相等
     *
     * @param other 要比较的对象
     * @return 如果两个对象相等则返回 true
     */
    override fun equals(other: Any?): Boolean {
        if (other !is ErasureTypeAttributes) return false
        return other.defaultType == this.defaultType &&
                other.howThisTypeIsUsed == this.howThisTypeIsUsed
    }

    /**
     * 哈希码计算
     *
     * 基于 `defaultType` 和 `howThisTypeIsUsed` 计算哈希码。
     *
     * ## 实现说明
     *
     * 使用标准的组合哈希算法：
     * ```
     * hash = defaultType.hashCode()
     * hash = 31 * hash + howThisTypeIsUsed.hashCode()
     * ```
     *
     * 与 `equals()` 一致，不包含 `visitedTypeParameters`。
     *
     * @return 对象的哈希码
     */
    override fun hashCode(): Int {
        var result = defaultType.hashCode()
        result = 31 * result + howThisTypeIsUsed.hashCode()
        return result
    }
}