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

package org.cangnova.cangjie.debugger.protobuf.execution.state

/**
 * 目标程序执行状态
 *
 * 表示被调试程序的执行状态，用于调试器状态管理。
 * 状态转换遵循明确的规则，确保调试流程的一致性。
 *
 * 状态转换流程：
 * ```
 * Idle -> Running -> Paused -> Running -> Exiting -> Terminated
 *        └────┬──────┘
 *             └─ 可循环
 * ```
 */
enum class TargetState {
    /**
     * 空闲状态
     *
     * 调试器正在初始化，程序尚未开始执行。
     */
    Idle,

    /**
     * 运行状态
     *
     * 程序正在自由执行，不受调试器干预。
     * 此状态下大多数调试操作不可用。
     */
    Running,

    /**
     * 暂停状态
     *
     * 程序已暂停执行，等待调试器指令。
     * 此状态下可以进行完整的调试操作。
     */
    Paused,

    /**
     * 退出中状态
     *
     * 程序正在执行清理工作并即将退出。
     */
    Exiting,

    /**
     * 已终止状态
     *
     * 程序已完全执行完毕并退出，调试会话结束。
     */
    Terminated;

    /**
     * 检查是否可以转换到指定状态
     */
    fun canTransitionTo(newState: TargetState): Boolean {
        return StateTransitions.isValidTransition(this, newState)
    }

    /**
     * 检查当前状态是否允许执行调试操作
     */
    val isDebuggable: Boolean
        get() = this == Paused

    /**
     * 检查程序是否正在运行
     */
    val isRunning: Boolean
        get() = this == Running

    /**
     * 检查程序是否已结束
     */
    val isTerminated: Boolean
        get() = this == Terminated || this == Exiting

    /**
     * 检查程序是否空闲
     */
    val isIdle: Boolean
        get() = this == Idle
}