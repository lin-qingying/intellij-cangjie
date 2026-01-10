/*
 * Copyright 2026 LinQingYing. and contributors.
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
import org.cangnova.cangjie.resolve.calls.inference.components.AbstractTypeSubstitutor
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

/**
 * 将带有类型变量期望类型的 Lambda 转换为已解析 Lambda
 *
 * 当 lambda 的期望类型是一个类型变量时,需要延迟处理。此扩展函数在类型变量被固定后,
 * 将延迟的 lambda 原子转换为完全解析的 lambda 原子。
 *
 * 工作流程:
 * 1. 使用当前约束系统的替换器固定期望类型
 * 2. 强制解析 lambda(forceResolution = true)
 * 3. 标记为已分析
 *
 * @receiver LambdaWithTypeVariableAsExpectedTypeAtom 延迟的 lambda 原子
 * @param csBuilder 约束系统构建器
 * @param diagnosticsHolder 诊断信息持有者
 * @param expectedType 可选的期望类型(如果为 null 则使用原子的期望类型)
 * @param returnTypeVariable lambda 返回类型的类型变量
 * @return 已解析的 lambda 原子
 */
fun LambdaWithTypeVariableAsExpectedTypeAtom.transformToResolvedLambda(
    csBuilder: ConstraintSystemBuilder,
    diagnosticsHolder: CangJieDiagnosticsHolder,
    expectedType: UnwrappedType? = null,
    returnTypeVariable: TypeVariableForLambdaReturnType? = null
): ResolvedLambdaAtom {
    // 使用当前的类型替换器固定期望类型
    val fixedExpectedType = (csBuilder.buildCurrentSubstitutor() as AbstractTypeSubstitutor)
        .safeSubstitute(expectedType ?: this.expectedType)

    // 强制解析 lambda 参数
    val resolvedLambdaAtom = preprocessLambdaArgument(
        csBuilder,
        atom,
        fixedExpectedType,
        diagnosticsHolder,
        forceResolution = true,
        returnTypeVariable = returnTypeVariable
    ) as ResolvedLambdaAtom

    // 标记为已分析
    setAnalyzed(resolvedLambdaAtom)

    return resolvedLambdaAtom
}

/**
 * 预处理 Lambda 参数
 *
 * 这是处理 lambda 参数的核心函数,负责决定是延迟处理还是立即解析。
 *
 * **延迟处理场景**:
 * 当期望类型是一个未固定的类型变量,且该类型变量可能被固定为复杂类型时(如带类型参数的函数类型),
 * 会创建 LambdaWithTypeVariableAsExpectedTypeAtom 延迟处理。
 *
 * **立即解析场景**:
 * - forceResolution = true (强制解析)
 * - 期望类型不是类型变量
 * - 期望类型是显式指定的简单函数类型(无类型参数)
 *
 * 解析过程:
 * 1. 尝试从函数类型提取 lambda 信息(接收者、参数、返回类型)
 * 2. 如果失败,则根据可用信息推断或创建类型变量
 * 3. 添加子类型约束: lambda类型 <: 期望类型
 *
 * @param csBuilder 约束系统构建器
 * @param argument lambda 调用参数
 * @param expectedType 期望类型
 * @param diagnosticsHolder 诊断信息持有者
 * @param forceResolution 是否强制解析(默认 false)
 * @param returnTypeVariable lambda 返回类型的类型变量
 * @return 已解析的原子(ResolvedLambdaAtom)或延迟的原子(LambdaWithTypeVariableAsExpectedTypeAtom)
 */
private fun preprocessLambdaArgument(
    csBuilder: ConstraintSystemBuilder,
    argument: LambdaCangJieCallArgument,
    expectedType: UnwrappedType?,
    diagnosticsHolder: CangJieDiagnosticsHolder,
    forceResolution: Boolean = false,
    returnTypeVariable: TypeVariableForLambdaReturnType? = null
): ResolvedAtom {

    if (expectedType != null && !forceResolution) {
        // 如果期望类型是类型变量,可能需要延迟 lambda 处理,直到该类型变量被固定为具体类型
        val expectedTypeVariableWithConstraints =
            csBuilder.currentStorage().notFixedTypeVariables[expectedType.constructor]

        if (expectedTypeVariableWithConstraints != null) {
            // 查找显式类型参数约束
            val explicitTypeArgument = expectedTypeVariableWithConstraints.constraints.find {
                it.kind == ConstraintKind.EQUALITY && it.position.from is ExplicitTypeParameterConstraintPosition<*>
            }?.type as? CangJieType

            // 如果没有显式类型参数,或显式类型参数有自己的类型参数(复杂类型),则延迟处理
            if (explicitTypeArgument == null || explicitTypeArgument.arguments.isNotEmpty()) {
                return LambdaWithTypeVariableAsExpectedTypeAtom(argument, expectedType)
            }
        }
    }

    // 立即解析 lambda
    // 1. 尝试从函数类型提取信息
    // 2. 如果失败,推断信息
    val resolvedArgument = extractLambdaInfoFromFunctionalType(expectedType, argument, returnTypeVariable)
        ?: extraLambdaInfo(expectedType, argument, csBuilder, diagnosticsHolder)

    // 添加子类型约束: lambda类型 <: 期望类型
    if (expectedType != null) {
        val lambdaType = createFunctionType(
            csBuilder.builtIns, Annotations.EMPTY, resolvedArgument.receiver ,
            resolvedArgument.parameters, null, resolvedArgument.returnType
        )
        csBuilder.addSubtypeConstraint(lambdaType, expectedType, ArgumentConstraintPositionImpl(argument))
    }

    return resolvedArgument
}

/**
 * 从函数类型中提取 Lambda 信息
 *
 * 当期望类型是一个内建函数类型时,从该类型中提取 lambda 所需的信息。
 * 这是最理想的情况,因为可以直接获取完整的类型信息。
 *
 * 提取逻辑:
 * 1. **参数类型**: 优先使用 lambda 定义中的参数类型,否则从期望类型中提取
 * 2. **接收者类型**: 仓颉没有扩展函数类型,只能从 FunctionExpression 中获取
 * 3. **返回类型**: 优先使用 FunctionExpression 的返回类型,否则从期望类型中提取
 *
 * @param expectedType 期望类型
 * @param argument lambda 调用参数
 * @param returnTypeVariable lambda 返回类型的类型变量
 * @return 如果成功提取则返回 ResolvedLambdaAtom,否则返回 null
 */
private fun extractLambdaInfoFromFunctionalType(
    expectedType: UnwrappedType?,
    argument: LambdaCangJieCallArgument,
    returnTypeVariable: TypeVariableForLambdaReturnType? = null
): ResolvedLambdaAtom? {
    // 期望类型必须是内建函数类型
    if (expectedType == null || !expectedType.isBuiltinFunctionalType) return null

    val parametersTypes = argument.parametersTypes
    val expectedParameters = expectedType.getValueParameterTypesFromFunctionType()
    val argumentAsFunctionExpression = argument as? FunctionExpression

    // 辅助函数: 如果类型为 null,则使用期望类型中对应位置的类型,或 Any
    fun UnwrappedType?.orExpected(index: Int) =
        this ?: expectedParameters.getOrNull(index)?.type?.unwrap() ?: expectedType.builtIns.stdlibTypes.anyType

    // 仓颉没有扩展函数类型,简化参数提取逻辑
    // 从实际 lambda 定义或期望的函数类型中提取参数类型
    val parameters = parametersTypes?.mapIndexed { index, type ->
        type.orExpected(index)
    } ?: expectedParameters.map { it.type.unwrap() }

    // 仓颉没有扩展函数类型,接收器只能来自显式的函数表达式类型
    val receiver = argumentAsFunctionExpression?.receiverType

    // 返回类型: 优先使用函数表达式的类型,否则使用期望类型的返回类型
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

/**
 * 推断额外的 Lambda 信息
 *
 * 当无法从期望类型中提取完整信息时(如期望类型不是函数类型),根据已有信息推断 lambda 的类型。
 *
 * 推断策略:
 * 1. **返回类型**:
 *    - 优先使用 FunctionExpression 的显式返回类型
 *    - 如果期望类型是 Function 超类型,使用其类型参数
 *    - 否则创建类型变量 `_L` 用于推导
 * 2. **参数类型**:
 *    - 使用 lambda 定义中的参数类型
 *    - 如果参数类型未指定,报告诊断错误 NotEnoughInformationForLambdaParameter
 * 3. **接收者类型**: 只能来自 FunctionExpression
 *
 * @param expectedType 期望类型(可能不是函数类型)
 * @param argument lambda 调用参数
 * @param csBuilder 约束系统构建器
 * @param diagnosticsHolder 诊断信息持有者
 * @return 已解析的 lambda 原子
 */
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

    // 为 lambda 返回类型创建类型变量
    val typeVariable = TypeVariableForLambdaReturnType(builtIns, "_L")

    val receiverType = argumentAsFunctionExpression?.receiverType
    val returnType =
        argumentAsFunctionExpression?.returnType ?: expectedType?.arguments?.singleOrNull()?.type?.unwrap()
            ?.takeIf { isFunctionSupertype }
        ?: typeVariable.defaultType

    // 处理参数类型,对未指定类型的参数报告错误
    val parameters = argument.parametersTypes?.mapIndexed { index, parameterType ->
        if (parameterType != null) {
            parameterType
        } else {
            diagnosticsHolder.addDiagnostic(NotEnoughInformationForLambdaParameter(argument, index))
            ErrorUtils.createErrorType(ErrorTypeKind.UNINFERRED_LAMBDA_PARAMETER_TYPE)
        }
    } ?: emptyList()

    // 如果使用了类型变量,注册到约束系统
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

/**
 * 扩展属性: 从约束系统构建器获取内建类型
 *
 * 通过类型系统上下文访问仓颉语言的内建类型(如 Int, String, Any 等)。
 */
internal val ConstraintSystemBuilder.builtIns: CangJieBuiltIns get() = ((this as ConstraintSystemImpl).typeSystemContext as BuiltInsProvider).builtIns

/**
 * 解析仓颉原始调用参数
 *
 * 这是参数解析的入口函数,根据参数类型分发到不同的处理函数:
 * - SimpleCangJieCallArgument: 简单参数(如变量、常量、表达式)
 * - LambdaCangJieCallArgument: Lambda 表达式或函数表达式
 * - CallableReferenceCangJieCallArgument: 可调用引用(如 ::function)
 * - CollectionLiteralCangJieCallArgument: 集合字面量(如数组字面量)
 *
 * @param csBuilder 约束系统构建器
 * @param argument 调用参数
 * @param expectedType 期望类型
 * @param diagnosticsHolder 诊断信息持有者
 * @param receiverInfo 接收者信息
 * @param convertedType 转换后的类型(用于类型转换场景)
 * @param inferenceSession 推导会话
 * @param selectorCall 选择器调用(如果是链式调用的一部分)
 * @return 已解析的原子
 */
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

/**
 * 预处理可调用引用参数
 *
 * 处理可调用引用(如 `::function`, `ClassName::method`)作为参数的情况。
 *
 * 检查项:
 * - 如果期望类型不是可调用类型(不是函数类型或 KFunction),报告 NotCallableExpectedType 诊断
 *
 * 可调用引用的实际解析在后续的 resolveCallableReferenceArgument 中完成,
 * 这里只是创建 EagerCallableReferenceAtom 占位。
 *
 * @param csBuilder 约束系统构建器
 * @param argument 可调用引用参数
 * @param expectedType 期望类型
 * @param diagnosticsHolder 诊断信息持有者
 * @return 急切的可调用引用原子
 */
private fun preprocessCallableReference(
    csBuilder: ConstraintSystemBuilder,
    argument: CallableReferenceCangJieCallArgument,
    expectedType: UnwrappedType?,
    diagnosticsHolder: CangJieDiagnosticsHolder
): ResolvedAtom {
    val result = EagerCallableReferenceAtom(argument, expectedType)

    if (expectedType == null) return result

    // 检查期望类型是否为可调用类型
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

/**
 * 预处理集合字面量参数
 *
 * 处理集合字面量(如数组字面量)作为参数的情况。
 * 目前仅创建 ResolvedCollectionLiteralAtom,未进行额外检查。
 *
 * @param collectionLiteralArgument 集合字面量参数
 * @param expectedType 期望类型
 * @return 已解析的集合字面量原子
 */
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

