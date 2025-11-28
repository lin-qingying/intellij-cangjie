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
import com.intellij.openapi.util.registry.Registry
import java.util.function.BiConsumer

/**
 * 调试器事件开始标记类
 *
 * 该类表示一个调试器事件的开始，用于标记某个操作或过程的起始时间点。
 * 与EndEvent配对使用，可以精确测量操作的执行时间。
 *
 * 使用场景：
 * - 标记调试器操作的开始时间
 * - 跟踪复杂操作的开始点
 * - 性能测量和分析
 * - 生成调试操作的详细日志
 *
 * @param threadName 事件发生的线程名称
 * @param time 事件发生的时间戳（微秒）
 * @param eventCategory 事件的分类，用于过滤和分组
 * @param eventName 事件的名称，描述具体的操作
 * @param details 事件的详细信息，可以是任意对象
 */
data class BeginEvent(
    override val threadName: String,
    override val time: Long,
    val eventCategory: String,
    val eventName: String,
    val details: Any?
) : Event(), BiConsumer<String?, Any?> {

    /**
     * 事件阶段标识
     *
     * 固定为"B"，表示Begin（开始）阶段，符合Chrome Tracing格式规范。
     */
    override val phase: String = "B"

    /**
     * 接受事件名称和详细信息
     *
     * 实现BiConsumer接口，允许动态更新事件名称和详细信息。
     * 注意：此方法当前实现为空，可以根据需要添加具体的更新逻辑。
     *
     * @param eventName 新的事件名称
     * @param details 新的事件详细信息
     */
    override fun accept(eventName: String?, details: Any?) {
        // 原有的递归调用被移除，需要根据实际需求实现具体逻辑
        // 例如：this.eventName = eventName ?: this.eventName
        //       this.details = details ?: this.details
    }

    /**
     * 将事件信息添加到JSON输出中
     *
     * 扩展基类的appendTrace方法，添加BeginEvent特有的信息。
     * 支持通过注册表过滤器来控制事件的输出，并包含详细的事件信息。
     *
     * 使用场景：
     * - 生成Chrome Tracing兼容的详细事件日志
     * - 根据事件分类进行过滤
     * - 包含事件的详细参数和错误处理
     *
     * @param json JSON写入器，用于输出事件信息
     */
    override fun appendTrace(json: JsonWriter) {
        json.apply {
            val filter = Registry.stringValue("cidr.tracing.filter")
            if (filter.isNotEmpty() && !filter.split(',').contains(eventCategory)) {
                return
            }

            name("name").value(eventName)
            name("cat").value(eventCategory)
            super.appendTrace(this)

            details?.let {
                name("args").beginObject()
                try {
                    val detailsValue = Result.runCatching { it.toString() }.getOrElse { it.toString() }
                    name("details").value(detailsValue)
                } catch (e: Throwable) {
                    name("traceError").value(e.toString())
                }
                endObject()
            }
        }
    }

    /**
     * 返回事件的字符串表示
     *
     * 提供包含所有关键信息的可读字符串，便于日志记录和调试。
     *
     * @return 事件的详细字符串表示
     */
    override fun toString(): String {
        return "BeginEvent(threadName=$threadName, time=$time, eventCategory=$eventCategory, eventName=$eventName, details=$details)"
    }

    /**
     * 计算事件的哈希码
     *
     * 基于事件的所有关键字段计算哈希码，确保equals方法返回true的对象
     * 具有相同的哈希码。
     *
     * @return 事件的哈希码
     */
    override fun hashCode(): Int {
        var result = threadName.hashCode()
        result = 31 * result + time.hashCode()
        result = 31 * result + eventCategory.hashCode()
        result = 31 * result + eventName.hashCode()
        result = 31 * result + (details?.hashCode() ?: 0)
        return result
    }

    /**
     * 比较两个BeginEvent是否相等
     *
     * 逐一比较事件的所有关键字段，只有所有字段都相等时才认为事件相等。
     *
     * @param other 要比较的对象
     * @return 如果所有字段都相等返回true，否则返回false
     */
    override fun equals(other: Any?): Boolean {
        return if (this === other) {
            true
        } else if (other !is BeginEvent) {
            false
        } else {
            val otherEvent = other as BeginEvent
            (
                    threadName == otherEvent.threadName &&
                            time == otherEvent.time &&
                            eventCategory == otherEvent.eventCategory &&
                            eventName == otherEvent.eventName &&
                            details == otherEvent.details
                    )
        }
    }
}

