/*
 * Copyright 2024 LinQingYing. and contributors.
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

package cn.cangnova.cangjie.resolve

import cn.cangnova.cangjie.descriptors.BindingTrace
import cn.cangnova.cangjie.ide.FrontendInternals
import cn.cangnova.cangjie.psi.CjExpression
import cn.cangnova.cangjie.resolve.caches.getResolutionFacade
import cn.cangnova.cangjie.resolve.calls.components.InferenceSession
import cn.cangnova.cangjie.resolve.calls.context.ContextDependency
import cn.cangnova.cangjie.resolve.calls.smartcasts.DataFlowInfo
import cn.cangnova.cangjie.resolve.scopes.LexicalScope
import cn.cangnova.cangjie.resolve.scopes.getResolutionScope
import cn.cangnova.cangjie.types.CangJieType
import cn.cangnova.cangjie.types.expressions.ExpressionTypingServices
import cn.cangnova.cangjie.types.expressions.PreliminaryDeclarationVisitor
import cn.cangnova.cangjie.types.util.TypeUtils
import cn.cangnova.cangjie.utils.exceptions.CangJieTypeInfo

@JvmOverloads
@OptIn(FrontendInternals::class)
fun CjExpression.computeTypeInfoInContext(
    scope: LexicalScope,
    contextExpression: CjExpression = this,
    trace: BindingTrace = BindingTraceContext(),
    dataFlowInfo: DataFlowInfo = DataFlowInfo.EMPTY,
    expectedType: CangJieType = TypeUtils.NO_EXPECTED_TYPE,
    isStatement: Boolean = false,
    contextDependency: ContextDependency = ContextDependency.INDEPENDENT,
    expressionTypingServices: ExpressionTypingServices = contextExpression.getResolutionFacade().frontendService<ExpressionTypingServices>()
): CangJieTypeInfo {
    PreliminaryDeclarationVisitor.createForExpression(this, trace, expressionTypingServices.languageVersionSettings)
    return expressionTypingServices.getTypeInfo(
        scope, this, expectedType, dataFlowInfo, InferenceSession.default, trace, isStatement, contextExpression, contextDependency
    )
}
@JvmOverloads
@OptIn(FrontendInternals::class)
fun CjExpression.analyzeInContext(
    scope: LexicalScope,
    contextExpression: CjExpression = this,
    trace: BindingTrace = BindingTraceContext(),
    dataFlowInfo: DataFlowInfo = DataFlowInfo.EMPTY,
    expectedType: CangJieType = TypeUtils.NO_EXPECTED_TYPE,
    isStatement: Boolean = false,
    contextDependency: ContextDependency = ContextDependency.INDEPENDENT,
    expressionTypingServices: ExpressionTypingServices = contextExpression.getResolutionFacade().frontendService<ExpressionTypingServices>()
): BindingContext {
    computeTypeInfoInContext(
        scope,
        contextExpression,
        trace,
        dataFlowInfo,
        expectedType,
        isStatement,
        contextDependency,
        expressionTypingServices
    )
    return trace.bindingContext
}

@JvmOverloads
fun CjExpression.analyzeAsReplacement(
    expressionToBeReplaced: CjExpression,
    bindingContext: BindingContext,
    scope: LexicalScope,
    trace: BindingTrace = DelegatingBindingTrace(bindingContext, "Temporary trace for analyzeAsReplacement()"),
    contextDependency: ContextDependency = ContextDependency.INDEPENDENT
): BindingContext = analyzeInContext(
    scope,
    expressionToBeReplaced,
    dataFlowInfo = bindingContext.getDataFlowInfoBefore(expressionToBeReplaced),
    expectedType = bindingContext[BindingContext.EXPECTED_EXPRESSION_TYPE, expressionToBeReplaced] ?: TypeUtils.NO_EXPECTED_TYPE,
    isStatement = expressionToBeReplaced.isUsedAsStatement(bindingContext),
    trace = trace,
    contextDependency = contextDependency
)
@JvmOverloads
fun CjExpression.analyzeAsReplacement(
    expressionToBeReplaced: CjExpression,
    bindingContext: BindingContext,
    resolutionFacade: ResolutionFacade = expressionToBeReplaced.getResolutionFacade(),
    trace: BindingTrace = DelegatingBindingTrace(bindingContext, "Temporary trace for analyzeAsReplacement()"),
    contextDependency: ContextDependency = ContextDependency.INDEPENDENT
): BindingContext {
    val scope = expressionToBeReplaced.getResolutionScope(bindingContext, resolutionFacade)
    return analyzeAsReplacement(expressionToBeReplaced, bindingContext, scope, trace, contextDependency)
}
