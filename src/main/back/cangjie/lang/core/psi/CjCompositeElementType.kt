package com.huawei.cangjie.lang.core.psi

import com.huawei.cangjie.lang.CjLanguage
import com.intellij.lang.ASTNode
import com.intellij.psi.tree.ICompositeElementType
import com.intellij.psi.tree.IElementType

//import com.huawei.cangjie.lang.CjLanguage
//import com.huawei.cangjie.lang.core.psi.CjElementTypes
//import com.intellij.lang.ASTNode
//import com.intellij.psi.PsiElement
//import com.intellij.psi.tree.ICompositeElementType
//import com.intellij.psi.tree.IElementType
//
  class CjElementType(s: String) : IElementType(s, CjLanguage), ICompositeElementType {
    override fun createCompositeNode(): ASTNode {
        return CjElementTypes.Factory.createElement(this)
    }
}
