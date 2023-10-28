package com.huawei.cangjie.psi

import com.intellij.lang.ASTNode


class CjContainerNodeForControlStructureBody(node: ASTNode) : CjContainerNode(node) {
    val expression: CjExpression?
        get() = findChildByClass(CjExpression::class.java)
}
