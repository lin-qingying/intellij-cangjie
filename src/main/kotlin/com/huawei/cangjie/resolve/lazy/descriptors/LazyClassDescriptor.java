package com.huawei.cangjie.resolve.lazy.descriptors;

import com.huawei.cangjie.builtins.CangJieBuiltIns;
import com.huawei.cangjie.config.LanguageFeature;
import com.huawei.cangjie.descriptors.*;
import com.huawei.cangjie.descriptors.annotations.Annotations;
import com.huawei.cangjie.descriptors.impl.FunctionDescriptorImpl;
import com.huawei.cangjie.name.Name;
import com.huawei.cangjie.psi.*;
import com.huawei.cangjie.resolve.BindingContext;
import com.huawei.cangjie.resolve.DescriptorToSourceUtils;
import com.huawei.cangjie.resolve.DescriptorUtils;
import com.huawei.cangjie.resolve.descriptorUtil.DescriptorUtilsKt;
import com.huawei.cangjie.resolve.lazy.ForceResolveUtil;
import com.huawei.cangjie.resolve.lazy.LazyClassContext;
import com.huawei.cangjie.resolve.lazy.LazyEntity;
import com.huawei.cangjie.resolve.lazy.data.CjClassLikeInfo;
import com.huawei.cangjie.resolve.scopes.LexicalScope;
import com.huawei.cangjie.resolve.scopes.MemberScope;
import com.huawei.cangjie.resolve.scopes.StaticScopeForCangJieEnum;
import com.huawei.cangjie.storage.NotNullLazyValue;
import com.huawei.cangjie.storage.StorageManager;
import com.huawei.cangjie.types.AbstractClassTypeConstructor;
import com.huawei.cangjie.types.CangJieType;
import com.huawei.cangjie.types.CangJieTypeKt;
import com.huawei.cangjie.types.TypeConstructor;
import com.huawei.cangjie.types.checker.CangJieTypeRefiner;
import com.huawei.cangjie.types.util.TypeUtils;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNameIdentifierOwner;
import kotlin.collections.CollectionsKt;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static com.huawei.cangjie.descriptors.Errors.CYCLIC_INHERITANCE_HIERARCHY;
import static com.huawei.cangjie.descriptors.Errors.CYCLIC_SCOPES_WITH_COMPANION;
import static com.huawei.cangjie.resolve.BindingContext.TYPE;
import static com.huawei.cangjie.resolve.ModifiersChecker.resolveModalityFromModifiers;
import static com.huawei.cangjie.resolve.ModifiersChecker.resolveVisibilityFromModifiers;

public class LazyClassDescriptor extends LazyClassDescriptorBase implements /*ClassDescriptorWithResolutionScopes,*/ LazyEntity {
    private static final Function1<CangJieType, Boolean> VALID_SUPERTYPE = type -> {
        assert !CangJieTypeKt.isError(type) : "Error types must be filtered out in DescriptorResolver";
        return TypeUtils.getClassDescriptor(type) != null;
    };
    private final LazyClassContext c;
    private final ClassMemberDeclarationProvider declarationProvider;
    private final ScopesHolderForClass<LazyClassMemberScope> scopesHolderForClass;
    private final MemberScope staticScope;
    private final ClassKind kind;
    private final NotNullLazyValue<LexicalScope> scopeForInitializerResolution;
    private final NotNullLazyValue<Modality> modality;
    private final NotNullLazyValue<List<TypeParameterDescriptor>> parameters;
    private final DescriptorVisibility visibility;
    private final Annotations annotations = Annotations.EMPTY;
    private final ClassResolutionScopesSupport resolutionScopesSupport;
    private final LazyClassTypeConstructor typeConstructor;
    private final NotNullLazyValue<Collection<ClassDescriptor>> sealedSubclasses;
    public Set<LazyExtendClassDescriptor> extendClassDescriptor = new HashSet<>();
    @Nullable
    private CjTypeStatement typeStatement;
    //    该方法是为扩展提供更改psi节点的，其他情况不要使用
    private BindingTrace extendTrace = null;
    private LexicalScope extendScope = null;


    public LazyClassDescriptor(
            @NotNull LazyClassContext c,
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull Name name,
            @NotNull CjClassLikeInfo classLikeInfo,
            boolean isExternal
    ) {
        super(c, containingDeclaration, name,
                classLikeInfo
                , isExternal);
        this.c = c;


        typeStatement = classLikeInfo.getCorrespondingClass();
        this.declarationProvider = c.getDeclarationProviderFactory().getClassMemberDeclarationProvider(classLikeInfo);
//
        StorageManager storageManager = c.getStorageManager();
//
        this.scopesHolderForClass = createScopesHolderForClass(c, this.declarationProvider);
        this.kind = classLikeInfo.getClassKind();
        this.staticScope = kind == ClassKind.ENUM ?
                new StaticScopeForCangJieEnum(
                        storageManager, this, /* enumEntriesCanBeUsed = */ true
                ) : MemberScope.Empty.INSTANCE;

        this.typeConstructor = new LazyClassTypeConstructor();
//
//        this.isCompanionObject = classLikeInfo instanceof KtObjectInfo && ((KtObjectInfo) classLikeInfo).isCompanionObject();
//
        CjModifierList modifierList = classLikeInfo.getModifierList();
        if (kind.isStruct()) {
            this.modality = storageManager.createLazyValue(() -> Modality.FINAL);
        } else {
            Modality defaultModality = kind == ClassKind.INTERFACE ? Modality.ABSTRACT : Modality.FINAL;
            this.modality = storageManager.createLazyValue(
                    () -> resolveModalityFromModifiers(typeStatement, defaultModality, c.getTrace().getBindingContext(),
                            null, /* allowSealed = */ true));
        }
//
        boolean isLocal = typeStatement != null && CjPsiUtil.isLocal(typeStatement);
//        默认为INTERNAL
        this.visibility = isLocal ? DescriptorVisibilities.LOCAL : resolveVisibilityFromModifiers(modifierList, DescriptorVisibilities.INTERNAL);
//
//        this.isInner = modifierList != null && modifierList.hasModifier(INNER_KEYWORD) && !isIllegalInner(this);
//        this.isData = modifierList != null && modifierList.hasModifier(KtTokens.DATA_KEYWORD);
//        this.isInline = modifierList != null && modifierList.hasModifier(KtTokens.INLINE_KEYWORD);
//        this.isActual = modifierList != null && PsiUtilsKt.hasActualModifier(modifierList);
//
//        this.isExpect = modifierList != null && PsiUtilsKt.hasExpectModifier(modifierList) ||
//                containingDeclaration instanceof ClassDescriptor && ((ClassDescriptor) containingDeclaration).isExpect();
//
//        this.isFun = modifierList != null && PsiUtilsKt.hasFunModifier(modifierList);
//        this.isValue = modifierList != null && PsiUtilsKt.hasValueModifier(modifierList);
//
//        // Annotation entries are taken from both own annotations (if any) and object literal annotations (if any)
//        List<KtAnnotationEntry> annotationEntries = new ArrayList<>();
//        if (typeStatement != null && typeStatement.getParent() instanceof KtObjectLiteralExpression) {
//            // TODO: it would be better to have separate ObjectLiteralDescriptor without so much magic
//            annotationEntries.addAll(KtPsiUtilKt.getAnnotationEntries((KtObjectLiteralExpression) typeStatement.getParent()));
//        }
//        if (modifierList != null) {
//            annotationEntries.addAll(modifierList.getAnnotationEntries());
//        }
//        if (!annotationEntries.isEmpty()) {
//            this.annotations = new LazyAnnotations(
//                    new LazyAnnotationsContext(
//                            c.getAnnotationResolver(),
//                            storageManager,
//                            c.getTrace()
//                    ) {
//                        @NotNull
//                        @Override
//                        public LexicalScope getScope() {
//                            return getOuterScope();
//                        }
//                    },
//                    annotationEntries
//            );
//        }
//        else {
//            this.annotations = Annotations.Companion.getEMPTY();
//        }
//
//        List<KtAnnotationEntry> ktDanglingAnnotations = classLikeInfo.getDanglingAnnotations();
//        if (ktDanglingAnnotations.isEmpty()) {
//            this.danglingAnnotations = Annotations.Companion.getEMPTY();
//        }
//        else {
//            this.danglingAnnotations = new LazyAnnotations(
//                    new LazyAnnotationsContext(
//                            c.getAnnotationResolver(),
//                            storageManager,
//                            c.getTrace()
//                    ) {
//                        @NotNull
//                        @Override
//                        public LexicalScope getScope() {
//                            return getScopeForMemberDeclarationResolution();
//                        }
//                    },
//                    ktDanglingAnnotations
//            );
//        }
//
//        this.companionObjectDescriptor = storageManager.createNullableLazyValue(
//                () -> computeCompanionObjectDescriptor(getCompanionObjectIfAllowed())
//        );
//        this.extraCompanionObjectDescriptors = storageManager.createMemoizedFunction(this::computeCompanionObjectDescriptor);
//        this.forceResolveAllContents = storageManager.createRecursionTolerantNullableLazyValue(() -> {
//            doForceResolveAllContents();
//            return null;
//        }, null);
//
        this.resolutionScopesSupport = new ClassResolutionScopesSupport(
                this,
                storageManager,
                c.getLanguageVersionSettings(),
                this::getOuterScope
        );
//
        this.parameters = c.getStorageManager().createLazyValue(() -> {
            CjClassLikeInfo classInfo = declarationProvider.getOwnerInfo();
            CjTypeParameterList typeParameterList = null;
            if (classInfo != null) {
                typeParameterList = classInfo.getTypeParameterList();
            }
            if (typeParameterList == null) return Collections.emptyList();
//
//            boolean isAnonymousObject = (classInfo.getClassKind() == ClassKind.CLASS) && (classInfo.getCorrespondingClassOrObject() instanceof KtObjectDeclaration);
//
//            if (classInfo.getClassKind() == ClassKind.ENUM ) {
//                c.getTrace().report(TYPE_PARAMETERS_IN_ENUM.on(typeParameterList));
//            }
//            if (classInfo.getClassKind() == ClassKind.OBJECT) {
//                c.getTrace().report(TYPE_PARAMETERS_IN_OBJECT.on(typeParameterList));
//            }
//            if (isAnonymousObject) {
//                DiagnosticFactory0<CjTypeParameterList> diagnosticFactory;
//                if (c.getLanguageVersionSettings().supportsFeature(LanguageFeature.ProhibitTypeParametersInAnonymousObjects)) {
//                    diagnosticFactory = TYPE_PARAMETERS_IN_OBJECT;
//                } else {
//                    diagnosticFactory = TYPE_PARAMETERS_IN_ANONYMOUS_OBJECT;
//                }
//                c.getTrace().report(diagnosticFactory.on(typeParameterList));
//            }
//
            List<CjTypeParameter> typeParameters = typeParameterList.getParameters();
            if (typeParameters.isEmpty()) return Collections.emptyList();
//
//            boolean supportClassTypeParameterAnnotations = c.getLanguageVersionSettings().supportsFeature(LanguageFeature.ClassTypeParameterAnnotations);
            List<TypeParameterDescriptor> parameters = new ArrayList<>(typeParameters.size());

            for (int i = 0; i < typeParameters.size(); i++) {
                CjTypeParameter parameter = typeParameters.get(i);
                Annotations lazyAnnotations = Annotations.EMPTY;
//                if (supportClassTypeParameterAnnotations) {
//                    lazyAnnotations = new LazyAnnotations(
//                            new LazyAnnotationsContext(
//                                    c.getAnnotationResolver(),
//                                    storageManager,
//                                    c.getTrace()
//                            ) {
//                                @NotNull
//                                @Override
//                                public LexicalScope getScope() {
//                                    return getOuterScope();
//                                }
//                            },
//                            parameter.getAnnotationEntries()
//                    );
//                } else {
//                    lazyAnnotations = Annotations.EMPTY;
//                }

                parameters.add(new LazyTypeParameterDescriptor(c, this, parameter, lazyAnnotations, i));
            }

            return parameters;
        });
//
        this.scopeForInitializerResolution = storageManager.createLazyValue(
                () -> ClassResolutionScopesSupportKt.scopeForInitializerResolution(
                        this, createInitializerScopeParent(), classLikeInfo.getPrimaryConstructorParameters()
                )
        );

        boolean freedomForSealedInterfacesSupported = c.getLanguageVersionSettings().supportsFeature(LanguageFeature.AllowSealedInheritorsInDifferentFilesOfSamePackage);
        this.sealedSubclasses =
                storageManager.createLazyValue(() -> {
                    if (getModality() == Modality.SEALED) {
                        return c.getSealedClassInheritorsProvider().computeSealedSubclasses(this, freedomForSealedInterfacesSupported);
                    } else {
                        return Collections.emptyList();
                    }
                });
//        this.contextReceivers = storageManager.createLazyValue(() -> {
//            if (typeStatement == null) {
//                return CollectionsKt.emptyList();
//            }
//            List<KtContextReceiver> contextReceivers = typeStatement.getContextReceivers();
//            List<ReceiverParameterDescriptor> contextReceiverDescriptors =
//                    IntStream.range(0, contextReceivers.size())
//                            .mapToObj(index -> {
//                                KtContextReceiver contextReceiver = contextReceivers.get(index);
//                                KtTypeReference typeReference = contextReceiver.typeReference();
//                                if (typeReference == null) return null;
//                                CangJieType cangjieType =
//                                        c.getTypeResolver().resolveType(getScopeForClassHeaderResolution(), typeReference, c.getTrace(), true);
//                                Name label = contextReceiver.labelNameAsName() != null
//                                        ? contextReceiver.labelNameAsName()
//                                        : contextReceiverName(index);
//                                return DescriptorFactory.createContextReceiverParameterForClass(
//                                        this,
//                                        cangjieType,
//                                        contextReceiver.labelNameAsName(),
//                                        Annotations.Companion.getEMPTY(),
//                                        index
//                                );
//                            })
//                            .filter(Objects::nonNull)
//                            .collect(Collectors.toList());
//
//            if (c.getLanguageVersionSettings().supportsFeature(LanguageFeature.ContextReceivers)) {
//                HashMultimap<String, ReceiverParameterDescriptor> labelNameToReceiverMap = HashMultimap.create();
//
//                for (int i = 0; i < contextReceivers.size(); i++) {
//                    labelNameToReceiverMap.put(contextReceivers.get(i).name(), contextReceiverDescriptors.get(i));
//                }
//
//                c.getTrace().record(BindingContext.DESCRIPTOR_TO_CONTEXT_RECEIVER_MAP, this, labelNameToReceiverMap);
//            }
//
//            return contextReceiverDescriptors;
//        });

        if (typeStatement != null) {
            this.c.getTrace().record(BindingContext.CLASS, typeStatement, this);
        }
//        this.c.getTrace().record(BindingContext.FQNAME_TO_CLASS_DESCRIPTOR, DescriptorUtils.getFqName(this), this);

    }

    @NotNull
    protected LexicalScope getOuterScope() {
        return c.getDeclarationScopeProvider().getResolutionScopeForDeclaration(declarationProvider.getOwnerInfo().getScopeAnchor());
    }

    public void resolveMemberHeaders() {
        ForceResolveUtil.forceResolveAllContents(getAnnotations());
//    ForceResolveUtil.forceResolveAllContents(getDanglingAnnotations());

//    getCompanionObjectDescriptor();
//
//    getDescriptorsForExtraCompanionObjects();

        getConstructors();
        getContainingDeclaration();
        getThisAsReceiverParameter();
        getKind();
        getModality();
        getName();
        getOriginal();
        getScopeForClassHeaderResolution();
        getScopeForMemberDeclarationResolution();
        DescriptorUtils.getAllDescriptors(getUnsubstitutedMemberScope());
        getScopeForInitializerResolution();
        getUnsubstitutedInnerClassesScope();
        getTypeConstructor().getSupertypes();
        for (TypeParameterDescriptor typeParameterDescriptor : getTypeConstructor().getParameters()) {
            typeParameterDescriptor.getUpperBounds();
        }
        getUnsubstitutedPrimaryConstructor();
        getVisibility();
        getContextReceivers();
    }

    @Override
    @NotNull
    public LexicalScope getScopeForConstructorHeaderResolution() {
        return resolutionScopesSupport.getScopeForConstructorHeaderResolution().invoke();
    }

    @NotNull
    protected ScopesHolderForClass<LazyClassMemberScope> createScopesHolderForClass(
            @NotNull LazyClassContext c,
            @NotNull ClassMemberDeclarationProvider declarationProvider
    ) {
        return ScopesHolderForClass.create(
                this,
                c.getStorageManager(),
                c.getCangjieTypeCheckerOfOwnerModule().getCangjieTypeRefiner(),
                cangjieTypeRefinerForDependentModule -> {
                    LazyClassMemberScope scopeForDeclaredMembers =
                            !cangjieTypeRefinerForDependentModule.isRefinementNeededForModule(c.getModuleDescriptor())
                                    ? null
                                    : scopesHolderForClass.getScope(c.getCangjieTypeCheckerOfOwnerModule().getCangjieTypeRefiner()); // essentially, a scope for owner-module

                    return new LazyClassMemberScope(
                            c, declarationProvider, this, c.getTrace(), cangjieTypeRefinerForDependentModule,
                            scopeForDeclaredMembers
                    );
                }
        );
    }

    @Override
    public @NotNull DescriptorVisibility getVisibility() {
        return visibility;

    }

    @Override
    public List<CjSuperTypeListEntry> getSuperTypeListEntries() {
        return typeStatement.getSuperTypeListEntries();
    }

    @Override
    public @NotNull MemberScope getUnsubstitutedInnerClassesScope() {
        return super.getUnsubstitutedInnerClassesScope();
    }

    @NotNull
    private DeclarationDescriptor createInitializerScopeParent() {
        ConstructorDescriptor primaryConstructor = getUnsubstitutedPrimaryConstructor();
        if (primaryConstructor != null) return primaryConstructor;

        return new FunctionDescriptorImpl(
                LazyClassDescriptor.this, null, Annotations.EMPTY, Name.special("<init-blocks>"),
                CallableMemberDescriptor.Kind.SYNTHESIZED, SourceElement.NO_SOURCE
        ) {
            {
                initialize(null, null, CollectionsKt.emptyList(), Collections.emptyList(), Collections.emptyList(),
                        null, Modality.FINAL, DescriptorVisibilities.PRIVATE);
            }

            @NotNull
            @Override
            protected FunctionDescriptorImpl createSubstitutedCopy(
                    @NotNull DeclarationDescriptor newOwner,
                    @Nullable FunctionDescriptor original,
                    @NotNull Kind kind,
                    @Nullable Name newName,
                    @NotNull Annotations annotations,
                    @NotNull SourceElement source
            ) {
                throw new UnsupportedOperationException();
            }
        };
    }

    @NotNull
    @Override
    @SuppressWarnings("unchecked")
    public Collection<CallableMemberDescriptor> getDeclaredCallableMembers() {
        return (Collection) CollectionsKt.filter(
                DescriptorUtils.getAllDescriptors(getUnsubstitutedMemberScope()),
                descriptor -> descriptor instanceof CallableMemberDescriptor
                        && ((CallableMemberDescriptor) descriptor).getKind() != CallableMemberDescriptor.Kind.FAKE_OVERRIDE
        );
    }

    @Override
    public @NotNull LexicalScope getScopeForInitializerResolution() {
        return scopeForInitializerResolution.invoke();

    }

    @Override
    public @NotNull MemberScope getUnsubstitutedMemberScope() {
        return getUnsubstitutedMemberScope(DescriptorUtilsKt.getCangJieTypeRefiner(DescriptorUtils.getContainingModule(this)));


    }

    @Override
    public @NotNull MemberScope getStaticScope() {
        return staticScope;

    }

    @Override
    public @NotNull Collection<ClassConstructorDescriptor> getConstructors() {
        return ((LazyClassMemberScope) getUnsubstitutedMemberScope()).getConstructors();

    }

    @Override
    public @NotNull ClassKind getKind() {
        return kind;

    }

    @Override
    public @NotNull Modality getModality() {
        return modality.invoke();

    }

    @Override
    public boolean isFun() {
        return false;
    }

    @Override
    public boolean isValue() {
        return false;
    }

    @Override
    public @Nullable ClassConstructorDescriptor getUnsubstitutedPrimaryConstructor() {
        return null;
    }

    @Override
    public @NotNull List<TypeParameterDescriptor> getDeclaredTypeParameters() {
        return parameters.invoke();

    }

    @Override
    public @NotNull Collection<ClassDescriptor> getSealedSubclasses() {
        return sealedSubclasses.invoke();

    }

    @Override
    public @NotNull TypeConstructor getTypeConstructor() {
        return typeConstructor;


    }

    @Deprecated()
    public void setExtendData(CjTypeStatement typeStatement, BindingTrace extendTrace, LexicalScope extendScope) {
        this.typeStatement = typeStatement;
        this.extendTrace = extendTrace;
        this.extendScope = extendScope;

    }

    @NotNull
    @Override
    public Annotations getAnnotations() {
        return annotations;

    }

    @NotNull
    @Override
    public MemberScope getUnsubstitutedMemberScope(@NotNull CangJieTypeRefiner cangjieTypeRefiner) {
        return scopesHolderForClass.getScope(cangjieTypeRefiner);

    }

    @Override
    public void forceResolveAllContents() {

    }

    @Override
    public @NotNull LexicalScope getScopeForMemberDeclarationResolution() {
        return resolutionScopesSupport.getScopeForMemberDeclarationResolution().invoke();


//        throw new UnsupportedOperationException();
    }

    @Override
    @NotNull
    public LexicalScope getScopeForClassHeaderResolution() {
        return resolutionScopesSupport.getScopeForClassHeaderResolution().invoke();
    }

    @Override
    public String toString() {
        // not using DescriptorRenderer to preserve laziness
        return typeStatement.toString();
    }

    protected @NotNull Collection<CangJieType> computeExtendSuperTypes(String extendId) {
//        val result = mutableListOf<CangJieType>()
//        classDescriptor.extendClassDescriptor.forEach {
//            if (it.typeStatement.getExtendId() != extendId) {
//                result.addAll(it.typeConstructor.supertypes)
//            }
//        }
//        return result

        List<CangJieType> result = new ArrayList<>();
        getExtendClass().forEach(it -> {
            if (!it.getTypeStatement().getExtendId().equals(extendId))
                result.addAll(it.getTypeConstructor().getSupertypes());
        });
        return result;

    }

    @NotNull
    protected Collection<CangJieType> computeSupertypes() {
        if (CangJieBuiltIns.isSpecialClassWithNoSupertypes(this)) {
            return Collections.emptyList();
        }
        BindingTrace trace = c.getTrace();
        LexicalScope scope = getScopeForClassHeaderResolution();
        CjTypeStatement classOrObject = declarationProvider.getOwnerInfo().getCorrespondingClass();
//        if (this.typeStatement instanceof CjExtend) {
//            if (extendTrace != null) {
//                trace = extendTrace;
//            }
//            if (extendScope != null) {
//                scope = extendScope;
//            }
//            classOrObject = this.typeStatement;
//        }
        if (classOrObject == null) {
            return Collections.singleton(c.getModuleDescriptor().getBuiltIns().getAnyType());
        }


        List<CangJieType> allSupertypes =
                c.getDescriptorResolver().resolveSupertypes(scope, this, classOrObject, trace);

        return new ArrayList<>(CollectionsKt.filter(allSupertypes, VALID_SUPERTYPE));
    }

    private class LazyClassTypeConstructor extends AbstractClassTypeConstructor {
        private final NotNullLazyValue<List<TypeParameterDescriptor>> parameters = c.getStorageManager().createLazyValue(
                () -> TypeParameterUtilsKt.computeConstructorTypeParameters(LazyClassDescriptor.this)
        );

        public LazyClassTypeConstructor() {
            super(LazyClassDescriptor.this.c.getStorageManager());
        }

        @NotNull
        @Override
        protected Collection<CangJieType> computeSupertypes() {
            return LazyClassDescriptor.this.computeSupertypes();
        }

        @Override
        protected @NotNull Collection<CangJieType> computeExtendSuperTypes(@Nullable String extendId) {
            return LazyClassDescriptor.this.computeExtendSuperTypes(extendId);
        }

        @Override
        protected void reportSupertypeLoopError(@NotNull CangJieType type) {
            ClassifierDescriptor supertypeDescriptor = type.getConstructor().getDeclarationDescriptor();
            if (supertypeDescriptor instanceof ClassDescriptor superclass) {
                reportCyclicInheritanceHierarchyError(c.getTrace(), LazyClassDescriptor.this, superclass);
            }
        }

        @Override
        protected boolean getShouldReportCyclicScopeWithCompanionWarning() {
            return !c.getLanguageVersionSettings()
                    .supportsFeature(LanguageFeature.ProhibitVisibilityOfNestedClassifiersFromSupertypes);
        }

        @Override
        protected void reportScopesLoopError(@NotNull CangJieType type) {
            PsiElement reportOn = DescriptorToSourceUtils.getSourceFromDescriptor(type.getConstructor().getDeclarationDescriptor());

            if (reportOn instanceof CjClass) {
                reportOn = ((CjClass) reportOn).getNameIdentifier();
            }

            if (reportOn != null) {
                c.getTrace().report(CYCLIC_SCOPES_WITH_COMPANION.on(reportOn));
            }
        }

        private void reportCyclicInheritanceHierarchyError(
                @NotNull BindingTrace trace,
                @NotNull ClassDescriptor classDescriptor,
                @NotNull ClassDescriptor superclass
        ) {
            PsiElement psiElement = DescriptorToSourceUtils.getSourceFromDescriptor(classDescriptor);

            PsiElement elementToMark = null;
            if (psiElement instanceof CjTypeStatement classOrObject) {
                for (CjSuperTypeListEntry delegationSpecifier : classOrObject.getSuperTypeListEntries()) {
                    CjTypeReference typeReference = delegationSpecifier.getTypeReference();
                    if (typeReference == null) continue;
                    CangJieType supertype = trace.get(TYPE, typeReference);
                    if (supertype != null && supertype.getConstructor() == superclass.getTypeConstructor()) {
                        elementToMark = typeReference;
                    }
                }
            }
            if (elementToMark == null && psiElement instanceof PsiNameIdentifierOwner namedElement) {
                PsiElement nameIdentifier = namedElement.getNameIdentifier();
                if (nameIdentifier != null) {
                    elementToMark = nameIdentifier;
                }
            }
            if (elementToMark != null) {
                trace.report(CYCLIC_INHERITANCE_HIERARCHY.on(elementToMark));
            }
        }

        @NotNull
        @Override
        protected SupertypeLoopChecker getSupertypeLoopChecker() {
            return c.getSupertypeLoopChecker();
        }

        @NotNull
        @Override
        public List<TypeParameterDescriptor> getParameters() {
            return parameters.invoke();
        }

        @Override
        public boolean isDenotable() {
            return true;
        }

        @Override
        @NotNull
        public ClassDescriptor getDeclarationDescriptor() {
            return LazyClassDescriptor.this;
        }

        @Override
        public String toString() {
            return LazyClassDescriptor.this.getName().toString();
        }
    }
}
