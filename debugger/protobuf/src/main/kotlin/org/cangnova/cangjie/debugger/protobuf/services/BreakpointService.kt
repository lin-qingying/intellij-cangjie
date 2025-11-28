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

import org.cangnova.cangjie.debugger.protobuf.breakpoint.AddBreakpointResult
import org.cangnova.cangjie.debugger.protobuf.breakpoint.SymbolicBreakpoint

import org.cangnova.cangjie.debugger.protobuf.data.LLDBVariable
import org.cangnova.cangjie.debugger.protobuf.data.LLDBWatchpoint
import org.cangnova.cangjie.debugger.protobuf.memory.Address

/**
 * 断点服务接口
 *
 * 负责管理所有类型的断点：行断点、地址断点、符号断点和观察点
 */
interface BreakpointService : AutoCloseable {
    /**
     * 添加行断点
     *
     * @param path 源文件路径
     * @param line 行号
     * @param condition 条件表达式（可选）
     * @return 断点添加结果
     */
    suspend fun addLineBreakpoint(
        path: String,
        line: Int,
        condition: String? = null
    ): AddBreakpointResult

    /**
     * 添加地址断点
     *
     * @param address 内存地址
     * @param condition 条件表达式（可选）
     * @return 断点添加结果
     */
    suspend fun addAddressBreakpoint(
        address: Address,
        condition: String? = null
    ): AddBreakpointResult

    /**
     * 添加符号断点
     *
     * @param symbolPattern 符号模式（函数名等）
     * @param module 模块名（可选）
     * @param condition 条件表达式（可选）
     * @return 符号断点对象，失败返回null
     */
    suspend fun addSymbolicBreakpoint(
        symbolPattern: String,
        module: String? = null,
        condition: String? = null
    ): AddBreakpointResult

    /**
     * 添加符号断点（使用SymbolicBreakpoint对象）
     *
     * @param breakpoint 符号断点配置
     * @return 符号断点对象，失败返回null
     */
    suspend fun addSymbolicBreakpoint(breakpoint: SymbolicBreakpoint): AddBreakpointResult?

    /**
     * 添加观察点（内存监视点）
     *
     * @param address 监视的内存地址
     * @param size 监视的字节数
     * @param read 是否监视读操作
     * @param write 是否监视写操作
     * @param condition 条件表达式（可选）
     * @return 观察点对象
     */
    suspend fun addWatchpoint(
        threadId: Long,
        frameIndex: Int,
        value: LLDBVariable,
        expr: String,
        lifetime: LLDBWatchpoint.Lifetime?,
        accessType: LLDBWatchpoint.AccessType
    ): LLDBWatchpoint

    /**
     * 移除断点
     *
     * @param ids 断点ID集合
     */
    suspend fun removeBreakpoints(ids: Collection<Long>)


    /**
     * 是否支持观察点
     */
    fun supportsWatchpoints(): Boolean

    /**
     * 是否支持观察点生命周期管理
     */
    fun supportsWatchpointLifetime(): Boolean
}
