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

package org.cangnova.cangjie.debugger.protobuf.memory

import org.cangnova.cangjie.debugger.protobuf.data.LLDBDisasmInstruction

/**
 * 内存单元格 - 最小数据单位
 *
 * 稀疏矩阵架构的核心数据单元，支持多种单元格类型。
 *
 * 设计要点：
 * - 不可变数据结构
 * - 状态明确定义
 * - 支持版本追踪
 */
sealed class MemoryCell {
    abstract val address: Address
    abstract val state: CellState

    open fun toBytes(): ByteArray {
        return ByteArray(0) // Placeholder implementation
    }

    /**
     * 数据单元格 - 表示实际的内存数据
     *
     * @param address 内存地址
     * @param value 字节值
     * @param version 版本号，用于跟踪数据变化
     * @param state 单元格状态
     */
    data class DataCell(
        override val address: Address,
        val value: Byte,
        val version: Long,
        override val state: CellState = CellState.LOADED
    ) : MemoryCell() {
        override fun toBytes(): ByteArray {
            return byteArrayOf(value)
        }


    }

    /**
     * 指令单元格 - 存储完整的反汇编指令对象
     *
     * 使用完整的 LLDBDisasmInstruction 对象，包含所有指令信息：
     * - 地址、机器码、汇编文本
     * - 注释、符号信息
     * - 源代码位置
     * - 函数偏移等
     *
     * @param instruction 完整的LLDB反汇编指令对象
     * @param state 单元格状态
     */
    data class InstructionCell(
        val instruction: LLDBDisasmInstruction,
        override val state: CellState = CellState.LOADED
    ) : MemoryCell() {

        override fun toBytes(): ByteArray {
            return instruction.machineCode
        }

        // 地址直接从指令对象获取
        override val address: Address
            get() = instruction.address
        val range = instruction.range

        // 便捷访问器
        val size: Int
            get() = instruction.size
        val machineCode: ByteArray
            get() = instruction.machineCode
        val disassembly: String
            get() = instruction.disassembly
        val comment: String
            get() = instruction.comment
        val sourceLocation: org.cangnova.cangjie.debugger.protobuf.location.SourceLocation?
            get() = instruction.sourceLocation
        val symbol: String?
            get() = instruction.symbol

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as InstructionCell

            if (instruction != other.instruction) return false
            if (state != other.state) return false

            return true
        }

        override fun hashCode(): Int {
            var result = instruction.hashCode()
            result = 31 * result + state.hashCode()
            return result
        }
    }

    /**
     * 占位单元格 - 表示正在加载或空的位置
     *
     * @param address 地址
     * @param state 单元格状态
     */
    data class PlaceholderCell(
        override val address: Address,
        override val state: CellState = CellState.LOADING
    ) : MemoryCell()
}

/**
 * 单元格状态
 *
 * 定义单元格的生命周期状态
 */
enum class CellState {
    /** 空单元格 - 还未加载数据 */
    EMPTY,

    /** 正在加载中 */
    LOADING,

    /** 已加载完成 */
    LOADED,

    /** 加载出错 */
    ERROR,

}