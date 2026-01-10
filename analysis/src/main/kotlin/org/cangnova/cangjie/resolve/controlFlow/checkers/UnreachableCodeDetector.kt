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

import org.cangnova.cangjie.diagnostics.infos.warnings.UNREACHABLE_CODE
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.resolve.controlFlow.ControlFlowAnalysisContext
import org.cangnova.cangjie.resolve.controlFlow.UnreachableCode
import org.cangnova.cangjie.resolve.controlFlow.UnreachableCodeImpl
import org.cangnova.cangjie.resolve.controlFlow.pseudocode.instructions.CjElementInstruction
import org.cangnova.cangjie.resolve.controlFlow.pseudocode.instructions.eval.LoadUnitValueInstruction
import org.cangnova.cangjie.resolve.controlFlow.pseudocode.instructions.eval.MagicInstruction
import org.cangnova.cangjie.resolve.controlFlow.pseudocode.instructions.eval.MergeInstruction
import org.cangnova.cangjie.resolve.controlFlow.pseudocode.instructions.jumps.JumpInstruction

/**
 * 不可达代码检测器
 *
 * 负责检测仓颉代码中永远不会被执行的代码段，并报告相应的警告。
 *
 * ## 主要检查项
 *
 * 1. **return/throw 后的代码**: 在 return 或 throw 语句之后的代码
 * 2. **break/continue 后的代码**: 在循环控制语句之后的代码
 * 3. **条件分支中的死代码**: 由于条件永远为真/假导致的不可达代码
 * 4. **无限循环后的代码**: 在没有 break 的无限循环之后的代码
 *
 * ## 检测原理
 *
 * 通过遍历伪代码指令序列，标记每条指令的可达性状态：
 * - 从入口指令开始，沿着控制流路径标记可达指令
 * - 未被标记的指令即为不可达代码
 *
 * ## 使用示例
 *
 * ```kotlin
 * val detector = UnreachableCodeDetector()
 * val unreachableCode = detector.detect(context)
 * detector.report(context, unreachableCode)
 * ```
 *
 * @see ControlFlowAnalysisContext
 * @see UnreachableCode
 */
class UnreachableCodeDetector {

    /**
     * 检测并报告不可达代码
     *
     * @param context 控制流分析上下文
     * @return 不可达代码信息
     */
    fun detectAndReport(context: ControlFlowAnalysisContext): UnreachableCode {
        val unreachableCode = detect(context)
        report(context, unreachableCode)
        return unreachableCode
    }

    /**
     * 仅检测不可达代码（不报告）
     *
     * @param context 控制流分析上下文
     * @return 不可达代码信息
     */
    fun detect(context: ControlFlowAnalysisContext): UnreachableCode {
        return collectUnreachableCode(context)
    }

    /**
     * 报告不可达代码警告
     *
     * @param context 控制流分析上下文
     * @param unreachableCode 不可达代码信息
     */
    fun report(context: ControlFlowAnalysisContext, unreachableCode: UnreachableCode) {
        for (element in unreachableCode.elements) {
            context.trace.report(
                UNREACHABLE_CODE.on(
                    element,
                    unreachableCode.reachableElements,
                    unreachableCode.unreachableElements
                )
            )
        }
    }

    // ==================== 不可达代码收集 ====================

    /**
     * 收集不可达代码
     *
     * 遍历伪代码中的所有指令（包括死代码），识别不可达的代码元素。
     * 不可达代码是指永远不会被执行的代码，通常出现在：
     * - return、throw 语句之后
     * - break、continue 语句之后
     * - 条件永远为假的分支中
     *
     * @param context 控制流分析上下文
     * @return 包含可达元素和不可达元素的 UnreachableCode 对象
     */
    private fun collectUnreachableCode(context: ControlFlowAnalysisContext): UnreachableCode {
        val reachableElements = hashSetOf<CjElement>()
        val unreachableElements = hashSetOf<CjElement>()

        for (instruction in context.pseudocode.instructionsIncludingDeadCode) {
            // 跳过非元素指令和特殊指令
            if (!shouldProcessInstruction(instruction)) continue

            val element = (instruction as CjElementInstruction).element

            // 对于跳转指令，只处理特定的跳转表达式
            if (instruction is JumpInstruction && !isRelevantJumpElement(element)) {
                continue
            }

            // 根据指令的可达性分类元素
            if (instruction.dead) {
                unreachableElements.add(element)
            } else {
                reachableElements.add(element)
            }
        }

        return UnreachableCodeImpl(reachableElements, unreachableElements)
    }

    /**
     * 判断是否应该处理该指令
     *
     * 过滤掉不需要进行不可达检测的指令类型。
     */
    private fun shouldProcessInstruction(instruction: org.cangnova.cangjie.resolve.controlFlow.pseudocode.instructions.Instruction): Boolean {
        return instruction is CjElementInstruction &&
                instruction !is LoadUnitValueInstruction &&
                instruction !is MergeInstruction &&
                !(instruction is MagicInstruction && instruction.synthetic)
    }

    /**
     * 判断是否是相关的跳转元素
     *
     * 只有特定的跳转表达式需要进行不可达检测。
     */
    private fun isRelevantJumpElement(element: CjElement): Boolean {
        return element is CjBreakExpression ||
                element is CjContinueExpression ||
                element is CjReturnExpression ||
                element is CjThrowExpression
    }

    companion object {
        /**
         * 便捷方法：检测并报告不可达代码
         *
         * @param context 控制流分析上下文
         * @return 不可达代码信息
         */
        fun detectAndReport(context: ControlFlowAnalysisContext): UnreachableCode {
            return UnreachableCodeDetector().detectAndReport(context)
        }
    }
}
