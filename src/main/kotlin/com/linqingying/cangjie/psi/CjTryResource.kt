package com.linqingying.cangjie.psi

import com.intellij.lang.ASTNode
import com.linqingying.cangjie.CjNodeTypes

class CjTryResource(node: ASTNode) : CjElementImpl(node) {

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R {
        return visitor.visitTryResource(this, data)
    }

    @get:IfNotParsed
    val parameter: CjParameter?
        get() {

            return findChildByType(CjNodeTypes.VALUE_PARAMETER)
        }

    val expression :CjExpression? get() {

        children.forEach {
            if (it is CjExpression && it !is CjParameter ) return it
        }

        return null


    }

}

class CjTryResourceList(node: ASTNode) : CjElementImpl(node) {

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R {
        return visitor.visitTryResourceList(this, data)
    }

    val resources: List<CjTryResource> get() = findChildrenByType(CjNodeTypes.TRY_RESOURCE)
}
