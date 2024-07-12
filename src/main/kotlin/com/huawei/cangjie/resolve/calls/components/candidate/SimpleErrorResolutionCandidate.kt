package com.huawei.cangjie.resolve.calls.components.candidate

import com.huawei.cangjie.resolve.calls.components.CangJieResolutionCallbacks
import com.huawei.cangjie.resolve.calls.components.ErrorDescriptorResolutionPart
import com.huawei.cangjie.resolve.calls.inference.model.ConstraintStorage
import com.huawei.cangjie.resolve.calls.model.CangJieCallComponents
import com.huawei.cangjie.resolve.calls.model.MutableResolvedCallAtom
import com.huawei.cangjie.resolve.calls.model.ResolutionPart
import com.huawei.cangjie.resolve.calls.tower.ImplicitScopeTower

class SimpleErrorResolutionCandidate(
    callComponents: CangJieCallComponents,
    resolutionCallbacks: CangJieResolutionCallbacks,
    scopeTower: ImplicitScopeTower,
    baseSystem: ConstraintStorage,
    resolvedCall: MutableResolvedCallAtom
) : SimpleResolutionCandidate(callComponents, resolutionCallbacks, scopeTower, baseSystem, resolvedCall) {
    override val resolutionSequence: List<ResolutionPart> = listOf(ErrorDescriptorResolutionPart)
}
