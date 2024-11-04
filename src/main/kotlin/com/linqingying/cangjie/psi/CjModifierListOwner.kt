package com.linqingying.cangjie.psi

import com.linqingying.cangjie.descriptors.DescriptorVisibility
import com.intellij.psi.PsiElement
import com.linqingying.cangjie.lexer.CjKeywordToken

interface CjModifierListOwner : PsiElement, CjAnnotated {

    val modifierList: CjModifierList?

    fun hasModifier(modifier: CjKeywordToken): Boolean

    fun addModifier(modifier: CjKeywordToken)

    fun removeModifier(modifier: CjKeywordToken)

    val modifierVisibility: DescriptorVisibility?
}
