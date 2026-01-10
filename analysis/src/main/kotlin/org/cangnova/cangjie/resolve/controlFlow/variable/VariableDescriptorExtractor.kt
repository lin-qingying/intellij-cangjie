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

package org.cangnova.cangjie.resolve.controlFlow.variable

import org.cangnova.cangjie.descriptors.VariableDescriptor
import org.cangnova.cangjie.psi.CjBindingPattern
import org.cangnova.cangjie.psi.CjDeclaration
import org.cangnova.cangjie.psi.CjPatternVariable
import org.cangnova.cangjie.psi.stubs.elements.getAllBindings
import org.cangnova.cangjie.resolve.binding.BindingContext
import org.cangnova.cangjie.resolve.binding.BindingContextUtils.variableDescriptorForDeclaration
import org.cangnova.cangjie.resolve.controlFlow.pseudocode.instructions.Instruction
import org.cangnova.cangjie.resolve.controlFlow.pseudocode.instructions.eval.AccessTarget
import org.cangnova.cangjie.resolve.controlFlow.pseudocode.instructions.eval.AccessValueInstruction
import org.cangnova.cangjie.resolve.controlFlow.pseudocode.instructions.eval.WriteValueInstruction
import org.cangnova.cangjie.resolve.controlFlow.pseudocode.instructions.special.VariableDeclarationInstruction

/**
 * 变量描述符提取器
 *
 * 负责从不同类型的 PSI 元素和伪代码指令中提取变量描述符。
 * 此类统一处理普通变量声明和模式匹配变量声明的描述符提取逻辑。
 *
 * ## 设计动机
 *
 * 仓颉语言支持模式匹配变量声明，例如：
 * ```
 * let (x, y) = tuple
 * let Some(value) = optional
 * ```
 *
 * 在这种情况下，`CjPatternVariable` 包含模式，但变量描述符实际绑定在
 * 子元素 `CjBindingPattern` 上。此类封装了这种复杂性，提供统一的提取接口。
 *
 * @property bindingContext 绑定上下文，用于获取 PSI 元素到描述符的映射
 */
class VariableDescriptorExtractor(private val bindingContext: BindingContext) {

    /**
     * 从声明指令中提取所有变量描述符
     *
     * @param instruction 变量声明指令
     * @return 变量描述符列表（可能为空）
     */
    fun extractFromDeclaration(instruction: VariableDeclarationInstruction): List<VariableDescriptor> {
        val element = instruction.variableDeclarationElement
        return extractFromDeclarationElement(element)
    }

    /**
     * 从声明元素中提取所有变量描述符
     *
     * 处理两种情况：
     * 1. 模式匹配变量声明 - 提取所有绑定模式的描述符
     * 2. 普通变量声明 - 直接获取描述符
     *
     * @param element 声明元素
     * @return 变量描述符列表
     */
    fun extractFromDeclarationElement(element: CjDeclaration): List<VariableDescriptor> {
        return if (element is CjPatternVariable) {
            extractFromPatternVariable(element)
        } else {
            extractSingleDescriptor(element)?.let { listOf(it) } ?: emptyList()
        }
    }

    /**
     * 从模式匹配变量中提取所有绑定模式的描述符
     *
     * @param patternVariable 模式匹配变量声明
     * @return 所有绑定模式对应的变量描述符列表
     */
    fun extractFromPatternVariable(patternVariable: CjPatternVariable): List<VariableDescriptor> {
        val bindings = patternVariable.pattern.getAllBindings()
        return bindings.mapNotNull { binding ->
            extractFromBindingPattern(binding)
        }
    }

    /**
     * 从绑定模式中提取变量描述符
     *
     * @param binding 绑定模式
     * @return 变量描述符，如果无法提取则返回 null
     */
    fun extractFromBindingPattern(binding: CjBindingPattern): VariableDescriptor? {
        val descriptor = bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, binding)
        return variableDescriptorForDeclaration(descriptor)
    }

    /**
     * 从单个声明元素中提取变量描述符
     *
     * @param element 声明元素（非模式匹配）
     * @return 变量描述符，如果无法提取则返回 null
     */
    fun extractSingleDescriptor(element: CjDeclaration): VariableDescriptor? {
        val descriptor = bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, element)
        return variableDescriptorForDeclaration(descriptor)
    }

    /**
     * 从读/写指令中提取变量描述符
     *
     * 用于从 ReadValueInstruction 或 WriteValueInstruction 中提取被引用的变量描述符。
     * 使用 AccessTarget 来获取描述符，这是 PseudocodeUtil 推荐的方式。
     *
     * @param instruction 读取或写入指令
     * @return 变量描述符，如果无法提取则返回 null
     */
    fun extractFromAccessInstruction(instruction: Instruction): VariableDescriptor? {
        if (instruction !is AccessValueInstruction) return null
        return extractFromAccessTarget(instruction.target)
    }

    /**
     * 从 AccessTarget 中提取变量描述符
     */
    private fun extractFromAccessTarget(target: AccessTarget): VariableDescriptor? {
        return when (target) {
            is AccessTarget.Declaration -> target.descriptor
            is AccessTarget.Call -> variableDescriptorForDeclaration(target.resolvedCall.resultingDescriptor)
            is AccessTarget.BlackBox -> null
        }
    }

    companion object {
        /**
         * 判断写入指令是否是平凡初始化器
         *
         * 平凡初始化器的特征：指令的元素是一个声明（CjDeclaration），
         * 表示在声明变量的同时进行初始化（如 `let x = 1`）。
         *
         * @param instruction 写入指令
         * @return true 如果是声明时的初始化
         */
        fun isTrivialInitializer(instruction: WriteValueInstruction): Boolean {
            return instruction.element is CjDeclaration
        }
    }
}
