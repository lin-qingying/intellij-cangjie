package com.huawei.cangjie.resolve.calls.context

import com.huawei.cangjie.descriptors.ValueParameterDescriptor
import com.huawei.cangjie.psi.CjExpression
import com.huawei.cangjie.psi.ValueArgument
import com.huawei.cangjie.resolve.calls.model.ResolvedCall


sealed class CallPosition {
    object Unknown : CallPosition()

    class ExtensionReceiverPosition(val resolvedCall: ResolvedCall<*>) : CallPosition()

    class ValueArgumentPosition(
        val resolvedCall: ResolvedCall<*>,
        val valueParameter: ValueParameterDescriptor,
        val valueArgument: ValueArgument
    ) : CallPosition()

//    class PropertyAssignment(val leftPart: CjExpression?, val isLeft: Boolean) : CallPosition()

//    class CallableReferenceRhs(val lhs: DoubleColonLHS?) : CallPosition()
}
