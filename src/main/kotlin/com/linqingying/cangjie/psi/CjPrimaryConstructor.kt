package com.linqingying.cangjie.psi

import com.linqingying.cangjie.lexer.CjModifierKeywordToken
import com.linqingying.cangjie.lexer.CjTokens
import com.linqingying.cangjie.psi.psiUtil.addModifier
import com.linqingying.cangjie.psi.stubs.CangJieConstructorStub
import com.linqingying.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.linqingying.cangjie.lexer.CjKeywordToken

//主构造函数
class CjPrimaryConstructor : CjConstructor<CjPrimaryConstructor> {
    constructor(node: ASTNode) : super(node)
    constructor(stub: CangJieConstructorStub<CjPrimaryConstructor>) : super(stub, CjStubElementTypes.PRIMARY_CONSTRUCTOR)

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R  = visitor.visitPrimaryConstructor(this, data)

    override fun getContainingTypeStatement() = parent?.parent as CjTypeStatement

    private fun getOrCreateConstructorKeyword(): PsiElement {
        return getInitKeyword() ?: addBefore(CjPsiFactory(project).createConstructorKeyword(), valueParameterList!!)
    }

    private fun removeRedundantConstructorKeywordAndSpace() {
        getInitKeyword()?.delete()
        if (prevSibling is PsiWhiteSpace) {
            prevSibling.delete()
        }
    }

    override fun getInitKeyword(): PsiElement? {
        return identifier
    }
    override fun addModifier(modifier: CjKeywordToken) {
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

    override fun getIdentifyingElement(): PsiElement? {
        return identifier
    }
      val identifier: PsiElement?  get() = findChildByType(CjTokens.IDENTIFIER)
    override fun getName(): String? {


        return identifier ?.text
    }
    override fun removeModifier(modifier: CjKeywordToken) {
        super.removeModifier(modifier)
        if (modifierList == null) {
            removeRedundantConstructorKeywordAndSpace()
        }
    }
    override fun toString(): String {
        return node.elementType.toString()
    }

}
