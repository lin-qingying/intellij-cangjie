package com.linqingying.cangjie.resolve.calls.checkers

import com.linqingying.cangjie.descriptors.ReceiverParameterDescriptor
import com.linqingying.cangjie.psi.CjExpression
import com.linqingying.cangjie.resolve.calls.context.CallResolutionContext
import com.linqingying.cangjie.resolve.calls.context.ResolutionContext
import com.linqingying.cangjie.resolve.scopes.receivers.ReceiverValue
import com.linqingying.cangjie.types.CangJieType


@JvmDefaultWithCompatibility
interface AdditionalTypeChecker {
    fun checkType(
        expression: CjExpression,
        expressionType: CangJieType,
        expressionTypeWithSmartCast: CangJieType,
        c: ResolutionContext<*>
    )

    fun checkReceiver(
        receiverParameter: ReceiverParameterDescriptor,
        receiverArgument: ReceiverValue,
        safeAccess: Boolean,
        c: CallResolutionContext<*>
    ) {
    }

}
