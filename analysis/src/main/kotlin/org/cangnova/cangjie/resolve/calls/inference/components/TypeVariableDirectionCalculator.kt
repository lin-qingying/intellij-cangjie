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

package org.cangnova.cangjie.resolve.calls.inference.components

import org.cangnova.cangjie.resolve.calls.model.PostponedResolvedAtomMarker
import org.cangnova.cangjie.types.model.CangJieTypeMarker

/**
 * 类型变量方向计算器
 *
 * 该类用于计算类型推断过程中类型变量的解析方向。在类型推断系统中，
 * 类型变量可能需要向子类型方向解析（协变），向超类型方向解析（逆变），
 * 或者方向未知。这个计算器根据上下文、延迟处理的原子和顶层类型来
 * 确定最合适的解析方向。
 *
 * @property c 变量固定查找器的上下文，提供类型推断所需的环境信息
 * @property postponedCjPrimitives 延迟解析的仓颉原始类型列表，
 *                                  这些原子在类型推断过程中被暂时挂起等待更多信息
 * @property topLevelType 顶层类型标记，表示类型推断的起始或目标类型
 */
class TypeVariableDirectionCalculator(
    private val c: VariableFixationFinder.Context,
    private val postponedCjPrimitives: List<PostponedResolvedAtomMarker>,
    topLevelType: CangJieTypeMarker
) {
    /**
     * 解析方向枚举
     *
     * 定义了类型变量在类型推断过程中可能的解析方向。
     */
    enum class ResolveDirection {
        /**
         * 向子类型方向解析
         *
         * 表示类型变量应该被推断为更具体的子类型。
         * 这通常发生在协变位置，例如函数返回类型。
         *
         * 示例：如果有 `List<T>`，且 T 向子类型解析，
         * 则 T 可能被推断为 String 而不是 Any
         */
        TO_SUBTYPE,

        /**
         * 向超类型方向解析
         *
         * 表示类型变量应该被推断为更通用的超类型。
         * 这通常发生在逆变位置，例如函数参数类型。
         *
         * 示例：如果有 `Consumer<T>`，且 T 向超类型解析，
         * 则 T 可能被推断为 Any 而不是 String
         */
        TO_SUPERTYPE,

        /**
         * 方向未知
         *
         * 表示无法确定类型变量的解析方向，或者该类型变量
         * 在不变位置（既不协变也不逆变）。在这种情况下，
         * 类型推断系统需要使用其他策略来确定类型。
         */
        UNKNOWN
    }
}