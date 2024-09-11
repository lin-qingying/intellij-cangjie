package com.huawei.cangjie.resolve.calls.inference

import com.huawei.cangjie.builtins.CangJieBuiltIns
import com.huawei.cangjie.descriptors.BindingTrace
import com.huawei.cangjie.descriptors.CallableDescriptor
import com.huawei.cangjie.descriptors.ModuleDescriptor
import com.huawei.cangjie.psi.CjElement
import com.huawei.cangjie.psi.psiUtil.anyDescendantOfType
import com.huawei.cangjie.resolve.MissingSupertypesResolver
import com.huawei.cangjie.resolve.calls.ArgumentTypeResolver
import com.huawei.cangjie.resolve.calls.components.InferenceSession
import com.huawei.cangjie.resolve.calls.components.NewConstraintSystemImpl
import com.huawei.cangjie.resolve.calls.components.PostponedArgumentsAnalyzer
import com.huawei.cangjie.resolve.calls.context.BasicCallResolutionContext
import com.huawei.cangjie.resolve.calls.inference.components.CangJieConstraintSystemCompleter
import com.huawei.cangjie.resolve.calls.inference.model.NewTypeVariable
import com.huawei.cangjie.resolve.calls.model.CangJieCallComponents
import com.huawei.cangjie.resolve.calls.model.LambdaCangJieCallArgument
import com.huawei.cangjie.resolve.calls.model.SingleCallResolutionResult
import com.huawei.cangjie.resolve.calls.tower.*
import com.huawei.cangjie.resolve.calls.util.FakeCallableDescriptorForObject
import com.huawei.cangjie.resolve.deprecation.DeprecationResolver
import com.huawei.cangjie.types.StubTypeForBuilderInference
import com.huawei.cangjie.types.TypeApproximator
import com.huawei.cangjie.types.expressions.ExpressionTypingServices

class BuilderInferenceSession(
    psiCallResolver: PSICallResolver,
    postponedArgumentsAnalyzer: PostponedArgumentsAnalyzer,
    kotlinConstraintSystemCompleter: CangJieConstraintSystemCompleter,
    callComponents: CangJieCallComponents,
    builtIns: CangJieBuiltIns,
    private val topLevelCallContext: BasicCallResolutionContext,
    private val stubsForPostponedVariables: Map<NewTypeVariable, StubTypeForBuilderInference>,
    private val trace: BindingTrace,
    private val kotlinToResolvedCallTransformer: CangJieToResolvedCallTransformer,
    private val expressionTypingServices: ExpressionTypingServices,
    private val argumentTypeResolver: ArgumentTypeResolver,

    private val deprecationResolver: DeprecationResolver,
    private val moduleDescriptor: ModuleDescriptor,
    private val typeApproximator: TypeApproximator,
    private val missingSupertypesResolver: MissingSupertypesResolver,
    private val lambdaArgument: LambdaCangJieCallArgument
) : StubTypesBasedInferenceSession<CallableDescriptor>(
    psiCallResolver, postponedArgumentsAnalyzer, kotlinConstraintSystemCompleter, callComponents, builtIns
) {
    private val commonSystem = NewConstraintSystemImpl(
        callComponents.constraintInjector,
        builtIns,
        callComponents.cangjieTypeRefiner,
        topLevelCallContext.languageVersionSettings
    )
    private var hasInapplicableCall = false
    private val commonCalls = arrayListOf<PSICompletedCallInfo>()

    override val parentSession: InferenceSession = topLevelCallContext.inferenceSession
    private fun skipCall(callInfo: SingleCallResolutionResult): Boolean {
        val descriptor = callInfo.resultCallAtom.candidateDescriptor

        // FakeCallableDescriptorForObject can't introduce new information for inference,
        // so it's safe to complete it fully
        return descriptor is FakeCallableDescriptorForObject
    }
    fun hasInapplicableCall(): Boolean = hasInapplicableCall

    private fun arePostponedVariablesInferred() = commonSystem.notFixedTypeVariables.isEmpty()

    override fun writeOnlyStubs(callInfo: SingleCallResolutionResult): Boolean {
        return !skipCall(callInfo) && !arePostponedVariablesInferred()

    }

    /*
  * It's used only for `+=` resolve to clear calls info before the second analysis of right side.
  * TODO: remove it after moving `+=` resolve into OR mechanism
  */
    fun clearCallsInfoByContainingElement(containingElement: CjElement) {
        commonCalls.removeIf remove@{ callInfo ->
            val atom = callInfo.callResolutionResult.resultCallAtom.atom
            if (atom !is PSICangJieCallImpl) return@remove false

            containingElement.anyDescendantOfType<CjElement> { it == atom.psiCall.callElement }
        }
    }
}
