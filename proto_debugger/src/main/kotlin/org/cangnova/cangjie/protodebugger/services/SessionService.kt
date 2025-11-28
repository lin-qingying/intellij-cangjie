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

package org.cangnova.cangjie.protodebugger.services

import com.intellij.execution.configurations.GeneralCommandLine
import org.cangnova.cangjie.protodebugger.data.LLDBFrame
import org.cangnova.cangjie.protodebugger.data.LLDBThread
import org.cangnova.cangjie.protodebugger.execution.state.TargetState
import java.io.OutputStream

/**
 * 会话服务接口
 *
 * 负责调试会话的生命周期管理、进程控制和状态管理
 */
interface SessionService {


    suspend fun startTarget()


    /**
     * 启动进程进行调试
     *
     * @param commandLine 可执行命令行
     * @param architecture 目标架构
     * @return DebugTarget对象
     */

    suspend fun loadForLaunch(commandLine: GeneralCommandLine, architecture: String?)

    /**
     * 附加到进程
     *
     * @param processId 进程ID
     * @return DebugTarget对象
     */
    suspend fun loadForAttach(processId: Long)


    /**
     * 加载远程调试
     *
     * @param commandLine 可执行命令行
     * @param architecture 目标架构
     * @param platform 平台名称
     * @param url 远程URL
     * @return DebugTarget对象
     */
    suspend fun loadForRemote(
        commandLine: GeneralCommandLine,
        architecture: String?,
        platform: String,
        url: String
    ): DebugTarget

    /**
     * 分离调试器
     *
     * @return 是否成功
     */
    suspend fun detach(): Boolean

    /**
     * 终止调试进程
     *
     * @return 是否成功
     */
    suspend fun terminate(): Boolean

    /**
     * 退出调试会话
     *
     * @return 是否成功
     */
    suspend fun exit(): Boolean

    /**
     * 断开与调试目标的连接
     *
     * 终止与当前调试目标的连接，可选择是否销毁目标进程。
     * 此方法用于清理调试会话并释放相关资源。
     *
     * @param shouldDestroy 是否应该销毁目标进程
     *                     - true: 销毁目标进程，强制终止
     *                     - false: 仅断开连接，保持目标进程运行
     */
    suspend fun disconnectTarget(shouldDestroy:Boolean): Unit
    /**
     * 获取所有线程
     *
     * @return 线程列表
     */
    suspend fun getThreads(): List<LLDBThread>

    /**
     * 获取栈帧
     *
     * @param thread 目标线程
     * @param startFrame 起始帧索引
     * @param maxFrames 最大帧数
     * @return 栈帧列表
     */
    suspend fun getFrames(
        thread: LLDBThread,
        startFrame: Int,
        maxFrames: Int
    ): List<LLDBFrame>

    /**
     * 获取当前停止的线程
     *
     * @return 停止的线程，如果没有则返回null
     */
    fun getStoppedThread(): LLDBThread?

    /**
     * 获取调试器状态
     *
     * @return 目标状态
     */
    fun getState(): TargetState

    /**
     * 获取进程输入流
     *
     * @return 输出流，如果不可用则返回null
     */
    fun getProcessInput(): OutputStream?





    /**
     * 执行解释器命令
     *
     * @param threadId 线程ID
     * @param frameIndex 栈帧索引
     * @param command 命令字符串
     * @return 命令输出
     */
    suspend fun executeInterpreterCommand(
        threadId: Long,
        frameIndex: Int,
        command: String
    ): String


    /**
     * 是否处于提示模式
     *
     * @return 是否处于提示模式
     */
    fun isInPromptMode(): Boolean

    /**
     * 完成控制台命令
     *
     * @param command 命令前缀
     * @param pos 光标位置
     * @return 补全建议列表
     */
    suspend fun completeConsoleCommand(command: String, pos: Int): List<String?>

    /**
     * 调整终端大小
     *
     * @param columns 列数
     * @param rows 行数
     */
    suspend fun resize(columns: Int, rows: Int)

    /**
     * 检查错误
     */
    suspend fun checkErrors()

    /**
     * 是否支持命令取消
     */
    fun supportsCommandCancellation(): Boolean

    /**
     * DebugTarget接口 - 代表被调试的进程
     */
    interface DebugTarget {
        /**
         * 获取进程ID
         */
        fun getId(): Long

        /**
         * 启动进程
         *
         * @return 进程ID
         */
        suspend fun start(): Long

        /**
         * 分离进程
         */
        suspend fun detach()

        /**
         * 销毁进程
         *
         * @return 是否成功
         */
        suspend fun destroy(): Boolean
    }
}
