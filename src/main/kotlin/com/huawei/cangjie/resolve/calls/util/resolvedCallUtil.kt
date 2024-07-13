package com.huawei.cangjie.resolve.calls.util

import com.huawei.cangjie.descriptors.CallableDescriptor
import com.huawei.cangjie.resolve.calls.model.ResolvedCall
import com.huawei.cangjie.resolve.calls.results.ResolutionStatus
import com.huawei.cangjie.resolve.calls.tower.CandidateApplicability
import com.huawei.cangjie.resolve.calls.tower.NewAbstractResolvedCall
import com.huawei.cangjie.resolve.constants.IntegerLiteralTypeConstructor
import com.huawei.cangjie.types.DefinitelyNotNullType
import com.huawei.cangjie.types.ErrorUtils
import com.huawei.cangjie.types.StubTypeForBuilderInference
import com.huawei.cangjie.types.checker.NewCapturedType
import com.huawei.cangjie.types.util.contains

fun ResolvedCall<*>.isNewNotCompleted(): Boolean = if (this is NewAbstractResolvedCall) !isCompleted() else false
fun CallableDescriptor.isNotSimpleCall(): Boolean =
    typeParameters.isNotEmpty() ||
            (returnType?.let { type ->
                type.contains {
                    it is NewCapturedType ||
                            it.constructor is IntegerLiteralTypeConstructor ||
                            it is DefinitelyNotNullType ||
                            it is StubTypeForBuilderInference
                }
            } ?: false)

fun ResolvedCall<*>.hasInferredReturnType(): Boolean {
    if (isNewNotCompleted()) return false

    val returnType = this.resultingDescriptor.returnType ?: return false
    return !returnType.contains { ErrorUtils.isUninferredTypeVariable(it) }
}

fun CandidateApplicability.toResolutionStatus(): ResolutionStatus = when (this) {
    CandidateApplicability.RESOLVED,
    CandidateApplicability.RESOLVED_LOW_PRIORITY,
    CandidateApplicability.RESOLVED_WITH_ERROR,
    CandidateApplicability.RESOLVED_NEED_PRESERVE_COMPATIBILITY -> ResolutionStatus.SUCCESS

    CandidateApplicability.INAPPLICABLE_WRONG_RECEIVER -> ResolutionStatus.RECEIVER_TYPE_ERROR
    CandidateApplicability.UNSAFE_CALL -> ResolutionStatus.UNSAFE_CALL_ERROR
    else -> ResolutionStatus.OTHER_ERROR
}
