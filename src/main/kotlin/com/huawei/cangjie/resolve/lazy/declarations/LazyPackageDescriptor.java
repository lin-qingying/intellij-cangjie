package com.huawei.cangjie.resolve.lazy.declarations;


import com.huawei.cangjie.descriptors.DeclarationDescriptorVisitor;
import com.huawei.cangjie.descriptors.DeclarationDescriptorWithSource;
import com.huawei.cangjie.descriptors.ModuleDescriptor;
import com.huawei.cangjie.name.FqName;
import com.huawei.cangjie.resolve.lazy.LazyEntity;
import com.huawei.cangjie.resolve.lazy.ResolveSession;
import com.huawei.cangjie.resolve.lazy.declarations.impl.PackageFragmentDescriptorImpl;
import com.huawei.cangjie.resolve.scopes.MemberScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.huawei.cangjie.resolve.lazy.ForceResolveUtil;

public class LazyPackageDescriptor extends PackageFragmentDescriptorImpl implements LazyEntity {
    private final MemberScope memberScope;
    private final PackageMemberDeclarationProvider declarationProvider;

    public LazyPackageDescriptor(
            @NotNull ModuleDescriptor module,
            @NotNull FqName fqName,
            @NotNull ResolveSession resolveSession,
            @NotNull PackageMemberDeclarationProvider declarationProvider
    ) {
        super(module, fqName);
        this.declarationProvider = declarationProvider;

        this.memberScope = new LazyPackageMemberScope(resolveSession, declarationProvider, this);
    }

    @NotNull
    @Override
    public MemberScope getMemberScope() {
        return memberScope;
    }

    @Override
    public void forceResolveAllContents() {
        ForceResolveUtil.forceResolveAllContents(memberScope);
    }

    @NotNull
    public PackageMemberDeclarationProvider getDeclarationProvider() {
        return declarationProvider;
    }




//    @Override
//    public @NotNull DeclarationDescriptorWithSource getOriginal() {
//        return null;
//    }
}
