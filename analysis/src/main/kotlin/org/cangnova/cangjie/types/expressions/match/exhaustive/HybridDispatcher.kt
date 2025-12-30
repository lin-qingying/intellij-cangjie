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

import org.cangnova.cangjie.builtins.CangJieBuiltIns
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.expressions.match.Matrix
import org.cangnova.cangjie.types.expressions.match.Pattern
import org.cangnova.cangjie.types.expressions.match.exhaustive.inria.MarangetChecker
import org.cangnova.cangjie.types.expressions.match.exhaustive.specialized.*
import org.cangnova.cangjie.types.expressions.match.exhaustive.structural.*
import org.cangnova.cangjie.types.expressions.match.exhaustive.trivial.TrivialChecker
import org.cangnova.cangjie.types.isBoolean
import org.cangnova.cangjie.types.isBuiltinTupleType
import org.cangnova.cangjie.types.isEnum

/**
 * 智能分派器
 *
 * 根据类型和模式复杂度选择最优的穷举性检查算法。
 * 实现混合模式穷举性检查的核心决策逻辑。
 *
 * ## 决策流程
 *
 * ```
 * 输入: match语句
 *
 * → 检查是否空匹配或单通配符？
 *   是 → 快速返回 (TrivialChecker)
 *   否 ↓
 *
 * → 被匹配类型是什么？
 *   ├─ 布尔型 → 标记检查法 (BooleanChecker)
 *   ├─ 小枚举(≤64) → 位向量法 (SmallEnumBitVectorChecker)
 *   ├─ 整数型 → 区间分析法 (IntegerIntervalChecker)
 *   ├─ 字符型 → 区间分析法 (CharIntervalChecker)
 *   └─ 其他 ↓
 *
 * → 模式是否简单？
 *   ├─ 是：无嵌套、无守卫
 *   │   ├─ 元组 → 分量独立检查 (TupleComponentChecker)
 *   │   └─ 嵌套枚举 → 扁平化 + 位向量 (NestedFlattenChecker)
 *   └─ 否 ↓
 *
 * → 完整Maranget算法 (MarangetChecker)
 * ```
 *
 * ## 设计原则
 *
 * - **帕累托原则**：80% 的情况用简单算法处理
 * - **渐进式复杂度**：简单情况 O(n)，复杂情况 O(2^n)
 * - **专业化优于通用化**：每种类型用最适合的算法
 */
class HybridDispatcher private constructor(
    private val checkers: List<ExhaustivenessChecker>
) {

    /**
     * 执行穷举性检查
     *
     * @param matrix 模式矩阵
     * @param type 被匹配的类型
     * @param crateRoot 当前文件
     * @return 检查结果
     */
    fun check(
        matrix: Matrix,
        type: CangJieType,
        crateRoot: CjFile?
    ): ExhaustivenessResult {
        // 提取第一列的模式用于复杂度分析
        val patterns = matrix.mapNotNull { it.firstOrNull() }

        // 按优先级尝试每个检查器
        for (checker in checkers) {
            if (checker.isApplicable(type, patterns)) {
                val result = checker.check(matrix, type, crateRoot)

                // 如果检查器返回了确定的结果，直接返回
                if (result !is ExhaustivenessResult.Skipped) {
                    return result
                }
            }
        }

        // 所有检查器都跳过，返回错误
        return ExhaustivenessResult.Error("无法确定穷举性：没有适用的检查器")
    }

    /**
     * 选择最优检查器（用于调试和分析）
     *
     * @param type 被匹配的类型
     * @param patterns 模式列表
     * @return 选中的检查器，如果没有适用的返回 null
     */
    fun selectChecker(type: CangJieType, patterns: List<Pattern>): ExhaustivenessChecker? {
        return checkers.find { it.isApplicable(type, patterns) }
    }

    /**
     * 获取类型的推荐检查器
     *
     * @param type 类型
     * @return 推荐的检查器来源
     */
    fun getRecommendedSource(type: CangJieType): CheckSource {
        return when {
            type.isBoolean -> CheckSource.BOOLEAN_FLAG
            type.isEnum -> CheckSource.ENUM_BITVECTOR
            CangJieBuiltIns.isIntegral(type) -> CheckSource.INTEGER_INTERVAL
            CangJieBuiltIns.isRune(type) -> CheckSource.CHAR_INTERVAL
            type.isBuiltinTupleType -> CheckSource.TUPLE_COMPONENT
            else -> CheckSource.MARANGET
        }
    }

    companion object {
        /**
         * 创建默认配置的分派器
         */
        fun createDefault(): HybridDispatcher {
            return HybridDispatcher(
                listOf(
                    // 第一层：琐碎情况
                    TrivialChecker.INSTANCE,

                    // 第二层：类型特化
                    BooleanChecker.INSTANCE,
                    SmallEnumBitVectorChecker.INSTANCE,
                    IntegerIntervalChecker.INSTANCE,
                    CharIntervalChecker.INSTANCE,

                    // 第三层：结构递归
                    TupleComponentChecker.INSTANCE,
                    NestedFlattenChecker.INSTANCE,

                    // 第四层：完整算法
                    MarangetChecker.INSTANCE
                )
            )
        }

        /**
         * 创建仅使用 Maranget 算法的分派器（用于测试和对比）
         */
        fun createMarangetOnly(): HybridDispatcher {
            return HybridDispatcher(listOf(MarangetChecker.INSTANCE))
        }

        /**
         * 创建自定义配置的分派器
         */
        fun create(checkers: List<ExhaustivenessChecker>): HybridDispatcher {
            // 按优先级排序
            val sorted = checkers.sortedBy { it.priority }
            return HybridDispatcher(sorted)
        }

        /** 默认实例 */
        val DEFAULT: HybridDispatcher by lazy { createDefault() }
    }
}

/**
 * 分析报告
 *
 * 用于分析和调试穷举性检查过程
 */
data class DispatchAnalysis(
    /** 类型名称 */
    val typeName: String,

    /** 模式数量 */
    val patternCount: Int,

    /** 模式复杂度 */
    val complexity: PatternComplexity,

    /** 推荐的检查器 */
    val recommendedSource: CheckSource,

    /** 实际使用的检查器 */
    val actualSource: CheckSource?,

    /** 检查结果 */
    val result: ExhaustivenessResult
)

/**
 * 带分析的分派器
 *
 * 在执行检查的同时收集分析信息
 */
class AnalyzingDispatcher(private val delegate: HybridDispatcher) {

    /**
     * 执行检查并返回分析报告
     */
    fun checkWithAnalysis(
        matrix: Matrix,
        type: CangJieType,
        crateRoot: CjFile?
    ): DispatchAnalysis {
        val patterns = matrix.mapNotNull { it.firstOrNull() }
        val complexity = PatternComplexity.analyze(patterns)
        val recommendedSource = delegate.getRecommendedSource(type)

        val result = delegate.check(matrix, type, crateRoot)
        val actualSource = when (result) {
            is ExhaustivenessResult.NonExhaustive -> result.source
            is ExhaustivenessResult.Exhaustive -> delegate.selectChecker(type, patterns)?.source
            else -> null
        }

        return DispatchAnalysis(
            typeName = type.toString(),
            patternCount = patterns.size,
            complexity = complexity,
            recommendedSource = recommendedSource,
            actualSource = actualSource,
            result = result
        )
    }

    companion object {
        val DEFAULT by lazy { AnalyzingDispatcher(HybridDispatcher.DEFAULT) }
    }
}
