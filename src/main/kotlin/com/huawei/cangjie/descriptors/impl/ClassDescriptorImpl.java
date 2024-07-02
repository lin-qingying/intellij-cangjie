package com.huawei.cangjie.descriptors.impl;


import com.huawei.cangjie.descriptors.*;
import com.huawei.cangjie.descriptors.annotations.Annotations;
import com.huawei.cangjie.name.Name;
import com.huawei.cangjie.resolve.scopes.MemberScope;
import com.huawei.cangjie.storage.StorageManager;
import com.huawei.cangjie.types.*;
import com.huawei.cangjie.types.checker.CangJieTypeRefiner;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class ClassDescriptorImpl extends ClassDescriptorBase {
    private final Modality modality;
    private final ClassKind kind;
//    private final TypeConstructor typeConstructor;

    private MemberScope unsubstitutedMemberScope;
    private Set<ClassConstructorDescriptor> constructors;
    private ClassConstructorDescriptor primaryConstructor;

    public ClassDescriptorImpl(
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull Name name,
            @NotNull Modality modality,
            @NotNull ClassKind kind,
            @NotNull Collection<CangJieType> supertypes,
            @NotNull SourceElement source,
            boolean isExternal,
            @NotNull StorageManager storageManager
    ) {
        super(storageManager, containingDeclaration, name, source, isExternal);
        assert modality != Modality.SEALED : "Implement getSealedSubclasses() for this class: " + getClass();
        this.modality = modality;
        this.kind = kind;

//        this.typeConstructor = new ClassTypeConstructorImpl(this, Collections.<TypeParameterDescriptor>emptyList(), supertypes, storageManager);
    }
    @NotNull
    @Override
    public MemberScope getUnsubstitutedMemberScope(@NotNull CangJieTypeRefiner cangjieTypeRefiner) {
        return unsubstitutedMemberScope;
    }
    public final void initialize(
            @NotNull MemberScope unsubstitutedMemberScope,
            @NotNull Set<ClassConstructorDescriptor> constructors,
            @Nullable ClassConstructorDescriptor primaryConstructor
    ) {
        this.unsubstitutedMemberScope = unsubstitutedMemberScope;
        this.constructors = constructors;
        this.primaryConstructor = primaryConstructor;
    }

//    @NotNull
//    @Override
//    public Annotations getAnnotations() {
//        return Annotations.Companion.getEMPTY();
//    }
//
//    @Override
//    @NotNull
//    public TypeConstructor getTypeConstructor() {
//        return typeConstructor;
//    }
//
//    @NotNull
//    @Override
//    public Collection<ClassConstructorDescriptor> getConstructors() {
//        return constructors;
//    }
//
//    @NotNull
//    @Override
//    public MemberScope getUnsubstitutedMemberScope(@NotNull CangJieTypeRefiner kotlinTypeRefiner) {
//        return unsubstitutedMemberScope;
//    }
//
//    @NotNull
//    @Override
//    public MemberScope getStaticScope() {
//        return MemberScope.Empty.INSTANCE;
//    }
//
//    @Nullable
//    @Override
//    public ClassDescriptor getCompanionObjectDescriptor() {
//        return null;
//    }
//
//    @NotNull
//    @Override
//    public ClassKind getKind() {
//        return kind;
//    }

//    @Override
//    public boolean isCompanionObject() {
//        return false;
//    }
//
//    @Override
//    public boolean isExpect() {
//        return false;
//    }
//
//    @Override
//    public boolean isActual() {
//        return false;
//    }

    @Override
    public ClassConstructorDescriptor getUnsubstitutedPrimaryConstructor() {
        return primaryConstructor;
    }



    @Override
    public @NotNull MemberScope getUnsubstitutedMemberScope() {
        return unsubstitutedMemberScope;

    }

    @Override
    public @NotNull MemberScope getStaticScope() {
        return MemberScope.Empty.INSTANCE;

    }

    @Override
    public @NotNull Collection<ClassConstructorDescriptor> getConstructors() {
        return constructors;

    }

    @Override
    public @Nullable ClassDescriptor getCompanionObjectDescriptor() {
        return null;
    }

    @Override
    public @NotNull ClassKind getKind() {
        return kind;
    }

    @Override
    @NotNull
    public Modality getModality() {
        return modality;
    }

    @NotNull
    @Override
    public DescriptorVisibility getVisibility() {
        throw new UnsupportedOperationException("replaceBindingTrace is not implemented");

//        return DescriptorVisibilities.PUBLIC;
    }
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

//    @Override
//    public boolean isInner() {
//        return false;
//    }

    @Override
    public String toString() {
        return "class " + getName();
    }

    @NotNull
    @Override
    public List<TypeParameterDescriptor> getDeclaredTypeParameters() {
        return Collections.emptyList();
    }

    @NotNull
    @Override
    public Collection<ClassDescriptor> getSealedSubclasses() {
        return Collections.emptyList();
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



//    @Nullable
//    @Override
//    public ValueClassRepresentation<SimpleType> getValueClassRepresentation() {
//        return null;
//    }
}
