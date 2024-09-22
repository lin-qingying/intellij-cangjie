package com.huawei.cangjie.psi

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType

open class CjContainerNode(node: ASTNode) : CjElementImpl(node) {
    public override fun <T> findChildByClass(aClass: Class<T>): T? {
        return super.findChildByClass(aClass)
    }

    public override fun <T : PsiElement > findChildByType(type: IElementType): T? {
        return super.findChildByType(type)
    }
}
