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
import org.cangnova.cangjie.psi.stubs.CangJieNamedFunctionStub
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes
import org.cangnova.cangjie.name.OperatorNameConventions
import com.intellij.lang.ASTNode
import com.intellij.navigation.ItemPresentation
import com.intellij.navigation.ItemPresentationProviders
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.name.*


open class CjNamedFunction : CjFunctionImpl<CangJieNamedFunctionStub, CjNamedFunction> {
    constructor(node: ASTNode) : super(node)

    constructor(stub: CangJieNamedFunctionStub) : super(stub, CjStubElementTypes.FUNCTION)

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D): R? {
        return visitor.visitNamedFunction(this, data)
    }

    override fun hasTypeParameterListBeforeFunctionName(): Boolean {
        val stub: CangJieNamedFunctionStub? = stub
        if (stub != null) {
            return stub.hasTypeParameterListBeforeFunctionName()
        }
        return hasTypeParameterListBeforeFunctionNameByTree()
    }

    private fun hasTypeParameterListBeforeFunctionNameByTree(): Boolean {
        val typeParameterList: CjTypeParameterList = typeParameterList ?: return false
        val nameIdentifier: PsiElement = nameIdentifier ?: return true
        return nameIdentifier.textOffset > typeParameterList.textOffset
    }

    //    当前方法是否是operator set方法
//    set方法规则，最后一个参数是命名参数，并且参数名称是value
    val isSetFunc: Boolean
        get() {
            if (!isOperator) return false
            return valueParameters.lastOrNull()?.isNamed == true && valueParameters.last().nameAsName?.asString() == "value"
        }

    override fun getName(): String? {
        return super.getName()
    }

    override val nameAsName: Name?
        get() {
            if (isSetFunc) return OperatorNameConventions.SET
            return super.nameAsName
        }
    override val fqName: FqName?
        get() = super.fqName

    override val nameAsSafeName: Name
        get() {

            if (isSetFunc) return OperatorNameConventions.SET
            return super.nameAsSafeName
        }

    override fun hasBlockBody(): Boolean {
        val stub: CangJieNamedFunctionStub? = stub
        if (stub != null) {
            return stub.hasBlockBody()
        }
        return equalsToken == null
    }

    @get:IfNotParsed
    val funKeyword: PsiElement?
        get() = findChildByType(CjTokens.FUNC_KEYWORD)

    override val equalsToken: PsiElement?
        get() = super.equalsToken

    override val initializer: CjExpression?
        get() = PsiTreeUtil.getNextSiblingOfType(
            equalsToken,
            CjExpression::class.java,
        )

    override fun hasInitializer(): Boolean {
        return initializer != null
    }

    override fun getPresentation(): ItemPresentation? {
        return ItemPresentationProviders.getItemPresentation(this)
    }

    override val valueParameterList: CjParameterList?
        get() {
            return super.valueParameterList
        }

    override val valueParameters: List<CjParameter>
        get() {
            val list: CjParameterList? = valueParameterList
            return list?.parameters ?: emptyList()
        }

    override val bodyExpression: CjExpression?
        get() {
            return super.bodyExpression
        }

    override val bodyBlockExpression: CjBlockExpression?
        get() {
            return super.bodyBlockExpression
        }

    override fun hasBody(): Boolean {
        val stub: CangJieNamedFunctionStub? = stub
        if (stub != null) {
            return stub.hasBody()
        }
        return bodyBlockExpression != null
    }

    override fun hasDeclaredReturnType(): Boolean {
        return typeReference != null
    }

    override fun toString(): String {
//        return getNode().getElementType().toString();
        return node.elementType.toString() + ": " + name
    }



    override val typeReference: CjTypeReference?
        get() {
            return super.typeReference
        }

    override fun setTypeReference(typeRef: CjTypeReference?): CjTypeReference? {
        return setTypeReference(this, valueParameterList, typeRef)
    }

    override val colon: PsiElement?
        get() {
            return super.colon
        }

    val isAnonymous: Boolean
        get() {
            return name == null && isLocal
        }

    override val isMut: Boolean
        get() = hasModifier(CjTokens.MUT_KEYWORD)
    override val isConst: Boolean
        get() = hasModifier(CjTokens.CONST_KEYWORD)
}
