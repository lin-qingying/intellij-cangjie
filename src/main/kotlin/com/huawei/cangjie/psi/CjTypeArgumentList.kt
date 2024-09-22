package com.huawei.cangjie.psi

import com.huawei.cangjie.lexer.CjTokens
import com.huawei.cangjie.psi.EditCommaSeparatedListHelper.addItem
import com.huawei.cangjie.psi.psiUtil.getTrailingCommaByClosingElement
import com.huawei.cangjie.psi.stubs.CangJiePlaceHolderStub
import com.huawei.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement

class CjTypeArgumentList : CjElementImplStub<CangJiePlaceHolderStub<CjTypeArgumentList > > {
    constructor(node: ASTNode) : super(node)

    constructor(stub: CangJiePlaceHolderStub<CjTypeArgumentList >) : super(stub, CjStubElementTypes.TYPE_ARGUMENT_LIST)

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R {
        return visitor.visitTypeArgumentList(this, data)
    }

    val arguments: List<CjTypeProjection>
        get() = getStubOrPsiChildrenAsList(CjStubElementTypes.TYPE_PROJECTION)

    fun addArgument(typeArgument: CjTypeProjection): CjTypeProjection {
        return addItem(
            this,
            arguments, typeArgument, CjTokens.LT
        )
    }

    val trailingComma: PsiElement?
        get() = getTrailingCommaByClosingElement(findChildByType(CjTokens.GT))
}
