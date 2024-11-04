package com.linqingying.cangjie.resolve

import com.linqingying.cangjie.descriptors.CallableDescriptor
import com.linqingying.cangjie.descriptors.FunctionDescriptor
import com.linqingying.cangjie.psi.Call
import com.linqingying.cangjie.psi.CjExpression
import com.linqingying.cangjie.psi.psiUtil.isContractDescriptionCallPsiCheck
import com.linqingying.cangjie.resolve.scopes.LexicalScope


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
