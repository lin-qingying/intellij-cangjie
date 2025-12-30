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
import org.cangnova.cangjie.psi.Call
import org.cangnova.cangjie.psi.CjExpression
import org.cangnova.cangjie.resolve.StatementFilter
import org.cangnova.cangjie.resolve.binding.BindingTrace
import org.cangnova.cangjie.resolve.calls.components.InferenceSession
import org.cangnova.cangjie.resolve.calls.model.MutableDataFlowInfoForArguments
import org.cangnova.cangjie.resolve.calls.smartcasts.DataFlowInfo
import org.cangnova.cangjie.resolve.calls.smartcasts.DataFlowValueFactory
import org.cangnova.cangjie.resolve.scopes.LexicalScope
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.expressions.ContextConfig

/**
 * 基本调用解析上下文
 *
 * 提供调用解析所需的基本上下文信息
 */
class BasicCallResolutionContext private constructor(
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
) : CallResolutionContext<BasicCallResolutionContext>(
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
    ): BasicCallResolutionContext {
        return BasicCallResolutionContext(
            trace, scope, call, expectedType, dataFlowInfo, contextDependency, checkArguments,
            resolutionResultsCache, dataFlowInfoForArguments, statementFilter, isAnnotationContext,
            isDebuggerContext, collectAllCandidates, isSaveTypeInfo, callPosition,
            expressionContextProvider, languageVersionSettings, dataFlowValueFactory,
            inferenceSession, config
        )
    }

    /**
     * 替换调用对象
     */
    fun replaceCall(newCall: Call): BasicCallResolutionContext {
        return BasicCallResolutionContext(
            trace, scope, newCall, expectedType, dataFlowInfo, contextDependency, checkArguments,
            resolutionResultsCache, dataFlowInfoForArguments, statementFilter, isAnnotationContext,
            isDebuggerContext, collectAllCandidates, isSaveTypeInfo, callPosition,
            expressionContextProvider, languageVersionSettings, dataFlowValueFactory,
            inferenceSession, config
        )
    }

    companion object {
        /**
         * 创建基本调用解析上下文
         */
        
        fun create(
            trace: BindingTrace,
            scope: LexicalScope,
            call: Call,
            expectedType: CangJieType,
            dataFlowInfo: DataFlowInfo,
            contextDependency: ContextDependency,
            checkArguments: CheckArgumentTypesMode,
            isAnnotationContext: Boolean,
            languageVersionSettings: LanguageVersionSettings,
            dataFlowValueFactory: DataFlowValueFactory,
            inferenceSession: InferenceSession
        ): BasicCallResolutionContext {
            return BasicCallResolutionContext(
                trace, scope, call, expectedType, dataFlowInfo, contextDependency, checkArguments,
                ResolutionResultsCacheImpl(), null, StatementFilter.NONE, isAnnotationContext,
                false, false, true, CallPosition.Unknown, DEFAULT_EXPRESSION_CONTEXT_PROVIDER,
                languageVersionSettings, dataFlowValueFactory, inferenceSession, ContextConfig.DEFAULT
            )
        }

        /**
         * 创建基本调用解析上下文（使用默认推断会话）
         */
        
        fun create(
            trace: BindingTrace,
            scope: LexicalScope,
            call: Call,
            expectedType: CangJieType,
            dataFlowInfo: DataFlowInfo,
            contextDependency: ContextDependency,
            checkArguments: CheckArgumentTypesMode,
            isAnnotationContext: Boolean,
            languageVersionSettings: LanguageVersionSettings,
            dataFlowValueFactory: DataFlowValueFactory
        ): BasicCallResolutionContext {
            return BasicCallResolutionContext(
                trace, scope, call, expectedType, dataFlowInfo, contextDependency, checkArguments,
                ResolutionResultsCacheImpl(), null, StatementFilter.NONE, isAnnotationContext,
                false, false, true, CallPosition.Unknown, DEFAULT_EXPRESSION_CONTEXT_PROVIDER,
                languageVersionSettings, dataFlowValueFactory, InferenceSession.default,
                ContextConfig.DEFAULT
            )
        }

        /**
         * 从已有上下文创建基本调用解析上下文
         */
        
        @JvmOverloads
        fun create(
            context: ResolutionContext<*>,
            call: Call,
            checkArguments: CheckArgumentTypesMode,
            dataFlowInfoForArguments: MutableDataFlowInfoForArguments? = null
        ): BasicCallResolutionContext {
            return BasicCallResolutionContext(
                context.trace, context.scope, call, context.expectedType, context.dataFlowInfo,
                context.contextDependency, checkArguments, context.resolutionResultsCache,
                dataFlowInfoForArguments, context.statementFilter, context.isAnnotationContext,
                context.isDebuggerContext, context.collectAllCandidates, context.isSaveTypeInfo,
                context.callPosition, context.expressionContextProvider,
                context.languageVersionSettings, context.dataFlowValueFactory,
                context.inferenceSession, context.config
            )
        }
    }
}
