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

import cn.cangnova.cangjie.descriptors.CallableDescriptor
import cn.cangnova.cangjie.descriptors.FunctionDescriptor
import cn.cangnova.cangjie.descriptors.ValueParameterDescriptor
import cn.cangnova.cangjie.descriptors.VariableDescriptor
import cn.cangnova.cangjie.resolve.calls.inference.components.FreshVariableNewTypeSubstitutor
import cn.cangnova.cangjie.resolve.calls.inference.components.NewTypeSubstitutor
import cn.cangnova.cangjie.resolve.calls.inference.model.ResolvedValueArgument
import cn.cangnova.cangjie.resolve.calls.model.CangJieCallDiagnostic
import cn.cangnova.cangjie.resolve.calls.model.ResolvedCallAtom
import cn.cangnova.cangjie.resolve.calls.model.VariableAsFunctionResolvedCall
import cn.cangnova.cangjie.types.CangJieType
import cn.cangnova.cangjie.types.TypeApproximator


class NewVariableAsFunctionResolvedCallImpl(
    override val variableCall: NewAbstractResolvedCall<VariableDescriptor>,
    override val functionCall: NewAbstractResolvedCall<FunctionDescriptor>,
) : VariableAsFunctionResolvedCall, NewAbstractResolvedCall<FunctionDescriptor>() {
    val baseCall: PSICangJieCallImpl = (functionCall.psiCangJieCall as PSICangJieCallForInvoke).baseCall

    override val resolvedCallAtom: ResolvedCallAtom? = functionCall.resolvedCallAtom
    override val psiCangJieCall: PSICangJieCall = functionCall.psiCangJieCall
    override val typeApproximator: TypeApproximator = functionCall.typeApproximator
    override val freshSubstitutor: FreshVariableNewTypeSubstitutor? = functionCall.freshSubstitutor

    override val cangjieCall = functionCall.cangjieCall
    override val languageVersionSettings = functionCall.languageVersionSettings
    override val argumentMappingByOriginal = functionCall.argumentMappingByOriginal

    override val diagnostics: Collection<CangJieCallDiagnostic> = functionCall.diagnostics

    override fun getStatus() = functionCall.status
    override fun getContextReceivers() = functionCall.contextReceivers

    override fun getTypeArguments() = functionCall.typeArguments


    override fun getCandidateDescriptor() = functionCall.candidateDescriptor
    override fun getSmartCastDispatchReceiverType() = functionCall.smartCastDispatchReceiverType

    override fun getExplicitReceiverKind() = functionCall.explicitReceiverKind


    override fun getExtensionReceiver() = functionCall.extensionReceiver


    override fun getResultingDescriptor() = functionCall.resultingDescriptor

    override fun getDispatchReceiver() = functionCall.dispatchReceiver

    override fun updateDispatchReceiverType(newType: CangJieType) = functionCall.updateDispatchReceiverType(newType)
    override fun argumentToParameterMap(
        resultingDescriptor: CallableDescriptor,
        valueArguments: Map<ValueParameterDescriptor, ResolvedValueArgument>
    ) = functionCall.argumentToParameterMap(resultingDescriptor, valueArguments)

    override fun updateExtensionReceiverType(newType: CangJieType) = functionCall.updateExtensionReceiverType(newType)

    override fun setResultingSubstitutor(substitutor: NewTypeSubstitutor?) {
        functionCall.setResultingSubstitutor(substitutor)
        variableCall.setResultingSubstitutor(substitutor)
    }
}

