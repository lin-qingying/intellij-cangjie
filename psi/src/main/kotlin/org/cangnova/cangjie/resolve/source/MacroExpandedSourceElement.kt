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

package org.cangnova.cangjie.resolve.source

import com.intellij.openapi.util.Key
import org.cangnova.cangjie.descriptors.SourceFile
import org.cangnova.cangjie.psi.CjMacroExpression
import com.intellij.psi.PsiElement

/**
 * 宏展开后的源元素
 *
 * 自定义 [org.cangnova.cangjie.descriptors.SourceElement] 实现，支持从合成描述符导航回原始宏调用处。
 * 当用户 Ctrl+Click 宏生成的成员时，将导航到原始的宏表达式。
 *
 * - **导航 (Ctrl+Click)**: 使用 [macroExpression]（跳转到原始宏调用处）
 * - **快速文档**: 使用 [expandedElement]（显示展开后的代码文档）
 *
 * 通过在虚拟 [CjFile][org.cangnova.cangjie.psi.CjFile] 上设置 [MACRO_EXPRESSION_KEY]，
 * [toSourceElement][org.cangnova.cangjie.resolve.source.toSourceElement] 会自动为宏展开文件中的所有 PSI 节点
 * 创建 [MacroExpandedSourceElement]，无需在每个描述符创建处手动处理。
 *
 * @param macroExpression 原始宏调用 PSI 元素
 * @param expandedElement 展开后的 PSI 元素（可选，用于快速文档等需要展开后代码的场景）
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

    companion object {
        /**
         * 存储在宏展开虚拟 CjFile 上的 UserData Key，关联原始宏调用表达式。
         *
         * 当此 Key 被设置后，[toSourceElement] 会自动为该文件中的所有 PSI 节点
         * 创建 [MacroExpandedSourceElement]，而非普通的 [CangJieSourceElement]。
         */
        @JvmField
        val MACRO_EXPRESSION_KEY = Key.create<CjMacroExpression>("MACRO_EXPANDED_SOURCE_EXPRESSION")
    }
}