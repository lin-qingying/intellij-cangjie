package com.huawei.cangjie.types.expressions;

import com.huawei.cangjie.config.LanguageFeature;
import com.huawei.cangjie.descriptors.FunctionDescriptor;
import com.huawei.cangjie.lexer.CjTokens;
import com.huawei.cangjie.psi.*;
import com.huawei.cangjie.resolve.calls.CallExpressionResolver;
import com.huawei.cangjie.resolve.calls.smartcasts.DataFlowInfo;
import com.huawei.cangjie.resolve.calls.smartcasts.DataFlowValue;
import com.huawei.cangjie.resolve.calls.smartcasts.Nullability;
import com.huawei.cangjie.types.CangJieType;
import com.huawei.cangjie.types.CangJieTypeKt;
import com.huawei.cangjie.utils.exceptions.CangJieTypeInfo;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;

import static com.huawei.cangjie.types.util.TypeUtils.NO_EXPECTED_TYPE;

@SuppressWarnings("SuspiciousMethodCalls")
public class BasicExpressionTypingVisitor extends ExpressionTypingVisitor{
    protected BasicExpressionTypingVisitor(@NotNull ExpressionTypingInternals facade) {
        super(facade);
    }


    @NotNull
    public CangJieTypeInfo checkInExpression(
            @NotNull CjElement callElement,
            @NotNull CjSimpleNameExpression operationSign,
            @NotNull ValueArgument leftArgument,
            @Nullable CjExpression right,
            @NotNull ExpressionTypingContext context
    ){

        CjExpression left = leftArgument.getArgumentExpression();
        ExpressionTypingContext contextWithNoExpectedType = context.replaceExpectedType(NO_EXPECTED_TYPE);
//        if (right == null) {
//            if (left != null) facade.getTypeInfo(left, contextWithNoExpectedType);
//            return TypeInfoFactoryCj.noTypeInfo(context);
//        }

        CangJieTypeInfo rightTypeInfo = facade.getTypeInfo(right, contextWithNoExpectedType);
        DataFlowInfo dataFlowInfo = rightTypeInfo.getDataFlowInfo();
//
//        ExpressionReceiver receiver = safeGetExpressionReceiver(facade, right, contextWithNoExpectedType);
//        ExpressionTypingContext contextWithDataFlow = context.replaceDataFlowInfo(dataFlowInfo);
//
//        Call containsCall = CallMaker.makeCall(
//                callElement, receiver, null, operationSign,
//                Collections.singletonList(leftArgument), Call.CallType.CONTAINS
//        );
//
//        OverloadResolutionResults<FunctionDescriptor> resolutionResult = components.callResolver.resolveCallWithGivenName(
//                contextWithDataFlow,
//                containsCall,
//                operationSign,
//                OperatorNameConventions.CONTAINS);
//        CangJieType containsType = OverloadResolutionResultsUtil.getResultingType(resolutionResult, context);
//        ensureBooleanResult(operationSign, OperatorNameConventions.CONTAINS, containsType, context);
//
//        if (left != null) {
//            dataFlowInfo = facade.getTypeInfo(left, contextWithDataFlow).getDataFlowInfo().and(dataFlowInfo);
//            rightTypeInfo = rightTypeInfo.replaceDataFlowInfo(dataFlowInfo);
//        }
//        if (resolutionResult.isSuccess() || isResolutionSuccessfulWithOnlyInputTypesWarnings(resolutionResult.getResultingCalls(), context)) {
//            return rightTypeInfo.replaceType(components.builtIns.getBooleanType());
//        } else {
            return rightTypeInfo.clearType();
//        }
    }

    @Override
    public CangJieTypeInfo visitCallExpression(CjCallExpression cjCallExpression, ExpressionTypingContext data) {
        CallExpressionResolver callExpressionResolver = components.callExpressionResolver;
        return callExpressionResolver.getCallExpressionTypeInfo(cjCallExpression, data);

    }

    public static boolean isLValue(@NotNull CjSimpleNameExpression expression, @Nullable PsiElement parent) {
        if (!(parent instanceof CjBinaryExpression)) {
            return false;
        }

        CjBinaryExpression binaryExpression = (CjBinaryExpression) parent;
//        if (!OperatorConventions.BINARY_OPERATION_NAMES.containsKey(binaryExpression.getOperationToken()) &&
//                !CjTokens.ALL_ASSIGNMENTS.contains(binaryExpression.getOperationToken())) {
//            return false;
//        }
        return PsiTreeUtil.isAncestor(binaryExpression.getLeft(), expression, false);
    }
    private static boolean isLValueOrUnsafeReceiver(@NotNull CjSimpleNameExpression expression) {
        PsiElement parent = PsiTreeUtil.skipParentsOfType(expression, CjParenthesizedExpression.class);
        if (parent instanceof CjQualifiedExpression) {
            CjQualifiedExpression qualifiedExpression = (CjQualifiedExpression) parent;
            // See KT-10175: receiver of unsafe call is always not-null at resolver
            // so we have to analyze its nullability here
            return qualifiedExpression.getOperationSign() == CjTokens.DOT &&
                    qualifiedExpression.getReceiverExpression() == CjPsiUtil.deparenthesize(expression);
        }

        return isLValue(expression, parent);
    }
    private void checkNull(
            @NotNull CjSimpleNameExpression expression,
            @NotNull ExpressionTypingContext context,
            @Nullable CangJieType type
    ) {
        // Receivers are normally analyzed at resolve, with an exception of KT-10175
//        if (type != null && !CangJieTypeKt.isError(type) && !isLValueOrUnsafeReceiver(expression)) {
//            DataFlowValue dataFlowValue = components.dataFlowValueFactory.createDataFlowValue(expression, type, context);
//            Nullability nullability = context.dataFlowInfo.getStableNullability(dataFlowValue);
//            if (!nullability.canBeNonNull() && nullability.canBeNull()) {
//                if (isDangerousWithNull(expression, context)) {
//                    context.trace.report(ALWAYS_NULL.on(expression));
//                }
//                else {
//                    context.trace.record(SMARTCAST_NULL, expression);
//                }
//            }
//        }
    }
    @Override
    public CangJieTypeInfo visitSimpleNameExpression(@NotNull CjSimpleNameExpression expression, ExpressionTypingContext context) {
//        if (!components.languageVersionSettings.supportsFeature(LanguageFeature.YieldIsNoMoreReserved)) {
//            ReservedCheckingKt.checkReservedYield(expression, context.trace);
//        }
//
//        // TODO : other members
//        // TODO : type substitutions???
        CallExpressionResolver callExpressionResolver = components.callExpressionResolver;
        CangJieTypeInfo typeInfo = callExpressionResolver.getSimpleNameExpressionTypeInfo(expression, null, null, context);
        checkNull(expression, context, typeInfo.getType());

        components.constantExpressionEvaluator.evaluateExpression(
                expression, context.trace, context.expectedType
        );
        return components.dataFlowAnalyzer.checkType(typeInfo, expression, context); // TODO : Extensions to this
    }
}
