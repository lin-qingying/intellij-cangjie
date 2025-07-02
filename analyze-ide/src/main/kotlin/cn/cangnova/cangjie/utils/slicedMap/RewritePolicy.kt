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
 * 重写策略接口
 * 
 * 定义了在切片映射中处理键值对重写时的策略
 */
interface RewritePolicy {
    
    /**
     * 判断是否需要对指定键进行重写处理
     * 
     * @param key 要检查的键
     * @return 如果需要处理重写返回true，否则返回false
     */
    fun <K> rewriteProcessingNeeded(key: K): Boolean
    
    /**
     * 处理重写操作
     * 
     * @param slice 可写切片
     * @param key 键
     * @param oldValue 旧值
     * @param newValue 新值
     * @return 如果允许重写返回true，否则返回false
     */
    fun <K, V> processRewrite(slice: WritableSlice<K, V>, key: K, oldValue: V, newValue: V): Boolean
    
    companion object {
        /**
         * 不执行任何重写处理的策略
         */
        @JvmField
        val DO_NOTHING: RewritePolicy = object : RewritePolicy {
            override fun <K> rewriteProcessingNeeded(key: K): Boolean {
                return false
            }
            
            override fun <K, V> processRewrite(slice: WritableSlice<K, V>, key: K, oldValue: V, newValue: V): Boolean {
                throw UnsupportedOperationException()
            }
        }
    }
} 