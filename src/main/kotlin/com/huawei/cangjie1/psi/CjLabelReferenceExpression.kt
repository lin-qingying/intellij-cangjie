package com.huawei.cangjie1.psi

import com.intellij.lang.ASTNode


class CjLabelReferenceExpression(node: ASTNode) : CjSimpleNameExpressionImpl(node) {
    override fun getReferencedNameElement() = getIdentifier() ?: this
}
