package com.huawei.cangjie.resolve.lazy;

import com.huawei.cangjie.config.LanguageVersionSettings;
import com.huawei.cangjie.context.GlobalContext;
import com.huawei.cangjie.descriptors.*;
import com.huawei.cangjie.incremental.components.LookupTracker;
import com.huawei.cangjie.name.FqName;
import com.huawei.cangjie.name.Name;
import com.huawei.cangjie.psi.*;
import com.huawei.cangjie.resolve.BindingContext;
import com.huawei.cangjie.resolve.DescriptorResolver;
import com.huawei.cangjie.resolve.FunctionDescriptorResolver;
import com.huawei.cangjie.resolve.TypeResolver;
import com.huawei.cangjie.resolve.calls.components.InferenceSession;
import com.huawei.cangjie.resolve.lazy.declarations.DeclarationProviderFactory;
import com.huawei.cangjie.resolve.lazy.declarations.LazyPackageDescriptor;
import com.huawei.cangjie.resolve.lazy.declarations.PackageMemberDeclarationProvider;
import com.huawei.cangjie.storage.CacheWithNotNullValues;
import com.huawei.cangjie.storage.ExceptionTracker;
import com.huawei.cangjie.storage.LazyResolveStorageManager;
import com.huawei.cangjie.storage.LockBasedLazyResolveStorageManager;
import com.huawei.cangjie.types.checker.NewCangJieTypeChecker;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.Project;
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

    private final BindingTrace trace;
    private final CacheWithNotNullValues<FqName, LazyPackageDescriptor> packages;
    private DeclarationProviderFactory declarationProviderFactory;
    private LazyDeclarationResolver lazyDeclarationResolver;
    private LocalDescriptorResolver localDescriptorResolver;
    private DelegationFilter delegationFilter;

    private DescriptorResolver descriptorResolver;
    private final PackageFragmentProvider packageFragmentProvider;
    private SupertypeLoopChecker supertypeLoopsResolver;

    private FunctionDescriptorResolver functionDescriptorResolver;

    private FileScopeProvider fileScopeProvider;
    private DeclarationScopeProvider declarationScopeProvider;
    private LookupTracker lookupTracker;
    private Project project;
    private LanguageVersionSettings languageVersionSettings;

    private final NewCangJieTypeChecker cangjieTypeChecker;

    private TypeResolver typeResolver;

    public ExceptionTracker getExceptionTracker() {
        return exceptionTracker;
    }

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
    }

    public static boolean areDescriptorsCreatedForDeclaration(@NotNull CjDeclaration declaration) {
        return !(declaration instanceof CjAnonymousInitializer || declaration instanceof CjDestructuringDeclaration);
    }
    @Override
    @NotNull
    public PackageFragmentProvider getPackageFragmentProvider() {
        return packageFragmentProvider;
    }
    public BindingTrace getTrace() {
        return trace;
    }
    @Inject
    public void setLookupTracker(@NotNull LookupTracker lookupTracker) {
        this.lookupTracker = lookupTracker;
    }

    public DeclarationProviderFactory getDeclarationProviderFactory() {
        return declarationProviderFactory;
    }

    @Inject
    public void setTypeResolver(TypeResolver typeResolver) {
        this.typeResolver = typeResolver;
    }

    @Inject
    public void setLocalDescriptorResolver(@NotNull LocalDescriptorResolver localDescriptorResolver) {
        this.localDescriptorResolver = localDescriptorResolver;
    }
    @Inject
    public void setDescriptorResolver(DescriptorResolver descriptorResolver) {
        this.descriptorResolver = descriptorResolver;
    }
    @Inject
    public void setSupertypeLoopsResolver(@NotNull SupertypeLoopChecker supertypeLoopsResolver) {
        this.supertypeLoopsResolver = supertypeLoopsResolver;
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
    @Inject
    public void setLanguageVersionSettings(@NotNull LanguageVersionSettings languageVersionSettings) {
        this.languageVersionSettings = languageVersionSettings;
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

    @Nullable
    @Override
    public LookupTracker getLookupTracker() {
        return lookupTracker;

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

    @NotNull
    @Override
    public DelegationFilter getDelegationFilter() {
        return delegationFilter;

    }

    @NotNull
    @Override
    public TypeResolver getTypeResolver() {
        return typeResolver;

    }

    @Inject
    public void setDelegationFilter(@NotNull DelegationFilter delegationFilter) {
        this.delegationFilter = delegationFilter;
    }

    @NotNull
    @Override
        public SupertypeLoopChecker getSupertypeLoopChecker() {
        return supertypeLoopsResolver;
    }
}
