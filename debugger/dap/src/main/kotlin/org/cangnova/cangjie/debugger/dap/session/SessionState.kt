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

package org.cangnova.cangjie.debugger.dap.session

/**
 * 会话状态
 *
 * 定义调试会话的所有可能状态
 */
sealed class SessionState {

    /**
     * 空闲状态 - 会话未启动
     */
    object Idle : SessionState()

    /**
     * 初始化中 - 正在启动服务器和建立连接
     */
    object Initializing : SessionState()

    /**
     * 配置中 - 正在发送配置请求
     */
    object Configuring : SessionState()

    /**
     * 启动中 - 正在启动调试目标
     */
    object Launching : SessionState()

    /**
     * 运行中 - 调试目标正在运行
     *
     * @param activeThreadId 当前活跃的线程ID，null表示没有活跃线程
     */
    data class Running(val activeThreadId: Long? = null) : SessionState()

    /**
     * 已暂停 - 调试目标在断点或步进时暂停
     *
     * @param threadId 暂停的线程ID
     * @param reason 暂停原因
     * @param description 暂停描述
     */
    data class Suspended(
        val threadId: Long,
        val reason: String,
        val description: String? = null
    ) : SessionState()

    /**
     * 终止中 - 正在终止调试会话
     */
    object Terminating : SessionState()

    /**
     * 已终止 - 调试会话已结束
     *
     * @param exitCode 退出代码，null表示未知或不适用
     */
    data class Terminated(val exitCode: Int? = null) : SessionState()

    /**
     * 错误状态 - 会话遇到错误
     *
     * @param error 错误信息
     * @param recoverable 是否可恢复
     */
    data class Error(val error: Throwable, val recoverable: Boolean = false) : SessionState()

    /**
     * 断开连接 - 连接已断开但可能恢复
     */
    object Disconnected : SessionState()
}

/**
 * 状态转换事件
 */
sealed class StateTransition {
    data class Changed(val from: SessionState, val to: SessionState) : StateTransition()
    data class Failed(val from: SessionState, val error: Throwable) : StateTransition()
}

/**
 * 判断状态是否为活跃状态（可以执行调试操作）
 */
fun SessionState.isActive(): Boolean = when (this) {
    is SessionState.Running,
    is SessionState.Suspended -> true

    else -> false
}

/**
 * 判断状态是否为终止状态
 */
fun SessionState.isTerminated(): Boolean = when (this) {
    is SessionState.Terminated,
    is SessionState.Error -> true

    else -> false
}

/**
 * 判断状态是否可以执行步进操作
 */
fun SessionState.canStep(): Boolean = this is SessionState.Suspended

/**
 * 判断状态是否可以继续执行
 */
fun SessionState.canContinue(): Boolean = this is SessionState.Suspended

/**
 * 判断状态是否可以暂停
 */
fun SessionState.canPause(): Boolean = this is SessionState.Running