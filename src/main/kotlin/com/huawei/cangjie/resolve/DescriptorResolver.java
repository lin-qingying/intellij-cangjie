package com.huawei.cangjie.resolve;

import com.google.common.collect.Lists;
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
import com.huawei.cangjie.name.ClassId;
import com.huawei.cangjie.name.Name;
import com.huawei.cangjie.name.SpecialNames;
import com.huawei.cangjie.psi.*;
import com.huawei.cangjie.resolve.calls.components.InferenceSession;
import com.huawei.cangjie.resolve.calls.smartcasts.DataFlowInfo;
import com.huawei.cangjie.resolve.calls.smartcasts.DataFlowValueFactory;
import com.huawei.cangjie.resolve.calls.util.CallResolverUtilKt;
import com.huawei.cangjie.resolve.calls.util.UnderscoreUtilKt;
import com.huawei.cangjie.resolve.descriptorUtil.DescriptorUtilsKt;
import com.huawei.cangjie.resolve.lazy.descriptors.LazyTypeAliasDescriptor;
import com.huawei.cangjie.resolve.scopes.*;
import com.huawei.cangjie.resolve.source.CangJieSourceElementKt;
import com.huawei.cangjie.storage.StorageManager;
import com.huawei.cangjie.types.*;
import com.huawei.cangjie.types.error.ErrorTypeKind;
import com.huawei.cangjie.types.expressions.ExpressionTypingServices;
import com.huawei.cangjie.types.util.TypeUtils;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import kotlin.Pair;
import kotlin.TuplesKt;
import kotlin.collections.SetsKt;
import kotlin.jvm.functions.Function0;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.IntStream;

import static com.huawei.cangjie.descriptors.Errors.*;
import static com.huawei.cangjie.descriptors.annotations.AnnotationUseSiteTarget.CONSTRUCTOR_PARAMETER;
import static com.huawei.cangjie.resolve.BindingContext.CONSTRUCTOR;
import static com.huawei.cangjie.resolve.BindingContext.TYPE_ALIAS;
import static com.huawei.cangjie.resolve.DescriptorUtils.*;
import static com.huawei.cangjie.resolve.ModifiersChecker.resolveMemberModalityFromModifiers;
import static com.huawei.cangjie.resolve.ModifiersChecker.resolveVisibilityFromModifiers;

public class DescriptorResolver {
    private final TypeResolver typeResolver;
    private final AnnotationResolver annotationResolver;
    private final StorageManager storageManager;
    private final CangJieBuiltIns builtIns;
    private final SupertypeLoopChecker supertypeLoopsResolver;
    private final VariableTypeAndInitializerResolver variableTypeAndInitializerResolver;
    private final ExpressionTypingServices expressionTypingServices;
    private final OverloadChecker overloadChecker;
    private final LanguageVersionSettings languageVersionSettings;
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
            @NotNull VariableTypeAndInitializerResolver variableTypeAndInitializerResolver,
            @NotNull ExpressionTypingServices expressionTypingServices,
            @NotNull OverloadChecker overloadChecker,
            @NotNull LanguageVersionSettings languageVersionSettings,
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
        this.variableTypeAndInitializerResolver = variableTypeAndInitializerResolver;
        this.expressionTypingServices = expressionTypingServices;
        this.overloadChecker = overloadChecker;
        this.languageVersionSettings = languageVersionSettings;
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

    @NotNull
    public static ClassConstructorDescriptorImpl createAndRecordPrimaryConstructorForObject(
            @Nullable CjPureTypeStatement object,
            @NotNull ClassDescriptor classDescriptor,
            @NotNull BindingTrace trace
    ) {
        ClassConstructorDescriptorImpl constructorDescriptor =
                DescriptorFactory.createPrimaryConstructorForObject(classDescriptor, CangJieSourceElementKt.toSourceElement(object));
        if (object instanceof PsiElement) {
            CjPrimaryConstructor primaryConstructor = object.getPrimaryConstructor();
            trace.record(CONSTRUCTOR, primaryConstructor != null ? primaryConstructor : (PsiElement)object, constructorDescriptor);
        }
        return constructorDescriptor;
    }

    private static boolean isInsideOuterClassOrItsSubclass(@Nullable DeclarationDescriptor nested, @NotNull ClassDescriptor outer) {
        if (nested == null) return false;

        if (nested instanceof ClassDescriptor && isSubclass((ClassDescriptor) nested, outer)) return true;

        return isInsideOuterClassOrItsSubclass(nested.getContainingDeclaration(), outer);
    }

    public static boolean checkHasOuterClassInstance(
            @NotNull LexicalScope scope,
            @NotNull BindingTrace trace,
            @NotNull PsiElement reportErrorsOn,
            @NotNull ClassDescriptor target
    ) {
        ClassDescriptor classDescriptor = getContainingClass(scope);

        if (!isInsideOuterClassOrItsSubclass(classDescriptor, target)) {
            return true;
        }

        while (classDescriptor != null) {
            if (isSubclass(classDescriptor, target)) {
                return true;
            }

            if (isStaticNestedClass(classDescriptor)) {
                PsiElement onReport = CallResolverUtilKt.reportOnElement(reportErrorsOn);
                trace.report(INACCESSIBLE_OUTER_CLASS_EXPRESSION.on(onReport, classDescriptor));
                return false;
            }
            classDescriptor = getParentOfType(classDescriptor, ClassDescriptor.class, true);
        }
        return true;
    }

    @Nullable
    public static ClassDescriptor getContainingClass(@NotNull LexicalScope scope) {
        return getParentOfType(scope.getOwnerDescriptor(), ClassDescriptor.class, false);
    }

    /**
     * @return true if descriptor is a class inside another class and does not have access to the outer class
     */
    public static boolean isStaticNestedClass(@NotNull DeclarationDescriptor descriptor) {
        DeclarationDescriptor containing = descriptor.getContainingDeclaration();
        return descriptor instanceof ClassDescriptor &&
                containing instanceof ClassDescriptor;
    }

    public static DescriptorVisibility getDefaultVisibility(CjModifierListOwner modifierListOwner, DeclarationDescriptor containingDescriptor) {
//        DescriptorVisibility defaultVisibility;
//        if (containingDescriptor instanceof ClassDescriptor) {
//            CjModifierList modifierList = modifierListOwner.getModifierList();
//            defaultVisibility =
//                    modifierList != null && modifierList.hasModifier(OVERRIDE_KEYWORD)
//                    ? DescriptorVisibilities.INHERITED
//                    :
//                    DescriptorVisibilities.DEFAULT_VISIBILITY;
//        } else if (containingDescriptor instanceof FunctionDescriptor /*|| containingDescriptor instanceof PropertyDescriptor*/) {
//            defaultVisibility = DescriptorVisibilities.LOCAL;
//        } else {
//            defaultVisibility = DescriptorVisibilities.DEFAULT_VISIBILITY;
//        }
//        return defaultVisibility;

        if (containingDescriptor instanceof ClassDescriptor && ((ClassDescriptor) containingDescriptor).getKind() == ClassKind.INTERFACE) {

            return DescriptorVisibilities.PUBLIC;
        }

        return DescriptorVisibilities.INTERNAL;
    }

    public static Modality getDefaultModality(DeclarationDescriptor containingDescriptor, DescriptorVisibility visibility, boolean isBodyPresent) {
        Modality defaultModality;
        if (containingDescriptor instanceof ClassDescriptor) {
            boolean isTrait = ((ClassDescriptor) containingDescriptor).getKind() == ClassKind.INTERFACE;
            boolean isDefinitelyAbstract = isTrait && !isBodyPresent;
            Modality basicModality = isTrait && !DescriptorVisibilities.isPrivate(visibility) ? Modality.OPEN : Modality.FINAL;
            defaultModality = isDefinitelyAbstract ? Modality.ABSTRACT : basicModality;
        } else {
            defaultModality = Modality.FINAL;
        }
        return defaultModality;
    }

    public static void checkConflictingUpperBounds(
            @NotNull BindingTrace trace,
            @NotNull TypeParameterDescriptor parameter,
            @NotNull CjTypeParameter typeParameter
    ) {
        if (CangJieBuiltIns.isNothing(TypeIntersector.getUpperBoundsAsType(parameter))) {
            trace.report(CONFLICTING_UPPER_BOUNDS.on(typeParameter, parameter));
        }
    }

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
//        if (classifier == null /*|| !DescriptorUtils.isAnonymousObject(classifier)*/ || DescriptorUtils.isLocal(descriptor)) {
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
//                boolean lowerNullable = FlexibleTypesKt.lowerIfFlexible(unwrapped).isMarkedOption();
//                boolean upperNullable = FlexibleTypesKt.upperIfFlexible(unwrapped).isMarkedOption();
//                if (languageVersionSettings.supportsFeature(LanguageFeature.KeepNullabilityWhenApproximatingLocalType)) {
//                    if (lowerNullable != upperNullable) {
//                        return CangJieTypeFactory.flexibleType(
//                                FlexibleTypesKt.lowerIfFlexible(substitutedSuperType),
//                                FlexibleTypesKt.upperIfFlexible(substitutedSuperType).makeNullableAsSpecified(true));
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
    public static void checkUpperBoundTypes(
            @NotNull BindingTrace trace,
            @NotNull List<UpperBoundCheckRequest> requests,
            boolean hasOverrideModifier
    ) {
        if (requests.isEmpty()) return;

        Set<Name> classBoundEncountered = new HashSet<>();
        Set<Pair<Name, TypeConstructor>> allBounds = new HashSet<>();

        for (UpperBoundCheckRequest request : requests) {
            Name typeParameterName = request.typeParameterName;
            CangJieType upperBound = request.upperBoundType;
            CjTypeReference upperBoundElement = request.upperBound;

            if (!CangJieTypeKt.isError(upperBound)) {
                if (!allBounds.add(new Pair<>(typeParameterName, upperBound.getConstructor()))) {
                    trace.report(REPEATED_BOUND.on(upperBoundElement));
                } else {
                    ClassDescriptor classDescriptor = TypeUtils.getClassDescriptor(upperBound);
                    if (classDescriptor != null) {
                        ClassKind kind = classDescriptor.getKind();
                        if (kind == ClassKind.CLASS || kind == ClassKind.ENUM || kind == ClassKind.STRUCT) {
                            if (!classBoundEncountered.add(typeParameterName)) {
                                trace.report(ONLY_ONE_CLASS_BOUND_ALLOWED.on(upperBoundElement));
                            }
                        }
                    }
                }
            }

            checkUpperBoundType(upperBoundElement, upperBound, trace, hasOverrideModifier);
        }
    }

    public static void checkUpperBoundType(
            CjTypeReference upperBound,
            @NotNull CangJieType upperBoundType,
            BindingTrace trace,
            boolean hasOverrideModifier
    ) {
//        if (!hasOverrideModifier && !TypeUtils.canHaveSubtypes(CangJieTypeChecker.DEFAULT, upperBoundType)) {
//            trace.report(FINAL_UPPER_BOUND.on(upperBound, upperBoundType));
//        }
//        if (DynamicTypesKt.isDynamic(upperBoundType)) {
//            trace.report(DYNAMIC_UPPER_BOUND.on(upperBound));
//        }
//        if (FunctionTypesKt.isExtensionFunctionType(upperBoundType)) {
//            trace.report(UPPER_BOUND_IS_EXTENSION_FUNCTION_TYPE.on(upperBound));
//        }
//        if (DefinitelyNonNullableTypesKt.containsIncorrectExplicitDefinitelyNonNullableType(upperBoundType)) {
//            trace.report(INCORRECT_LEFT_COMPONENT_OF_INTERSECTION.on(upperBound));
//        }
    }

    private static void checkNoGenericBoundsOnTypeAliasParameters(@NotNull CjTypeAlias typeAlias, @NotNull BindingTrace trace) {
        for (CjTypeParameter typeParameter : typeAlias.getTypeParameters()) {
            CjTypeReference bound = typeParameter.getExtendsBound();
            if (bound != null) {
                trace.report(BOUND_ON_TYPE_ALIAS_PARAMETER_NOT_ALLOWED.on(bound));
            }
        }
    }

    private static Collection<CangJieType> resolveSuperTypeListEntries(
            LexicalScope extensibleScope,
            List<CjSuperTypeListEntry> delegationSpecifiers,
            @NotNull TypeResolver resolver,
            BindingTrace trace,
            boolean checkBounds
    ) {
        if (delegationSpecifiers.isEmpty()) {
            return Collections.emptyList();
        }
        Collection<CangJieType> result = Lists.newArrayList();
        for (CjSuperTypeListEntry delegationSpecifier : delegationSpecifiers) {
            CjTypeReference typeReference = delegationSpecifier.getTypeReference();
            if (typeReference != null) {
                CangJieType supertype = resolver.resolveType(extensibleScope, typeReference, trace, checkBounds);
                if (DynamicTypesKt.isDynamic(supertype)) {
                    trace.report(DYNAMIC_SUPERTYPE.on(typeReference));
                } else {
                    result.add(supertype);
                    CjTypeElement bareSuperType = checkNullableSupertypeAndStripQuestionMarks(trace, typeReference.getTypeElement());
                    checkProjectionsInImmediateArguments(trace, bareSuperType, supertype);
                }
            } else {
                result.add(ErrorUtils.createErrorType(ErrorTypeKind.UNRESOLVED_TYPE, delegationSpecifier.getText()));
            }
        }
        return result;
    }

    private static void checkProjectionsInImmediateArguments(
            @NotNull BindingTrace trace,
            @Nullable CjTypeElement typeElement,
            @NotNull CangJieType type
    ) {
//        if (typeElement == null) return;

//        boolean hasProjectionsInWrittenArguments = false;
//        处理类型投影
//        if (typeElement instanceof CjUserType userType) {
//            List<CjTypeProjection> typeArguments = userType.getTypeArguments();
//            for (CjTypeProjection typeArgument : typeArguments) {
//                if (typeArgument.getProjectionKind() != CjProjectionKind.NONE) {
//                    trace.report(PROJECTION_IN_IMMEDIATE_ARGUMENT_TO_SUPERTYPE.on(typeArgument));
//                    hasProjectionsInWrittenArguments = true;
//                }
//            }
//        }

        // If we have an abbreviated type (written with a type alias), it still can contain type projections in top-level arguments.
//        if (!CangJieTypeKt.isError(type) && SpecialTypesKt.getAbbreviatedType(type) != null && !hasProjectionsInWrittenArguments) {
        // Only interface inheritance should be checked here.
        // Corresponding check for classes is performed for type alias constructor calls in CandidateResolver.
//            if (TypeUtilKt.isInterface(type) && TypeUtilKt.containsTypeProjectionsInTopLevelArguments(type)) {
//                trace.report(EXPANDED_TYPE_CANNOT_BE_INHERITED.on(typeElement, type));
//            }
//        }
    }

    @Nullable
    private static CjTypeElement checkNullableSupertypeAndStripQuestionMarks(@NotNull BindingTrace trace, @Nullable CjTypeElement typeElement) {
//        while (typeElement instanceof CjNullableType) {
//            CjNullableType nullableType = (CjNullableType) typeElement;
//            typeElement = nullableType.getInnerType();
//            // report only for innermost '?', the rest gets a 'redundant' warning
//            if (!(typeElement instanceof CjNullableType) && typeElement != null) {
//                trace.report(NULLABLE_SUPERTYPE.on(nullableType));
//            }
//        }
        return typeElement;
    }

    @NotNull
    private CangJieType getDefaultSupertype(@NotNull ClassDescriptor classDescriptor) {
        if (classDescriptor.getKind() == ClassKind.ENUM_ENTRY) {
            return ((ClassDescriptor) classDescriptor.getContainingDeclaration()).getDefaultType();
        }
//        else if (classDescriptor.getKind() == ClassKind.ANNOTATION_CLASS) {
//            return builtIns.getAnnotationType();
//        }


//        moduleDescriptor.getPackage(StandardNames.FqNames.core);

//        获取Any类型
//   CangJieType anyType =     Objects.requireNonNull(moduleDescriptor.getPackage(StandardNames.FqNames.core).getMemberScope().getContributedClassifier(
//                Name.identifier("Any"), NoLookupLocation.FROM_BUILTINS
//        )).getDefaultType();
        return builtIns.getAnyType();
//        return anyType;
    }

    public List<CangJieType> resolveSupertypes(
            @NotNull LexicalScope scope,
            @NotNull ClassDescriptor classDescriptor,
            @Nullable CjPureTypeStatement typeStatement,
            BindingTrace trace
    ) {
        builtIns.setSourcesModuleDescriptor(DescriptorUtilsKt.getModule(scope.getOwnerDescriptor()));

        List<CangJieType> supertypes = Lists.newArrayList();
        List<CjSuperTypeListEntry> delegationSpecifiers =
                typeStatement == null ? Collections.emptyList() : typeStatement.getSuperTypeListEntries();
        Collection<CangJieType> declaredSupertypes = resolveSuperTypeListEntries(
                scope,
                delegationSpecifiers,
                typeResolver, trace, false);

        for (CangJieType declaredSupertype : declaredSupertypes) {
            addValidSupertype(supertypes, declaredSupertype);
        }
//
//        if (classDescriptor.getKind() == ClassKind.ENUM && !containsClass(supertypes)) {
//            supertypes.add(0, builtIns.getEnumType(classDescriptor.getDefaultType()));
//        }
//
//        syntheticResolveExtension.addSyntheticSupertypes(classDescriptor, supertypes);
//        supertypes.addAll(additionalClassPartsProvider.getAdditionalSupertypes(classDescriptor, supertypes));


        ClassId classId = ((CjClassLikeDeclaration) typeStatement).getClassId();

        if (supertypes.isEmpty() && !ClassId.fromString("std/core/Any").equals(classId)) {
            addValidSupertype(supertypes, getDefaultSupertype(classDescriptor));
        }


        return supertypes;
    }

    @NotNull
    private PropertyDescriptor resolveAsPropertyDescriptor(
            @NotNull DeclarationDescriptor container,
            @NotNull LexicalScope scopeForDeclarationResolution,
            @NotNull LexicalScope scopeForInitializerResolution,
            @NotNull CjVariableDeclaration variableDeclaration,
            @NotNull BindingTrace trace,
            @NotNull DataFlowInfo dataFlowInfo,
            @Nullable InferenceSession inferenceSession,
            @NotNull VariableAsPropertyInfo propertyInfo
    ) {
        CjModifierList modifierList = variableDeclaration.getModifierList();
        boolean isVar = variableDeclaration.isVar();

        DescriptorVisibility visibility = resolveVisibilityFromModifiers(variableDeclaration, getDefaultVisibility(variableDeclaration, container));
        Modality modality = container instanceof ClassDescriptor
                ? resolveMemberModalityFromModifiers(variableDeclaration,
                getDefaultModality(container, visibility, propertyInfo.getHasBody()),
                trace.getBindingContext(), container)
                : Modality.FINAL;

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
//        Annotations propertyAnnotations = new CompositeAnnotations(CollectionsKt.listOf(
//                annotationSplitter.getAnnotationsForTarget(PROPERTY),
//                annotationSplitter.getOtherAnnotations())
//        );

        PropertyDescriptorImpl propertyDescriptor = PropertyDescriptorImpl.create(
                container,
                /*      propertyAnnotations,*/Annotations.EMPTY,
                modality,
                visibility,
                isVar,
                CjPsiUtil.safeName(variableDeclaration.getName()),
                CallableMemberDescriptor.Kind.DECLARATION,
                CangJieSourceElementKt.toSourceElement(variableDeclaration)
//                modifierList != null && modifierList.hasModifier(CjTokens.LATEINIT_KEYWORD),
//                modifierList != null && modifierList.hasModifier(CjTokens.CONST_KEYWORD),
//                modifierList != null && PsiUtilsKt.hasExpectModifier(modifierList) && container instanceof PackageFragmentDescriptor ||
//                        container instanceof ClassDescriptor && ((ClassDescriptor) container).isExpect(),
//                modifierList != null && PsiUtilsKt.hasActualModifier(modifierList),
//                modifierList != null && modifierList.hasModifier(CjTokens.EXTERNAL_KEYWORD),
//                propertyInfo.getHasDelegate()
        );
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
//            } else {
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
        return propertyDescriptor;
    }

    @NotNull
    public PropertyDescriptor resolvePropertyDescriptor(
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull LexicalScope scopeForDeclarationResolution,
            @NotNull LexicalScope scopeForInitializerResolution,
            @NotNull CjProperty property,
            @NotNull BindingTrace trace,
            @NotNull DataFlowInfo dataFlowInfo,
            @NotNull InferenceSession inferenceSession
    ) {
        return resolveAsPropertyDescriptor(
                containingDeclaration,
                scopeForDeclarationResolution,
                scopeForInitializerResolution,
                property,
                trace,
                dataFlowInfo,
                inferenceSession,
                VariableAsPropertyInfo.Companion.createFromProperty(property));
    }

    public void resolveGenericBounds(
            @NotNull CjTypeParameterListOwner declaration,
            @NotNull DeclarationDescriptor descriptor,
            LexicalScope scope,
            List<TypeParameterDescriptorImpl> parameters,
            BindingTrace trace
    ) {
        List<UpperBoundCheckRequest> upperBoundCheckRequests = Lists.newArrayList();

        List<CjTypeParameter> typeParameters = declaration.getTypeParameters();
        Map<Name, TypeParameterDescriptorImpl> parameterByName = new HashMap<>();
        for (int i = 0; i < typeParameters.size(); i++) {
            CjTypeParameter cjTypeParameter = typeParameters.get(i);
            TypeParameterDescriptorImpl typeParameterDescriptor = parameters.get(i);

            parameterByName.put(typeParameterDescriptor.getName(), typeParameterDescriptor);

            CjTypeReference extendsBound = cjTypeParameter.getExtendsBound();
            if (extendsBound != null) {
                CangJieType type = typeResolver.resolveType(scope, extendsBound, trace, false);
                typeParameterDescriptor.addUpperBound(type);
                upperBoundCheckRequests.add(new UpperBoundCheckRequest(cjTypeParameter.getNameAsName(), extendsBound, type));
            }
        }
        for (CjTypeConstraint constraint : declaration.getTypeConstraints()) {
            CjSimpleNameExpression subjectTypeParameterName = constraint.getSubjectTypeParameterName();
            if (subjectTypeParameterName == null) {
                continue;
            }
            Name referencedName = subjectTypeParameterName.getReferencedNameAsName();
            TypeParameterDescriptorImpl typeParameterDescriptor = parameterByName.get(referencedName);
            CjTypeReference boundTypeReference = constraint.getBoundTypeReference();
            CangJieType bound = null;
            if (boundTypeReference != null) {
                bound = typeResolver.resolveType(scope, boundTypeReference, trace, false);
                upperBoundCheckRequests.add(new UpperBoundCheckRequest(referencedName, boundTypeReference, bound));
            }

            if (typeParameterDescriptor != null) {
                trace.record(BindingContext.REFERENCE_TARGET, subjectTypeParameterName, typeParameterDescriptor);
                if (bound != null) {
                    typeParameterDescriptor.addUpperBound(bound);
                }
            }
        }

        for (TypeParameterDescriptorImpl parameter : parameters) {
            parameter.addDefaultUpperBound();
            parameter.setInitialized();
        }

        for (TypeParameterDescriptorImpl parameter : parameters) {
            checkConflictingUpperBounds(trace, parameter, typeParameters.get(parameter.getIndex()));
        }

        if (!(declaration instanceof CjClass)) {
            checkUpperBoundTypes(trace, upperBoundCheckRequests, declaration.hasModifier(CjTokens.OVERRIDE_KEYWORD));
            checkNamesInConstraints(declaration, descriptor, scope, trace);
        }
    }

    public void checkNamesInConstraints(
            @NotNull CjTypeParameterListOwner declaration,
            @NotNull DeclarationDescriptor descriptor,
            @NotNull LexicalScope scope,
            @NotNull BindingTrace trace
    ) {
        for (CjTypeConstraint constraint : declaration.getTypeConstraints()) {
            CjSimpleNameExpression nameExpression = constraint.getSubjectTypeParameterName();
            if (nameExpression == null) continue;

            Name name = nameExpression.getReferencedNameAsName();

            ClassifierDescriptor classifier = ScopeUtilsKt.findClassifier(scope, name, NoLookupLocation.FOR_NON_TRACKED_SCOPE);
            if (classifier instanceof TypeParameterDescriptor && classifier.getContainingDeclaration() == descriptor)
                continue;

            if (classifier != null) {
                // To tell the user that we look only for locally defined type parameters
                trace.report(NAME_IN_CONSTRAINT_IS_NOT_A_TYPE_PARAMETER.on(nameExpression, constraint, declaration));
                trace.record(BindingContext.REFERENCE_TARGET, nameExpression, classifier);
            } else {
                trace.report(UNRESOLVED_REFERENCE.on(nameExpression, nameExpression));
            }

            CjTypeReference boundTypeReference = constraint.getBoundTypeReference();
            if (boundTypeReference != null) {
                typeResolver.resolveType(scope, boundTypeReference, trace, true);
            }
        }
    }

    @NotNull
        /*package*/ CangJieType inferReturnTypeFromExpressionBody(
            @NotNull BindingTrace trace,
            @NotNull LexicalScope scope,
            @NotNull DataFlowInfo dataFlowInfo,
            @NotNull CjDeclarationWithBody function,
            @NotNull FunctionDescriptor functionDescriptor,
            @Nullable InferenceSession inferenceSession
    ) {

//        TODO 推断返回值类型
        return builtIns.getUnitType();
//    return wrappedTypeFactory.createRecursionIntolerantDeferredType(trace, () -> {
//        PreliminaryDeclarationVisitor.Companion.createForDeclaration(function, trace, languageVersionSettings);
//        CangJieType type = expressionTypingServices.getBodyExpressionType(
//                trace, scope, dataFlowInfo, function, functionDescriptor, inferenceSession
//        );
//        CangJieType publicType = transformAnonymousTypeIfNeeded(
//                functionDescriptor, function, type, trace, anonymousTypeTransformers, languageVersionSettings
//        );
//        UnwrappedType approximatedType = typeApproximator.approximateDeclarationType(publicType, false);
//        CangJieType sanitizedType = declarationReturnTypeSanitizer.sanitizeReturnType(approximatedType, wrappedTypeFactory, trace, languageVersionSettings);
//        functionsTypingVisitor.checCjypesForReturnStatements(function, trace, sanitizedType);
//        return sanitizedType;
//    });
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
//        CangJieType varargElementType = null;
        CangJieType variableType = type;
//        if (valueParameter.hasModifier(VARARG_KEYWORD)) {
//            varargElementType = type;
//            variableType = getVarargParameterType(type);
//        }

        Annotations valueParameterAnnotations = resolveValueParameterAnnotations(scope, valueParameter, trace, additionalAnnotations);

        CjDestructuringDeclaration destructuringDeclaration = valueParameter.getDestructuringDeclaration();

        Function0<List<VariableDescriptorBase>> destructuringVariables;
        if (destructuringDeclaration != null) {
//            if (!languageVersionSettings.supportsFeature(LanguageFeature.DestructuringLambdaParameters)) {
//                trace.report(Errors.UNSUPPORTED_FEATURE.on(valueParameter,
//                        TuplesCj.to(LanguageFeature.DestructuringLambdaParameters, languageVersionSettings)));
//            }

            //                ReceiverParameterDescriptor dispatchReceiver = owner.getDispatchReceiverParameter();
            //                assert dispatchReceiver == null || dispatchReceiver.getContainingDeclaration() instanceof ScriptDescriptor
            //                        : "Destructuring declarations are only be parsed for lambdas, and they must not have a dispatch receiver";
            //                LexicalScope scopeForDestructuring =
            //                        ScopeUtilsCj.createScopeForDestructuring(scope, owner.getExtensionReceiverParameter());
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
        } else {
            destructuringVariables = null;
        }

        Name parameterName;

        if (destructuringDeclaration == null) {
            // NB: val/var for parameter is only allowed in primary constructors where single underscore names are still prohibited.
            // The problem with val/var is that when lazy resolve try to find their descriptor, it searches through the member scope
            // of containing class where, it can not find a descriptor with special name.
            // Thus, to preserve behavior, we don't use a special name for val/var.
            parameterName = !valueParameter.hasLetOrVar() && UnderscoreUtilKt.isSingleUnderscore(valueParameter)
                    ? SpecialNames.anonymousParameterName(index)
                    : CjPsiUtil.safeName(valueParameter.getName());
        } else {
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

//    @NotNull
//    private CangJieType getVarargParameterType(@NotNull CangJieType elementType) {
//        CangJieType primitiveArrayType = builtIns.getPrimitiveArrayCangJieTypeByPrimitiveCangJieType(elementType);
//        if (primitiveArrayType != null) {
//            return primitiveArrayType;
//        }
//        return builtIns.getArrayType(Variance.OUT_VARIANCE, elementType);
//    }

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
        if (!parameter.hasLetOrVar()) {
            return new CompositeAnnotations(allAnnotations, additionalAnnotations);
        }

        AnnotationSplitter splitter = new AnnotationSplitter(storageManager, allAnnotations, SetsKt.setOf(CONSTRUCTOR_PARAMETER));
        return new CompositeAnnotations(splitter.getAnnotationsForTarget(CONSTRUCTOR_PARAMETER), additionalAnnotations);
    }

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
                //                typeParameter.hasModifier(CjTokens.REIFIED_KEYWORD),
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

    @NotNull
    public TypeAliasDescriptor resolveTypeAliasDescriptor(
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull LexicalScope scope,
            @NotNull CjTypeAlias typeAlias,
            @NotNull BindingTrace trace
    ) {


//        if (!(containingDeclaration instanceof PackageFragmentDescriptor) &&
//                !(containingDeclaration instanceof ScriptDescriptor)) {
//            trace.report(TOPLEVEL_TYPEALIASES_ONLY.on(typeAlias));
//        }

        CjModifierList modifierList = typeAlias.getModifierList();
        DescriptorVisibility visibility = resolveVisibilityFromModifiers(typeAlias, getDefaultVisibility(typeAlias, containingDeclaration));

        Annotations allAnnotations = annotationResolver.resolveAnnotationsWithArguments(scope, modifierList, trace);
        Name name = CjPsiUtil.safeName(typeAlias.getName());
        SourceElement sourceElement = CangJieSourceElementKt.toSourceElement(typeAlias);
        LazyTypeAliasDescriptor typeAliasDescriptor = LazyTypeAliasDescriptor.create(
                storageManager, trace, containingDeclaration, allAnnotations, name, sourceElement, visibility);

        List<TypeParameterDescriptorImpl> typeParameterDescriptors;
        LexicalScope scopeWithTypeParameters;
        {
            List<CjTypeParameter> typeParameters = typeAlias.getTypeParameters();
            if (typeParameters.isEmpty()) {
                scopeWithTypeParameters = scope;
                typeParameterDescriptors = Collections.emptyList();
            } else {
                LexicalWritableScope writableScope = new LexicalWritableScope(
                        scope, containingDeclaration, false, new TraceBasedLocalRedeclarationChecker(trace, overloadChecker),
                        LexicalScopeKind.TYPE_ALIAS_HEADER);
                typeParameterDescriptors = resolveTypeParametersForDescriptor(
                        typeAliasDescriptor, writableScope, scope, typeParameters, trace);
                writableScope.freeze();
                checkNoGenericBoundsOnTypeAliasParameters(typeAlias, trace);
                resolveGenericBounds(typeAlias, typeAliasDescriptor, writableScope, typeParameterDescriptors, trace);
                scopeWithTypeParameters = writableScope;
            }
        }

        CjTypeReference typeReference = typeAlias.getTypeReference();
        if (typeReference == null) {
            typeAliasDescriptor.initialize(
                    typeParameterDescriptors,
                    ErrorUtils.createErrorType(ErrorTypeKind.UNRESOLVED_TYPE_ALIAS, name.asString()),
                    ErrorUtils.createErrorType(ErrorTypeKind.UNRESOLVED_TYPE_ALIAS, name.asString()));
        } else if (!languageVersionSettings.supportsFeature(LanguageFeature.TypeAliases)) {
            typeResolver.resolveAbbreviatedType(scopeWithTypeParameters, typeReference, trace);
            PsiElement typeAliasKeyword = typeAlias.getTypeAliasKeyword();
            trace.report(UNSUPPORTED_FEATURE.on(typeAliasKeyword != null ? typeAliasKeyword : typeAlias,
                    TuplesKt.to(LanguageFeature.TypeAliases, languageVersionSettings)));
            typeAliasDescriptor.initialize(
                    typeParameterDescriptors,
                    ErrorUtils.createErrorType(ErrorTypeKind.UNRESOLVED_TYPE_ALIAS, name.asString()),
                    ErrorUtils.createErrorType(ErrorTypeKind.UNRESOLVED_TYPE_ALIAS, name.asString()));
        } else {
            typeAliasDescriptor.initialize(
                    typeParameterDescriptors,
                    storageManager.createRecursionTolerantLazyValue(
                            () -> typeResolver.resolveAbbreviatedType(scopeWithTypeParameters, typeReference, trace),
                            ErrorUtils.createErrorType(ErrorTypeKind.RECURSIVE_TYPE_ALIAS, typeAliasDescriptor.getName().asString())
                    ),
                    storageManager.createRecursionTolerantLazyValue(
                            () -> typeResolver.resolveExpandedTypeForTypeAlias(typeAliasDescriptor),
                            ErrorUtils.createErrorType(ErrorTypeKind.RECURSIVE_TYPE_ALIAS, typeAliasDescriptor.getName().asString())
                    )
            );
        }

        trace.record(TYPE_ALIAS, typeAlias, typeAliasDescriptor);
        return typeAliasDescriptor;
    }

    @NotNull
    public VariableDescriptor resolveVariableDescriptor(DeclarationDescriptor container,
                                                        @NotNull LexicalScope scopeForDeclarationResolution,
                                                        @NotNull LexicalScope scopeForInitializerResolution,
                                                        @NotNull CjVariable variableDeclaration,
                                                        @NotNull BindingTrace trace,
                                                        @NotNull DataFlowInfo dataFlowInfo,
                                                        @NotNull InferenceSession inferenceSession) {
        VariableAsPropertyInfo variableInfo = VariableAsPropertyInfo.createFromProperty(variableDeclaration);

        CjModifierList modifierList = variableDeclaration.getModifierList();
        boolean isVar = variableDeclaration.isVar();

        DescriptorVisibility visibility = resolveVisibilityFromModifiers(variableDeclaration, getDefaultVisibility(variableDeclaration, container));

        Modality modality = container instanceof ClassDescriptor
                ? resolveMemberModalityFromModifiers(variableDeclaration,
                getDefaultModality(container, visibility, variableInfo.getHasBody()),
                trace.getBindingContext(), container)
                : Modality.FINAL;
//暂时使用EMPTY
        Annotations variableAnnotations = Annotations.EMPTY;

        VariableDescriptorImpl variableDescriptor = VariableDescriptorImpl.create(
                container,
                variableAnnotations,
                modality,
                visibility,
                isVar,
                CjPsiUtil.safeName(variableDeclaration.getName()),
                CallableMemberDescriptor.Kind.DECLARATION,
                CangJieSourceElementKt.toSourceElement(variableDeclaration)
//                modifierList != null && modifierList.hasModifier(CjTokens.LATEINIT_KEYWORD),
//                modifierList != null && modifierList.hasModifier(CjTokens.CONST_KEYWORD),
//                modifierList != null && PsiUtilsKt.hasExpectModifier(modifierList) && container instanceof PackageFragmentDescriptor ||
//                        container instanceof ClassDescriptor && ((ClassDescriptor) container).isExpect(),
//                modifierList != null && PsiUtilsKt.hasActualModifier(modifierList),
//                modifierList != null && modifierList.hasModifier(CjTokens.EXTERNAL_KEYWORD),
//                propertyInfo.getHasDelegate()
        );
        List<TypeParameterDescriptorImpl> typeParameterDescriptors;
        LexicalScope scopeForDeclarationResolutionWithTypeParameters;
        LexicalScope scopeForInitializerResolutionWithTypeParameters;
        CangJieType receiverType = null;
        {
            List<CjTypeParameter> typeParameters = variableDeclaration.getTypeParameters();
            if (typeParameters.isEmpty()) {
                scopeForDeclarationResolutionWithTypeParameters = scopeForDeclarationResolution;
                scopeForInitializerResolutionWithTypeParameters = scopeForInitializerResolution;
                typeParameterDescriptors = Collections.emptyList();
            } else {
                LexicalWritableScope writableScopeForDeclarationResolution = new LexicalWritableScope(
                        scopeForDeclarationResolution, container, false, new TraceBasedLocalRedeclarationChecker(trace, overloadChecker),
                        LexicalScopeKind.PROPERTY_HEADER);
                LexicalWritableScope writableScopeForInitializerResolution = new LexicalWritableScope(
                        scopeForInitializerResolution, container, false, LocalRedeclarationChecker.DO_NOTHING.INSTANCE,
                        LexicalScopeKind.PROPERTY_HEADER);
                typeParameterDescriptors = resolveTypeParametersForDescriptor(
                        variableDescriptor,
                        scopeForDeclarationResolution, typeParameters, trace);
                for (TypeParameterDescriptor descriptor : typeParameterDescriptors) {
                    writableScopeForDeclarationResolution.addClassifierDescriptor(descriptor);
                    writableScopeForInitializerResolution.addClassifierDescriptor(descriptor);
                }
                writableScopeForDeclarationResolution.freeze();
                writableScopeForInitializerResolution.freeze();
                resolveGenericBounds(variableDeclaration, variableDescriptor, writableScopeForDeclarationResolution, typeParameterDescriptors, trace);
                scopeForDeclarationResolutionWithTypeParameters = writableScopeForDeclarationResolution;
                scopeForInitializerResolutionWithTypeParameters = writableScopeForInitializerResolution;
            }
        }

        CjTypeReference receiverTypeRef = variableDeclaration.getReceiverTypeReference();
        ReceiverParameterDescriptor receiverDescriptor = null;
        if (receiverTypeRef != null) {
            receiverType = typeResolver.resolveType(scopeForDeclarationResolutionWithTypeParameters, receiverTypeRef, trace, true);
            AnnotationSplitter splitter = new AnnotationSplitter(storageManager, receiverType.getAnnotations(), EnumSet.of(AnnotationUseSiteTarget.RECEIVER));
            receiverDescriptor = DescriptorFactory.createExtensionReceiverParameterForCallable(
                    variableDescriptor, receiverType, splitter.getAnnotationsForTarget(AnnotationUseSiteTarget.RECEIVER)
            );
        }

        List<CjContextReceiver> contextReceivers = variableDeclaration.getContextReceivers();
        List<ReceiverParameterDescriptor> contextReceiverDescriptors = IntStream.range(0, contextReceivers.size()).mapToObj(index -> {
            CjContextReceiver contextReceiver = contextReceivers.get(index);
            CjTypeReference typeReference = contextReceiver.typeReference();
            if (typeReference == null) {
                return null;
            }
            CangJieType type = typeResolver.resolveType(scopeForDeclarationResolutionWithTypeParameters, typeReference, trace, true);
            AnnotationSplitter splitter = new AnnotationSplitter(storageManager, type.getAnnotations(), EnumSet.of(AnnotationUseSiteTarget.RECEIVER));
            return DescriptorFactory.createContextReceiverParameterForCallable(
                    variableDescriptor, type, contextReceiver.labelNameAsName(), splitter.getAnnotationsForTarget(AnnotationUseSiteTarget.RECEIVER), index
            );
        }).toList();

        LexicalScope scopeForInitializer = ScopeUtils.makeScopeForVariableInitializer(scopeForInitializerResolutionWithTypeParameters, variableDescriptor);
        CangJieType propertyType = variableInfo.getVariableType();
        CangJieType typeIfKnown = propertyType != null ? propertyType : variableTypeAndInitializerResolver.resolveTypeNullable(
                variableDescriptor, scopeForInitializer,
                variableDeclaration, dataFlowInfo, inferenceSession,
                trace, /* local = */ false
        );
        CangJieType type = typeIfKnown != null ? typeIfKnown : builtIns.getUnitType();
//        assert type != null : "At least getter type must be initialized via resolvePropertyGetterDescriptor";
        variableTypeAndInitializerResolver.setConstantForVariableIfNeeded(
                variableDescriptor, scopeForInitializer, variableDeclaration, dataFlowInfo, type, inferenceSession, trace
        );

        variableDescriptor.setType(type, typeParameterDescriptors, getDispatchReceiverParameterIfNeeded(container), receiverDescriptor,
                contextReceiverDescriptors);


        trace.record(BindingContext.VARIABLE, variableDeclaration, variableDescriptor);

        return variableDescriptor;
    }


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

}
