/*
 * Copyright 2026 LinQingYing. and contributors.
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

package org.cangnova.cangjie.resolve.calls.inference

import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.FunctionType
import org.cangnova.cangjie.types.model.TypeVariableMarker

/**
 * 参数顺序优化器
 *
 * 按以下优先级排序参数，以优化类型推导效果：
 *
 * 1. Option 类型（嵌套深度深的优先）
 *    - 原因：仓颉支持自动装箱，先处理 Option 可正确推导 Equatable<Option<A>>
 *
 * 2. 非 Lambda、非理想类型参数
 *    - 原因：这些参数提供最确定的类型信息
 *
 * 3. Lambda 参数
 *    - 原因：Lambda 参数类型通常依赖其他参数的推导结果
 *
 * 4. 理想类型参数（最后）
 *    - 原因：理想类型可转换为多种具体类型，最后处理避免过度泛化
 */
object ArgumentOrderOptimizer {

    /**
     * 获取优化后的参数处理顺序
     *
     * @param arguments 原始参数状态列表
     * @return 优化后的参数索引顺序
     */
    fun getOptimizedOrder(arguments: List<ArgumentSynthesisState>): List<Int> {
        data class IndexedArg(val index: Int, val arg: ArgumentSynthesisState)

        val indexed = arguments.mapIndexed { i, arg -> IndexedArg(i, arg) }

        // 分组
        val options = indexed.filter { it.arg.isOptionType && !it.arg.isLambda }
        val normalNonLambda = indexed.filter {
            !it.arg.isOptionType && !it.arg.isLambda && !it.arg.isIdealType
        }
        val lambdas = indexed.filter { it.arg.isLambda }
        val ideals = indexed.filter { it.arg.isIdealType && !it.arg.isLambda }

        // Option 按嵌套深度排序（深的优先）
        val sortedOptions = options.sortedByDescending { it.arg.optionNestingLevel }

        // 合并顺序：Option → 普通非Lambda → Lambda → 理想类型
        return (sortedOptions + normalNonLambda + lambdas + ideals).map { it.index }
    }

    /**
     * 获取可以用部分解分析的参数
     *
     * @param arguments 参数状态列表
     * @param partialSolution 当前部分解
     * @return 可以分析的参数索引列表
     */
    fun getAnalyzableArguments(
        arguments: List<ArgumentSynthesisState>,
        partialSolution: Map<TypeVariableMarker, CangJieType>
    ): List<Int> {
        return arguments.indices.filter { index ->
            val arg = arguments[index]
            !arg.failed && !arg.analyzed && canAnalyzeWithPartialSolution(arg, partialSolution)
        }
    }

    /**
     * 检查参数是否可以用部分解分析
     */
    private fun canAnalyzeWithPartialSolution(
        arg: ArgumentSynthesisState,
        partialSolution: Map<TypeVariableMarker, CangJieType>
    ): Boolean {
        if (!arg.isLambda) {
            // 非 Lambda 参数总是可以分析
            return true
        }

        // Lambda 参数需要检查参数类型是否已确定
        val functionType = arg.parameterType as? FunctionType ?: return false
        return functionType.parameterTypes.all { paramType ->
            !paramType.containsUnresolvedTypeVariables(partialSolution)
        }
    }
}