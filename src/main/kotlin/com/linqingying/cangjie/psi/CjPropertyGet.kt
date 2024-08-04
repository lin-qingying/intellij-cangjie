package com.linqingying.cangjie.psi

import com.linqingying.cangjie.psi.stubs.CangJiePlaceHolderStub
import com.linqingying.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode
import com.intellij.psi.util.PsiTreeUtil

class CjPropertyGet:CjElementImplStub<CangJiePlaceHolderStub<CjPropertyGet>>, CjDeclarationContainer  {

    constructor(node: ASTNode) : super(node)




    constructor(stub: CangJiePlaceHolderStub<CjPropertyGet>) : super(stub, CjStubElementTypes.PROPERTY_GET)

    override val declarations: List<CjDeclaration>
        get() = stub?.getChildrenByType(CjFile.FILE_DECLARATION_TYPES, CjDeclaration.ARRAY_FACTORY)?.toList()
            ?: PsiTreeUtil.getChildrenOfTypeAsList(this, CjDeclaration::class.java)

}
