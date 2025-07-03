/*
 * Copyright 2024 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package cn.cangnova.cangjie.resolve;

import com.google.common.collect.Maps;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import cn.cangnova.cangjie.builtins.CangJieBuiltIns;
import cn.cangnova.cangjie.config.LanguageVersionSettings;
import cn.cangnova.cangjie.descriptors.*;
import cn.cangnova.cangjie.descriptors.impl.PropertyAccessorDescriptor;
import cn.cangnova.cangjie.descriptors.impl.SyntheticFieldDescriptor;
import cn.cangnova.cangjie.descriptors.macro.MacroDescriptor;
import cn.cangnova.cangjie.diagnostics.Errors;
import cn.cangnova.cangjie.psi.*;
import cn.cangnova.cangjie.psi.psiUtil.PsiUtilsKt;
import cn.cangnova.cangjie.resolve.calls.CallResolver;
import cn.cangnova.cangjie.resolve.calls.components.InferenceSession;
import cn.cangnova.cangjie.resolve.calls.model.ResolvedCall;
import cn.cangnova.cangjie.resolve.calls.results.OverloadResolutionResults;
import cn.cangnova.cangjie.resolve.calls.smartcasts.DataFlowInfo;
import cn.cangnova.cangjie.resolve.lazy.ForceResolveUtil;
import cn.cangnova.cangjie.resolve.lazy.descriptors.LazyExtendClassDescriptor;
import cn.cangnova.cangjie.resolve.scopes.*;
import cn.cangnova.cangjie.resolve.source.CangJieSourceElementKt;
import cn.cangnova.cangjie.resolve.source.PsiSourceElementKt;
import cn.cangnova.cangjie.types.*;
import cn.cangnova.cangjie.types.expressions.ExpressionTypingContext;
import cn.cangnova.cangjie.types.expressions.ExpressionTypingServices;
import cn.cangnova.cangjie.types.expressions.PreliminaryDeclarationVisitor;
import cn.cangnova.cangjie.types.expressions.ValueParameterResolver;
import cn.cangnova.cangjie.types.util.TypeUtils;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static cn.cangnova.cangjie.diagnostics.Errors.*;
import static cn.cangnova.cangjie.resolve.BindingContext.CONSTRUCTOR_RESOLVED_DELEGATION_CALL;
import static cn.cangnova.cangjie.resolve.descriptorUtil.DescriptorUtilsKt.isEffectivelyExternal;
import static cn.cangnova.cangjie.types.util.TypeUtils.NO_EXPECTED_TYPE;

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
    @NotNull
    private final ValueParameterResolver valueParameterResolver;
    @NotNull
    private final CallResolver callResolver;
    @NotNull
    private final ControlFlowAnalyzer controlFlowAnalyzer;
    boolean hasExtendSource = false;
    //解决扩展的原类污染报错
    Map<CjTypeReference, Boolean> hasExtendSourceMap = Maps.newHashMap();

    public BodyResolver(
            @NotNull Project project,
//            @NotNull AnnotationResolver annotationResolver,
            @NotNull BodyResolveCache bodyResolveCache,
            @NotNull CallResolver callResolver,
            @NotNull ControlFlowAnalyzer controlFlowAnalyzer,
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
        this.controlFlowAnalyzer = controlFlowAnalyzer;

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

    private static LexicalScope getPrimaryConstructorParametersScope(
            LexicalScope originalScope,
            ConstructorDescriptor unsubstitutedPrimaryConstructor
    ) {
        return new LexicalScopeImpl(originalScope, unsubstitutedPrimaryConstructor, false, null,
                Collections.emptyList(), LexicalScopeKind.DEFAULT_VALUE, LocalRedeclarationChecker.DO_NOTHING.INSTANCE,
                handler -> {
                    for (ValueParameterDescriptor valueParameter : unsubstitutedPrimaryConstructor.valueParameters) {
                        handler.addVariableDescriptor(valueParameter);
                    }
                    return Unit.INSTANCE;
                });
    }

    private static LexicalScope makeScopeForPropertyAccessor(
            @NotNull BodiesResolveContext c, @NotNull CjPropertyAccessor accessor, @NotNull PropertyDescriptor descriptor
    ) {
        LexicalScope accessorDeclaringScope = c.getDeclaringScope(accessor);
        assert accessorDeclaringScope != null : "Scope for accessor " + accessor.getText() + " should exists";
        LexicalScope headerScope = ScopeUtils.makeScopeForPropertyHeader(accessorDeclaringScope, descriptor);
        return new LexicalScopeImpl(headerScope, descriptor, true, descriptor.extensionReceiverParameter, descriptor.contextReceiverParameters,
                LexicalScopeKind.PROPERTY_ACCESSOR_BODY);
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
//            for (CangJieType supertype : supertypes.values()) {
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
            @NotNull CjTypeStatement typeStatement,
            @NotNull Set<CangJieType> sourceSuperClass
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

                if (classDescriptor.kind != ClassKind.INTERFACE) {
                    /*if (supertypeOwner.getKind() == ClassKind.ENUM) {
                        trace.report(CLASS_IN_SUPERTYPE_FOR_ENUM.on(typeReference));
                        addSupertype = false;
                    } else*/
                    if (supertypeOwner.kind == ClassKind.INTERFACE &&
                            !classAppeared && !DynamicTypesKt.isDynamic(supertype) /* avoid duplicate diagnostics */) {
                        trace.report(INTERFACE_WITH_SUPERCLASS.on(typeReference));
                        addSupertype = false;
                    } else if (supertypeOwner.kind == ClassKind.EXTEND &&
                            !hasExtendSourceMap.get(typeReference) && !DynamicTypesKt.isDynamic(supertype) /* avoid duplicate diagnostics */) {
                        trace.report(EXTEND_WITH_SUPERCLASS.on(typeReference));
                        addSupertype = false;
                        return;
                    } else if (supertypeOwner.kind == ClassKind.STRUCT &&
                            !classAppeared && !DynamicTypesKt.isDynamic(supertype) /* avoid duplicate diagnostics */) {
                        trace.report(STRUCT_WITH_SUPERCLASS.on(typeReference));
                        addSupertype = false;
                        return;
                    }


                    if (classAppeared && supertypeOwner.kind != ClassKind.EXTEND) {
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
//            验证原类超类型
            for (CangJieType _supertype : sourceSuperClass) {
                TypeConstructor _constructor = _supertype.getConstructor();
                if (addSupertype && !typeConstructors.add(_constructor) && _constructor == constructor) {
                    trace.report(SUPERTYPE_APPEARS_TWICE.on(typeReference));
                }
            }

            if (classDescriptor == null) return;
            if (classDescriptor.kind.isEnum()) {
                if (!DescriptorUtils.isEnumEntry(classDescriptor)) {
                    trace.report(ENUM_IN_SUPERTYPE.on(typeReference));
                }
            } else if (classDescriptor.kind.isObject()) {
                if (!DescriptorUtils.isEnumEntry(classDescriptor)) {
                    trace.report(STRUCT_IN_SUPERTYPE.on(typeReference));
                }
            } else if (!allowedFinalSupertypes.contains(constructor)) {
                if (DescriptorUtils.isSealedClass(classDescriptor)) {
                    DeclarationDescriptor containingDescriptor = supertypeOwner.containingDeclaration;
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
                    trace.report(FINAL_SUPERTYPE.on(typeReference, classDescriptor.defaultType));
                }
//                else if (CangJieBuiltIns.isEnum(classDescriptor)) {
//                    trace.report(CLASS_CANNOT_BE_EXTENDED_DIRECTLY.on(typeReference, classDescriptor));
//                }
            }
        }
    }

    private void resolveVariableDeclarationBodies(@NotNull BodiesResolveContext c) {

        // Member   veraible
        Set<CjVariable> processed = new HashSet<>();
        for (Map.Entry<CjTypeStatement, ClassDescriptorWithResolutionScopes> entry : c.getDeclaredClasses().entrySet()) {

            ClassDescriptorWithResolutionScopes classDescriptor = entry.getValue();

            for (CjVariable variable : entry.getKey().getVariables()) {
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

        for (Map.Entry<CjVariable, List<VariableDescriptor>> entry : c.getVariablesByPattern().entrySet()) {
            CjVariable variable = entry.getKey();
            if (processed.contains(variable)) continue;

            List<VariableDescriptor> variableDescriptors = entry.getValue();

            variableDescriptors.forEach(
                    variableDescriptor -> resolveVariable(c, variable, variableDescriptor)
            );

        }
    }

    //构造函数重载
    @Nullable
    private DataFlowInfo resolveConstructorDelegationCall(
            @NotNull DataFlowInfo outerDataFlowInfo,
            @NotNull BindingTrace trace,
            @NotNull LexicalScope scope,
            @NotNull CjConstructor<?> constructor,
            @NotNull ClassConstructorDescriptor descriptor,
            @Nullable InferenceSession inferenceSession
    ) {
        if (descriptor.getContainingDeclaration().kind == ClassKind.STRUCT && descriptor.isPrimary && constructor instanceof CjPrimaryConstructor) {
            return DataFlowInfo.Companion.getEMPTY();
        }
        if (descriptor.isExpect() || isEffectivelyExternal(descriptor)) {
            // For expected and external classes, we do not resolve constructor delegation calls because they are prohibited
            return DataFlowInfo.Companion.getEMPTY();
        }

        try {
            OverloadResolutionResults<?> results = callResolver.resolveConstructorDelegationCall(
                    trace, scope, outerDataFlowInfo,
                    descriptor, constructor.getDelegationCall(), inferenceSession);

            if (results != null && results.isSingleResult()) {
                ResolvedCall<? extends CallableDescriptor> resolvedCall = results.getResultingCall();
                recordConstructorDelegationCall(trace, descriptor, resolvedCall);
                return resolvedCall.getDataFlowInfoForArguments().getResultInfo();
            }
        } catch (NullPointerException nullPointerException) {
            return null;
        }
        return null;
    }

    private void checkPrimaryConstructorIsThis(@NotNull CjPrimaryConstructor constructor) {
        if (constructor.getDelegationCallOrNull() != null) {
            if (constructor.getDelegationCall().getCalleeExpression() != null) {
                if (constructor.getDelegationCall().getCalleeExpression().isThis()) {
                    trace.report(INVALID_CALLING_THIS_IN_PRIMARY_CONSTRUCTOR.on(constructor.getDelegationCall().getCalleeExpression()));
                }
            }
        }

    }

    public void resolvePrimaryConstructorBody(
            @NotNull DataFlowInfo outerDataFlowInfo,
            @NotNull BindingTrace trace,
            @NotNull CjPrimaryConstructor constructor,
            @NotNull ClassConstructorDescriptor descriptor,
            @NotNull LexicalScope declaringScope,
            @Nullable ExpressionTypingContext localContext
    ) {


        checkPrimaryConstructorIsThis(constructor);
        resolveConstructorBody(outerDataFlowInfo, trace, constructor, descriptor, declaringScope, localContext);

    }

    public void resolveConstructorBody(
            @NotNull DataFlowInfo outerDataFlowInfo,
            @NotNull BindingTrace trace,
            @NotNull CjConstructor<?> constructor,
            @NotNull ClassConstructorDescriptor descriptor,
            @NotNull LexicalScope declaringScope,
            @Nullable ExpressionTypingContext localContext
    ) {
        ForceResolveUtil.forceResolveAllContents(descriptor.getAnnotations());

        resolveFunctionBody(
                outerDataFlowInfo, trace, constructor, descriptor, declaringScope,
                headerInnerScope -> resolveConstructorDelegationCall(
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

    public void resolveEndSecondaryConstructorBody(
            @NotNull DataFlowInfo outerDataFlowInfo,
            @NotNull BindingTrace trace,
            @NotNull CjEndSecondaryConstructor constructor,
            @NotNull ClassConstructorDescriptor descriptor,
            @NotNull LexicalScope declaringScope,
            @Nullable ExpressionTypingContext localContext
    ) {
        resolveConstructorBody(outerDataFlowInfo, trace, constructor, descriptor, declaringScope, localContext);
    }

    public void resolveSecondaryConstructorBody(
            @NotNull DataFlowInfo outerDataFlowInfo,
            @NotNull BindingTrace trace,
            @NotNull CjSecondaryConstructor constructor,
            @NotNull ClassConstructorDescriptor descriptor,
            @NotNull LexicalScope declaringScope,
            @Nullable ExpressionTypingContext localContext
    ) {
        resolveConstructorBody(outerDataFlowInfo, trace, constructor, descriptor, declaringScope, localContext);
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
        CangJieType expectedTypeForInitializer = variable.getTypeReference() != null ? variableDescriptor.type : NO_EXPECTED_TYPE;
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
        CangJieType expectedTypeForInitializer = property.getTypeReference() != null ? propertyDescriptor.type : NO_EXPECTED_TYPE;
        if (propertyDescriptor.getCompileTimeInitializer() == null) {
            expressionTypingServices.getType(
                    propertyDeclarationInnerScope, initializer, expectedTypeForInitializer,
                    outerDataFlowInfo, inferenceSession != null ? inferenceSession : InferenceSession.Companion.getDefault(), trace
            );
        }
    }

    private ObservableBindingTrace createFieldTrackingTrace(PropertyDescriptor propertyDescriptor) {
        return new ObservableBindingTrace(trace).addHandler(
                BindingContext.REFERENCE_TARGET,
                (slice, expression, descriptor) -> {
                    if (expression instanceof CjSimpleNameExpression &&
                            descriptor instanceof SyntheticFieldDescriptor) {
                        trace.record(BindingContext.BACKING_FIELD_REQUIRED,
                                propertyDescriptor);
                    }
                }
        );
    }

    private void resolvePropertyAccessors(
            @NotNull BodiesResolveContext c,
            @NotNull CjProperty property,
            @NotNull PropertyDescriptor propertyDescriptor
    ) {
        ObservableBindingTrace fieldAccessTrackingTrace = createFieldTrackingTrace(propertyDescriptor);

        CjPropertyAccessor getter = property.getGetter();
        PropertyGetterDescriptor getterDescriptor = propertyDescriptor.getter;

        boolean forceResolveAnnotations = true;
        if (getterDescriptor != null) {
            if (getter != null) {
                LexicalScope accessorScope = makeScopeForPropertyAccessor(c, getter, propertyDescriptor);
                resolveFunctionBody(c.getOuterDataFlowInfo(), fieldAccessTrackingTrace, getter, getterDescriptor, accessorScope, c.getLocalContext());
            }

            if (getter != null || forceResolveAnnotations) {
                ForceResolveUtil.forceResolveAllContents(getterDescriptor.getAnnotations());
            }
        }

        CjPropertyAccessor setter = property.getSetter();
        PropertySetterDescriptor setterDescriptor = propertyDescriptor.setter;

        if (setterDescriptor != null) {
            if (setter != null) {
                LexicalScope accessorScope = makeScopeForPropertyAccessor(c, setter, propertyDescriptor);
                resolveFunctionBody(c.getOuterDataFlowInfo(), fieldAccessTrackingTrace, setter, setterDescriptor, accessorScope, c.getLocalContext());
            }

            if (setter != null || forceResolveAnnotations) {
                ForceResolveUtil.forceResolveAllContents(setterDescriptor.getAnnotations());
            }
        }
    }

    void resolveProperty(BodiesResolveContext c, CjProperty property, PropertyDescriptor propertyDescriptor) {
        computeDeferredType(propertyDescriptor.returnType);
        PreliminaryDeclarationVisitor.Companion.createForDeclaration(property, trace, languageVersionSettings);
//        CjExpression initializer = property.getInitializer();
//        LexicalScope variableHeaderScope = ScopeUtils.makeScopeForPropertyHeader(getScopeForProperty(c, property), propertyDescriptor);
//        ExpressionTypingContext context = c.getLocalContext();


//委托 没有
//        CjExpression delegateExpression = variable.getDelegateExpression();
//        if (delegateExpression != null) {
//            assert initializer == null : "Initializer should be null for delegated property : " + variable.getText();
//            resolveVariableDelegate(
//                    c.getOuterDataFlowInfo(), variable, variableDescriptor,
//                    delegateExpression, variableHeaderScope, context != null ? context.inferenceSession : null
//            );
//        }
        resolvePropertyAccessors(c, property, propertyDescriptor);

//注解 宏？
//        ForceResolveUtil.forceResolveAllContents(propertyDescriptor.getAnnotations());

//元数据 没有
//        FieldDescriptor backingField = variableDescriptor.getBackingField();
//        if (backingField != null) {
//            ForceResolveUtil.forceResolveAllContents(backingField.getAnnotations());
//        }
    }

    void resolveVariable(BodiesResolveContext c, CjVariable variable, VariableDescriptor variableDescriptor) {
        computeDeferredType(variableDescriptor.returnType);
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
                    descriptor.scopeForConstructorHeaderResolution,
                    descriptor.scopeForMemberDeclarationResolution,
                    localContext != null ? localContext.inferenceSession : null);
        }
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
        List<ValueParameterDescriptor> valueParameterDescriptors = constructorDescriptor.valueParameters;

        LexicalScope scope = getPrimaryConstructorParametersScope(declaringScope, constructorDescriptor);

        valueParameterResolver.resolveValueParameters(valueParameters, valueParameterDescriptors, scope, outerDataFlowInfo, trace, inferenceSession);
    }

    /**
     * 检查主构造函数数量
     * 检查非class struct的意外构造函数
     *
     * @param c
     */
    private void checkConstructorSize(@NotNull BodiesResolveContext c) {
        Set<Map.Entry<CjTypeStatement, ClassDescriptorWithResolutionScopes>> typeStatments = c.getDeclaredClasses().entrySet();

        for (Map.Entry<CjTypeStatement, ClassDescriptorWithResolutionScopes> entry : typeStatments) {


            for (CjConstructor constructor : entry.getKey().getEndSecondaryConstructors()) {
                if (!(entry.getKey() instanceof CjClass)) {
                    trace.report(UNEXPECTED_FINALIZER_IN_BODY_ERROR.on(constructor.getIdentifyingElement(), entry.getKey().getTypeName()));
                } else {
                    if (!constructor.getValueParameters().isEmpty()) {
                        trace.report(FINALIZER_CANNOT_HAVE_PARAMETERS_ERROR.on(constructor.getValueParameterList()));

                    }
                }
            }

            if (entry.getKey().isExtend()) {
                for (CjConstructor constructor : entry.getKey().getConstructors()) {
                    trace.report(UNEXPECTED_CONSTRUCTOR_IN_BODY_ERROR.on(constructor.getIdentifyingElement(), "extend"));
                }
            }
            if (entry.getKey().isEnum()) {
                for (CjConstructor constructor : entry.getKey().getConstructors()) {
                    trace.report(UNEXPECTED_CONSTRUCTOR_IN_BODY_ERROR.on(constructor.getIdentifyingElement(), "enum"));

                }
            }

            for (CjPrimaryConstructor constructor : entry.getKey().getPrimaryConstructors()) {
                if (!constructor.getName().equals(entry.getValue().getName().asString())) {
//                    主构造函数名称不一致
                    trace.report(CONSTRUCTOR_NAME_INCONSISTENCY.on(constructor));

                }
                if (entry.getKey().getPrimaryConstructors().size() > 1) {
                    trace.report(MULTIPLE_PRIMARY_CONSTRUCTORS.on(constructor, entry.getValue()));

                }
            }

        }
    }

    //    与从构造函数行为一致，但是不能有this()
    private void resolvePrimaryConstructorParameters(@NotNull BodiesResolveContext c) {
        // 检查主构造名称
//        检查this的使用
        Set<Map.Entry<CjPrimaryConstructor, ClassConstructorDescriptor>> constructors = c.getPrimaryConstructors().entrySet();


        for (Map.Entry<CjPrimaryConstructor, ClassConstructorDescriptor> entry : constructors) {

//            if (constructors.size() > 1) {
//                trace.report(MULTIPLE_PRIMARY_CONSTRUCTORS.on(entry.getKey(), entry.getValue().getContainingDeclaration()));
//
//            }

            LexicalScope declaringScope = c.getDeclaringScope(entry.getKey());
            assert declaringScope != null : "Declaring scope should be registered before body resolve";
            resolvePrimaryConstructorBody(c.getOuterDataFlowInfo(), trace, entry.getKey(), entry.getValue(), declaringScope, c.getLocalContext());
        }
        if (c.getPrimaryConstructors().isEmpty()) return;
        Set<ConstructorDescriptor> visitedConstructors = new HashSet<>();
        for (Map.Entry<CjPrimaryConstructor, ClassConstructorDescriptor> entry : c.getPrimaryConstructors().entrySet()) {
            checkCyclicConstructorDelegationCall(entry.getValue(), visitedConstructors);
        }
    }

    private void resolveEndSecondaryConstructors(@NotNull BodiesResolveContext c) {
        for (Map.Entry<CjEndSecondaryConstructor, ClassConstructorDescriptor> entry : c.getEndSecondaryConstructors().entrySet()) {
            LexicalScope declaringScope = c.getDeclaringScope(entry.getKey());
            assert declaringScope != null : "Declaring scope should be registered before body resolve";
            resolveEndSecondaryConstructorBody(c.getOuterDataFlowInfo(), trace, entry.getKey(), entry.getValue(), declaringScope, c.getLocalContext());
        }

    }

    private void resolveSecondaryConstructors(@NotNull BodiesResolveContext c) {
        for (Map.Entry<CjSecondaryConstructor, ClassConstructorDescriptor> entry : c.getSecondaryConstructors().entrySet()) {
            LexicalScope declaringScope = c.getDeclaringScope(entry.getKey());
            assert declaringScope != null : "Declaring scope should be registered before body resolve";
            resolveSecondaryConstructorBody(c.getOuterDataFlowInfo(), trace, entry.getKey(), entry.getValue(), declaringScope, c.getLocalContext());
        }
        if (c.getSecondaryConstructors().isEmpty()) return;
        Set<ConstructorDescriptor> visitedConstructors = new HashSet<>();
        for (Map.Entry<CjSecondaryConstructor, ClassConstructorDescriptor> entry : c.getSecondaryConstructors().entrySet()) {
            checkCyclicConstructorDelegationCall(entry.getValue(), visitedConstructors);
        }
    }

    @Nullable
    private ConstructorDescriptor getDelegatedConstructor(@NotNull ConstructorDescriptor constructor) {
        ResolvedCall<ConstructorDescriptor> call = trace.get(CONSTRUCTOR_RESOLVED_DELEGATION_CALL, constructor);
        return call == null || !call.getStatus().isSuccess() ? null : call.getResultingDescriptor().original;
    }

    private void reportEachConstructorOnCycle(@NotNull ConstructorDescriptor startConstructor) {
        ConstructorDescriptor currentConstructor = startConstructor;
        do {
            PsiElement constructorToReport = DescriptorToSourceUtils.descriptorToDeclaration(currentConstructor);
            if (constructorToReport != null) {
                CjConstructorDelegationCall call = ((CjConstructor<?>) constructorToReport).getDelegationCall();
                assert call.getCalleeExpression() != null
                        : "Callee expression of delegation call should not be null on cycle as there should be explicit 'this' calls";
                trace.report(CYCLIC_CONSTRUCTOR_DELEGATION_CALL.on(call.getCalleeExpression()));
            }

            currentConstructor = getDelegatedConstructor(currentConstructor);
            assert currentConstructor != null : "Delegated constructor should not be null in cycle";
        }
        while (startConstructor != currentConstructor);
    }

    private void checkCyclicConstructorDelegationCall(
            @NotNull ConstructorDescriptor constructorDescriptor,
            @NotNull Set<ConstructorDescriptor> visitedConstructors
    ) {
        if (visitedConstructors.contains(constructorDescriptor)) return;

        // if visit constructor that is already in current chain
        // such constructor is on cycle
        Set<ConstructorDescriptor> visitedInCurrentChain = new HashSet<>();
        ConstructorDescriptor currentConstructorDescriptor = constructorDescriptor;
        while (true) {
            ProgressManager.checkCanceled();

            visitedInCurrentChain.add(currentConstructorDescriptor);
            ConstructorDescriptor delegatedConstructorDescriptor = getDelegatedConstructor(currentConstructorDescriptor);
            if (delegatedConstructorDescriptor == null) break;

            // if next delegation call is super or primary constructor or already visited
            if (!constructorDescriptor.containingDeclaration.equals(delegatedConstructorDescriptor.containingDeclaration) ||
                    delegatedConstructorDescriptor.isPrimary ||
                    visitedConstructors.contains(delegatedConstructorDescriptor)) {
                break;
            }

            if (visitedInCurrentChain.contains(delegatedConstructorDescriptor)) {
                reportEachConstructorOnCycle(delegatedConstructorDescriptor);
                break;
            }
            currentConstructorDescriptor = delegatedConstructorDescriptor;
        }
        visitedConstructors.addAll(visitedInCurrentChain);
    }


    private void resolveBehaviorDeclarationBodies(@NotNull BodiesResolveContext c) {
        resolveSuperTypeEntryLists(c);


        resolvePropertyDeclarationBodies(c);

        resolveVariableDeclarationBodies(c);


        checkConstructorSize(c);
        resolvePrimaryConstructorParameters(c);
        resolveSecondaryConstructors(c);
        //TODO 析构函数
        resolveEndSecondaryConstructors(c);
        resolveMainFunctionBodies(c);


        resolveFunctionBodies(c);
        resolveMacroBodies(c);

    }

    private void resolveMainFunctionBodies(BodiesResolveContext c) {

        for (Map.Entry<CjMainFunction, SimpleFunctionDescriptor> entry : c.getMainFunctions().entrySet()) {
            CjMainFunction declaration = entry.getKey();

            LexicalScope scope = c.getDeclaringScope(declaration);
            assert scope != null : "Scope is null: " + PsiUtilsKt.getElementTextWithContext(declaration);

            if (!c.getTopDownAnalysisMode().isLocalDeclarations() && !(bodyResolveCache instanceof BodyResolveCache.ThrowException) &&
                    expressionTypingServices.statementFilter != StatementFilter.NONE) {
                bodyResolveCache.resolveMainFunctionBody(declaration).addOwnDataTo(trace, true);
            } else {
                resolveFunctionBody(c.getOuterDataFlowInfo(), trace, declaration, entry.getValue(), scope, c.getLocalContext());
            }
        }
    }

    private void resolvePropertyDeclarationBodies(@NotNull BodiesResolveContext c) {

        // Member properties
        Set<CjProperty> processed = new HashSet<>();
        for (Map.Entry<CjTypeStatement, ClassDescriptorWithResolutionScopes> entry : c.getDeclaredClasses().entrySet()) {

            if (entry.getKey() instanceof CjEnumEntry) continue;
            CjTypeStatement cjClass = entry.getKey();
            ClassDescriptorWithResolutionScopes classDescriptor = entry.getValue();

            for (CjProperty property : cjClass.getProperties()) {
                PropertyDescriptor propertyDescriptor = c.getProperties().get(property);
                assert propertyDescriptor != null;

                resolveProperty(c, property, propertyDescriptor);
                processed.add(property);
            }
        }

        // Top-level properties & properties of objects
        for (Map.Entry<CjProperty, PropertyDescriptor> entry : c.getProperties().entrySet()) {
            CjProperty property = entry.getKey();
            if (processed.contains(property)) continue;

            PropertyDescriptor propertyDescriptor = entry.getValue();

            resolveProperty(c, property, propertyDescriptor);
        }
    }

    private void resolveMacroBodies(BodiesResolveContext c) {

        for (Map.Entry<CjMacroDeclaration, MacroDescriptor> entry : c.getMacros().entrySet()) {
            CjMacroDeclaration declaration = entry.getKey();

            LexicalScope scope = c.getDeclaringScope(declaration);
            assert scope != null : "Scope is null: " + PsiUtilsKt.getElementTextWithContext(declaration);

            if (!c.getTopDownAnalysisMode().isLocalDeclarations() && !(bodyResolveCache instanceof BodyResolveCache.ThrowException) &&
                    expressionTypingServices.statementFilter != StatementFilter.NONE) {
                bodyResolveCache.resolveMacroBody(declaration).addOwnDataTo(trace, true);
            } else {
                resolveFunctionBody(c.getOuterDataFlowInfo(), trace, declaration, entry.getValue(), scope, c.getLocalContext());
            }
        }
    }

    private void resolveFunctionBodies(BodiesResolveContext c) {

        for (Map.Entry<CjNamedFunction, SimpleFunctionDescriptor> entry : c.getFunctions().entrySet()) {
            CjNamedFunction declaration = entry.getKey();

            LexicalScope scope = c.getDeclaringScope(declaration);
            assert scope != null : "Scope is null: " + PsiUtilsKt.getElementTextWithContext(declaration);

            if (!c.getTopDownAnalysisMode().isLocalDeclarations() && !(bodyResolveCache instanceof BodyResolveCache.ThrowException) &&
                    expressionTypingServices.statementFilter != StatementFilter.NONE) {
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

        resolveFunctionBody(outerDataFlowInfo, trace, function, functionDescriptor, declaringScope, null, null, localContext);
//TODO 检查返回值
        assert functionDescriptor.returnType != null;
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

            @Nullable ExpressionTypingContext localContext
    ) {
        ProgressManager.checkCanceled();

        PreliminaryDeclarationVisitor.Companion.createForDeclaration(function, trace, languageVersionSettings);
        LexicalScope innerScope = FunctionDescriptorUtil.getFunctionInnerScope(scope, functionDescriptor, trace, overloadChecker);
        List<CjParameter> valueParameters = function.getValueParameters();
        List<ValueParameterDescriptor> valueParameterDescriptors = functionDescriptor.valueParameters;

        LexicalScope headerScope = headerScopeFactory != null ? headerScopeFactory.invoke(innerScope) : innerScope;
        valueParameterResolver.resolveValueParameters(
                valueParameters, valueParameterDescriptors, headerScope, outerDataFlowInfo, trace,
                localContext != null ? localContext.inferenceSession : null
        );


        if (functionDescriptor instanceof PropertyAccessorDescriptor accessorDescriptor && functionDescriptor.extensionReceiverParameter == null
                && functionDescriptor.contextReceiverParameters.isEmpty()) {
            CjProperty property = (CjProperty) function.getParent().getParent();
            SourceElement propertySourceElement = CangJieSourceElementKt.toSourceElement(property);
            SyntheticFieldDescriptor fieldDescriptor = new SyntheticFieldDescriptor(accessorDescriptor, propertySourceElement);
            innerScope = new LexicalScopeImpl(innerScope, functionDescriptor, true, null, Collections.emptyList(),
                    LexicalScopeKind.PROPERTY_ACCESSOR_BODY,
                    LocalRedeclarationChecker.DO_NOTHING.INSTANCE, handler -> {
                handler.addVariableDescriptor(fieldDescriptor);
                return Unit.INSTANCE;
            });
            // Check parameter name shadowing
            for (CjParameter parameter : function.getValueParameters()) {
                if (SyntheticFieldDescriptor.NAME.equals(parameter.getNameAsName())) {
                    trace.report(Errors.ACCESSOR_PARAMETER_NAME_SHADOWING.on(parameter));
                }
            }
        }

        DataFlowInfo dataFlowInfo = null;

        if (beforeBlockBody != null) {
            dataFlowInfo = beforeBlockBody.invoke(headerScope);
        }

        if (function.hasBody()) {
            expressionTypingServices.checkFunctionReturnType(
                    innerScope, function, functionDescriptor, dataFlowInfo != null ? dataFlowInfo : outerDataFlowInfo, null, trace, localContext
            );
        } else {
            functionDescriptor.returnType;
        }
//TODO 检查返回值
        assert functionDescriptor.returnType != null;
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

    private boolean checkPrimaryConstructor(@Nullable ClassConstructorDescriptor unsubstitutedPrimaryConstructor) {

        return unsubstitutedPrimaryConstructor != null &&
                PsiSourceElementKt.getPsi(unsubstitutedPrimaryConstructor.getSource()) != null &&
                !(PsiSourceElementKt.getPsi(unsubstitutedPrimaryConstructor.getSource()) instanceof CjPrimaryConstructor);
    }

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


                hasExtendSourceMap.put(typeReference, hasExtendSource);
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
                if (superClass.kind.isObject()) {
//                     A "singleton in supertype" diagnostic will be reported later
                    return;
                }
                if (descriptor.kind != ClassKind.INTERFACE &&

                        checkPrimaryConstructor(descriptor.getUnsubstitutedPrimaryConstructor()) &&
//                        descriptor.getConstructors().isEmpty() &&
                        superClass.kind != ClassKind.INTERFACE &&
                        !descriptor.isExpect() && !isEffectivelyExternal(descriptor) &&
                        !ErrorUtils.isError(superClass) && TypeUtils.checkConstructorsNotParameter(superClass)
                ) {
                    trace.report(SUPERTYPE_NOT_INITIALIZED.on(specifier, supertype));
                }
            }

            @Override
            public void visitCjElement(@NotNull CjElement element) {
                throw new UnsupportedOperationException(element.getText() + " : " + element);
            }
        };

        Set<CangJieType> sourceSuperClass = new HashSet<>();
        //   TODO      如果是扩展，将源类型加上，但是这里还缺少其他扩展
        if (typeStatement instanceof CjExtend && descriptor instanceof LazyExtendClassDescriptor) {
            sourceSuperClass.addAll(((LazyExtendClassDescriptor) descriptor).getClassDescriptor().typeConstructor.getSupertypes());
            sourceSuperClass.addAll(((LazyExtendClassDescriptor) descriptor).getClassDescriptor().typeConstructor.getExtendSupertypes(((LazyExtendClassDescriptor) descriptor).getTypeStatement().getExtendId()));

//            List<CjSuperTypeListEntry> superTypeListEntries = descriptor.getSuperTypeListEntries();
//            CjTypeStatement sourceClassElement = ((LazyExtendClassDescriptor) descriptor).getSourceClassElement();
//
//            if (sourceClassElement != null) {
//                hasExtendSource = true;
//                for (CjSuperTypeListEntry delegationSpecifier : sourceClassElement.getSuperTypeListEntries()) {
//
//
//                    ProgressManager.checkCanceled();
//
////                    if (delegationSpecifier.getParentDeclaration() != null && delegationSpecifier.getParentDeclaration() instanceof CjExtend) {
//////                    判断扩展id是否一致，如果一致则不解析
////                        if (((CjExtend) delegationSpecifier.getParentDeclaration()).getExtendId().equals(((LazyExtendClassDescriptor) descriptor).getTypeStatement().getExtendId())) {
////                            continue;
////                        }
////                    }
//
//                    delegationSpecifier.accept(visitor);
//                }
//                hasExtendSource = false;
//
//            }
        }


        for (CjSuperTypeListEntry delegationSpecifier : typeStatement.getSuperTypeListEntries()) {
            ProgressManager.checkCanceled();

            delegationSpecifier.accept(visitor);
        }


        if (primaryConstructorDelegationCall[0] != null && primaryConstructor != null) {
            recordConstructorDelegationCall(trace, primaryConstructor, primaryConstructorDelegationCall[0]);
        }


        checkSupertypeList(descriptor, supertypes, typeStatement, sourceSuperClass);

    }

    public void resolveBodies(@NotNull BodiesResolveContext c) {
        // 解析行为声明体
        resolveBehaviorDeclarationBodies(c);
        // 处理控制流分析
        controlFlowAnalyzer.process(c);
        // 处理声明检查
        declarationsChecker.process(c);
//        // 处理分析器扩展
//        analyzerExtensions.process(c);
    }
}
