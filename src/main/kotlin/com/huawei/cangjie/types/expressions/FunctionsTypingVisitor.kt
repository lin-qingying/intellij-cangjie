package com.huawei.cangjie.types.expressions

import com.google.common.collect.Lists
import  com.huawei.cangjie.builtins.createFunctionType
import com.huawei.cangjie.builtins.CangJieBuiltIns
import com.huawei.cangjie.builtins.getReturnTypeFromFunctionType
import com.huawei.cangjie.builtins.isBuiltinFunctionalType
import com.huawei.cangjie.config.LanguageFeature
import com.huawei.cangjie.descriptors.BindingTrace
import com.huawei.cangjie.descriptors.CallableMemberDescriptor
import com.huawei.cangjie.diagnostics.Errors.*
import com.huawei.cangjie.descriptors.PsiDiagnosticUtils
import com.huawei.cangjie.descriptors.SimpleFunctionDescriptor
import com.huawei.cangjie.descriptors.annotations.Annotations
import com.huawei.cangjie.descriptors.impl.AnonymousFunctionDescriptor
import com.huawei.cangjie.descriptors.impl.SimpleFunctionDescriptorImpl
import com.huawei.cangjie.psi.*
import com.huawei.cangjie.resolve.BindingContext
import com.huawei.cangjie.resolve.BindingContext.EXPECTED_RETURN_TYPE
import com.huawei.cangjie.resolve.BindingContextUtils
import com.huawei.cangjie.resolve.FunctionDescriptorUtil
import com.huawei.cangjie.resolve.calls.context.ContextDependency
import com.huawei.cangjie.resolve.calls.inference.model.TypeVariableTypeConstructor
import com.huawei.cangjie.resolve.check.UnderscoreChecker
import com.huawei.cangjie.resolve.lazy.ForceResolveUtil
import com.huawei.cangjie.resolve.scopes.LexicalWritableScope
import com.huawei.cangjie.resolve.source.toSourceElement
import com.huawei.cangjie.types.CangJieType
import com.huawei.cangjie.types.CommonSupertypes
import com.huawei.cangjie.types.checker.TrailingCommaChecker
import com.huawei.cangjie.types.expressions.typeInfoFactory.createTypeInfo
import com.huawei.cangjie.types.util.TypeUtils
import com.huawei.cangjie.types.util.TypeUtils.CANNOT_INFER_FUNCTION_PARAM_TYPE
import com.huawei.cangjie.types.util.TypeUtils.NO_EXPECTED_TYPE
import com.huawei.cangjie.types.util.contains
import com.huawei.cangjie.utils.addIfNotNull
import com.huawei.cangjie.utils.exceptions.CangJieTypeInfo

internal class FunctionsTypingVisitor(facade: ExpressionTypingInternals) : ExpressionTypingVisitor(facade) {

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
                context.scope.ownerDescriptor, context.scope, function, context.trace, context.dataFlowInfo, context.inferenceSession
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
            FunctionDescriptorUtil.getFunctionInnerScope(context.scope, functionDescriptor, context.trace, components.overloadChecker)
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

        components.modifiersChecker.withTrace(context.trace).checkModifiersForLocalDeclaration(function, functionDescriptor)
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
        val functionTypeExpected = expectedType.isBuiltinFunctionalType

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
            functionDescriptor.createFunctionType(components.builtIns )!!
        )

//        if (context.inferenceSession is BuilderInferenceSession) {
//            context.inferenceSession.addExpression(expression)
//        }

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
        val expectedReturnType = if (functionTypeExpected) context.expectedType.getReturnTypeFromFunctionType() else null
        val returnType = computeUnsafeReturnType(expression, context, functionDescriptor, expectedReturnType)

        if (!expression.functionLiteral.hasDeclaredReturnType() && functionTypeExpected) {
            if (!TypeUtils.noExpectedType(expectedReturnType!!) && CangJieBuiltIns.isUnit(expectedReturnType)) {
                return components.builtIns.unitType
            }
        }
        return returnType ?: CANNOT_INFER_FUNCTION_PARAM_TYPE
    }

    private fun computeUnsafeReturnType(
        expression: CjLambdaExpression,
        context: ExpressionTypingContext,
        functionDescriptor: SimpleFunctionDescriptorImpl,
        expectedReturnType: CangJieType?
    ): CangJieType? {
        val functionLiteral = expression.functionLiteral

        val expectedType = expectedReturnType ?: NO_EXPECTED_TYPE
        val functionInnerScope =
            FunctionDescriptorUtil.getFunctionInnerScope(context.scope, functionDescriptor, context.trace, components.overloadChecker)
        var newContext = context.replaceScope(functionInnerScope).replaceExpectedType(expectedType)

        // This is needed for ControlStructureTypingVisitor#visitReturnExpression() to properly type-check returned expressions
        context.trace.record(EXPECTED_RETURN_TYPE, functionLiteral, expectedType)

        val newInferenceLambdaInfo = context.trace[BindingContext.NEW_INFERENCE_LAMBDA_INFO, expression.functionLiteral]

        // i.e. this lambda isn't call arguments
        if (newInferenceLambdaInfo == null && context.languageVersionSettings.supportsFeature(LanguageFeature.NewInference)) {
            newContext = newContext.replaceContextDependency(ContextDependency.INDEPENDENT)
        }

        // Type-check the body
        val blockReturnedType =
            components.expressionTypingServices.getBlockReturnedType(functionLiteral.bodyExpression!!,CoercionStrategy. COERCION_TO_UNIT, newContext)
        val typeOfBodyExpression = blockReturnedType.type

        newInferenceLambdaInfo?.let {
            it.lastExpressionInfo.dataFlowInfoAfter = blockReturnedType.dataFlowInfo
        }

        return computeReturnTypeBasedOnReturnExpressions(functionLiteral, context, typeOfBodyExpression)
    }

    private fun collectReturns(functionLiteral: CjFunctionLiteral, trace: BindingTrace): Collection<CjReturnExpression> {
        val result = Lists.newArrayList<CjReturnExpression>()
        val bodyExpression = functionLiteral.bodyExpression
        bodyExpression?.accept(object : CjTreeVisitor<MutableList<CjReturnExpression>>() {
            override fun visitReturnExpression(
                expression: CjReturnExpression,
                insideActualFunction: MutableList<CjReturnExpression>
            ): Void? {
                insideActualFunction.add(expression)
                return null
            }
        }, result)
        return result.filter {
            // No label => non-local return
            // Either a local return of inner lambda/function or a non-local return
            it.getTargetLabel()?.let { trace.get(BindingContext.LABEL_TARGET, it) } == functionLiteral
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
        if (returnedExpressionTypes.any { it.contains { it.constructor is TypeVariableTypeConstructor }}) return null
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
