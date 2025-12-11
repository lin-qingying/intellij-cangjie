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
import org.cangnova.cangjie.psi.CjExpression
import org.cangnova.cangjie.resolve.StatementFilter
import org.cangnova.cangjie.resolve.calls.components.InferenceSession
import org.cangnova.cangjie.resolve.calls.smartcasts.DataFlowInfo
import org.cangnova.cangjie.resolve.calls.smartcasts.DataFlowValueFactory
import org.cangnova.cangjie.resolve.scopes.LexicalScope
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.expressions.ContextConfig
import org.cangnova.cangjie.types.expressions.ProcessingMode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.cangnova.cangjie.resolve.binding.BindingTrace
import org.cangnova.cangjie.types.TypeUtils

/**
 * 解析上下文
 *
 * 此类及其子类用于在自顶向下的方向上传递数据流分析信息，从 AST 父节点到子节点。
 *
 * **注意**：所有子类都必须是不可变的！
 *
 * @param Context 具体的上下文类型（自引用泛型）
 */
abstract class ResolutionContext<Context : ResolutionContext<Context>> protected constructor(
    val trace: BindingTrace,
    scope: LexicalScope,
    val expectedType: CangJieType,
    val dataFlowInfo: DataFlowInfo,
    val contextDependency: ContextDependency,
    val resolutionResultsCache: ResolutionResultsCache,
    val statementFilter: StatementFilter,
    val isAnnotationContext: Boolean,
    val isDebuggerContext: Boolean,
    val collectAllCandidates: Boolean,
    val isSaveTypeInfo: Boolean,
    val callPosition: CallPosition,
    /**
     * 用于在给定上下文中分析表达式。
     * 应该用于遍历父节点以查找包含的函数、循环等。
     * 提供者应该为给定表达式返回特定的上下文表达式（可以用来代替父节点），否则返回 null。
     *
     * @see getContextParentOfType
     */
    val expressionContextProvider: (CjExpression) -> CjExpression?,
    val languageVersionSettings: LanguageVersionSettings,
    val dataFlowValueFactory: DataFlowValueFactory,
    val inferenceSession: InferenceSession,
    val config: ContextConfig
) {
    /**
     * 词法作用域（可变）
     */
    var scope: LexicalScope = scope

    /**
     * 替换表达式上下文提供者
     */
    fun replaceExpressionContextProvider(
        expressionContextProvider: (CjExpression) -> CjExpression?
    ): Context {
        return create(
            trace, scope, dataFlowInfo, expectedType, contextDependency, resolutionResultsCache,
            statementFilter, collectAllCandidates, isSaveTypeInfo, callPosition,
            expressionContextProvider, languageVersionSettings, dataFlowValueFactory,
            inferenceSession, config
        )
    }

    /**
     * 替换上下文依赖
     */
    fun replaceContextDependency(newContextDependency: ContextDependency): Context {
        if (newContextDependency === contextDependency) return self()
        return create(
            trace, scope, dataFlowInfo, expectedType, newContextDependency, resolutionResultsCache,
            statementFilter, collectAllCandidates, isSaveTypeInfo, callPosition,
            expressionContextProvider, languageVersionSettings, dataFlowValueFactory,
            inferenceSession, config
        )
    }

    /**
     * 替换 trace 和缓存
     */
    fun replaceTraceAndCache(traceAndCache: TemporaryTraceAndCache): Context {
        return replaceBindingTrace(traceAndCache.trace).replaceResolutionResultsCache(traceAndCache.cache)
    }

    /**
     * 替换解析结果缓存
     */
    fun replaceResolutionResultsCache(newResolutionResultsCache: ResolutionResultsCache): Context {
        if (newResolutionResultsCache === resolutionResultsCache) return self()
        return create(
            trace, scope, dataFlowInfo, expectedType, contextDependency, newResolutionResultsCache,
            statementFilter, collectAllCandidates, isSaveTypeInfo, callPosition,
            expressionContextProvider, languageVersionSettings, dataFlowValueFactory,
            inferenceSession, config
        )
    }

    /**
     * 替换语句过滤器
     */
    fun replaceStatementFilter(statementFilter: StatementFilter): Context {
        return create(
            trace, scope, dataFlowInfo, expectedType, contextDependency, resolutionResultsCache,
            statementFilter, collectAllCandidates, isSaveTypeInfo, callPosition,
            expressionContextProvider, languageVersionSettings, dataFlowValueFactory,
            inferenceSession, config
        )
    }

    /**
     * 创建新的上下文实例
     *
     * 子类必须实现此方法以创建特定类型的上下文
     */
    protected abstract fun create(
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
    ): Context

    /**
     * 替换是否收集所有候选
     */
    fun replaceCollectAllCandidates(newCollectAllCandidates: Boolean): Context {
        return create(
            trace, scope, dataFlowInfo, expectedType, contextDependency, resolutionResultsCache,
            statementFilter, newCollectAllCandidates, isSaveTypeInfo, callPosition,
            expressionContextProvider, languageVersionSettings, dataFlowValueFactory,
            inferenceSession, config
        )
    }

    /**
     * 替换处理模式
     */
    fun replaceProcessingMode(processingMode: ProcessingMode): Context {
        return create(
            trace, scope, dataFlowInfo, expectedType, contextDependency, resolutionResultsCache,
            statementFilter, collectAllCandidates, isSaveTypeInfo, callPosition,
            expressionContextProvider, languageVersionSettings, dataFlowValueFactory,
            inferenceSession, config.copy(processingMode)
        )
    }

    /**
     * 替换调用位置
     */
    fun replaceCallPosition(callPosition: CallPosition): Context {
        return create(
            trace, scope, dataFlowInfo, expectedType, contextDependency, resolutionResultsCache,
            statementFilter, collectAllCandidates, isSaveTypeInfo, callPosition,
            expressionContextProvider, languageVersionSettings, dataFlowValueFactory,
            inferenceSession, config
        )
    }

    /**
     * 返回当前实例（强制转换为 Context 类型）
     */
    @Suppress("UNCHECKED_CAST")
    private fun self(): Context = this as Context

    /**
     * 替换绑定跟踪
     */
    fun replaceBindingTrace(trace: BindingTrace): Context {
        if (this.trace === trace) return self()
        return create(
            trace, scope, dataFlowInfo, expectedType, contextDependency, resolutionResultsCache,
            statementFilter, collectAllCandidates, isSaveTypeInfo, callPosition,
            expressionContextProvider, languageVersionSettings, dataFlowValueFactory,
            inferenceSession, config
        )
    }

    /**
     * 替换数据流信息
     */
    fun replaceDataFlowInfo(newDataFlowInfo: DataFlowInfo): Context {
        if (newDataFlowInfo === dataFlowInfo) return self()
        return create(
            trace, scope, newDataFlowInfo, expectedType, contextDependency, resolutionResultsCache,
            statementFilter, collectAllCandidates, isSaveTypeInfo, callPosition,
            expressionContextProvider, languageVersionSettings, dataFlowValueFactory,
            inferenceSession, config
        )
    }

    /**
     * 替换推断会话
     */
    fun replaceInferenceSession(newInferenceSession: InferenceSession): Context {
        if (newInferenceSession === inferenceSession) return self()
        return create(
            trace, scope, dataFlowInfo, expectedType, contextDependency, resolutionResultsCache,
            statementFilter, collectAllCandidates, isSaveTypeInfo, callPosition,
            expressionContextProvider, languageVersionSettings, dataFlowValueFactory,
            newInferenceSession, config
        )
    }

    /**
     * 替换是否保存类型信息
     */
    fun replaceIsSaveTypeInfo(isSaveTypeInfo: Boolean): Context {
        return create(
            trace, scope, dataFlowInfo, expectedType, contextDependency, resolutionResultsCache,
            statementFilter, collectAllCandidates, isSaveTypeInfo, callPosition,
            expressionContextProvider, languageVersionSettings, dataFlowValueFactory,
            inferenceSession, config
        )
    }

    /**
     * 替换期望类型
     */
    fun replaceExpectedType(newExpectedType: CangJieType?): Context {
        val type = newExpectedType ?: TypeUtils.NO_EXPECTED_TYPE
        if (expectedType === type) return self()
        return create(
            trace, scope, dataFlowInfo, type, contextDependency, resolutionResultsCache,
            statementFilter, collectAllCandidates, isSaveTypeInfo, callPosition,
            expressionContextProvider, languageVersionSettings, dataFlowValueFactory,
            inferenceSession, config
        )
    }

    /**
     * 替换词法作用域
     */
    fun replaceScope(newScope: LexicalScope): Context {
        if (newScope === scope) return self()
        return create(
            trace, newScope, dataFlowInfo, expectedType, contextDependency, resolutionResultsCache,
            statementFilter, collectAllCandidates, isSaveTypeInfo, callPosition,
            expressionContextProvider, languageVersionSettings, dataFlowValueFactory,
            inferenceSession, config
        )
    }

    /**
     * 获取指定类型的上下文父节点
     *
     * @param expression 起始表达式
     * @param classes 要查找的父节点类型
     * @return 找到的父节点，如果未找到则返回 null
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : PsiElement> getContextParentOfType(
        expression: CjExpression,
        vararg classes: Class<out T>
    ): T? {
        val context = expressionContextProvider(expression)
        var current: PsiElement? = context ?: expression.parent

        while (current != null) {
            for (klass in classes) {
                if (klass.isInstance(current)) {
                    return current as T
                }
            }

            if (current is PsiFile) return null

            if (current is CjExpression) {
                val newContext = expressionContextProvider(current)
                if (newContext != null) {
                    current = newContext
                    continue
                }
            }

            current = current.parent
        }
        return null
    }

    companion object {
        /**
         * 默认表达式上下文提供者（总是返回 null）
         */
        @JvmField
        val DEFAULT_EXPRESSION_CONTEXT_PROVIDER: (CjExpression) -> CjExpression? = { null }
    }
}
