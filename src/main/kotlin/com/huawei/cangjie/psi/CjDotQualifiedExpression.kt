package com.huawei.cangjie.psi

import com.huawei.cangjie.psi.stubs.CangJiePlaceHolderStub
import com.huawei.cangjie.psi.stubs.elements.CjStubElementTypes
import com.huawei.cangjie.psi.stubs.elements.CjTokenSets.INSIDE_DIRECTIVE_EXPRESSIONS
import com.huawei.cangjie.utils.exceptions.logErrorWithAttachment
import com.huawei.cangjie.utils.exceptions.withPsiEntry
import com.intellij.lang.ASTNode
import com.intellij.openapi.diagnostic.Logger


class CjDotQualifiedExpression : CjExpressionImplStub<CangJiePlaceHolderStub<CjDotQualifiedExpression>>,
    CjQualifiedExpression {


    constructor(node: ASTNode) : super(node)

    constructor(stub: CangJiePlaceHolderStub<CjDotQualifiedExpression>) : super(
        stub, CjStubElementTypes.DOT_QUALIFIED_EXPRESSION
    )

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R {
        return visitor.visitDotQualifiedExpression(this, data)
    }

    override val receiverExpression: CjExpression?
        get() {
            val stub = stub
            if (stub != null) {
                val childExpressionsByStub = getChildExpressionsByStub(stub)
                if (childExpressionsByStub != null) {
                    return childExpressionsByStub[0]
                }
            }
            return super.receiverExpression
        }

    override val selectorExpression: CjExpression?
        get() {
            val stub = stub
            if (stub != null) {
                val childExpressionsByStub = getChildExpressionsByStub(stub)
                if (childExpressionsByStub != null && childExpressionsByStub.size == 2) {
                    return childExpressionsByStub[1]
                }
            }
            return super.selectorExpression
        }

    private fun getChildExpressionsByStub(stub: CangJiePlaceHolderStub<CjDotQualifiedExpression>): Array<out CjExpression?>? {
        if (stub.getParentStubOfType(CjImportDirective::class.java) == null && stub.getParentStubOfType(
                CjPackageDirective::class.java
            ) == null && stub.getParentStubOfType(CjValueArgument::class.java) == null
        ) {
            LOG.error(
                "CjDotQualifiedExpression should only have stubs inside import, argument or package directives.\n" + "Stubs were created for:\n$text\nFile text:\n${containingFile.text}"
            )
            return null
        } else {
            val expressions = stub.getChildrenByType(INSIDE_DIRECTIVE_EXPRESSIONS, CjExpression.ARRAY_FACTORY)
            if (expressions.size !in 1..2) {
                LOG.logErrorWithAttachment("Invalid stub structure. DOT_QUALIFIED_EXPRESSION must have one or two children. Was: ${expressions.size}") {
                    withPsiEntry("file", containingFile)
                }
                return null
            }
            return expressions
        }
    }

    companion object {
        private val LOG = Logger.getInstance(CjDotQualifiedExpression::class.java)
    }
}
