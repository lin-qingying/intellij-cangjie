package com.huawei.cangjie.psi

import com.huawei.cangjie.descriptors.DescriptorVisibility
import com.huawei.cangjie.lexer.CjModifierKeywordToken
import com.intellij.psi.PsiElement

interface CjModifierListOwner : PsiElement, CjAnnotated {

    val modifierList: CjModifierList?

    fun hasModifier(modifier: CjModifierKeywordToken): Boolean

    fun addModifier(modifier: CjModifierKeywordToken)

    fun removeModifier(modifier: CjModifierKeywordToken)

    val modifierVisibility: DescriptorVisibility?
}
