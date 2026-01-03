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

import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.diagnostics.reportUnresolvedReference
import org.cangnova.cangjie.diagnostics.infos.errors.*
import org.cangnova.cangjie.psi.Call
import org.cangnova.cangjie.psi.CjConstructorDelegationCall
import org.cangnova.cangjie.resolve.binding.BindingContext.Companion.CALL
import org.cangnova.cangjie.resolve.binding.BindingContext.Companion.REFERENCE_TARGET
import org.cangnova.cangjie.resolve.binding.BindingContext.Companion.RESOLVED_CALL
import org.cangnova.cangjie.resolve.binding.BindingTrace
import org.cangnova.cangjie.resolve.calls.model.ResolvedCall
import org.cangnova.cangjie.resolve.calls.util.reportOnElement
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.ErrorUtils


class TracingStrategyForImplicitConstructorDelegationCall(
    val delegationCall: CjConstructorDelegationCall, call: Call
) : AbstractTracingStrategy(delegationCall.calleeExpression!!, call) {

    val calleeExpression = delegationCall.calleeExpression

    override fun bindCall(trace: BindingTrace, call: Call) {
        call.calleeExpression?.let {
            trace.record(CALL, it, call)

        }
    }

    override fun <D : CallableDescriptor> bindReference(trace: BindingTrace, resolvedCall: ResolvedCall<D>) {
        val descriptor = resolvedCall.candidateDescriptor
        val storedReference = trace[REFERENCE_TARGET, calleeExpression ?: return]
        if (storedReference == null || !ErrorUtils.isError(descriptor)) {
            trace.record(REFERENCE_TARGET, calleeExpression, descriptor)
        }
    }

    override fun <D : CallableDescriptor> bindResolvedCall(trace: BindingTrace, resolvedCall: ResolvedCall<D>) {
        trace.record(RESOLVED_CALL, call, resolvedCall)
    }

    override fun unresolvedReference(trace: BindingTrace) {
        // 使用统一的错误报告方法
        trace.reportUnresolvedReference(calleeExpression ?: return)
    }

    override fun <D : CallableDescriptor> unresolvedReferenceWrongReceiver(
        trace: BindingTrace,
        candidates: Collection<ResolvedCall<D>>
    ) {
        trace.report(UNRESOLVED_REFERENCE_WRONG_RECEIVER.on(reference, candidates))
    }


    override fun <D : CallableDescriptor> ambiguity(trace: BindingTrace, resolvedCalls: Collection<ResolvedCall<D>>) {
        reportError(trace)
    }

    override fun <D : CallableDescriptor> noneApplicable(
        trace: BindingTrace,
        descriptors: Collection<ResolvedCall<D>>
    ) {
        reportError(trace)
    }


    override fun noValueForParameter(trace: BindingTrace, valueParameter: ValueParameterDescriptor) {
        reportError(trace)
    }

    override fun unsafeCall(trace: BindingTrace, type: CangJieType, isCallForImplicitInvoke: Boolean) {
        unexpectedError("unsafeCall")

    }

    private fun reportError(trace: BindingTrace) {
        val reportOn = delegationCall.reportOnElement()
        if (!trace.bindingContext.diagnostics.forElement(reportOn)
                .any { it.factory == EXPLICIT_DELEGATION_CALL_REQUIRED }
        ) {
            trace.report(EXPLICIT_DELEGATION_CALL_REQUIRED.on(reportOn))
        }
    }

    // Underlying methods should not be called because such errors are impossible
    // when resolving delegation call


    override fun <D : CallableDescriptor> cannotCompleteResolve(
        trace: BindingTrace,
        descriptors: Collection<ResolvedCall<D>>
    ) {
        unexpectedError("cannotCompleteResolve")
    }

    override fun invisibleMember(trace: BindingTrace, descriptor: DeclarationDescriptorWithVisibility) {
        reportError(trace)

    }

    private fun unexpectedError(type: String) {
        throw AssertionError("Unexpected error type: $type")
    }
}
