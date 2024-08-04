package com.linqingying.cangjie.psi

import com.linqingying.cangjie.lexer.CjTokens
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement

interface CjDoubleColonExpression : CjExpression {
    val receiverExpression: CjExpression?
        get() = node.firstChildNode.psi as? CjExpression

    val hasQuestionMarks: Boolean
        get() {
            for (element in generateSequence(node.firstChildNode, ASTNode::getTreeNext)) {
                when (element.elementType) {
                    CjTokens.QUEST -> return true
//                    CjTokens.COLONCOLON -> return false
                }
            }
            error("Double colon expression must have '::': $text")
        }

    fun findColonColon(): PsiElement?

    val doubleColonTokenReference: PsiElement
        get() = findColonColon()!!

    val lhs: PsiElement?
        get() = doubleColonTokenReference.prevSibling

    fun setReceiverExpression(newReceiverExpression: CjExpression) {
        val oldReceiverExpression = this.receiverExpression
        oldReceiverExpression?.replace(newReceiverExpression) ?: addBefore(newReceiverExpression, doubleColonTokenReference)
    }

    val isEmptyLHS: Boolean
        get() = lhs == null

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R {
        return visitor.visitDoubleColonExpression(this, data)
    }
}
