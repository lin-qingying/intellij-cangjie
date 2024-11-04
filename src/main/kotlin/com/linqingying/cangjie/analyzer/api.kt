package com.linqingying.cangjie.analyzer

import com.linqingying.cangjie.descriptors.BindingTrace
import com.linqingying.cangjie.ide.FrontendInternals
import com.linqingying.cangjie.psi.CjExpression
import com.linqingying.cangjie.resolve.*
import com.linqingying.cangjie.resolve.caches.getResolutionFacade
import com.linqingying.cangjie.resolve.calls.components.InferenceSession
import com.linqingying.cangjie.resolve.calls.context.ContextDependency
import com.linqingying.cangjie.resolve.calls.smartcasts.DataFlowInfo
import com.linqingying.cangjie.resolve.scopes.LexicalScope
import com.linqingying.cangjie.resolve.scopes.getResolutionScope
import com.linqingying.cangjie.types.CangJieType
import com.linqingying.cangjie.types.expressions.ExpressionTypingServices
import com.linqingying.cangjie.types.expressions.PreliminaryDeclarationVisitor
import com.linqingying.cangjie.types.util.TypeUtils
import com.linqingying.cangjie.utils.exceptions.CangJieTypeInfo

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
