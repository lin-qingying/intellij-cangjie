package com.huawei.cangjie.resolve.calls.components.candidate


import com.huawei.cangjie.descriptors.CallableDescriptor
import com.huawei.cangjie.resolve.calls.components.CallableReceiver
import com.huawei.cangjie.resolve.calls.components.CallableReferenceAdaptation
import com.huawei.cangjie.resolve.calls.components.CangJieResolutionCallbacks
import com.huawei.cangjie.resolve.calls.inference.components.FreshVariableNewTypeSubstitutor
import com.huawei.cangjie.resolve.calls.inference.model.ConstraintStorage
import com.huawei.cangjie.resolve.calls.model.*
import com.huawei.cangjie.resolve.calls.tasks.ExplicitReceiverKind
import com.huawei.cangjie.resolve.calls.tower.CandidateApplicability
import com.huawei.cangjie.resolve.calls.tower.ImplicitScopeTower
import com.huawei.cangjie.types.TypeSubstitutor
import com.huawei.cangjie.types.UnwrappedType

/**
 * Suppose we have class A with staticM, memberM, memberExtM.
 * For A::staticM both receivers will be null
 * For A::memberM dispatchReceiver = UnboundReceiver, extensionReceiver = null
 * For a::memberExtM dispatchReceiver = ExplicitValueReceiver, extensionReceiver = ExplicitValueReceiver
 *
 */
class CallableReferenceResolutionCandidate(
    val candidate: CallableDescriptor,
    val dispatchReceiver: CallableReceiver?,
    val extensionReceiver: CallableReceiver?,
    val explicitReceiverKind: ExplicitReceiverKind,
    val reflectionCandidateType: UnwrappedType,
    val callableReferenceAdaptation: CallableReferenceAdaptation?,
    val cangjieCall: CallableReferenceResolutionAtom,
    val expectedType: UnwrappedType?,
    override val callComponents: CangJieCallComponents,
    override val scopeTower: ImplicitScopeTower,
    override val resolutionCallbacks: CangJieResolutionCallbacks,
    override val baseSystem: ConstraintStorage?
) : ResolutionCandidate() {
    override val variableCandidateIfInvoke: ResolutionCandidate? = null

    override val knownTypeParametersResultingSubstitutor: TypeSubstitutor? = null // callable reference's rhs doesn't have type parameters

    override val resolvedCall = ResolvedCallableReferenceCallAtom(
        cangjieCall.call, candidate, explicitReceiverKind,
        if (dispatchReceiver != null) ReceiverExpressionCangJieCallArgument(dispatchReceiver.receiver) else null,
        if (extensionReceiver != null) ReceiverExpressionCangJieCallArgument(extensionReceiver.receiver) else null,
        reflectionCandidateType,
        candidate = this
    )

    override fun addResolvedCjPrimitive(resolvedAtom: ResolvedAtom) {} // there aren't nested resolved primitives for callable references


    override fun getSubResolvedAtoms(): List<ResolvedAtom> = emptyList()

    var freshVariablesSubstitutor: FreshVariableNewTypeSubstitutor? = null
        internal set

    val numDefaults get() = callableReferenceAdaptation?.defaults ?: 0
}
