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
 * 可写 Slice 接口，扩展了 ReadOnlySlice，增加了写入相关的操作
 *
 * @param K 键的类型
 * @param V 值的类型
 */
interface WritableSlice<K : Any, V> : ReadOnlySlice<K, V> {
    /**
     * 获取与此 slice 关联的键
     */
    override fun getKey(): KeyWithSlice<K, V, WritableSlice<K, V>>

    /**
     * 在写入之前检查键值对是否有效
     *
     * @param key 键
     * @param value 值
     * @return true 表示执行写入，false 表示跳过
     */
    fun check(key: K, value: V): Boolean

    /**
     * 写入后的回调
     *
     * @param map 目标 SlicedMap
     * @param key 键
     * @param value 写入的值
     */
    fun afterPut(map: MutableSlicedMap, key: K, value: V)

    /**
     * 获取重写策略
     *
     * @return 重写策略
     */
    fun getRewritePolicy(): RewritePolicy

    /**
     * 判断是否为集合式 slice
     * 在 SlicedMap 中，可以请求集合式 slice 的所有键
     *
     * @return 如果是集合式 slice 返回 true
     */
    fun isCollective(): Boolean
}
