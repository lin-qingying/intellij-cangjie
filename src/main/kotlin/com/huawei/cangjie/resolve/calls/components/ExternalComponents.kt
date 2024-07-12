package com.huawei.cangjie.resolve.calls.components

import com.huawei.cangjie.descriptors.CallableDescriptor
import com.huawei.cangjie.descriptors.DeclarationDescriptor
import com.huawei.cangjie.descriptors.ValueParameterDescriptor
import com.huawei.cangjie.resolve.calls.components.candidate.CallableReferenceResolutionCandidate
import com.huawei.cangjie.resolve.calls.components.candidate.ResolutionCandidate
import com.huawei.cangjie.resolve.calls.inference.NewConstraintSystem
import com.huawei.cangjie.resolve.calls.inference.model.ConstraintStorage
import com.huawei.cangjie.resolve.calls.inference.model.TypeVariableTypeConstructor
import com.huawei.cangjie.resolve.calls.model.*
import com.huawei.cangjie.resolve.calls.tower.CandidateFactoryProviderForInvoke
import com.huawei.cangjie.resolve.calls.tower.ImplicitScopeTower
import com.huawei.cangjie.types.CangJieType
import com.huawei.cangjie.types.UnwrappedType

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

//    fun createConstraintSystemForOverloadResolution(
//        constraintInjector: ConstraintInjector, builtIns: CangJieBuiltIns
//    ): SimpleConstraintSystem
}

// This components hold state (trace). Work with this carefully.
interface CangJieResolutionCallbacks {
//    fun analyzeAndGetLambdaReturnArguments(
//        lambdaArgument: LambdaCangJieCallArgument,
//        isSuspend: Boolean,
//        receiverType: UnwrappedType?,
//        contextReceiversTypes: List<UnwrappedType>,
//        parameters: List<UnwrappedType>,
//        expectedReturnType: UnwrappedType?, // null means, that return type is not proper i.e. it depends on some type variables
//        annotations: Annotations,
//        stubsForPostponedVariables: Map<NewTypeVariable, StubTypeForBuilderInference>,
//    ): ReturnArgumentsAnalysisResult

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

    fun isCompileTimeConstant(resolvedAtom: ResolvedCallAtom, expectedType: UnwrappedType): Boolean

    val inferenceSession: InferenceSession

    fun getExpectedTypeFromAsExpressionAndRecordItInTrace(resolvedAtom: ResolvedCallAtom): UnwrappedType?

    fun disableContractsIfNecessary(resolvedAtom: ResolvedCallAtom)

    fun getLhsResult(call: CangJieCall): LHSResult

//    fun convertSignedConstantToUnsigned(argument: CangJieCallArgument): IntegerValueTypeConstant?

//    fun recordInlinabilityOfLambda(atom: Set<Map.Entry<SimpleResolutionCandidate, ResolvedLambdaAtom>>)
}
