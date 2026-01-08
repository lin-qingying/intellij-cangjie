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

import org.cangnova.cangjie.descriptors.ClassifierDescriptorWithTypeParameters
import org.cangnova.cangjie.descriptors.TypeParameterDescriptor
import org.cangnova.cangjie.resolve.calls.inference.ConstraintSystemBuilder
import org.cangnova.cangjie.resolve.calls.inference.addSubtypeConstraintIfCompatible
import org.cangnova.cangjie.resolve.calls.inference.components.NewTypeSubstitutor
import org.cangnova.cangjie.resolve.calls.inference.model.ArgumentConstraintPositionImpl
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintPosition
import org.cangnova.cangjie.resolve.calls.inference.model.ReceiverConstraintPositionImpl
import org.cangnova.cangjie.resolve.calls.model.*
import org.cangnova.cangjie.types.*
import org.cangnova.cangjie.types.checker.SimpleClassicTypeSystemContext.isMarkedOption
import org.cangnova.cangjie.types.checker.captureFromExpression
import org.cangnova.cangjie.types.checker.hasSupertypeWithGivenTypeConstructor

/**
 * 检查简单参数
 *
 * 根据参数类型分发到对应的检查函数。这是简单参数检查的入口点，
 * 负责处理表达式参数、子调用参数等不同类型的参数。
 *
 * @param csBuilder 约束系统构建器，用于添加类型约束
 * @param argument 待检查的简单调用参数
 * @param expectedType 期望的参数类型（可能为 null）
 * @param diagnosticsHolder 诊断信息持有者，用于收集错误和警告
 * @param receiverInfo 接收者信息，标识参数是否为接收者
 * @param convertedType 转换后的类型（可能为 null）
 * @param inferenceSession 推断会话（可能为 null）
 * @param selectorCall 选择器调用（用于错误报告位置）
 * @return 已解析的原子（表示参数的解析结果）
 */
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
    // 表达式参数：直接的表达式值
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

    // 子调用参数：嵌套的函数调用结果作为参数
    is SubCangJieCallArgument ->
        checkSubCallArgument(csBuilder, argument, expectedType, diagnosticsHolder, receiverInfo, inferenceSession)

    // 未知参数类型：抛出错误
    else ->
        unexpectedArgument(argument)
}

/**
 * 检查子调用参数
 *
 * 处理子调用（嵌套函数调用）作为参数的情况。子调用的返回类型会被用作参数类型，
 * 并根据是否为安全调用（?.）添加相应的类型约束。
 *
 * 示例：
 * ```
 * function(getValue()?.result)  // getValue() 是子调用，?.result 产生可空类型
 * ```
 *
 * @param csBuilder 约束系统构建器
 * @param subCallArgument 子调用参数
 * @param expectedType 期望的参数类型
 * @param diagnosticsHolder 诊断信息持有者
 * @param receiverInfo 接收者信息
 * @param inferenceSession 推断会话
 * @return 已解析的子调用参数原子
 */

private fun checkSubCallArgument(
    csBuilder: ConstraintSystemBuilder,
    subCallArgument: SubCangJieCallArgument,
    expectedType: UnwrappedType?,
    diagnosticsHolder: CangJieDiagnosticsHolder,
    receiverInfo: ReceiverInfo,
    inferenceSession: InferenceSession?
): ResolvedAtom {
    // 创建已解析的子调用参数原子
    // 如果是接收者且推断会话要求独立解析接收者，则标记为独立解析
    val subCallResult = ResolvedSubCallArgument(
        subCallArgument, receiverInfo.isReceiver && inferenceSession?.resolveReceiverIndependently() == true
    )

    // 如果没有期望类型，直接返回结果（无需类型检查）
    if (expectedType == null) return subCallResult

    // 创建期望类型的可空版本（用于安全调用检查）
    val expectedNullableType = expectedType.makeOptionAsSpecified(true)
    // 确定约束位置：接收者或普通参数
    val position =
        if (receiverInfo.isReceiver) ReceiverConstraintPositionImpl(
            subCallArgument,
            subCallArgument.callResult.resultCallAtom.atom
        )
        else ArgumentConstraintPositionImpl(subCallArgument)

    // 子调用参数不能有稳定的智能转换
    // 返回类型可能包含固定的类型变量
    // 获取子调用的当前返回类型（应用类型替换）
    val currentReturnType =
        (csBuilder.buildCurrentSubstitutor() as NewTypeSubstitutor)
            .safeSubstitute(subCallArgument.receiver.receiverValue.type.unwrap())

    // 如果是安全调用（?.），返回类型必须是可空的
    if (subCallArgument.isSafeCall) {
        csBuilder.addSubtypeConstraint(currentReturnType, expectedNullableType, position)
        return subCallResult
    }

    // 非安全调用时的类型检查
    // 如果是接收者参数，需要检查是否需要报告不安全调用错误
    if (receiverInfo.isReceiver
        && !csBuilder.addSubtypeConstraintIfCompatible(currentReturnType, expectedType, position)
        && csBuilder.addSubtypeConstraintIfCompatible(currentReturnType, expectedNullableType, position)
    ) {
        // 返回类型与非空期望类型不兼容，但与可空期望类型兼容
        // 这意味着在可空接收者上进行了不安全调用
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

    // 添加标准的子类型约束
    csBuilder.addSubtypeConstraint(currentReturnType, expectedType, position)
    return subCallResult
}

/**
 * 检查表达式参数
 *
 * 处理直接表达式作为参数的情况。这是最常见的参数类型，包括：
 * - 变量引用
 * - 字面量
 * - 复杂表达式
 * - 智能转换后的表达式
 *
 * 此函数负责：
 * 1. 处理智能转换（stable/unstable）
 * 2. 检查可空性（nullability）
 * 3. 添加类型约束
 * 4. 报告不安全调用和可空性错误
 *
 * @param csBuilder 约束系统构建器
 * @param expressionArgument 表达式参数
 * @param expectedType 期望的参数类型
 * @param diagnosticsHolder 诊断信息持有者
 * @param isReceiver 是否为接收者参数
 * @param convertedType 转换后的类型（如果有）
 * @param selectorCall 选择器调用
 * @return 已解析的表达式原子
 */

private fun checkExpressionArgument(
    csBuilder: ConstraintSystemBuilder,
    expressionArgument: ExpressionCangJieCallArgument,
    expectedType: UnwrappedType?,
    diagnosticsHolder: CangJieDiagnosticsHolder,
    isReceiver: Boolean,
    convertedType: UnwrappedType?,
    selectorCall: CangJieCall?
): ResolvedAtom {
    // 创建已解析的表达式原子
    val resolvedExpression = ResolvedExpressionAtom(expressionArgument)
    // 如果没有期望类型，无需类型检查，直接返回
    if (expectedType == null) return resolvedExpression

    // TODO: 只为调用运行一次此近似
    // 获取参数的实际类型：使用转换后的类型或从类型参数上界捕获
    val argumentType = convertedType ?: captureFromTypeParameterUpperBoundIfNeeded(
        expressionArgument.receiver.stableType,
        expectedType
    )

    /**
     * 处理不稳定智能转换或子类型错误
     *
     * 此内部函数尝试添加类型约束，并在失败时生成相应的诊断信息。
     * 处理三种情况：
     * 1. 不稳定智能转换可以满足约束
     * 2. 可空性不匹配（可空类型传递给非空类型）
     * 3. 类型完全不兼容
     *
     * @param unstableType 不稳定智能转换后的类型
     * @param actualExpectedType 实际期望的类型
     * @param position 约束位置
     * @return 诊断信息，如果成功添加约束则返回 null
     */
    fun unstableSmartCastOrSubtypeError(
        unstableType: UnwrappedType?, actualExpectedType: UnwrappedType, position: ConstraintPosition
    ): CangJieCallDiagnostic? {

//        if (diagnosticsHolder is ResolutionCandidate) {
//            if (OperatorConventions.isConventionName(diagnosticsHolder.resolvedCall.descriptor.name)) {
////              可以为重载的运算符 并且找的了重载函数，但是参数类型不正确 报告可能需要的重载函数
//                return NoneOperatorCallDiagnostic(actualExpectedType, argumentType)
//            }
//        }

        // 尝试使用不稳定类型添加约束
        if (unstableType != null) {
            if (csBuilder.addSubtypeConstraintIfCompatible(unstableType, actualExpectedType, position)) {
                // 不稳定智能转换可以满足类型要求，但需要警告
                return UnstableSmartCast(expressionArgument, unstableType, isReceiver)
            }
        }

        // 检查可空性不匹配
        if (argumentType.isMarkedOption()) {
            // 参数类型是可空的
            if (csBuilder.addSubtypeConstraintIfCompatible(argumentType, actualExpectedType, position)) return null
            // 尝试使用非空版本的参数类型
            if (csBuilder.addSubtypeConstraintIfCompatible(
                    argumentType.makeNonOption(),
                    actualExpectedType,
                    position
                )
            ) {
                // 非空版本可以满足约束，报告可空性错误
                return ArgumentNullabilityErrorDiagnostic(actualExpectedType, argumentType, expressionArgument)
            }
        }

        // 类型完全不兼容，添加约束（将导致类型推断错误）
        csBuilder.addSubtypeConstraint(argumentType, actualExpectedType, position)


        return null
    }

    // 确定约束位置：接收者或普通参数
    val position =
        if (isReceiver) ReceiverConstraintPositionImpl(expressionArgument, selectorCall)
        else ArgumentConstraintPositionImpl(expressionArgument)

    // 用于带有 @NotNull 注解的参数
    // 如果期望非空类型参数但传递了可空值，立即报告错误
    if (expectedType is NonOptionTypeParameter && argumentType.isMarkedOption()) {
        diagnosticsHolder.addDiagnostic(
            ArgumentNullabilityErrorDiagnostic(
                expectedType,
                argumentType,
                expressionArgument
            )
        )
    }

    // 处理安全调用（?.）的情况
    if (expressionArgument.isSafeCall) {
        // 安全调用的结果总是可空的，所以期望类型也应该是可空的
        val expectedNullableType = expectedType.makeOptionAsSpecified(true)
        if (!csBuilder.addSubtypeConstraintIfCompatible(argumentType, expectedNullableType, position)) {
            // 即使安全调用，类型仍然不兼容
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

    // 非安全调用的类型检查
    if (!csBuilder.addSubtypeConstraintIfCompatible(argumentType, expectedType, position)) {
        // 参数类型与期望类型不兼容
        if (!isReceiver) {
            // 普通参数：报告智能转换或子类型错误
            diagnosticsHolder.addDiagnosticIfNotNull(
                unstableSmartCastOrSubtypeError(
                    expressionArgument.receiver.unstableType,
                    expectedType,
                    position
                )
            )

            return resolvedExpression
        }

        // 接收者参数的特殊处理
        val unstableType = expressionArgument.receiver.unstableType
        val expectedNullableType = expectedType.makeOptionAsSpecified(true)

        // 尝试使用不稳定智能转换
        if (unstableType != null && csBuilder.addSubtypeConstraintIfCompatible(unstableType, expectedType, position)) {
            // 不稳定智能转换可以满足要求
            diagnosticsHolder.addDiagnostic(UnstableSmartCast(expressionArgument, unstableType, isReceiver))
        } else if (csBuilder.addSubtypeConstraintIfCompatible(argumentType, expectedNullableType, position)) {
            // 参数类型与可空期望类型兼容，报告不安全调用错误
            diagnosticsHolder.addDiagnostic(UnsafeCallError(expressionArgument))
        } else {
            // 类型完全不兼容，添加约束（将导致类型推断错误）
            csBuilder.addSubtypeConstraint(argumentType, expectedType, position)
        }
    }

    return resolvedExpression
}

/**
 * 从类型参数上界捕获类型（如果需要）
 *
 * 当参数类型是类型参数，且期望类型包含该类型参数的某个超类型时，
 * 尝试从类型参数的上界中捕获更具体的类型信息。
 *
 * ## 示例场景
 *
 * ```kotlin
 * interface Inv<T>
 * fun <Y> bar(l: Inv<Y>): Y = ...
 *
 * fun <X : Inv<out Int>> foo(x: X) {
 *     val xr = bar(x)
 * }
 * ```
 *
 * 在这里，我们尝试从类型参数的上界捕获类型。
 * 我们将 `x` 的类型替换为 `Inv<out Int>`（选择包含期望类型构造器的超类型）并从该类型捕获。
 *
 * 这是正确的，因为它相当于以下代码：
 *
 * ```kotlin
 * fun <X : Inv<out Int>> foo(x: X) {
 *     val inv: Inv<out Int> = x
 *     val xr = bar(inv)
 * }
 * ```
 *
 * ## 实现逻辑
 *
 * 1. 检查参数类型的构造器是否为类型参数
 * 2. 在类型参数的上界中查找包含期望类型构造器的超类型
 * 3. 从选定的超类型捕获类型信息
 * 4. 如果参数类型是明确非空类型，保持这一属性
 *
 * @param argumentType 参数的实际类型
 * @param expectedType 期望的参数类型
 * @return 捕获后的类型，如果无需捕获则返回原始类型
 */
fun captureFromTypeParameterUpperBoundIfNeeded(
    argumentType: UnwrappedType,
    expectedType: UnwrappedType
): UnwrappedType {
    // 获取期望类型的上界的类型构造器
    val expectedTypeConstructor = expectedType.upperIfFlexible().constructor

    // 检查参数类型的下界构造器是否为类型参数
    if (argumentType.lowerIfFlexible().constructor.declarationDescriptor is TypeParameterDescriptor) {
        // 在类型参数的所有超类型中查找单个匹配的超类型
        val chosenSupertype = argumentType.lowerIfFlexible().supertypes().singleOrNull {
            // 超类型的描述符必须是带有类型参数的分类器
            it.constructor.declarationDescriptor is ClassifierDescriptorWithTypeParameters &&
                    // 并且该超类型包含期望类型构造器的超类型
                    it.unwrap().hasSupertypeWithGivenTypeConstructor(expectedTypeConstructor)
        }
        // 如果找到了匹配的超类型
        if (chosenSupertype != null) {
            // 从该超类型捕获类型信息
            val capturedType = captureFromExpression(chosenSupertype.unwrap())
            // 如果成功捕获且原参数类型是明确非空类型，保持非空属性
            return if (capturedType != null && argumentType.isDefinitelyNonOptionType)
                capturedType.makeDefinitelyNonOptionOrNonOption()
            else
                // 否则返回捕获的类型，如果捕获失败则返回原始类型
                capturedType ?: argumentType
        }
    }

    // 无需捕获，返回原始参数类型
    return argumentType
}
