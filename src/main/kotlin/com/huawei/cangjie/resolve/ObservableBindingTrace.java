package com.huawei.cangjie.resolve;

import com.huawei.cangjie.descriptors.BindingTrace;
import com.huawei.cangjie.diagnostics.Diagnostic;
import com.huawei.cangjie.psi.CjExpression;
import com.huawei.cangjie.types.CangJieType;
import com.huawei.cangjie.utils.slicedMap.ReadOnlySlice;
import com.huawei.cangjie.utils.slicedMap.WritableSlice;
import com.intellij.util.SmartFMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public class ObservableBindingTrace implements BindingTrace  {

    @Override
    public void recordType(@NotNull CjExpression expression, @Nullable CangJieType type) {
        originalTrace.recordType(expression, type);

    }

    public interface RecordHandler<K, V> {

        void handleRecord(WritableSlice<K, V> slice, K key, V value);
    }


    private final BindingTrace originalTrace;

    private   SmartFMap<WritableSlice, RecordHandler> handlers = SmartFMap.emptyMap();

    public ObservableBindingTrace(BindingTrace originalTrace) {
        this.originalTrace = originalTrace;
    }
    @NotNull
    @Override
    public BindingContext getBindingContext() {
        return originalTrace.getBindingContext();
    }

    public <K, V> ObservableBindingTrace addHandler(@NotNull WritableSlice<K, V> slice, @NotNull RecordHandler<K, V> handler) {
        handlers = handlers.plus(slice, handler);
        return this;
    }
    @Override
    @NotNull
    public <K, V> Collection<K> getKeys(WritableSlice<K, V> slice) {
        return originalTrace.getKeys(slice);
    }


    @Nullable
    @Override
    public CangJieType getType(@NotNull CjExpression expression) {
        return originalTrace.getType(expression);

    }

    @Override
    public <K, V> void record(@NotNull WritableSlice<K, V> slice, K key, V value) {

        originalTrace.record(slice, key, value);
        RecordHandler<K, V> recordHandler = (RecordHandler) handlers.get(slice);
        if (recordHandler != null) {
            recordHandler.handleRecord(slice, key, value);
        }
    }

    @Override
    public <K> void record(@NotNull WritableSlice<K, Boolean> slice, K key) {
        record(slice, key, true);
    }

    @Nullable
    @Override
    public <K, V> V get(@NotNull ReadOnlySlice<K, V> slice, K key) {
        return originalTrace.get(slice, key);

    }

    @Override
    public void report(@NotNull Diagnostic diagnostic) {
        originalTrace.report(diagnostic);

    }

    @Override
    public String toString() {
        return "ObservableTrace over " + originalTrace.toString();
    }
    @Override
    public boolean wantsDiagnostics() {
        return originalTrace.wantsDiagnostics();

    }
}
