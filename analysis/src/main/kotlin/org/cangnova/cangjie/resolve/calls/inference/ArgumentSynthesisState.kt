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
import org.cangnova.cangjie.types.isOptionType
import org.cangnova.cangjie.types.optionNestedLevel
import org.cangnova.cangjie.types.FunctionType
import org.cangnova.cangjie.types.model.TypeVariableMarker

/**
 * 参数合成状态
 *
 * 在迭代类型推断过程中，跟踪每个参数的分析状态和特征。
 *
 * ## 参数特征
 * - **Option 类型**: 支持自动装箱的参数，需要先处理以正确推导泛型约束
 * - **Lambda 类型**: 函数类型参数，其类型推导依赖其他参数的结果
 * - **理想类型**: 可转换为多种类型的参数（如整数字面量），最后处理避免过度泛化
 *
 * ## 状态管理
 * - `analyzed`: 参数是否已分析完成
 * - `failed`: 参数分析是否失败
 * - `constraintsCollected`: 是否已收集约束
 *
 * ## 示例
 * ```kotlin
 * // 对于调用: foo(Some(42), { x -> x + 1 })
 * val arg0 = ArgumentSynthesisState(
 *     argumentType = Option<Int64>,
 *     parameterType = T,
 *     isOptionType = true,
 *     optionNestingLevel = 1
 * )
 *
 * val arg1 = ArgumentSynthesisState(
 *     argumentType = (Int64) -> Int64,
 *     parameterType = (U) -> U,
 *     isLambda = true
 * )
 * ```
 */
data class ArgumentSynthesisState(
    /**
     * 参数位置（从0开始）
     */
    val index: Int,

    /**
     * 实参类型（调用处提供的类型）
     *
     * 例如：foo(Some(42))  中 Some(42) 的类型
     */
    val argumentType: CangJieType?,

    /**
     * 形参类型（函数声明的参数类型）
     *
     * 例如：fun foo<T>(x: Equatable<Option<T>>)  中的 Equatable<Option<T>>
     */
    val parameterType: CangJieType,

    /**
     * 是否为 Lambda 参数
     *
     * Lambda 参数需要延迟处理，因为其参数类型依赖其他参数的推导结果
     */
    val isLambda: Boolean = parameterType is FunctionType,

    /**
     * 是否为 Option 类型
     *
     * Option 类型支持自动装箱，需要优先处理以正确推导泛型约束
     */
    val isOptionType: Boolean = argumentType?.isOptionType() == true,

    /**
     * Option 嵌套深度
     *
     * 例如：
     * - `Option<Int>` → 1
     * - `Option<Option<Int>>` → 2
     * - `Int` → 0
     */
    val optionNestingLevel: Int = argumentType?.optionNestedLevel() ?: 0,

    /**
     * 是否为理想类型
     *
     * 理想类型是可以转换为多种具体类型的类型，如整数字面量。
     * 这些类型最后处理，避免过度泛化类型推导结果。
     */
    val isIdealType: Boolean = false,

    /**
     * 参数是否已分析完成
     */
    var analyzed: Boolean = false,

    /**
     * 参数分析是否失败
     */
    var failed: Boolean = false,

    /**
     * 是否已收集约束
     */
    var constraintsCollected: Boolean = false,

    /**
     * 推导出的具体类型（如果已完成推导）
     */
    var inferredType: CangJieType? = null
) {
    /**
     * 检查参数类型是否包含未解析的类型变量
     *
     * @param partialSolution 当前的部分解（已推导的类型变量映射）
     * @return true 如果包含未解析的类型变量
     */
    fun containsUnresolvedTypeVariables(
        partialSolution: Map<TypeVariableMarker, CangJieType>
    ): Boolean {
        return parameterType.containsUnresolvedTypeVariables(partialSolution)
    }

    /**
     * 重置分析状态
     *
     * 在新的迭代轮次开始时，重置状态以进行重新分析
     */
    fun reset() {
        analyzed = false
        failed = false
        constraintsCollected = false
        inferredType = null
    }

    override fun toString(): String {
        return buildString {
            append("Arg[$index]")
            if (isOptionType) append(" Option($optionNestingLevel)")
            if (isLambda) append(" Lambda")
            if (isIdealType) append(" Ideal")
            if (analyzed) append(" ✓")
            if (failed) append(" ✗")
            if (constraintsCollected) append(" C")
            inferredType?.let { append(" → $it") }
        }
    }
}

/**
 * 检查类型是否包含未解析的类型变量
 *
 * 简化实现：检查类型构造器和类型参数
 */
internal fun CangJieType.containsUnresolvedTypeVariables(
    partialSolution: Map<TypeVariableMarker, CangJieType>
): Boolean {
    // 检查类型构造器是否是类型变量
    val constructor = this.constructor
    if (constructor is org.cangnova.cangjie.types.model.TypeVariableTypeConstructorMarker) {
        // 这是一个类型变量，检查是否已在部分解中
        // TypeVariableTypeConstructorMarker 有 typeVariable 属性
        val typeVariable = (constructor as? org.cangnova.cangjie.resolve.calls.inference.model.TypeVariableFromCallableDescriptor)
        if (typeVariable != null && typeVariable !in partialSolution) {
            return true
        }
    }

    // 递归检查类型参数
    for (argument in this.arguments) {
        val argType = argument.type
        if (argType.containsUnresolvedTypeVariables(partialSolution)) {
            return true
        }
    }

    // 如果是 Flexible 类型，检查上下界
    val unwrapped = this.unwrap()
    if (unwrapped is org.cangnova.cangjie.types.FlexibleType) {
        if (unwrapped.lowerBound.containsUnresolvedTypeVariables(partialSolution)) {
            return true
        }
        if (unwrapped.upperBound.containsUnresolvedTypeVariables(partialSolution)) {
            return true
        }
    }

    return false
}
