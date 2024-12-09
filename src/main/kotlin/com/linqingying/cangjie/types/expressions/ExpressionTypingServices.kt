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

package com.linqingying.cangjie.types.expressions

import com.intellij.openapi.progress.ProgressManager
import com.linqingying.cangjie.builtins.CangJieBuiltIns.Companion.isUnit
import com.linqingying.cangjie.config.LanguageFeature
import com.linqingying.cangjie.config.LanguageVersionSettings
import com.linqingying.cangjie.descriptors.BindingTrace
import com.linqingying.cangjie.descriptors.FunctionDescriptor
import com.linqingying.cangjie.psi.*
import com.linqingying.cangjie.psi.psiUtil.getNonStrictParentOfType
import com.linqingying.cangjie.psi.psiUtil.returnTarget
import com.linqingying.cangjie.resolve.*
import com.linqingying.cangjie.resolve.calls.NewCommonSuperTypeCalculator.commonSuperType
import com.linqingying.cangjie.resolve.calls.components.InferenceSession
import com.linqingying.cangjie.resolve.calls.components.InferenceSession.Companion.default
import com.linqingying.cangjie.resolve.calls.context.ContextDependency
import com.linqingying.cangjie.resolve.calls.context.ResolutionContext
import com.linqingying.cangjie.resolve.calls.inference.BuilderInferenceSession
import com.linqingying.cangjie.resolve.calls.smartcasts.DataFlowInfo
import com.linqingying.cangjie.resolve.calls.smartcasts.DataFlowInfo.Companion.EMPTY
import com.linqingying.cangjie.resolve.calls.tower.CangJieResolutionCallbacksImpl
import com.linqingying.cangjie.resolve.scopes.*
import com.linqingying.cangjie.types.CangJieType
import com.linqingying.cangjie.types.ErrorUtils.createErrorType
import com.linqingying.cangjie.types.checker.SimpleClassicTypeSystemContext
import com.linqingying.cangjie.types.error.ErrorTypeKind
import com.linqingying.cangjie.types.expressions.typeInfoFactory.createTypeInfo
import com.linqingying.cangjie.types.expressions.typeInfoFactory.noTypeInfo
import com.linqingying.cangjie.types.util.TypeUtils.DONT_CARE
import com.linqingying.cangjie.types.util.TypeUtils.NO_EXPECTED_TYPE
import com.linqingying.cangjie.types.util.TypeUtils.UNIT_EXPECTED_TYPE
import com.linqingying.cangjie.utils.exceptions.CangJieTypeInfo
import com.linqingying.cangjie.utils.slicedMap.WritableSlice

class ExpressionTypingServices(
    val expressionTypingComponents: ExpressionTypingComponents,
    private val annotationChecker: AnnotationChecker,
    @JvmField val statementFilter: StatementFilter,
    facade: ExpressionTypingVisitorDispatcher.ForDeclarations
) {
    val expressionTypingFacade: ExpressionTypingFacade = facade

    fun getTypeInfo(
        scope: LexicalScope,
        expression: CjExpression,

        dataFlowInfo: DataFlowInfo,
        inferenceSession: InferenceSession,
        trace: BindingTrace,
        isStatement: Boolean
    ): CangJieTypeInfo {
        return getTypeInfo(
            scope, expression, NO_EXPECTED_TYPE, dataFlowInfo, inferenceSession,
            trace, isStatement, expression, ContextDependency.INDEPENDENT
        )
    }

    fun getTypeInfo(
        scope: LexicalScope,
        expression: CjExpression,


        trace: BindingTrace

    ): CangJieTypeInfo {
        return getTypeInfo(
            scope, expression, NO_EXPECTED_TYPE, EMPTY, default,
            trace, false, expression, ContextDependency.INDEPENDENT
        )
    }

    fun getTypeInfo(
        scope: LexicalScope,
        expression: CjExpression,

        dataFlowInfo: DataFlowInfo,

        trace: BindingTrace

    ): CangJieTypeInfo {
        return getTypeInfo(
            scope, expression, NO_EXPECTED_TYPE, dataFlowInfo, default,
            trace, false, expression, ContextDependency.INDEPENDENT
        )
    }

    fun getTypeInfo(
        scope: LexicalScope,
        expression: CjExpression,

        dataFlowInfo: DataFlowInfo,
        inferenceSession: InferenceSession,
        trace: BindingTrace

    ): CangJieTypeInfo {
        return getTypeInfo(
            scope, expression, NO_EXPECTED_TYPE, dataFlowInfo, inferenceSession,
            trace, false, expression, ContextDependency.INDEPENDENT
        )
    }

    fun getTypeInfo(
        scope: LexicalScope,
        expression: CjExpression,
        expectedType: CangJieType,
        dataFlowInfo: DataFlowInfo,
        inferenceSession: InferenceSession,
        trace: BindingTrace,
        isStatement: Boolean
    ): CangJieTypeInfo {
        return getTypeInfo(
            scope, expression, expectedType, dataFlowInfo, inferenceSession,
            trace, isStatement, expression, ContextDependency.INDEPENDENT
        )
    }

    fun getNewContext(
        scope: LexicalScope,


        trace: BindingTrace

    ): ExpressionTypingContext {
        return ExpressionTypingContext.newContext(
            trace,
            scope,
            EMPTY,
            NO_EXPECTED_TYPE,
            ContextDependency.INDEPENDENT,
            statementFilter,
            languageVersionSettings,
            expressionTypingComponents.dataFlowValueFactory,
            default
        )
    }

    fun getNewContext(
        scope: LexicalScope,
        trace: BindingTrace,
        dataFlowInfo: DataFlowInfo,
        inferenceSession: InferenceSession?
    ): ExpressionTypingContext {
        return ExpressionTypingContext.newContext(
            trace,
            scope,
            dataFlowInfo,
            NO_EXPECTED_TYPE,
            ContextDependency.INDEPENDENT,
            statementFilter,
            languageVersionSettings,
            expressionTypingComponents.dataFlowValueFactory,
            inferenceSession ?: default
        )
    }

    fun getTypeInfo(
        scope: LexicalScope,
        expression: CjExpression,
        expectedType: CangJieType,
        dataFlowInfo: DataFlowInfo,
        inferenceSession: InferenceSession,
        trace: BindingTrace,
        isStatement: Boolean,
        contextExpression: CjExpression,
        contextDependency: ContextDependency
    ): CangJieTypeInfo {
        var context = ExpressionTypingContext.newContext(
            trace, scope, dataFlowInfo, expectedType, contextDependency, statementFilter, languageVersionSettings,
            expressionTypingComponents.dataFlowValueFactory, inferenceSession
        )
        if (contextExpression !== expression) {
            context =
                context.replaceExpressionContextProvider { arg: CjExpression -> if (arg === expression) contextExpression else null }
        }
        return expressionTypingFacade.getTypeInfo(expression, context, isStatement)
    }

    fun getType(
        scope: LexicalScope,
        expression: CjExpression,
        expectedType: CangJieType,
        dataFlowInfo: DataFlowInfo,
        inferenceSession: InferenceSession,
        trace: BindingTrace
    ): CangJieType? {
        return getTypeInfo(scope, expression, expectedType, dataFlowInfo, inferenceSession, trace, false).type
    }

    fun safeGetType(
        scope: LexicalScope,
        expression: CjExpression,
        expectedType: CangJieType,
        dataFlowInfo: DataFlowInfo,
        inferenceSession: InferenceSession,
        trace: BindingTrace
    ): CangJieType {
        val type = getType(scope, expression, expectedType, dataFlowInfo, inferenceSession, trace)

        return type
            ?: createErrorType(
                ErrorTypeKind.NO_RECORDED_TYPE,
                expression.text
            )
    }


    fun getTypeInfo(expression: CjExpression, resolutionContext: ResolutionContext<*>): CangJieTypeInfo {
        return expressionTypingFacade.getTypeInfo(expression, ExpressionTypingContext.newContext(resolutionContext))
    }

    fun createLocalRedeclarationChecker(trace: BindingTrace): LocalRedeclarationChecker {
        return TraceBasedLocalRedeclarationChecker(trace, expressionTypingComponents.overloadChecker)
    }

    private fun getTypeOfLastExpressionInBlock(
        statementExpression: CjExpression,
        context: ExpressionTypingContext,
        coercionStrategyForLastExpression: CoercionStrategy,
        blockLevelVisitor: ExpressionTypingInternals
    ): CangJieTypeInfo {
        var context = context
        val isUnitExpectedType = context.expectedType !== NO_EXPECTED_TYPE &&
                (context.expectedType === UNIT_EXPECTED_TYPE ||  //the first check is necessary to avoid invocation 'isUnit(UNIT_EXPECTED_TYPE)'
                        (coercionStrategyForLastExpression == CoercionStrategy.COERCION_TO_UNIT &&
                                isUnit(context.expectedType)
                                )
                        )


        if (context.expectedType !== NO_EXPECTED_TYPE) {
            val expectedType: CangJieType
            if (isUnitExpectedType) {
                expectedType = UNIT_EXPECTED_TYPE
            } else {
                expectedType = context.expectedType
            }
            return if (statementExpression is CjLocalNamedDeclaration) {
//                一定是Unit  TODO 这里使用checkType会导致最后一条语句没有解析
//                expressionTypingComponents.dataFlowAnalyzer.checkType(
//                    expressionTypingComponents.builtIns.unitType, statementExpression, context
//                )?.let {
//                    createTypeInfo(it)
//                } ?:
                blockLevelVisitor.getTypeInfo(statementExpression, context.replaceExpectedType(expectedType), true)
            } else {
                blockLevelVisitor.getTypeInfo(statementExpression, context.replaceExpectedType(expectedType), true)
            }

        }


        if (CjPsiUtil.deparenthesize(statementExpression) is CjLambdaExpression && context.contextDependency == ContextDependency.DEPENDENT) {
            val typeInfo = createDontCareTypeInfoForNILambda(statementExpression, context)
            if (typeInfo != null) return typeInfo
        }


        var result = blockLevelVisitor.getTypeInfo(statementExpression, context, true)
        if (coercionStrategyForLastExpression == CoercionStrategy.COERCION_TO_UNIT) {
            var mightBeUnit = false
            if (statementExpression is CjDeclaration) {
                if (statementExpression !is CjNamedFunction || statementExpression.getName() != null) {
                    mightBeUnit = true
                }
            }
            if (statementExpression is CjBinaryExpression) {
                val operationType = statementExpression.operationToken

//                if (operationType == CjTokens.EQ || OperatorConventions.ASSIGNMENT_OPERATIONS.containsKey(operationType)) {
//                    mightBeUnit = true;
//                }
            }
            if (mightBeUnit) {
                // ExpressionTypingVisitorForStatements should return only null or Unit for declarations and assignments,
                // but (for correct assignment / initialization analysis) data flow info must be preserved
                assert(result.type == null || isUnit(result.type!!))
                result = result.replaceType(expressionTypingComponents.builtIns.unitType)
            }
        }
        return result
    }

    private fun createDontCareTypeInfoForNILambda(
        statementExpression: CjExpression,
        context: ExpressionTypingContext
    ): CangJieTypeInfo? {
        if (!context.languageVersionSettings.supportsFeature(LanguageFeature.NewInference) || context.inferenceSession is BuilderInferenceSession) {
            return null
        }

        val functionLiteral = statementExpression.getNonStrictParentOfType<CjFunctionLiteral>()
        if (functionLiteral != null) {
            val info = context.trace.bindingContext[BindingContext.NEW_INFERENCE_LAMBDA_INFO, functionLiteral]
            if (info != null) {
                info.lastExpressionInfo.lexicalScope = context.scope
                info.lastExpressionInfo.trace = context.trace
                return CangJieTypeInfo(DONT_CARE, context.dataFlowInfo)
            }
        }

        return null
    }

    /**
     * 访问块语句，从第一个到最后一个传播数据流信息。
     * 确定块的返回类型以及块结束时和从块开始最近的跳转点的数据流信息。
     *
     * @param scope 词法可写作用域
     * @param block 块中的元素列表
     * @param coercionStrategyForLastExpression 最后一个表达式的强制策略
     * @param context 表达式类型检查上下文
     * @return 包含块返回类型和数据流信息的 [CangJieTypeInfo]
     */
    /*package*/
    fun getBlockReturnedTypeWithWritableScope(
        scope: LexicalWritableScope,
        block: List<CjElement>,
        coercionStrategyForLastExpression: CoercionStrategy,
        context: ExpressionTypingContext
    ): CangJieTypeInfo {

        if (block.isEmpty()) {
            return createTypeInfo(expressionTypingComponents.builtIns.unitType, context)
        }


        var blockLevelVisitor: ExpressionTypingInternals = ExpressionTypingVisitorDispatcher.ForBlock(
            expressionTypingComponents, annotationChecker, scope
        )

//        去除期望类型
        var newContext = context.replaceScope(scope).replaceExpectedType(NO_EXPECTED_TYPE)

        var result = noTypeInfo(context)
        var beforeJumpInfo = newContext.dataFlowInfo
        var jumpOutPossible = false

        var isFirstStatement = true
        val iterator = block.iterator()
//        var parentDeclaration: DeclarationDescriptor? = null
        while (iterator.hasNext()) {
            ProgressManager.checkCanceled()

            // 使用过滤跟踪以仅保留一个语句的效果系统缓存
            val traceForSingleStatement: AbstractFilteringTrace = EffectsFilteringTrace(context.trace)
            newContext = newContext.replaceBindingTrace(traceForSingleStatement)

            val statement = iterator.next() as? CjExpression ?: continue

//            if (parentDeclaration == null) {
//                parentDeclaration =
//                    context.trace.bindingContext.get(
//                        BindingContext.DECLARATION_TO_DESCRIPTOR, context.getContextParentOfType(
//                            statement,
//                            CjDeclaration::class.java
//                        )
//                    )
//            }
            if (!iterator.hasNext()) {


                // 最后一条语句也需要检查类型，即使前面有 return 语句而无法到达，检查类型也是必要的
                result = getTypeOfLastExpressionInBlock(
                    statement,
                    newContext.replaceExpectedType(context.expectedType) /*重新添加期望类型*/,
                    coercionStrategyForLastExpression,
                    blockLevelVisitor
                )
                if (result.type != null && statement.parent is CjBlockExpression) {
                    val lastExpressionValue = expressionTypingComponents.dataFlowValueFactory.createDataFlowValue(
                        statement, result.type!!, context
                    )
                    val blockExpressionValue = expressionTypingComponents.dataFlowValueFactory.createDataFlowValue(
                        (statement.parent as CjBlockExpression), result.type!!, context
                    )
                    result = result.replaceDataFlowInfo(
                        result.dataFlowInfo.assign(
                            blockExpressionValue, lastExpressionValue
                        )
                    )
                }
            } else {
                result = blockLevelVisitor.getTypeInfo(
                    statement,
//                    始终以去除的期望类型为参数
                    newContext.replaceContextDependency(ContextDependency.INDEPENDENT),
                    true
                )
            }

            val newDataFlowInfo = result.dataFlowInfo
            // 如果没有可能跳转，我们获取跳转前的新数据流信息
            if (!jumpOutPossible) {
                beforeJumpInfo = result.jumpFlowInfo
                jumpOutPossible = result.jumpOutPossible
            }
            if (newDataFlowInfo !== newContext.dataFlowInfo) {
                newContext = newContext.replaceDataFlowInfo(newDataFlowInfo)
            }
            blockLevelVisitor = ExpressionTypingVisitorDispatcher.ForBlock(
                expressionTypingComponents,
                annotationChecker, scope
            )

            val ownerDescriptor = scope.ownerDescriptor
            if (isFirstStatement && ownerDescriptor is FunctionDescriptor) {
                isFirstStatement = false
            }
        }
//        if (parentDeclaration is FunctionDescriptorImpl) {
//            parentDeclaration.setReturnType(
//                result.type ?: ErrorUtils.invalidType
//            )
//        }
        return result.replaceJumpOutPossible(jumpOutPossible).replaceJumpFlowInfo(beforeJumpInfo)
    }


    fun getBodyExpressionType(
        trace: BindingTrace,
        outerScope: LexicalScope,
        dataFlowInfo: DataFlowInfo,
        function: CjDeclarationWithBody,
        functionDescriptor: FunctionDescriptor,
        inferenceSession: InferenceSession?
    ): CangJieType {
        val bodyExpression = function.bodyBlockExpression
        val functionInnerScope = FunctionDescriptorUtil.getFunctionInnerScope(
            outerScope, functionDescriptor, trace, expressionTypingComponents.overloadChecker
        )

        val context = ExpressionTypingContext.newContext(
            trace,
            functionInnerScope,
            dataFlowInfo,
            NO_EXPECTED_TYPE,
            languageVersionSettings,
            expressionTypingComponents.dataFlowValueFactory,
            inferenceSession
        )


        val typeInfo = bodyExpression?.let { expressionTypingFacade.getTypeInfo(it, context, function.hasBlockBody()) }

        return typeInfo?.type ?: createErrorType(ErrorTypeKind.RETURN_TYPE_FOR_FUNCTION)
    }

    fun getBlockReturnedType(
        expression: CjBlockExpression,
        context: ExpressionTypingContext,
        isStatement: Boolean
    ): CangJieTypeInfo {


        return getBlockReturnedType(
            expression,
            if (isStatement) CoercionStrategy.COERCION_TO_UNIT else CoercionStrategy.NO_COERCION,
            context
        )
    }

    fun getBlockReturnedType(
        expression: CjBlockExpression,
        coercionStrategyForLastExpression: CoercionStrategy,
        context: ExpressionTypingContext
    ): CangJieTypeInfo {
        val block = statementFilter.filterStatements(expression)

        val containingDescriptor = context.scope.ownerDescriptor
        val redeclarationChecker =
            TraceBasedLocalRedeclarationChecker(context.trace, expressionTypingComponents.overloadChecker)
        val scope = LexicalWritableScope(
            context.scope, containingDescriptor, false, redeclarationChecker,
            LexicalScopeKind.CODE_BLOCK
        )
        val r = if (block.isEmpty()) {
            expressionTypingComponents.dataFlowAnalyzer
                .createCheckedTypeInfo(expressionTypingComponents.builtIns.unitType, context, expression)
        } else {
            getBlockReturnedTypeWithWritableScope(
                scope, block, coercionStrategyForLastExpression,
                context.replaceStatementFilter(statementFilter)
            )
        }
        scope.freeze()

        val target = expression.returnTarget

        if (expression.parent is CjFunction && target is CjFunctionImpl && target.isInferReturnType) {
            val returns = context.trace[BindingContext.RETURN_TARGET, target]

            val types = returns?.mapNotNull {
                getTypeInfo(it, context).type
            }?.toMutableList()?.apply {
                r.type?.let { add(it) }
            } ?: return r

            val type = SimpleClassicTypeSystemContext.commonSuperType(types)

            return createTypeInfo(type as? CangJieType)


        }
        return r
    }

    /*package*/
    fun checkFunctionReturnType(function: CjDeclarationWithBody, context: ExpressionTypingContext) {
        val bodyExpression = function.bodyExpression ?: return

        val blockBody = function.hasBlockBody()
        val newContext = if (function is CjSecondaryConstructor || function is CjPrimaryConstructor) {
            context.replaceExpectedType(NO_EXPECTED_TYPE)
        } else {
            context
        }
//            if (blockBody //                        ? context.replaceExpectedType(NO_EXPECTED_TYPE)
//            )
//                context.replaceExpectedType(NO_EXPECTED_TYPE)
//            else
//                context

        expressionTypingFacade.getTypeInfo(bodyExpression, newContext, blockBody)
    }

    fun createContext(
        functionInnerScope: LexicalScope,

        dataFlowInfo: DataFlowInfo,
        expectedReturnType: CangJieType?,
        trace: BindingTrace

    ): ExpressionTypingContext {
        return ExpressionTypingContext.newContext(
            trace,
            functionInnerScope, dataFlowInfo, expectedReturnType ?: NO_EXPECTED_TYPE,
            languageVersionSettings, expressionTypingComponents.dataFlowValueFactory,
            default
        )
    }

    fun resolveFunctionReturnType(
        functionInnerScope: LexicalScope,
        function: CjDeclarationWithBody,
        functionDescriptor: FunctionDescriptor,
        dataFlowInfo: DataFlowInfo,
        expectedReturnType: CangJieType?,
        trace: BindingTrace,
        localContext: ExpressionTypingContext?
    ): CangJieTypeInfo {
        val context = ExpressionTypingContext.newContext(
            trace,
            functionInnerScope, dataFlowInfo, expectedReturnType ?: NO_EXPECTED_TYPE,
            languageVersionSettings, expressionTypingComponents.dataFlowValueFactory,
            localContext?.inferenceSession ?: default
        )
        return getBlockReturnedType(function.bodyBlockExpression!!, context, false)
    }

    fun checkFunctionReturnType(
        functionInnerScope: LexicalScope,
        function: CjDeclarationWithBody,
        functionDescriptor: FunctionDescriptor,
        dataFlowInfo: DataFlowInfo,
        expectedReturnType: CangJieType?,
        trace: BindingTrace,
        localContext: ExpressionTypingContext?
    ) {
        var expectedReturnType = expectedReturnType
        if (expectedReturnType == null) {
            expectedReturnType = functionDescriptor.returnType
            if (!function.hasBlockBody() && !function.hasDeclaredReturnType()) {
                expectedReturnType = NO_EXPECTED_TYPE
            }
        }

        val context = ExpressionTypingContext.newContext(
            trace,
            functionInnerScope, dataFlowInfo, expectedReturnType ?: NO_EXPECTED_TYPE,
            languageVersionSettings, expressionTypingComponents.dataFlowValueFactory,
            localContext?.inferenceSession ?: default
        )

        checkFunctionReturnType(function, context)
    }


    val languageVersionSettings: LanguageVersionSettings
        get() = expressionTypingComponents.languageVersionSettings

    private class EffectsFilteringTrace(parentTrace: BindingTrace) :
        AbstractFilteringTrace(parentTrace, "Effects filtering trace") {
        override fun <K, V> shouldBeHiddenFromParent(slice: WritableSlice<K, V>, key: K): Boolean {
            return slice === BindingContext.EXPRESSION_EFFECTS
        }
    }

    companion object {
        fun getNewInferenceLambdaInfo(
            context: ExpressionTypingContext,
            function: CjElement
        ): CangJieResolutionCallbacksImpl.LambdaInfo? {
            if (function is CjFunction) {
                return context.trace.get(
                    BindingContext.NEW_INFERENCE_LAMBDA_INFO,
                    function
                )
            }
            return null
        }
    }
}
