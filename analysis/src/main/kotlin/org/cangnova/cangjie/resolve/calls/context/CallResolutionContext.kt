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
import org.cangnova.cangjie.resolve.calls.model.DataFlowInfoForArgumentsImpl
import org.cangnova.cangjie.resolve.calls.model.MutableDataFlowInfoForArguments
import org.cangnova.cangjie.resolve.calls.smartcasts.DataFlowInfo
import org.cangnova.cangjie.resolve.calls.smartcasts.DataFlowValueFactory
import org.cangnova.cangjie.resolve.scopes.LexicalScope
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.expressions.ContextConfig

/**
 * 调用解析上下文
 *
 * 扩展 [ResolutionContext]，添加调用相关的信息，如调用对象、参数检查模式和参数数据流信息。
 *
 * @param Context 具体的上下文类型（自引用泛型）
 */
abstract class CallResolutionContext<Context : CallResolutionContext<Context>> protected constructor(
    trace: BindingTrace,
    scope: LexicalScope,
    /**
     * 调用对象
     */
    val call: Call,
    expectedType: CangJieType,
    dataFlowInfo: DataFlowInfo,
    contextDependency: ContextDependency,
    /**
     * 参数类型检查模式
     */
    val checkArguments: CheckArgumentTypesMode,
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
) : ResolutionContext<Context>(
    trace, scope, expectedType, dataFlowInfo, contextDependency, resolutionResultsCache,
    statementFilter, isAnnotationContext, isDebuggerContext, collectAllCandidates,
    isSaveTypeInfo, callPosition, expressionContextProvider, languageVersionSettings,
    dataFlowValueFactory, inferenceSession, config
) {
    /**
     * 参数的可变数据流信息
     */
    val dataFlowInfoForArguments: MutableDataFlowInfoForArguments = when {
        dataFlowInfoForArguments != null -> dataFlowInfoForArguments
        checkArguments == CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS -> {
            DataFlowInfoForArgumentsImpl(dataFlowInfo, call)
        }

        else -> {
            MutableDataFlowInfoForArguments.WithoutArgumentsCheck(dataFlowInfo)
        }
    }
}
