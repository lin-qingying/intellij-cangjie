package com.linqingying.cangjie.psi

import com.linqingying.cangjie.name.ClassId
import com.linqingying.cangjie.psi.psiUtil.ClassIdCalculator
import com.linqingying.cangjie.psi.stubs.CangJieTypeAliasStub
import com.linqingying.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode


class CjTypeAlias : CjTypeParameterListOwnerStub<CangJieTypeAliasStub>, CjNamedDeclaration,CjClassLikeDeclaration {
    constructor(node: ASTNode) : super(node)
    constructor(stub: CangJieTypeAliasStub) : super(stub, CjStubElementTypes.TYPEALIAS)

    override fun getClassId(): ClassId? {
        stub?.let { return it.getClassId() }
        return ClassIdCalculator.calculateClassId(this)
    }


}


