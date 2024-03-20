package com.huawei.cangjie.utils.slicedMap;


import org.jetbrains.annotations.NotNull;

public interface WritableSlice<K, V> extends ReadOnlySlice<K, V> {
    @NotNull
    @Override
    KeyWithSlice<K, V, WritableSlice<K, V>> getKey();

    // True to put, false to skip
    boolean check(K key, V value);

    void afterPut(MutableSlicedMap map, K key, V value);

    RewritePolicy getRewritePolicy();

    // In a sliced map one can request all keys for a collective slice
    boolean isCollective();
}
