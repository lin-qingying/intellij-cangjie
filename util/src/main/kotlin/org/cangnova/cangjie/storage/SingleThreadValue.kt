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
package org.cangnova.cangjie.storage


/**
 * 单线程值存储
 *
 * 该类用于存储只应在单个线程中存在和访问的值。
 *
 * 与 [ThreadLocal] 的区别：
 * - [ThreadLocal]：线程不存储对值的引用，使其在全局范围内不可访问，但简化了内存管理
 * - [SingleThreadValue]：无法为每个线程设置不同的值，因此需要使用外部锁保护实例免受重写
 *
 * 使用场景：
 * - 需要确保某个值只能在创建它的线程中访问
 * - 需要更简单的内存管理策略
 * - 不需要为每个线程存储不同的值
 *
 * 注意：该实例应该使用外部锁来保护，避免被重写。
 *
 * @param T 存储的值类型
 * @property value 存储的值
 */
internal class SingleThreadValue<T>(val value: T) {
    /**
     * 创建此存储时的线程
     */
    private val thread: Thread

    init {
        thread = Thread.currentThread()
    }

    /**
     * 检查当前线程是否是创建此存储的线程
     *
     * @return 如果当前线程是创建此存储的线程，返回 true；否则返回 false
     */
    fun hasValue(): Boolean {
        return thread === Thread.currentThread()
    }


}
