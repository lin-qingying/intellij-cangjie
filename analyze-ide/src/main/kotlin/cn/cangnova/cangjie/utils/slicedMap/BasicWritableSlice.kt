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

import java.lang.reflect.Modifier

/**
 * 基本可写切片实现类
 *
 * 提供可写切片接口的基本实现，支持重写策略和集合操作。
 *
 * @param K 键的类型
 * @param V 值的类型
 * @property rewritePolicy 重写策略
 * @property isCollective 是否为集合类型切片
 */
open class BasicWritableSlice<K, V>(
    override val rewritePolicy: RewritePolicy,
    override val isCollective: Boolean = false
) : AbstractWritableSlice<K, V>(
    "<BasicWritableSlice>"
) {

    /**
     * 调试名称，用于标识切片
     */
    var debugName: String? = null
        set(value) {
            if (field != null) {
                throw IllegalStateException("Debug name already set for $this")
            }
            field = value
        }


    /**
     * 检查键值对是否有效
     *
     * @param key 键
     * @param value 值
     * @return 如果有效返回true（允许写入），否则返回false（跳过写入）
     */
    override fun check(key: K, value: V): Boolean {
        // assert(key != null) { "$this called with null key" }
        assert(value != null) { "$this called with null value" }
        return true
    }

    /**
     * 在写入键值对后执行的处理操作
     *
     * @param map 可变切片映射
     * @param key 键
     * @param value 值
     */
    override fun afterPut(map: MutableSlicedMap, key: K, value: V) {
        // 默认不执行任何操作
    }

    /**
     * 计算给定键和值的实际值
     *
     * @param map 切片映射
     * @param key 键
     * @param value 值
     * @param valueNotFound 是否未找到值
     * @return 计算后的值
     */
    override fun computeValue(map: SlicedMap?, key: K, value: V?, valueNotFound: Boolean): V? {
        assert(!valueNotFound || value == null)
        return value
    }

    /**
     * 返回切片的字符串表示
     *
     * @return 调试名称或默认字符串表示
     */
    override fun toString(): String {
        return debugName ?: super.toString()
    }

    /**
     * 创建此切片的原始值版本
     *
     * @return 只读的原始值切片
     */
    override fun makeRawValueVersion(): ReadOnlySlice<K, V> {
        return object : DelegatingSlice<K, V>(this) {


            override fun computeValue(
                map: SlicedMap?,
                key: K,
                value: V?,
                valueNotFound: Boolean
            ): V? {
                assert(!valueNotFound || value == null)
                return value
            }
        }
    }

    companion object {
        /**
         * 初始化指定类中所有切片字段的调试名称
         *
         * @param declarationOwner 声明切片的类
         * @return null
         */
        @JvmStatic
        fun initSliceDebugNames(declarationOwner: Class<*>): Void? {
            for (field in declarationOwner.fields) {
                if (!Modifier.isStatic(field.modifiers)) continue
                try {
                    val value = field.get(null)
                    if (value is BasicWritableSlice<*, *>) {
                        value.debugName = field.name
                    }
                } catch (e: IllegalAccessException) {
                    throw IllegalStateException(e)
                }
            }
            return null
        }
    }
}
