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

package org.cangnova.cangjie.protodebugger.result

/**
 * 分页结果
 *
 * 表示支持分页查询的结果集，包含当前页数据和是否有更多数据的标志。
 *
 * @param T 结果项的类型
 * @property items 当前页的数据项
 * @property hasMore 是否还有更多数据
 */
data class PagedResult<out T>(
    val items: List<T>,
    val hasMore: Boolean
) {
    /**
     * 数据项数量
     */
    val size: Int
        get() = items.size

    /**
     * 是否为空
     */
    val isEmpty: Boolean
        get() = items.isEmpty()

    /**
     * 是否非空
     */
    val isNotEmpty: Boolean
        get() = items.isNotEmpty()

    /**
     * 转换数据项类型
     */
    fun <R> map(transform: (T) -> R): PagedResult<R> {
        return PagedResult(items.map(transform), hasMore)
    }

    /**
     * 过滤数据项
     */
    fun filter(predicate: (T) -> Boolean): PagedResult<T> {
        return PagedResult(items.filter(predicate), hasMore)
    }

    companion object {
        /**
         * 创建空结果
         */
        @JvmStatic
        fun <T> empty(): PagedResult<T> {
            return PagedResult(emptyList(), false)
        }

        /**
         * 创建完整结果（无更多数据）
         */
        @JvmStatic
        fun <T> complete(items: List<T>): PagedResult<T> {
            return PagedResult(items, false)
        }

        /**
         * 创建部分结果（有更多数据）
         */
        @JvmStatic
        fun <T> partial(items: List<T>): PagedResult<T> {
            return PagedResult(items, true)
        }
    }
}