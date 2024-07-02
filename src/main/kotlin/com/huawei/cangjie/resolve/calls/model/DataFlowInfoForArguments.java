package com.huawei.cangjie.resolve.calls.model;

import com.huawei.cangjie.psi.ValueArgument;
import com.huawei.cangjie.resolve.calls.smartcasts.DataFlowInfo;
import org.jetbrains.annotations.NotNull;

public interface DataFlowInfoForArguments {
    @NotNull
    DataFlowInfo getInfo(@NotNull ValueArgument valueArgument);

    @NotNull
    DataFlowInfo getResultInfo();
}
