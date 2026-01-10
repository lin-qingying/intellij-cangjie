/*
 * Copyright 2026 LinQingYing. and contributors.
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


/**
 * 单例恒等函数，返回输入值本身。
 * 用于性能优化，避免创建多个相同的函数实例。
 */
private val IDENTITY: (Any?) -> Any? = { it }

/**
 * 返回一个恒等函数，该函数返回输入值本身。
 * 适用于函数式编程中需要传递恒等转换的场景。
 *
 * @return 恒等函数，输入什么就返回什么
 *
 * 示例：
 * ```kotlin
 * val list = listOf(1, 2, 3)
 * list.map(identity<Int>()) // 返回 [1, 2, 3]
 * ```
 */
@Suppress("UNCHECKED_CAST") fun <T> identity(): (T) -> T = IDENTITY as (T) -> T


/**
 * 单例恒真函数，总是返回 true。
 * 用于性能优化，避免创建多个相同的函数实例。
 */
private val ALWAYS_TRUE: (Any?) -> Boolean = { true }

/**
 * 返回一个恒真函数，该函数对任何输入都返回 true。
 * 适用于过滤、条件判断等场景中需要通过所有元素的情况。
 *
 * @return 恒真函数，总是返回 true
 *
 * 示例：
 * ```kotlin
 * val list = listOf(1, 2, 3)
 * list.filter(alwaysTrue()) // 返回 [1, 2, 3]
 * ```
 */
fun <T> alwaysTrue(): (T) -> Boolean = ALWAYS_TRUE

/**
 * 单例恒空函数，总是返回 null。
 * 用于性能优化，避免创建多个相同的函数实例。
 */
private val ALWAYS_NULL: (Any?) -> Any? = { null }

/**
 * 返回一个恒空函数，该函数对任何输入都返回 null。
 * 适用于映射、转换等场景中需要过滤掉所有元素的情况。
 *
 * @return 恒空函数，总是返回 null
 *
 * 示例：
 * ```kotlin
 * val list = listOf(1, 2, 3)
 * list.mapNotNull(alwaysNull<Int, String>()) // 返回空列表
 * ```
 */
@Suppress("UNCHECKED_CAST")
fun <T, R: Any> alwaysNull(): (T) -> R? = ALWAYS_NULL as (T) -> R?

/**
 * 单参数空操作函数，不执行任何操作。
 * 用于需要传递一个函数但不需要执行任何操作的场景。
 */
val DO_NOTHING: (Any?) -> Unit = { }

/**
 * 双参数空操作函数，不执行任何操作。
 * 用于需要传递一个双参数函数但不需要执行任何操作的场景。
 */
val DO_NOTHING_2: (Any?, Any?) -> Unit = { _, _ -> }

/**
 * 三参数空操作函数，不执行任何操作。
 * 用于需要传递一个三参数函数但不需要执行任何操作的场景。
 */
val DO_NOTHING_3: (Any?, Any?, Any?) -> Unit = { _, _, _ -> }

/**
 * 返回一个不执行任何操作的函数。
 * 适用于回调、监听器等场景中需要提供空实现的情况。
 *
 * @return 空操作函数
 *
 * 示例：
 * ```kotlin
 * val list = listOf(1, 2, 3)
 * list.forEach(doNothing<Int>()) // 不执行任何操作
 * ```
 */
fun <T> doNothing(): (T) -> Unit = DO_NOTHING

/**
 * 不执行任何操作的函数。
 * 适用于需要提供空方法实现的场景。
 *
 * 示例：
 * ```kotlin
 * val callback: () -> Unit = ::doNothing
 * ```
 */
fun doNothing() {}
