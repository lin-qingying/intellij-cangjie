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

package org.cangnova.cangjie.resolve.binding.slicedMap

/**
 * Set Slice 实现 - 用于在 SlicedMap 中实现集合（Set）语义
 *
 * ## 核心概念
 *
 * SetSlice 是一个特殊的 WritableSlice，它将 Set 语义映射到 SlicedMap 的键值对存储中：
 * - **键存在于集合** → 值为 `true`
 * - **键不存在于集合** → 值为 `false`（默认值）
 *
 * 这种设计允许使用统一的 SlicedMap 接口来存储集合数据，而无需单独的 Set 数据结构。
 *
 * ## 工作原理
 *
 * ### 1. 默认值语义
 * - DEFAULT = false，表示"不在集合中"
 * - 只有值为 true 的键才会被实际存储
 * - 查询不存在的键返回 false（而非 null）
 *
 * ### 2. 存储优化
 * - check() 方法过滤掉 DEFAULT 值，避免存储"不在集合中"的标记
 * - 只有明确添加到集合的键才会占用存储空间
 *
 * ### 3. 查询行为
 * - computeValue() 确保未找到的键返回 false（而非 null）
 * - 提供与标准 Set 一致的查询语义
 *
 * ## 使用场景
 *
 * ```kotlin
 * // 创建一个追踪"已处理元素"的集合
 * val PROCESSED_ELEMENTS = SetSlice<PsiElement>(RewritePolicy.DO_NOTHING)
 *
 * // 标记元素已处理
 * bindingTrace.put(PROCESSED_ELEMENTS, element, true)
 *
 * // 检查元素是否已处理
 * val isProcessed = bindingTrace.get(PROCESSED_ELEMENTS, element) // 返回 Boolean，永不为 null
 *
 * // 如果是 collective slice，可以获取所有已处理元素
 * val allProcessed = bindingTrace.getKeys(PROCESSED_ELEMENTS)
 * ```
 *
 * ## 与普通 WritableSlice 的区别
 *
 * | 特性 | SetSlice | 普通 WritableSlice |
 * |------|----------|-------------------|
 * | 值类型 | 固定为 Boolean | 泛型 V |
 * | 默认值 | false | null |
 * | 语义 | 集合成员关系 | 键值映射 |
 * | 存储优化 | 只存储 true | 存储所有非 null 值 |
 *
 * @param K 键类型
 * @param rewritePolicy 重写策略
 * @param isCollective 是否为集合式 slice（允许查询所有键）
 */
open class SetSlice<K : Any> @JvmOverloads constructor(
    rewritePolicy: RewritePolicy,
    isCollective: Boolean = false
) : BasicWritableSlice<K, Boolean>(rewritePolicy, isCollective) {

    companion object {
        /**
         * 默认值：false
         *
         * 表示键不在集合中。这个常量用于：
         * - 过滤不需要存储的值（check 方法）
         * - 为未找到的键提供默认返回值（computeValue 方法）
         */

        const val DEFAULT = false
    }

    /**
     * 检查值是否应该被存储
     *
     * 此方法实现存储优化：只存储值为 true 的条目（表示"在集合中"）。
     * 值为 false 的条目会被过滤掉，因为 false 是默认值，无需显式存储。
     *
     * 实现逻辑：
     * 1. 断言值非 null（Set 语义要求明确的 true/false）
     * 2. 只有值不等于 DEFAULT（即 true）时才返回 true，允许存储
     * 3. 值为 false 时返回 false，跳过存储
     *
     * @param key 键
     * @param value 值（true 表示添加到集合，false 表示移除）
     * @return true 表示应该存储（即 value 为 true），false 表示跳过存储
     */
    override fun check(key: K, value: Boolean): Boolean {

        return value != DEFAULT
    }

    /**
     * 计算查询结果值
     *
     * 此方法确保 Set 语义：查询任何键都返回非 null 的 Boolean 值。
     *
     * 实现逻辑：
     * 1. 调用父类 computeValue 获取存储的值
     * 2. 如果返回 null（键不存在），返回 DEFAULT (false)
     * 3. 否则返回实际存储的值（通常是 true）
     *
     * 这保证了：
     * - 集合中的键返回 true
     * - 不在集合中的键返回 false（而非 null）
     *
     * @param map SlicedMap 实例
     * @param key 要查询的键
     * @param value 存储的原始值
     * @param valueNotFound 是否未找到值
     * @return true 表示键在集合中，false 表示不在集合中
     */
    override fun computeValue(map: SlicedMap, key: K, value: Boolean?, valueNotFound: Boolean): Boolean {
        val result = super.computeValue(map, key, value, valueNotFound)
        return result ?: DEFAULT
    }

    /**
     * 创建原始值版本的 slice
     *
     * 原始值版本直接返回存储的值，不经过额外的计算逻辑。
     * 对于 SetSlice，原始值版本的行为与标准版本类似，但更直接。
     *
     * 返回的 slice 行为：
     * - 如果值未找到（valueNotFound = true），返回 DEFAULT (false)
     * - 否则返回存储的原始值
     *
     * @return 原始值版本的 ReadOnlySlice
     */
    override fun makeRawValueVersion(): ReadOnlySlice<K, Boolean> {
        return object : DelegatingSlice<K, Boolean>(this) {
            override fun computeValue(map: SlicedMap, key: K, value: Boolean?, valueNotFound: Boolean): Boolean {
                if (valueNotFound) return DEFAULT
                return value == true
            }
        }
    }
}
