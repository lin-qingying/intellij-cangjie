package com.linqingying.cangjie.resolve.calls.components.candidate

import com.linqingying.cangjie.resolve.calls.components.CangJieResolutionCallbacks
import com.linqingying.cangjie.resolve.calls.inference.model.ConstraintStorage
import com.linqingying.cangjie.resolve.calls.model.CangJieCallComponents
import com.linqingying.cangjie.resolve.calls.model.MutableResolvedCallAtom
import com.linqingying.cangjie.resolve.calls.model.ResolvedAtom
import com.linqingying.cangjie.resolve.calls.tower.ImplicitScopeTower
import com.linqingying.cangjie.types.TypeSubstitutor

/**
 * baseSystem contains all information from arguments, i.e. it is union of all system of arguments
 * Also by convention we suppose that baseSystem has no contradiction
 */
open class SimpleResolutionCandidate(
    override val callComponents: CangJieCallComponents,
    override val resolutionCallbacks: CangJieResolutionCallbacks,
    override val scopeTower: ImplicitScopeTower,
    override val baseSystem: ConstraintStorage,
    override val resolvedCall: MutableResolvedCallAtom,
    override val knownTypeParametersResultingSubstitutor: TypeSubstitutor? = null,
) : ResolutionCandidate() {
    override val variableCandidateIfInvoke: ResolutionCandidate?
        get() = callComponents.statelessCallbacks.getVariableCandidateIfInvoke(resolvedCall.atom)

    override fun getSubResolvedAtoms(): List<ResolvedAtom> = subResolvedAtoms

    override fun addResolvedCjPrimitive(resolvedAtom: ResolvedAtom) {
        subResolvedAtoms.add(resolvedAtom)
    }

    private var subResolvedAtoms: MutableList<ResolvedAtom> = arrayListOf()
}
