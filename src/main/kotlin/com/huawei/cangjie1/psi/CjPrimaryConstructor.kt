package com.huawei.cangjie1.psi

import com.huawei.cangjie1.lexer.CjModifierKeywordToken
import com.huawei.cangjie1.lexer.CjTokens
import com.huawei.cangjie1.psi.stubs.CangJieConstructorStub
import com.huawei.cangjie1.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace

import com.huawei.cangjie1.psi.psiUtil.addModifier

class CjPrimaryConstructor : CjConstructor<CjPrimaryConstructor> {
    constructor(node: ASTNode) : super(node)
    constructor(stub: CangJieConstructorStub<CjPrimaryConstructor>) : super(stub, CjStubElementTypes.PRIMARY_CONSTRUCTOR)

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D) = visitor.visitPrimaryConstructor(this, data)

    override fun getContainingClassOrObject() = parent as CjClassOrObject

    private fun getOrCreateConstructorKeyword(): PsiElement {
        return getConstructorKeyword() ?: addBefore(CjPsiFactory(project).createConstructorKeyword(), valueParameterList!!)
    }

    fun removeRedundantConstructorKeywordAndSpace() {
        getConstructorKeyword()?.delete()
        if (prevSibling is PsiWhiteSpace) {
            prevSibling.delete()
        }
    }

    override fun addModifier(modifier: CjModifierKeywordToken) {
        val modifierList = modifierList
        if (modifierList != null) {
            addModifier(modifierList, modifier)
            if (this.modifierList == null) {
                getConstructorKeyword()?.delete()
            }
        } else {
            if (modifier == CjTokens.PUBLIC_KEYWORD) return
            val newModifierList = CjPsiFactory(project).createModifierList(modifier)
            addBefore(newModifierList, getOrCreateConstructorKeyword())
        }
    }

    override fun removeModifier(modifier: CjModifierKeywordToken) {
        super.removeModifier(modifier)
        if (modifierList == null) {
            removeRedundantConstructorKeywordAndSpace()
        }
    }


}
