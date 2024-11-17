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

package com.linqingying.cangjie.resolve.calls.model

import com.linqingying.cangjie.config.LanguageFeature
import com.linqingying.cangjie.descriptors.CallableDescriptor
import com.linqingying.cangjie.descriptors.ValueParameterDescriptor
import com.linqingying.cangjie.resolve.calls.components.TypeArgumentsToParametersMapper
import com.linqingying.cangjie.resolve.calls.components.candidate.CallableReferenceResolutionCandidate
import com.linqingying.cangjie.resolve.calls.components.candidate.ResolutionCandidate
import com.linqingying.cangjie.resolve.calls.inference.components.FreshVariableNewTypeSubstitutor
import com.linqingying.cangjie.resolve.calls.inference.components.NewTypeSubstitutor
import com.linqingying.cangjie.resolve.calls.inference.model.ConstraintSystemError
import com.linqingying.cangjie.resolve.calls.tasks.ExplicitReceiverKind
import com.linqingying.cangjie.resolve.constants.IntegerValueTypeConstant
import com.linqingying.cangjie.types.UnwrappedType

/**
 * 表示解析过程的一个步骤
 * 所有解析过程都需要继承该类，并且需要实现process方法
 */
abstract class ResolutionPart {
    /**
     * 处理一个过程
     * @param workIndex: Int 该步骤的索引
     */
    abstract fun ResolutionCandidate.process(workIndex: Int)

    open fun ResolutionCandidate.workCount(): Int = 1

    // helper functions
    protected inline val ResolutionCandidate.candidateDescriptor get() = resolvedCall.candidateDescriptor
    protected inline val ResolutionCandidate.cangjieCall get() = resolvedCall.atom
}

fun CangJieDiagnosticsHolder.addDiagnosticIfNotNull(diagnostic: CangJieCallDiagnostic?) {
    diagnostic?.let { addDiagnostic(it) }
}
open class MutableResolvedCallAtom(
    override val atom: CangJieCall,
    originalCandidateDescriptor: CallableDescriptor, // original candidate descriptor
    override val explicitReceiverKind: ExplicitReceiverKind,
    override val dispatchReceiverArgument: SimpleCangJieCallArgument?,
    override var extensionReceiverArgument: SimpleCangJieCallArgument?,
    override val extensionReceiverArgumentCandidates: List<SimpleCangJieCallArgument>?,
    open val reflectionCandidateType: UnwrappedType? = null,
    open val candidate: CallableReferenceResolutionCandidate? = null
) : ResolvedCallAtom() {
    private var _candidateDescriptor = originalCandidateDescriptor
    private var unitAdapterMap: HashMap<CangJieCallArgument, UnwrappedType>? = null
    private var suspendAdapterMap: HashMap<CangJieCallArgument, UnwrappedType>? = null
    override lateinit var typeArgumentMappingByOriginal: TypeArgumentsToParametersMapper.TypeArgumentsMapping

    override val candidateDescriptor: CallableDescriptor
        get() = _candidateDescriptor
    private var signedUnsignedConstantConversions: HashMap<CangJieCallArgument, IntegerValueTypeConstant>? = null

    override var contextReceiversArguments: List<SimpleCangJieCallArgument> = listOf()
    override lateinit var argumentMappingByOriginal: Map<ValueParameterDescriptor, ResolvedCallArgument>
    override lateinit var freshVariablesSubstitutor: FreshVariableNewTypeSubstitutor
    override val argumentsWithSuspendConversion: Map<CangJieCallArgument, UnwrappedType>
        get() = suspendAdapterMap ?: emptyMap()

    override lateinit var knownParametersSubstitutor: NewTypeSubstitutor

    fun registerArgumentWithSuspendConversion(argument: CangJieCallArgument, convertedType: UnwrappedType) {
        if (suspendAdapterMap == null)
            suspendAdapterMap = hashMapOf()

        suspendAdapterMap!![argument] = convertedType
    }

    lateinit var argumentToCandidateParameter: Map<CangJieCallArgument, ValueParameterDescriptor>
    private var samAdapterMap: HashMap<CangJieCallArgument, SamConversionDescription>? = null

    override val argumentsWithConversion: Map<CangJieCallArgument, SamConversionDescription>
        get() = samAdapterMap ?: emptyMap()

//    override val argumentsWithSuspendConversion: Map<CangJieCallArgument, UnwrappedType>
//        get() = suspendAdapterMap ?: emptyMap()

    val hasSamConversion: Boolean
        get() = samAdapterMap != null

    override val argumentsWithUnitConversion: Map<CangJieCallArgument, UnwrappedType>
        get() = unitAdapterMap ?: emptyMap()
    override val argumentsWithConstantConversion: Map<CangJieCallArgument, IntegerValueTypeConstant>
        get() = signedUnsignedConstantConversions ?: emptyMap()

    override fun setCandidateDescriptor(newCandidateDescriptor: CallableDescriptor) {
        if (newCandidateDescriptor == candidateDescriptor) return
        _candidateDescriptor = newCandidateDescriptor
    }
    fun registerArgumentWithConstantConversion(argument: CangJieCallArgument, convertedConstant: IntegerValueTypeConstant) {
        if (signedUnsignedConstantConversions == null)
            signedUnsignedConstantConversions = hashMapOf()

        signedUnsignedConstantConversions!![argument] = convertedConstant
    }
    fun registerArgumentWithSamConversion(argument: CangJieCallArgument, samConversionDescription: SamConversionDescription) {
        if (samAdapterMap == null)
            samAdapterMap = hashMapOf()

        samAdapterMap!![argument] = samConversionDescription
    }
    override fun toString(): String = "$atom, candidate = $candidateDescriptor"


    fun registerArgumentWithUnitConversion(argument: CangJieCallArgument, convertedType: UnwrappedType) {
        if (unitAdapterMap == null)
            unitAdapterMap = hashMapOf()

        unitAdapterMap!![argument] = convertedType
    }
    public override fun setAnalyzedResults(subResolvedAtoms: List<ResolvedAtom>) {
        super.setAnalyzedResults(subResolvedAtoms)
    }
}

interface CangJieDiagnosticsHolder {
    fun addDiagnostic(diagnostic: CangJieCallDiagnostic)

    class SimpleHolder : CangJieDiagnosticsHolder {
        private val diagnostics = arrayListOf<CangJieCallDiagnostic>()

        override fun addDiagnostic(diagnostic: CangJieCallDiagnostic) {
            diagnostics.add(diagnostic)
        }

        fun getDiagnostics(): List<CangJieCallDiagnostic> = diagnostics
    }
}


class ResolvedCallableReferenceCallAtom(
    atom: CangJieCall,
    candidateDescriptor: CallableDescriptor,
    explicitReceiverKind: ExplicitReceiverKind,
    dispatchReceiverArgument: SimpleCangJieCallArgument?,
    extensionReceiverArgument: SimpleCangJieCallArgument?,
    reflectionCandidateType: UnwrappedType? = null,
    candidate: CallableReferenceResolutionCandidate? = null
) : MutableResolvedCallAtom(
    atom, candidateDescriptor, explicitReceiverKind, dispatchReceiverArgument, extensionReceiverArgument, emptyList(), reflectionCandidateType, candidate
), ResolvedCallableReferenceAtom


fun CangJieDiagnosticsHolder.addError(error: ConstraintSystemError) {
    addDiagnostic(error.asDiagnostic())
}
fun ResolutionCandidate.markCandidateForCompatibilityResolve(needToReportWarning: Boolean = true) {
//    if (callComponents.languageVersionSettings.supportsFeature(LanguageFeature.DisableCompatibilityModeForNewInference)) return
//    addDiagnostic(LowerPriorityToPreserveCompatibility(needToReportWarning).asDiagnostic())
}
