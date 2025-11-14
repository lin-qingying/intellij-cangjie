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

package org.cangnova.cangjie.protodebugger.output

/**
 * 结果列表类
 *
 * 该类封装了调试器查询的结果列表，支持分页和增量加载功能。
 * 它不仅包含实际的结果数据，还提供了是否有更多数据的标志，
 * 用于支持大数据集的分批显示和处理。
 *
 * 使用场景：
 * - 变量列表的分页显示
 * - 调栈帧的批量加载
 * - 内存数据的分块读取
 * - 符号信息的渐进式获取
 *
 * 主要功能：
 * - 结果数据的封装和管理
 * - 分页查询的支持
 * - 数据完整性的标识
 * - 泛型支持，适用于各种结果类型
 *
 * 技术特点：
 * - 泛型设计，支持任意类型的结果
 * - 不可变的数据结构，线程安全
 * - 简洁的工厂方法创建
 * - 与调试器协议的无缝集成
 *
 * @param T 结果数据的类型
 * @param list 实际的结果数据列表
 * @param hasMore 是否还有更多数据未返回
 */
class ResultList<T>(val list: List<T>, val hasMore: Boolean) {
    companion object {
        /**
         * 创建结果列表实例
         *
         * 使用指定的数据列表和分页标志创建ResultList实例。
         *
         * @param T 结果数据的类型
         * @param list 结果数据列表
         * @param hasMore 是否还有更多数据
         * @return 新的ResultList实例
         */
        @JvmStatic
        fun <T> create(list: List<T>, hasMore: Boolean): ResultList<T> {
            return ResultList(list, hasMore)
        }

        /**
         * 创建空的结果列表
         *
         * 创建一个包含空数据且无更多数据的ResultList实例。
         * 常用于表示查询无结果或初始状态。
         *
         * @param T 结果数据的类型
         * @return 空的ResultList实例
         */
        @JvmStatic
        fun <T> empty(): ResultList<T> {
            return create(emptyList(), false)
        }
    }
}
