/*
 * Copyright 2025 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.cangjie.resolve.caches

import com.intellij.util.NotNullFunction
import com.intellij.util.containers.SLRUMap
import com.intellij.util.containers.hash.EqualityPolicy
import org.jetbrains.annotations.NotNull
import java.util.function.Function


abstract class SLRUCache<K, V> protected constructor(protectedQueueSize: Int, probationalQueueSize: Int) :
    SLRUMap<K, V>(protectedQueueSize, probationalQueueSize) {

    protected constructor(
        protectedQueueSize: Int,
        probationalQueueSize: Int,
        @NotNull hashingStrategy: EqualityPolicy<in K>
    ) : this(protectedQueueSize, probationalQueueSize)

    abstract fun createValue(key: K): V

    override fun get(key: K): V {
        var value = getIfCached(key)
        if (value != null) {
            return value
        }

        value = createValue(key)
        value?.let { put(key, it) }

        return value
    }

    open fun getIfCached(key: K): V? = super.get(key)

    companion object {
        
        fun <K, V> slruCache(
            protectedQueueSize: Int,
            probationalQueueSize: Int,
            @NotNull valueProducer: Function<@NotNull K, @NotNull V>
        ): SLRUCache<K, V> {
            return object : SLRUCache<K, V>(protectedQueueSize, probationalQueueSize) {
                override fun createValue(key: K): V = valueProducer.apply(key)
            }
        }

        /**
         * @deprecated Use Caffeine.
         */
        @Deprecated("Use Caffeine.")
        
        fun <K, V> create(
            protectedQueueSize: Int,
            probationalQueueSize: Int,
            @SuppressWarnings("UsagesOfObsoleteApi") @NotNull valueProducer: NotNullFunction<in K, V>
        ): SLRUCache<K, V> {
            return slruCache(protectedQueueSize, probationalQueueSize) { key -> valueProducer.`fun`(key) }
        }
    }
}
