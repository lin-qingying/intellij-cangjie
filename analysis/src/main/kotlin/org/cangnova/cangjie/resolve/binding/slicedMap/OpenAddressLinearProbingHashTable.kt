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

import java.util.*
import java.util.function.BiConsumer

/**
 * 黄金比例常数的二进制表示：phi 的小数部分 = (sqrt(5) - 1) / 2
 * 用于 Knuth 的乘法哈希算法，提供良好的哈希分布特性
 */
private const val MAGIC: Int = 0x9E3779B9L.toInt() // ((sqrt(5.0) - 1) / 2 * pow(2.0, 32.0)).toLong().toString(16)

/**
 * 最大位移量，决定初始容量
 * 初始容量 = 2^(33 - MAX_SHIFT) = 2^6 = 64 个槽位（32 个键值对）
 */
private const val MAX_SHIFT = 27

/**
 * 负载因子阈值：50%
 * 当元素数量超过容量的 50% 时触发扩容，以保持性能
 */
private const val THRESHOLD = ((1L shl 31) - 1).toInt()

/**
 * 空数组常量，用于初始化
 */
private val EMPTY_ARRAY = arrayOf<Any?>()

/**
 * 计算键的哈希值
 *
 * 使用 Knuth 的乘法哈希算法配合黄金比例常数，确保哈希值分布均匀。
 * 这对于线性探测哈希表尤为重要，因为它使用简单的线性探测策略。
 *
 * 算法步骤：
 * 1. 将键的 hashCode 乘以黄金比例常数 (MAGIC)
 * 2. 右移 shift 位，提取高位比特（高位比特分布更均匀）
 * 3. 左移 1 位，确保结果是偶数（因为键存储在偶数索引）
 *
 * @param shift 位移量，决定哈希表容量
 * @return 计算得到的哈希值（偶数）
 */
@Suppress("NOTHING_TO_INLINE")
private inline fun Any.computeHash(shift: Int) = ((hashCode() * MAGIC) ushr shift) shl 1

/**
 * 开放地址线性探测哈希表实现
 *
 * ## 核心设计思想
 *
 * ### 1. 提升缓存局部性的优化策略
 * - **键值紧凑存储**：键和值存储在同一数组中，键在偶数索引，值在奇数索引
 *   - `array[0]` = key1, `array[1]` = value1
 *   - `array[2]` = key2, `array[3]` = value2
 *   - 这种布局提高了 CPU 缓存命中率
 *
 * - **线性探测**：冲突时顺序向前查找空槽，避免随机跳转
 *   - 探测序列：`i, i-2, i-4, ..., n, n-2, ..., 0`
 *   - 利用了 CPU 缓存预取机制
 *
 * ### 2. 哈希函数
 * - 使用 Knuth 乘法哈希配合黄金比例常数 (0x9E3779B9)
 * - 保证哈希值分布均匀，减少聚集现象
 *
 * ### 3. 动态扩容
 * - 负载因子：50%（较低的负载因子保证查找性能）
 * - 扩容策略：容量翻 8 倍（shift 减少 3）
 * - 每次扩容触发 rehash，重新分布所有元素
 *
 * ## 特殊约定（非标准 Map 实现）
 *
 * 为了性能优化，此实现不遵循某些 Map 接口约定：
 * - `put()` 不返回旧值（总是返回 null）
 * - 不支持 `entries` 属性（请使用 `forEach` 代替）
 * - 不支持 `remove()` 操作
 *
 * ## 性能特征
 * - **查找**：O(1) 平均，O(n) 最坏
 * - **插入**：O(1) 平均，O(n) 最坏（包括可能的 rehash）
 * - **空间开销**：~2x（因为维持 50% 负载因子）
 *
 * ## 类型约束
 * - **键（K）**: 必须非空（`K : Any`），确保 hashCode() 总是可用
 * - **值（V）**: 支持可空（`V`），允许存储 null 值
 *
 * @param K 键类型（必须非空）
 * @param V 值类型（支持可空）
 */
internal class OpenAddressLinearProbingHashTable<K : Any, V> : AbstractMutableMap<K, V>() {
    /**
     * 位移量，用于计算容量
     * 容量 = 2^(33 - shift)
     * shift 越小，容量越大
     */
    private var shift = 0

    /**
     * 存储键值对的数组
     * - 偶数索引 (0, 2, 4, ...) 存储键
     * - 奇数索引 (1, 3, 5, ...) 存储值
     * - null 表示空槽位
     */
    private var array = EMPTY_ARRAY

    /**
     * 当前存储的键值对数量
     */
    private var size_ = 0

    init {
        clear()
    }

    /**
     * 获取当前存储的键值对数量
     */
    override val size
        get() = size_

    /**
     * 根据键查找对应的值
     *
     * 查找算法（线性探测）：
     * 1. 计算键的哈希值，得到起始索引 i
     * 2. 检查 array[i]：
     *    - 如果是 null，说明键不存在，返回 null
     *    - 如果等于查找的键，返回 array[i+1]（对应的值）
     *    - 否则，继续向前探测（i -= 2）
     * 3. 如果到达数组开头（i == 0），循环到数组末尾继续探测
     * 4. 重复步骤 2，直到找到键或遇到 null
     *
     * @param key 要查找的键
     * @return 对应的值，如果不存在则返回 null
     */
    override fun get(key: K): V? {
        var i = key.computeHash(shift)
        var k = array[i]

        while (true) {
            if (k === null) return null
            @Suppress("UNCHECKED_CAST")
            if (k == key) return array[i + 1] as V
            if (i == 0) {
                i = array.size
            }
            i -= 2
            k = array[i]
        }
    }

    /**
     * 插入或更新键值对
     *
     * 注意：为了性能优化，此方法总是返回 null，不返回旧值
     *
     * 插入流程：
     * 1. 调用内部 put 函数尝试插入
     * 2. 如果是新插入（返回 true），增加 size 计数
     * 3. 检查负载因子是否超过阈值（50%）
     * 4. 如果超过，触发 rehash 扩容
     *
     * @param key 键
     * @param value 值
     * @return 总是返回 null（不返回旧值）
     */
    override fun put(key: K, value: V): V? {
        if (put(array, shift, key, value)) {
            if (++size_ >= (THRESHOLD ushr shift)) {
                rehash()
            }
        }

        return null
    }

    /**
     * 扩容并重新哈希所有元素
     *
     * 扩容策略：
     * 1. 计算新的 shift 值：shift - 3（容量翻 8 倍）
     * 2. 创建新数组，大小为 2^(33 - newShift)
     * 3. 遍历旧数组，将所有非空键值对重新插入新数组
     * 4. 更新 shift 和 array 字段
     *
     * 时间复杂度：O(n)，其中 n 是当前存储的元素数量
     */
    private fun rehash() {
        val newShift = maxOf(shift - 3, 0)
        val newArraySize = 1 shl (33 - newShift)
        val newArray = arrayOfNulls<Any>(newArraySize)

        var i = 0
        val arraySize = array.size
        while (i < arraySize) {
            val key = array[i]
            if (key != null) {
                put(newArray, newShift, key, array[i + 1])
            }
            i += 2
        }

        shift = newShift
        array = newArray
    }

    /**
     * 清空哈希表
     *
     * 重置为初始状态：
     * - shift 设为 MAX_SHIFT (27)
     * - 创建大小为 2^6 = 64 的新数组（可存储 32 个键值对）
     * - size 重置为 0
     */
    override fun clear() {
        shift = MAX_SHIFT
        array = arrayOfNulls(1 shl (33 - shift))

        size_ = 0
    }

    /**
     * 遍历所有键值对
     *
     * 遍历策略：
     * - 按数组顺序遍历（索引 0, 2, 4, ...）
     * - 跳过 null 槽位
     * - 对每个非空键值对调用 action
     *
     * @param action 对每个键值对执行的操作
     */
    override fun forEach(action: BiConsumer<in K, in V>) {
        var i = 0
        val arraySize = array.size
        while (i < arraySize) {
            val key = array[i]
            if (key != null) {
                @Suppress("UNCHECKED_CAST")
                action.accept(key as K, array[i + 1] as V)
            }
            i += 2
        }
    }

    /**
     * 获取所有条目的集合
     *
     * 注意：此操作不被支持（为了性能优化）
     *
     * - 在 DEBUG 模式下，返回一个只读的条目集合（性能较差）
     * - 在生产模式下，抛出 IllegalStateException
     *
     * 建议使用 forEach 方法代替
     *
     * @throws IllegalStateException 在非 DEBUG 模式下总是抛出
     */
    override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
        get() {
            if (@Suppress("ConstantConditionIf") DEBUG) {
                return Collections.unmodifiableSet(mutableSetOf<MutableMap.MutableEntry<K, V>>().apply {
                    forEach { (key, value) -> add(Entry(key, value)) }
                })
            }

            throw IllegalStateException("OpenAddressLinearProbingHashTable::entries is not supported and hardly will be")
        }

    /**
     * Map.Entry 的简单实现
     *
     * 注意：此实现是不可变的，调用 setValue 会抛出异常
     */
    private class Entry<K, V>(override val key: K, override val value: V) : MutableMap.MutableEntry<K, V> {
        /**
         * 不支持修改值
         *
         * @throws UnsupportedOperationException 总是抛出
         */
        override fun setValue(newValue: V): V = throw UnsupportedOperationException("This Entry is not mutable.")
    }

    companion object {
        /**
         * DEBUG 标志
         *
         * - 设为 true 时，可以在调试器中查看 Map 的内容（通过 entries 属性）
         * - 生产环境应设为 false，以保持性能
         */
        private const val DEBUG = false
    }
}

/**
 * 内部 put 函数，用于将键值对插入数组
 *
 * 此函数使用线性探测算法查找插入位置：
 * 1. 计算键的哈希值，得到起始索引 i
 * 2. 检查 array[i]：
 *    - 如果是 null，找到空槽，插入键值对，返回 true（表示新插入）
 *    - 如果等于键，找到已存在的键，更新值，返回 false（表示更新）
 *    - 否则，继续向前探测（i -= 2）
 * 3. 如果到达数组开头（i == 0），循环到数组末尾继续探测
 * 4. 重复步骤 2，直到找到空槽或已存在的键
 *
 * @param array 存储数组
 * @param aShift 位移量
 * @param key 键
 * @param value 值
 * @return true 表示新插入，false 表示更新已有键的值
 */
private fun put(array: Array<Any?>, aShift: Int, key: Any, value: Any?): Boolean {
    var i = key.computeHash(aShift)

    while (true) {
        val k = array[i]
        if (k == null) {
            // 找到空槽，插入新键值对
            array[i] = key
            array[i + 1] = value
            return true
        }
        if (k == key) break // 找到已存在的键，跳出循环更新值
        if (i == 0) {
            i = array.size
        }
        i -= 2
    }

    // 更新已存在键的值
    array[i + 1] = value

    return false
}
