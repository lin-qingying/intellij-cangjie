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

package org.cangnova.cangjie.resolve.calls.model

import com.intellij.util.SmartList
import org.cangnova.cangjie.builtins.CangJieBuiltIns
import org.cangnova.cangjie.descriptors.CallableDescriptor
import org.cangnova.cangjie.descriptors.FunctionDescriptor
import org.cangnova.cangjie.resolve.calls.components.CallableReceiver
import org.cangnova.cangjie.resolve.calls.components.CallableReferenceAdaptation
import org.cangnova.cangjie.resolve.calls.components.CangJieResolutionCallbacks
import org.cangnova.cangjie.resolve.calls.components.candidate.CallableReferenceResolutionCandidate
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintStorage
import org.cangnova.cangjie.resolve.calls.tasks.ExplicitReceiverKind
import org.cangnova.cangjie.resolve.calls.tower.CandidateFactory
import org.cangnova.cangjie.resolve.calls.tower.CandidateWithBoundDispatchReceiver
import org.cangnova.cangjie.resolve.calls.tower.ImplicitScopeTower
import org.cangnova.cangjie.resolve.calls.tower.createCallableReferenceProcessor
import org.cangnova.cangjie.resolve.scopes.receivers.DetailedReceiver
import org.cangnova.cangjie.resolve.scopes.receivers.ReceiverValueWithSmartCastInfo
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.ErrorUtils
import org.cangnova.cangjie.types.UnwrappedType
import org.cangnova.cangjie.types.error.ErrorScopeKind
import org.cangnova.cangjie.types.error.ErrorTypeKind
import org.cangnova.cangjie.types.toFunctionType


class CallableReferencesCandidateFactory(
    val cangjieCall: CallableReferenceResolutionAtom,
    val callComponents: CangJieCallComponents,
    val scopeTower: ImplicitScopeTower,
    val expectedType: UnwrappedType?,
    private val baseSystem: ConstraintStorage?,
    private val resolutionCallbacks: CangJieResolutionCallbacks
) : CandidateFactory<CallableReferenceResolutionCandidate> {

    private fun toCallableReceiver(receiver: ReceiverValueWithSmartCastInfo, isExplicit: Boolean): CallableReceiver {
        if (!isExplicit) return CallableReceiver.ScopeReceiver(receiver)

        return CallableReceiver.ExplicitValueReceiver(receiver)
//        return when (val lhsResult = cangjieCall.lhsResult) {
//            is LHSResult.Expression -> CallableReceiver.ExplicitValueReceiver(receiver)
//            is LHSResult.Type -> {
//                if (lhsResult.qualifier?.classValueReceiver?.type == receiver.receiverValue.type) {
//                    CallableReceiver.BoundValueReference(receiver)
//                } else {
//                    CallableReceiver.UnboundReference(receiver)
//                }
//            }
//
//
//            else -> throw IllegalStateException("Unsupported kind of lhsResult: $lhsResult")
//        }
    }

    fun createCallableProcessor(explicitReceiver: DetailedReceiver?) =
        createCallableReferenceProcessor(scopeTower, cangjieCall.rhsName, this, explicitReceiver)

    private fun buildReflectionType(
        descriptor: CallableDescriptor,
        dispatchReceiver: CallableReceiver?,
        extensionReceiver: CallableReceiver?,
        expectedType: UnwrappedType?,
        builtins: CangJieBuiltIns,
    ): Pair<UnwrappedType, CallableReferenceAdaptation?> {
        val argumentsAndReceivers =
            ArrayList<CangJieType>(descriptor.valueParameters.size + 2 + descriptor.contextReceiverParameters.size)

        val contextReceiversTypes = descriptor.contextReceiverParameters.map { it.type }
        argumentsAndReceivers.addAll(contextReceiversTypes)
//
//        if (dispatchReceiver is CallableReceiver.UnboundReference) {
//            argumentsAndReceivers.add(dispatchReceiver.receiver.stableType)
//        }
//        if (extensionReceiver is CallableReceiver.UnboundReference) {
//            argumentsAndReceivers.add(extensionReceiver.receiver.stableType)
//        }

        val descriptorReturnType = descriptor.returnType
            ?: ErrorUtils.createErrorType(ErrorTypeKind.RETURN_TYPE, descriptor.toString())

        return when (descriptor) {
//            is VariableCallableDescriptor -> {
//                val mutable = descriptor.isVar
////                        && run {
////                    val setter = descriptor.setter
////                    setter == null || DescriptorVisibilities.isVisible(
////                        dispatchReceiver?.asReceiverValueForVisibilityChecks, setter,
////                        scopeTower.lexicalScope.ownerDescriptor, false
////                    )
////                }
//
//                callComponents.reflectionTypes.getKPropertyType(
//                    Annotations.EMPTY,
//                    argumentsAndReceivers,
//                    descriptorReturnType,
//                    mutable
//                ) to null

//            }
            is FunctionDescriptor -> {


                descriptor.toFunctionType() as UnwrappedType to null
            }

            else -> {
//                assert(!descriptor.isSupportedForCallableReference()) { "${descriptor::class} isn't supported to use in callable references actually, but it's listed in `isSupportedForCallableReference` method" }
                ErrorUtils.createErrorType(
                    ErrorTypeKind.UNSUPPORTED_CALLABLE_REFERENCE_TYPE,
                    descriptor.toString()
                ) to null
            }
        }
    }

    override fun createCandidate(
        towerCandidate: CandidateWithBoundDispatchReceiver,
        explicitReceiverKind: ExplicitReceiverKind,
        extensionReceiver: ReceiverValueWithSmartCastInfo?
    ): CallableReferenceResolutionCandidate {
        val dispatchCallableReceiver =
            towerCandidate.dispatchReceiver?.let {
                toCallableReceiver(
                    it,
                    explicitReceiverKind == ExplicitReceiverKind.DISPATCH_RECEIVER
                )
            }
        val extensionCallableReceiver = extensionReceiver?.let {
            toCallableReceiver(
                it,
                explicitReceiverKind == ExplicitReceiverKind.EXTENSION_RECEIVER
            )
        }
        val candidateDescriptor = towerCandidate.descriptor
        val diagnostics = SmartList<CangJieCallDiagnostic>()
//
        val (reflectionCandidateType, callableReferenceAdaptation) = buildReflectionType(
            candidateDescriptor,
            dispatchCallableReceiver,
            extensionCallableReceiver,
            expectedType,
            callComponents.builtIns,
        )

        fun createCallableReferenceCallCandidate(diagnostics: List<CangJieCallDiagnostic>) =
            CallableReferenceResolutionCandidate(
                candidateDescriptor, dispatchCallableReceiver, extensionCallableReceiver,
                explicitReceiverKind, reflectionCandidateType, callableReferenceAdaptation,
                cangjieCall, expectedType, callComponents, scopeTower, resolutionCallbacks, baseSystem
            ).also { diagnostics.forEach(it::addDiagnostic) }

//        if (callComponents.statelessCallbacks.isHiddenInResolution(descriptor, cangjieCall.call, resolutionCallbacks)) {
//            diagnostics.add(HiddenDescriptor)
//            return createCallableReferenceCallCandidate(diagnostics)
//        }
//
//        if (needCompatibilityResolveForCallableReference(callableReferenceAdaptation, descriptor)) {
//            markCandidateForCompatibilityResolve(diagnostics)
//        }
//
//        if (callableReferenceAdaptation != null && expectedType != null && hasNonTrivialAdaptation(callableReferenceAdaptation)) {
//            if (!expectedType.isFunctionType && !expectedType.isSuspendFunctionType) { // expectedType has some reflection type
//                diagnostics.add(AdaptedCallableReferenceIsUsedWithReflection(cangjieCall))
//            }
//        }
//
//        if (callableReferenceAdaptation != null &&
//            callableReferenceAdaptation.defaults != 0 &&
//            !callComponents.languageVersionSettings.supportsFeature(LanguageFeature.FunctionReferenceWithDefaultValueAsOtherType)
//        ) {
//            diagnostics.add(CallableReferencesDefaultArgumentUsed(cangjieCall, descriptor, callableReferenceAdaptation.defaults))
//        }
//
//        if (descriptor !is CallableMemberDescriptor) {
//            return createCallableReferenceCallCandidate(listOf(NotCallableMemberReference(cangjieCall, descriptor)))
//        }
//
//        if (descriptor is PropertyDescriptor && descriptor.isSyntheticEnumEntries()) {
//            diagnostics.add(LowerPriorityToPreserveCompatibility(needToReportWarning = false).asDiagnostic())
//        }

        diagnostics.addAll(towerCandidate.diagnostics)
        // todo smartcast on receiver diagnostic and CheckInstantiationOfAbstractClass

        return createCallableReferenceCallCandidate(diagnostics)
    }

    override fun createErrorCandidate(): CallableReferenceResolutionCandidate {
        val errorScope =
            ErrorUtils.createErrorScope(ErrorScopeKind.SCOPE_FOR_ERROR_RESOLUTION_CANDIDATE, cangjieCall.toString())
        val errorDescriptor = errorScope.getContributedFunctions(cangjieCall.rhsName, scopeTower.location).first()

        val (reflectionCandidateType, callableReferenceAdaptation) = buildReflectionType(
            errorDescriptor,
            dispatchReceiver = null,
            extensionReceiver = null,
            expectedType,
            callComponents.builtIns,
        )

        return CallableReferenceResolutionCandidate(
            errorDescriptor, dispatchReceiver = null, extensionReceiver = null,
            ExplicitReceiverKind.NO_EXPLICIT_RECEIVER, reflectionCandidateType, callableReferenceAdaptation,
            cangjieCall, expectedType, callComponents, scopeTower, resolutionCallbacks, baseSystem
        )
    }

    override fun createCandidate(
        towerCandidate: CandidateWithBoundDispatchReceiver,
        explicitReceiverKind: ExplicitReceiverKind,
        extensionReceiverCandidates: List<ReceiverValueWithSmartCastInfo>
    ): CallableReferenceResolutionCandidate {
        error("${this::class.simpleName} doesn't support candidates with multiple extension receiver candidates")

    }
}
