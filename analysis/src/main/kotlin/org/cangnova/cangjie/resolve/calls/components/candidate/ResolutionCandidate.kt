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

package org.cangnova.cangjie.resolve.calls.components.candidate

import org.cangnova.cangjie.descriptors.CallableDescriptor
import org.cangnova.cangjie.renderer.DescriptorRenderer
import org.cangnova.cangjie.resolve.calls.components.CangJieResolutionCallbacks
import org.cangnova.cangjie.resolve.calls.components.ConstraintSystemImpl
import org.cangnova.cangjie.resolve.calls.inference.ConstraintSystem
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintStorage
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintSystemImpl
import org.cangnova.cangjie.resolve.calls.model.*
import org.cangnova.cangjie.resolve.calls.tower.*
import org.cangnova.cangjie.types.ComposableTypeSubstitutor
import org.cangnova.cangjie.types.model.TypeSubstitutorMarker

/**
 * 解析候选类 (Resolution Candidate)
 *
 * 这是解析过程中的核心抽象类,表示一个可能的调用解析候选。在重载解析过程中,
 * 编译器会为每个可能的目标(函数、构造器等)创建一个 ResolutionCandidate 实例,
 * 然后通过一系列检查步骤来确定该候选是否适用以及适用程度。
 *
 * ## 核心职责
 * - **候选管理**: 封装一个解析候选的所有信息
 * - **步骤执行**: 按顺序执行解析步骤(参数检查、类型推导等)
 * - **诊断收集**: 收集解析过程中产生的错误和警告
 * - **适用性评估**: 计算候选的最终适用性级别
 *
 * ## 解析流程
 * 解析候选按以下步骤处理:
 * 1. **接收者检查**: 验证接收者类型是否匹配
 * 2. **类型参数检查**: 验证类型参数数量和约束
 * 3. **参数映射**: 将调用参数映射到函数参数
 * 4. **类型推导**: 推导未指定的类型参数
 * 5. **约束检查**: 验证所有类型约束是否满足
 *
 * ## 适用性级别
 * 候选的适用性按以下优先级排序(从高到低):
 * - [CandidateApplicability.RESOLVED]: 完全匹配
 * - [CandidateApplicability.RESOLVED_LOW_PRIORITY]: 需要隐式转换
 * - [CandidateApplicability.INAPPLICABLE]: 不适用
 *
 * @see Candidate 候选接口
 * @see CangJieDiagnosticsHolder 诊断持有者接口
 * @see MutableResolvedCallAtom 可变解析调用原子
 */
sealed class ResolutionCandidate : Candidate, CangJieDiagnosticsHolder {
    /**
     * 已解析的调用原子
     *
     * 包含调用的所有解析信息,包括目标描述符、参数映射、类型参数等。
     * 这是一个可变对象,在解析过程中会逐步填充信息。
     */
    abstract val resolvedCall: MutableResolvedCallAtom

    /**
     * 调用组件
     *
     * 提供解析过程中需要的各种工具和服务,如:
     * - 约束注入器: 生成类型约束
     * - 内置类型: 访问语言的内置类型
     * - 类型精炼器: 处理智能类型转换
     * - 语言版本设置: 获取语言特性开关
     */
    abstract val callComponents: CangJieCallComponents

    /**
     * invoke 调用时的变量候选
     *
     * 当解析 `obj()` 这种调用时:
     * 1. 首先将 `obj` 解析为变量(variableCandidate)
     * 2. 然后在该变量的类型上查找 `invoke` 操作符
     * 3. 这个属性指向第一步的变量候选
     *
     * 对于普通函数调用,此属性为 null。
     */
    abstract val variableCandidateIfInvoke: ResolutionCandidate?

    /**
     * 作用域塔
     *
     * 提供解析过程中的作用域访问能力:
     * - 成员作用域: 访问类型的成员
     * - 导入作用域: 访问导入的符号
     * - 隐式接收者: 访问 this 和扩展接收者
     */
    abstract val scopeTower: ImplicitScopeTower

    /**
     * 已知类型参数的替换器
     *
     * 当部分类型参数已知时(例如通过显式类型参数或外部上下文),
     * 这个替换器用于将已知类型参数应用到候选描述符上。
     */
    abstract val knownTypeParametersResultingSubstitutor: ComposableTypeSubstitutor?

    /**
     * 解析回调
     *
     * 提供解析过程中需要的回调接口,用于:
     * - 报告诊断信息
     * - 记录解析决策
     * - 触发后续处理
     */
    abstract val resolutionCallbacks: CangJieResolutionCallbacks

    /**
     * 基础约束系统
     *
     * 来自外部上下文的约束存储。例如,在嵌套调用中,
     * 外层调用的约束可能会影响内层调用的解析。
     */
    protected abstract val baseSystem: ConstraintStorage?

    /**
     * 解析步骤序列
     *
     * 定义了该候选需要执行的解析步骤列表。不同类型的调用有不同的步骤序列:
     * - 普通函数调用: 接收者检查 → 类型参数 → 参数映射 → 类型推导
     * - 构造器调用: 类型参数 → 参数映射 → 类型推导
     * - invoke 调用: 变量解析 → invoke 操作符解析
     */
    open val resolutionSequence: List<ResolutionPart> get() = resolvedCall.atom.callKind.resolutionSequence

    /**
     * 最终适用性
     *
     * 计算候选的最终适用性级别,考虑所有解析步骤的结果。
     * 返回所有适用性级别中最低的那个(最不适用的),因为任何一个步骤失败都会影响整体。
     *
     * 此属性会触发所有解析步骤的执行(如果尚未执行)。
     */
    override val resultingApplicability: CandidateApplicability
        get() {
            processParts(stopOnFirstError = false)
            return resultingApplicabilities.minOrNull() ?: CandidateApplicability.RESOLVED
        }

    /**
     * 是否成功
     *
     * 判断候选是否成功解析。成功的标准是:
     * 1. 适用性级别至少为 RESOLVED_LOW_PRIORITY
     * 2. 约束系统没有矛盾
     *
     * 注意: RESOLVED_WITH_ERROR 级别也被视为成功,允许在有非致命错误时继续解析。
     * 此属性会在第一个致命错误时停止处理,以提高性能。
     */
    override val isSuccessful: Boolean
        get() {
            processParts(stopOnFirstError = true)
            // Note: candidate with  RESOLVED_WITH_ERROR is exceptionally treated as successful
            return resultingApplicabilities.minOrNull()!!.isSuccessOrSuccessWithError && !getSystem().hasContradiction
        }


    /**
     * 适用性级别是否为成功或带错误的成功
     *
     * 扩展属性,用于判断适用性级别是否足够高,可以被视为成功。
     * RESOLVED_LOW_PRIORITY 及以上级别都被视为成功。
     */
    private val CandidateApplicability.isSuccessOrSuccessWithError: Boolean
        get() = this >= CandidateApplicability.RESOLVED_LOW_PRIORITY


    /**
     * 可变诊断列表
     *
     * 存储解析过程中产生的所有诊断(错误和警告)。
     * 这是一个内部可变列表,外部通过 [diagnostics] 属性访问不可变视图。
     */
    protected val mutableDiagnostics: ArrayList<CangJieCallDiagnostic> = arrayListOf()

    /**
     * 候选描述符
     *
     * 被调用的函数、构造器或属性的描述符。
     * 这是候选的核心标识,包含签名、类型参数、可见性等所有元信息。
     */
    val descriptor: CallableDescriptor get() = resolvedCall.candidateDescriptor

    /**
     * 诊断列表(只读)
     *
     * 提供对收集的诊断信息的只读访问。
     */
    val diagnostics: List<CangJieCallDiagnostic> = mutableDiagnostics

    /**
     * 结果适用性数组
     *
     * 返回影响最终适用性的所有因素:
     * 1. [currentApplicability]: 当前步骤产生的适用性
     * 2. 约束系统错误产生的适用性
     * 3. [variableApplicability]: invoke 调用时变量候选的适用性
     *
     * 最终适用性取这些值中的最小值(最不适用的)。
     */
    val resultingApplicabilities: Array<CandidateApplicability>
        get() = arrayOf(currentApplicability, getResultApplicability(getSystem().errors), variableApplicability)

    /**
     * 变量适用性
     *
     * 对于 invoke 调用,返回变量候选的适用性;对于普通调用,返回 RESOLVED。
     */
    private val variableApplicability
        get() = variableCandidateIfInvoke?.resultingApplicability ?: CandidateApplicability.RESOLVED

    /**
     * 总步骤数
     *
     * 计算所有解析步骤的工作量总和。每个步骤可能包含多个工作单元。
     */
    private val stepCount get() = resolutionSequence.sumOf { it.run { workCount() } }

    /**
     * 当前步骤索引
     *
     * 记录已经处理到第几个工作单元。用于实现增量解析和懒加载。
     */
    private var step = 0

    /**
     * 新约束系统
     *
     * 类型推导使用的约束系统实例。延迟初始化,只在需要时创建。
     */
    private var newSystem: ConstraintSystemImpl? = null

    /**
     * 当前适用性
     *
     * 记录当前已处理步骤产生的适用性级别。会随着步骤处理逐步降级。
     */
    private var currentApplicability: CandidateApplicability = CandidateApplicability.RESOLVED

    /**
     * 获取结果替换器
     *
     * 返回类型推导完成后的类型替换器,可用于将推导出的类型参数应用到表达式上。
     *
     * @return 类型替换器,如果约束系统尚未创建则返回 null
     */
    fun getResultingSubstitutor(): TypeSubstitutorMarker? = newSystem?.buildCurrentSubstitutor()

    /**
     * 获取子解析原子列表
     *
     * 返回该候选内部的所有子解析原子。例如,lambda 参数、嵌套调用等。
     * 子类需要实现此方法来提供具体的子原子。
     */
    abstract fun getSubResolvedAtoms(): List<ResolvedAtom>

    /**
     * 添加已解析的仓颉原语
     *
     * 将一个已解析的原子添加到候选中。用于记录嵌套解析的结果。
     *
     * @param resolvedAtom 已解析的原子
     */
    abstract fun addResolvedCjPrimitive(resolvedAtom: ResolvedAtom)

    /**
     * 添加诊断
     *
     * 记录一个诊断信息,并根据诊断的严重性更新当前适用性级别。
     * 更严重的诊断会降低适用性级别。
     *
     * @param diagnostic 要添加的诊断信息
     */
    override fun addDiagnostic(diagnostic: CangJieCallDiagnostic) {
        mutableDiagnostics.add(diagnostic)
        currentApplicability = minOf(diagnostic.candidateApplicability, currentApplicability)
    }

    /**
     * 添加兼容性警告
     *
     * 当存在多个候选时,可能需要添加兼容性警告。
     * 当前实现已注释,保留接口供未来使用。
     *
     * @param other 另一个候选
     */
    override fun addCompatibilityWarning(other: Candidate) {
//        if (other is ResolutionCandidate && this !== other && this::class == other::class) {
//            addDiagnostic(CompatibilityWarning(other.descriptor))
//        }
    }

    /**
     * 转换为字符串表示
     *
     * 返回候选的人类可读表示,包含:
     * - 状态: OK 或 FAIL
     * - 进度: 已处理步骤数/总步骤数
     * - 描述符: 候选的简洁描述
     *
     * 示例: "OK(3/5): fun foo(Int): String"
     */
    override fun toString(): String {

        val descriptor = DescriptorRenderer.COMPACT.render(resolvedCall.candidateDescriptor)
        val okOrFail = if (resultingApplicabilities.minOrNull()?.isSuccess != false) "OK" else "FAIL"
        val step = "$step/$stepCount"
        return "$okOrFail($step): $descriptor"
    }

    /**
     * 获取约束系统
     *
     * 返回用于类型推导的约束系统实例。如果尚未创建,则初始化一个新的约束系统,
     * 并将基础约束系统(如果有)的约束复制进来。
     *
     * 约束系统用于:
     * - 收集类型约束(如 T <: Int)
     * - 求解类型参数
     * - 检测类型冲突
     *
     * @return 约束系统实例
     */
    fun getSystem(): ConstraintSystem {
        if (newSystem == null) {
            newSystem = ConstraintSystemImpl(
                callComponents.constraintInjector, callComponents.builtIns,
                callComponents.cangjieTypeRefiner, callComponents.languageVersionSettings
            )
            if (baseSystem != null) {
                newSystem!!.addOtherSystem(baseSystem!!)
            }
        }
        return newSystem!!
    }

    /**
     * 处理解析步骤
     *
     * 这是解析候选的核心方法,负责执行所有解析步骤。采用增量处理策略,
     * 只处理尚未处理的步骤,支持懒加载和提前终止。
     *
     * ## 工作原理
     * 1. **状态检查**: 如果已经发生错误且要求提前终止,则直接返回
     * 2. **步骤定位**: 根据当前步骤索引,定位到要处理的解析步骤
     * 3. **增量处理**: 只处理从上次停止位置开始的步骤
     * 4. **结果记录**: 所有步骤完成后,记录解析结果
     *
     * ## 提前终止优化
     * 当 [stopOnFirstError] 为 true 时,一旦遇到致命错误就停止处理。
     * 这对于 [isSuccessful] 属性很有用,因为它只需要知道是否成功,
     * 不需要收集所有错误。
     *
     * ## 增量处理机制
     * [step] 字段记录了已处理的工作单元数。每次调用此方法时:
     * - 如果 step == 0: 从头开始处理所有步骤
     * - 如果 0 < step < stepCount: 从中断位置继续处理
     * - 如果 step == stepCount: 所有步骤已完成,直接返回
     *
     * @param stopOnFirstError 是否在第一个错误时停止处理
     *                         - true: 性能优先,只需要知道成败
     *                         - false: 完整性优先,收集所有诊断信息
     */
    private fun processParts(stopOnFirstError: Boolean) {
        if (stopOnFirstError && step > 0) return // 错误已经发生，直接返回
        if (step == stepCount) return // 已经处理完所有步骤，直接返回

        // 定位到当前应该处理的解析步骤
        var partIndex = 0
        var workStep = step
        while (workStep > 0) {
            val workCount = resolutionSequence[partIndex].run { workCount() }
            if (workStep >= workCount) {
                partIndex++
                workStep -= workCount
            } else {
                break
            }
        }

        // 处理当前部分(可能只处理部分工作单元)
        if (partIndex < resolutionSequence.size) {
            if (processPart(resolutionSequence[partIndex], stopOnFirstError, workStep)) return
            partIndex++
        }

        // 继续处理剩余部分
        while (partIndex < resolutionSequence.size) {
            if (processPart(resolutionSequence[partIndex], stopOnFirstError)) return
            partIndex++
        }

        // 如果所有步骤都已处理完成，设置分析结果
        if (step == stepCount) {
            resolvedCall.setAnalyzedResults(getSubResolvedAtoms())
        }
    }


    /**
     * 处理单个解析步骤
     *
     * 处理解析序列中的一个步骤。每个步骤可能包含多个工作单元,
     * 此方法会逐个处理这些工作单元,直到:
     * 1. 所有工作单元都处理完成
     * 2. 或遇到错误且要求提前终止
     *
     * ## 工作单元
     * 一个解析步骤可以包含多个工作单元,例如:
     * - 参数映射步骤: 每个参数是一个工作单元
     * - 类型参数检查步骤: 每个类型参数是一个工作单元
     * - 约束求解步骤: 可能只有一个工作单元
     *
     * ## 中断机制
     * 如果在处理过程中适用性级别降到不成功,且 [stopOnFirstError] 为 true,
     * 则立即停止处理并返回 true,通知调用者发生了中断。
     *
     * @param part 要处理的解析步骤
     * @param stopOnFirstError 是否在遇到错误时提前终止
     * @param startWorkIndex 从哪个工作单元开始处理(默认为 0,用于增量处理)
     * @return true 表示处理被中断(因为遇到错误),false 表示正常完成
     */
    private fun processPart(part: ResolutionPart, stopOnFirstError: Boolean, startWorkIndex: Int = 0): Boolean {
        for (workIndex in startWorkIndex until (part.run { workCount() })) {
            // 如果要求提前终止且当前已失败,则中断处理
            if (stopOnFirstError && !currentApplicability.isSuccess) return true

            // 处理当前工作单元
            part.run { process(workIndex) }
            step++
        }
        return false
    }
}
