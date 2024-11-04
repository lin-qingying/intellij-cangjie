package com.linqingying.cangjie.psi

import com.intellij.lang.ASTNode

class CjKeyword(node: ASTNode) : CjElementImpl(node), CjElement {
    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R {
        return visitor.visitKeyword(this, data)
    }
}
