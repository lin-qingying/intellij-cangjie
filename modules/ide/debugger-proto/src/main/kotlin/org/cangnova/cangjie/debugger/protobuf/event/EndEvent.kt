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

/**
 * 调试器事件结束标记类
 *
 * 该类表示一个调试器事件的结束，用于标记某个操作或过程的结束时间点。
 * 与BeginEvent配对使用，可以精确测量操作的执行时间。
 *
 * 使用场景：
 * - 标记调试器操作的结束时间
 * - 与BeginEvent配对计算操作耗时
 * - 跟踪复杂操作的完成点
 * - 生成调试操作的完整生命周期日志
 *
 * @param threadName 事件发生的线程名称
 * @param time 事件发生的时间戳（微秒）
 */
data class EndEvent(override val threadName: String, override val time: Long) :
    Event() {
    /**
     * 事件阶段标识
     *
     * 固定为"E"，表示End（结束）阶段，符合Chrome Tracing格式规范。
     */
    override val phase: String = "E"


    /**
     * 返回事件的字符串表示
     *
     * 提供包含关键信息的可读字符串，便于日志记录和调试。
     *
     * @return 事件的字符串表示
     */
    override fun toString(): String {
        return "EndEvent(threadName=$threadName, time=$time)"
    }

    /**
     * 计算事件的哈希码
     *
     * 基于线程名称和时间戳计算哈希码，确保equals方法返回true的对象
     * 具有相同的哈希码。
     *
     * @return 事件的哈希码
     */
    override fun hashCode(): Int {
        var result = threadName.hashCode()
        result = 31 * result + time.hashCode()
        return result
    }

    /**
     * 比较两个EndEvent是否相等
     *
     * 比较线程名称和时间戳，只有两者都相等时才认为事件相等。
     *
     * @param other 要比较的对象
     * @return 如果线程名称和时间戳都相等返回true，否则返回false
     */
    override fun equals(other: Any?): Boolean {
        return if (this === other) {
            true
        } else if (other !is EndEvent) {
            false
        } else {
            threadName == other.threadName && time == other.time
        }
    }
}
