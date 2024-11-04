package com.linqingying.cangjie.psi

import com.linqingying.cangjie.psi.stubs.CangJieConstructorStub
import com.linqingying.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement

//从构造函数

class CjSecondaryConstructor : CjConstructor<CjSecondaryConstructor> {

    constructor(node: ASTNode) : super(node)
    constructor(stub: CangJieConstructorStub<CjSecondaryConstructor>) : super(
        stub,
        CjStubElementTypes.SECONDARY_CONSTRUCTOR
    )

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R = visitor.visitSecondaryConstructor(this, data)
    override fun getConstructorKeyword() = notNullChild<PsiElement>(super.getConstructorKeyword())

    override fun getContainingTypeStatement() = parent?.parent as CjTypeStatement



    override fun getInitKeyword() = notNullChild<PsiElement>(super.getInitKeyword())


}
