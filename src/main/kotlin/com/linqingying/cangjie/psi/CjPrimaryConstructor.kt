package com.linqingying.cangjie.psi

import com.linqingying.cangjie.lexer.CjModifierKeywordToken
import com.linqingying.cangjie.lexer.CjTokens
import com.linqingying.cangjie.psi.psiUtil.addModifier
import com.linqingying.cangjie.psi.stubs.CangJieConstructorStub
import com.linqingying.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace

class CjPrimaryConstructor : CjConstructor<CjPrimaryConstructor> {
    constructor(node: ASTNode) : super(node)
    constructor(stub: CangJieConstructorStub<CjPrimaryConstructor>) : super(stub, CjStubElementTypes.PRIMARY_CONSTRUCTOR)

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?) = visitor.visitPrimaryConstructor(this, data)

    override fun getContainingClassOrStruct() = parent as CjClassOrStruct

    private fun getOrCreateConstructorKeyword(): PsiElement {
        return getInitKeyword() ?: addBefore(CjPsiFactory(project).createConstructorKeyword(), valueParameterList!!)
    }

    fun removeRedundantConstructorKeywordAndSpace() {
        getInitKeyword()?.delete()
        if (prevSibling is PsiWhiteSpace) {
            prevSibling.delete()
        }
    }

    override fun addModifier(modifier: CjModifierKeywordToken) {
        val modifierList = modifierList
        if (modifierList != null) {
            addModifier(modifierList, modifier)
            if (this.modifierList == null) {
                getInitKeyword()?.delete()
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
    override fun toString(): String {
        return node.elementType.toString()
    }

}
