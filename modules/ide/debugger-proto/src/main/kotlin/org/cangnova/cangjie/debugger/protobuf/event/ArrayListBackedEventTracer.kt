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
import java.io.Writer
import java.util.function.BiConsumer

/**
 * 基于ArrayList的事件跟踪器
 *
 * 该类实现了EventTracer接口，使用内存中的ArrayList来存储事件信息。
 * 它提供了完整的事件跟踪功能，包括事件记录、时间戳管理和JSON格式输出。
 *
 * 使用场景：
 * - 开发和调试阶段的事件跟踪
 * - 性能分析和优化
 * - 调试器行为分析
 * - 问题诊断和故障排查
 *
 * 主要功能：
 * - 记录事件的开始和结束时间
 * - 维护线程安全的事件列表
 * - 支持JSON格式的事件输出
 * - 提供微秒级的时间戳精度
 * - 自动清理已输出的事件
 */
class ArrayListBackedEventTracer : EventTracer {
    /**
     * 事件列表
     *
     * 使用可变列表存储所有事件，支持动态添加和清空操作。
     * 访问时需要同步以确保线程安全。
     */
    val events = mutableListOf<Event>()

    /**
     * 开始事件跟踪
     *
     * 创建一个开始事件并添加到事件列表中，返回BiConsumer用于后续标记。
     * 事件包含线程名、时间戳、类别、名称和详细信息。
     *
     * 使用场景：
     * - 开始跟踪一个调试操作
     * - 记录操作的启动时间
     * - 建立事件上下文信息
     *
     * @param eventCategory 事件类别，如"debugger.command"、"evaluation"等
     * @param eventName 事件名称的懒加载函数，避免不必要的字符串构建
     * @param details 事件详细信息的懒加载函数
     * @param threadName 线程名称
     * @return BeginEvent对象，用于接受事件标记
     */
    override fun begin(
        @NonNls eventCategory: String,
        @NonNls eventName: () -> String,
        @NonNls details: () -> String,
        @NonNls threadName: String
    ): BiConsumer<String?, Any?> {
        val beginEvent = BeginEvent(threadName, currentTimeMks(), eventCategory, eventName.invoke(), details.invoke())

        return synchronized(events) {
            events.add(beginEvent)
            beginEvent
        }
    }

    /**
     * 获取当前时间（微秒）
     *
     * 将系统纳秒时间转换为微秒时间，提供足够的时间精度用于性能分析。
     *
     * @return 当前时间的微秒数
     */
    private fun currentTimeMks(): Long {
        return System.nanoTime() / 1000L
    }

    /**
     * 结束事件跟踪
     *
     * 创建一个结束事件并添加到事件列表中，标记某个跟踪操作的完成。
     *
     * 使用场景：
     * - 结束跟踪一个调试操作
     * - 记录操作的完成时间
     * - 计算操作执行时长
     *
     * @param threadName 线程名称
     */
    override fun end(@NonNls threadName: String) {
        val endEvent = EndEvent(threadName, currentTimeMks())

        synchronized(events) {
            events.add(endEvent)
        }
    }

    /**
     * 写入事件数据到输出流
     *
     * 将所有收集的事件以JSON格式写入到指定的输出流中，
     * 并在写入完成后清空事件列表以释放内存。
     *
     * 输出格式为JSON数组，每个事件都是一个JSON对象，包含事件的完整信息。
     *
     * 使用场景：
     * - 生成调试器性能报告
     * - 保存事件数据用于后续分析
     * - 导出调试日志
     * - 与外部分析工具集成
     *
     * @param out 输出流，用于写入JSON格式的事件数据
     */
    override fun write(out: Writer) {
        val json = JsonWriter(out)

        synchronized(events) {
            json.beginArray()
            events.forEach { e ->
                json.beginObject()
                e.appendTrace(json)
                json.endObject()
            }
            json.endArray()
            events.clear()
        }
    }
}

