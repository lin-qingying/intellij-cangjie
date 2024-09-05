package com.huawei.cangjie.resolve.calls.components

import com.huawei.cangjie.descriptors.ClassifierDescriptorWithTypeParameters
import com.huawei.cangjie.descriptors.TypeParameterDescriptor
import com.huawei.cangjie.resolve.calls.inference.ConstraintSystemBuilder
import com.huawei.cangjie.resolve.calls.inference.addSubtypeConstraintIfCompatible
import com.huawei.cangjie.resolve.calls.inference.components.NewTypeSubstitutor
import com.huawei.cangjie.resolve.calls.inference.model.ArgumentConstraintPositionImpl
import com.huawei.cangjie.resolve.calls.inference.model.ConstraintPosition
import com.huawei.cangjie.resolve.calls.inference.model.ReceiverConstraintPositionImpl
import com.huawei.cangjie.resolve.calls.model.*
import com.huawei.cangjie.types.*
import com.huawei.cangjie.types.checker.captureFromExpression
import com.huawei.cangjie.types.checker.hasSupertypeWithGivenTypeConstructor
import com.huawei.cangjie.types.util.makeNotNullable
import com.huawei.cangjie.types.util.supertypes

fun checkSimpleArgument(
    csBuilder: ConstraintSystemBuilder,
    argument: SimpleCangJieCallArgument,
    expectedType: UnwrappedType?,
    diagnosticsHolder: CangJieDiagnosticsHolder,
    receiverInfo: ReceiverInfo,
    convertedType: UnwrappedType?,
    inferenceSession: InferenceSession?,
    selectorCall: CangJieCall?
): ResolvedAtom = when (argument) {
    is ExpressionCangJieCallArgument ->
        checkExpressionArgument(
            csBuilder,
            argument,
            expectedType,
            diagnosticsHolder,
            receiverInfo.isReceiver,
            convertedType,
            selectorCall
        )

    is SubCangJieCallArgument ->
        checkSubCallArgument(csBuilder, argument, expectedType, diagnosticsHolder, receiverInfo, inferenceSession)

    else ->
        unexpectedArgument(argument)
}

private fun checkSubCallArgument(
    csBuilder: ConstraintSystemBuilder,
    subCallArgument: SubCangJieCallArgument,
    expectedType: UnwrappedType?,
    diagnosticsHolder: CangJieDiagnosticsHolder,
    receiverInfo: ReceiverInfo,
    inferenceSession: InferenceSession?
): ResolvedAtom {
    val subCallResult = ResolvedSubCallArgument(
        subCallArgument, receiverInfo.isReceiver && inferenceSession?.resolveReceiverIndependently() == true
    )

    if (expectedType == null) return subCallResult

    val expectedNullableType = expectedType.makeOptionalAsSpecified(true)
    val position =
        if (receiverInfo.isReceiver) ReceiverConstraintPositionImpl(
            subCallArgument,
            subCallArgument.callResult.resultCallAtom.atom
        )
        else ArgumentConstraintPositionImpl(subCallArgument)

    // subArgument cannot has stable smartcast
    // return type can contains fixed type variables
    val currentReturnType =
        (csBuilder.buildCurrentSubstitutor() as NewTypeSubstitutor)
            .safeSubstitute(subCallArgument.receiver.receiverValue.type.unwrap())
    if (subCallArgument.isSafeCall) {
        csBuilder.addSubtypeConstraint(currentReturnType, expectedNullableType, position)
        return subCallResult
    }

    if (receiverInfo.isReceiver
        && !csBuilder.addSubtypeConstraintIfCompatible(currentReturnType, expectedType, position)
        && csBuilder.addSubtypeConstraintIfCompatible(currentReturnType, expectedNullableType, position)
    ) {
        if (receiverInfo.shouldReportUnsafeCall) {
            diagnosticsHolder.addDiagnostic(
                UnsafeCallError(
                    subCallArgument,
                    isForImplicitInvoke = receiverInfo.reportUnsafeCallAsUnsafeImplicitInvoke
                )
            )
        }
        return subCallResult
    }

    csBuilder.addSubtypeConstraint(currentReturnType, expectedType, position)
    return subCallResult
}

private fun checkExpressionArgument(
    csBuilder: ConstraintSystemBuilder,
    expressionArgument: ExpressionCangJieCallArgument,
    expectedType: UnwrappedType?,
    diagnosticsHolder: CangJieDiagnosticsHolder,
    isReceiver: Boolean,
    convertedType: UnwrappedType?,
    selectorCall: CangJieCall?
): ResolvedAtom {
    val resolvedExpression = ResolvedExpressionAtom(expressionArgument)
    if (expectedType == null) return resolvedExpression

    // todo run this approximation only once for call
    val argumentType = convertedType ?: captureFromTypeParameterUpperBoundIfNeeded(
        expressionArgument.receiver.stableType,
        expectedType
    )

    fun unstableSmartCastOrSubtypeError(
        unstableType: UnwrappedType?, actualExpectedType: UnwrappedType, position: ConstraintPosition
    ): CangJieCallDiagnostic? {
        if (unstableType != null) {
            if (csBuilder.addSubtypeConstraintIfCompatible(unstableType, actualExpectedType, position)) {
                return UnstableSmartCast(expressionArgument, unstableType, isReceiver)
            }
        }

        if (argumentType.isMarkedOption) {
            if (csBuilder.addSubtypeConstraintIfCompatible(argumentType, actualExpectedType, position)) return null
            if (csBuilder.addSubtypeConstraintIfCompatible(
                    argumentType.makeNotNullable(),
                    actualExpectedType,
                    position
                )
            ) {
                return ArgumentNullabilityErrorDiagnostic(actualExpectedType, argumentType, expressionArgument)
            }
        }

        csBuilder.addSubtypeConstraint(argumentType, actualExpectedType, position)
        return null
    }

    val position =
        if (isReceiver) ReceiverConstraintPositionImpl(expressionArgument, selectorCall)
        else ArgumentConstraintPositionImpl(expressionArgument)

    // Used only for arguments with @NotNull annotation
    if (expectedType is NotNullTypeParameter && argumentType.isMarkedOption) {
        diagnosticsHolder.addDiagnostic(
            ArgumentNullabilityErrorDiagnostic(
                expectedType,
                argumentType,
                expressionArgument
            )
        )
    }

    if (expressionArgument.isSafeCall) {
        val expectedNullableType = expectedType.makeOptionalAsSpecified(true)
        if (!csBuilder.addSubtypeConstraintIfCompatible(argumentType, expectedNullableType, position)) {
            diagnosticsHolder.addDiagnosticIfNotNull(
                unstableSmartCastOrSubtypeError(
                    expressionArgument.receiver.unstableType,
                    expectedNullableType,
                    position
                )
            )
        }
        return resolvedExpression
    }

    if (!csBuilder.addSubtypeConstraintIfCompatible(argumentType, expectedType, position)) {
        if (!isReceiver) {
            diagnosticsHolder.addDiagnosticIfNotNull(
                unstableSmartCastOrSubtypeError(
                    expressionArgument.receiver.unstableType,
                    expectedType,
                    position
                )
            )
            return resolvedExpression
        }

        val unstableType = expressionArgument.receiver.unstableType
        val expectedNullableType = expectedType.makeOptionalAsSpecified(true)

        if (unstableType != null && csBuilder.addSubtypeConstraintIfCompatible(unstableType, expectedType, position)) {
            diagnosticsHolder.addDiagnostic(UnstableSmartCast(expressionArgument, unstableType, isReceiver))
        } else if (csBuilder.addSubtypeConstraintIfCompatible(argumentType, expectedNullableType, position)) {
            diagnosticsHolder.addDiagnostic(UnsafeCallError(expressionArgument))
        } else {
            csBuilder.addSubtypeConstraint(argumentType, expectedType, position)
        }
    }

    return resolvedExpression
}

/**
 * interface Inv<T>
 * fun <Y> bar(l: Inv<Y>): Y = ...
 *
 * fun <X : Inv<out Int>> foo(x: X) {
 *      val xr = bar(x)
 * }
 * Here we try to capture from upper bound from type parameter.
 * We replace type of `x` to `Inv<out Int>`(we chose supertype which contains supertype with expectedTypeConstructor) and capture from this type.
 * It is correct, because it is like this code:
 * fun <X : Inv<out Int>> foo(x: X) {
 *      val inv: Inv<out Int> = x
 *      val xr = bar(inv)
 * }
 *
 */
fun captureFromTypeParameterUpperBoundIfNeeded(
    argumentType: UnwrappedType,
    expectedType: UnwrappedType
): UnwrappedType {
    val expectedTypeConstructor = expectedType.upperIfFlexible().constructor

    if (argumentType.lowerIfFlexible().constructor.declarationDescriptor is TypeParameterDescriptor) {
        val chosenSupertype = argumentType.lowerIfFlexible().supertypes().singleOrNull {
            it.constructor.declarationDescriptor is ClassifierDescriptorWithTypeParameters &&
                    it.unwrap().hasSupertypeWithGivenTypeConstructor(expectedTypeConstructor)
        }
        if (chosenSupertype != null) {
            val capturedType = captureFromExpression(chosenSupertype.unwrap())
            return if (capturedType != null && argumentType.isDefinitelyNotNullType)
                capturedType.makeDefinitelyNotNullOrNotNull()
            else
                capturedType ?: argumentType
        }
    }

    return argumentType
}
