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
import org.cangnova.cangjie.psi.stubs.CangJieNamedFunctionStub
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.IncorrectOperationException
import org.cangnova.cangjie.psi.stubs.CangJieFunctionStub

abstract class CjFunctionImpl<Stub: CangJieFunctionStub<F>,F: CjFunction> :
    CjTypeParameterListOwnerStub<Stub>,
    CjFunction,
    CjDeclarationWithInitializer {
    constructor(node: ASTNode) : super(node)

    constructor(stub: Stub, nodeType: IStubElementType<*, *>) : super(stub, nodeType)

    override val valueParameterList: CjParameterList?
        get() = getStubOrPsiChild(CjStubElementTypes.VALUE_PARAMETER_LIST)

    override fun toString(): String {
        return node.elementType.toString()
    }

//    是否需要推断返回值类型
    open val isInferReturnType: Boolean get() = typeReference == null
    open fun hasTypeParameterListBeforeFunctionName(): Boolean {
        val stub = stub
        if (stub != null) {
            return stub.hasTypeParameterListBeforeFunctionName()
        }
        return hasTypeParameterListBeforeFunctionNameByTree()
    }

    private fun hasTypeParameterListBeforeFunctionNameByTree(): Boolean {
        val typeParameterList = typeParameterList ?: return false
        val nameIdentifier = nameIdentifier ?: return true
        return nameIdentifier.textOffset > typeParameterList.textOffset
    }


    val originalTypeParameterList: CjTypeParameterList? get() = super.typeParameterList

    override val typeParameterList: CjTypeParameterList?
        get() {

            val superTypeParameterList = super.typeParameterList

            if (superTypeParameterList != null) return superTypeParameterList

            return null
        }
    private val receiverTypeRefByTree: CjTypeReference?
        get() {
            val parent = this.getStrictParentOfType<CjExtend>()

            return parent?.receiverTypeReceiver
        }

    override val typeReference: CjTypeReference?
        get() {
            val stub = stub
            if (stub != null) {
                val typeReferences =
                    getStubOrPsiChildrenAsList(
                        CjStubElementTypes.TYPE_REFERENCE,
                    )
                val returnTypeIndex = if (stub.isExtension()) 1 else 0
                if (returnTypeIndex >= typeReferences.size) {
                    return null
                }
                return typeReferences[returnTypeIndex]
            }
            return getTypeReference(this)
        }

    override fun setTypeReference(typeRef: CjTypeReference?): CjTypeReference? {
        return setTypeReference(this, valueParameterList, typeRef)
    }

    override val colon: PsiElement?
        get() = findChildByType(CjTokens.COLON)
    override val bodyExpression: CjExpression?
        get() {
            val stub = stub
            if (stub != null) {
                if (!stub.hasBody()) {
                    return null
                }
                if (containingCjFile.isCompiled) {
                    return null
                }
            }

            return findChildByClass(CjExpression::class.java)
        }

    override val keyword: PsiElement?
        get() = findChildByType(CjTokens.FUNC_KEYWORD)
    override val equalsToken: PsiElement?
        get() = findChildByType(CjTokens.EQ)
    override val bodyBlockExpression: CjBlockExpression?
        get() {

            val stub = stub
            if (stub != null) {
                if (!(stub.hasBlockBody() && stub.hasBody())) {
                    return null
                }
                if (containingCjFile.isCompiled) {
                    return null
                }
            }

            val bodyExpression = findChildByClass(
                CjExpression::class.java,
            )
            if (bodyExpression is CjBlockExpression) {
                return bodyExpression
            }

            return null
        }

    override fun hasBlockBody(): Boolean {
        val stub = stub
        if (stub != null) {
            return stub.hasBlockBody()
        }
        return equalsToken == null
    }

    override fun hasBody(): Boolean {
        val stub = stub
        if (stub != null) {
            return stub.hasBody()
        }
        return bodyExpression != null
    }

    override fun hasDeclaredReturnType(): Boolean {
        return false
    }

    override val valueParameters: List<CjParameter>
        get() {

            val list = valueParameterList
            return list?.parameters ?: emptyList()
        }

    override val isLocal: Boolean
        get() {
            val parent = parent
            return !(parent is CjFile || parent is CjAbstractClassBody)
        }
    override val isUnsafe
        get() = hasModifier(CjTokens.UNSAFE_KEYWORD)
    override val isStatic: Boolean
        get() = hasModifier(CjTokens.STATIC_KEYWORD)
    override val isOperator: Boolean
        get() = hasModifier(CjTokens.OPERATOR_KEYWORD)

    override val initializer: CjExpression?
        //    public bool mayHaveContract() {
        get() = PsiTreeUtil.getNextSiblingOfType(
            equalsToken,
            CjExpression::class.java,
        )

    override fun hasInitializer(): Boolean {
        return initializer != null
    }

    open val isTopLevel: Boolean
        //    @Override
        get() {
            val stub = stub
            if (stub != null) {
                return stub.isTopLevel()
            }

            return parent is CjFile
        }

    @Throws(IncorrectOperationException::class)
    override fun setName(name: String): PsiElement? {
        return super.setName(name)
    }
}
