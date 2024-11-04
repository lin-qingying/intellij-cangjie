package com.linqingying.cangjie.resolve.calls.components.candidate

import com.linqingying.cangjie.resolve.calls.components.CangJieResolutionCallbacks
import com.linqingying.cangjie.resolve.calls.components.ErrorDescriptorResolutionPart
import com.linqingying.cangjie.resolve.calls.inference.model.ConstraintStorage
import com.linqingying.cangjie.resolve.calls.model.CangJieCallComponents
import com.linqingying.cangjie.resolve.calls.model.MutableResolvedCallAtom
import com.linqingying.cangjie.resolve.calls.model.ResolutionPart
import com.linqingying.cangjie.resolve.calls.tower.ImplicitScopeTower

class SimpleErrorResolutionCandidate(
    callComponents: CangJieCallComponents,
    resolutionCallbacks: CangJieResolutionCallbacks,
    scopeTower: ImplicitScopeTower,
    baseSystem: ConstraintStorage,
    resolvedCall: MutableResolvedCallAtom
) : SimpleResolutionCandidate(callComponents, resolutionCallbacks, scopeTower, baseSystem, resolvedCall) {
    override val resolutionSequence: List<ResolutionPart> = listOf(ErrorDescriptorResolutionPart)
}
