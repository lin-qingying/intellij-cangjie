package com.linqingying.cangjie.psi

import com.linqingying.cangjie.psi.stubs.CangJiePlaceHolderStub
import com.linqingying.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode

class CjConstructorCalleeExpression : CjExpressionImplStub<CangJiePlaceHolderStub<CjConstructorCalleeExpression > > {
    constructor(node: ASTNode) : super(node)

    constructor(stub: CangJiePlaceHolderStub<CjConstructorCalleeExpression >) : super(
        stub,
        CjStubElementTypes.CONSTRUCTOR_CALLEE
    )

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R {
        return visitor.visitConstructorCalleeExpression(this, data)
    }

    @get:IfNotParsed
    val typeReference: CjTypeReference?
        get() = getStubOrPsiChild(CjStubElementTypes.TYPE_REFERENCE)

    @get:IfNotParsed
    val constructorReferenceExpression: CjSimpleNameExpression?
        get() {
            val typeReference = typeReference ?: return null
            val typeElement = typeReference.typeElement as? CjUserType ?: return null
            return typeElement.referenceExpression
        }
}
