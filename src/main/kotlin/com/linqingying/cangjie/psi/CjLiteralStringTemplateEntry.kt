package com.linqingying.cangjie.psi

import com.linqingying.cangjie.psi.stubs.CangJiePlaceHolderWithTextStub
import com.linqingying.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode

class CjLiteralStringTemplateEntry : CjStringTemplateEntry {
    constructor(node: ASTNode) : super(node)

    constructor(stub: CangJiePlaceHolderWithTextStub<CjLiteralStringTemplateEntry >) : super(
        stub,
        CjStubElementTypes.LITERAL_STRING_TEMPLATE_ENTRY
    )

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R {
        return visitor.visitLiteralStringTemplateEntry(this, data)
    }

    override fun getText(): String {
        val stub = stub
        if (stub != null) {
            return stub.text()
        }

        return super.getText()
    }
}
