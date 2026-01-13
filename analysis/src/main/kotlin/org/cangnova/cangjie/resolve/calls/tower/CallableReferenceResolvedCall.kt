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

package org.cangnova.cangjie.resolve.calls.tower

import org.cangnova.cangjie.config.LanguageVersionSettings
import org.cangnova.cangjie.descriptors.CallableDescriptor
import org.cangnova.cangjie.descriptors.TypeParameterDescriptor
import org.cangnova.cangjie.descriptors.ValueParameterDescriptor
import org.cangnova.cangjie.psi.ValueArgument
import org.cangnova.cangjie.resolve.calls.inference.model.ResolvedValueArgument
import org.cangnova.cangjie.resolve.calls.model.*
import org.cangnova.cangjie.resolve.calls.smartcasts.DataFlowInfo
import org.cangnova.cangjie.resolve.calls.tasks.ExplicitReceiverKind
import org.cangnova.cangjie.resolve.calls.util.toResolutionStatus
import org.cangnova.cangjie.resolve.scopes.receivers.ReceiverValue
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.ComposableTypeSubstitutor
import org.cangnova.cangjie.types.TypeApproximator
import org.cangnova.cangjie.types.TypeApproximatorConfiguration
import org.cangnova.cangjie.types.UnwrappedType

/**
 * 可调用引用的已解析调用
 *
 * 处理可调用引用（如函数引用、属性引用）的解析调用实现。
 * 可调用引用是指 `::functionName` 或 `::propertyName` 这样的语法。
 *
 * @param D 可调用描述符类型
 * @param resolvedAtom 已解析的可调用引用原子
 * @param typeApproximator 类型近似器
 * @param languageVersionSettings 语言版本设置
 * @param substitutor 类型替换器
 */
class CallableReferenceResolvedCall<D : CallableDescriptor>(
    val resolvedAtom: ResolvedCallableReferenceAtom,
    override val typeApproximator: TypeApproximator,
    override val languageVersionSettings: LanguageVersionSettings,
    substitutor: ComposableTypeSubstitutor? = null,
) : AbstractResolvedCall<D>() {

    override val positionDependentApproximation: Boolean = true
    override val argumentMappingByOriginal: Map<ValueParameterDescriptor, ResolvedCallArgument> = emptyMap()
    override val diagnostics: Collection<CangJieCallDiagnostic> = emptyList()

    // ========== 接收者相关 ==========



    /** 调度接收者（私有字段，带下划线前缀避免与属性冲突） */
    private var _dispatchReceiver = when (resolvedAtom) {
        is ResolvedCallableReferenceCallAtom -> resolvedAtom.dispatchReceiverArgument?.receiverValue
        is ResolvedCallableReferenceArgumentAtom -> resolvedAtom.candidate?.dispatchReceiver?.receiver?.receiverValue
    }

    // ========== 描述符相关 ==========

    /** 结果描述符（私有字段） */
    private lateinit var _resultingDescriptor: D

    /** 类型参数列表（私有字段） */
    private lateinit var _typeArguments: List<UnwrappedType>

    // ========== 从父类实现的属性 ==========

    override val resolvedCallAtom: ResolvedCallableReferenceCallAtom?
        get() = when (resolvedAtom) {
            is ResolvedCallableReferenceCallAtom -> resolvedAtom
            is ResolvedCallableReferenceArgumentAtom -> resolvedAtom.candidate?.resolvedCall
        }

    override val psiCangJieCall: PSICangJieCall =
        when (resolvedAtom) {
            is ResolvedCallableReferenceCallAtom -> resolvedAtom.atom.psiCangJieCall
            is ResolvedCallableReferenceArgumentAtom -> resolvedAtom.atom.call.psiCangJieCall
        }

    override val freshSubstitutor: ComposableTypeSubstitutor?
        get() = when (resolvedAtom) {
            is ResolvedCallableReferenceCallAtom -> resolvedAtom.freshVariablesSubstitutor
            is ResolvedCallableReferenceArgumentAtom -> resolvedAtom.candidate?.freshVariablesSubstitutor
        }

    override val cangjieCall: CangJieCall?
        get() = when (resolvedAtom) {
            is ResolvedCallableReferenceArgumentAtom -> resolvedAtom.candidate?.cangjieCall?.call
            is ResolvedCallableReferenceCallAtom -> resolvedAtom.atom
        }

    // ========== 从接口实现的属性 ==========

    /**
     * 调度接收者
     * 返回可调用引用的调度接收者
     */
    override val dispatchReceiver: ReceiverValue?
        get() = _dispatchReceiver

    /**
     * 候选描述符
     * 返回可调用引用的候选描述符
     */
    @Suppress("UNCHECKED_CAST")
    override val candidateDescriptor: D
        get() = when (resolvedAtom) {
            is ResolvedCallableReferenceCallAtom -> resolvedAtom.candidateDescriptor as D
            is ResolvedCallableReferenceArgumentAtom -> resolvedAtom.candidate?.candidate as D
        }

    /**
     * 智能转换调度接收者类型
     * 可调用引用不支持智能转换，总是返回 null
     */
    override var smartCastDispatchReceiverType: CangJieType? = null

    /**
     * 显式接收者类型
     * 返回可调用引用的显式接收者类型
     */
    override val explicitReceiverKind: ExplicitReceiverKind
        get() = when (resolvedAtom) {
            is ResolvedCallableReferenceArgumentAtom ->
                resolvedAtom.candidate?.explicitReceiverKind ?: ExplicitReceiverKind.NO_EXPLICIT_RECEIVER
            is ResolvedCallableReferenceCallAtom -> resolvedAtom.explicitReceiverKind
        }


    /**
     * 结果描述符
     * 返回经过类型替换的最终描述符
     */
    override val resultingDescriptor: D
        get() = _resultingDescriptor

    /**
     * 解析状态
     * 可调用引用总是已解析状态
     */
    override val status
        get() = CandidateApplicability.RESOLVED.toResolutionStatus()


    /**
     * 参数的数据流信息
     * 可调用引用没有参数，返回空数据流信息
     */
    override val dataFlowInfoForArguments: DataFlowInfoForArguments
        get() = MutableDataFlowInfoForArguments.WithoutArgumentsCheck(DataFlowInfo.EMPTY)

    /**
     * 类型参数映射
     * 返回类型参数到实际类型的映射
     */
    override val typeArguments: Map<TypeParameterDescriptor, CangJieType>
        get() {
            val typeParameters = candidateDescriptor.typeParameters.takeIf { it.isNotEmpty() } ?: return emptyMap()
            return typeParameters.zip(_typeArguments).toMap()
        }

    // ========== 方法实现 ==========

    /**
     * 获取参数映射
     * 可调用引用没有值参数，总是返回 ArgumentUnmapped
     */
    override fun getArgumentMapping(valueArgument: ValueArgument): ArgumentMapping = ArgumentUnmapped



    /**
     * 更新调度接收者类型
     */
    override fun updateDispatchReceiverType(newType: CangJieType) {
        if (_dispatchReceiver?.type == newType) return
        _dispatchReceiver = _dispatchReceiver?.replaceType(newType)
    }

    /**
     * 设置结果替换器
     * 应用类型替换器并计算类型参数
     */
    override fun setResultingSubstitutor(substitutor: ComposableTypeSubstitutor?) {
        substituteReceivers(substitutor)

        @Suppress("UNCHECKED_CAST")
        _resultingDescriptor = substitutedResultingDescriptor(substitutor) as D

        freshSubstitutor?.let { freshSubstitutor ->
            // 从 resolvedAtom 中获取 freshVariables
            val freshVariables = when (resolvedAtom) {
                is ResolvedCallableReferenceCallAtom -> resolvedAtom.freshVariables
                is ResolvedCallableReferenceArgumentAtom -> resolvedAtom.candidate?.freshVariables
            }
            _typeArguments = freshVariables?.map {
                val substituted = (substitutor ?: ComposableTypeSubstitutor.EMPTY).safeSubstitute(it.defaultType)
                typeApproximator.approximateToSuperType(
                    substituted,
                    TypeApproximatorConfiguration.IntegerLiteralsTypesApproximation
                )
                    ?: substituted
            } ?: emptyList()
        }
    }

    /**
     * 构建参数到参数描述符的映射
     * 可调用引用没有参数，返回空映射
     */
    override fun argumentToParameterMap(
        resultingDescriptor: CallableDescriptor,
        valueArguments: Map<ValueParameterDescriptor, ResolvedValueArgument>
    ): Map<ValueArgument, ArgumentMatchImpl> = emptyMap()

    // ========== 初始化块 ==========

    init {
        setResultingSubstitutor(substitutor)
    }
}
