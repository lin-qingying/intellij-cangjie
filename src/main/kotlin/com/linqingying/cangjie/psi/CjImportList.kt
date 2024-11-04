package com.linqingying.cangjie.psi

import com.linqingying.cangjie.psi.stubs.CangJiePlaceHolderStub
import com.linqingying.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode

class CjImportList : CjElementImplStub<CangJiePlaceHolderStub<CjImportList > > {
    constructor(node: ASTNode) : super(node)

    constructor(stub: CangJiePlaceHolderStub<CjImportList >) : super(stub, CjStubElementTypes.IMPORT_LIST)

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R {
        return visitor.visitImportList(this, data)
    }

    val imports: List<CjImportDirective>
        get() = getStubOrPsiChildrenAsList(CjStubElementTypes.IMPORT_DIRECTIVE)
}
