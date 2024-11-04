package com.linqingying.cangjie.resolve.calls.model;


import com.linqingying.cangjie.psi.ValueArgument;
import com.linqingying.cangjie.resolve.calls.smartcasts.DataFlowInfo;
import org.jetbrains.annotations.NotNull;

public abstract class MutableDataFlowInfoForArguments implements DataFlowInfoForArguments {

    @NotNull
    protected final DataFlowInfo initialDataFlowInfo;

    public MutableDataFlowInfoForArguments(@NotNull DataFlowInfo initialDataFlowInfo) {
        this.initialDataFlowInfo = initialDataFlowInfo;
    }

    public abstract void updateInfo(@NotNull ValueArgument valueArgument, @NotNull DataFlowInfo dataFlowInfo);
    public abstract void updateResultInfo(@NotNull DataFlowInfo dataFlowInfo);

    @NotNull
    @Override
    public DataFlowInfo getResultInfo() {
        return initialDataFlowInfo;
    }

    public static class WithoutArgumentsCheck extends MutableDataFlowInfoForArguments {

        public WithoutArgumentsCheck(@NotNull DataFlowInfo dataFlowInfo) {
            super(dataFlowInfo);
        }

        @Override
        public void updateInfo(@NotNull ValueArgument valueArgument, @NotNull DataFlowInfo dataFlowInfo) {
            throw new IllegalStateException();
        }

        @Override
        public void updateResultInfo(@NotNull DataFlowInfo dataFlowInfo) {
            throw new IllegalStateException();
        }

        @NotNull
        @Override
        public DataFlowInfo getInfo(@NotNull ValueArgument valueArgument) {
            throw new IllegalStateException();
        }
    };
}
