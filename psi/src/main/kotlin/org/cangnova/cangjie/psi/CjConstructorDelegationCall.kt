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

import com.intellij.lang.ASTNode

class CjConstructorDelegationCall(node: ASTNode) : CjElementImpl(node), CjCallElement {
    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D): R? {
        return visitor.visitConstructorDelegationCall(this, data)
    }

    override val calleeExpression: CjConstructorDelegationReferenceExpression?
        get() =
            findChildByClass(CjConstructorDelegationReferenceExpression::class.java)

    override val lambdaArguments: List<CjLambdaArgument> = emptyList()

    override val typeArguments: List<CjTypeProjection> = emptyList()

    override val typeArgumentList: CjTypeArgumentList? = null

    override val valueArgumentList: CjValueArgumentList? get() = findChildByType(CjNodeTypes.VALUE_ARGUMENT_LIST)

    override val valueArguments: List<ValueArgument>
        get() {
            val list = valueArgumentList
            return list?.arguments ?: emptyList<CjValueArgument>()
        }

    val isImplicit: Boolean
        get() {
            val callee = calleeExpression
            return callee != null && callee.firstChild == null
        }

    val isCallToThis: Boolean
        get() {
            val callee = calleeExpression
            return callee != null && callee.isThis
        }
}
