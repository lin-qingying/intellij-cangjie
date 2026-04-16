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
import org.cangnova.cangjie.debugger.protobuf.memory.CellState
import org.cangnova.cangjie.debugger.protobuf.memory.MemoryCell
import org.cangnova.cangjie.debugger.protobuf.services.DisasmService

/**
 * 反汇编单元格提供者
 *
 * 使用 DisasmService 从调试器加载反汇编数据，转换为 InstructionCell
 *
 * @param disasmService 反汇编服务
 * @param instructionCount 每次加载的指令数量，默认 50
 */
class DisasmCellLoader(
    private val disasmService: DisasmService,
    private val instructionCount: Int = 50
) : MemoryCellLoader<MemoryCell.InstructionCell> {

    /**
     * 从指定地址开始反汇编指令
     *
     * 加载策略：
     * 1. 从 address 开始反汇编 instructionCount 条指令
     * 2. 转换为 InstructionCell 列表
     *
     * @param address 起始地址
     * @return InstructionCell 列表
     */
    override suspend fun load(address: Address): List<MemoryCell.InstructionCell> {
        // 1. 从 DisasmService 反汇编
        val instructions = disasmService.disassembleFunction(
            address = address,

            )

        // 2. 转换为 InstructionCell
        return instructions.map { lldbInstruction ->
            MemoryCell.InstructionCell(

                instruction = lldbInstruction,
                state = CellState.LOADED
            )
        }
    }

    override suspend fun loadRange(address: AddressRange): List<MemoryCell.InstructionCell> {
        // 1. 从 DisasmService 反汇编
        val instructions = disasmService.disassembleRange(
            range = address,

            )

        // 2. 转换为 InstructionCell
        return instructions.map { lldbInstruction ->
            MemoryCell.InstructionCell(

                instruction = lldbInstruction,
                state = CellState.LOADED
            )
        }
    }
}