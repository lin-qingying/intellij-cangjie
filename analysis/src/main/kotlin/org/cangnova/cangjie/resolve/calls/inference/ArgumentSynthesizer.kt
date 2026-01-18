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
import org.cangnova.cangjie.types.ComposableTypeSubstitutor
import org.cangnova.cangjie.types.isOptionType
import org.cangnova.cangjie.types.makeOption
import org.cangnova.cangjie.types.model.TypeVariableMarker

/**
 * 参数合成器
 *
 * 负责在迭代类型推断过程中合成参数的类型，支持：
 * - 使用部分解替换类型变量
 * - Option 类型的自动装箱分析
 * - Lambda 参数的延迟分析
 * - 类型一致性检查
 *
 * ## 工作原理
 * 1. **类型替换**: 使用部分解中已推导的类型变量替换形参类型中的类型变量
 * 2. **类型分析**: 分析实参类型与替换后的形参类型的关系
 * 3. **约束生成**: 生成子类型约束或相等约束
 *
 * ## 示例
 * ```kotlin
 * // 函数声明: fun foo<T>(x: Equatable<Option<T>>)
 * // 调用: foo(Some(42))
 *
 * // 第1轮迭代：
 * // - 部分解: {}
 * // - 形参类型: Equatable<Option<T>>  (包含未解析的 T)
 * // - 分析 Some(42)，发现是 Option<Int64>
 * // - 生成约束: Option<Int64> <: Option<T> → Int64 <: T
 *
 * // 第2轮迭代：
 * // - 部分解: {T → Int64}
 * // - 形参类型替换后: Equatable<Option<Int64>>
 * // - 验证 Some(42) 的类型 Option<Int64> 与 Equatable<Option<Int64>> 兼容
 * // - 收敛
 * ```
 */
class ArgumentSynthesizer(
    /**
     * 约束系统构建器
     */
    private val constraintSystemBuilder: ConstraintSystemBuilder
) {
    /**
     * 合成参数
     *
     * 使用部分解分析参数，并生成相应的类型约束。
     *
     * @param arg 参数状态
     * @param partialSolution 当前的部分解
     * @return true 如果合成成功，false 如果需要等待更多信息
     */
    fun synthesizeArgument(
        arg: ArgumentSynthesisState,
        partialSolution: Map<TypeVariableMarker, CangJieType>
    ): SynthesisResult {
        // 1. 检查 Lambda 参数的先决条件
        if (arg.isLambda && !canAnalyzeLambda(arg, partialSolution)) {
            return SynthesisResult.NeedsMoreInfo("Lambda 参数类型包含未解析的类型变量")
        }

        // 2. 使用部分解替换形参类型中的类型变量
        val substitutedParameterType = substituteTypeVariables(
            type = arg.parameterType,
            partialSolution = partialSolution
        )

        // 3. 分析实参类型
        val argumentType = arg.argumentType
        if (argumentType == null) {
            return SynthesisResult.Failed("实参类型为空")
        }

        // 4. 检查类型兼容性并生成约束
        val compatible = checkTypeCompatibility(
            argumentType = argumentType,
            parameterType = substitutedParameterType,
            arg = arg
        )

        if (!compatible) {
            return SynthesisResult.Failed("类型不兼容: $argumentType 不是 $substitutedParameterType 的子类型")
        }

        // 5. 记录推导出的类型
        arg.inferredType = substitutedParameterType

        return SynthesisResult.Success(substitutedParameterType)
    }

    /**
     * 检查 Lambda 是否可以分析
     *
     * Lambda 参数需要其参数类型完全确定后才能分析。
     */
    private fun canAnalyzeLambda(
        arg: ArgumentSynthesisState,
        partialSolution: Map<TypeVariableMarker, CangJieType>
    ): Boolean {
        val functionType = arg.parameterType as? FunctionType ?: return true

        // 检查 Lambda 的参数类型是否都已解析
        return functionType.parameterTypes.all { paramType ->
            !paramType.containsUnresolvedTypeVariables(partialSolution)
        }
    }

    /**
     * 使用部分解替换类型中的类型变量
     *
     * @param type 待替换的类型
     * @param partialSolution 部分解（类型变量到具体类型的映射）
     * @return 替换后的类型
     */
    private fun substituteTypeVariables(
        type: CangJieType,
        partialSolution: Map<TypeVariableMarker, CangJieType>
    ): CangJieType {
        if (partialSolution.isEmpty()) {
            return type
        }

        // 创建类型替换器
        val substitutor = createTypeSubstitutor(partialSolution)

        // 执行替换 - 使用 safeSubstitute
        return substitutor.safeSubstitute(type.unwrap()) as? CangJieType ?: type
    }

    /**
     * 创建类型替换器
     *
     * 使用 ComposableTypeSubstitutor 基于部分解创建类型替换器。
     * 该替换器用于将类型中的类型变量替换为已推导的具体类型。
     *
     * @param partialSolution 部分解（类型变量到具体类型的映射）
     * @return 类型替换器
     */
    private fun createTypeSubstitutor(
        partialSolution: Map<TypeVariableMarker, CangJieType>
    ): ComposableTypeSubstitutor {
        if (partialSolution.isEmpty()) {
            return ComposableTypeSubstitutor.EMPTY
        }

        // 将 TypeVariableMarker 映射转换为 TypeConstructor 映射
        val constructorMap = mutableMapOf<org.cangnova.cangjie.types.TypeConstructor, org.cangnova.cangjie.types.UnwrappedType>()

        for ((variable, type) in partialSolution) {
            // 获取类型变量的类型构造器
            val constructor = (variable as? org.cangnova.cangjie.resolve.calls.inference.model.TypeVariableFromCallableDescriptor)
                ?.freshTypeConstructor

            if (constructor is org.cangnova.cangjie.types.TypeConstructor) {
                constructorMap[constructor] = type.unwrap()
            }
        }

        // 使用 ComposableTypeSubstitutor 创建替换器
        return ComposableTypeSubstitutor.create(constructorMap)
    }

    /**
     * 检查类型兼容性
     *
     * 验证实参类型是否与形参类型兼容，并生成相应的约束。
     * 简化版本：直接使用约束系统的子类型检查
     *
     * @param argumentType 实参类型
     * @param parameterType 形参类型（已替换类型变量）
     * @param arg 参数状态（用于特殊处理）
     * @return true 如果类型兼容
     */
    private fun checkTypeCompatibility(
        argumentType: CangJieType,
        parameterType: CangJieType,
        arg: ArgumentSynthesisState
    ): Boolean {
        // 特殊处理 Option 类型
        if (arg.isOptionType) {
            return checkOptionTypeCompatibility(argumentType, parameterType)
        }

        // 一般情况：检查子类型关系
        // 简化：直接检查类型相等或子类型关系
        return org.cangnova.cangjie.types.TypeUtils.equalTypes(argumentType, parameterType) ||
                isSubtype(argumentType, parameterType)
    }

    /**
     * 检查 Option 类型兼容性
     *
     * 仓颉支持自动装箱：T 可以自动装箱为 Option<T>
     */
    private fun checkOptionTypeCompatibility(
        argumentType: CangJieType,
        parameterType: CangJieType
    ): Boolean {
        // 情况 1: 如果参数类型是 Option<T>，形参类型也是 Option<U>
        // 检查 T <: U
        val argIsOption = argumentType.unwrap().isOptionType()
        val paramIsOption = parameterType.unwrap().isOptionType()

        if (argIsOption && paramIsOption) {
            // Option<T> <: Option<U> 当 T <: U
            return org.cangnova.cangjie.types.TypeUtils.equalTypes(argumentType, parameterType) ||
                    isSubtype(argumentType, parameterType)
        }

        // 情况 2: 自动装箱 - T <: Option<T>
        // 如果参数类型不是 Option，但形参类型是 Option
        if (!argIsOption && paramIsOption) {
            // 将 argumentType 装箱为 Option<argumentType>
            val boxedArgumentType = argumentType.makeOption()
            return org.cangnova.cangjie.types.TypeUtils.equalTypes(boxedArgumentType, parameterType) ||
                    isSubtype(boxedArgumentType, parameterType)
        }

        // 情况 3: 普通子类型检查
        return org.cangnova.cangjie.types.TypeUtils.equalTypes(argumentType, parameterType) ||
                isSubtype(argumentType, parameterType)
    }

    /**
     * 简化的子类型检查
     */
    private fun isSubtype(subType: CangJieType, superType: CangJieType): Boolean {
        // 使用约束系统的子类型检查
        return try {
            // 简化：使用现有的类型检查器
            org.cangnova.cangjie.types.checker.CangJieTypeChecker.DEFAULT.isSubtypeOf(subType, superType)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 批量合成参数
     *
     * @param arguments 参数状态列表
     * @param partialSolution 部分解
     * @param order 参数处理顺序（可选，如果为空则按索引顺序）
     * @return 合成结果列表
     */
    fun synthesizeArguments(
        arguments: List<ArgumentSynthesisState>,
        partialSolution: Map<TypeVariableMarker, CangJieType>,
        order: List<Int>? = null
    ): List<Pair<Int, SynthesisResult>> {
        val processingOrder = order ?: arguments.indices.toList()
        val results = mutableListOf<Pair<Int, SynthesisResult>>()

        for (index in processingOrder) {
            val arg = arguments[index]

            // 跳过已分析或失败的参数
            if (arg.analyzed || arg.failed) {
                continue
            }

            val result = synthesizeArgument(arg, partialSolution)
            results.add(index to result)

            // 更新参数状态
            when (result) {
                is SynthesisResult.Success -> {
                    arg.analyzed = true
                }
                is SynthesisResult.Failed -> {
                    arg.failed = true
                }
                is SynthesisResult.NeedsMoreInfo -> {
                    // 需要更多信息，暂不标记为失败
                }
            }
        }

        return results
    }
}

/**
 * 参数合成结果
 */
sealed class SynthesisResult {
    /**
     * 合成成功
     */
    data class Success(val inferredType: CangJieType) : SynthesisResult()

    /**
     * 需要更多信息（例如等待其他类型变量推导）
     */
    data class NeedsMoreInfo(val reason: String) : SynthesisResult()

    /**
     * 合成失败
     */
    data class Failed(val reason: String) : SynthesisResult()

    /**
     * 是否成功
     */
    val isSuccess: Boolean
        get() = this is Success

    /**
     * 是否失败
     */
    val isFailed: Boolean
        get() = this is Failed

    override fun toString(): String = when (this) {
        is Success -> "成功: $inferredType"
        is NeedsMoreInfo -> "需要更多信息: $reason"
        is Failed -> "失败: $reason"
    }
}
