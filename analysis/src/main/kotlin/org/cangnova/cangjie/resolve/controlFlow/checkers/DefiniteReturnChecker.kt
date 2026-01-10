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
import org.cangnova.cangjie.diagnostics.infos.errors.*
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.resolve.controlFlow.ControlFlowAnalysisContext
import org.cangnova.cangjie.resolve.controlFlow.UnreachableCode
import org.cangnova.cangjie.resolve.controlFlow.pseudocode.instructions.InstructionVisitor
import org.cangnova.cangjie.resolve.controlFlow.pseudocode.instructions.Instruction
import org.cangnova.cangjie.resolve.controlFlow.pseudocode.instructions.CjElementInstruction
import org.cangnova.cangjie.resolve.controlFlow.pseudocode.instructions.jumps.*
import org.cangnova.cangjie.resolve.controlFlow.pseudocode.instructions.special.MarkInstruction
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.TypeUtils.noExpectedType

/**
 * 确定性返回检查器
 *
 * 负责检查仓颉函数是否在所有控制流路径上都有确定的返回值。
 *
 * ## 主要检查项
 *
 * 1. **缺失返回语句**: 检测块体函数是否缺少返回语句
 * 2. **表达式体返回**: 检测表达式体函数中的非法 return 语句
 * 3. **内联 lambda 返回**: 检测内联 lambda 中的返回语句影响
 *
 * ## 仓颉语言特性支持
 *
 * - **块体函数**: `func foo(): Int { ... }` 必须显式返回
 * - **表达式体函数**: `func foo() => expr` 不应包含 return 语句
 * - **Unit 返回类型**: 返回 Unit 的函数可以省略 return
 *
 * ## 检测原理
 *
 * 通过分析伪代码的出口指令，收集所有可能的返回表达式：
 * - 显式的 return 语句
 * - 隐式的表达式返回（最后一个表达式）
 * - 块体结束（需要判断是否缺少返回）
 *
 * ## 使用示例
 *
 * ```kotlin
 * val checker = DefiniteReturnChecker()
 * checker.check(context, expectedReturnType, unreachableCode)
 * ```
 *
 * @see ControlFlowAnalysisContext
 */
class DefiniteReturnChecker {

    /**
     * 执行确定性返回检查
     *
     * @param context 控制流分析上下文
     * @param expectedReturnType 期望的返回类型
     * @param unreachableCode 不可达代码信息（用于排除不可达的返回语句）
     */
    fun check(
        context: ControlFlowAnalysisContext,
        expectedReturnType: CangJieType,
        unreachableCode: UnreachableCode
    ) {
        val subroutine = context.subroutine as? CjDeclarationWithBody
            ?: throw AssertionError("checkDefiniteReturn is called for ${context.subroutine.text} which is not CjDeclarationWithBody")

        if (!subroutine.hasBody()) return

        val returnInfo = collectReturnExpressions(context)
        val blockBody = subroutine.hasBlockBody()

        var noReturnError = false
        for (returnedExpression in returnInfo.returnedExpressions) {
            val result = checkReturnedExpression(
                context, returnedExpression, blockBody, expectedReturnType, unreachableCode
            )
            if (result == ReturnCheckResult.MISSING_RETURN) {
                noReturnError = true
            }
        }

        if (noReturnError) {
            reportMissingReturn(context, subroutine, returnInfo.hasReturnsInInlinedLambda)
        }
    }

    // ==================== 返回表达式收集 ====================

    /**
     * 返回表达式信息
     *
     * @property returnedExpressions 所有返回表达式的集合
     * @property hasReturnsInInlinedLambda 是否在内联 lambda 中有返回语句
     */
    private data class ReturnedExpressionsInfo(
        val returnedExpressions: Collection<CjElement>,
        val hasReturnsInInlinedLambda: Boolean
    )

    /**
     * 收集函数中的所有返回表达式
     *
     * 遍历伪代码的出口指令，收集所有返回表达式，包括：
     * - 显式的 return 语句
     * - 隐式的表达式返回
     *
     * @param context 控制流分析上下文
     * @return 返回表达式信息
     */
    private fun collectReturnExpressions(context: ControlFlowAnalysisContext): ReturnedExpressionsInfo {
        val instructions = context.pseudocode.instructions.toHashSet()
        val exitInstruction = context.pseudocode.exitInstruction

        val returnedExpressions = arrayListOf<CjElement>()
        var hasReturnsInInlinedLambda = false

        for (previousInstruction in exitInstruction.previousInstructions) {
            previousInstruction.accept(object : InstructionVisitor() {
                override fun visitReturnValue(instruction: ReturnValueInstruction) {
                    if (instructions.contains(instruction)) {
                        returnedExpressions.add(instruction.element)
                    }
                    if (instruction.owner.isInlined) {
                        hasReturnsInInlinedLambda = true
                    }
                }

                override fun visitReturnNoValue(instruction: ReturnNoValueInstruction) {
                    if (instructions.contains(instruction)) {
                        returnedExpressions.add(instruction.element)
                    }
                    if (instruction.owner.isInlined) {
                        hasReturnsInInlinedLambda = true
                    }
                }

                override fun visitUnconditionalJump(instruction: UnconditionalJumpInstruction) {
                    redirectToPrevInstructions(instruction)
                }

                override fun visitConditionalJump(instruction: ConditionalJumpInstruction) {
                    redirectToPrevInstructions(instruction)
                }

                override fun visitNondeterministicJump(instruction: NondeterministicJumpInstruction) {
                    redirectToPrevInstructions(instruction)
                }

                override fun visitMarkInstruction(instruction: MarkInstruction) {
                    redirectToPrevInstructions(instruction)
                }

                private fun redirectToPrevInstructions(instruction: Instruction) {
                    for (redirectInstruction in instruction.previousInstructions) {
                        redirectInstruction.accept(this)
                    }
                }

                override fun visitInstruction(instruction: Instruction) {
                    if (instruction is CjElementInstruction) {
                        // 对于空块体，会发出 read(Unit) 指令
                        // 对于 Unit 强制转换的块，最后一个表达式会在这里处理
                        returnedExpressions.add(instruction.element)
                    } else {
                        throw IllegalStateException("$instruction precedes the exit point")
                    }
                }
            })
        }

        return ReturnedExpressionsInfo(returnedExpressions, hasReturnsInInlinedLambda)
    }

    // ==================== 返回表达式检查 ====================

    /**
     * 返回检查结果
     */
    private enum class ReturnCheckResult {
        /** 检查通过 */
        OK,
        /** 缺少返回语句 */
        MISSING_RETURN,
        /** 表达式体中的非法 return */
        RETURN_IN_EXPRESSION_BODY
    }

    /**
     * 检查单个返回表达式
     *
     * @param context 控制流分析上下文
     * @param returnedExpression 返回表达式
     * @param blockBody 是否是块体函数
     * @param expectedReturnType 期望的返回类型
     * @param unreachableCode 不可达代码信息
     * @return 检查结果
     */
    private fun checkReturnedExpression(
        context: ControlFlowAnalysisContext,
        returnedExpression: CjElement,
        blockBody: Boolean,
        expectedReturnType: CangJieType,
        unreachableCode: UnreachableCode
    ): ReturnCheckResult {
        return returnedExpression.accept(object : CjVisitor<ReturnCheckResult, Unit?>() {
            override fun visitReturnExpression(expression: CjReturnExpression, data: Unit?): ReturnCheckResult {
                if (!blockBody) {
                    context.trace.report(RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY.on(expression))
                    return ReturnCheckResult.RETURN_IN_EXPRESSION_BODY
                }
                return ReturnCheckResult.OK
            }

            override fun visitExpression(expression: CjExpression, data: Unit?): ReturnCheckResult {
                return ReturnCheckResult.OK
            }

            override fun visitBlockExpression(expression: CjBlockExpression, data: Unit?): ReturnCheckResult {
                return visitCjElement(expression, data)
            }

            override fun visitCjElement(element: CjElement, data: Unit?): ReturnCheckResult {
                if (element !is CjExpression && element !is CjCasePatternElement) {
                    return ReturnCheckResult.OK
                }

                if (blockBody &&
                    !noExpectedType(expectedReturnType) &&
                    !CangJieBuiltIns.isUnit(expectedReturnType) &&
                    !unreachableCode.elements.contains(element)) {
                    return ReturnCheckResult.MISSING_RETURN
                }

                return ReturnCheckResult.OK
            }
        }, null) ?: ReturnCheckResult.OK
    }

    // ==================== 错误报告 ====================

    /**
     * 报告缺少返回语句的错误
     *
     * @param context 控制流分析上下文
     * @param function 函数声明
     * @param hasReturnsInInlinedLambda 是否在内联 lambda 中有返回
     */
    private fun reportMissingReturn(
        context: ControlFlowAnalysisContext,
        function: CjDeclarationWithBody,
        hasReturnsInInlinedLambda: Boolean
    ) {
        if (hasReturnsInInlinedLambda) {
            context.trace.report(NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY_MIGRATION.on(function))
        } else {
            context.trace.report(NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY.on(function))
        }
    }

    companion object {
        /**
         * 便捷方法：执行确定性返回检查
         *
         * @param context 控制流分析上下文
         * @param expectedReturnType 期望的返回类型
         * @param unreachableCode 不可达代码信息
         */
        fun check(
            context: ControlFlowAnalysisContext,
            expectedReturnType: CangJieType,
            unreachableCode: UnreachableCode
        ) {
            DefiniteReturnChecker().check(context, expectedReturnType, unreachableCode)
        }
    }
}
