package com.huawei.cangjie.psi

import com.huawei.cangjie.CjNodeTypes
import com.huawei.cangjie.lexer.CjTokens
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement



class CjForExpression(node: ASTNode) : CjLoopExpression(node) {
    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R {
        return visitor.visitForExpression(this, data)
    }

    @get: IfNotParsed
    val loopParameter: CjParameter?
        get() = findChildByType< PsiElement>(CjNodeTypes.VALUE_PARAMETER) as CjParameter?
    val destructuringDeclaration: CjDestructuringDeclaration?
        get() {
            val loopParameter: CjParameter = loopParameter ?: return null
            return loopParameter.getDestructuringDeclaration()
        }

    @get: IfNotParsed
    val loopRange: CjExpression?
        get() = findExpressionUnder(CjNodeTypes.LOOP_RANGE)

    @get: IfNotParsed
    val inKeyword: PsiElement?
        get() = findChildByType(CjTokens.IN_KEYWORD)
    val forKeyword: PsiElement?
        get() = findChildByType(CjTokens.FOR_KEYWORD)
}

