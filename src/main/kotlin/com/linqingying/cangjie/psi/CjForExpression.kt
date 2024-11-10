package com.linqingying.cangjie.psi

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.linqingying.cangjie.CjNodeTypes
import com.linqingying.cangjie.lexer.CjTokens


class CjForExpression(node: ASTNode) : CjLoopExpression(node), CjPatternEntryBlock {
    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R {
        return visitor.visitForExpression(this, data)
    }

    @get: IfNotParsed
    val loopParameter: CjParameter?
        get() = findChildByType<PsiElement>(CjNodeTypes.VALUE_PARAMETER) as CjParameter?
    val destructuringDeclaration: CjDestructuringDeclaration?
        get() {
            val loopParameter = loopParameter ?: return null
            return loopParameter.destructuringDeclaration
        }
    val pattern: CjCasePattern?
        get() {


            return findChildByClass(CjCasePattern::class.java)
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

