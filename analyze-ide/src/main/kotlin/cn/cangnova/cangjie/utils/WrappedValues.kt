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
package cn.cangnova.cangjie.utils

import kotlin.concurrent.Volatile

/**
 * 工具类，用于处理包装值，特别是处理可空值和异常的包装与解包装。
 * 
 * 该类提供了一种机制来安全地处理可空值和异常，允许它们在不支持直接处理可空值或异常的上下文中传递。
 * 主要用于在缓存、延迟计算等场景中，处理可空值和异常情况。
 */
object WrappedValues {
    /**
     * 表示空值的特殊对象，用于在不能直接使用null的上下文中表示null值。
     */
    private val NULL_VALUE: Any = object : Any() {
        override fun toString(): String {
            return "NULL_VALUE"
        }
    }

    /**
     * 控制是否在解包装时重新抛出ProcessCanceledException异常。
     * 当设置为true时，如果包装的异常是ProcessCanceledException，将重新抛出该异常。
     */
    @Volatile
    var throwWrappedProcessCanceledException: Boolean = false

    /**
     * 将可能是NULL_VALUE的值解包装为实际的可空值。
     *
     * @param value 可能是NULL_VALUE的对象
     * @return 如果输入是NULL_VALUE则返回null，否则返回原值
     */
    fun <V> unescapeNull(value: Any): V? {
        if (value === NULL_VALUE) return null
        return value as V
    }

    /**
     * 将可空值包装为非空对象。
     * 如果输入是null，则返回NULL_VALUE对象；否则返回原值。
     *
     * @param value 需要包装的可空值
     * @return 包装后的非空对象
     */
    @JvmStatic
    fun <V> escapeNull(value: V?): Any {
        if (value == null) return NULL_VALUE
        return value
    }

    /**
     * 将异常包装为ThrowableWrapper对象。
     * 这允许异常在不直接抛出的情况下被传递。
     *
     * @param throwable 需要包装的异常
     * @return 包装后的ThrowableWrapper对象
     */
    @JvmStatic
    fun escapeThrowable(throwable: Throwable): Any {
        return ThrowableWrapper(throwable)
    }

    /**
     * 尝试解包装可能包含异常的对象，如果是异常则抛出，否则解包装为可空值。
     *
     * @param value 可能包含异常的对象
     * @return 解包装后的值，如果原对象包含异常则抛出该异常
     */
    @JvmStatic
    fun <V> unescapeExceptionOrNull(value: Any): V? {
        return WrappedValues.unescapeNull<V?>(unescapeThrowable<Any?>(value)!!)
    }

    /**
     * 解包装可能包含异常的对象。
     * 如果对象是ThrowableWrapper，则抛出其中包含的异常；否则返回原值。
     *
     * @param value 可能包含异常的对象
     * @return 如果不是异常包装，则返回原值
     * @throws Throwable 如果对象是ThrowableWrapper，则抛出其中包含的异常
     */
    @JvmStatic
    fun <V> unescapeThrowable(value: Any?): V? {
        if (value is ThrowableWrapper) {
            val originThrowable =
                value.throwable

            if (throwWrappedProcessCanceledException && originThrowable.isProcessCanceledException()) {
                throw WrappedProcessCanceledException(originThrowable)
            }

            throw rethrow(originThrowable)
        }

        return value as V?
    }

    /**
     * 异常包装器，用于在不直接抛出异常的情况下存储和传递异常。
     *
     * @property throwable 被包装的异常
     */
    private class ThrowableWrapper(val throwable: Throwable) {
        override fun toString(): String {
            return throwable.toString()
        }
    }

    /**
     * 表示被重新抛出的ProcessCanceledException异常的包装异常。
     * 
     * @param cause 原始的ProcessCanceledException异常
     */
    class WrappedProcessCanceledException(cause: Throwable?) : RuntimeException("Rethrow stored exception", cause)
}
