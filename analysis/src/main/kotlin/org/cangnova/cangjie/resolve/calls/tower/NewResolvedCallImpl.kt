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

package org.cangnova.cangjie.resolve.calls.tower

import org.cangnova.cangjie.config.LanguageVersionSettings
import org.cangnova.cangjie.descriptors.CallableDescriptor
import org.cangnova.cangjie.descriptors.ClassDescriptor
import org.cangnova.cangjie.descriptors.TypeParameterDescriptor
import org.cangnova.cangjie.descriptors.ValueParameterDescriptor
import org.cangnova.cangjie.psi.CjExpression
import org.cangnova.cangjie.psi.ValueArgument
import org.cangnova.cangjie.resolve.calls.inference.components.FreshVariableNewTypeSubstitutor
import org.cangnova.cangjie.resolve.calls.inference.components.NewTypeSubstitutor
import org.cangnova.cangjie.resolve.calls.inference.model.*

import org.cangnova.cangjie.resolve.calls.model.*
import org.cangnova.cangjie.resolve.calls.results.ResolutionStatus
import org.cangnova.cangjie.resolve.calls.tasks.ExplicitReceiverKind
import org.cangnova.cangjie.resolve.calls.util.toResolutionStatus
import org.cangnova.cangjie.resolve.constants.IntegerValueTypeConstant
import org.cangnova.cangjie.resolve.scopes.receivers.ImplicitClassReceiver
import org.cangnova.cangjie.resolve.scopes.receivers.ReceiverValue
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.TypeApproximator
import org.cangnova.cangjie.types.TypeApproximatorConfiguration
import org.cangnova.cangjie.types.UnwrappedType

/**
 * 新的已解析调用实现
 *
 * 这个类实现了已解析的可调用对象调用，包含了调用的所有信息，如类型参数、接收者、参数映射等。
 * 它将父类的一些抽象方法实现为属性（利用Kotlin特性，属性可以override方法的getter）。
 *
 * @param D 可调用描述符类型
 * @param resolvedCallAtom 已解析的调用原子，包含调用的基本信息
 * @param substitutor 类型替换器，用于类型推断后的类型替换
 * @param diagnostics 诊断信息集合，包含调用过程中的错误和警告
 * @param typeApproximator 类型近似器，用于类型近似处理
 * @param languageVersionSettings 语言版本设置
 */
class NewResolvedCallImpl<D : CallableDescriptor>(
    override val resolvedCallAtom: ResolvedCallAtom,
    substitutor: NewTypeSubstitutor?,
    diagnostics: Collection<CangJieCallDiagnostic>,
    override val typeApproximator: TypeApproximator,
    override val languageVersionSettings: LanguageVersionSettings,
) : NewAbstractResolvedCall<D>() {

    // ========== 接收者相关 ==========

    /** 调度接收者(dispatch receiver)，即调用的对象实例 */
    private var _dispatchReceiver = resolvedCallAtom.dispatchReceiverArgument?.receiver?.receiverValue


    /** 智能转换后的调度接收者类型 */
    override var smartCastDispatchReceiverType: CangJieType? = null

    /** 上下文接收者列表 */
    private var _contextReceivers = resolvedCallAtom.contextReceiversArguments.map { it.receiver.receiverValue }

    // ========== 描述符相关 ==========

    /** 结果描述符，经过类型替换后的最终描述符 */
    private lateinit var _resultingDescriptor: D

    /** 类型参数列表 */
    private lateinit var _typeArguments: List<UnwrappedType>

    /**
     * 候选描述符
     * 这是一个计算属性，将父类的抽象方法实现为属性形式
     */
    @Suppress("UNCHECKED_CAST")
    override val candidateDescriptor: D
        get() = resolvedCallAtom.candidateDescriptor as D

    // ========== 诊断信息 ==========

    /** 诊断信息集合，包含类型错误、参数不匹配等信息 */
    override var diagnostics: Collection<CangJieCallDiagnostic> = diagnostics
        private set

    // ========== 类型转换相关的映射 ==========

    /** Unit类型转换的参数映射: 参数 -> 预期的Unit类型 */
    private var expectedTypeForUnitConvertedArgumentMap: Map<ValueArgument, UnwrappedType>? = null

    /** Suspend类型转换的参数映射: 参数 -> 预期的Suspend类型 */
    private var expectedTypeForSuspendConvertedArgumentMap: Map<ValueArgument, UnwrappedType>? = null

    /** SAM类型转换的参数映射: 参数 -> 预期的SAM类型 */
    private var expectedTypeForSamConvertedArgumentMap: Map<ValueArgument, UnwrappedType>? = null

    /** 常量类型转换的参数映射: 表达式 -> 整数字面量类型常量 */
    private var argumentTypeForConstantConvertedMap: Map<CjExpression, IntegerValueTypeConstant>? = null

    // ========== 从父类实现的属性(override方法为属性) ==========

    override val psiCangJieCall: PSICangJieCall = resolvedCallAtom.atom.psiCangJieCall
    override val cangjieCall: CangJieCall = resolvedCallAtom.atom
    override val freshSubstitutor: FreshVariableNewTypeSubstitutor
        get() = resolvedCallAtom.freshVariablesSubstitutor
    override val argumentMappingByOriginal: Map<ValueParameterDescriptor, ResolvedCallArgument>
        get() = resolvedCallAtom.argumentMappingByOriginal

    // ========== 公共方法 ==========





    /**
     * 更新调度接收者类型
     * 如果新类型与当前类型相同则不做任何操作
     */
    override fun updateDispatchReceiverType(newType: CangJieType) {
        if (_dispatchReceiver?.type == newType) return
        _dispatchReceiver = _dispatchReceiver?.replaceType(newType)
    }

    /**
     * 更新诊断信息
     * 用完成的诊断信息替换当前的诊断信息集合
     */
    fun updateDiagnostics(completedDiagnostics: Collection<CangJieCallDiagnostic>) {
        diagnostics = completedDiagnostics
    }

    // ========== 从父类/接口实现的属性(override属性) ==========

    /**
     * 显式接收者类型
     * 返回调用中显式指定的接收者类型（DISPATCH_RECEIVER、EXTENSION_RECEIVER等）
     */
    override val explicitReceiverKind: ExplicitReceiverKind
        get() = resolvedCallAtom.explicitReceiverKind



    /**
     * 调度接收者
     * 返回方法调用中的对象实例
     */
    override val dispatchReceiver: ReceiverValue?
        get() = _dispatchReceiver



    /**
     * 解析状态
     * 根据诊断信息确定调用的解析状态(成功、类型不匹配、参数不匹配等)
     */
    override val status: ResolutionStatus
        get() = getResultApplicability(diagnostics).toResolutionStatus()

    /**
     * 结果描述符
     * 返回经过类型替换和近似处理后的最终可调用描述符
     */
    override val resultingDescriptor: D
        get() = _resultingDescriptor

    /**
     * 类型参数映射
     * 返回类型参数描述符到实际类型的映射
     */
    override val typeArguments: Map<TypeParameterDescriptor, CangJieType>
        get() {
            val typeParameters = candidateDescriptor.typeParameters.takeIf { it.isNotEmpty() } ?: return emptyMap()
            return typeParameters.zip(_typeArguments).toMap()
        }

    // ========== 类型转换相关的getter方法 ==========

    /**
     * 获取Unit类型转换参数的预期类型
     * @param valueArgument 值参数
     * @return 预期的Unit类型，如果不存在则返回null
     */
    fun getExpectedTypeForUnitConvertedArgument(valueArgument: ValueArgument): UnwrappedType? =
        expectedTypeForUnitConvertedArgumentMap?.get(valueArgument)

    /**
     * 获取Suspend类型转换参数的预期类型
     * @param valueArgument 值参数
     * @return 预期的Suspend类型，如果不存在则返回null
     */
    fun getExpectedTypeForSuspendConvertedArgument(valueArgument: ValueArgument): UnwrappedType? =
        expectedTypeForSuspendConvertedArgumentMap?.get(valueArgument)

    /**
     * 获取SAM类型转换参数的预期类型
     * @param valueArgument 值参数
     * @return 预期的SAM类型，如果不存在则返回null
     */
    fun getExpectedTypeForSamConvertedArgument(valueArgument: ValueArgument): UnwrappedType? =
        expectedTypeForSamConvertedArgumentMap?.get(valueArgument)

    /**
     * 获取常量转换参数的参数类型
     * @param valueArgument 值参数
     * @return 整数字面量类型常量，如果不存在则返回null
     */
    fun getArgumentTypeForConstantConvertedArgument(valueArgument: ValueArgument): IntegerValueTypeConstant? {
        val expression = valueArgument.getArgumentExpression() ?: return null
        return argumentTypeForConstantConvertedMap?.get(expression)
    }

    // ========== 内部辅助方法 ==========

    /**
     * 计算转换参数的预期类型映射
     * 通用方法，用于计算各种类型转换（Unit、Suspend、SAM）的预期类型
     *
     * @param arguments 参数到转换后类型的映射
     * @param substitutor 类型替换器
     * @return 值参数到预期类型的映射，如果参数为空则返回null
     */
    private fun calculateExpectedTypeForConvertedArguments(
        arguments: Map<CangJieCallArgument, UnwrappedType>,
        substitutor: NewTypeSubstitutor?,
    ): Map<ValueArgument, UnwrappedType>? {
        if (arguments.isEmpty()) return null

        val expectedTypeForConvertedArguments = hashMapOf<ValueArgument, UnwrappedType>()
        for ((argument, convertedType) in arguments) {
            val typeWithFreshVariables = resolvedCallAtom.freshVariablesSubstitutor.safeSubstitute(convertedType)
            val expectedType = substitutor?.safeSubstitute(typeWithFreshVariables) ?: typeWithFreshVariables
            expectedTypeForConvertedArguments[argument.psiCallArgument.valueArgument] = expectedType
        }

        return expectedTypeForConvertedArguments
    }

    /**
     * 计算Unit类型转换参数的预期类型映射
     */
    private fun calculateExpectedTypeForUnitConvertedArgumentMap(substitutor: NewTypeSubstitutor?) {
        expectedTypeForUnitConvertedArgumentMap = calculateExpectedTypeForConvertedArguments(
            resolvedCallAtom.argumentsWithUnitConversion, substitutor
        )
    }

    /**
     * 计算SAM类型转换参数的预期类型映射
     */
    private fun calculateExpectedTypeForSamConvertedArgumentMap(substitutor: NewTypeSubstitutor?) {
        expectedTypeForSamConvertedArgumentMap = calculateExpectedTypeForConvertedArguments(
            resolvedCallAtom.argumentsWithConversion.mapValues { it.value.convertedTypeByCandidateParameter },
            substitutor
        )
    }

    /**
     * 计算Suspend类型转换参数的预期类型映射
     */
    private fun calculateExpectedTypeForSuspendConvertedArgumentMap(substitutor: NewTypeSubstitutor?) {
        expectedTypeForSuspendConvertedArgumentMap = calculateExpectedTypeForConvertedArguments(
            resolvedCallAtom.argumentsWithSuspendConversion, substitutor
        )
    }

    /**
     * 计算常量类型转换参数的类型映射
     * 将整数字面量参数映射到其对应的类型常量
     */
    private fun calculateExpectedTypeForConstantConvertedArgumentMap() {
        if (resolvedCallAtom.argumentsWithConstantConversion.isEmpty()) return

        val expectedTypeForConvertedArguments = hashMapOf<CjExpression, IntegerValueTypeConstant>()

        for ((argument, convertedConstant) in resolvedCallAtom.argumentsWithConstantConversion) {
            val expression = argument.psiExpression ?: continue
            expectedTypeForConvertedArguments[expression] = convertedConstant
        }

        argumentTypeForConstantConvertedMap = expectedTypeForConvertedArguments
    }

    /**
     * 收集错误位置信息
     * 遍历诊断信息，收集参数位置的错误信息
     *
     * @return 值参数到诊断信息列表的映射
     */
    private fun collectErrorPositions(): Map<ValueArgument, List<CangJieCallDiagnostic>> {
        val result = mutableListOf<Pair<ValueArgument, CangJieCallDiagnostic>>()

        fun ConstraintPosition.originalPosition(): ConstraintPosition =
            if (this is IncorporationConstraintPosition) {
                from.originalPosition()
            } else {
                this
            }

        diagnostics.forEach {
            val position = when (val error = it.constraintSystemError) {
                is NewConstraintError -> error.position.originalPosition()
                else -> null
            } as? ArgumentConstraintPositionImpl ?: return@forEach

            val argument = (position.argument as? PSICangJieCallArgument)?.valueArgument ?: return@forEach
            result += argument to it
        }

        return result.groupBy({ it.first }) { it.second }
    }

    /**
     * 构建参数到参数描述符的映射
     * 根据诊断信息确定每个参数的匹配状态
     *
     * @param resultingDescriptor 结果可调用描述符
     * @param valueArguments 值参数映射
     * @return 值参数到参数匹配结果的映射
     */
    override fun argumentToParameterMap(
        resultingDescriptor: CallableDescriptor,
        valueArguments: Map<ValueParameterDescriptor, ResolvedValueArgument>
    ): Map<ValueArgument, ArgumentMatchImpl> {
        val argumentErrors = collectErrorPositions()

        return LinkedHashMap<ValueArgument, ArgumentMatchImpl>().also { result ->
            for (parameter in resultingDescriptor.valueParameters) {
                val resolvedArgument = valueArguments[parameter] ?: continue
                for (argument in resolvedArgument.arguments) {
                    val status = argumentErrors[argument]?.let {
                        ArgumentMatchStatus.TYPE_MISMATCH
                    } ?: ArgumentMatchStatus.SUCCESS
                    result[argument] = ArgumentMatchImpl(parameter).apply { recordMatchStatus(status) }
                }
            }
        }
    }

    /**
     * 设置结果替换器
     * 应用类型替换器，更新所有相关的类型信息
     *
     * 此方法会：
     * 1. 清除缓存的值
     * 2. 替换接收者类型
     * 3. 生成结果描述符
     * 4. 计算类型参数
     * 5. 计算各种类型转换的预期类型映射
     *
     * @param substitutor 新的类型替换器，如果为null则使用空替换器
     */
    override fun setResultingSubstitutor(substitutor: NewTypeSubstitutor?) {
        // 清除缓存的值
        updateArgumentsMapping(null)
        updateValueArguments(emptyMap())

        // 替换接收者类型
        substituteReceivers(substitutor)

        // 生成结果描述符
        @Suppress("UNCHECKED_CAST")
        _resultingDescriptor = substitutedResultingDescriptor(substitutor) as D

        // 计算类型参数
        _typeArguments = freshSubstitutor.freshVariables.map {
            val substituted = (substitutor ?: FreshVariableNewTypeSubstitutor.Empty).safeSubstitute(it.defaultType)
            typeApproximator
                .approximateToSuperType(substituted, TypeApproximatorConfiguration.IntegerLiteralsTypesApproximation)
                ?: substituted
        }

        // 计算各种类型转换的预期类型映射
        calculateExpectedTypeForSamConvertedArgumentMap(substitutor)
        calculateExpectedTypeForSuspendConvertedArgumentMap(substitutor)
        calculateExpectedTypeForUnitConvertedArgumentMap(substitutor)
        calculateExpectedTypeForConstantConvertedArgumentMap()
    }

    // ========== 初始化块 ==========

    /**
     * 初始化时应用传入的替换器
     */
    init {
        setResultingSubstitutor(substitutor)
    }
}

/**
 * 转换后的隐式类接收者
 *
 * 表示经过智能转换后的隐式类接收者，包含原始描述符和目标类型
 *
 * @param originalDescriptor 原始类描述符
 * @param targetType 智能转换后的目标类型
 */
class CastImplicitClassReceiver(originalDescriptor: ClassDescriptor, val targetType: CangJieType) :
    ImplicitClassReceiver(originalDescriptor)
