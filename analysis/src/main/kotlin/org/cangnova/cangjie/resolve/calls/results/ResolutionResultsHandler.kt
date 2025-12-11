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

package org.cangnova.cangjie.resolve.calls.results

import com.intellij.diff.comparison.CancellationChecker
import org.cangnova.cangjie.builtins.CangJieBuiltIns
import org.cangnova.cangjie.config.LanguageFeature
import org.cangnova.cangjie.config.LanguageVersionSettings
import org.cangnova.cangjie.descriptors.CallableDescriptor
import org.cangnova.cangjie.descriptors.ModuleDescriptor
import org.cangnova.cangjie.resolve.binding.BindingTrace
import org.cangnova.cangjie.resolve.calls.context.CallResolutionContext
import org.cangnova.cangjie.resolve.calls.context.CheckArgumentTypesMode
import org.cangnova.cangjie.resolve.calls.model.MutableResolvedCall
import org.cangnova.cangjie.resolve.calls.model.ResolvedCall
import org.cangnova.cangjie.resolve.calls.results.ResolutionStatus.*
import org.cangnova.cangjie.resolve.calls.results.ResolutionStatus.Companion.SEVERITY_LEVELS
import org.cangnova.cangjie.resolve.calls.tasks.TracingStrategy
import org.cangnova.cangjie.resolve.calls.tower.isSynthesized
import org.cangnova.cangjie.resolve.calls.util.hasUnresolvedArguments
import org.cangnova.cangjie.types.checker.CangJieTypeRefiner

/**
 * 解析结果处理器
 *
 * 负责处理函数/属性调用的重载解析结果，包括成功、失败、歧义等各种情况。
 * 使用重载冲突解析器来选择最具体的候选。
 *
 * @property overloadingConflictResolver 重载冲突解析器
 */
class ResolutionResultsHandler(
    builtIns: CangJieBuiltIns,
    module: ModuleDescriptor,
    specificityComparator: TypeSpecificityComparator,
    platformOverloadsSpecificityComparator: PlatformOverloadsSpecificityComparator,
    cancellationChecker: CancellationChecker,
    cangjieTypeRefiner: CangJieTypeRefiner
) {

    private val overloadingConflictResolver: OverloadingConflictResolver<ResolvedCall<*>> =
        createOverloadingConflictResolver(
            builtIns,
            module,
            specificityComparator,
            platformOverloadsSpecificityComparator,
            cancellationChecker,
            cangjieTypeRefiner
        )

    /**
     * 计算解析结果并报告错误
     *
     * 这是主要的入口方法，负责处理所有候选调用并生成最终的解析结果。
     *
     * @param context 调用解析上下文
     * @param tracing 追踪策略
     * @param candidates 所有候选调用集合
     * @param languageVersionSettings 语言版本设置
     * @return 重载解析结果
     */
    fun <D : CallableDescriptor> computeResultAndReportErrors(
        context: CallResolutionContext<*>,
        tracing: TracingStrategy,
        candidates: Collection<MutableResolvedCall<D>>,
        languageVersionSettings: LanguageVersionSettings
    ): OverloadResolutionResultsImpl<D> {
        // 按状态分类候选
        val successfulCandidates = LinkedHashSet<MutableResolvedCall<D>>()
        val failedCandidates = LinkedHashSet<MutableResolvedCall<D>>()
        val incompleteCandidates = LinkedHashSet<MutableResolvedCall<D>>()
        val candidatesWithWrongReceiver = LinkedHashSet<MutableResolvedCall<D>>()

        for (candidateCall in candidates) {
            val status = candidateCall.status
            check(status != UNKNOWN_STATUS) { "No resolution for ${candidateCall.candidateDescriptor}" }

            when {
                status.isSuccess -> successfulCandidates.add(candidateCall)
                status == INCOMPLETE_TYPE_INFERENCE -> incompleteCandidates.add(candidateCall)
                candidateCall.status == RECEIVER_TYPE_ERROR -> candidatesWithWrongReceiver.add(candidateCall)
                candidateCall.status != RECEIVER_PRESENCE_ERROR -> failedCandidates.add(candidateCall)
            }
        }

        // TODO: 也许最好先过滤掉重写，然后再寻找最具体的
        return when {
            successfulCandidates.isNotEmpty() || incompleteCandidates.isNotEmpty() -> {
                computeSuccessfulResult(
                    context,
                    tracing,
                    successfulCandidates,
                    incompleteCandidates,
                    context.checkArguments,
                    languageVersionSettings
                )
            }

            failedCandidates.isNotEmpty() -> {
                computeFailedResult(
                    tracing,
                    context.trace,
                    failedCandidates,
                    context.checkArguments,
                    languageVersionSettings
                )
            }

            candidatesWithWrongReceiver.isNotEmpty() -> {
                tracing.unresolvedReferenceWrongReceiver(context.trace, candidatesWithWrongReceiver)
                OverloadResolutionResultsImpl.candidatesWithWrongReceiver(candidatesWithWrongReceiver)
            }

            else -> {
                tracing.unresolvedReference(context.trace)
                OverloadResolutionResultsImpl.nameNotFound()
            }
        }
    }

    /**
     * 计算成功的解析结果
     *
     * 处理成功和不完整的候选，选择最具体的候选。
     *
     * @param context 调用解析上下文
     * @param tracing 追踪策略
     * @param successfulCandidates 成功的候选集合
     * @param incompleteCandidates 不完整的候选集合
     * @param checkArgumentsMode 参数检查模式
     * @param languageVersionSettings 语言版本设置
     * @return 重载解析结果
     */
    private fun <D : CallableDescriptor> computeSuccessfulResult(
        context: CallResolutionContext<*>,
        tracing: TracingStrategy,
        successfulCandidates: Set<MutableResolvedCall<D>>,
        incompleteCandidates: Set<MutableResolvedCall<D>>,
        checkArgumentsMode: CheckArgumentTypesMode,
        languageVersionSettings: LanguageVersionSettings
    ): OverloadResolutionResultsImpl<D> {
        val successfulAndIncomplete = LinkedHashSet<MutableResolvedCall<D>>().apply {
            addAll(successfulCandidates)
            addAll(incompleteCandidates)
        }

        val results = chooseAndReportMaximallySpecific(
            successfulAndIncomplete,
            discriminateGenerics = true,
            checkArgumentsMode,
            languageVersionSettings
        )

        if (results.isSingleResult) {
            val resultingCall = results.resultingCall
            resultingCall.trace.moveAllMyDataTo(context.trace)
            if (resultingCall.status == INCOMPLETE_TYPE_INFERENCE) {
                return OverloadResolutionResultsImpl.incompleteTypeInference(resultingCall)
            }
        }

        if (results.isAmbiguity) {
            tracing.recordAmbiguity(context.trace, results.resultingCalls)
            val allCandidatesIncomplete = allIncomplete(results.resultingCalls)

            // 检查是否需要报告歧义
            // 这个检查用于以下情况：
            //   x.foo(unresolved) -- 如果有多个 foo，我们会报告歧义，但在这里没有意义
            if (context.checkArguments != CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS ||
                !context.call.hasUnresolvedArguments(context)
            ) {
                if (allCandidatesIncomplete) {
                    tracing.cannotCompleteResolve(context.trace, results.resultingCalls)
                } else {
                    tracing.ambiguity(context.trace, results.resultingCalls)
                }
            }

            if (allCandidatesIncomplete) {
                return OverloadResolutionResultsImpl.incompleteTypeInference(results.resultingCalls)
            }
        }

        return results
    }

    /**
     * 计算失败的解析结果
     *
     * 处理所有失败的候选，按严重级别选择最具体的失败原因。
     *
     * @param tracing 追踪策略
     * @param trace 绑定追踪
     * @param failedCandidates 失败的候选集合
     * @param checkArgumentsMode 参数检查模式
     * @param languageVersionSettings 语言版本设置
     * @return 重载解析结果
     */
    private fun <D : CallableDescriptor> computeFailedResult(
        tracing: TracingStrategy,
        trace: BindingTrace,
        failedCandidates: Set<MutableResolvedCall<D>>,
        checkArgumentsMode: CheckArgumentTypesMode,
        languageVersionSettings: LanguageVersionSettings
    ): OverloadResolutionResultsImpl<D> {
        if (failedCandidates.size == 1) {
            return recordFailedInfo(tracing, trace, failedCandidates)
        }

        // 按严重级别处理失败的候选
        for (severityLevel in SEVERITY_LEVELS) {
            val thisLevel = LinkedHashSet<MutableResolvedCall<D>>()
            for (candidate in failedCandidates) {
                if (severityLevel.contains(candidate.status)) {
                    thisLevel.add(candidate)
                }
            }

            if (thisLevel.isNotEmpty()) {
                if (severityLevel.contains(ARGUMENTS_MAPPING_ERROR)) {
                    @Suppress("UNCHECKED_CAST")
                    val myResolver = overloadingConflictResolver as OverloadingConflictResolver<MutableResolvedCall<D>>
                    return recordFailedInfo(tracing, trace, myResolver.filterOutEquivalentCalls(LinkedHashSet(thisLevel)))
                }

                val results = chooseAndReportMaximallySpecific(
                    thisLevel,
                    discriminateGenerics = false,
                    checkArgumentsMode,
                    languageVersionSettings
                )
                return recordFailedInfo(tracing, trace, results.resultingCalls)
            }
        }

        error("Should not be reachable, cause every status must belong to some level: $failedCandidates")
    }

    /**
     * 选择并报告最具体的候选
     *
     * 使用重载冲突解析器选择最具体的候选。
     *
     * @param candidates 候选集合
     * @param discriminateGenerics 是否区分泛型
     * @param checkArgumentsMode 参数检查模式
     * @param languageVersionSettings 语言版本设置
     * @return 重载解析结果
     */
    @Suppress("UNCHECKED_CAST")
    private fun <D : CallableDescriptor> chooseAndReportMaximallySpecific(
        candidates: Set<MutableResolvedCall<D>>,
        discriminateGenerics: Boolean,
        checkArgumentsMode: CheckArgumentTypesMode,
        languageVersionSettings: LanguageVersionSettings
    ): OverloadResolutionResultsImpl<D> {
        val myResolver = overloadingConflictResolver as OverloadingConflictResolver<MutableResolvedCall<D>>

        // 过滤掉合成的候选（如果不支持精细的 SAM 适配器优先级）
        var refinedCandidates = candidates
        if (!languageVersionSettings.supportsFeature(LanguageFeature.RefinedSamAdaptersPriority)) {
            val nonSynthesized = candidates.filter { !it.candidateDescriptor.isSynthesized }.toSet()
            if (nonSynthesized.isNotEmpty()) {
                refinedCandidates = nonSynthesized
            }
        }

        // 选择最具体的候选
        var specificCalls = myResolver.chooseMaximallySpecificCandidates(
            refinedCandidates,
            checkArgumentsMode,
            discriminateGenerics
        )

        // 过滤掉带有特定注解的候选
        if (specificCalls.size > 1) {
            specificCalls = specificCalls.filter {
                false
//                !it.candidateDescriptor.annotations.hasAnnotation(
//                    OVERLOAD_RESOLUTION_BY_LAMBDA_ANNOTATION_FQ_NAME
//                )
            }.toSet()
        }

        return when (specificCalls.size) {
            1 -> OverloadResolutionResultsImpl.success(specificCalls.single())
            else -> OverloadResolutionResultsImpl.ambiguity(specificCalls)
        }
    }

    companion object {
        /**
         * 记录失败信息
         *
         * 记录失败的候选信息到追踪中。
         *
         * @param tracing 追踪策略
         * @param trace 绑定追踪
         * @param candidates 候选集合
         * @return 重载解析结果
         */
        private fun <D : CallableDescriptor> recordFailedInfo(
            tracing: TracingStrategy,
            trace: BindingTrace,
            candidates: Collection<MutableResolvedCall<D>>
        ): OverloadResolutionResultsImpl<D> {
            if (candidates.size == 1) {
                val failed = candidates.single()
                failed.trace.moveAllMyDataTo(trace)
                return OverloadResolutionResultsImpl.singleFailedCandidate(failed)
            }

            tracing.noneApplicable(trace, candidates)
            tracing.recordAmbiguity(trace, candidates)
            return OverloadResolutionResultsImpl.manyFailedCandidates(candidates)
        }

        /**
         * 检查是否所有结果都是不完整的
         *
         * @param results 结果集合
         * @return 如果所有结果都是不完整类型推断，返回 true
         */
        private fun <D : CallableDescriptor> allIncomplete(results: Collection<MutableResolvedCall<D>>): Boolean {
            return results.all { it.status == INCOMPLETE_TYPE_INFERENCE }
        }
    }
}
