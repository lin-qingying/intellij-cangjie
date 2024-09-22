package com.huawei.cangjie.resolve.calls.util

import com.google.common.collect.Lists
import com.huawei.cangjie.psi.*
import com.huawei.cangjie.psi.debugtext.getDebugText
import com.huawei.cangjie.resolve.scopes.receivers.Receiver
import com.huawei.cangjie.resolve.scopes.receivers.ReceiverValue
import com.intellij.lang.ASTNode
import com.intellij.psi.impl.source.tree.LeafPsiElement

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

    @JvmStatic
    fun makeCall(leftAsReceiver: ReceiverValue, expression: CjBinaryExpression): Call {
        return makeCallWithExpressions(
            expression,
            leftAsReceiver,
            null,
            expression.operationReference,
            listOf(expression.right)
        )
    }

    @JvmStatic
    fun makeCall(baseAsReceiver: ReceiverValue, expression: CjUnaryExpression): Call {
        return makeCall(expression, baseAsReceiver, null, expression.operationReference, emptyList())
    }

    @JvmStatic
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
                get() = callElement.valueArgumentList

            override val valueArguments: List<ValueArgument>
                get() = callElement.valueArguments

            override val functionLiteralArguments: List<LambdaArgument>
                get() = callElement.lambdaArguments

            override val typeArguments: List<CjTypeProjection>
                get() = callElement.typeArguments

            override val typeArgumentList: CjTypeArgumentList?
                get() = callElement.typeArgumentList

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

    private class CallImpl(
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
            get() = emptyList()

        override val typeArgumentList: CjTypeArgumentList?
            get() = null

        override fun toString(): String {
            return callElement.text
        }
    }
}
