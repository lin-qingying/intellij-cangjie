package org.cangnova.cangjie.protodebugger.services.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import lldbprotobuf.ResponseOuterClass
import org.cangnova.cangjie.protodebugger.exception.DebuggerCommandException
import org.cangnova.cangjie.protodebugger.memory.*
import org.cangnova.cangjie.protodebugger.protocol.ProtobufFactory
import org.cangnova.cangjie.protodebugger.services.MemoryService
import org.cangnova.cangjie.protodebugger.services.Watchpoint
import org.cangnova.cangjie.protodebugger.services.WatchpointAccess
import org.cangnova.cangjie.protodebugger.transport.MessageBus

/**
 * 内存服务实现
 *
 * 实现 MemoryDataSource 接口以集成到内存视图框架。
 * 负责与调试器通信以读写内存数据。
 */
class MemoryServiceImpl(
    private val messageBus: MessageBus,
    private val capabilities: Long
) : MemoryService {

    // 内存变化事件流
    private val memoryChanges = MutableSharedFlow<AddressRange>(replay = 0, extraBufferCapacity = 64)



    /**
     * 读取内存范围
     *
     * 实现 MemoryDataSource 接口，返回 MemoryCell 列表。
     */
    override suspend fun readMemory(range: AddressRange): List<MemoryCell> {
        val request = ProtobufFactory.readMemory(
            range.start.asLong,
            range.size.toInt()
        )

        val response = messageBus.request(request, ResponseOuterClass.ReadMemoryResponse::class.java)

        if (!response.status.success) {

            throw DebuggerCommandException("Unable to read memory $range: ${response.status.message}")
        }

        val data = response.data.toByteArray()

        // 验证数据大小
        if (data.size.toLong() != range.size.toLong()) {
            throw DebuggerCommandException("Unable to read memory $range: expected ${range.size} bytes, got ${data.size}")
        }

        // 转换为 MemoryCell 列表
        return convertBytesToCells(range.start, data)
    }



    /**
     * 写入单个字节
     */
    override suspend fun writeMemory(address: Address, value: Byte) {
        writeMemory(address, byteArrayOf(value))
    }

    /**
     * 写入字节数组
     */
    override suspend fun writeMemory(address: Address, bytes: ByteArray) {
        if (!supportsMemoryWrite()) {
            throw UnsupportedOperationException("Memory write is not supported")
        }

        // 将字节数组转换为十六进制字符串
        val hexString = bytes.joinToString("") { byte -> "%02x".format(byte) }

        val request = ProtobufFactory.writeMemory(
            address.asLong,
            hexString
        )

        val response = messageBus.request(request, lldbprotobuf.ResponseOuterClass.WriteMemoryResponse::class.java)

        if (!response.status.success) {
            throw DebuggerCommandException("Failed to write memory: no response data")
        }

        // 发出内存变化事件
        val range = address.rangeTo(address + bytes.size - 1)
        memoryChanges.emit(range)
    }

    /**
     * 批量写入内存
     */
    override suspend fun writeMemory(updates: Map<Address, Byte>) {
        if (updates.isEmpty()) return

        // 按地址排序
        val sorted = updates.toSortedMap(compareBy { it.value })

        // 合并连续区域以优化写入
        val ranges = mergeConsecutiveAddresses(sorted)

        // 批量写入
        for ((start, bytes) in ranges) {
            writeMemory(start, bytes)
        }
    }

    /**
     * 是否支持内存写入
     */
    override fun supportsMemoryWrite(): Boolean {
        return (capabilities and CAPABILITY_MEMORY_WRITE) != 0L
    }



    /**
     * 搜索内存
     */
    override suspend fun searchMemory(pattern: ByteArray, range: AddressRange): List<Address> {
        // TODO: 实现内存搜索
        // 可以选择调用调试器的搜索命令，或者读取内存后在本地搜索
        throw UnsupportedOperationException("Memory search not yet implemented")
    }

    /**
     * 将字节数组转换为 MemoryCell 列表
     */
    private fun convertBytesToCells(startAddress: Address, data: ByteArray): List<MemoryCell> {
        val currentTime = System.currentTimeMillis()
        return data.mapIndexed { index, byte ->
            MemoryCell.DataCell(
                address = startAddress + index,
                value = byte,
                version = currentTime,
                state = CellState.LOADED
            )
        }
    }

    /**
     * 合并连续地址以优化批量写入
     */
    private fun mergeConsecutiveAddresses(updates: Map<Address, Byte>): Map<Address, ByteArray> {
        val result = mutableMapOf<Address, ByteArray>()
        val sorted = updates.toList().sortedBy { it.first.value }

        if (sorted.isEmpty()) return result

        var currentStart = sorted[0].first
        val currentBytes = mutableListOf(sorted[0].second)

        for (i in 1 until sorted.size) {
            val (addr, byte) = sorted[i]
            val expectedNext = currentStart + currentBytes.size

            if (addr == expectedNext) {
                // 连续地址，添加到当前序列
                currentBytes.add(byte)
            } else {
                // 不连续，保存当前序列并开始新序列
                result[currentStart] = currentBytes.toByteArray()
                currentStart = addr
                currentBytes.clear()
                currentBytes.add(byte)
            }
        }

        // 保存最后一个序列
        result[currentStart] = currentBytes.toByteArray()

        return result
    }

    /**
     * 发出内存变化事件（供调试器事件监听器调用）
     */
    fun emitMemoryChange(range: AddressRange) {
        // 使用 tryEmit 避免阻塞
        memoryChanges.tryEmit(range)
    }

    companion object {
        private const val CAPABILITY_MEMORY_WRITE = 1L shl 4
    }
}
