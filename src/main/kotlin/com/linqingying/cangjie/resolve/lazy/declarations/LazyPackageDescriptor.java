package com.linqingying.cangjie.resolve.lazy.declarations;


import com.linqingying.cangjie.descriptors.DeclarationDescriptorVisitor;
import com.linqingying.cangjie.descriptors.DeclarationDescriptorWithSource;
import com.linqingying.cangjie.descriptors.ModuleDescriptor;
import com.linqingying.cangjie.name.FqName;
import com.linqingying.cangjie.resolve.lazy.LazyEntity;
import com.linqingying.cangjie.resolve.lazy.ResolveSession;
import com.linqingying.cangjie.resolve.lazy.declarations.impl.PackageFragmentDescriptorImpl;
import com.linqingying.cangjie.resolve.scopes.MemberScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.linqingying.cangjie.resolve.lazy.ForceResolveUtil;

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
