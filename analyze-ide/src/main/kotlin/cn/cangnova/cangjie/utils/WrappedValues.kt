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


object WrappedValues {
    private val NULL_VALUE: Any = object : Any() {
        override fun toString(): String {
            return "NULL_VALUE"
        }
    }

    @Volatile
    var throwWrappedProcessCanceledException: Boolean = false

    fun <V> unescapeNull(value: Any): V? {
        if (value === NULL_VALUE) return null
        return value as V
    }

    @JvmStatic


    fun <V> escapeNull(value: V?): Any {
        if (value == null) return NULL_VALUE
        return value
    }

    @JvmStatic
    fun escapeThrowable(throwable: Throwable): Any {
        return ThrowableWrapper(throwable)
    }

    @JvmStatic
    fun <V> unescapeExceptionOrNull(value: Any): V? {
        return WrappedValues.unescapeNull<V?>(unescapeThrowable<Any?>(value)!!)
    }

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

    private class ThrowableWrapper(val throwable: Throwable) {
        override fun toString(): String {
            return throwable.toString()
        }
    }

    class WrappedProcessCanceledException(cause: Throwable?) : RuntimeException("Rethrow stored exception", cause)
}
