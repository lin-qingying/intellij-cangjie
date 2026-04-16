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

package org.cangnova.cangjie.debugger.protobuf.data

import org.cangnova.cangjie.debugger.protobuf.location.SourceLocation
import org.cangnova.cangjie.debugger.protobuf.memory.Address
import org.cangnova.cangjie.debugger.protobuf.memory.AddressRange

/**
 * LLDB 反汇编指令类 (LLDBDisasmInstruction)
 *
 * 该类表示调试器中的一条机器指令，对应 proto 中的 DisassembleInstruction。
 * 它是反汇编器和调试器显示机器代码的基础数据结构。
 *
 * 使用场景：
 * - 在调试器中显示反汇编代码
 * - 提供机器指令的详细信息
 * - 支持断点和单步执行
 * - 显示指令和注释信息
 * - 显示源代码位置关联信息
 *
 * 主要功能：
 * - 存储指令的虚拟地址
 * - 记录指令的机器码（字节数据）
 * - 提供指令的汇编文本表示
 * - 包含指令的注释和符号信息
 * - 关联对应的源代码位置
 *
 * @param address 指令的虚拟地址（对应 proto address 字段）
 * @param machineCode 指令的机器码（字节数组，对应 proto machine_code 字段）
 * @param instruction 指令的汇编文本表示（对应 proto instruction 字段）
 * @param comment 指令的注释说明（对应 proto comment 字段）
 * @param size 指令长度（字节数，对应 proto size 字段）
 * @param sourceLocation 对应的源代码位置（对应 proto source_location 字段）
 * @param symbol 符号信息（如果地址对应符号，对应 proto symbol 字段）
 * @param functionOffset 指令相对于函数起始地址的偏移量（保持向后兼容）
 */
data class LLDBDisasmInstruction(
    val address: Address,
    val machineCode: ByteArray,
    val instruction: String,
    val comment: String,
    val size: Int,
    val sourceLocation: SourceLocation?,
    val symbol: String?,
    val functionOffset: LLDBSymbolOffset? = null  // 保持向后兼容
) {
    companion object {
        /**
         * 从 DisassembleInstruction proto 创建指令对象
         *
         * 使用场景：
         * - 从调试器服务返回的 proto 消息创建指令对象
         * - 反汇编服务处理结果转换
         *
         * @param address 指令的虚拟地址
         * @param machineCode 指令的机器码（字节数组）
         * @param instruction 指令的汇编文本表示
         * @param comment 指令的注释说明
         * @param size 指令长度（字节数）
         * @param sourceLocation 对应的源代码位置（可选）
         * @param symbol 符号信息（可选）
         * @param functionOffset 函数偏移量（向后兼容，可选）
         * @return 创建的LLDBDisasmInstruction对象
         */
        @JvmStatic
        fun fromProto(
            address: Address,
            machineCode: ByteArray,
            instruction: String,
            comment: String = "",
            size: Int,
            sourceLocation: SourceLocation? = null,
            symbol: String? = null,
            functionOffset: LLDBSymbolOffset? = null
        ): LLDBDisasmInstruction {
            return LLDBDisasmInstruction(
                address = address,
                machineCode = machineCode,
                instruction = instruction,
                comment = comment,
                size = size,
                sourceLocation = sourceLocation,
                symbol = symbol,
                functionOffset = functionOffset
            )
        }

        /**
         * 从基本组件创建指令对象（向后兼容方法）
         *
         * 根据地址、操作码、助记符、操作数等信息创建指令对象。
         * 自动格式化汇编文本，将助记符和操作数组合成标准格式。
         *
         * 使用场景：
         * - 反汇编器生成指令对象
         * - 调试器解析机器码
         * - 从二进制文件创建指令信息
         *
         * @param address 指令的起始地址
         * @param opcodesBytes 指令的机器码字节序列
         * @param mnemonic 指令的助记符（如MOV、ADD等）
         * @param operands 指令的操作数部分
         * @param comment 指令的注释信息，可以为null
         * @param sourceLocation 源代码位置信息（可选）
         * @param symbol 符号信息（可选）
         * @param functionOffset 函数偏移量（向后兼容，可选）
         * @return 创建的LLDBDisasmInstruction对象
         */
        @JvmStatic
        fun create(
            address: Address,
            opcodesBytes: Iterable<Byte>,
            mnemonic: String,
            operands: String,
            comment: String? = null,
            sourceLocation: SourceLocation? = null,
            symbol: String? = null,
            functionOffset: LLDBSymbolOffset? = null
        ): LLDBDisasmInstruction {
            val opcodesList = opcodesBytes.toList()
            val formattedInstruction = "%1$-6s %2\$s".format(mnemonic, operands)

            return LLDBDisasmInstruction(
                address = address,
                machineCode = opcodesList.toByteArray(),
                instruction = formattedInstruction,
                comment = comment ?: "",
                size = opcodesList.size,
                sourceLocation = sourceLocation,
                symbol = symbol,
                functionOffset = functionOffset
            )
        }
    }

    /**
     * 获取指令的内存地址范围
     *
     * @return 指令在内存中的地址范围
     */
    val range: AddressRange get() = address.rangeTo(address + size)

    /**
     * 获取机器码的十六进制字符串表示
     *
     * @return 机器码的十六进制字符串
     */
    val machineCodeHex: String get() = machineCode.joinToString("") { "%02X".format(it) }

    /**
     * 获取完整的反汇编文本
     *
     * 将指令文本和注释信息组合成完整的显示格式。
     * 如果注释不为空，则在指令文本后添加注释。
     *
     * 格式规则：
     * - 如果有注释：指令文本（33字符宽度对齐，使用空格填充） + " ; " + 注释
     * - 如果无注释：仅显示指令文本
     * - 最终结果会去除首尾空白字符
     *
     * 使用场景：
     * - 在调试器UI中显示反汇编代码
     * - 生成汇编代码的文本输出
     * - 调试日志和报告生成
     *
     * @return 格式化的反汇编文本字符串
     */
    val disassembly: String = ((if ((comment as CharSequence).isNotBlank()) instruction.padEnd(
        33,
        ' ',  // 使用空格而不是 null 字符
    ) + " ; " + comment else instruction) as CharSequence).trim().toString()

    /**
     * 检查是否有符号信息
     *
     * @return true 如果有符号名称，false 否则
     */
    fun hasSymbol(): Boolean = !symbol.isNullOrBlank()

    /**
     * 检查是否有源代码位置信息
     *
     * @return true 如果有源代码位置，false 否则
     */
    fun hasSourceLocation(): Boolean = sourceLocation != null

    /**
     * 返回指令的字符串表示
     *
     * @return 指令的反汇编文本
     */
    override fun toString(): String {
        return disassembly
    }

    /**
     * 返回详细的指令信息字符串
     *
     * @return 包含地址、机器码、指令文本等详细信息的字符串
     */
    fun toDetailedString(): String {
        return buildString {
            appendLine("地址: 0x${address.value.toString(16).uppercase()}")
            appendLine("机器码: $machineCodeHex")
            appendLine("指令: $instruction")
            if (comment.isNotBlank()) {
                appendLine("注释: $comment")
            }
            if (hasSymbol()) {
                appendLine("符号: $symbol")
            }
            if (hasSourceLocation()) {
                appendLine("源码: ${sourceLocation?.path}:${sourceLocation?.line}")
            }
            appendLine("大小: $size 字节")
        }
    }
}


/**
 * 从 DisassembleInstruction proto 消息转换的扩展函数
 *
 * @return 转换后的 LLDBDisasmInstruction 对象
 */
fun lldbprotobuf.Model.DisassembleInstruction.toLLDBInstruction(): LLDBDisasmInstruction {
    val address = Address.Companion.Factory.fromLong(this.address)
    val sourceLocation = if (this.hasSourceLocation()) {
        SourceLocation(
            path = this.sourceLocation.filePath,
            line = this.sourceLocation.line.toInt()
        )
    } else {
        null
    }

    return LLDBDisasmInstruction.fromProto(
        address = address,
        machineCode = this.machineCode.toByteArray(),
        instruction = this.instruction,
        comment = this.comment,
        size = this.size.toInt(),
        sourceLocation = sourceLocation,
        symbol = if (this.symbol.isBlank()) null else this.symbol
    )
}

data class LLDBSymbolOffset(val symbolName: String, val offset: Long)
