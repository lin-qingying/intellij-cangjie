package com.huawei.cangjie.psi

import com.huawei.cangjie.CjNodeTypes
import com.intellij.lang.ASTNode


class CjWhileExpression(node: ASTNode) : CjWhileExpressionBase(node),CjPatternEntryBlock {
    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R {
        return visitor.visitWhileExpression( this, data)
    }

    val letExpression get() = findChildByType<CjLetExpression>(CjNodeTypes.LET_EXPRESSION)

}

