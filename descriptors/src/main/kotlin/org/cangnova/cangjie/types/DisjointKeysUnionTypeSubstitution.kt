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

package org.cangnova.cangjie.types

import org.cangnova.cangjie.descriptors.annotations.Annotations

// ===================================================================
// 此文件已废弃 - TypeSubstitution 接口已被删除，使用 ComposableTypeSubstitutor 替代
// This file is deprecated - TypeSubstitution interface has been removed, use ComposableTypeSubstitutor instead
// ===================================================================

/*
/** 不相交键并集类型替换
 *
 * 这个类实现了两个类型替换的组合，要求两个替换的键集合不相交（disjoint）。
 * 它通过联合（union）两个类型替换来创建一个新的替换，查找时会依次在两个替换中查找。
 *
 * ## 设计目的
 *
 * 在类型系统中，经常需要组合多个类型替换：
 * - 一个替换来自外部作用域
 * - 另一个替换来自内部作用域
 * - 两者的类型参数不重叠（不相交）
 *
 * ## 工作原理
 *
 * ```
 * 假设有两个替换：
 * first:  {T -> String, U -> Int}
 * second: {V -> Bool, W -> Float}
 *
 * 组合后：{T -> String, U -> Int, V -> Bool, W -> Float}
 *
 * 查找顺序：先查 first，如果没找到再查 second
 * ```
 *
 * ## 使用场景
 *
 * 1. **嵌套泛型类型**
 *    ```
 *    class Outer<T> {
 *        class Inner<U> {
 *            // 需要同时替换 T 和 U
 *        }
 *    }
 *    ```
 *
 * 2. **类型推断**
 *    - 组合多个推断阶段的结果
 *    - 合并不同约束来源的替换
 *
 * 3. **类型实例化**
 *    - 逐步应用多个类型参数的替换
 *
 * ## 不变性要求
 *
 * - **键不相交**：first 和 second 不应有相同的类型参数
 * - 如果有重叠，first 的替换会优先（但这违反了设计意图）
 *
 * @property first 第一个类型替换（优先级更高）
 * @property second 第二个类型替换（作为后备）
 *
 * @see TypeSubstitution 类型替换的基类
 */
@Deprecated("")
class DisjointKeysUnionTypeSubstitution private constructor(
    private val first: TypeSubstitution,
    private val second: TypeSubstitution
) : TypeSubstitution() {

    companion object {
        /**
         * 创建不相交键并集类型替换
         *
         * 这是创建实例的唯一入口，包含优化逻辑：
         * - 如果任一替换为空，直接返回另一个（避免不必要的包装）
         * - 只有两者都非空时才创建组合替换
         *
         * ## 优化原理
         *
         * 避免创建不必要的包装对象，减少内存开销和方法调用层次：
         * ```
         * create(empty, subst) => subst  // 而不是 Union(empty, subst)
         * create(subst, empty) => subst  // 而不是 Union(subst, empty)
         * ```
         *
         * @param first 第一个类型替换
         * @param second 第二个类型替换
         * @return 组合后的类型替换，可能是原始替换之一或新的并集替换
         */
        @JvmStatic
        fun create(first: TypeSubstitution, second: TypeSubstitution): TypeSubstitution {
            // 优化：如果第一个为空，直接返回第二个
            if (first.isEmpty()) return second

            // 优化：如果第二个为空，直接返回第一个
            if (second.isEmpty()) return first

            // 两者都非空时创建组合替换
            return DisjointKeysUnionTypeSubstitution(first, second)
        }
    }

    /**
     * 获取类型参数的替换结果
     *
     * 实现查找策略：
     * 1. 首先在 first 中查找
     * 2. 如果找到，返回结果
     * 3. 如果没找到，在 second 中查找
     * 4. 返回 second 的结果（可能为 null）
     *
     * ## 查找顺序的重要性
     *
     * first 优先级高于 second，这在以下情况很重要：
     * - 内部作用域的替换应该覆盖外部作用域
     * - 更具体的类型信息应该优先使用
     *
     * @param key 要查找的类型参数
     * @return 替换后的类型投影，如果两个替换中都没有则返回 null
     */
    override fun get(key: CangJieType): TypeArgument? = first[key] ?: second[key]

    /**
     * 准备顶层类型
     *
     * 依次应用两个替换的顶层类型准备逻辑：
     * 1. 先用 first 准备类型
     * 2. 将结果再用 second 准备
     *
     * ## 应用场景
     *
     * 顶层类型准备通常用于：
     * - 处理类型投影（in/out）
     * - 处理型变（variance）
     * - 标准化类型表示
     *
     * ## 顺序说明
     *
     * 与 get() 不同，这里是顺序应用而不是优先级选择：
     * ```
     * result = second.prepare(first.prepare(type))
     * ```
     *
     * @param topLevelType 要准备的顶层类型
     * @param position 类型参数的型变位置
     * @return 准备后的类型
     */
    override fun prepareTopLevelType(topLevelType: CangJieType ): CangJieType =
        second.prepareTopLevelType(first.prepareTopLevelType(topLevelType, ),  )

    /**
     * 判断替换是否为空
     *
     * 由于这个类只在两个替换都非空时创建（参见 create 方法），
     * 因此总是返回 false。
     *
     * ## 设计保证
     *
     * create() 方法确保了：
     * - 如果任一替换为空，不会创建 DisjointKeysUnionTypeSubstitution
     * - 因此这个方法永远返回 false
     *
     * @return 总是返回 false
     */
    override fun isEmpty(): Boolean = false

    /**
     * 是否近似捕获类型
     *
     * 捕获类型（captured types）是类型系统中的高级特性，
     * 用于处理通配符类型的捕获转换。
     *
     * ## 逻辑说明
     *
     * 只要任一替换需要近似捕获类型，组合替换就需要近似：
     * ```
     * approximateCapturedTypes = first.approximate OR second.approximate
     * ```
     *
     * ## 使用场景
     *
     * - 处理 Java 互操作中的通配符类型
     * - 类型推断中的捕获转换
     * - 型变类型的安全处理
     *
     * @return 如果任一替换需要近似捕获类型则返回 true
     */
    override fun approximateCapturedTypes(): Boolean =
        first.approximateCapturedTypes() || second.approximateCapturedTypes()

    /**
     * 是否近似逆变捕获类型
     *
     * 逆变（contravariant）捕获类型需要特殊处理，因为它们的子类型关系是反向的。
     *
     * ## 逆变的特殊性
     *
     * 在逆变位置（如函数参数）：
     * - 子类型关系反转
     * - 需要更保守的近似策略
     *
     * @return 如果任一替换需要近似逆变捕获类型则返回 true
     */
    override fun approximateContravariantCapturedTypes(): Boolean =
        first.approximateContravariantCapturedTypes() || second.approximateContravariantCapturedTypes()

    /**
     * 过滤注解
     *
     * 依次应用两个替换的注解过滤逻辑：
     * 1. 先用 first 过滤注解
     * 2. 将结果再用 second 过滤
     *
     * ## 应用场景
     *
     * 类型替换时可能需要：
     * - 移除某些不适用的注解
     * - 转换注解的目标类型
     * - 添加额外的类型注解
     *
     * ## 顺序的重要性
     *
     * 与 prepareTopLevelType 类似，这是顺序应用：
     * ```
     * result = second.filter(first.filter(annotations))
     * ```
     *
     * @param annotations 要过滤的注解集合
     * @return 过滤后的注解集合
     */
    override fun filterAnnotations(annotations: Annotations): Annotations =
        second.filterAnnotations(first.filterAnnotations(annotations))
}
*/