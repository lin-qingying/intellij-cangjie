package com.linqingying.cangjie.resolve.calls.components

import com.linqingying.cangjie.builtins.getReceiverTypeFromFunctionType
import com.linqingying.cangjie.builtins.getReturnTypeFromFunctionType
import com.linqingying.cangjie.builtins.getValueParameterTypesFromFunctionType
import com.linqingying.cangjie.builtins.isFunctionType
import com.linqingying.cangjie.descriptors.ReceiverParameterDescriptor
import com.linqingying.cangjie.descriptors.ValueParameterDescriptor
import com.linqingying.cangjie.resolve.DescriptorUtils
import com.linqingying.cangjie.resolve.calls.components.candidate.CallableReferenceResolutionCandidate
import com.linqingying.cangjie.resolve.calls.inference.ConstraintSystemOperation
import com.linqingying.cangjie.resolve.calls.inference.components.FreshVariableNewTypeSubstitutor
import com.linqingying.cangjie.resolve.calls.inference.model.ArgumentConstraintPositionImpl
import com.linqingying.cangjie.resolve.calls.inference.model.CallableReferenceConstraintPositionImpl
import com.linqingying.cangjie.resolve.calls.inference.model.ConstraintPosition
import com.linqingying.cangjie.resolve.calls.model.*
import com.linqingying.cangjie.resolve.scopes.receivers.ReceiverValueWithSmartCastInfo
import com.linqingying.cangjie.types.CangJieType
import com.linqingying.cangjie.types.ErrorUtils
import com.linqingying.cangjie.types.UnwrappedType
import com.linqingying.cangjie.types.checker.captureFromExpression
import com.linqingying.cangjie.types.expressions.CoercionStrategy
import com.linqingying.cangjie.types.util.TypeUtils


sealed class CallableReceiver(val receiver: ReceiverValueWithSmartCastInfo) {
    class UnboundReference(receiver: ReceiverValueWithSmartCastInfo) : CallableReceiver(receiver)
    class BoundValueReference(receiver: ReceiverValueWithSmartCastInfo) : CallableReceiver(receiver)
    class ScopeReceiver(receiver: ReceiverValueWithSmartCastInfo) : CallableReceiver(receiver)
    class ExplicitValueReceiver(receiver: ReceiverValueWithSmartCastInfo) : CallableReceiver(receiver)
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

//    if (lhsResult is LHSResult.Type && expectedType != null && !TypeUtils.noExpectedType(expectedType)) {
//        // NB: regular objects have lhsResult of `LHSResult.Object` type and won't be proceeded here
//        val isStaticOrCompanionMember =
//            DescriptorUtils.isStaticDeclaration(candidate) || candidate.containingDeclaration.isCompanionObject()
//        if (!isStaticOrCompanionMember) {
//            constraintSystem.addLhsTypeConstraint(lhsResult.unboundDetailedReceiver.stableType, expectedType, position)
//        }
//    }

    if (!ErrorUtils.isError(candidate)) {
        constraintSystem.addReceiverConstraint(substitutor, dispatchReceiver, candidate.dispatchReceiverParameter, position)
        constraintSystem.addReceiverConstraint(substitutor, extensionReceiver, candidate.extensionReceiverParameter, position)
    }

    if (expectedType != null && !TypeUtils.noExpectedType(expectedType) && !constraintSystem.hasContradiction) {
        constraintSystem.addSubtypeConstraint(substitutor.safeSubstitute(reflectionCandidateType), expectedType, position)
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
        expectedType.isFunctionType  ->
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
    val receiver = functionType.getReceiverTypeFromFunctionType()?.unwrap()
    val parameters = functionType.getValueParameterTypesFromFunctionType().map { it.type.unwrap() }

    val inputTypes = listOfNotNull(receiver) + parameters
    val outputType = functionType.getReturnTypeFromFunctionType().unwrap()

    return InputOutputTypes(inputTypes, outputType)
}
