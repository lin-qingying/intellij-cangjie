package com.huawei.cangjie.psi

import com.huawei.cangjie.lexer.CjTokens
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement

class CjConstructorDelegationReferenceExpression(node: ASTNode) : CjExpressionImpl(node),
    CjReferenceExpression {
    val isThis: Boolean
        get() = findChildByType<PsiElement?>(CjTokens.THIS_KEYWORD) != null
}
