package com.linqingying.cangjie.psi

import com.linqingying.cangjie.CjNodeTypes
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement


abstract class CjInstanceExpressionWithLabel(node: ASTNode) : CjExpressionWithLabel(node) {
    val instanceReference: CjReferenceExpression
        get() = findChildByType<PsiElement>(CjNodeTypes.REFERENCE_EXPRESSION) as CjReferenceExpression
}

