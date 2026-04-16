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

import org.cangnova.cangjie.debugger.protobuf.data.*
import org.cangnova.cangjie.debugger.protobuf.memory.Address
import org.cangnova.cangjie.debugger.protobuf.memory.AddressRange

/**
 * 反汇编服务接口
 *
 * 职责：
 * - 调试器的反汇编和寄存器访问协议
 * - 提供 LLDB/GDB 特定的功能
 * - 返回调试器特定的数据类型（LLDBDisasmInstruction, LLDBRegister）
 *
 * 不包含：
 * - 内存读写（由 MemoryService 负责）
 * - 视图显示逻辑（由 DisasmStore 和 MemoryViewFacade 负责）
 * - 数据缓存（由 MemoryStore 负责）
 *
 * 这是调试器协议层，为上层服务提供原始的反汇编能力。
 */
interface DisasmService {
    /**
     * 反汇编函数
     *
     * @param address 函数地址
     * @param fallbackRange 备用地址范围
     * @return 指令列表
     */
    suspend fun disassembleFunction(
        address: Address,

        ): List<LLDBDisasmInstruction>

    suspend fun disassembleRange(
        range: AddressRange,

        ): List<LLDBDisasmInstruction>

    /**
     * 获取寄存器值
     *
     * @param thread 目标线程
     * @param frame 目标栈帧
     * @return 寄存器值列表
     */
    suspend fun getRegisters(
        thread: LLDBThread,
        frame: LLDBFrame
    ): List<LLDBRegister>

    /**
     * 获取指定寄存器的值
     *
     * @param thread 目标线程
     * @param frame 目标栈帧
     * @param registerNames 寄存器名称集合
     * @return 寄存器值列表
     */
    suspend fun getRegisters(
        thread: LLDBThread,
        frame: LLDBFrame,
        registerNames: Set<String>
    ): List<LLDBRegister>


    /**
     * 获取寄存器组信息
     *
     * 只获取寄存器组的元数据信息（组名、寄存器数量等），
     * 不包含具体的寄存器值。这可以用于初始化寄存器UI界面。
     *
     * @param thread 目标线程
     * @param frame 目标栈帧
     * @return 寄存器组信息列表，不包含具体的寄存器值
     */
    suspend fun getRegisterGroups(
        thread: LLDBThread,
        frame: LLDBFrame
    ): List<LLDBRegisterGroup>

    /**
     * 是否支持寄存器访问
     */
    fun supportsRegisters(): Boolean
}
