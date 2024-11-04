package com.linqingying.cangjie.psi

import com.linqingying.cangjie.CjNodeTypes
import com.intellij.lang.ASTNode

class CjCatchClause(node: ASTNode) : CjElementImpl(node) {
    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R {
        return visitor.visitCatchSection(this, data)
    }

    @get:IfNotParsed
    val parameterList: CjParameterList?
        get() = findChildByType(CjNodeTypes.VALUE_PARAMETER_LIST)

    @get:IfNotParsed
    val catchParameter: CjParameter?
        get() {
            val list = parameterList ?: return null
            val parameters = list.parameters
            return if (parameters.size == 1) parameters[0] else null
        }


    @get:IfNotParsed
    val catchBody: CjExpression?
        get() = findChildByClass(CjExpression::class.java)
}
