package com.huawei.cangjie.psi

import com.huawei.cangjie.psi.stubs.CangJieConstructorStub
import com.huawei.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement


//终结器函数
class CjEndSecondaryConstructor: CjConstructor<CjEndSecondaryConstructor> {

    constructor(node: ASTNode) : super(node)
    constructor(stub: CangJieConstructorStub<CjEndSecondaryConstructor>) : super(
        stub,
        CjStubElementTypes.END_SECONDARY_CONSTRUCTOR
    )

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R = visitor.visitEndSecondaryConstructor(this, data)
    override fun getConstructorKeyword() = notNullChild<PsiElement>(super.getConstructorKeyword())

    override fun getContainingTypeStatement() = parent?.parent as CjTypeStatement



    override fun getInitKeyword() = notNullChild<PsiElement>(super.getInitKeyword())


}
