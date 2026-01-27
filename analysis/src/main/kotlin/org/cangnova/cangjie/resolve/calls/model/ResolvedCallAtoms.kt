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

import org.cangnova.cangjie.descriptors.CallableDescriptor
import org.cangnova.cangjie.descriptors.ValueParameterDescriptor
import org.cangnova.cangjie.resolve.calls.components.TypeArgumentsToParametersMapper
import org.cangnova.cangjie.resolve.calls.components.candidate.CallableReferenceResolutionCandidate
import org.cangnova.cangjie.resolve.calls.components.candidate.ResolutionCandidate
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintSystemError
import org.cangnova.cangjie.resolve.calls.inference.model.TypeVariableFromCallableDescriptor
import org.cangnova.cangjie.resolve.calls.tasks.ExplicitReceiverKind
import org.cangnova.cangjie.resolve.constants.IntegerValueTypeConstant
import org.cangnova.cangjie.types.ComposableTypeSubstitutor
import org.cangnova.cangjie.types.UnwrappedType

/**
 * 表示解析过程的一个步骤
 * 所有解析过程都需要继承该类，并且需要实现 process 方法
 *
 * 这是一个抽象基类，用于定义函数调用解析过程中的各个处理步骤。
 * 每个具体的解析步骤（如类型推断、参数匹配、转换检查等）都应该继承此类。
 */
abstract class ResolutionPart {
    /**
     * 处理一个解析步骤
     *
     * @param workIndex 该步骤的索引，用于标识当前是第几个处理步骤
     */
    abstract fun ResolutionCandidate.process(workIndex: Int)

    /**
     * 返回该解析步骤需要执行的工作数量
     *
     * @return 工作数量，默认为 1
     */
    open fun ResolutionCandidate.workCount(): Int = 1

    // 辅助函数：提供便捷的属性访问

    /** 获取候选函数的描述符 */
    protected inline val ResolutionCandidate.candidateDescriptor get() = resolvedCall.candidateDescriptor

    /** 获取仓颉调用对象 */
    protected inline val ResolutionCandidate.cangjieCall get() = resolvedCall.atom
}

/**
 * 如果诊断信息不为空，则添加到诊断持有者中
 *
 * @param diagnostic 可能为空的调用诊断信息
 */
fun CangJieDiagnosticsHolder.addDiagnosticIfNotNull(diagnostic: CangJieCallDiagnostic?) {
    diagnostic?.let { addDiagnostic(it) }
}

/**
 * 可变的已解析调用原子
 *
 * 表示一个正在解析过程中的函数调用，包含了调用的所有相关信息和状态。
 * 这个类在解析过程中会被逐步填充和更新。
 *
 * @param atom 仓颉调用对象，表示源代码中的函数调用
 * @param originalCandidateDescriptor 原始候选函数描述符
 * @param explicitReceiverKind 显式接收者类型（如对象方法调用中的对象）
 * @param dispatchReceiverArgument 分发接收者参数
 * @param reflectionCandidateType 反射候选类型（可选）
 * @param candidate 可调用引用解析候选（可选）
 */
open class MutableResolvedCallAtom(
    override val atom: CangJieCall,
    originalCandidateDescriptor: CallableDescriptor,
    override val explicitReceiverKind: ExplicitReceiverKind,
    override val dispatchReceiverArgument: SimpleCangJieCallArgument?,
    open val reflectionCandidateType: UnwrappedType? = null,
    open val candidate: CallableReferenceResolutionCandidate? = null
) : ResolvedCallAtom() {

    /** 当前候选函数描述符（可能在解析过程中被更新） */
    private var _candidateDescriptor = originalCandidateDescriptor

    /** Unit 类型转换映射表：记录哪些参数需要进行 Unit 类型转换 */
    private var unitAdapterMap: HashMap<CangJieCallArgument, UnwrappedType>? = null

    /** 挂起函数转换映射表：记录哪些参数需要进行挂起函数转换 */
    private var suspendAdapterMap: HashMap<CangJieCallArgument, UnwrappedType>? = null

    /** 类型参数到参数的映射（按原始定义） */
    override lateinit var typeArgumentMappingByOriginal: TypeArgumentsToParametersMapper.TypeArgumentsMapping

    /** 获取当前候选函数描述符 */
    override val candidateDescriptor: CallableDescriptor
        get() = _candidateDescriptor

    /** 有符号/无符号常量转换映射表：记录整数字面量的类型转换 */
    private var signedUnsignedConstantConversions: HashMap<CangJieCallArgument, IntegerValueTypeConstant>? = null

    /** 上下文接收者参数列表 */
    override var contextReceiversArguments: List<SimpleCangJieCallArgument> = listOf()

    /** 参数映射（按原始定义）：将值参数描述符映射到已解析的调用参数 */
    override lateinit var argumentMappingByOriginal: Map<ValueParameterDescriptor, ResolvedCallArgument>

    /** 新鲜类型变量替换器：用于类型推断中的类型变量替换 */
    override lateinit var freshVariablesSubstitutor: ComposableTypeSubstitutor

    /** 新鲜类型变量列表：在类型推断过程中创建的新类型变量 */
    override var freshVariables: List<TypeVariableFromCallableDescriptor> = emptyList()

    /** 获取需要进行挂起函数转换的参数映射 */
    override val argumentsWithSuspendConversion: Map<CangJieCallArgument, UnwrappedType>
        get() = suspendAdapterMap ?: emptyMap()

    /** 已知参数替换器：基于已知类型信息进行的类型替换 */
    override lateinit var knownParametersSubstitutor: ComposableTypeSubstitutor

    /**
     * 注册一个需要进行挂起函数转换的参数
     *
     * @param argument 需要转换的参数
     * @param convertedType 转换后的类型
     */
    fun registerArgumentWithSuspendConversion(argument: CangJieCallArgument, convertedType: UnwrappedType) {
        if (suspendAdapterMap == null)
            suspendAdapterMap = hashMapOf()

        suspendAdapterMap!![argument] = convertedType
    }

    /** 参数到候选函数参数的映射 */
    lateinit var argumentToCandidateParameter: Map<CangJieCallArgument, ValueParameterDescriptor>

    /** SAM（Single Abstract Method）转换映射表：记录函数式接口转换 */
    private var samAdapterMap: HashMap<CangJieCallArgument, SamConversionDescription>? = null

    /** 获取需要进行 SAM 转换的参数映射 */
    override val argumentsWithConversion: Map<CangJieCallArgument, SamConversionDescription>
        get() = samAdapterMap ?: emptyMap()

    /** 是否存在 SAM 转换 */
    val hasSamConversion: Boolean
        get() = samAdapterMap != null

    /** 获取需要进行 Unit 类型转换的参数映射 */
    override val argumentsWithUnitConversion: Map<CangJieCallArgument, UnwrappedType>
        get() = unitAdapterMap ?: emptyMap()

    /** 获取需要进行常量转换的参数映射 */
    override val argumentsWithConstantConversion: Map<CangJieCallArgument, IntegerValueTypeConstant>
        get() = signedUnsignedConstantConversions ?: emptyMap()

    /**
     * 设置新的候选函数描述符
     *
     * 在解析过程中，可能需要更新候选函数（例如，在类型推断后替换为更具体的版本）
     *
     * @param newCandidateDescriptor 新的候选函数描述符
     */
    override fun setCandidateDescriptor(newCandidateDescriptor: CallableDescriptor) {
        if (newCandidateDescriptor == candidateDescriptor) return
        _candidateDescriptor = newCandidateDescriptor
    }

    /**
     * 注册一个需要进行常量转换的参数
     *
     * 用于处理整数字面量在有符号和无符号类型之间的转换
     *
     * @param argument 需要转换的参数
     * @param convertedConstant 转换后的常量类型
     */
    fun registerArgumentWithConstantConversion(
        argument: CangJieCallArgument,
        convertedConstant: IntegerValueTypeConstant
    ) {
        if (signedUnsignedConstantConversions == null)
            signedUnsignedConstantConversions = hashMapOf()

        signedUnsignedConstantConversions!![argument] = convertedConstant
    }

    /**
     * 注册一个需要进行 SAM 转换的参数
     *
     * SAM 转换允许将 lambda 表达式转换为函数式接口（只有一个抽象方法的接口）
     *
     * @param argument 需要转换的参数
     * @param samConversionDescription SAM 转换描述信息
     */
    fun registerArgumentWithSamConversion(
        argument: CangJieCallArgument,
        samConversionDescription: SamConversionDescription
    ) {
        if (samAdapterMap == null)
            samAdapterMap = hashMapOf()

        samAdapterMap!![argument] = samConversionDescription
    }

    /** 返回调用和候选函数的字符串表示 */
    override fun toString(): String = "$atom, candidate = $candidateDescriptor"

    /**
     * 注册一个需要进行 Unit 类型转换的参数
     *
     * 用于将非 Unit 类型的表达式转换为 Unit 类型（丢弃返回值）
     *
     * @param argument 需要转换的参数
     * @param convertedType 转换后的类型
     */
    fun registerArgumentWithUnitConversion(argument: CangJieCallArgument, convertedType: UnwrappedType) {
        if (unitAdapterMap == null)
            unitAdapterMap = hashMapOf()

        unitAdapterMap!![argument] = convertedType
    }

    /**
     * 设置分析结果
     *
     * 在嵌套调用的情况下，将子调用的解析结果保存到当前调用中
     *
     * @param subResolvedAtoms 子已解析原子列表
     */
    public override fun setAnalyzedResults(subResolvedAtoms: List<ResolvedAtom>) {
        super.setAnalyzedResults(subResolvedAtoms)
    }
}

/**
 * 仓颉诊断信息持有者接口
 *
 * 用于收集和管理函数调用解析过程中产生的诊断信息（错误、警告等）
 */
interface CangJieDiagnosticsHolder {
    /**
     * 添加一个诊断信息
     *
     * @param diagnostic 调用诊断信息
     */
    fun addDiagnostic(diagnostic: CangJieCallDiagnostic)

    /**
     * 简单的诊断信息持有者实现
     *
     * 使用 ArrayList 存储所有诊断信息
     */
    class SimpleHolder : CangJieDiagnosticsHolder {
        /** 诊断信息列表 */
        private val diagnostics = arrayListOf<CangJieCallDiagnostic>()

        /** 添加诊断信息到列表 */
        override fun addDiagnostic(diagnostic: CangJieCallDiagnostic) {
            diagnostics.add(diagnostic)
        }

        /** 获取所有诊断信息 */
        fun getDiagnostics(): List<CangJieCallDiagnostic> = diagnostics
    }
}

/**
 * 已解析的可调用引用调用原子
 *
 * 表示一个已解析的可调用引用（如函数引用 `::functionName`）
 * 这是 MutableResolvedCallAtom 的特化版本，专门用于处理可调用引用
 *
 * @param atom 仓颉调用对象
 * @param candidateDescriptor 候选函数描述符
 * @param explicitReceiverKind 显式接收者类型
 * @param dispatchReceiverArgument 分发接收者参数
 * @param reflectionCandidateType 反射候选类型（可选）
 * @param candidate 可调用引用解析候选（可选）
 */
class ResolvedCallableReferenceCallAtom(
    atom: CangJieCall,
    candidateDescriptor: CallableDescriptor,
    explicitReceiverKind: ExplicitReceiverKind,
    dispatchReceiverArgument: SimpleCangJieCallArgument?,
    reflectionCandidateType: UnwrappedType? = null,
    candidate: CallableReferenceResolutionCandidate? = null
) : MutableResolvedCallAtom(
    atom,
    candidateDescriptor,
    explicitReceiverKind,
    dispatchReceiverArgument,
    reflectionCandidateType,
    candidate
), ResolvedCallableReferenceAtom

/**
 * 将约束系统错误添加为诊断信息
 *
 * 这是一个扩展函数，用于将类型推断约束系统中的错误转换为诊断信息
 *
 * @param error 约束系统错误
 */
fun CangJieDiagnosticsHolder.addError(error: ConstraintSystemError) {
    addDiagnostic(error.asDiagnostic())
}

/**
 * 标记候选函数以保持兼容性解析
 *
 * 用于在类型推断的新旧模式之间保持兼容性
 * 当需要使用旧的解析模式以保持向后兼容时调用此函数
 *
 * @param needToReportWarning 是否需要报告警告，默认为 true
 */
fun ResolutionCandidate.markCandidateForCompatibilityResolve(needToReportWarning: Boolean = true) {
    // 如果语言版本支持禁用兼容模式特性，则跳过
    // if (callComponents.languageVersionSettings.supportsFeature(LanguageFeature.DisableCompatibilityModeForNewInference)) return

    // 添加降低优先级以保持兼容性的诊断信息
    // addDiagnostic(LowerPriorityToPreserveCompatibility(needToReportWarning).asDiagnostic())
}