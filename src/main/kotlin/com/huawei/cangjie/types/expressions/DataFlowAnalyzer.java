package com.huawei.cangjie.types.expressions;

import com.huawei.cangjie.builtins.CangJieBuiltIns;
import com.huawei.cangjie.descriptors.BindingTrace;
import com.huawei.cangjie.descriptors.ModuleDescriptor;
import com.huawei.cangjie.psi.CjConstantExpression;
import com.huawei.cangjie.psi.CjExpression;
import com.huawei.cangjie.resolve.calls.context.ResolutionContext;
import com.huawei.cangjie.resolve.calls.smartcasts.DataFlowValueFactory;
import com.huawei.cangjie.resolve.constants.ConstantValue;
import com.huawei.cangjie.resolve.constants.evaluate.ConstantExpressionEvaluator;
import com.huawei.cangjie.types.CangJieType;
import com.huawei.cangjie.types.checker.CangJieTypeChecker;

import com.huawei.cangjie.types.expressions.typeInfoFactory.TypeInfoFactoryKt;
import com.huawei.cangjie.utils.exceptions.CangJieTypeInfo;
import com.intellij.openapi.util.Ref;
import kotlin.reflect.jvm.internal.impl.types.typeUtil.TypeUtilsKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.huawei.cangjie.psi.CjPsiUtil;

import com.huawei.cangjie.resolve.BindingContext;

import static com.huawei.cangjie.types.util.TypeUtils.*;

public class DataFlowAnalyzer {


//    private final Iterable<AdditionalTypeChecker> additionalTypeCheckers;
    private final ConstantExpressionEvaluator constantExpressionEvaluator;
    private final ModuleDescriptor module;
    private final CangJieBuiltIns builtIns;
    private final ExpressionTypingFacade facade;

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
            @NotNull CjExpression expressionToCheck,
            @NotNull ResolutionContext c,
            @Nullable Ref<Boolean> hasError,
            boolean reportErrorForTypeMismatch
    ){
        if (hasError == null) {
            hasError = Ref.create(false);
        }
        else {
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

    @NotNull
    private CangJieType checkTypeInternal(
            @NotNull CangJieType expressionType,
            @NotNull CjExpression expression,
            @NotNull ResolutionContext c,
            @NotNull Ref<Boolean> hasError,
            boolean reportErrorForTypeMismatch
    ){

//        if (!noExpectedType(c.expectedType) && TypeUtilsKt.contains(expressionType, (type) -> type instanceof StubTypeForBuilderInference)) {
//            if (c.inferenceSession instanceof BuilderInferenceSession) {
//                ((BuilderInferenceSession) c.inferenceSession).addExpectedTypeConstraint(expression, expressionType, c.expectedType);
//            }
//        }
//        if (noExpectedType(c.expectedType) || !c.expectedType.getConstructor().isDenotable() ||
//                cangjieTypeChecker.isSubtypeOf(expressionType, c.expectedType)) {
//            return expressionType;
//        }
//
//        if (expression instanceof CjConstantExpression && reportErrorForTypeMismatch) {
//            ConstantValue<?> constantValue = constantExpressionEvaluator.evaluateToConstantValue(expression, c.trace, c.expectedType);
//            boolean error = new CompileTimeConstantChecker(c, module, true)
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
//        if (reportErrorForTypeMismatch &&
//                !DiagnosticUtilsKt.reportTypeMismatchDueToTypeProjection(c, expression, c.expectedType, expressionType) &&
//                !DiagnosticUtilsKt.reportTypeMismatchDueToScalaLikeNamedFunctionSyntax(c, expression, c.expectedType, expressionType)) {
//            c.trace.report(TYPE_MISMATCH.on(expression, c.expectedType, expressionType));
//        }
//        hasError.set(true);
        return expressionType;
    }
    @Nullable
    public CangJieType  checkType(@Nullable CangJieType expressionType, @NotNull CjExpression expression, @NotNull ResolutionContext context) {
        return checkType(expressionType, expression, context, null, true);
    }

    @NotNull
    public CangJieTypeInfo createCheckedTypeInfo(
            @Nullable CangJieType type,
            @NotNull ResolutionContext<?> context,
            @NotNull CjExpression expression
    ) {
        return checkType(TypeInfoFactoryKt.createTypeInfo(type, context), expression, context);
    }

}
