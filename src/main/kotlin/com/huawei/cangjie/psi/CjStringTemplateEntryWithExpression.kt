package com.huawei.cangjie.psi

import com.huawei.cangjie.psi.stubs.CangJiePlaceHolderWithTextStub
import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IStubElementType

abstract class CjStringTemplateEntryWithExpression : CjStringTemplateEntry {
    constructor(node: ASTNode) : super(node)

    constructor(
        stub: CangJiePlaceHolderWithTextStub<out CjStringTemplateEntryWithExpression >,
        elementType: IStubElementType<*, *>
    ) : super(stub, elementType)

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R {
        return visitor.visitStringTemplateEntryWithExpression(this, data)
    }
}
