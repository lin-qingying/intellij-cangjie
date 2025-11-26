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

package org.cangnova.cangjie.protodebugger.core

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.Key
import org.cangnova.cangjie.protodebugger.breakpoint.DebugPausePoint
import org.cangnova.cangjie.protodebugger.execution.exit.ExitStatus
import org.cangnova.cangjie.protodebugger.memory.Address
import java.util.*

/**
 * 调试器事件处理器接口
 *
 * 该接口定义了调试器运行过程中的各种事件处理方法，
 * 包括进程状态变化、断点事件、异常处理、输出处理等。
 * 实现该接口可以监听和响应调试器的各种事件。
 *
 * 使用场景：
 * - 调试器UI更新和状态同步
 * - 断点管理和状态跟踪
 * - 异常处理和错误报告
 * - 输出信息的显示和处理
 *
 * 主要功能：
 * - 调试进程生命周期事件处理
 * - 断点和观察点事件处理
 * - 线程和栈帧变化通知
 * - 异常和信号事件处理
 * - 输出和通知信息处理
 */
interface DebuggerHandler : EventListener {

    /**
     * 处理程序运行事件
     *
     * 当调试目标开始运行时调用。
     */
    fun handleRunning() {
    }


    /**
     * 处理程序停止事件
     *
     * 当程序执行被中断（如用户手动暂停）时调用。
     *
     * @param debugPausePoint 中断的位置信息
     */
    fun handleStopped(debugPausePoint: DebugPausePoint) {

    }

    /**
     * 处理信号事件
     *
     * 当程序接收到信号时调用。
     *
     * @param debugPausePoint 信号发生的位置
     * @param signal 信号名称
     * @param meaning 信号含义说明
     */
    fun handleSignal(debugPausePoint: DebugPausePoint) {
        handleStopped(debugPausePoint)
    }

    /**
     * 处理异常事件
     *
     * 当程序抛出异常时调用。
     *
     * @param debugPausePoint 异常发生的位置，包含文件和行号信息
     * @param exceptionType 异常类型名称（如 "NullPointerException"、"IndexOutOfBoundsException"）
     * @param exceptionMessage 异常的详细描述消息
     * @param exceptionAddress 异常发生的内存地址（可选，某些异常可能没有特定地址）
     */
    fun handleException(
        debugPausePoint: DebugPausePoint,
        exceptionType: String,
        exceptionMessage: String,
        exceptionAddress: Address? = null
    ) {
        handleStopped(debugPausePoint)

    }

    fun handleBreakpoint(debugPausePoint: DebugPausePoint) {
        handleStopped(debugPausePoint)

    }


    fun handleTargetOutput(text: String, type: Key<*>) {


    }

    fun handleDebuggerOutput(text: String, type: Key<*>) {

    }


    fun handleTargetTerminated(exitStatus: ExitStatus) {

    }

    fun handleExited(code: Int) {
    }


    /**
     * 处理模块加载事件
     *
     * 当新的模块（可执行文件或共享库）被加载到进程中时调用。
     *
     * @param modules 加载的模块列表
     */
    fun handleModulesLoaded(modules: List<lldbprotobuf.Model.Module>) {
    }

    /**
     * 处理模块卸载事件
     *
     * 当模块（共享库）被卸载时调用。
     *
     * @param modules 卸载的模块列表
     */
    fun handleModulesUnloaded(modules: List<lldbprotobuf.Model.Module>) {
    }

    /**
     * 处理断点状态变更事件
     *
     * 当断点被添加、删除、启用、禁用或解析时调用。
     *
     * @param breakpoint 变更的断点信息
     * @param changeType 变更类型
     * @param description 变更描述
     */
    fun handleBreakpointChanged(
        breakpoint: lldbprotobuf.Model.Breakpoint,
        changeType: lldbprotobuf.Model.BreakpointEventType,
        description: String?
    ) {
    }

    /**
     * 处理线程状态变更事件
     *
     * 当线程被创建、销毁或状态改变时调用。
     *
     * @param thread 变更的线程信息
     * @param changeType 变更类型
     * @param description 变更描述
     */
    fun handleThreadStateChanged(
        thread: lldbprotobuf.Model.Thread,
        changeType: lldbprotobuf.Model.ThreadStateChangeType,
        description: String?
    ) {
    }

    /**
     * 处理符号加载事件
     *
     * 当调试符号被加载时调用。
     *
     * @param module 加载符号的模块
     * @param symbolCount 加载的符号数量
     * @param symbolFilePath 符号文件路径（可选）
     */
    fun handleSymbolsLoaded(
        module: lldbprotobuf.Model.Module,
        symbolCount: Int,
        symbolFilePath: String?
    ) {
    }


    companion object {
        /**
         * 日志记录器，用于调试和问题排查
         */
        val LOG = Logger.getInstance("#" + DebuggerHandler::class.java.getPackage().name)

    }
}

