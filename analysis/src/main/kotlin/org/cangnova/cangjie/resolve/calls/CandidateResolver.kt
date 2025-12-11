/*
 * Copyright 2025 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.cangjie.resolve.calls

import org.cangnova.cangjie.builtins.ReflectionTypes
import org.cangnova.cangjie.descriptors.CallableDescriptor
import org.cangnova.cangjie.progress.ProgressIndicatorAndCompilationCanceledStatus
import org.cangnova.cangjie.resolve.UpperBoundChecker
import org.cangnova.cangjie.resolve.calls.checkers.AdditionalTypeChecker
import org.cangnova.cangjie.resolve.calls.context.CallCandidateResolutionContext
import org.cangnova.cangjie.resolve.calls.context.CandidateResolveMode
import org.cangnova.cangjie.resolve.calls.context.CheckArgumentTypesMode
import org.cangnova.cangjie.resolve.calls.results.ResolutionStatus.ARGUMENTS_MAPPING_ERROR
import org.cangnova.cangjie.resolve.calls.results.ResolutionStatus.SUCCESS
import org.cangnova.cangjie.resolve.calls.smartcasts.DataFlowValueFactory
import org.cangnova.cangjie.resolve.calls.smartcasts.SmartCastManager
import org.cangnova.cangjie.types.ErrorUtils

class CandidateResolver(
    private val argumentTypeResolver: ArgumentTypeResolver,
    private val genericCandidateResolver: GenericCandidateResolver,
    private val reflectionTypes: ReflectionTypes,
    private val additionalTypeCheckers: Iterable<AdditionalTypeChecker>,
    private val smartCastManager: SmartCastManager,
    private val dataFlowValueFactory: DataFlowValueFactory,
    private val upperBoundChecker: UpperBoundChecker
) {
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
