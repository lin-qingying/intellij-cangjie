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
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.cangjie.types.expressions.match

import org.cangnova.cangjie.psi.CjCasePatternElement
import org.cangnova.cangjie.psi.CjExpression
import org.cangnova.cangjie.psi.CjMatchEntry
import org.cangnova.cangjie.psi.CjMatchExpression
import org.cangnova.cangjie.resolve.binding.BindingContext
import org.cangnova.cangjie.types.expressions.match.exhaustive.ExhaustivenessAnalyzer
import org.cangnova.cangjie.types.expressions.match.exhaustive.ExhaustivenessResult

/**
 * 模式矩阵类型别名
 */
typealias Matrix = List<List<Pattern>>

/**
 * 计算模式矩阵
 *
 * 将 match 表达式的所有分支转换为模式矩阵，每一行对应一个 case 分支的模式。
 *
 * @param context 绑定上下文
 * @return 模式矩阵
 */
fun List<CjMatchEntry>.calculateMatrix(context: BindingContext): Matrix =
    flatMap { arm -> arm.conditions.map { listOf(getPattern(context, it)) } }

/**
 * 将单个模式元素转换为单行模式矩阵
 *
 * @param context 绑定上下文
 * @return 单行模式矩阵
 */
fun CjCasePatternElement.calculateMatrix(context: BindingContext): Matrix = listOf(
    listOf(getPattern(context, this))
)

/**
 * 从绑定上下文中获取模式
 *
 * @param context 绑定上下文
 * @param element 模式元素
 * @return 模式对象，如果未找到则返回 Pattern.Error
 */
fun getPattern(context: BindingContext, element: CjCasePatternElement): Pattern {
    return context[BindingContext.PATTERN, element] ?: Pattern.Error
}

/**
 * 执行匹配表达式的穷尽性检查
 *
 * 该函数检查给定的 match 表达式是否覆盖了所有可能的情况。
 *
 * @param match 匹配表达式对象
 * @param context 绑定上下文
 * @return 如果不穷尽，返回缺失的模式列表；如果穷尽，返回 null
 * @see ExhaustivenessAnalyzer.checkMatch
 */
fun doCheckExhaustive(match: CjMatchExpression, context: BindingContext): List<Pattern>? {
    return ExhaustivenessAnalyzer.getMissingPatterns(match, context)
}

/**
 * 执行匹配表达式的穷尽性检查（返回完整结果）
 *
 * @param match 匹配表达式对象
 * @param context 绑定上下文
 * @return 穷尽性检查结果
 * @see ExhaustivenessAnalyzer.checkMatch
 */
fun doCheckExhaustiveNew(match: CjMatchExpression, context: BindingContext): ExhaustivenessResult {
    return ExhaustivenessAnalyzer.checkMatch(match, context)
}

/**
 * 检查单个模式的穷尽性
 *
 * 用于 for-in 表达式、let-in 表达式等场景，检查模式是否为 irrefutable。
 *
 * @param expression 被匹配的表达式
 * @param context 绑定上下文
 * @return 如果不穷尽，返回缺失的模式列表；如果穷尽，返回 null
 * @see ExhaustivenessAnalyzer.checkPattern
 */
fun CjCasePatternElement.getExhaustive(expression: CjExpression?, context: BindingContext): List<Pattern>? {
    return when (val result = ExhaustivenessAnalyzer.checkPattern(this, expression, context)) {
        is ExhaustivenessResult.NonExhaustive -> result.missingPatterns
        else -> null
    }
}
