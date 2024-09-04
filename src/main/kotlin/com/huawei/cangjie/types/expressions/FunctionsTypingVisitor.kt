package com.huawei.cangjie.types.expressions

import com.google.common.collect.Lists
import  com.huawei.cangjie.builtins.createFunctionType
import com.huawei.cangjie.builtins.CangJieBuiltIns
import com.huawei.cangjie.builtins.getReturnTypeFromFunctionType
import com.huawei.cangjie.builtins.isBuiltinFunctionalType
import com.huawei.cangjie.config.LanguageFeature
import com.huawei.cangjie.descriptors.BindingTrace
import com.huawei.cangjie.descriptors.CallableMemberDescriptor
import com.huawei.cangjie.descriptors.Errors.RETURN_TYPE_MISMATCH
import com.huawei.cangjie.descriptors.SimpleFunctionDescriptor
import com.huawei.cangjie.descriptors.annotations.Annotations
import com.huawei.cangjie.descriptors.impl.AnonymousFunctionDescriptor
import com.huawei.cangjie.descriptors.impl.SimpleFunctionDescriptorImpl
import com.huawei.cangjie.psi.CjFunctionLiteral
import com.huawei.cangjie.psi.CjLambdaExpression
import com.huawei.cangjie.psi.CjReturnExpression
import com.huawei.cangjie.psi.CjTreeVisitor
import com.huawei.cangjie.resolve.BindingContext
import com.huawei.cangjie.resolve.BindingContext.EXPECTED_RETURN_TYPE
import com.huawei.cangjie.resolve.BindingContextUtils
import com.huawei.cangjie.resolve.FunctionDescriptorUtil
import com.huawei.cangjie.resolve.calls.context.ContextDependency
import com.huawei.cangjie.resolve.calls.inference.model.TypeVariableTypeConstructor
import com.huawei.cangjie.resolve.check.UnderscoreChecker
import com.huawei.cangjie.resolve.lazy.ForceResolveUtil
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
