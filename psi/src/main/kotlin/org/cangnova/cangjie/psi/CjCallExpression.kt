/*
 * Copyright 2026 LinQingYing. and contributors.
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

import com.google.common.collect.Lists
import com.intellij.lang.ASTNode

open class CjCallExpression(node: ASTNode) : CjExpressionImpl(node), CjCallElement, CjReferenceExpression {
    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D): R? {
        return visitor.visitCallExpression(this, data)
    }

    override val lambdaArguments: List<CjLambdaArgument>
        get() {
            return findChildrenByType<CjLambdaArgument>(CjNodeTypes.LAMBDA_ARGUMENT)
        }
    val referenceExpression: CjNameReferenceExpression? get() = calleeExpression as? CjNameReferenceExpression

    override val typeArguments: List<CjTypeProjection>
        get() {
            return referenceExpression?.typeArguments ?: emptyList()
        }

    override val typeArgumentList: CjTypeArgumentList?
        get() {
            return referenceExpression?.typeArgumentList
        }

    fun getBasicTypeExpr(): CjExpression? {
        val typeElement = findChildByType<CjTypeReference>(CjNodeTypes.TYPE_REFERENCE) ?: return null

        return typeElement.node.findChildByType(CjNodeTypes.BASIC_TYPE)?.psi as? CjExpression
    }

    override val calleeExpression: CjExpression?
        get() {
            return findChildByClass(CjExpression::class.java) ?: getBasicTypeExpr()
        }

    override val valueArgumentList: CjValueArgumentList?
        get() {

            return findChildByType(CjNodeTypes.VALUE_ARGUMENT_LIST) as CjValueArgumentList?
        }

    override val valueArguments: List<CjValueArgument>
        get() {

            val valueArgumentsInParentheses =
                valueArgumentList?.arguments ?: emptyList<CjValueArgument>()
            val functionLiteralArguments: List<CjLambdaArgument> =
                lambdaArguments
            if (functionLiteralArguments.isEmpty()) {
                return valueArgumentsInParentheses
            }
            val allValueArguments: MutableList<CjValueArgument> =
                Lists.newArrayList<CjValueArgument>()
            allValueArguments.addAll(valueArgumentsInParentheses)
            allValueArguments.addAll(functionLiteralArguments)
            return allValueArguments
        }

    override fun toString(): String {
        return node.elementType.toString()
    }
}
