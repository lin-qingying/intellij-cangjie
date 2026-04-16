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

package org.cangnova.cangjie.debugger.protobuf.services

import org.cangnova.cangjie.debugger.protobuf.breakpoint.DebugPausePoint
import org.cangnova.cangjie.debugger.protobuf.data.LLDBThread
import org.cangnova.cangjie.debugger.protobuf.memory.Address

/**
 * 步进控制服务接口
 *
 * 负责程序执行控制：单步、继续、中断等
 */
interface SteppingService {
    /**
     * 继续执行程序
     *
     * @return 是否成功
     */
    suspend fun resume(): Boolean

    /**
     * 运行到指定行
     *
     * @param path 源文件路径
     * @param line 行号
     */
    suspend fun runToLine(path: String, line: Int)


    /**
     * 运行到指定地址
     *
     * @param address 目标地址
     */
    suspend fun runToAddress(address: Address)


    /**
     * 中断程序执行
     *
     * @return 是否成功
     */
    suspend fun suspend(): Boolean

    /**
     * 单步进入
     *
     * @param thread 目标线程
     * @param forceStepIntoFramesWithNoDebugInfo 是否强制进入无调试信息的帧
     * @param stepByInstruction 是否按指令单步
     */
    suspend fun stepInto(
        thread: LLDBThread,
        forceStepIntoFramesWithNoDebugInfo: Boolean = false,
        stepByInstruction: Boolean = false
    )

    /**
     * 单步跨过
     *
     * @param thread 目标线程
     * @param stepByInstruction 是否按指令单步
     */
    suspend fun stepOver(
        thread: LLDBThread,
        stepByInstruction: Boolean = false
    )

    /**
     * 单步跳出
     *
     * @param thread 目标线程
     * @param stopInFramesWithNoDebugInfo 是否在无调试信息的帧中停止
     */
    suspend fun stepOut(
        thread: LLDBThread,
        stopInFramesWithNoDebugInfo: Boolean = false
    )


    /**
     * 跳转到指定地址
     *
     * @param thread 目标线程
     * @param address 目标地址
     * @param canLeaveFunction 是否允许离开当前函数
     * @return 停止位置
     */
    suspend fun jumpToAddress(
        thread: LLDBThread,
        address: Address,
        canLeaveFunction: Boolean
    ): DebugPausePoint

    /**
     * 跳转到指定行
     *
     * @param thread 目标线程
     * @param path 源文件路径
     * @param line 行号
     * @param canLeaveFunction 是否允许离开当前函数
     * @return 停止位置
     */
    suspend fun jumpToLine(
        thread: LLDBThread,
        path: String,
        line: Int,
        canLeaveFunction: Boolean
    ): DebugPausePoint

    /**
     * 冻结线程
     *
     * @param thread 要冻结的线程
     */
    suspend fun freezeThread(thread: LLDBThread)

    /**
     * 解冻线程
     *
     * @param thread 要解冻的线程
     */
    suspend fun unfreezeThread(thread: LLDBThread)

    /**
     * 冻结其他线程
     *
     * @param thread 不冻结的线程
     */
    suspend fun freezeOtherThreads(thread: LLDBThread)

    /**
     * 解冻所有线程
     *
     * @param thread 参考线程
     */
    suspend fun unfreezeAllThreads(thread: LLDBThread)

    /**
     * 是否支持跳转到行
     */
    fun supportsJumpToLine(): Boolean

    /**
     * 是否支持冻结其他线程
     */
    fun supportsFreezeOtherThreads(): Boolean

    /**
     * 是否支持冻结单个线程
     */
    fun supportsFreezeSingleThread(): Boolean
}
