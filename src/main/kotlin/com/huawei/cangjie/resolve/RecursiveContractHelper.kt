package com.huawei.cangjie.resolve

import com.huawei.cangjie.descriptors.CallableDescriptor
import com.huawei.cangjie.descriptors.FunctionDescriptor
import com.huawei.cangjie.psi.Call
import com.huawei.cangjie.psi.CjExpression
import com.huawei.cangjie.psi.psiUtil.isContractDescriptionCallPsiCheck
import com.huawei.cangjie.resolve.scopes.LexicalScope


//fun disableContractsInsideContractsBlock(call: Call, descriptor: CallableDescriptor?, scope: LexicalScope, trace: BindingTrace) {
//    (call.callElement as? CjExpression)?.let { callExpression ->
//        if (callExpression.isFirstStatement() && callExpression.isContractDescriptionCallPsiCheck()) {
//            if (descriptor?.isContractCallDescriptor() != true) {
//                val functionDescriptor = scope.ownerDescriptor as? FunctionDescriptor
//                val contractProvider = functionDescriptor?.getUserData(ContractProviderKey) as? LazyContractProvider
//                contractProvider?.setContractDescription(null)
//            } else {
//                trace.record(BindingContext.IS_CONTRACT_DECLARATION_BLOCK, callExpression, true)
//            }
//        }
//    }
//}
