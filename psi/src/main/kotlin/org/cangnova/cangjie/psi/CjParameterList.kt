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
import org.cangnova.cangjie.psi.EditCommaSeparatedListHelper.addItem
import org.cangnova.cangjie.psi.EditCommaSeparatedListHelper.addItemAfter
import org.cangnova.cangjie.psi.EditCommaSeparatedListHelper.addItemBefore
import org.cangnova.cangjie.psi.EditCommaSeparatedListHelper.removeItem
import org.cangnova.cangjie.psi.psiUtil.getTrailingCommaByClosingElement
import org.cangnova.cangjie.psi.stubs.CangJiePlaceHolderStub
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement

class CjParameterList : CjElementImplStub<CangJiePlaceHolderStub<CjParameterList>> {
    constructor(node: ASTNode) : super(node)

    constructor(stub: CangJiePlaceHolderStub<CjParameterList>) : super(stub, CjStubElementTypes.VALUE_PARAMETER_LIST)

    override fun toString(): String {
        return node.elementType.toString()
    }

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D): R? {
        return visitor.visitParameterList(this, data)
    }

    override fun getParent(): PsiElement? {
        val stub: CangJiePlaceHolderStub<CjParameterList>? = stub
        return if (stub != null) stub.parentStub.psi else super.getParent()
    }

    val parameters: List<CjParameter>
        get() = getStubOrPsiChildrenAsList(CjStubElementTypes.VALUE_PARAMETER)

    fun addParameter(parameter: CjParameter): CjParameter {
        return addItem(
            this,
            parameters,
            parameter,
        )
    }

    fun addParameterBefore(parameter: CjParameter, anchor: CjParameter?): CjParameter {
        return addItemBefore(
            this,
            parameters,
            parameter,
            anchor,
        )
    }

    fun addParameterAfter(parameter: CjParameter, anchor: CjParameter?): CjParameter {
        return addItemAfter(
            this,
            parameters,
            parameter,
            anchor,
        )
    }

    fun removeParameter(parameter: CjParameter) {
        removeItem(parameter)
    }

    fun removeParameter(index: Int) {
        removeParameter(parameters[index])
    }

    val ownerFunction: CjDeclarationWithBody?
        get() {
            val parent = parentByStub as? CjDeclarationWithBody ?: return null
            return parent
        }

    val rightParenthesis: PsiElement?
        get() = findChildByType(CjTokens.RPAR)

    val leftParenthesis: PsiElement?
        get() = findChildByType(CjTokens.LPAR)

    val firstComma: PsiElement?
        get() = findChildByType(CjTokens.COMMA)

    val trailingComma: PsiElement?
        get() {
            val parentElement = parent
            //        if (parentElement instanceof CjFunctionLiteral || parentElement instanceof CjPropertyAccessor) {
//            return CjPsiUtilKt.getTrailingCommaByElementsList(this);
//        } else {
            return getTrailingCommaByClosingElement(rightParenthesis)
            //        }
        }
}
