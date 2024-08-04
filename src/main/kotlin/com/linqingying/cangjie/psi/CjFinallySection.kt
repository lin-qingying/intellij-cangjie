package com.linqingying.cangjie.psi

import com.linqingying.cangjie.CjNodeTypes
import com.intellij.lang.ASTNode

class CjFinallySection(node:ASTNode):CjElementImpl(node),CjStatementExpression{

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R {
        return visitor.visitFinallySection(this, data);
    }

    val finalExpression: CjBlockExpression?
        get() = findChildByType(CjNodeTypes.BLOCK) as CjBlockExpression?
}


