package org.cangnova.cangjie.protodebugger.services.impl

import com.intellij.openapi.util.Ref
import org.cangnova.cangjie.protodebugger.data.LLFrame
import org.cangnova.cangjie.protodebugger.data.LLInstruction
import org.cangnova.cangjie.protodebugger.data.LLRegisterSet
import org.cangnova.cangjie.protodebugger.data.LLThread
import org.cangnova.cangjie.protodebugger.data.LLValue
import org.cangnova.cangjie.protodebugger.exception.DebuggerCommandException
import org.cangnova.cangjie.protodebugger.memory.Address
import org.cangnova.cangjie.protodebugger.memory.AddressRange
import org.cangnova.cangjie.protodebugger.memory.endCoerced
import org.cangnova.cangjie.protodebugger.protocol.ProtobufMessageFactory
import org.cangnova.cangjie.protodebugger.services.DisasmService
import org.cangnova.cangjie.protodebugger.settings.DisasmFlavor
import org.cangnova.cangjie.protodebugger.settings.DisasmOptions
import org.cangnova.cangjie.protodebugger.transport.MessageBus
import proto.Model
import proto.ProtocolResponses
import java.util.concurrent.atomic.AtomicReference

/**
 * 反汇编服务实现
 */
class DisasmServiceImpl(
    private val messageBus: MessageBus,
    private val capabilities: Long
) : DisasmService {

    private val cachedArchInfo = AtomicReference<ArchInfo>()

    @Volatile
    private var disasmFlavor: DisasmFlavor? = null

    override suspend fun disassembleFunction(
        address: Address,
        fallbackRange: AddressRange
    ): List<LLInstruction> {
        val request = ProtobufMessageFactory.disassemble(
            fallbackRange.start.unsignedLongValue,
            fallbackRange.endCoerced.unsignedLongValue
        )

        val response = messageBus.request(
            request,
            ProtocolResponses.DisassembleResponse::class.java
        )

        if (!response.status.success) {
            throw DebuggerCommandException(response.status.errorMessage)
        }

        return response.instructionsList.map { inst ->
            convertInstruction(inst, null, fallbackRange.start.unsignedLongValue)
        }
    }

    private fun convertInstruction(
        instruction: Model.Instruction,
        functionName: String?,
        functionStart: Long
    ): LLInstruction {
        val addr = instruction.address
        val functionOffset = if (functionName != null) {
            org.cangnova.cangjie.protodebugger.data.LLSymbolOffset(functionName, addr - functionStart)
        } else null

        return LLInstruction.create(
            Address.fromUnsignedLong(addr),
            instruction.opcodeBytes,
            instruction.mnemonic,
            instruction.operands,
            instruction.comment,
            functionOffset
        )
    }

    override suspend fun getRegisters(
        thread: LLThread,
        frame: LLFrame
    ): List<LLValue> {
        return getRegisters(thread, frame, emptySet())
    }

    override suspend fun getRegisters(
        thread: LLThread,
        frame: LLFrame,
        registerNames: Set<String>
    ): List<LLValue> {
        return emptyList()

//        val request = ProtobufMessageFactory.getRegisters(thread.id, frame.index, registerNames)
//
//        val response = messageBus.request(
//            request,
//            ProtocolResponses.GetRegistersResponse::class.java
//        )
//
//        if (!response.status.success) {
//            throw DebuggerCommandException(response.status.errorMessage)
//        }
//
//        return response.registersList.map { reg ->
//            org.cangnova.cangjie.protodebugger.data.createLLValue(reg, null,::loadValueDataForRegister)
//        }
    }

    private fun loadValueDataForRegister(value: org.cangnova.cangjie.protodebugger.data.LLValue): org.cangnova.cangjie.protodebugger.data.LLValueData {
        // 寄存器值通常直接从响应中获取，不需要额外加载
        // 返回一个简单的 LLValueData
        return org.cangnova.cangjie.protodebugger.data.LLValueData(
            value.name,
            null,
            false,
            false,
            false
        )
    }

    override suspend fun getRegisterSets(): List<LLRegisterSet> {
        val archInfo = getArchInfo()
        return archInfo.registerSets
    }

    override suspend fun getArchitecture(): String {
        val archInfo = getArchInfo()
        return archInfo.architecture
    }

    override fun extractRegisterName(expression: String): String? {
        val trimmed = expression.trim()
        if (trimmed.startsWith("$")) {
            return trimmed.substring(1)
        }
        return null
    }

    override fun getDisasmFlavor(): DisasmFlavor? {
        return disasmFlavor
    }

    override suspend fun setDisasmFlavor(flavor: DisasmFlavor) {
        disasmFlavor = flavor
        // TODO: 协议中暂时没有 setDisasmFlavor 方法
        // 当前只在本地缓存 flavor 设置
    }

    override fun supportsRegisters(): Boolean {
        return (capabilities and CAPABILITY_REGISTERS) != 0L
    }



    private suspend fun getArchInfo(): ArchInfo {
        var info = cachedArchInfo.get()
        if (info == null) {
            info = computeArchInfo()
            cachedArchInfo.set(info)
        }
        return info
    }

    private suspend fun computeArchInfo(): ArchInfo {
        val architecture = computeArchitecture()
        val registerSets = computeRegisterSets()
        return ArchInfo(architecture, registerSets)
    }

    private suspend fun computeArchitecture(): String {
        val request = ProtobufMessageFactory.getArch()
        val response = messageBus.request(
            request,
            ProtocolResponses.GetArchitectureResponse::class.java
        )

        if (!response.status.success) {
            throw DebuggerCommandException(response.status.errorMessage)
        }

        return response.architecture
    }

    private suspend fun computeRegisterSets(): List<LLRegisterSet> {
        val request = ProtobufMessageFactory.getRegisterSets()
        val response = messageBus.request(
            request,
            ProtocolResponses.GetRegisterSetsResponse::class.java
        )

        if (!response.status.success) {
            throw DebuggerCommandException(response.status.errorMessage)
        }

        return response.registerSetsList.map { set ->
            LLRegisterSet(
                set.name,
                set.registerNamesList
            )
        }
    }



    private data class ArchInfo(
        val architecture: String,
        val registerSets: List<LLRegisterSet>
    )

    companion object {
        private const val CAPABILITY_REGISTERS = 1L shl 5
    }
}
