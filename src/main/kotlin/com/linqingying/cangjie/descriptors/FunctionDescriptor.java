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

import com.linqingying.cangjie.descriptors.annotations.Annotations;
import com.linqingying.cangjie.mpp.FunctionSymbolMarker;
import com.linqingying.cangjie.name.Name;
import com.linqingying.cangjie.types.CangJieType;
import com.linqingying.cangjie.types.TypeSubstitution;
import com.linqingying.cangjie.types.TypeSubstitutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;


public interface FunctionDescriptor extends CallableMemberDescriptor, FunctionSymbolMarker {
    @NotNull
    boolean getIsExtend();


    @Override
    @NotNull
    DeclarationDescriptor getContainingDeclaration();

    @NotNull
    @Override
    FunctionDescriptor getOriginal();

    @Nullable
    @Override
    FunctionDescriptor substitute(@NotNull TypeSubstitutor substitutor);

    /**
     * This method should be used with a great care, because if descriptor is substituted one, calling 'getOverriddenDescriptors'
     * may force lazy computation, that's unnecessary in most cases.
     * So, if 'getOriginal().getOverriddenDescriptors()' is enough for you, please use it instead.
     * @return
     */
    @Override
    @NotNull
    Collection<? extends FunctionDescriptor> getOverriddenDescriptors();

    /**
     * @return descriptor that represents initial signature, e.g in case of result SimpleFunctionDescriptor.createRenamedCopy it returns
     * descriptor before rename
     */
    @Nullable
    FunctionDescriptor getInitialSignatureDescriptor();

    /**
     * @return true if descriptor signature clashed with some other signature and it's supposed to be legal
     * See java.nio.CharBuffer
     */
    boolean isHiddenToOvercomeSignatureClash();

    @NotNull
    @Override
    FunctionDescriptor copy(DeclarationDescriptor newOwner, Modality modality, DescriptorVisibility visibility, Kind kind, boolean copyOverrides);

    boolean isOperator();
   default boolean isConst(){
       return false;
   }

//    bool isInfix();

//    bool isInline();

//    bool isTailrec();

    boolean isHiddenForResolutionEverywhereBesideSupercalls();

//    bool isSuspend();

    @NotNull
    @Override
    CopyBuilder<? extends FunctionDescriptor> newCopyBuilder();

    interface CopyBuilder<D extends FunctionDescriptor> extends CallableMemberDescriptor.CopyBuilder<D> {
        @NotNull
        @Override
        CopyBuilder<D> setOwner(@NotNull DeclarationDescriptor owner);

        @NotNull
        @Override
        CopyBuilder<D> setModality(@NotNull Modality modality);

        @NotNull
        @Override
        CopyBuilder<D> setVisibility(@NotNull DescriptorVisibility visibility);

        @NotNull
        @Override
        CopyBuilder<D> setKind(@NotNull Kind kind);

        @NotNull
        @Override
        CopyBuilder<D> setCopyOverrides(boolean copyOverrides);

        @Override
        @NotNull
        CopyBuilder<D> setName(@NotNull Name name);

        @NotNull
        CopyBuilder<D> setValueParameters(@NotNull List<ValueParameterDescriptor> parameters);

        @NotNull
        @Override
        CopyBuilder<D> setTypeParameters(@NotNull List<TypeParameterDescriptor> parameters);

        @NotNull
        @Override
        CopyBuilder<D> setReturnType(@NotNull CangJieType type);

        @NotNull
        CopyBuilder<D> setContextReceiverParameters(@NotNull List<ReceiverParameterDescriptor> contextReceiverParameters);

        @NotNull
        CopyBuilder<D> setExtensionReceiverParameter(@Nullable ReceiverParameterDescriptor extensionReceiverParameter);

        @NotNull
        @Override
        CopyBuilder<D> setDispatchReceiverParameter(@Nullable ReceiverParameterDescriptor dispatchReceiverParameter);

        @NotNull
        @Override
        CopyBuilder<D> setOriginal(@Nullable CallableMemberDescriptor original);

        @NotNull
        CopyBuilder<D> setSignatureChange();

        @NotNull
        @Override
        CopyBuilder<D> setPreserveSourceElement();

        @NotNull
        CopyBuilder<D> setDropOriginalInContainingParts();

        @NotNull
        CopyBuilder<D> setHiddenToOvercomeSignatureClash();

        @NotNull
        CopyBuilder<D> setHiddenForResolutionEverywhereBesideSupercalls();

        @NotNull
        CopyBuilder<D> setAdditionalAnnotations(@NotNull Annotations additionalAnnotations);

        @NotNull
        @Override
        CopyBuilder<D> setSubstitution(@NotNull TypeSubstitution substitution);

        @NotNull
        <V> CopyBuilder<D> putUserData(@NotNull UserDataKey<V> userDataKey, V value);

        @Nullable
        @Override
        D build();
    }
}
