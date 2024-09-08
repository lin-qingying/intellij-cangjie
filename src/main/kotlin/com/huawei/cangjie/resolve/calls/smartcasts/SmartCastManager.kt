package com.huawei.cangjie.resolve.calls.smartcasts

import com.huawei.cangjie.config.LanguageFeature
import com.huawei.cangjie.config.LanguageVersionSettings
import com.huawei.cangjie.descriptors.DeclarationDescriptor
import com.huawei.cangjie.psi.Call
import com.huawei.cangjie.psi.CjExpression
import com.huawei.cangjie.resolve.BindingContext
import com.huawei.cangjie.resolve.calls.ArgumentTypeResolver
import com.huawei.cangjie.resolve.calls.context.ResolutionContext
import com.huawei.cangjie.resolve.scopes.receivers.ReceiverValue
import com.huawei.cangjie.types.CangJieType
import com.huawei.cangjie.types.util.expandIntersectionTypeIfNecessary


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
            bindingContext, containingDeclarationOrModule, dataFlowInfo, receiverToCast, languageVersionSettings, dataFlowValueFactory
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
        val dataFlowValue = dataFlowValueFactory.createDataFlowValue(receiverToCast, bindingContext, containingDeclarationOrModule)
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
