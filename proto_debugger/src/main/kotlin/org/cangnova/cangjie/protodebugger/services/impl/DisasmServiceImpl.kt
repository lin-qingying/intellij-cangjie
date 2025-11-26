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

package org.cangnova.cangjie.protodebugger.services.impl

import com.intellij.openapi.diagnostic.Logger
import com.intellij.xdebugger.XDebugSession
import lldbprotobuf.ResponseOuterClass
import org.cangnova.cangjie.protodebugger.data.*
import org.cangnova.cangjie.protodebugger.exception.DebuggerCommandExceptionException
import org.cangnova.cangjie.protodebugger.memory.Address
import org.cangnova.cangjie.protodebugger.memory.AddressRange
import org.cangnova.cangjie.protodebugger.protocol.ProtobufFactory
import org.cangnova.cangjie.protodebugger.services.DisasmService
import org.cangnova.cangjie.protodebugger.transport.MessageBus

/**
 * 反汇编服务实现
 */
class DisasmServiceImpl(
    private val messageBus: MessageBus,
    private val capabilities: Long,
    private val debugSession: XDebugSession
) : DisasmService {

    companion object {
        private const val CAPABILITY_REGISTERS = 1L shl 5

        private val LOG = Logger.getInstance(DisasmServiceImpl::class.java)
    }

    override suspend fun disassembleFunction(
        address: Address,
    ): List<LLDBDisasmInstruction> {
        LOG.debug("Disassembling function at address: 0x${address.asLong.toString(16)}")

        // 步骤1: 获取函数信息
        val functionInfo = fetchFunctionInfo(address)
        if (functionInfo == null) {
            LOG.debug("No function info available, using anchor-based disassembly")
            return disassembleByAnchor(address, backwardCount = 100, forwardCount = 100)
        }

        // 步骤2: 计算函数地址范围
        val functionRange = calculateFunctionRange(functionInfo)
        LOG.debug(
            "Function info: name=${functionInfo.name}, " +
            "range=0x${functionRange.start.asLong.toString(16)}..0x${functionRange.endInclusive.asLong.toString(16)}, " +
            "size=${functionRange.size} bytes"
        )

        // 步骤3: 验证目标地址在函数范围内
        if (!functionRange.contains(address)) {
            LOG.warn("Address 0x${address.asLong.toString(16)} is outside function range, using anchor-based disassembly")
            return disassembleByAnchor(address, backwardCount = 100, forwardCount = 100)
        }

        // 步骤4: 处理边缘情况
        if (functionRange.size == 1UL) {
            LOG.debug("Single-instruction function, using anchor-based disassembly")
            return disassembleByAnchor(address, backwardCount = 50, forwardCount = 50)
        }

        // 步骤5: 限制过大函数的反汇编范围
        val effectiveRange = limitRangeIfTooLarge(functionRange, address, maxSize = 65536UL)

        // 步骤6: 选择合适的反汇编策略
        return when {
            effectiveRange.start == address -> {
                // 情况A: 从范围起始处反汇编（自然对齐）
                LOG.debug("Target is at range start, using simple range-based disassembly")
                disassembleRange(effectiveRange)
            }
            else -> {
                // 情况B: 从范围中间反汇编（需要确保指令对齐）
                LOG.debug("Target is mid-range, using alignment-aware disassembly")
                disassembleByRangeWithAlignment(effectiveRange, pivotAddress = address)
            }
        }
    }

    /**
     * 获取函数信息
     *
     * @param address 目标地址
     * @return 函数信息，如果无法获取则返回 null
     */
    private suspend fun fetchFunctionInfo(address: Address): lldbprotobuf.Model.FunctionInfo? {
        return try {
            val request = ProtobufFactory.getFunctionInfoByAddress(address.asLong)
            val response = messageBus.request(
                request,
                ResponseOuterClass.GetFunctionInfoResponse::class.java
            )

            if (response.status.success && response.hasFunction()) {
                response.function
            } else {
                null
            }
        } catch (e: Exception) {
            LOG.debug("Failed to get function info for address $address", e)
            null
        }
    }

    /**
     * 计算函数的地址范围
     *
     * @param functionInfo 函数信息
     * @return 函数的闭区间地址范围
     */
    private fun calculateFunctionRange(functionInfo: lldbprotobuf.Model.FunctionInfo): AddressRange {
        val startAddr = Address.Companion.Factory.fromLong(functionInfo.startAddress)

        // 计算结束地址
        val endAddr = if (functionInfo.endAddress > functionInfo.startAddress) {
            // 使用显式的结束地址
            Address.Companion.Factory.fromLong(functionInfo.endAddress)
        } else {
            // 使用函数大小计算结束地址
            startAddr + functionInfo.size.toLong()
        }

        // 创建闭区间范围
        // endAddr 通常指向函数结束后的第一个字节，所以减1得到函数最后一个字节
        return startAddr.rangeTo(endAddr - 1)
    }

    /**
     * 限制过大范围的大小
     *
     * 对于超大函数，以目标地址为中心限制反汇编范围，避免性能问题
     *
     * @param range 原始范围
     * @param targetAddress 目标地址
     * @param maxSize 最大允许大小（字节）
     * @return 限制后的范围
     */
    private fun limitRangeIfTooLarge(
        range: AddressRange,
        targetAddress: Address,
        maxSize: ULong
    ): AddressRange {
        if (range.size <= maxSize) {
            return range
        }

        LOG.debug("Range too large (${range.size} bytes), limiting to $maxSize bytes around target address")

        // 以目标地址为中心，取 maxSize 大小的窗口
        val halfSize = (maxSize / 2UL).toLong()
        val limitedStart = (targetAddress - halfSize).coerceAtLeast(range.start)
        val limitedEnd = (targetAddress + halfSize).coerceAtMost(range.endInclusive)

        return limitedStart.rangeTo(limitedEnd)
    }

    /**
     * 基于锚点的反汇编
     *
     * 当无法获取函数边界信息时，以指定地址为锚点，
     * 向前和向后反汇编指定数量的指令。
     *
     * @param anchorAddress 锚点地址
     * @param backwardCount 向前反汇编的指令数量
     * @param forwardCount 向后反汇编的指令数量
     * @return 反汇编指令列表
     */
    private suspend fun disassembleByAnchor(
        anchorAddress: Address,
        backwardCount: Int,
        forwardCount: Int
    ): List<LLDBDisasmInstruction> {
        LOG.debug(
            "Anchor-based disassembly: anchor=0x${anchorAddress.asLong.toString(16)}, " +
            "backward=$backwardCount, forward=$forwardCount"
        )

        val options = buildDisassembleOptions()
        val request = ProtobufFactory.disassembleAnchor(
            anchorAddress = anchorAddress.asLong,
            backwardCount = backwardCount,
            forwardCount = forwardCount,
            options = options
        )

        val response = messageBus.request(
            request,
            ResponseOuterClass.DisassembleResponse::class.java
        )

        if (!response.status.success) {
            throw DebuggerCommandExceptionException("Anchor-based disassembly failed: ${response.status.message}")
        }

        LOG.debug("Disassembled ${response.instructionsCount} instructions using anchor mode")
        return response.instructionsList.map { it.toLLDBInstruction() }
    }

    /**
     * 基于地址范围的反汇编
     *
     * 从范围起始地址顺序反汇编到结束地址，
     * 适用于从函数开头反汇编的场景（自然对齐）。
     *
     * @param range 地址范围
     * @return 反汇编指令列表
     */
      override suspend fun disassembleRange(
        range: AddressRange
    ): List<LLDBDisasmInstruction> {
        val start = range.start.asLong
        val end = range.endInclusive.asLong

        LOG.debug("Range-based disassembly: 0x${start.toString(16)}..0x${end.toString(16)}")

        val options = buildDisassembleOptions()
        val request = ProtobufFactory.disassembleRange(start, end, options)
        val response = messageBus.request(
            request,
            ResponseOuterClass.DisassembleResponse::class.java
        )

        if (!response.status.success) {
            throw DebuggerCommandExceptionException("Range-based disassembly failed: ${response.status.message}")
        }

        LOG.debug("Disassembled ${response.instructionsCount} instructions")
        return response.instructionsList.map { it.toLLDBInstruction() }
    }

    /**
     * 带对齐保证的范围反汇编
     *
     * 当目标地址位于范围中间时，为确保指令边界对齐：
     * 1. 从 pivot 地址开始反汇编到范围结束（pivot 自然对齐）
     * 2. 从范围起始反汇编到 pivot（使用对齐验证）
     * 3. 合并两部分结果
     *
     * @param range 反汇编范围
     * @param pivotAddress 对齐锚点地址（范围内的目标地址）
     * @return 反汇编指令列表
     */
    private suspend fun disassembleByRangeWithAlignment(
        range: AddressRange,
        pivotAddress: Address
    ): List<LLDBDisasmInstruction> {
        require(range.contains(pivotAddress)) {
            "Pivot address 0x${pivotAddress.asLong.toString(16)} is outside range"
        }

        LOG.debug(
            "Alignment-aware disassembly: " +
            "range=[0x${range.start.asLong.toString(16)}..0x${range.endInclusive.asLong.toString(16)}], " +
            "pivot=0x${pivotAddress.asLong.toString(16)}"
        )

        // 步骤1: 从 pivot 到范围结束（pivot 作为起始点，自然对齐）
        val forwardRange = pivotAddress.rangeTo(range.endInclusive)
        val forwardInstructions = disassembleRange(forwardRange)

        if (forwardInstructions.isEmpty()) {
            LOG.warn("No instructions found from pivot to range end")
            return emptyList()
        }

        // 步骤2: 从范围起始到 pivot（带对齐验证）
        val pivotInstruction = forwardInstructions.first()
        val backwardInstructions = disassembleBackwardToAlignedInstruction(
            startAddress = range.start,
            alignedInstruction = pivotInstruction
        )

        // 步骤3: 合并结果
        val totalCount = backwardInstructions.size + forwardInstructions.size
        LOG.debug(
            "Alignment completed: ${backwardInstructions.size} instructions before pivot + " +
            "${forwardInstructions.size} from pivot = $totalCount total"
        )

        return backwardInstructions + forwardInstructions
    }

    /**
     * 反汇编到已对齐的指令
     *
     * 从起始地址反汇编，直到到达指定的已对齐指令位置。
     * 该方法会验证反汇编结果是否正确对齐到目标指令。
     *
     * @param startAddress 起始地址
     * @param alignedInstruction 已对齐的目标指令（作为对齐验证点）
     * @return 反汇编指令列表（不包含 alignedInstruction）
     */
    private suspend fun disassembleBackwardToAlignedInstruction(
        startAddress: Address,
        alignedInstruction: LLDBDisasmInstruction
    ): List<LLDBDisasmInstruction> {
        val startAddr = startAddress.asLong
        val targetAddr = alignedInstruction.address.asLong

        LOG.debug(
            "Backward disassembly to aligned instruction: " +
            "start=0x${startAddr.toString(16)}, target=0x${targetAddr.toString(16)}"
        )

        val options = buildDisassembleOptions()
        val request = ProtobufFactory.disassembleUntilPivot(
            startAddress = startAddr,
            pivotAddress = targetAddr,
            includePivot = false, // 不包含目标指令，因为它已在 forward 部分
            options = options
        )

        val response = messageBus.request(
            request,
            ResponseOuterClass.DisassembleResponse::class.java
        )

        if (!response.status.success) {
            throw DebuggerCommandExceptionException(
                "Backward disassembly to aligned instruction failed: ${response.status.message}"
            )
        }

        // 验证对齐结果
        if (!response.alignmentVerified) {
            LOG.warn(
                "Instruction alignment verification failed. " +
                "Expected to reach 0x${targetAddr.toString(16)}, " +
                "but actually reached 0x${response.actualEndAddress.toString(16)}"
            )
        }

        LOG.debug("Disassembled ${response.instructionsCount} instructions backward to aligned instruction")
        return response.instructionsList.map { it.toLLDBInstruction() }
    }

    /**
     * 构建反汇编选项
     */
    private fun buildDisassembleOptions(): lldbprotobuf.Model.DisassembleOptions {
        return lldbprotobuf.Model.DisassembleOptions.newBuilder().apply {
            showMachineCode = true
            symbolizeAddresses = true
            // disasmFlavor 相关的字段需要根据 proto 定义设置
        }.build()
    }


    override suspend fun getRegisters(
        thread: LLDBThread,
        frame: LLDBFrame
    ): List<LLDBRegister> {
        return getRegisters(thread, frame, emptySet())
    }

    override suspend fun getRegisters(
        thread: LLDBThread,
        frame: LLDBFrame,
        registerNames: Set<String>
    ): List<LLDBRegister> {


        val request = ProtobufFactory.getRegisters(thread.id, frame.index, registerNames)

        val response = messageBus.request(
            request,
              ResponseOuterClass.RegistersResponse::class.java
        )

        if (!response.status.success) {
            throw DebuggerCommandExceptionException(response.status.message)
        }

        return response.registersList.map { reg ->
            createLLDBRegister(reg)
        }
    }



    override suspend fun getRegisterGroups(
        thread: LLDBThread,
        frame: LLDBFrame
    ): List<LLDBRegisterGroup> {
        val request = ProtobufFactory.getRegisterGroups(thread.id, frame.index)
        val response = messageBus.request(
            request,
            ResponseOuterClass.RegisterGroupsResponse::class.java
        )

        if (!response.status.success) {
            throw DebuggerCommandExceptionException(response.status.message)
        }

        return response.groupsList.map { group ->
            LLDBRegisterGroup(
                name = group.name,
                registerCount = group.registerCount.toInt()
            )
        }
    }

    override fun supportsRegisters(): Boolean {
        return (capabilities and CAPABILITY_REGISTERS) != 0L
    }
}
