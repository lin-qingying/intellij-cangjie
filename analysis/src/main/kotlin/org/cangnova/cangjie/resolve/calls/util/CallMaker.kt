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

package org.cangnova.cangjie.resolve.calls.util

import com.google.common.collect.Lists
import com.intellij.lang.ASTNode
import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.debugtext.getDebugText
import org.cangnova.cangjie.resolve.scopes.receivers.Receiver
import org.cangnova.cangjie.resolve.scopes.receivers.ReceiverValue

object CallMaker {
    fun makeExternalValueArgument(expression: CjExpression): ValueArgument {
        return ExpressionValueArgument(expression, expression, true)
    }

    fun makeCallForRangeLiteral(rangeExpression: CjRangeExpression): Call {
        return makeCallWithExpressions(
            rangeExpression,
            null,
            null,
            rangeExpression,
            rangeExpression.getInnerExpressions(),
            Call.CallType.DEFAULT
        )
    }

    fun makeCallForBlock(blockExpression: CjBlockExpression): Call {
        return makeCallWithExpressions(
            blockExpression,
            null,
            null,
            blockExpression,
            blockExpression.statementsWithoutReturnKeyword.stream().toList(),
            Call.CallType.DEFAULT
        )
    }


    fun makeCallForSpawnExpression(callExpression: CjSpawnExpression): Call {
        return makeCallWithExpressions(
            callExpression,
            null,
            null,
            callExpression,
            listOf(callExpression.lambdaExpression),
            Call.CallType.DEFAULT
        )
    }

    fun makeCallForBinaryExpression(binaryExpression: CjBinaryExpression): Call {
        return makeCallWithExpressions(
            binaryExpression,
            null,
            null,
            binaryExpression,
            listOf(binaryExpression.left, binaryExpression.right),
            Call.CallType.DEFAULT
        )
    }

    fun makeCallForCollectionLiteral(collectionLiteralExpression: CjCollectionLiteralExpression): Call {
        return makeCallWithExpressions(
            collectionLiteralExpression,
            null,
            null,
            collectionLiteralExpression,
            collectionLiteralExpression.innerExpressions,
            Call.CallType.DEFAULT
        )
    }

    fun makeArrayGetCall(
        arrayAsReceiver: ReceiverValue, arrayAccessExpression: CjArrayAccessExpression,
        callType: Call.CallType
    ): Call {
        return makeCallWithExpressions(
            arrayAccessExpression,
            arrayAsReceiver,
            null,
            arrayAccessExpression,
            arrayAccessExpression.indexExpressions,
            callType
        )
    }

    fun makeArraySetCall(
        arrayAsReceiver: ReceiverValue, arrayAccessExpression: CjArrayAccessExpression,
        rightHandSide: CjExpression, callType: Call.CallType
    ): Call {
        val arguments: MutableList<CjExpression?> = Lists.newArrayList(arrayAccessExpression.indexExpressions)
        arguments.add(rightHandSide)
        return makeCallWithExpressions(
            arrayAccessExpression,
            arrayAsReceiver,
            null,
            arrayAccessExpression,
            arguments,
            callType
        )
    }

    @JvmOverloads
    fun makeCallWithExpressions(
        callElement: CjElement, explicitReceiver: Receiver?,
        callOperationNode: ASTNode?, calleeExpression: CjExpression,
        argumentExpressions: List<CjExpression?>, callType: Call.CallType = Call.CallType.DEFAULT,
        isSemanticallyEquivalentToSafeCall: Boolean = false
    ): Call {
        val arguments: List<ValueArgument>
        if (argumentExpressions.isEmpty()) {
            arguments = emptyList()
        } else {
            arguments = ArrayList(argumentExpressions.size)
            for (argumentExpression in argumentExpressions) {
                arguments.add(makeValueArgument(argumentExpression, calleeExpression))
            }
        }
        return makeCall(
            callElement,
            explicitReceiver,
            callOperationNode,
            calleeExpression,
            arguments,
            callType,
            isSemanticallyEquivalentToSafeCall
        )
    }

    @JvmOverloads
    fun makeValueArgument(expression: CjExpression?, reportErrorsOn: CjElement = expression!!): ValueArgument {
        return ExpressionValueArgument(expression, reportErrorsOn, false)
    }

    
    fun makeCall(leftAsReceiver: ReceiverValue, expression: CjBinaryExpression): Call {
        return makeCallWithExpressions(
            expression,
            leftAsReceiver,
            null,
            expression.operationReference,
            listOf(expression.right)
        )
    }

    
    fun makeCall(baseAsReceiver: ReceiverValue, expression: CjUnaryExpression): Call {
        return makeCall(expression, baseAsReceiver, null, expression.operationReference, emptyList())
    }

    
    fun makeCall(explicitReceiver: Receiver?, callOperationNode: ASTNode?, callElement: CjCallElement): Call {
        return object : Call {
            override val callOperationNode: ASTNode?
                get() = callOperationNode

            override val explicitReceiver: Receiver?
                get() = explicitReceiver

            override val dispatchReceiver: ReceiverValue?
                get() = null

            override val calleeExpression: CjExpression?
                get() = callElement.calleeExpression

            override val valueArgumentList: CjValueArgumentList?
                get() {
                    if (noValueArgument) return null

                    return callElement.valueArgumentList
                }

            override var noValueArgument: Boolean = false
            override val valueArguments: List<ValueArgument>
                get() {
                    if (noValueArgument) return emptyList()
                    return callElement.valueArguments
                }

            override val functionLiteralArguments: List<LambdaArgument>
                get() = callElement.lambdaArguments

            override var noTypeParameter: Boolean = false
            override val typeArguments: List<CjTypeProjection>
                get() {
                    if (noTypeParameter) return emptyList()
                    return callElement.typeArguments
                }


            override val typeArgumentList: CjTypeArgumentList?
                get() {
                    if (noTypeParameter) return null
                    return callElement.typeArgumentList
                }


            override val callElement: CjElement
                get() = callElement

            override fun toString(): String {
                return callElement.getDebugText()
            }

            override val callType: Call.CallType
                get() = Call.CallType.DEFAULT
        }
    }

    @JvmOverloads
    fun makeCall(
        callElement: CjElement,
        explicitReceiver: Receiver?,
        callOperationNode: ASTNode?,
        calleeExpression: CjExpression?,
        arguments: List<ValueArgument>,
        callType: Call.CallType = Call.CallType.DEFAULT,
        isSemanticallyEquivalentToSafeCall: Boolean = false
    ): Call {
        return CallImpl(
            callElement,
            explicitReceiver,
            callOperationNode,
            calleeExpression,
            arguments,
            callType,
            isSemanticallyEquivalentToSafeCall
        )
    }

    fun makePropertyCall(
        explicitReceiver: Receiver?,
        callOperationNode: ASTNode?,
        nameExpression: CjSimpleNameExpression
    ): Call {
        return makeCallWithExpressions(
            nameExpression,
            explicitReceiver,
            callOperationNode,
            nameExpression,
            emptyList<CjExpression>()
        )
    }

    private class ExpressionValueArgument(
        private val expression: CjExpression?,
        reportErrorsOn: CjElement,
        private val isExternal: Boolean
    ) : ValueArgument {
        private val reportErrorsOn = expression ?: reportErrorsOn

        override fun isExternal(): Boolean {
            return isExternal
        }

        override fun getArgumentExpression(): CjExpression? {
            return expression
        }

        override fun getArgumentName(): ValueArgumentName? {
            return null
        }

        override fun isNamed(): Boolean {
            return false
        }

        override fun asElement(): CjElement {
            return reportErrorsOn
        }

        override fun getSpreadElement(): LeafPsiElement? {
            return null
        }

        override fun equals(o: Any?): Boolean {
            if (this === o) return true
            if (o == null || javaClass != o.javaClass) return false

            val argument = o as ExpressionValueArgument

            return expression == argument.expression
        }

        override fun hashCode(): Int {
            return expression?.hashCode() ?: 0
        }
    }

    class CallImpl(
        override val callElement: CjElement,
        override val explicitReceiver: Receiver?,
        override val callOperationNode: ASTNode?,
        override val calleeExpression: CjExpression?,
        override val valueArguments: List<ValueArgument>,
        override val callType: Call.CallType,
        val _isSemanticallyEquivalentToSafeCall: Boolean
    ) : Call {
        constructor(
            callElement: CjElement,
            explicitReceiver: Receiver,
            callOperationNode: ASTNode?,
            calleeExpression: CjExpression?,
            valueArguments: List<ValueArgument>
        ) : this(
            callElement,
            explicitReceiver,
            callOperationNode,
            calleeExpression,
            valueArguments,
            Call.CallType.DEFAULT,
            false
        )

        override var noTypeParameter: Boolean = false
        override var noValueArgument: Boolean = false

        override val isSemanticallyEquivalentToSafeCall: Boolean
            get() = _isSemanticallyEquivalentToSafeCall || super.isSemanticallyEquivalentToSafeCall


        override val dispatchReceiver: ReceiverValue?
            //
            get() = null


        override val functionLiteralArguments: List<LambdaArgument>
            get() = emptyList()

        override val valueArgumentList: CjValueArgumentList?
            get() = null


        override val typeArguments: List<CjTypeProjection>
            get() {
                if (callElement is CjNameReferenceExpression) {
                    return callElement.typeArguments
                }
                return emptyList()
            }

        override val typeArgumentList: CjTypeArgumentList?
            get() {
                if (callElement is CjNameReferenceExpression) {
                    return callElement.typeArgumentList
                }
                return null
            }


        override fun toString(): String {
            return callElement.text
        }
    }
}
