package com.huawei.cangjie.psi

import com.huawei.cangjie.psi.stubs.CangJieConstructorStub
import com.huawei.cangjie.psi.stubs.elements.CjStubElementTypes
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

    override fun getBodyExpression(): CjInitBlockExpression? {
        val stub = stub
        if (stub != null) {
            if (stub.hasBody() == false) {
                return null
            }
            if (getContainingCjFile().isCompiled) {
                return null
            }
        }
        return findChildByClass(CjInitBlockExpression::class.java)
    }

    override fun getInitKeyword() = notNullChild<PsiElement>(super.getInitKeyword())

    fun getDelegationCall(): CjConstructorDelegationCall = bodyExpression!!.getDelegationCall()

    fun getDelegationCallOrNull(): CjConstructorDelegationCall? = bodyExpression?.getDelegationCallOrNull()

    fun hasImplicitDelegationCall(): Boolean = getDelegationCall().isImplicit

    fun replaceImplicitDelegationCallWithExplicit(isThis: Boolean): CjConstructorDelegationCall {
     return bodyExpression!!.replaceImplicitDelegationCallWithExplicit(isThis)
    }
}
