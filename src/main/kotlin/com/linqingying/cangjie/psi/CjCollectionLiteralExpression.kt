package com.linqingying.cangjie.psi

import com.linqingying.cangjie.CjNodeTypes
import com.linqingying.cangjie.lexer.CjTokens
import com.linqingying.cangjie.psi.psiUtil.getTrailingCommaByClosingElement
import com.linqingying.cangjie.psi.stubs.CangJieCollectionLiteralExpressionStub
import com.linqingying.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import java.util.*

class CjCollectionLiteralExpression : CjElementImplStub<CangJieCollectionLiteralExpressionStub  >,
    CjReferenceExpression {
    constructor(stub: CangJieCollectionLiteralExpressionStub) : super(
        stub,
        CjStubElementTypes.COLLECTION_LITERAL_EXPRESSION
    )

    constructor(node: ASTNode) : super(node)

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R {
        return visitor.visitCollectionLiteralExpression(this, data)
    }

    val leftBracket: PsiElement?
        get() {
            val astNode = node.findChildByType(CjTokens.LBRACKET)
            return astNode?.psi
        }

    val rightBracket: PsiElement?
        get() {
            val astNode = node.findChildByType(CjTokens.RBRACKET)
            return astNode?.psi
        }

    val trailingComma: PsiElement?
        get() {
            val rightBracket = rightBracket
            return getTrailingCommaByClosingElement(rightBracket)
        }

    val innerExpressions: List<CjExpression>
        get() {
            val stub = stub
            if (stub != null) {
                return Arrays.asList(
                    *stub.getChildrenByType(
                        CjNodeTypes.CONSTANT_EXPRESSIONS_TYPES,
                        CjExpression.EMPTY_ARRAY
                    )
                )
            }
            return PsiTreeUtil.getChildrenOfTypeAsList(
                this,
                CjExpression::class.java
            )
        }
}
