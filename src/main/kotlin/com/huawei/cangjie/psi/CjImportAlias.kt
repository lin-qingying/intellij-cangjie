package com.huawei.cangjie.psi

import com.huawei.cangjie.lexer.CjTokens
import com.huawei.cangjie.psi.psiUtil.startOffset
import com.huawei.cangjie.psi.stubs.CangJieImportAliasStub
import com.huawei.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.search.LocalSearchScope


class CjImportAlias : CjElementImplStub<CangJieImportAliasStub>, PsiNameIdentifierOwner {
    @Suppress("unused")
    constructor(node: ASTNode) : super(node)
    @Suppress("unused")
    constructor(stub: CangJieImportAliasStub) : super(stub, CjStubElementTypes.IMPORT_ALIAS)

    override fun <R : Any?, D : Any?> accept(visitor: CjVisitor<R, D>, data: D?): R  {
        return visitor.visitImportAlias(this, data)
    }

    val importDirective: CjImportDirective?
        get() = parent as? CjImportDirective

    override fun getName() = stub?.getName() ?: nameIdentifier?.text

    override fun setName(name: String): PsiElement {
        nameIdentifier?.replace(CjPsiFactory(project).createNameIdentifier(name))
        return this
    }

    override fun getNameIdentifier(): PsiElement? = findChildByType(CjTokens.IDENTIFIER)

    override fun getTextOffset() = nameIdentifier?.textOffset ?: startOffset

    override fun getUseScope() = LocalSearchScope(containingFile)
}
