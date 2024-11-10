package com.linqingying.cangjie.psi

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.linqingying.cangjie.lexer.CjTokens
import com.linqingying.cangjie.psi.stubs.CangJieCatchParameterStub
import com.linqingying.cangjie.psi.stubs.elements.CjStubElementTypes

class CjCatchParameter : CjNamedDeclarationStub<CangJieCatchParameterStub>,CjParameterBase {

    constructor(node: ASTNode) : super(node)
    constructor(stub: CangJieCatchParameterStub) : super(stub, CjStubElementTypes.CATCH_PARAMETER)

    val typeReferences: List<CjTypeReference>
        get() = getStubOrPsiChildrenAsList(CjStubElementTypes.TYPE_REFERENCE)

    override fun hasLetOrVar(): Boolean {
        return false
    }

    override fun hasDefaultValue(): Boolean {
       return false
    }

    override val valueParameterList: CjParameterList? = null
    override val valueParameters: List<CjParameter> = emptyList()
    override val receiverTypeReference: CjTypeReference? get() = typeReference
    override val typeReference: CjTypeReference? get() =  null

    override fun setTypeReference(typeRef: CjTypeReference?): CjTypeReference? {
       return typeReference
    }

    override val colon: PsiElement?
        get() = findChildByType(CjTokens.COLON)
    override val typeParameterList: CjTypeParameterList? = null
    override val typeConstraintList: CjTypeConstraintList?= null
    override val typeConstraints: List<CjTypeConstraint> = emptyList()
    override val typeParameters: List<CjTypeParameter> = emptyList()
    override val letOrVarKeyword: PsiElement? = null
    val equalsToken: PsiElement?
        get() = findChildByType(CjTokens.EQ)



}
