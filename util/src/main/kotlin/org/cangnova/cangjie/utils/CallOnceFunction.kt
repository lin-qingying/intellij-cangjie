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

import java.util.concurrent.atomic.AtomicReference

/**
 * 只能调用一次的函数包装器
 *
 * 该类将一个函数包装为只能调用一次的版本。
 * 第一次调用时执行实际函数，后续调用返回默认值。
 *
 * 实现特点：
 * - 使用原子引用确保线程安全
 * - 第一次调用后自动清空函数引用，防止内存泄漏
 * - 后续调用返回预设的默认值
 *
 * 使用场景：
 * - 需要确保某个操作只执行一次
 * - 需要在第一次调用后释放函数引用
 * - 需要线程安全的一次性函数调用
 *
 * 示例：
 * ```kotlin
 * val initFunction = CallOnceFunction(Unit) { config ->
 *     println("Initializing with config: $config")
 *     // 执行初始化操作
 * }
 *
 * initFunction(myConfig)  // 输出：Initializing with config: ...
 * initFunction(myConfig)  // 不执行任何操作，返回 Unit
 * ```
 *
 * @param F 函数参数类型
 * @param T 函数返回值类型
 * @property defaultValue 后续调用返回的默认值
 * @param delegate 要包装的函数
 */
class CallOnceFunction<F, T>(private val defaultValue: T, delegate: Function1<F, T>) : Function1<F, T> {
    /**
     * 原子引用，存储函数实例
     *
     * 第一次调用后会被设置为 null，确保函数只执行一次。
     */
    private val functionRef: AtomicReference<Function1<F, T>?> = AtomicReference(delegate)

    /**
     * 调用函数
     *
     * 第一次调用时执行实际函数并返回结果，
     * 后续调用直接返回默认值。
     *
     * 该方法是线程安全的。
     *
     * @param p1 函数参数
     * @return 第一次调用返回实际结果，后续返回默认值
     */
    override fun invoke(p1: F): T = functionRef.getAndSet(null)?.invoke(p1) ?: defaultValue
}
