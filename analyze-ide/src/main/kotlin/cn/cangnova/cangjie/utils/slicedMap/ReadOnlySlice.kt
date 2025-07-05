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
import com.intellij.openapi.util.Key
import org.jetbrains.annotations.TestOnly

/**
 * 只读切片接口
 * 
 * 定义了切片映射中的只读切片，提供了获取值和计算值的功能。
 * 切片是SlicedMap数据结构中的基本组件，每个切片代表一种特定类型的键值映射。
 *
 * @param K 键的类型
 * @param V 值的类型
 */
interface ReadOnlySlice<K, V> {
    /**
     * 与此切片关联的键
     * 
     * @return 键与切片的组合
     */
    val key: KeyWithSlice<K, V, ReadOnlySlice<K, V>>

    /**
     * 计算指定键的值
     * 
     * 此方法允许切片实现自定义的值计算逻辑，例如从其他切片获取值、
     * 执行懒加载或根据当前映射状态计算派生值等。
     *
     * @param map 切片映射，可能为null
     * @param key 要计算值的键
     * @param value 当前存储的值，如果不存在则为null
     * @param valueNotFound 指示是否在映射中找到了值
     * @return 计算后的值，可能为null
     */
    fun computeValue(map: SlicedMap?, key: K, value: V?, valueNotFound: Boolean): V?

    /**
     * 创建一个只从存储中检索原始值的切片版本，跳过computeValue调用
     * 
     * @return 原始值版本的切片，如果不支持则返回null
     */
    fun makeRawValueVersion(): ReadOnlySlice<K, V>?
}

