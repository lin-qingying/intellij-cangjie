package com.huawei.cangjie.resolve.calls;

import com.huawei.cangjie.builtins.CangJieBuiltIns;
import com.huawei.cangjie.builtins.ReflectionTypes;
import com.huawei.cangjie.descriptors.BindingTrace;
import com.huawei.cangjie.descriptors.Errors;
import com.huawei.cangjie.descriptors.ModuleDescriptor;
import com.huawei.cangjie.psi.*;
import com.huawei.cangjie.resolve.StatementFilter;
import com.huawei.cangjie.resolve.TypeResolver;
import com.huawei.cangjie.resolve.calls.context.CallResolutionContext;
import com.huawei.cangjie.resolve.calls.context.CheckArgumentTypesMode;
import com.huawei.cangjie.resolve.calls.context.ContextDependency;
import com.huawei.cangjie.resolve.calls.context.ResolutionContext;
import com.huawei.cangjie.resolve.calls.model.MutableDataFlowInfoForArguments;
import com.huawei.cangjie.resolve.calls.util.ResolveArgumentsMode;
import com.huawei.cangjie.resolve.constants.IntegerLiteralTypeConstructor;
import com.huawei.cangjie.resolve.constants.IntegerValueTypeConstructor;
import com.huawei.cangjie.resolve.constants.evaluate.ConstantExpressionEvaluator;
import com.huawei.cangjie.types.CangJieType;
import com.huawei.cangjie.types.TypeConstructor;
import com.huawei.cangjie.types.checker.CangJieTypeChecker;
import com.huawei.cangjie.types.expressions.ExpressionTypingServices;
import com.huawei.cangjie.types.expressions.typeInfoFactory.TypeInfoFactoryKt;
import com.huawei.cangjie.types.util.TypeUtils;
import com.huawei.cangjie.utils.exceptions.CangJieTypeInfo;
import jakarta.inject.Inject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.huawei.cangjie.psi.CjPsiUtil.getLastElementDeparenthesized;
import static com.huawei.cangjie.resolve.BindingContextUtils.getRecordedTypeInfo;
import static com.huawei.cangjie.types.util.TypeUtils.NO_EXPECTED_TYPE;

public class ArgumentTypeResolver {
    @NotNull
    private final TypeResolver typeResolver;
    @NotNull
    private final CangJieBuiltIns builtIns;
    @NotNull
    private final ReflectionTypes reflectionTypes;
    @NotNull
    private final ConstantExpressionEvaluator constantExpressionEvaluator;
    //    @NotNull private final FunctionPlaceholders functionPlaceholders;
    @NotNull
    private final ModuleDescriptor moduleDescriptor;
    @NotNull
    private final CangJieTypeChecker kotlinTypeChecker;
    private ExpressionTypingServices expressionTypingServices;

    public ArgumentTypeResolver(
            @NotNull TypeResolver typeResolver,
            @NotNull CangJieBuiltIns builtIns,
            @NotNull ReflectionTypes reflectionTypes,
            @NotNull ConstantExpressionEvaluator constantExpressionEvaluator,
//            @NotNull FunctionPlaceholders functionPlaceholders,
            @NotNull ModuleDescriptor moduleDescriptor,
            @NotNull CangJieTypeChecker kotlinTypeChecker
    ) {
        this.typeResolver = typeResolver;
        this.builtIns = builtIns;
        this.reflectionTypes = reflectionTypes;
        this.constantExpressionEvaluator = constantExpressionEvaluator;
//        this.functionPlaceholders = functionPlaceholders;
        this.moduleDescriptor = moduleDescriptor;
        this.kotlinTypeChecker = kotlinTypeChecker;
    }

    private static boolean isCollectionLiteralInsideAnnotation(CjExpression expression, CallResolutionContext<?> context) {
        return expression instanceof CjCollectionLiteralExpression && context.call.getCallElement() instanceof CjAnnotationEntry;
    }

    private static boolean isCallableReferenceArgument(
            @NotNull CjExpression expression, @NotNull StatementFilter statementFilter
    ) {
        return getCallableReferenceExpressionIfAny(expression, statementFilter) != null;
    }

    private static boolean isFunctionLiteralArgument(
            @NotNull CjExpression expression, @NotNull StatementFilter statementFilter
    ) {
        return getFunctionLiteralArgumentIfAny(expression, statementFilter) != null;
    }

    public static boolean isFunctionLiteralOrCallableReference(
            @NotNull CjExpression expression, @NotNull StatementFilter statementFilter
    ) {
        return isFunctionLiteralArgument(expression, statementFilter) || isCallableReferenceArgument(expression, statementFilter);
    }

    public static boolean isFunctionLiteralArgument(
            @NotNull CjExpression expression, @NotNull ResolutionContext context
    ) {
        return isFunctionLiteralArgument(expression, context.statementFilter);
    }

    @Nullable
    public static CjCallableReferenceExpression getCallableReferenceExpressionIfAny(
            @NotNull CjExpression expression,
            @NotNull StatementFilter statementFilter
    ) {
        CjExpression deparenthesizedExpression = getLastElementDeparenthesized(expression, statementFilter);
        if (deparenthesizedExpression instanceof CjCallableReferenceExpression) {
            return (CjCallableReferenceExpression) deparenthesizedExpression;
        }
        return null;
    }

    @Nullable
    public static CjCallableReferenceExpression getCallableReferenceExpressionIfAny(
            @NotNull CjExpression expression,
            @NotNull ResolutionContext context
    ) {
        return getCallableReferenceExpressionIfAny(expression, context.statementFilter);
    }

    @Nullable
    public static CjFunction getFunctionLiteralArgumentIfAny(
            @NotNull CjExpression expression, @NotNull StatementFilter statementFilter
    ) {
        CjExpression deparenthesizedExpression = getLastElementDeparenthesized(expression, statementFilter);
        if (deparenthesizedExpression instanceof CjLambdaExpression) {
            return ((CjLambdaExpression) deparenthesizedExpression).getFunctionLiteral();
        }
        if (deparenthesizedExpression instanceof CjFunction) {
            return (CjFunction) deparenthesizedExpression;
        }
        return null;
    }

    @Nullable
    public static CjFunction getFunctionLiteralArgumentIfAny(
            @NotNull CjExpression expression, @NotNull ResolutionContext context
    ) {
        return getFunctionLiteralArgumentIfAny(expression, context.statementFilter);
    }

    // component dependency cycle
    @Inject
    public void setExpressionTypingServices(@NotNull ExpressionTypingServices expressionTypingServices) {
        this.expressionTypingServices = expressionTypingServices;
    }

    @Nullable
    public CangJieType updateResultArgumentTypeIfNotDenotable(
            @NotNull ResolutionContext context,
            @NotNull CjExpression expression
    ) {
        return updateResultArgumentTypeIfNotDenotable(context.trace, context.statementFilter, context.expectedType, expression);
    }

    @Nullable
    public CangJieType updateResultArgumentTypeIfNotDenotable(
            @NotNull BindingTrace trace,
            @NotNull StatementFilter statementFilter,
            @NotNull CangJieType expectedType,
            @NotNull CjExpression expression
    ) {
        CangJieType type = trace.getType(expression);
        return type != null ? updateResultArgumentTypeIfNotDenotable(trace, statementFilter, expectedType, type, expression) : null;
    }

    @Nullable
    public CangJieType updateResultArgumentTypeIfNotDenotable(
            @NotNull BindingTrace trace,
            @NotNull StatementFilter statementFilter,
            @NotNull CangJieType expectedType,
            @NotNull CangJieType targetType,
            @NotNull CjExpression expression
    ) {
        TypeConstructor typeConstructor = targetType.getConstructor();
        if (!typeConstructor.isDenotable()) {
            if (typeConstructor instanceof IntegerValueTypeConstructor constructor) {
                CangJieType primitiveType = TypeUtils.getPrimitiveNumberType(constructor, expectedType);
                constantExpressionEvaluator.updateNumberType(primitiveType, expression, statementFilter, trace);
                return primitiveType;
            }
            if (typeConstructor instanceof IntegerLiteralTypeConstructor constructor) {
                CangJieType primitiveType = TypeUtils.getPrimitiveNumberType(constructor, expectedType);
                constantExpressionEvaluator.updateNumberType(primitiveType, expression, statementFilter, trace);
                return primitiveType;
            }
        }
        return null;
    }

    private void checkArgumentTypeWithNoCallee(CallResolutionContext<?> context, CjExpression argumentExpression) {
        expressionTypingServices.getTypeInfo(argumentExpression, context.replaceExpectedType(NO_EXPECTED_TYPE));
        updateResultArgumentTypeIfNotDenotable(context, argumentExpression);
    }

    public void checkTypesWithNoCallee(
            @NotNull CallResolutionContext<?> context
    ) {
        if (context.checkArguments != CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS) return;

        for (ValueArgument valueArgument : context.call.getValueArguments()) {
            CjExpression argumentExpression = valueArgument.getArgumentExpression();
            if (argumentExpression != null && !(argumentExpression instanceof CjLambdaExpression)) {
                checkArgumentTypeWithNoCallee(context, argumentExpression);
            }
        }

        checkTypesForFunctionArgumentsWithNoCallee(context);

        for (CjTypeProjection typeProjection : context.call.getTypeArguments()) {
            CjTypeReference typeReference = typeProjection.getTypeReference();
            if (typeReference == null) {
                context.trace.report(Errors.PROJECTION_ON_NON_CLASS_TYPE_ARGUMENT.on(typeProjection));
            } else {
                typeResolver.resolveType(context.scope, typeReference, context.trace, true);
            }
        }
    }

    private void checkTypesForFunctionArgumentsWithNoCallee(@NotNull CallResolutionContext<?> context) {
        if (context.checkArguments != CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS) return;

        for (ValueArgument valueArgument : context.call.getValueArguments()) {
            CjExpression argumentExpression = valueArgument.getArgumentExpression();
            if (argumentExpression != null && isFunctionLiteralArgument(argumentExpression, context)) {
                checkArgumentTypeWithNoCallee(context, argumentExpression);
            }
        }
    }

    /**
     * Visits function call arguments and determines data flow information changes
     */
    public void analyzeArgumentsAndRecordTypes(
            @NotNull CallResolutionContext<?> context, @NotNull ResolveArgumentsMode resolveArgumentsMode
    ) {
        MutableDataFlowInfoForArguments infoForArguments = context.dataFlowInfoForArguments;
        Call call = context.call;

        for (ValueArgument argument : call.getValueArguments()) {
            CjExpression expression = argument.getArgumentExpression();
            if (expression == null) continue;

            if (isCollectionLiteralInsideAnnotation(expression, context)) {
                continue;
            }

            CallResolutionContext<?> newContext = context.replaceDataFlowInfo(infoForArguments.getInfo(argument));
            // Here we go inside arguments and determine additional data flow information for them
            CangJieTypeInfo typeInfoForCall = getArgumentTypeInfo(expression, newContext, resolveArgumentsMode, false);
            infoForArguments.updateInfo(argument, typeInfoForCall.getDataFlowInfo());
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
    @NotNull
    public CangJieTypeInfo getArgumentTypeInfo(
            @Nullable CjExpression expression,
            @NotNull CallResolutionContext<?> context,
            @NotNull ResolveArgumentsMode resolveArgumentsMode,
            boolean suspendFunctionTypeExpected
    ) {
        if (expression == null) {
            return TypeInfoFactoryKt.noTypeInfo(context);
        }

        CjFunction functionLiteralArgument = getFunctionLiteralArgumentIfAny(expression, context);
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
            ResolutionContext newContext = context.replaceContextDependency(ContextDependency.INDEPENDENT);
            return expressionTypingServices.getTypeInfo(expression, newContext);
        }

//        // TODO: probably should be "is unsigned type or is supertype of unsigned type" to support Comparable<UInt> expected types too
//        if (UnsignedTypes.INSTANCE.isUnsignedType(context.expectedType)) {
//            convertSignedConstantToUnsigned(expression, context);
//        }

        CangJieTypeInfo recordedTypeInfo = getRecordedTypeInfo(expression, context.trace.getBindingContext());
        if (recordedTypeInfo != null) {
            return recordedTypeInfo;
        }

        ResolutionContext newContext = context.replaceExpectedType(NO_EXPECTED_TYPE).replaceContextDependency(ContextDependency.DEPENDENT);

        return expressionTypingServices.getTypeInfo(expression, newContext);
    }
}
