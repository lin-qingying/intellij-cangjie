package com.linqingying.cangjie.resolve.calls.context

import com.linqingying.cangjie.descriptors.ValueParameterDescriptor
import com.linqingying.cangjie.psi.CjExpression
import com.linqingying.cangjie.psi.ValueArgument
import com.linqingying.cangjie.resolve.calls.model.ResolvedCall


sealed class CallPosition {
    object Unknown : CallPosition()

    class ExtensionReceiverPosition(val resolvedCall: ResolvedCall<*>) : CallPosition()

    class ValueArgumentPosition(
        val resolvedCall: ResolvedCall<*>,
        val valueParameter: ValueParameterDescriptor,
        val valueArgument: ValueArgument
    ) : CallPosition()
    class VariableAssignment(val leftPart: CjExpression?, val isLeft: Boolean) : CallPosition()

//    class PropertyAssignment(val leftPart: CjExpression?, val isLeft: Boolean) : CallPosition()

//    class CallableReferenceRhs(val lhs: DoubleColonLHS?) : CallPosition()
}
