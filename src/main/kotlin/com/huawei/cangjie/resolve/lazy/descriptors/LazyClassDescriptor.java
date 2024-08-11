package com.huawei.cangjie.resolve.lazy.descriptors;

import com.huawei.cangjie.descriptors.*;
import com.huawei.cangjie.descriptors.annotations.Annotations;
import com.huawei.cangjie.descriptors.impl.ClassDescriptorBase;
import com.huawei.cangjie.name.Name;
import com.huawei.cangjie.resolve.DescriptorUtils;
import com.huawei.cangjie.resolve.lazy.LazyClassContext;
import com.huawei.cangjie.resolve.lazy.LazyEntity;
import com.huawei.cangjie.resolve.scopes.LexicalScope;
import com.huawei.cangjie.resolve.scopes.MemberScope;
import com.huawei.cangjie.types.TypeConstructor;
import com.huawei.cangjie.types.checker.CangJieTypeRefiner;
import kotlin.collections.CollectionsKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

public class LazyClassDescriptor extends ClassDescriptorBase implements ClassDescriptorWithResolutionScopes, LazyEntity {
//    private final NotNullLazyValue<LexicalScope> scopeForInitializerResolution;

//    private final ClassResolutionScopesSupport resolutionScopesSupport;
    private final LazyClassContext c;
//    private final ClassMemberDeclarationProvider declarationProvider;

    protected LazyClassDescriptor(@NotNull LazyClassContext c, @NotNull DeclarationDescriptor containingDeclaration, @NotNull Name name, @NotNull SourceElement source, boolean isExternal) {
        super(c.getStorageManager(), containingDeclaration, name, source, isExternal);
        this.c = c;
//        this.declarationProvider = c.getDeclarationProviderFactory().getClassMemberDeclarationProvider(classLikeInfo);
//
//        StorageManager storageManager = c.getStorageManager();
//
//        this.resolutionScopesSupport = new ClassResolutionScopesSupport(
//                this,
//                storageManager,
//                c.getLanguageVersionSettings(),
//                this::getOuterScope
//        );

    }
//

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
    public @NotNull MemberScope getUnsubstitutedMemberScope() {
        return null;
    }

    @Override
    public @NotNull MemberScope getStaticScope() {
        return null;
    }

    @Override
    public @NotNull Collection<ClassConstructorDescriptor> getConstructors() {
        return null;
    }

    @Override
    public @NotNull ClassKind getKind() {
        return null;
    }

    @Override
    public @NotNull Modality getModality() {
        return null;
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
        return null;
    }

    @Override
    public @NotNull Collection<ClassDescriptor> getSealedSubclasses() {
        return null;
    }

    @Override
    public @NotNull TypeConstructor getTypeConstructor() {
        return null;
    }

    @NotNull
    @Override
    public Annotations getAnnotations() {
        return null;
    }

    @NotNull
    @Override
    public MemberScope getUnsubstitutedMemberScope(@NotNull CangJieTypeRefiner cangjieTypeRefiner) {
        return null;
    }

    @Override
    public void forceResolveAllContents() {

    }

    @Override
    public @NotNull LexicalScope getScopeForMemberDeclarationResolution() {
//        return resolutionScopesSupport.getScopeForMemberDeclarationResolution().invoke();


        throw new UnsupportedOperationException();
    }
}
