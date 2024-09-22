package com.huawei.cangjie.psi

import com.huawei.cangjie.name.Name
import com.huawei.cangjie.psi.stubs.CangJiePlaceHolderStub
import com.huawei.cangjie.psi.stubs.elements.CjStubElementTypes
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
