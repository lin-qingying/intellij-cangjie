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

package org.cangnova.cangjie.resolve.calls.model

import com.intellij.util.SmartList
import org.cangnova.cangjie.builtins.CangJieBuiltIns
import org.cangnova.cangjie.descriptors.CallableDescriptor
import org.cangnova.cangjie.descriptors.FunctionDescriptor
import org.cangnova.cangjie.resolve.calls.components.CallableReceiver
import org.cangnova.cangjie.resolve.calls.components.CallableReferenceAdaptation
import org.cangnova.cangjie.resolve.calls.components.CangJieResolutionCallbacks
import org.cangnova.cangjie.resolve.calls.components.candidate.CallableReferenceResolutionCandidate
import org.cangnova.cangjie.resolve.calls.components.stableType
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintStorage
import org.cangnova.cangjie.resolve.calls.tasks.ExplicitReceiverKind
import org.cangnova.cangjie.resolve.calls.tower.CandidateFactory
import org.cangnova.cangjie.resolve.calls.tower.CandidateWithBoundDispatchReceiver
import org.cangnova.cangjie.resolve.calls.tower.ImplicitScopeTower
import org.cangnova.cangjie.resolve.calls.tower.createCallableReferenceProcessor
import org.cangnova.cangjie.resolve.scopes.receivers.DetailedReceiver
import org.cangnova.cangjie.resolve.scopes.receivers.ReceiverValueWithSmartCastInfo
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.ErrorUtils
import org.cangnova.cangjie.types.UnwrappedType
import org.cangnova.cangjie.types.error.ErrorScopeKind
import org.cangnova.cangjie.types.error.ErrorTypeKind
import org.cangnova.cangjie.types.toFunctionType

/**
 * 可调用引用候选项工厂
 *
 * 负责创建可调用引用（Callable Reference）的解析候选项。
 * 可调用引用是对函数或属性的引用，可以作为值传递和使用。
 *
 * ## 可调用引用示例
 *
 * ```cangjie
 * // 函数引用
 * func add(x: Int64, y: Int64): Int64 => x + y
 * let addRef = add  // 函数引用
 *
 * // 成员函数引用
 * class MyClass {
 *     func method() { }
 * }
 * let methodRef = MyClass::method  // 成员函数引用
 * ```
 *
 * ## 主要职责
 *
 * 1. **创建解析候选项**: 为可调用引用创建对应的解析候选项
 * 2. **处理接收者**: 处理绑定/未绑定的接收者
 * 3. **构建反射类型**: 构建可调用引用的函数类型
 * 4. **适配处理**: 处理参数适配（如默认参数）
 *
 * ## 工作流程
 *
 * ```
 * 可调用引用表达式 (::function)
 *   ↓
 * CallableReferencesCandidateFactory.createCandidate(towerCandidate)
 *   ↓
 * 1. 处理 dispatch 接收者
 * 2. 构建反射类型（函数类型）
 * 3. 处理可调用引用适配
 * 4. 创建 CallableReferenceResolutionCandidate
 *   ↓
 * 返回候选项供后续重载解析
 * ```
 *
 * @param cangjieCall 可调用引用解析原子，包含引用的基本信息
 * @param callComponents 调用解析所需的组件集合
 * @param scopeTower 隐式作用域塔，用于查找候选项
 * @param expectedType 期望的类型（通常是函数类型）
 * @param baseSystem 基础约束系统
 * @param resolutionCallbacks 解析回调
 */
class CallableReferencesCandidateFactory(
    val cangjieCall: CallableReferenceResolutionAtom,
    val callComponents: CangJieCallComponents,
    val scopeTower: ImplicitScopeTower,
    val expectedType: UnwrappedType?,
    private val baseSystem: ConstraintStorage?,
    private val resolutionCallbacks: CangJieResolutionCallbacks
) : CandidateFactory<CallableReferenceResolutionCandidate> {

    /**
     * 将接收者值转换为可调用接收者
     *
     * 可调用引用的接收者有多种形式：
     * - **显式值接收者**: 如 `obj::method`
     * - **作用域接收者**: 如在类内部引用 `::method`
     * - **未绑定引用**: 如 `MyClass::method`（接收者作为第一个参数）
     * - **绑定值引用**: 如 `myObject::method`（接收者已绑定）
     *
     * @param receiver 接收者值（包含智能转换信息）
     * @param isExplicit 是否为显式接收者
     * @return 可调用接收者
     */
    private fun toCallableReceiver(receiver: ReceiverValueWithSmartCastInfo, isExplicit: Boolean): CallableReceiver {
        if (!isExplicit) return CallableReceiver.ScopeReceiver(receiver)

        return CallableReceiver.ExplicitValueReceiver(receiver)
        // 以下代码用于区分绑定和未绑定引用（当前已简化）
//        return when (val lhsResult = cangjieCall.lhsResult) {
//            is LHSResult.Expression -> CallableReceiver.ExplicitValueReceiver(receiver)
//            is LHSResult.Type -> {
//                if (lhsResult.qualifier?.classValueReceiver?.type == receiver.receiverValue.type) {
//                    CallableReceiver.BoundValueReference(receiver)
//                } else {
//                    CallableReceiver.UnboundReference(receiver)
//                }
//            }
//
//
//            else -> throw IllegalStateException("Unsupported kind of lhsResult: $lhsResult")
//        }
    }

    /**
     * 创建可调用引用处理器
     *
     * 处理器负责在作用域塔中查找可调用引用的候选项。
     *
     * @param explicitReceiver 显式接收者（如果有）
     * @return 可调用引用处理器
     */
    fun createCallableProcessor(explicitReceiver: DetailedReceiver?) =
        createCallableReferenceProcessor(scopeTower, cangjieCall.rhsName, this, explicitReceiver)

    /**
     * 构建可调用引用的反射类型
     *
     * 可调用引用会被转换为对应的函数类型。根据描述符的不同类型，生成不同的反射类型：
     *
     * - **函数描述符**: 转换为函数类型 `(参数类型...) -> 返回类型`
     * - **属性描述符**: 转换为 KProperty 类型（当前版本暂未实现）
     *
     * ## 接收者处理
     *
     * 对于未绑定引用（如 `MyClass::method`），接收者会作为第一个参数添加到参数列表中。
     *
     * @param descriptor 可调用描述符（函数或属性）
     * @param dispatchReceiver Dispatch 接收者
     * @param expectedType 期望类型
     * @param builtins 内置类型系统
     * @return 反射类型和可调用引用适配信息的配对
     */
    private fun buildReflectionType(
        descriptor: CallableDescriptor,
        dispatchReceiver: CallableReceiver?,

        expectedType: UnwrappedType?,
        builtins: CangJieBuiltIns,
    ): Pair<UnwrappedType, CallableReferenceAdaptation?> {
        // 构建参数和接收者列表
        val argumentsAndReceivers =
            ArrayList<CangJieType>(descriptor.valueParameters.size + 2  )

        // 未绑定引用：接收者作为第一个参数
        if (dispatchReceiver is CallableReceiver.UnboundReference) {
            argumentsAndReceivers.add(dispatchReceiver.receiver.stableType)
        }

        // 获取返回类型
        val descriptorReturnType = descriptor.returnType
            ?: ErrorUtils.createErrorType(ErrorTypeKind.RETURN_TYPE, descriptor.toString())

        return when (descriptor) {


            // 函数引用转换为函数类型
            is FunctionDescriptor -> {
                descriptor.toFunctionType(builtins) as UnwrappedType to null
            }

            // 不支持的可调用引用类型
            else -> {
//                assert(!descriptor.isSupportedForCallableReference()) { "${descriptor::class} isn't supported to use in callable references actually, but it's listed in `isSupportedForCallableReference` method" }
                ErrorUtils.createErrorType(
                    ErrorTypeKind.UNSUPPORTED_CALLABLE_REFERENCE_TYPE,
                    descriptor.toString()
                ) to null
            }
        }
    }

    /**
     * 从 Tower 候选项创建可调用引用解析候选项
     *
     * 将作用域塔中找到的候选项转换为完整的可调用引用解析候选项。
     *
     * ## 处理步骤
     *
     * 1. **转换 dispatch 接收者**: 将接收者值转换为可调用接收者
     * 2. **构建反射类型**: 生成可调用引用对应的函数类型
     * 3. **收集诊断信息**: 包括隐藏描述符、适配警告等
     * 4. **创建候选项**: 组装所有信息创建最终候选项
     *
     * ## 特殊处理（当前已禁用）
     *
     * - 隐藏描述符检查
     * - 兼容性解析标记
     * - 适配的可调用引用警告
     * - 默认参数使用检查
     * - 合成枚举 entries 属性优先级
     *
     * @param towerCandidate 作用域塔中找到的候选项
     * @param explicitReceiverKind 显式接收者类型
     * @return 可调用引用解析候选项
     */
    override fun createCandidate(
        towerCandidate: CandidateWithBoundDispatchReceiver,
        explicitReceiverKind: ExplicitReceiverKind,

    ): CallableReferenceResolutionCandidate {
        // 转换 dispatch 接收者
        val dispatchCallableReceiver =
            towerCandidate.dispatchReceiver?.let {
                toCallableReceiver(
                    it,
                    explicitReceiverKind == ExplicitReceiverKind.DISPATCH_RECEIVER
                )
            }

        val candidateDescriptor = towerCandidate.descriptor
        val diagnostics = SmartList<CangJieCallDiagnostic>()

        // 构建反射类型（函数类型）
        val (reflectionCandidateType, callableReferenceAdaptation) = buildReflectionType(
            candidateDescriptor,
            dispatchCallableReceiver,

            expectedType,
            callComponents.builtIns,
        )

        // 创建可调用引用候选项的辅助函数
        fun createCallableReferenceCallCandidate(diagnostics: List<CangJieCallDiagnostic>) =
            CallableReferenceResolutionCandidate(
                candidateDescriptor, dispatchCallableReceiver,
                explicitReceiverKind, reflectionCandidateType, callableReferenceAdaptation,
                cangjieCall, expectedType, callComponents, scopeTower, resolutionCallbacks, baseSystem
            ).also { diagnostics.forEach(it::addDiagnostic) }

        // 以下特殊处理当前已禁用
//        if (callComponents.statelessCallbacks.isHiddenInResolution(descriptor, cangjieCall.call, resolutionCallbacks)) {
//            diagnostics.add(HiddenDescriptor)
//            return createCallableReferenceCallCandidate(diagnostics)
//        }
//
//        if (needCompatibilityResolveForCallableReference(callableReferenceAdaptation, descriptor)) {
//            markCandidateForCompatibilityResolve(diagnostics)
//        }
//
//        if (callableReferenceAdaptation != null && expectedType != null && hasNonTrivialAdaptation(callableReferenceAdaptation)) {
//            if (!expectedType.isFunctionType && !expectedType.isSuspendFunctionType) { // expectedType has some reflection type
//                diagnostics.add(AdaptedCallableReferenceIsUsedWithReflection(cangjieCall))
//            }
//        }
//
//        if (callableReferenceAdaptation != null &&
//            callableReferenceAdaptation.defaults != 0 &&
//            !callComponents.languageVersionSettings.supportsFeature(LanguageFeature.FunctionReferenceWithDefaultValueAsOtherType)
//        ) {
//            diagnostics.add(CallableReferencesDefaultArgumentUsed(cangjieCall, descriptor, callableReferenceAdaptation.defaults))
//        }
//
//        if (descriptor !is CallableMemberDescriptor) {
//            return createCallableReferenceCallCandidate(listOf(NotCallableMemberReference(cangjieCall, descriptor)))
//        }
//
//        if (descriptor is PropertyDescriptor && descriptor.isSyntheticEnumEntries()) {
//            diagnostics.add(LowerPriorityToPreserveCompatibility(needToReportWarning = false).asDiagnostic())
//        }

        // 添加来自作用域塔的诊断信息
        diagnostics.addAll(towerCandidate.diagnostics)
        // todo smartcast on receiver diagnostic and CheckInstantiationOfAbstractClass

        return createCallableReferenceCallCandidate(diagnostics)
    }

    /**
     * 创建错误候选项
     *
     * 当无法找到匹配的可调用引用时，创建错误候选项用于错误恢复和诊断。
     * 这允许 IDE 在代码存在错误时仍能提供基本的语言服务功能。
     *
     * ## 实现步骤
     *
     * 1. 创建错误作用域
     * 2. 获取错误函数描述符
     * 3. 构建错误的反射类型
     * 4. 创建错误的可调用引用解析候选项
     *
     * @return 错误可调用引用解析候选项
     */
    override fun createErrorCandidate(): CallableReferenceResolutionCandidate {
        val errorScope =
            ErrorUtils.createErrorScope(ErrorScopeKind.SCOPE_FOR_ERROR_RESOLUTION_CANDIDATE, cangjieCall.toString())
        val errorDescriptor = errorScope.getContributedFunctions(cangjieCall.rhsName, scopeTower.location).first()

        val (reflectionCandidateType, callableReferenceAdaptation) = buildReflectionType(
            errorDescriptor,
            dispatchReceiver = null,

            expectedType,
            callComponents.builtIns,
        )

        return CallableReferenceResolutionCandidate(
            errorDescriptor, dispatchReceiver = null,
            ExplicitReceiverKind.NO_EXPLICIT_RECEIVER, reflectionCandidateType, callableReferenceAdaptation,
            cangjieCall, expectedType, callComponents, scopeTower, resolutionCallbacks, baseSystem
        )
    }
}
