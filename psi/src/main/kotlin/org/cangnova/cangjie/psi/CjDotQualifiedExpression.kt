/*
 * Copyright 2025 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.cangjie.psi

import org.cangnova.cangjie.psi.stubs.CangJiePlaceHolderStub
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes
import org.cangnova.cangjie.psi.stubs.elements.CjTokenSets.INSIDE_DIRECTIVE_EXPRESSIONS
import org.cangnova.cangjie.utils.exceptions.logErrorWithAttachment
import org.cangnova.cangjie.utils.exceptions.withPsiEntry
import com.intellij.lang.ASTNode
import com.intellij.openapi.diagnostic.Logger

class CjDotQualifiedExpression :
    CjExpressionImplStub<CangJiePlaceHolderStub<CjDotQualifiedExpression>>,
    CjQualifiedExpression,
    CjCallableReference {

    constructor(node: ASTNode) : super(node)

    constructor(stub: CangJiePlaceHolderStub<CjDotQualifiedExpression>) : super(
        stub,
        CjStubElementTypes.DOT_QUALIFIED_EXPRESSION,
    )

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D): R? {
        return visitor.visitDotQualifiedExpression(this, data)
    }

    override val receiverExpression: CjExpression
        get() {
            val stub = stub
            if (stub != null) {
                val childExpressionsByStub = getChildExpressionsByStub(stub)
                if (childExpressionsByStub != null) {
                    return childExpressionsByStub[0]!!
                }
            }
            return super<CjQualifiedExpression>.receiverExpression
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
        if (stub.getParentStubOfType(CjImportItem::class.java) == null &&
            stub.getParentStubOfType(CjPackageDirective::class.java) == null &&
            stub.getParentStubOfType(CjValueArgument::class.java) == null
        ) {
            LOG.error(
                "CjDotQualifiedExpression should only have stubs inside import, argument or package directives.\n" + "Stubs were created for:\n$text\nFile text:\n${containingFile.text}",
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

    override val callableReference: CjNameReferenceExpression
        get() {
            val selector = selectorExpression
            return when (selector) {
                is CjNameReferenceExpression -> selector
                is CjCallExpression -> selector.calleeExpression as? CjNameReferenceExpression
                    ?: error("Callable reference expected to be CjNameReferenceExpression, but was: ${selector::class.java}")
                else -> error("Callable reference expected to be CjNameReferenceExpression, but was: ${selector?.let { it::class.java }}")
            }
        }
}
