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

import org.cangnova.cangjie.descriptors.ClassifierDescriptorWithTypeParameters
import org.cangnova.cangjie.descriptors.TypeParameterDescriptor
import org.cangnova.cangjie.resolve.calls.inference.ConstraintSystemBuilder
import org.cangnova.cangjie.resolve.calls.inference.addSubtypeConstraintIfCompatible
import org.cangnova.cangjie.resolve.calls.inference.model.ArgumentConstraintPositionImpl
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintPosition
import org.cangnova.cangjie.resolve.calls.inference.model.ReceiverConstraintPositionImpl
import org.cangnova.cangjie.resolve.calls.model.*
import org.cangnova.cangjie.types.*
import org.cangnova.cangjie.types.checker.SimpleClassicTypeSystemContext.isMarkedOption
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
    val subCallResult = ResolvedSubCallArgument(
        subCallArgument,
        receiverInfo.isReceiver && inferenceSession?.resolveReceiverIndependently() == true
    )

    if (expectedType == null) return subCallResult

    val position =
        if (receiverInfo.isReceiver) ReceiverConstraintPositionImpl(
            subCallArgument,
            subCallArgument.callResult.resultCallAtom.atom
        )
        else ArgumentConstraintPositionImpl(subCallArgument)

    val currentReturnType =
        (csBuilder.buildCurrentSubstitutor() as ComposableTypeSubstitutor)
            .safeSubstitute(subCallArgument.receiver.receiverValue.type.unwrap())

    // 直接添加子类型约束,让类型系统自己处理兼容性
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

    val argumentType = convertedType ?: captureFromTypeParameterUpperBoundIfNeeded(
        expressionArgument.receiver.stableType,
        expectedType
    )

    val position =
        if (isReceiver) ReceiverConstraintPositionImpl(expressionArgument, selectorCall)
        else ArgumentConstraintPositionImpl(expressionArgument)

    // 尝试添加子类型约束
    if (!csBuilder.addSubtypeConstraintIfCompatible(argumentType, expectedType, position)) {
        // 类型不兼容,尝试不稳定智能转换
        val unstableType = expressionArgument.receiver.unstableType
        if (unstableType != null && csBuilder.addSubtypeConstraintIfCompatible(unstableType, expectedType, position)) {
            diagnosticsHolder.addDiagnostic(UnstableSmartCast(expressionArgument, unstableType, isReceiver))
        } else {
            // 完全不兼容,添加约束让类型推断报错
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
        // 如果找到了匹配的超类型，直接返回该超类型
        if (chosenSupertype != null) {
            return chosenSupertype.unwrap()
        }
    }

    // 无需捕获，返回原始参数类型
    return argumentType
}
