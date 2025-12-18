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


/**
 * 可锁定且可清除的延迟初始化值
 *
 * 该类提供线程安全的延迟初始化和值清除功能。
 * 值只会在第一次访问时初始化，并且可以随时清除以强制重新初始化。
 *
 * 实现特点：
 * - 使用双重检查锁定（Double-Checked Locking）模式
 * - 使用 @Volatile 确保可见性
 * - 支持清除已初始化的值
 *
 * 使用场景：
 * - 需要延迟初始化的昂贵资源
 * - 需要能够清除和重新初始化的缓存值
 * - 需要线程安全的懒加载
 *
 * 示例：
 * ```kotlin
 * val lock = Any()
 * val lazyValue = LockedClearableLazyValue(lock) {
 *     // 昂贵的初始化操作
 *     computeExpensiveValue()
 * }
 *
 * val value1 = lazyValue.get() // 执行初始化
 * val value2 = lazyValue.get() // 返回缓存值
 * lazyValue.drop()             // 清除缓存
 * val value3 = lazyValue.get() // 重新初始化
 * ```
 *
 * @param T 值的类型（必须是非空类型）
 * @property lock 用于同步的锁对象
 * @property init 初始化函数
 */
class LockedClearableLazyValue<out T: Any>(val lock: Any, val init: () -> T) {
    /**
     * 缓存的值（使用 volatile 确保多线程可见性）
     */
    @Volatile private var value: T? = null

    /**
     * 获取值
     *
     * 如果值尚未初始化，则执行初始化；
     * 如果值已初始化，则直接返回缓存的值。
     *
     * 该方法使用双重检查锁定模式确保线程安全和性能。
     *
     * @return 初始化后的值
     */
    fun get(): T {
        val _v1 = value
        if (_v1 != null) {
            return _v1
        }

        return synchronized(lock) {
            val _v2 = value

            @Suppress("UNNECESSARY_NOT_NULL_ASSERTION")
            if (_v2 != null) {
                _v2!!
            }
            else {
                val _v3 = init()
                this.value = _v3
                _v3
            }
        }
    }

    /**
     * 清除缓存的值
     *
     * 调用此方法后，下次调用 [get] 将重新执行初始化。
     * 该方法是线程安全的。
     */
    fun drop() {
        synchronized (lock) {
            value = null
        }
    }
}

