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

package com.linqingying.cangjie.psi

import com.intellij.lang.ASTNode
import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.TokenSet
import com.intellij.psi.util.PsiTreeUtil
import com.linqingying.cangjie.CjNodeTypes
import com.linqingying.cangjie.lexer.CjTokens
import com.linqingying.cangjie.psi.stubs.CangJieVariableStub
import com.linqingying.cangjie.psi.stubs.elements.CjStubElementTypes

class CjVariable : CjTypeParameterListOwnerStub<CangJieVariableStub>, CjVariableDeclaration, CjLocalNamedDeclaration {
    constructor(stub: CangJieVariableStub) : super(stub, CjStubElementTypes.VARIABLE)
    constructor(node: ASTNode) : super(node)


    override val valueParameterList: CjParameterList?
        //    @Override
        get() = null
    val pattern: CjCasePattern? get() = findChildByClass(CjCasePattern::class.java)

    val equalsToken: PsiElement? get() = findChildByType(CjTokens.EQ)

    override fun toString(): String {
        return super.toString() + ": " + name
    }

    val isPattern get() = pattern != null && name == null
    override val valueParameters: List<CjParameter>
        get() = emptyList()

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R {
        return visitor.visitVariable(this, data)
    }


    override val receiverTypeReference: CjTypeReference?
        get() {
            val stub = stub
            if (stub != null) {
                return if (!stub.isExtension()) {
                    null
                } else {
                    getStubOrPsiChild(CjStubElementTypes.TYPE_REFERENCE)
                }
            }
            return receiverTypeRefByTree
        }

    private val receiverTypeRefByTree: CjTypeReference?
        get() {
            var node = node.firstChildNode
            while (node != null) {
                val tt = node.elementType
                if (tt === CjTokens.COLON) break

                if (tt === CjNodeTypes.TYPE_REFERENCE) {
                    return node.psi as CjTypeReference
                }
                node = node.treeNext
            }

            return null
        }

    val isTopLevel: Boolean
        get() {
            val stub = stub
            if (stub != null) {
                return stub.isTopLevel()
            }

            return parent is CjFile
        }
    override val typeReference: CjTypeReference?
        get() {
            val stub = stub
            if (stub != null) {
                if (!stub.hasReturnTypeRef()) {
                    return null
                } else {
                    val typeReferences =
                        getStubOrPsiChildrenAsList(
                            CjStubElementTypes.TYPE_REFERENCE
                        )
                    val returnTypeRefPositionInPsi = if (stub.isExtension()) 1 else 0
                    if (typeReferences.size <= returnTypeRefPositionInPsi) {
                        LOG.error(
                            """
                                Invalid stub structure built for property:
                                $text
                                """.trimIndent()
                        )
                        return null
                    }
                    return typeReferences[returnTypeRefPositionInPsi]
                }
            }
            return getTypeReference(this)
        }
    val isLocal: Boolean
        get() = !isTopLevel && !isMember


    val isMember: Boolean
        get() {
            val parent = parent
            return parent is CjTypeStatement || parent is CjAbstractClassBody
        }

    override fun setTypeReference(typeRef: CjTypeReference?): CjTypeReference? {
        return setTypeReference(this, node.findChildByType(CjNodeTypes.BINDING_PATTERN)?.getPsi(), typeRef)
    }

    override fun getNameIdentifier(): PsiElement? {
       return node.findChildByType(CjNodeTypes.BINDING_PATTERN)?.findChildByType(CjNodeTypes.REFERENCE_EXPRESSION)?.findChildByType(CjTokens.IDENTIFIER)?.getPsi()
    }

    override val colon: PsiElement?
        get() = findChildByType(CjTokens.COLON)

    override val isVar: Boolean
        get() {
            val stub = stub
            if (stub != null) {
                return stub.isVar()
            }

            return node.findChildByType(CjTokens.VAR_KEYWORD) != null
        }
    override val isStatic: Boolean
        get() = hasModifier(CjTokens.STATIC_KEYWORD)
    override val letOrVarKeyword: PsiElement
        get() {
            val element =
                checkNotNull(findChildByType(LET_VAR_TOKEN_SET)) { "Let or var should always exist for property" + this.text }
            return element
        }

    override val initializer: CjExpression?
        get() {
            val stub = stub
            if (stub != null) {
                if (!stub.hasInitializer()) {
                    return null
                }

                if (containingCjFile.isCompiled) {
                    return null
                }
            }

            return PsiTreeUtil.getNextSiblingOfType(
                findChildByType(CjTokens.EQ),
                CjExpression::class.java
            )
        }

    override fun hasInitializer(): Boolean {
        val stub = stub
        if (stub != null) {
            return stub.hasInitializer()
        }

        return initializer != null
    }


    companion object {
        private val LOG = Logger.getInstance(
            CjVariable::class.java
        )

        private val LET_VAR_TOKEN_SET =
            TokenSet.create(CjTokens.LET_KEYWORD, CjTokens.CONST_KEYWORD, CjTokens.VAR_KEYWORD)
    }
}
