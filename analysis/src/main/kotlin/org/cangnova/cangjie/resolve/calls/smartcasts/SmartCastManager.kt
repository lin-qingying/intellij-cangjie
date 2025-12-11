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

package org.cangnova.cangjie.resolve.calls.smartcasts

import org.cangnova.cangjie.config.LanguageVersionSettings
import org.cangnova.cangjie.descriptors.DeclarationDescriptor
import org.cangnova.cangjie.psi.Call
import org.cangnova.cangjie.psi.CjExpression
import org.cangnova.cangjie.resolve.binding.BindingContext
import org.cangnova.cangjie.resolve.calls.ArgumentTypeResolver
import org.cangnova.cangjie.resolve.calls.context.ResolutionContext
import org.cangnova.cangjie.types.CangJieType


class SmartCastManager(private val argumentTypeResolver: ArgumentTypeResolver) {
    fun getSmartCastVariants(
        receiverToCast: ReceiverValue,
        bindingContext: BindingContext,
        containingDeclarationOrModule: DeclarationDescriptor,
        dataFlowInfo: DataFlowInfo,
        languageVersionSettings: LanguageVersionSettings,
        dataFlowValueFactory: DataFlowValueFactory
    ): List<CangJieType> {
        val variants = getSmartCastVariantsExcludingReceiver(
            bindingContext,
            containingDeclarationOrModule,
            dataFlowInfo,
            receiverToCast,
            languageVersionSettings,
            dataFlowValueFactory
        )
        val result = ArrayList<CangJieType>(variants.size + 1)
        result.add(receiverToCast.type)
        result.addAll(variants)
        return result
    }

    private fun getSmartCastVariantsExcludingReceiver(
        bindingContext: BindingContext,
        containingDeclarationOrModule: DeclarationDescriptor,
        dataFlowInfo: DataFlowInfo,
        receiverToCast: ReceiverValue,
        languageVersionSettings: LanguageVersionSettings,
        dataFlowValueFactory: DataFlowValueFactory
    ): Collection<CangJieType> {
        val dataFlowValue =
            dataFlowValueFactory.createDataFlowValue(receiverToCast, bindingContext, containingDeclarationOrModule)
        return dataFlowInfo.getCollectedTypes(dataFlowValue, languageVersionSettings)
    }

    fun checkAndRecordPossibleCast(
        dataFlowValue: DataFlowValue,
        expectedType: CangJieType,
        expression: CjExpression?,
        c: ResolutionContext<*>,
        call: Call?,
        recordExpressionType: Boolean,
        additionalPredicate: ((CangJieType) -> Boolean)? = null
    ): SmartCastResult? {
//        val calleeExpression = call?.calleeExpression
//        val expectedTypes = if (c.languageVersionSettings.supportsFeature(LanguageFeature.NewInference))
//            expectedType.expandIntersectionTypeIfNecessary()
//        else
//            listOf(expectedType)
//
//        val builderInferenceSubstitutor =
//            (c.inferenceSession as? BuilderInferenceSession)?.getNotFixedToInferredTypesSubstitutor()
//        val collectedTypes = c.dataFlowInfo.getCollectedTypes(dataFlowValue, c.languageVersionSettings).let { types ->
//            if (builderInferenceSubstitutor != null) types.map { builderInferenceSubstitutor.safeSubstitute(it.unwrap()) } else types
//        }.toMutableList()
//
//        if (collectedTypes.isNotEmpty() && c.languageVersionSettings.supportsFeature(LanguageFeature.NewInference)) {
//            // Sometime expected type may be inferred to be an intersection of all of the smart-cast types
//            val typeToIntersect = collectedTypes + dataFlowValue.type
//            collectedTypes.addIfNotNull(intersectWrappedTypes(typeToIntersect))
//        }
//
//        for (possibleType in collectedTypes) {
//            if (expectedTypes.any { argumentTypeResolver.isSubtypeOfForArgumentType(possibleType, it) } &&
//                (additionalPredicate == null || additionalPredicate(possibleType))
//            ) {
//                if (expression != null) {
//                    recordCastOrError(expression, possibleType, c.trace, dataFlowValue, call, recordExpressionType)
//                } else if (calleeExpression != null && dataFlowValue.isStable) {
//                    val receiver = (dataFlowValue.identifierInfo as? IdentifierInfo.Receiver)?.value
//                    if (receiver is ImplicitReceiver) {
//                        val oldSmartCasts = c.trace[IMPLICIT_RECEIVER_SMARTCAST, calleeExpression]
//                        val newSmartCasts = ImplicitSmartCasts(receiver, possibleType)
//                        if (oldSmartCasts != null) {
//                            val oldType = oldSmartCasts.receiverTypes[receiver]
//                            if (oldType != null && oldType != possibleType) {
//                                throw AssertionError(
//                                    "Rewriting key $receiver for implicit smart cast on ${calleeExpression.text}: " +
//                                            "was $oldType, now $possibleType"
//                                )
//                            }
//                        }
//                        c.trace.record(IMPLICIT_RECEIVER_SMARTCAST, calleeExpression,
//                            oldSmartCasts?.let { it + newSmartCasts } ?: newSmartCasts)
//
//                    }
//                }
//                return SmartCastResult(possibleType, dataFlowValue.isStable)
//            }
//        }
//
//        if (!c.dataFlowInfo.getCollectedNullability(dataFlowValue).canBeNull() && !expectedType.isMarkedNullable) {
//            // Handling cases like:
//            // fun bar(x: Any) {}
//            // fun <T : Any?> foo(x: T) {
//            //      if (x != null) {
//            //          bar(x) // Should be allowed with smart cast
//            //      }
//            // }
//            //
//            // It doesn't handled by lower code with getPossibleTypes because smart cast of T after `x != null` is still has same type T.
//            // But at the same time we're sure that `x` can't be null and just check for such cases manually
//
//            // E.g. in case x!! when x has type of T where T is type parameter with nullable upper bounds
//            // x!! is immanently not null (see DataFlowValueFactory.createDataFlowValue for expression)
//            val immanentlyNotNull = !dataFlowValue.immanentNullability.canBeNull()
//            val nullableExpectedType = TypeUtils.makeNullable(expectedType)
//
//            if (argumentTypeResolver.isSubtypeOfForArgumentType(dataFlowValue.type, nullableExpectedType) &&
//                (additionalPredicate == null || additionalPredicate(dataFlowValue.type))
//            ) {
//                if (!immanentlyNotNull && expression != null) {
//                    recordCastOrError(
//                        expression,
//                        dataFlowValue.type,
//                        c.trace,
//                        dataFlowValue,
//                        call,
//                        recordExpressionType
//                    )
//                }
//
//                return SmartCastResult(dataFlowValue.type, immanentlyNotNull || dataFlowValue.isStable)
//            }
//            return checkAndRecordPossibleCast(
//                dataFlowValue,
//                nullableExpectedType,
//                expression,
//                c,
//                call,
//                recordExpressionType
//            )
//        }

        return null
    }
}
