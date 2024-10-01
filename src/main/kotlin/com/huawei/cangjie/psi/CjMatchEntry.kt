package com.huawei.cangjie.psi

import com.huawei.cangjie.CjNodeTypes
import com.huawei.cangjie.lexer.CjTokens
import com.huawei.cangjie.psi.psiUtil.getTrailingCommaByClosingElement
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement

interface CjPatternEntryBlock:PsiElement

class CjMatchEntry(node: ASTNode) : CjElementImpl(node),CjPatternEntryBlock {
    val isElse: Boolean
        get() {
            return elseKeyword != null
        }

    val elseKeyword: PsiElement?
        get() {
            return findChildByType(CjNodeTypes.WILDCARD_PATTERN)
        }

    val expression: CjCaseBlockExpression?
        get() = findChildByClass(CjCaseBlockExpression::class.java)

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R {
        return visitor.visitMatchEntry(this, data)
    }

    val conditions: Array<CjCasePattern>
        get() = findChildrenByClass(CjCasePattern::class.java)

    val trailingComma: PsiElement?
        get() = getTrailingCommaByClosingElement(arrow)

    val arrow: PsiElement?
        get() = findChildByType(CjTokens.DOUBLE_ARROW)
}
