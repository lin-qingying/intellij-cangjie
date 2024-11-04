package com.linqingying.cangjie.psi

import com.intellij.lang.ASTNode

class CjLetExpression(node: ASTNode) : CjElementImpl(node) {
    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R {
        return visitor.visitLetExpression(this, data)
    }

    val pattern get() =   findChildByClass(CjCasePattern::class.java)
    val expression:CjExpression? get() {
        val list = findChildrenByClass(CjExpression::class.java)
        list.forEach {
            if (it !is CjCasePattern){
                return it
            }
        }
        return null
    }
}
