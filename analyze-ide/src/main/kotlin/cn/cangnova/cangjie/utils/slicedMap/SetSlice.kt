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


/**
 * 集合切片类
 *
 * 用于存储布尔值的特殊切片，通常用作标记集合。
 * 键存在于切片中表示true，不存在表示false。
 *
 * @param K 键类型
 * @property rewritePolicy 重写策略
 * @property isCollectiveOverride 是否覆盖集合行为
 */
open class SetSlice<K> @JvmOverloads constructor(rewritePolicy: RewritePolicy, isCollective: Boolean = false) :
    BasicWritableSlice<K, Boolean>(rewritePolicy, isCollective) {
    companion object {
        @JvmField
        val DEFAULT = false
    }

    /**
     * 计算给定键和值的实际值
     *
     * @param map 切片映射
     * @param key 键
     * @param value 值
     * @param valueNotFound 是否未找到值
     * @return true或false
     */
    override fun check(key: K, value: Boolean): Boolean {
        return value != DEFAULT
    }

    override fun computeValue(map: SlicedMap?, key: K, value: Boolean?, valueNotFound: Boolean): Boolean? {
        val result = super.computeValue(map, key, value, valueNotFound)
        return result ?: DEFAULT
    }

    override fun makeRawValueVersion(): ReadOnlySlice<K, Boolean> {
        return object : DelegatingSlice<K, Boolean>(this) {
            override fun computeValue(map: SlicedMap?, key: K, value: Boolean?, valueNotFound: Boolean): Boolean? {
                if (valueNotFound) return DEFAULT
                return value
            }


        }
    }


}
