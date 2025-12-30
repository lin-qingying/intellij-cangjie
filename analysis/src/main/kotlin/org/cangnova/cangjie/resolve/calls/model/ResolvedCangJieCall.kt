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

package org.cangnova.cangjie.resolve.calls.model


/**
 * 已解析的调用参数
 *
 * 这是一个密封类，表示函数调用中参数解析的结果。
 * 每个参数可能是以下三种类型之一：
 * - [DefaultArgument]: 使用默认值的参数
 * - [SimpleArgument]: 普通的单个参数
 * - [VarargArgument]: 可变参数
 *
 * 该类用于在调用解析过程中跟踪每个参数的匹配情况。
 */
sealed class ResolvedCallArgument {
    /**
     * 参数列表
     *
     * 包含与此解析结果关联的所有调用参数。
     */
    abstract val arguments: List<CangJieCallArgument>

    /**
     * 默认参数
     *
     * 表示使用参数的默认值，因此不需要传递实际参数。
     * 该对象的 [arguments] 列表始终为空。
     */
    object DefaultArgument : ResolvedCallArgument() {
        override val arguments: List<CangJieCallArgument>
            get() = emptyList()

    }

    /**
     * 简单参数
     *
     * 表示一个普通的单个参数，包含传递给函数的实际参数值。
     *
     * @property callArgument 实际传递的调用参数
     */
    class SimpleArgument(val callArgument: CangJieCallArgument) : ResolvedCallArgument() {
        override val arguments: List<CangJieCallArgument>
            get() = listOf(callArgument)

    }

    /**
     * 可变参数
     *
     * 表示传递给可变参数（vararg）的一组参数。
     * 可变参数允许传递零个或多个参数，这些参数会被组合成一个数组或列表。
     *
     * @property arguments 传递给可变参数的所有参数列表
     */
    class VarargArgument(override val arguments: List<CangJieCallArgument>) : ResolvedCallArgument()
}
