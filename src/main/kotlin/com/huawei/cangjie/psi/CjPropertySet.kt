package com.huawei.cangjie.psi

import com.huawei.cangjie.psi.stubs.CangJiePlaceHolderStub
import com.huawei.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode
import com.intellij.psi.util.PsiTreeUtil

class CjPropertySet:CjElementImplStub<CangJiePlaceHolderStub<CjPropertySet>>, CjDeclarationContainer  {


    constructor(node: ASTNode) : super(node)




    constructor(stub: CangJiePlaceHolderStub<CjPropertySet>) : super(stub, CjStubElementTypes.PROPERTY_SET)

    override val declarations: List<CjDeclaration>
        get() = stub?.getChildrenByType(CjFile.FILE_DECLARATION_TYPES, CjDeclaration.ARRAY_FACTORY)?.toList()
            ?: PsiTreeUtil.getChildrenOfTypeAsList(this, CjDeclaration::class.java)

}
