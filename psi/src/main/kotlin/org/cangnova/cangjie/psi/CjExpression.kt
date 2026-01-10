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

package org.cangnova.cangjie.psi

import org.cangnova.cangjie.psi.psiUtil.createExpressionByPattern
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType
import com.intellij.util.ArrayFactory

// 表达式
interface CjExpression : CjElement {
    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D): R?

    companion object {
        @JvmStatic
        val EMPTY_ARRAY = arrayOf<CjExpression>()

        @JvmStatic
        val ARRAY_FACTORY =
            ArrayFactory { count: Int ->
                if (count == 0) EMPTY_ARRAY else arrayOfNulls<CjExpression>(count)
            }
    }
}

abstract class CjExpressionImpl(node: ASTNode) : CjElementImpl(node), CjExpression {

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D): R? = visitor.visitExpression(this, data)

    protected fun findExpressionUnder(type: IElementType): CjExpression? {
        val containerNode = findChildByType<CjContainerNode>(type) ?: return null
        return containerNode.findChildByClass(CjExpression::class.java)
    }

    override fun replace(newElement: PsiElement): PsiElement {
        return replaceExpression(this, newElement) { super.replace(it) }
    }

    companion object {
        fun replaceExpression(
            expression: CjExpression,
            newElement: PsiElement,
            reformat: Boolean = true,
            rawReplaceHandler: (PsiElement) -> PsiElement,
        ): PsiElement {
            val parent = expression.parent

            if (newElement is CjExpression) {
                when (parent) {
                    is CjExpression, is CjValueArgument -> {
                        if (CjPsiUtil.areParenthesesNecessary(newElement, expression, parent as CjElement)) {
                            val factory = CjPsiFactory(expression.project)
                            return rawReplaceHandler(
                                factory.createExpressionByPattern(
                                    "($0)",
                                    newElement,
                                    reformat = reformat,
                                ),
                            )
                        }
                    }
//                    is CjSimpleNameStringTemplateEntry -> {
//                        if (newElement !is CjSimpleNameExpression && !newElement.isThisWithoutLabel()) {
//                            val factory = CjPsiFactory(expression.project)
//                            val newEntry = parent.replace(factory.createBlockStringTemplateEntry(newElement)) as CjBlockStringTemplateEntry
//                            return newEntry.expression!!
//                        }
//                    }
                }
            }

            return rawReplaceHandler(newElement)
        }
    }
}
