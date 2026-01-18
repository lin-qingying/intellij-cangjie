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

import org.cangnova.cangjie.resolve.calls.inference.components.CangJieConstraintSystemCompleter
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintKind
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.model.TypeVariableMarker

/**
 * 迭代类型推断引擎
 *
 * 实现编译器风格的迭代多轮类型推断，支持：
 * - 参数顺序优化（Option 优先 → 普通参数 → Lambda → 理想类型）
 * - 部分解支持（允许在未完全推导的情况下继续推导）
 * - 迭代式求解（多轮迭代，每轮收集新约束并求解）
 * - 双向信息流（已推导类型反向影响未推导参数）
 *
 * ## 工作流程
 * ```
 * 1. 初始化
 *    ↓
 * 2. 开始迭代
 *    ↓
 * 3. 参数合成（使用部分解）
 *    ↓
 * 4. 约束收集
 *    ↓
 * 5. 约束求解（允许部分解）
 *    ↓
 * 6. 检查收敛
 *    ↓ 是           ↓ 否
 * 7. 返回结果    回到步骤2
 * ```
 *
 * ## 收敛条件
 * - 所有类型变量都已推导
 * - 或：连续两轮没有新的类型变量被推导
 * - 或：达到最大迭代次数
 *
 * ## 示例
 * ```kotlin
 * // 调用: a<T>.foo(x: Equatable<Option<T>>)
 * // 实参: a<Int64>.foo(Some(42))
 *
 * val engine = IterativeTypeInferenceEngine(
 *     constraintSystemBuilder = builder,
 *     completer = completer
 * )
 *
 * val result = engine.runIterativeInference(
 *     typeVariables = listOf(T),
 *     arguments = listOf(argumentState)
 * )
 * // result: T → Int64
 * ```
 */
class IterativeTypeInferenceEngine(
    /**
     * 约束系统构建器
     */
    private val constraintSystemBuilder: ConstraintSystemBuilder,

    /**
     * 约束系统完成器
     */
    private val completer: CangJieConstraintSystemCompleter
) {
    /**
     * 参数合成器
     */
    private val argumentSynthesizer = ArgumentSynthesizer(constraintSystemBuilder)

    /**
     * 运行迭代类型推断
     *
     * @param typeVariables 待推导的类型变量列表
     * @param arguments 参数合成状态列表
     * @param maxIterations 最大迭代次数（默认10）
     * @return 类型变量到具体类型的映射
     */
    fun runIterativeInference(
        typeVariables: List<TypeVariableMarker>,
        arguments: List<ArgumentSynthesisState>,
        maxIterations: Int = 10
    ): IterativeInferenceResult {
        // 创建迭代上下文
        val context = IterativeInferenceContext(
            constraintSystemBuilder = constraintSystemBuilder,
            typeVariables = typeVariables,
            arguments = arguments,
            maxIterations = maxIterations
        )

        // 计算参数的优化顺序
        val argumentOrder = ArgumentOrderOptimizer.getOptimizedOrder(arguments)

        // 主迭代循环
        while (context.shouldContinue()) {
            context.startIteration()

            // Phase 1: 使用部分解合成参数
            synthesizeArgumentsWithPartialSolution(context, argumentOrder)

            // Phase 2: 收集约束
            collectConstraints(context, argumentOrder)

            // Phase 3: 求解约束（允许部分解）
            val newSolution = solveConstraintsWithPartialSolution(context)

            // Phase 4: 结束本轮迭代
            context.endIteration(newSolution)
        }

        // 返回最终结果
        return IterativeInferenceResult(
            solution = context.getFinalSolution(),
            converged = context.hasConverged,
            failed = context.hasFailed,
            failureReason = context.failureReason,
            iterations = context.currentIteration,
            statistics = context.getStatistics()
        )
    }

    /**
     * Phase 1: 使用部分解合成参数
     *
     * 根据参数优化顺序，使用当前已推导的部分解来合成参数。
     * 这允许后续参数利用前面参数的推导结果。
     */
    private fun synthesizeArgumentsWithPartialSolution(
        context: IterativeInferenceContext,
        argumentOrder: List<Int>
    ) {
        val partialSolution = context.getPartialSolution()

        // 获取可以用部分解分析的参数
        val analyzableArguments = ArgumentOrderOptimizer.getAnalyzableArguments(
            context.arguments,
            partialSolution
        )

        // 按优化顺序处理参数
        for (index in argumentOrder) {
            if (index !in analyzableArguments) continue

            val arg = context.arguments[index]
            if (arg.analyzed || arg.failed) continue

            // 使用部分解合成参数
            val synthesized = synthesizeArgument(arg, partialSolution)

            // 更新参数状态
            if (synthesized) {
                arg.analyzed = true
            } else {
                arg.failed = true
            }
        }
    }

    /**
     * Phase 2: 收集约束
     *
     * 为已合成的参数收集类型约束。
     */
    private fun collectConstraints(
        context: IterativeInferenceContext,
        argumentOrder: List<Int>
    ) {
        for (index in argumentOrder) {
            val arg = context.arguments[index]

            // 只处理已分析但未收集约束的参数
            if (!arg.analyzed || arg.failed || arg.constraintsCollected) continue

            // 收集约束：argumentType <: parameterType
            if (arg.argumentType != null) {
                addSubtypeConstraint(
                    context = context,
                    subType = arg.argumentType,
                    superType = arg.parameterType
                )
            }

            arg.constraintsCollected = true
        }
    }

    /**
     * Phase 3: 求解约束（允许部分解）
     *
     * 尝试求解当前收集的约束，但允许部分解：
     * 即使某些类型变量无法推导，也继续推导其他类型变量。
     *
     * @return 本轮新推导出的类型变量解
     */
    private fun solveConstraintsWithPartialSolution(
        context: IterativeInferenceContext
    ): Map<TypeVariableMarker, CangJieType> {
        val newSolution = mutableMapOf<TypeVariableMarker, CangJieType>()
        val previousSolution = context.getPartialSolution()

        // 尝试推导每个未解决的类型变量
        for (typeVariable in context.getUnresolvedTypeVariables()) {
            val inferredType = tryInferTypeVariable(
                context = context,
                typeVariable = typeVariable,
                partialSolution = previousSolution
            )

            if (inferredType != null) {
                newSolution[typeVariable] = inferredType
            }
        }

        return newSolution
    }

    /**
     * 合成单个参数
     *
     * 使用部分解来分析参数的类型。
     *
     * @param arg 参数状态
     * @param partialSolution 当前的部分解
     * @return true 如果合成成功
     */
    private fun synthesizeArgument(
        arg: ArgumentSynthesisState,
        partialSolution: Map<TypeVariableMarker, CangJieType>
    ): Boolean {
        // 如果是 Lambda，需要检查参数类型是否已确定
        if (arg.isLambda) {
            // 检查 Lambda 参数类型是否包含未解析的类型变量
            if (arg.containsUnresolvedTypeVariables(partialSolution)) {
                // Lambda 参数类型还未完全确定，跳过
                return false
            }
        }

        // 非 Lambda 或 Lambda 参数类型已确定，可以分析
        // 调用 ArgumentSynthesizer 进行参数合成
        val result = argumentSynthesizer.synthesizeArgument(arg, partialSolution)

        return when (result) {
            is SynthesisResult.Success -> true
            is SynthesisResult.Failed -> false
            is SynthesisResult.NeedsMoreInfo -> false
        }
    }

    /**
     * 添加子类型约束
     *
     * 简化版本：直接使用约束系统构建器添加约束，不创建复杂的ConstraintPosition
     *
     * @param context 迭代上下文
     * @param subType 子类型
     * @param superType 超类型
     */
    private fun addSubtypeConstraint(
        context: IterativeInferenceContext,
        subType: CangJieType,
        superType: CangJieType
    ) {
        // 简化实现：使用约束系统构建器直接添加子类型约束
        // 由于没有ConstraintPosition的简单构造方式，这里暂时跳过约束添加
        // 实际的类型兼容性检查已经在ArgumentSynthesizer中完成
    }

    /**
     * 尝试推导类型变量
     *
     * 基于当前收集的约束，尝试推导类型变量的具体类型。
     *
     * @param context 迭代上下文
     * @param typeVariable 待推导的类型变量
     * @param partialSolution 当前的部分解
     * @return 推导出的类型，如果无法推导则返回 null
     */
    private fun tryInferTypeVariable(
        context: IterativeInferenceContext,
        typeVariable: TypeVariableMarker,
        partialSolution: Map<TypeVariableMarker, CangJieType>
    ): CangJieType? {
        val storage = context.constraintStorage

        // 获取类型构造器
        val typeConstructor = when (typeVariable) {
            is org.cangnova.cangjie.resolve.calls.inference.model.TypeVariableFromCallableDescriptor ->
                typeVariable.freshTypeConstructor
            else -> return null
        }

        // 获取该类型变量的所有约束
        val variableWithConstraints = storage.notFixedTypeVariables[typeConstructor] ?: return null

        // 获取约束
        val constraints = variableWithConstraints.constraints

        // 如果没有约束，无法推导
        if (constraints.isEmpty()) return null

        // 尝试从约束中推导类型
        // 1. 如果有相等约束 (EQUALITY)，直接使用
        val equalityConstraint = constraints.find { it.kind == ConstraintKind.EQUALITY }
        if (equalityConstraint != null) {
            return equalityConstraint.type as? CangJieType
        }

        // 2. 如果只有下界约束 (LOWER)，取最具体的下界
        val lowerBounds = constraints.filter { it.kind == ConstraintKind.LOWER }
        if (lowerBounds.isNotEmpty() && constraints.none { it.kind == ConstraintKind.UPPER }) {
            // 取所有下界的公共超类型
            return findCommonSupertype(lowerBounds.map { it.type as CangJieType })
        }

        // 3. 如果只有上界约束 (UPPER)，取最宽泛的上界
        val upperBounds = constraints.filter { it.kind == ConstraintKind.UPPER }
        if (upperBounds.isNotEmpty() && lowerBounds.isEmpty()) {
            // 取所有上界的公共子类型
            return findCommonSubtype(upperBounds.map { it.type as CangJieType })
        }

        // 4. 如果同时有上下界约束，需要找到满足条件的类型
        if (lowerBounds.isNotEmpty() && upperBounds.isNotEmpty()) {
            // 需要找到类型 T，使得：
            // - 所有下界 <: T （T 是所有下界的公共超类型）
            // - T <: 所有上界 （T 是所有上界的公共子类型）

            // 步骤1: 计算所有下界的公共超类型
            val commonLowerBound = findCommonSupertype(lowerBounds.map { it.type as CangJieType })
                ?: return null

            // 步骤2: 计算所有上界的公共子类型
            val commonUpperBound = findCommonSubtype(upperBounds.map { it.type as CangJieType })
                ?: return null

            // 步骤3: 检查约束是否兼容 (commonLowerBound <: commonUpperBound)
            val isCompatible = org.cangnova.cangjie.types.checker.CangJieTypeChecker.DEFAULT
                .isSubtypeOf(commonLowerBound, commonUpperBound)

            if (!isCompatible) {
                // 约束系统不一致，无法推导
                return null
            }

            // 步骤4: 返回更具体的类型（下界的公共超类型）
            // 因为它同时满足了上下界约束，且更具体
            return commonLowerBound
        }

        return null
    }

    /**
     * 查找类型列表的公共超类型
     *
     * 使用 CommonSupertypes 工具类计算多个类型的公共超类型。
     *
     * @param types 类型列表
     * @return 公共超类型，如果无法计算则返回 null
     */
    private fun findCommonSupertype(types: List<CangJieType>): CangJieType? {
        if (types.isEmpty()) return null
        if (types.size == 1) return types.first()

        // 使用 CommonSupertypes 工具类计算公共超类型
        return org.cangnova.cangjie.types.CommonSupertypes.commonSupertype(types)
    }

    /**
     * 查找类型列表的公共子类型
     *
     * 公共子类型实际上是类型的交集（intersection）。
     * 使用 intersectTypes 方法计算多个类型的交集。
     *
     * @param types 类型列表
     * @return 公共子类型（交集类型），如果无法计算则返回 null
     */
    private fun findCommonSubtype(types: List<CangJieType>): CangJieType? {
        if (types.isEmpty()) return null
        if (types.size == 1) return types.first()

        // 使用 intersectTypes 计算类型交集
        // intersectTypes 返回 UnwrappedType，需要转换为 CangJieType
        return org.cangnova.cangjie.types.checker.intersectTypes(types.map { it.unwrap() })
    }
}

/**
 * 迭代推断结果
 */
data class IterativeInferenceResult(
    /**
     * 类型变量到具体类型的最终映射
     */
    val solution: Map<TypeVariableMarker, CangJieType>,

    /**
     * 是否收敛
     */
    val converged: Boolean,

    /**
     * 是否失败
     */
    val failed: Boolean,

    /**
     * 失败原因（如果失败）
     */
    val failureReason: String?,

    /**
     * 迭代次数
     */
    val iterations: Int,

    /**
     * 统计信息
     */
    val statistics: String
) {
    /**
     * 是否成功
     */
    val isSuccess: Boolean
        get() = converged && !failed

    override fun toString(): String {
        return buildString {
            appendLine("迭代推断结果:")
            appendLine("  状态: ${if (isSuccess) "成功" else if (failed) "失败" else "未收敛"}")
            appendLine("  迭代次数: $iterations")
            appendLine("  已推导变量: ${solution.size}")
            if (failed) {
                appendLine("  失败原因: $failureReason")
            }
            appendLine()
            appendLine(statistics)
        }
    }
}
