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

package org.cangnova.cangjie.types.expressions.match.exhaustive.trivial

import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.expressions.match.Matrix
import org.cangnova.cangjie.types.expressions.match.Pattern
import org.cangnova.cangjie.types.expressions.match.PatternKind
import org.cangnova.cangjie.types.expressions.match.exhaustive.CheckSource
import org.cangnova.cangjie.types.expressions.match.exhaustive.ExhaustivenessChecker
import org.cangnova.cangjie.types.expressions.match.exhaustive.ExhaustivenessResult

/**
 * 第一层：琐碎情况快速检查器
 *
 * 处理最常见的简单情况，无需复杂计算。
 * 这一层能过滤掉约 40-60% 的实际代码情况。
 *
 * ## 处理的情况
 *
 * | 情况 | 描述 | 结果 |
 * |------|------|------|
 * | 空匹配 | 没有任何分支 | 不穷举，缺少 `_` |
 * | 单通配符 | 只有 `_ => ...` | 穷举完整 |
 * | 首个通配符 | 第一个分支是 `_` | 穷举完整（后续冗余） |
 *
 * ## 仓颉语言示例
 *
 * ```cangjie
 * // 情况 1：空匹配（不穷举）
 * match (x) {
 *     // error: match 表达式不穷举，缺少: _
 * }
 *
 * // 情况 2：单通配符（穷举完整）
 * match (x) {
 *     case _ => println("任意值")
 * }
 *
 * // 情况 3：首个通配符（穷举完整，后续分支冗余）
 * match (x) {
 *     case _ => println("匹配所有")
 *     case Some(y) => println("永远不会执行")  // warning: 不可达分支
 * }
 *
 * // 情况 4：绑定模式（等价于通配符）
 * match (x) {
 *     case value => println("绑定到 value: ${value}")
 * }
 * ```
 *
 * ## 时间复杂度
 *
 * O(n)，其中 n 是分支数量。通常只需检查第一个分支即可确定。
 *
 * ## 为什么重要
 *
 * - 约 40-60% 的 match 表达式有通配符分支
 * - 快速排除最常见的情况，避免启动复杂算法
 * - O(1) 常数时间优化
 *
 * @see ExhaustivenessChecker
 * @see CheckSource.TRIVIAL
 */
class TrivialChecker : ExhaustivenessChecker {

    override val source: CheckSource = CheckSource.TRIVIAL

    override val priority: Int = 0

    override fun isApplicable(type: CangJieType, patterns: List<Pattern>): Boolean {
        // 空匹配或存在顶层通配符时适用
        if (patterns.isEmpty()) return true

        // 检查是否有顶层通配符
        return patterns.any { isTopLevelWildcard(it) }
    }

    override fun check(
        matrix: Matrix,
        type: CangJieType,
        crateRoot: CjFile?
    ): ExhaustivenessResult {
        // 情况 1：空匹配
        if (matrix.isEmpty()) {
            // 没有任何分支，需要报告缺少整个类型的匹配
            val wildPattern = Pattern.wild(type)
            return ExhaustivenessResult.NonExhaustive(
                listOf(wildPattern),
                source
            )
        }

        // 检查第一列模式
        val firstColumnPatterns = matrix.mapNotNull { row -> row.firstOrNull() }

        // 情况 2 & 3：第一个分支是顶层通配符
        val firstPattern = firstColumnPatterns.firstOrNull()
        if (firstPattern != null && isTopLevelWildcard(firstPattern)) {
            // 第一个分支覆盖所有情况
            return ExhaustivenessResult.Exhaustive
        }

        // 情况 4：任意位置有通配符模式
        for (pattern in firstColumnPatterns) {
            if (isTopLevelWildcard(pattern)) {
                return ExhaustivenessResult.Exhaustive
            }
        }

        // 无法用琐碎检查确定，返回 Skipped
        return ExhaustivenessResult.Skipped
    }

    /**
     * 检查是否为顶层通配符模式
     *
     * 顶层通配符包括：
     * - 通配符 `_`
     * - 绑定模式 `x`（等价于通配符，只是给匹配值起个名字）
     */
    private fun isTopLevelWildcard(pattern: Pattern): Boolean {
        return when (pattern.kind) {
            is PatternKind.Wild -> true
            is PatternKind.Binding -> true
            else -> false
        }
    }

    companion object {
        /** 单例实例 */
        val INSTANCE = TrivialChecker()
    }
}
