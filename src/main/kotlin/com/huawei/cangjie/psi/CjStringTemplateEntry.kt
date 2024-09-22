package com.huawei.cangjie.psi

import com.huawei.cangjie.psi.stubs.CangJiePlaceHolderWithTextStub
import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IStubElementType


abstract class CjStringTemplateEntry : CjElementImplStub<CangJiePlaceHolderWithTextStub<out CjStringTemplateEntry > > {
    constructor(node: ASTNode) : super(node)

    constructor(
        stub: CangJiePlaceHolderWithTextStub<out CjStringTemplateEntry >,
        elementType: IStubElementType<*, *>
    ) : super(stub, elementType)

    val expression: CjExpression?
        get() = findChildByClass(CjExpression::class.java)

    companion object {
        val EMPTY_ARRAY: Array<CjStringTemplateEntry?> = arrayOfNulls(0)
    }
}
