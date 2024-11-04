package com.linqingying.cangjie.psi

import com.linqingying.cangjie.psi.stubs.CangJiePlaceHolderWithTextStub
import com.linqingying.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.text.StringUtil

class CjEscapeStringTemplateEntry : CjStringTemplateEntry {
    constructor(node: ASTNode) : super(node)

    constructor(stub: CangJiePlaceHolderWithTextStub<CjEscapeStringTemplateEntry >) : super(
        stub,
        CjStubElementTypes.ESCAPE_STRING_TEMPLATE_ENTRY
    )

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R {
        return visitor.visitEscapeStringTemplateEntry(this, data)
    }

    val unescapedValue: String
        get() = StringUtil.unescapeStringCharacters(text)
}
