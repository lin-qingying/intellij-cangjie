package com.huawei.cangjie.storage;


import com.huawei.cangjie.descriptors.BindingTrace;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;

public interface LazyResolveStorageManager extends StorageManager {
    @NotNull
    <K, V> MemoizedFunctionToNotNull<K, V> createSoftlyRetainedMemoizedFunction(@NotNull Function1<K, V> compute);

    @NotNull
    <K, V> MemoizedFunctionToNullable<K, V> createSoftlyRetainedMemoizedFunctionWithNullableValues(@NotNull Function1<K, V> compute);

    @NotNull
    BindingTrace createSafeTrace(@NotNull BindingTrace originalTrace);
}
