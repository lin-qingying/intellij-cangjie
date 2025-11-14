package org.cangnova.cangjie.protodebugger.services

import org.cangnova.cangjie.protodebugger.data.LLMemoryHunk
import org.cangnova.cangjie.protodebugger.memory.Address
import org.cangnova.cangjie.protodebugger.memory.AddressRange

/**
 * 内存服务接口
 *
 * 负责内存读写和内存区域管理
 */
interface MemoryService {
    /**
     * 读取内存区域
     *
     * @param range 地址范围
     * @return 内存块列表
     */
    suspend fun readMemory(range: AddressRange): List<LLMemoryHunk>

    /**
     * 写入内存
     *
     * @param address 起始地址
     * @param bytes 要写入的字节数组
     */
    suspend fun writeMemory(address: Address, bytes: ByteArray)

    /**
     * 是否支持内存写入
     */
    fun supportsMemoryWrite(): Boolean
}
