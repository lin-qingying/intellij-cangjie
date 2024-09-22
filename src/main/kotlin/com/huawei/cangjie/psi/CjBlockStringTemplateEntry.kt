package com.huawei.cangjie.psi

import com.huawei.cangjie.psi.stubs.CangJiePlaceHolderWithTextStub
import com.huawei.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode

class CjBlockStringTemplateEntry : CjStringTemplateEntryWithExpression {
    constructor(node: ASTNode) : super(node)

    constructor(stub: CangJiePlaceHolderWithTextStub<CjBlockStringTemplateEntry >) : super(
        stub,
        CjStubElementTypes.LONG_STRING_TEMPLATE_ENTRY
    )

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R {
        return visitor.visitBlockStringTemplateEntry(this, data)
    }
}
