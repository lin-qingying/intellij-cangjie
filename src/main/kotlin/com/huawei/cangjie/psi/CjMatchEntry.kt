package com.huawei.cangjie.psi

import com.huawei.cangjie.lexer.CjTokens
import com.huawei.cangjie.psi.psiUtil.getTrailingCommaByClosingElement
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement

class CjMatchEntry(node: ASTNode) : CjElementImpl(node) {
    fun is_(): Boolean {
        return getKeyword() != null
    }

    fun getKeyword(): PsiElement? {
        return findChildByType(CjTokens.UNDERLINE)
    }

    val expression: CjExpression?
        get() = findChildByClass(CjExpression::class.java)

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R {
        return visitor.visitMatchEntry(this, data)
    }

    val conditions: Array<CjMatchCondition>
        get() = findChildrenByClass(CjMatchCondition::class.java)

    val trailingComma: PsiElement?
        get() = getTrailingCommaByClosingElement(arrow)

    val arrow: PsiElement?
        get() = findChildByType(CjTokens.DOUBLE_ARROW)
}
