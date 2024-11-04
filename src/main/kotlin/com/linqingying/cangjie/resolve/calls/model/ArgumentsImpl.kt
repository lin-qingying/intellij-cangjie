package com.linqingying.cangjie.resolve.calls.model

import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.resolve.scopes.receivers.ReceiverValueWithSmartCastInfo
import com.linqingying.cangjie.resolve.scopes.receivers.prepareReceiverRegardingCaptureTypes


class FakeCangJieCallArgumentForCallableReference(
    val index: Int,
    val name: Name?
) : CangJieCallArgument {
    override val isSpread: Boolean get() = false
    override val argumentName: Name? get() = name
}

class ReceiverExpressionCangJieCallArgument private constructor(
    override val receiver: ReceiverValueWithSmartCastInfo,
    override val isSafeCall: Boolean = false,
    val isForImplicitInvoke: Boolean = false
) : ExpressionCangJieCallArgument {
    override val isSpread: Boolean get() = false
    override val argumentName: Name? get() = null
    override fun toString() = "$receiver" + if (isSafeCall) "?" else ""

    companion object {
        // we create ReceiverArgument and fix capture types
        operator fun invoke(
            receiver: ReceiverValueWithSmartCastInfo,
            isSafeCall: Boolean = false,
            isForImplicitInvoke: Boolean = false
        ) = ReceiverExpressionCangJieCallArgument(receiver.prepareReceiverRegardingCaptureTypes(), isSafeCall, isForImplicitInvoke)
    }
}
