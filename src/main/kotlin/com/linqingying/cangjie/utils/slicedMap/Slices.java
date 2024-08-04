package com.linqingying.cangjie.utils.slicedMap;

import com.linqingying.cangjie.psi.CjElement;
import com.linqingying.cangjie.utils.PsiUtilsKt;
import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

public class Slices {

    public static final RewritePolicy ONLY_REWRITE_TO_EQUAL = new RewritePolicy() {

        @Override
        public <K> boolean rewriteProcessingNeeded(K key) {
            return true;
        }

        @Override
        public <K, V> boolean processRewrite(WritableSlice<K, V> slice, K key, V oldValue, V newValue) {
            if (!((oldValue == null && newValue == null) || (oldValue != null && oldValue.equals(newValue)))) {
                logErrorAboutRewritingNonEqualObjects(slice, key, oldValue, newValue);
            }
            return true;
        }
    };

    public static <K, V> SliceBuilder<K, V> sliceBuilder() {
        return new SliceBuilder<>(ONLY_REWRITE_TO_EQUAL);
    }
    private static final Logger LOG = Logger.getInstance(Slices.class);

    private static <K, V> void logErrorAboutRewritingNonEqualObjects(WritableSlice<K, V> slice, K key, V oldValue, V newValue) {
        // NOTE: Use BindingTraceContext.TRACK_REWRITES to debug this exception
        LOG.error("Rewrite at slice " + slice +
                " key: " + key +
                " old value: " + oldValue + '@' + System.identityHashCode(oldValue) +
                " new value: " + newValue + '@' + System.identityHashCode(newValue) +
                (key instanceof CjElement ? "\n" + PsiUtilsKt.getElementTextWithContext((CjElement) key) : ""));
    }

    public static <K, V> WritableSlice<K, V> createSimpleSlice() {
        return new BasicWritableSlice<>(ONLY_REWRITE_TO_EQUAL);
    }

    public static class SliceBuilder<K, V> {
        private List<ReadOnlySlice<K, V>> furtherLookupSlices;
        private final RewritePolicy rewritePolicy;
        private String debugName;

        private SliceBuilder(RewritePolicy rewritePolicy) {
            this.rewritePolicy = rewritePolicy;
        }

        @SafeVarargs
        public final SliceBuilder<K, V> setFurtherLookupSlices(ReadOnlySlice<K, V>... furtherLookupSlices) {
            this.furtherLookupSlices = Arrays.asList(furtherLookupSlices);
            return this;
        }

        public SliceBuilder<K, V> setDebugName(@NotNull String debugName) {
            this.debugName = debugName;
            return this;
        }

        public WritableSlice<K, V> build() {
            BasicWritableSlice<K, V> result = doBuild();
            if (debugName != null) {
                result.setDebugName(debugName);
            }
            return result;
        }

        private BasicWritableSlice<K, V> doBuild() {
            if (furtherLookupSlices != null) {
                return new BasicWritableSlice<K, V>(rewritePolicy) {
                    @Override
                    public V computeValue(SlicedMap map, K key, V value, boolean valueNotFound) {
                        if (valueNotFound) {
                            for (ReadOnlySlice<K, V> slice : furtherLookupSlices) {
                                V v = map.get(slice, key);
                                if (v != null) {
                                    return v;
                                }
                            }
                            return null;
                        }
                        return super.computeValue(map, key, value, false);
                    }
                };
            }
            return new BasicWritableSlice<>(rewritePolicy);
        }
    }
}
