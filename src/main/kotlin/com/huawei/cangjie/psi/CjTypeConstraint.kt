package com.huawei.cangjie.psi

import com.huawei.cangjie.psi.stubs.CangJiePlaceHolderStub
import com.huawei.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode

class CjTypeConstraint : CjElementImplStub<CangJiePlaceHolderStub<CjTypeConstraint > >, CjElement {
    constructor(node: ASTNode) : super(node)

    constructor(stub: CangJiePlaceHolderStub<CjTypeConstraint >) : super(stub, CjStubElementTypes.TYPE_CONSTRAINT)

    override fun toString(): String {
        return node.elementType.toString()
    }

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R {
        return visitor.visitTypeConstraint(this, data)
    }

    @get:IfNotParsed
    val subjectTypeParameterName: CjSimpleNameExpression?
        get() = getStubOrPsiChild(CjStubElementTypes.REFERENCE_EXPRESSION)

    @get:IfNotParsed
    val boundTypeReference: CjTypeReference?
        get() = getStubOrPsiChild(CjStubElementTypes.TYPE_REFERENCE)

    @get:IfNotParsed
    val boundTypeReferences: List<CjTypeReference>
        get() = getStubOrPsiChildrenAsList(
            CjStubElementTypes.TYPE_REFERENCE
        )
}
