/*
 * Copyright 2024 LinQingYing. and contributors.
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

package cn.cangnova.cangjie.resolve.calls.tower

import cn.cangnova.cangjie.config.LanguageVersionSettings
import cn.cangnova.cangjie.descriptors.CallableDescriptor
import cn.cangnova.cangjie.descriptors.TypeParameterDescriptor
import cn.cangnova.cangjie.descriptors.ValueParameterDescriptor
import cn.cangnova.cangjie.psi.ValueArgument
import cn.cangnova.cangjie.resolve.calls.inference.components.FreshVariableNewTypeSubstitutor
import cn.cangnova.cangjie.resolve.calls.inference.components.NewTypeSubstitutor
import cn.cangnova.cangjie.resolve.calls.inference.model.ResolvedValueArgument
import cn.cangnova.cangjie.resolve.calls.model.*
import cn.cangnova.cangjie.resolve.calls.smartcasts.DataFlowInfo
import cn.cangnova.cangjie.resolve.calls.tasks.ExplicitReceiverKind
import cn.cangnova.cangjie.resolve.calls.util.toResolutionStatus
import cn.cangnova.cangjie.resolve.scopes.receivers.ReceiverValue
import cn.cangnova.cangjie.types.CangJieType
import cn.cangnova.cangjie.types.TypeApproximator
import cn.cangnova.cangjie.types.TypeApproximatorConfiguration
import cn.cangnova.cangjie.types.UnwrappedType


class NewCallableReferenceResolvedCall<D : CallableDescriptor>(
    val resolvedAtom: ResolvedCallableReferenceAtom,
    override val typeApproximator: TypeApproximator,
    override val languageVersionSettings: LanguageVersionSettings,
    substitutor: NewTypeSubstitutor? = null,
) : NewAbstractResolvedCall<D>() {
    override val positionDependentApproximation: Boolean = true
    override val argumentMappingByOriginal: Map<ValueParameterDescriptor, ResolvedCallArgument> = emptyMap()
    override val diagnostics: Collection<CangJieCallDiagnostic> = emptyList()
    override fun updateExtensionReceiverType(newType: CangJieType) {
        if (extensionReceiver?.type == newType) return
        extensionReceiver = extensionReceiver?.replaceType(newType)
    }

    override val resolvedCallAtom: ResolvedCallableReferenceCallAtom?
        get() = when (resolvedAtom) {
            is ResolvedCallableReferenceCallAtom -> resolvedAtom
            is ResolvedCallableReferenceArgumentAtom -> resolvedAtom.candidate?.resolvedCall

        }

    override val psiCangJieCall: PSICangJieCall =
        when (resolvedAtom) {
            is ResolvedCallableReferenceCallAtom -> resolvedAtom.atom.psiCangJieCall
            is ResolvedCallableReferenceArgumentAtom -> resolvedAtom.atom.call.psiCangJieCall
        }

    override val freshSubstitutor: FreshVariableNewTypeSubstitutor?
        get() = when (resolvedAtom) {
            is ResolvedCallableReferenceCallAtom -> resolvedAtom.freshVariablesSubstitutor
            is ResolvedCallableReferenceArgumentAtom -> resolvedAtom.candidate?.freshVariablesSubstitutor
        }

    override val cangjieCall: CangJieCall?
        get() = when (resolvedAtom) {
            is ResolvedCallableReferenceArgumentAtom -> resolvedAtom.candidate?.cangjieCall?.call
            is ResolvedCallableReferenceCallAtom -> resolvedAtom.atom
        }

    private lateinit var resultingDescriptor: D
    private lateinit var typeArguments: List<UnwrappedType>

    private var extensionReceiver: ReceiverValue? = when (resolvedAtom) {
        is ResolvedCallableReferenceCallAtom -> resolvedAtom.extensionReceiverArgument?.receiverValue
        is ResolvedCallableReferenceArgumentAtom -> resolvedAtom.candidate?.extensionReceiver?.receiver?.receiverValue
    }

    private var dispatchReceiver = when (resolvedAtom) {
        is ResolvedCallableReferenceCallAtom -> resolvedAtom.dispatchReceiverArgument?.receiverValue
        is ResolvedCallableReferenceArgumentAtom -> resolvedAtom.candidate?.dispatchReceiver?.receiver?.receiverValue
//        else -> {
//            error("Unexpected resolved atom: $resolvedAtom")}
    }

    override fun getDispatchReceiver(): ReceiverValue? = dispatchReceiver


    @Suppress("UNCHECKED_CAST")
    override fun getCandidateDescriptor(): D = when (resolvedAtom) {
        is ResolvedCallableReferenceCallAtom -> resolvedAtom.candidateDescriptor as D
        is ResolvedCallableReferenceArgumentAtom -> resolvedAtom.candidate?.candidate as D
    }


    override fun getSmartCastDispatchReceiverType(): CangJieType? = null

    override fun getExplicitReceiverKind() = when (resolvedAtom) {
        is ResolvedCallableReferenceArgumentAtom ->
            resolvedAtom.candidate?.explicitReceiverKind ?: ExplicitReceiverKind.NO_EXPLICIT_RECEIVER

        is ResolvedCallableReferenceCallAtom -> resolvedAtom.explicitReceiverKind
    }

    override fun getArgumentMapping(valueArgument: ValueArgument): ArgumentMapping = ArgumentUnmapped


    override fun getExtensionReceiver(): ReceiverValue? = extensionReceiver


    override fun getResultingDescriptor(): D = resultingDescriptor


    override fun getStatus() = CandidateApplicability.RESOLVED.toResolutionStatus()
    override fun getContextReceivers() = emptyList<ReceiverValue>()


    override fun getDataFlowInfoForArguments(): DataFlowInfoForArguments =
        MutableDataFlowInfoForArguments.WithoutArgumentsCheck(DataFlowInfo.EMPTY)


    override fun getTypeArguments(): Map<TypeParameterDescriptor, CangJieType> {
        val typeParameters = candidateDescriptor.typeParameters.takeIf { it.isNotEmpty() } ?: return emptyMap()
        return typeParameters.zip(typeArguments).toMap()
    }

    override fun setResultingSubstitutor(substitutor: NewTypeSubstitutor?) {
        substituteReceivers(substitutor)

        @Suppress("UNCHECKED_CAST")
        resultingDescriptor = substitutedResultingDescriptor(substitutor) as D

        freshSubstitutor?.let { freshSubstitutor ->
            typeArguments = freshSubstitutor.freshVariables.map {
                val substituted = (substitutor ?: FreshVariableNewTypeSubstitutor.Empty).safeSubstitute(it.defaultType)
                typeApproximator.approximateToSuperType(substituted, TypeApproximatorConfiguration.IntegerLiteralsTypesApproximation)
                    ?: substituted
            }
        }
    }

    override fun updateDispatchReceiverType(newType: CangJieType) {
        if (dispatchReceiver?.type == newType) return
        dispatchReceiver = dispatchReceiver?.replaceType(newType)
    }

    override fun argumentToParameterMap(
        resultingDescriptor: CallableDescriptor,
        valueArguments: Map<ValueParameterDescriptor, ResolvedValueArgument>
    ): Map<ValueArgument, ArgumentMatchImpl> = emptyMap()


    init {
        setResultingSubstitutor(substitutor)
    }
}
