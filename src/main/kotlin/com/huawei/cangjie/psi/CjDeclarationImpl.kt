package com.huawei.cangjie.psi

import com.huawei.cangjie.CjNodeTypes
import com.huawei.cangjie.descriptors.DescriptorVisibilities
import com.huawei.cangjie.descriptors.DescriptorVisibility
import com.huawei.cangjie.doc.psi.CDoc
import com.huawei.cangjie.lexer.CjModifierKeywordToken
import com.huawei.cangjie.psi.psiUtil.addModifier
import com.huawei.cangjie.psi.psiUtil.findDocComment
import com.huawei.cangjie.psi.psiUtil.removeModifier
import com.huawei.cangjie.resolve.ModifiersChecker.Companion.resolveVisibilityFromModifiers
import com.intellij.lang.ASTNode
import com.intellij.psi.util.PsiTreeUtil

abstract class CjDeclarationImpl(node: ASTNode) : CjExpressionImpl(node), CjDeclaration {
    override val modifierList : CjModifierList? get() =
          findChildByType(CjNodeTypes.MODIFIER_LIST)


    override fun hasModifier(modifier: CjModifierKeywordToken): Boolean {
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

    override fun addModifier(modifier: CjModifierKeywordToken) {
        addModifier(this, modifier)
    }

    override fun removeModifier(modifier: CjModifierKeywordToken) {
        removeModifier(this, modifier)
    }


    override val modifierVisibility : DescriptorVisibility get() =
          resolveVisibilityFromModifiers(this, DescriptorVisibilities.INTERNAL)



    override val docComment: CDoc?
        get() {
            return findDocComment(this)
        }
}
