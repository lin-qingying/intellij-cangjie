package com.huawei.cangjie.resolve;

import com.huawei.cangjie.descriptors.BindingTrace;
import com.huawei.cangjie.descriptors.FunctionDescriptor;
import com.huawei.cangjie.descriptors.SimpleFunctionDescriptor;
import com.huawei.cangjie.psi.CjDeclarationWithBody;
import com.huawei.cangjie.psi.CjNamedFunction;
import com.huawei.cangjie.resolve.calls.smartcasts.DataFlowInfo;
import com.huawei.cangjie.resolve.scopes.LexicalScope;
import com.huawei.cangjie.types.expressions.ExpressionTypingContext;
import com.huawei.cangjie.types.expressions.ExpressionTypingServices;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class BodyResolver {
    @NotNull
    private final ObservableBindingTrace trace;
    @NotNull
    private final OverloadChecker overloadChecker;
    @NotNull
    private final BodyResolveCache bodyResolveCache;
    @NotNull
    private ExpressionTypingServices expressionTypingServices;

    public BodyResolver(
            @NotNull Project project,
//            @NotNull AnnotationResolver annotationResolver,
            @NotNull BodyResolveCache bodyResolveCache,
//            @NotNull CallResolver callResolver,
//            @NotNull ControlFlowAnalyzer controlFlowAnalyzer,
//            @NotNull DeclarationsChecker declarationsChecker,
//            @NotNull DelegatedPropertyResolver delegatedPropertyResolver,
            @NotNull ExpressionTypingServices expressionTypingServices,
//            @NotNull AnalyzerExtensions analyzerExtensions,
            @NotNull BindingTrace trace,
//            @NotNull ValueParameterResolver valueParameterResolver,
//            @NotNull AnnotationChecker annotationChecker,
//            @NotNull CangJieBuiltIns builtIns,
            @NotNull OverloadChecker overloadChecker
//            @NotNull LanguageVersionSettings languageVersionSettings
    ) {
        this.bodyResolveCache = bodyResolveCache;
        this.trace = new ObservableBindingTrace(trace);
        this.overloadChecker = overloadChecker;
        this.expressionTypingServices = expressionTypingServices;
    }

    private void resolveBehaviorDeclarationBodies(@NotNull BodiesResolveContext c) {


        resolveFunctionBodies(c);


    }

    private void resolveFunctionBodies(BodiesResolveContext c) {

        for (Map.Entry<CjNamedFunction, SimpleFunctionDescriptor> entry : c.getFunctions().entrySet()) {
            CjNamedFunction declaration = entry.getKey();

//            LexicalScope scope = c.getDeclaringScope(declaration);
//            assert scope != null : "Scope is null: " + PsiUtilsCj.getElementTextWithContext(declaration);

//            if (!c.getTopDownAnalysisMode().isLocalDeclarations() && !(bodyResolveCache instanceof BodyResolveCache.ThrowException) &&
//                    expressionTypingServices.getStatementFilter() != StatementFilter.NONE) {
            bodyResolveCache.resolveFunctionBody(declaration).addOwnDataTo(trace, true);
//            }
//            else {
//                resolveFunctionBody(c.getOuterDataFlowInfo(), trace, declaration, entry.getValue(), scope, c.getLocalContext());
//            }
        }
    }

    public void resolveFunctionBody(
            @NotNull DataFlowInfo outerDataFlowInfo,
            @NotNull BindingTrace trace,
            @NotNull CjDeclarationWithBody function,
            @NotNull FunctionDescriptor functionDescriptor,
            @NotNull LexicalScope declaringScope
            ,
            @Nullable ExpressionTypingContext localContext
    ) {
//        computeDeferredType(functionDescriptor.getReturnType());

        resolveFunctionBody(outerDataFlowInfo, trace, function, functionDescriptor, declaringScope, null, null, localContext);
//TODO 检查返回值
//        assert functionDescriptor.getReturnType() != null;
    }

    private void resolveFunctionBody(
            @NotNull DataFlowInfo outerDataFlowInfo,
            @NotNull BindingTrace trace,
            @NotNull CjDeclarationWithBody function,
            @NotNull FunctionDescriptor functionDescriptor,
            @NotNull LexicalScope scope,
            @Nullable Function1<LexicalScope, DataFlowInfo> beforeBlockBody,
            // Creates wrapper scope for header resolution if necessary (see resolveSecondaryConstructorBody)
            @Nullable Function1<LexicalScope, LexicalScope> headerScopeFactory,
//            ,
            @Nullable ExpressionTypingContext localContext
    ) {
        ProgressManager.checkCanceled();

//        PreliminaryDeclarationVisitor.Companion.createForDeclaration(function, trace, languageVersionSettings);
        LexicalScope innerScope = FunctionDescriptorUtil.getFunctionInnerScope(scope, functionDescriptor, trace, overloadChecker);
//        List<CjParameter> valueParameters = function.getValueParameters();
//        List<ValueParameterDescriptor> valueParameterDescriptors = functionDescriptor.getValueParameters();
//
        LexicalScope headerScope = headerScopeFactory != null ? headerScopeFactory.invoke(innerScope) : innerScope;
//        valueParameterResolver.resolveValueParameters(
//                valueParameters, valueParameterDescriptors, headerScope, outerDataFlowInfo, trace,
//                localContext != null ? localContext.inferenceSession : null
//        );

        // Synthetic "field" creation
//        if (functionDescriptor instanceof PropertyAccessorDescriptor && functionDescriptor.getExtensionReceiverParameter() == null
//                && functionDescriptor.getContextReceiverParameters().isEmpty()) {
//            PropertyAccessorDescriptor accessorDescriptor = (PropertyAccessorDescriptor) functionDescriptor;
//            CjProperty property = (CjProperty) function.getParent();
//            SourceElement propertySourceElement = CangJieSourceElementCj.toSourceElement(property);
//            SyntheticFieldDescriptor fieldDescriptor = new SyntheticFieldDescriptor(accessorDescriptor, propertySourceElement);
//            innerScope = new LexicalScopeImpl(innerScope, functionDescriptor, true, null, Collections.emptyList(),
//                    LexicalScopeKind.PROPERTY_ACCESSOR_BODY,
//                    LocalRedeclarationChecker.DO_NOTHING.INSTANCE, handler -> {
//                handler.addVariableDescriptor(fieldDescriptor);
//                return Unit.INSTANCE;
//            });
//            // Check parameter name shadowing
//            for (CjParameter parameter : function.getValueParameters()) {
//                if (SyntheticFieldDescriptor.NAME.equals(parameter.getNameAsName())) {
//                    trace.report(Errors.getACCESSOR_PARAMETER_NAME_SHADOWING().on(parameter));
//                }
//            }
//        }

        DataFlowInfo dataFlowInfo = null;

        if (beforeBlockBody != null) {
            dataFlowInfo = beforeBlockBody.invoke(headerScope);
        }

        if (function.hasBody()) {
            expressionTypingServices.checkFunctionReturnType(
                    innerScope, function, functionDescriptor, dataFlowInfo != null ? dataFlowInfo : outerDataFlowInfo, null, trace, localContext
            );
        }
//TODO 检查返回值
//        assert functionDescriptor.getReturnType() != null;
    }

    public void resolveBodies(@NotNull BodiesResolveContext c) {
        resolveBehaviorDeclarationBodies(c);
//        controlFlowAnalyzer.process(c);
//        declarationsChecker.process(c);
//        analyzerExtensions.process(c);
    }
}
