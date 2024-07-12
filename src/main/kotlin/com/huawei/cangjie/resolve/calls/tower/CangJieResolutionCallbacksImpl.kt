package com.huawei.cangjie.resolve.calls.tower

import com.huawei.cangjie.descriptors.BindingTrace
import com.huawei.cangjie.descriptors.ModuleDescriptor
import com.huawei.cangjie.resolve.MissingSupertypesResolver
import com.huawei.cangjie.resolve.TypeResolver
import com.huawei.cangjie.resolve.calls.ArgumentTypeResolver
import com.huawei.cangjie.resolve.calls.CangJieCallResolver
import com.huawei.cangjie.resolve.calls.components.CangJieResolutionCallbacks
import com.huawei.cangjie.resolve.calls.components.InferenceSession
import com.huawei.cangjie.resolve.calls.components.candidate.CallableReferenceResolutionCandidate
import com.huawei.cangjie.resolve.calls.context.BasicCallResolutionContext
import com.huawei.cangjie.resolve.calls.inference.NewConstraintSystem
import com.huawei.cangjie.resolve.calls.inference.model.ConstraintStorage
import com.huawei.cangjie.resolve.calls.inference.model.TypeVariableTypeConstructor
import com.huawei.cangjie.resolve.calls.model.*
import com.huawei.cangjie.resolve.calls.smartcasts.DataFlowValueFactory
import com.huawei.cangjie.resolve.constants.evaluate.ConstantExpressionEvaluator
import com.huawei.cangjie.resolve.deprecation.DeprecationResolver
import com.huawei.cangjie.types.CangJieType
import com.huawei.cangjie.types.UnwrappedType
import com.huawei.cangjie.types.expressions.DoubleColonExpressionResolver
import com.huawei.cangjie.types.expressions.ExpressionTypingServices


class CangJieResolutionCallbacksImpl(
    val trace: BindingTrace,
    private val expressionTypingServices: ExpressionTypingServices,
//    private val typeApproximator: TypeApproximator,
    private val argumentTypeResolver: ArgumentTypeResolver,
//    private val languageVersionSettings: LanguageVersionSettings,
    private val cangjieToResolvedCallTransformer: CangJieToResolvedCallTransformer,
    private val dataFlowValueFactory: DataFlowValueFactory,
    override val inferenceSession: InferenceSession,
    private val constantExpressionEvaluator: ConstantExpressionEvaluator,
    private val typeResolver: TypeResolver,
    private val psiCallResolver: PSICallResolver,
//    private val postponedArgumentsAnalyzer: PostponedArgumentsAnalyzer,
//    private val cangjieConstraintSystemCompleter: CangJieConstraintSystemCompleter,
    private val callComponents: CangJieCallComponents,
    private val doubleColonExpressionResolver: DoubleColonExpressionResolver,
    private val deprecationResolver: DeprecationResolver,
    private val moduleDescriptor: ModuleDescriptor,
    private val topLevelCallContext: BasicCallResolutionContext,
    private val missingSupertypesResolver: MissingSupertypesResolver,
    private val cangjieCallResolver: CangJieCallResolver,
//    private val resultTypeResolver: ResultTypeResolver,
) : CangJieResolutionCallbacks {
    override fun resolveCallableReferenceArgument(
        argument: CallableReferenceCangJieCallArgument,
        expectedType: UnwrappedType?,
        baseSystem: ConstraintStorage
    ): Collection<CallableReferenceResolutionCandidate> {
        TODO("Not yet implemented")
    }
    override fun getCandidateFactoryForInvoke(
        scopeTower: ImplicitScopeTower,
        cangjieCall: CangJieCall
    ): PSICallResolver.FactoryProviderForInvoke =
        psiCallResolver.FactoryProviderForInvoke(topLevelCallContext, scopeTower, cangjieCall as PSICangJieCallImpl)

    override fun findResultType(
        constraintSystem: NewConstraintSystem,
        typeVariable: TypeVariableTypeConstructor
    ): CangJieType? {
        TODO("Not yet implemented")
    }

    override fun createEmptyConstraintSystem(): NewConstraintSystem {
        TODO("Not yet implemented")
    }

    override fun bindStubResolvedCallForCandidate(candidate: ResolvedCallAtom) {
        TODO("Not yet implemented")
    }

    override fun isCompileTimeConstant(resolvedAtom: ResolvedCallAtom, expectedType: UnwrappedType): Boolean {
        TODO("Not yet implemented")
    }

    override fun getExpectedTypeFromAsExpressionAndRecordItInTrace(resolvedAtom: ResolvedCallAtom): UnwrappedType? {
        TODO("Not yet implemented")
    }

    override fun disableContractsIfNecessary(resolvedAtom: ResolvedCallAtom) {
        TODO("Not yet implemented")
    }

    override fun getLhsResult(call: CangJieCall): LHSResult {
        TODO("Not yet implemented")
    }
}
