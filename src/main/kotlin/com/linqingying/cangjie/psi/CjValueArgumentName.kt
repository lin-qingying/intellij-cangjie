package com.linqingying.cangjie.psi

import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.psi.stubs.CangJiePlaceHolderStub
import com.linqingying.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode

class CjValueArgumentName : CjElementImplStub<CangJiePlaceHolderStub<CjValueArgumentName > >, ValueArgumentName {
    constructor(node: ASTNode) : super(node)

    constructor(stub: CangJiePlaceHolderStub<CjValueArgumentName >) : super(
        stub,
        CjStubElementTypes.VALUE_ARGUMENT_NAME
    )

    override val referenceExpression: CjSimpleNameExpression
        get() = getStubOrPsiChild(CjStubElementTypes.REFERENCE_EXPRESSION)!!

    override val asName: Name
        get() = referenceExpression.getReferencedNameAsName()
}
