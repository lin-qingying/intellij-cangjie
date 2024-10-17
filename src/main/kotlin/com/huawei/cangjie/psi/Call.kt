package com.huawei.cangjie.psi

import com.huawei.cangjie.lexer.CjTokens
import com.huawei.cangjie.resolve.scopes.receivers.Receiver
import com.huawei.cangjie.resolve.scopes.receivers.ReceiverValue
import com.huawei.cangjie.utils.ReadOnly
import com.intellij.lang.ASTNode
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty

interface Call {
    // SAFE_ACCESS or DOT or so
    var  noValueArgument: Boolean

    //控制typeArgumentList返回空值
    var noTypeParameter: Boolean
    val callOperationNode: ASTNode?

    val isSemanticallyEquivalentToSafeCall: Boolean
        get() = callOperationNode != null && callOperationNode!!.elementType === CjTokens.SAFE_ACCESS


    val explicitReceiver: Receiver?


    val dispatchReceiver: ReceiverValue?


    val calleeExpression: CjExpression?


    val valueArgumentList: CjValueArgumentList?


    @get:ReadOnly
    val valueArguments: List<ValueArgument>


    @get:ReadOnly
    val functionLiteralArguments: List<LambdaArgument>


    @get:ReadOnly
    val typeArguments: List<CjTypeProjection>


    val typeArgumentList: CjTypeArgumentList?


    val callElement: CjElement

    enum class CallType {
        DEFAULT, ARRAY_GET_METHOD, ARRAY_SET_METHOD, INVOKE, CONTAINS
    }


    val callType: CallType
}
