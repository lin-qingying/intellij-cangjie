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

package org.cangnova.cangjie.resolve.calls.model

import com.intellij.openapi.diagnostic.Logger
import org.cangnova.cangjie.descriptors.CallableDescriptor
import org.cangnova.cangjie.descriptors.TypeParameterDescriptor
import org.cangnova.cangjie.descriptors.ValueParameterDescriptor
import org.cangnova.cangjie.psi.Call
import org.cangnova.cangjie.psi.ValueArgument
import org.cangnova.cangjie.resolve.binding.DelegatingBindingTrace
import org.cangnova.cangjie.resolve.calls.inference.ConstraintSystem
import org.cangnova.cangjie.resolve.calls.inference.model.ResolvedValueArgument
import org.cangnova.cangjie.resolve.calls.results.ResolutionStatus
import org.cangnova.cangjie.resolve.calls.results.ResolutionStatus.*
import org.cangnova.cangjie.resolve.calls.tasks.ExplicitReceiverKind
import org.cangnova.cangjie.resolve.calls.tasks.OldResolutionCandidate
import org.cangnova.cangjie.resolve.calls.tasks.TracingStrategy
import org.cangnova.cangjie.resolve.calls.util.hasInferredReturnType
import org.cangnova.cangjie.resolve.scopes.receivers.*
import org.cangnova.cangjie.resolve.shouldBeSubstituteWithStubTypes
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.ComposableTypeSubstitutor
import java.util.*

/**
 * 已解析调用的实现类
 *
 * 表示一个已解析的函数或属性调用，包含调用的所有信息，如候选描述符、接收者、类型参数、值参数等。
 * 这个类是可变的（MutableResolvedCall），在解析过程中会逐步填充信息。
 *
 * @param D 可调用描述符类型
 * @property call 调用对象，包含调用的PSI信息
 * @property candidateDescriptor 候选描述符，表示被调用的函数或属性
 * @property dispatchReceiver 调度接收者（方法调用的对象实例）
 * @property explicitReceiverKind 显式接收者类型
 * @property knownTypeParametersSubstitutor 已知类型参数的替换器
 * @property trace 委托绑定追踪
 * @property tracing 追踪策略
 * @property dataFlowInfoForArguments 参数的数据流信息
 */
class MutableResolvedCallImpl<D : CallableDescriptor> : MutableResolvedCall<D> {

    // ========== 基本属性 ==========

    override val call: Call
    override val candidateDescriptor: D
    override val explicitReceiverKind: ExplicitReceiverKind
    override val knownTypeParametersSubstitutor: ComposableTypeSubstitutor?

    // ========== 可变属性 ==========

    /** 值参数映射：参数描述符 -> 已解析的值参数 */
    private val _valueArguments: MutableMap<ValueParameterDescriptor, ResolvedValueArgument>

    /** 参数到参数描述符的映射 */
    private val _argumentToParameterMap: MutableMap<ValueArgument, ArgumentMatchImpl>

    /** 类型参数映射 */
    private var _typeArguments: MutableMap<TypeParameterDescriptor, CangJieType>

    /** 结果描述符（可能被替换） */
    private var _resultingDescriptor: D? = null

    /** 调度接收者 */
    private var _dispatchReceiver: ReceiverValue?


    /** 委托绑定追踪 */
    private var _trace: DelegatingBindingTrace?

    /** 追踪策略 */
    private var _tracing: TracingStrategy?

    /** 解析状态 */
    private var _status: ResolutionStatus = UNKNOWN_STATUS

    /** 约束系统 */
    override var constraintSystem: ConstraintSystem? = null

    /** 是否已推断返回类型 */
    private var _hasInferredReturnType: Boolean? = null

    /** 是否已完成 */
    private var _completed: Boolean = false

    /** 智能转换调度接收者类型 */
    private var _smartCastDispatchReceiverType: CangJieType? = null

    /** 剩余任务队列 */
    private var _remainingTasks: Queue<() -> Unit>? = null

    /** 参数的数据流信息 */
    override val dataFlowInfoForArguments: MutableDataFlowInfoForArguments

    // ========== 构造函数 ==========

    /**
     * 从解析候选创建已解析调用
     */
    private constructor(
        candidate: OldResolutionCandidate<D>,
        trace: DelegatingBindingTrace,
        tracing: TracingStrategy,
        dataFlowInfoForArguments: MutableDataFlowInfoForArguments
    ) {
        this.call = candidate.call
        this.candidateDescriptor = candidate.descriptor
        this._dispatchReceiver = candidate.dispatchReceiver
        this.explicitReceiverKind = candidate.explicitReceiverKind
        this.knownTypeParametersSubstitutor = candidate.knownTypeParametersResultingSubstitutor
        this._trace = trace
        this._tracing = tracing
        this.dataFlowInfoForArguments = dataFlowInfoForArguments
        this._typeArguments = createTypeArgumentsMap(candidateDescriptor)
        this._valueArguments = createValueArgumentsMap(candidateDescriptor)
        this._argumentToParameterMap = createArgumentsToParameterMap(candidateDescriptor)
    }

    /**
     * 完整构造函数
     */
    constructor(
        call: Call,
        candidateDescriptor: D,
        dispatchReceiver: ReceiverValue?,
        explicitReceiverKind: ExplicitReceiverKind,
        knownTypeParametersSubstitutor: ComposableTypeSubstitutor?,
        trace: DelegatingBindingTrace,
        tracing: TracingStrategy,
        dataFlowInfoForArguments: MutableDataFlowInfoForArguments
    ) {
        this.call = call
        this.candidateDescriptor = candidateDescriptor
        this._dispatchReceiver = dispatchReceiver
        this.explicitReceiverKind = explicitReceiverKind
        this.knownTypeParametersSubstitutor = knownTypeParametersSubstitutor
        this._trace = trace
        this._tracing = tracing
        this.dataFlowInfoForArguments = dataFlowInfoForArguments
        this._typeArguments = createTypeArgumentsMap(candidateDescriptor)
        this._valueArguments = createValueArgumentsMap(candidateDescriptor)
        this._argumentToParameterMap = createArgumentsToParameterMap(candidateDescriptor)
    }

    // ========== 状态管理 ==========

    override val status: ResolutionStatus
        get() = _status

    override fun addStatus(status: ResolutionStatus) {
        _status = _status.combine(status)
    }

    override fun setStatusToSuccess() {
        check(_status == INCOMPLETE_TYPE_INFERENCE || _status == UNKNOWN_STATUS)
        _status = SUCCESS
    }

    // ========== 追踪管理 ==========

    override val trace: DelegatingBindingTrace
        get() {
            assertNotCompleted("Trace")
            return _trace!!
        }

    override val tracingStrategy: TracingStrategy
        get() {
            assertNotCompleted("TracingStrategy")
            return _tracing!!
        }

    // ========== 描述符管理 ==========

    override val resultingDescriptor: D
        get() = _resultingDescriptor ?: candidateDescriptor

    /**
     * 设置结果替换器
     */
    fun setResultingSubstitutor(substitutor: ComposableTypeSubstitutor) {
        val descriptorToSubstitute = if (_resultingDescriptor != null && (_resultingDescriptor ?: return).shouldBeSubstituteWithStubTypes()) {
            _resultingDescriptor ?: return
        } else {
            candidateDescriptor
        }
        @Suppress("UNCHECKED_CAST")
        _resultingDescriptor = descriptorToSubstitute.substitute(substitutor) as D
    }

    /**
     * 设置已解析调用的替换器
     */
    fun setResolvedCallSubstitutor(substitutor: ComposableTypeSubstitutor) {
        // 第一阶段：直接类型参数替换（等价于 substitution[...]）
        for (typeParameter in candidateDescriptor.typeParameters) {
            val substituted = substitutor.substituteByConstructor(
                typeParameter.defaultType.constructor
            )

            if (substituted != null) {
                _typeArguments[typeParameter] = substituted
            }
        }

// 第二阶段：递归替换参数内部（safeSubstitute）
        _typeArguments = _typeArguments
            .mapValues { (_, type) ->
                substitutor.safeSubstitute(type.unwrap())
            }
            .toMutableMap()


        // 更新调度接收者类型
        if (_dispatchReceiver is ExpressionReceiver) {
            _dispatchReceiver = (_dispatchReceiver ?: return).replaceType(
                substitutor.safeSubstitute((_dispatchReceiver ?: return).type.unwrap() )
            )
        }



        // 更新值参数映射
        if (candidateDescriptor.valueParameters.isEmpty()) return

        val substitutedParameters = resultingDescriptor.valueParameters
        val valueArgumentsBeforeSubstitution = _valueArguments.entries.toList()
        _valueArguments.clear()

        for ((parameter, argument) in valueArgumentsBeforeSubstitution) {
            val substitutedVersion = substitutedParameters[parameter.index]
            _valueArguments[substitutedVersion] = argument
        }

        // 更新参数映射
        val unsubstitutedArgumentMappings = _argumentToParameterMap.entries.toList()
        _argumentToParameterMap.clear()

        for ((valueArgument, argumentMatch) in unsubstitutedArgumentMappings) {
            val valueParameterDescriptor = argumentMatch.valueParameter
            val substitutedVersion = substitutedParameters[valueParameterDescriptor.index]
            _argumentToParameterMap[valueArgument] = argumentMatch.replaceValueParameter(substitutedVersion)
        }
    }

    override fun setSubstitutor(substitutor: ComposableTypeSubstitutor) {
        setResultingSubstitutor(substitutor)
        setResolvedCallSubstitutor(substitutor)
    }

    // ========== 参数管理 ==========

    override fun recordValueArgument(valueParameter: ValueParameterDescriptor, valueArgument: ResolvedValueArgument) {
        check(!_valueArguments.containsKey(valueParameter)) {
            "$valueParameter -> $valueArgument"
        }
        _valueArguments[valueParameter] = valueArgument
        for (argument in valueArgument.arguments) {
            _argumentToParameterMap[argument] = ArgumentMatchImpl(valueParameter)
        }
    }

    override fun recordArgumentMatchStatus(valueArgument: ValueArgument, matchStatus: ArgumentMatchStatus) {
        val argumentMatch = _argumentToParameterMap[valueArgument]
        argumentMatch?.recordMatchStatus(matchStatus)
    }

    override fun getArgumentMapping(valueArgument: ValueArgument): ArgumentMapping {
        val argumentMatch = _argumentToParameterMap[valueArgument]
        if (argumentMatch == null) {
            if (isReallySuccess()) {
                LOG.error("ArgumentUnmapped for $valueArgument in successfully resolved call: ${call.callElement.text}")
            }
            return ArgumentUnmapped
        }
        return argumentMatch
    }

    override val valueArguments: Map<ValueParameterDescriptor, ResolvedValueArgument>
        get() = _valueArguments

    override val valueArgumentsByIndex: List<ResolvedValueArgument>?
        get() {
            val arguments = MutableList<ResolvedValueArgument?>(candidateDescriptor.valueParameters.size) { null }

            for ((parameterDescriptor, value) in _valueArguments) {
                val oldValue = arguments.set(parameterDescriptor.index, value)
                if (oldValue != null) {
                    return null
                }
            }

            return if (arguments.all { it != null }) {
                @Suppress("UNCHECKED_CAST")
                arguments as List<ResolvedValueArgument>
            } else {
                null
            }
        }

    // ========== 接收者管理 ==========



    override val dispatchReceiver: ReceiverValue?
        get() = _dispatchReceiver



    override val smartCastDispatchReceiverType: CangJieType?
        get() = _smartCastDispatchReceiverType

    override fun setSmartCastDispatchReceiverType(smartCastDispatchReceiverType: CangJieType) {
        _smartCastDispatchReceiverType = smartCastDispatchReceiverType
    }


    // ========== 类型参数管理 ==========

    override val typeArguments: Map<TypeParameterDescriptor, CangJieType>
        get() = _typeArguments

    // ========== 完成状态管理 ==========

    override val hasInferredReturnType: Boolean
        get() {
            if (!_completed) {
                _hasInferredReturnType = constraintSystem == null ||
                        candidateDescriptor.hasInferredReturnType(constraintSystem!!)
            }
            checkNotNull(_hasInferredReturnType) {
                "The property 'hasInferredReturnType' was not set when the call was completed."
            }
            return _hasInferredReturnType!!
        }

    override fun markCallAsCompleted() {
        if (!_completed) {
            // 触发 hasInferredReturnType 的计算
            hasInferredReturnType
        }
        _trace = null
        constraintSystem = null
        _tracing = null
        _completed = true
        _remainingTasks = null
    }

    override fun addRemainingTasks(task: () -> Unit) {
        if (_remainingTasks == null) {
            _remainingTasks = ArrayDeque()
        }
        (_remainingTasks ?: return).add(task)
    }

    override fun performRemainingTasks() {
        _remainingTasks?.let { tasks ->
            while (tasks.isNotEmpty()) {
                tasks.poll()()
            }
        }
    }

    override val isCompleted: Boolean
        get() = _completed

    // ========== 辅助方法 ==========

    /**
     * 断言调用未完成
     */
    private fun assertNotCompleted(elementName: String) {
        check(!_completed) { "$elementName is erased after resolution completion." }
    }

    companion object {
        private val LOG = Logger.getInstance(MutableResolvedCallImpl::class.java)

        /**
         * 从解析候选创建已解析调用
         */
        
        fun <D : CallableDescriptor> create(
            candidate: OldResolutionCandidate<D>,
            trace: DelegatingBindingTrace,
            tracing: TracingStrategy,
            dataFlowInfoForArguments: MutableDataFlowInfoForArguments
        ): MutableResolvedCallImpl<D> {
            return MutableResolvedCallImpl(candidate, trace, tracing, dataFlowInfoForArguments)
        }

        /**
         * 创建值参数映射
         */
        private fun createValueArgumentsMap(descriptor: CallableDescriptor): MutableMap<ValueParameterDescriptor, ResolvedValueArgument> {
            return if (descriptor.valueParameters.isEmpty()) {
                mutableMapOf()
            } else {
                LinkedHashMap()
            }
        }

        /**
         * 创建参数到参数描述符的映射
         */
        private fun createArgumentsToParameterMap(descriptor: CallableDescriptor): MutableMap<ValueArgument, ArgumentMatchImpl> {
            return if (descriptor.valueParameters.isEmpty()) {
                mutableMapOf()
            } else {
                HashMap()
            }
        }

        /**
         * 创建类型参数映射
         */
        private fun createTypeArgumentsMap(descriptor: CallableDescriptor): MutableMap<TypeParameterDescriptor, CangJieType> {
            return if (descriptor.typeParameters.isEmpty()) {
                mutableMapOf()
            } else {
                LinkedHashMap()
            }
        }
    }
}
