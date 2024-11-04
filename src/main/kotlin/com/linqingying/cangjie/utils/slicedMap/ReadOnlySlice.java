package com.linqingying.cangjie.utils.slicedMap;

import org.jetbrains.annotations.NotNull;

public interface ReadOnlySlice<K, V> {
    @NotNull
    KeyWithSlice<K, V, ? extends ReadOnlySlice<K, V>> getKey();

    V computeValue(SlicedMap map, K key, V value, boolean valueNotFound);

    /**
     * @return a slice that only retrieves the value from the storage and skips any computeValue() calls
     */
    ReadOnlySlice<K, V> makeRawValueVersion();
}
