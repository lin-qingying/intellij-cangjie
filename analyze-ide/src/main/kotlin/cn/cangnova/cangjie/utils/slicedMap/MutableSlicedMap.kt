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

import com.google.common.collect.ImmutableMap
import org.jetbrains.annotations.TestOnly

/**
 * 可变切片映射接口
 * 
 * 扩展了SlicedMap接口，增加了修改映射内容的功能，
 * 如添加键值对、清空映射、移除键以及获取切片内容等操作。
 */
interface MutableSlicedMap : SlicedMap {
    
    /**
     * 获取映射中键值对的数量
     * 
     * @return 键值对数量
     */
    val size: Int get() = 0
    

    /**
     * 将指定的键值对放入对应的切片中
     * 
     * @param slice 可写切片
     * @param key 键
     * @param value 值
     */
    fun <K, V> put(slice: WritableSlice<K, V>, key: K, value: V)

    /**
     * 清空映射中的所有内容
     */
    fun clear()
    
    /**
     * 获取指定切片的所有内容
     * 
     * @param slice 只读切片
     * @return 包含所有键值对的不可变映射
     */
    @TestOnly
    fun <K, V> getSliceContents(slice: ReadOnlySlice<K, V>): ImmutableMap<K, V>
    
    /**
     * 从映射中移除指定的键
     * 
     * @param key 要移除的键
     */
    fun <K> remove(key: K)
}

