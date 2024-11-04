package com.linqingying.cangjie.utils.slicedMap;

import com.linqingying.cangjie.utils.Printer;
import kotlin.jvm.functions.Function3;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class TrackingSlicedMap extends SlicedMapImpl {
    private final Map<ReadOnlySlice<?, ?>, SliceWithStackTrace<?, ?>> sliceTranslationMap = new HashMap<>();
    private final boolean trackWithStackTraces;

    public TrackingSlicedMap(boolean trackWithStackTraces) {
        super(false);
        this.trackWithStackTraces = trackWithStackTraces;
    }

    private static class TrackableValue<V> {
        private final static StackTraceElement[] EMPTY_STACK_TRACE = new StackTraceElement[0];

        private final V value;
        private final StackTraceElement[] stackTrace;
        private final String threadName;

        private TrackableValue(V value, boolean storeStack) {
            this.value = value;
            this.stackTrace = storeStack ? Thread.currentThread().getStackTrace() : EMPTY_STACK_TRACE;
            this.threadName = Thread.currentThread().getName();
        }

        private Appendable printStackTrace(Appendable appendable) {
            Printer s = new Printer(appendable);
            s.println(value);
            s.println("Thread: " + threadName);
            s.println("Written at ");
            StackTraceElement[] trace = stackTrace;
            for (StackTraceElement aTrace : trace) {
                s.println("\tat " + aTrace);
            }
            s.println("---------");
            return appendable;
        }

        @Override
        public String toString() {
            return printStackTrace(new StringBuilder()).toString();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            TrackableValue other = (TrackableValue) o;

            return Objects.equals(value, other.value);
        }

        @Override
        public int hashCode() {
            return value != null ? value.hashCode() : 0;
        }
    }

    @Override
    public <K, V> V get(@NotNull ReadOnlySlice<K, V> slice, K key) {
        return super.get(wrapSlice(slice), key).value;
    }

    @Override
    public <K, V> @NotNull Collection<K> getKeys(@NotNull WritableSlice<K, V> slice) {
        return super.getKeys(wrapSlice(slice));
    }

    @Override
    public void forEach(@NotNull Function3<WritableSlice, Object, Object, Void> f) {
        super.forEach((slice, key, value) -> {
            f.invoke(((SliceWithStackTrace) slice).getWritableDelegate(), key, ((TrackableValue<?>) value).value);
            return null;
        });
    }





    public <K, V> void put(@NotNull WritableSlice<K, V> slice, K key, V value) {
        super.put(wrapSlice(slice), key, new TrackableValue<>(value, trackWithStackTraces));
    }

    @SuppressWarnings("unchecked")
    private <K, V> SliceWithStackTrace<K, V> wrapSlice(ReadOnlySlice<K, V> slice) {
        return (SliceWithStackTrace) sliceTranslationMap.computeIfAbsent(slice, k -> new SliceWithStackTrace<>(slice));
    }

    private class SliceWithStackTrace<K, V>
            extends AbstractWritableSlice<K, TrackableValue<V>>
            implements WritableSlice<K, TrackableValue<V>> {

        private final ReadOnlySlice<K, V> delegate;

        private SliceWithStackTrace(@NotNull ReadOnlySlice<K, V> delegate) {
            super(delegate.toString());
            this.delegate = delegate;
        }

        // Methods of ReadOnlySlice

        @Override
        public TrackableValue<V> computeValue(SlicedMap map, K key, TrackableValue<V> value, boolean valueNotFound) {
            return new TrackableValue<>(delegate.computeValue(map, key, value == null ? null : value.value, valueNotFound), trackWithStackTraces);
        }

        @Override
        public ReadOnlySlice<K, TrackableValue<V>> makeRawValueVersion() {
            return wrapSlice(delegate.makeRawValueVersion());
        }

        // Methods of WritableSlice

        private WritableSlice<K, V> getWritableDelegate() {
            return (WritableSlice<K, V>) delegate;
        }

        @Override
        public boolean isCollective() {
            return getWritableDelegate().isCollective();
        }

        @Override
        public RewritePolicy getRewritePolicy() {
            return getWritableDelegate().getRewritePolicy();
        }

        @Override
        public void afterPut(MutableSlicedMap map, K key, TrackableValue<V> value) {
            getWritableDelegate().afterPut(map, key, value.value);
        }

        @Override
        public boolean check(K key, TrackableValue<V> value) {
            return getWritableDelegate().check(key, value.value);
        }
    }
}
