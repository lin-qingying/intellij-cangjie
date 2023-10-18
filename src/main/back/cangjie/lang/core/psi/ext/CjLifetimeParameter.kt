package com.huawei.cangjie.lang.core.psi.ext


import com.huawei.cangjie.lang.core.psi.CjElementTypes
import com.huawei.cangjie.lang.core.psi.CjLifetimeParameter
import com.huawei.cangjie.lang.core.psi.CjPsiFactory
import com.huawei.cangjie.lang.core.psi.CjPsiImplUtil
import com.huawei.cangjie.lang.core.stubs.CjLifetimeParameterStub
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.search.SearchScope
import com.intellij.psi.stubs.IStubElementType

//val CangJieLifetimeParameter.bounds: List<CangJieLifetime>
//    get() {
//        val owner = parent?.parent as? CjGenericDeclaration
//        val whereBounds = owner?.wherePreds.orEmpty()
//            .filter { it.lifetime?.reference?.resolve() == this }
//            .flatMap { it.lifetimeParamBounds?.lifetimeList.orEmpty() }
//        return lifetimeParamBounds?.lifetimeList.orEmpty() + whereBounds
//    }




abstract class CjLifetimeParameterImplMixin : CjStubbedNamedElementImpl<CjLifetimeParameterStub>, CjLifetimeParameter {

    constructor(node: ASTNode) : super(node)

    constructor(stub: CjLifetimeParameterStub, nodeType: IStubElementType<*, *>) : super(stub, nodeType)




    override fun getNameIdentifier(): PsiElement =  quoteIdentifier

    override fun setName(name: String): PsiElement? {
        nameIdentifier.replace(CjPsiFactory(project).createQuoteIdentifier(name))
        return this
    }

    override fun getUseScope(): SearchScope =   super.getUseScope()
}
