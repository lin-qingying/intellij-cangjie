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

package org.cangnova.cangjie.debugger.protobuf.event

import org.jetbrains.annotations.NonNls
import java.io.Writer
import java.util.function.BiConsumer

/**
 * 空操作事件跟踪器
 *
 * 该类是EventTracer接口的空实现，不执行任何实际的事件跟踪操作。
 * 它是一个轻量级的占位符实现，适用于不需要事件跟踪功能的场景。
 *
 * 使用场景：
 * - 生产环境中禁用事件跟踪以提高性能
 * - 单元测试中的模拟对象
 * - 开发初期的占位符实现
 * - 不需要调试信息收集的应用场景
 *
 * 主要特点：
 * - 所有方法都是空实现，不执行任何操作
 * - 最小的内存和性能开销
 * - 线程安全的空操作
 * - 符合EventTracer接口契约
 */
class NoopEventTracer : EventTracer,
    BiConsumer<String?, Any?> {

    /**
     * 空的标记接受方法
     *
     * 不执行任何操作，直接返回。
     *
     * @param eventName 事件名称（被忽略）
     * @param details 事件详细信息（被忽略）
     */
    override fun accept(eventName: String?, details: Any?): Unit {
    }

    /**
     * 空的事件开始方法
     *
     * 不执行任何实际的跟踪操作，直接返回当前实例作为BiConsumer。
     *
     * @param eventCategory 事件类别（被忽略）
     * @param eventName 事件名称的懒加载函数（被忽略）
     * @param details 事件详细信息的懒加载函数（被忽略）
     * @param threadName 线程名称（被忽略）
     * @return 当前实例（不执行任何操作）
     */
    override fun begin(
        @NonNls eventCategory: String,
        @NonNls eventName: () -> String,
        @NonNls details: () -> String,
        @NonNls threadName: String
    ): BiConsumer<String?, Any?> {
        return this
    }

    /**
     * 空的事件结束方法
     *
     * 不执行任何操作，直接返回。
     *
     * @param threadName 线程名称（被忽略）
     */
    override fun end(@NonNls threadName: String) {
    }

    /**
     * 空的写入方法
     *
     * 不执行任何写入操作，直接返回。
     *
     * @param out 输出流（被忽略）
     */
    override fun write(out: Writer): Unit {
    }
}

