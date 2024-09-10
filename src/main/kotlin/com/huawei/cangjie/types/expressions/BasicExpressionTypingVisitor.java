package com.huawei.cangjie.types.expressions;

import com.huawei.cangjie.CjNodeTypes;
import com.huawei.cangjie.builtins.BinaryOperatorRule;
import com.huawei.cangjie.builtins.CangJieBuiltIns;
import com.huawei.cangjie.builtins.StandardNames;
import com.huawei.cangjie.descriptors.*;
import com.huawei.cangjie.diagnostics.InvalidBinaryData;
import com.huawei.cangjie.incremental.components.NoLookupLocation;
import com.huawei.cangjie.lexer.CjKeywordToken;
import com.huawei.cangjie.lexer.CjSingleValueToken;
import com.huawei.cangjie.lexer.CjToken;
import com.huawei.cangjie.lexer.CjTokens;
import com.huawei.cangjie.name.Name;
import com.huawei.cangjie.parsing.ParseUtilsKt;
import com.huawei.cangjie.psi.*;
import com.huawei.cangjie.resolve.calls.CallExpressionResolver;
import com.huawei.cangjie.resolve.calls.context.ContextDependency;
import com.huawei.cangjie.resolve.calls.results.OverloadResolutionResults;
import com.huawei.cangjie.resolve.calls.results.OverloadResolutionResultsImpl;
import com.huawei.cangjie.resolve.calls.results.OverloadResolutionResultsUtil;
import com.huawei.cangjie.resolve.calls.smartcasts.DataFlowInfo;
import com.huawei.cangjie.resolve.calls.smartcasts.DataFlowValue;
import com.huawei.cangjie.resolve.constants.*;
import com.huawei.cangjie.resolve.scopes.LexicalScopeKind;
import com.huawei.cangjie.resolve.scopes.LexicalWritableScope;
import com.huawei.cangjie.resolve.scopes.ScopeUtilsKt;
import com.huawei.cangjie.resolve.scopes.receivers.ExpressionReceiver;
import com.huawei.cangjie.types.CangJieType;
import com.huawei.cangjie.types.ErrorUtils;
import com.huawei.cangjie.types.error.ErrorType;
import com.huawei.cangjie.types.expressions.typeInfoFactory.TypeInfoFactoryKt;
import com.huawei.cangjie.utils.exceptions.CangJieTypeInfo;
import com.huawei.cangjie.utils.exceptions.OperatorConventions;
import com.intellij.psi.PsiElement;
import com.intellij.psi.StubBasedPsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.huawei.cangjie.descriptors.Errors.*;
import static com.huawei.cangjie.lexer.CjTokens.*;
import static com.huawei.cangjie.types.expressions.ExpressionTypingUtils.*;
import static com.huawei.cangjie.types.util.TypeUtils.NO_EXPECTED_TYPE;
import static com.huawei.cangjie.utils.exceptions.OperatorConventions.isConventionType;

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

            // so we have to analyze its nullability here
            return qualifiedExpression.getOperationSign() == DOT &&
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
            context.trace.report(UNSUPPORTED.on(prefixOrSuffix, "literal prefixes and suffixes"));
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

    @NotNull
    private CangJieTypeInfo getTypeInfoForBinaryCall(
            @NotNull Name name,
            @NotNull ExpressionTypingContext context,
            @NotNull CjBinaryExpression binaryExpression
    ) {
        CjExpression left = binaryExpression.getLeft();
        CjExpression right = binaryExpression.getRight();

        CangJieTypeInfo typeInfo;
        if (left != null) {
            //left here is a receiver, so it doesn't depend on expected type
            typeInfo = facade.getTypeInfo(left, context.replaceContextDependency(ContextDependency.INDEPENDENT).replaceExpectedType(NO_EXPECTED_TYPE));
        } else {
            typeInfo = TypeInfoFactoryKt.noTypeInfo(context);
        }

        ExpressionTypingContext contextWithDataFlow = context.replaceDataFlowInfo(typeInfo.getDataFlowInfo());

        OverloadResolutionResults<FunctionDescriptor> resolutionResults;
        if (left != null) {
            ExpressionReceiver receiver = safeGetExpressionReceiver(facade, left, context);
            resolutionResults = components.callResolver.resolveBinaryCall(contextWithDataFlow, receiver, binaryExpression, name);
        } else {
            resolutionResults = OverloadResolutionResultsImpl.nameNotFound();
        }

        if (resolutionResults.isSingleResult()) {
            typeInfo = typeInfo.replaceDataFlowInfo(resolutionResults.getResultingCall().getDataFlowInfoForArguments().getResultInfo());
        }

//        if (OverloadResolutionResultsUtil.getResultingType(resolutionResults, context) == null) {
//            if (right != null) {
//                CjSimpleNameExpression operationSign = binaryExpression.getOperationReference();
//                IElementType operationType = operationSign.getReferencedNameElementType();
//
//                CangJieTypeInfo rightInfo = facade.getTypeInfo(right
//                        , context.replaceContextDependency(ContextDependency.INDEPENDENT).replaceExpectedType(NO_EXPECTED_TYPE));
//                context.trace.report(INVALID_BINARY_OPERATOR.on(operationSign, new InvalidBinaryData(
//                        ((CjSingleValueToken) operationType).getValue(), typeInfo.getType(), rightInfo.getType()
//                )));
//            }
//        }

        return typeInfo.replaceType(OverloadResolutionResultsUtil.getResultingType(resolutionResults, context));
    }

    @NotNull
    private CangJieTypeInfo assignmentIsNotAnExpressionError(CjBinaryExpression expression, ExpressionTypingContext context) {
        facade.checkStatementType(expression, context);
        if (!context.isDebuggerContext) {
            context.trace.report(ASSIGNMENT_IN_EXPRESSION_CONTEXT.on(expression));
        }
        return TypeInfoFactoryKt.noTypeInfo(context);
    }

    @NotNull
    private CangJieTypeInfo visitAssignment(CjBinaryExpression expression, ExpressionTypingContext context) {
        return assignmentIsNotAnExpressionError(expression, context);
    }

    public CangJieTypeInfo operatorOverloading(IElementType operationType, ExpressionTypingContext context, CjBinaryExpression expression) {
        CangJieTypeInfo result = null;


//        if (result != null && result.getType() instanceof ErrorType && ((ErrorType) result.getType()).getKind() == ErrorTypeKind.RETURN_TYPE_FOR_FUNCTION) {
//            return null;
//        }

        return result;
    }

    //    @NotNull
//    private CangJieTypeInfo visitElvisExpression(
//            @NotNull CjBinaryExpression expression,
//            @NotNull ExpressionTypingContext contextWithExpectedType
//    ) {
//        ExpressionTypingContext context = contextWithExpectedType.replaceExpectedType(NO_EXPECTED_TYPE);
//        CjExpression left = expression.getLeft();
//        CjExpression right = expression.getRight();
//
//        if (left == null || right == null) {
//            getTypeInfoOrNullType(left, context, facade);
//            return TypeInfoFactoryKt.noTypeInfo(context);
//        }
//
//        Call call = createCallForSpecialConstruction(expression, expression.getOperationReference(), Lists.newArrayList(left, right));
//        ResolvedCall<FunctionDescriptor> resolvedCall = components.controlStructureTypingUtils.resolveSpecialConstructionAsCall(
//                call, ControlStructureTypingUtils.ResolveConstruct.ELVIS, Lists.newArrayList("left", "right"),
//                Lists.newArrayList(true, false), contextWithExpectedType, null);
//        CangJieTypeInfo leftTypeInfo = BindingContextUtils.getRecordedTypeInfo(left, context.trace.getBindingContext());
//        boolean isLeftFunctionLiteral = ArgumentTypeResolver.isFunctionLiteralArgument(left, context);
//        boolean isLeftCallableReference = ArgumentTypeResolver.isCallableReferenceArgument(left, context);
//        boolean isLeftCollectionLiteral = ArgumentTypeResolver.isCollectionLiteralArgument(left);
//        if (leftTypeInfo == null && (isLeftFunctionLiteral || isLeftCallableReference || isLeftCollectionLiteral)) {
//            return TypeInfoFactoryKt.noTypeInfo(context);
//        }
//        assert leftTypeInfo != null : "Left expression was not processed: " + expression;
//        CangJieType leftType = leftTypeInfo.getType();
//        CangJieTypeInfo rightTypeInfo = BindingContextUtils.getRecordedTypeInfo(right, context.trace.getBindingContext());
//        if (rightTypeInfo == null && ArgumentTypeResolver.isFunctionLiteralOrCallableReference(right, context)) {
//            // the type is computed later in call completer according to the '?:' semantics as a function
//            return TypeInfoFactoryKt.noTypeInfo(context);
//        }
//        assert rightTypeInfo != null : "Right expression was not processed: " + expression;
//        boolean loopBreakContinuePossible = leftTypeInfo.getJumpOutPossible() || rightTypeInfo.getJumpOutPossible();
//        CangJieType rightType = rightTypeInfo.getType();
//
//        // Only left argument DFA is taken into account here: we cannot be sure that right argument is joined
//        // (we merge it with right DFA if right argument contains no jump outside)
//        DataFlowInfo dataFlowInfo = resolvedCall.getDataFlowInfoForArguments().getInfo(call.getValueArguments().get(1));
//
//        CangJieType type = resolvedCall.getResultingDescriptor().getReturnType();
//        if (type == null ||
//                rightType == null ||
//                leftType == null && CangJieBuiltIns.isNothing(rightType)) return TypeInfoFactoryKt.noTypeInfo(dataFlowInfo);
//
//        if (leftType != null) {
//            DataFlowValue leftValue = components.dataFlowValueFactory.createDataFlowValue(left, leftType, context);
//            DataFlowInfo rightDataFlowInfo = resolvedCall.getDataFlowInfoForArguments().getResultInfo();
//            boolean jumpInRight = CangJieBuiltIns.isNothing(rightType);
//            DataFlowValue nullValue = DataFlowValue.nullValue(components.builtIns);
//            // left argument is considered not-null if it's not-null also in right part or if we have jump in right part
//            if (jumpInRight || !rightDataFlowInfo.getStableNullability(leftValue).canBeNull()) {
//                dataFlowInfo = dataFlowInfo.disequate(leftValue, nullValue, components.languageVersionSettings);
//                if (left instanceof CjBinaryExpressionWithTypeRHS) {
//                    dataFlowInfo = establishSubtypingForTypeRHS((CjBinaryExpressionWithTypeRHS) left, dataFlowInfo, context,
//                            components.languageVersionSettings);
//                }
//            }
//            DataFlowValue resultValue = components.dataFlowValueFactory.createDataFlowValue(expression, type, context);
//            dataFlowInfo =
//                    dataFlowInfo.assign(resultValue, leftValue, components.languageVersionSettings)
//                            .disequate(resultValue, nullValue, components.languageVersionSettings);
//            if (!jumpInRight) {
//                DataFlowValue rightValue = components.dataFlowValueFactory.createDataFlowValue(right, rightType, context);
//                rightDataFlowInfo = rightDataFlowInfo.assign(resultValue, rightValue, components.languageVersionSettings);
//                dataFlowInfo = dataFlowInfo.or(rightDataFlowInfo);
//            }
//        }
//
//        // Sometimes return type for special call for elvis operator might be nullable,
//        // but result is not nullable if the right type is not nullable
//        if (!TypeUtils.isNullableType(rightType) && TypeUtils.isNullableType(type)) {
//            type = TypeUtils.makeNotNullable(type);
//        }
//        if (context.contextDependency == DEPENDENT) {
//            return TypeInfoFactoryKt.createTypeInfo(type, dataFlowInfo);
//        }
//
//        // If break or continue was possible, take condition check info as the jump info
//        return TypeInfoFactoryKt.createTypeInfo(components.dataFlowAnalyzer.checkType(type, expression, contextWithExpectedType),
//                dataFlowInfo,
//                loopBreakContinuePossible,
//                context.dataFlowInfo);
//    }
    @NotNull
    private CangJieTypeInfo visitComparison(
            @NotNull CjBinaryExpression expression,
            @NotNull ExpressionTypingContext context,
            @NotNull CjSimpleNameExpression operationSign
    ) {
        IElementType operationType = operationSign.getReferencedNameElementType();

        Name referencedName = OperatorConventions.COMPARISON_OPERATIONS_NAMES.get(operationType);

        return getTypeInfoForBinaryCall(referencedName, context, expression);

    }
//    @NotNull
//    private CangJieTypeInfo visitBooleanOperationExpression(
//            @Nullable IElementType operationType,
//            @Nullable CjExpression left,
//            @Nullable CjExpression right,
//            @NotNull ExpressionTypingContext context
//    ) {
//        CangJieType booleanType = components.builtIns.getBoolType();
//        CangJieTypeInfo leftTypeInfo = getTypeInfoOrNullType(left, context.replaceExpectedType(booleanType), facade);
//        DataFlowInfo dataFlowInfo = leftTypeInfo.getDataFlowInfo();
//
//        LexicalWritableScope leftScope = newWritableScopeImpl(context, LexicalScopeKind.LEFT_BOOLEAN_EXPRESSION, facade.getComponents().overloadChecker);
//        // TODO: This gets computed twice: here and in extractDataFlowInfoFromCondition() for the whole condition
//        boolean isAnd = operationType == CjTokens.ANDAND;
//        DataFlowInfo flowInfoLeft = components.dataFlowAnalyzer.extractDataFlowInfoFromCondition(left, isAnd, context).and(dataFlowInfo);
//        LexicalWritableScope rightScope = isAnd ? leftScope : newWritableScopeImpl(context, LexicalScopeKind.RIGHT_BOOLEAN_EXPRESSION,
//                facade.getComponents().overloadChecker);
//
//        ExpressionTypingContext contextForRightExpr =
//                context.replaceDataFlowInfo(flowInfoLeft).replaceScope(rightScope).replaceExpectedType(booleanType);
//        if (right != null) {
//            facade.getTypeInfo(right, contextForRightExpr);
//        }
//        return leftTypeInfo.replaceType(booleanType);
//    }

    @Override
    public CangJieTypeInfo visitBinaryExpression(@NotNull CjBinaryExpression expression, ExpressionTypingContext contextWithExpectedType) {
        ExpressionTypingContext context = isBinaryExpressionDependentOnExpectedType(expression)
                ? contextWithExpectedType
                : contextWithExpectedType.replaceContextDependency(ContextDependency.INDEPENDENT)
                .replaceExpectedType(NO_EXPECTED_TYPE);

        CjSimpleNameExpression operationSign = expression.getOperationReference();
        CjExpression left = expression.getLeft();
        CjExpression right = expression.getRight();
        IElementType operationType = operationSign.getReferencedNameElementType();

        CangJieTypeInfo result;

        if (OperatorConventions.BINARY_OPERATION_NAMES.containsKey(operationType)) {
            Name referencedName = OperatorConventions.BINARY_OPERATION_NAMES.get(operationType);
            result = getTypeInfoForBinaryCall(referencedName, context, expression);
        }   /*else if (operationType == CjTokens.ELVIS) {
            //base expression of elvis operator is checked for 'type mismatch', so the whole expression shouldn't be checked
            return visitElvisExpression(expression, context);
        }*/ else if (OperatorConventions.COMPARISON_OPERATIONS_NAMES.containsKey(operationType)) {
            result = visitComparison(expression, context, operationSign);
        } else if (operationType == CjTokens.EQ) {
            result = visitAssignment(expression, context);
        }   /* else if (OperatorConventions.BOOLEAN_OPERATIONS.containsKey(operationType)) {
            result = visitBooleanOperationExpression(operationType, left, right, context);
        }*/else {
            context.trace.report(UNSUPPORTED.on(operationSign, "Unknown operation"));
            result = TypeInfoFactoryKt.noTypeInfo(context);
        }

        CompileTimeConstant<?> value = components.constantExpressionEvaluator.evaluateExpression(
                expression, contextWithExpectedType.trace, contextWithExpectedType.expectedType
        );
        if (value != null) {
            return components.dataFlowAnalyzer.createCompileTimeConstantTypeInfo(value, expression, contextWithExpectedType);
        }
        return components.dataFlowAnalyzer.checkType(result, expression, contextWithExpectedType);
    }

    private CangJieTypeInfo checkOperatorByType(CangJieTypeInfo leftTypeInfo, CangJieTypeInfo rightTypeInfo,
                                                CjSimpleNameExpression operationSign, ExpressionTypingContext context) {
        IElementType operationType = operationSign.getReferencedNameElementType();


        if (leftTypeInfo.getType() instanceof ErrorType) {
            return leftTypeInfo;
        }

        BinaryOperatorRule result = components.builtIns.matchBinaryOperatorRule((CjToken) operationType, leftTypeInfo.getType(), rightTypeInfo.getType());

        switch (result.getResultType()) {
            case LEFT -> {
                return leftTypeInfo;
            }
            case RIGHT -> {
                return rightTypeInfo;
            }
            case ERROR -> {
                if (isConventionType(operationType)) {
                    context.trace.report(INVALID_BINARY_OPERATOR.on(operationSign, new InvalidBinaryData(
                            ((CjSingleValueToken) operationType).getValue(), leftTypeInfo.getType(), rightTypeInfo.getType()
                    )));
                } else {
//                    不可被重载的操作符报告错误
                    throw new UnsupportedOperationException("Unsupported operator: " + operationType);
                }

            }

        }
        return TypeInfoFactoryKt.errorTypeInfo(ErrorUtils.getInvalidType(), context);

    }

    private boolean checkOperatorByType(CjSimpleNameExpression operationSign, CangJieType type) {
        IElementType operationType = operationSign.getReferencedNameElementType();

//        int类型
        if (CangJieBuiltIns.isNumber(type) && INT_SUPPORT_OPERATOR.contains(operationType)) {
            return true;
        } else return CangJieBuiltIns.isFloat(type) && FLOAT_SUPPORT_OPERATOR.contains(operationType);
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
