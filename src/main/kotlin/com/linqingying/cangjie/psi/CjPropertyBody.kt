package com.linqingying.cangjie.psi

import com.linqingying.cangjie.psi.stubs.CangJiePlaceHolderStub
import com.linqingying.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode
import com.intellij.psi.util.PsiTreeUtil

class CjPropertyBody: CjElementImplStub<CangJiePlaceHolderStub<CjPropertyBody>>, CjDeclarationContainer {



    constructor(node: ASTNode) : super(node)




    constructor(stub: CangJiePlaceHolderStub<CjPropertyBody>) : super(stub, CjStubElementTypes.PROPERTY_BODY)

    override val declarations: List<CjDeclaration>
        get() = stub?.getChildrenByType(CjFile.FILE_DECLARATION_TYPES, CjDeclaration.ARRAY_FACTORY)?.toList()
            ?: PsiTreeUtil.getChildrenOfTypeAsList(this, CjDeclaration::class.java)


}
