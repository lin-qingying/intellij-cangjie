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

package cn.cangnova.cangjie.utils.slicedMap

/**
 * 委托切片类
 * 
 * 不做任何特殊处理，只是将所有调用委托给内部的可写切片。
 * 用于在保持原有切片功能的同时添加新的行为或修改部分功能。
 * 
 * @param K 键类型
 * @param V 值类型
 * @property delegate 被委托的可写切片
 */
open class DelegatingSlice<K, V>(
    private val delegate: WritableSlice<K, V>
) : WritableSlice<K, V> {
    
    /**
     * 判断是否为集合类型切片
     */
    override val isCollective: Boolean
        get() = delegate.isCollective
    
    /**
     * 检查键值对是否有效
     * 
     * @param key 键
     * @param value 值
     * @return 如果有效返回true，否则返回false
     */
    override fun check(key: K, value: V): Boolean {
        return delegate.check(key, value)
    }
    
    /**
     * 在放置键值对后执行的操作
     * 
     * @param map 可变切片映射
     * @param key 键
     * @param value 值
     */
    override fun afterPut(map: MutableSlicedMap, key: K, value: V) {
        delegate.afterPut(map, key, value)
    }
    
    /**
     * 获取重写策略
     */
    override val rewritePolicy: RewritePolicy
        get() = delegate.rewritePolicy
    
    /**
     * 获取带切片的键
     */
    override val key: KeyWithSlice<K, V, WritableSlice<K, V>>
        get() = delegate.key
    
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
        return delegate.computeValue(map, key, value, valueNotFound)

    }
    
    /**
     * 创建此切片的原始值版本
     * 
     * @return 只读的原始值切片
     */
    override fun makeRawValueVersion(): ReadOnlySlice<K, V> ?{
        return delegate.makeRawValueVersion()
    }

}
