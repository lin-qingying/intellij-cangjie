package com.huawei.cangjie.resolve;

import com.huawei.cangjie.descriptors.CallableDescriptor;
import com.huawei.cangjie.descriptors.ClassDescriptor;
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
