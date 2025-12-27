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

package org.cangnova.cangjie.resolve.calls.components

import org.cangnova.cangjie.builtins.*
import org.cangnova.cangjie.descriptors.ReceiverParameterDescriptor
import org.cangnova.cangjie.descriptors.ValueParameterDescriptor
import org.cangnova.cangjie.resolve.DescriptorUtils
import org.cangnova.cangjie.resolve.calls.components.candidate.CallableReferenceResolutionCandidate
import org.cangnova.cangjie.resolve.calls.inference.ConstraintSystemOperation
import org.cangnova.cangjie.resolve.calls.inference.components.FreshVariableNewTypeSubstitutor
import org.cangnova.cangjie.resolve.calls.inference.model.ArgumentConstraintPositionImpl
import org.cangnova.cangjie.resolve.calls.inference.model.CallableReferenceConstraintPositionImpl
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintPosition
import org.cangnova.cangjie.resolve.calls.model.*
import org.cangnova.cangjie.resolve.calls.tower.PrioritizedCompositeScopeTowerProcessor
import org.cangnova.cangjie.resolve.calls.tower.SamePriorityCompositeScopeTowerProcessor
import org.cangnova.cangjie.resolve.calls.tower.ScopeTowerProcessor
import org.cangnova.cangjie.resolve.scopes.receivers.ReceiverValueWithSmartCastInfo
import org.cangnova.cangjie.types.AbstractTypeChecker
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.ErrorUtils
import org.cangnova.cangjie.types.TypeUtils
import org.cangnova.cangjie.types.UnwrappedType
import org.cangnova.cangjie.types.checker.captureFromExpression
import org.cangnova.cangjie.types.expressions.CoercionStrategy
import org.cangnova.cangjie.types.getReturnTypeFromFunctionType
import org.cangnova.cangjie.types.getValueParameterTypesFromFunctionType
import org.cangnova.cangjie.types.isFunctionType
import org.cangnova.cangjie.types.model.TypeVariance
import org.cangnova.cangjie.types.model.convertVariance
import kotlin.Array
import kotlin.Int
import kotlin.assert
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.first
import kotlin.collections.listOfNotNull
import kotlin.collections.map
import kotlin.collections.plus
import kotlin.let
import kotlin.map
import kotlin.sequences.map
import kotlin.text.map


sealed class CallableReceiver(val receiver: ReceiverValueWithSmartCastInfo) {
    class UnboundReference(receiver: ReceiverValueWithSmartCastInfo) : CallableReceiver(receiver)
    class BoundValueReference(receiver: ReceiverValueWithSmartCastInfo) : CallableReceiver(receiver)
    class ScopeReceiver(receiver: ReceiverValueWithSmartCastInfo) : CallableReceiver(receiver)
    class ExplicitValueReceiver(receiver: ReceiverValueWithSmartCastInfo) : CallableReceiver(receiver)
}

private fun ConstraintSystemOperation.addLhsTypeConstraint(
    lhsType: CangJieType,
    expectedType: UnwrappedType,
    position: ConstraintPosition
) {
    if (!ReflectionTypes.isNumberedTypeWithOneOrMoreNumber(expectedType)) return

    val expectedTypeProjectionForLHS = expectedType.arguments.first()
    val expectedTypeForLHS = expectedTypeProjectionForLHS.type
    val expectedTypeVariance = expectedTypeProjectionForLHS.projectionKind.convertVariance()
    val effectiveVariance = AbstractTypeChecker.effectiveVariance(
        expectedType.constructor.parameters.first().variance.convertVariance(),
        expectedTypeVariance
    ) ?: expectedTypeVariance

    when (effectiveVariance) {
        TypeVariance.INV -> addEqualityConstraint(lhsType, expectedTypeForLHS, position)
//        TypeVariance.IN -> addSubtypeConstraint(expectedTypeForLHS, lhsType, position)
//        TypeVariance.OUT -> addSubtypeConstraint(lhsType, expectedTypeForLHS, position)
    }
}

class CallableReferenceAdaptation(
    val argumentTypes: Array<CangJieType>,
    val coercionStrategy: CoercionStrategy,
    val defaults: Int,
    val mappedArguments: Map<ValueParameterDescriptor, ResolvedCallArgument>,
//    val suspendConversionStrategy: SuspendConversionStrategy
)

fun CallableReferenceResolutionCandidate.addConstraints(
    constraintSystem: ConstraintSystemOperation,
    substitutor: FreshVariableNewTypeSubstitutor,
    callableReference: CallableReferenceResolutionAtom
) {
    val lhsResult = callableReference.lhsResult
    val position = when (callableReference) {
        is CallableReferenceCangJieCallArgument -> ArgumentConstraintPositionImpl(callableReference)
        is CallableReferenceCangJieCall -> CallableReferenceConstraintPositionImpl(callableReference)

    }

    if (lhsResult is LHSResult.Type && expectedType != null && !TypeUtils.noExpectedType(expectedType)) {
        // NB: regular objects have lhsResult of `LHSResult.Object` type and won't be proceeded here
        val isStaticOrCompanionMember =
            DescriptorUtils.isStaticDeclaration(candidate)
        if (!isStaticOrCompanionMember) {
            constraintSystem.addLhsTypeConstraint(lhsResult.unboundDetailedReceiver.stableType, expectedType, position)
        }
    }

    if (!ErrorUtils.isError(candidate)) {
        constraintSystem.addReceiverConstraint(
            substitutor,
            dispatchReceiver,
            candidate.dispatchReceiverParameter,
            position
        )

    }

    if (expectedType != null && !TypeUtils.noExpectedType(expectedType) && !constraintSystem.hasContradiction) {
        constraintSystem.addSubtypeConstraint(
            substitutor.safeSubstitute(reflectionCandidateType),
            expectedType,
            position
        )
    }
}

private fun ConstraintSystemOperation.addReceiverConstraint(
    toFreshSubstitutor: FreshVariableNewTypeSubstitutor,
    receiverArgument: CallableReceiver?,
    receiverParameter: ReceiverParameterDescriptor?,
    position: ConstraintPosition
) {
    if (receiverArgument == null || receiverParameter == null) {
        assert(receiverArgument == null) { "Receiver argument should be null if parameter is: $receiverArgument" }
        assert(receiverParameter == null) { "Receiver parameter should be null if argument is: $receiverParameter" }
        return
    }

    val expectedType = toFreshSubstitutor.safeSubstitute(receiverParameter.value.type.unwrap())
    val receiverType = receiverArgument.receiver.stableType.let { captureFromExpression(it) ?: it }

    addSubtypeConstraint(receiverType, expectedType, position)
}

data class InputOutputTypes(val inputTypes: List<UnwrappedType>, val outputType: UnwrappedType)

fun extractInputOutputTypesFromCallableReferenceExpectedType(expectedType: UnwrappedType?): InputOutputTypes? {
    if (expectedType == null) return null

    return when {
        expectedType.isFunctionType ->
            extractInputOutputTypesFromFunctionType(expectedType)

//        ReflectionTypes.isBaseTypeForNumberedReferenceTypes(expectedType) ->
//            InputOutputTypes(emptyList(), expectedType.arguments.single().type.unwrap())
//
//        ReflectionTypes.isNumberedKFunction(expectedType) -> {
//            val functionFromSupertype = expectedType.immediateSupertypes().first { it.isFunctionType }.unwrap()
//            extractInputOutputTypesFromFunctionType(functionFromSupertype)
//        }
//
//        ReflectionTypes.isNumberedKSuspendFunction(expectedType) -> {
//            val kSuspendFunctionType = expectedType.immediateSupertypes().first { it.isSuspendFunctionType }.unwrap()
//            extractInputOutputTypesFromFunctionType(kSuspendFunctionType)
//        }
//
//        ReflectionTypes.isNumberedKPropertyOrKMutablePropertyType(expectedType) -> {
//            val functionFromSupertype = expectedType.supertypes().first { it.isFunctionType }.unwrap()
//            extractInputOutputTypesFromFunctionType(functionFromSupertype)
//        }

        else -> null
    }
}

private fun extractInputOutputTypesFromFunctionType(functionType: UnwrappedType): InputOutputTypes {
    // 仓颉没有扩展函数类型，输入类型只包含参数类型
    val parameters = functionType.getValueParameterTypesFromFunctionType().map { it.type.unwrap() }
    val outputType = functionType.getReturnTypeFromFunctionType().unwrap()

    return InputOutputTypes(parameters, outputType)
}


fun createCallableReferenceProcessor(factory: CallableReferencesCandidateFactory): ScopeTowerProcessor<CallableReferenceResolutionCandidate> {
    when (val lhsResult = factory.cangjieCall.lhsResult) {
        LHSResult.Empty, LHSResult.Error, is LHSResult.Expression -> {
            val explicitReceiver = (lhsResult as? LHSResult.Expression)?.lshCallArgument?.receiver
            return factory.createCallableProcessor(explicitReceiver)
        }

        is LHSResult.Type -> {
            val static = lhsResult.qualifier?.let(factory::createCallableProcessor)
            val unbound = factory.createCallableProcessor(lhsResult.unboundDetailedReceiver)

            // note that if we use PrioritizedCompositeScopeTowerProcessor then static will win over unbound members
            val staticOrUnbound =
                if (static != null)
                    SamePriorityCompositeScopeTowerProcessor(static, unbound)
                else
                    unbound

            val asValue = lhsResult.qualifier?.classValueReceiverWithSmartCastInfo ?: return staticOrUnbound
            return PrioritizedCompositeScopeTowerProcessor(staticOrUnbound, factory.createCallableProcessor(asValue))
        }


    }
//    return factory.createCallableProcessor(factory.cangjieCall.call.explicitReceiver?.receiver)

}
