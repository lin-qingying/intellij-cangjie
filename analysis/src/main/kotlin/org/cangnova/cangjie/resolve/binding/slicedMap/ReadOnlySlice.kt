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
 * 只读 Slice 接口，用于从 SlicedMap 中检索值
 *
 * @param K 键的类型
 * @param V 值的类型
 */
interface ReadOnlySlice<K : Any, V> {
    /**
     * 获取与此 slice 关联的键
     */
    fun getKey(): KeyWithSlice<K, V, out ReadOnlySlice<K, V>>

    /**
     * 计算或转换存储的值
     *
     * @param map 包含数据的 SlicedMap
     * @param key 要查找的键
     * @param value 存储的原始值
     * @param valueNotFound 是否未找到值
     * @return 计算后的值
     */
    fun computeValue(map: SlicedMap, key: K, value: V?, valueNotFound: Boolean): V?

    /**
     * 创建一个原始值版本的 slice，跳过所有 computeValue() 调用，
     * 仅从存储中检索原始值
     *
     * @return 原始值版本的 slice
     */
    fun makeRawValueVersion(): ReadOnlySlice<K, V>
}
