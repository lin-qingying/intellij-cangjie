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

import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.name.Name
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType

interface CjSimpleNameExpression : CjReferenceExpression {

    val referencedName: String

    val referencedNameAsName: Name

    val referencedNameElement: PsiElement

    val identifier: PsiElement?

    val referencedNameElementType: IElementType
}

fun CjSimpleNameExpression.getTypeArguments(): List<CjTypeProjection> {
    return when (this) {
        is CjNameReferenceExpression -> typeArguments
        else -> emptyList()
    }
}

abstract class CjSimpleNameExpressionImpl(node: ASTNode) : CjExpressionImpl(node), CjSimpleNameExpression {
    override val identifier get(): PsiElement? = findChildByType(CjTokens.IDENTIFIER)

    override val referencedNameElementType get() = getReferencedNameElementTypeImpl(this)

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D): R? {
        return visitor.visitSimpleNameExpression(this, data)
    }

    override val referencedNameAsName get() = getReferencedNameAsNameImpl(this)

    override val referencedName get() = getReferencedNameImpl(this)

    companion object {
        fun getReferencedNameElementTypeImpl(expression: CjSimpleNameExpression): IElementType {
            return expression.referencedNameElement.node!!.elementType
        }

        fun getReferencedNameAsNameImpl(expresssion: CjSimpleNameExpression): Name {
            val name = expresssion.referencedName
            return Name.identifier(name)
        }

        fun getReferencedNameImpl(expression: CjSimpleNameExpression): String {
            val text = expression.referencedNameElement.node!!.text
            return CjPsiUtil.unquoteIdentifierOrFieldReference(text)
        }
    }
}
