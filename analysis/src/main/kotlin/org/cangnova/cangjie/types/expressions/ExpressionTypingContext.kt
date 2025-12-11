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

package org.cangnova.cangjie.types.expressions

import org.cangnova.cangjie.config.LanguageVersionSettings
import org.cangnova.cangjie.psi.CjExpression
import org.cangnova.cangjie.resolve.StatementFilter
import org.cangnova.cangjie.resolve.binding.BindingTrace
import org.cangnova.cangjie.resolve.calls.components.InferenceSession
import org.cangnova.cangjie.resolve.calls.context.CallPosition
import org.cangnova.cangjie.resolve.calls.context.ContextDependency
import org.cangnova.cangjie.resolve.calls.context.ResolutionContext
import org.cangnova.cangjie.resolve.calls.context.ResolutionResultsCache
import org.cangnova.cangjie.resolve.calls.context.ResolutionResultsCacheImpl
import org.cangnova.cangjie.resolve.calls.smartcasts.DataFlowInfo
import org.cangnova.cangjie.resolve.calls.smartcasts.DataFlowValueFactory
import org.cangnova.cangjie.resolve.scopes.LexicalScope
import org.cangnova.cangjie.types.CangJieType

/**
 * 表达式类型推导上下文
 *
 * 扩展 [org.cangnova.cangjie.resolve.calls.context.ResolutionContext]，专门用于表达式的类型推导过程。
 * 提供了多个工厂方法来创建不同配置的上下文实例。
 *
 * ## 使用场景
 *
 * - 表达式类型推导
 * - 语句类型检查
 * - 控制流分析
 * - 数据流分析
 *
 * ## 创建方式
 *
 * 使用伴生对象中的静态工厂方法：
 * ```kotlin
 * val context = ExpressionTypingContext.newContext(trace, scope, dataFlowInfo, expectedType, languageVersionSettings, dataFlowValueFactory)
 * ```
 */
class ExpressionTypingContext private constructor(
    trace: BindingTrace,
    scope: LexicalScope,
    dataFlowInfo: DataFlowInfo,
    expectedType: CangJieType,
    contextDependency: ContextDependency,
    resolutionResultsCache: ResolutionResultsCache,
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
) : ResolutionContext<ExpressionTypingContext>(
    trace, scope, expectedType, dataFlowInfo, contextDependency, resolutionResultsCache,
    statementFilter, isAnnotationContext, isDebuggerContext, collectAllCandidates, isSaveTypeInfo,
    callPosition, expressionContextProvider, languageVersionSettings, dataFlowValueFactory,
    inferenceSession, config
) {

    /**
     * 创建新的上下文实例
     *
     * 子类必须实现此方法以创建特定类型的上下文
     */
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
    ): ExpressionTypingContext {
        return ExpressionTypingContext(
            trace, scope, dataFlowInfo, expectedType, contextDependency, resolutionResultsCache,
            statementFilter, isAnnotationContext, isDebuggerContext, collectAllCandidates,
            isSaveTypeInfo, callPosition, expressionContextProvider, languageVersionSettings,
            dataFlowValueFactory, inferenceSession, config
        )
    }

    companion object {
        /**
         * 从现有上下文创建新的表达式类型推导上下文
         *
         * @param context 源上下文
         * @return 新的表达式类型推导上下文
         */
        @JvmStatic
        fun newContext(context: ResolutionContext<*>): ExpressionTypingContext {
            return ExpressionTypingContext(
                context.trace, context.scope, context.dataFlowInfo, context.expectedType,
                context.contextDependency, context.resolutionResultsCache,
                context.statementFilter, context.isAnnotationContext, context.isDebuggerContext,
                context.collectAllCandidates, context.isSaveTypeInfo, context.callPosition,
                context.expressionContextProvider, context.languageVersionSettings,
                context.dataFlowValueFactory, context.inferenceSession, context.config
            )
        }

        /**
         * 从现有上下文创建新的表达式类型推导上下文，并指定调试器模式
         *
         * @param context 源上下文
         * @param isDebuggerContext 是否为调试器上下文
         * @return 新的表达式类型推导上下文
         */
        @JvmStatic
        fun newContext(context: ResolutionContext<*>, isDebuggerContext: Boolean): ExpressionTypingContext {
            return ExpressionTypingContext(
                context.trace, context.scope, context.dataFlowInfo, context.expectedType,
                context.contextDependency, context.resolutionResultsCache,
                context.statementFilter, context.isAnnotationContext, isDebuggerContext,
                context.collectAllCandidates, context.isSaveTypeInfo, context.callPosition,
                context.expressionContextProvider, context.languageVersionSettings,
                context.dataFlowValueFactory, context.inferenceSession, context.config
            )
        }

        /**
         * 创建完整配置的表达式类型推导上下文
         *
         * @param trace 绑定跟踪
         * @param scope 词法作用域
         * @param dataFlowInfo 数据流信息
         * @param expectedType 期望类型
         * @param contextDependency 上下文依赖
         * @param statementFilter 语句过滤器
         * @param languageVersionSettings 语言版本设置
         * @param dataFlowValueFactory 数据流值工厂
         * @param inferenceSession 推断会话
         * @return 新的表达式类型推导上下文
         */
        @JvmStatic
        fun newContext(
            trace: BindingTrace,
            scope: LexicalScope,
            dataFlowInfo: DataFlowInfo,
            expectedType: CangJieType,
            contextDependency: ContextDependency,
            statementFilter: StatementFilter,
            languageVersionSettings: LanguageVersionSettings,
            dataFlowValueFactory: DataFlowValueFactory,
            inferenceSession: InferenceSession
        ): ExpressionTypingContext {
            return newContext(
                trace, scope, dataFlowInfo, expectedType, contextDependency,
                ResolutionResultsCacheImpl(), statementFilter, false, languageVersionSettings,
                dataFlowValueFactory, inferenceSession
            )
        }

        /**
         * 创建带解析结果缓存的表达式类型推导上下文
         *
         * @param trace 绑定跟踪
         * @param scope 词法作用域
         * @param dataFlowInfo 数据流信息
         * @param expectedType 期望类型
         * @param contextDependency 上下文依赖
         * @param resolutionResultsCache 解析结果缓存
         * @param statementFilter 语句过滤器
         * @param isAnnotationContext 是否为注解上下文
         * @param languageVersionSettings 语言版本设置
         * @param dataFlowValueFactory 数据流值工厂
         * @param inferenceSession 推断会话
         * @return 新的表达式类型推导上下文
         */
        @JvmStatic
        fun newContext(
            trace: BindingTrace,
            scope: LexicalScope,
            dataFlowInfo: DataFlowInfo,
            expectedType: CangJieType,
            contextDependency: ContextDependency,
            resolutionResultsCache: ResolutionResultsCache,
            statementFilter: StatementFilter,
            isAnnotationContext: Boolean,
            languageVersionSettings: LanguageVersionSettings,
            dataFlowValueFactory: DataFlowValueFactory,
            inferenceSession: InferenceSession
        ): ExpressionTypingContext {
            return ExpressionTypingContext(
                trace, scope, dataFlowInfo, expectedType, contextDependency, resolutionResultsCache,
                statementFilter, isAnnotationContext, false, false, true,
                CallPosition.Unknown, DEFAULT_EXPRESSION_CONTEXT_PROVIDER, languageVersionSettings,
                dataFlowValueFactory, inferenceSession, ContextConfig.DEFAULT
            )
        }

        /**
         * 创建简化配置的表达式类型推导上下文
         *
         * 使用默认的上下文依赖和语句过滤器
         *
         * @param trace 绑定跟踪
         * @param scope 词法作用域
         * @param dataFlowInfo 数据流信息
         * @param expectedType 期望类型
         * @param languageVersionSettings 语言版本设置
         * @param dataFlowValueFactory 数据流值工厂
         * @return 新的表达式类型推导上下文
         */
        @JvmStatic
        fun newContext(
            trace: BindingTrace,
            scope: LexicalScope,
            dataFlowInfo: DataFlowInfo,
            expectedType: CangJieType,
            languageVersionSettings: LanguageVersionSettings,
            dataFlowValueFactory: DataFlowValueFactory
        ): ExpressionTypingContext {
            return newContext(
                trace, scope, dataFlowInfo, expectedType, ContextDependency.INDEPENDENT,
                StatementFilter.Companion.NONE, languageVersionSettings, dataFlowValueFactory,
                InferenceSession.Companion.default
            )
        }

        /**
         * 创建带可选推断会话的表达式类型推导上下文
         *
         * @param trace 绑定跟踪
         * @param scope 词法作用域
         * @param dataFlowInfo 数据流信息
         * @param expectedType 期望类型
         * @param languageVersionSettings 语言版本设置
         * @param dataFlowValueFactory 数据流值工厂
         * @param inferenceSession 推断会话（可为 null，使用默认值）
         * @return 新的表达式类型推导上下文
         */
        @JvmStatic
        fun newContext(
            trace: BindingTrace,
            scope: LexicalScope,
            dataFlowInfo: DataFlowInfo,
            expectedType: CangJieType,
            languageVersionSettings: LanguageVersionSettings,
            dataFlowValueFactory: DataFlowValueFactory,
            inferenceSession: InferenceSession?
        ): ExpressionTypingContext {
            return newContext(
                trace, scope, dataFlowInfo, expectedType, ContextDependency.INDEPENDENT,
                StatementFilter.Companion.NONE, languageVersionSettings, dataFlowValueFactory,
                inferenceSession ?: InferenceSession.Companion.default
            )
        }
    }
}