package com.huawei.cangjie.psi

import com.huawei.cangjie.psi.stubs.CangJieConstructorStub
import com.huawei.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement


class CjSecondaryConstructor : CjConstructor<CjSecondaryConstructor> {

    constructor(node: ASTNode) : super(node)
    constructor(stub: CangJieConstructorStub<CjSecondaryConstructor>) : super(stub, CjStubElementTypes.SECONDARY_CONSTRUCTOR)

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?) = visitor.visitSecondaryConstructor(this, data)
    override fun getConstructorKeyword() = notNullChild<PsiElement>(super.getConstructorKeyword())

    override fun getContainingClassOrStruct() = parent?.parent as CjClassOrStruct

    override fun getBodyExpression(): CjBlockExpression? {
        val stub = stub
        if (stub != null) {
            if (stub.hasBody() == false) {
                return null
            }
            if (getContainingCjFile().isCompiled) {
                return null
            }
        }
        return findChildByClass(CjBlockExpression::class.java)
    }

    override fun getInitKeyword() = notNullChild<PsiElement>(super.getInitKeyword())

    fun getDelegationCall(): CjConstructorDelegationCall = findNotNullChildByClass(CjConstructorDelegationCall::class.java)

    fun getDelegationCallOrNull(): CjConstructorDelegationCall? = findChildByClass(CjConstructorDelegationCall::class.java)

    fun hasImplicitDelegationCall(): Boolean = getDelegationCall().isImplicit

    fun replaceImplicitDelegationCallWithExplicit(isThis: Boolean): CjConstructorDelegationCall {
        val psiFactory = CjPsiFactory(project)
        val current = getDelegationCall()

        assert(current.isImplicit) { "Method should not be called with explicit delegation call: " + text }
        current.delete()

        val colon = addAfter(psiFactory.createColon(), valueParameterList)

        val delegationName = if (isThis) "this" else "super"

        return addAfter(psiFactory.creareDelegatedSuperTypeEntry(delegationName + "()"), colon) as CjConstructorDelegationCall
    }
}
