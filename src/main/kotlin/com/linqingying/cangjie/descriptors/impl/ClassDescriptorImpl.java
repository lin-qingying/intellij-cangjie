package com.linqingying.cangjie.descriptors.impl;


import com.linqingying.cangjie.descriptors.*;
import com.linqingying.cangjie.descriptors.annotations.Annotations;
import com.linqingying.cangjie.name.Name;
import com.linqingying.cangjie.resolve.scopes.InstanceMemberScope;
import com.linqingying.cangjie.resolve.scopes.MemberScope;
import com.linqingying.cangjie.resolve.scopes.StaticMemberScope;
import com.linqingying.cangjie.storage.StorageManager;
import com.linqingying.cangjie.types.*;
import com.linqingying.cangjie.types.checker.CangJieTypeRefiner;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class ClassDescriptorImpl extends ClassDescriptorBase {
    private final Modality modality;
    private final ClassKind kind;
    private final TypeConstructor typeConstructor;

    private MemberScope unsubstitutedMemberScope;
    private Set<ClassConstructorDescriptor> constructors;
    private ClassConstructorDescriptor primaryConstructor;
    private Set<ClassConstructorDescriptor> endConstructors;

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

        this.typeConstructor = new ClassTypeConstructorImpl(this, Collections.emptyList(), supertypes, storageManager);
    }
    @NotNull
    @Override
    public MemberScope getUnsubstitutedMemberScope(@NotNull CangJieTypeRefiner cangjieTypeRefiner) {
        return unsubstitutedMemberScope;
    }
    public final void initialize(
            @NotNull MemberScope unsubstitutedMemberScope,
            @NotNull Set<ClassConstructorDescriptor> constructors,
            @Nullable ClassConstructorDescriptor primaryConstructor,
            @NotNull Set<ClassConstructorDescriptor> endConstructors
    ) {
        this.unsubstitutedMemberScope = unsubstitutedMemberScope;
        this.constructors = constructors;
        this.primaryConstructor = primaryConstructor;
        this.endConstructors = endConstructors;
    }

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
        return new StaticMemberScope(unsubstitutedMemberScope);
    }

    @Override
    public @NotNull MemberScope getInstanceScope() {
        return new InstanceMemberScope(unsubstitutedMemberScope);
    }

    @Override
    public @NotNull Collection<ClassConstructorDescriptor> getConstructors() {
        return constructors;

    }

    @Override
    @NotNull
    public Set<ClassConstructorDescriptor> getEndConstructors() {
        return endConstructors;
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
        return DescriptorVisibilities.PUBLIC;


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
        return typeConstructor;

    }



    @NotNull
    @Override
    public Annotations getAnnotations() {
        return Annotations.EMPTY;
    }




}
