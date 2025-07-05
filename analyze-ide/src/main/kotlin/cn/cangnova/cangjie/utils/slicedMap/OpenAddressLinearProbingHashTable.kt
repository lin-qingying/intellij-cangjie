/*
 * Copyright 2024 LinQingYing. and contributors.
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

package cn.cangnova.cangjie.utils.slicedMap

import java.util.*
import java.util.function.BiConsumer

/**
 * 黄金分割比例的二进制表示：(sqrt(5) - 1) / 2
 * 用于Knuth的乘法哈希算法
 */
private const val MAGIC: Int = 0x9E3779B9L.toInt() // ((sqrt(5.0) - 1) / 2 * pow(2.0, 32.0)).toLong().toString(16)

/**
 * 最大移位值，用于控制哈希表的初始大小
 */
private const val MAX_SHIFT = 27

/**
 * 负载因子阈值，当哈希表填充率达到50%时触发扩容
 */
private const val THRESHOLD = ((1L shl 31) - 1).toInt() // 50% fill factor for speed

/**
 * 空数组常量，用于初始化
 */
private val EMPTY_ARRAY = arrayOf<Any?>()


/**
 * 计算哈希值的扩展函数
 * 
 * 使用Knuth的乘法哈希与黄金分割比例，使哈希分布更均匀，
 * 这对于使用简单线性探测的哈希表非常重要。
 * 
 * @param shift 移位值
 * @return 计算后的哈希值
 */
@Suppress("NOTHING_TO_INLINE")
private inline fun Any.computeHash(shift: Int) = ((hashCode() * MAGIC) ushr shift) shl 1

/**
 * 开放寻址线性探测哈希表实现
 * 
 * 主要设计思想是提高局部性：
 * - 在同一数组中存储键和值（键在偶数索引，值在奇数索引）
 * - 使用线性探测避免跳转到新的随机索引
 * 
 * 注意：此Map实现不遵循某些Map接口的约定：
 * - `put`不返回先前的值
 * - 不支持`entries`（请改用forEach）
 * - 不支持`remove`
 *
 * @param K 键的类型，必须是非空类型
 * @param V 值的类型，必须是非空类型
 */
internal class OpenAddressLinearProbingHashTable<K : Any, V : Any> : AbstractMutableMap<K, V>() {
    /**
     * 移位值，用于控制哈希表容量
     * 容量 = 1 << (32 - shift)
     */
    private var shift = 0

    /**
     * 存储键值对的数组
     * 键存储在偶数索引位置，值存储在奇数索引位置
     */
    private var array = EMPTY_ARRAY
    
    /**
     * 哈希表中的条目数量
     */
    private var size_ = 0

    init {
        clear()
    }

    /**
     * 获取哈希表中的条目数量
     */
    override val size
        get() = size_

    /**
     * 获取指定键对应的值
     *
     * @param key 键
     * @return 对应的值，如果不存在则返回null
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
     * 将键值对放入哈希表
     * 
     * 注意：此方法不返回先前的值
     *
     * @param key 键
     * @param value 值
     * @return 始终返回null
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
     * 重新哈希表以增加容量
     * 
     * 当哈希表填充率达到阈值时调用此方法
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
     */
    override fun clear() {
        shift = MAX_SHIFT
        array = arrayOfNulls(1 shl (33 - shift))

        size_ = 0
    }

    /**
     * 对哈希表中的每个条目执行给定操作
     *
     * @param action 要执行的操作
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
     * 注意：此实现不支持entries，仅在调试模式下返回不可变集合
     *
     * @return 条目的集合
     * @throws IllegalStateException 在非调试模式下抛出异常
     */
    override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
        get() {
            if (@Suppress("ConstantConditionIf") DEBUG) {
                return Collections.unmodifiableSet(mutableSetOf<MutableMap.MutableEntry<K, V>>().apply {
                    forEach { key, value -> add(Entry(key, value)) }
                })
            }

            throw IllegalStateException("OpenAddressLinearProbingHashTable::entries is not supported and hardly will be")
        }

    /**
     * 不可变条目实现
     *
     * @param K 键的类型
     * @param V 值的类型
     * @property key 键
     * @property value 值
     */
    private class Entry<K, V>(override val key: K, override val value: V) : MutableMap.MutableEntry<K, V> {
        /**
         * 不支持设置新值
         * 
         * @throws UnsupportedOperationException 总是抛出此异常
         */
        override fun setValue(newValue: V): V = throw UnsupportedOperationException("This Entry is not mutable.")
    }

    companion object {
        /**
         * 调试标志，设置为true可在调试器视图中查看映射内容
         */
        private const val DEBUG = false
    }
}

/**
 * 将键值对放入数组
 * 
 * 使用线性探测解决哈希冲突
 *
 * @param array 目标数组
 * @param aShift 移位值
 * @param key 键
 * @param value 值
 * @return 如果是新键则返回true，如果是更新现有键则返回false
 */
private fun put(array: Array<Any?>, aShift: Int, key: Any, value: Any?): Boolean {
    var i = key.computeHash(aShift)

    while (true) {
        val k = array[i]
        if (k == null) {
            array[i] = key
            array[i + 1] = value
            return true
        }
        if (k == key) break
        if (i == 0) {
            i = array.size
        }
        i -= 2
    }

    array[i + 1] = value

    return false
}
