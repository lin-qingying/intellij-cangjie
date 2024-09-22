package com.huawei.cangjie.psi

import com.huawei.cangjie.lexer.CjTokens
import com.huawei.cangjie.psi.EditCommaSeparatedListHelper.addItem
import com.huawei.cangjie.psi.psiUtil.getTrailingCommaByClosingElement
import com.huawei.cangjie.psi.stubs.CangJiePlaceHolderStub
import com.huawei.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement

class CjTypeParameterList : CjElementImplStub<CangJiePlaceHolderStub<CjTypeParameterList > > {
    constructor(node: ASTNode) : super(node)

    constructor(stub: CangJiePlaceHolderStub<CjTypeParameterList >) : super(
        stub,
        CjStubElementTypes.TYPE_PARAMETER_LIST
    )

    override fun toString(): String {
        return node.elementType.toString()
    }

    val parameters: List<CjTypeParameter>
        get() = getStubOrPsiChildrenAsList(CjStubElementTypes.TYPE_PARAMETER)

    fun addParameter(typeParameter: CjTypeParameter): CjTypeParameter {
        return addItem(
            this,
            parameters, typeParameter, CjTokens.LT
        )
    }

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R {
        return visitor.visitTypeParameterList(this, data)
    }

    val trailingComma: PsiElement?
        get() = getTrailingCommaByClosingElement(findChildByType(CjTokens.GT))
}
