/*
 * Copyright 2025 LinQingYing. and contributors.
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

package org.cangnova.cangjie.resolve

import org.cangnova.cangjie.descriptors.FunctionDescriptor
import org.cangnova.cangjie.descriptors.ValueParameterDescriptor
import org.cangnova.cangjie.descriptors.VariableDescriptor
import org.cangnova.cangjie.descriptors.impl.ValueParameterDescriptorImpl
import org.cangnova.cangjie.resolve.binding.BindingTrace
import org.cangnova.cangjie.resolve.scopes.*

object FunctionDescriptorUtil {
    fun getFunctionInnerScope(
        outerScope: LexicalScope,
        descriptor: FunctionDescriptor,
        redeclarationChecker: LocalRedeclarationChecker
    ): LexicalScope = LexicalScopeImpl(
        outerScope, descriptor, true, descriptor.extensionReceiverParameter,
        descriptor.contextReceiverParameters, LexicalScopeKind.FUNCTION_INNER_SCOPE, redeclarationChecker, { handler ->
            for (valueParameterDescriptor in descriptor.valueParameters) {
                when (valueParameterDescriptor) {
                    is ValueParameterDescriptorImpl.WithDestructuringDeclaration -> {
                        val entries: List<VariableDescriptor> = valueParameterDescriptor.destructuringVariables
                        for (entry in entries) {
                            handler.addVariableDescriptor(entry)
                        }
                    }

                    else -> handler.addVariableDescriptor(valueParameterDescriptor)
                }
            }
        }
    )

    fun getFunctionInnerScope(
        outerScope: LexicalScope,
        descriptor: FunctionDescriptor,
        trace: BindingTrace,
        overloadChecker: OverloadChecker
    ): LexicalScope = getFunctionInnerScope(
        outerScope,
        descriptor,
        TraceBasedLocalRedeclarationChecker(trace, overloadChecker)
    )
}
