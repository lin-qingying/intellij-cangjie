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

package com.linqingying.cangjie.resolve.calls.tasks

import com.linqingying.cangjie.descriptors.FunctionDescriptor

// Creates a descriptor denoting an extension function for a collection of non-extension "invoke"s from function types.
// For example, `fun invoke(param: P): R` becomes `fun P.invoke(): R`
fun createSynthesizedInvokes(functions: Collection<FunctionDescriptor>): Collection<FunctionDescriptor> {
    val result = ArrayList<FunctionDescriptor>(1)

//    for (invoke in functions) {
//        if (invoke.name != OperatorNameConventions.INVOKE) continue
//
//        // "invoke" must have at least one parameter, which will become the receiver parameter of the synthesized "invoke"
//        if (invoke.valueParameters.isEmpty()) continue
//
//        val containerClassId = (invoke.containingDeclaration as ClassDescriptor).classId
//        val synthesized = if (containerClassId != null && isBuiltinFunctionClass(containerClassId)) {
//            createSynthesizedFunctionWithFirstParameterAsReceiver(invoke)
//        } else {
//            val invokeDeclaration = invoke.overriddenDescriptors.singleOrNull()
//                ?: error("No single overridden invoke for $invoke: ${invoke.overriddenDescriptors}")
//            val synthesizedSuperFun = createSynthesizedFunctionWithFirstParameterAsReceiver(invokeDeclaration)
//            val fakeOverride = synthesizedSuperFun.copy(
//                invoke.containingDeclaration,
//                synthesizedSuperFun.modality,
//                synthesizedSuperFun.visibility,
//                CallableMemberDescriptor.Kind.FAKE_OVERRIDE,
//                /* copyOverrides = */ false
//            )
//            fakeOverride.setSingleOverridden(synthesizedSuperFun)
//            fakeOverride
//        }
//
//        result.add(synthesized.substitute(TypeSubstitutor.create(invoke.dispatchReceiverParameter!!.type)) ?: continue)
//    }

    return result
}
