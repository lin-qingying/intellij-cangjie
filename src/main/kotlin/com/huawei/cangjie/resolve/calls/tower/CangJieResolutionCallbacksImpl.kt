package com.huawei.cangjie.resolve.calls.tower

import com.huawei.cangjie.config.LanguageVersionSettings
import com.huawei.cangjie.descriptors.BindingTrace
import com.huawei.cangjie.descriptors.CallableDescriptor
import com.huawei.cangjie.descriptors.FunctionDescriptor
import com.huawei.cangjie.descriptors.ModuleDescriptor
import com.huawei.cangjie.lexer.CjTokens
import com.huawei.cangjie.psi.psiUtil.getBinaryWithTypeParent
import com.huawei.cangjie.resolve.BindingContext
import com.huawei.cangjie.resolve.MissingSupertypesResolver
import com.huawei.cangjie.resolve.TypeResolver
import com.huawei.cangjie.resolve.calls.ArgumentTypeResolver
import com.huawei.cangjie.resolve.calls.CangJieCallResolver
import com.huawei.cangjie.resolve.calls.components.CangJieResolutionCallbacks
import com.huawei.cangjie.resolve.calls.components.InferenceSession
import com.huawei.cangjie.resolve.calls.components.NewConstraintSystemImpl
import com.huawei.cangjie.resolve.calls.components.candidate.CallableReferenceResolutionCandidate
import com.huawei.cangjie.resolve.calls.context.BasicCallResolutionContext
import com.huawei.cangjie.resolve.calls.inference.NewConstraintSystem
import com.huawei.cangjie.resolve.calls.inference.components.CangJieConstraintSystemCompleter
import com.huawei.cangjie.resolve.calls.inference.components.ResultTypeResolver
import com.huawei.cangjie.resolve.calls.inference.components.TypeVariableDirectionCalculator
import com.huawei.cangjie.resolve.calls.inference.model.ConstraintStorage
import com.huawei.cangjie.resolve.calls.inference.model.TypeVariableTypeConstructor
import com.huawei.cangjie.resolve.calls.model.*
import com.huawei.cangjie.resolve.calls.smartcasts.DataFlowValueFactory
import com.huawei.cangjie.resolve.constants.evaluate.ConstantExpressionEvaluator
import com.huawei.cangjie.resolve.deprecation.DeprecationResolver
import com.huawei.cangjie.resolve.descriptorUtil.isFunctionForExpectTypeFromCastFeature
import com.huawei.cangjie.types.CangJieType
import com.huawei.cangjie.types.TypeApproximator
import com.huawei.cangjie.types.UnwrappedType
import com.huawei.cangjie.types.expressions.DoubleColonExpressionResolver
import com.huawei.cangjie.types.expressions.ExpressionTypingServices


class CangJieResolutionCallbacksImpl(
    val trace: BindingTrace,
    private val expressionTypingServices: ExpressionTypingServices,
    private val typeApproximator: TypeApproximator,
    private val argumentTypeResolver: ArgumentTypeResolver,
    private val languageVersionSettings: LanguageVersionSettings,
    private val cangjieToResolvedCallTransformer: CangJieToResolvedCallTransformer,
    private val dataFlowValueFactory: DataFlowValueFactory,
    override val inferenceSession: InferenceSession,
    private val constantExpressionEvaluator: ConstantExpressionEvaluator,
    private val typeResolver: TypeResolver,
    private val psiCallResolver: PSICallResolver,
//    private val postponedArgumentsAnalyzer: PostponedArgumentsAnalyzer,
    private val cangjieConstraintSystemCompleter: CangJieConstraintSystemCompleter,
    private val callComponents: CangJieCallComponents,
    private val doubleColonExpressionResolver: DoubleColonExpressionResolver,
    private val deprecationResolver: DeprecationResolver,
    private val moduleDescriptor: ModuleDescriptor,
    private val topLevelCallContext: BasicCallResolutionContext,
    private val missingSupertypesResolver: MissingSupertypesResolver,
    private val cangjieCallResolver: CangJieCallResolver,
    private val resultTypeResolver: ResultTypeResolver,
) : CangJieResolutionCallbacks {
    override fun resolveCallableReferenceArgument(
        argument: CallableReferenceCangJieCallArgument,
        expectedType: UnwrappedType?,
        baseSystem: ConstraintStorage
    ): Collection<CallableReferenceResolutionCandidate> =
        cangjieCallResolver.resolveCallableReferenceArgument(argument, expectedType, baseSystem, this)

    override fun getCandidateFactoryForInvoke(
        scopeTower: ImplicitScopeTower,
        cangjieCall: CangJieCall
    ): PSICallResolver.FactoryProviderForInvoke =
        psiCallResolver.FactoryProviderForInvoke(topLevelCallContext, scopeTower, cangjieCall as PSICangJieCallImpl)

    override fun findResultType(
        constraintSystem: NewConstraintSystem,
        typeVariable: TypeVariableTypeConstructor
    ): CangJieType? {
        val variableWithConstraints =
            constraintSystem.getBuilder().currentStorage().notFixedTypeVariables[typeVariable] ?: return null
        return resultTypeResolver.findResultType(
            constraintSystem.asConstraintSystemCompleterContext(),
            variableWithConstraints,
            TypeVariableDirectionCalculator.ResolveDirection.UNKNOWN
        ) as CangJieType
    }

    override fun createEmptyConstraintSystem(): NewConstraintSystem = NewConstraintSystemImpl(
        callComponents.constraintInjector,
        callComponents.builtIns,
        callComponents.cangjieTypeRefiner,
        callComponents.languageVersionSettings
    )

    override fun bindStubResolvedCallForCandidate(candidate: ResolvedCallAtom) {
        cangjieToResolvedCallTransformer.createStubResolvedCallAndWriteItToTrace<CallableDescriptor>(
            candidate, trace, emptyList(), substitutor = null
        )
    }

//    override fun isCompileTimeConstant(resolvedAtom: ResolvedCallAtom, expectedType: UnwrappedType): Boolean {
//        TODO("Not yet implemented")
//    }

    override fun getExpectedTypeFromAsExpressionAndRecordItInTrace(resolvedAtom: ResolvedCallAtom): UnwrappedType? {
        val candidateDescriptor = resolvedAtom.candidateDescriptor as? FunctionDescriptor ?: return null
        val call = (resolvedAtom.atom as? PSICangJieCall)?.psiCall ?: return null

        if (call.typeArgumentList != null || !candidateDescriptor.isFunctionForExpectTypeFromCastFeature()) return null
        val binaryParent = call.calleeExpression?.getBinaryWithTypeParent() ?: return null
        val operationType = binaryParent.operationReference.getReferencedNameElementType().takeIf {
            it == CjTokens.AS_KEYWORD
        } ?: return null

        val leftType = trace.get(BindingContext.TYPE, binaryParent.right ?: return null) ?: return null
        val expectedType = /*if (operationType == CjTokens.AS_SAFE) leftType.makeOptional() else*/ leftType
        val resultType = expectedType.unwrap()
        trace.record(BindingContext.CAST_TYPE_USED_AS_EXPECTED_TYPE, binaryParent)
        return resultType
    }

    override fun disableContractsIfNecessary(resolvedAtom: ResolvedCallAtom) {
//        val atom = resolvedAtom.atom as? PSICangJieCall ?: return
//        disableContractsInsideContractsBlock(atom.psiCall, resolvedAtom.candidateDescriptor, topLevelCallContext.scope, trace)

    }

    override fun getLhsResult(call: CangJieCall): LHSResult {
        return LHSResult.Empty
//        val callableReferenceExpression = call.extractCallableReferenceExpression()
//            ?: throw IllegalStateException("Not a callable reference")
//        val (_, lhsResult) = psiCallResolver.getLhsResult(topLevelCallContext, callableReferenceExpression)
//        return lhsResult
    }
}
