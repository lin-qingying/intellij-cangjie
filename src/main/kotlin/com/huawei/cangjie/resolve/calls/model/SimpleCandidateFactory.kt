package com.huawei.cangjie.resolve.calls.model

import com.huawei.cangjie.resolve.calls.components.CangJieResolutionCallbacks
import com.huawei.cangjie.resolve.calls.components.candidate.SimpleResolutionCandidate
import com.huawei.cangjie.resolve.calls.tasks.ExplicitReceiverKind
import com.huawei.cangjie.resolve.calls.tower.CandidateFactory
import com.huawei.cangjie.resolve.calls.tower.CandidateWithBoundDispatchReceiver
import com.huawei.cangjie.resolve.calls.tower.ImplicitScopeTower
import com.huawei.cangjie.resolve.scopes.receivers.ReceiverValueWithSmartCastInfo

class SimpleCandidateFactory(
    val callComponents: CangJieCallComponents,
    val scopeTower: ImplicitScopeTower,
    val kotlinCall: CangJieCall,
    val resolutionCallbacks: CangJieResolutionCallbacks,
) : CandidateFactory<SimpleResolutionCandidate> {
    override fun createCandidate(
        towerCandidate: CandidateWithBoundDispatchReceiver,
        explicitReceiverKind: ExplicitReceiverKind,
        extensionReceiver: ReceiverValueWithSmartCastInfo?
    ): SimpleResolutionCandidate {
        TODO("Not yet implemented")
    }

    override fun createErrorCandidate(): SimpleResolutionCandidate {
        TODO("Not yet implemented")
    }

    override fun createCandidate(
        towerCandidate: CandidateWithBoundDispatchReceiver,
        explicitReceiverKind: ExplicitReceiverKind,
        extensionReceiverCandidates: List<ReceiverValueWithSmartCastInfo>
    ): SimpleResolutionCandidate {
        TODO("Not yet implemented")
    }
}
