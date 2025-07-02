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

import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.ImmutableMap
import com.google.common.collect.Multimap
import com.intellij.util.keyFMap.KeyFMap


/**
 * 切片映射实现类
 *
 * 实现可变切片映射接口，提供对数据的存取和管理功能。
 *
 * @property alwaysAllowRewrite 是否始终允许重写值
 * @property debugName 用于调试的名称
 */
open class SlicedMapImpl(
    final val alwaysAllowRewrite: Boolean, val debugName: String = ""
) : MutableSlicedMap {
    /**
     * 返回调试名称作为此对象的字符串表示
     *
     * @return 调试名称
     */
    override fun toString(): String {
        return debugName
    }

    /**
     * 存储键和对应的KeyFMap的映射
     * KeyFMap保存了与每个键相关联的所有切片的值
     */
    private var map: MutableMap<Any, KeyFMap>? = null

    /**
     * 存储集合类型切片及其对应的所有键
     */
    private var collectiveSliceKeys: Multimap<WritableSlice<*, *>, Any>? = null
    
    /**
     * 获取映射中的条目数
     *
     * @return 条目数量
     */
    override val size: Int
        get() =  map?.size ?: 0
    /**
     * 获取指定切片和键对应的值
     *
     * @param slice 只读切片
     * @param key 键
     * @return 值，如果不存在则为null
     */
    override fun <K, V> get(slice: ReadOnlySlice<K, V>, key: K): V? {
        key ?: return null
        val holder = map?.get(key) ?: return slice.computeValue(this, key, null, true)
        val value = holder.get(slice.key)
        return slice.computeValue(this, key, value, value == null)
    }

    /**
     * 获取指定切片的所有键
     *
     * @param slice 可写切片
     * @return 键的集合
     */
    override fun <K, V> getKeys(slice: WritableSlice<K, V>): Collection<K> {
        assert(slice.isCollective) { "Keys are not collected for slice " + slice }

        return collectiveSliceKeys?.get(slice) as? Collection<K> ?: emptyList()
    }

    /**
     * 遍历映射中的所有条目
     *
     * @param f 处理每个条目的函数
     */
    override fun forEach(f: (WritableSlice<*, *>, Any, Any) -> Unit) {
        map?.forEach { (key, holder) ->
            holder.keys.forEach { sliceKey ->
                val value = holder[sliceKey] ?: return@forEach
                f.invoke(
                    (sliceKey as AbstractWritableSlice<*, *>).slice,
                    key,
                    value
                )
            }
        }
    }

    /**
     * 将键值对放入指定切片
     *
     * @param slice 可写切片
     * @param key 键
     * @param value 值
     */
    override fun <K, V> put(slice: WritableSlice<K, V>, key: K, value: V) {
        key ?: return
        value ?: return
        if (!slice.check(key, value)) return

        val currentMap = map ?: OpenAddressLinearProbingHashTable<Any, KeyFMap>().also { map = it }
        val holder = currentMap[key] ?: KeyFMap.EMPTY_MAP

        val sliceKey = slice.key
        val rewritePolicy = slice.rewritePolicy

        if (!alwaysAllowRewrite && rewritePolicy.rewriteProcessingNeeded(key)) {
            val oldValue = holder.get(sliceKey) as V?
            if (oldValue != null && !rewritePolicy.processRewrite(slice, key, oldValue, value)) {
                return
            }
        }

        if (slice.isCollective) {
            val currentCollective =
                collectiveSliceKeys ?: ArrayListMultimap.create<WritableSlice<*, *>, Any>().also {
                    collectiveSliceKeys = it
                }
            currentCollective.put(slice, key)
        }

        currentMap[key] = holder.plus(sliceKey, value)
        slice.afterPut(this, key, value)
    }

    /**
     * 清空映射
     * 
     * 删除所有切片中的数据和收集的键
     */
    override fun clear() {
        map = null
        collectiveSliceKeys = null
    }

    /**
     * 获取指定切片的所有内容
     *
     * @param slice 只读切片
     * @return 包含所有键值对的不可变映射
     */
    override fun <K, V> getSliceContents(slice: ReadOnlySlice<K, V>): ImmutableMap<K, V> {
        val builder = ImmutableMap.builder<K, V>()

        map?.forEach { (key, holder) ->

            (holder.get(slice.key) as? V)?.let { value ->
                @Suppress("UNCHECKED_CAST")
                builder.put(key as K, value)
            }
        }
        return builder.build()
    }

    /**
     * 从映射中删除指定键的所有条目
     *
     * @param key 要删除的键
     */
    override fun <K> remove(key: K) {
        key ?: return
        map?.remove(key)
    }

    /**
     * 删除某个切片中特定键的映射
     *
     * 注：此方法在Java原始实现中未完成
     *
     * @param slice 只读切片
     * @param key 键
     */
    fun <K, V> removeBySlice(slice: ReadOnlySlice<K, V>, key: K) {
        // 此功能未实现
    }
}