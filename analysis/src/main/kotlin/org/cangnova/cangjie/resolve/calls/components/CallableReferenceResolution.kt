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
import org.cangnova.cangjie.descriptors.ReceiverParameterDescriptor
import org.cangnova.cangjie.descriptors.ValueParameterDescriptor
import org.cangnova.cangjie.resolve.DescriptorUtils
import org.cangnova.cangjie.resolve.calls.components.candidate.CallableReferenceResolutionCandidate
import org.cangnova.cangjie.resolve.calls.inference.ConstraintSystemOperation
import org.cangnova.cangjie.resolve.calls.inference.model.ArgumentConstraintPositionImpl
import org.cangnova.cangjie.resolve.calls.inference.model.CallableReferenceConstraintPositionImpl
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintPosition
import org.cangnova.cangjie.resolve.calls.model.*
import org.cangnova.cangjie.resolve.calls.tower.PrioritizedCompositeScopeTowerProcessor
import org.cangnova.cangjie.resolve.calls.tower.SamePriorityCompositeScopeTowerProcessor
import org.cangnova.cangjie.resolve.calls.tower.ScopeTowerProcessor
import org.cangnova.cangjie.resolve.scopes.receivers.ReceiverValueWithSmartCastInfo
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.ComposableTypeSubstitutor
import org.cangnova.cangjie.types.ErrorUtils
import org.cangnova.cangjie.types.TypeUtils
import org.cangnova.cangjie.types.UnwrappedType
// captureFromExpression removed - not needed in invariant type system
import org.cangnova.cangjie.types.expressions.CoercionStrategy
import org.cangnova.cangjie.types.getReturnTypeFromFunctionType
import org.cangnova.cangjie.types.getValueParameterTypesFromFunctionType
import org.cangnova.cangjie.types.isFunctionType

import kotlin.Array
import kotlin.Int
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.first
import kotlin.collections.map
import kotlin.let


/**
 * 可调用引用接收者基类
 *
 * 表示可调用引用（函数引用、属性引用）的接收者类型。
 * 接收者决定了如何访问和调用被引用的可调用对象。
 *
 * 接收者类型包括：
 * - **未绑定引用（UnboundReference）**: 引用类的成员但未绑定到具体实例，如 `MyClass::method`
 * - **绑定值引用（BoundValueReference）**: 引用已绑定到特定实例的成员，如 `instance::method`
 * - **作用域接收者（ScopeReceiver）**: 来自作用域的隐式接收者
 * - **显式值接收者（ExplicitValueReceiver）**: 显式指定的接收者表达式
 *
 * @property receiver 带有智能转换信息的接收者值
 */
sealed class CallableReceiver(val receiver: ReceiverValueWithSmartCastInfo) {
    /**
     * 未绑定引用接收者
     *
     * 表示引用类的成员但未绑定到具体实例的情况。
     * 例如：`String::length` 引用了 String 类的 length 属性，但没有绑定到特定的 String 实例。
     *
     * @param receiver 接收者值（通常是类类型）
     */
    class UnboundReference(receiver: ReceiverValueWithSmartCastInfo) : CallableReceiver(receiver)

    /**
     * 绑定值引用接收者
     *
     * 表示引用已绑定到特定实例的成员。
     * 例如：`val s = "hello"; s::length` 引用了特定字符串实例 s 的 length 属性。
     *
     * @param receiver 绑定的实例接收者
     */
    class BoundValueReference(receiver: ReceiverValueWithSmartCastInfo) : CallableReceiver(receiver)

    /**
     * 作用域接收者
     *
     * 表示来自当前作用域的隐式接收者。
     * 例如：在类的方法中引用 `::method` 时，隐式使用 this 作为接收者。
     *
     * @param receiver 作用域中的接收者
     */
    class ScopeReceiver(receiver: ReceiverValueWithSmartCastInfo) : CallableReceiver(receiver)

    /**
     * 显式值接收者
     *
     * 表示显式指定的接收者表达式。
     * 例如：`obj.also { it::method }` 中的 it 是显式接收者。
     *
     * @param receiver 显式指定的接收者
     */
    class ExplicitValueReceiver(receiver: ReceiverValueWithSmartCastInfo) : CallableReceiver(receiver)
}

/**
 * 添加左侧类型约束
 *
 * 对于可调用引用表达式，将左侧类型（LHS，通常是接收者类型）与期望类型之间的约束添加到约束系统中。
 * 这用于处理带有反射类型的可调用引用，例如 KFunction、KProperty 等。
 *
 * 约束添加逻辑：
 * 1. 检查期望类型是否为编号类型（如 KFunction1、KFunction2 等）
 * 2. 提取期望类型的第一个类型参数（通常是接收者类型）
 * 3. 根据类型参数的有效变异性添加相应的约束：
 *    - 不变（INVARIANT）：添加相等性约束
 *    - 逆变（IN）：添加超类型约束（已注释）
 *    - 协变（OUT）：添加子类型约束（已注释）
 *
 * @param lhsType 左侧类型（接收者类型）
 * @param expectedType 期望的类型
 * @param position 约束位置，用于错误报告
 */
private fun ConstraintSystemOperation.addLhsTypeConstraint(
    lhsType: CangJieType,
    expectedType: UnwrappedType,
    position: ConstraintPosition
) {
    if (!ReflectionTypes.isNumberedTypeWithOneOrMoreNumber(expectedType)) return

    // 仓颉语言所有类型参数都是不变的,直接添加相等性约束
    val expectedTypeArgumentForLHS = expectedType.arguments.first()
    val expectedTypeForLHS = expectedTypeArgumentForLHS.type
    addEqualityConstraint(lhsType, expectedTypeForLHS, position)
}

/**
 * 可调用引用适配信息
 *
 * 当可调用引用需要适配以匹配期望的函数类型时，此类封装了所有必要的适配信息。
 * 适配可能包括参数类型转换、默认参数使用、协变返回类型等。
 *
 * 适配场景示例：
 * - 引用的函数有3个参数，但期望类型只需要2个参数（使用默认参数）
 * - 需要对参数或返回类型进行强制转换
 * - 挂起函数与普通函数之间的转换（已注释）
 *
 * @property argumentTypes 适配后的参数类型数组
 * @property coercionStrategy 强制转换策略（如何转换类型）
 * @property defaults 使用的默认参数数量
 * @property mappedArguments 参数映射表，将值参数描述符映射到已解析的调用参数
 */
class CallableReferenceAdaptation(
    val argumentTypes: Array<CangJieType>,
    val coercionStrategy: CoercionStrategy,
    val defaults: Int,
    val mappedArguments: Map<ValueParameterDescriptor, ResolvedCallArgument>,
//    val suspendConversionStrategy: SuspendConversionStrategy
)

/**
 * 为可调用引用解析候选添加类型约束
 *
 * 将可调用引用表达式产生的所有类型约束添加到约束系统中。
 * 这些约束用于类型推断，确保引用的可调用对象与期望类型兼容。
 *
 * 添加的约束包括：
 * 1. **左侧类型约束**：如果 LHS 是类型且有期望类型，添加接收者类型约束
 *    - 对于非静态/非伴生对象成员，将未绑定接收者类型与期望类型关联
 * 2. **调度接收者约束**：如果候选不是错误类型，添加调度接收者的类型约束
 * 3. **反射类型约束**：将引用的反射类型（如 KFunction）与期望类型建立子类型关系
 *
 * @receiver 可调用引用解析候选
 * @param constraintSystem 约束系统操作接口
 * @param substitutor 新鲜类型变量替换器，用于替换类型变量
 * @param callableReference 可调用引用解析原子
 */
fun CallableReferenceResolutionCandidate.addConstraints(
    constraintSystem: ConstraintSystemOperation,
    substitutor: ComposableTypeSubstitutor,
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

/**
 * 添加接收者约束
 *
 * 为可调用引用的接收者添加类型约束到约束系统。
 * 确保实际提供的接收者类型与被引用可调用对象期望的接收者类型兼容。
 *
 * 处理逻辑：
 * 1. 如果接收者参数或接收者参数描述符为 null，则直接返回（无需约束）
 * 2. 获取期望的接收者类型（经过类型变量替换）
 * 3. 获取实际的接收者类型（使用稳定类型，并处理捕获类型）
 * 4. 添加子类型约束：实际接收者类型 <: 期望接收者类型
 *
 * @receiver 约束系统操作接口
 * @param toFreshSubstitutor 新鲜类型变量替换器
 * @param receiverArgument 实际的接收者参数（可能为 null）
 * @param receiverParameter 期望的接收者参数描述符（可能为 null）
 * @param position 约束位置，用于错误报告
 */
private fun ConstraintSystemOperation.addReceiverConstraint(
    toFreshSubstitutor: ComposableTypeSubstitutor,
    receiverArgument: CallableReceiver?,
    receiverParameter: ReceiverParameterDescriptor?,
    position: ConstraintPosition
) {
    if (receiverArgument == null || receiverParameter == null) {
//        assert(receiverArgument == null) { "Receiver argument should be null if parameter is: $receiverArgument" }
//        assert(receiverParameter == null) { "Receiver parameter should be null if argument is: $receiverParameter" }
        return
    }

    val expectedType = toFreshSubstitutor.safeSubstitute(receiverParameter.value.type.unwrap())
    // 仓颉语言的类型系统不需要捕获类型,直接使用稳定类型即可
    val receiverType = receiverArgument.receiver.stableType

    addSubtypeConstraint(receiverType, expectedType, position)
}

/**
 * 输入输出类型
 *
 * 表示可调用引用的输入类型（参数类型）和输出类型（返回类型）。
 * 用于将期望类型分解为输入和输出部分，以便进行类型检查和推断。
 *
 * 例如，对于函数类型 `(Int, String) -> Boolean`：
 * - inputTypes = [Int, String]
 * - outputType = Boolean
 *
 * @property inputTypes 输入类型列表（函数参数类型）
 * @property outputType 输出类型（函数返回类型）
 */
data class InputOutputTypes(val inputTypes: List<UnwrappedType>, val outputType: UnwrappedType)

/**
 * 从可调用引用的期望类型中提取输入输出类型
 *
 * 分析期望类型，提取其输入类型（参数）和输出类型（返回值）。
 * 支持多种期望类型：
 * - 函数类型（如 `(Int) -> String`）
 * - 反射类型（如 `KFunction`、`KProperty` 等，已注释）
 *
 * 当前实现仅支持函数类型的提取。
 *
 * @param expectedType 期望类型（可能为 null）
 * @return 提取的输入输出类型，如果无法提取则返回 null
 */
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

/**
 * 从函数类型中提取输入输出类型
 *
 * 将函数类型分解为参数类型列表和返回类型。
 *
 * 注意：仓颉语言没有扩展函数类型（不同于 Kotlin），
 * 因此输入类型仅包含值参数类型，不包含扩展接收者类型。
 *
 * @param functionType 函数类型
 * @return 输入输出类型对象
 */
private fun extractInputOutputTypesFromFunctionType(functionType: UnwrappedType): InputOutputTypes {
    // 仓颉没有扩展函数类型，输入类型只包含参数类型
    val parameters = functionType.getValueParameterTypesFromFunctionType().map { it.type.unwrap() }
    val outputType = functionType.getReturnTypeFromFunctionType().unwrap()

    return InputOutputTypes(parameters, outputType)
}


/**
 * 创建可调用引用处理器
 *
 * 根据可调用引用的左侧（LHS）结果类型，创建相应的作用域塔处理器。
 * 作用域塔处理器负责在不同的作用域层次中查找和解析可调用引用的候选项。
 *
 * 处理器创建策略：
 *
 * 1. **空结果、错误或表达式**：
 *    - 创建基于显式接收者的处理器
 *    - 例如：`obj::method` 中的 `obj` 是显式表达式接收者
 *
 * 2. **类型结果（LHSResult.Type）**：
 *    - 处理静态成员引用和未绑定成员引用的组合
 *    - 静态处理器：处理类的静态成员或伴生对象成员
 *    - 未绑定处理器：处理类的实例成员（未绑定到具体实例）
 *    - 值处理器：处理类值接收者（如伴生对象作为值使用）
 *
 *    优先级策略：
 *    - 如果有静态处理器，使用 SamePriorityComposite（静态和未绑定同优先级）
 *    - 如果有类值接收者，使用 PrioritizedComposite（静态/未绑定优先于值）
 *
 * 示例：
 * - `MyClass::staticMethod` -> 静态处理器
 * - `MyClass::instanceMethod` -> 未绑定处理器
 * - `MyClass.Companion::method` -> 值处理器
 *
 * @param factory 可调用引用候选工厂
 * @return 作用域塔处理器，用于解析可调用引用候选
 */
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
