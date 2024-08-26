package com.huawei.cangjie.resolve;

import com.google.common.collect.Maps;
import com.huawei.cangjie.builtins.CangJieBuiltIns;
import com.huawei.cangjie.config.LanguageVersionSettings;
import com.huawei.cangjie.descriptors.*;
import com.huawei.cangjie.psi.*;
import com.huawei.cangjie.psi.psiUtil.PsiUtilsKt;
import com.huawei.cangjie.resolve.calls.CallResolver;
import com.huawei.cangjie.resolve.calls.components.InferenceSession;
import com.huawei.cangjie.resolve.calls.model.ResolvedCall;
import com.huawei.cangjie.resolve.calls.smartcasts.DataFlowInfo;
import com.huawei.cangjie.resolve.check.DeclarationsChecker;
import com.huawei.cangjie.resolve.scopes.*;
import com.huawei.cangjie.types.CangJieType;
import com.huawei.cangjie.types.DeferredType;
import com.huawei.cangjie.types.ErrorUtils;
import com.huawei.cangjie.types.expressions.ExpressionTypingContext;
import com.huawei.cangjie.types.expressions.ExpressionTypingServices;
import com.huawei.cangjie.types.expressions.PreliminaryDeclarationVisitor;
import com.huawei.cangjie.types.util.TypeUtils;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static com.huawei.cangjie.descriptors.Errors.SUPERTYPE_NOT_INITIALIZED;
import static com.huawei.cangjie.resolve.descriptorUtil.DescriptorUtilsKt.isEffectivelyExternal;
import static com.huawei.cangjie.types.util.TypeUtils.NO_EXPECTED_TYPE;

public class BodyResolver {
    @NotNull
    private final ObservableBindingTrace trace;
    @NotNull
    private final OverloadChecker overloadChecker;
    @NotNull
    private final BodyResolveCache bodyResolveCache;
    @NotNull
    private final ExpressionTypingServices expressionTypingServices;
    @NotNull
    private final CangJieBuiltIns builtIns;
    @NotNull
    private final LanguageVersionSettings languageVersionSettings;
    @NotNull
    private final DeclarationsChecker declarationsChecker;

    public BodyResolver(
            @NotNull Project project,
//            @NotNull AnnotationResolver annotationResolver,
            @NotNull BodyResolveCache bodyResolveCache,
            @NotNull CallResolver callResolver,
//            @NotNull ControlFlowAnalyzer controlFlowAnalyzer,
            @NotNull DeclarationsChecker declarationsChecker,
//            @NotNull DelegatedPropertyResolver delegatedPropertyResolver,
            @NotNull ExpressionTypingServices expressionTypingServices,
//            @NotNull AnalyzerExtensions analyzerExtensions,
            @NotNull BindingTrace trace,
//            @NotNull ValueParameterResolver valueParameterResolver,
//            @NotNull AnnotationChecker annotationChecker,
            @NotNull CangJieBuiltIns builtIns,
            @NotNull OverloadChecker overloadChecker,
            @NotNull LanguageVersionSettings languageVersionSettings
    ) {
        this.bodyResolveCache = bodyResolveCache;
        this.trace = new ObservableBindingTrace(trace);
        this.overloadChecker = overloadChecker;
        this.expressionTypingServices = expressionTypingServices;
        this.declarationsChecker = declarationsChecker;

        this.builtIns = builtIns;
        this.languageVersionSettings = languageVersionSettings;
    }

    @NotNull
    private static LexicalScope getScopeForVariable(@NotNull BodiesResolveContext c, @NotNull CjVariable variable) {
        return getScopeForDeclaration(c, variable);
    }

    @NotNull
    private static LexicalScope getScopeForDeclaration(@NotNull BodiesResolveContext c, @NotNull CjDeclaration declaration) {
        LexicalScope scope = c.getDeclaringScope(declaration);
        assert scope != null : "Scope for property " + declaration.getText() + " should exists";
        return scope;
    }

    @NotNull
    private static LexicalScope getScopeForProperty(@NotNull BodiesResolveContext c, @NotNull CjProperty property) {
        return getScopeForDeclaration(c, property);
    }

    public static void computeDeferredType(CangJieType type) {
        // handle type inference loop: function or property body contains a reference to itself
        // fun f() = { f() }
        // val x = x
        // type resolution must be started before body resolution
        if (type instanceof DeferredType deferredType) {
            if (!deferredType.isComputed()) {
                deferredType.getDelegate();
            }
        }
    }

    private void resolvePropertyDeclarationBodies(@NotNull BodiesResolveContext c) {

        // Member   veraible
        Set<CjProperty> processed = new HashSet<>();
        for (Map.Entry<CjTypeStatement, ClassDescriptorWithResolutionScopes> entry : c.getDeclaredClasses().entrySet()) {
            if (!(entry.getKey() instanceof CjClass cjClass)) continue;
            ClassDescriptorWithResolutionScopes classDescriptor = entry.getValue();

            for (CjProperty property : cjClass.getProperties()) {
                PropertyDescriptor propertyDescriptor = c.getProperties().get(property);
                assert propertyDescriptor != null;

                resolveProperty(c, property, propertyDescriptor);
                processed.add(property);
            }
        }

//        // Top-level properties & properties of objects
//        for (Map.Entry<CjVariable, VariableDescriptor> entry : c.getVariables().entrySet()) {
//            CjVariable variable = entry.getKey();
//            if (processed.contains(variable)) continue;
//
//            VariableDescriptor variableDescriptor = entry.getValue();
//
//            resolveVariable(c, variable, variableDescriptor);
//        }
    }

    private void resolveVariableDeclarationBodies(@NotNull BodiesResolveContext c) {

        // Member   veraible
        Set<CjVariable> processed = new HashSet<>();
        for (Map.Entry<CjTypeStatement, ClassDescriptorWithResolutionScopes> entry : c.getDeclaredClasses().entrySet()) {
            if (!(entry.getKey() instanceof CjClass cjClass)) continue;
            ClassDescriptorWithResolutionScopes classDescriptor = entry.getValue();

            for (CjVariable variable : cjClass.getVariables()) {
                VariableDescriptor variableDescriptor = c.getVariables().get(variable);
                assert variableDescriptor != null;

                resolveVariable(c, variable, variableDescriptor);
                processed.add(variable);
            }
        }

        // Top-level properties & properties of objects
        for (Map.Entry<CjVariable, VariableDescriptor> entry : c.getVariables().entrySet()) {
            CjVariable variable = entry.getKey();
            if (processed.contains(variable)) continue;

            VariableDescriptor variableDescriptor = entry.getValue();

            resolveVariable(c, variable, variableDescriptor);
        }
    }

    private void resolveVariableInitializer(
            @NotNull DataFlowInfo outerDataFlowInfo,
            @NotNull CjVariable variable,
            @NotNull VariableDescriptor variableDescriptor,
            @NotNull CjExpression initializer,
            @NotNull LexicalScope propertyHeader,
            @Nullable InferenceSession inferenceSession
    ) {
        LexicalScope propertyDeclarationInnerScope = ScopeUtils.makeScopeForVariableInitializer(propertyHeader, variableDescriptor);
        CangJieType expectedTypeForInitializer = variable.getTypeReference() != null ? variableDescriptor.getType() : NO_EXPECTED_TYPE;
        if (variableDescriptor.getCompileTimeInitializer() == null) {
            expressionTypingServices.getType(
                    propertyDeclarationInnerScope, initializer, expectedTypeForInitializer,
                    outerDataFlowInfo, inferenceSession != null ? inferenceSession : InferenceSession.Companion.getDefault(), trace
            );
        }
    }

    private void resolvePropertyInitializer(
            @NotNull DataFlowInfo outerDataFlowInfo,
            @NotNull CjProperty property,
            @NotNull PropertyDescriptor propertyDescriptor,
            @NotNull CjExpression initializer,
            @NotNull LexicalScope propertyHeader,
            @Nullable InferenceSession inferenceSession
    ) {
        LexicalScope propertyDeclarationInnerScope = ScopeUtils.makeScopeForPropertyInitializer(propertyHeader, propertyDescriptor);
        CangJieType expectedTypeForInitializer = property.getTypeReference() != null ? propertyDescriptor.getType() : NO_EXPECTED_TYPE;
        if (propertyDescriptor.getCompileTimeInitializer() == null) {
            expressionTypingServices.getType(
                    propertyDeclarationInnerScope, initializer, expectedTypeForInitializer,
                    outerDataFlowInfo, inferenceSession != null ? inferenceSession : InferenceSession.Companion.getDefault(), trace
            );
        }
    }

    private void resolveProperty(BodiesResolveContext c, CjProperty property, PropertyDescriptor propertyDescriptor) {
        computeDeferredType(propertyDescriptor.getReturnType());
        PreliminaryDeclarationVisitor.Companion.createForDeclaration(property, trace, languageVersionSettings);
        CjExpression initializer = property.getInitializer();
        LexicalScope variableHeaderScope = ScopeUtils.makeScopeForPropertyHeader(getScopeForProperty(c, property), propertyDescriptor);
        ExpressionTypingContext context = c.getLocalContext();

        if (initializer != null) {
            resolvePropertyInitializer(
                    c.getOuterDataFlowInfo(), property, propertyDescriptor,
                    initializer, variableHeaderScope, context != null ? context.inferenceSession : null
            );
        }
//委托 没有
//        CjExpression delegateExpression = variable.getDelegateExpression();
//        if (delegateExpression != null) {
//            assert initializer == null : "Initializer should be null for delegated property : " + variable.getText();
//            resolveVariableDelegate(
//                    c.getOuterDataFlowInfo(), variable, variableDescriptor,
//                    delegateExpression, variableHeaderScope, context != null ? context.inferenceSession : null
//            );
//        }

//注解 宏？
//        ForceResolveUtil.forceResolveAllContents(propertyDescriptor.getAnnotations());

//元数据 没有
//        FieldDescriptor backingField = variableDescriptor.getBackingField();
//        if (backingField != null) {
//            ForceResolveUtil.forceResolveAllContents(backingField.getAnnotations());
//        }
    }

    private void resolveVariable(BodiesResolveContext c, CjVariable variable, VariableDescriptor variableDescriptor) {
        computeDeferredType(variableDescriptor.getReturnType());
        PreliminaryDeclarationVisitor.Companion.createForDeclaration(variable, trace, languageVersionSettings);
        CjExpression initializer = variable.getInitializer();
        LexicalScope variableHeaderScope = ScopeUtils.makeScopeForVariableHeader(getScopeForVariable(c, variable), variableDescriptor);
        ExpressionTypingContext context = c.getLocalContext();

        if (initializer != null) {
            resolveVariableInitializer(
                    c.getOuterDataFlowInfo(), variable, variableDescriptor,
                    initializer, variableHeaderScope, context != null ? context.inferenceSession : null
            );
        }
//委托 没有
//        CjExpression delegateExpression = variable.getDelegateExpression();
//        if (delegateExpression != null) {
//            assert initializer == null : "Initializer should be null for delegated property : " + variable.getText();
//            resolveVariableDelegate(
//                    c.getOuterDataFlowInfo(), variable, variableDescriptor,
//                    delegateExpression, variableHeaderScope, context != null ? context.inferenceSession : null
//            );
//        }

//注解 宏？
//        ForceResolveUtil.forceResolveAllContents(variableDescriptor.getAnnotations());

//元数据 没有
//        FieldDescriptor backingField = variableDescriptor.getBackingField();
//        if (backingField != null) {
//            ForceResolveUtil.forceResolveAllContents(backingField.getAnnotations());
//        }
    }

    private void resolveBehaviorDeclarationBodies(@NotNull BodiesResolveContext c) {
        resolveVariableDeclarationBodies(c);


        resolveFunctionBodies(c);


    }

    private void resolveFunctionBodies(BodiesResolveContext c) {

        for (Map.Entry<CjNamedFunction, SimpleFunctionDescriptor> entry : c.getFunctions().entrySet()) {
            CjNamedFunction declaration = entry.getKey();

            LexicalScope scope = c.getDeclaringScope(declaration);
            assert scope != null : "Scope is null: " + PsiUtilsKt.getElementTextWithContext(declaration);

            if (!c.getTopDownAnalysisMode().isLocalDeclarations() && !(bodyResolveCache instanceof BodyResolveCache.ThrowException) &&
                    expressionTypingServices.getStatementFilter() != StatementFilter.NONE) {
                bodyResolveCache.resolveFunctionBody(declaration).addOwnDataTo(trace, true);
            } else {
                resolveFunctionBody(c.getOuterDataFlowInfo(), trace, declaration, entry.getValue(), scope, c.getLocalContext());
            }
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
        assert functionDescriptor.getReturnType() != null;
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

        PreliminaryDeclarationVisitor.Companion.createForDeclaration(function, trace, languageVersionSettings);
        LexicalScope innerScope = FunctionDescriptorUtil.getFunctionInnerScope(scope, functionDescriptor, trace, overloadChecker);
        List<CjParameter> valueParameters = function.getValueParameters();
        List<ValueParameterDescriptor> valueParameterDescriptors = functionDescriptor.getValueParameters();
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
        assert functionDescriptor.getReturnType() != null;
    }

    private void checkRedeclarationsInClassHeaderWithoutPrimaryConstructor(
            @NotNull final ClassDescriptor descriptor, @NotNull LexicalScope scopeForConstructorResolution
    ) {
        // Initializing a scope will report errors if any.
        new LexicalScopeImpl(
                scopeForConstructorResolution, descriptor, true, null, Collections.emptyList(), LexicalScopeKind.CLASS_HEADER,
                new TraceBasedLocalRedeclarationChecker(trace, overloadChecker),
                new Function1<LexicalScopeImpl.InitializeHandler, Unit>() {
                    @Override
                    public Unit invoke(LexicalScopeImpl.InitializeHandler handler) {
                        // If a class has no primary constructor, it still can have type parameters declared in header.
                        for (TypeParameterDescriptor typeParameter : descriptor.getDeclaredTypeParameters()) {
                            handler.addClassifierDescriptor(typeParameter);
                        }
                        return Unit.INSTANCE;
                    }
                });
    }

    public void resolveSuperTypeEntryList(
            @NotNull DataFlowInfo outerDataFlowInfo,
            @NotNull CjTypeStatement cjClass,
            @NotNull ClassDescriptor descriptor,
            @Nullable ConstructorDescriptor primaryConstructor,
            @NotNull LexicalScope scopeForConstructorResolution,
            @NotNull LexicalScope scopeForMemberResolution,
            @Nullable InferenceSession inferenceSession
    ) {
        ProgressManager.checkCanceled();
        LexicalScope scopeForConstructor =
                primaryConstructor == null
                        ? null
                        : FunctionDescriptorUtil.getFunctionInnerScope(scopeForConstructorResolution, primaryConstructor, trace, overloadChecker);
        if (primaryConstructor == null) {
            checkRedeclarationsInClassHeaderWithoutPrimaryConstructor(descriptor, scopeForConstructorResolution);
        }
        ExpressionTypingServices typeInferrer = expressionTypingServices; // TODO : flow

        Map<CjTypeReference, CangJieType> supertypes = Maps.newLinkedHashMap();
        ResolvedCall<?>[] primaryConstructorDelegationCall = new ResolvedCall[1];

        CjVisitorVoid visitor = new CjVisitorVoid() {
            private void recordSupertype(CjTypeReference typeReference, CangJieType supertype) {
                if (supertype == null) return;
                supertypes.put(typeReference, supertype);
            }

//            @Override
//            public void visitDelegatedSuperTypeEntry(@NotNull CjDelegatedSuperTypeEntry specifier) {
//                if (descriptor.getKind() == ClassKind.INTERFACE) {
//                    trace.report(DELEGATION_IN_INTERFACE.on(specifier));
//                }
//                CangJieType supertype = trace.getBindingContext().get(BindingContext.TYPE, specifier.getTypeReference());
//                recordSupertype(specifier.getTypeReference(), supertype);
//                if (supertype != null) {
//                    DeclarationDescriptor declarationDescriptor = supertype.getConstructor().getDeclarationDescriptor();
//                    if (declarationDescriptor instanceof ClassDescriptor) {
//                        ClassDescriptor classDescriptor = (ClassDescriptor) declarationDescriptor;
//                        if (classDescriptor.getKind() != ClassKind.INTERFACE) {
//                            trace.report(DELEGATION_NOT_TO_INTERFACE.on(specifier.getTypeReference()));
//                        }
//                    }
//                }
//                CjExpression delegateExpression = specifier.getDelegateExpression();
//                if (delegateExpression != null) {
//                    LexicalScope scope = scopeForConstructor == null ? scopeForMemberResolution : scopeForConstructor;
//                    CangJieType expectedType = supertype != null ? supertype : NO_EXPECTED_TYPE;
//                    typeInferrer.getType(
//                            scope, delegateExpression, expectedType, outerDataFlowInfo,
//                            inferenceSession != null ? inferenceSession : InferenceSession.Companion.getDefault(), trace
//                    );
//                }
//
//                if (descriptor.isExpect()) {
//                    trace.report(IMPLEMENTATION_BY_DELEGATION_IN_EXPECT_CLASS.on(specifier));
//                }
//                else if (primaryConstructor == null) {
//                    trace.report(UNSUPPORTED.on(specifier, "Delegation without primary constructor is not supported"));
//                }
//            }

//            @Override
//            public void visitSuperTypeCallEntry(@NotNull KtSuperTypeCallEntry call) {
//                KtValueArgumentList valueArgumentList = call.getValueArgumentList();
//                PsiElement elementToMark = valueArgumentList == null ? call : valueArgumentList;
//                if (descriptor.getKind() == ClassKind.INTERFACE) {
//                    trace.report(SUPERTYPE_INITIALIZED_IN_INTERFACE.on(elementToMark));
//                }
//                if (descriptor.isExpect()) {
//                    trace.report(SUPERTYPE_INITIALIZED_IN_EXPECTED_CLASS.on(elementToMark));
//                }
//                KtTypeReference typeReference = call.getTypeReference();
//                if (typeReference == null) return;
//                if (primaryConstructor == null) {
//                    if (descriptor.getKind() != ClassKind.INTERFACE) {
//                        trace.report(SUPERTYPE_INITIALIZED_WITHOUT_PRIMARY_CONSTRUCTOR.on(call));
//                    }
//                    recordSupertype(typeReference, trace.getBindingContext().get(BindingContext.TYPE, typeReference));
//                    return;
//                }
//                OverloadResolutionResults<FunctionDescriptor> results = callResolver.resolveFunctionCall(
//                        trace, scopeForConstructor, CallMaker.makeConstructorCallWithoutTypeArguments(call),
//                        NO_EXPECTED_TYPE, outerDataFlowInfo, false, inferenceSession
//                );
//                if (results.isSingleResult()) {
//                    CangJieType supertype = results.getResultingDescriptor().getReturnType();
//                    recordSupertype(typeReference, supertype);
//                    ClassDescriptor classDescriptor = TypeUtils.getClassDescriptor(supertype);
//                    if (classDescriptor != null) {
//                        // allow only one delegating constructor
//                        if (primaryConstructorDelegationCall[0] == null) {
//                            primaryConstructorDelegationCall[0] = results.getResultingCall();
//                        }
//                        else {
//                            primaryConstructorDelegationCall[0] = null;
//                        }
//                    }
//                    // Recording type info for callee to use later in JetObjectLiteralExpression
//                    trace.record(PROCESSED, call.getCalleeExpression(), true);
//                    trace.record(EXPRESSION_TYPE_INFO, call.getCalleeExpression(),
//                            TypeInfoFactoryKt.noTypeInfo(results.getResultingCall().getDataFlowInfoForArguments().getResultInfo()));
//                }
//                else {
//                    recordSupertype(typeReference, trace.getBindingContext().get(BindingContext.TYPE, typeReference));
//                }
//            }

            @Override
            public void visitSuperTypeEntry(@NotNull CjSuperTypeEntry specifier) {
                CjTypeReference typeReference = specifier.getTypeReference();
                CangJieType supertype = trace.getBindingContext().get(BindingContext.TYPE, typeReference);
                recordSupertype(typeReference, supertype);
                if (supertype == null) return;
                ClassDescriptor superClass = TypeUtils.getClassDescriptor(supertype);
                if (superClass == null) return;
                if (superClass.getKind().isSingleton()) {
                    // A "singleton in supertype" diagnostic will be reported later
                    return;
                }
                if (descriptor.getKind() != ClassKind.INTERFACE &&
                        descriptor.getUnsubstitutedPrimaryConstructor() != null &&
                        superClass.getKind() != ClassKind.INTERFACE &&
                        !descriptor.isExpect() && !isEffectivelyExternal(descriptor) &&
                        !ErrorUtils.isError(superClass)
                ) {
                    trace.report(SUPERTYPE_NOT_INITIALIZED.on(specifier));
                }
            }

            @Override
            public void visitCjElement(@NotNull CjElement element) {
                throw new UnsupportedOperationException(element.getText() + " : " + element);
            }
        };

    }

    public void resolveBodies(@NotNull BodiesResolveContext c) {
        resolveBehaviorDeclarationBodies(c);
//        controlFlowAnalyzer.process(c);
        declarationsChecker.process(c);
//        analyzerExtensions.process(c);
    }
}
