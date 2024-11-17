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

package com.linqingying.cangjie.resolve.calls.tower

import com.linqingying.cangjie.config.LanguageVersionSettings
import com.linqingying.cangjie.descriptors.CallableDescriptor
import com.linqingying.cangjie.descriptors.ClassDescriptor
import com.linqingying.cangjie.descriptors.TypeParameterDescriptor
import com.linqingying.cangjie.descriptors.ValueParameterDescriptor
import com.linqingying.cangjie.psi.CjExpression
import com.linqingying.cangjie.psi.ValueArgument
import com.linqingying.cangjie.resolve.calls.inference.components.FreshVariableNewTypeSubstitutor
import com.linqingying.cangjie.resolve.calls.inference.components.NewTypeSubstitutor
import com.linqingying.cangjie.resolve.calls.inference.model.*

import com.linqingying.cangjie.resolve.calls.model.*
import com.linqingying.cangjie.resolve.calls.results.ResolutionStatus
import com.linqingying.cangjie.resolve.calls.tasks.ExplicitReceiverKind
import com.linqingying.cangjie.resolve.calls.util.toResolutionStatus
import com.linqingying.cangjie.resolve.constants.IntegerValueTypeConstant
import com.linqingying.cangjie.resolve.scopes.receivers.ImplicitClassReceiver
import com.linqingying.cangjie.resolve.scopes.receivers.ReceiverValue
import com.linqingying.cangjie.types.CangJieType
import com.linqingying.cangjie.types.TypeApproximator
import com.linqingying.cangjie.types.TypeApproximatorConfiguration
import com.linqingying.cangjie.types.UnwrappedType


class NewResolvedCallImpl<D : CallableDescriptor>(
    override val resolvedCallAtom: ResolvedCallAtom,
    substitutor: NewTypeSubstitutor?,
    diagnostics: Collection<CangJieCallDiagnostic>,
    override val typeApproximator: TypeApproximator,
    override val languageVersionSettings: LanguageVersionSettings,
) : NewAbstractResolvedCall<D>() {
    private var dispatchReceiver = resolvedCallAtom.dispatchReceiverArgument?.receiver?.receiverValue
    private lateinit var resultingDescriptor: D
    override var diagnostics: Collection<CangJieCallDiagnostic> = diagnostics
        private set
    private var extensionReceiver = resolvedCallAtom.extensionReceiverArgument?.receiver?.receiverValue
    private var smartCastDispatchReceiverType: CangJieType? = null
    private var contextReceivers = resolvedCallAtom.contextReceiversArguments.map { it.receiver.receiverValue }
    private var expectedTypeForUnitConvertedArgumentMap: Map<ValueArgument, UnwrappedType>? = null
    private var expectedTypeForSuspendConvertedArgumentMap: Map<ValueArgument, UnwrappedType>? = null
    private var expectedTypeForSamConvertedArgumentMap: Map<ValueArgument, UnwrappedType>? = null
    private var argumentTypeForConstantConvertedMap: Map<CjExpression, IntegerValueTypeConstant>? = null

    override fun updateExtensionReceiverType(newType: CangJieType) {
        if (extensionReceiver?.type == newType) return
        extensionReceiver = extensionReceiver?.replaceType(newType)
    }
    private lateinit var typeArguments: List<UnwrappedType>

    @Suppress("UNCHECKED_CAST")
    override fun getCandidateDescriptor(): D = resolvedCallAtom.candidateDescriptor as D
    override fun getSmartCastDispatchReceiverType(): CangJieType? = smartCastDispatchReceiverType
    private fun calculateExpectedTypeForUnitConvertedArgumentMap(substitutor: NewTypeSubstitutor?) {
        expectedTypeForUnitConvertedArgumentMap = calculateExpectedTypeForConvertedArguments(
            resolvedCallAtom.argumentsWithUnitConversion, substitutor
        )
    }
    private fun calculateExpectedTypeForConvertedArguments(
        arguments: Map<CangJieCallArgument, UnwrappedType>,
        substitutor: NewTypeSubstitutor?,
    ): Map<ValueArgument, UnwrappedType>? {
        if (arguments.isEmpty()) return null

        val expectedTypeForConvertedArguments = hashMapOf<ValueArgument, UnwrappedType>()
        for ((argument, convertedType) in arguments) {
            val typeWithFreshVariables = resolvedCallAtom.freshVariablesSubstitutor.safeSubstitute(convertedType)
            val expectedType = substitutor?.safeSubstitute(typeWithFreshVariables) ?: typeWithFreshVariables
            expectedTypeForConvertedArguments[argument.psiCallArgument.valueArgument] = expectedType
        }

        return expectedTypeForConvertedArguments
    }
    fun updateExtensionReceiverWithSmartCastIfNeeded(smartCastExtensionReceiverType: CangJieType) {
        if (extensionReceiver is ImplicitClassReceiver) {
            extensionReceiver = CastImplicitClassReceiver(
                (extensionReceiver as ImplicitClassReceiver).classDescriptor,
                smartCastExtensionReceiverType,
            )
        }
    }
    fun setSmartCastDispatchReceiverType(smartCastDispatchReceiverType: CangJieType) {
        this.smartCastDispatchReceiverType = smartCastDispatchReceiverType
    }
    override fun getExplicitReceiverKind(): ExplicitReceiverKind = resolvedCallAtom.explicitReceiverKind

    fun getExpectedTypeForUnitConvertedArgument(valueArgument: ValueArgument): UnwrappedType? =
        expectedTypeForUnitConvertedArgumentMap?.get(valueArgument)


    override fun getExtensionReceiver(): ReceiverValue? = extensionReceiver


    override fun getStatus(): ResolutionStatus = getResultApplicability(diagnostics).toResolutionStatus()
    override fun getContextReceivers(): List<ReceiverValue> = contextReceivers



    fun getExpectedTypeForSamConvertedArgument(valueArgument: ValueArgument): UnwrappedType? =
        expectedTypeForSamConvertedArgumentMap?.get(valueArgument)

    override fun getTypeArguments(): Map<TypeParameterDescriptor, CangJieType> {
        val typeParameters = candidateDescriptor.typeParameters.takeIf { it.isNotEmpty() } ?: return emptyMap()
        return typeParameters.zip(typeArguments).toMap()
    }

    override fun getDispatchReceiver(): ReceiverValue? = dispatchReceiver


    override fun getResultingDescriptor(): D = resultingDescriptor


    override val psiCangJieCall: PSICangJieCall = resolvedCallAtom.atom.psiCangJieCall
    override val cangjieCall: CangJieCall = resolvedCallAtom.atom
    override val freshSubstitutor: FreshVariableNewTypeSubstitutor
        get() = resolvedCallAtom.freshVariablesSubstitutor
    override val argumentMappingByOriginal: Map<ValueParameterDescriptor, ResolvedCallArgument>
        get() = resolvedCallAtom.argumentMappingByOriginal

    fun updateDiagnostics(completedDiagnostics: Collection<CangJieCallDiagnostic>) {
        diagnostics = completedDiagnostics
    }
    fun getExpectedTypeForSuspendConvertedArgument(valueArgument: ValueArgument): UnwrappedType? =
        expectedTypeForSuspendConvertedArgumentMap?.get(valueArgument)
    fun getArgumentTypeForConstantConvertedArgument(valueArgument: ValueArgument): IntegerValueTypeConstant? {
        val expression = valueArgument.getArgumentExpression() ?: return null
        return argumentTypeForConstantConvertedMap?.get(expression)
    }
    override fun setResultingSubstitutor(substitutor: NewTypeSubstitutor?) {
        //clear cached values
        updateArgumentsMapping(null)
        updateValueArguments(null)

        substituteReceivers(substitutor)

        @Suppress("UNCHECKED_CAST")
        resultingDescriptor = substitutedResultingDescriptor(substitutor) as D

        typeArguments = freshSubstitutor.freshVariables.map {
            val substituted = (substitutor ?: FreshVariableNewTypeSubstitutor.Empty).safeSubstitute(it.defaultType)
            typeApproximator
                .approximateToSuperType(substituted, TypeApproximatorConfiguration.IntegerLiteralsTypesApproximation)
                ?: substituted
        }
//
        calculateExpectedTypeForSamConvertedArgumentMap(substitutor)
        calculateExpectedTypeForSuspendConvertedArgumentMap(substitutor)
        calculateExpectedTypeForUnitConvertedArgumentMap(substitutor)
        calculateExpectedTypeForConstantConvertedArgumentMap()
    }
    private fun calculateExpectedTypeForSamConvertedArgumentMap(substitutor: NewTypeSubstitutor?) {
        expectedTypeForSamConvertedArgumentMap = calculateExpectedTypeForConvertedArguments(
            resolvedCallAtom.argumentsWithConversion.mapValues { it.value.convertedTypeByCandidateParameter },
            substitutor
        )
    }
    private fun calculateExpectedTypeForConstantConvertedArgumentMap() {
        if (resolvedCallAtom.argumentsWithConstantConversion.isEmpty()) return

        val expectedTypeForConvertedArguments = hashMapOf<CjExpression, IntegerValueTypeConstant>()

        for ((argument, convertedConstant) in resolvedCallAtom.argumentsWithConstantConversion) {
            val expression = argument.psiExpression ?: continue
            expectedTypeForConvertedArguments[expression] = convertedConstant
        }

        argumentTypeForConstantConvertedMap = expectedTypeForConvertedArguments
    }

    private fun calculateExpectedTypeForSuspendConvertedArgumentMap(substitutor: NewTypeSubstitutor?) {
        expectedTypeForSuspendConvertedArgumentMap = calculateExpectedTypeForConvertedArguments(
            resolvedCallAtom.argumentsWithSuspendConversion, substitutor
        )
    }


    override fun updateDispatchReceiverType(newType: CangJieType) {
        if (dispatchReceiver?.type == newType) return
        dispatchReceiver = dispatchReceiver?.replaceType(newType)
    }
    private fun collectErrorPositions(): Map<ValueArgument, List<CangJieCallDiagnostic>> {
        val result = mutableListOf<Pair<ValueArgument, CangJieCallDiagnostic>>()

        fun ConstraintPosition.originalPosition(): ConstraintPosition =
            if (this is IncorporationConstraintPosition) {
                from.originalPosition()
            } else {
                this
            }

        diagnostics.forEach {
            val position = when (val error = it.constraintSystemError) {
                is NewConstraintError -> error.position.originalPosition()
//                is CapturedTypeFromSubtyping -> error.position.originalPosition()
//                is ConstrainingTypeIsError -> error.position.originalPosition()
                else -> null
            } as? ArgumentConstraintPositionImpl ?: return@forEach

            val argument = (position.argument as? PSICangJieCallArgument)?.valueArgument ?: return@forEach
            result += argument to it
        }

        return result.groupBy({ it.first }) { it.second }
    }
    override fun argumentToParameterMap(
        resultingDescriptor: CallableDescriptor,
        valueArguments: Map<ValueParameterDescriptor, ResolvedValueArgument>
    ): Map<ValueArgument, ArgumentMatchImpl> {
        val argumentErrors = collectErrorPositions()

        return LinkedHashMap<ValueArgument, ArgumentMatchImpl>().also { result ->
            for (parameter in resultingDescriptor.valueParameters) {
                val resolvedArgument = valueArguments[parameter] ?: continue
                for (argument in resolvedArgument.arguments) {
                    val status = argumentErrors[argument]?.let {
                        ArgumentMatchStatus.TYPE_MISMATCH
                    } ?: ArgumentMatchStatus.SUCCESS
                    result[argument] = ArgumentMatchImpl(parameter).apply { recordMatchStatus(status) }
                }
            }
        }
    }

    init {
        setResultingSubstitutor(substitutor)
    }
}
class CastImplicitClassReceiver(originalDescriptor: ClassDescriptor, val targetType: CangJieType) : ImplicitClassReceiver(originalDescriptor)
