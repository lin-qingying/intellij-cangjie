/*
 * Copyright 2024 LinQingYing. and contributors.
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
import org.cangnova.cangjie.psi.psiUtil.getTrailingCommaByClosingElement
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.TokenSet
import com.intellij.psi.util.PsiTreeUtil

class CjDestructuringDeclaration(node: ASTNode) :
    CjDeclarationImpl(node),
    CjLetVarKeywordOwner,
    CjDeclarationWithInitializer {
    override val expression: CjExpression?
        get() = PsiTreeUtil.getStubChildOfType(
            this,
            CjExpression::class.java,
        )

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R? {
        return visitor.visitDestructuringDeclaration(this, data)
    }

    val entries: List<CjDestructuringDeclarationEntry>
        get() {
            return findChildrenByType(CjNodeTypes.DESTRUCTURING_DECLARATION_ENTRY)
        }

    override val initializer: CjExpression?
        get() {
            val eqNode: ASTNode? = node.findChildByType(CjTokens.EQ)
            if (eqNode == null) {
                return null
            }
            return PsiTreeUtil.getNextSiblingOfType(
                eqNode.psi,
                CjExpression::class.java,
            )
        }

    override fun hasInitializer(): Boolean {
        return initializer != null
    }

    val isVar: Boolean
        get() {
            return node.findChildByType(CjTokens.VAR_KEYWORD) != null
        }

    override val letOrVarKeyword: PsiElement?
        get() {
            return findChildByType(VAL_VAR_KEYWORDS)
        }

    val rPar: PsiElement?
        get() {
            return findChildByType(CjTokens.RPAR)
        }

    val lPar: PsiElement?
        get() {
            return findChildByType(CjTokens.LPAR)
        }

    val trailingComma: PsiElement?
        get() {
            return getTrailingCommaByClosingElement(rPar)
        }

    companion object {
        private val VAL_VAR_KEYWORDS: TokenSet =
            TokenSet.create(CjTokens.LET_KEYWORD, CjTokens.VAR_KEYWORD, CjTokens.CONST_KEYWORD)
    }
}
