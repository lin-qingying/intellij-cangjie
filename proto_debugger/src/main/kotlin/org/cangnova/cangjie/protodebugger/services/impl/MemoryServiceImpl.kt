package org.cangnova.cangjie.protodebugger.services.impl

import org.cangnova.cangjie.protodebugger.data.LLMemoryHunk
import org.cangnova.cangjie.protodebugger.exception.DebuggerCommandException
import org.cangnova.cangjie.protodebugger.memory.Address
import org.cangnova.cangjie.protodebugger.memory.AddressRange
import org.cangnova.cangjie.protodebugger.protocol.ProtobufMessageFactory
import org.cangnova.cangjie.protodebugger.services.MemoryService
import org.cangnova.cangjie.protodebugger.transport.MessageBus
import proto.ProtocolResponses

/**
 * 内存服务实现
 */
class MemoryServiceImpl(
    private val messageBus: MessageBus,
    private val capabilities: Long
) : MemoryService {

    override suspend fun readMemory(range: AddressRange): List<LLMemoryHunk> {
        val request = ProtobufMessageFactory.dumpMemory(
            range.start.unsignedLongValue,
            range.endInclusive.unsignedLongValue + 1L
        )

        val response = messageBus.request(
            request,
            ProtocolResponses.DumpMemoryResponse::class.java
        )

        if (!response.status.success) {
            throw DebuggerCommandException(response.status.errorMessage)
        }

        // DumpMemoryResponse returns a single bytes field, not a list of chunks
        val data = response.data.toByteArray()

        // Verify the data size matches the requested range
        if (data.size.toLong() != range.size) {
            throw DebuggerCommandException("Unable to read memory $range")
        }

        return listOf(LLMemoryHunk(range, data))
    }

    override suspend fun writeMemory(address: Address, bytes: ByteArray) {
        if (!supportsMemoryWrite()) {
            throw UnsupportedOperationException("Memory write is not supported")
        }

        val request = ProtobufMessageFactory.writeMemory(
            address.unsignedLongValue,
            bytes
        )

        val response = messageBus.request(
            request,
            ProtocolResponses.WriteMemoryResponse::class.java
        )

        if (!response.status.success) {
            throw DebuggerCommandException(response.status.errorMessage)
        }
    }

    override fun supportsMemoryWrite(): Boolean {
        return (capabilities and CAPABILITY_MEMORY_WRITE) != 0L
    }

    companion object {
        private const val CAPABILITY_MEMORY_WRITE = 1L shl 4
    }
}
