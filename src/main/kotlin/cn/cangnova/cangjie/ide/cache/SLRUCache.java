///*
// * Copyright 2024 LinQingYing. and contributors.
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// *
// * The use of this source code is governed by the Apache License 2.0,
// * which allows users to freely use, modify, and distribute the code,
// * provided they adhere to the terms of the license.
// *
// * The software is provided "as-is", and the authors are not responsible for
// * any damages or issues arising from its use.
// *
// */
//
//package cn.cangnova.cangjie.ide.cache;
//
//import com.intellij.util.NotNullFunction;
//import com.intellij.util.containers.SLRUMap;
//import com.intellij.util.containers.hash.EqualityPolicy;
//import org.jetbrains.annotations.NotNull;
//import org.jetbrains.annotations.Nullable;
//
//import java.util.function.Function;
//
//public abstract class SLRUCache<K, V> extends SLRUMap<K, V> {
//    protected SLRUCache(int protectedQueueSize, int probationalQueueSize) {
//        super(protectedQueueSize, probationalQueueSize);
//    }
//
//    protected SLRUCache(int protectedQueueSize, int probationalQueueSize, @NotNull EqualityPolicy<? super K> hashingStrategy) {
//        super(protectedQueueSize, probationalQueueSize, hashingStrategy);
//    }
//
//    public abstract @NotNull V createValue(K key);
//
//    @Override
//    public @NotNull V get(K key) {
//        V value = getIfCached(key);
//        if (value != null) {
//            return value;
//        }
//
//        value = createValue(key);
//        put(key, value);
//
//        return value;
//    }
//
//    public @Nullable V getIfCached(K key) {
//        return super.get(key);
//    }
//
//    public static @NotNull <K, V>  SLRUCache<K, V> slruCache(int protectedQueueSize,
//                                                                                         int probationalQueueSize,
//                                                                                         @NotNull Function<@NotNull K, @NotNull V> valueProducer) {
//        return new SLRUCache<>(protectedQueueSize, probationalQueueSize) {
//            @Override
//            public @NotNull V createValue(K key) {
//                return valueProducer.apply(key);
//            }
//        };
//    }
//
//    /**
//     * @deprecated Use Caffeine.
//     */
//    @Deprecated
//    public static @NotNull <K, V>  SLRUCache<K, V> create(
//            int protectedQueueSize,
//            int probationalQueueSize,
//            @SuppressWarnings("UsagesOfObsoleteApi") @NotNull NotNullFunction<? super K, ? extends V> valueProducer
//    ) {
//        return slruCache(protectedQueueSize, probationalQueueSize, valueProducer::fun);
//    }
//}
