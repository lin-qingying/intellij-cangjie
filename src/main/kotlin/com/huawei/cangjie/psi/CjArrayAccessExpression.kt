package com.huawei.cangjie.psi

import com.google.common.collect.Lists
import com.huawei.cangjie.CjNodeTypes
import com.huawei.cangjie.lexer.CjTokens
import com.huawei.cangjie.psi.psiUtil.getTrailingCommaByClosingElement
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil

class CjArrayAccessExpression(node: ASTNode) : CjExpressionImpl(node), CjReferenceExpression {
    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R  {
        return visitor.visitArrayAccessExpression(this, data)
    }

    @get:IfNotParsed
    val arrayExpression: CjExpression?
        get() = findChildByClass(CjExpression::class.java)

    val indexExpressions: List<CjExpression>
        get() = PsiTreeUtil.getChildrenOfTypeAsList(
            indicesNode,
            CjExpression::class.java
        )

    val indicesNode: CjContainerNode
        get() {
            val indicesNode =
                checkNotNull(findChildByType<CjContainerNode>(CjNodeTypes.INDICES)) { "Can't be null because of parser" }
            return indicesNode
        }

    val bracketRanges: List<TextRange>
        get() {
            val lBracket = leftBracket
            val rBracket = rightBracket
            if (lBracket == null || rBracket == null) {
                return emptyList()
            }
            return Lists.newArrayList(
                lBracket.textRange,
                rBracket.textRange
            )
        }

    val leftBracket: PsiElement?
        get() = indicesNode.findChildByType(CjTokens.LBRACKET)

    val rightBracket: PsiElement?
        get() = indicesNode.findChildByType(CjTokens.RBRACKET)

    val trailingComma: PsiElement?
        get() = getTrailingCommaByClosingElement(rightBracket)
}
