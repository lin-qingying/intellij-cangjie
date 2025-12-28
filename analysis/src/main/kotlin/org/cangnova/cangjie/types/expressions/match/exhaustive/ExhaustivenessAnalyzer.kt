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
 */

package org.cangnova.cangjie.types.expressions.match.exhaustive

import org.cangnova.cangjie.psi.CjCasePatternElement
import org.cangnova.cangjie.psi.CjExpression
import org.cangnova.cangjie.psi.CjMatchExpression
import org.cangnova.cangjie.resolve.binding.BindingContext
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.ErrorUtils
import org.cangnova.cangjie.types.expressions.match.Matrix
import org.cangnova.cangjie.types.expressions.match.Pattern
import org.cangnova.cangjie.types.expressions.match.exhaustive.inria.isWellTyped

/**
 * 穷举性检查门面类
 *
 * 提供穷举性检查的统一入口，整合混合模式检查算法。
 * 这是外部代码应该使用的主要接口。
 *
 * ## 使用示例
 *
 * ```kotlin
 * // 检查 match 表达式
 * val result = ExhaustivenessChecker.checkMatch(matchExpr, context)
 * when (result) {
 *     is ExhaustivenessResult.Exhaustive -> { /* 穷举完整 */ }
 *     is ExhaustivenessResult.NonExhaustive -> {
 *         // 报告缺失的模式
 *         result.missingPatterns.forEach { pattern ->
 *             reportMissing(pattern.text(null))
 *         }
 *     }
 *     is ExhaustivenessResult.Error -> { /* 检查过程出错 */ }
 *     is ExhaustivenessResult.Skipped -> { /* 无法检查 */ }
 * }
 * ```
 *
 * ## 算法选择
 *
 * 默认使用混合模式检查，根据类型和模式复杂度自动选择最优算法：
 *
 * 1. **琐碎情况**：空匹配、单通配符等，O(1)
 * 2. **布尔类型**：标记法，O(n)
 * 3. **小枚举**：位向量法，O(n)
 * 4. **整数类型**：区间分析，O(n log n)
 * 5. **元组类型**：分量独立检查，O(n * m)
 * 6. **复杂情况**：Maranget 算法，O(2^n) with pruning
 */
object ExhaustivenessAnalyzer {

    /**
     * 默认分派器
     */
    private val dispatcher = HybridDispatcher.DEFAULT

    /**
     * 检查 match 表达式的穷举性
     *
     * @param match match 表达式
     * @param context 绑定上下文
     * @return 检查结果
     */
    fun checkMatch(match: CjMatchExpression, context: BindingContext): ExhaustivenessResult {
        // 获取被匹配表达式的类型
        val matchedExprType = match.subjectExpression?.let { context.getType(it) }
            ?: return ExhaustivenessResult.Skipped

        // 构建模式矩阵
        val matrix = try {
            match.entries.calculateMatrix(context)
        } catch (e: Exception) {
            return ExhaustivenessResult.Error("构建模式矩阵失败: ${e.message}")
        }

        // 检查矩阵类型一致性
        if (!matrix.isWellTyped()) {
            return ExhaustivenessResult.Error("模式矩阵类型不一致")
        }

        // 执行检查
        return dispatcher.check(matrix, matchedExprType, match.containingCjFile)
    }

    /**
     * 检查单个模式元素的穷举性
     *
     * 用于 let-in 表达式、for-in 表达式等场景
     *
     * @param pattern 模式元素
     * @param expression 被匹配的表达式
     * @param context 绑定上下文
     * @return 检查结果
     */
    fun checkPattern(
        pattern: CjCasePatternElement,
        expression: CjExpression?,
        context: BindingContext
    ): ExhaustivenessResult {
        // 获取表达式类型
        val type = expression?.let { context.getType(it) } ?: ErrorUtils.invalidType

        // 构建单行矩阵
        val matrix = try {
            pattern.calculateMatrix(context)
        } catch (e: Exception) {
            return ExhaustivenessResult.Error("构建模式矩阵失败: ${e.message}")
        }

        // 检查矩阵类型一致性
        if (!matrix.isWellTyped()) {
            return ExhaustivenessResult.Error("模式类型不一致")
        }

        // 执行检查
        return dispatcher.check(matrix, type, pattern.containingCjFile)
    }

    /**
     * 检查给定的模式矩阵是否穷举
     *
     * 底层 API，允许直接传入模式矩阵
     *
     * @param matrix 模式矩阵
     * @param type 被匹配的类型
     * @param context 上下文信息（可选）
     * @return 检查结果
     */
    fun checkMatrix(
        matrix: Matrix,
        type: CangJieType,
        context: org.cangnova.cangjie.psi.CjFile? = null
    ): ExhaustivenessResult {
        if (!matrix.isWellTyped()) {
            return ExhaustivenessResult.Error("模式矩阵类型不一致")
        }

        return dispatcher.check(matrix, type, context)
    }

    /**
     * 获取缺失的模式（兼容旧 API）
     *
     * @param match match 表达式
     * @param context 绑定上下文
     * @return 缺失的模式列表，如果穷举则返回 null
     */
    fun getMissingPatterns(match: CjMatchExpression, context: BindingContext): List<Pattern>? {
        return when (val result = checkMatch(match, context)) {
            is ExhaustivenessResult.NonExhaustive -> result.missingPatterns
            is ExhaustivenessResult.Exhaustive -> null
            else -> null
        }
    }

    /**
     * 获取缺失的模式文本（用于诊断消息）
     *
     * @param match match 表达式
     * @param context 绑定上下文
     * @return 缺失模式的文本列表，如果穷举则返回空列表
     */
    fun getMissingPatternTexts(match: CjMatchExpression, context: BindingContext): List<String> {
        return when (val result = checkMatch(match, context)) {
            is ExhaustivenessResult.NonExhaustive -> result.getMissingPatternTexts()
            else -> emptyList()
        }
    }

    /**
     * 带分析的检查（用于调试）
     */
    fun checkWithAnalysis(
        match: CjMatchExpression,
        context: BindingContext
    ): DispatchAnalysis? {
        val matchedExprType = match.subjectExpression?.let { context.getType(it) }
            ?: return null

        val matrix = try {
            match.entries.calculateMatrix(context)
        } catch (e: Exception) {
            return null
        }

        return AnalyzingDispatcher.DEFAULT.checkWithAnalysis(
            matrix, matchedExprType, match.containingCjFile
        )
    }
}

// 矩阵构建辅助函数
private fun List<org.cangnova.cangjie.psi.CjMatchEntry>.calculateMatrix(
    context: BindingContext
): Matrix = flatMap { arm ->
    arm.conditions.map { listOf(getPattern(context, it)) }
}

private fun CjCasePatternElement.calculateMatrix(context: BindingContext): Matrix = listOf(
    listOf(getPattern(context, this))
)

private fun getPattern(context: BindingContext, element: CjCasePatternElement): Pattern {
    return context[BindingContext.PATTERN, element] ?: Pattern.Error
}

// CjMatchExpression 扩展属性
private val CjMatchExpression.containingCjFile: org.cangnova.cangjie.psi.CjFile?
    get() = containingFile as? org.cangnova.cangjie.psi.CjFile

private val CjCasePatternElement.containingCjFile: org.cangnova.cangjie.psi.CjFile?
    get() = containingFile as? org.cangnova.cangjie.psi.CjFile
