package com.huawei.cangjie.resolve.calls;

import com.huawei.cangjie.psi.*;
import com.huawei.cangjie.resolve.StatementFilter;
import com.huawei.cangjie.resolve.calls.context.CallResolutionContext;
import com.huawei.cangjie.resolve.calls.context.ContextDependency;
import com.huawei.cangjie.resolve.calls.context.ResolutionContext;
import com.huawei.cangjie.resolve.calls.model.MutableDataFlowInfoForArguments;
import com.huawei.cangjie.resolve.calls.util.ResolveArgumentsMode;
import com.huawei.cangjie.resolve.calls.context.ContextDependency.*;
import com.huawei.cangjie.types.CangJieType;
import com.huawei.cangjie.types.expressions.ExpressionTypingServices;
import com.huawei.cangjie.types.expressions.typeInfoFactory.TypeInfoFactoryKt;
import com.huawei.cangjie.utils.exceptions.CangJieTypeInfo;
import jakarta.inject.Inject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.huawei.cangjie.psi.CjPsiUtil.getLastElementDeparenthesized;
import static com.huawei.cangjie.resolve.BindingContextUtils.getRecordedTypeInfo;
import static com.huawei.cangjie.types.TypeUtils.NO_EXPECTED_TYPE;

public class ArgumentTypeResolver {
    private ExpressionTypingServices expressionTypingServices;

    // component dependency cycle
    @Inject
    public void setExpressionTypingServices(@NotNull ExpressionTypingServices expressionTypingServices) {
        this.expressionTypingServices = expressionTypingServices;
    }
    private static boolean isCollectionLiteralInsideAnnotation(CjExpression expression, CallResolutionContext<?> context) {
        return expression instanceof CjCollectionLiteralExpression && context.call.getCallElement() instanceof CjAnnotationEntry;
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

//    @NotNull
//    public CangJieTypeInfo getFunctionLiteralTypeInfo(
//            @NotNull CjExpression expression,
//            @NotNull CjFunction functionLiteral,
//            @NotNull CallResolutionContext<?> context,
//            @NotNull ResolveArgumentsMode resolveArgumentsMode,
//            boolean suspendFunctionTypeExpected
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
