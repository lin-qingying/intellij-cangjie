package com.linqingying.cangjie.psi

import com.linqingying.cangjie.psi.stubs.CangJiePlaceHolderWithTextStub
import com.linqingying.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode

class CjSimpleNameStringTemplateEntry : CjStringTemplateEntryWithExpression {
    constructor(node: ASTNode) : super(node)

    constructor(stub: CangJiePlaceHolderWithTextStub<CjSimpleNameStringTemplateEntry >) : super(
        stub,
        CjStubElementTypes.SHORT_STRING_TEMPLATE_ENTRY
    )

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R {
        return visitor.visitSimpleNameStringTemplateEntry(this, data)
    }
}
