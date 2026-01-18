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

import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintStorage
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.model.TypeVariableMarker

/**
 * 迭代推断上下文
 *
 * 管理迭代类型推断过程中的全局状态，包括：
 * - 迭代轮次计数
 * - 参数合成状态
 * - 部分解（已推导的类型变量）
 * - 约束系统
 * - 收敛检测
 *
 * ## 工作流程
 * ```
 * 初始化 → 迭代轮次1 → 迭代轮次2 → ... → 收敛/失败
 *   ↓         ↓            ↓                ↓
 * 参数合成  约束收集    约束求解         最终结果
 * ```
 *
 * ## 收敛条件
 * 1. 所有类型变量都已推导出具体类型
 * 2. 或：连续两轮没有新的类型变量被推导
 * 3. 或：达到最大迭代次数
 *
 * ## 示例
 * ```kotlin
 * val context = IterativeInferenceContext(
 *     constraintSystem = system,
 *     typeVariables = listOf(T, U),
 *     arguments = listOf(arg0State, arg1State)
 * )
 *
 * while (context.shouldContinue()) {
 *     context.startIteration()
 *     // ... 合成、约束收集、求解
 *     context.endIteration(newSolution)
 * }
 *
 * val result = context.getFinalSolution()
 * ```
 */
class IterativeInferenceContext(
    /**
     * 约束系统构建器
     */
    val constraintSystemBuilder: ConstraintSystemBuilder,

    /**
     * 待推导的类型变量列表
     */
    val typeVariables: List<TypeVariableMarker>,

    /**
     * 参数合成状态列表
     */
    val arguments: List<ArgumentSynthesisState>,

    /**
     * 最大迭代次数（防止无限循环）
     */
    val maxIterations: Int = 10
) {
    /**
     * 当前迭代轮次（从1开始）
     */
    var currentIteration: Int = 0
        private set

    /**
     * 部分解：已推导的类型变量到具体类型的映射
     *
     * 在每轮迭代中逐步完善，直到所有类型变量都被推导
     */
    private val partialSolution: MutableMap<TypeVariableMarker, CangJieType> = mutableMapOf()

    /**
     * 上一轮已解决的类型变量数量
     *
     * 用于检测收敛：如果连续两轮没有新的类型变量被推导，则认为收敛
     */
    private var previousSolvedCount: Int = 0

    /**
     * 约束系统的只读存储（用于查询）
     */
    val constraintStorage: ConstraintStorage
        get() = constraintSystemBuilder.currentStorage()

    /**
     * 是否已收敛
     *
     * 收敛条件：
     * 1. 所有类型变量都已推导
     * 2. 或：连续两轮没有新的类型变量被推导
     */
    var hasConverged: Boolean = false
        private set

    /**
     * 是否失败
     *
     * 失败条件：
     * 1. 约束系统有矛盾
     * 2. 或：达到最大迭代次数
     */
    var hasFailed: Boolean = false
        private set

    /**
     * 失败原因
     */
    var failureReason: String? = null
        private set

    /**
     * 开始新的迭代轮次
     */
    fun startIteration() {
        currentIteration++

        if (currentIteration > maxIterations) {
            hasFailed = true
            failureReason = "达到最大迭代次数 $maxIterations"
            return
        }

        // 重置参数状态（保留 analyzed 标记，但允许重新分析）
        arguments.forEach { arg ->
            arg.constraintsCollected = false
        }
    }

    /**
     * 结束当前迭代轮次
     *
     * @param newSolution 本轮推导出的新类型变量解
     */
    fun endIteration(newSolution: Map<TypeVariableMarker, CangJieType>) {
        // 合并新解到部分解
        partialSolution.putAll(newSolution)

        // 检查收敛
        val currentSolvedCount = partialSolution.size
        if (currentSolvedCount == typeVariables.size) {
            // 所有类型变量都已推导
            hasConverged = true
        } else if (currentSolvedCount == previousSolvedCount) {
            // 连续两轮没有新的类型变量被推导
            hasConverged = true
        }

        previousSolvedCount = currentSolvedCount

        // 检查约束系统是否有矛盾
        if (constraintSystemBuilder.hasContradiction) {
            hasFailed = true
            failureReason = "约束系统有矛盾"
        }
    }

    /**
     * 检查是否应该继续迭代
     *
     * @return true 如果应该继续迭代
     */
    fun shouldContinue(): Boolean {
        return !hasConverged && !hasFailed
    }

    /**
     * 获取当前的部分解（只读）
     */
    fun getPartialSolution(): Map<TypeVariableMarker, CangJieType> {
        return partialSolution.toMap()
    }

    /**
     * 获取最终解
     *
     * 只有在收敛或失败后才能调用
     *
     * @return 类型变量到具体类型的完整映射，如果失败则返回部分解
     */
    fun getFinalSolution(): Map<TypeVariableMarker, CangJieType> {
        require(hasConverged || hasFailed) {
            "只有在收敛或失败后才能获取最终解"
        }
        return partialSolution.toMap()
    }

    /**
     * 获取未解决的类型变量
     */
    fun getUnresolvedTypeVariables(): List<TypeVariableMarker> {
        return typeVariables.filter { it !in partialSolution }
    }

    /**
     * 添加部分解
     *
     * 用于在迭代过程中逐步添加推导出的类型
     *
     * @param variable 类型变量
     * @param type 推导出的具体类型
     */
    fun addPartialSolution(variable: TypeVariableMarker, type: CangJieType) {
        partialSolution[variable] = type
    }

    /**
     * 检查类型变量是否已解决
     */
    fun isResolved(variable: TypeVariableMarker): Boolean {
        return variable in partialSolution
    }

    /**
     * 获取统计信息（用于调试）
     */
    fun getStatistics(): String {
        return buildString {
            appendLine("迭代统计:")
            appendLine("  当前轮次: $currentIteration / $maxIterations")
            appendLine("  已解决变量: ${partialSolution.size} / ${typeVariables.size}")
            appendLine("  已分析参数: ${arguments.count { it.analyzed }} / ${arguments.size}")
            appendLine("  收敛状态: $hasConverged")
            appendLine("  失败状态: $hasFailed")
            failureReason?.let { appendLine("  失败原因: $it") }
        }
    }

    override fun toString(): String {
        return "IterativeInferenceContext(iteration=$currentIteration, " +
                "solved=${partialSolution.size}/${typeVariables.size}, " +
                "converged=$hasConverged, failed=$hasFailed)"
    }
}
