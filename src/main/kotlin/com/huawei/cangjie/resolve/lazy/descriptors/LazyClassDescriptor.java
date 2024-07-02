package com.huawei.cangjie.resolve.lazy.descriptors;

import com.huawei.cangjie.descriptors.*;
import com.huawei.cangjie.descriptors.annotations.Annotations;
import com.huawei.cangjie.descriptors.impl.ClassDescriptorBase;
import com.huawei.cangjie.name.Name;
import com.huawei.cangjie.resolve.lazy.LazyEntity;
import com.huawei.cangjie.resolve.scopes.LexicalScope;
import com.huawei.cangjie.resolve.scopes.MemberScope;
import com.huawei.cangjie.storage.NotNullLazyValue;
import com.huawei.cangjie.storage.StorageManager;
import com.huawei.cangjie.types.TypeConstructor;
import com.huawei.cangjie.types.TypeProjection;
import com.huawei.cangjie.types.TypeSubstitution;
import com.huawei.cangjie.types.TypeSubstitutor;
import com.huawei.cangjie.types.checker.CangJieTypeRefiner;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

public class LazyClassDescriptor extends ClassDescriptorBase implements ClassDescriptorWithResolutionScopes, LazyEntity {
//    private final NotNullLazyValue<LexicalScope> scopeForInitializerResolution;


    protected LazyClassDescriptor(@NotNull StorageManager storageManager, @NotNull DeclarationDescriptor containingDeclaration, @NotNull Name name, @NotNull SourceElement source, boolean isExternal) {
        super(storageManager, containingDeclaration, name, source, isExternal);


    }
//    @Override
//    @NotNull
//    public LexicalScope getScopeForInitializerResolution() {
//        return scopeForInitializerResolution.invoke();
//    }
    @Override
    public @NotNull MemberScope getMemberScope(@NotNull List<? extends TypeProjection> typeArguments) {
        return null;
    }

    @Override
    public @NotNull MemberScope getMemberScope(@NotNull TypeSubstitution typeSubstitution) {
        return null;
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
    public @Nullable ClassDescriptor getCompanionObjectDescriptor() {
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
    public @NotNull DescriptorVisibility getVisibility() {
        return null;
    }

//    @Override
//    public boolean isExpect() {
//        return false;
//    }
//
//    @Override
//    public boolean isActual() {
//        return false;
//    }
//
//    @Override
//    public boolean isCompanionObject() {
//        return false;
//    }
//
//    @Override
//    public boolean isData() {
//        return false;
//    }
//
//    @Override
//    public boolean isInline() {
//        return false;
//    }

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

//    @Override
//    public boolean isInner() {
//        return false;
//    }

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
    public ClassifierDescriptorWithTypeParameters substitute(@NotNull TypeSubstitutor substitutor) {
        return null;
    }

    @NotNull
    @Override
    public Annotations getAnnotations() {
        return null;
    }

    @Override
    public void forceResolveAllContents() {

    }

    @NotNull
    @Override
    protected MemberScope getUnsubstitutedMemberScope(@NotNull CangJieTypeRefiner kotlinTypeRefiner) {
        return null;
    }
}
