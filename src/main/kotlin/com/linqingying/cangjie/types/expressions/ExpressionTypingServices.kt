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
import com.linqingying.cangjie.config.LanguageVersionSettings
import com.linqingying.cangjie.descriptors.BindingTrace
import com.linqingying.cangjie.descriptors.FunctionDescriptor
import com.linqingying.cangjie.descriptors.impl.FunctionDescriptorImpl
import com.linqingying.cangjie.descriptors.impl.PropertyAccessorDescriptorImpl
import com.linqingying.cangjie.psi.*
import com.linqingying.cangjie.resolve.*
import com.linqingying.cangjie.resolve.calls.components.InferenceSession
import com.linqingying.cangjie.resolve.calls.components.InferenceSession.Companion.default
import com.linqingying.cangjie.resolve.calls.context.ContextDependency
import com.linqingying.cangjie.resolve.calls.context.ResolutionContext
import com.linqingying.cangjie.resolve.calls.smartcasts.DataFlowInfo
import com.linqingying.cangjie.resolve.calls.smartcasts.DataFlowInfo.Companion.EMPTY
import com.linqingying.cangjie.resolve.calls.tower.CangJieResolutionCallbacksImpl
import com.linqingying.cangjie.resolve.scopes.*
import com.linqingying.cangjie.resolve.source.PsiSourceElement
import com.linqingying.cangjie.types.CangJieType
import com.linqingying.cangjie.types.ErrorUtils.createErrorType
import com.linqingying.cangjie.types.error.ErrorTypeKind
import com.linqingying.cangjie.types.expressions.typeInfoFactory.createTypeInfo
import com.linqingying.cangjie.types.expressions.typeInfoFactory.noTypeInfo
import com.linqingying.cangjie.types.util.TypeUtils.EXPRESSION_TYPE
import com.linqingying.cangjie.types.util.TypeUtils.NO_EXPECTED_TYPE
import com.linqingying.cangjie.types.util.TypeUtils.UNIT_EXPECTED_TYPE
import com.linqingying.cangjie.types.util.isUnit
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
        inferenceSession: InferenceSession
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
            inferenceSession
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


        if (context.expectedType !== NO_EXPECTED_TYPE && context.expectedType !== EXPRESSION_TYPE) {
            val expectedType: CangJieType
            if (isUnitExpectedType) {
                expectedType = UNIT_EXPECTED_TYPE
            } else {
                expectedType = context.expectedType
            }

            return blockLevelVisitor.getTypeInfo(statementExpression, context.replaceExpectedType(expectedType), true)
        }


        //        if (CjPsiUtil.deparenthesize(statementExpression) instanceof CjLambdaExpression && context.contextDependency == ContextDependency.DEPENDENT) {
//            CangJieTypeInfo typeInfo = createDontCareTypeInfoForNILambda(statementExpression, context);
//            if (typeInfo != null) return typeInfo;
//        }
        context = context.replaceExpectedType(NO_EXPECTED_TYPE)

        if (statementExpression !is CjReturnExpression) {
            val parentDeclaration =
                context.trace.bindingContext.get(
                    BindingContext.DECLARATION_TO_DESCRIPTOR, context.getContextParentOfType(
                        statementExpression,
                        CjDeclaration::class.java
                    )
                )

            var type: CangJieType? = null
            if (parentDeclaration is PropertyAccessorDescriptorImpl) {
                type = parentDeclaration.returnType
            }
            if (parentDeclaration is FunctionDescriptorImpl && (statementExpression.parent is CjFunction || statementExpression.parent is CjPropertyAccessor)) {
                if (parentDeclaration.returnType != null && !isUnit(
                        parentDeclaration.returnType!!
                    )
                ) {
//                    context = context.replaceExpectedType(parentDeclaration.getReturnType());
//fix 修复对于该语句执行时，方法返回值还为推断时出现的类型一致
                    if (parentDeclaration.source is PsiSourceElement && (parentDeclaration.source as PsiSourceElement).psi is CjFunction) {
                        if (((parentDeclaration.source as PsiSourceElement).psi as CjFunction).typeReference != null) {
                            type = parentDeclaration.returnType
                        }
                    }
                }
            }
            if (type != null && !type.isUnit()) {
                context = context.replaceExpectedType(type)
            }
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

    /**
     * Visits block statements propagating data flow information from the first to the last.
     * Determines block returned type and data flow information at the end of the block AND
     * at the nearest jump point from the block beginning.
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
        //        ExpressionTypingContext newContext = context.replaceScope(scope).replaceExpectedType(NO_EXPECTED_TYPE);
        var newContext = context.replaceScope(scope).replaceExpectedType(EXPRESSION_TYPE)


        var result = noTypeInfo(context)

        var beforeJumpInfo = newContext.dataFlowInfo
        var jumpOutPossible = false

        var isFirstStatement = true
        val iterator = block.iterator()
        while (iterator.hasNext()) {
            ProgressManager.checkCanceled()
            // Use filtering trace to keep effect system cache only for one statement
            val traceForSingleStatement: AbstractFilteringTrace = EffectsFilteringTrace(context.trace)

            newContext = newContext.replaceBindingTrace(traceForSingleStatement)


            val statement = iterator.next() as? CjExpression ?: continue
            if (!iterator.hasNext()) {
//                最后一条语句也需要检查类型，虽然在前面如果有return语句而无法到达，但是检查类型是必要的  该分支一定会执行
                result = getTypeOfLastExpressionInBlock(
                    statement, newContext.replaceExpectedType(context.expectedType), coercionStrategyForLastExpression,
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
                            blockExpressionValue, lastExpressionValue /*,
                            expressionTypingComponents.languageVersionSettings*/
                        )
                    )
                }
            } else {
                result = blockLevelVisitor
                    .getTypeInfo(statement, newContext.replaceContextDependency(ContextDependency.INDEPENDENT), true)
            }

            val newDataFlowInfo = result.dataFlowInfo
            // If jump is not possible, we take new data flow info before jump
            if (!jumpOutPossible) {
                beforeJumpInfo = result.jumpFlowInfo
                jumpOutPossible = result.jumpOutPossible
            }
            if (newDataFlowInfo !== newContext.dataFlowInfo) {
                newContext = newContext.replaceDataFlowInfo(newDataFlowInfo)
                // We take current data flow info if jump there is not possible
            }
            blockLevelVisitor = ExpressionTypingVisitorDispatcher.ForBlock(
                expressionTypingComponents,
                annotationChecker, scope
            )

            val ownerDescriptor = scope.ownerDescriptor

            if (isFirstStatement && ownerDescriptor is FunctionDescriptor) {
//                expressionTypingComponents.contractParsingServices.checkContractAndRecordIfPresent(
//                        statementExpression, context.trace, (FunctionDescriptor) ownerDescriptor
//                );
                isFirstStatement = false
            }
        }
        return result.replaceJumpOutPossible(jumpOutPossible).replaceJumpFlowInfo(beforeJumpInfo)
    }

    fun getBlockReturnedType(
        expression: CjBlockExpression,
        context: ExpressionTypingContext,
        isStatement: Boolean
    ): CangJieTypeInfo {
//如方法没有显示指定返回值，推断返回值并更改
//        PsiElement blockParent = expression.getParent();
//        if (blockParent instanceof CjFunction && ((CjFunction) blockParent).getTypeReference() == null) {
//            CangJieType returnType = expressionTypingComponents.functionReturnResolver.resolveFunctionReturn(expression, context);
//            FunctionDescriptor functionDescriptor = context.trace.getBindingContext().get(BindingContext.FUNCTION, blockParent);
//            if (functionDescriptor instanceof FunctionDescriptorImpl) {
//                if (returnType != null) {
//                    ((FunctionDescriptorImpl) functionDescriptor).setReturnType(returnType);
//                    return TypeInfoFactoryKt.createTypeInfo(returnType);
//                }
//            }
//        }

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


        return r
    }

    /*package*/
    fun checkFunctionReturnType(function: CjDeclarationWithBody, context: ExpressionTypingContext) {
        val bodyExpression = function.bodyExpression ?: return

        val blockBody = function.hasBlockBody()
        val newContext =
            if (blockBody //                        ? context.replaceExpectedType(NO_EXPECTED_TYPE)
            )
                context.replaceExpectedType(EXPRESSION_TYPE)
            else
                context

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
