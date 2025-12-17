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

package org.cangnova.cangjie.resolve.binding.slicedMap

import org.cangnova.cangjie.utils.Printer

/**
 * 带有追踪功能的 SlicedMap 实现，可以记录值的写入堆栈跟踪
 *
 * @property trackWithStackTraces 是否记录堆栈跟踪
 */
class TrackingSlicedMap(
    private val trackWithStackTraces: Boolean
) : SlicedMapImpl(alwaysAllowRewrite = false) {

    private val sliceTranslationMap = mutableMapOf<ReadOnlySlice<*, *>, SliceWithStackTrace<*, *>>()

    /**
     * 可追踪的值包装器，记录值及其写入时的堆栈信息
     */
    private data class TrackableValue<V>(
        val value: V,
        val stackTrace: Array<StackTraceElement>,
        val threadName: String
    ) {
        constructor(value: V, storeStack: Boolean) : this(
            value = value,
            stackTrace = if (storeStack) Thread.currentThread().stackTrace else EMPTY_STACK_TRACE,
            threadName = Thread.currentThread().name
        )

        fun printStackTrace(appendable: Appendable): Appendable {
            val printer = Printer(appendable)
            printer.println(value ?: "")
            printer.println("Thread: $threadName")
            printer.println("Written at ")
            stackTrace.forEach { printer.println("\tat $it") }
            printer.println("---------")
            return appendable
        }

        override fun toString(): String = printStackTrace(StringBuilder()).toString()

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is TrackableValue<*>) return false
            return value == other.value
        }

        override fun hashCode(): Int = value.hashCode()

        companion object {
            private val EMPTY_STACK_TRACE = emptyArray<StackTraceElement>()
        }
    }

    override fun <K : Any, V : Any> get(slice: ReadOnlySlice<K, V>, key: K): V? {
        return super.get(wrapSlice(slice), key)?.value
    }

    override fun <K : Any, V : Any> getKeys(slice: WritableSlice<K, V>): Collection<K> {
        return super.getKeys(wrapSlice(slice))
    }

    override fun <K : Any, V : Any> forEach(f: (WritableSlice<K, V>, K, V) -> Unit) {
        super.forEach(
            { slice: WritableSlice<K, V>, key, value ->
                @Suppress("UNCHECKED_CAST")
                f(
                    (slice as SliceWithStackTrace<*, *>).writableDelegate as WritableSlice<K, V>,
                    key,
                    (value as TrackableValue<*>).value as V
                )
            }
        )

    }

    override fun <K : Any, V : Any> put(slice: WritableSlice<K, V>, key: K, value: V) {
        super.put(wrapSlice(slice), key, TrackableValue(value, trackWithStackTraces))
    }

    @Suppress("UNCHECKED_CAST")
    private fun <K : Any, V : Any> wrapSlice(slice: ReadOnlySlice<K, V>): SliceWithStackTrace<K, V> {
        return sliceTranslationMap.getOrPut(slice) {
            SliceWithStackTrace(slice)
        } as SliceWithStackTrace<K, V>
    }

    /**
     * 带有堆栈跟踪的 Slice 包装器
     */
    private inner class SliceWithStackTrace<K : Any, V : Any>(
        private val delegate: ReadOnlySlice<K, V>
    ) : AbstractWritableSlice<K, TrackableValue<V>>(delegate.toString()),
        WritableSlice<K, TrackableValue<V>> {

        val writableDelegate: WritableSlice<K, V>
            @Suppress("UNCHECKED_CAST")
            get() = delegate as WritableSlice<K, V>

        // ReadOnlySlice 方法

        override fun computeValue(
            map: SlicedMap,
            key: K,
            value: TrackableValue<V>?,
            valueNotFound: Boolean
        ): TrackableValue<V> {
            // 注意：delegate.computeValue 返回 V?，但我们需要 TrackableValue<V>
            // 在运行时，V? 和 V 是兼容的（Java 泛型擦除的遗留行为）
            // 这个转换在运行时是安全的
            @Suppress("UNCHECKED_CAST")
            return TrackableValue(
                delegate.computeValue(map, key, value?.value, valueNotFound),
                trackWithStackTraces
            ) as TrackableValue<V>
        }

        override fun makeRawValueVersion(): ReadOnlySlice<K, TrackableValue<V>> {
            return wrapSlice(delegate.makeRawValueVersion())
        }

        // WritableSlice 方法

        override fun isCollective(): Boolean = writableDelegate.isCollective()

        override fun getRewritePolicy(): RewritePolicy = writableDelegate.getRewritePolicy()

        override fun afterPut(map: MutableSlicedMap, key: K, value: TrackableValue<V>) {
            // TrackableValue 包装的值在 put 时总是非空的
            @Suppress("UNCHECKED_CAST")
            writableDelegate.afterPut(map, key, value.value)
        }

        override fun check(key: K, value: TrackableValue<V>): Boolean {
            // TrackableValue 包装的值在 check 时总是非空的
            @Suppress("UNCHECKED_CAST")
            return writableDelegate.check(key, value.value)
        }
    }
}
