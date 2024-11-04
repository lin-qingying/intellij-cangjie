package com.linqingying.cangjie.psi

import com.linqingying.cangjie.lexer.CjTokens
import com.linqingying.cangjie.psi.stubs.CangJieTypeParameterStub
import com.linqingying.cangjie.psi.stubs.elements.CjStubElementTypes
import com.linqingying.cangjie.types.Variance
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.SearchScope
import com.intellij.psi.util.PsiTreeUtil

class CjTypeParameter : CjNamedDeclarationStub<CangJieTypeParameterStub > {
    constructor(node: ASTNode) : super(node)

    constructor(stub: CangJieTypeParameterStub) : super(stub, CjStubElementTypes.TYPE_PARAMETER)

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R {
        return visitor.visitTypeParameter(this, data)
    }


    override fun toString(): String {
        return node.elementType.toString()
    }

    val variance: Variance
        get() =//        CangJieTypeParameterStub stub = getStub();
//        if (stub != null) {
//
////            if (stub.isInVariance()) return Variance.IN_VARIANCE;
//            return Variance.INVARIANT;
//        }
//
//        CjModifierList modifierList = getModifierList();
//        if (modifierList == null) return Variance.INVARIANT;
            Variance.INVARIANT

    fun setExtendsBound(typeReference: CjTypeReference?): CjTypeReference? {
        val currentExtendsBound = extendsBound
        if (currentExtendsBound != null) {
            if (typeReference == null) {
                val colon = findChildByType<PsiElement>(CjTokens.COLON)
                colon?.delete()
                currentExtendsBound.delete()
                return null
            }
            return currentExtendsBound.replace(typeReference) as CjTypeReference
        }

        if (typeReference != null) {
            val colon = addAfter(CjPsiFactory(project).createColon(), nameIdentifier)
            return addAfter(typeReference, colon) as CjTypeReference
        }

        return null
    }

    val extendsBound: CjTypeReference?
        get() = getStubOrPsiChild(CjStubElementTypes.TYPE_REFERENCE)
    val extendsBounds: List<CjTypeReference>
        get() = getStubOrPsiChildrenAsList(
            CjStubElementTypes.TYPE_REFERENCE
        )

    override fun getUseScope(): SearchScope {
        val owner = PsiTreeUtil.getParentOfType(
            this,
            CjTypeParameterListOwner::class.java
        )
        return LocalSearchScope(owner ?: this)
    }
}
