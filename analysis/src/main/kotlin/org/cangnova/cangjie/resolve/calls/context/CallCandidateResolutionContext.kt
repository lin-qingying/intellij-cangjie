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

package org.cangnova.cangjie.resolve.calls.context

import org.cangnova.cangjie.config.LanguageVersionSettings
import org.cangnova.cangjie.descriptors.BindingTrace
import org.cangnova.cangjie.descriptors.CallableDescriptor
import org.cangnova.cangjie.psi.Call
import org.cangnova.cangjie.psi.CjExpression
import org.cangnova.cangjie.resolve.StatementFilter
import org.cangnova.cangjie.resolve.calls.components.InferenceSession
import org.cangnova.cangjie.resolve.calls.model.MutableDataFlowInfoForArguments
import org.cangnova.cangjie.resolve.calls.model.MutableResolvedCall
import org.cangnova.cangjie.resolve.calls.smartcasts.DataFlowInfo
import org.cangnova.cangjie.resolve.calls.smartcasts.DataFlowValueFactory
import org.cangnova.cangjie.resolve.calls.tasks.TracingStrategy
import org.cangnova.cangjie.resolve.scopes.LexicalScope
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.expressions.ContextConfig

/**
 * 调用候选解析上下文
 *
 * 用于解析特定调用候选的上下文，包含候选调用、跟踪策略和解析模式等信息。
 *
 * @param D 可调用描述符类型
 */
class CallCandidateResolutionContext<D : CallableDescriptor> private constructor(
    /**
     * 候选调用
     */
    val candidateCall: MutableResolvedCall<D>,
    /**
     * 跟踪策略
     */
    val tracing: TracingStrategy,
    trace: BindingTrace,
    scope: LexicalScope,
    call: Call,
    expectedType: CangJieType,
    dataFlowInfo: DataFlowInfo,
    contextDependency: ContextDependency,
    checkArguments: CheckArgumentTypesMode,
    resolutionResultsCache: ResolutionResultsCache,
    dataFlowInfoForArguments: MutableDataFlowInfoForArguments?,
    statementFilter: StatementFilter,
    /**
     * 候选解析模式
     */
    val candidateResolveMode: CandidateResolveMode,
    isAnnotationContext: Boolean,
    isDebuggerContext: Boolean,
    collectAllCandidates: Boolean,
    isSaveTypeInfo: Boolean,
    callPosition: CallPosition,
    expressionContextProvider: (CjExpression) -> CjExpression?,
    languageVersionSettings: LanguageVersionSettings,
    dataFlowValueFactory: DataFlowValueFactory,
    inferenceSession: InferenceSession,
    config: ContextConfig
) : CallResolutionContext<CallCandidateResolutionContext<D>>(
    trace, scope, call, expectedType, dataFlowInfo, contextDependency, checkArguments,
    resolutionResultsCache, dataFlowInfoForArguments, statementFilter, isAnnotationContext,
    isDebuggerContext, collectAllCandidates, isSaveTypeInfo, callPosition,
    expressionContextProvider, languageVersionSettings, dataFlowValueFactory,
    inferenceSession, config
) {

    override fun create(
        trace: BindingTrace,
        scope: LexicalScope,
        dataFlowInfo: DataFlowInfo,
        expectedType: CangJieType,
        contextDependency: ContextDependency,
        resolutionResultsCache: ResolutionResultsCache,
        statementFilter: StatementFilter,
        collectAllCandidates: Boolean,
        isSaveTypeInfo: Boolean,
        callPosition: CallPosition,
        expressionContextProvider: (CjExpression) -> CjExpression?,
        languageVersionSettings: LanguageVersionSettings,
        dataFlowValueFactory: DataFlowValueFactory,
        inferenceSession: InferenceSession,
        config: ContextConfig
    ): CallCandidateResolutionContext<D> {
        return CallCandidateResolutionContext(
            candidateCall, tracing, trace, scope, call, expectedType, dataFlowInfo,
            contextDependency, checkArguments, resolutionResultsCache, dataFlowInfoForArguments,
            statementFilter, candidateResolveMode, isAnnotationContext, isDebuggerContext,
            collectAllCandidates, isSaveTypeInfo, callPosition, expressionContextProvider,
            languageVersionSettings, dataFlowValueFactory, inferenceSession, config
        )
    }

    companion object {
        /**
         * 创建调用候选解析上下文
         */
        @JvmStatic
        fun <D : CallableDescriptor> create(
            candidateCall: MutableResolvedCall<D>,
            context: CallResolutionContext<*>,
            trace: BindingTrace,
            tracing: TracingStrategy,
            call: Call,
            candidateResolveMode: CandidateResolveMode
        ): CallCandidateResolutionContext<D> {
            return CallCandidateResolutionContext(
                candidateCall, tracing, trace, context.scope, call, context.expectedType,
                context.dataFlowInfo, context.contextDependency, context.checkArguments,
                context.resolutionResultsCache, context.dataFlowInfoForArguments,
                context.statementFilter, candidateResolveMode, context.isAnnotationContext,
                context.isDebuggerContext, context.collectAllCandidates, context.isSaveTypeInfo,
                context.callPosition, context.expressionContextProvider,
                context.languageVersionSettings, context.dataFlowValueFactory,
                context.inferenceSession, context.config
            )
        }

        /**
         * 为正在分析的调用创建上下文
         */
        @JvmStatic
        fun <D : CallableDescriptor> createForCallBeingAnalyzed(
            candidateCall: MutableResolvedCall<D>,
            context: BasicCallResolutionContext,
            tracing: TracingStrategy
        ): CallCandidateResolutionContext<D> {
            return CallCandidateResolutionContext(
                candidateCall, tracing, context.trace, context.scope, context.call,
                context.expectedType, context.dataFlowInfo, context.contextDependency,
                context.checkArguments, context.resolutionResultsCache,
                context.dataFlowInfoForArguments, context.statementFilter,
                CandidateResolveMode.FULLY, context.isAnnotationContext,
                context.isDebuggerContext, context.collectAllCandidates, context.isSaveTypeInfo,
                context.callPosition, context.expressionContextProvider,
                context.languageVersionSettings, context.dataFlowValueFactory,
                context.inferenceSession, context.config
            )
        }
    }
}
