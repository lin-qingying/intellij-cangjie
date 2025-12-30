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
 * 重写策略接口，用于控制 SlicedMap 中值的重写行为
 */
interface RewritePolicy {
    /**
     * 判断是否需要进行重写处理
     *
     * @param key 要检查的键
     * @return 如果需要重写处理返回 true，否则返回 false
     */
    fun <K : Any> rewriteProcessingNeeded(key: K): Boolean

    /**
     * 处理重写操作
     *
     * @param slice 要重写的 slice
     * @param key 键
     * @param oldValue 旧值
     * @param newValue 新值
     * @return true 表示执行写入，false 表示跳过
     */
    fun <K : Any, V: Any> processRewrite(slice: WritableSlice<K, V>, key: K, oldValue: V?, newValue: V): Boolean

    companion object {
        /**
         * 什么都不做的重写策略，所有重写操作都会被跳过
         */

        object DO_NOTHING : RewritePolicy {
            override fun <K : Any> rewriteProcessingNeeded(key: K): Boolean = false

            override fun <K : Any, V: Any> processRewrite(
                slice: WritableSlice<K, V>,
                key: K,
                oldValue: V?,
                newValue: V
            ): Boolean {
                throw UnsupportedOperationException("DO_NOTHING policy should not process rewrites")
            }
        }
    }
}
