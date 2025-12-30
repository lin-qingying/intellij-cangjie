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
import org.cangnova.cangjie.descriptors.annotations.Annotations
import org.cangnova.cangjie.resolve.calls.inference.ConstraintSystemBuilder
import org.cangnova.cangjie.resolve.calls.inference.components.NewTypeSubstitutor
import org.cangnova.cangjie.resolve.calls.inference.model.*
import org.cangnova.cangjie.resolve.calls.model.*
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.ErrorUtils
import org.cangnova.cangjie.types.UnwrappedType
import org.cangnova.cangjie.types.createFunctionType
import org.cangnova.cangjie.types.error.ErrorTypeKind
import org.cangnova.cangjie.types.getReturnTypeFromFunctionType
import org.cangnova.cangjie.types.getValueParameterTypesFromFunctionType
import org.cangnova.cangjie.types.isBuiltinFunctionalType
import org.cangnova.cangjie.types.builtIns
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
            csBuilder.builtIns, Annotations.EMPTY, resolvedArgument.receiver ,
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
    val argumentAsFunctionExpression = argument as? FunctionExpression

    fun UnwrappedType?.orExpected(index: Int) =
        this ?: expectedParameters.getOrNull(index)?.type?.unwrap() ?: expectedType.builtIns.stdlibTypes.anyType

    // 仓颉没有扩展函数类型，简化参数提取逻辑
    // 从实际 lambda 定义或期望的函数类型中提取参数类型
    val parameters = parametersTypes?.mapIndexed { index, type ->
        type.orExpected(index)
    } ?: expectedParameters.map { it.type.unwrap() }

    // 仓颉没有扩展函数类型，接收器只能来自显式的函数表达式类型
    val receiver = argumentAsFunctionExpression?.receiverType

    val returnType = argumentAsFunctionExpression?.returnType ?: expectedType.getReturnTypeFromFunctionType().unwrap()

    return ResolvedLambdaAtom(
        argument,
        receiver,
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

/**
 * 确保当前对象是特定类型，并返回该对象的类型
 *
 * 该函数使用reified类型参数，以允许在运行时检查对象是否为指定类型如果对象不是指定类型，
 * 则抛出异常，否则返回该对象的指定类型版本
 *
 * @throws IllegalArgumentException 如果当前对象不是指定类型
 * @return T 当前对象的指定类型版本
 */
internal inline fun <reified T : Any> Any.requireIs(): T {
    require(this is T)
    return this
}

