package com.huawei.cangjie.resolve;


import com.huawei.cangjie.storage.StorageManager;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import kotlin.Pair;
import kotlin.TuplesKt;
import kotlin.collections.CollectionsKt;
import kotlin.collections.SetsKt;
import kotlin.jvm.functions.Function0;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.huawei.cangjie.builtins.FunctionTypesKt;
import com.huawei.cangjie.builtins.CangJieBuiltIns;
import com.huawei.cangjie.config.LanguageFeature;
import com.huawei.cangjie.config.LanguageVersionSettings;
import com.huawei.cangjie.descriptors.*;
import com.huawei.cangjie.descriptors.annotations.AnnotationSplitter;
import com.huawei.cangjie.descriptors.annotations.AnnotationUseSiteTarget;
import com.huawei.cangjie.descriptors.annotations.Annotations;
import com.huawei.cangjie.descriptors.annotations.CompositeAnnotations;
import com.huawei.cangjie.descriptors.impl.*;

import com.huawei.cangjie.incremental.components.NoLookupLocation;
import com.huawei.cangjie.lexer.CjTokens;
import com.huawei.cangjie.name.Name;
import com.huawei.cangjie.name.SpecialNames;
import com.huawei.cangjie.psi.*;

import com.huawei.cangjie.resolve.calls.components.InferenceSession;
import com.huawei.cangjie.resolve.calls.smartcasts.DataFlowInfo;
import com.huawei.cangjie.resolve.calls.smartcasts.DataFlowInfoFactory;
import com.huawei.cangjie.resolve.calls.smartcasts.DataFlowValueFactory;
import com.huawei.cangjie.resolve.calls.util.CallResolverUtilKt;
import com.huawei.cangjie.resolve.calls.util.UnderscoreUtilKt;

import com.huawei.cangjie.resolve.lazy.ForceResolveUtil;

import com.huawei.cangjie.resolve.scopes.*;
import com.huawei.cangjie.resolve.scopes.receivers.ExpressionReceiver;
import com.huawei.cangjie.resolve.scopes.receivers.TransientReceiver;

import com.huawei.cangjie.resolve.source.CangJieSourceElementKt;
import com.huawei.cangjie.storage.StorageManager;
import com.huawei.cangjie.types.*;
import com.huawei.cangjie.types.checker.CangJieTypeChecker;
import com.huawei.cangjie.types.error.ErrorTypeKind;

import com.huawei.cangjie.types.expressions.*;


import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.huawei.cangjie.descriptors.Errors.VARIANCE_ON_TYPE_PARAMETER_NOT_ALLOWED;
import static com.huawei.cangjie.descriptors.annotations.AnnotationUseSiteTarget.*;

import static com.huawei.cangjie.lexer.CjTokens.*;
import static com.huawei.cangjie.resolve.BindingContext.*;
import static com.huawei.cangjie.resolve.DescriptorUtils.*;

public class DescriptorResolver {
    private final TypeResolver typeResolver;
    private final AnnotationResolver annotationResolver;
    private final StorageManager storageManager;
    private final CangJieBuiltIns builtIns;
    private final SupertypeLoopChecker supertypeLoopsResolver;
//    private final VariableTypeAndInitializerResolver variableTypeAndInitializerResolver;
    private final ExpressionTypingServices expressionTypingServices;
    private final OverloadChecker overloadChecker;
//    private final LanguageVersionSettings languageVersionSettings;
//    private final FunctionsTypingVisitor functionsTypingVisitor;
//    private final DestructuringDeclarationResolver destructuringDeclarationResolver;
    private final ModifiersChecker modifiersChecker;
//    private final WrappedTypeFactory wrappedTypeFactory;
//    private final SyntheticResolveExtension syntheticResolveExtension;
    private final TypeApproximator typeApproximator;
//    private final DeclarationReturnTypeSanitizer declarationReturnTypeSanitizer;
    private final DataFlowValueFactory dataFlowValueFactory;
//    private final Iterable<DeclarationSignatureAnonymousTypeTransformer> anonymousTypeTransformers;
//    private final AdditionalClassPartsProvider additionalClassPartsProvider;

    public DescriptorResolver(
            @NotNull AnnotationResolver annotationResolver,
            @NotNull CangJieBuiltIns builtIns,
            @NotNull StorageManager storageManager,
            @NotNull TypeResolver typeResolver,
            @NotNull SupertypeLoopChecker supertypeLoopsResolver,
//            @NotNull VariableTypeAndInitializerResolver variableTypeAndInitializerResolver,
            @NotNull ExpressionTypingServices expressionTypingServices,
            @NotNull OverloadChecker overloadChecker,
//            @NotNull LanguageVersionSettings languageVersionSettings,
//            @NotNull FunctionsTypingVisitor functionsTypingVisitor,
//            @NotNull DestructuringDeclarationResolver destructuringDeclarationResolver,
            @NotNull ModifiersChecker modifiersChecker,
//            @NotNull WrappedTypeFactory wrappedTypeFactory,
            @NotNull Project project,
            @NotNull TypeApproximator approximator,
//            @NotNull DeclarationReturnTypeSanitizer declarationReturnTypeSanitizer,
            @NotNull DataFlowValueFactory dataFlowValueFactory
//            ,
//            @NotNull Iterable<DeclarationSignatureAnonymousTypeTransformer> anonymousTypeTransformers,
//            @NotNull AdditionalClassPartsProvider additionalClassPartsProvider
    ) {
        this.annotationResolver = annotationResolver;
        this.builtIns = builtIns;
        this.storageManager = storageManager;
        this.typeResolver = typeResolver;
        this.supertypeLoopsResolver = supertypeLoopsResolver;
//        this.variableTypeAndInitializerResolver = variableTypeAndInitializerResolver;
        this.expressionTypingServices = expressionTypingServices;
        this.overloadChecker = overloadChecker;
//        this.languageVersionSettings = languageVersionSettings;
//        this.functionsTypingVisitor = functionsTypingVisitor;
//        this.destructuringDeclarationResolver = destructuringDeclarationResolver;
        this.modifiersChecker = modifiersChecker;
//        this.wrappedTypeFactory = wrappedTypeFactory;
//        this.syntheticResolveExtension = SyntheticResolveExtension.Companion.getInstance(project);
        typeApproximator = approximator;
//        this.declarationReturnTypeSanitizer = declarationReturnTypeSanitizer;
        this.dataFlowValueFactory = dataFlowValueFactory;
//        this.anonymousTypeTransformers = anonymousTypeTransformers;
//        this.additionalClassPartsProvider = additionalClassPartsProvider;
    }

//    public List<CangJieType> resolveSupertypes(
//            @NotNull LexicalScope scope,
//            @NotNull ClassDescriptor classDescriptor,
//            @Nullable CjPureClassOrObject correspondingClassOrObject,
//            BindingTrace trace
//    ) {
//        List<CangJieType> supertypes = Lists.newArrayList();
//        List<CjSuperTypeListEntry> delegationSpecifiers =
//                correspondingClassOrObject == null ? Collections.emptyList() : correspondingClassOrObject.getSuperTypeListEntries();
//        Collection<CangJieType> declaredSupertypes = resolveSuperTypeListEntries(
//                scope,
//                delegationSpecifiers,
//                typeResolver, trace, false);
//
//        for (CangJieType declaredSupertype : declaredSupertypes) {
//            addValidSupertype(supertypes, declaredSupertype);
//        }
//
//        if (classDescriptor.getKind() == ClassKind.ENUM_CLASS && !containsClass(supertypes)) {
//            supertypes.add(0, builtIns.getEnumType(classDescriptor.getDefaultType()));
//        }
//
//        syntheticResolveExtension.addSyntheticSupertypes(classDescriptor, supertypes);
//        supertypes.addAll(additionalClassPartsProvider.getAdditionalSupertypes(classDescriptor, supertypes));
//
//        if (supertypes.isEmpty()) {
//            addValidSupertype(supertypes, getDefaultSupertype(classDescriptor));
//        }
//
//        return supertypes;
//    }

    private static void addValidSupertype(List<CangJieType> supertypes, CangJieType declaredSupertype) {
        if (!CangJieTypeKt.isError(declaredSupertype)) {
            supertypes.add(declaredSupertype);
        }
    }

    private static boolean containsClass(Collection<CangJieType> result) {
        for (CangJieType type : result) {
            ClassifierDescriptor descriptor = type.getConstructor().getDeclarationDescriptor();
            if (descriptor instanceof ClassDescriptor && ((ClassDescriptor) descriptor).getKind() != ClassKind.INTERFACE) {
                return true;
            }
        }
        return false;
    }

//    @NotNull
//    private CangJieType getDefaultSupertype(@NotNull ClassDescriptor classDescriptor) {
//        if (classDescriptor.getKind() == ClassKind.ENUM_ENTRY) {
//            return ((ClassDescriptor) classDescriptor.getContainingDeclaration()).getDefaultType();
//        }
//        else if (classDescriptor.getKind() == ClassKind.ANNOTATION_CLASS) {
//            return builtIns.getAnnotationType();
//        }
//        return builtIns.getAnyType();
//    }

//    private static Collection<CangJieType> resolveSuperTypeListEntries(
//            LexicalScope extensibleScope,
//            List<CjSuperTypeListEntry> delegationSpecifiers,
//            @NotNull TypeResolver resolver,
//            BindingTrace trace,
//            boolean checkBounds
//    ) {
//        if (delegationSpecifiers.isEmpty()) {
//            return Collections.emptyList();
//        }
//        Collection<CangJieType> result = Lists.newArrayList();
//        for (CjSuperTypeListEntry delegationSpecifier : delegationSpecifiers) {
//            CjTypeReference typeReference = delegationSpecifier.getTypeReference();
//            if (typeReference != null) {
//                CangJieType supertype = resolver.resolveType(extensibleScope, typeReference, trace, checkBounds);
//                if (DynamicTypesCj.isDynamic(supertype)) {
//                    trace.report(DYNAMIC_SUPERTYPE.on(typeReference));
//                }
//                else {
//                    result.add(supertype);
//                    CjTypeElement bareSuperType = checkNullableSupertypeAndStripQuestionMarks(trace, typeReference.getTypeElement());
//                    checkProjectionsInImmediateArguments(trace, bareSuperType, supertype);
//                }
//            }
//            else {
//                result.add(ErrorUtils.createErrorType(ErrorTypeKind.UNRESOLVED_TYPE, delegationSpecifier.getText()));
//            }
//        }
//        return result;
//    }

//    @Nullable
//    private static CjTypeElement checkNullableSupertypeAndStripQuestionMarks(@NotNull BindingTrace trace, @Nullable CjTypeElement typeElement) {
//        while (typeElement instanceof CjNullableType) {
//            CjNullableType nullableType = (CjNullableType) typeElement;
//            typeElement = nullableType.getInnerType();
//            // report only for innermost '?', the rest gets a 'redundant' warning
//            if (!(typeElement instanceof CjNullableType) && typeElement != null) {
//                trace.report(NULLABLE_SUPERTYPE.on(nullableType));
//            }
//        }
//        return typeElement;
//    }

//    private static void checkProjectionsInImmediateArguments(
//            @NotNull BindingTrace trace,
//            @Nullable CjTypeElement typeElement,
//            @NotNull CangJieType type
//    ) {
//        if (typeElement == null) return;
//
//        boolean hasProjectionsInWrittenArguments = false;
//        if (typeElement instanceof CjUserType) {
//            CjUserType userType = (CjUserType) typeElement;
//            List<CjTypeProjection> typeArguments = userType.getTypeArguments();
//            for (CjTypeProjection typeArgument : typeArguments) {
//                if (typeArgument.getProjectionKind() != CjProjectionKind.NONE) {
//                    trace.report(PROJECTION_IN_IMMEDIATE_ARGUMENT_TO_SUPERTYPE.on(typeArgument));
//                    hasProjectionsInWrittenArguments = true;
//                }
//            }
//        }
//
//        // If we have an abbreviated type (written with a type alias), it still can contain type projections in top-level arguments.
//        if (!CangJieTypeKt.isError(type) && SpecialTypesKt.getAbbreviatedType(type) != null && !hasProjectionsInWrittenArguments) {
//            // Only interface inheritance should be checked here.
//            // Corresponding check for classes is performed for type alias constructor calls in CandidateResolver.
//            if (TypeUtilsKt.isInterface(type) && TypeUtilsKt.containsTypeProjectionsInTopLevelArguments(type)) {
//                trace.report(EXPANDED_TYPE_CANNOT_BE_INHERITED.on(typeElement, type));
//            }
//        }
//    }

    public static DescriptorVisibility getDefaultVisibility(CjModifierListOwner modifierListOwner, DeclarationDescriptor containingDescriptor) {
        DescriptorVisibility defaultVisibility;
        if (containingDescriptor instanceof ClassDescriptor) {
            CjModifierList modifierList = modifierListOwner.getModifierList();
            defaultVisibility =
//                    modifierList != null && modifierList.hasModifier(OVERRIDE_KEYWORD)
//                    ? DescriptorVisibilities.INHERITED
//                    :
                    DescriptorVisibilities.DEFAULT_VISIBILITY;
        }
        else if (containingDescriptor instanceof FunctionDescriptor /*|| containingDescriptor instanceof PropertyDescriptor*/) {
            defaultVisibility = DescriptorVisibilities.LOCAL;
        }
        else {
            defaultVisibility = DescriptorVisibilities.DEFAULT_VISIBILITY;
        }
        return defaultVisibility;
    }

    public static Modality getDefaultModality(DeclarationDescriptor containingDescriptor, DescriptorVisibility visibility, boolean isBodyPresent) {
        Modality defaultModality;
        if (containingDescriptor instanceof ClassDescriptor) {
            boolean isTrait = ((ClassDescriptor) containingDescriptor).getKind() == ClassKind.INTERFACE;
            boolean isDefinitelyAbstract = isTrait && !isBodyPresent;
            Modality basicModality = isTrait && !DescriptorVisibilities.isPrivate(visibility) ? Modality.OPEN : Modality.FINAL;
            defaultModality = isDefinitelyAbstract ? Modality.ABSTRACT : basicModality;
        }
        else {
            defaultModality = Modality.FINAL;
        }
        return defaultModality;
    }

    @NotNull
    public ValueParameterDescriptorImpl resolveValueParameterDescriptor(
            @NotNull LexicalScope scope,
            @NotNull FunctionDescriptor owner,
            @NotNull CjParameter valueParameter,
            int index,
            @NotNull CangJieType type,
            @NotNull BindingTrace trace,
            @NotNull Annotations additionalAnnotations,
            @Nullable InferenceSession inferenceSession
    ) {
        CangJieType varargElementType = null;
        CangJieType variableType = type;
//        if (valueParameter.hasModifier(VARARG_KEYWORD)) {
//            varargElementType = type;
//            variableType = getVarargParameterType(type);
//        }

        Annotations valueParameterAnnotations = resolveValueParameterAnnotations(scope, valueParameter, trace, additionalAnnotations);

        CjDestructuringDeclaration destructuringDeclaration = valueParameter.getDestructuringDeclaration();

        Function0<List<VariableDescriptor>> destructuringVariables;
        if (destructuringDeclaration != null) {
//            if (!languageVersionSettings.supportsFeature(LanguageFeature.DestructuringLambdaParameters)) {
//                trace.report(Errors.UNSUPPORTED_FEATURE.on(valueParameter,
//                        TuplesKt.to(LanguageFeature.DestructuringLambdaParameters, languageVersionSettings)));
//            }

            //                ReceiverParameterDescriptor dispatchReceiver = owner.getDispatchReceiverParameter();
            //                assert dispatchReceiver == null || dispatchReceiver.getContainingDeclaration() instanceof ScriptDescriptor
            //                        : "Destructuring declarations are only be parsed for lambdas, and they must not have a dispatch receiver";
            //                LexicalScope scopeForDestructuring =
            //                        ScopeUtilsKt.createScopeForDestructuring(scope, owner.getExtensionReceiverParameter());
            //
            //                List<VariableDescriptor> result =
            //                        destructuringDeclarationResolver.resolveLocalVariablesFromDestructuringDeclaration(
            //                                scope,
            //                                destructuringDeclaration, new TransientReceiver(type), /* initializer = */ null,
            //                                ExpressionTypingContext.newContext(
            //                                        trace, scopeForDestructuring, DataFlowInfoFactory.EMPTY, TypeUtils.NO_EXPECTED_TYPE,
            //                                        languageVersionSettings, dataFlowValueFactory, inferenceSession
            //                                )
            //                        );
            //
            //                modifiersChecker.withTrace(trace).checkModifiersForDestructuringDeclaration(destructuringDeclaration);
            //                return result;
            destructuringVariables = ArrayList::new;
        }
        else {
            destructuringVariables = null;
        }

        Name parameterName;

        if (destructuringDeclaration == null) {
            // NB: val/var for parameter is only allowed in primary constructors where single underscore names are still prohibited.
            // The problem with val/var is that when lazy resolve try to find their descriptor, it searches through the member scope
            // of containing class where, it can not find a descriptor with special name.
            // Thus, to preserve behavior, we don't use a special name for val/var.
            parameterName = !valueParameter.hasValOrVar() && UnderscoreUtilKt.isSingleUnderscore(valueParameter)
                    ? SpecialNames.anonymousParameterName(index)
                    : CjPsiUtil.safeName(valueParameter.getName());
        }
        else {
            parameterName = Name.special("<name for destructuring parameter " + index + ">");
        }

        ValueParameterDescriptorImpl valueParameterDescriptor = ValueParameterDescriptorImpl.createWithDestructuringDeclarations(
                owner,
                null,
                index,
                valueParameterAnnotations,
                parameterName,
                variableType,
                valueParameter.hasDefaultValue(),
//                valueParameter.hasModifier(CROSSINLINE_KEYWORD),
//                valueParameter.hasModifier(NOINLINE_KEYWORD),
//                varargElementType,
                CangJieSourceElementKt.toSourceElement(valueParameter),
                destructuringVariables
        );

        trace.record(BindingContext.VALUE_PARAMETER, valueParameter, valueParameterDescriptor);
        return valueParameterDescriptor;
    }

    @NotNull
    private Annotations resolveValueParameterAnnotations(
            @NotNull LexicalScope scope,
            @NotNull CjParameter parameter,
            @NotNull BindingTrace trace,
            @NotNull Annotations additionalAnnotations
    ) {
       CjModifierList modifierList = parameter.getModifierList();
        if (modifierList == null) {
            return additionalAnnotations;
        }

        Annotations allAnnotations = annotationResolver.resolveAnnotationsWithoutArguments(scope, modifierList, trace);
        if (!parameter.hasValOrVar()) {
            return new CompositeAnnotations(allAnnotations, additionalAnnotations);
        }

        AnnotationSplitter splitter = new AnnotationSplitter(storageManager, allAnnotations, SetsKt.setOf(CONSTRUCTOR_PARAMETER));
        return new CompositeAnnotations(splitter.getAnnotationsForTarget(CONSTRUCTOR_PARAMETER), additionalAnnotations);
    }

//    @NotNull
//    private CangJieType getVarargParameterType(@NotNull CangJieType elementType) {
//        CangJieType primitiveArrayType = builtIns.getPrimitiveArrayCangJieTypeByPrimitiveCangJieType(elementType);
//        if (primitiveArrayType != null) {
//            return primitiveArrayType;
//        }
//        return builtIns.getArrayType(Variance.OUT_VARIANCE, elementType);
//    }

    public List<TypeParameterDescriptorImpl> resolveTypeParametersForDescriptor(
            DeclarationDescriptor containingDescriptor,
            LexicalWritableScope extensibleScope,
            LexicalScope scopeForAnnotationsResolve,
            List<CjTypeParameter> typeParameters,
            BindingTrace trace
    ) {
        List<TypeParameterDescriptorImpl> descriptors =
                resolveTypeParametersForDescriptor(containingDescriptor, scopeForAnnotationsResolve, typeParameters, trace);
        for (TypeParameterDescriptorImpl descriptor : descriptors) {
            extensibleScope.addClassifierDescriptor(descriptor);
        }
        return descriptors;
    }

    private List<TypeParameterDescriptorImpl> resolveTypeParametersForDescriptor(
            DeclarationDescriptor containingDescriptor,
            LexicalScope scopeForAnnotationsResolve,
            List<CjTypeParameter> typeParameters,
            BindingTrace trace
    ) {
        assert containingDescriptor instanceof FunctionDescriptor ||
//                containingDescriptor instanceof PropertyDescriptor ||
                containingDescriptor instanceof TypeAliasDescriptor
                : "This method should be called for functions, properties, or type aliases, got " + containingDescriptor;

        List<TypeParameterDescriptorImpl> result = new ArrayList<>();
        for (int i = 0, typeParametersSize = typeParameters.size(); i < typeParametersSize; i++) {
            CjTypeParameter typeParameter = typeParameters.get(i);
            result.add(resolveTypeParameterForDescriptor(containingDescriptor, scopeForAnnotationsResolve, typeParameter, i, trace));
        }
        return result;
    }

    private TypeParameterDescriptorImpl resolveTypeParameterForDescriptor(
            DeclarationDescriptor containingDescriptor,
            LexicalScope scopeForAnnotationsResolve,
            CjTypeParameter typeParameter,
            int index,
            BindingTrace trace
    ) {
        if (typeParameter.getVariance() != Variance.INVARIANT) {
            trace.report(VARIANCE_ON_TYPE_PARAMETER_NOT_ALLOWED.on(typeParameter));
        }

        Annotations annotations =
                annotationResolver.resolveAnnotationsWithArguments(scopeForAnnotationsResolve, typeParameter.getModifierList(), trace);

        TypeParameterDescriptorImpl typeParameterDescriptor = TypeParameterDescriptorImpl.createForFurtherModification(
                containingDescriptor,
                annotations,
                false,   //                typeParameter.hasModifier(CjTokens.REIFIED_KEYWORD),
                typeParameter.getVariance(),
                CjPsiUtil.safeName(typeParameter.getName()),
                index,
                CangJieSourceElementKt.toSourceElement(typeParameter),
                type -> {
//                    if (!(containingDescriptor instanceof TypeAliasDescriptor)) {
//                        trace.report(Errors.CYCLIC_GENERIC_UPPER_BOUND.on(typeParameter));
//                    }
                    return null;
                },
                supertypeLoopsResolver,
                storageManager
        );
        trace.record(BindingContext.TYPE_PARAMETER, typeParameter, typeParameterDescriptor);
        return typeParameterDescriptor;
    }

//    @NotNull
//    public static ClassConstructorDescriptorImpl createAndRecordPrimaryConstructorForObject(
//            @Nullable CjPureClassOrObject object,
//            @NotNull ClassDescriptor classDescriptor,
//            @NotNull BindingTrace trace
//    ) {
//        ClassConstructorDescriptorImpl constructorDescriptor =
//                DescriptorFactory.createPrimaryConstructorForObject(classDescriptor, CangJieSourceElementCj.toSourceElement(object));
//        if (object instanceof PsiElement) {
//            CjPrimaryConstructor primaryConstructor = object.getPrimaryConstructor();
//            trace.record(CONSTRUCTOR, primaryConstructor != null ? primaryConstructor : (PsiElement)object, constructorDescriptor);
//        }
//        return constructorDescriptor;
//    }

    static final class UpperBoundCheckRequest {
        public final Name typeParameterName;
        public final CjTypeReference upperBound;
        public final CangJieType upperBoundType;

        UpperBoundCheckRequest(Name typeParameterName, CjTypeReference upperBound, CangJieType upperBoundType) {
            this.typeParameterName = typeParameterName;
            this.upperBound = upperBound;
            this.upperBoundType = upperBoundType;
        }
    }

//    public void resolveGenericBounds(
//            @NotNull CjTypeParameterListOwner declaration,
//            @NotNull DeclarationDescriptor descriptor,
//            LexicalScope scope,
//            List<TypeParameterDescriptorImpl> parameters,
//            BindingTrace trace
//    ) {
//        List<UpperBoundCheckRequest> upperBoundCheckRequests = Lists.newArrayList();
//
//        List<CjTypeParameter> typeParameters = declaration.getTypeParameters();
//        Map<Name, TypeParameterDescriptorImpl> parameterByName = new HashMap<>();
//        for (int i = 0; i < typeParameters.size(); i++) {
//            CjTypeParameter ktTypeParameter = typeParameters.get(i);
//            TypeParameterDescriptorImpl typeParameterDescriptor = parameters.get(i);
//
//            parameterByName.put(typeParameterDescriptor.getName(), typeParameterDescriptor);
//
//            CjTypeReference extendsBound = ktTypeParameter.getExtendsBound();
//            if (extendsBound != null) {
//                CangJieType type = typeResolver.resolveType(scope, extendsBound, trace, false);
//                typeParameterDescriptor.addUpperBound(type);
//                upperBoundCheckRequests.add(new UpperBoundCheckRequest(ktTypeParameter.getNameAsName(), extendsBound, type));
//            }
//        }
//        for (CjTypeConstraint constraint : declaration.getTypeConstraints()) {
//            CjSimpleNameExpression subjectTypeParameterName = constraint.getSubjectTypeParameterName();
//            if (subjectTypeParameterName == null) {
//                continue;
//            }
//            Name referencedName = subjectTypeParameterName.getReferencedNameAsName();
//            TypeParameterDescriptorImpl typeParameterDescriptor = parameterByName.get(referencedName);
//            CjTypeReference boundTypeReference = constraint.getBoundTypeReference();
//            CangJieType bound = null;
//            if (boundTypeReference != null) {
//                bound = typeResolver.resolveType(scope, boundTypeReference, trace, false);
//                upperBoundCheckRequests.add(new UpperBoundCheckRequest(referencedName, boundTypeReference, bound));
//            }
//
//            if (typeParameterDescriptor != null) {
//                trace.record(BindingContext.REFERENCE_TARGET, subjectTypeParameterName, typeParameterDescriptor);
//                if (bound != null) {
//                    typeParameterDescriptor.addUpperBound(bound);
//                }
//            }
//        }
//
//        for (TypeParameterDescriptorImpl parameter : parameters) {
//            parameter.addDefaultUpperBound();
//            parameter.setInitialized();
//        }
//
//        for (TypeParameterDescriptorImpl parameter : parameters) {
//            checkConflictingUpperBounds(trace, parameter, typeParameters.get(parameter.getIndex()));
//        }
//
//        if (!(declaration instanceof CjClass)) {
//            checkUpperBoundTypes(trace, upperBoundCheckRequests, declaration.hasModifier(CjTokens.OVERRIDE_KEYWORD));
//            checkNamesInConstraints(declaration, descriptor, scope, trace);
//        }
//    }

//    public static void checkUpperBoundTypes(
//            @NotNull BindingTrace trace,
//            @NotNull List<UpperBoundCheckRequest> requests,
//            boolean hasOverrideModifier
//    ) {
//        if (requests.isEmpty()) return;
//
//        Set<Name> classBoundEncountered = new HashSet<>();
//        Set<Pair<Name, TypeConstructor>> allBounds = new HashSet<>();
//
//        for (UpperBoundCheckRequest request : requests) {
//            Name typeParameterName = request.typeParameterName;
//            CangJieType upperBound = request.upperBoundType;
//            CjTypeReference upperBoundElement = request.upperBound;
//
//            if (!CangJieTypeCj.isError(upperBound)) {
//                if (!allBounds.add(new Pair<>(typeParameterName, upperBound.getConstructor()))) {
//                    trace.report(REPEATED_BOUND.on(upperBoundElement));
//                }
//                else {
//                    ClassDescriptor classDescriptor = TypeUtils.getClassDescriptor(upperBound);
//                    if (classDescriptor != null) {
//                        ClassKind kind = classDescriptor.getKind();
//                        if (kind == ClassKind.CLASS || kind == ClassKind.ENUM_CLASS || kind == ClassKind.OBJECT) {
//                            if (!classBoundEncountered.add(typeParameterName)) {
//                                trace.report(ONLY_ONE_CLASS_BOUND_ALLOWED.on(upperBoundElement));
//                            }
//                        }
//                    }
//                }
//            }
//
//            checkUpperBoundType(upperBoundElement, upperBound, trace, hasOverrideModifier);
//        }
//    }

//    public static void checkConflictingUpperBounds(
//            @NotNull BindingTrace trace,
//            @NotNull TypeParameterDescriptor parameter,
//            @NotNull CjTypeParameter typeParameter
//    ) {
//        if (CangJieBuiltIns.isNothing(TypeIntersector.getUpperBoundsAsType(parameter))) {
//            trace.report(CONFLICTING_UPPER_BOUNDS.on(typeParameter, parameter));
//        }
//    }

//    public void checkNamesInConstraints(
//            @NotNull CjTypeParameterListOwner declaration,
//            @NotNull DeclarationDescriptor descriptor,
//            @NotNull LexicalScope scope,
//            @NotNull BindingTrace trace
//    ) {
//        for (CjTypeConstraint constraint : declaration.getTypeConstraints()) {
//            CjSimpleNameExpression nameExpression = constraint.getSubjectTypeParameterName();
//            if (nameExpression == null) continue;
//
//            Name name = nameExpression.getReferencedNameAsName();
//
//            ClassifierDescriptor classifier = ScopeUtilsCj.findClassifier(scope, name, NoLookupLocation.FOR_NON_TRACKED_SCOPE);
//            if (classifier instanceof TypeParameterDescriptor && classifier.getContainingDeclaration() == descriptor) continue;
//
//            if (classifier != null) {
//                // To tell the user that we look only for locally defined type parameters
//                trace.report(NAME_IN_CONSTRAINT_IS_NOT_A_TYPE_PARAMETER.on(nameExpression, constraint, declaration));
//                trace.record(BindingContext.REFERENCE_TARGET, nameExpression, classifier);
//            }
//            else {
//                trace.report(UNRESOLVED_REFERENCE.on(nameExpression, nameExpression));
//            }
//
//            CjTypeReference boundTypeReference = constraint.getBoundTypeReference();
//            if (boundTypeReference != null) {
//                typeResolver.resolveType(scope, boundTypeReference, trace, true);
//            }
//        }
//    }
//
//    public static void checkUpperBoundType(
//            CjTypeReference upperBound,
//            @NotNull CangJieType upperBoundType,
//            BindingTrace trace,
//            boolean hasOverrideModifier
//    ) {
//        if (!hasOverrideModifier && !TypeUtils.canHaveSubtypes(CangJieTypeChecker.DEFAULT, upperBoundType)) {
//            trace.report(FINAL_UPPER_BOUND.on(upperBound, upperBoundType));
//        }
//        if (DynamicTypesCj.isDynamic(upperBoundType)) {
//            trace.report(DYNAMIC_UPPER_BOUND.on(upperBound));
//        }
//        if (FunctionTypesCj.isExtensionFunctionType(upperBoundType)) {
//            trace.report(UPPER_BOUND_IS_EXTENSION_FUNCTION_TYPE.on(upperBound));
//        }
//        if (DefinitelyNonNullableTypesCj.containsIncorrectExplicitDefinitelyNonNullableType(upperBoundType)) {
//            trace.report(INCORRECT_LEFT_COMPONENT_OF_INTERSECTION.on(upperBound));
//        }
//    }
//
//    @NotNull
//    public VariableDescriptor resolveLocalVariableDescriptor(
//            @NotNull LexicalScope scope,
//            @NotNull CjParameter parameter,
//            BindingTrace trace
//    ) {
//        CangJieType type = resolveParameterType(scope, parameter, trace);
//        return resolveLocalVariableDescriptor(parameter, type, trace, scope);
//    }

//    private CangJieType resolveParameterType(LexicalScope scope, CjParameter parameter, BindingTrace trace) {
//        CjTypeReference typeReference = parameter.getTypeReference();
//        CangJieType type;
//        if (typeReference != null) {
//            type = typeResolver.resolveType(scope, typeReference, trace, true);
//        }
//        else {
//            // Error is reported by the parser
//            type = ErrorUtils.createErrorType(ErrorTypeKind.NO_TYPE_SPECIFIED, parameter.getText());
//        }
//        if (parameter.hasModifier(VARARG_KEYWORD)) {
//            return getVarargParameterType(type);
//        }
//        return type;
//    }

//    public VariableDescriptor resolveLocalVariableDescriptor(
//            @NotNull CjParameter parameter,
//            @NotNull CangJieType type,
//            BindingTrace trace,
//            @NotNull LexicalScope scope
//    ) {
//        UnwrappedType approximatedType = typeApproximator.approximateDeclarationType(type, true);
//        VariableDescriptor variableDescriptor = new LocalVariableDescriptor(
//                scope.getOwnerDescriptor(),
//                annotationResolver.resolveAnnotationsWithArguments(scope, parameter.getModifierList(), trace),
//                CjPsiUtil.safeName(parameter.getName()),
//                approximatedType,
//                CangJieSourceElementCj.toSourceElement(parameter)
//        );
//        trace.record(BindingContext.VALUE_PARAMETER, parameter, variableDescriptor);
//        // Type annotations also should be resolved
//        ForceResolveUtil.forceResolveAllContents(type.getAnnotations());
//        return variableDescriptor;
//    }

    @NotNull
    public TypeAliasDescriptor resolveTypeAliasDescriptor(
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull LexicalScope scope,
            @NotNull CjTypeAlias typeAlias,
            @NotNull BindingTrace trace
    ) {

        throw new RuntimeException("not implemented");
//        if (!(containingDeclaration instanceof PackageFragmentDescriptor) &&
//                !(containingDeclaration instanceof ScriptDescriptor)) {
//            trace.report(TOPLEVEL_TYPEALIASES_ONLY.on(typeAlias));
//        }
//
//        CjModifierList modifierList = typeAlias.getModifierList();
//        DescriptorVisibility visibility = resolveVisibilityFromModifiers(typeAlias, getDefaultVisibility(typeAlias, containingDeclaration));
//
//        Annotations allAnnotations = annotationResolver.resolveAnnotationsWithArguments(scope, modifierList, trace);
//        Name name = CjPsiUtil.safeName(typeAlias.getName());
//        SourceElement sourceElement = CangJieSourceElementKt.toSourceElement(typeAlias);
//        LazyTypeAliasDescriptor typeAliasDescriptor = LazyTypeAliasDescriptor.create(
//                storageManager, trace, containingDeclaration, allAnnotations, name, sourceElement, visibility);
//
//        List<TypeParameterDescriptorImpl> typeParameterDescriptors;
//        LexicalScope scopeWithTypeParameters;
//        {
//            List<CjTypeParameter> typeParameters = typeAlias.getTypeParameters();
//            if (typeParameters.isEmpty()) {
//                scopeWithTypeParameters = scope;
//                typeParameterDescriptors = Collections.emptyList();
//            }
//            else {
//                LexicalWritableScope writableScope = new LexicalWritableScope(
//                        scope, containingDeclaration, false, new TraceBasedLocalRedeclarationChecker(trace, overloadChecker),
//                        LexicalScopeKind.TYPE_ALIAS_HEADER);
//                typeParameterDescriptors = resolveTypeParametersForDescriptor(
//                        typeAliasDescriptor, writableScope, scope, typeParameters, trace);
//                writableScope.freeze();
//                checkNoGenericBoundsOnTypeAliasParameters(typeAlias, trace);
//                resolveGenericBounds(typeAlias, typeAliasDescriptor, writableScope, typeParameterDescriptors, trace);
//                scopeWithTypeParameters = writableScope;
//            }
//        }
//
//        CjTypeReference typeReference = typeAlias.getTypeReference();
//        if (typeReference == null) {
//            typeAliasDescriptor.initialize(
//                    typeParameterDescriptors,
//                    ErrorUtils.createErrorType(ErrorTypeKind.UNRESOLVED_TYPE_ALIAS, name.asString()),
//                    ErrorUtils.createErrorType(ErrorTypeKind.UNRESOLVED_TYPE_ALIAS, name.asString()));
//        }
//        else if (!languageVersionSettings.supportsFeature(LanguageFeature.TypeAliases)) {
//            typeResolver.resolveAbbreviatedType(scopeWithTypeParameters, typeReference, trace);
//            PsiElement typeAliasKeyword = typeAlias.getTypeAliasKeyword();
//            trace.report(UNSUPPORTED_FEATURE.on(typeAliasKeyword != null ? typeAliasKeyword : typeAlias,
//                    TuplesCj.to(LanguageFeature.TypeAliases, languageVersionSettings)));
//            typeAliasDescriptor.initialize(
//                    typeParameterDescriptors,
//                    ErrorUtils.createErrorType(ErrorTypeKind.UNRESOLVED_TYPE_ALIAS, name.asString()),
//                    ErrorUtils.createErrorType(ErrorTypeKind.UNRESOLVED_TYPE_ALIAS, name.asString()));
//        }
//        else {
//            typeAliasDescriptor.initialize(
//                    typeParameterDescriptors,
//                    storageManager.createRecursionTolerantLazyValue(
//                            () -> typeResolver.resolveAbbreviatedType(scopeWithTypeParameters, typeReference, trace),
//                            ErrorUtils.createErrorType(ErrorTypeKind.RECURSIVE_TYPE_ALIAS, typeAliasDescriptor.getName().asString())
//                    ),
//                    storageManager.createRecursionTolerantLazyValue(
//                            () -> typeResolver.resolveExpandedTypeForTypeAlias(typeAliasDescriptor),
//                            ErrorUtils.createErrorType(ErrorTypeKind.RECURSIVE_TYPE_ALIAS, typeAliasDescriptor.getName().asString())
//                    )
//            );
//        }
//
//        trace.record(TYPE_ALIAS, typeAlias, typeAliasDescriptor);
//        return typeAliasDescriptor;
    }

//    private static void checkNoGenericBoundsOnTypeAliasParameters(@NotNull CjTypeAlias typeAlias, @NotNull BindingTrace trace) {
//        for (CjTypeParameter typeParameter : typeAlias.getTypeParameters()) {
//            CjTypeReference bound = typeParameter.getExtendsBound();
//            if (bound != null) {
//                trace.report(BOUND_ON_TYPE_ALIAS_PARAMETER_NOT_ALLOWED.on(bound));
//            }
//        }
//    }
//
//    @NotNull
//    public PropertyDescriptor resolveDestructuringDeclarationEntryAsProperty(
//            @NotNull DeclarationDescriptor containingDeclaration,
//            @NotNull LexicalScope scopeForDeclarationResolution,
//            @NotNull LexicalScope scopeForInitializerResolution,
//            @NotNull CjDestructuringDeclarationEntry entry,
//            @NotNull BindingTrace trace,
//            @NotNull DataFlowInfo dataFlowInfo,
//            @NotNull InferenceSession inferenceSession
//    ) {
//        CjDestructuringDeclaration destructuringDeclaration = (CjDestructuringDeclaration) entry.getParent();
//        CjExpression initializer = destructuringDeclaration.getInitializer();
//
//        ExpressionTypingContext context = ExpressionTypingContext.newContext(
//                trace, scopeForDeclarationResolution, dataFlowInfo, TypeUtils.NO_EXPECTED_TYPE,
//                languageVersionSettings, dataFlowValueFactory, inferenceSession
//        );
//
//        ExpressionReceiver receiver = createReceiverForDestructuringDeclaration(destructuringDeclaration, context);
//
//        int componentIndex = destructuringDeclaration.getEntries().indexOf(entry);
//        CangJieType componentType = destructuringDeclarationResolver.resolveInitializer(entry, receiver, initializer, context, componentIndex);
//
//        return resolveAsPropertyDescriptor(
//                containingDeclaration,
//                scopeForDeclarationResolution,
//                scopeForInitializerResolution,
//                entry,
//                trace,
//                dataFlowInfo,
//                inferenceSession,
//                VariableAsPropertyInfo.Companion.createFromDestructuringDeclarationEntry(componentType));
//    }

//    private ExpressionReceiver createReceiverForDestructuringDeclaration(
//            @NotNull CjDestructuringDeclaration destructuringDeclaration,
//            @NotNull ExpressionTypingContext context
//    ) {
//        CjExpression initializer = destructuringDeclaration.getInitializer();
//        if (initializer == null) return null;
//
//        CangJieType initializerType = expressionTypingServices.getTypeInfo(initializer, context).getType();
//        if (initializerType == null) return null;
//
//        return ExpressionReceiver.Companion.create(initializer, initializerType, context.trace.getBindingContext());
//    }

//    @NotNull
//    public PropertyDescriptor resolvePropertyDescriptor(
//            @NotNull DeclarationDescriptor containingDeclaration,
//            @NotNull LexicalScope scopeForDeclarationResolution,
//            @NotNull LexicalScope scopeForInitializerResolution,
//            @NotNull CjProperty property,
//            @NotNull BindingTrace trace,
//            @NotNull DataFlowInfo dataFlowInfo,
//            @NotNull InferenceSession inferenceSession
//    ) {
//        return resolveAsPropertyDescriptor(
//                containingDeclaration,
//                scopeForDeclarationResolution,
//                scopeForInitializerResolution,
//                property,
//                trace,
//                dataFlowInfo,
//                inferenceSession,
//                VariableAsPropertyInfo.Companion.createFromProperty(property));
//    }
//
//    @NotNull
//    private PropertyDescriptor resolveAsPropertyDescriptor(
//            @NotNull DeclarationDescriptor container,
//            @NotNull LexicalScope scopeForDeclarationResolution,
//            @NotNull LexicalScope scopeForInitializerResolution,
//            @NotNull CjVariableDeclaration variableDeclaration,
//            @NotNull BindingTrace trace,
//            @NotNull DataFlowInfo dataFlowInfo,
//            @Nullable InferenceSession inferenceSession,
//            @NotNull VariableAsPropertyInfo propertyInfo
//    ) {
//        CjModifierList modifierList = variableDeclaration.getModifierList();
//        boolean isVar = variableDeclaration.isVar();
//
//        DescriptorVisibility visibility = resolveVisibilityFromModifiers(variableDeclaration, getDefaultVisibility(variableDeclaration, container));
//        Modality modality = container instanceof ClassDescriptor
//                ? resolveMemberModalityFromModifiers(variableDeclaration,
//                getDefaultModality(container, visibility, propertyInfo.getHasBody()),
//                trace.getBindingContext(), container)
//                : Modality.FINAL;
//
//        Annotations allAnnotations = annotationResolver.resolveAnnotationsWithoutArguments(scopeForDeclarationResolution, modifierList, trace);
//        Set<AnnotationUseSiteTarget> targetSet = EnumSet.of(PROPERTY, PROPERTY_GETTER, FIELD);
//        if (isVar) {
//            targetSet.add(PROPERTY_SETTER);
//            targetSet.add(SETTER_PARAMETER);
//        }
//        if (variableDeclaration instanceof CjProperty && ((CjProperty) variableDeclaration).hasDelegate()) {
//            targetSet.add(PROPERTY_DELEGATE_FIELD);
//        }
//        AnnotationSplitter annotationSplitter = new AnnotationSplitter(storageManager, allAnnotations, targetSet);
//
//        Annotations propertyAnnotations = new CompositeAnnotations(CollectionsCj.listOf(
//                annotationSplitter.getAnnotationsForTarget(PROPERTY),
//                annotationSplitter.getOtherAnnotations())
//        );
//
//        PropertyDescriptorImpl propertyDescriptor = PropertyDescriptorImpl.create(
//                container,
//                propertyAnnotations,
//                modality,
//                visibility,
//                isVar,
//                CjPsiUtil.safeName(variableDeclaration.getName()),
//                CallableMemberDescriptor.Kind.DECLARATION,
//                CangJieSourceElementCj.toSourceElement(variableDeclaration),
//                modifierList != null && modifierList.hasModifier(CjTokens.LATEINIT_KEYWORD),
//                modifierList != null && modifierList.hasModifier(CjTokens.CONST_KEYWORD),
//                modifierList != null && PsiUtilsCj.hasExpectModifier(modifierList) && container instanceof PackageFragmentDescriptor ||
//                        container instanceof ClassDescriptor && ((ClassDescriptor) container).isExpect(),
//                modifierList != null && PsiUtilsCj.hasActualModifier(modifierList),
//                modifierList != null && modifierList.hasModifier(CjTokens.EXTERNAL_KEYWORD),
//                propertyInfo.getHasDelegate()
//        );
//
//        List<TypeParameterDescriptorImpl> typeParameterDescriptors;
//        LexicalScope scopeForDeclarationResolutionWithTypeParameters;
//        LexicalScope scopeForInitializerResolutionWithTypeParameters;
//        CangJieType receiverType = null;
//
//        {
//            List<CjTypeParameter> typeParameters = variableDeclaration.getTypeParameters();
//            if (typeParameters.isEmpty()) {
//                scopeForDeclarationResolutionWithTypeParameters = scopeForDeclarationResolution;
//                scopeForInitializerResolutionWithTypeParameters = scopeForInitializerResolution;
//                typeParameterDescriptors = Collections.emptyList();
//            }
//            else {
//                LexicalWritableScope writableScopeForDeclarationResolution = new LexicalWritableScope(
//                        scopeForDeclarationResolution, container, false, new TraceBasedLocalRedeclarationChecker(trace, overloadChecker),
//                        LexicalScopeKind.PROPERTY_HEADER);
//                LexicalWritableScope writableScopeForInitializerResolution = new LexicalWritableScope(
//                        scopeForInitializerResolution, container, false, LocalRedeclarationChecker.DO_NOTHING.INSTANCE,
//                        LexicalScopeKind.PROPERTY_HEADER);
//                typeParameterDescriptors = resolveTypeParametersForDescriptor(
//                        propertyDescriptor,
//                        scopeForDeclarationResolution, typeParameters, trace);
//                for (TypeParameterDescriptor descriptor : typeParameterDescriptors) {
//                    writableScopeForDeclarationResolution.addClassifierDescriptor(descriptor);
//                    writableScopeForInitializerResolution.addClassifierDescriptor(descriptor);
//                }
//                writableScopeForDeclarationResolution.freeze();
//                writableScopeForInitializerResolution.freeze();
//                resolveGenericBounds(variableDeclaration, propertyDescriptor, writableScopeForDeclarationResolution, typeParameterDescriptors, trace);
//                scopeForDeclarationResolutionWithTypeParameters = writableScopeForDeclarationResolution;
//                scopeForInitializerResolutionWithTypeParameters = writableScopeForInitializerResolution;
//            }
//        }
//
//        CjTypeReference receiverTypeRef = variableDeclaration.getReceiverTypeReference();
//        ReceiverParameterDescriptor receiverDescriptor = null;
//        if (receiverTypeRef != null) {
//            receiverType = typeResolver.resolveType(scopeForDeclarationResolutionWithTypeParameters, receiverTypeRef, trace, true);
//            AnnotationSplitter splitter = new AnnotationSplitter(storageManager, receiverType.getAnnotations(), EnumSet.of(RECEIVER));
//            receiverDescriptor = DescriptorFactory.createExtensionReceiverParameterForCallable(
//                    propertyDescriptor, receiverType, splitter.getAnnotationsForTarget(RECEIVER)
//            );
//        }
//
//        List<CjContextReceiver> contextReceivers = variableDeclaration.getContextReceivers();
//        List<ReceiverParameterDescriptor> contextReceiverDescriptors = IntStream.range(0, contextReceivers.size()).mapToObj(index -> {
//            CjContextReceiver contextReceiver = contextReceivers.get(index);
//            CjTypeReference typeReference = contextReceiver.typeReference();
//            if (typeReference == null) {
//                return null;
//            }
//            CangJieType type = typeResolver.resolveType(scopeForDeclarationResolutionWithTypeParameters, typeReference, trace, true);
//            AnnotationSplitter splitter = new AnnotationSplitter(storageManager, type.getAnnotations(), EnumSet.of(RECEIVER));
//            return DescriptorFactory.createContextReceiverParameterForCallable(
//                    propertyDescriptor, type, contextReceiver.labelNameAsName(), splitter.getAnnotationsForTarget(RECEIVER), index
//            );
//        }).collect(Collectors.toList());
//
//        if (languageVersionSettings.supportsFeature(LanguageFeature.ContextReceivers)) {
//            Multimap<String, ReceiverParameterDescriptor> nameToReceiverMap = HashMultimap.create();
//            if (receiverTypeRef != null) {
//                String receiverName = receiverTypeRef.nameForReceiverLabel();
//                if (receiverName != null) {
//                    nameToReceiverMap.put(receiverName, receiverDescriptor);
//                }
//            }
//            for (int i = 0; i < contextReceivers.size(); i++) {
//                String contextReceiverName = contextReceivers.get(i).name();
//                if (contextReceiverName != null) {
//                    nameToReceiverMap.put(contextReceiverName, contextReceiverDescriptors.get(i));
//                }
//            }
//            trace.record(DESCRIPTOR_TO_CONTEXT_RECEIVER_MAP, propertyDescriptor, nameToReceiverMap);
//        }
//
//        LexicalScope scopeForInitializer = ScopeUtils.makeScopeForPropertyInitializer(scopeForInitializerResolutionWithTypeParameters, propertyDescriptor);
//        CangJieType propertyType = propertyInfo.getVariableType();
//        CangJieType typeIfKnown = propertyType != null ? propertyType : variableTypeAndInitializerResolver.resolveTypeNullable(
//                propertyDescriptor, scopeForInitializer,
//                variableDeclaration, dataFlowInfo, inferenceSession,
//                trace, /* local = */ false
//        );
//
//        PropertyGetterDescriptorImpl getter = resolvePropertyGetterDescriptor(
//                scopeForDeclarationResolutionWithTypeParameters,
//                variableDeclaration,
//                propertyDescriptor,
//                annotationSplitter,
//                trace,
//                typeIfKnown,
//                propertyInfo.getPropertyGetter(),
//                propertyInfo.getHasDelegate(),
//                inferenceSession
//        );
//
//        CangJieType type = typeIfKnown != null ? typeIfKnown : getter.getReturnType();
//
//        assert type != null : "At least getter type must be initialized via resolvePropertyGetterDescriptor";
//
//        variableTypeAndInitializerResolver.setConstantForVariableIfNeeded(
//                propertyDescriptor, scopeForInitializer, variableDeclaration, dataFlowInfo, type, inferenceSession, trace
//        );
//
//        propertyDescriptor.setType(type, typeParameterDescriptors, getDispatchReceiverParameterIfNeeded(container), receiverDescriptor,
//                contextReceiverDescriptors);
//
//        PropertySetterDescriptor setter = resolvePropertySetterDescriptor(
//                scopeForDeclarationResolutionWithTypeParameters,
//                variableDeclaration,
//                propertyDescriptor,
//                annotationSplitter,
//                trace,
//                propertyInfo.getPropertySetter(),
//                propertyInfo.getHasDelegate(),
//                inferenceSession
//        );
//
//        propertyDescriptor.initialize(
//                getter, setter,
//                new FieldDescriptorImpl(annotationSplitter.getAnnotationsForTarget(FIELD), propertyDescriptor),
//                new FieldDescriptorImpl(annotationSplitter.getAnnotationsForTarget(PROPERTY_DELEGATE_FIELD), propertyDescriptor)
//        );
//        trace.record(BindingContext.VARIABLE, variableDeclaration, propertyDescriptor);
//        return propertyDescriptor;
//    }
//
//    @NotNull
//    /*package*/ static CangJieType transformAnonymousTypeIfNeeded(
//            @NotNull DeclarationDescriptorWithVisibility descriptor,
//            @NotNull CjDeclaration declaration,
//            @NotNull CangJieType type,
//            @NotNull BindingTrace trace,
//            @NotNull Iterable<DeclarationSignatureAnonymousTypeTransformer> anonymousTypeTransformers,
//            @NotNull LanguageVersionSettings languageVersionSettings
//    ) {
//        for (DeclarationSignatureAnonymousTypeTransformer transformer : anonymousTypeTransformers) {
//            CangJieType transformedType = transformer.transformAnonymousType(descriptor, type);
//            if (transformedType != null) {
//                return transformedType;
//            }
//        }
//
//        ClassifierDescriptor classifier = type.getConstructor().getDeclarationDescriptor();
//        if (classifier == null || !DescriptorUtils.isAnonymousObject(classifier) || DescriptorUtils.isLocal(descriptor)) {
//            return type;
//        }
//
//        boolean isPrivate = DescriptorVisibilities.isPrivate(descriptor.getVisibility());
//        boolean isInlineFunction = descriptor instanceof SimpleFunctionDescriptor && ((SimpleFunctionDescriptor) descriptor).isInline();
//        boolean isAnonymousReturnTypesInPrivateInlineFunctionsForbidden =
//                languageVersionSettings.supportsFeature(LanguageFeature.ApproximateAnonymousReturnTypesInPrivateInlineFunctions);
//
//        if (!isPrivate || (isInlineFunction && isAnonymousReturnTypesInPrivateInlineFunctionsForbidden)) {
//            if (type.getConstructor().getSupertypes().size() == 1) {
//                CangJieType approximatingSuperType = type.getConstructor().getSupertypes().iterator().next();
//                CangJieType substitutedSuperType;
//                MemberScope memberScope = type.getMemberScope();
//
//                if (memberScope instanceof SubstitutingScope) {
//                    substitutedSuperType = ((SubstitutingScope) memberScope).substitute(approximatingSuperType);
//                } else {
//                    substitutedSuperType = approximatingSuperType;
//                }
//
//                UnwrappedType unwrapped = type.unwrap();
//                boolean lowerNullable = FlexibleTypesCj.lowerIfFlexible(unwrapped).isMarkedNullable();
//                boolean upperNullable = FlexibleTypesCj.upperIfFlexible(unwrapped).isMarkedNullable();
//                if (languageVersionSettings.supportsFeature(LanguageFeature.KeepNullabilityWhenApproximatingLocalType)) {
//                    if (lowerNullable != upperNullable) {
//                        return CangJieTypeFactory.flexibleType(
//                                FlexibleTypesCj.lowerIfFlexible(substitutedSuperType),
//                                FlexibleTypesCj.upperIfFlexible(substitutedSuperType).makeNullableAsSpecified(true));
//                    }
//                    return TypeUtils.makeNullableIfNeeded(substitutedSuperType, upperNullable);
//                } else if (upperNullable) {
//                    if (lowerNullable) {
//                        trace.report(APPROXIMATED_LOCAL_TYPE_WILL_BECOME_NULLABLE.on(declaration, substitutedSuperType));
//                    } else {
//                        trace.report(APPROXIMATED_LOCAL_TYPE_WILL_BECOME_FLEXIBLE.on(declaration, substitutedSuperType));
//                    }
//                }
//                return substitutedSuperType;
//            }
//            else {
//                trace.report(AMBIGUOUS_ANONYMOUS_TYPE_INFERRED.on(declaration, type.getConstructor().getSupertypes()));
//            }
//        }
//
//        return type;
//    }
//
//    @Nullable
//    private PropertySetterDescriptor resolvePropertySetterDescriptor(
//            @NotNull LexicalScope scopeWithTypeParameters,
//            @NotNull CjVariableDeclaration property,
//            @NotNull PropertyDescriptor propertyDescriptor,
//            @NotNull AnnotationSplitter annotationSplitter,
//            @NotNull BindingTrace trace,
//            @Nullable CjPropertyAccessor setter,
//            boolean hasDelegate,
//            @Nullable InferenceSession inferenceSession
//    ) {
//        PropertySetterDescriptorImpl setterDescriptor = null;
//        Annotations setterTargetedAnnotations = annotationSplitter.getAnnotationsForTarget(PROPERTY_SETTER);
//        Annotations parameterTargetedAnnotations = annotationSplitter.getAnnotationsForTarget(SETTER_PARAMETER);
//        if (setter != null) {
//            Annotations annotations = new CompositeAnnotations(CollectionsCj.listOf(
//                    setterTargetedAnnotations,
//                    annotationResolver.resolveAnnotationsWithoutArguments(scopeWithTypeParameters, setter.getModifierList(), trace)
//            ));
//            CjParameter parameter = setter.getParameter();
//
//            setterDescriptor = new PropertySetterDescriptorImpl(
//                    propertyDescriptor, annotations,
//                    resolveMemberModalityFromModifiers(setter, propertyDescriptor.getModality(),
//                            trace.getBindingContext(), propertyDescriptor.getContainingDeclaration()),
//                    resolveVisibilityFromModifiers(setter, propertyDescriptor.getVisibility()),
//                    /* isDefault = */ false, setter.hasModifier(EXTERNAL_KEYWORD),
//                    property.hasModifier(CjTokens.INLINE_KEYWORD) || setter.hasModifier(CjTokens.INLINE_KEYWORD),
//                    CallableMemberDescriptor.Kind.DECLARATION, null, CangJieSourceElementCj.toSourceElement(setter)
//            );
//            CjTypeReference returnTypeReference = setter.getReturnTypeReference();
//            if (returnTypeReference != null) {
//                CangJieType returnType = typeResolver.resolveType(scopeWithTypeParameters, returnTypeReference, trace, true);
//                if (!CangJieBuiltIns.isUnit(returnType)) {
//                    trace.report(WRONG_SETTER_RETURN_TYPE.on(returnTypeReference));
//                }
//            }
//
//            if (parameter != null) {
//
//                // This check is redundant: the parser does not allow a default value, but we'll keep it just in case
//                if (parameter.hasDefaultValue()) {
//                    trace.report(SETTER_PARAMETER_WITH_DEFAULT_VALUE.on(parameter.getDefaultValue()));
//                }
//
//                CangJieType type;
//                CjTypeReference typeReference = parameter.getTypeReference();
//                if (typeReference == null) {
//                    type = propertyDescriptor.getType(); // TODO : this maybe unknown at this point
//                }
//                else {
//                    type = typeResolver.resolveType(scopeWithTypeParameters, typeReference, trace, true);
//                    CangJieType inType = propertyDescriptor.getType();
//                    if (!TypeUtils.equalTypes(type, inType)) {
//                        trace.report(WRONG_SETTER_PARAMETER_TYPE.on(typeReference, inType, type));
//                    }
//                }
//
//                ValueParameterDescriptorImpl valueParameterDescriptor = resolveValueParameterDescriptor(
//                        scopeWithTypeParameters, setterDescriptor, parameter, 0, type, trace, parameterTargetedAnnotations, inferenceSession
//                );
//                setterDescriptor.initialize(valueParameterDescriptor);
//            }
//            else {
//                setterDescriptor.initializeDefault();
//            }
//
//            trace.record(BindingContext.PROPERTY_ACCESSOR, setter, setterDescriptor);
//        }
//        else if (property.isVar()) {
//            setterDescriptor = DescriptorFactory.createSetter(
//                    propertyDescriptor, setterTargetedAnnotations, parameterTargetedAnnotations,
//                    !hasDelegate && setterTargetedAnnotations.isEmpty() && parameterTargetedAnnotations.isEmpty(),
//                    false, property.hasModifier(CjTokens.INLINE_KEYWORD),
//                    propertyDescriptor.getSource()
//            );
//        }
//
//        if (!property.isVar()) {
//            if (setter != null) {
//                trace.report(VAL_WITH_SETTER.on(setter));
//            }
//        }
//        return setterDescriptor;
//    }
//
//    @NotNull
//    private PropertyGetterDescriptorImpl resolvePropertyGetterDescriptor(
//            @NotNull LexicalScope scopeForDeclarationResolution,
//            @NotNull CjVariableDeclaration property,
//            @NotNull PropertyDescriptor propertyDescriptor,
//            @NotNull AnnotationSplitter annotationSplitter,
//            @NotNull BindingTrace trace,
//            @Nullable CangJieType propertyTypeIfKnown,
//            @Nullable CjPropertyAccessor getter,
//            boolean hasDelegate,
//            @Nullable InferenceSession inferenceSession
//    ) {
//        PropertyGetterDescriptorImpl getterDescriptor;
//        CangJieType getterType;
//        Annotations getterTargetedAnnotations = annotationSplitter.getAnnotationsForTarget(PROPERTY_GETTER);
//        if (getter != null) {
//            Annotations getterAnnotations = new CompositeAnnotations(CollectionsCj.listOf(
//                    getterTargetedAnnotations,
//                    annotationResolver.resolveAnnotationsWithoutArguments(scopeForDeclarationResolution, getter.getModifierList(), trace)
//            ));
//
//            getterDescriptor = new PropertyGetterDescriptorImpl(
//                    propertyDescriptor, getterAnnotations,
//                    resolveMemberModalityFromModifiers(getter, propertyDescriptor.getModality(),
//                            trace.getBindingContext(), propertyDescriptor.getContainingDeclaration()),
//                    resolveVisibilityFromModifiers(getter, propertyDescriptor.getVisibility()),
//                    /* isDefault = */ false, getter.hasModifier(EXTERNAL_KEYWORD),
//                    property.hasModifier(CjTokens.INLINE_KEYWORD) || getter.hasModifier(CjTokens.INLINE_KEYWORD),
//                    CallableMemberDescriptor.Kind.DECLARATION, null, CangJieSourceElementCj.toSourceElement(getter)
//            );
//            getterType = determineGetterReturnType(
//                    scopeForDeclarationResolution, trace, getterDescriptor, getter, propertyTypeIfKnown, inferenceSession
//            );
//        }
//        else {
//            getterDescriptor = DescriptorFactory.createGetter(
//                    propertyDescriptor, getterTargetedAnnotations,
//                    !hasDelegate && getterTargetedAnnotations.isEmpty(),
//                    /* isExternal = */ false, property.hasModifier(CjTokens.INLINE_KEYWORD)
//            );
//            getterType = propertyTypeIfKnown;
//        }
//
//        getterDescriptor.initialize(getterType != null ? getterType : VariableTypeAndInitializerResolver.getTypeForPropertyWithoutReturnType(propertyDescriptor.getName().asString()));
//
//        if (getter != null) {
//            trace.record(BindingContext.PROPERTY_ACCESSOR, getter, getterDescriptor);
//        }
//
//        return getterDescriptor;
//    }
//
//    @Nullable
//    private CangJieType determineGetterReturnType(
//            @NotNull LexicalScope scope,
//            @NotNull BindingTrace trace,
//            @NotNull PropertyGetterDescriptor getterDescriptor,
//            @NotNull CjPropertyAccessor getter,
//            @Nullable CangJieType propertyTypeIfKnown,
//            @Nullable InferenceSession inferenceSession
//    ) {
//        CjTypeReference returnTypeReference = getter.getReturnTypeReference();
//        if (returnTypeReference != null) {
//            CangJieType explicitReturnType = typeResolver.resolveType(scope, returnTypeReference, trace, true);
//            if (propertyTypeIfKnown != null && !TypeUtils.equalTypes(explicitReturnType, propertyTypeIfKnown)) {
//                trace.report(WRONG_GETTER_RETURN_TYPE.on(returnTypeReference, propertyTypeIfKnown, explicitReturnType));
//            }
//            return explicitReturnType;
//        }
//
//        // If a property has no type specified in the PSI but the getter does (or has an initializer e.g. "val x get() = ..."),
//        // infer the correct type for the getter but leave the error type for the property.
//        // This is useful for an IDE quick fix which would add the type to the property
//        CjProperty property = getter.getProperty();
//        if (!property.hasDelegateExpressionOrInitializer() && property.getTypeReference() == null &&
//                getter.hasBody() && !getter.hasBlockBody()) {
//            return inferReturnTypeFromExpressionBody(trace, scope, DataFlowInfoFactory.EMPTY, getter, getterDescriptor, inferenceSession);
//        }
//
//        return propertyTypeIfKnown;
//    }
//
//    @NotNull
//        /*package*/ CangJieType inferReturnTypeFromExpressionBody(
//            @NotNull BindingTrace trace,
//            @NotNull LexicalScope scope,
//            @NotNull DataFlowInfo dataFlowInfo,
//            @NotNull CjDeclarationWithBody function,
//            @NotNull FunctionDescriptor functionDescriptor,
//            @Nullable InferenceSession inferenceSession
//    ) {
//        return wrappedTypeFactory.createRecursionIntolerantDeferredType(trace, () -> {
//            PreliminaryDeclarationVisitor.Companion.createForDeclaration(function, trace, languageVersionSettings);
//            CangJieType type = expressionTypingServices.getBodyExpressionType(
//                    trace, scope, dataFlowInfo, function, functionDescriptor, inferenceSession
//            );
//            CangJieType publicType = transformAnonymousTypeIfNeeded(
//                    functionDescriptor, function, type, trace, anonymousTypeTransformers, languageVersionSettings
//            );
//            UnwrappedType approximatedType = typeApproximator.approximateDeclarationType(publicType, false);
//            CangJieType sanitizedType = declarationReturnTypeSanitizer.sanitizeReturnType(approximatedType, wrappedTypeFactory, trace, languageVersionSettings);
//            functionsTypingVisitor.checkTypesForReturnStatements(function, trace, sanitizedType);
//            return sanitizedType;
//        });
//    }

//    public PropertyDescriptor resolvePrimaryConstructorParameterToAProperty(
//            @NotNull ClassDescriptor classDescriptor,
//            @NotNull ValueParameterDescriptor valueParameter,
//            @NotNull LexicalScope scope,
//            @NotNull CjParameter parameter,
//            BindingTrace trace
//    ) {
//        CangJieType type = resolveParameterType(scope, parameter, trace);
//        Name name = parameter.getNameAsSafeName();
//        boolean isMutable = parameter.isMutable();
//        CjModifierList modifierList = parameter.getModifierList();
//
//        if (modifierList != null) {
//            if (modifierList.hasModifier(CjTokens.ABSTRACT_KEYWORD)) {
//                trace.report(ABSTRACT_PROPERTY_IN_PRIMARY_CONSTRUCTOR_PARAMETERS.on(parameter));
//            }
//        }
//
//        Annotations allAnnotations = annotationResolver.resolveAnnotationsWithoutArguments(scope, parameter.getModifierList(), trace);
//        Set<AnnotationUseSiteTarget> targetSet = EnumSet.of(PROPERTY, PROPERTY_GETTER, FIELD, CONSTRUCTOR_PARAMETER, PROPERTY_SETTER);
//        if (isMutable) {
//            targetSet.add(PROPERTY_SETTER);
//            targetSet.add(SETTER_PARAMETER);
//        }
//        AnnotationSplitter annotationSplitter = new AnnotationSplitter(storageManager, allAnnotations, targetSet);
//
//        Annotations propertyAnnotations = new CompositeAnnotations(
//                annotationSplitter.getAnnotationsForTarget(PROPERTY),
//                annotationSplitter.getOtherAnnotations()
//        );
//
//        PropertyDescriptorImpl propertyDescriptor = PropertyDescriptorImpl.create(
//                classDescriptor,
//                propertyAnnotations,
//                resolveMemberModalityFromModifiers(parameter, Modality.FINAL, trace.getBindingContext(), classDescriptor),
//                resolveVisibilityFromModifiers(parameter, getDefaultVisibility(parameter, classDescriptor)),
//                isMutable,
//                name,
//                CallableMemberDescriptor.Kind.DECLARATION,
//                CangJieSourceElementCj.toSourceElement(parameter),
//                false,
//                false,
//                classDescriptor.isExpect(),
//                modifierList != null && PsiUtilsCj.hasActualModifier(modifierList),
//                false,
//                false
//        );
//        propertyDescriptor.setType(type, Collections.emptyList(), getDispatchReceiverParameterIfNeeded(classDescriptor), null,
//                CollectionsKt.emptyList());
//
//        Annotations setterAnnotations = annotationSplitter.getAnnotationsForTarget(PROPERTY_SETTER);
//        Annotations getterAnnotations = new CompositeAnnotations(CollectionsCj.listOf(
//                annotationSplitter.getAnnotationsForTarget(PROPERTY_GETTER)));
//
//        PropertyGetterDescriptorImpl getter = DescriptorFactory.createDefaultGetter(propertyDescriptor, getterAnnotations);
//        PropertySetterDescriptor setter =
//                propertyDescriptor.isVar()
//                        ? DescriptorFactory.createDefaultSetter(
//                        propertyDescriptor, setterAnnotations, annotationSplitter.getAnnotationsForTarget(SETTER_PARAMETER)
//                )
//                        : null;
//
//        propertyDescriptor.initialize(
//                getter, setter,
//                new FieldDescriptorImpl(annotationSplitter.getAnnotationsForTarget(FIELD), propertyDescriptor),
//                null
//        );
//
//        getter.initialize(propertyDescriptor.getType());
//
//        trace.record(BindingContext.PRIMARY_CONSTRUCTOR_PARAMETER, parameter, propertyDescriptor);
//        trace.record(BindingContext.VALUE_PARAMETER_AS_PROPERTY, valueParameter, propertyDescriptor);
//        return propertyDescriptor;
//    }

//    public static boolean checkHasOuterClassInstance(
//            @NotNull LexicalScope scope,
//            @NotNull BindingTrace trace,
//            @NotNull PsiElement reportErrorsOn,
//            @NotNull ClassDescriptor target
//    ) {
//        ClassDescriptor classDescriptor = getContainingClass(scope);
//
//        if (!isInsideOuterClassOrItsSubclass(classDescriptor, target)) {
//            return true;
//        }
//
//        while (classDescriptor != null) {
//            if (isSubclass(classDescriptor, target)) {
//                return true;
//            }
//
//            if (isStaticNestedClass(classDescriptor)) {
//                PsiElement onReport = CallResolverUtilCj.reportOnElement(reportErrorsOn);
//                trace.report(INACCESSIBLE_OUTER_CLASS_EXPRESSION.on(onReport, classDescriptor));
//                return false;
//            }
//            classDescriptor = getParentOfType(classDescriptor, ClassDescriptor.class, true);
//        }
//        return true;
//    }

//    private static boolean isInsideOuterClassOrItsSubclass(@Nullable DeclarationDescriptor nested, @NotNull ClassDescriptor outer) {
//        if (nested == null) return false;
//
//        if (nested instanceof ClassDescriptor && isSubclass((ClassDescriptor) nested, outer)) return true;
//
//        return isInsideOuterClassOrItsSubclass(nested.getContainingDeclaration(), outer);
//    }

//    @Nullable
//    public static ClassDescriptor getContainingClass(@NotNull LexicalScope scope) {
//        return getParentOfType(scope.getOwnerDescriptor(), ClassDescriptor.class, false);
//    }
}
