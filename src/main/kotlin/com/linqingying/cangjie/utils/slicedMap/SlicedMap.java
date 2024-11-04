package com.linqingying.cangjie.utils.slicedMap;

import kotlin.jvm.functions.Function3;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;

public interface SlicedMap {

    SlicedMap DO_NOTHING = new SlicedMap() {
        @Override
        public <K, V> V get(ReadOnlySlice<K, V> slice, K key) {
            return slice.computeValue(this, key, null, true);
        }

        @Override
        public <K, V> Collection<K> getKeys(WritableSlice<K, V> slice) {
            return Collections.emptySet();
        }

        @Override
        public void forEach(@NotNull Function3<WritableSlice, Object, Object, Void> f) {
        }
    };

    <K, V> V get(ReadOnlySlice<K, V> slice, K key);

    // slice.isCollective() must return true
    <K, V> Collection<K> getKeys(WritableSlice<K, V> slice);

    void forEach(@NotNull Function3<WritableSlice, Object, Object, Void> f);
}
