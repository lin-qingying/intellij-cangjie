/*
 * Copyright 2024 LinQingYing. and contributors.
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

package cn.cangnova.cangjie.utils.slicedMap

import cn.cangnova.cangjie.utils.exceptions.Printer

/**
 * 带追踪功能的切片映射实现
 *
 * 此类扩展了SlicedMapImpl，增加了对切片操作的追踪功能，
 * 可选择是否记录堆栈跟踪信息，有助于调试和追踪切片映射的使用情况。
 *
 * @property trackWithStackTraces 是否记录堆栈跟踪信息
 */
class TrackingSlicedMap(private val trackWithStackTraces: Boolean) : SlicedMapImpl(false) {

    private val sliceTranslationMap = HashMap<ReadOnlySlice<*, *>, SliceWithStackTrace<*, *>>()

    /**
     * 可追踪的值包装类
     *
     * @param V 原始值类型
     * @property value 原始值
     * @property stackTrace 堆栈跟踪信息
     * @property threadName 线程名称
     */
    private class TrackableValue<V>(
        val value: V,
        storeStack: Boolean
    ) {
        private val stackTrace: Array<StackTraceElement>
        private val threadName: String = Thread.currentThread().name

        init {
            stackTrace = if (storeStack)
                Thread.currentThread().stackTrace
            else
                EMPTY_STACK_TRACE
        }

        /**
         * 打印堆栈跟踪信息
         *
         * @param appendable 输出目标
         * @return 输出目标本身
         */
        fun printStackTrace(appendable: Appendable): Appendable {
            val s = Printer(appendable)
            s.println(value!!)
            s.println("Thread: $threadName")
            s.println("Written at ")
            for (element in stackTrace) {
                s.println("\tat $element")
            }
            s.println("---------")
            return appendable
        }

        override fun toString(): String {
            return printStackTrace(StringBuilder()).toString()
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || javaClass != other.javaClass) return false

            val otherValue = other as TrackableValue<*>

            return value == otherValue.value
        }

        override fun hashCode(): Int {
            return value?.hashCode() ?: 0
        }

        companion object {
            private val EMPTY_STACK_TRACE = emptyArray<StackTraceElement>()
        }
    }

    /**
     * 获取指定切片和键的值
     *
     * @param slice 切片
     * @param key 键
     * @return 值
     */
    override fun <K, V> get(slice: ReadOnlySlice<K, V>, key: K): V? {
        return super.get(wrapSlice(slice), key)?.value
    }

    /**
     * 获取指定可写切片的所有键
     *
     * @param slice 可写切片
     * @return 键的集合
     */
    override fun <K, V> getKeys(slice: WritableSlice<K, V>): Collection<K> {
        return super.getKeys(wrapSlice(slice))
    }

    /**
     * 对映射中的每个条目执行给定函数
     *
     * @param f 要执行的函数
     */


    override fun forEach(f: (WritableSlice<*, *>, Any, Any) -> Unit) {
        super.forEach { slice, key, value ->
            f.invoke(slice, key, value)
        }
    }

    /**
     * 将指定的键值对放入对应的切片中
     *
     * @param slice 可写切片
     * @param key 键
     * @param value 值
     */
    override fun <K, V> put(slice: WritableSlice<K, V>, key: K, value: V) {
        super.put(wrapSlice(slice), key, TrackableValue(value, trackWithStackTraces))
    }

    /**
     * 包装切片以支持追踪功能
     *
     * @param slice 原始切片
     * @return 包装后的切片
     */
    @Suppress("UNCHECKED_CAST")
    private fun <K, V> wrapSlice(slice: ReadOnlySlice<K, V>): SliceWithStackTrace<K, V> {
        return sliceTranslationMap.computeIfAbsent(slice) {
            SliceWithStackTrace(slice)
        } as SliceWithStackTrace<K, V>
    }

    /**
     * 支持堆栈跟踪的切片包装类
     *
     * @param K 键类型
     * @param V 值类型
     * @property delegate 原始切片
     */
    private inner class SliceWithStackTrace<K, V>(
        private val delegate: ReadOnlySlice<K, V>
    ) : AbstractWritableSlice<K, TrackableValue<V>>(delegate.toString()), WritableSlice<K, TrackableValue<V>> {

        /**
         * 计算给定键和值的实际值
         */
        override fun computeValue(
            map: SlicedMap?,
            key: K,
            value: TrackableValue<V>?,
            valueNotFound: Boolean
        ): TrackableValue<V>? {
            return TrackableValue(
                delegate.computeValue(map, key, value?.value, valueNotFound) ?: return null,
                trackWithStackTraces
            )
        }

        /**
         * 创建此切片的原始值版本
         */
        override fun makeRawValueVersion(): ReadOnlySlice<K, TrackableValue<V>>? {
            return delegate.makeRawValueVersion()?.let {
                wrapSlice(it)
            }
        }

        /**
         * 获取可写的原始委托
         */
        @Suppress("UNCHECKED_CAST")
        fun getWritableDelegate(): WritableSlice<K, V> {
            return delegate as WritableSlice<K, V>
        }

        /**
         * 判断是否为集合类型切片
         */
        override val isCollective: Boolean
            get() = getWritableDelegate().isCollective

        /**
         * 获取重写策略
         */
        override val rewritePolicy: RewritePolicy
            get() = getWritableDelegate().rewritePolicy

        /**
         * 在放置键值对后执行的操作
         */
        override fun afterPut(map: MutableSlicedMap, key: K, value: TrackableValue<V>) {
            getWritableDelegate().afterPut(map, key, value.value)
        }

        /**
         * 检查键值对是否有效
         */
        override fun check(key: K, value: TrackableValue<V>): Boolean {
            return getWritableDelegate().check(key, value.value)
        }
    }
}

