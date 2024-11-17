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


import com.linqingying.cangjie.mpp.CallableSymbolMarker;
import com.linqingying.cangjie.types.CangJieType;
import com.linqingying.cangjie.utils.ReadOnly;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public interface CallableDescriptor extends DeclarationDescriptorWithVisibility, DeclarationDescriptorNonRoot,
        Substitutable<CallableDescriptor>, CallableSymbolMarker {
    @NotNull
    List<ValueParameterDescriptor> getValueParameters();

    @NotNull
    @Override
    CallableDescriptor getOriginal();

    @NotNull
    @ReadOnly
    List<ReceiverParameterDescriptor> getContextReceiverParameters();

    /**
     * Method may return null for not yet fully initialized object or if error occurred.
     */
    @Nullable
    CangJieType getReturnType();

    @Nullable
    ReceiverParameterDescriptor getExtensionReceiverParameter();

    @NotNull
    Collection<? extends CallableDescriptor> getOverriddenDescriptors();

    @Nullable
    ReceiverParameterDescriptor getDispatchReceiverParameter();
    /**
     * Sometimes parameter names are not available at all .
     * In this case, getName() returns synthetic names such as "p0", "p1" etc.
     */
    boolean hasSynthesizedParameterNames();
    @NotNull
    @ReadOnly
    List<TypeParameterDescriptor> getTypeParameters();
    @NotNull
    @ReadOnly
   default List<TypeParameterDescriptor> getTypeParametersNotExtend(){
        return Collections.emptyList();
    }
    @NotNull

    boolean hasStableParameterNames();

    interface UserDataKey<V> {
    }

}
