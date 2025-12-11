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

package org.cangnova.cangjie.resolve.calls.tasks

import org.cangnova.cangjie.descriptors.CallableDescriptor
import org.cangnova.cangjie.psi.Call
import org.cangnova.cangjie.psi.CjBlockExpression
import org.cangnova.cangjie.resolve.binding.BindingTrace
import org.cangnova.cangjie.resolve.calls.model.ResolvedCall

class TracingStrategyBlockExpression(
    private val blockExpression: CjBlockExpression,
    call: Call
) : AbstractTracingStrategy(blockExpression, call) {
    override fun bindCall(trace: BindingTrace, call: Call) {

    }

    override fun <D : CallableDescriptor> bindReference(trace: BindingTrace, resolvedCall: ResolvedCall<D>) {

    }

    override fun <D : CallableDescriptor> bindResolvedCall(trace: BindingTrace, resolvedCall: ResolvedCall<D>) {

    }

    override fun unresolvedReference(trace: BindingTrace) {

    }

    override fun <D : CallableDescriptor> unresolvedReferenceWrongReceiver(
        trace: BindingTrace,
        candidates: Collection<ResolvedCall<D>>
    ) {

    }
}
