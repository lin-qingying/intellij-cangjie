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

package cn.cangnova.cangjie.resolve.calls.components

import cn.cangnova.cangjie.builtins.CangJieBuiltIns
import cn.cangnova.cangjie.descriptors.CallableDescriptor
import cn.cangnova.cangjie.descriptors.DeclarationDescriptor
import cn.cangnova.cangjie.descriptors.ValueParameterDescriptor
import cn.cangnova.cangjie.descriptors.annotations.Annotations
import cn.cangnova.cangjie.resolve.calls.components.candidate.CallableReferenceResolutionCandidate
import cn.cangnova.cangjie.resolve.calls.components.candidate.ResolutionCandidate
import cn.cangnova.cangjie.resolve.calls.inference.NewConstraintSystem
import cn.cangnova.cangjie.resolve.calls.inference.components.ConstraintInjector
import cn.cangnova.cangjie.resolve.calls.inference.model.ConstraintStorage
import cn.cangnova.cangjie.resolve.calls.inference.model.NewTypeVariable
import cn.cangnova.cangjie.resolve.calls.inference.model.TypeVariableTypeConstructor
import cn.cangnova.cangjie.resolve.calls.model.*
import cn.cangnova.cangjie.resolve.calls.results.SimpleConstraintSystem
import cn.cangnova.cangjie.resolve.calls.tower.CandidateFactoryProviderForInvoke
import cn.cangnova.cangjie.resolve.calls.tower.ImplicitScopeTower
import cn.cangnova.cangjie.resolve.constants.IntegerValueTypeConstant
import cn.cangnova.cangjie.types.CangJieType
import cn.cangnova.cangjie.types.StubTypeForBuilderInference
import cn.cangnova.cangjie.types.UnwrappedType

interface CangJieResolutionStatelessCallbacks {
    fun isDescriptorFromSource(descriptor: CallableDescriptor): Boolean
    fun isInfixCall(cangjieCall: CangJieCall): Boolean
    fun isOperatorCall(cangjieCall: CangJieCall): Boolean
    fun isSuperOrDelegatingConstructorCall(cangjieCall: CangJieCall): Boolean
    fun isHiddenInResolution(
        descriptor: DeclarationDescriptor,
        cangjieCallArgument: CangJieCallArgument,
        resolutionCallbacks: CangJieResolutionCallbacks
    ): Boolean

    fun isHiddenInResolution(
        descriptor: DeclarationDescriptor, cangjieCall: CangJieCall, resolutionCallbacks: CangJieResolutionCallbacks
    ): Boolean

    fun isSuperExpression(receiver: SimpleCangJieCallArgument?): Boolean
    fun getScopeTowerForCallableReferenceArgument(argument: CallableReferenceCangJieCallArgument): ImplicitScopeTower
    fun getVariableCandidateIfInvoke(functionCall: CangJieCall): ResolutionCandidate?
    fun isBuilderInferenceCall(argument: CangJieCallArgument, parameter: ValueParameterDescriptor): Boolean
//    fun isApplicableCallForBuilderInference(descriptor: CallableDescriptor, languageVersionSettings: LanguageVersionSettings): Boolean

    fun isOldIntersectionIsEmpty(types: Collection<CangJieType>): Boolean

    fun createConstraintSystemForOverloadResolution(
        constraintInjector: ConstraintInjector, builtIns: CangJieBuiltIns
    ): SimpleConstraintSystem
}

// This components hold state (trace). Work with this carefully.
interface CangJieResolutionCallbacks {
    fun analyzeAndGetLambdaReturnArguments(
        lambdaArgument: LambdaCangJieCallArgument,

        receiverType: UnwrappedType?,
        contextReceiversTypes: List<UnwrappedType>,
        parameters: List<UnwrappedType>,
        expectedReturnType: UnwrappedType?, // null means, that return type is not proper i.e. it depends on some type variables
        annotations: Annotations,
        stubsForPostponedVariables: Map<NewTypeVariable, StubTypeForBuilderInference>,
    ): ReturnArgumentsAnalysisResult

    fun getCandidateFactoryForInvoke(
        scopeTower: ImplicitScopeTower,
        cangjieCall: CangJieCall,
    ): CandidateFactoryProviderForInvoke<ResolutionCandidate>

    fun resolveCallableReferenceArgument(
        argument: CallableReferenceCangJieCallArgument,
        expectedType: UnwrappedType?,
        baseSystem: ConstraintStorage,
    ): Collection<CallableReferenceResolutionCandidate>

    fun findResultType(constraintSystem: NewConstraintSystem, typeVariable: TypeVariableTypeConstructor): CangJieType?

    fun createEmptyConstraintSystem(): NewConstraintSystem

    fun bindStubResolvedCallForCandidate(candidate: ResolvedCallAtom)

    fun convertSignedConstantToUnsigned(argument: CangJieCallArgument): IntegerValueTypeConstant?

    val inferenceSession: InferenceSession

    fun getExpectedTypeFromAsExpressionAndRecordItInTrace(resolvedAtom: ResolvedCallAtom): UnwrappedType?

    fun disableContractsIfNecessary(resolvedAtom: ResolvedCallAtom)

    fun getLhsResult(call: CangJieCall): LHSResult

//    fun convertSignedConstantToUnsigned(argument: CangJieCallArgument): IntegerValueTypeConstant?

//    fun recordInlinabilityOfLambda(atom: Set<Map.Entry<SimpleResolutionCandidate, ResolvedLambdaAtom>>)
}
data class ReturnArgumentsInfo(
    val nonErrorArguments: List<CangJieCallArgument>,
    val lastExpression: CangJieCallArgument?,
    val lastExpressionCoercedToUnit: Boolean,
    val returnArgumentsExist: Boolean
) {
    companion object {
        val empty = ReturnArgumentsInfo(emptyList(), null, lastExpressionCoercedToUnit = false, returnArgumentsExist = false)
    }
}
data class ReturnArgumentsAnalysisResult(
    val returnArgumentsInfo: ReturnArgumentsInfo,
    val inferenceSession: InferenceSession?,
    val hasInapplicableCallForBuilderInference: Boolean = false
)
