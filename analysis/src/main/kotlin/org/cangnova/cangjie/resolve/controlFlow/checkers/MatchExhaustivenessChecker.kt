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

package org.cangnova.cangjie.resolve.controlFlow.checkers

import org.cangnova.cangjie.builtins.CangJieBuiltIns
import org.cangnova.cangjie.cfg.pseudocodeTraverser.TraversalOrder
import org.cangnova.cangjie.cfg.pseudocodeTraverser.traverse
import org.cangnova.cangjie.diagnostics.MatchMissingCase
import org.cangnova.cangjie.diagnostics.infos.errors.*
import org.cangnova.cangjie.diagnostics.infos.warnings.IMPLICIT_CAST_TO_ANY
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.resolve.binding.BindingContext.Companion.EXPECTED_EXPRESSION_TYPE
import org.cangnova.cangjie.resolve.binding.BindingContext.Companion.IMPLICIT_EXHAUSTIVE_MATCH
import org.cangnova.cangjie.resolve.binding.isUsedAsExpression
import org.cangnova.cangjie.resolve.binding.isUsedAsResultOfLambda
import org.cangnova.cangjie.resolve.controlFlow.ControlFlowAnalysisContext
import org.cangnova.cangjie.resolve.controlFlow.pseudocode.instructions.eval.InstructionWithValue
import org.cangnova.cangjie.resolve.controlFlow.pseudocode.instructions.eval.MagicInstruction
import org.cangnova.cangjie.resolve.controlFlow.pseudocode.instructions.eval.MagicKind
import org.cangnova.cangjie.resolve.controlFlow.pseudocode.instructions.eval.MergeInstruction
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.TypeUtils.DONT_CARE
import org.cangnova.cangjie.types.expressions.match.PatternAnalyzer
import org.cangnova.cangjie.types.expressions.match.checkExhaustive
import org.cangnova.cangjie.types.isBoolean

/**
 * Match 表达式穷尽性检查器
 *
 * 负责检查仓颉代码中 match 表达式的完整性和正确性。
 *
 * ## 主要检查项
 *
 * 1. **穷尽性检查**: 验证 match 表达式是否覆盖了所有可能的情况
 * 2. **隐式类型转换警告**: 当 match 表达式结果被隐式转换为 Any 时发出警告
 * 3. **缺失 else 分支**: 检测需要但缺失的 else 分支
 * 4. **模式匹配完整性**: 检测枚举、密封类、元组等类型的覆盖完整性
 *
 * ## 仓颉语言特性支持
 *
 * - **枚举类型**: 检查所有枚举成员是否都被覆盖
 * - **密封类**: 检查所有子类型是否都被覆盖
 * - **元组类型**: 检查元组的所有可能组合
 * - **布尔类型**: 检查 true/false 是否都被覆盖
 * - **常量模式**: 检查常量值的覆盖
 *
 * ## 使用示例
 *
 * ```kotlin
 * val checker = MatchExhaustivenessChecker()
 * checker.check(context)
 * ```
 *
 * @see ControlFlowAnalysisContext
 */
class MatchExhaustivenessChecker {

    /**
     * 执行 Match 表达式穷尽性检查
     *
     * @param context 控制流分析上下文
     */
    fun check(context: ControlFlowAnalysisContext) {
        checkExhaustiveness(context)
        checkImplicitExhaustiveMatch(context)
    }

    // ==================== 穷尽性检查 ====================

    /**
     * 检查 match 表达式的穷尽性
     *
     * 遍历所有 match 表达式，验证：
     * - 是否覆盖了所有可能的情况
     * - 是否有缺失的分支
     * - 是否需要 else 分支
     */
    private fun checkExhaustiveness(context: ControlFlowAnalysisContext) {
        context.pseudocode.traverse(TraversalOrder.FORWARD) { instruction ->
            val value = (instruction as? InstructionWithValue)?.outputValue
            for (element in instruction.owner.getValueElements(value)) {
                if (element !is CjMatchExpression) continue

                val bindingContext = context.bindingContext
                val usedAsExpression = element.isUsedAsExpression(bindingContext)

                // 检查隐式类型转换
                if (usedAsExpression) {
                    checkImplicitCastOnConditionalExpression(context, element)
                }

                val elseEntry = element.entries.find { it.isElse }
                val subjectExpression = element.subjectExpression

                if (subjectExpression != null) {
                    // 有 else 分支则无需检查穷尽性
                    if (elseEntry != null) return@traverse

                    // 模式匹配的穷尽性检查
                    val patterns = element.checkExhaustive(bindingContext) ?: return@traverse
                    context.trace.report(NO_ELSE_IN_MATCH_BY_PATTERN.on(element, patterns))
                } else {
                    // 非模式匹配（类似 when）的穷尽性检查
                    checkNonPatternMatchExhaustiveness(context, element, elseEntry)
                }
            }
        }
    }

    /**
     * 检查非模式匹配的穷尽性
     */
    private fun checkNonPatternMatchExhaustiveness(
        context: ControlFlowAnalysisContext,
        element: CjMatchExpression,
        elseEntry: CjMatchEntry?
    ) {
        // TODO: 实现 getMissingCases 检查
        val missingCases = emptyList<MatchMissingCase>()

        if (missingCases.isNotEmpty()) {
            if (elseEntry != null) return
            if (element.entries.any { it.conditions.first() is CjBindingPattern }) {
                return
            }
            context.trace.report(NO_ELSE_IN_MATCH.on(element, missingCases))
            missingCases.firstOrNull { it is MatchMissingCase.ConditionTypeIsExpect }?.let {
                require(it is MatchMissingCase.ConditionTypeIsExpect)
                context.trace.report(EXPECT_TYPE_IN_MATCH_WITHOUT_ELSE.on(element, it.typeOfDeclaration))
            }
        }
    }

    // ==================== 隐式穷尽性标记 ====================

    /**
     * 检查并标记隐式穷尽的 match 表达式
     *
     * 当 match 表达式的所有分支都已初始化相同的变量时，
     * 即使没有 else 分支，也可以认为是隐式穷尽的。
     */
    private fun checkImplicitExhaustiveMatch(context: ControlFlowAnalysisContext) {
        val initializers = context.variablesData.variableInitializers

        context.pseudocode.traverse(TraversalOrder.FORWARD, initializers) { instruction, _, _ ->
            if (instruction !is MagicInstruction) return@traverse
            if (instruction.kind !== MagicKind.EXHAUSTIVE_MATCH_ELSE) return@traverse

            val next = instruction.next
            if (next !is MergeInstruction) return@traverse

            val mergeInfo = initializers[next]?.incoming ?: return@traverse
            val magicInfo = initializers[instruction]?.outgoing ?: return@traverse

            if (next.element is CjMatchExpression &&
                magicInfo.checkDefiniteInitializationInMatch(mergeInfo)) {
                context.trace.record(IMPLICIT_EXHAUSTIVE_MATCH, next.element)
            }
        }
    }

    // ==================== 隐式类型转换检查 ====================

    /**
     * 检查条件表达式的隐式类型转换
     *
     * 当 match 表达式的结果类型是 Any，但各分支类型不同时，
     * 发出隐式转换警告。
     */
    private fun checkImplicitCastOnConditionalExpression(
        context: ControlFlowAnalysisContext,
        expression: CjExpression
    ) {
        val branchExpressions = collectResultingExpressionsOfConditionalExpression(expression)

        val expectedExpressionType = context.trace[EXPECTED_EXPRESSION_TYPE, expression]
        if (expectedExpressionType != null && expectedExpressionType !== DONT_CARE) return

        val expressionType = context.trace.getType(expression) ?: return
        if (!CangJieBuiltIns.isAny(expressionType)) return

        val isUsedAsResultOfLambda = expression.isUsedAsResultOfLambda(context.bindingContext)

        // 检查是否所有分支都不是 Any 类型
        for (branchExpression in branchExpressions) {
            val branchType = context.trace.getType(branchExpression) ?: return
            if (CangJieBuiltIns.isAny(branchType) ||
                isUsedAsResultOfLambda && CangJieBuiltIns.isUnit(branchType)) {
                return
            }
        }

        // 报告隐式类型转换警告
        for (branchExpression in branchExpressions) {
            val branchType = context.trace.getType(branchExpression) ?: continue
            if (CangJieBuiltIns.isNothing(branchType)) continue

            context.trace.report(
                IMPLICIT_CAST_TO_ANY.on(
                    getResultingExpression(branchExpression),
                    branchType,
                    expressionType
                )
            )
        }
    }

    // ==================== 代数类型检查 ====================

    /**
     * 代数类型种类
     *
     * 用于标识 match 表达式匹配的代数类型。
     */
    private enum class AlgebraicTypeKind(val displayName: String) {
        Constant("constant"),
        Sealed("sealed class/interface"),
        Enum("enum"),
        Tuple("Tuple"),
        Boolean("Boolean")
    }

    /**
     * 检查穷尽性语句（用于非表达式 match）
     *
     * 检查 match 语句是否覆盖了所有代数类型的情况。
     */
    fun checkExhaustiveMatchStatement(
        context: ControlFlowAnalysisContext,
        subjectType: CangJieType?,
        element: CjMatchExpression,
        missingCases: List<MatchMissingCase>
    ) {
        if (missingCases.isEmpty()) return

        val kind = when {
            missingCases.all { it is MatchMissingCase.OtherCheckIsMissing } -> AlgebraicTypeKind.Constant
            PatternAnalyzer.getClassDescriptorOfTypeIfTuple(subjectType) != null -> AlgebraicTypeKind.Tuple
            PatternAnalyzer.getClassDescriptorOfTypeIfSealed(subjectType) != null -> AlgebraicTypeKind.Sealed
            PatternAnalyzer.getClassDescriptorOfTypeIfEnum(subjectType) != null -> AlgebraicTypeKind.Enum
            subjectType?.isBoolean == true -> AlgebraicTypeKind.Boolean
            else -> null
        }

        if (kind != null) {
            context.trace.report(NO_ELSE_IN_MATCH.on(element, missingCases))
        }
    }

    // ==================== 辅助方法 ====================

    companion object {
        /**
         * 收集条件表达式的所有结果表达式
         *
         * 递归收集 if/match 表达式的所有分支结果。
         */
        private fun collectResultingExpressionsOfConditionalExpression(
            expression: CjExpression
        ): List<CjExpression> {
            val leafBranches = ArrayList<CjExpression>()
            collectResultingExpressionsOfConditionalExpressionRec(expression, leafBranches)
            return leafBranches
        }

        /**
         * 递归收集结果表达式
         */
        private fun collectResultingExpressionsOfConditionalExpressionRec(
            expression: CjExpression?,
            resultingExpressions: MutableList<CjExpression>
        ) {
            when (expression) {
                is CjIfExpression -> {
                    collectResultingExpressionsOfConditionalExpressionRec(
                        expression.then, resultingExpressions
                    )
                    collectResultingExpressionsOfConditionalExpressionRec(
                        expression.`else`, resultingExpressions
                    )
                }
                is CjMatchExpression -> {
                    for (whenEntry in expression.entries) {
                        collectResultingExpressionsOfConditionalExpressionRec(
                            whenEntry.expression, resultingExpressions
                        )
                    }
                }
                is CjExpression -> {
                    val resultingExpression = getResultingExpression(expression)
                    if (resultingExpression is CjIfExpression ||
                        resultingExpression is CjMatchExpression) {
                        collectResultingExpressionsOfConditionalExpressionRec(
                            resultingExpression, resultingExpressions
                        )
                    } else {
                        resultingExpressions.add(resultingExpression)
                    }
                }
            }
        }

        /**
         * 获取表达式的最终结果表达式
         *
         * 剥离括号、标签等包装，获取真正的表达式。
         */
        private fun getResultingExpression(expression: CjExpression): CjExpression {
            var finger = expression
            while (true) {
                var deparenthesized = CjPsiUtil.deparenthesize(finger)
                deparenthesized = CjPsiUtil.getExpressionOrLastStatementInBlock(deparenthesized)
                if (deparenthesized == null || deparenthesized === finger) break
                finger = deparenthesized
            }
            return finger
        }
    }
}
