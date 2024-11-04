package com.linqingying.cangjie.psi

import com.linqingying.cangjie.CjNodeTypes
import com.linqingying.cangjie.lexer.CjTokens
import com.linqingying.cangjie.name.ClassId
import com.linqingying.cangjie.psi.psiUtil.ClassIdCalculator
import com.linqingying.cangjie.psi.stubs.CangJiePlaceHolderStub
import com.linqingying.cangjie.psi.stubs.CangJieTypeAliasStub
import com.linqingying.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode
import com.intellij.navigation.ItemPresentationProviders
import com.intellij.psi.PsiElement


class CjTypeAlias : CjTypeParameterListOwnerStub<CangJieTypeAliasStub>, CjNamedDeclaration, CjClassLikeDeclaration {
    constructor(node: ASTNode) : super(node)
    constructor(stub: CangJieTypeAliasStub) : super(stub, CjStubElementTypes.TYPEALIAS)

    override fun getClassId(): ClassId? {
        stub?.let { return it.getClassId() }
        return ClassIdCalculator.calculateClassId(this)
    }

    override fun toString(): String {
        return super.toString()
    }
    override fun getPresentation() = ItemPresentationProviders.getItemPresentation(this)

    @IfNotParsed
    fun getTypeAliasKeyword(): PsiElement? =
        findChildByType(CjTokens.TYPE_KEYWORD)

    override fun <R : Any?, D : Any?> accept(visitor: CjVisitor<R, D>, data: D?): R  {
        return visitor.visitTypeAlias(this, data)
    }


    @IfNotParsed
    fun getTypeReference(): CjTypeReference? {
        return if (stub != null) {
            val typeReferences =
                getStubOrPsiChildrenAsList<CjTypeReference, CangJiePlaceHolderStub<CjTypeReference>>(CjStubElementTypes.TYPE_REFERENCE)
            typeReferences[0]
        } else {
            findChildByType(CjNodeTypes.TYPE_REFERENCE)
        }
    }

}


