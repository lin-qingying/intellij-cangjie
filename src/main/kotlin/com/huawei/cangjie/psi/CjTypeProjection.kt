package com.huawei.cangjie.psi

import com.huawei.cangjie.lexer.CjTokens
import com.huawei.cangjie.psi.stubs.CangJieTypeProjectionStub
import com.huawei.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement

class CjTypeProjection : CjModifierListOwnerStub<CangJieTypeProjectionStub > {
    constructor(node: ASTNode) : super(node)

    constructor(stub: CangJieTypeProjectionStub) : super(stub, CjStubElementTypes.TYPE_PROJECTION)

    val projectionKind: CjProjectionKind
        get() {
            val stub = stub
            if (stub != null) {
                return stub.getProjectionKind()
            }

            val projectionToken = projectionToken
            val token =
                projectionToken?.node?.elementType
            for (projectionKind in CjProjectionKind.entries) {
                if (projectionKind.getToken() === token) {
                    return projectionKind
                }
            }
            throw IllegalStateException(projectionToken!!.text)
        }

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R {
        return visitor.visitTypeProjection(this, data)
    }

    val typeReference: CjTypeReference?
        get() = getStubOrPsiChild(CjStubElementTypes.TYPE_REFERENCE)

    val projectionToken: PsiElement?
        get() {
            val star = findChildByType<PsiElement>(CjTokens.MUL)
            if (star != null) {
                return star
            }

            val modifierList = modifierList
            if (modifierList != null) {
                val element = modifierList.getModifier(CjTokens.IN_KEYWORD)
                return element
            }

            return null
        }
}
