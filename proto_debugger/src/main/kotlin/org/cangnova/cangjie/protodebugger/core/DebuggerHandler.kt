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
import java.util.EventListener
import com.intellij.openapi.util.Key
import org.cangnova.cangjie.protodebugger.breakpoint.StopPlace
import org.cangnova.cangjie.protodebugger.data.*
import org.cangnova.cangjie.protodebugger.execution.ExitStatus
import org.cangnova.cangjie.protodebugger.memory.Address
import org.cangnova.cangjie.protodebugger.notification.NotificationType
import org.cangnova.cangjie.protodebugger.util.DebuggerSourceFileHash

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
interface TargetHandler : EventListener {

    /**
     * 处理程序运行事件
     *
     * 当调试目标开始运行时调用。
     */
    fun handleRunning() {
    }

    /**
     * 处理模块加载事件
     *
     * 当新的模块（动态库、可执行文件等）被加载到调试目标时调用。
     *
     * @param modules 新加载的模块列表
     */
    fun handleModulesLoaded(modules: List<LLModule>) {
    }

    /**
     * 处理模块卸载事件
     *
     * 当模块从调试目标中卸载时调用。
     *
     * @param modules 被卸载的模块列表
     */
    fun handleModulesUnloaded(modules: List<LLModule>) {
    }

    /**
     * 处理断点添加事件
     *
     * 当新的断点被添加到调试器时调用。
     *
     * @param breakpoint 新添加的断点信息
     */
    fun handleBreakpointAdded(breakpoint: LLBreakpoint) {
    }

    /**
     * 处理断点移除事件
     *
     * 当断点从调试器中移除时调用。
     *
     * @param breakpointId 被移除的断点ID
     */
    fun handleBreakpointRemoved(breakpointId: Int) {
    }

    /**
     * 处理断点更新事件
     *
     * 当断点信息被更新时调用。
     *
     * @param breakpoint 更新后的断点信息
     */
    fun handleBreakpointUpdated(breakpoint: LLBreakpoint) {

    }

    /**
     * 处理断点位置替换事件
     *
     * 当断点的位置被完全替换时调用。
     *
     * @param breakpointId 断点ID
     * @param locations 新的断点位置列表
     */
    fun handleBreakpointLocationsReplaced(breakpointId: Int, locations: List<LLBreakpointLocation>) {

    }

    /**
     * 处理断点位置更新事件
     *
     * 当断点的位置信息被更新时调用。
     *
     * @param breakpointId 断点ID
     * @param locations 更新后的断点位置列表
     */
    fun handleBreakpointLocationsUpdated(breakpointId: Int, locations: List<LLBreakpointLocation>) {

    }

    /**
     * 处理断点位置移除事件
     *
     * 当断点的某些位置被移除时调用。
     *
     * @param breakpointId 断点ID
     * @param locationIds 被移除的位置ID列表
     */
    fun handleBreakpointLocationsRemoved(breakpointId: Int, locationIds: List<String>) {

    }

    /**
     * 处理程序中断事件
     *
     * 当程序执行被中断（如用户手动暂停）时调用。
     *
     * @param stopPlace 中断的位置信息
     */
    fun handleInterrupted(stopPlace: StopPlace) {

    }

    /**
     * 处理信号事件
     *
     * 当程序接收到信号时调用。
     *
     * @param stopPlace 信号发生的位置
     * @param signal 信号名称
     * @param meaning 信号含义说明
     */
    fun handleSignal(stopPlace: StopPlace, signal: String, meaning: String) {

    }

    /**
     * 处理异常事件
     *
     * 当程序抛出异常时调用。
     *
     * @param stopPlace 异常发生的位置
     * @param exceptionAddress 异常发生的地址
     * @param exceptionFile 异常发生的文件名
     * @param exceptionHash 异常文件的哈希值
     * @param exceptionLine 异常发生的行号
     * @param description 异常描述
     */
    fun handleException(
        stopPlace: StopPlace,
        exceptionAddress: Address,
        exceptionFile: String?,
        exceptionHash: DebuggerSourceFileHash?,
        exceptionLine: Int,
        description: String
    ) {

    }

    fun handleBreakpoint(stopPlace: StopPlace, breakpointNumber: Int) {

    }

    fun handleWatchpoint(stopPlace: StopPlace, watchpointNumber: Int) {

    }

    fun handleWatchpointScope(watchpointNumber: Int) {
    }

    fun handleTargetOutput(text: String, type: Key<*>) {


    }

    fun handleDebuggerOutput(text: String, type: Key<*>) {

    }

    fun handlePrompt(prompt: String) {

    }

    fun handleTargetTerminated(exitStatus: ExitStatus) {

    }

    fun handleExited(code: Int) {
    }

    fun handleAttached(pid: Int) {
    }

    fun handleDetached() {
    }

    fun handleConnected(connection: String) {

    }

    fun handleDisconnected() {
    }

    fun handleSelectedFrameChanged(thread: LLThread, frame: LLFrame) {

    }

    fun handleSymbolsDownloadStarted(caption: String, details: String) {

    }

    fun handleSymbolsDownloadProgress(percent: Int) {
    }

    fun handleSymbolsDownloadFinished() {
    }

    fun handleNotification(message: String, type: NotificationType) {

    }

    companion object {
        /**
         * 日志记录器，用于调试和问题排查
         */
        val LOG = Logger.getInstance("#" + TargetHandler::class.java.getPackage().name)

    }
}

