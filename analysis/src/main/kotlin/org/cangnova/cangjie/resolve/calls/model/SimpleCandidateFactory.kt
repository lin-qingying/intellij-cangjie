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
import org.cangnova.cangjie.descriptors.ClassDescriptor
import org.cangnova.cangjie.descriptors.ClassKind
import org.cangnova.cangjie.descriptors.PropertyDescriptor
import org.cangnova.cangjie.resolve.calls.components.CangJieResolutionCallbacks
import org.cangnova.cangjie.resolve.calls.components.InferenceSession
import org.cangnova.cangjie.resolve.calls.components.ConstraintSystemImpl
import org.cangnova.cangjie.resolve.calls.components.candidate.SimpleErrorResolutionCandidate
import org.cangnova.cangjie.resolve.calls.components.candidate.SimpleResolutionCandidate
import org.cangnova.cangjie.resolve.calls.inference.addSubsystemFromArgument
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintStorage
import org.cangnova.cangjie.resolve.calls.inference.model.LowerPriorityToPreserveCompatibility
import org.cangnova.cangjie.resolve.calls.tasks.ExplicitReceiverKind
import org.cangnova.cangjie.resolve.calls.tower.CandidateFactory
import org.cangnova.cangjie.resolve.calls.tower.CandidateWithBoundDispatchReceiver
import org.cangnova.cangjie.resolve.calls.tower.ImplicitScopeTower
import org.cangnova.cangjie.resolve.calls.tower.isSynthesized
import org.cangnova.cangjie.resolve.scopes.receivers.ReceiverValueWithSmartCastInfo
import org.cangnova.cangjie.types.ComposableTypeSubstitutor
import org.cangnova.cangjie.types.ErrorUtils
import org.cangnova.cangjie.types.error.ErrorScopeKind

/**
 * 简单候选项工厂
 *
 * 负责在调用解析过程中创建解析候选项（Resolution Candidates）。
 * 候选项代表一个可能匹配调用的函数或属性，包含了类型推断、接收者绑定等信息。
 *
 * ## 主要职责
 *
 * 1. **创建解析候选项**: 为每个可能的函数/属性描述符创建对应的解析候选项
 * 2. **初始化约束系统**: 构建类型推断所需的基础约束系统
 * 3. **处理接收者**: 处理显式接收者（dispatch receiver）和扩展接收者
 * 4. **错误处理**: 创建错误候选项用于错误恢复
 *
 * ## 工作流程
 *
 * ```
 * 调用表达式
 *   ↓
 * SimpleCandidateFactory.createCandidate(towerCandidate)
 *   ↓
 * 1. 处理接收者（显式/隐式）
 * 2. 创建 MutableResolvedCallAtom
 * 3. 初始化约束系统
 * 4. 创建 SimpleResolutionCandidate
 *   ↓
 * 返回候选项供后续重载解析
 * ```
 *
 * ## 约束系统初始化
 *
 * 在初始化时构建基础约束系统，包含：
 * - 显式接收者的类型约束
 * - 调用参数的类型约束
 * - 外部参数的类型约束
 * - 推断会话中已有的约束
 *
 * @param callComponents 调用解析所需的组件集合
 * @param scopeTower 隐式作用域塔，用于查找隐式接收者
 * @param cangjieCall 待解析的仓颉调用
 * @param resolutionCallbacks 解析回调，提供推断会话等上下文
 */
class SimpleCandidateFactory(
    val callComponents: CangJieCallComponents,
    val scopeTower: ImplicitScopeTower,
    val cangjieCall: CangJieCall,
    val resolutionCallbacks: CangJieResolutionCallbacks,
) : CandidateFactory<SimpleResolutionCandidate> {

    /** 推断会话，用于类型推断和约束求解 */
    val inferenceSession: InferenceSession = resolutionCallbacks.inferenceSession

    /** 基础约束存储，包含调用参数和接收者的初始类型约束 */
    val baseSystem: ConstraintStorage

    init {
        // 创建新的约束系统实例
        val baseSystem = ConstraintSystemImpl(
            callComponents.constraintInjector, callComponents.builtIns,
            callComponents.cangjieTypeRefiner, callComponents.languageVersionSettings
        )

        // 如果不是独立解析接收者，则添加接收者的约束
        if (!inferenceSession.resolveReceiverIndependently()) {
            baseSystem.addSubsystemFromArgument(cangjieCall.explicitReceiver)
            baseSystem.addSubsystemFromArgument(cangjieCall.dispatchReceiverForInvokeExtension)
        }

        // 添加所有参数的约束
        for (argument in cangjieCall.argumentsInParenthesis) {
            baseSystem.addSubsystemFromArgument(argument)
        }
        baseSystem.addSubsystemFromArgument(cangjieCall.externalArgument)

        // 合并推断会话中已有的约束系统
        baseSystem.addOtherSystem(inferenceSession.currentConstraintSystem())

        // 转换为只读存储
        this.baseSystem = baseSystem.asReadOnlyStorage()
    }


    /**
     * 创建接收者参数
     *
     * 将接收者信息转换为调用参数形式，用于后续的类型检查和推断。
     *
     * 优先级：
     * 1. 使用显式接收者（如果已经是 SimpleCangJieCallArgument）
     * 2. 使用解析得到的接收者（从隐式作用域中查找到的）
     *
     * @param explicitReceiver 显式接收者（如 `obj.method()` 中的 `obj`）
     * @param fromResolution 从作用域解析得到的接收者（隐式 this 等）
     * @return 接收者参数，如果都为 null 则返回 null
     */
    // todo: try something else, because current method is ugly and unstable
    private fun createReceiverArgument(
        explicitReceiver: ReceiverCangJieCallArgument?,
        fromResolution: ReceiverValueWithSmartCastInfo?
    ): SimpleCangJieCallArgument? =
        explicitReceiver as? SimpleCangJieCallArgument ?: // qualifier receiver cannot be safe
        fromResolution?.let {
            ReceiverExpressionCangJieCallArgument(
                it,
                isSafeCall = false,
                isForImplicitInvoke = cangjieCall.isForImplicitInvoke
            )
        }

    /**
     * 创建错误候选项
     *
     * 当没有找到匹配的函数或属性时，创建一个错误候选项用于错误恢复和诊断报告。
     * 错误候选项允许 IDE 继续提供基本功能（如代码补全、导航等），即使代码存在错误。
     *
     * ## 实现步骤
     *
     * 1. 创建错误作用域
     * 2. 根据调用类型（变量访问/函数调用）创建对应的错误描述符
     * 3. 处理显式接收者（如果有）
     * 4. 创建并返回错误解析候选项
     *
     * @return 错误解析候选项
     */
    override fun createErrorCandidate(): SimpleResolutionCandidate {
        val errorScope =
            ErrorUtils.createErrorScope(ErrorScopeKind.SCOPE_FOR_ERROR_RESOLUTION_CANDIDATE, cangjieCall.toString())
        val errorDescriptor = if (cangjieCall.callKind == CangJieCallKind.VARIABLE) {
            errorScope.getContributedVariables(cangjieCall.name, scopeTower.location)
        } else {
            errorScope.getContributedFunctions(cangjieCall.name, scopeTower.location)
        }.first()

        val dispatchReceiver = createReceiverArgument(cangjieCall.explicitReceiver, fromResolution = null)
        val explicitReceiverKind =
            if (dispatchReceiver == null) ExplicitReceiverKind.NO_EXPLICIT_RECEIVER else ExplicitReceiverKind.DISPATCH_RECEIVER

        return createCandidate(
            errorDescriptor, explicitReceiverKind, dispatchReceiver, extensionArgumentReceiver = null,
            extensionArgumentReceiverCandidates = null, initialDiagnostics = listOf(), knownSubstitutor = null
        )
    }

    private fun CangJieCall.getExplicitDispatchReceiver(explicitReceiverKind: ExplicitReceiverKind) =
        when (explicitReceiverKind) {
            ExplicitReceiverKind.DISPATCH_RECEIVER -> explicitReceiver
            else -> null
        }


    /**
     * 从 Tower 候选项创建解析候选项
     *
     * 将作用域塔（Scope Tower）中找到的候选项转换为完整的解析候选项。
     * 这是创建候选项的主要入口点，由调用解析器在遍历作用域时调用。
     *
     * ## 特殊处理
     *
     * - 合成的枚举 entries 属性会被标记为低优先级（为了保持兼容性）
     *
     * @param towerCandidate 作用域塔中找到的候选项，包含描述符和绑定的 dispatch 接收者
     * @param explicitReceiverKind 显式接收者类型（无/dispatch/扩展）
     * @return 完整的简单解析候选项
     */
    override fun createCandidate(
        towerCandidate: CandidateWithBoundDispatchReceiver,
        explicitReceiverKind: ExplicitReceiverKind
    ): SimpleResolutionCandidate {
        val dispatchArgumentReceiver = createReceiverArgument(
            cangjieCall.getExplicitDispatchReceiver(explicitReceiverKind),
            towerCandidate.dispatchReceiver
        )
        val extensionArgumentReceiver = null
        val descriptor = towerCandidate.descriptor
        var diagnostics: List<CangJieCallDiagnostic> = towerCandidate.diagnostics

        // 合成的枚举 entries 属性使用低优先级
        if (descriptor is PropertyDescriptor && descriptor.isSyntheticEnumEntries()) {
            diagnostics = diagnostics + LowerPriorityToPreserveCompatibility(needToReportWarning = false).asDiagnostic()
        }

        return createCandidate(
            descriptor, explicitReceiverKind, dispatchArgumentReceiver,
            extensionArgumentReceiver, extensionArgumentReceiverCandidates = null, diagnostics, knownSubstitutor = null
        )
    }

    /**
     * 创建解析候选项（内部实现）
     *
     * 核心的候选项创建逻辑，处理所有必要的信息组装。
     *
     * ## 创建步骤
     *
     * 1. **创建 ResolvedCallAtom**: 包含调用、描述符、接收者等基本信息
     * 2. **错误处理**: 如果是错误描述符，创建错误候选项
     * 3. **创建正常候选项**: 使用约束系统、作用域塔等信息
     * 4. **添加诊断**: 将初始诊断信息添加到候选项
     *
     * @param descriptor 可调用描述符（函数或属性）
     * @param explicitReceiverKind 显式接收者类型
     * @param dispatchArgumentReceiver Dispatch 接收者参数
     * @param extensionArgumentReceiver 扩展接收者参数（当前未使用）
     * @param extensionArgumentReceiverCandidates 扩展接收者候选项列表（当前未使用）
     * @param initialDiagnostics 初始诊断信息
     * @param knownSubstitutor 已知的类型替换器（用于处理泛型）
     * @return 简单解析候选项或错误解析候选项
     */
    private fun createCandidate(
        descriptor: CallableDescriptor,
        explicitReceiverKind: ExplicitReceiverKind,
        dispatchArgumentReceiver: SimpleCangJieCallArgument?,
        extensionArgumentReceiver: SimpleCangJieCallArgument?,
        extensionArgumentReceiverCandidates: List<SimpleCangJieCallArgument>?,
        initialDiagnostics: Collection<CangJieCallDiagnostic>,
        knownSubstitutor: ComposableTypeSubstitutor?
    ): SimpleResolutionCandidate {
        // 创建可变的已解析调用原子
        val resolvedCjCall = MutableResolvedCallAtom(
            cangjieCall, descriptor, explicitReceiverKind,
            dispatchArgumentReceiver
        )

        // 如果是错误描述符，返回错误候选项
        if (ErrorUtils.isError(descriptor)) {
            return SimpleErrorResolutionCandidate(
                callComponents,
                resolutionCallbacks,
                scopeTower,
                baseSystem,
                resolvedCjCall
            )
        }

        // 创建正常的解析候选项
        val candidate =
            SimpleResolutionCandidate(
                callComponents,
                resolutionCallbacks,
                scopeTower,
                baseSystem,
                resolvedCjCall,
                knownSubstitutor
            )

        // 添加初始诊断信息
        initialDiagnostics.forEach(candidate::addDiagnostic)

        // 隐藏描述符检查（当前已禁用）
//        if (callComponents.statelessCallbacks.isHiddenInResolution(descriptor, cangjieCall, resolutionCallbacks)) {
//            candidate.addDiagnostic(HiddenDescriptor)
//        }

        return candidate
    }

    /**
     * 从给定候选项创建解析候选项
     *
     * 用于处理预先确定的候选项，通常用于特殊的解析场景。
     * 例如，在某些上下文中已经知道应该使用哪个描述符。
     *
     * @param givenCandidate 给定的候选项，包含描述符和已知的类型替换器
     * @return 简单解析候选项
     */
    fun createCandidate(givenCandidate: GivenCandidate): SimpleResolutionCandidate {
        val isSafeCall = (cangjieCall.explicitReceiver as? SimpleCangJieCallArgument)?.isSafeCall ?: false

        val explicitReceiverKind =
            if (givenCandidate.dispatchReceiver == null) ExplicitReceiverKind.NO_EXPLICIT_RECEIVER else ExplicitReceiverKind.DISPATCH_RECEIVER
        val dispatchArgumentReceiver = givenCandidate.dispatchReceiver?.let {
            ReceiverExpressionCangJieCallArgument(it, isSafeCall)
        }
        return createCandidate(
            givenCandidate.descriptor, explicitReceiverKind, dispatchArgumentReceiver, null, null,
            listOf(), givenCandidate.knownTypeParametersResultingSubstitutor
        )
    }
}

/**
 * 判断属性是否为合成的枚举 entries 属性
 *
 * 枚举类型有一个合成的 `entries` 属性，提供所有枚举值的列表。
 * 此属性满足以下条件：
 * - 由编译器合成（非用户定义）
 * - 没有 dispatch 接收者参数（静态属性）
 * - 声明在枚举类中
 *
 * @return 如果是合成的枚举 entries 属性则返回 true
 */
fun PropertyDescriptor.isSyntheticEnumEntries(): Boolean {
    return isSynthesized && dispatchReceiverParameter == null &&
            (containingDeclaration as? ClassDescriptor)?.kind == ClassKind.ENUM
}


