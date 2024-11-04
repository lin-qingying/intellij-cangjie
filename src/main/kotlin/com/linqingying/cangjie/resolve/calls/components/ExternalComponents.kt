package com.linqingying.cangjie.resolve.calls.components

import com.linqingying.cangjie.builtins.CangJieBuiltIns
import com.linqingying.cangjie.descriptors.CallableDescriptor
import com.linqingying.cangjie.descriptors.DeclarationDescriptor
import com.linqingying.cangjie.descriptors.ValueParameterDescriptor
import com.linqingying.cangjie.descriptors.annotations.Annotations
import com.linqingying.cangjie.resolve.calls.components.candidate.CallableReferenceResolutionCandidate
import com.linqingying.cangjie.resolve.calls.components.candidate.ResolutionCandidate
import com.linqingying.cangjie.resolve.calls.inference.NewConstraintSystem
import com.linqingying.cangjie.resolve.calls.inference.components.ConstraintInjector
import com.linqingying.cangjie.resolve.calls.inference.model.ConstraintStorage
import com.linqingying.cangjie.resolve.calls.inference.model.NewTypeVariable
import com.linqingying.cangjie.resolve.calls.inference.model.TypeVariableTypeConstructor
import com.linqingying.cangjie.resolve.calls.model.*
import com.linqingying.cangjie.resolve.calls.results.SimpleConstraintSystem
import com.linqingying.cangjie.resolve.calls.tower.CandidateFactoryProviderForInvoke
import com.linqingying.cangjie.resolve.calls.tower.ImplicitScopeTower
import com.linqingying.cangjie.resolve.constants.IntegerValueTypeConstant
import com.linqingying.cangjie.types.CangJieType
import com.linqingying.cangjie.types.StubTypeForBuilderInference
import com.linqingying.cangjie.types.UnwrappedType

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
