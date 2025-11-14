package org.cangnova.cangjie.protodebugger.services

import org.cangnova.cangjie.protodebugger.data.LLInstruction
import org.cangnova.cangjie.protodebugger.data.LLRegisterSet
import org.cangnova.cangjie.protodebugger.data.LLValue
import org.cangnova.cangjie.protodebugger.data.LLFrame
import org.cangnova.cangjie.protodebugger.data.LLThread
import org.cangnova.cangjie.protodebugger.memory.Address
import org.cangnova.cangjie.protodebugger.memory.AddressRange
import org.cangnova.cangjie.protodebugger.settings.DisasmFlavor

/**
 * 反汇编服务接口
 *
 * 负责反汇编、寄存器访问和架构信息
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
        fallbackRange: AddressRange
    ): List<LLInstruction>

    /**
     * 获取寄存器值
     *
     * @param thread 目标线程
     * @param frame 目标栈帧
     * @return 寄存器值列表
     */
    suspend fun getRegisters(
        thread: LLThread,
        frame: LLFrame
    ): List<LLValue>

    /**
     * 获取指定寄存器的值
     *
     * @param thread 目标线程
     * @param frame 目标栈帧
     * @param registerNames 寄存器名称集合
     * @return 寄存器值列表
     */
    suspend fun getRegisters(
        thread: LLThread,
        frame: LLFrame,
        registerNames: Set<String>
    ): List<LLValue>

    /**
     * 获取寄存器集合
     *
     * @return 寄存器集合列表
     */
    suspend fun getRegisterSets(): List<LLRegisterSet>

    /**
     * 获取目标架构
     *
     * @return 架构字符串
     */
    suspend fun getArchitecture(): String?

    /**
     * 从表达式中提取寄存器名
     *
     * @param expression 表达式
     * @return 寄存器名，如果不是寄存器表达式则返回null
     */
    fun extractRegisterName(expression: String): String?

    /**
     * 获取反汇编风格
     *
     * @return 反汇编风格
     */
    fun getDisasmFlavor(): DisasmFlavor?

    /**
     * 设置反汇编风格
     *
     * @param flavor 反汇编风格
     */
    suspend fun setDisasmFlavor(flavor: DisasmFlavor)

    /**
     * 是否支持寄存器访问
     */
    fun supportsRegisters(): Boolean


}
