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

import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.expressions.match.Matrix
import org.cangnova.cangjie.types.expressions.match.Pattern
import org.cangnova.cangjie.types.expressions.match.PatternKind

/**
 * 穷举性检查器接口
 *
 * 定义了所有穷举性检查算法必须实现的统一接口。
 * 混合模式检查器通过此接口调用不同的专门化算法。
 *
 * ## 实现指南
 *
 * 实现新的检查器时需要：
 *
 * 1. 实现 [isApplicable] 判断适用条件
 * 2. 实现 [check] 执行实际检查
 * 3. 设置正确的 [source] 标识
 * 4. 可选：覆盖 [priority] 调整优先级
 *
 * ## 示例实现
 *
 * ```kotlin
 * class MyChecker : ExhaustivenessChecker {
 *     override val source = CheckSource.MY_ALGORITHM
 *     override val priority = 25
 *
 *     override fun isApplicable(type: CangJieType, patterns: List<Pattern>): Boolean {
 *         return type.isMySpecialType && patterns.all { it.isSimple }
 *     }
 *
 *     override fun check(matrix: Matrix, type: CangJieType, crateRoot: CjFile?): ExhaustivenessResult {
 *         // 实现检查逻辑
 *     }
 * }
 * ```
 *
 * @see HybridDispatcher
 * @see ExhaustivenessResult
 */
interface ExhaustivenessChecker {

    /**
     * 执行穷举性检查
     *
     * 检查给定的模式矩阵是否穷举覆盖了指定类型的所有可能值。
     *
     * @param matrix 模式矩阵，每行代表一个 match 分支的模式列表
     * @param type 被匹配表达式的类型
     * @param crateRoot 当前源文件（用于判断 non-exhaustive 枚举的可见性）
     * @return 穷举性检查结果
     *
     * ## 契约
     *
     * - 如果返回 [ExhaustivenessResult.Exhaustive]，保证所有可能的值都被覆盖
     * - 如果返回 [ExhaustivenessResult.NonExhaustive]，应尽可能提供缺失模式
     * - 如果无法确定，应返回 [ExhaustivenessResult.Skipped] 让其他检查器尝试
     */
    fun check(
        matrix: Matrix,
        type: CangJieType,
        crateRoot: CjFile?
    ): ExhaustivenessResult

    /**
     * 判断此检查器是否适用于给定的类型和模式
     *
     * 分派器在选择检查器时首先调用此方法。
     * 只有返回 true 的检查器才会被用于实际检查。
     *
     * @param type 被匹配的类型
     * @param patterns 模式列表（通常是矩阵的第一列）
     * @return 如果此检查器适用于该类型和模式组合则返回 true
     *
     * ## 实现建议
     *
     * - 检查类型是否匹配（如布尔、枚举、整数等）
     * - 检查模式复杂度是否在算法能力范围内
     * - 尽量快速返回，避免复杂计算
     */
    fun isApplicable(type: CangJieType, patterns: List<Pattern>): Boolean

    /**
     * 检查来源标识
     *
     * 用于标识此检查器使用的算法，便于调试和性能分析。
     */
    val source: CheckSource

    /**
     * 检查器优先级
     *
     * 数字越小优先级越高。分派器按优先级顺序尝试检查器。
     *
     * ## 默认优先级
     *
     * - 0: TRIVIAL（琐碎情况）
     * - 10-35: 类型特化检查器
     * - 40-50: 结构递归检查器
     * - 100: MARANGET（通用算法，兜底）
     */
    val priority: Int
        get() = when (source) {
            CheckSource.TRIVIAL -> 0
            CheckSource.BOOLEAN_FLAG -> 10
            CheckSource.ENUM_BITVECTOR -> 20
            CheckSource.INTEGER_INTERVAL -> 30
            CheckSource.CHAR_INTERVAL -> 35
            CheckSource.TUPLE_COMPONENT -> 40
            CheckSource.NESTED_FLATTEN -> 50
            CheckSource.MARANGET -> 100
            else -> 1000
        }
}

/**
 * 模式复杂度信息
 *
 * 用于评估模式集合的复杂度，帮助分派器选择最优算法。
 * 复杂度分析是 O(n) 的，其中 n 是模式总数。
 *
 * ## 复杂度指标
 *
 * - **嵌套深度**: 影响递归深度和算法复杂度
 * - **特殊模式**: Or 模式、守卫等需要特殊处理
 * - **构造器数量**: 影响穷举空间大小
 *
 * @property maxNestingDepth 最大嵌套深度
 * @property hasOrPattern 是否包含 Or 模式（`|` 连接的模式）
 * @property hasGuard 是否包含守卫条件（`where` 子句）
 * @property hasSlicePattern 是否包含切片模式
 * @property hasRangePattern 是否包含范围模式
 * @property totalPatterns 总模式数量
 * @property distinctConstructors 不同构造器的数量
 */
data class PatternComplexity(
    val maxNestingDepth: Int,
    val hasOrPattern: Boolean,
    val hasGuard: Boolean,
    val hasSlicePattern: Boolean,
    val hasRangePattern: Boolean,
    val totalPatterns: Int,
    val distinctConstructors: Int
) {
    /**
     * 判断是否为简单模式
     *
     * 简单模式可以使用快速路径算法。
     *
     * @return 如果嵌套深度 ≤2 且不包含复杂模式则返回 true
     */
    val isSimple: Boolean
        get() = maxNestingDepth <= 2 &&
                !hasOrPattern &&
                !hasGuard &&
                !hasSlicePattern

    /**
     * 判断是否需要完整 Maranget 算法
     *
     * 某些复杂情况只能用完整算法处理。
     *
     * @return 如果需要使用 Maranget 算法则返回 true
     */
    val needsFullMaranget: Boolean
        get() = maxNestingDepth > 3 ||
                hasGuard ||
                hasSlicePattern ||
                (hasOrPattern && maxNestingDepth > 1)

    companion object {
        /**
         * 分析模式列表的复杂度
         *
         * 遍历所有模式并收集复杂度指标。
         *
         * @param patterns 要分析的模式列表
         * @return 复杂度信息
         */
        fun analyze(patterns: List<Pattern>): PatternComplexity {
            var maxDepth = 0
            var hasOr = false
            var hasGuard = false
            var hasSlice = false
            var hasRange = false
            val constructors = mutableSetOf<Any>()

            fun analyzePattern(pattern: Pattern, depth: Int) {
                maxDepth = maxOf(maxDepth, depth)
                pattern.constructors?.forEach { constructors.add(it) }

                // 递归分析子模式
                when (val kind = pattern.kind) {
                    is PatternKind.Enum -> {
                        kind.subPatterns.forEach { analyzePattern(it, depth + 1) }
                    }
                    is PatternKind.Tuple -> {
                        kind.subPatterns.forEach { analyzePattern(it, depth + 1) }
                    }
                    else -> {}
                }
            }

            patterns.forEach { analyzePattern(it, 1) }

            return PatternComplexity(
                maxNestingDepth = maxDepth,
                hasOrPattern = hasOr,
                hasGuard = hasGuard,
                hasSlicePattern = hasSlice,
                hasRangePattern = hasRange,
                totalPatterns = patterns.size,
                distinctConstructors = constructors.size
            )
        }
    }
}
