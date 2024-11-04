package com.linqingying.cangjie.utils.slicedMap;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import com.intellij.openapi.util.Key;
import com.intellij.util.keyFMap.KeyFMap;
import kotlin.jvm.functions.Function3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public class SlicedMapImpl implements MutableSlicedMap {

    private final boolean alwaysAllowRewrite;
    String debugName = "";
    @Nullable
    private Map<Object, KeyFMap> map = null;
    private Multimap<WritableSlice<?, ?>, Object> collectiveSliceKeys = null;

    public SlicedMapImpl(boolean alwaysAllowRewrite) {
        this.alwaysAllowRewrite = alwaysAllowRewrite;
    }

    public SlicedMapImpl(boolean alwaysAllowRewrite, String name) {
        this(alwaysAllowRewrite);
        this.debugName = name;
    }

    @Override
    public String toString() {
        return debugName;
    }

    @Override
    public int getSize() {
        if (map != null) {
            return map.size();
        }
        return 0;
    }

    @Nullable
    @Override
    public <K, V> V get(@NotNull ReadOnlySlice<K, V> slice, K key) {
        KeyFMap holder = map != null ? map.get(key) : null;

        V value = holder == null ? null : holder.get(slice.getKey());

        return slice.computeValue(this, key, value, value == null);
    }

    @NotNull
    @Override
    public <K, V> Collection<K> getKeys(@NotNull WritableSlice<K, V> slice) {
        assert slice.isCollective() : "Keys are not collected for slice " + slice;

        if (collectiveSliceKeys == null) return Collections.emptyList();
        return (Collection<K>) collectiveSliceKeys.get(slice);
    }


    @Override
    public void forEach(@NotNull Function3<WritableSlice, Object, Object, Void> f) {
        if (map == null) return;
        map.forEach((key, holder) -> {
            if (holder == null) return;

            for (Key<?> sliceKey : holder.getKeys()) {
                Object value = holder.get(sliceKey);

                f.invoke(((AbstractWritableSlice) sliceKey).getSlice(), key, value);
            }
        });
    }


    @Override
    public <K, V> void put(@NotNull WritableSlice<K, V> slice, K key, V value) {
        if (!slice.check(key, value)) {
            return;
        }

        if (map == null) {
            map = new OpenAddressLinearProbingHashTable<>();
        }

        KeyFMap holder = map.get(key);
        if (holder == null) {
            holder = KeyFMap.EMPTY_MAP;
        }

        Key<V> sliceKey = slice.getKey();

        RewritePolicy rewritePolicy = slice.getRewritePolicy();
        if (!alwaysAllowRewrite && rewritePolicy.rewriteProcessingNeeded(key)) {
            V oldValue = holder.get(sliceKey);
            if (oldValue != null) {
                if (!rewritePolicy.processRewrite(slice, key, oldValue, value)) {
                    return;
                }
            }
        }

        if (slice.isCollective()) {
            if (collectiveSliceKeys == null) {
                collectiveSliceKeys = ArrayListMultimap.create();
            }

            collectiveSliceKeys.put(slice, key);
        }

        map.put(key, holder.plus(sliceKey, value));
        slice.afterPut(this, key, value);
    }

    @Override
    public void clear() {
        map = null;
        collectiveSliceKeys = null;
    }

    @NotNull
    @Override
    public <K, V> ImmutableMap<K, V> getSliceContents(@NotNull ReadOnlySlice<K, V> slice) {
        if (map == null) return ImmutableMap.of();

        ImmutableMap.Builder<K, V> builder = ImmutableMap.builder();

        map.forEach((key, holder) -> {
            V value = holder.get(slice.getKey());

            if (value != null) {
                builder.put((K) key, value);
            }
        });

        return builder.build();
    }

    /**
     * 删除某个ReadOnlySlice
     */
    public <K, V> void removeBySlice(@NotNull ReadOnlySlice<K, V> slice, K key ){
        if(map != null){
            KeyFMap holder = map.get(key);
            if (holder == null) {
                holder = KeyFMap.EMPTY_MAP;
            }


            System.out.println();
        }
    }


    @Override
    public <K> void remove(K key) {
        map.remove(key);
    }
}
