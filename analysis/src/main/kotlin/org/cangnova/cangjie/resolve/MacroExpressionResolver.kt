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
 */

package org.cangnova.cangjie.resolve

import org.cangnova.cangjie.config.LanguageVersionSettings
import org.cangnova.cangjie.descriptors.ClassifierDescriptor
import org.cangnova.cangjie.descriptors.DeclarationDescriptor
import org.cangnova.cangjie.descriptors.macro.MacroDescriptor
import org.cangnova.cangjie.diagnostics.infos.errors.NONE_APPLICABLE
import org.cangnova.cangjie.diagnostics.infos.errors.OVERLOAD_RESOLUTION_AMBIGUITY
import org.cangnova.cangjie.diagnostics.infos.errors.UNRESOLVED_REFERENCE
import org.cangnova.cangjie.incremental.components.LookupLocation
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.Call
import org.cangnova.cangjie.psi.CjMacroExpression
import org.cangnova.cangjie.resolve.binding.BindingContext
import org.cangnova.cangjie.resolve.binding.BindingTrace
import org.cangnova.cangjie.resolve.calls.model.ResolvedCall
import org.cangnova.cangjie.resolve.calls.model.ResolvedMacroCall
import org.cangnova.cangjie.resolve.calls.results.ManyCandidates
import org.cangnova.cangjie.resolve.calls.results.NameNotFoundResolutionResult
import org.cangnova.cangjie.resolve.calls.results.OverloadResolutionResults
import org.cangnova.cangjie.resolve.calls.results.ResolutionStatus
import org.cangnova.cangjie.resolve.calls.results.SingleOverloadResolutionResult
import org.cangnova.cangjie.resolve.calls.util.CallMaker
import org.cangnova.cangjie.resolve.qualified.QualifierPosition
import org.cangnova.cangjie.resolve.qualified.validation.VisibilityChecker
import org.cangnova.cangjie.resolve.scopes.HierarchicalScope
import org.cangnova.cangjie.resolve.scopes.collectMacros
import org.cangnova.cangjie.resolve.scopes.findClassifier

/**
 * 宏表达式解析器
 *
 * 封装宏调用的完整解析流程：候选收集、可见性过滤、参数匹配、歧义检测、错误诊断。
 * 统一 BasicExpressionTypingVisitor 和 ResolveElementCache 中的宏解析逻辑，消除代码重复。
 */
class MacroExpressionResolver {

    /**
     * 宏解析结果（密封类）
     */
    sealed class MacroResolutionResult {
        /** 宏解析结果 */
        class MacroResult(
            val results: OverloadResolutionResults<MacroDescriptor>
        ) : MacroResolutionResult()

        /** 回退到注解类 */
        class ClassifierFallback(
            val classifier: ClassifierDescriptor
        ) : MacroResolutionResult()

        /** 未找到 */
        data object NotFound : MacroResolutionResult()
    }

    /**
     * 解析宏表达式
     *
     * @param macroExpression 宏表达式 PSI 节点
     * @param macroName 宏名称
     * @param scope 作用域
     * @param ownerDescriptor 所属声明描述符（用于可见性检查）
     * @param location 查找位置
     * @param languageVersionSettings 语言版本设置
     * @return 解析结果
     */
    fun resolve(
        macroExpression: CjMacroExpression,
        macroName: Name,
        scope: HierarchicalScope,
        ownerDescriptor: DeclarationDescriptor,
        location: LookupLocation,
        languageVersionSettings: LanguageVersionSettings
    ): MacroResolutionResult {
        // 1. 收集所有同名宏候选
        val allCandidates = scope.collectMacros(macroName, location)

        if (allCandidates.isEmpty()) {
            // 没有宏候选，回退到注解类查找
            val classifier = scope.findClassifier(macroName, location)
            return if (classifier != null) {
                MacroResolutionResult.ClassifierFallback(classifier)
            } else {
                MacroResolutionResult.NotFound
            }
        }

        // 2. 可见性过滤
        val visibleCandidates = allCandidates.filter { descriptor ->
            VisibilityChecker.isVisible(
                descriptor,
                ownerDescriptor,
                QualifierPosition.EXPRESSION,
                languageVersionSettings
            )
        }

        val call = makeMacroCall(macroExpression)

        if (visibleCandidates.isEmpty()) {
            // 所有候选都不可见，报告参数映射错误（使用第一个候选）
            val resolvedCall = ResolvedMacroCall(
                allCandidates.first(),
                call,
                ResolutionStatus.INVISIBLE_MEMBER_ERROR
            )
            return MacroResolutionResult.MacroResult(
                SingleOverloadResolutionResult(resolvedCall)
            )
        }

        // 3. 参数数量匹配
        val callArgCount = listOfNotNull(macroExpression.attr, macroExpression.input).size
        val matched = visibleCandidates.filter { it.valueParameters.size == callArgCount }

        return when {
            // 恰好匹配一个 → 成功
            matched.size == 1 -> {
                val resolvedCall = ResolvedMacroCall(matched.single(), call, ResolutionStatus.SUCCESS)
                MacroResolutionResult.MacroResult(
                    SingleOverloadResolutionResult(resolvedCall)
                )
            }
            // 匹配多个 → 歧义
            matched.size > 1 -> {
                val resolvedCalls = matched.map { ResolvedMacroCall(it, call, ResolutionStatus.SUCCESS) }
                MacroResolutionResult.MacroResult(
                    ManyCandidates(resolvedCalls)
                )
            }
            // 无匹配但有可见候选 → 参数不匹配
            visibleCandidates.size == 1 -> {
                val resolvedCall = ResolvedMacroCall(
                    visibleCandidates.single(),
                    call,
                    ResolutionStatus.ARGUMENTS_MAPPING_ERROR
                )
                MacroResolutionResult.MacroResult(
                    SingleOverloadResolutionResult(resolvedCall)
                )
            }
            else -> {
                val resolvedCalls = visibleCandidates.map {
                    ResolvedMacroCall(it, call, ResolutionStatus.ARGUMENTS_MAPPING_ERROR)
                }
                MacroResolutionResult.MacroResult(
                    ManyCandidates(resolvedCalls)
                )
            }
        }
    }

    /**
     * 将解析结果记录到 BindingTrace
     */
    fun recordResults(
        trace: BindingTrace,
        macroExpression: CjMacroExpression,
        result: MacroResolutionResult
    ) {
        when (result) {
            is MacroResolutionResult.MacroResult -> {
                trace.record(BindingContext.RESOLVED_MACRO_CALL, macroExpression, result.results)

                // 兼容现有代码：记录 MACRO slice 和 REFERENCE_TARGET
                if (result.results.isSingleResult) {
                    val descriptor = result.results.resultingDescriptor
                    trace.record(BindingContext.MACRO, macroExpression, descriptor)
                    macroExpression.referenceExpression?.let { refExpr ->
                        trace.record(BindingContext.REFERENCE_TARGET, refExpr, descriptor)
                    }
                } else if (!result.results.isNothing && result.results.resultingCalls.isNotEmpty()) {
                    // 歧义或失败时记录第一个候选
                    val firstDescriptor = result.results.resultingCalls.first().candidateDescriptor
                    trace.record(BindingContext.MACRO, macroExpression, firstDescriptor)
                    macroExpression.referenceExpression?.let { refExpr ->
                        trace.record(BindingContext.REFERENCE_TARGET, refExpr, firstDescriptor)
                    }
                }
            }
            is MacroResolutionResult.ClassifierFallback -> {
                macroExpression.referenceExpression?.let { refExpr ->
                    trace.record(BindingContext.REFERENCE_TARGET, refExpr, result.classifier)
                }
            }
            is MacroResolutionResult.NotFound -> {
                // 不记录任何结果
            }
        }
    }

    /**
     * 根据解析结果报告诊断信息
     */
    fun reportDiagnostics(
        trace: BindingTrace,
        macroExpression: CjMacroExpression,
        result: MacroResolutionResult
    ) {
        when (result) {
            is MacroResolutionResult.MacroResult -> {
                val code = result.results.resultCode
                when (code) {
                    OverloadResolutionResults.Code.AMBIGUITY -> {
                        val reportOn = macroExpression.referenceExpression ?: macroExpression
                        trace.report(
                            OVERLOAD_RESOLUTION_AMBIGUITY.on(
                                reportOn,
                                result.results.resultingCalls as Collection<ResolvedCall<*>>
                            )
                        )
                    }
                    OverloadResolutionResults.Code.SINGLE_CANDIDATE_ARGUMENT_MISMATCH,
                    OverloadResolutionResults.Code.MANY_FAILED_CANDIDATES -> {
                        val reportOn = macroExpression.referenceExpression ?: macroExpression
                        trace.report(
                            NONE_APPLICABLE.on(
                                reportOn,
                                result.results.resultingCalls as Collection<ResolvedCall<*>>
                            )
                        )
                    }
                    OverloadResolutionResults.Code.CANDIDATES_WITH_WRONG_RECEIVER -> {
                        // 不可见的候选——报告未解析引用
                        val referenceExpression = macroExpression.referenceExpression
                        if (referenceExpression != null) {
                            trace.report(UNRESOLVED_REFERENCE.on(referenceExpression, referenceExpression))
                        }
                    }
                    else -> {
                        // SUCCESS 或其他状态，不报告错误
                    }
                }
            }
            is MacroResolutionResult.NotFound -> {
                val referenceExpression = macroExpression.referenceExpression
                if (referenceExpression != null) {
                    trace.report(UNRESOLVED_REFERENCE.on(referenceExpression, referenceExpression))
                }
            }
            is MacroResolutionResult.ClassifierFallback -> {
                // 注解类回退成功，不报告错误
            }
        }
    }

    /**
     * 为宏表达式构建 Call 对象
     */
    private fun makeMacroCall(macroExpression: CjMacroExpression): Call {
        val calleeExpression = macroExpression.referenceExpression ?: macroExpression
        val argumentExpressions = listOfNotNull(macroExpression.attr, macroExpression.input)
        return CallMaker.makeCallWithExpressions(
            macroExpression,
            null,
            null,
            calleeExpression,
            argumentExpressions
        )
    }
}
