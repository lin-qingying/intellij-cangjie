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

import org.cangnova.cangjie.builtins.isFunctionType
import org.cangnova.cangjie.descriptors.BindingTrace
import org.cangnova.cangjie.descriptors.CallableDescriptor
import org.cangnova.cangjie.descriptors.DeclarationDescriptor
import org.cangnova.cangjie.descriptors.VariableDescriptor
import org.cangnova.cangjie.diagnostics.Errors
import org.cangnova.cangjie.psi.Call
import org.cangnova.cangjie.psi.CjReferenceExpression
import org.cangnova.cangjie.resolve.calls.model.ResolvedCall
import org.cangnova.cangjie.resolve.calls.model.VariableAsFunctionResolvedCall
import org.cangnova.cangjie.resolve.calls.util.isInvokeCallOnVariable

class TracingStrategyImpl private constructor(override val reference: CjReferenceExpression, call: Call) :
    AbstractTracingStrategy(reference, call) {
    override fun bindCall(trace: BindingTrace, call: Call) {
        trace.record(BindingContext.CALL, call.calleeExpression, call)
    }

    override fun <D : CallableDescriptor> bindReference(trace: BindingTrace, resolvedCall: ResolvedCall<D>) {
        val descriptor: DeclarationDescriptor = resolvedCall.candidateDescriptor
        //        if (resolvedCall instanceof VariableAsFunctionResolvedCall) {
//            descriptor = ((VariableAsFunctionResolvedCall) resolvedCall).getVariableCall().getCandidateDescriptor();
//        }
//        if (descriptor instanceof FakeCallableDescriptorForObject) {
//            FakeCallableDescriptorForObject fakeCallableDescriptorForObject = (FakeCallableDescriptorForObject) descriptor;
//            descriptor = fakeCallableDescriptorForObject.getReferencedDescriptor();
//            if (fakeCallableDescriptorForObject.getClassDescriptor().getCompanionObjectDescriptor() != null) {
//                trace.record(SHORT_REFERENCE_TO_COMPANION_OBJECT, reference, fakeCallableDescriptorForObject.getClassDescriptor());
//            }
//        }
        val storedReference = trace[BindingContext.REFERENCE_TARGET, reference]
        if (storedReference == null || !isError(descriptor)) {
            trace.record(
                BindingContext.REFERENCE_TARGET,
                reference, descriptor
            )
        }
    }

    override fun <D : CallableDescriptor> bindResolvedCall(trace: BindingTrace, resolvedCall: ResolvedCall<D>) {
        trace.record(BindingContext.RESOLVED_CALL, call, resolvedCall)
    }

    override fun unresolvedReference(trace: BindingTrace) {
        trace.report(Errors.UNRESOLVED_REFERENCE.on(reference, reference))
    }

    override fun <D : CallableDescriptor> unresolvedReferenceWrongReceiver(
        trace: BindingTrace,
        candidates: Collection<ResolvedCall<D>>
    ) {
        val variableDescriptor = isFunctionExpectedError(candidates)
        if (variableDescriptor != null) {
            trace.report(
                Errors.FUNCTION_EXPECTED.on(
                    reference,
                    reference, variableDescriptor.type
                )
            )
        } else {
            trace.report(Errors.UNRESOLVED_REFERENCE_WRONG_RECEIVER.on(reference, candidates))
        }
    } //    @Override
    //    public <D extends CallableDescriptor> void bindReference(@NotNull BindingTrace trace, @NotNull ResolvedCall<D> resolvedCall) {
    //        DeclarationDescriptor descriptor = resolvedCall.getCandidateDescriptor();
    //        if (resolvedCall instanceof VariableAsFunctionResolvedCall) {
    //            descriptor = ((VariableAsFunctionResolvedCall) resolvedCall).getVariableCall().getCandidateDescriptor();
    //        }
    //        if (descriptor instanceof FakeCallableDescriptorForObject) {
    //            FakeCallableDescriptorForObject fakeCallableDescriptorForObject = (FakeCallableDescriptorForObject) descriptor;
    //            descriptor = fakeCallableDescriptorForObject.getReferencedDescriptor();
    //            if (fakeCallableDescriptorForObject.getClassDescriptor().getCompanionObjectDescriptor() != null) {
    //                trace.record(SHORT_REFERENCE_TO_COMPANION_OBJECT, reference, fakeCallableDescriptorForObject.getClassDescriptor());
    //            }
    //        }
    //        DeclarationDescriptor storedReference = trace.get(REFERENCE_TARGET, reference);
    //        if (storedReference == null || !ErrorUtils.isError(descriptor)) {
    //            trace.record(REFERENCE_TARGET, reference, descriptor);
    //        }
    //    }
    //
    //    @Override
    //    public <D extends CallableDescriptor> void bindResolvedCall(@NotNull BindingTrace trace, @NotNull ResolvedCall<D> resolvedCall) {
    //        trace.record(RESOLVED_CALL, call, resolvedCall);
    //    }
    //
    //    @Override
    //    public void unresolvedReference(@NotNull BindingTrace trace) {
    //        trace.report(UNRESOLVED_REFERENCE.on(reference, reference));
    //    }
    //
    //    @Override
    //    public <D extends CallableDescriptor> void unresolvedReferenceWrongReceiver(@NotNull BindingTrace trace, @NotNull Collection<? extends ResolvedCall<D>> candidates) {
    //        VariableCallableDescriptor variableDescriptor = isFunctionExpectedError(candidates);
    //        if (variableDescriptor != null) {
    //            trace.report(Errors.FUNCTION_EXPECTED.on(reference, reference, variableDescriptor.getType()));
    //        }
    //        else {
    //            trace.report(UNRESOLVED_REFERENCE_WRONG_RECEIVER.on(reference, candidates));
    //        }
    //    }
    //
    //    @Nullable
    //    private static <D extends CallableDescriptor> VariableCallableDescriptor isFunctionExpectedError(
    //            @NotNull Collection<? extends ResolvedCall<D>> candidates
    //    ) {
    //        List<VariableCallableDescriptor> variables = CollectionsKt.map(candidates, TracingStrategyImpl::variableIfFunctionExpectedError);
    //        List<VariableCallableDescriptor> distinctVariables = CollectionsKt.distinct(variables);
    //        return CollectionsKt.singleOrNull(distinctVariables);
    //    }
    //
    //    @Nullable
    //    private static <D extends CallableDescriptor> VariableCallableDescriptor variableIfFunctionExpectedError(
    //            @NotNull ResolvedCall<D> candidate
    //    ) {
    //        if (!(candidate instanceof VariableAsFunctionResolvedCall)) return null;
    //
    //        ResolvedCall<VariableCallableDescriptor> variableCall = ((VariableAsFunctionResolvedCall) candidate).getVariableCall();
    //        ResolvedCall<FunctionDescriptor> functionCall = ((VariableAsFunctionResolvedCall) candidate).getFunctionCall();
    //
    //        CangJieType type = variableCall.getCandidateDescriptor().getType();
    //
    //        bool nonFunctionalVar = variableCall.getStatus().isSuccess() && !FunctionTypesCj.isFunctionType(type);
    //        Call functionPsiCall = functionCall.getCall();
    //        if (nonFunctionalVar && CallResolverUtilCj.isInvokeCallOnVariable(functionPsiCall) && functionPsiCall.getValueArguments().isEmpty()) {
    //            return variableCall.getCandidateDescriptor();
    //        }
    //
    //        return null;
    //    }


    companion object {
        //
        @JvmStatic
        fun create(reference: CjReferenceExpression, call: Call): TracingStrategy {
            return TracingStrategyImpl(reference, call)
        }


        private fun <D : CallableDescriptor> isFunctionExpectedError(
            candidates: Collection<ResolvedCall<D>>
        ): VariableDescriptor? {
            val variables =
                candidates.map { candidate: ResolvedCall<D> -> variableIfFunctionExpectedError(candidate) }
            val distinctVariables = variables.distinct()
            return distinctVariables.singleOrNull()
        }

        private fun <D : CallableDescriptor> variableIfFunctionExpectedError(
            candidate: ResolvedCall<D>
        ): VariableDescriptor? {
            if (candidate !is VariableAsFunctionResolvedCall) return null

            val variableCall = (candidate as VariableAsFunctionResolvedCall).variableCall
            val functionCall = (candidate as VariableAsFunctionResolvedCall).functionCall

            val type = variableCall.candidateDescriptor.type

            val nonFunctionalVar = variableCall.status.isSuccess && !type.isFunctionType
            val functionPsiCall = functionCall.call
            if (nonFunctionalVar && isInvokeCallOnVariable(functionPsiCall) && functionPsiCall.valueArguments.isEmpty()) {
                return variableCall.candidateDescriptor
            }

            return null
        }
    }
}
