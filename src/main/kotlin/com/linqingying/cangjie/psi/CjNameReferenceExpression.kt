package com.linqingying.cangjie.psi

import com.linqingying.cangjie.lexer.CjTokens.*
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.psi.stubs.CangJieNameReferenceExpressionStub
import com.linqingying.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet



class CjNameReferenceExpression : CjExpressionImplStub<CangJieNameReferenceExpressionStub>, CjSimpleNameExpression {
    constructor(node: ASTNode) : super(node)

    constructor(stub: CangJieNameReferenceExpressionStub) : super(stub, CjStubElementTypes.REFERENCE_EXPRESSION)

    override fun getReferencedName(): String {
        val stub = stub
        if (stub != null) {
            return stub.getReferencedName()
        }
        return CjSimpleNameExpressionImpl.getReferencedNameImpl(this)
    }

    override fun getReferencedNameAsName(): Name {
        return CjSimpleNameExpressionImpl.getReferencedNameAsNameImpl(this)
    }

    override fun getReferencedNameElement(): PsiElement {
        return findChildByType(NAME_REFERENCE_EXPRESSIONS) ?: this
    }

    override fun getIdentifier(): PsiElement? {
        return findChildByType(IDENTIFIER)
    }

    override fun getReferencedNameElementType(): IElementType {
        return CjSimpleNameExpressionImpl.getReferencedNameElementTypeImpl(this)
    }

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R {
        return visitor.visitSimpleNameExpression(this, data)
    }

    val isPlaceholder: Boolean
        get() = getIdentifier()?.text?.equals("_") == true

    companion object {
        private val NAME_REFERENCE_EXPRESSIONS = TokenSet.create(IDENTIFIER, THIS_KEYWORD, SUPER_KEYWORD)
    }
}
