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

package cn.cangnova.cangjie.resolve.lazy;

import cn.cangnova.cangjie.config.LanguageVersionSettings;
import cn.cangnova.cangjie.context.GlobalContext;
import cn.cangnova.cangjie.descriptors.*;
import cn.cangnova.cangjie.descriptors.annotations.Annotations;
import cn.cangnova.cangjie.incremental.components.LookupLocation;
import cn.cangnova.cangjie.incremental.components.LookupTracker;
import cn.cangnova.cangjie.name.FqName;
import cn.cangnova.cangjie.name.Name;
import cn.cangnova.cangjie.psi.*;
import cn.cangnova.cangjie.resolve.*;
import cn.cangnova.cangjie.resolve.calls.components.InferenceSession;
import cn.cangnova.cangjie.resolve.extensions.SyntheticResolveExtension;
import cn.cangnova.cangjie.resolve.lazy.declarations.DeclarationProviderFactory;
import cn.cangnova.cangjie.resolve.lazy.declarations.LazyPackageDescriptor;
import cn.cangnova.cangjie.resolve.lazy.declarations.PackageMemberDeclarationProvider;
import cn.cangnova.cangjie.resolve.lazy.descriptors.LazyAnnotations;
import cn.cangnova.cangjie.resolve.lazy.descriptors.LazyAnnotationsContextImpl;
import cn.cangnova.cangjie.resolve.scopes.LexicalScope;
import cn.cangnova.cangjie.storage.*;
import cn.cangnova.cangjie.types.WrappedTypeFactory;
import cn.cangnova.cangjie.types.checker.NewCangJieTypeChecker;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.Project;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import jakarta.inject.Inject;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ResolveSession implements CangJieCodeAnalyzer, LazyClassContext {
    private final LazyResolveStorageManager storageManager;
    private final ExceptionTracker exceptionTracker;

    private final ModuleDescriptor module;
    private final MemoizedFunctionToNotNull<CjFile, LazyAnnotations> fileAnnotations;
    private final BindingTrace trace;
    private final CacheWithNotNullValues<FqName, LazyPackageDescriptor> packages;
    private final SyntheticResolveExtension syntheticResolveExtension;
    private final PackageFragmentProvider packageFragmentProvider;
    private final NewCangJieTypeChecker cangjieTypeChecker;
    private final DeclarationProviderFactory declarationProviderFactory;
    private final Project project;
    private ControlFlowAnalyzer controlFlowAnalyzer;
    private LazyDeclarationResolver lazyDeclarationResolver;
    private LocalDescriptorResolver localDescriptorResolver;
    private DelegationFilter delegationFilter;
    private WrappedTypeFactory wrappedTypeFactory;
    private DescriptorResolver descriptorResolver;
    private SupertypeLoopChecker supertypeLoopsResolver;
    private ExtendDescriptorResolver extendDescriptorResolver;
    private FunctionDescriptorResolver functionDescriptorResolver;
    private FileScopeProvider fileScopeProvider;
    private DeclarationScopeProvider declarationScopeProvider;
    private LookupTracker lookupTracker;
    private LanguageVersionSettings languageVersionSettings;
    private TypeResolver typeResolver;
    private SealedClassInheritorsProvider sealedClassInheritorsProvider;
    private OverloadChecker overloadChecker;
    private OverloadResolver overloadResolver;
    private AnnotationResolver annotationResolver;
    private EnumDescriptorResolver enumDescriptorResolver;
    // Only calls from injectors expected
    @Deprecated
    public ResolveSession(
            @NotNull Project project,
            @NotNull GlobalContext globalContext,
            @NotNull ModuleDescriptor rootDescriptor,
            @NotNull DeclarationProviderFactory declarationProviderFactory,
            @NotNull BindingTrace delegationTrace,
            @NotNull NewCangJieTypeChecker cangjieTypeChecker
    ) {
        LockBasedLazyResolveStorageManager lockBasedLazyResolveStorageManager =
                new LockBasedLazyResolveStorageManager(globalContext.getStorageManager());

        this.storageManager = lockBasedLazyResolveStorageManager;
        this.exceptionTracker = globalContext.getExceptionTracker();
        this.trace = lockBasedLazyResolveStorageManager.createSafeTrace(delegationTrace);
        this.module = rootDescriptor;

        this.packages = storageManager.createCacheWithNotNullValues();

        this.declarationProviderFactory = declarationProviderFactory;
        this.cangjieTypeChecker = cangjieTypeChecker;
        this.project = project;
        fileAnnotations = storageManager.createMemoizedFunction(file -> createAnnotations(file, Collections.emptyList()));

        this.packageFragmentProvider = new PackageFragmentProviderOptimized() {
            @Override
            public void collectPackageFragments(
                    @NotNull FqName fqName, @NotNull Collection<PackageFragmentDescriptor> packageFragments
            ) {
                LazyPackageDescriptor fragment = getPackageFragment(fqName);
                if (fragment != null) {
                    packageFragments.add(fragment);
                }
            }

            @Override
            public boolean isEmpty(@NotNull FqName fqName) {
                PackageMemberDeclarationProvider provider = declarationProviderFactory.getPackageMemberDeclarationProvider(fqName);
                return provider == null;
            }

            @NotNull
            @Override
            public List<PackageFragmentDescriptor> getPackageFragments(@NotNull FqName fqName) {
                return ContainerUtil.createMaybeSingletonList(getPackageFragment(fqName));
            }

            @NotNull
            @Override
            public Collection<FqName> getSubPackagesOf(
                    @NotNull FqName fqName, @NotNull Function1<? super Name, Boolean> nameFilter
            ) {
                LazyPackageDescriptor packageDescriptor = getPackageFragment(fqName);
                if (packageDescriptor == null) {
                    return Collections.emptyList();
                }
                return packageDescriptor.getDeclarationProvider().getAllDeclaredSubPackages(nameFilter);
            }
        };
        syntheticResolveExtension = SyntheticResolveExtension.Companion.getInstance(project);

    }

    public static boolean areDescriptorsCreatedForDeclaration(@NotNull CjDeclaration declaration) {
        return !(declaration instanceof CjAnonymousInitializer || declaration instanceof CjDestructuringDeclaration);
    }

    public ExceptionTracker getExceptionTracker() {
        return exceptionTracker;
    }

    @Override
    @NotNull
    public PackageFragmentProvider getPackageFragmentProvider() {
        return packageFragmentProvider;
    }

    public BindingTrace getTrace() {
        return trace;
    }

    @Nullable
    public ControlFlowAnalyzer getControlFlowAnalyzer() {
        return this.controlFlowAnalyzer;
    }

    @Inject
    public void setControlFlowAnalyzer(ControlFlowAnalyzer controlFlowAnalyzer) {
        this.controlFlowAnalyzer = controlFlowAnalyzer;
    }

    @Inject
    public void setAnnotationResolve(AnnotationResolver annotationResolver) {
        this.annotationResolver = annotationResolver;
    }

    public DeclarationProviderFactory getDeclarationProviderFactory() {
        return declarationProviderFactory;
    }

    @NotNull
    public Annotations getFileAnnotations(@NotNull CjFile file) {
        return fileAnnotations.invoke(file);
    }

    @Inject
    public void setLocalDescriptorResolver(@NotNull LocalDescriptorResolver localDescriptorResolver) {
        this.localDescriptorResolver = localDescriptorResolver;
    }

    @Inject
    public void setSupertypeLoopsResolver(@NotNull SupertypeLoopChecker supertypeLoopsResolver) {
        this.supertypeLoopsResolver = supertypeLoopsResolver;
    }

    private LazyAnnotations createAnnotations(CjFile file, List<CjAnnotationEntry> annotationEntries) {
        LexicalScope scope = fileScopeProvider.getFileResolutionScope(file);
        LazyAnnotationsContextImpl lazyAnnotationContext =
                new LazyAnnotationsContextImpl(annotationResolver, storageManager, trace, scope);
        return new LazyAnnotations(lazyAnnotationContext, annotationEntries);
    }

    @NotNull
    @Override
    public ModuleDescriptor getModuleDescriptor() {
        return module;
    }

    @Inject
    public void setLazyDeclarationResolver(LazyDeclarationResolver lazyDeclarationResolver) {
        this.lazyDeclarationResolver = lazyDeclarationResolver;
    }

    @Override
    @NotNull
    public DeclarationDescriptor resolveToDescriptor(@NotNull CjDeclaration declaration) {
        assertValid();
        if (!areDescriptorsCreatedForDeclaration(declaration)) {
            throw new IllegalStateException(
                    "No descriptors are created for declarations of type " + declaration.getClass().getSimpleName()
                            + "\n. Change the caller accordingly"
            );
        }
        final boolean isLocal = ReadAction.compute(() -> CjPsiUtil.isLocal(declaration));
        if (!isLocal) {
            return lazyDeclarationResolver.resolveToDescriptor(declaration);
        }
        return localDescriptorResolver.resolveLocalDeclaration(declaration);
    }

    public FileScopeProvider getFileScopeProvider() {
        return fileScopeProvider;
    }

    @Inject
    public void setFileScopeProvider(FileScopeProvider fileScopeProvider) {
        this.fileScopeProvider = fileScopeProvider;
    }

    @NotNull
    @Override
    public BindingContext getBindingContext() {
        return trace.getBindingContext();

    }

    @Nullable
    @Override
    public InferenceSession getInferenceSession() {
        return null;
    }

    @NotNull
    @Override
    public DescriptorResolver getDescriptorResolver() {
        return descriptorResolver;

    }

    @Inject
    public void setDescriptorResolver(DescriptorResolver descriptorResolver) {
        this.descriptorResolver = descriptorResolver;
    }

    @Nullable
    @Override
    public LookupTracker getLookupTracker() {
        return lookupTracker;

    }

    @Inject
    public void setLookupTracker(@NotNull LookupTracker lookupTracker) {
        this.lookupTracker = lookupTracker;
    }

    @NotNull
    @Override
    public LazyResolveStorageManager getStorageManager() {
        return storageManager;

    }

    @NotNull
    @Override
    public FunctionDescriptorResolver getFunctionDescriptorResolver() {
        return functionDescriptorResolver;

    }

    @Inject
    public void setFunctionDescriptorResolver(FunctionDescriptorResolver functionDescriptorResolver) {
        this.functionDescriptorResolver = functionDescriptorResolver;
    }

    @NotNull
    @Override
    public DeclarationScopeProvider getDeclarationScopeProvider() {
        return declarationScopeProvider;
    }

    @Inject
    public void setDeclarationScopeProvider(DeclarationScopeProvider declarationScopeProvider) {
        this.declarationScopeProvider = declarationScopeProvider;
    }

    @NotNull
    @Override
    public LazyPackageDescriptor getPackageFragmentOrDiagnoseFailure(@NotNull FqName fqName, @Nullable CjFile from) {
        LazyPackageDescriptor packageDescriptor = getPackageFragment(fqName);
        if (packageDescriptor == null) {
            declarationProviderFactory.diagnoseMissingPackageFragment(fqName, from);
            assert false : "diagnoseMissingPackageFragment should throw!";
        }
        return packageDescriptor;
    }

    @Nullable
    @Override
    public LazyPackageDescriptor getPackageFragment(@NotNull FqName fqName) {
        PackageMemberDeclarationProvider provider = declarationProviderFactory.getPackageMemberDeclarationProvider(fqName);
        if (provider == null) {
            return null;
        }

        return packages.computeIfAbsent(
                fqName,
                () -> new LazyPackageDescriptor(module, fqName, this, provider)
        );
    }

    @Override
    public void assertValid() {
        module.assertValid();

    }

    @NotNull
    @Override
    public NewCangJieTypeChecker getCangjieTypeCheckerOfOwnerModule() {
        return cangjieTypeChecker;

    }

    @NotNull
    @Override
    public LanguageVersionSettings getLanguageVersionSettings() {
        return languageVersionSettings;
    }

    @Inject
    public void setLanguageVersionSettings(@NotNull LanguageVersionSettings languageVersionSettings) {
        this.languageVersionSettings = languageVersionSettings;
    }

    @NotNull
    @Override
    public DelegationFilter getDelegationFilter() {
        return delegationFilter;

    }

    @Inject
    public void setDelegationFilter(@NotNull DelegationFilter delegationFilter) {
        this.delegationFilter = delegationFilter;
    }

    @NotNull
    @Override
    public TypeResolver getTypeResolver() {
        return typeResolver;

    }

    @Inject
    public void setTypeResolver(TypeResolver typeResolver) {
        this.typeResolver = typeResolver;
    }

    @NotNull
    @Override
    public SupertypeLoopChecker getSupertypeLoopChecker() {
        return supertypeLoopsResolver;
    }

    @NotNull
    @Override
    public SyntheticResolveExtension getSyntheticResolveExtension() {
        return syntheticResolveExtension;

    }

    @NotNull
    @Override
    public WrappedTypeFactory getWrappedTypeFactory() {
        return wrappedTypeFactory;

    }

    @Inject
    public void setWrappedTypeFactory(@NotNull WrappedTypeFactory wrappedTypeFactory) {
        this.wrappedTypeFactory = wrappedTypeFactory;
    }

    @NotNull
    @Override
    public SealedClassInheritorsProvider getSealedClassInheritorsProvider() {
        return sealedClassInheritorsProvider;
    }

    @Inject
    public void setSealedClassInheritorsProvider(@NotNull SealedClassInheritorsProvider sealedClassInheritorsProvider) {
        this.sealedClassInheritorsProvider = sealedClassInheritorsProvider;
    }

    @NotNull
    @Override
    public ExtendDescriptorResolver getExtendDescriptorResolver() {
        return extendDescriptorResolver;
    }

    @Inject
    public void setExtendDescriptorResolver(@NotNull ExtendDescriptorResolver extendDescriptorResolver) {
        this.extendDescriptorResolver = extendDescriptorResolver;
    }

    @Override
    @NotNull
    public ClassDescriptor getClassDescriptor(@NotNull CjTypeStatement classOrObject, @NotNull LookupLocation location) {
        return lazyDeclarationResolver.getClassDescriptor(classOrObject, location);
    }

    @Override
    public @NotNull Collection<ClassifierDescriptor> getTopLevelClassifierDescriptors(@NotNull FqName fqName, @NotNull LookupLocation location) {
        if (fqName.isRoot()) return Collections.emptyList();

        PackageMemberDeclarationProvider provider = declarationProviderFactory.getPackageMemberDeclarationProvider(fqName.parent());
        if (provider == null) return Collections.emptyList();

        Collection<ClassifierDescriptor> result = new SmartList<>();

        result.addAll(ContainerUtil.mapNotNull(
                provider.getTypeStatementDeclarations(fqName.shortName()),
                classOrObjectInfo -> getClassDescriptor(classOrObjectInfo.getCorrespondingClass(), location)
        ));

//        result.addAll(ContainerUtil.mapNotNull(
//                provider.getScriptDeclarations(fqName.shortName()),
//                scriptInfo -> getScriptDescriptor(scriptInfo.getScript())
//        ));

        result.addAll(ContainerUtil.map(
                provider.getTypeAliasDeclarations(fqName.shortName()),
                alias -> (ClassifierDescriptor) lazyDeclarationResolver.resolveToDescriptor(alias)
        ));

        return result;
    }
    @Inject
    public void setOverloadResolver(@NotNull OverloadResolver overloadResolver) {
        this.overloadResolver = overloadResolver;
    }
    @Override
    public @NotNull OverloadResolver getOverloadResolver() {
        return overloadResolver;
    }
    @Override
    public @NotNull OverloadChecker getOverloadChecker() {
        return overloadChecker;
    }
    @Inject
    public void setEnumDescriptorResolver(@NotNull EnumDescriptorResolver enumDescriptorResolver) {
        this.enumDescriptorResolver = enumDescriptorResolver;
    }
    @Inject
    public void setOverloadChecker(@NotNull OverloadChecker overloadChecker) {
        this.overloadChecker = overloadChecker;
    }

    @Override
    public @NotNull EnumDescriptorResolver getEnumDescriptorResolver() {
        return  enumDescriptorResolver;
    }
}
