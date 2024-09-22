package com.huawei.cangjie.resolve.calls.model;

import com.huawei.cangjie.psi.Call;
import com.huawei.cangjie.psi.ValueArgument;
import com.huawei.cangjie.resolve.calls.smartcasts.DataFlowInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


public class DataFlowInfoForArgumentsImpl extends MutableDataFlowInfoForArguments {
    @Nullable
    private Map<ValueArgument, DataFlowInfo> infoMap = null;
    @Nullable private Map<ValueArgument, ValueArgument> nextArgument = null;
    @Nullable private DataFlowInfo resultInfo;

    public DataFlowInfoForArgumentsImpl(@NotNull DataFlowInfo initialInfo, @NotNull Call call) {
        super(initialInfo);
        initNextArgMap(call.getValueArguments());
    }

    private void initNextArgMap(@NotNull List<? extends ValueArgument> valueArguments) {
        Iterator<? extends ValueArgument> iterator = valueArguments.iterator();
        ValueArgument prev = null;
        while (iterator.hasNext()) {
            ValueArgument argument = iterator.next();
            if (prev != null) {
                if (nextArgument == null) {
                    nextArgument = new HashMap<>();
                }
                nextArgument.put(prev, argument);
            }
            prev = argument;
        }
    }

    @NotNull
    @Override
    public DataFlowInfo getInfo(@NotNull ValueArgument valueArgument) {
        DataFlowInfo infoForArgument = infoMap == null ? null : infoMap.get(valueArgument);
        if (infoForArgument == null) {
            return initialDataFlowInfo;
        }
        return initialDataFlowInfo.and(infoForArgument);
    }

    @Override
    public void updateInfo(@NotNull ValueArgument valueArgument, @NotNull DataFlowInfo dataFlowInfo) {
        ValueArgument next = nextArgument == null ? null : nextArgument.get(valueArgument);
        if (next != null) {
            if (infoMap == null) {
                infoMap = new HashMap<>();
            }
            infoMap.put(next, dataFlowInfo);
            return;
        }
        //TODO assert resultInfo == null
        resultInfo = dataFlowInfo;
    }

    @NotNull
    @Override
    public DataFlowInfo getResultInfo() {
        if (resultInfo == null) return initialDataFlowInfo;
        return initialDataFlowInfo.and(resultInfo);
    }

    @Override
    public void updateResultInfo(@NotNull DataFlowInfo dataFlowInfo) {
        if (dataFlowInfo.equals(DataFlowInfo.Companion.getEMPTY())) return;

        if (resultInfo == null) resultInfo = initialDataFlowInfo;
        resultInfo = resultInfo.and(dataFlowInfo);
    }
}
