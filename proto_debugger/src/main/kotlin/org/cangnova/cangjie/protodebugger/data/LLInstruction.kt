package org.cangnova.cangjie.protodebugger.data

import org.cangnova.cangjie.protodebugger.memory.Address
import org.cangnova.cangjie.protodebugger.memory.AddressRange
import org.cangnova.cangjie.protodebugger.util.ByteList

import org.cangnova.cangjie.protodebugger.memory.rangeTo
import proto.Model
import java.util.ArrayList

/**
 * 低级别指令（LLInstruction）类
 *
 * 该类表示调试器中的一条机器指令，包含了指令的完整信息。
 * 它是反汇编器和调试器显示机器代码的基础数据结构。
 *
 * 使用场景：
 * - 在调试器中显示反汇编代码
 * - 提供机器指令的详细信息
 * - 支持断点和单步执行
 * - 显示指令和注释信息
 *
 * 主要功能：
 * - 存储指令的内存地址范围
 * - 记录指令的机器码（十六进制）
 * - 提供指令的汇编文本表示
 * - 包含指令的注释和函数偏移信息
 *
 * @param range 指令在内存中的地址范围
 * @param hexOpcodes 指令的机器码字节序列
 * @param instruction 指令的汇编文本表示
 * @param comment 指令的注释信息
 * @param functionOffset 指令相对于函数起始地址的偏移量
 */
data class LLInstruction(
    val range: AddressRange,
    val hexOpcodes: List<Byte>,
    val instruction: String,
    val comment: String,
    val functionOffset: LLSymbolOffset?
) {
    companion object {
        /**
         * 从基本组件创建指令对象
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
         * @param functionOffset 指令相对于函数起始地址的偏移量
         * @return 创建的LLInstruction对象
         */
        @JvmStatic
        fun create(
            address: Address,
            opcodesBytes: Iterable<Byte>,
            mnemonic: String,
            operands: String,
            comment: String?,
            functionOffset: LLSymbolOffset?
        ): LLInstruction {
            val opcodesList = ByteList.fromIterable(opcodesBytes)
            val formattedString = "%1$-6s %2\$s".format(mnemonic, operands)
            return create(address, opcodesList, formattedString, comment, functionOffset)
        }

        /**
         * 从预格式化的组件创建指令对象
         *
         * 根据地址、操作码、已格式化的汇编文本等信息创建指令对象。
         * 适用于已经有完整汇编文本的情况。
         *
         * 使用场景：
         * - 从已有的汇编文本创建指令
         * - 调试器保存和恢复指令信息
         * - 处理预解析的汇编代码
         *
         * @param address 指令的起始地址
         * @param opcodes 指令的机器码字节列表
         * @param disassembly 已格式化的汇编文本
         * @param comment 指令的注释信息，可以为null
         * @param functionOffset 指令相对于函数起始地址的偏移量
         * @return 创建的LLInstruction对象
         */
        @JvmStatic
        fun create(
            address: Address,
            opcodes: List<Byte>,
            disassembly: String,
            comment: String?,
            functionOffset: LLSymbolOffset?
        ): LLInstruction {
            val addressRange = address.rangeTo(address + opcodes.size)
            val finalComment = comment ?: ""
            return LLInstruction(addressRange, opcodes, disassembly, finalComment, functionOffset)
        }
    }

    /**
     * 获取指令的起始地址
     *
     * @return 指令的第一个字节的内存地址
     */
    val address: Address get() = range.start

    /**
     * 获取完整的反汇编文本
     *
     * 将指令文本和注释信息组合成完整的显示格式。
     * 如果注释不为空，则在指令文本后添加注释。
     *
     * 格式规则：
     * - 如果有注释：指令文本（33字符宽度对齐） + " ; " + 注释
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
        '\u0000',
    ) + " ; " + comment else instruction) as CharSequence).trim().toString()

    /**
     * 返回指令的字符串表示
     *
     * @return 指令的反汇编文本
     */
    override fun toString(): String {
        return disassembly
    }
}


private fun convertInstructionList(
    instructions: List<Model.Instruction>,
    functionName: String?,
    functionStart: Long
): MutableList<LLInstruction> {

    val result = ArrayList<LLInstruction>(instructions.size)
    instructions.forEach { instruction ->
        val addr = instruction.address
        val functionOffset =
            if (functionName != null) LLSymbolOffset(functionName, addr - functionStart) else null
        val llInstruction = LLInstruction.create(
            Address.fromUnsignedLong(addr),
            instruction.opcodeBytes,
            instruction.mnemonic,
            instruction.operands,
            instruction.comment,
            functionOffset
        )
        result.add(llInstruction)
    }


    return result
}
