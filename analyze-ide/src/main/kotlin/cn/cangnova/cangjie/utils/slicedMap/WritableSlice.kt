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
 * 可写切片接口
 * 
 * 定义了可以进行写入操作的切片接口，扩展了只读切片接口。
 * 可写切片支持键值对的检查、写入后处理、重写策略以及集合操作等功能。
 *
 * @param K 键的类型
 * @param V 值的类型
 */
interface WritableSlice<K, V> : ReadOnlySlice<K, V> {
    
    /**
     * 获取与此切片关联的键
     * 
     * @return 键与切片的组合
     */
    override val key: KeyWithSlice<K, V, WritableSlice<K, V>>
    
    /**
     * 检查键值对是否有效
     * 
     * @param key 键
     * @param value 值
     * @return 如果有效返回true（允许写入），否则返回false（跳过写入）
     */
    fun check(key: K, value: V): Boolean
    
    /**
     * 在写入键值对后执行的处理操作
     * 
     * @param map 可变切片映射
     * @param key 键
     * @param value 值
     */
    fun afterPut(map: MutableSlicedMap, key: K, value: V)
    
    /**
     * 获取此切片的重写策略
     * 
     * @return 重写策略
     */
    val rewritePolicy: RewritePolicy
    
    /**
     * 是否为集合类型切片
     * 
     * 集合类型的切片允许通过getKeys方法查询所有键
     * 
     * @return 如果是集合类型返回true，否则返回false
     */
    val isCollective: Boolean
} 