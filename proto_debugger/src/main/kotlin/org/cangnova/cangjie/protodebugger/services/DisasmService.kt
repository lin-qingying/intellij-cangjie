package org.cangnova.cangjie.protodebugger.services

import com.intellij.xdebugger.XSourcePosition
import org.cangnova.cangjie.protodebugger.data.LLDBDisasmInstruction
import org.cangnova.cangjie.protodebugger.data.LLDBVariable
import org.cangnova.cangjie.protodebugger.data.LLDBFrame
import org.cangnova.cangjie.protodebugger.data.LLDBRegister
import org.cangnova.cangjie.protodebugger.data.LLDBThread
import org.cangnova.cangjie.protodebugger.data.LLDBRegisterGroup

import org.cangnova.cangjie.protodebugger.memory.Address
import org.cangnova.cangjie.protodebugger.memory.AddressRange
import org.cangnova.cangjie.protodebugger.settings.DisasmFlavor

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
