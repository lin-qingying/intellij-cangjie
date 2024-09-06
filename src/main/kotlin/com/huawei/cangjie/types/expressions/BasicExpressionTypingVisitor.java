package com.huawei.cangjie.types.expressions;

import com.huawei.cangjie.CjNodeTypes;
import com.huawei.cangjie.builtins.CangJieBuiltIns;
import com.huawei.cangjie.builtins.StandardNames;
import com.huawei.cangjie.descriptors.ClassifierDescriptor;
import com.huawei.cangjie.descriptors.DescriptorWithDeprecation;
import com.huawei.cangjie.descriptors.Diagnostic;
import com.huawei.cangjie.descriptors.Errors;
import com.huawei.cangjie.incremental.components.NoLookupLocation;
import com.huawei.cangjie.lexer.CjKeywordToken;
import com.huawei.cangjie.lexer.CjTokens;
import com.huawei.cangjie.parsing.ParseUtilsKt;
import com.huawei.cangjie.psi.*;
import com.huawei.cangjie.resolve.calls.CallExpressionResolver;
import com.huawei.cangjie.resolve.calls.context.ContextDependency;
import com.huawei.cangjie.resolve.calls.smartcasts.DataFlowInfo;
import com.huawei.cangjie.resolve.calls.smartcasts.DataFlowValue;
import com.huawei.cangjie.resolve.constants.*;
import com.huawei.cangjie.resolve.scopes.ScopeUtilsKt;
import com.huawei.cangjie.types.CangJieType;
import com.huawei.cangjie.types.expressions.typeInfoFactory.TypeInfoFactoryKt;
import com.huawei.cangjie.utils.exceptions.CangJieTypeInfo;
import com.intellij.psi.PsiElement;
import com.intellij.psi.StubBasedPsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.huawei.cangjie.lexer.CjTokens.*;
import static com.huawei.cangjie.types.util.TypeUtils.NO_EXPECTED_TYPE;

@SuppressWarnings("SuspiciousMethodCalls")
public class BasicExpressionTypingVisitor extends ExpressionTypingVisitor {
    protected BasicExpressionTypingVisitor(@NotNull ExpressionTypingInternals facade) {
        super(facade);
    }

    public static boolean isLValue(@NotNull CjSimpleNameExpression expression, @Nullable PsiElement parent) {
        if (!(parent instanceof CjBinaryExpression binaryExpression)) {
            return false;
        }

        //        if (!OperatorConventions.BINARY_OPERATION_NAMES.containsKey(binaryExpression.getOperationToken()) &&
//                !CjTokens.ALL_ASSIGNMENTS.contains(binaryExpression.getOperationToken())) {
//            return false;
//        }
        return PsiTreeUtil.isAncestor(binaryExpression.getLeft(), expression, false);
    }

    private static boolean isLValueOrUnsafeReceiver(@NotNull CjSimpleNameExpression expression) {
        PsiElement parent = PsiTreeUtil.skipParentsOfType(expression, CjParenthesizedExpression.class);
        if (parent instanceof CjQualifiedExpression qualifiedExpression) {
            // See KT-10175: receiver of unsafe call is always not-null at resolver
            // so we have to analyze its nullability here
            return qualifiedExpression.getOperationSign() == CjTokens.DOT &&
                    qualifiedExpression.getReceiverExpression() == CjPsiUtil.deparenthesize(expression);
        }

        return isLValue(expression, parent);
    }

    //字面量前缀和后缀
    private static void checkLiteralPrefixAndSuffix(@NotNull PsiElement expression, ExpressionTypingContext context) {
        if (expression instanceof StubBasedPsiElement && ((StubBasedPsiElement) expression).getStub() != null) {
            return;
        }

        checkLiteralPrefixOrSuffix(PsiTreeUtil.prevLeaf(expression), context);
        checkLiteralPrefixOrSuffix(PsiTreeUtil.nextLeaf(expression), context);
    }

    //    前缀或后缀
    private static void checkLiteralPrefixOrSuffix(PsiElement prefixOrSuffix, ExpressionTypingContext context) {
        if (illegalLiteralPrefixOrSuffix(prefixOrSuffix)) {
            context.trace.report(Errors.UNSUPPORTED.on(prefixOrSuffix, "literal prefixes and suffixes"));
        }
    }

    private static boolean illegalLiteralPrefixOrSuffix(@Nullable PsiElement element) {
        if (element == null) return false;

        IElementType elementType = element.getNode().getElementType();
        return elementType == IDENTIFIER ||
                elementType == INTEGER_LITERAL ||
                elementType == FLOAT_LITERAL ||
                elementType instanceof CjKeywordToken;
    }

    @NotNull
    public CangJieTypeInfo checkInExpression(
            @NotNull CjElement callElement,
            @NotNull CjSimpleNameExpression operationSign,
            @NotNull ValueArgument leftArgument,
            @Nullable CjExpression right,
            @NotNull ExpressionTypingContext context
    ) {

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

    @NotNull
    public CangJieType getDefaultType(IElementType constantType) {
        CangJieBuiltIns builtIns = components.builtIns;
        if (constantType == CjNodeTypes.INTEGER_CONSTANT) {
            return builtIns.getInt64Type();
        } else if (constantType == CjNodeTypes.FLOAT_CONSTANT) {
            return builtIns.getFloat64Type();
        } else if (constantType == CjNodeTypes.BOOLEAN_CONSTANT) {
            return builtIns.getBoolType();
        } else if (constantType == CjNodeTypes.RUNE_CONSTANT) {
            return builtIns.getRuneType();
        } else {
            throw new IllegalArgumentException("Unsupported constant type: " + constantType);
        }
    }

    /**
     * 检查字面量中的下划线
     *
     * @param elementType
     */
    private void checkUnderscores(
            @NotNull CjConstantExpression expression,
            @NotNull IElementType elementType,
            @NotNull ExpressionTypingContext context
    ) {
        String text = expression.getText().toLowerCase();

        if (!text.contains("_")) return;

//        if (!components.languageVersionSettings.supportsFeature(LanguageFeature.UnderscoresInNumericLiterals)) {
//            context.trace.report(Errors.UNSUPPORTED_FEATURE.on(expression,
//                    TuplesKt.to(LanguageFeature.UnderscoresInNumericLiterals, components.languageVersionSettings)));
//            return;
//        }

        if (ParseUtilsKt.hasIllegalUnderscore(expression.getText(), elementType)) {
            context.trace.report(Errors.ILLEGAL_UNDERSCORE.on(expression));
        }
    }

    @Override
    public CangJieTypeInfo visitStringTemplateExpression(@NotNull CjStringTemplateExpression expression, ExpressionTypingContext contextWithExpectedType) {
        ExpressionTypingContext context = contextWithExpectedType
                .replaceExpectedType(NO_EXPECTED_TYPE)
                .replaceContextDependency(ContextDependency.INDEPENDENT);

        checkLiteralPrefixAndSuffix(expression, context);

        class StringTemplateVisitor extends CjVisitorVoid {
            private CangJieTypeInfo typeInfo = TypeInfoFactoryKt.noTypeInfo(context);

            @Override
            public void visitStringTemplateEntryWithExpression(@NotNull CjStringTemplateEntryWithExpression entry) {
                CjExpression entryExpression = entry.getExpression();
                if (entryExpression != null) {

                    DescriptorWithDeprecation<ClassifierDescriptor> toString = ScopeUtilsKt.findFirstClassifierWithDeprecationStatus(context.scope, StandardNames.TOSTRING, NoLookupLocation.FROM_BUILTINS);

                    if (toString != null) {
                        //                  约束toString类型
                        typeInfo = facade.getTypeInfo(entryExpression, context.replaceExpectedType(toString.getDescriptor().getDefaultType()).replaceDataFlowInfo(typeInfo.getDataFlowInfo()));

                    } else {
                        typeInfo = facade.getTypeInfo(entryExpression, context.replaceDataFlowInfo(typeInfo.getDataFlowInfo()));

                    }


                }
            }

            @Override
            public void visitEscapeStringTemplateEntry(@NotNull CjEscapeStringTemplateEntry entry) {
                CompileTimeConstantChecker.CharacterWithDiagnostic value =
                        CompileTimeConstantChecker.escapedStringToCharacter(entry.getText(), entry);
                Diagnostic diagnostic = value.getDiagnostic();
                if (diagnostic != null) {
                    context.trace.report(diagnostic);
                }
            }
        }
        StringTemplateVisitor visitor = new StringTemplateVisitor();
        for (CjStringTemplateEntry entry : expression.getEntries()) {
            entry.accept(visitor);
        }
        components.constantExpressionEvaluator.evaluateExpression(expression, context.trace, contextWithExpectedType.expectedType);
        return components.dataFlowAnalyzer.checkType(visitor.typeInfo.replaceType(components.builtIns.getStringType()),
                expression,

                contextWithExpectedType);
    }

    //    根据字面量返回类型信息
    @Override
    public CangJieTypeInfo visitConstantExpression(@NotNull CjConstantExpression expression, ExpressionTypingContext context) {
        IElementType elementType = expression.getNode().getElementType();
        if (elementType == CjNodeTypes.RUNE_CONSTANT
                || elementType == CjNodeTypes.INTEGER_CONSTANT
                || elementType == CjNodeTypes.FLOAT_CONSTANT) {
            checkLiteralPrefixAndSuffix(expression, context);
        }


        if (elementType == CjNodeTypes.INTEGER_CONSTANT || elementType == CjNodeTypes.FLOAT_CONSTANT) {
            checkUnderscores(expression, elementType, context);
        }

        CompileTimeConstant<?> compileTimeConstant = components.constantExpressionEvaluator.evaluateExpression(
                expression, context.trace, context.expectedType
        );

        if (compileTimeConstant instanceof UnsignedErrorValueTypeConstant) {
            ErrorValue.ErrorValueWithMessage value = ((UnsignedErrorValueTypeConstant) compileTimeConstant).getErrorValue();
            context.trace.report(Errors.UNSIGNED_LITERAL_WITHOUT_DECLARATIONS_ON_CLASSPATH.on(expression));

            return TypeInfoFactoryKt.createTypeInfo(value.getType(components.moduleDescriptor), context);
        } else if (!(compileTimeConstant instanceof IntegerValueTypeConstant)) {
            CompileTimeConstantChecker constantChecker = new CompileTimeConstantChecker(context, components.moduleDescriptor, false);
            ConstantValue constantValue = compileTimeConstant != null ? ((TypedCompileTimeConstant) compileTimeConstant).getConstantValue() : null;
            boolean hasError = constantChecker.checkConstantExpressionType(constantValue, expression, context.expectedType);
            if (hasError) {
                return TypeInfoFactoryKt.createTypeInfo(
                        constantValue != null ? constantValue.getType(components.moduleDescriptor) : getDefaultType(elementType),
                        context
                );
            }
        }

        assert compileTimeConstant != null :
                "CompileTimeConstant should be evaluated for constant expression or an error should be recorded " +
                        expression.getText();
        return components.dataFlowAnalyzer.createCompileTimeConstantTypeInfo(compileTimeConstant, expression, context);

    }

    @Override
    public CangJieTypeInfo visitCollectionLiteralExpression(
            @NotNull CjCollectionLiteralExpression expression, ExpressionTypingContext context
    ) {
        return components.collectionLiteralResolver.resolveCollectionLiteral(expression, context);
    }
    @Override
    public CangJieTypeInfo visitQualifiedExpression(@NotNull CjQualifiedExpression expression, ExpressionTypingContext context) {
        CallExpressionResolver callExpressionResolver = components.callExpressionResolver;
        return callExpressionResolver.getQualifiedExpressionTypeInfo(expression, context);
    }

    @Override
    public CangJieTypeInfo visitParenthesizedExpression(@NotNull CjParenthesizedExpression expression, ExpressionTypingContext context) {
        CjExpression innerExpression = expression.getExpression();
        if (innerExpression == null) {
            return TypeInfoFactoryKt.noTypeInfo(context);
        }
        CangJieTypeInfo result = facade.getTypeInfo(innerExpression, context);
        CangJieType resultType = result.getType();
        if (resultType != null) {
            DataFlowValue innerValue = components.dataFlowValueFactory.createDataFlowValue(innerExpression, resultType, context);
            DataFlowValue resultValue = components.dataFlowValueFactory.createDataFlowValue(expression, resultType, context);
            result = result.replaceDataFlowInfo(result.getDataFlowInfo().assign(resultValue, innerValue
                    /*,             components.languageVersionSettings*/));
        }
        return result;
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
