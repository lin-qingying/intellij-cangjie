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

package cn.cangnova.cangjie.psi

import cn.cangnova.cangjie.lexer.CjTokens
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.SearchScope
import com.intellij.psi.tree.TokenSet
import cn.cangnova.cangjie.name.*

class CjDestructuringDeclarationEntry(node: ASTNode) : CjNamedDeclarationNotStubbed(node), CjVariableDeclaration {

    override val typeReference: CjTypeReference?
        get() = getTypeReference(this)

    override fun setTypeReference(typeRef: CjTypeReference?): CjTypeReference? {
        return setTypeReference(this, nameIdentifier, typeRef)
    }

    override val colon: PsiElement?
        get() = findChildByType(CjTokens.COLON)

    override val contextReceivers: List<CjContextReceiver> = emptyList()
    override val valueParameterList: CjParameterList? = null
    override val valueParameters: List<CjParameter> = emptyList()
    override val receiverTypeReference: CjTypeReference? = null

    override val typeParameterList: CjTypeParameterList?
        get() = null

    override val typeConstraintList: CjTypeConstraintList?
        get() = null

    override val typeConstraints: List<CjTypeConstraint>
        get() = emptyList()

    override val typeParameters: List<CjTypeParameter>
        get() = emptyList()

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R? {
        return visitor.visitDestructuringDeclarationEntry(this, data)
    }

    override val isVar: Boolean
        get() = parentNode.findChildByType(CjTokens.VAR_KEYWORD) != null
    override val isStatic: Boolean
        get() = parentNode.findChildByType(CjTokens.STATIC_KEYWORD) != null

    override val initializer: CjExpression?
        get() = null

    override fun hasInitializer(): Boolean {
        return false
    }

    private val parentNode: ASTNode
        get() {
            val parent = node.treeParent
            assert(parent.elementType === CjNodeTypes.DESTRUCTURING_DECLARATION) { "parent is " + parent.elementType }
            return parent
        }

    override val letOrVarKeyword: PsiElement?
        get() {
            val node =
                parentNode.findChildByType(LET_VAR_KEYWORDS) ?: return null
            return node.psi
        }

    override val fqName: FqName?
        get() = null

    override fun getUseScope(): SearchScope {
        var enclosingBlock = CjPsiUtil.getEnclosingElementForLocalDeclaration(this, false)
        if (enclosingBlock is CjParameter) {
            enclosingBlock = CjPsiUtil.getEnclosingElementForLocalDeclaration(enclosingBlock, false)
        }
        if (enclosingBlock != null) return LocalSearchScope(enclosingBlock)

        return super.getUseScope()
    }

    companion object {
        private val LET_VAR_KEYWORDS =
            TokenSet.create(CjTokens.LET_KEYWORD, CjTokens.VAR_KEYWORD, CjTokens.CONST_KEYWORD)
    }
}
