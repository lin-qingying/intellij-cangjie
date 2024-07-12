package com.huawei.cangjie.descriptors;

import com.huawei.cangjie.name.Name;
import com.huawei.cangjie.types.CangJieType;
import com.huawei.cangjie.types.TypeSubstitution;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

public interface CallableMemberDescriptor extends CallableDescriptor, MemberDescriptor {
    /**
     * Is this a real function or function projection.
     */
    @NotNull
    Kind getKind();
    void setOverriddenDescriptors(@NotNull Collection<? extends CallableMemberDescriptor> overriddenDescriptors);

    @NotNull
    @Override
    Collection<? extends CallableMemberDescriptor> getOverriddenDescriptors();

    @NotNull
    CallableMemberDescriptor copy(DeclarationDescriptor newOwner, Modality modality, DescriptorVisibility visibility, Kind kind, boolean copyOverrides);

    @NotNull
    CopyBuilder<? extends CallableMemberDescriptor> newCopyBuilder();

    enum Kind {
        DECLARATION,
        FAKE_OVERRIDE,
        DELEGATION,
        SYNTHESIZED;

        public boolean isReal() {
            return this != FAKE_OVERRIDE;
        }
    }

    interface CopyBuilder<D extends CallableMemberDescriptor> {
        @NotNull
        CopyBuilder<D> setOwner(@NotNull DeclarationDescriptor owner);

        @NotNull
        CopyBuilder<D> setModality(@NotNull Modality modality);

        @NotNull
        CopyBuilder<D> setVisibility(@NotNull DescriptorVisibility visibility);

        @NotNull
        CopyBuilder<D> setKind(@NotNull Kind kind);

        @NotNull
        CopyBuilder<D> setTypeParameters(@NotNull List<TypeParameterDescriptor> parameters);

        @NotNull
        CopyBuilder<D> setDispatchReceiverParameter(@Nullable ReceiverParameterDescriptor dispatchReceiverParameter);

        @NotNull
        CopyBuilder<D> setSubstitution(@NotNull TypeSubstitution substitution);

        @NotNull
        CopyBuilder<D> setCopyOverrides(boolean copyOverrides);

        @NotNull
        CopyBuilder<D> setName(@NotNull Name name);

        @NotNull
        CopyBuilder<D> setOriginal(@Nullable CallableMemberDescriptor original);

        @NotNull
        CopyBuilder<D> setPreserveSourceElement();

        @NotNull
        CopyBuilder<D> setReturnType(@NotNull CangJieType type);

        @Nullable
        D build();
    }
}
