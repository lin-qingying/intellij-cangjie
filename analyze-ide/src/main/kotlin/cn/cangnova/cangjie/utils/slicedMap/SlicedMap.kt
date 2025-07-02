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
import kotlin.jvm.functions.Function3

/**
 * 切片映射接口
 *
 * 定义了切片映射的基本操作，包括获取值、获取键集合和遍历等功能。
 */
interface SlicedMap {

    /**
     * 获取指定切片和键的值
     *
     * @param slice 只读切片
     * @param key 键
     * @return 值，如果不存在则为null
     */
    operator fun <K, V> get(slice: ReadOnlySlice<K, V>, key: K): V?

    /**
     * 获取指定可写切片的所有键
     *
     * 注意：slice.isCollective 必须返回true才能使用此方法
     *
     * @param slice 可写切片
     * @return 键的集合
     */
    fun <K, V> getKeys(slice: WritableSlice<K, V>): Collection<K>

    /**
     * 对映射中的每个条目执行给定函数
     *
     * @param f 要执行的函数
     */
    fun forEach(f: (WritableSlice<*, *>, Any, Any) -> Unit)

    companion object {
        /**
         * 不执行任何操作的空实现
         */
        @JvmField
        val DO_NOTHING: SlicedMap = object : SlicedMap {
            override fun <K, V> get(slice: ReadOnlySlice<K, V>, key: K): V? {
                return slice.computeValue(this, key, null, true)
            }

            override fun <K, V> getKeys(slice: WritableSlice<K, V>): Collection<K> {
                return Collections.emptySet()
            }

            override fun forEach(f: (WritableSlice<*, *>, Any, Any) -> Unit) {
                // 不执行任何操作
            }
        }
    }
} 