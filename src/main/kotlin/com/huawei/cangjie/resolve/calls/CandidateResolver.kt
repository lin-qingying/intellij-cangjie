package com.huawei.cangjie.resolve.calls
import  com.huawei.cangjie.resolve.calls.results.ResolutionStatus.*
import com.huawei.cangjie.builtins.ReflectionTypes
import com.huawei.cangjie.descriptors.CallableDescriptor
import com.huawei.cangjie.progress.ProgressIndicatorAndCompilationCanceledStatus
import com.huawei.cangjie.resolve.UpperBoundChecker
import com.huawei.cangjie.resolve.calls.checkers.AdditionalTypeChecker
import com.huawei.cangjie.resolve.calls.context.CallCandidateResolutionContext
import com.huawei.cangjie.resolve.calls.context.CandidateResolveMode
import com.huawei.cangjie.resolve.calls.context.CheckArgumentTypesMode
import com.huawei.cangjie.resolve.calls.smartcasts.DataFlowValueFactory
import com.huawei.cangjie.resolve.calls.smartcasts.SmartCastManager
import com.huawei.cangjie.types.ErrorUtils

class CandidateResolver(
    private val argumentTypeResolver: ArgumentTypeResolver,
    private val genericCandidateResolver: GenericCandidateResolver,
    private val reflectionTypes: ReflectionTypes,
    private val additionalTypeCheckers: Iterable<AdditionalTypeChecker>,
    private val smartCastManager: SmartCastManager,
    private val dataFlowValueFactory: DataFlowValueFactory,
    private val upperBoundChecker: UpperBoundChecker
){
    private fun <D : CallableDescriptor> CallCandidateResolutionContext<D>.shouldContinue() =
        candidateResolveMode == CandidateResolveMode.FULLY || candidateCall.status.possibleTransformToSuccess()

    private inline fun <D : CallableDescriptor> CallCandidateResolutionContext<D>.check(
        crossinline checker: CallCandidateResolutionContext<D>.() -> Unit
    ) {
        if (shouldContinue()) checker() else candidateCall.addRemainingTasks { checker() }
    }

    private fun <D : CallableDescriptor> CallCandidateResolutionContext<D>.mapArguments() = check {
        val argumentMappingStatus = ValueArgumentsToParametersMapper.mapValueArgumentsToParameters(
            call, tracing, candidateCall, languageVersionSettings
        )
        if (!argumentMappingStatus.isSuccess) {
            candidateCall.addStatus(ARGUMENTS_MAPPING_ERROR)
        }
    }
    private val CallCandidateResolutionContext<*>.candidateDescriptor: CallableDescriptor
        get() = candidateCall.candidateDescriptor
    fun <D : CallableDescriptor> performResolutionForCandidateCall(
        context: CallCandidateResolutionContext<D>,
        checkArguments: CheckArgumentTypesMode
    ): Unit = with(context) {
        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()

        if (ErrorUtils.isError(candidateDescriptor)) {
            candidateCall.addStatus(SUCCESS)
            return
        }
//
//        if (!checkOuterClassMemberIsAccessible(this)) {
//            candidateCall.addStatus(OTHER_ERROR)
//            return
//        }
//
//        if (!context.isDebuggerContext) {
//            checkVisibilityWithoutReceiver()
//        }
//
        when (checkArguments) {
            CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS ->
                mapArguments()
            CheckArgumentTypesMode.CHECK_CALLABLE_TYPE -> TODO()
//                checkExpectedCallableType()
        }

//        checkReceiverTypeError()
//        checkExtensionReceiver()
//        checkDispatchReceiver()
//
//        processTypeArguments()
//        checkValueArguments()
//
//        checkAbstractAndSuper()
//        checkConstructedExpandedType()
    }
}
