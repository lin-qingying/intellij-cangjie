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

package com.linqingying.cangjie.storage

import kotlin.reflect.KProperty

interface MemoizedFunctionToNotNull<in P, out R : Any> : Function1<P, R> {
    fun isComputed(key: P): Boolean
}
interface MemoizedFunctionToNullable<in P, out R : Any> : Function1<P, R?> {
    fun isComputed(key: P): Boolean
}
interface NullableLazyValue<out T : Any> : Function0<T?> {
    fun isComputed(): Boolean
    fun isComputing(): Boolean

}


operator fun <T : Any> NullableLazyValue<T>.getValue(_this: Any?, p: KProperty<*>): T? = invoke()
interface CacheWithNullableValues<in K, V : Any> {
    fun computeIfAbsent(key: K, computation: () -> V?): V?
}
interface CacheWithNotNullValues<in K, V : Any> {
    fun computeIfAbsent(key: K, computation: () -> V): V
}

interface NotNullLazyValue<out T : Any> : Function0<T> {
    fun isComputed(): Boolean
    fun isComputing(): Boolean

    // Only for debugging
    fun renderDebugInformation(): String = ""
}

operator fun <T : Any> NotNullLazyValue<T>.getValue(_this: Any?, p: KProperty<*>): T = invoke()
