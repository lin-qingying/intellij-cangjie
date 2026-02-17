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

package org.cangnova.cangjie.references

import com.intellij.openapi.util.TextRange
import org.cangnova.cangjie.descriptors.DeclarationDescriptor
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjMacroExpression
import org.cangnova.cangjie.psi.psiUtil.startOffset
import org.cangnova.cangjie.resolve.binding.BindingContext

/**
 * 宏表达式引用解析器
 *
 * 负责解析宏表达式 (@macroName) 到其定义的引用关系，
 * 支持宏定义和注解类的回退解析，以及跳转到定义、查找引用、重命名等 IDE 功能。
 */
class CjMacroExpressionReference(expression: CjMacroExpression) :
    AbstractCjReference<CjMacroExpression>(expression), CjReference {

    override val resolvesByNames: Collection<Name>
        get() = listOfNotNull(expression.shortName)

    override fun getTargetDescriptors(context: BindingContext): Collection<DeclarationDescriptor> {
        // 1. 优先从完整解析结果获取
        val resolvedResults = context[BindingContext.RESOLVED_MACRO_CALL, expression]
        if (resolvedResults != null && resolvedResults.isSuccess) {
            return listOf(resolvedResults.resultingDescriptor)
        }

        // 2. 回退到 MACRO slice
        val macroDescriptor = context[BindingContext.MACRO, expression]
        if (macroDescriptor != null) {
            return listOf(macroDescriptor)
        }

        // 3. 从 REFERENCE_TARGET 获取（注解类回退）
        val referenceExpression = expression.referenceExpression
        if (referenceExpression != null) {
            val target = context[BindingContext.REFERENCE_TARGET, referenceExpression]
            if (target != null) {
                return listOf(target)
            }
        }

        return emptyList()
    }

    override fun getRangeInElement(): TextRange {
        // 返回 referenceExpression 的范围，这是宏名称部分
        val referenceElement = expression.referenceExpression?.referencedNameElement
            ?: return TextRange.EMPTY_RANGE
        val startOffset = element.startOffset
        return referenceElement.textRange.shiftRight(-startOffset)
    }

    override fun canRename(): Boolean = true
}
