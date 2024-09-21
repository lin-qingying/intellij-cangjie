package com.huawei.cangjie.descriptors;


import com.huawei.cangjie.mpp.CallableSymbolMarker;
import com.huawei.cangjie.types.CangJieType;
import com.huawei.cangjie.utils.ReadOnly;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
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

    boolean hasStableParameterNames();

    interface UserDataKey<V> {
    }

}
