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

package cn.cangnova.cangjie.resolve.calls.tasks

import cn.cangnova.cangjie.builtins.isNonExtensionFunctionType
import cn.cangnova.cangjie.descriptors.BindingTrace
import cn.cangnova.cangjie.descriptors.CallableDescriptor
import cn.cangnova.cangjie.diagnostics.Errors
import cn.cangnova.cangjie.psi.Call
import cn.cangnova.cangjie.psi.CjExpression
import cn.cangnova.cangjie.psi.CjReferenceExpression
import cn.cangnova.cangjie.psi.CjSimpleNameExpression
import cn.cangnova.cangjie.resolve.BindingContext
import cn.cangnova.cangjie.resolve.calls.model.ResolvedCall
import cn.cangnova.cangjie.types.CangJieType
import com.intellij.psi.PsiElement


class TracingStrategyForInvoke(
    reference: CjExpression,
    call: Call,
    private val calleeType: CangJieType
) : AbstractTracingStrategy(reference, call) {
    override fun bindCall(trace: BindingTrace, call: Call) {
        // If reference is a simple name, it's 'variable as function call' case ('foo(a, b)' where 'foo' is a variable).
        // The outer call is bound ('foo(a, b)'), while 'invoke' call for this case is 'foo.invoke(a, b)' and shouldn't be bound.
        if (reference is CjSimpleNameExpression) return
        trace.record(BindingContext.CALL, reference, call)
    }

    override fun <D : CallableDescriptor> bindReference(
        trace: BindingTrace, resolvedCall: ResolvedCall<D>
    ) {
        val callElement: PsiElement = call.callElement
        if (callElement is CjReferenceExpression) {
            trace.record(BindingContext.REFERENCE_TARGET, callElement, resolvedCall.candidateDescriptor)
        }
    }

    override fun <D : CallableDescriptor> bindResolvedCall(
        trace: BindingTrace, resolvedCall: ResolvedCall<D>
    ) {
        if (reference is CjSimpleNameExpression) return
        trace.record(BindingContext.RESOLVED_CALL, call, resolvedCall)
    }

    override fun unresolvedReference(trace: BindingTrace) {
        functionExpectedOrNoReceiverAllowed(trace)
    }

    override fun <D : CallableDescriptor> unresolvedReferenceWrongReceiver(
        trace: BindingTrace, candidates: Collection<ResolvedCall<D>>
    ) {
        functionExpectedOrNoReceiverAllowed(trace)
    }


    private fun functionExpectedOrNoReceiverAllowed(trace: BindingTrace) {
        if (calleeType.isNonExtensionFunctionType) {
            trace.report(Errors.NO_RECEIVER_ALLOWED.on(reference))
        } else {
            trace.report(Errors.FUNCTION_EXPECTED.on(reference, reference, calleeType))
        }
    }
}
