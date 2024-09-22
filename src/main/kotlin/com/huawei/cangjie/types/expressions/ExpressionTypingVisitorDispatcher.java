package com.huawei.cangjie.types.expressions;

import com.huawei.cangjie.descriptors.PsiDiagnosticUtils;
import com.huawei.cangjie.diagnostics.Errors;
import com.huawei.cangjie.psi.*;
import com.huawei.cangjie.psi.codeFragmentUtil.CodeFragmentUtilKt;
import com.huawei.cangjie.resolve.AnnotationChecker;
import com.huawei.cangjie.resolve.BindingContext;
import com.huawei.cangjie.resolve.BindingContextUtils;
import com.huawei.cangjie.resolve.BindingContextUtilsKt;
import com.huawei.cangjie.resolve.calls.context.CallPosition;
import com.huawei.cangjie.resolve.scopes.LexicalScopeKind;
import com.huawei.cangjie.resolve.scopes.LexicalWritableScope;
import com.huawei.cangjie.storage.ReenteringLazyValueComputationException;
import com.huawei.cangjie.types.CangJieType;
import com.huawei.cangjie.types.DeferredType;
import com.huawei.cangjie.types.ErrorUtils;
import com.huawei.cangjie.types.error.ErrorTypeKind;
import com.huawei.cangjie.types.expressions.typeInfoFactory.TypeInfoFactoryKt;
import com.huawei.cangjie.utils.CangJieExceptionWithAttachments;
import com.huawei.cangjie.utils.CangJieFrontEndException;
import com.huawei.cangjie.utils.PerformanceCounter;
import com.huawei.cangjie.utils.exceptions.CangJieTypeInfo;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.IndexNotReadyException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.huawei.cangjie.types.util.TypeUtils.EXPRESSION_TYPE;

public abstract class ExpressionTypingVisitorDispatcher extends CjVisitor<CangJieTypeInfo, ExpressionTypingContext>
        implements ExpressionTypingInternals {
    public static final PerformanceCounter typeInfoPerfCounter = PerformanceCounter.Companion.create("Type info", true);
    private static final Logger LOG = Logger.getInstance(ExpressionTypingVisitor.class);
    protected final BasicExpressionTypingVisitor basic;
    protected final FunctionsTypingVisitor functions;
    protected final TuplesTypingVisitor tuples;
    protected final ControlStructureTypingVisitor controlStructures;
    private final ExpressionTypingComponents components;
    @NotNull
    private final AnnotationChecker annotationChecker;
    protected final PatternMatchingTypingVisitor patterns;

    private ExpressionTypingVisitorDispatcher(
            @NotNull ExpressionTypingComponents components,
            @NotNull AnnotationChecker annotationChecker
    ) {
        this.components = components;
        this.annotationChecker = annotationChecker;
        this.basic = new BasicExpressionTypingVisitor(this);
        this.controlStructures = new ControlStructureTypingVisitor(this);
        this.patterns = new PatternMatchingTypingVisitor(this);
        this.functions = new FunctionsTypingVisitor(this);
        this.tuples = new TuplesTypingVisitor(this);
//        this.declarationsCheckerBuilder = components.declarationsCheckerBuilder;
    }

    private static void logOrThrowException(@NotNull CjExpression expression, Throwable e) {
        try {
            String location;
            try {
                location = " in " + PsiDiagnosticUtils.atLocation(expression);
            } catch (Exception ex) {
                location = "";
            }
            // This trows AssertionError in CLI and reports the error in the IDE
            LOG.error(
                    new CangJieExceptionWithAttachments("Exception while analyzing expression" + location, e)
                            .withPsiAttachment("expression.cj", expression)
            );
        } catch (AssertionError errorFromLogger) {
            // If we ended up here, we are in CLI, and the initial exception needs to be rethrown,
            // simply throwing AssertionError causes its being wrapped over and over again
            throw new CangJieFrontEndException(errorFromLogger.getMessage(), e);
        }
    }

    protected ExpressionTypingVisitorForStatements createStatementVisitor(ExpressionTypingContext context) {
        return new ExpressionTypingVisitorForStatements(this,
                ExpressionTypingUtils.newWritableScopeImpl(context, LexicalScopeKind.CODE_BLOCK, components.overloadChecker)
                ,
                basic
                , controlStructures, /*patterns,*/ functions);
    }

    @Override
    public @NotNull CangJieTypeInfo safeGetTypeInfo(@NotNull CjExpression expression, ExpressionTypingContext context) {
        CangJieTypeInfo typeInfo = getTypeInfo(expression, context);
        if (typeInfo.getType() != null) {
            return typeInfo;
        }
        return typeInfo
                .replaceType(ErrorUtils.createErrorType(ErrorTypeKind.NO_RECORDED_TYPE, expression.getText()))
                .replaceDataFlowInfo(context.dataFlowInfo);
    }

    @Override
    public @NotNull CangJieTypeInfo getTypeInfo(@NotNull CjExpression expression, ExpressionTypingContext context) {


        if (context.expectedType == EXPRESSION_TYPE) {
            context = context.replaceExpectedType(components.builtIns.getAnyType());
        }
        CangJieTypeInfo result = getTypeInfo(expression, context, this);
        annotationChecker.checkExpression(expression, context.trace);
        return result;
    }

    protected abstract ExpressionTypingVisitorForStatements getStatementVisitor(@NotNull ExpressionTypingContext context);

    @Override
    public CangJieTypeInfo visitVariable(@NotNull CjVariable variable, ExpressionTypingContext data) {
        return basic.visitVariable(variable, data);
    }

    @NotNull
    private CangJieTypeInfo getTypeInfo(@NotNull CjExpression expression, ExpressionTypingContext context, CjVisitor<CangJieTypeInfo, ExpressionTypingContext> visitor) {
        ProgressManager.checkCanceled();
        return typeInfoPerfCounter.time(() -> {

            try {
                CangJieTypeInfo recordedTypeInfo = BindingContextUtils.getRecordedTypeInfo(expression, context.trace.getBindingContext());
                if (recordedTypeInfo != null) {
                    return recordedTypeInfo;
                }

                context.trace.record(BindingContext.DATA_FLOW_INFO_BEFORE, expression, context.dataFlowInfo);

                CangJieTypeInfo result;
                try {
                    result = expression.accept(visitor, context);

                    if (context.trace.get(BindingContext.PROCESSED, expression) == Boolean.TRUE) {
                        CangJieType type = context.trace.getBindingContext().getType(expression);
                        return result.replaceType(type);
                    }
//
                    if (result.getType() instanceof DeferredType) {
                        result = result.replaceType(((DeferredType) result.getType()).getDelegate());
                    }


                    CangJieType refinedType = result.getType() != null
                            ? components.cangjieTypeChecker.getCangjieTypeRefiner().refineType(result.getType())
                            : null;

                    if (refinedType != result.getType()) {
                        result = result.replaceType(refinedType);
                    }

                    context.trace.record(BindingContext.EXPRESSION_TYPE_INFO, expression, result);

                } catch (ReenteringLazyValueComputationException e) {
//                    context.trace.report(TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM.onError(expression));
                    result = TypeInfoFactoryKt.noTypeInfo(context);
                }
                if (context.isSaveTypeInfo) {

                    context.trace.record(BindingContext.PROCESSED, expression);
                }
                BindingContextUtilsKt.recordScope(context.trace, context.scope, expression);
//                if (context.isSaveTypeInfo) {
                BindingContextUtilsKt.recordDataFlowInfo(context.replaceDataFlowInfo(result.getDataFlowInfo()), expression);

//                }
//                try {
//                    // Here we have to resolve some types, so the following exception is possible
//                    // Example: val a = ::a, fun foo() = ::foo
//                    recordTypeInfo(expression, result);
//                }
//                catch (ReenteringLazyValueComputationException e) {
//                    context.trace.report(TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM.onError(expression));
//                    return TypeInfoFactoryKt.noTypeInfo(context);
//                }
                return result;

            } catch (ProcessCanceledException | CangJieFrontEndException | IndexNotReadyException e) {
                throw e;
            } catch (Throwable e) {
                context.trace.report(Errors.EXCEPTION_FROM_ANALYZER.on(expression, e));
                logOrThrowException(expression, e);
                return TypeInfoFactoryKt.createTypeInfo(
                        ErrorUtils.createErrorType(ErrorTypeKind.TYPE_FOR_COMPILER_EXCEPTION, e.getClass().getSimpleName()),
                        context
                );
            }
        });


    }

    @Override
    public @NotNull CangJieTypeInfo getTypeInfo(@NotNull CjExpression expression, ExpressionTypingContext context, boolean isStatement) {
        ExpressionTypingContext newContext = context;
        if (CodeFragmentUtilKt.suppressDiagnosticsInDebugMode(expression)) {
            newContext = ExpressionTypingContext.newContext(context, true);
        }
        if (!isStatement) return getTypeInfo(expression, newContext);
        return getTypeInfo(expression, newContext, getStatementVisitor(newContext));
    }

    @Override
    public @NotNull CangJieTypeInfo checkInExpression(@NotNull CjElement callElement, @NotNull CjSimpleNameExpression operationSign, @NotNull ValueArgument leftArgument, @Nullable CjExpression right, @NotNull ExpressionTypingContext context) {
        return basic.checkInExpression(callElement, operationSign, leftArgument, right, context);

    }

    @Override
    public void checkStatementType(@NotNull CjExpression expression, ExpressionTypingContext context) {

    }

    @Override
    public @NotNull ExpressionTypingComponents getComponents() {
        return components;

    }

    @Override
    public CangJieTypeInfo visitSimpleNameExpression(@NotNull CjSimpleNameExpression expression, ExpressionTypingContext data) {
        return basic.visitSimpleNameExpression(expression, data);
    }


    @Override
    public CangJieTypeInfo visitParenthesizedExpression(@NotNull CjParenthesizedExpression expression, ExpressionTypingContext data) {
        return basic.visitParenthesizedExpression(expression, data);
    }

    @Override
    public CangJieTypeInfo visitTupleExpression(@NotNull CjTupleExpression expression, ExpressionTypingContext data) {
        return tuples.visitTupleExpression(expression, data);
    }

//////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public CangJieTypeInfo visitConstructorDelegationCall(@NotNull CjConstructorDelegationCall cjConstructorDelegationCall, ExpressionTypingContext data) {
        return super.visitConstructorDelegationCall(cjConstructorDelegationCall, data);
    }


//////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public CangJieTypeInfo visitLambdaExpression(@NotNull CjLambdaExpression expression, ExpressionTypingContext data) {
        // Erasing call position to unknown is necessary to prevent wrong call positions when type checking lambda's body
        return functions.visitLambdaExpression(expression, data.replaceCallPosition(CallPosition.Unknown.INSTANCE));
    }

    @Override
    public CangJieTypeInfo visitNamedFunction(@NotNull CjNamedFunction function, ExpressionTypingContext data) {
        return functions.visitNamedFunction(function, data);
    }

//////////////////////////////////////////////////////////////////////////////////////////////

        @Override
    public CangJieTypeInfo visitThrowExpression(@NotNull CjThrowExpression expression, ExpressionTypingContext data) {
        return controlStructures.visitThrowExpression(expression, data);
    }

    @Override
    public CangJieTypeInfo visitReturnExpression(@NotNull CjReturnExpression expression, ExpressionTypingContext data) {
        return controlStructures.visitReturnExpression(expression, data);
    }


    @Override
    public CangJieTypeInfo visitContinueExpression(@NotNull CjContinueExpression expression, ExpressionTypingContext data) {
        return controlStructures.visitContinueExpression(expression, data);
    }

    @Override
    public CangJieTypeInfo visitIfExpression(@NotNull CjIfExpression expression, ExpressionTypingContext data) {
        return controlStructures.visitIfExpression(expression, data);
    }

    @Override
    public CangJieTypeInfo visitTryExpression(@NotNull CjTryExpression expression, ExpressionTypingContext data) {
        return controlStructures.visitTryExpression(expression, data);
    }

    @Override
    public CangJieTypeInfo visitForExpression(@NotNull CjForExpression expression, ExpressionTypingContext data) {
        return controlStructures.visitForExpression(expression, data);
    }

    @Override
    public CangJieTypeInfo visitWhileExpression(@NotNull CjWhileExpression expression, ExpressionTypingContext data) {
        return controlStructures.visitWhileExpression(expression, data);
    }

    @Override
    public CangJieTypeInfo visitDoWhileExpression(@NotNull CjDoWhileExpression expression, ExpressionTypingContext data) {
        return controlStructures.visitDoWhileExpression(expression, data);
    }

    @Override
    public CangJieTypeInfo visitBreakExpression(@NotNull CjBreakExpression expression, ExpressionTypingContext data) {
        return controlStructures.visitBreakExpression(expression, data);
    }

//////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public CangJieTypeInfo visitIsExpression(@NotNull CjIsExpression expression, ExpressionTypingContext data) {
        return patterns.visitIsExpression(expression, data);
    }

    @Override
    public CangJieTypeInfo visitMatchExpression(@NotNull CjMatchExpression expression, ExpressionTypingContext data) {
        return patterns.visitMatchExpression(expression, data);
    }

//////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public CangJieTypeInfo visitConstantExpression(@NotNull CjConstantExpression expression, ExpressionTypingContext data) {
        return basic.visitConstantExpression(expression, data);
    }

    @Override
    public CangJieTypeInfo visitBinaryWithTypeRHSExpression(@NotNull CjBinaryExpressionWithTypeRHS expression, ExpressionTypingContext data) {
        return basic.visitBinaryWithTypeRHSExpression(expression, data);
    }

    @Override
    public CangJieTypeInfo visitThisExpression(@NotNull CjThisExpression expression, ExpressionTypingContext data) {
        return basic.visitThisExpression(expression, data);
    }

    @Override
    public CangJieTypeInfo visitSuperExpression(@NotNull CjSuperExpression expression, ExpressionTypingContext data) {
        return basic.visitSuperExpression(expression, data);
    }

    @Override
    public CangJieTypeInfo visitBlockExpression(@NotNull CjBlockExpression expression, ExpressionTypingContext data) {
        return basic.visitBlockExpression(expression, data);
    }

    @Override
    public CangJieTypeInfo visitQualifiedExpression(@NotNull CjQualifiedExpression expression, ExpressionTypingContext data) {
        return basic.visitQualifiedExpression(expression, data);
    }

    @Override
    public CangJieTypeInfo visitCallExpression(@NotNull CjCallExpression expression, ExpressionTypingContext data) {
        return basic.visitCallExpression(expression, data);
    }
//
//    @Override
//    public CangJieTypeInfo visitClassLiteralExpression(@NotNull CjClassLiteralExpression expression, ExpressionTypingContext data) {
//        return basic.visitClassLiteralExpression(expression, data);
//    }
//
//    @Override
//    public CangJieTypeInfo visitCallableReferenceExpression(@NotNull CjCallableReferenceExpression expression, ExpressionTypingContext data) {
//        return basic.visitCallableReferenceExpression(expression, data);
//    }
//
//    @Override
//    public CangJieTypeInfo visitObjectLiteralExpression(@NotNull CjObjectLiteralExpression expression, ExpressionTypingContext data) {
//        return basic.visitObjectLiteralExpression(expression, data);
//    }

    @Override
    public CangJieTypeInfo visitUnaryExpression(@NotNull CjUnaryExpression expression, ExpressionTypingContext data) {
        return basic.visitUnaryExpression(expression, data);
    }

    @Override
    public CangJieTypeInfo visitBinaryExpression(@NotNull CjBinaryExpression expression, ExpressionTypingContext data) {
        return basic.visitBinaryExpression(expression, data);
    }

    @Override
    public CangJieTypeInfo visitRangeExpression(@NotNull CjRangeExpression expression, ExpressionTypingContext data) {
        return basic.visitRangeExpression(expression, data);
    }

    @Override
    public CangJieTypeInfo visitArrayAccessExpression(@NotNull CjArrayAccessExpression expression, ExpressionTypingContext data) {
        return basic.visitArrayAccessExpression(expression, data);
    }

//    @Override
//    public CangJieTypeInfo visitLabeledExpression(@NotNull CjLabeledExpression expression, ExpressionTypingContext data) {
//        return basic.visitLabeledExpression(expression, data);
//    }

    @Override
    public CangJieTypeInfo visitDeclaration(@NotNull CjDeclaration dcl, ExpressionTypingContext data) {
        return basic.visitDeclaration(dcl, data);
    }

    @Override
    public CangJieTypeInfo visitStringTemplateExpression(@NotNull CjStringTemplateExpression expression, ExpressionTypingContext data) {
        return basic.visitStringTemplateExpression(expression, data);
    }

    @Override
    public CangJieTypeInfo visitCjElement(@NotNull CjElement element, ExpressionTypingContext data) {
        return element.accept(basic, data);
    }

//    @Override
//    public CangJieTypeInfo visitClass(@NotNull CjClass klass, ExpressionTypingContext data) {
//        return basic.visitClass(klass, data);
//    }
//
//    @Override
//    public CangJieTypeInfo visitProperty(@NotNull CjProperty property, ExpressionTypingContext data) {
//        return basic.visitProperty(property, data);
//    }

    //    protected final BasicExpressionTypingVisitor basic;
//    protected final FunctionsTypingVisitor functions;
//    protected final ControlStructureTypingVisitor controlStructures;
//    protected final PatternMatchingTypingVisitor patterns;
//    protected final DeclarationsCheckerBuilder declarationsCheckerBuilder;
    public static class ForBlock extends ExpressionTypingVisitorDispatcher {

        private final ExpressionTypingVisitorForStatements visitorForBlock;

        public ForBlock(
                @NotNull ExpressionTypingComponents components,
                @NotNull AnnotationChecker annotationChecker,
                @NotNull LexicalWritableScope writableScope
        ) {
            super(components, annotationChecker);
            this.visitorForBlock = new ExpressionTypingVisitorForStatements(
                    this, writableScope
                    , basic
                    , controlStructures,/* patterns,*/ functions
            );
        }

        @Override
        protected ExpressionTypingVisitorForStatements getStatementVisitor(@NotNull ExpressionTypingContext context) {
            return visitorForBlock;
        }
    }

//    @Override
//    public CangJieTypeInfo visitAnnotatedExpression(@NotNull CjAnnotatedExpression expression, ExpressionTypingContext data) {
//        return basic.visitAnnotatedExpression(expression, data);
//    }

    public static class ForDeclarations extends ExpressionTypingVisitorDispatcher {
        public ForDeclarations(@NotNull ExpressionTypingComponents components, @NotNull AnnotationChecker annotationChecker) {
            super(components, annotationChecker);
        }

        @Override
        protected ExpressionTypingVisitorForStatements getStatementVisitor(@NotNull ExpressionTypingContext context) {
            return createStatementVisitor(context);
        }
    }

}
