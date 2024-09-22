package com.huawei.cangjie.resolve.calls.util

import com.huawei.cangjie.psi.*
import com.huawei.cangjie.resolve.scopes.receivers.Receiver
import com.huawei.cangjie.resolve.scopes.receivers.ReceiverValue
import com.huawei.cangjie.utils.ReadOnly
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


    override val valueArgumentList: CjValueArgumentList?
        get() = delegate.valueArgumentList

    @get:ReadOnly
    override val valueArguments: List<ValueArgument>
        //
        get() = delegate.valueArguments


    override val functionLiteralArguments: List<LambdaArgument>
        get() = delegate.functionLiteralArguments

    override val typeArguments: List<CjTypeProjection>
        get() = delegate.typeArguments


    override val typeArgumentList: CjTypeArgumentList?
        //
        get() = delegate.typeArgumentList


    override val callElement: CjElement
        get() = delegate.callElement

    override val callType: Call.CallType
        get() = delegate.callType

    override fun toString(): String {
        return "*$delegate"
    }
}
