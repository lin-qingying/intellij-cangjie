package com.huawei.cangjie.resolve.calls.tower

import com.huawei.cangjie.builtins.isBuiltinExtensionFunctionalType
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.resolve.calls.tasks.ExplicitReceiverKind
import com.huawei.cangjie.resolve.calls.tasks.createSynthesizedInvokes
import com.huawei.cangjie.resolve.scopes.receivers.ReceiverValueWithSmartCastInfo
import com.huawei.cangjie.utils.OperatorNameConventions

private class InvokeExtensionScopeTowerProcessor<C : Candidate>(
    context: CandidateFactory<C>,
    private val invokeCandidateDescriptor: CandidateWithBoundDispatchReceiver,
    private val explicitReceiver: ReceiverValueWithSmartCastInfo?
) : AbstractSimpleScopeTowerProcessor<C>(context) {

    override fun simpleProcess(data: TowerData): Collection<C> {
        if (explicitReceiver != null && data == TowerData.Empty) {
            return listOf(
                candidateFactory.createCandidate(
                    invokeCandidateDescriptor,
                    ExplicitReceiverKind.BOTH_RECEIVERS,
                    explicitReceiver
                )
            )
        }

        if (explicitReceiver == null && data is TowerData.OnlyImplicitReceiver) {
            return listOf(
                candidateFactory.createCandidate(
                    invokeCandidateDescriptor,
                    ExplicitReceiverKind.DISPATCH_RECEIVER,
                    data.implicitReceiver
                )
            )
        }

        return emptyList()
    }

    // No lookups happen in `simpleProcess`
    override fun recordLookups(skippedData: Collection<TowerData>, name: Name) {}
}
// todo debug info
private fun ImplicitScopeTower.getExtensionInvokeCandidateDescriptor(
    extensionFunctionReceiver: ReceiverValueWithSmartCastInfo
): CandidateWithBoundDispatchReceiver? {
    val type = extensionFunctionReceiver.receiverValue.type
    if (!type.isBuiltinExtensionFunctionalType) return null // todo: missing smart cast?

    val invokeDescriptor = type.memberScope.getContributedFunctions(OperatorNameConventions.INVOKE, location).single()
    val synthesizedInvokes = createSynthesizedInvokes(listOf(invokeDescriptor))
    val synthesizedInvoke = synthesizedInvokes.singleOrNull()
        ?: error("No single synthesized invoke for $invokeDescriptor: $synthesizedInvokes")

    // here we don't add SynthesizedDescriptor diagnostic because it should has priority as member
    return CandidateWithBoundDispatchReceiver(extensionFunctionReceiver, synthesizedInvoke, listOf())
}
// case 1.(foo())() or (foo())()
fun <C : Candidate> createCallTowerProcessorForExplicitInvoke(
    scopeTower: ImplicitScopeTower,
    functionContext: CandidateFactory<C>,
    expressionForInvoke: ReceiverValueWithSmartCastInfo,
    explicitReceiver: ReceiverValueWithSmartCastInfo?
): ScopeTowerProcessor<C> {
    val invokeExtensionDescriptor = scopeTower.getExtensionInvokeCandidateDescriptor(expressionForInvoke)
    if (explicitReceiver != null) {
        return if (invokeExtensionDescriptor == null) {
            // case 1.(foo())(), where foo() isn't extension function
            KnownResultProcessor(emptyList())
        } else {
            InvokeExtensionScopeTowerProcessor(
                functionContext,
                invokeExtensionDescriptor,
                explicitReceiver = explicitReceiver
            )
        }
    } else {
        val usualInvoke = ExplicitReceiverScopeTowerProcessor(
            scopeTower,
            functionContext,
            expressionForInvoke
        ) { getFunctions(OperatorNameConventions.INVOKE, it) } // todo operator

        return if (invokeExtensionDescriptor == null) {
            usualInvoke
        } else {
            PrioritizedCompositeScopeTowerProcessor(
                usualInvoke,
                InvokeExtensionScopeTowerProcessor(functionContext, invokeExtensionDescriptor, explicitReceiver = null)
            )
        }
    }

}
