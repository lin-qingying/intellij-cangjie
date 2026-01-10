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
import org.cangnova.cangjie.psi.psiUtil.getStrictParentOfType
import org.cangnova.cangjie.psi.stubs.CangJiePropertyStub
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode
import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.TokenSet

open class CjProperty : CjTypeParameterListOwnerStub<CangJiePropertyStub>, CjVariableDeclaration {
    companion object {
        private val LET_VAR_TOKEN_SET =
            TokenSet.create(CjTokens.PROP_KEYWORD, CjTokens.MUT_KEYWORD, CjTokens.CONST_KEYWORD)
        val LOG: Logger = Logger.getInstance(
            CjProperty::class.java,
        )
    }
    val isLocal: Boolean
        get() = !isMember

    val isMember: Boolean
        get() {
            val parent = parent
            return parent is CjTypeStatement || parent is CjAbstractClassBody
        }
    override val isStatic: Boolean
        get() = hasModifier(CjTokens.STATIC_KEYWORD)

    override fun <R : Any?, D : Any?> accept(visitor: CjVisitor<R, D>, data: D): R? {
        return visitor.visitProperty(this, data)
    }

    fun hasBody(): Boolean {
//        if (hasDelegateExpressionOrInitializer()) return true

        if (getter != null && getter!!.hasBody()) {
            return true
        }

        if (setter != null && setter!!.hasBody()) {
            return true
        }
        return false
    }

    override val typeReference: CjTypeReference?
        get() {
            val stub: CangJiePropertyStub? = stub
            if (stub != null) {
                if (!stub.hasReturnTypeRef()) {
                    return null
                } else {
                    val typeReferences: List<CjTypeReference> =
                        getStubOrPsiChildrenAsList(CjStubElementTypes.TYPE_REFERENCE)
                    val returnTypeRefPositionInPsi = /*if (stub.isExtension()) 1 else*/ 0
                    if (typeReferences.size <= returnTypeRefPositionInPsi) {
                        LOG.error(
                            """
                        Invalid stub structure built for property:
                        $text
                            """.trimIndent(),
                        )
                        return null
                    }
                    return typeReferences[returnTypeRefPositionInPsi]
                }
            }
            return getTypeReference(this)
        }

    override fun setTypeReference(typeRef: CjTypeReference?): CjTypeReference? {
        return setTypeReference(this, nameIdentifier, typeRef)
    }

    override val colon: PsiElement?
        get() = findChildByType(CjTokens.COLON)

    override val initializer: CjExpression?
        get() = null

    override fun hasInitializer(): Boolean {
        val stub: CangJiePropertyStub? = stub
//        if (stub != null) {
//            return stub.hasInitializer()
//        }

        return initializer != null
    }

    override val letOrVarKeyword: PsiElement?
        get() {
            val element =
                checkNotNull(findChildByType(LET_VAR_TOKEN_SET)) { "Let or var should always exist for property" + this.text }
            return element
        }

    constructor(stub: CangJiePropertyStub) : super(stub, CjStubElementTypes.PROPERTY)
    constructor(node: ASTNode) : super(node)

    override val isVar: Boolean
        get() = hasModifier(CjTokens.MUT_KEYWORD)

    override fun toString(): String = super.toString() + ": " + name
    private val receiverTypeRefByTree: CjTypeReference?
        get() {
            val parent = this.getStrictParentOfType<CjExtend>()

            return parent?.receiverTypeReceiver
        }

    override val valueParameterList: CjParameterList? = null
    override val valueParameters: List<CjParameter> = emptyList()

    val body: CjPropertyBody?
        get() = getStubOrPsiChild(CjStubElementTypes.PROPERTY_BODY)

    val accessors: List<CjPropertyAccessor>
        get() = body?.accessors ?: emptyList()

    val getter: CjPropertyAccessor?
        get() {
            for (accessor in accessors) {
                if (accessor.isGetter) return accessor
            }
            return null
        }

    val setter: CjPropertyAccessor?
        get() {
            for (accessor in accessors)
                if (accessor.isSetter) return accessor
            return null
        }
}
