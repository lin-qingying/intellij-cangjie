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

package cn.cangnova.cangjie.storage

import kotlin.reflect.KProperty

/**
 * 表示一个具有记忆化功能的函数接口，计算结果从不为 null。
 * 该接口扩展了 Function1，支持将单个参数转换为结果，并确保结果不是 null。
 * 记忆化是一种通过存储昂贵函数调用的结果来加速程序的技术。
 *
 * @param P 输入参数类型，可以是任何类型。
 * @param R 输出结果类型，必须是 Any 的子类型且不能为 null。
 */
interface MemoizedFunctionToNotNull<in P, out R : Any> : MemoizedFunction<P, R>, (P) -> R {

    /**
     * 检查给定键的计算结果是否已经存在。
     *
     * @param key 要检查的键。
     * @return 如果键的计算结果已存在则返回 true，否则返回 false。
     */
    override fun isComputed(key: P): Boolean
}

/**
 * 表示一个可返回空值的缓存函数接口。
 * 缓存函数是指能够记住之前调用结果的函数，当再次传递相同的参数时，返回缓存的结果。
 *
 * @param P 函数参数的类型。
 * @param R 函数返回值的非空类型。
 */
interface MemoizedFunctionToNullable<in P, out R : Any> : MemoizedFunction<P, R>, (P) -> R? {
    /**
     * 检查给定键的结果是否已计算。
     *
     * @param key 要检查的输入参数。
     * @return 如果结果已计算则返回 `true`，否则返回 `false`。
     */
    override fun isComputed(key: P): Boolean
}

interface MemoizedFunction<in P, out R : Any>{
    /**
     * 检查给定键的结果是否已计算。
     *
     * @param key 要检查的输入参数。
     * @return 如果结果已计算则返回 `true`，否则返回 `false`。
     */
    fun isComputed(key: P): Boolean
}
interface LazyValue<out T : Any> {
    /**
     * 检查值是否已计算。
     *
     * @return 如果值已计算则返回 `true`，否则返回 `false`。
     */
    fun isComputed(): Boolean

    /**
     * 检查值是否正在计算中。
     *
     * @return 如果值正在计算中则返回 `true`，否则返回 `false`。
     */
    fun isComputing(): Boolean

    fun invoke(): T?
}

/**
 * 表示一个可能返回空值的惰性值接口。
 * 惰性值是指在首次访问时才计算其值的对象。
 *
 * @param T 惰性值的非空类型。
 */
interface NullableLazyValue<out T : Any> : LazyValue<T>, () -> T? {
    /**
     * 检查值是否已计算。
     *
     * @return 如果值已计算则返回 `true`，否则返回 `false`。
     */
    override fun isComputed(): Boolean

    /**
     * 检查值是否正在计算中。
     *
     * @return 如果值正在计算中则返回 `true`，否则返回 `false`。
     */
    override fun isComputing(): Boolean
}

/**
 * 操作符函数，用于获取惰性值。
 * 当惰性值被访问时，调用 `invoke` 方法来计算并返回值。
 *
 * @param _this 调用者对象，通常为 `null`。
 * @param p 属性元数据，通常为 `KProperty` 类型。
 * @return 计算后的值，可能为 `null`。
 */
operator fun <T : Any> NullableLazyValue<T>.getValue(_this: Any?, p: KProperty<*>): T? = invoke()

/**
 * 表示一个可能返回空值的缓存接口。
 * 缓存接口用于存储和检索计算结果，如果结果不存在则进行计算并存储。
 *
 * @param K 缓存键的类型。
 * @param V 缓存值的非空类型。
 */
interface CacheWithNullableValues<in K, V : Any> {
    /**
     * 如果给定键的结果不存在，则计算并返回结果。
     *
     * @param key 缓存键。
     * @param computation 计算结果的函数。
     * @return 计算后的值，可能为 `null`。
     */
    fun computeIfAbsent(key: K, computation: () -> V?): V?
}

/**
 * 表示一个必须返回非空值的缓存接口。
 * 缓存接口用于存储和检索计算结果，如果结果不存在则进行计算并存储。
 *
 * @param K 缓存键的类型。
 * @param V 缓存值的非空类型。
 */
interface CacheWithNotNullValues<in K, V : Any> {
    /**
     * 如果给定键的结果不存在，则计算并返回结果。
     *
     * @param key 缓存键。
     * @param computation 计算结果的函数。
     * @return 计算后的值，必须为非空。
     */
    fun computeIfAbsent(key: K, computation: () -> V): V
}


/**
 * 表示一个不可为空的惰性初始化值。
 * 该接口扩展了 Function0，以提供获取值的方法 [get]。
 * 它确保值仅在首次请求时计算，并提供了检查值是否已计算或正在计算的方法。
 */
interface NotNullLazyValue<out T : Any> : LazyValue<T>, () -> T {

    /**
     * 检查值是否已被计算。
     * @return 如果值已被计算则返回 `true`，否则返回 `false`。
     */
    override fun isComputed(): Boolean

    /**
     * 检查值是否正在计算中。
     * @return 如果值正在计算中则返回 `true`，否则返回 `false`。
     */
    override fun isComputing(): Boolean

    /**
     * 仅用于调试目的，返回调试信息字符串。
     * @return 调试信息字符串。
     */
    fun renderDebugInformation(): String = ""
}


operator fun <T : Any> NotNullLazyValue<T>.getValue(_this: Any?, p: KProperty<*>): T = invoke()
