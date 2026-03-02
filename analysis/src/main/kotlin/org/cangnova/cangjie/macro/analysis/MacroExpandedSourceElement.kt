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
 */

package org.cangnova.cangjie.macro.analysis

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.descriptors.SourceElement
import org.cangnova.cangjie.descriptors.SourceFile
import org.cangnova.cangjie.psi.CjMacroExpression
import org.cangnova.cangjie.resolve.source.PsiSourceElement
import org.cangnova.cangjie.resolve.source.PsiSourceFile

/**
 * 宏展开后的源元素
 *
 * 自定义 [SourceElement] 实现，支持从合成描述符导航回原始宏调用处。
 * 当用户 Ctrl+Click 宏生成的成员时，将导航到原始的宏表达式。
 *
 * @param macroExpression 原始宏调用 PSI 元素
 * @param expandedElement 展开后的 PSI 元素（可选，用于展开后代码的精确导航）
 */
class MacroExpandedSourceElement(
    val macroExpression: CjMacroExpression,
    val expandedElement: PsiElement? = null
) : PsiSourceElement {

    override val psi: PsiElement
        get() = macroExpression

    override val containingFile: SourceFile
        get() = macroExpression.containingFile.let(::PsiSourceFile)

    override fun toString(): String =
        "MacroExpandedSourceElement(macro=${macroExpression.shortName}, expanded=${expandedElement?.text?.take(50)})"
}
