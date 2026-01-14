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

import org.cangnova.cangjie.config.LanguageVersionSettings
import org.cangnova.cangjie.descriptors.CallableDescriptor
import org.cangnova.cangjie.descriptors.ReceiverParameterDescriptor
import org.cangnova.cangjie.resolve.calls.model.*
import org.cangnova.cangjie.types.UnwrappedType
import org.cangnova.cangjie.types.checker.CangJieTypeChecker
import org.cangnova.cangjie.types.expandIntersectionTypeIfNecessary
import org.cangnova.cangjie.types.makeOptionAsSpecified


/**
 * 附加诊断报告器
 *
 * 负责为已解析的函数调用报告附加的诊断信息，特别是智能转换（smart cast）相关的诊断。
 * 这是诊断系统的早期实现组件，未来计划将所有诊断处理集成到 DiagnosticReporterByTrackingStrategy 中。
 *
 * **智能转换诊断**：
 * 智能转换是指编译器通过控制流分析自动推断出变量的更精确类型。
 * 当需要智能转换但实际类型与期望类型不匹配时，需要报告相应的诊断信息。
 *
 * **主要职责**：
 * 1. 检测是否需要智能转换才能使调用成功
 * 2. 验证智能转换的安全性（避免不稳定的智能转换）
 * 3. 避免重复报告已经存在的错误（如 UnsafeCallError）
 *
 * **诊断场景示例**：
 * ```
 * let x: Any = "hello"
 * x.length  // 需要智能转换 x 从 Any 到 String
 * ```
 *
 * **TODO**：
 * - 将所有诊断处理移到 DiagnosticReporterByTrackingStrategy
 * - 将此组件移到前端模块
 *
 * @property languageVersionSettings 语言版本设置，用于版本相关的行为控制
 */
class AdditionalDiagnosticReporter(
    private val languageVersionSettings: LanguageVersionSettings
) {

    /**
     * 报告附加诊断信息
     *
     * 这是报告附加诊断的主入口方法。当前主要报告智能转换相关的诊断。
     *
     * 调用时机：
     * - 在调用解析完成后
     * - 在基本的类型检查完成后
     * - 需要补充报告智能转换等附加信息时
     *
     * @param candidate 已解析的调用原子
     * @param resultingDescriptor 解析得到的可调用描述符（函数、属性等）
     * @param cangjieDiagnosticsHolder 诊断信息持有者，用于收集诊断
     * @param diagnostics 已有的诊断信息集合，用于避免重复报告
     */
    fun reportAdditionalDiagnostics(
        candidate: ResolvedCallAtom,
        resultingDescriptor: CallableDescriptor,
        cangjieDiagnosticsHolder: CangJieDiagnosticsHolder,
        diagnostics: Collection<CangJieCallDiagnostic>
    ) {
        reportSmartCasts(candidate, resultingDescriptor, cangjieDiagnosticsHolder, diagnostics)
    }

    /**
     * 创建智能转换诊断
     *
     * 检查参数是否需要智能转换才能满足期望类型，并创建相应的诊断信息。
     *
     * **检查逻辑**：
     * 1. 只处理表达式参数（非类型参数、非其他特殊参数）
     * 2. 展开期望类型中的交集类型（如 `A & B` 展开为 `[A, B]`）
     * 3. 检查实际参数类型是否为期望类型的子类型
     * 4. 如果已经是子类型，则不需要智能转换，返回 null
     * 5. 否则创建智能转换诊断，指示需要将参数转换为期望类型
     *
     * **示例**：
     * ```
     * fun foo(x: String) { }
     * val y: Any = "hello"
     * foo(y)  // 需要智能转换 y 从 Any 到 String
     * ```
     *
     * @param candidate 调用候选
     * @param argument 需要检查的参数
     * @param expectedResultType 期望的结果类型
     * @return 智能转换诊断，如果不需要智能转换则返回 null
     */
    private fun createSmartCastDiagnostic(
        candidate: ResolvedCallAtom,
        argument: CangJieCallArgument,
        expectedResultType: UnwrappedType
    ): SmartCastDiagnostic? {
        if (argument !is ExpressionCangJieCallArgument) return null

        val types = expectedResultType.expandIntersectionTypeIfNecessary()

        val argumentType = argument.receiver.receiverValue.type
        val isSubtype = types.map { CangJieTypeChecker.DEFAULT.isSubtypeOf(argumentType, it) }
        if (isSubtype.any { it }) return null

        return SmartCastDiagnostic(argument, types.first().unwrap(), candidate.atom)
    }

    /**
     * 报告接收者上的智能转换
     *
     * 检查函数调用的接收者是否需要智能转换，并创建相应的诊断信息。
     * 此方法专门处理调度接收者（dispatch receiver）和扩展接收者（extension receiver）。
     *
     * **处理逻辑**：
     * 1. 检查接收者和参数是否都存在（任一为 null 则无需处理）
     * 2. 根据是否为安全调用（`?.`）调整期望类型：
     *    - 安全调用：将期望类型转换为可选类型（Option）
     *    - 普通调用：使用原始期望类型
     * 3. 创建智能转换诊断
     * 4. 过滤已有的诊断，避免重复报告：
     *    - 如果已有 UnsafeCallError（不安全调用错误），不再报告智能转换
     *    - 如果已有 UnstableSmartCast（不稳定智能转换），不再报告智能转换
     *
     * **安全调用示例**：
     * ```
     * val x: Any? = getSomething()
     * x?.length  // 安全调用，期望类型应为 String?
     * ```
     *
     * **错误去重原理**：
     * 避免对同一个问题报告多个诊断信息，优先报告更具体的错误类型。
     *
     * @param candidate 调用候选
     * @param receiver 接收者参数（可能为 null）
     * @param parameter 接收者参数描述符（可能为 null）
     * @param diagnostics 已有的诊断信息集合
     * @return 智能转换诊断，如果不需要或已有其他诊断则返回 null
     */
    private fun reportSmartCastOnReceiver(
        candidate: ResolvedCallAtom,
        receiver: SimpleCangJieCallArgument?,
        parameter: ReceiverParameterDescriptor?,
        diagnostics: Collection<CangJieCallDiagnostic>
    ): SmartCastDiagnostic? {
        if (receiver == null || parameter == null) return null
        val expectedType =
            parameter.type.unwrap().let { if (receiver.isSafeCall) it.makeOptionAsSpecified(true) else it }

        val smartCastDiagnostic = createSmartCastDiagnostic(candidate, receiver, expectedType.unwrap()) ?: return null

        // todo may be we have smart cast to Int?
        return smartCastDiagnostic.takeIf {
            diagnostics.filterIsInstance<UnsafeCallError>().none {
                it.receiver == receiver
            }
                    &&
                    diagnostics.filterIsInstance<UnstableSmartCast>().none {
                        it.argument == receiver
                    }
        }
    }

    /**
     * 报告智能转换诊断
     *
     * 这是智能转换诊断报告的主要工作方法，负责检查调用的所有参数和接收者，
     * 并为需要智能转换的位置创建诊断信息。
     *
     * **处理流程**：
     * 1. **报告调度接收者的智能转换**：
     *    - 检查候选调用的调度接收者（dispatch receiver）
     *    - 与结果描述符的调度接收者参数进行匹配
     *    - 如果需要智能转换，添加到诊断持有者
     *
     * 2. **报告值参数的智能转换**：
     *    - 遍历所有值参数（value parameters）
     *    - 获取每个参数的参数映射（原始参数到实际参数）
     *    - 对每个实际参数：
     *      a. 计算有效期望类型（考虑语言版本设置）
     *      b. 创建智能转换诊断
     *      c. 检查是否已有不稳定智能转换错误
     *      d. 如果没有重复错误，添加诊断
     *
     * **去重策略**：
     * - 对于调度接收者：通过 `addDiagnosticIfNotNull` 避免添加 null 诊断
     * - 对于值参数：检查是否已存在 UnstableSmartCast 错误，避免重复报告
     *
     * **语言版本考虑**：
     * 使用 `argument.getExpectedType(parameter, languageVersionSettings)` 获取期望类型，
     * 确保类型检查符合当前语言版本的语义。
     *
     * @param candidate 已解析的调用原子
     * @param resultingDescriptor 解析得到的可调用描述符（函数、属性等）
     * @param cangjieDiagnosticsHolder 诊断信息持有者，用于收集诊断
     * @param diagnostics 已有的诊断信息集合，用于避免重复报告
     */
    private fun reportSmartCasts(
        candidate: ResolvedCallAtom,
        resultingDescriptor: CallableDescriptor,
        cangjieDiagnosticsHolder: CangJieDiagnosticsHolder,
        diagnostics: Collection<CangJieCallDiagnostic>
    ) {
        cangjieDiagnosticsHolder.addDiagnosticIfNotNull(
            reportSmartCastOnReceiver(
                candidate,
                candidate.dispatchReceiverArgument,
                resultingDescriptor.dispatchReceiverParameter,
                diagnostics
            )
        )

        for (parameter in resultingDescriptor.valueParameters) {
            for (argument in candidate.argumentMappingByOriginal[parameter.original]?.arguments ?: continue) {
                val effectiveExpectedType = argument.getExpectedType(parameter, languageVersionSettings)
                val smartCastDiagnostic =
                    createSmartCastDiagnostic(candidate, argument, effectiveExpectedType) ?: continue

                val thereIsUnstableSmartCastError = diagnostics.filterIsInstance<UnstableSmartCast>().any {
                    it.argument == argument
                }

                if (!thereIsUnstableSmartCastError) {
                    cangjieDiagnosticsHolder.addDiagnostic(smartCastDiagnostic)
                }
            }
        }
    }
}
