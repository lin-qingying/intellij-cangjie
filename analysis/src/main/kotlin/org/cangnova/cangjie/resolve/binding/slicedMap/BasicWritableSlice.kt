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

import java.lang.reflect.Modifier

/**
 * 基础的可写 Slice 实现
 *
 * @param K 键的类型
 * @param V 值的类型
 * @property rewritePolicy 重写策略
 * @property isCollective 是否为集合式 slice
 */
open class BasicWritableSlice<K : Any, V: Any>(
    private val rewritePolicy: RewritePolicy,
    private val isCollective: Boolean = false
) : AbstractWritableSlice<K, V>("<BasicWritableSlice>") {

    private var debugName: String? = null

    /**
     * 在写入前检查键值对
     *
     * @param key 键
     * @param value 值
     * @return true 表示执行写入
     */
    override fun check(key: K, value: V): Boolean {
        require(value != null) { "$this called with null value" }
        return true
    }

    /**
     * 写入后的回调，默认不执行任何操作
     */
    override fun afterPut(map: MutableSlicedMap, key: K, value: V) {
        // 默认不做任何操作
    }

    /**
     * 计算值，默认直接返回存储的值
     */
    override fun computeValue(map: SlicedMap, key: K, value: V?, valueNotFound: Boolean): V? {
        check(!valueNotFound || value == null) { "Value found but valueNotFound is true" }
        return value
    }

    /**
     * 获取重写策略
     */
    override fun getRewritePolicy(): RewritePolicy = rewritePolicy

    /**
     * 判断是否为集合式 slice
     */
    override fun isCollective(): Boolean = isCollective

    /**
     * 设置调试名称
     *
     * @param debugName 调试名称
     */
    fun setDebugName(debugName: String) {
        check(this.debugName == null) { "Debug name already set for $this" }
        this.debugName = debugName
    }

    override fun toString(): String = debugName ?: super.toString()

    /**
     * 创建原始值版本的 slice
     */
    override fun makeRawValueVersion(): ReadOnlySlice<K, V> {
        return object : DelegatingSlice<K, V>(this) {
            override fun computeValue(map: SlicedMap, key: K, value: V?, valueNotFound: Boolean): V? {
                check(!valueNotFound || value == null) { "Value found but valueNotFound is true" }
                return value
            }
        }
    }

    companion object {
        /**
         * 初始化 Slice 的调试名称
         * 通过反射遍历声明类的所有静态字段，将字段名设置为 BasicWritableSlice 的调试名称
         *
         * @param declarationOwner 声明 slice 的类
         */
        @JvmStatic
        fun initSliceDebugNames(declarationOwner: Class<*>) {
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
        }
    }
}
