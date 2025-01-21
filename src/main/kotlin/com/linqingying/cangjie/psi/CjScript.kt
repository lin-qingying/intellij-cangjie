package com.linqingying.cangjie.psi

import com.intellij.lang.ASTNode
import com.intellij.psi.util.PsiTreeUtil
import com.linqingying.cangjie.name.FqName
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.name.NameUtils
import com.linqingying.cangjie.psi.stubs.CangJieScriptStub
import com.linqingying.cangjie.psi.stubs.elements.CjStubElementTypes

class CjScript : CjNamedDeclarationStub<CangJieScriptStub>, CjDeclarationContainer {
    constructor(node: ASTNode) : super(node)

    constructor(stub: CangJieScriptStub) : super(stub, CjStubElementTypes.CJ_SCRIPT)

    override val fqName: FqName
        get() {

            if (stub != null) {
                return stub!!.getFqName()
            }
            val containingCjFile: CjFile = getContainingCjFile()
            val fileBasedName: Name = NameUtils.getScriptNameForFile(containingCjFile.name)
            return containingCjFile.packageFqName.child(fileBasedName)
        }


    override fun getName(): String {
        return fqName.shortName().asString()
    }
    val blockExpression: CjBlockExpression
        get() = findNotNullChildByClass(CjBlockExpression::class.java)

    override val declarations: List<CjDeclaration>
        get() = PsiTreeUtil.getChildrenOfTypeAsList(blockExpression, CjDeclaration::class.java)

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R {
        return visitor.visitScript(this, data)
    }
}
