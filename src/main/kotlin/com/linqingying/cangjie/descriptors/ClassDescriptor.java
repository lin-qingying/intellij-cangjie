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

package com.linqingying.cangjie.descriptors;


import com.linqingying.cangjie.mpp.RegularClassSymbolMarker;
import com.linqingying.cangjie.psi.CjSuperTypeListEntry;
import com.linqingying.cangjie.resolve.scopes.MemberScope;
import com.linqingying.cangjie.types.SimpleType;
import com.linqingying.cangjie.types.TypeProjection;
import com.linqingying.cangjie.types.TypeSubstitution;
import com.linqingying.cangjie.types.expressions.match.ClassAndEnumConstructorDescriptor;
import com.linqingying.cangjie.utils.ReadOnly;
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
    // 获取静态作用域
    MemberScope getStaticScope();

    @NotNull
    // 获取构造函数的集合
    @ReadOnly
    Collection<ClassConstructorDescriptor> getConstructors();

    @NotNull
    // 获取结束构造函数的集合
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

    /**
     * 是否具有const 构造函数
     */
    default boolean hasConstConstructor() {
        Collection<ClassConstructorDescriptor> constructors = getConstructors();
        for (ClassConstructorDescriptor constructor : constructors) {
            if (constructor.isConst()) {
                return true;
            }
        }


        return false;
    }

}
