package com.linqingying.cangjie.psi

import com.linqingying.cangjie.CjNodeTypes
import com.intellij.lang.ASTNode

class CjCatchClause(node: ASTNode) : CjElementImpl(node) {
    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R {
        return visitor.visitCatchSection(this, data)
    }


    @get:IfNotParsed
    val catchParameter: CjCatchParameter?
        get() {

            return findChildByType(CjNodeTypes.CATCH_PARAMETER)
        }


    @get:IfNotParsed
    val catchBody: CjExpression?
        get() = findChildByClass(CjBlockExpression::class.java)
}
