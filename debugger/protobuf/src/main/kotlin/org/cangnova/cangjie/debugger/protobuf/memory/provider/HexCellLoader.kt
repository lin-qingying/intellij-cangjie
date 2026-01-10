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
import org.cangnova.cangjie.debugger.protobuf.services.MemoryService

/**
 * 十六进制内存单元格加载器
 *
 * 使用 MemoryService 从调试器加载内存数据，转换为 DataCell
 *
 * @param memoryService 内存服务
 * @param pageSize 每次加载的页面大小（字节），默认 256
 */
class HexCellLoader(
    private val memoryService: MemoryService,

    ) : MemoryCellLoader<MemoryCell.DataCell> {

    /**
     * 从指定地址开始加载一页内存
     *
     * 加载策略：
     * 1. 对齐到页面边界（address / pageSize * pageSize）
     * 2. 从对齐后的地址开始加载 pageSize 字节
     * 3. 转换为 DataCell 列表
     *
     * @param address 起始地址（会对齐到页面边界）
     * @return DataCell 列表（从页面开始到结束）
     */
    override suspend fun load(address: Address): List<MemoryCell.DataCell> {
        TODO("请使用loadRange")
    }

    override suspend fun loadRange(address: AddressRange): List<MemoryCell.DataCell> {


        // 2. 从 MemoryService 读取内存
        return memoryService.readMemory(address).mapNotNull {
            if (it is MemoryCell.DataCell) it else null
        }
    }
}