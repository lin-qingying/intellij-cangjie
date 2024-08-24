package com.huawei.cangjie.resolve.calls.smartcasts

import com.huawei.cangjie.resolve.calls.ArgumentTypeResolver


class SmartCastManager(private val argumentTypeResolver: ArgumentTypeResolver) {

//    fun getSmartCastVariants(
//        receiverToCast: ReceiverValue,
//        bindingContext: BindingContext,
//        containingDeclarationOrModule: DeclarationDescriptor,
//        dataFlowInfo: DataFlowInfo,
//        languageVersionSettings: LanguageVersionSettings,
//        dataFlowValueFactory: DataFlowValueFactory
//    ): List<CangJieType> {
//        val variants = getSmartCastVariantsExcludingReceiver(
//            bindingContext, containingDeclarationOrModule, dataFlowInfo, receiverToCast, languageVersionSettings, dataFlowValueFactory
//        )
//        val result = ArrayList<CangJieType>(variants.size + 1)
//        result.add(receiverToCast.type)
//        result.addAll(variants)
//        return result
//    }
//
//    /**
//     * @return variants @param receiverToCast may be cast to according to context dataFlowInfo, receiverToCast itself is NOT included
//     */
//    fun getSmartCastVariantsExcludingReceiver(
//        context: ResolutionContext<*>,
//        receiverToCast: ReceiverValue
//    ): Collection<CangJieType> {
//        return getSmartCastVariantsExcludingReceiver(
//            context.trace.bindingContext,
//            context.scope.ownerDescriptor,
//            context.dataFlowInfo,
//            receiverToCast,
//            context.languageVersionSettings,
//            context.dataFlowValueFactory
//        )
//    }
//
//    /**
//     * @return variants @param receiverToCast may be cast to according to @param dataFlowInfo, @param receiverToCast itself is NOT included
//     */
//    private fun getSmartCastVariantsExcludingReceiver(
//        bindingContext: BindingContext,
//        containingDeclarationOrModule: DeclarationDescriptor,
//        dataFlowInfo: DataFlowInfo,
//        receiverToCast: ReceiverValue,
////        languageVersionSettings: LanguageVersionSettings,
//        dataFlowValueFactory: DataFlowValueFactory
//    ): Collection<CangJieType> {
//        val dataFlowValue = dataFlowValueFactory.createDataFlowValue(receiverToCast, bindingContext, containingDeclarationOrModule)
//        return dataFlowInfo.getCollectedTypes(dataFlowValue, languageVersionSettings)
//    }
//
//    fun getSmartCastReceiverResult(
//        receiverArgument: ReceiverValue,
//        receiverParameterType: CangJieType,
//        context: ResolutionContext<*>
//    ): ReceiverSmartCastResult? {
//        getSmartCastReceiverResultWithGivenNullability(receiverArgument, receiverParameterType, context)?.let {
//            return it
//        }
//
//        val nullableParameterType = TypeUtils.makeNullable(receiverParameterType)
//        return when {
//            getSmartCastReceiverResultWithGivenNullability(receiverArgument, nullableParameterType, context) == null -> null
//            else -> ReceiverSmartCastResult.SMARTCAST_NEEDED_OR_NOT_NULL_EXPECTED
//        }
//    }
//
//    private fun getSmartCastReceiverResultWithGivenNullability(
//        receiverArgument: ReceiverValue,
//        receiverParameterType: CangJieType,
//        context: ResolutionContext<*>
//    ): ReceiverSmartCastResult? =
//        when {
//            argumentTypeResolver.isSubtypeOfForArgumentType(receiverArgument.type, receiverParameterType) ->
//                ReceiverSmartCastResult.OK
//            getSmartCastVariantsExcludingReceiver(context, receiverArgument).any {
//                argumentTypeResolver.isSubtypeOfForArgumentType(it, receiverParameterType)
//            } ->
//                ReceiverSmartCastResult.SMARTCAST_NEEDED_OR_NOT_NULL_EXPECTED
//            else -> null
//        }
//
//    fun checkAndRecordPossibleCast(
//        dataFlowValue: DataFlowValue,
//        expectedType: CangJieType,
//        expression: CjExpression?,
//        c: ResolutionContext<*>,
//        call: Call?,
//        recordExpressionType: Boolean,
//        additionalPredicate: ((CangJieType) -> Boolean)? = null
//    ): SmartCastResult? {
//        val calleeExpression = call?.calleeExpression
//        val expectedTypes = if (c.languageVersionSettings.supportsFeature(LanguageFeature.NewInference))
//            expectedType.expandIntersectionTypeIfNecessary()
//        else
//            listOf(expectedType)
//
//        val builderInferenceSubstitutor = (c.inferenceSession as? BuilderInferenceSession)?.getNotFixedToInferredTypesSubstitutor()
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
//        if (!c.dataFlowInfo.getCollectedNullability(dataFlowValue).canBeNull() && !expectedType.isMarkedOption) {
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
//                    recordCastOrError(expression, dataFlowValue.type, c.trace, dataFlowValue, call, recordExpressionType)
//                }
//
//                return SmartCastResult(dataFlowValue.type, immanentlyNotNull || dataFlowValue.isStable)
//            }
//            return checkAndRecordPossibleCast(dataFlowValue, nullableExpectedType, expression, c, call, recordExpressionType)
//        }
//
//        return null
//    }
//
//    enum class ReceiverSmartCastResult {
//        OK,
//        SMARTCAST_NEEDED_OR_NOT_NULL_EXPECTED
//    }
//
//    companion object {
//        // REVIEW: make it non-static too?
//        private fun recordCastOrError(
//            expression: CjExpression,
//            type: CangJieType,
//            trace: BindingTrace,
//            dataFlowValue: DataFlowValue,
//            call: Call?,
//            recordExpressionType: Boolean
//        ) {
//            if (CangJieBuiltIns.isNullableNothing(type)) return
//            if (dataFlowValue.isStable) {
//                if (dataFlowValue.kind == DataFlowValue.Kind.LEGACY_ALIEN_BASE_PROPERTY ||
//                    dataFlowValue.kind == DataFlowValue.Kind.LEGACY_ALIEN_BASE_PROPERTY_INHERITED_IN_INVISIBLE_CLASS ||
//                    dataFlowValue.kind == DataFlowValue.Kind.LEGACY_STABLE_LOCAL_DELEGATED_PROPERTY
//                ) {
//                    trace.report(Errors.DEPRECATED_SMARTCAST.on(expression, type, expression.text, dataFlowValue.kind.description))
//                }
//
//                updateSmartCast(trace, expression, call, type, SMARTCAST)
//                if (recordExpressionType) {
//                    //TODO
//                    //Why the expression type is rewritten for receivers and is not rewritten for arguments? Is it necessary?
//                    trace.recordType(expression, type)
//                }
//            } else {
//                updateSmartCast(trace, expression, call, type, UNSTABLE_SMARTCAST)
//                trace.report(SMARTCAST_IMPOSSIBLE.on(expression, type, expression.text, dataFlowValue.kind.description))
//            }
//        }
//
//        private fun updateSmartCast(
//            trace: BindingTrace,
//            expression: CjExpression,
//            call: Call?,
//            type: CangJieType,
//            key: WritableSlice<CjExpression, ExplicitSmartCasts>?
//        ) {
//            val oldSmartCasts = trace[key, expression]
//            val newSmartCast = SingleSmartCast(call, type)
//            if (oldSmartCasts != null) {
//                val oldType = oldSmartCasts.type(call)
//                if (oldType != null && oldType != type) {
//                    throw AssertionError("Rewriting key $call for smart cast on ${expression.text}")
//                }
//            }
//            val updatedSmartCasts = oldSmartCasts?.let { it + newSmartCast } ?: newSmartCast
//            trace.record(key, expression, updatedSmartCasts)
//        }
//    }
}
