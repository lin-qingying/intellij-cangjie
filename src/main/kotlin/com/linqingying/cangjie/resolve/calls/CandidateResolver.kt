package com.linqingying.cangjie.resolve.calls
import  com.linqingying.cangjie.resolve.calls.results.ResolutionStatus.*
import com.linqingying.cangjie.builtins.ReflectionTypes
import com.linqingying.cangjie.descriptors.CallableDescriptor
import com.linqingying.cangjie.progress.ProgressIndicatorAndCompilationCanceledStatus
import com.linqingying.cangjie.resolve.UpperBoundChecker
import com.linqingying.cangjie.resolve.calls.checkers.AdditionalTypeChecker
import com.linqingying.cangjie.resolve.calls.context.CallCandidateResolutionContext
import com.linqingying.cangjie.resolve.calls.context.CandidateResolveMode
import com.linqingying.cangjie.resolve.calls.context.CheckArgumentTypesMode
import com.linqingying.cangjie.resolve.calls.smartcasts.DataFlowValueFactory
import com.linqingying.cangjie.resolve.calls.smartcasts.SmartCastManager
import com.linqingying.cangjie.types.ErrorUtils

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
