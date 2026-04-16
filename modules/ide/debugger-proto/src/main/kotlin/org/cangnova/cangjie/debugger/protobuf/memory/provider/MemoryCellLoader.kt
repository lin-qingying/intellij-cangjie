/*
 * Copyright 2026 LinQingYing. and contributors.
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

package org.cangnova.cangjie.debugger.protobuf.memory.provider

import org.cangnova.cangjie.debugger.protobuf.memory.Address
import org.cangnova.cangjie.debugger.protobuf.memory.AddressRange
import org.cangnova.cangjie.debugger.protobuf.memory.MemoryCell

/**
 * 内存单元格数据加载器接口
 *
 * 职责：
 * - 从数据源（MemoryService、DisasmService 等）加载数据
 * - 转换为 MemoryCell 类型
 * - 返回加载的单元格列表
 *
 * 设计原则：
 * - 接口职责单一：只负责数据加载
 * - 参数是单个 address，由实现决定加载多少数据
 * - 返回的 List 可以包含多个连续的 Cell（例如一页内存或多条指令）
 *
 * @param T 单元格类型（DataCell 或 InstructionCell）
 */
interface MemoryCellLoader<T : MemoryCell> {

    /**
     * 加载指定地址的数据
     *
     * 实现注意事项：
     * - HexCellLoader: 从 address 开始加载一页内存（如 256 字节）
     * - DisasmCellLoader: 从 address 开始反汇编一组指令（如 20 条）
     * - 返回的 List 包含从 address 开始的连续数据
     * - 如果加载失败，返回空列表或抛出异常
     *
     * @param address 起始地址
     * @return 加载的单元格列表（从 address 开始的连续数据）
     */
    suspend fun load(address: Address): List<T>


    suspend fun loadRange(address: AddressRange): List<T>
}