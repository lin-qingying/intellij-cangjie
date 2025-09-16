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

package org.cangnova.cangjie.utils

import java.util.Collections

/**
 * 针对小规模集合进行优化的 Set 实现，保持元素插入顺序。
 * 该集合非线程安全，且不支持诸如 [MutableSet.remove]、[MutableSet.removeAll] 和 [MutableSet.retainAll] 的删除操作。
 * 其迭代器也不支持 [MutableIterator.remove]。
 */
@Suppress("UNCHECKED_CAST")
class SmartSet<T> private constructor() : AbstractMutableSet<T>() {
    companion object {
        private const val ARRAY_THRESHOLD = 5

        /**
         * 创建一个空的 SmartSet 实例。
         */
        @JvmStatic
        fun <T> create() = SmartSet<T>()

        /**
         * 基于已有集合创建一个 SmartSet 实例，并将集合中的元素添加到新集合中。
         *
         * @param set 要复制的集合
         */
        @JvmStatic
        fun <T> create(set: Collection<T>) = SmartSet<T>().apply { this.addAll(set) }
    }

    // 当 size = 0 时为 null，size = 1 时为单个对象；当 size < 阈值时为对象数组；否则使用 LinkedHashSet
    private var data: Any? = null

    override var size: Int = 0

    override fun iterator(): MutableIterator<T> = when {
        size == 0 -> Collections.emptySet<T>().iterator()
        size == 1 -> SingletonIterator(data as T)
        size < ARRAY_THRESHOLD -> ArrayIterator(data as Array<T>)
        else -> (data as MutableSet<T>).iterator()
    }

    override fun add(element: T): Boolean {
        when {
            size == 0 -> {
                data = element
            }
            size == 1 -> {
                if (data == element) return false
                data = arrayOf(data, element)
            }
            size < ARRAY_THRESHOLD -> {
                val arr = data as Array<T>
                if (element in arr) return false
                data = if (size == ARRAY_THRESHOLD - 1) linkedSetOf(*arr).apply { add(element) }
                else arr.copyOf(size + 1).apply { set(size - 1, element) }
            }
            else -> {
                val set = data as MutableSet<T>
                if (!set.add(element)) return false
            }
        }

        size++
        return true
    }

    override fun clear() {
        data = null
        size = 0
    }

    override fun contains(element: T): Boolean = when {
        size == 0 -> false
        size == 1 -> data == element
        size < ARRAY_THRESHOLD -> element in data as Array<T>
        else -> element in data as Set<T>
    }

    private class SingletonIterator<out T>(private val element: T) : MutableIterator<T> {
        private var hasNext = true

        override fun next(): T =
            if (hasNext) {
                hasNext = false
                element
            } else throw NoSuchElementException()

        override fun hasNext() = hasNext

        override fun remove() = throw UnsupportedOperationException()
    }

    private class ArrayIterator<out T>(array: Array<T>) : MutableIterator<T> {
        private val arrayIterator = array.iterator()

        override fun hasNext(): Boolean = arrayIterator.hasNext()
        override fun next(): T = arrayIterator.next()
        override fun remove() = throw UnsupportedOperationException()
    }
}
