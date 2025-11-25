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

package org.cangnova.cangjie.protodebugger.data

import com.intellij.openapi.util.NlsSafe
import lldbprotobuf.Model


/**
 * 低级别线程（LLDBThread）类
 *
 * 该类表示调试器中的一个线程对象，包含了线程的基本信息如ID、状态、名称等。
 * 它是调试器管理和显示多线程程序状态的基础类。
 *
 * 使用场景：
 * - 在调试器中显示和管理线程列表
 * - 跟踪线程的执行状态
 * - 支持多线程调试功能
 * - 显示线程的详细信息和上下文
 *
 * @param id 线程的唯一标识符
 * @param state 线程的当前状态描述
 * @param workQueue 线程的工作队列信息
 * @param name 线程的可读名称
 * @param tid 线程的系统线程ID
 * @param frozen 线程是否被冻结（暂停执行）
 */
open class LLDBThread(
    val index: Int,
    val id: Long,
    private val state: String?,
    private val workQueue: String?,
    val name: String?,

    private val frozen: Boolean
) {
    /**
     * 简化构造函数，默认线程不被冻结
     *
     * @param id 线程ID
     * @param state 线程状态
     * @param workQueue 工作队列
     * @param name 线程名称
     * @param tid 系统线程ID
     */
    constructor(
        id: Long,
        index: Int,
        state: String?,
        workQueue: String?,
        name: String?,

        ) : this(index, id, state, workQueue, name, false)

    /**
     * 获取线程的显示名称
     *
     * 生成一个用户友好的线程显示名称，包含线程ID、名称、
     * 工作队列和系统线程ID等信息。格式为：
     * "Thread-{id}-<workQueue>-[name] (tid)"
     *
     * 使用场景：
     * - 在调试器UI中显示线程列表
     * - 生成线程的可读标识
     * - 区分不同的线程实例
     *
     * @return 格式化的线程显示名称
     */
    @NlsSafe
    open fun getDisplayName(): String {
        val builder = StringBuilder()
        builder.append("Thread-").append(id)
        workQueue?.let { builder.append("-<$it>") }
        name?.let { builder.append("-[$it]") }

        return builder.toString()
    }

    /**
     * 返回线程的字符串表示
     *
     * @return 线程的显示名称
     */
    override fun toString(): String {
        return getDisplayName()
    }

    /**
     * 检查线程是否被冻结
     *
     * 冻结状态表示线程的执行被暂停，通常在调试过程中用于
     * 检查线程状态或控制线程执行。
     *
     * @return 如果线程被冻结返回true，否则返回false
     */
    fun isFrozen(): Boolean {
        return this.frozen
    }

    /**
     * 比较两个线程对象是否相等
     *
     * 逐一比较线程的所有关键字段，包括ID、状态、工作队列、
     * 线程ID和冻结状态。只有所有字段都相等时才认为线程相等。
     *
     * @param other 要比较的对象
     * @return 如果所有字段都相等返回true，否则返回false
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LLDBThread) return false

        if (id != other.id) return false
        if (state != other.state) return false
        if (workQueue != other.workQueue) return false

        if (frozen != other.frozen) return false

        return true
    }

    /**
     * 计算线程对象的哈希码
     *
     * 基于线程的所有字段计算哈希码，确保equals方法返回true的对象
     * 具有相同的哈希码。线程ID是主要的哈希依据。
     *
     * @return 线程对象的哈希码
     */
    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + (state?.hashCode() ?: 0)
        result = 31 * result + (workQueue?.hashCode() ?: 0)

        result = 31 * result + frozen.hashCode()
        return result
    }
}


fun newLLThread(thread: Model.Thread): LLDBThread {
    val stopReasonInfo: Model.ThreadStopInfo = thread.stopInfo
    val isStopped = stopReasonInfo.reason !== Model.StopReason.STOP_REASON_INVALID
    return LLDBThread(

        thread.index,
        thread.threadId.id,
        if (isStopped) "STOPPED" else null,
        /*thread.queueName*/ null,
        thread.getName(),

        thread.isFrozen
    )
}