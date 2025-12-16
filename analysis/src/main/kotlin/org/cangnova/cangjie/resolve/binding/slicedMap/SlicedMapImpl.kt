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

import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.ImmutableMap
import com.google.common.collect.Multimap
import com.intellij.openapi.util.Key
import com.intellij.util.keyFMap.KeyFMap

/**
 * SlicedMap 的标准实现，提供可变的多维键值对存储
 *
 * ## 核心设计
 *
 * ### 存储结构
 * - **主存储**: `Map<Any, KeyFMap>` - 每个键关联一个 KeyFMap，存储多个 slice 的值
 *   - 键类型为 `Any`（非空），确保 hashCode 可用且语义清晰
 *   - KeyFMap 使用 IntelliJ Platform 的高效不可变 Map 实现
 *   - 值支持可空类型，通过 KeyFMap 存储
 * - **集合索引**: `Multimap<Slice, Any>` - 为集合式 slice 维护键列表
 *
 * ### 延迟初始化策略
 * - 使用 backing property 模式（`_map` 和 `map`）
 * - 首次访问时自动创建 OpenAddressLinearProbingHashTable
 * - 避免空 SlicedMap 占用内存
 * - clear() 后重新变为未初始化状态
 *
 * ### 内存优化
 * - **空 SlicedMap**：仅占用对象头和两个 null 引用的空间（约 24 字节）
 * - **使用 OpenAddressLinearProbingHashTable**：
 *   - 紧凑的键值存储布局
 *   - 提高 CPU 缓存命中率
 *   - 线性探测减少随机内存访问
 * - **延迟初始化集合索引**：仅在使用集合式 slice 时创建
 *
 * ### 类型系统设计
 * - `Map<Any, KeyFMap>`：主存储的键必须非空
 * - `OpenAddressLinearProbingHashTable<Any, KeyFMap>`：内部使用非空键优化性能
 * - KeyFMap 内部的值支持可空类型
 * - 设计原则：键用于索引（不应为 null），值用于存储（可以为 null）
 *
 * ## 性能特征
 * - **查找**: O(1) 平均，受益于 OpenAddressLinearProbingHashTable 的缓存友好性
 * - **插入**: O(1) 平均，包含重写策略检查
 * - **遍历**: O(n)，其中 n 是存储的键值对总数
 * - **内存**: 约 2x 键值对数量（50% 负载因子）
 *
 * @property alwaysAllowRewrite 是否总是允许重写已存在的值
 * @property debugName 调试名称，用于诊断和日志记录
 */
open class SlicedMapImpl(
    private val alwaysAllowRewrite: Boolean,
    var debugName: String = ""
) : MutableSlicedMap {

    constructor(alwaysAllowRewrite: Boolean) : this(alwaysAllowRewrite, "")

    /**
     * 主存储结构的 backing field，使用延迟初始化
     *
     * 设计说明：
     * - 类型为 `MutableMap<Any, KeyFMap>?`，键必须非空
     * - 初始为 null，节省空 SlicedMap 的内存
     * - 通过 `map` 属性访问时自动初始化
     *
     * 内部实现：
     * - 使用 OpenAddressLinearProbingHashTable 作为底层存储
     * - key -> KeyFMap 映射，每个键下可存储多个 slice 的值
     * - KeyFMap 是不可变的，更新时创建新实例
     */
    private var _map: MutableMap<Any, KeyFMap>? = null

    /**
     * 获取或创建主存储 Map
     *
     * 延迟初始化策略：
     * - 首次访问时创建 OpenAddressLinearProbingHashTable 实例
     * - 使用 `<Any, KeyFMap>` 类型，键非空，值为 KeyFMap
     * - 自动扩容，负载因子 50%，保证查找性能
     * - clear() 后 _map 重置为 null，下次访问重新创建
     */
    private val map: MutableMap<Any, KeyFMap>
        get() = _map ?: OpenAddressLinearProbingHashTable<Any, KeyFMap>().also { _map = it }

    /**
     * 收集性 slice 的键集合索引
     *
     * 用途：允许查询某个集合式 slice 包含的所有键
     * 仅在第一次使用集合式 slice 时创建
     */
    private var _collectiveSliceKeys: Multimap<WritableSlice<*, *>, Any>? = null

    /**
     * 获取或创建集合索引
     */
    private val collectiveSliceKeys: Multimap<WritableSlice<*, *>, Any>
        get() = _collectiveSliceKeys ?: ArrayListMultimap.create<WritableSlice<*, *>, Any>().also {
            _collectiveSliceKeys = it
        }

    override fun toString(): String = debugName.ifEmpty { "SlicedMap@${hashCode()}" }

    /**
     * 获取当前存储的键数量
     */
    override val size: Int
        get() = _map?.size ?: 0

    /**
     * 检查 SlicedMap 是否为空
     */
    val isEmpty: Boolean
        get() = _map == null || _map!!.isEmpty()

    /**
     * 从 SlicedMap 中获取值
     *
     * 查找流程：
     * 1. 如果主存储未初始化或键不存在，调用 slice.computeValue 处理未找到情况
     * 2. 从 KeyFMap 中获取此 slice 的值
     * 3. 调用 slice.computeValue 进行值转换（如果需要）
     *
     * @param slice 用于访问的 slice
     * @param key 键
     * @return 对应的值，如果不存在则返回 slice 的默认值
     */
    override fun <K : Any, V> get(slice: ReadOnlySlice<K, V>, key: K): V? {
        val currentMap = _map
        if (currentMap == null) {
            return slice.computeValue(this, key, null, valueNotFound = true)
        }

        val holder = currentMap[key]
        if (holder == null) {
            return slice.computeValue(this, key, null, valueNotFound = true)
        }

        val value = holder[slice.getKey()]
        return slice.computeValue(this, key, value, valueNotFound = value == null)
    }

    /**
     * 获取集合式 slice 的所有键
     *
     * 注意：只有 isCollective() 返回 true 的 slice 才能调用此方法
     *
     * @param slice 集合式 slice
     * @return 所有键的集合
     * @throws IllegalArgumentException 如果 slice 不是集合式的
     */
    override fun <K : Any, V> getKeys(slice: WritableSlice<K, V>): Collection<K> {
        require(slice.isCollective()) { "Keys are not collected for slice $slice" }

        val keys = _collectiveSliceKeys
        @Suppress("UNCHECKED_CAST")
        return keys?.get(slice) as? Collection<K> ?: emptyList()
    }

    /**
     * 遍历所有存储的键值对
     *
     * 遍历策略：
     * - 遍历所有键
     * - 对每个键，遍历其下所有 slice 的值
     * - 为每个 (slice, key, value) 三元组调用回调函数
     *
     * @param f 回调函数，接收 (WritableSlice, Key, Value) 三个参数
     */
    override fun forEach(f: (WritableSlice<*, *>, Any?, Any?) -> Unit) {
        // 使用 Java 的 forEach(BiConsumer) 方法而不是 Kotlin 的扩展函数
        // 避免调用 entries 属性（在 OpenAddressLinearProbingHashTable 中不支持）
        _map?.forEach { key, holder ->
            holder.keys.forEach { sliceKey ->
                val value = holder[sliceKey]
                val slice = (sliceKey as AbstractWritableSlice<*, *>).slice
                f(slice, key, value)
            }
        }
    }

    /**
     * 插入或更新键值对
     *
     * 插入流程：
     * 1. 验证键值对是否符合 slice 的约束（check 方法）
     * 2. 获取或创建主存储 Map
     * 3. 检查重写策略是否允许更新
     * 4. 为集合式 slice 记录键
     * 5. 更新存储
     * 6. 触发 afterPut 回调
     *
     * @param slice 目标 slice
     * @param key 键
     * @param value 值
     */
    override fun <K : Any, V> put(slice: WritableSlice<K, V>, key: K, value: V) {
        // 1. 验证键值对
        if (!slice.check(key, value)) {
            return
        }

        // 2. 获取或创建主存储
        val currentMap = map  // 触发延迟初始化

        val holder = currentMap[key] ?: KeyFMap.EMPTY_MAP
        val sliceKey = slice.getKey()

        // 3. 检查重写策略
        if (!alwaysAllowRewrite && !canRewrite(slice, key, holder, sliceKey, value)) {
            return
        }

        // 4. 为集合式 slice 记录键
        if (slice.isCollective()) {
            collectiveSliceKeys.put(slice, key)  // 触发延迟初始化
        }

        // 5. 更新存储
        currentMap[key] = holder.plus(sliceKey, value ?: return)

        // 6. 触发后置回调
        slice.afterPut(this, key, value)
    }

    /**
     * 检查是否可以重写现有值
     *
     * 重写检查逻辑：
     * 1. 如果重写策略不需要处理，直接允许
     * 2. 如果当前没有旧值，允许写入
     * 3. 调用重写策略的 processRewrite 方法决定是否允许
     *
     * @return true 允许重写，false 禁止重写
     */
    private fun <K : Any, V> canRewrite(
        slice: WritableSlice<K, V>,
        key: K,
        holder: KeyFMap,
        sliceKey: Any,
        newValue: V
    ): Boolean {
        val rewritePolicy = slice.getRewritePolicy()
        if (!rewritePolicy.rewriteProcessingNeeded(key)) {
            return true
        }

        @Suppress("UNCHECKED_CAST")
        val oldValue = holder.get(sliceKey as Key<V>) ?: return true
        return rewritePolicy.processRewrite(slice, key, oldValue, newValue)
    }

    /**
     * 清空 SlicedMap
     *
     * 重置策略：
     * - 将内部存储设为 null，释放内存
     * - 下次访问时会重新延迟初始化
     * - 适合需要完全重置状态的场景
     */
    override fun clear() {
        _map = null
        _collectiveSliceKeys = null
    }

    /**
     * 获取指定 slice 的所有内容
     *
     * 返回一个不可变的 Map，包含此 slice 的所有键值对
     *
     * @param slice 要查询的 slice
     * @return 不可变的 Map，包含所有 (key, value) 对
     */
    override fun <K : Any, V> getSliceContents(slice: ReadOnlySlice<K, V>): ImmutableMap<K, V> {
        val currentMap = _map ?: return ImmutableMap.of()
        return buildSliceContents(currentMap, slice)
    }

    /**
     * 构建指定 slice 的所有内容
     *
     * 遍历所有键，提取此 slice 的值
     *
     * @param currentMap 当前存储的 Map（键为非空 Any 类型）
     * @param slice 目标 slice
     * @return 不可变的 Map
     */
    private fun <K : Any, V> buildSliceContents(
        currentMap: Map<Any, KeyFMap>,
        slice: ReadOnlySlice<K, V>
    ): ImmutableMap<K, V> {
        val builder = ImmutableMap.builder<K, V>()
        val sliceKey = slice.getKey()

        currentMap.forEach { (key, holder) ->
            holder[sliceKey]?.let { value ->
                @Suppress("UNCHECKED_CAST")
                builder.put(key as K, value)
            }
        }

        return builder.build()
    }

    /**
     * 删除某个 slice 中的特定键值对
     *
     * 删除逻辑：
     * 1. 从 KeyFMap 中移除此 slice 的值
     * 2. 如果该键下所有 slice 都被删除，移除整个键
     * 3. 从集合式 slice 索引中移除键
     *
     * @param slice 要操作的 slice
     * @param key 要删除的键
     * @return true 成功删除，false 键不存在
     */
    fun <K : Any, V> removeBySlice(slice: ReadOnlySlice<K, V>, key: K): Boolean {
        val currentMap = _map ?: return false
        val holder = currentMap[key] ?: return false
        val sliceKey = slice.getKey()

        // 检查此 slice 是否存在值
        if (holder[sliceKey] == null) {
            return false
        }

        val newHolder = holder.minus(sliceKey)

        if (newHolder.isEmpty) {
            // 如果该键下所有 slice 都被删除，移除整个键
            currentMap.remove(key)
            removeFromCollectiveKeys(slice, key)
        } else {
            // 否则只更新 KeyFMap
            currentMap[key] = newHolder
        }

        return true
    }

    /**
     * 从收集性 slice 索引中移除键
     *
     * @param slice 目标 slice
     * @param key 要移除的键
     */
    private fun <K : Any, V> removeFromCollectiveKeys(slice: ReadOnlySlice<K, V>, key: K) {
        if (slice is WritableSlice && slice.isCollective()) {
            _collectiveSliceKeys?.remove(slice, key)
        }
    }

    /**
     * 删除指定键的所有 slice 数据
     *
     * 删除流程：
     * 1. 从主存储中移除键
     * 2. 清理所有相关的集合式 slice 索引
     *
     * @param key 要删除的键
     */
    override fun <K : Any> remove(key: K) {
        _map?.remove(key)?.let { holder ->
            // 清理收集性 slice 索引
            holder.keys.forEach { sliceKey ->
                val slice = (sliceKey as? AbstractWritableSlice<*, *>)?.slice
                if (slice?.isCollective() == true) {
                    _collectiveSliceKeys?.remove(slice, key)
                }
            }
        }
    }
}