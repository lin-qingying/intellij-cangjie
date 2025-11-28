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

import com.google.gson.stream.JsonWriter
import org.jetbrains.annotations.NonNls

/**
 * 调试器事件基类
 *
 * 该密封类表示调试器中的各种事件，用于跟踪和分析调试过程中的操作。
 * 所有事件都包含基本的时间戳、线程名称和阶段信息。
 *
 * 使用场景：
 * - 调试器操作的事件跟踪
 * - 性能分析和调试
 * - 生成调试过程的时序图
 * - 监控调试器的内部状态变化
 *
 * 事件类型包括：
 * - BeginEvent: 事件开始
 * - EndEvent: 事件结束
 * - 其他自定义事件类型
 */
sealed class Event {
    /**
     * 事件阶段
     *
     * 表示事件在生命周期中的阶段，通常为：
     * - "B": Begin (开始)
     * - "E": End (结束)
     * - 其他自定义阶段标识
     */
    protected abstract val phase: String
        @NonNls get

    /**
     * 线程名称
     *
     * 事件发生的线程名称，用于多线程环境下的事件跟踪。
     * 会自动移除"ApplicationImpl "前缀以简化显示。
     */
    protected abstract val threadName: String
        @NonNls get

    /**
     * 事件时间戳
     *
     * 事件发生的精确时间，通常以微秒为单位。
     * 用于计算事件持续时间和时序分析。
     */
    protected abstract val time: Long

    /**
     * 将事件信息添加到JSON输出中
     *
     * 该方法将事件的基本信息以Chrome Tracing格式写入JSON输出。
     * 包含阶段(phase)、进程ID(pid)、线程ID(tid)和时间戳(ts)。
     *
     * 使用场景：
     * - 生成Chrome Tracing兼容的事件日志
     * - 调试器性能分析
     * - 事件序列的可视化
     *
     * @param json JSON写入器，用于输出事件信息
     */
    open fun appendTrace(json: JsonWriter) {
        with(json) {
            name("ph").value(phase)
            name("pid").value(0L)
            name("tid").value(threadName.removePrefix("ApplicationImpl "))
            name("ts").value(time)
        }
    }

}

