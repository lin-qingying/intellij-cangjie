package com.linqingying.cangjie.psi

import com.linqingying.cangjie.lexer.CjTokens
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement


class CjRangeExpression(node: ASTNode) : CjBinaryExpression(node), CjReferenceExpression {
    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R {
        return visitor.visitRangeExpression(this, data)
    }

    fun getInnerExpressions(): List<CjExpression> {

        return listOfNotNull(left, right,step)
    }

    val step: CjExpression?
        get() {
            val COLON = findChildByType<PsiElement>(CjTokens.COLON) ?: return null

//            获取COLON后面的元素
            return COLON.node.treeNext.psi as? CjExpression


        }

}
