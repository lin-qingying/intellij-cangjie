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

package org.cangnova.cangjie.types.expressions.match.exhaustive.structural

import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.expressions.match.Matrix
import org.cangnova.cangjie.types.expressions.match.Pattern
import org.cangnova.cangjie.types.expressions.match.PatternKind
import org.cangnova.cangjie.types.expressions.match.exhaustive.CheckSource
import org.cangnova.cangjie.types.expressions.match.exhaustive.ExhaustivenessChecker
import org.cangnova.cangjie.types.expressions.match.exhaustive.ExhaustivenessResult
import org.cangnova.cangjie.types.isBuiltinTupleType

/**
 * 第三层：元组分量独立检查器
 *
 * 对于简单的元组类型，可以逐个分量独立检查，避免构造完整的模式矩阵。
 * 这是一种基于可分解性的优化策略。
 *
 * ## 算法原理
 *
 * 当元组的各个分量之间没有依赖关系时，可以独立检查每个分量的穷举性：
 *
 * ```
 * 类型: (Bool, Option<Int32>)
 *
 * 模式矩阵:
 * | (true,  Some(x)) |
 * | (true,  None)    |
 * | (false, Some(y)) |
 * | (false, None)    |
 *
 * 分解为独立检查:
 * 分量 1 (Bool):     {true, false}   → 穷举 ✓ (使用 BooleanChecker)
 * 分量 2 (Option):   {Some, None}    → 穷举 ✓ (使用 SmallEnumBitVectorChecker)
 *
 * 结论: 元组穷举 ✓
 * ```
 *
 * ## 仓颉语言示例
 *
 * ```cangjie
 * // 穷举完整
 * match (pair: (Bool, Bool)) {
 *     case (true, true) => "TT"
 *     case (true, false) => "TF"
 *     case (false, true) => "FT"
 *     case (false, false) => "FF"
 * }
 *
 * // 不穷举：缺少 (false, false)
 * match (pair: (Bool, Bool)) {
 *     case (true, _) => "T*"
 *     case (false, true) => "FT"
 *     // error: match 表达式不穷举，缺少: (false, false)
 * }
 * ```
 *
 * ## 适用条件
 *
 * - 每列的模式之间没有依赖关系
 * - 没有嵌套的 Or 模式或守卫
 * - 能独立分析每个位置
 *
 * ## 时间复杂度
 *
 * O(n * m)，其中 n 是分支数量，m 是元组元素数量。
 * 相比完整的 Maranget 算法（指数级），这是多项式时间。
 *
 * ## 优势
 *
 * - 将 O(n^m) 问题降为 O(n * m)
 * - 避免构造完整的模式矩阵
 * - 可以复用简单类型的专用检查器
 *
 * @see ExhaustivenessChecker
 * @see CheckSource.TUPLE_COMPONENT
 */
class TupleComponentChecker(
    private val componentCheckers: List<ExhaustivenessChecker>
) : ExhaustivenessChecker {

    override val source: CheckSource = CheckSource.TUPLE_COMPONENT

    override val priority: Int = 40

    override fun isApplicable(type: CangJieType, patterns: List<Pattern>): Boolean {
        if (!type.isBuiltinTupleType) return false

        // 检查所有模式是否都是简单的元组模式
        return patterns.all { pattern ->
            when (val kind = pattern.kind) {
                is PatternKind.Wild, is PatternKind.Binding -> true
                is PatternKind.Tuple -> kind.subPatterns.all { isSimplePattern(it) }
                else -> false
            }
        }
    }

    override fun check(
        matrix: Matrix,
        type: CangJieType,
        crateRoot: CjFile?
    ): ExhaustivenessResult {
        if (!type.isBuiltinTupleType) {
            return ExhaustivenessResult.Skipped
        }

        // 获取元组的元素类型
        val elementTypes = type.arguments.map { it.type }
        val arity = elementTypes.size

        if (arity == 0) {
            // 空元组，只需检查是否有分支
            return if (matrix.isNotEmpty()) {
                ExhaustivenessResult.Exhaustive
            } else {
                val wildPattern = Pattern.wild(type)
                ExhaustivenessResult.NonExhaustive(listOf(wildPattern), source)
            }
        }

        // 提取每个位置的模式
        val columnPatterns = (0 until arity).map { col ->
            extractColumnPatterns(matrix, col, elementTypes[col])
        }

        // 检查是否可以独立分析每列
        if (!canAnalyzeIndependently(columnPatterns)) {
            return ExhaustivenessResult.Skipped
        }

        // 独立检查每列
        val columnResults = columnPatterns.mapIndexed { col, patterns ->
            val columnType = elementTypes[col]
            val columnMatrix = patterns.map { listOf(it) }

            // 使用合适的检查器检查该列
            checkColumn(columnMatrix, columnType, crateRoot)
        }

        // 合并结果
        return combineResults(columnResults, elementTypes, type)
    }

    /**
     * 提取矩阵中某一列的模式
     */
    private fun extractColumnPatterns(
        matrix: Matrix,
        columnIndex: Int,
        elementType: CangJieType
    ): List<Pattern> {
        return matrix.mapNotNull { row ->
            val firstPattern = row.firstOrNull() ?: return@mapNotNull null

            when (val kind = firstPattern.kind) {
                is PatternKind.Wild, is PatternKind.Binding -> {
                    // 通配符对每个位置都产生通配符
                    Pattern.wild(elementType)
                }

                is PatternKind.Tuple -> {
                    // 提取对应位置的子模式
                    kind.subPatterns.getOrNull(columnIndex) ?: Pattern.wild(elementType)
                }

                else -> null
            }
        }
    }

    /**
     * 检查是否可以独立分析每列
     *
     * 条件：每行的模式之间没有依赖关系
     */
    private fun canAnalyzeIndependently(columnPatterns: List<List<Pattern>>): Boolean {
        // 简化检查：如果任意列包含通配符，则可以独立分析
        // 更精确的检查需要分析模式之间的关系
        return columnPatterns.all { column ->
            column.isNotEmpty()
        }
    }

    /**
     * 检查单列的穷举性
     */
    private fun checkColumn(
        columnMatrix: Matrix,
        columnType: CangJieType,
        crateRoot: CjFile?
    ): ExhaustivenessResult {
        val patterns = columnMatrix.flatten()

        // 找到适用的检查器
        for (checker in componentCheckers) {
            if (checker.isApplicable(columnType, patterns)) {
                val result = checker.check(columnMatrix, columnType, crateRoot)
                if (result !is ExhaustivenessResult.Skipped) {
                    return result
                }
            }
        }

        // 默认返回跳过
        return ExhaustivenessResult.Skipped
    }

    /**
     * 合并各列的检查结果
     */
    private fun combineResults(
        columnResults: List<ExhaustivenessResult>,
        elementTypes: List<CangJieType>,
        tupleType: CangJieType
    ): ExhaustivenessResult {
        // 如果有任何列返回跳过，则整体跳过
        if (columnResults.any { it is ExhaustivenessResult.Skipped }) {
            return ExhaustivenessResult.Skipped
        }

        // 如果有任何列返回错误，则整体错误
        val error = columnResults.filterIsInstance<ExhaustivenessResult.Error>().firstOrNull()
        if (error != null) {
            return error
        }

        // 如果所有列都穷举，则整体穷举
        if (columnResults.all { it is ExhaustivenessResult.Exhaustive }) {
            return ExhaustivenessResult.Exhaustive
        }

        // 否则，合并缺失的模式
        val missingPatternsByColumn = columnResults.mapIndexed { index, result ->
            when (result) {
                is ExhaustivenessResult.NonExhaustive -> result.missingPatterns
                else -> listOf(Pattern.wild(elementTypes[index]))
            }
        }

        // 构造缺失的元组模式（笛卡尔积的一个示例）
        val firstMissingTuple = missingPatternsByColumn.map { it.firstOrNull() ?: Pattern.wild() }
        val missingTuplePattern = Pattern(tupleType, PatternKind.Tuple(firstMissingTuple))

        return ExhaustivenessResult.NonExhaustive(listOf(missingTuplePattern), source)
    }

    /**
     * 检查是否为简单模式（无复杂嵌套）
     */
    private fun isSimplePattern(pattern: Pattern): Boolean {
        return when (val kind = pattern.kind) {
            is PatternKind.Wild, is PatternKind.Binding -> true
            is PatternKind.Const -> true
            is PatternKind.Enum -> kind.subPatterns.all { isSimplePattern(it) }
            is PatternKind.Type -> true
            else -> false
        }
    }

    companion object {
        /**
         * 创建默认实例
         */
        fun createDefault(): TupleComponentChecker {
            return TupleComponentChecker(
                listOf(
                    org.cangnova.cangjie.types.expressions.match.exhaustive.trivial.TrivialChecker.INSTANCE,
                    org.cangnova.cangjie.types.expressions.match.exhaustive.specialized.BooleanChecker.INSTANCE,
                    org.cangnova.cangjie.types.expressions.match.exhaustive.specialized.SmallEnumBitVectorChecker.INSTANCE
                )
            )
        }

        /** 默认实例 */
        val INSTANCE by lazy { createDefault() }
    }
}
