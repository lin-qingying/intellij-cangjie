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

import org.cangnova.cangjie.resolve.calls.inference.ConstraintSystemBuilder
import org.cangnova.cangjie.resolve.calls.inference.components.AbstractTypeSubstitutor
import org.cangnova.cangjie.resolve.calls.model.*
import org.cangnova.cangjie.resolve.calls.tower.VisibilityError
import org.cangnova.cangjie.resolve.calls.tower.VisibilityErrorOnArgument
import org.cangnova.cangjie.resolve.calls.tower.isInapplicable

/**
 * 可调用引用参数解析器
 *
 * 负责处理函数调用中的可调用引用参数（如函数引用、属性引用等）。
 * 该解析器将可调用引用参数与其候选符号进行匹配，处理重载解析，
 * 并将解析结果和约束添加到类型推导系统中。
 *
 * ## 核心功能
 *
 * 1. **候选解析**：通过 [CangJieResolutionCallbacks] 获取可调用引用的候选符号
 * 2. **重载解析**：使用 [CallableReferenceOverloadConflictResolver] 处理多个候选的情况
 * 3. **约束生成**：为选中的候选添加类型约束到约束系统
 * 4. **诊断收集**：收集兼容性警告、可见性错误等诊断信息
 * 5. **延迟解析**：对于有歧义的情况，转换为延迟解析原子
 *
 * ## 工作流程
 *
 * ```
 * 可调用引用参数
 *   ↓
 * 1. 获取期望类型（经过类型替换）
 *   ↓
 * 2. 解析候选符号
 *   ↓
 * 3. 检查候选数量
 *   ├─ 0 个 → 报告 NoneCallableReferenceCallCandidates 错误
 *   ├─ 1 个 → 添加约束和诊断
 *   └─ 多个 → 处理重载解析或转为延迟解析
 *   ↓
 * 4. 设置解析结果
 * ```
 *
 * ## 延迟解析处理
 *
 * 当满足以下条件时，可调用引用会被转换为延迟解析：
 * - 存在多个候选符号
 * - 原子是 [EagerCallableReferenceAtom]（急切解析模式）
 * - 所有候选都不适用时会报告歧义错误
 *
 * @property callableReferenceOverloadConflictResolver 可调用引用重载冲突解析器，用于处理多个候选的情况
 *
 * @see CallableReferenceOverloadConflictResolver
 * @see ResolvedCallableReferenceArgumentAtom
 * @see CangJieResolutionCallbacks.resolveCallableReferenceArgument
 */
class CallableReferenceArgumentResolver(val callableReferenceOverloadConflictResolver: CallableReferenceOverloadConflictResolver) {

    /**
     * 处理可调用引用参数
     *
     * 这是解析器的主入口，负责完整的可调用引用参数解析流程。
     *
     * ## 解析步骤
     *
     * 1. **类型替换**：获取期望类型并应用当前的类型替换器
     * 2. **候选解析**：调用回调获取所有可能的候选符号
     * 3. **多候选处理**：
     *    - 如果候选数 > 1 且是急切解析模式，转为延迟解析
     *    - 如果所有候选都不适用，报告歧义错误
     * 4. **单候选处理**：
     *    - 创建新类型变量替换器
     *    - 添加类型约束到约束系统
     *    - 转换并收集诊断信息（兼容性警告、可见性错误等）
     * 5. **错误处理**：
     *    - 无候选：报告 [NoneCallableReferenceCallCandidates]
     *    - 多候选：报告 [CallableReferenceCallCandidatesAmbiguity]
     * 6. **构建子参数**：从 LHS 结果构建已解析的子参数
     * 7. **设置结果**：将选中的候选和子参数设置到解析原子中
     *
     * ## 诊断转换
     *
     * 候选的诊断会被转换为参数级别的诊断：
     * - [CompatibilityWarning] → [CompatibilityWarningOnArgument]
     * - [VisibilityError] → [VisibilityErrorOnArgument]
     *
     * @param csBuilder 约束系统构建器，用于添加类型约束和进行类型替换
     * @param resolvedAtom 已解析的可调用引用参数原子，包含参数信息和期望类型
     * @param diagnosticsHolder 诊断收集器，用于收集解析过程中的错误和警告
     * @param resolutionCallbacks 解析回调接口，用于获取候选符号
     *
     * @see ResolvedCallableReferenceArgumentAtom
     * @see EagerCallableReferenceAtom
     * @see CreateFreshVariablesSubstitutor.createToFreshVariableSubstitutorAndAddInitialConstraints
     */
    fun processCallableReferenceArgument(
        csBuilder: ConstraintSystemBuilder,
        resolvedAtom: ResolvedCallableReferenceArgumentAtom,
        diagnosticsHolder: CangJieDiagnosticsHolder,
        resolutionCallbacks: CangJieResolutionCallbacks
    ) {
        // 获取原始参数
        val argument = resolvedAtom.atom

        // 应用当前类型替换器获取实际期望类型
        val expectedType = resolvedAtom.expectedType?.let {
            (csBuilder.buildCurrentSubstitutor() as AbstractTypeSubstitutor).safeSubstitute(it)
        }

        // 解析可调用引用，获取所有候选符号
        val candidates = resolutionCallbacks.resolveCallableReferenceArgument(
            resolvedAtom.atom,
            expectedType,
            csBuilder.currentStorage()
        )

        // 处理多候选的急切解析情况
        // 如果有多个候选且是急切解析模式，需要转为延迟解析
        if (candidates.size > 1 && resolvedAtom is EagerCallableReferenceAtom) {
            // 如果所有候选都不适用，报告歧义错误
            if (candidates.all { it.resultingApplicability.isInapplicable }) {
                diagnosticsHolder.addDiagnostic(CallableReferenceCallCandidatesAmbiguity(argument, candidates))
            }

            // 转换为延迟解析原子，推迟到后续阶段处理
            resolvedAtom.setAnalyzedResults(
                candidate = null,
                subResolvedAtoms = listOf(resolvedAtom.transformToPostponed())
            )
            return
        }

        // 尝试获取唯一候选
        val chosenCandidate = candidates.singleOrNull()
        if (chosenCandidate != null) {
            // 为候选创建新的类型变量替换器，并添加初始约束
            val toFreshSubstitutor =
                CreateFreshVariablesSubstitutor.createToFreshVariableSubstitutorAndAddInitialConstraints(
                    chosenCandidate.candidate,
                    resolvedAtom.atom.call,
                    csBuilder
                )

            // 将候选的约束添加到约束系统中
            chosenCandidate.addConstraints(csBuilder, toFreshSubstitutor, callableReference = argument)

            // 转换并收集候选的诊断信息
            // 需要将候选级别的诊断转换为参数级别的诊断
            chosenCandidate.diagnostics.forEach {
                val transformedDiagnostic = when (it) {
                    is CompatibilityWarning -> CompatibilityWarningOnArgument(argument, it.candidate)
                    is VisibilityError -> VisibilityErrorOnArgument(argument, it.invisibleMember)
                    else -> it
                }
                diagnosticsHolder.addDiagnostic(transformedDiagnostic)
            }

            // 设置新变量替换器到候选中
            chosenCandidate.freshVariablesSubstitutor = toFreshSubstitutor
        } else {
            // 处理无候选或多候选的错误情况
            if (candidates.isEmpty()) {
                // 没有找到任何候选符号
                diagnosticsHolder.addDiagnostic(NoneCallableReferenceCallCandidates(argument))
            } else {
                // 有多个候选但无法选择（非急切解析模式的多候选情况）
                diagnosticsHolder.addDiagnostic(CallableReferenceCallCandidatesAmbiguity(argument, candidates))
            }
        }

        // 构建子参数列表
        // TODO: 将此逻辑移到 CallableReferencesCandidateFactory 中创建
        val subCjArguments = listOfNotNull(buildResolvedCjArgument(argument.lhsResult))

        // 设置解析结果到原子中
        resolvedAtom.setAnalyzedResults(chosenCandidate, subCjArguments)
    }

    /**
     * 从 LHS 结果构建已解析的仓颉调用参数
     *
     * 将可调用引用的左侧结果转换为已解析的参数原子。
     * 这个方法处理可调用引用中的接收者或限定符部分。
     *
     * ## 处理逻辑
     *
     * - 如果 LHS 结果不是表达式类型，返回 null
     * - 如果是 [SubCangJieCallArgument]，返回其调用结果
     * - 如果是 [ExpressionCangJieCallArgument]，创建 [ResolvedExpressionAtom]
     * - 其他类型抛出 [unexpectedArgument] 异常
     *
     * ## 示例
     *
     * ```kotlin
     * // 对于可调用引用 String::length
     * // lhsResult 是 LHSResult.Expression，包含 String 类型信息
     * // 返回一个 ResolvedExpressionAtom 包装该表达式
     * ```
     *
     * @param lhsResult 可调用引用的左侧结果（接收者或限定符）
     * @return 已解析的参数原子，如果 LHS 不是表达式则返回 null
     *
     * @see LHSResult
     * @see ResolvedExpressionAtom
     * @see SubCangJieCallArgument
     * @see ExpressionCangJieCallArgument
     */
    private fun buildResolvedCjArgument(lhsResult: LHSResult): ResolvedAtom? {
        // 只处理表达式类型的 LHS 结果
        if (lhsResult !is LHSResult.Expression) return null

        // 根据调用参数类型构建相应的已解析原子
        return when (val lshCallArgument = lhsResult.lshCallArgument) {
            // 子调用参数：直接返回其调用结果
            is SubCangJieCallArgument -> lshCallArgument.callResult
            // 表达式参数：包装为已解析表达式原子
            is ExpressionCangJieCallArgument -> ResolvedExpressionAtom(lshCallArgument)
            // 未预期的参数类型
            else -> unexpectedArgument(lshCallArgument)
        }
    }
}
