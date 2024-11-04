//package com.linqingying.cangjie.resolve.calls.model
//
//import com.linqingying.cangjie.descriptors.CallableDescriptor
//import com.linqingying.cangjie.psi.Call
//import com.linqingying.cangjie.resolve.calls.results.ResolutionStatus
//import com.linqingying.cangjie.resolve.scopes.receivers.ReceiverValue
//
//class ResolvedCallImpl<D: CallableDescriptor>: MutableResolvedCall<D> {
//
//    private var status: ResolutionStatus = ResolutionStatus.UNKNOWN_STATUS
//
//    override fun getCandidateDescriptor(): D {
//        TODO("Not yet implemented")
//    }
//    override fun addStatus(status: ResolutionStatus) {
//        this.status = status.combine(status)
//    }
//    override fun getCall(): Call {
//        TODO("Not yet implemented")
//    }
//
//    override fun getStatus(): ResolutionStatus {
//        TODO("Not yet implemented")
//    }
//
//    override fun getDataFlowInfoForArguments(): DataFlowInfoForArguments {
//        TODO("Not yet implemented")
//    }
//
//    override fun getDispatchReceiver(): ReceiverValue? {
//        TODO("Not yet implemented")
//    }
//
//    override fun getResultingDescriptor(): D {
//        TODO("Not yet implemented")
//    }
//
//    override fun hasInferredReturnType(): Boolean {
//        TODO("Not yet implemented")
//    }
//}
