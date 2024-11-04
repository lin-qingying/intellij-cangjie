package com.linqingying.cangjie.resolve.calls.util

import com.linqingying.cangjie.psi.*
import com.linqingying.cangjie.resolve.scopes.receivers.Receiver
import com.linqingying.cangjie.resolve.scopes.receivers.ReceiverValue
import com.linqingying.cangjie.utils.ReadOnly
import com.intellij.lang.ASTNode

open class DelegatingCall(private val delegate: Call) : Call {

    override val callOperationNode: ASTNode?
        get() = delegate.callOperationNode
    override val explicitReceiver: Receiver?
        get() = delegate.explicitReceiver


    override val dispatchReceiver: ReceiverValue?
        get() = delegate.dispatchReceiver
    override val calleeExpression: CjExpression?
        //
        get() = delegate.calleeExpression

    override var noValueArgument: Boolean = false

    override val valueArgumentList: CjValueArgumentList?
        get() {
            if (noValueArgument) return null
            return delegate.valueArgumentList
        }


    @get:ReadOnly
    override val valueArguments: List<ValueArgument>
        get() {
            if (noValueArgument) return emptyList()
            return delegate.valueArguments
        }


    override val functionLiteralArguments: List<LambdaArgument>
        get() = delegate.functionLiteralArguments
    override var noTypeParameter: Boolean = false

    override val typeArguments: List<CjTypeProjection>
        get() {
            if (noTypeParameter) return emptyList()
            return delegate.typeArguments
        }


    override val typeArgumentList: CjTypeArgumentList?
        //
        get() {
            if (noTypeParameter) return null
            return delegate.typeArgumentList
        }


    override val callElement: CjElement
        get() = delegate.callElement

    override val callType: Call.CallType
        get() = delegate.callType

    override fun toString(): String {
        return "*$delegate"
    }
}
