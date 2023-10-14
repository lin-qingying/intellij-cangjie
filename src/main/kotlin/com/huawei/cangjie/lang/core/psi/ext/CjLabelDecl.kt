package com.huawei.cangjie.lang.core.psi.ext




import com.huawei.cangjie.lang.core.psi.CjLabelDecl
import com.huawei.cangjie.lang.core.psi.CjPsiFactory
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType

val CjLabelDecl.owner: CjLabeledExpression
    get() = parent as CjLabeledExpression

abstract class CjLabelDeclImplMixin(type: IElementType) : CjNamedElementImpl(type), CjLabelDecl {
    override fun getNameIdentifier(): PsiElement? = quoteIdentifier

    override fun setName(name: String): PsiElement? {
        nameIdentifier?.replace(CjPsiFactory(project).createQuoteIdentifier(name))
        return this
    }
}
