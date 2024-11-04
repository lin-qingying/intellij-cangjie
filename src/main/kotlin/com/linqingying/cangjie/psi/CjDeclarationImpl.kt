package com.linqingying.cangjie.psi

import com.linqingying.cangjie.CjNodeTypes
import com.linqingying.cangjie.descriptors.DescriptorVisibilities
import com.linqingying.cangjie.descriptors.DescriptorVisibility
import com.linqingying.cangjie.doc.psi.CDoc
import com.linqingying.cangjie.lexer.CjModifierKeywordToken
import com.linqingying.cangjie.psi.psiUtil.addModifier
import com.linqingying.cangjie.psi.psiUtil.findDocComment
import com.linqingying.cangjie.psi.psiUtil.removeModifier
import com.linqingying.cangjie.resolve.ModifiersChecker.Companion.resolveVisibilityFromModifiers
import com.intellij.lang.ASTNode
import com.intellij.psi.util.PsiTreeUtil
import com.linqingying.cangjie.lexer.CjKeywordToken

abstract class CjDeclarationImpl(node: ASTNode) : CjExpressionImpl(node), CjDeclaration {
    override val modifierList : CjModifierList? get() =
          findChildByType(CjNodeTypes.MODIFIER_LIST)


    override fun hasModifier(modifier: CjKeywordToken): Boolean {
        val modifierList: CjModifierList? = modifierList
        return modifierList != null && modifierList.hasModifier(modifier)
    }

    override val expression: CjExpression?
        get() = PsiTreeUtil.getStubChildOfType(
            this,
            CjExpression::class.java
        )
    override val annotations: List<CjAnnotation>
        get() {
            val modifierList: CjModifierList = modifierList ?: return emptyList()
            return modifierList.annotations
        }

    override val annotationEntries: List<CjAnnotationEntry>
        get() {
            val modifierList: CjModifierList = modifierList ?: return emptyList()
            return modifierList.annotationEntries
        }

    override fun addModifier(modifier: CjKeywordToken) {
        addModifier(this, modifier)
    }

    override fun removeModifier(modifier: CjKeywordToken) {
        removeModifier(this, modifier)
    }


    override val modifierVisibility : DescriptorVisibility get() =
          resolveVisibilityFromModifiers(this, DescriptorVisibilities.INTERNAL)



    override val docComment: CDoc?
        get() {
            return findDocComment(this)
        }
}
