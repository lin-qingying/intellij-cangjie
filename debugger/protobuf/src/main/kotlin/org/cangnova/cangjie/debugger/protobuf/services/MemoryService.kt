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

import org.cangnova.cangjie.debugger.protobuf.memory.Address
import org.cangnova.cangjie.debugger.protobuf.memory.AddressRange
import org.cangnova.cangjie.debugger.protobuf.memory.MemoryCell

/**
 * 内存服务接口
 *
 * 职责：
 * - 调试器内存数据的读写接口
 * - 实现 MemoryDataSource 接口以集成到内存视图
 * - 监听调试器的内存变化事件
 * - 提供内存监视点 (watchpoint) 功能
 *
 * 不包含：
 * - 反汇编功能（由 DisasmService 和 DisasmCellLoader 负责）
 * - 寄存器访问（由 DisasmService 负责）
 * - 视图显示逻辑（由 MemoryViewFacade 负责）
 */
interface MemoryService {
    /**
     * 读取内存范围（实现 MemoryDataSource 接口）
     *
     * @param range 地址范围
     * @return 内存单元格列表
     */
    suspend fun readMemory(range: AddressRange): List<MemoryCell>


    /**
     * 写入单个地址
     *
     * @param address 地址
     * @param value 字节值
     */
    suspend fun writeMemory(address: Address, value: Byte)

    /**
     * 写入连续内存
     *
     * @param address 起始地址
     * @param bytes 要写入的字节数组
     */
    suspend fun writeMemory(address: Address, bytes: ByteArray)

    /**
     * 批量写入内存
     *
     * @param updates 地址到字节的映射
     */
    suspend fun writeMemory(updates: Map<Address, Byte>)

    /**
     * 是否支持内存写入
     */
    fun supportsMemoryWrite(): Boolean


    /**
     * 内存搜索
     *
     * @param pattern 搜索模式（字节数组）
     * @param range 搜索范围
     * @return 匹配地址列表
     */
    suspend fun searchMemory(pattern: ByteArray, range: AddressRange): List<Address>
}

/**
 * 监视点访问类型
 */
enum class WatchpointAccess {
    READ,       // 读取时触发
    WRITE,      // 写入时触发
    READ_WRITE  // 读取或写入时触发
}

/**
 * 内存监视点
 */
data class Watchpoint(
    val address: Address,
    val size: Int,
    val access: WatchpointAccess,
    val enabled: Boolean = true
)
