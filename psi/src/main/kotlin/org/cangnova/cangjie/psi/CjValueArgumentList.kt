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

class CjValueArgumentList : CjElementImplStub<CangJiePlaceHolderStub<CjValueArgumentList>> {
    constructor(node: ASTNode) : super(node)

    constructor(stub: CangJiePlaceHolderStub<CjValueArgumentList>) : super(
        stub,
        CjStubElementTypes.VALUE_ARGUMENT_LIST,
    )

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D): R? {
        return visitor.visitValueArgumentList(this, data)
    }

    val arguments: List<CjValueArgument>
        get() = getStubOrPsiChildrenAsList(
            CjStubElementTypes.VALUE_ARGUMENT,
        )

    val rightParenthesis: PsiElement?
        get() {
            return findChildByType(CjTokens.RPAR)
        }

    val leftParenthesis: PsiElement?
        get() {
            return findChildByType(CjTokens.LPAR)
        }

    fun addArgument(argument: CjValueArgument): CjValueArgument {
        return addItem(
            this,
            arguments,
            argument,
        )
    }

    fun addArgumentAfter(argument: CjValueArgument, anchor: CjValueArgument?): CjValueArgument {
        return addItemAfter(
            this,
            arguments,
            argument,
            anchor,
        )
    }

    fun addArgumentBefore(argument: CjValueArgument, anchor: CjValueArgument?): CjValueArgument {
        return addItemBefore(
            this,
            arguments,
            argument,
            anchor,
        )
    }

    fun removeArgument(argument: CjValueArgument) {
        assert(argument.parent === this)
        removeItem(argument)
    }

    fun removeArgument(index: Int) {
        removeArgument(arguments[index])
    }

    val trailingComma: PsiElement?
        get() {
            return getTrailingCommaByClosingElement(rightParenthesis)
        }
}
