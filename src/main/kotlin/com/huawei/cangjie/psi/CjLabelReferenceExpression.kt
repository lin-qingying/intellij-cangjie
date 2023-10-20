package com.huawei.cangjie.psi

import com.intellij.lang.ASTNode


class CjLabelReferenceExpression(node: ASTNode) : CjSimpleNameExpressionImpl(node) {
    override fun getReferencedNameElement() = getIdentifier() ?: this
}
