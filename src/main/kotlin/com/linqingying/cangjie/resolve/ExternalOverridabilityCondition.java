package com.linqingying.cangjie.resolve;

import com.linqingying.cangjie.descriptors.CallableDescriptor;
import com.linqingying.cangjie.descriptors.ClassDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface ExternalOverridabilityCondition {
    enum Result {
        OVERRIDABLE, INCOMPATIBLE, UNKNOWN
    }

    enum Contract {
        CONFLICTS_ONLY, SUCCESS_ONLY, BOTH
    }

    @NotNull
    Result isOverridable(
            @NotNull CallableDescriptor superDescriptor,
            @NotNull CallableDescriptor subDescriptor,
            @Nullable ClassDescriptor subClassDescriptor
    );

    @NotNull
    Contract getContract();
}
