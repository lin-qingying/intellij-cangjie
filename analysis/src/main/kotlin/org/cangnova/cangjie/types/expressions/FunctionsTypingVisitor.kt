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
import org.cangnova.cangjie.types.isBuiltinFunctionalType
import com.google.common.collect.Lists
import org.cangnova.cangjie.builtins.CangJieBuiltIns
import org.cangnova.cangjie.config.LanguageFeature
import org.cangnova.cangjie.descriptors.CallableMemberDescriptor
import org.cangnova.cangjie.descriptors.SimpleFunctionDescriptor
import org.cangnova.cangjie.descriptors.annotations.Annotations
import org.cangnova.cangjie.descriptors.impl.AnonymousFunctionDescriptor
import org.cangnova.cangjie.descriptors.impl.SimpleFunctionDescriptorImpl
import org.cangnova.cangjie.diagnostics.PsiDiagnosticUtils

import org.cangnova.cangjie.diagnostics.infos.errors.*
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.resolve.FunctionDescriptorUtil
import org.cangnova.cangjie.resolve.binding.BindingContext
import org.cangnova.cangjie.resolve.binding.BindingContext.Companion.EXPECTED_RETURN_TYPE
import org.cangnova.cangjie.resolve.binding.BindingContextUtils
import org.cangnova.cangjie.resolve.binding.BindingTrace
import org.cangnova.cangjie.resolve.calls.context.ContextDependency
import org.cangnova.cangjie.resolve.calls.inference.BuilderInferenceSession
import org.cangnova.cangjie.resolve.calls.inference.model.TypeVariableTypeConstructor
import org.cangnova.cangjie.resolve.check.UnderscoreChecker
import org.cangnova.cangjie.resolve.lazy.ForceResolveUtil
import org.cangnova.cangjie.resolve.scopes.LexicalWritableScope
import org.cangnova.cangjie.resolve.source.toSourceElement
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.CommonSupertypes
import org.cangnova.cangjie.types.TypeUtils.CANNOT_INFER_FUNCTION_PARAM_TYPE
import org.cangnova.cangjie.types.TypeUtils.NO_EXPECTED_TYPE
import org.cangnova.cangjie.types.TypeUtils.noExpectedType
import org.cangnova.cangjie.types.checker.CangJieTypeChecker
import org.cangnova.cangjie.types.checker.SimpleClassicTypeSystemContext.isUnit
import org.cangnova.cangjie.types.checker.TrailingCommaChecker
import org.cangnova.cangjie.types.contains
import org.cangnova.cangjie.types.createFunctionType
import org.cangnova.cangjie.types.expressions.typeInfoFactory.createTypeInfo
import org.cangnova.cangjie.types.getReturnTypeFromFunctionType
import org.cangnova.cangjie.utils.addIfNotNull

class FunctionsTypingVisitor(facade: ExpressionTypingInternals) : ExpressionTypingVisitor(facade) {
    fun checkTypesForReturnStatements(
        function: CjDeclarationWithBody,
        trace: BindingTrace,
        actualReturnType: CangJieType
    ) {
        if (function.hasBlockBody()) return
        if ((function !is CjNamedFunction || function.typeReference != null)
            && (function !is CjPropertyAccessor || function.returnTypeReference == null)
        ) return

        for (returnForCheck in collectReturns(function, trace)) {
            val expression = returnForCheck.returnedExpression
            if (expression == null) {
                if (!actualReturnType.isUnit()) {
                    trace.report(RETURN_TYPE_MISMATCH.on(returnForCheck, actualReturnType))
                }
                continue
            }

            val expressionType = trace.getType(expression) ?: continue
            if (!CangJieTypeChecker.DEFAULT.isSubtypeOf(expressionType, actualReturnType)) {
                trace.report(TYPE_MISMATCH.on(expression, expressionType, actualReturnType))
            }
        }
    }

    fun visitNamedFunction(
        function: CjNamedFunction,
        context: ExpressionTypingContext,
        isDeclaration: Boolean,
        statementScope: LexicalWritableScope? // must be not null if isDeclaration
    ): CangJieTypeInfo {
        if (!isDeclaration) {
            // function expression
            if (function.typeParameters.isNotEmpty()) {
                context.trace.report(TYPE_PARAMETERS_NOT_ALLOWED.on(function))
            }

            if (function.name != null) {
                context.trace.report(ANONYMOUS_FUNCTION_WITH_NAME.on(function.nameIdentifier!!))
            }

            for (parameter in function.valueParameters) {
                if (parameter.hasDefaultValue()) {
                    context.trace.report(ANONYMOUS_FUNCTION_PARAMETER_WITH_DEFAULT_VALUE.on(parameter))
                }
//                if (parameter.isVarArg) {
//                    context.trace.report(USELESS_VARARG_ON_PARAMETER.on(parameter))
//                }
            }
        }

        val functionDescriptor: SimpleFunctionDescriptor
        if (isDeclaration) {
            functionDescriptor = components.functionDescriptorResolver.resolveFunctionDescriptor(
                context.scope.ownerDescriptor,
                context.scope,
                function,
                context.trace,
                context.dataFlowInfo,
                context.inferenceSession
            )
            assert(statementScope != null) {
                "statementScope must be not null for function: " + function.name + " at location " + PsiDiagnosticUtils.atLocation(
                    function
                )
            }
            statementScope!!.addFunctionDescriptor(functionDescriptor)
        } else {
            functionDescriptor = components.functionDescriptorResolver.resolveFunctionExpressionDescriptor(
                context.scope.ownerDescriptor, context.scope, function,
                context.trace, context.dataFlowInfo, context.expectedType, context.inferenceSession
            )
        }
        // Necessary for local functions
        ForceResolveUtil.forceResolveAllContents(functionDescriptor.annotations)

        val functionInnerScope =
            FunctionDescriptorUtil.getFunctionInnerScope(
                context.scope,
                functionDescriptor,
                context.trace,
                components.overloadChecker
            )
        if (!function.hasDeclaredReturnType() && !function.hasBlockBody()) {
            ForceResolveUtil.forceResolveAllContents(functionDescriptor.returnType)
        } else {
            components.expressionTypingServices.checkFunctionReturnType(
                functionInnerScope, function, functionDescriptor, context.dataFlowInfo, null, context.trace, context
            )
        }

        components.valueParameterResolver.resolveValueParameters(
            function.valueParameters, functionDescriptor.valueParameters, functionInnerScope,
            context.dataFlowInfo, context.trace, context.inferenceSession
        )

        components.modifiersChecker.withTrace(context.trace)
            .checkModifiersForLocalDeclaration(function, functionDescriptor)
        components.identifierChecker.checkDeclaration(function, context.trace)
//        components.declarationsCheckerBuilder.withTrace(context.trace).checkFunction(function, functionDescriptor)

        return if (isDeclaration) {
            createTypeInfo(components.dataFlowAnalyzer.checkStatementType(function, context), context)
        } else {
            val newInferenceEnabled = components.languageVersionSettings.supportsFeature(LanguageFeature.NewInference)

            // We forbid anonymous function expressions to suspend type coercion for now, until `suspend fun` syntax is supported
            val resultType = functionDescriptor.createFunctionType(
                components.builtIns,

                )

            if (newInferenceEnabled) {
                // We should avoid type checking for types containing `NO_EXPECTED_TYPE`, the error will be report later if needed
                if (!context.expectedType.contains { it === NO_EXPECTED_TYPE }) {
                    /*
                     * We do type checking without converted vararg type as the new inference create expected type with raw vararg type (see CangJieResolutionCallbacksImpl.cj)
                     * Example:
                     *      fun foo(x: Any?) {}
                     *      val x = foo(fun(vararg p: Int) {})
                     *      In NI, context.expectedType = `Function1<Int, Unit>`
                     */
                    val typeToTypeCheck = functionDescriptor.createFunctionType(
                        components.builtIns,
//                        suspendFunction = false,
//                        shouldUseVarargType = true
                    )
                    components.dataFlowAnalyzer.checkType(typeToTypeCheck, function, context)
                }
                createTypeInfo(resultType, context)
            } else {
                components.dataFlowAnalyzer.createCheckedTypeInfo(resultType, context, function)
            }
        }
    }


    private fun createFunctionLiteralDescriptor(
        expression: CjLambdaExpression,
        context: ExpressionTypingContext
    ): AnonymousFunctionDescriptor {
        val functionLiteral = expression.functionLiteral
        val functionDescriptor = AnonymousFunctionDescriptor(
            context.scope.ownerDescriptor,
            Annotations.EMPTY,
//            components.annotationResolver.resolveAnnotationsWithArguments(context.scope, expression.getAnnotationEntries(), context.trace),
            CallableMemberDescriptor.Kind.DECLARATION, functionLiteral.toSourceElement(),

            ).let {
            facade.components.typeResolutionInterceptor.interceptFunctionLiteralDescriptor(expression, context, it)
        }
        components.functionDescriptorResolver.initializeFunctionDescriptorAndExplicitReturnType(
            context.scope.ownerDescriptor, context.scope, functionLiteral,
            functionDescriptor, context.trace, context.expectedType, context.dataFlowInfo, context.inferenceSession
        )
        for (parameterDescriptor in functionDescriptor.valueParameters) {
            ForceResolveUtil.forceResolveAllContents(parameterDescriptor.annotations)
        }
        BindingContextUtils.recordFunctionDeclarationToDescriptor(context.trace, functionLiteral, functionDescriptor)
        return functionDescriptor
    }


    override fun visitLambdaExpression(
        expression: CjLambdaExpression,
        context: ExpressionTypingContext
    ): CangJieTypeInfo? {

//        if (!components.languageVersionSettings.supportsFeature(LanguageFeature.YieldIsNoMoreReserved)) {
//            checkReservedYieldBeforeLambda(expression, context.trace)
//        }
        if (!expression.functionLiteral.hasBody()) return null

        val expectedType = context.expectedType
        val functionTypeExpected = expectedType.isBuiltinFunctionalType()

        val functionDescriptor = createFunctionLiteralDescriptor(expression, context)
        expression.valueParameters.forEach {
            components.identifierChecker.checkDeclaration(it, context.trace)
            UnderscoreChecker.checkNamed(
                it,
                context.trace,
                components.languageVersionSettings,
                allowSingleUnderscore = true
            )
        }

        val valueParameterList = expression.functionLiteral.valueParameterList
        if (valueParameterList?.stub == null) {
            TrailingCommaChecker.check(
                valueParameterList?.trailingComma,
                context.trace,
                context.languageVersionSettings
            )
        }

        val safeReturnType = computeReturnType(expression, context, functionDescriptor, functionTypeExpected)
        functionDescriptor.setReturnType(safeReturnType)

        val resultType = components.typeResolutionInterceptor.interceptType(
            expression,
            context,
            functionDescriptor.createFunctionType(components.builtIns)!!
        )

        if (context.inferenceSession is BuilderInferenceSession) {
            context.inferenceSession.addExpression(expression)
        }

        if (functionTypeExpected) {
            // all checks were done before
            return createTypeInfo(resultType, context)
        }

        return components.dataFlowAnalyzer.createCheckedTypeInfo(resultType, context, expression)
    }

    private fun computeReturnType(
        expression: CjLambdaExpression,
        context: ExpressionTypingContext,
        functionDescriptor: SimpleFunctionDescriptorImpl,
        functionTypeExpected: Boolean
    ): CangJieType {
        val expectedReturnType =
            if (functionTypeExpected) context.expectedType.getReturnTypeFromFunctionType() else null
        val returnType = computeUnsafeReturnType(expression, context, functionDescriptor, expectedReturnType)

        if (!expression.functionLiteral.hasDeclaredReturnType() && functionTypeExpected) {
            if (!noExpectedType(expectedReturnType!!) && CangJieBuiltIns.isUnit(expectedReturnType)) {
                return components.builtIns.unitType
            }
        }
        return returnType ?: CANNOT_INFER_FUNCTION_PARAM_TYPE
    }

    /**
     * 计算lambda表达式的不安全返回类型。
     *
     * @param expression lambda表达式
     * @param context 表达式类型检查上下文
     * @param functionDescriptor 函数描述符
     * @param expectedReturnType 预期的返回类型，可选
     * @return 计算得到的返回类型，可能为null
     */
    private fun computeUnsafeReturnType(
        expression: CjLambdaExpression,
        context: ExpressionTypingContext,
        functionDescriptor: SimpleFunctionDescriptorImpl,
        expectedReturnType: CangJieType?
    ): CangJieType? {
        val functionLiteral = expression.functionLiteral

        // 获取预期的返回类型，如果没有则使用默认值NO_EXPECTED_TYPE
        val expectedType = expectedReturnType ?: NO_EXPECTED_TYPE

        // 获取函数内部作用域
        val functionInnerScope =
            FunctionDescriptorUtil.getFunctionInnerScope(
                context.scope,
                functionDescriptor,
                context.trace,
                components.overloadChecker
            )

        // 替换上下文的作用域和预期类型
        var newContext = context.replaceScope(functionInnerScope).replaceExpectedType(expectedType)

        // 记录预期返回类型，以便于ControlStructureTypingVisitor#visitReturnExpression()正确类型检查返回表达式
        context.trace.record(EXPECTED_RETURN_TYPE, functionLiteral, expectedType)

        // 获取新的推断lambda信息
        val newInferenceLambdaInfo = context.trace[BindingContext.NEW_INFERENCE_LAMBDA_INFO, expression.functionLiteral]

        // 如果lambda不是调用参数，并且支持新推断功能，则更新上下文依赖关系
        if (newInferenceLambdaInfo == null && context.languageVersionSettings.supportsFeature(LanguageFeature.NewInference)) {
            newContext = newContext.replaceContextDependency(ContextDependency.INDEPENDENT)
        }

        // 类型检查函数体
        val blockReturnedType =
            components.expressionTypingServices.getBlockReturnedType(
                functionLiteral.bodyExpression!!,
                CoercionStrategy.COERCION_TO_UNIT,
                newContext
            )
        val typeOfBodyExpression = blockReturnedType.type

        // 更新新的推断lambda信息的数据流信息
        newInferenceLambdaInfo?.let {
            it.lastExpressionInfo.dataFlowInfoAfter = blockReturnedType.dataFlowInfo
        }

        // 根据返回表达式计算最终的返回类型
        return computeReturnTypeBasedOnReturnExpressions(functionLiteral, context, typeOfBodyExpression)
    }


    private fun collectReturns(function: CjDeclarationWithBody, trace: BindingTrace): List<CjReturnExpression> {
        val bodyExpression = function.bodyExpression ?: return emptyList()
        val returns = ArrayList<CjReturnExpression>()

        bodyExpression.accept(object : CjTreeVisitor<Boolean>() {
            override fun visitReturnExpression(expression: CjReturnExpression, insideActualFunction: Boolean) {
                val labelTarget = expression.getTargetLabel()?.let { trace[BindingContext.LABEL_TARGET, it] }
                if (labelTarget == function || (labelTarget == null && insideActualFunction)) {
                    returns.add(expression)
                }

                super.visitReturnExpression(expression, insideActualFunction)
            }

            override fun visitNamedFunction(function: CjNamedFunction, data: Boolean) {
                super.visitNamedFunction(function, false)
            }

            override fun visitPropertyAccessor(accessor: CjPropertyAccessor, data: Boolean) {
                super.visitPropertyAccessor(accessor, false)
            }

            override fun visitAnonymousInitializer(initializer: CjAnonymousInitializer, data: Boolean) {
                super.visitAnonymousInitializer(initializer, false)
            }
        }, true)

        return returns
    }

    private fun collectReturns(
        functionLiteral: CjFunctionLiteral,
        trace: BindingTrace
    ): Collection<CjReturnExpression> {
        val result = Lists.newArrayList<CjReturnExpression>()
        val bodyExpression = functionLiteral.bodyExpression
        bodyExpression?.accept(object : CjTreeVisitor<MutableList<CjReturnExpression>>() {
            override fun visitReturnExpression(
                expression: CjReturnExpression,
                insideActualFunction: MutableList<CjReturnExpression>
            ) {
                insideActualFunction.add(expression)

            }
        }, result)
        return result.filter {
            // No label => non-local return
            // Either a local return of inner lambda/function or a non-local return
            it.getTargetLabel()
                ?.let { simpleNameExpression -> trace[BindingContext.LABEL_TARGET, simpleNameExpression] } == functionLiteral
        }
    }

    private fun computeReturnTypeBasedOnReturnExpressions(
        functionLiteral: CjFunctionLiteral,
        context: ExpressionTypingContext,
        typeOfBodyExpression: CangJieType?
    ): CangJieType? {
        val returnedExpressionTypes = Lists.newArrayList<CangJieType>()

        var hasEmptyReturn = false
        val returnExpressions = collectReturns(functionLiteral, context.trace)
        for (returnExpression in returnExpressions) {
            val returnedExpression = returnExpression.returnedExpression
            if (returnedExpression == null) {
                hasEmptyReturn = true
            } else {
                // the type should have been computed by getBlockReturnedType() above, but can be null, if returnExpression contains some error
                returnedExpressionTypes.addIfNotNull(context.trace.getType(returnedExpression))
            }
        }

        if (hasEmptyReturn) {
            for (returnExpression in returnExpressions) {
                val returnedExpression = returnExpression.returnedExpression
                if (returnedExpression != null) {
                    val type = context.trace.getType(returnedExpression)
                    if (type == null || !CangJieBuiltIns.isUnit(type)) {
                        context.trace.report(RETURN_TYPE_MISMATCH.on(returnedExpression, components.builtIns.unitType))
                    }
                }
            }
            return components.builtIns.unitType
        }
        returnedExpressionTypes.addIfNotNull(typeOfBodyExpression)

        if (returnedExpressionTypes.isEmpty()) return null
        if (returnedExpressionTypes.any { cangJieType -> cangJieType.contains { it.constructor is TypeVariableTypeConstructor } }) return null
        return CommonSupertypes.commonSupertype(returnedExpressionTypes)
    }
}

fun SimpleFunctionDescriptor.createFunctionType(
    builtIns: CangJieBuiltIns,

    ): CangJieType? {

    return createFunctionType(
        builtIns,
        Annotations.EMPTY,
        extensionReceiverParameter?.type,
        contextReceiverParameters.map { it.type },
        valueParameters.map { it.type },
        null,
        returnType ?: return null,
//        suspendFunction = suspendFunction
    )
}
private fun CangJieType.isBuiltinFunctionalType() =
    !noExpectedType(this) && isBuiltinFunctionalType
