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
 * SlicedMap 接口，提供多维键值对存储
 * 使用 Slice 对象作为类型安全的键来访问不同类型的值
 */
interface SlicedMap {

    /**
     * 从 SlicedMap 中获取值
     *
     * @param slice 用于访问的 slice
     * @param key 键
     * @return 对应的值，如果不存在则返回 null
     */
    operator fun <K : Any, V> get(slice: ReadOnlySlice<K, V>, key: K): V?

    /**
     * 获取集合式 slice 的所有键
     * 注意：slice.isCollective() 必须返回 true
     *
     * @param slice 集合式 slice
     * @return 所有键的集合
     */
    fun <K : Any, V> getKeys(slice: WritableSlice<K, V>): Collection<K>

    /**
     * 遍历 SlicedMap 中的所有条目
     *
     * @param f 遍历函数，接收 (WritableSlice, Key, Value) 三个参数
     */
    fun forEach(f: (WritableSlice<*, *>, Any?, Any?) -> Unit)

    companion object {
        /**
         * 什么都不做的 SlicedMap 实现
         */
        @JvmField
        val DO_NOTHING: SlicedMap = object : SlicedMap {
            override fun <K : Any, V> get(slice: ReadOnlySlice<K, V>, key: K): V? {
                return slice.computeValue(this, key, null, true)
            }

            override fun <K : Any, V> getKeys(slice: WritableSlice<K, V>): Collection<K> {
                return emptySet()
            }

            override fun forEach(f: (WritableSlice<*, *>, Any?, Any?) -> Unit) {
                // 不做任何操作
            }
        }
    }
}
