package com.linqingying.cangjie.resolve.calls.model;

import com.linqingying.cangjie.psi.ValueArgument;
import com.linqingying.cangjie.resolve.calls.smartcasts.DataFlowInfo;
import org.jetbrains.annotations.NotNull;

public interface DataFlowInfoForArguments {
    @NotNull
    DataFlowInfo getInfo(@NotNull ValueArgument valueArgument);

    @NotNull
    DataFlowInfo getResultInfo();
}
