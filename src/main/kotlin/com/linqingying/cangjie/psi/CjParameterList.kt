package com.linqingying.cangjie.psi

import com.linqingying.cangjie.lexer.CjTokens
import com.linqingying.cangjie.psi.EditCommaSeparatedListHelper.addItem
import com.linqingying.cangjie.psi.EditCommaSeparatedListHelper.addItemAfter
import com.linqingying.cangjie.psi.EditCommaSeparatedListHelper.addItemBefore
import com.linqingying.cangjie.psi.EditCommaSeparatedListHelper.removeItem
import com.linqingying.cangjie.psi.psiUtil.getTrailingCommaByClosingElement
import com.linqingying.cangjie.psi.stubs.CangJiePlaceHolderStub
import com.linqingying.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement

class CjParameterList : CjElementImplStub<CangJiePlaceHolderStub<CjParameterList>> {
    constructor(node: ASTNode) : super(node)

    constructor(stub: CangJiePlaceHolderStub<CjParameterList>) : super(stub, CjStubElementTypes.VALUE_PARAMETER_LIST)


    override fun toString(): String {
        return node.elementType.toString()
    }

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R {
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
            parameters, parameter
        )
    }

    fun addParameterBefore(parameter: CjParameter, anchor: CjParameter?): CjParameter {
        return addItemBefore(
            this,
            parameters, parameter, anchor
        )
    }

    fun addParameterAfter(parameter: CjParameter, anchor: CjParameter?): CjParameter {
        return addItemAfter(
            this,
            parameters, parameter, anchor
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
