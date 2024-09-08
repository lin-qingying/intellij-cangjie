package com.huawei.cangjie.types.expressions;

import com.huawei.cangjie.builtins.CangJieBuiltIns;
import com.huawei.cangjie.config.LanguageVersionSettings;
import com.huawei.cangjie.descriptors.BindingTrace;
import com.huawei.cangjie.descriptors.ModuleDescriptor;
import com.huawei.cangjie.diagnostics.DiagnosticUtilsKt;
import com.huawei.cangjie.lexer.CjTokens;
import com.huawei.cangjie.psi.*;
import com.huawei.cangjie.resolve.BindingContext;
import com.huawei.cangjie.resolve.DescriptorUtils;
import com.huawei.cangjie.resolve.calls.checkers.NewSchemeOfIntegerOperatorResolutionChecker;
import com.huawei.cangjie.resolve.calls.context.ContextDependency;
import com.huawei.cangjie.resolve.calls.context.ResolutionContext;
import com.huawei.cangjie.resolve.calls.smartcasts.DataFlowInfo;
import com.huawei.cangjie.resolve.calls.smartcasts.DataFlowValue;
import com.huawei.cangjie.resolve.calls.smartcasts.DataFlowValueFactory;
import com.huawei.cangjie.resolve.constants.CompileTimeConstant;
import com.huawei.cangjie.resolve.constants.IntegerValueTypeConstant;
import com.huawei.cangjie.resolve.constants.TypedCompileTimeConstant;
import com.huawei.cangjie.resolve.constants.evaluate.ConstantExpressionEvaluator;
import com.huawei.cangjie.types.CangJieType;
import com.huawei.cangjie.types.CangJieTypeKt;
import com.huawei.cangjie.types.TypeConstructor;
import com.huawei.cangjie.types.checker.CangJieTypeChecker;
import com.huawei.cangjie.types.expressions.typeInfoFactory.TypeInfoFactoryKt;
import com.huawei.cangjie.utils.exceptions.CangJieTypeInfo;
import com.huawei.cangjie.utils.exceptions.OperatorConventions;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.huawei.cangjie.descriptors.Errors.EXPECTED_TYPE_MISMATCH;
import static com.huawei.cangjie.descriptors.Errors.TYPE_MISMATCH;
import static com.huawei.cangjie.types.util.TypeUtils.*;

public class DataFlowAnalyzer {


    //    private final Iterable<AdditionalTypeChecker> additionalTypeCheckers;
    private final ConstantExpressionEvaluator constantExpressionEvaluator;
    private final ModuleDescriptor module;
    private final CangJieBuiltIns builtIns;
    private final ExpressionTypingFacade facade;
    private final LanguageVersionSettings languageVersionSettings;

    //    private final EffectSystem effectSystem;
    private final DataFlowValueFactory dataFlowValueFactory;
    //    private final SmartCastManager smartCastManager;
    private final CangJieTypeChecker cangjieTypeChecker;

    public DataFlowAnalyzer(
//            @NotNull Iterable<AdditionalTypeChecker> additionalTypeCheckers,
            @NotNull ConstantExpressionEvaluator constantExpressionEvaluator,
            @NotNull ModuleDescriptor module,
            @NotNull CangJieBuiltIns builtIns,
            @NotNull ExpressionTypingFacade facade,
            @NotNull LanguageVersionSettings languageVersionSettings,

//            @NotNull EffectSystem effectSystem,
            @NotNull DataFlowValueFactory factory,
//            @NotNull SmartCastManager smartCastManager,
            @NotNull CangJieTypeChecker cangjieTypeChecker
    ) {
//        this.additionalTypeCheckers = additionalTypeCheckers;
        this.constantExpressionEvaluator = constantExpressionEvaluator;
        this.module = module;
        this.builtIns = builtIns;
        this.facade = facade;
        this.languageVersionSettings = languageVersionSettings;

//        this.effectSystem = effectSystem;
        this.dataFlowValueFactory = factory;
//        this.smartCastManager = smartCastManager;
        this.cangjieTypeChecker = cangjieTypeChecker;
    }

    @NotNull
    public CangJieTypeInfo checkType(@NotNull CangJieTypeInfo typeInfo, @NotNull CjExpression expression, @NotNull ResolutionContext context) {
        return typeInfo.replaceType(checkType(typeInfo.getType(), expression, context));
    }

    public void recordExpectedType(@NotNull BindingTrace trace, @NotNull CjExpression expression, @NotNull CangJieType expectedType) {
        if (expectedType != NO_EXPECTED_TYPE) {
            CangJieType normalizeExpectedType = expectedType == UNIT_EXPECTED_TYPE ? builtIns.getUnitType() : expectedType;
            trace.record(BindingContext.EXPECTED_EXPRESSION_TYPE, expression, normalizeExpectedType);
        }
    }

    @Nullable
    public CangJieType checkType(
            @Nullable CangJieType expressionType,
            @NotNull CjExpression expression,
            @NotNull ResolutionContext context,
            boolean reportErrorForTypeMismatch
    ) {
        return checkType(expressionType, expression, context, null, reportErrorForTypeMismatch);
    }

    // Returns true if we can prove that 'type' has equals method from 'Any' base type
    public boolean typeHasEqualsFromAny(@NotNull CangJieType type, @NotNull CjElement lookupElement) {
        TypeConstructor constructor = type.getConstructor();
        // Subtypes can override equals for non-final types
        if (!constructor.isFinal()) return false;
        // check whether 'equals' is overriden
        return !typeHasOverriddenEquals(type, lookupElement);
    }


    private boolean typeHasOverriddenEquals(@NotNull CangJieType type, @NotNull CjElement lookupElement) {
        // `equals` from `String` is not fake override because it is marked as `IntrinsicConstEvaluation`
//        if (CangJieBuiltIns.isString(type)) return false;
//
//        Collection<? extends SimpleFunctionDescriptor> members = type.getMemberScope().getContributedFunctions(
//                OperatorNameConventions.EQUALS, new CangJieLookupLocation(lookupElement)
//        );
//        for (FunctionDescriptor member : members) {
//            CangJieType returnType = member.getReturnType();
//            if (returnType == null || !CangJieBuiltIns.isBoolean(returnType)) continue;
//            if (member.getValueParameters().size() != 1) continue;
//            CangJieType parameterType = member.getValueParameters().iterator().next().getType();
//            if (!CangJieBuiltIns.isAny(parameterType)) continue;
//            FunctionDescriptor fromSuperClass = getOverriddenDescriptorFromClass(member);
//            if (fromSuperClass == null) return false;
//            ClassifierDescriptor superClassDescriptor = (ClassifierDescriptor) fromSuperClass.getContainingDeclaration();
//            // We should have override fun in class other than Any (to prove unknown behaviour)
//            return !CangJieBuiltIns.isAny(superClassDescriptor.getDefaultType());
//        }
        return false;
    }


    @Nullable
    public CangJieType checkType(
            @Nullable CangJieType expressionType,
            @NotNull CjExpression expressionToCheck,
            @NotNull ResolutionContext c,
            @Nullable Ref<Boolean> hasError,
            boolean reportErrorForTypeMismatch
    ) {
        if (hasError == null) {
            hasError = Ref.create(false);
        } else {
            hasError.set(false);
        }

        CjExpression expression = CjPsiUtil.safeDeparenthesize(expressionToCheck);
        recordExpectedType(c.trace, expression, c.expectedType);

        if (expressionType == null) return null;

        CangJieType result = checkTypeInternal(expressionType, expression, c, hasError, reportErrorForTypeMismatch);
//        if (Boolean.FALSE.equals(hasError.get())) {
//            for (AdditionalTypeChecker checker : additionalTypeCheckers) {
//                checker.checkType(expression, expressionType, result, c);
//            }
//        }

        return result;
    }

//    @NotNull
//    public DataFlowInfo extractDataFlowInfoFromCondition(
//            @Nullable CjExpression condition,
//            boolean conditionValue,
//            ExpressionTypingContext context
//    ) {
//        if (condition == null) return context.dataFlowInfo;
//        Ref<DataFlowInfo> result = new Ref<>(null);
//        condition.accept(new CjVisitorVoid() {
//            @Override
//            public void visitIsExpression(@NotNull CjIsExpression expression) {
//                if (conditionValue) {
//                    result.set(context.trace.get(BindingContext.DATAFLOW_INFO_AFTER_CONDITION, expression));
//                }
//            }
//
//            @Override
//            public void visitBinaryExpression(@NotNull CjBinaryExpression expression) {
//                IElementType operationToken = expression.getOperationToken();
//                if (OperatorConventions.BOOLEAN_OPERATIONS.containsKey(operationToken)) {
//                    DataFlowInfo dataFlowInfo = extractDataFlowInfoFromCondition(expression.getLeft(), conditionValue, context);
//                    CjExpression expressionRight = expression.getRight();
//                    if (expressionRight != null) {
//                        boolean and = operationToken == CjTokens.ANDAND;
//                        DataFlowInfo rightInfo = extractDataFlowInfoFromCondition(
//                                expressionRight, conditionValue,
//                                and == conditionValue ? context.replaceDataFlowInfo(dataFlowInfo) : context
//                        );
//                        if (and == conditionValue) { // this means: and && conditionValue || !and && !conditionValue
//                            dataFlowInfo = dataFlowInfo.and(rightInfo);
//                        } else {
//                            dataFlowInfo = dataFlowInfo.or(rightInfo);
//                        }
//                    }
//                    result.set(dataFlowInfo);
//                } else {
//                    DataFlowInfo expressionFlowInfo = facade.getTypeInfo(expression, context).getDataFlowInfo();
//                    CjExpression left = expression.getLeft();
//                    if (left == null) return;
//                    CjExpression right = expression.getRight();
//                    if (right == null) return;
//
//                    CangJieType lhsType = context.trace.getBindingContext().getType(left);
//                    if (lhsType == null) return;
//                    CangJieType rhsType = context.trace.getBindingContext().getType(right);
//                    if (rhsType == null) return;
//
//                    DataFlowValue leftValue = dataFlowValueFactory.createDataFlowValue(left, lhsType, context);
//                    DataFlowValue rightValue = dataFlowValueFactory.createDataFlowValue(right, rhsType, context);
//
//                    Boolean equals = null;
//                    if (operationToken == CjTokens.EQEQ) {
//                        equals = true;
//                    } else if (operationToken == CjTokens.EXCLEQ) {
//                        equals = false;
//                    }
//                    if (equals != null) {
//                        if (equals == conditionValue) { // this means: equals && conditionValue || !equals && !conditionValue
//                            boolean identityEquals =
//                                    typeHasEqualsFromAny(lhsType, condition);
//                            result.set(context.dataFlowInfo
//                                    .equate(leftValue, rightValue, identityEquals, languageVersionSettings)
//                                    .and(expressionFlowInfo));
//                        } else {
//                            result.set(context.dataFlowInfo
//                                    .disequate(leftValue, rightValue, languageVersionSettings)
//                                    .and(expressionFlowInfo));
//                        }
//                    } else {
//                        result.set(expressionFlowInfo);
//                    }
//                }
//            }
//
//            @Override
//            public void visitUnaryExpression(@NotNull CjUnaryExpression expression) {
//                IElementType operationTokenType = expression.getOperationReference().getReferencedNameElementType();
//                if (operationTokenType == CjTokens.EXCL) {
//                    CjExpression baseExpression = expression.getBaseExpression();
//                    if (baseExpression != null) {
//                        result.set(extractDataFlowInfoFromCondition(baseExpression, !conditionValue, context));
//                    }
//                } else {
//                    visitExpression(expression);
//                }
//            }
//
//            @Override
//            public void visitExpression(@NotNull CjExpression expression) {
//                // In fact, everything is taken from trace here
//                result.set(facade.getTypeInfo(expression, context).getDataFlowInfo());
//            }
//
//            @Override
//            public void visitParenthesizedExpression(@NotNull CjParenthesizedExpression expression) {
//                CjExpression body = expression.getExpression();
//                if (body != null) {
//                    body.accept(this);
//                }
//            }
//        });
//
//        DataFlowInfo infoFromEffectSystem = effectSystem.extractDataFlowInfoFromCondition(
//                condition, conditionValue, context.trace, DescriptorUtils.getContainingModule(context.scope.getOwnerDescriptor())
//        );
//
//        if (result.get() == null) {
//            return context.dataFlowInfo.and(infoFromEffectSystem);
//        }
//
//        return context.dataFlowInfo.and(result.get()).and(infoFromEffectSystem);
//    }

    @NotNull
    private CangJieType checkTypeInternal(
            @NotNull CangJieType expressionType,
            @NotNull CjExpression expression,
            @NotNull ResolutionContext c,
            @NotNull Ref<Boolean> hasError,
            boolean reportErrorForTypeMismatch
    ) {

//        if (!noExpectedType(c.expectedType) && TypeUtilsKt.contains(expressionType, (type) -> type instanceof StubTypeForBuilderInference)) {
//            if (c.inferenceSession instanceof BuilderInferenceSession) {
//                ((BuilderInferenceSession) c.inferenceSession).addExpectedTypeConstraint(expression, expressionType, c.expectedType);
//            }
//        }

        if (noExpectedType(c.expectedType) || !c.expectedType.getConstructor().isDenotable() ||
                cangjieTypeChecker.isSubtypeOf(expressionType, c.expectedType)) {
            return expressionType;
        }
//
//        if (expression instanceof CjConstantExpression && reportErrorForTypeMismatch) {
//            ConstantValue<?> constantValue = constantExpressionEvaluator.evaluateToConstantValue(expression, c.trace, c.expectedType);
//            bool error = new CompileTimeConstantChecker(c, module, true)
//                    .checkConstantExpressionType(constantValue, (CjConstantExpression) expression, c.expectedType);
//            hasError.set(error);
//            return expressionType;
//        }
//
//        if (expression instanceof CjWhenExpression) {
//            CjWhenExpression whenExpression = (CjWhenExpression) expression;
//            if (!whenExpression.getEntries().isEmpty()) {
//                // No need in additional check because type mismatch is already reported for entries
//                return expressionType;
//            }
//        }
//
//        SmartCastResult castResult = checkPossibleCast(expressionType, expression, c);
//        if (castResult != null) return castResult.getResultType();
//
        if (reportErrorForTypeMismatch &&
                !DiagnosticUtilsKt.reportTypeMismatchDueToTypeProjection(c, expression, c.expectedType, expressionType) &&
                !DiagnosticUtilsKt.reportTypeMismatchDueToScalaLikeNamedFunctionSyntax(c, expression, c.expectedType, expressionType)) {
            c.trace.report(TYPE_MISMATCH.on(expression, c.expectedType, expressionType));
        }
        hasError.set(true);
        return expressionType;
    }

    @Nullable
    public CangJieType checkType(@Nullable CangJieType expressionType, @NotNull CjExpression expression, @NotNull ResolutionContext context) {
        return checkType(expressionType, expression, context, null, true);
    }

    @Nullable
    public CangJieType checkStatementType(@NotNull CjExpression expression, @NotNull ResolutionContext context) {
        if (!noExpectedType(context.expectedType) && !CangJieBuiltIns.isUnit(context.expectedType) &&
                !CangJieTypeKt.isError(context.expectedType)) {
            context.trace.report(EXPECTED_TYPE_MISMATCH.on(expression, context.expectedType));
            return null;
        }
        return builtIns.getUnitType();
    }

    @NotNull
    public CangJieTypeInfo createCheckedTypeInfo(
            @Nullable CangJieType type,
            @NotNull ResolutionContext<?> context,
            @NotNull CjExpression expression
    ) {
        return checkType(TypeInfoFactoryKt.createTypeInfo(type, context), expression, context);
    }

    @NotNull
    public CangJieTypeInfo createCompileTimeConstantTypeInfo(
            @NotNull CompileTimeConstant<?> value,
            @NotNull CjExpression expression,
            @NotNull ExpressionTypingContext context
    ) {
        CangJieType expressionType;
        if (value instanceof IntegerValueTypeConstant integerValueTypeConstant) {
            if (context.contextDependency == ContextDependency.INDEPENDENT) {
                expressionType = integerValueTypeConstant.getType(context.expectedType);
                constantExpressionEvaluator.updateNumberType(expressionType, expression, context.statementFilter, context.trace);
            } else {
                expressionType = integerValueTypeConstant.getUnknownIntegerType();
            }
        } else {
            expressionType = ((TypedCompileTimeConstant<?>) value).getType();
        }

        NewSchemeOfIntegerOperatorResolutionChecker.checkArgument(context.expectedType, expression, context.trace, module);

        return createCheckedTypeInfo(expressionType, context, expression);
    }
}
