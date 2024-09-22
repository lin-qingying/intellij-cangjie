package com.huawei.cangjie.resolve.calls

import com.huawei.cangjie.builtins.CangJieBuiltIns
import com.huawei.cangjie.builtins.ReflectionTypes
import com.huawei.cangjie.descriptors.BindingTrace
import com.huawei.cangjie.descriptors.ModuleDescriptor
import com.huawei.cangjie.diagnostics.Errors
import com.huawei.cangjie.psi.*
import com.huawei.cangjie.resolve.BindingContextUtils
import com.huawei.cangjie.resolve.StatementFilter
import com.huawei.cangjie.resolve.TypeResolver
import com.huawei.cangjie.resolve.calls.context.CallResolutionContext
import com.huawei.cangjie.resolve.calls.context.CheckArgumentTypesMode
import com.huawei.cangjie.resolve.calls.context.ContextDependency
import com.huawei.cangjie.resolve.calls.context.ResolutionContext
import com.huawei.cangjie.resolve.calls.util.ResolveArgumentsMode
import com.huawei.cangjie.resolve.constants.IntegerLiteralTypeConstructor
import com.huawei.cangjie.resolve.constants.IntegerValueTypeConstructor
import com.huawei.cangjie.resolve.constants.evaluate.ConstantExpressionEvaluator
import com.huawei.cangjie.resolve.scopes.LexicalScope
import com.huawei.cangjie.types.CangJieType
import com.huawei.cangjie.types.checker.CangJieTypeChecker
import com.huawei.cangjie.types.expressions.ExpressionTypingServices
import com.huawei.cangjie.types.expressions.typeInfoFactory.noTypeInfo
import com.huawei.cangjie.types.util.TypeUtils.NO_EXPECTED_TYPE
import com.huawei.cangjie.types.util.TypeUtils.getPrimitiveNumberType
import com.huawei.cangjie.utils.exceptions.CangJieTypeInfo
import jakarta.inject.Inject

class ArgumentTypeResolver //        this.functionPlaceholders = functionPlaceholders;
    (
    private val typeResolver: TypeResolver,
    private val builtIns: CangJieBuiltIns,
    private val reflectionTypes: ReflectionTypes,
    private val constantExpressionEvaluator: ConstantExpressionEvaluator,  //            @NotNull FunctionPlaceholders functionPlaceholders,
    //    @NotNull private final FunctionPlaceholders functionPlaceholders;
    private val moduleDescriptor: ModuleDescriptor,
    private val cangjieTypeChecker: CangJieTypeChecker
) {
    private var expressionTypingServices: ExpressionTypingServices? = null

    fun resolveTypeRefWithDefault(
        returnTypeRef: CjTypeReference?,
        scope: LexicalScope,
        trace: BindingTrace,
        defaultValue: CangJieType?
    ): CangJieType? {
        if (returnTypeRef != null) {
            return typeResolver.resolveType(scope, returnTypeRef, trace, true)
        }
        return defaultValue
    }

    // component dependency cycle
    @Inject
    fun setExpressionTypingServices(expressionTypingServices: ExpressionTypingServices) {
        this.expressionTypingServices = expressionTypingServices
    }

    fun updateResultArgumentTypeIfNotDenotable(
        context: ResolutionContext<*>,
        expression: CjExpression
    ): CangJieType? {
        return updateResultArgumentTypeIfNotDenotable(
            context.trace,
            context.statementFilter,
            context.expectedType,
            expression
        )
    }

    fun updateResultArgumentTypeIfNotDenotable(
        trace: BindingTrace,
        statementFilter: StatementFilter,
        expectedType: CangJieType,
        expression: CjExpression
    ): CangJieType? {
        val type = trace.getType(expression)
        return if (type != null) updateResultArgumentTypeIfNotDenotable(
            trace,
            statementFilter,
            expectedType,
            type,
            expression
        ) else null
    }

    fun updateResultArgumentTypeIfNotDenotable(
        trace: BindingTrace,
        statementFilter: StatementFilter,
        expectedType: CangJieType,
        targetType: CangJieType,
        expression: CjExpression
    ): CangJieType? {
        val typeConstructor = targetType.constructor
        if (!typeConstructor.isDenotable) {
            if (typeConstructor is IntegerValueTypeConstructor) {
                val primitiveType = getPrimitiveNumberType(typeConstructor, expectedType)
                constantExpressionEvaluator.updateNumberType(primitiveType, expression, statementFilter, trace)
                return primitiveType
            }
            if (typeConstructor is IntegerLiteralTypeConstructor) {
                val primitiveType = getPrimitiveNumberType(typeConstructor, expectedType)
                constantExpressionEvaluator.updateNumberType(primitiveType, expression, statementFilter, trace)
                return primitiveType
            }
        }
        return null
    }

    private fun checkArgumentTypeWithNoCallee(context: CallResolutionContext<*>, argumentExpression: CjExpression) {
        expressionTypingServices!!.getTypeInfo(argumentExpression, context.replaceExpectedType(NO_EXPECTED_TYPE))
        updateResultArgumentTypeIfNotDenotable(context, argumentExpression)
    }

    fun checkTypesWithNoCallee(
        context: CallResolutionContext<*>
    ) {
        if (context.checkArguments != CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS) return

        for (valueArgument in context.call.valueArguments) {
            val argumentExpression = valueArgument.getArgumentExpression()
            if (argumentExpression != null && argumentExpression !is CjLambdaExpression) {
                checkArgumentTypeWithNoCallee(context, argumentExpression)
            }
        }

        checkTypesForFunctionArgumentsWithNoCallee(context)

        for (typeProjection in context.call.typeArguments) {
            val typeReference = typeProjection.typeReference
            if (typeReference == null) {
                context.trace.report(Errors.PROJECTION_ON_NON_CLASS_TYPE_ARGUMENT.on(typeProjection))
            } else {
                typeResolver.resolveType(context.scope, typeReference, context.trace, true)
            }
        }
    }

    private fun checkTypesForFunctionArgumentsWithNoCallee(context: CallResolutionContext<*>) {
        if (context.checkArguments != CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS) return

        for (valueArgument in context.call.valueArguments) {
            val argumentExpression = valueArgument.getArgumentExpression()
            if (argumentExpression != null && isFunctionLiteralArgument(argumentExpression, context)) {
                checkArgumentTypeWithNoCallee(context, argumentExpression)
            }
        }
    }

    /**
     * Visits function call arguments and determines data flow information changes
     */
    fun analyzeArgumentsAndRecordTypes(
        context: CallResolutionContext<*>, resolveArgumentsMode: ResolveArgumentsMode
    ) {
        val infoForArguments = context.dataFlowInfoForArguments
        val call = context.call

        for (argument in call.valueArguments) {
            val expression = argument.getArgumentExpression() ?: continue

            if (isCollectionLiteralInsideAnnotation(expression, context)) {
                continue
            }

            val newContext = context.replaceDataFlowInfo(infoForArguments.getInfo(argument))
            // Here we go inside arguments and determine additional data flow information for them
            val typeInfoForCall = getArgumentTypeInfo(expression, newContext, resolveArgumentsMode, false)
            infoForArguments.updateInfo(argument, typeInfoForCall.dataFlowInfo)
        }
    }

    //    @NotNull
    //    public CangJieTypeInfo getFunctionLiteralTypeInfo(
    //            @NotNull CjExpression expression,
    //            @NotNull CjFunction functionLiteral,
    //            @NotNull CallResolutionContext<?> context,
    //            @NotNull ResolveArgumentsMode resolveArgumentsMode,
    //            bool suspendFunctionTypeExpected
    //    ) {
    //        if (resolveArgumentsMode == SHAPE_FUNCTION_ARGUMENTS) {
    //            CangJieType type = getShapeTypeOfFunctionLiteral(functionLiteral, context.scope, context.trace, true, suspendFunctionTypeExpected);
    //            return TypeInfoFactoryKt.createTypeInfo(type, context);
    //        }
    //        return expressionTypingServices.getTypeInfo(expression, context.replaceContextDependency(ContextDependency.INDEPENDENT));
    //    }
    fun getArgumentTypeInfo(
        expression: CjExpression?,
        context: CallResolutionContext<*>,
        resolveArgumentsMode: ResolveArgumentsMode,
        suspendFunctionTypeExpected: Boolean
    ): CangJieTypeInfo {
        if (expression == null) {
            return noTypeInfo(context)
        }

        val functionLiteralArgument = getFunctionLiteralArgumentIfAny(expression, context)

        //        if (functionLiteralArgument != null) {
//            return getFunctionLiteralTypeInfo(expression, functionLiteralArgument, context, resolveArgumentsMode, suspendFunctionTypeExpected);
//        }

//        CjCallableReferenceExpression callableReferenceExpression = getCallableReferenceExpressionIfAny(expression, context);
//        if (callableReferenceExpression != null) {
//            return getCallableReferenceTypeInfo(expression, callableReferenceExpression, context, resolveArgumentsMode);
//        }
        if (isCollectionLiteralInsideAnnotation(expression, context)) {
            // We assume that there is only one candidate resolver for annotation call
            // And to resolve collection literal correctly, we need mapping of argument to parameter to get expected type and
            // to choose corresponding call (i.e arrayOf/intArrayOf...)
            val newContext: ResolutionContext<*> = context.replaceContextDependency(ContextDependency.INDEPENDENT)
            return expressionTypingServices!!.getTypeInfo(expression, newContext)
        }

        //        // TODO: probably should be "is unsigned type or is supertype of unsigned type" to support Comparable<UInt> expected types too
//        if (UnsignedTypes.INSTANCE.isUnsignedType(context.expectedType)) {
//            convertSignedConstantToUnsigned(expression, context);
//        }
        val recordedTypeInfo = BindingContextUtils.getRecordedTypeInfo(expression, context.trace.bindingContext)
        if (recordedTypeInfo != null) {
            return recordedTypeInfo
        }

        val newContext: ResolutionContext<*> = context.replaceExpectedType(NO_EXPECTED_TYPE).replaceContextDependency(
            ContextDependency.DEPENDENT
        )

        return expressionTypingServices!!.getTypeInfo(expression, newContext)
    }

    companion object {
        private fun isCollectionLiteralInsideAnnotation(
            expression: CjExpression,
            context: CallResolutionContext<*>
        ): Boolean {
            return expression is CjCollectionLiteralExpression && context.call.callElement is CjAnnotationEntry
        }

        @JvmStatic
        fun isCollectionLiteralArgument(expression: CjExpression): Boolean {
            return expression is CjCollectionLiteralExpression
        }

        fun isCallableReferenceArgument(
            expression: CjExpression, statementFilter: StatementFilter
        ): Boolean {
            return getCallableReferenceExpressionIfAny(expression, statementFilter) != null
        }

        @JvmStatic
        fun isCallableReferenceArgument(
            expression: CjExpression, context: ResolutionContext<*>
        ): Boolean {
            return isCallableReferenceArgument(expression, context.statementFilter)
        }

        private fun isFunctionLiteralArgument(
            expression: CjExpression, statementFilter: StatementFilter
        ): Boolean {
            return getFunctionLiteralArgumentIfAny(expression, statementFilter) != null
        }

        fun isFunctionLiteralOrCallableReference(
            expression: CjExpression, statementFilter: StatementFilter
        ): Boolean {
            return isFunctionLiteralArgument(expression, statementFilter) || isCallableReferenceArgument(
                expression,
                statementFilter
            )
        }

        @JvmStatic
        fun isFunctionLiteralOrCallableReference(
            expression: CjExpression, context: ResolutionContext<*>
        ): Boolean {
            return isFunctionLiteralOrCallableReference(expression, context.statementFilter)
        }

        @JvmStatic
        fun isFunctionLiteralArgument(
            expression: CjExpression, context: ResolutionContext<*>
        ): Boolean {
            return isFunctionLiteralArgument(expression, context.statementFilter)
        }

        fun getCallableReferenceExpressionIfAny(
            expression: CjExpression,
            statementFilter: StatementFilter
        ): CjCallableReferenceExpression? {
            val deparenthesizedExpression = CjPsiUtil.getLastElementDeparenthesized(expression, statementFilter)
            if (deparenthesizedExpression is CjCallableReferenceExpression) {
                return deparenthesizedExpression
            }
            return null
        }

        fun getCallableReferenceExpressionIfAny(
            expression: CjExpression,
            context: ResolutionContext<*>
        ): CjCallableReferenceExpression? {
            return getCallableReferenceExpressionIfAny(expression, context.statementFilter)
        }

        fun getFunctionLiteralArgumentIfAny(
            expression: CjExpression, statementFilter: StatementFilter
        ): CjFunction? {
            val deparenthesizedExpression = CjPsiUtil.getLastElementDeparenthesized(expression, statementFilter)
            if (deparenthesizedExpression is CjLambdaExpression) {
                return deparenthesizedExpression.functionLiteral
            }
            if (deparenthesizedExpression is CjFunction) {
                return deparenthesizedExpression
            }
            return null
        }

        fun getFunctionLiteralArgumentIfAny(
            expression: CjExpression, context: ResolutionContext<*>
        ): CjFunction? {
            return getFunctionLiteralArgumentIfAny(expression, context.statementFilter)
        }
    }
}
