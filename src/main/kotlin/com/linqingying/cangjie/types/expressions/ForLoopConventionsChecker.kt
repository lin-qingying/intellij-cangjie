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

package com.linqingying.cangjie.types.expressions

import com.linqingying.cangjie.builtins.CangJieBuiltIns
import com.linqingying.cangjie.diagnostics.DiagnosticSink
import com.linqingying.cangjie.descriptors.FunctionDescriptor
import com.linqingying.cangjie.diagnostics.DiagnosticFactory1
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.psi.CjExpression
import com.linqingying.cangjie.resolve.BindingContext.LOOP_RANGE_ITERATOR_RESOLVED_CALL
import com.linqingying.cangjie.resolve.calls.model.ResolvedCall
import com.linqingying.cangjie.resolve.scopes.receivers.ExpressionReceiver
import com.linqingying.cangjie.resolve.scopes.receivers.TransientReceiver
import com.linqingying.cangjie.types.CangJieType
import com.linqingying.cangjie.types.ErrorUtils.isError
import com.linqingying.cangjie.types.isDynamic
import com.linqingying.cangjie.utils.OperatorNameConventions
import com.linqingying.cangjie.utils.slicedMap.WritableSlice

class ForLoopConventionsChecker(
    val builtIns: CangJieBuiltIns,
    val fakeCallResolver: FakeCallResolver
) {
    //检查迭代器对象
    fun checkIterableConvention(loopRange: ExpressionReceiver, context: ExpressionTypingContext): CangJieType? {
        val loopRangeExpression = loopRange.expression

        // Make a fake call loopRange.iterator(), and try to resolve it
        val iteratorResolutionResults = fakeCallResolver.resolveFakeCall(
            context, loopRange, OperatorNameConventions.ITERATOR, loopRangeExpression,
            loopRangeExpression, FakeCallKind.ITERATOR, emptyList()
        )
        if (!iteratorResolutionResults.isSuccess) return null

        val iteratorResolvedCall = iteratorResolutionResults.resultingCall
        context.trace.record(LOOP_RANGE_ITERATOR_RESOLVED_CALL, loopRangeExpression, iteratorResolvedCall)
        val iteratorFunction = iteratorResolvedCall.resultingDescriptor

        checkIfOperatorModifierPresent(loopRangeExpression, iteratorFunction, context.trace)

        val iteratorType = iteratorFunction.returnType


        return iteratorType
//        val hasNextType = checkConventionForIterator(
//            context, loopRangeExpression, iteratorType, OperatorNameConventions.HAS_NEXT,
//            HAS_NEXT_FUNCTION_AMBIGUITY, HAS_NEXT_MISSING, HAS_NEXT_FUNCTION_NONE_APPLICABLE, LOOP_RANGE_HAS_NEXT_RESOLVED_CALL
//        )
//        if (hasNextType != null && !builtIns.isBooleanOrSubtype(hasNextType)) {
//            context.trace.report(HAS_NEXT_FUNCTION_TYPE_MISMATCH.on(loopRangeExpression, hasNextType))
//        }
//        return checkConventionForIterator(
//            context, loopRangeExpression, iteratorType, OperatorNameConventions.NEXT,
//            NEXT_AMBIGUITY, NEXT_MISSING, NEXT_NONE_APPLICABLE, LOOP_RANGE_NEXT_RESOLVED_CALL
//        )
    }

    private fun checkConventionForIterator(
        context: ExpressionTypingContext,
        loopRangeExpression: CjExpression,
        iteratorType: CangJieType,
        name: Name,
        ambiguity: DiagnosticFactory1<CjExpression, CangJieType>,
        missing: DiagnosticFactory1<CjExpression, CangJieType>,
        noneApplicable: DiagnosticFactory1<CjExpression, CangJieType>,
        resolvedCallKey: WritableSlice<CjExpression, ResolvedCall<FunctionDescriptor>>
    ): CangJieType? {
        val nextResolutionResults = fakeCallResolver.resolveFakeCall(
            context,
            TransientReceiver(iteratorType),
            name,
            loopRangeExpression,
            loopRangeExpression,
            FakeCallKind.OTHER,
            emptyList()
        )
        if (nextResolutionResults.isAmbiguity()) {
            context.trace.report(ambiguity.on(loopRangeExpression, iteratorType))
        } else if (nextResolutionResults.isNothing()) {
            context.trace.report(missing.on(loopRangeExpression, iteratorType))
        } else if (!nextResolutionResults.isSuccess()) {
            context.trace.report(noneApplicable.on(loopRangeExpression, iteratorType))
        } else {
            assert(nextResolutionResults.isSuccess())
            val resolvedCall = nextResolutionResults.resultingCall
            context.trace.record(resolvedCallKey, loopRangeExpression, resolvedCall)

            val functionDescriptor = resolvedCall.resultingDescriptor
            checkIfOperatorModifierPresent(loopRangeExpression, functionDescriptor, context.trace)

            return functionDescriptor.returnType
        }
        return null
    }

    private fun checkIfOperatorModifierPresent(
        expression: CjExpression,
        descriptor: FunctionDescriptor,
        sink: DiagnosticSink
    ) {
        if (isError(descriptor)) return
        val extensionReceiverParameter =
            descriptor.getExtensionReceiverParameter()
        if ((extensionReceiverParameter != null) && (extensionReceiverParameter.getType().isDynamic())) return

//        if (!descriptor.isOperator()) {
//            OperatorCallChecker.report(
//                expression,
//                descriptor,
//                sink
//            )
//        }
    }
}
