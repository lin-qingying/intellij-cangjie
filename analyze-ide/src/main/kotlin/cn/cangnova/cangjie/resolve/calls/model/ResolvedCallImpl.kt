package cn.cangnova.cangjie.resolve.calls.model/*
 * Copyright 2024 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

//package cn.cangnova.cangjie.resolve.calls.model
//
//import cn.cangnova.cangjie.descriptors.CallableDescriptor
//import cn.cangnova.cangjie.psi.Call
//import cn.cangnova.cangjie.resolve.calls.results.ResolutionStatus
//import cn.cangnova.cangjie.resolve.scopes.receivers.ReceiverValue
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
