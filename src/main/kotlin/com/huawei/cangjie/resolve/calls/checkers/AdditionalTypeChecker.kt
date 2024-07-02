package com.huawei.cangjie.resolve.calls.checkers

import com.huawei.cangjie.descriptors.ReceiverParameterDescriptor
import com.huawei.cangjie.psi.CjExpression
import com.huawei.cangjie.resolve.calls.context.CallResolutionContext
import com.huawei.cangjie.resolve.calls.context.ResolutionContext
import com.huawei.cangjie.resolve.scopes.receivers.ReceiverValue
import com.huawei.cangjie.types.CangJieType


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
