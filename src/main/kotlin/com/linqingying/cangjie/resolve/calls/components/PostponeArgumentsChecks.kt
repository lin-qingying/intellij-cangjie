package com.linqingying.cangjie.resolve.calls.components

import com.linqingying.cangjie.builtins.*
import com.linqingying.cangjie.descriptors.annotations.Annotations
import com.linqingying.cangjie.resolve.calls.inference.ConstraintSystemBuilder
import com.linqingying.cangjie.resolve.calls.inference.components.NewTypeSubstitutor
import com.linqingying.cangjie.resolve.calls.inference.model.*
import com.linqingying.cangjie.resolve.calls.model.*
import com.linqingying.cangjie.types.CangJieType
import com.linqingying.cangjie.types.ErrorUtils
import com.linqingying.cangjie.types.UnwrappedType
import com.linqingying.cangjie.types.error.ErrorTypeKind
import com.linqingying.cangjie.types.util.builtIns
import com.intellij.testFramework.requireIs

fun LambdaWithTypeVariableAsExpectedTypeAtom.transformToResolvedLambda(
    csBuilder: ConstraintSystemBuilder,
    diagnosticsHolder: CangJieDiagnosticsHolder,
    expectedType: UnwrappedType? = null,
    returnTypeVariable: TypeVariableForLambdaReturnType? = null
): ResolvedLambdaAtom {
    val fixedExpectedType = (csBuilder.buildCurrentSubstitutor() as NewTypeSubstitutor)
        .safeSubstitute(expectedType ?: this.expectedType)
    val resolvedLambdaAtom = preprocessLambdaArgument(
        csBuilder,
        atom,
        fixedExpectedType,
        diagnosticsHolder,
        forceResolution = true,
        returnTypeVariable = returnTypeVariable
    ) as ResolvedLambdaAtom

    setAnalyzed(resolvedLambdaAtom)

    return resolvedLambdaAtom
}

private fun preprocessLambdaArgument(
    csBuilder: ConstraintSystemBuilder,
    argument: LambdaCangJieCallArgument,
    expectedType: UnwrappedType?,
    diagnosticsHolder: CangJieDiagnosticsHolder,
    forceResolution: Boolean = false,
    returnTypeVariable: TypeVariableForLambdaReturnType? = null
): ResolvedAtom {

    if (expectedType != null && !forceResolution) {
        // postpone lambda processing if expected type is a type variable that could be fixed into something non-trivial
        val expectedTypeVariableWithConstraints =
            csBuilder.currentStorage().notFixedTypeVariables[expectedType.constructor]

        if (expectedTypeVariableWithConstraints != null) {
            val explicitTypeArgument = expectedTypeVariableWithConstraints.constraints.find {
                it.kind == ConstraintKind.EQUALITY && it.position.from is ExplicitTypeParameterConstraintPosition<*>
            }?.type as? CangJieType

            if (explicitTypeArgument == null || explicitTypeArgument.arguments.isNotEmpty()) {
                return LambdaWithTypeVariableAsExpectedTypeAtom(argument, expectedType)
            }
        }
    }

    val resolvedArgument = extractLambdaInfoFromFunctionalType(expectedType, argument, returnTypeVariable)
        ?: extraLambdaInfo(expectedType, argument, csBuilder, diagnosticsHolder)

    if (expectedType != null) {
        val lambdaType = createFunctionType(
            csBuilder.builtIns, Annotations.EMPTY, resolvedArgument.receiver, resolvedArgument.contextReceivers,
            resolvedArgument.parameters, null, resolvedArgument.returnType
        )
        csBuilder.addSubtypeConstraint(lambdaType, expectedType, ArgumentConstraintPositionImpl(argument))
    }

    return resolvedArgument
}

private fun extractLambdaInfoFromFunctionalType(
    expectedType: UnwrappedType?,
    argument: LambdaCangJieCallArgument,
    returnTypeVariable: TypeVariableForLambdaReturnType? = null
): ResolvedLambdaAtom? {
    if (expectedType == null || !expectedType.isBuiltinFunctionalType) return null
    val parametersTypes = argument.parametersTypes
    val expectedParameters = expectedType.getValueParameterTypesFromFunctionType()
    val expectedReceiver = expectedType.getReceiverTypeFromFunctionType()?.unwrap()
    val expectedContextReceivers =
        expectedType.getContextReceiverTypesFromFunctionType().map { it.unwrap() }.toTypedArray()
    val argumentAsFunctionExpression = argument as? FunctionExpression

    val receiverFromExpected = argumentAsFunctionExpression?.receiverType == null && expectedReceiver != null

    fun UnwrappedType?.orExpected(index: Int) =
        this ?: expectedParameters.getOrNull(index)?.type?.unwrap() ?: expectedType.builtIns.anyType

    // Extracting parameters and receiver type, taking into account the actual lambda definition and expected lambda type
    val (parameters, receiver) = when {
        argumentAsFunctionExpression != null -> {
            // lambda has explicit functional type - use types from it if available
            (parametersTypes?.mapIndexed { index, type ->
                type.orExpected(index)
            } ?: emptyList()) to argumentAsFunctionExpression.receiverType
        }

        (parametersTypes?.size ?: 0) == expectedParameters.size && receiverFromExpected -> {
            // expected type has receiver, but arguments sizes are the same in actual and expected, so assuming missing (maybe unused) receiver in lambda
            // TODO: in case of implicit parameters in lambda ("this" and "it") this case assumes "this", probably we should generate two possible overloads and choose among them later
            (parametersTypes?.mapIndexed { index, type ->
                type.orExpected(index)
            } ?: expectedParameters.map { it.type.unwrap() }) to expectedReceiver
        }

        (parametersTypes?.size ?: 0) - expectedParameters.size == 1 && receiverFromExpected -> {
            // one "missing" parameter in the expected parameters - first lambda parameter should be mapped to expected receiver
            // TODO: same "this" or "it" case from above could be applicable here as well

            (parametersTypes?.mapIndexed { index, type ->
                type ?: run {
                    expectedParameters.getOrNull(index)?.type?.unwrap()
                } ?: expectedType.builtIns.anyType
            } ?: expectedParameters.map { it.type.unwrap() }) to expectedReceiver?.unwrap()
        }

        else ->
            (parametersTypes?.mapIndexed { index, type ->
                type.orExpected(index)
            } ?: expectedParameters.map { it.type.unwrap() }) to (if (receiverFromExpected) expectedReceiver else null)
    }
    val contextReceivers =
        (argumentAsFunctionExpression?.contextReceiversTypes ?: expectedContextReceivers).filterNotNull()

    val returnType = argumentAsFunctionExpression?.returnType ?: expectedType.getReturnTypeFromFunctionType().unwrap()

    return ResolvedLambdaAtom(
        argument,

        receiver,
        contextReceivers,
        parameters,
        returnType,
        typeVariableForLambdaReturnType = returnTypeVariable,
        expectedType = expectedType
    )
}

private fun extraLambdaInfo(
    expectedType: UnwrappedType?,
    argument: LambdaCangJieCallArgument,
    csBuilder: ConstraintSystemBuilder,
    diagnosticsHolder: CangJieDiagnosticsHolder
): ResolvedLambdaAtom {
    val builtIns = csBuilder.builtIns


    val isFunctionSupertype =
        expectedType != null/* && CangJieBuiltIns.isNotNullOrNullableFunctionSupertype(expectedType)*/
    val argumentAsFunctionExpression = argument as? FunctionExpression

    val typeVariable = TypeVariableForLambdaReturnType(builtIns, "_L")

    val receiverType = argumentAsFunctionExpression?.receiverType
    val returnType =
        argumentAsFunctionExpression?.returnType ?: expectedType?.arguments?.singleOrNull()?.type?.unwrap()
            ?.takeIf { isFunctionSupertype }
        ?: typeVariable.defaultType

    val contextReceiversTypes =
        argumentAsFunctionExpression?.contextReceiversTypes?.mapIndexed { index, contextReceiverType ->
            if (contextReceiverType != null) {
                contextReceiverType
            } else {
                diagnosticsHolder.addDiagnostic(NotEnoughInformationForLambdaParameter(argument, index))
                ErrorUtils.createErrorType(ErrorTypeKind.UNINFERRED_LAMBDA_CONTEXT_RECEIVER_TYPE)
            }
        } ?: emptyList()
    val parameters = argument.parametersTypes?.mapIndexed { index, parameterType ->
        if (parameterType != null) {
            parameterType
        } else {
            diagnosticsHolder.addDiagnostic(NotEnoughInformationForLambdaParameter(argument, index))
            ErrorUtils.createErrorType(ErrorTypeKind.UNINFERRED_LAMBDA_PARAMETER_TYPE)
        }
    } ?: emptyList()

    val newTypeVariableUsed = returnType == typeVariable.defaultType
    if (newTypeVariableUsed) csBuilder.registerVariable(typeVariable)

    return ResolvedLambdaAtom(
        argument,

        receiverType,
        contextReceiversTypes,
        parameters,
        returnType,
        typeVariable.takeIf { newTypeVariableUsed },
        expectedType
    )
}

internal val ConstraintSystemBuilder.builtIns: CangJieBuiltIns get() = ((this as NewConstraintSystemImpl).typeSystemContext as BuiltInsProvider).builtIns
fun resolveCjPrimitive(
    csBuilder: ConstraintSystemBuilder,
    argument: CangJieCallArgument,
    expectedType: UnwrappedType?,
    diagnosticsHolder: CangJieDiagnosticsHolder,
    receiverInfo: ReceiverInfo,
    convertedType: UnwrappedType?,
    inferenceSession: InferenceSession?,
    selectorCall: CangJieCall? = null,
): ResolvedAtom = when (argument) {
    is SimpleCangJieCallArgument -> checkSimpleArgument(
        csBuilder,
        argument,
        expectedType,
        diagnosticsHolder,
        receiverInfo,
        convertedType,
        inferenceSession,
        selectorCall
    )

    is LambdaCangJieCallArgument ->
        preprocessLambdaArgument(csBuilder, argument, expectedType, diagnosticsHolder)

    is CallableReferenceCangJieCallArgument ->
        preprocessCallableReference(csBuilder, argument, expectedType, diagnosticsHolder)

    is CollectionLiteralCangJieCallArgument ->
        preprocessCollectionLiteralArgument(argument, expectedType)

    else -> unexpectedArgument(argument)
}

private fun preprocessCallableReference(
    csBuilder: ConstraintSystemBuilder,
    argument: CallableReferenceCangJieCallArgument,
    expectedType: UnwrappedType?,
    diagnosticsHolder: CangJieDiagnosticsHolder
): ResolvedAtom {
    val result = EagerCallableReferenceAtom(argument, expectedType)

    if (expectedType == null) return result

    val notCallableTypeConstructor =
        csBuilder.getProperSuperTypeConstructors(expectedType)
            .firstOrNull { !ReflectionTypes.isPossibleExpectedCallableType(it.requireIs()) }

    if (notCallableTypeConstructor != null) {
        diagnosticsHolder.addDiagnostic(
            NotCallableExpectedType(
                argument,
                expectedType,
                notCallableTypeConstructor.requireIs()
            )
        )
    }
    return result
}

private fun preprocessCollectionLiteralArgument(
    collectionLiteralArgument: CollectionLiteralCangJieCallArgument,
    expectedType: UnwrappedType?
): ResolvedAtom {
    // todo add some checks about expected type
    return ResolvedCollectionLiteralAtom(collectionLiteralArgument, expectedType)
}
