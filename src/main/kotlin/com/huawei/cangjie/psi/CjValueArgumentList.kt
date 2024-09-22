package com.huawei.cangjie.psi

import com.huawei.cangjie.lexer.CjTokens
import com.huawei.cangjie.psi.EditCommaSeparatedListHelper.addItem
import com.huawei.cangjie.psi.EditCommaSeparatedListHelper.addItemAfter
import com.huawei.cangjie.psi.EditCommaSeparatedListHelper.addItemBefore
import com.huawei.cangjie.psi.EditCommaSeparatedListHelper.removeItem
import com.huawei.cangjie.psi.psiUtil.getTrailingCommaByClosingElement
import com.huawei.cangjie.psi.stubs.CangJiePlaceHolderStub
import com.huawei.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement

class CjValueArgumentList : CjElementImplStub<CangJiePlaceHolderStub<CjValueArgumentList  > > {
    constructor(node: ASTNode) : super(node)

    constructor(stub: CangJiePlaceHolderStub<CjValueArgumentList >) : super(
        stub,
        CjStubElementTypes.VALUE_ARGUMENT_LIST
    )

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R {
        return visitor.visitValueArgumentList(this, data)
    }

    val arguments: List<CjValueArgument>
        get() = getStubOrPsiChildrenAsList(
            CjStubElementTypes.VALUE_ARGUMENT
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
            arguments, argument
        )
    }

    fun addArgumentAfter(argument: CjValueArgument, anchor: CjValueArgument?): CjValueArgument {
        return addItemAfter(
            this,
            arguments, argument, anchor
        )
    }

    fun addArgumentBefore(argument: CjValueArgument, anchor: CjValueArgument?): CjValueArgument {
        return addItemBefore(
            this,
            arguments, argument, anchor
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
