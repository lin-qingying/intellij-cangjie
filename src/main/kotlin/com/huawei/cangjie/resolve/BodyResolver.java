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
import com.huawei.cangjie.resolve.calls.results.OverloadResolutionResults;
import com.huawei.cangjie.resolve.calls.smartcasts.DataFlowInfo;
import com.huawei.cangjie.resolve.lazy.ForceResolveUtil;
import com.huawei.cangjie.resolve.lazy.descriptors.LazyExtendClassDescriptor;
import com.huawei.cangjie.resolve.scopes.*;
import com.huawei.cangjie.types.*;
import com.huawei.cangjie.types.expressions.ExpressionTypingContext;
import com.huawei.cangjie.types.expressions.ExpressionTypingServices;
import com.huawei.cangjie.types.expressions.PreliminaryDeclarationVisitor;
import com.huawei.cangjie.types.expressions.ValueParameterResolver;
import com.huawei.cangjie.types.util.TypeUtils;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static com.huawei.cangjie.descriptors.Errors.*;
import static com.huawei.cangjie.resolve.BindingContext.CONSTRUCTOR_RESOLVED_DELEGATION_CALL;
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
    @NotNull private final ValueParameterResolver valueParameterResolver;
    @NotNull private final CallResolver callResolver;

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
            @NotNull ValueParameterResolver valueParameterResolver,
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
        this.valueParameterResolver = valueParameterResolver;
        this.callResolver = callResolver;

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

    @SuppressWarnings("unchecked")
    private static void recordConstructorDelegationCall(
            @NotNull BindingTrace trace,
            @NotNull ConstructorDescriptor constructor,
            @NotNull ResolvedCall<?> call
    ) {
        trace.record(CONSTRUCTOR_RESOLVED_DELEGATION_CALL, constructor, (ResolvedCall<ConstructorDescriptor>) call);
    }

    // Returns a set of enum or sealed types of which supertypeOwner is an entry or a member
    @NotNull
    private Set<TypeConstructor> getAllowedFinalSupertypes(
            @NotNull ClassDescriptor descriptor,
            @NotNull Map<CjTypeReference, CangJieType> supertypes,
            @NotNull CjTypeStatement typeStatement
    ) {
        Set<TypeConstructor> parentEnumOrSealed = Collections.emptySet();
//        if (typeStatement instanceof CjEnumEntry) {
//            parentEnumOrSealed = Collections.singleton(((ClassDescriptor) descriptor.getContainingDeclaration()).getTypeConstructor());
//        }
//        else if (languageVersionSettings.supportsFeature(TopLevelSealedInheritance) && DescriptorUtils.isTopLevelDeclaration(descriptor)) {
//            // TODO: improve diagnostic when top level sealed inheritance is disabled
//            for (KotlinType supertype : supertypes.values()) {
//                ClassifierDescriptor classifierDescriptor = supertype.getConstructor().getDeclarationDescriptor();
//                if (DescriptorUtils.isSealedClass(classifierDescriptor) && DescriptorUtils.isTopLevelDeclaration(classifierDescriptor)) {
//                    parentEnumOrSealed = Collections.singleton(classifierDescriptor.getTypeConstructor());
//                }
//            }
//        }
//        else {
//            ClassDescriptor currentDescriptor = descriptor;
//            while (currentDescriptor.getContainingDeclaration() instanceof ClassDescriptor) {
//                currentDescriptor = (ClassDescriptor) currentDescriptor.getContainingDeclaration();
//                if (DescriptorUtils.isSealedClass(currentDescriptor)) {
//                    if (parentEnumOrSealed.isEmpty()) {
//                        parentEnumOrSealed = new HashSet<>();
//                    }
//                    parentEnumOrSealed.add(currentDescriptor.getTypeConstructor());
//                    if (currentDescriptor.isExpect()) {
//                        List<MemberDescriptor> actualDescriptors = ExpectedActualResolverKt.findCompatibleActualsForExpected(
//                                currentDescriptor, DescriptorUtilsKt.getModule( currentDescriptor)
//                        );
//                        for (MemberDescriptor actualDescriptor: actualDescriptors) {
//                            if (actualDescriptor instanceof TypeAliasDescriptor) {
//                                parentEnumOrSealed.add(((TypeAliasDescriptor) actualDescriptor).getExpandedType().getConstructor());
//                            }
//                        }
//                    }
//                }
//            }
//        }
        return parentEnumOrSealed;
    }

    private void checkSupertypeList(
            @NotNull ClassDescriptor supertypeOwner,
            @NotNull Map<CjTypeReference, CangJieType> supertypes,
            @NotNull CjTypeStatement typeStatement
    ) {
        Set<TypeConstructor> allowedFinalSupertypes = getAllowedFinalSupertypes(supertypeOwner, supertypes, typeStatement);
        Set<TypeConstructor> typeConstructors = new HashSet<>();
        boolean classAppeared = false;
        for (Map.Entry<CjTypeReference, CangJieType> entry : supertypes.entrySet()) {
            CjTypeReference typeReference = entry.getKey();
            CangJieType supertype = entry.getValue();

            CjTypeElement typeElement = typeReference.getTypeElement();
            if (typeElement instanceof CjFunctionType) {
                for (CjParameter parameter : ((CjFunctionType) typeElement).getParameters()) {
                    PsiElement nameIdentifier = parameter.getNameIdentifier();

                    if (nameIdentifier != null) {
                        trace.report(Errors.UNSUPPORTED.on(nameIdentifier, "named parameter in function type in supertype position"));
                    }
                }
            }

            boolean addSupertype = true;

            ClassDescriptor classDescriptor = TypeUtils.getClassDescriptor(supertype);
            if (classDescriptor != null) {
                if (ErrorUtils.isError(classDescriptor)) continue;

//                if (FunctionTypesKt.isExtensionFunctionType(supertype) &&
//                        !languageVersionSettings.supportsFeature(LanguageFeature.FunctionalTypeWithExtensionAsSupertype)
//                ) {
//                    trace.report(SUPERTYPE_IS_EXTENSION_FUNCTION_TYPE.on(typeReference));
//                }
//                else if (FunctionTypesKt.isSuspendExtensionFunctionType(supertype) &&
//                        !languageVersionSettings.supportsFeature(LanguageFeature.FunctionalTypeWithExtensionAsSupertype) &&
//                        languageVersionSettings.supportsFeature(LanguageFeature.SuspendFunctionAsSupertype)) {
//                    trace.report(SUPERTYPE_IS_SUSPEND_EXTENSION_FUNCTION_TYPE.on(typeReference));
//                }
//                else if (FunctionTypesKt.isSuspendFunctionType(supertype) &&
//                        !languageVersionSettings.supportsFeature(LanguageFeature.SuspendFunctionAsSupertype)
//                ) {
//                    trace.report(SUPERTYPE_IS_SUSPEND_FUNCTION_TYPE.on(typeReference));
//                }
//                else if (FunctionTypesKt.isKSuspendFunctionType(supertype) &&
//                        !languageVersionSettings.supportsFeature(LanguageFeature.SuspendFunctionAsSupertype)) {
//                    trace.report(SUPERTYPE_IS_KSUSPEND_FUNCTION_TYPE.on(typeReference));
//                }

                if (classDescriptor.getKind() != ClassKind.INTERFACE) {
                    if (supertypeOwner.getKind() == ClassKind.ENUM) {
                        trace.report(CLASS_IN_SUPERTYPE_FOR_ENUM.on(typeReference));
                        addSupertype = false;
                    } else if (supertypeOwner.getKind() == ClassKind.INTERFACE &&
                            !classAppeared && !DynamicTypesKt.isDynamic(supertype) /* avoid duplicate diagnostics */) {
                        trace.report(INTERFACE_WITH_SUPERCLASS.on(typeReference));
                        addSupertype = false;
                    } else if (supertypeOwner.getKind() == ClassKind.EXTEND &&
                            !hasExtendSourceMap.get(typeReference)       &&  !DynamicTypesKt.isDynamic(supertype) /* avoid duplicate diagnostics */) {
                        trace.report(EXTEND_WITH_SUPERCLASS.on(typeReference));
                        addSupertype = false;
                        return;
                    }


                    if (classAppeared && supertypeOwner.getKind() != ClassKind.EXTEND) {
                        trace.report(MANY_CLASSES_IN_SUPERTYPE_LIST.on(typeReference));
                    } else {
                        classAppeared = true;
                    }
                }
            } else {
                trace.report(SUPERTYPE_NOT_A_CLASS_OR_INTERFACE.on(typeReference));
            }

            TypeConstructor constructor = supertype.getConstructor();
            if (addSupertype && !typeConstructors.add(constructor)) {
                trace.report(SUPERTYPE_APPEARS_TWICE.on(typeReference));
            }

            if (classDescriptor == null) return;
            if (classDescriptor.getKind().isEnum()) {
                if (!DescriptorUtils.isEnumEntry(classDescriptor)) {
                    trace.report(ENUM_IN_SUPERTYPE.on(typeReference));
                }
            } else if (classDescriptor.getKind().isStruct()) {
                if (!DescriptorUtils.isEnumEntry(classDescriptor)) {
                    trace.report(STRUCT_IN_SUPERTYPE.on(typeReference));
                }
            } else if (!allowedFinalSupertypes.contains(constructor)) {
                if (DescriptorUtils.isSealedClass(classDescriptor)) {
                    DeclarationDescriptor containingDescriptor = supertypeOwner.getContainingDeclaration();
                    while (containingDescriptor != null && containingDescriptor != classDescriptor) {
                        containingDescriptor = containingDescriptor.getContainingDeclaration();
                    }
//                    if (containingDescriptor == null) {
//                        if (
//                                !languageVersionSettings.supportsFeature(AllowSealedInheritorsInDifferentFilesOfSamePackage) ||
//                                        DescriptorUtils.isLocal(supertypeOwner)
//                        ) {
//                            trace.report(SEALED_SUPERTYPE.on(typeReference));
//                        }
//                    }
//                    else {
//                        String declarationName;
//                        if (supertypeOwner.getName() == SpecialNames.NO_NAME_PROVIDED) {
//                            declarationName = "Anonymous object";
//                        } else {
//                            declarationName = "Local class";
//                        }
//                        trace.report(SEALED_SUPERTYPE_IN_LOCAL_CLASS.on(typeReference, declarationName, classDescriptor.getKind()));
//                    }
                } else if (ModalityUtilsKt.isFinalOrEnum(classDescriptor)) {
                    trace.report(FINAL_SUPERTYPE.on(typeReference, classDescriptor.getDefaultType()));
                }
//                else if (CangJieBuiltIns.isEnum(classDescriptor)) {
//                    trace.report(CLASS_CANNOT_BE_EXTENDED_DIRECTLY.on(typeReference, classDescriptor));
//                }
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

    @Nullable
    private DataFlowInfo resolveSecondaryConstructorDelegationCall(
            @NotNull DataFlowInfo outerDataFlowInfo,
            @NotNull BindingTrace trace,
            @NotNull LexicalScope scope,
            @NotNull CjSecondaryConstructor constructor,
            @NotNull ClassConstructorDescriptor descriptor,
            @Nullable InferenceSession inferenceSession
    ) {
        if (descriptor.isExpect() || isEffectivelyExternal(descriptor)) {
            // For expected and external classes, we do not resolve constructor delegation calls because they are prohibited
            return DataFlowInfo.Companion.getEMPTY();
        }

        OverloadResolutionResults<?> results = callResolver.resolveConstructorDelegationCall(
                trace, scope, outerDataFlowInfo,
                descriptor, constructor.getDelegationCall(), inferenceSession);

        if (results != null && results.isSingleResult()) {
            ResolvedCall<? extends CallableDescriptor> resolvedCall = results.getResultingCall();
            recordConstructorDelegationCall(trace, descriptor, resolvedCall);
            return resolvedCall.getDataFlowInfoForArguments().getResultInfo();
        }
        return null;
    }
    public void resolveSecondaryConstructorBody(
            @NotNull DataFlowInfo outerDataFlowInfo,
            @NotNull BindingTrace trace,
            @NotNull CjSecondaryConstructor constructor,
            @NotNull ClassConstructorDescriptor descriptor,
            @NotNull LexicalScope declaringScope,
            @Nullable ExpressionTypingContext localContext
    ) {
        ForceResolveUtil.forceResolveAllContents(descriptor.getAnnotations());

        resolveFunctionBody(
                outerDataFlowInfo, trace, constructor, descriptor, declaringScope,
                headerInnerScope -> resolveSecondaryConstructorDelegationCall(
                        outerDataFlowInfo, trace, headerInnerScope, constructor,
                        descriptor, localContext != null ? localContext.inferenceSession : null
                ),
                scope -> new LexicalScopeImpl(
                        scope, descriptor, scope.isOwnerDescriptorAccessibleByLabel(), scope.getImplicitReceiver(), scope.getContextReceiversGroup(),
                        LexicalScopeKind.CONSTRUCTOR_HEADER
                ),
                localContext
        );
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

    private void resolveSuperTypeEntryLists(@NotNull BodiesResolveContext c) {
        // TODO : Make sure the same thing is not initialized twice
        for (Map.Entry<CjTypeStatement, ClassDescriptorWithResolutionScopes> entry : c.getDeclaredClasses().entrySet()) {
            CjTypeStatement typeStatement = entry.getKey();
            ClassDescriptorWithResolutionScopes descriptor = entry.getValue();
            ExpressionTypingContext localContext = c.getLocalContext();

            resolveSuperTypeEntryList(c.getOuterDataFlowInfo(), typeStatement, descriptor,
                    descriptor.getUnsubstitutedPrimaryConstructor(),
                    descriptor.getScopeForConstructorHeaderResolution(),
                    descriptor.getScopeForMemberDeclarationResolution(),
                    localContext != null ? localContext.inferenceSession : null);
        }
    }
    private static LexicalScope getPrimaryConstructorParametersScope(
            LexicalScope originalScope,
            ConstructorDescriptor unsubstitutedPrimaryConstructor
    ) {
        return new LexicalScopeImpl(originalScope, unsubstitutedPrimaryConstructor, false, null,
                Collections.emptyList(), LexicalScopeKind.DEFAULT_VALUE, LocalRedeclarationChecker.DO_NOTHING.INSTANCE,
                handler -> {
                    for (ValueParameterDescriptor valueParameter : unsubstitutedPrimaryConstructor.getValueParameters()) {
                        handler.addVariableDescriptor(valueParameter);
                    }
                    return Unit.INSTANCE;
                });
    }
    public void resolveConstructorParameterDefaultValues(
            @NotNull DataFlowInfo outerDataFlowInfo,
            @NotNull BindingTrace trace,
            @NotNull CjPrimaryConstructor constructor,
            @NotNull ConstructorDescriptor constructorDescriptor,
            @NotNull LexicalScope declaringScope,
            @Nullable InferenceSession inferenceSession
    ) {
        List<CjParameter> valueParameters = constructor.getValueParameters();
        List<ValueParameterDescriptor> valueParameterDescriptors = constructorDescriptor.getValueParameters();

        LexicalScope scope = getPrimaryConstructorParametersScope(declaringScope, constructorDescriptor);

        valueParameterResolver.resolveValueParameters(valueParameters, valueParameterDescriptors, scope, outerDataFlowInfo, trace, inferenceSession);
    }
    private void resolveBehaviorDeclarationBodies(@NotNull BodiesResolveContext c) {
        resolveSuperTypeEntryLists(c);

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
    boolean hasExtendSource = false;

//解决扩展的原类污染报错
    Map<CjTypeReference,Boolean> hasExtendSourceMap = Maps.newHashMap();
    public void resolveSuperTypeEntryList(
            @NotNull DataFlowInfo outerDataFlowInfo,
            @NotNull CjTypeStatement typeStatement,
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


                hasExtendSourceMap.put(typeReference,hasExtendSource);
                supertypes.put(typeReference, supertype);
            }

            @Override
            public void visitSuperTypeEntry(@NotNull CjSuperTypeEntry specifier) {
                CjTypeReference typeReference = specifier.getTypeReference();
                CangJieType supertype = trace.getBindingContext().get(BindingContext.TYPE, typeReference);
                recordSupertype(typeReference, supertype);
                if (supertype == null) return;
                ClassDescriptor superClass = TypeUtils.getClassDescriptor(supertype);
                if (superClass == null) return;
                if (superClass.getKind().isStruct()) {
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


        //   TODO      如果是扩展，将源类型加上，但是这里还缺少其他扩展
        if (typeStatement instanceof CjExtend && descriptor instanceof LazyExtendClassDescriptor) {

            CjTypeStatement sourceClassElement = ((LazyExtendClassDescriptor) descriptor).getSourceClassElement();

            if (sourceClassElement != null) {
                hasExtendSource = true;
                for (CjSuperTypeListEntry delegationSpecifier : sourceClassElement.getSuperTypeListEntries()) {
                    ProgressManager.checkCanceled();

                    delegationSpecifier.accept(visitor);
                }
                hasExtendSource = false;

            }
        }


        for (CjSuperTypeListEntry delegationSpecifier : typeStatement.getSuperTypeListEntries()) {
            ProgressManager.checkCanceled();

            delegationSpecifier.accept(visitor);
        }


        if (primaryConstructorDelegationCall[0] != null && primaryConstructor != null) {
            recordConstructorDelegationCall(trace, primaryConstructor, primaryConstructorDelegationCall[0]);
        }


        checkSupertypeList(descriptor, supertypes, typeStatement);

    }

    public void resolveBodies(@NotNull BodiesResolveContext c) {
        resolveBehaviorDeclarationBodies(c);
//        controlFlowAnalyzer.process(c);
        declarationsChecker.process(c);
//        analyzerExtensions.process(c);
    }
}
