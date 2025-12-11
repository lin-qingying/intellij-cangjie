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

package org.cangnova.cangjie.types.expressions

import org.cangnova.cangjie.descriptors.CallableDescriptor
import org.cangnova.cangjie.descriptors.FunctionDescriptor
import org.cangnova.cangjie.diagnostics.Severity
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.resolve.calls.CallResolver
import org.cangnova.cangjie.resolve.calls.context.ResolutionContext
import org.cangnova.cangjie.resolve.calls.model.ResolvedCall
import org.cangnova.cangjie.resolve.calls.tasks.TracingStrategy
import org.cangnova.cangjie.resolve.calls.tasks.TracingStrategyImpl
import org.cangnova.cangjie.resolve.calls.util.CallMaker
import org.cangnova.cangjie.types.CangJieType
import com.intellij.openapi.project.Project
import org.cangnova.cangjie.diagnostics.infos.errors.COMPONENT_FUNCTION_MISSING
import org.cangnova.cangjie.diagnostics.infos.errors.*
import org.cangnova.cangjie.diagnostics.infos.errors.ITERATOR_MISSING
import org.cangnova.cangjie.resolve.binding.BindingTrace
import org.cangnova.cangjie.resolve.binding.TemporaryBindingTrace
import org.cangnova.cangjie.resolve.binding.TraceEntryFilter
import org.cangnova.cangjie.resolve.binding.slicedMap.WritableSlice
import org.cangnova.cangjie.resolve.calls.results.OverloadResolutionResults
import org.cangnova.cangjie.resolve.scopes.receivers.ReceiverValue

enum class FakeCallKind {
    ITERATOR,
    COMPONENT,
    OTHER
}

class FakeCallResolver(
    private val project: Project,
    private val callResolver: CallResolver
) {
    class RealExpression(val expressionToReportErrorsOn: CjExpression, val callKind: FakeCallKind)

    @JvmOverloads
    fun makeAndResolveFakeCallInContext(
        receiver: ReceiverValue?,
        context: ResolutionContext<*>,
        valueArguments: List<CjExpression>,
        name: Name,
        callElement: CjExpression,
        realExpression: RealExpression? = null,
        onComplete: (CjSimpleNameExpression) -> Unit = { _ -> }
    ): Pair<Call, OverloadResolutionResults<FunctionDescriptor>> {
        val fakeCalleeExpression = CjPsiFactory(project, markGenerated = false).createSimpleName(name.asString())
        val call = CallMaker.makeCallWithExpressions(callElement, receiver, null, fakeCalleeExpression, valueArguments)

        val tracingStrategy = when (realExpression?.callKind) {
            FakeCallKind.ITERATOR -> TracingStrategyForIteratorCall(
                fakeCalleeExpression,
                realExpression.expressionToReportErrorsOn,
                call
            )

            FakeCallKind.COMPONENT -> TracingStrategyForComponentCall(
                fakeCalleeExpression,
                realExpression.expressionToReportErrorsOn,
                name,
                call
            )

            else -> null
        }

        val results = if (tracingStrategy != null)
            callResolver.resolveCallWithGivenName(context, call, name, tracingStrategy)
        else
            callResolver.resolveCallWithGivenName(context, call, fakeCalleeExpression, name)

        onComplete(fakeCalleeExpression)

        return Pair(call, results)
    }

    fun resolveFakeCall(
        context: ResolutionContext<*>,
        receiver: ReceiverValue?,
        name: Name,
        callElement: CjExpression,
        reportErrorsOn: CjExpression,
        callKind: FakeCallKind,
        valueArguments: List<CjExpression>
    ): OverloadResolutionResults<FunctionDescriptor> {
        val fakeTrace = TemporaryBindingTrace.create(context.trace, "trace to resolve fake call for", name)
        val fakeBindingTrace = context.replaceBindingTrace(fakeTrace)

        var reportIsMissingError = false
        val realExpression = RealExpression(reportErrorsOn, callKind)
        val result =
            makeAndResolveFakeCallInContext(
                receiver,
                fakeBindingTrace,
                valueArguments,
                name,
                callElement,
                realExpression
            ) { fake ->
                reportIsMissingError =
                    fakeTrace.bindingContext.diagnostics.noSuppression().forElement(fake)
                        .any { it.severity == Severity.ERROR }
                fakeTrace.commit(
                    object : TraceEntryFilter {
                        override fun accept(slice: WritableSlice<*, *>?, key: Any?): Boolean {
                            return key != fake
                        }

                    }, true
                )
            }

        val resolutionResults = result.second
        if (reportIsMissingError) {
            val diagnostic = when (callKind) {
                FakeCallKind.ITERATOR -> ITERATOR_MISSING.on(reportErrorsOn)
                FakeCallKind.COMPONENT -> if (receiver != null) COMPONENT_FUNCTION_MISSING.on(
                    reportErrorsOn,
                    name,
                    receiver.type
                ) else null

                FakeCallKind.OTHER -> null
            }

            if (diagnostic != null) {
                context.trace.report(diagnostic)
            }
        }

        return resolutionResults
    }

    private class TracingStrategyForComponentCall(
        fakeExpression: CjReferenceExpression,
        val reportErrorsOn: CjExpression,
        val name: Name,
        val call: Call
    ) : TracingStrategy by TracingStrategyImpl.create(fakeExpression, call) {

        override fun <D : CallableDescriptor> ambiguity(
            trace: BindingTrace,
            resolvedCalls: Collection<ResolvedCall<D>>
        ) {
            trace.report(COMPONENT_FUNCTION_AMBIGUITY.on(reportErrorsOn, name, resolvedCalls))
        }

        override fun unsafeCall(trace: BindingTrace, type: CangJieType, isCallForImplicitInvoke: Boolean) {
            trace.report(COMPONENT_FUNCTION_ON_NULLABLE.on(reportErrorsOn, name))
        }

//        override fun typeInferenceFailed(context: ResolutionContext<*>, inferenceErrorData: InferenceErrorData) {
//            val diagnostic = AbstractTracingStrategy.typeInferenceFailedDiagnostic(context, inferenceErrorData, reportErrorsOn, call)
//            if (diagnostic != null) {
//                context.trace.report(diagnostic)
//            }
//        }
    }

    private class TracingStrategyForIteratorCall(
        fakeExpression: CjReferenceExpression,
        val reportErrorsOn: CjExpression,
        val call: Call
    ) : TracingStrategy by TracingStrategyImpl.create(fakeExpression, call) {

        override fun <D : CallableDescriptor> ambiguity(
            trace: BindingTrace,
            resolvedCalls: Collection<ResolvedCall<D>>
        ) {
            trace.report(ITERATOR_AMBIGUITY.on(reportErrorsOn, resolvedCalls))
        }

        override fun unsafeCall(trace: BindingTrace, type: CangJieType, isCallForImplicitInvoke: Boolean) {
            trace.report(ITERATOR_ON_NULLABLE.on(reportErrorsOn))
        }

//        override fun typeInferenceFailed(context: ResolutionContext<*>, inferenceErrorData: InferenceErrorData) {
//            val diagnostic = AbstractTracingStrategy.typeInferenceFailedDiagnostic(context, inferenceErrorData, reportErrorsOn, call)
//            if (diagnostic != null) {
//                context.trace.report(diagnostic)
//            }
//        }
    }

}
