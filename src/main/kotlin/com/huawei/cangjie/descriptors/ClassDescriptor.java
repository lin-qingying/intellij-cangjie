/*
 * Copyright 2000-2018 JetBrains s.r.o. and CangJie Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.huawei.cangjie.descriptors;


import com.huawei.cangjie.mpp.RegularClassSymbolMarker;
import com.huawei.cangjie.psi.CjSuperTypeListEntry;
import com.huawei.cangjie.resolve.scopes.MemberScope;
import com.huawei.cangjie.types.SimpleType;
import com.huawei.cangjie.types.TypeProjection;
import com.huawei.cangjie.types.TypeSubstitution;
import com.huawei.cangjie.types.expressions.match.ClassAndEnumConstructorDescriptor;
import com.huawei.cangjie.utils.ReadOnly;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public interface ClassDescriptor extends ClassifierDescriptorWithTypeParameters, ClassOrPackageFragmentDescriptor,
        RegularClassSymbolMarker, ClassAndEnumConstructorDescriptor {
    @NotNull
    MemberScope getMemberScope(@NotNull List<? extends TypeProjection> typeArguments);

    @NotNull
    ReceiverParameterDescriptor getThisAsReceiverParameter();

    @NotNull
    @ReadOnly
    List<ReceiverParameterDescriptor> getContextReceivers();

    @NotNull
    MemberScope getMemberScope(@NotNull TypeSubstitution typeSubstitution);

    @NotNull
    MemberScope getUnsubstitutedMemberScope();

    @NotNull
    MemberScope getUnsubstitutedInnerClassesScope();

    @NotNull
    default MemberScope getInstanceScope() {

        return MemberScope.Empty.INSTANCE;
    }

    @NotNull
    MemberScope getStaticScope();

    @NotNull
    @ReadOnly
    Collection<ClassConstructorDescriptor> getConstructors();

    @NotNull
    @ReadOnly
    Collection<ClassConstructorDescriptor> getEndConstructors();

    @Override
    @NotNull
    DeclarationDescriptor getContainingDeclaration();

    /**
     * @return type A&lt;T&gt; for the class A&lt;T&gt;
     */
    @NotNull
    @Override
    SimpleType getDefaultType();

    /**
     * @return nested object declared as 'companion' if one is present.
     */

    @NotNull
    ClassKind getKind();

    @Override
    @NotNull
    Modality getModality();

    @Override
    @NotNull
    DescriptorVisibility getVisibility();

//    bool isCompanionObject();

//    bool isData();
//
//    bool isInline();

    boolean isFun();

    boolean isValue();


    @Nullable
    ClassConstructorDescriptor getUnsubstitutedPrimaryConstructor();

    /**
     * It may differ from 'typeConstructor.parameters' in current class is inner, 'typeConstructor.parameters' contains
     * captured parameters from outer declaration.
     *
     * @return list of type parameters actually declared type parameters in current class
     */
    @Override
    @ReadOnly
    @NotNull
    List<TypeParameterDescriptor> getDeclaredTypeParameters();

    /**
     * @return direct subclasses of this class if it's a sealed class, empty list otherwise
     */
    @ReadOnly
    @NotNull
    Collection<ClassDescriptor> getSealedSubclasses();

    @NotNull
    @Override
    ClassDescriptor getOriginal();

    // Use SingleAbstractMethodUtils.getFunctionTypeForSamInterface() where possible. This is only a fallback
    @Nullable
    SimpleType getDefaultFunctionTypeForSamInterface();

    /**
     * May return false even in case when the class is not SAM interface, but returns true only if it's definitely not a SAM.
     * But it should work much faster than the exact check.
     */
    boolean isDefinitelyNotSamInterface();

    /**
     * 获取所有SuperTypeListEntry 包含扩展
     */
    default List<CjSuperTypeListEntry> getSuperTypeListEntries() {

        return Collections.emptyList();
    }

}
