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

import org.cangnova.cangjie.lexer.CjTokens.*
import org.cangnova.cangjie.psi.stubs.CangJieNameBasicReferenceExpressionStub
import org.cangnova.cangjie.psi.stubs.CangJieNameReferenceExpressionStub
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import org.cangnova.cangjie.name.*

interface CjCallableReference : CjReferenceExpression {
    val callableReference: CjSimpleNameExpression
    val receiverExpression: CjExpression? get() = null
}

class CjNameReferenceExpression :
    CjExpressionImplStub<CangJieNameReferenceExpressionStub>,
    CjSimpleNameExpression,
    CjCallableReference {
    constructor(node: ASTNode) : super(node)

    constructor(stub: CangJieNameReferenceExpressionStub) : super(stub, CjStubElementTypes.REFERENCE_EXPRESSION)

    override val referencedName: String
        get() {
            val stub = stub
            if (stub != null) {
                return stub.getReferencedName()
            }
            return CjSimpleNameExpressionImpl.getReferencedNameImpl(this)
        }

    override val referencedNameAsName: Name
        get() {
            return CjSimpleNameExpressionImpl.getReferencedNameAsNameImpl(this)
        }

    override val referencedNameElement: PsiElement
        get() {
            return findChildByType(NAME_REFERENCE_EXPRESSIONS) ?: this
        }
    val typeArguments: List<CjTypeProjection>
        get() {

            return typeArgumentList?.arguments ?: emptyList()
        }
    val typeArgumentList: CjTypeArgumentList?
        get() {

            return findChildByType<PsiElement>(CjNodeTypes.TYPE_ARGUMENT_LIST) as CjTypeArgumentList?
        }

    override val identifier: PsiElement?
        get() {
            return findChildByType(IDENTIFIER)
        }

    override val referencedNameElementType: IElementType
        get() {
            return CjSimpleNameExpressionImpl.getReferencedNameElementTypeImpl(this)
        }

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D): R? {
        return visitor.visitSimpleNameExpression(this, data)
    }

    val isPlaceholder: Boolean
        get() = identifier?.text?.equals("_") == true

    companion object {
        private val NAME_REFERENCE_EXPRESSIONS = TokenSet.create(IDENTIFIER, THIS_KEYWORD, SUPER_KEYWORD, VARRAY_KEYWORD)
    }

    override val callableReference: CjNameReferenceExpression
        get() = this
}

class CjNameBasicReferenceExpression :
    CjExpressionImplStub<CangJieNameBasicReferenceExpressionStub>,
    CjSimpleNameExpression,
    CjCallableReference {
    constructor(node: ASTNode) : super(node)

    constructor(stub: CangJieNameBasicReferenceExpressionStub) : super(stub, CjStubElementTypes.BASIC_REFERENCE_EXPRESSION)

    override val referencedName: String
        get() {
            val stub = stub
            if (stub != null) {
                return stub.getReferencedName()
            }
            return CjSimpleNameExpressionImpl.getReferencedNameImpl(this)
        }

    override val referencedNameAsName: Name
        get() {
            return CjSimpleNameExpressionImpl.getReferencedNameAsNameImpl(this)
        }

    override val referencedNameElement: PsiElement
        get() {
            return findChildByType(NAME_REFERENCE_EXPRESSIONS) ?: this
        }
    val typeArguments: List<CjTypeProjection>
        get() {

            return typeArgumentList?.arguments ?: emptyList()
        }
    val typeArgumentList: CjTypeArgumentList?
        get() {

            return findChildByType<PsiElement>(CjNodeTypes.TYPE_ARGUMENT_LIST) as CjTypeArgumentList?
        }

    override val identifier: PsiElement?
        get() {
            return findChildByType(IDENTIFIER)
        }

    override val referencedNameElementType: IElementType
        get() {
            return CjSimpleNameExpressionImpl.getReferencedNameElementTypeImpl(this)
        }

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D): R? {
        return visitor.visitSimpleNameExpression(this, data)
    }

    val isPlaceholder: Boolean
        get() = identifier?.text?.equals("_") == true

    companion object {
        private val NAME_REFERENCE_EXPRESSIONS = TokenSet.create(IDENTIFIER, THIS_KEYWORD, SUPER_KEYWORD, VARRAY_KEYWORD)
    }

    override val callableReference: CjNameBasicReferenceExpression
        get() = this
}
