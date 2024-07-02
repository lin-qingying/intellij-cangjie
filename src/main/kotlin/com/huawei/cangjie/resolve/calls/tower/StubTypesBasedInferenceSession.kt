package com.huawei.cangjie.resolve.calls.tower

import com.huawei.cangjie.resolve.calls.components.CompletedCallInfo
import com.huawei.cangjie.resolve.calls.components.PartialCallInfo
import com.huawei.cangjie.resolve.calls.context.BasicCallResolutionContext
import com.huawei.cangjie.resolve.calls.model.CompletedCallResolutionResult
import com.huawei.cangjie.resolve.calls.model.PartialCallResolutionResult
import com.huawei.cangjie.resolve.calls.model.SingleCallResolutionResult
import com.huawei.cangjie.resolve.calls.tasks.TracingStrategy

abstract class CallInfo(
    open val callResolutionResult: SingleCallResolutionResult,
    val context: BasicCallResolutionContext,
    val tracingStrategy: TracingStrategy
)
class PSIPartialCallInfo(
    override val callResolutionResult: PartialCallResolutionResult,
    context: BasicCallResolutionContext,
    tracingStrategy: TracingStrategy
) : CallInfo(callResolutionResult, context, tracingStrategy), PartialCallInfo
class PSICompletedCallInfo(
    override val callResolutionResult: CompletedCallResolutionResult,
    context: BasicCallResolutionContext,
    val resolvedCall: NewAbstractResolvedCall<*>,
    tracingStrategy: TracingStrategy
) : CallInfo(callResolutionResult, context, tracingStrategy), CompletedCallInfo
