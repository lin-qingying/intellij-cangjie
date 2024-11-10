package com.linqingying.cangjie.types.expressions;

import com.intellij.openapi.progress.ProgressManager;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import com.linqingying.cangjie.builtins.CangJieBuiltIns;
import com.linqingying.cangjie.config.LanguageVersionSettings;
import com.linqingying.cangjie.descriptors.BindingTrace;
import com.linqingying.cangjie.descriptors.DeclarationDescriptor;
import com.linqingying.cangjie.descriptors.FunctionDescriptor;
import com.linqingying.cangjie.descriptors.impl.FunctionDescriptorImpl;
import com.linqingying.cangjie.descriptors.impl.PropertyAccessorDescriptorImpl;
import com.linqingying.cangjie.psi.*;
import com.linqingying.cangjie.resolve.*;
import com.linqingying.cangjie.resolve.calls.components.InferenceSession;
import com.linqingying.cangjie.resolve.calls.context.ContextDependency;
import com.linqingying.cangjie.resolve.calls.context.ResolutionContext;
import com.linqingying.cangjie.resolve.calls.smartcasts.DataFlowInfo;
import com.linqingying.cangjie.resolve.calls.smartcasts.DataFlowValue;
import com.linqingying.cangjie.resolve.calls.tower.CangJieResolutionCallbacksImpl;
import com.linqingying.cangjie.resolve.scopes.*;
import com.linqingying.cangjie.resolve.source.PsiSourceElement;
import com.linqingying.cangjie.types.CangJieType;
import com.linqingying.cangjie.types.ErrorUtils;
import com.linqingying.cangjie.types.error.ErrorTypeKind;
import com.linqingying.cangjie.types.expressions.typeInfoFactory.TypeInfoFactoryKt;
import com.linqingying.cangjie.types.util.TypeUtilKt;
import com.linqingying.cangjie.utils.exceptions.CangJieTypeInfo;
import com.linqingying.cangjie.utils.slicedMap.WritableSlice;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.List;

import static com.linqingying.cangjie.types.expressions.CoercionStrategy.COERCION_TO_UNIT;
import static com.linqingying.cangjie.types.util.TypeUtils.*;

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

    @Nullable
    public static CangJieResolutionCallbacksImpl.LambdaInfo getNewInferenceLambdaInfo(
            @NotNull ExpressionTypingContext context,
            @NotNull CjElement function
    ) {
        if (function instanceof CjFunction) {
            return context.trace.get(BindingContext.NEW_INFERENCE_LAMBDA_INFO, (CjFunction) function);
        }
        return null;
    }

    @NotNull
    public CangJieTypeInfo getTypeInfo(
            @NotNull LexicalScope scope,
            @NotNull CjExpression expression,

            @NotNull DataFlowInfo dataFlowInfo,
            @NotNull InferenceSession inferenceSession,
            @NotNull BindingTrace trace,
            boolean isStatement
    ) {
        return getTypeInfo(
                scope, expression, NO_EXPECTED_TYPE, dataFlowInfo, inferenceSession,
                trace, isStatement, expression, ContextDependency.INDEPENDENT
        );
    }
    @NotNull
    public CangJieTypeInfo getTypeInfo(
            @NotNull LexicalScope scope,
            @NotNull CjExpression expression,



            @NotNull BindingTrace trace

    ) {
        return getTypeInfo(
                scope, expression, NO_EXPECTED_TYPE, DataFlowInfo.Companion.getEMPTY(), InferenceSession.Companion.getDefault(),
                trace, false, expression, ContextDependency.INDEPENDENT
        );
    }
    @NotNull
    public CangJieTypeInfo getTypeInfo(
            @NotNull LexicalScope scope,
            @NotNull CjExpression expression,

            @NotNull DataFlowInfo dataFlowInfo,

            @NotNull BindingTrace trace

    ) {
        return getTypeInfo(
                scope, expression, NO_EXPECTED_TYPE, dataFlowInfo, InferenceSession.Companion.getDefault(),
                trace, false, expression, ContextDependency.INDEPENDENT
        );
    }
    @NotNull
    public CangJieTypeInfo getTypeInfo(
            @NotNull LexicalScope scope,
            @NotNull CjExpression expression,

            @NotNull DataFlowInfo dataFlowInfo,
            @NotNull InferenceSession inferenceSession,
            @NotNull BindingTrace trace

    ) {
        return getTypeInfo(
                scope, expression, NO_EXPECTED_TYPE, dataFlowInfo, inferenceSession,
                trace, false, expression, ContextDependency.INDEPENDENT
        );
    }
    @NotNull
    public CangJieTypeInfo getTypeInfo(
            @NotNull LexicalScope scope,
            @NotNull CjExpression expression,
            @NotNull CangJieType expectedType,
            @NotNull DataFlowInfo dataFlowInfo,
            @NotNull InferenceSession inferenceSession,
            @NotNull BindingTrace trace,
            boolean isStatement
    ) {
        return getTypeInfo(
                scope, expression, expectedType, dataFlowInfo, inferenceSession,
                trace, isStatement, expression, ContextDependency.INDEPENDENT
        );
    }

    @NotNull
    public StatementFilter getStatementFilter() {
        return statementFilter;
    }

    @NotNull
    public CangJieTypeInfo getTypeInfo(
            @NotNull LexicalScope scope,
            @NotNull CjExpression expression,
            @NotNull CangJieType expectedType,
            @NotNull DataFlowInfo dataFlowInfo,
            @NotNull InferenceSession inferenceSession,
            @NotNull BindingTrace trace,
            boolean isStatement,
            @NotNull CjExpression contextExpression,
            @NotNull ContextDependency contextDependency
    ) {
        ExpressionTypingContext context = ExpressionTypingContext.newContext(
                trace, scope, dataFlowInfo, expectedType, contextDependency, statementFilter, getLanguageVersionSettings(),
                expressionTypingComponents.dataFlowValueFactory, inferenceSession
        );
        if (contextExpression != expression) {
            context = context.replaceExpressionContextProvider(arg -> arg == expression ? contextExpression : null);
        }
        return expressionTypingFacade.getTypeInfo(expression, context, isStatement);
    }

    @Nullable
    public CangJieType getType(
            @NotNull LexicalScope scope,
            @NotNull CjExpression expression,
            @NotNull CangJieType expectedType,
            @NotNull DataFlowInfo dataFlowInfo,
            @NotNull InferenceSession inferenceSession,
            @NotNull BindingTrace trace
    ) {
        return getTypeInfo(scope, expression, expectedType, dataFlowInfo, inferenceSession, trace, false).getType();
    }

    @NotNull
    public CangJieType safeGetType(
            @NotNull LexicalScope scope,
            @NotNull CjExpression expression,
            @NotNull CangJieType expectedType,
            @NotNull DataFlowInfo dataFlowInfo,
            @NotNull InferenceSession inferenceSession,
            @NotNull BindingTrace trace
    ) {
        CangJieType type = getType(scope, expression, expectedType, dataFlowInfo, inferenceSession, trace);

        return type != null ? type : ErrorUtils.createErrorType(ErrorTypeKind.NO_RECORDED_TYPE, expression.getText());
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
                (context.expectedType == UNIT_EXPECTED_TYPE ||
                        //the first check is necessary to avoid invocation 'isUnit(UNIT_EXPECTED_TYPE)'
                        (
                                coercionStrategyForLastExpression == COERCION_TO_UNIT &&
                                        CangJieBuiltIns.isUnit(context.expectedType)
                        )
                );


        if (context.expectedType != NO_EXPECTED_TYPE && context.expectedType != EXPRESSION_TYPE) {
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
        context = context.replaceExpectedType(NO_EXPECTED_TYPE);

        if (!(statementExpression instanceof CjReturnExpression)) {
            var parentDeclaration =
                    context.trace.getBindingContext().get(BindingContext.DECLARATION_TO_DESCRIPTOR, context.getContextParentOfType(
                            statementExpression,
                            CjDeclaration.class
                    ));

            CangJieType type = null;
            if (parentDeclaration instanceof PropertyAccessorDescriptorImpl) {
                type = ((PropertyAccessorDescriptorImpl) parentDeclaration).getReturnType();
            }
            if (parentDeclaration instanceof FunctionDescriptorImpl && (statementExpression.getParent() instanceof CjFunction || statementExpression.getParent() instanceof CjPropertyAccessor)) {
                if (((FunctionDescriptorImpl) parentDeclaration).getReturnType() != null && !CangJieBuiltIns.isUnit(((FunctionDescriptorImpl) parentDeclaration).getReturnType())) {
//                    context = context.replaceExpectedType(parentDeclaration.getReturnType());
//fix 修复对于该语句执行时，方法返回值还为推断时出现的类型一致
                    if (((FunctionDescriptorImpl) parentDeclaration).getSource() instanceof PsiSourceElement && ((PsiSourceElement) ((FunctionDescriptorImpl) parentDeclaration).getSource()).getPsi() instanceof CjFunction) {

                        if (!(((CjFunction) ((PsiSourceElement) ((FunctionDescriptorImpl) parentDeclaration).getSource()).getPsi()).getTypeReference() == null
                        )) {
                            type = ((FunctionDescriptorImpl) parentDeclaration).getReturnType();
                        }
                    }
                }
            }
            if (type != null && !TypeUtilKt.isUnit(type)) {
                context = context.replaceExpectedType(type);

            }
        }
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
//        ExpressionTypingContext newContext = context.replaceScope(scope).replaceExpectedType(NO_EXPECTED_TYPE);
        ExpressionTypingContext newContext = context.replaceScope(scope).replaceExpectedType(EXPRESSION_TYPE);


        CangJieTypeInfo result = TypeInfoFactoryKt.noTypeInfo(context);

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
//                最后一条语句也需要检查类型，虽然在前面如果有return语句而无法到达，但是检查类型是必要的  该分支一定会执行
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
//如方法没有显示指定返回值，推断返回值并更改
//        PsiElement blockParent = expression.getParent();
//        if (blockParent instanceof CjFunction && ((CjFunction) blockParent).getTypeReference() == null) {
//            CangJieType returnType = expressionTypingComponents.functionReturnResolver.resolveFunctionReturn(expression, context);
//            FunctionDescriptor functionDescriptor = context.trace.getBindingContext().get(BindingContext.FUNCTION, blockParent);
//            if (functionDescriptor instanceof FunctionDescriptorImpl) {
//                if (returnType != null) {
//                    ((FunctionDescriptorImpl) functionDescriptor).setReturnType(returnType);
//                    return TypeInfoFactoryKt.createTypeInfo(returnType);
//                }
//            }
//        }

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

        try {

            context.config.getAddVariableDescriptor().get(PsiTreeUtil.getParentOfType(expression, CjPatternEntryBlock.class)).forEach(
                    it -> it.invoke(scope)
            );
//            清空
            context.config.getAddVariableDescriptor().put(PsiTreeUtil.getParentOfType(expression, CjPatternEntryBlock.class), null);

        } catch (NullPointerException ignored) {

        }

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
//                        ? context.replaceExpectedType(NO_EXPECTED_TYPE)
                        ? context.replaceExpectedType(EXPRESSION_TYPE)
                        : context;

        expressionTypingFacade.getTypeInfo(bodyExpression, newContext, blockBody);
    }

    public ExpressionTypingContext createContext(
            @NotNull LexicalScope functionInnerScope,

            @NotNull DataFlowInfo dataFlowInfo,
            @Nullable CangJieType expectedReturnType,
            BindingTrace trace

    ) {

        return ExpressionTypingContext.newContext(
                trace,
                functionInnerScope, dataFlowInfo, expectedReturnType != null ? expectedReturnType : NO_EXPECTED_TYPE,
                getLanguageVersionSettings(), expressionTypingComponents.dataFlowValueFactory,
                InferenceSession.Companion.getDefault()
        );
    }

    public CangJieTypeInfo resolveFunctionReturnType(
            @NotNull LexicalScope functionInnerScope,
            @NotNull CjDeclarationWithBody function,
            @NotNull FunctionDescriptor functionDescriptor,
            @NotNull DataFlowInfo dataFlowInfo,
            @Nullable CangJieType expectedReturnType,
            BindingTrace trace,
            @Nullable ExpressionTypingContext localContext
    ) {

        ExpressionTypingContext context = ExpressionTypingContext.newContext(
                trace,
                functionInnerScope, dataFlowInfo, expectedReturnType != null ? expectedReturnType : NO_EXPECTED_TYPE,
                getLanguageVersionSettings(), expressionTypingComponents.dataFlowValueFactory,
                localContext != null ? localContext.inferenceSession : InferenceSession.Companion.getDefault()
        );
        return getBlockReturnedType(function.getBodyBlockExpression(), context, false);
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
                getLanguageVersionSettings(), expressionTypingComponents.dataFlowValueFactory,
                localContext != null ? localContext.inferenceSession : InferenceSession.Companion.getDefault()
        );

        checkFunctionReturnType(function, context);
    }

    @NotNull
    public LanguageVersionSettings getLanguageVersionSettings() {
        return expressionTypingComponents.languageVersionSettings;
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
