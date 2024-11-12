package com.linqingying.cangjie.psi

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElementVisitor
import com.linqingying.cangjie.CjNodeTypes.PATTERN_GUARD

class CjPatternGuard (node:ASTNode): CjElementImpl(node) {

    override fun <R,D> accept(visitor: CjVisitor<R, D>, data: D?): R  {
        return visitor.visitPatternGuard(this, data)
    }


    val expression:CjExpression? get() = findChildByClass(CjExpression::class.java)

}
