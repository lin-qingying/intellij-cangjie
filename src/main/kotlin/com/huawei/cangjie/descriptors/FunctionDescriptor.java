package com.huawei.cangjie.descriptors;

import com.huawei.cangjie.mpp.FunctionSymbolMarker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public interface FunctionDescriptor extends CallableMemberDescriptor, FunctionSymbolMarker {
    boolean isHiddenForResolutionEverywhereBesideSupercalls();
    boolean isSuspend();

    @NotNull    /**
     * @return true if descriptor signature clashed with some other signature and it's supposed to be legal
     * See java.nio.CharBuffer
     */
    boolean isHiddenToOvercomeSignatureClash();
    @Override
    FunctionDescriptor copy(DeclarationDescriptor newOwner, Modality modality, DescriptorVisibility visibility, Kind kind, boolean copyOverrides);
    @NotNull
    @Override
    FunctionDescriptor getOriginal();
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
}