package org.cangnova.cangjie.protodebugger.data


import org.cangnova.cangjie.protodebugger.memory.AddressRange
import org.cangnova.cangjie.protodebugger.util.ByteList
import kotlin.jvm.internal.Intrinsics


/**
 * 表示调试器中的内存块数据
 *
 * 该数据类封装了一段连续内存地址范围及其对应的字节数据，用于在调试过程中表示内存内容。
 * 内存块通常用于显示内存视图、内存搜索、变量值存储等场景。
 *
 * @property range 内存地址范围，包含起始地址和结束地址
 * @property bytes 内存块中包含的字节数据列表
 *
 * 使用场景：
 * - 内存调试：查看特定地址范围的内存内容
 * - 变量监控：获取变量在内存中的实际存储值
 * - 内存搜索：在内存中查找特定的字节模式
 * - 缓存管理：缓存从目标进程读取的内存数据
 *
 * 示例用法：
 * ```
 * val memoryRange = AddressRange(0x1000, 0x10FF)
 * val memoryData = byteArrayOf(0x01, 0x02, 0x03, 0x04)
 * val memoryHunk = LLMemoryHunk(memoryRange, memoryData)
 *
 * // 访问内存范围
 * println("内存范围: ${memoryHunk.range}")
 *
 * // 访问字节数据
 * memoryHunk.bytes.forEach { byte ->
 *     println("0x${byte.toString(16).uppercase()}")
 * }
 * ```
 *
 * @see AddressRange 内存地址范围类
 * @see ByteList 字节列表工具类
 */
data class LLMemoryHunk(
    val range: AddressRange,
    val bytes: List<Byte>
) {
    constructor(
        range:  AddressRange,
        bytes: ByteArray
    ) : this(range, ByteList(bytes) as List<Byte>)


    override operator fun equals(other: Any?): Boolean {
        return if (this === other) {
            true
        } else if (other !is LLMemoryHunk) {
            false
        } else {
            val (range1, bytes1) = other
            if (!Intrinsics.areEqual(range, range1)) {
                false
            } else {
                Intrinsics.areEqual(bytes, bytes1)
            }
        }
    }


    override fun toString(): String {
        return "LLMemoryHunk(range=$range, bytes=$bytes)"

    }

    override fun hashCode(): Int {
        var result = range.hashCode()
        result = 31 * result + bytes.hashCode()
        return result
    }
}

