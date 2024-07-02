package com.huawei.cangjie.types.expressions;

import com.huawei.cangjie.builtins.CangJieBuiltIns;
import com.huawei.cangjie.descriptors.BindingTrace;
import com.huawei.cangjie.descriptors.DeclarationDescriptor;
import com.huawei.cangjie.descriptors.FunctionDescriptor;
import com.huawei.cangjie.psi.*;
import com.huawei.cangjie.resolve.*;
import com.huawei.cangjie.resolve.calls.components.InferenceSession;
import com.huawei.cangjie.resolve.calls.context.ContextDependency;
import com.huawei.cangjie.resolve.calls.context.ResolutionContext;
import com.huawei.cangjie.resolve.calls.smartcasts.DataFlowInfo;
import com.huawei.cangjie.resolve.calls.smartcasts.DataFlowValue;
import com.huawei.cangjie.resolve.scopes.*;
import com.huawei.cangjie.types.CangJieType;
import com.huawei.cangjie.types.expressions.typeInfoFactory.TypeInfoFactoryKt;
import com.huawei.cangjie.utils.exceptions.CangJieTypeInfo;
import com.huawei.cangjie.utils.slicedMap.WritableSlice;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.List;

import static com.huawei.cangjie.types.TypeUtils.NO_EXPECTED_TYPE;
import static com.huawei.cangjie.types.TypeUtils.UNIT_EXPECTED_TYPE;
import static com.huawei.cangjie.types.expressions.CoercionStrategy.COERCION_TO_UNIT;

public class ExpressionTypingServices {
    private final ExpressionTypingFacade expressionTypingFacade;
    private final ExpressionTypingComponents expressionTypingComponents;

    @NotNull
    private final AnnotationChecker annotationChecker;
    @NotNull
    private final StatementFilter statementFilter;

    public ExpressionTypingServices(
            @NotNull ExpressionTypingComponents components,
            @NotNull AnnotationChecker annotationChecker,
            @NotNull StatementFilter statementFilter,
            @NotNull ExpressionTypingVisitorDispatcher.ForDeclarations facade
    ) {
        this.expressionTypingComponents = components;
        this.annotationChecker = annotationChecker;
        this.statementFilter = statementFilter;
        this.expressionTypingFacade = facade;
    }
    @NotNull
    public CangJieTypeInfo getTypeInfo(@NotNull CjExpression expression, @NotNull ResolutionContext resolutionContext) {
        return expressionTypingFacade.getTypeInfo(expression, ExpressionTypingContext.newContext(resolutionContext));
    }
    public LocalRedeclarationChecker createLocalRedeclarationChecker(BindingTrace trace) {
        return new TraceBasedLocalRedeclarationChecker(trace, expressionTypingComponents.overloadChecker);
    }
    private CangJieTypeInfo getTypeOfLastExpressionInBlock(
            @NotNull CjExpression statementExpression,
            @NotNull ExpressionTypingContext context,
            @NotNull CoercionStrategy coercionStrategyForLastExpression,
            @NotNull ExpressionTypingInternals blockLevelVisitor
    ) {
        boolean isUnitExpectedType = context.expectedType != NO_EXPECTED_TYPE &&
                (
                        context.expectedType == UNIT_EXPECTED_TYPE ||
                                //the first check is necessary to avoid invocation 'isUnit(UNIT_EXPECTED_TYPE)'
                                (
                                        coercionStrategyForLastExpression == COERCION_TO_UNIT &&
                                                CangJieBuiltIns.isUnit(context.expectedType)
                                )
                );

        // This part is necessary for properly handling the case of calls like `foo { ::bar }`.
        // In NI, `::bar` shouldn't be eagerly resolved, but introduced as a postponed atom to the general inference system.
        // The correct definition for the case would be satisfaction of three conditions:
        // (1) lastStatement is CjCallableReferenceExpression
        // (2) expected type is not Unit
        // (3) lastStatement.parent.parent [statement -> block -> function literal] is a lambda that is handled by NI
        //
        // When the conditions are met, `createCallArgument` at `org.jetbrains.kotlin.resolve.calls.tower.CangJieResolutionCallbacksImpl.analyzeAndGetLambdaReturnArguments`
        // we just pick that last statement in the lambda.
        //
        // But due to a bug in initial implementation `createDontCareTypeInfoForNILambda` (third condition), it was checking that
        // "lastStatement has a NI lambda among one of the parents".
        //
        // That immediately leads to incorrectly treatment of cases like:
        // foo {
        //     val x = if (b) {
        //          ::baz <-- the problem is here
        //     } else {
        //        ....
        //     }
        //     println()
        // }
        //
        // The problem with the `::baz` is that it should not be processed as a postponed atom for the call, but it has been.
        // At the same time, it's being ignored by the NI (because it's not the last statement), thus left unresolved.
        // The bugs are described at KT-52270 and KT-55931.
        //
        // During attempt to fix KT-52270, instead of fixing the third condition (that is actually broken), the second one has been changed to
        // "expected type is NO_EXPECTED_TYPE".
        // That helped to the main use case from the issue, but still didn't help with KT-55931.
        //
        // But the actual problem is that brought another regression bug (KT-55729) because after that we started
        // resolving last-statement callable references with expected type eagerly as regular top-level references (without conversions).
        // So, conversions stopped working there since 1.8.0
        //
        // While it might be tempting to revert the incorrect fix for KT-52270, and fix the third condition, we can't do it easily
        // because it should be a language feature (since it allows new green code), that can't be released in 1.8.10.
        // So, we're just trying to _partially_ revert the change, by allowing skipping reference not only in case of NO_EXPECTED_TYPE
        // but also in case we have non-trivial expected type AND lastStatement.parent.parent is function literal.
        //
        // That would mean that everything will work just the same as in 1.8.0, but the single case of
        // `expectSomeSpecificFunctionTypeReturnedFromLambda { ::functionNeededAConversion }` (that was been working in 1.7.20).
        //
        // On the KT-55931, currently we don't have plans to fix it until K2 (where it already seems to be fixed).
//        if (isCallableReferenceShouldBeAttemptedToBeProcessedByNI(statementExpression, context, isUnitExpectedType)) {
//            CangJieTypeInfo typeInfo = createDontCareTypeInfoForNILambda(statementExpression, context);
//            if (typeInfo != null) return typeInfo;
//        }

        if (context.expectedType != NO_EXPECTED_TYPE) {
            CangJieType expectedType;
            if (isUnitExpectedType) {
                expectedType = UNIT_EXPECTED_TYPE;
            } else {
                expectedType = context.expectedType;
            }

            return blockLevelVisitor.getTypeInfo(statementExpression, context.replaceExpectedType(expectedType), true);
        }

//        if (CjPsiUtil.deparenthesize(statementExpression) instanceof CjLambdaExpression && context.contextDependency == ContextDependency.DEPENDENT) {
//            CangJieTypeInfo typeInfo = createDontCareTypeInfoForNILambda(statementExpression, context);
//            if (typeInfo != null) return typeInfo;
//        }

        CangJieTypeInfo result = blockLevelVisitor.getTypeInfo(statementExpression, context, true);
        if (coercionStrategyForLastExpression == COERCION_TO_UNIT) {
            boolean mightBeUnit = false;
            if (statementExpression instanceof CjDeclaration) {
                if (!(statementExpression instanceof CjNamedFunction) || statementExpression.getName() != null) {
                    mightBeUnit = true;
                }
            }
            if (statementExpression instanceof CjBinaryExpression binaryExpression) {
                IElementType operationType = binaryExpression.getOperationToken();
                //noinspection SuspiciousMethodCalls
//                if (operationType == CjTokens.EQ || OperatorConventions.ASSIGNMENT_OPERATIONS.containsKey(operationType)) {
//                    mightBeUnit = true;
//                }
            }
            if (mightBeUnit) {
                // ExpressionTypingVisitorForStatements should return only null or Unit for declarations and assignments,
                // but (for correct assignment / initialization analysis) data flow info must be preserved
                assert result.getType() == null || CangJieBuiltIns.isUnit(result.getType());
                result = result.replaceType(expressionTypingComponents.builtIns.getUnitType());
            }
        }
        return result;
    }

    /**
     * Visits block statements propagating data flow information from the first to the last.
     * Determines block returned type and data flow information at the end of the block AND
     * at the nearest jump point from the block beginning.
     */
    /*package*/ CangJieTypeInfo getBlockReturnedTypeWithWritableScope(
            @NotNull LexicalWritableScope scope,
            @NotNull List<? extends CjElement> block,
            @NotNull CoercionStrategy coercionStrategyForLastExpression,
            @NotNull ExpressionTypingContext context
    ) {
        if (block.isEmpty()) {
            return TypeInfoFactoryKt.createTypeInfo(expressionTypingComponents.builtIns.getUnitType(), context);
        }

        ExpressionTypingInternals blockLevelVisitor = new ExpressionTypingVisitorDispatcher.ForBlock(
                expressionTypingComponents, annotationChecker, scope);
        ExpressionTypingContext newContext = context.replaceScope(scope).replaceExpectedType(NO_EXPECTED_TYPE);

        CangJieTypeInfo result = TypeInfoFactoryKt.noTypeInfo(context);
        // Jump point data flow info
        DataFlowInfo beforeJumpInfo = newContext.dataFlowInfo;
        boolean jumpOutPossible = false;

        boolean isFirstStatement = true;
        for (Iterator<? extends CjElement> iterator = block.iterator(); iterator.hasNext(); ) {
            ProgressManager.checkCanceled();
            // Use filtering trace to keep effect system cache only for one statement
            AbstractFilteringTrace traceForSingleStatement = new EffectsFilteringTrace(context.trace);

            newContext = newContext.replaceBindingTrace(traceForSingleStatement);


            CjElement statement = iterator.next();
            if (!(statement instanceof CjExpression statementExpression)) {
                continue;
            }
            if (!iterator.hasNext()) {
                result = getTypeOfLastExpressionInBlock(
                        statementExpression, newContext.replaceExpectedType(context.expectedType), coercionStrategyForLastExpression,
                        blockLevelVisitor);
                if (result.getType() != null && statementExpression.getParent() instanceof CjBlockExpression) {
                    DataFlowValue lastExpressionValue = expressionTypingComponents.dataFlowValueFactory.createDataFlowValue(
                            statementExpression, result.getType(), context);
                    DataFlowValue blockExpressionValue = expressionTypingComponents.dataFlowValueFactory.createDataFlowValue(
                            (CjBlockExpression) statementExpression.getParent(), result.getType(), context);
                    result = result.replaceDataFlowInfo(result.getDataFlowInfo().assign(blockExpressionValue, lastExpressionValue/*,
                            expressionTypingComponents.languageVersionSettings*/));
                }
            } else {
                result = blockLevelVisitor
                        .getTypeInfo(statementExpression, newContext.replaceContextDependency(ContextDependency.INDEPENDENT), true);
            }

            DataFlowInfo newDataFlowInfo = result.getDataFlowInfo();
            // If jump is not possible, we take new data flow info before jump
            if (!jumpOutPossible) {
                beforeJumpInfo = result.getJumpFlowInfo();
                jumpOutPossible = result.getJumpOutPossible();
            }
            if (newDataFlowInfo != newContext.dataFlowInfo) {
                newContext = newContext.replaceDataFlowInfo(newDataFlowInfo);
                // We take current data flow info if jump there is not possible
            }
            blockLevelVisitor = new ExpressionTypingVisitorDispatcher.ForBlock(expressionTypingComponents, annotationChecker, scope);

            DeclarationDescriptor ownerDescriptor = scope.getOwnerDescriptor();

            if (isFirstStatement && ownerDescriptor instanceof FunctionDescriptor) {
//                expressionTypingComponents.contractParsingServices.checkContractAndRecordIfPresent(
//                        statementExpression, context.trace, (FunctionDescriptor) ownerDescriptor
//                );
                isFirstStatement = false;
            }
        }
        return result.replaceJumpOutPossible(jumpOutPossible).replaceJumpFlowInfo(beforeJumpInfo);

    }

    @NotNull
    public CangJieTypeInfo getBlockReturnedType(CjBlockExpression expression, ExpressionTypingContext context, boolean isStatement) {
        return getBlockReturnedType(expression, isStatement ? COERCION_TO_UNIT : CoercionStrategy.NO_COERCION, context);
    }

    @NotNull
    public CangJieTypeInfo getBlockReturnedType(
            @NotNull CjBlockExpression expression,
            @NotNull CoercionStrategy coercionStrategyForLastExpression,
            @NotNull ExpressionTypingContext context
    ) {
        List<CjExpression> block = StatementFilterKt.filterStatements(statementFilter, expression);

        DeclarationDescriptor containingDescriptor = context.scope.getOwnerDescriptor();
        TraceBasedLocalRedeclarationChecker redeclarationChecker
                = new TraceBasedLocalRedeclarationChecker(context.trace, expressionTypingComponents.overloadChecker);
        LexicalWritableScope scope = new LexicalWritableScope(context.scope, containingDescriptor, false, redeclarationChecker,
                LexicalScopeKind.CODE_BLOCK);

        CangJieTypeInfo r;
        if (block.isEmpty()) {
            r = expressionTypingComponents.dataFlowAnalyzer
                    .createCheckedTypeInfo(expressionTypingComponents.builtIns.getUnitType(), context, expression);
        } else {
            r = getBlockReturnedTypeWithWritableScope(scope, block, coercionStrategyForLastExpression,
                    context.replaceStatementFilter(statementFilter));
        }
        scope.freeze();


        return r;
    }

    /*package*/ void checkFunctionReturnType(CjDeclarationWithBody function, ExpressionTypingContext context) {
        CjExpression bodyExpression = function.getBodyExpression();
        if (bodyExpression == null) return;

        boolean blockBody = function.hasBlockBody();
        ExpressionTypingContext newContext =
                blockBody
                        ? context.replaceExpectedType(NO_EXPECTED_TYPE)
                        : context;

        expressionTypingFacade.getTypeInfo(bodyExpression, newContext, blockBody);
    }

    public void checkFunctionReturnType(
            @NotNull LexicalScope functionInnerScope,
            @NotNull CjDeclarationWithBody function,
            @NotNull FunctionDescriptor functionDescriptor,
            @NotNull DataFlowInfo dataFlowInfo,
            @Nullable CangJieType expectedReturnType,
            BindingTrace trace,
            @Nullable ExpressionTypingContext localContext
    ) {
        if (expectedReturnType == null) {
            expectedReturnType = functionDescriptor.getReturnType();
            if (!function.hasBlockBody() && !function.hasDeclaredReturnType()) {
                expectedReturnType = NO_EXPECTED_TYPE;
            }
        }

        ExpressionTypingContext context = ExpressionTypingContext.newContext(
                trace,
                functionInnerScope, dataFlowInfo, expectedReturnType != null ? expectedReturnType : NO_EXPECTED_TYPE,
                /*getLanguageVersionSettings(),*/ expressionTypingComponents.dataFlowValueFactory,
                localContext != null ? localContext.inferenceSession : InferenceSession.Companion.getDefault()
        );

        checkFunctionReturnType(function, context);
    }

    private static class EffectsFilteringTrace extends AbstractFilteringTrace {
        public EffectsFilteringTrace(BindingTrace parentTrace) {
            super(parentTrace, "Effects filtering trace");
        }

        @Override
        protected <K, V> boolean shouldBeHiddenFromParent(@NotNull WritableSlice<K, V> slice, K key) {
            return slice == BindingContext.EXPRESSION_EFFECTS;
        }
    }
}
