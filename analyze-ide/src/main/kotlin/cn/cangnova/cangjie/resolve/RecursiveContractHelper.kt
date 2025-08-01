/*
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

package cn.cangnova.cangjie.resolve


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
