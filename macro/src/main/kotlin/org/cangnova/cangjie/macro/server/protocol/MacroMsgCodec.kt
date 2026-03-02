/*
 * Copyright 2026 LinQingYing. and contributors.
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
 */

package org.cangnova.cangjie.macro.server.protocol

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * LSPMacroServer FlatBuffers 消息编解码器
 *
 * 根据 `MacroMsgFormat.fbs` Schema 手工实现 FlatBuffers 消息的构建和解析，
 * 无需外部 FlatBuffers 依赖。
 *
 * ## Schema 摘要
 *
 * ```flatbuffers
 * union MsgContent { defLib, multiCalls, macroResult, exitTask }
 * table MacroMsg { content: MsgContent; }
 * table DefLib { paths: [string]; }
 * struct ExitTask { flag: bool; }
 * table MacroResult { id, status: uint8, tks: [Token], diags: [Diagnostic], ... }
 * ```
 *
 * ## FlatBuffers 消息类型常量（union 序号，1-based）
 *
 * | 消息类型 | 值 |
 * |---|---|
 * | DefLib | 1 |
 * | MultiMacroCalls | 2 |
 * | MacroResult | 3 |
 * | ExitTask | 4 |
 */
object MacroMsgCodec {

    // ─── Union 类型常量 ────────────────────────────────────────────────────────
    const val TYPE_DEF_LIB: Byte = 1
    const val TYPE_MULTI_CALLS: Byte = 2
    const val TYPE_MACRO_RESULT: Byte = 3
    const val TYPE_EXIT_TASK: Byte = 4

    // ─── MacroEvalStatus 常量 ─────────────────────────────────────────────────
    const val STATUS_SUCCESS: Byte = 3
    const val STATUS_FAIL: Byte = 4
    const val STATUS_FINISH: Byte = 6

    // ─── 消息构建 ─────────────────────────────────────────────────────────────

    /**
     * 构建 DefLib 消息
     *
     * 通知服务端加载宏动态库，必须在发送 [buildMultiMacroCalls] 之前调用。
     *
     * @param libPaths 宏动态库文件的绝对路径列表
     */
    fun buildDefLib(libPaths: List<String>): ByteArray {
        val b = FlatBufferBuilder()

        // 1. 创建路径字符串（从后向前，因 builder 逆向写入）
        val pathOffsets = libPaths.map { b.createString(it) }

        // 2. 创建 paths 向量
        val pathsVec = b.createVectorOfOffsets(pathOffsets.toIntArray())

        // 3. 创建 DefLib table（1 个字段：paths at field 0）
        b.startTable(1)
        b.addOffsetField(0, pathsVec)
        val defLib = b.endTable()

        // 4. 创建 MacroMsg table（union: content_type at field 0, content at field 1）
        b.startTable(2)
        b.addByteField(0, TYPE_DEF_LIB)
        b.addOffsetField(1, defLib)
        val macroMsg = b.endTable()

        b.finish(macroMsg)
        return b.toByteArray()
    }

    /**
     * 构建 ExitTask 消息，通知服务端正常退出
     *
     * ExitTask 在 Schema 中声明为 `struct { flag: bool }`，
     * 但作为 union 值时以 table 形式传输。
     */
    fun buildExitTask(): ByteArray {
        val b = FlatBufferBuilder()

        // ExitTask 是 struct，在 union 中作为内联 1 字节 bool（flag=true）
        // FlatBuffers struct 在 union 中存储为 table 包装（单字段 bool）
        b.startTable(1)
        b.addByteField(0, 1.toByte()) // flag = true
        val exitTask = b.endTable()

        b.startTable(2)
        b.addByteField(0, TYPE_EXIT_TASK)
        b.addOffsetField(1, exitTask)
        val macroMsg = b.endTable()

        b.finish(macroMsg)
        return b.toByteArray()
    }

    // ─── 消息解析 ─────────────────────────────────────────────────────────────

    /**
     * 解析服务端返回的原始消息，返回消息类型
     */
    fun getMsgType(payload: ByteArray): Byte {
        val buf = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN)
        val reader = FlatBufferReader(buf)
        // MacroMsg root table → field 0 = content_type (byte)
        return reader.readByteField(reader.rootTablePos, 0) ?: 0
    }

    /**
     * 解析 MacroResult 消息
     *
     * @param payload 从管道接收的原始字节（完整 FlatBuffers buffer）
     * @return 解析后的宏展开结果，失败返回 null
     */
    fun parseMacroResult(payload: ByteArray): ParsedMacroResult? {
        return runCatching {
            val buf = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN)
            val reader = FlatBufferReader(buf)

            // MacroMsg.content（field 1）指向 MacroResult table
            val macroResultPos = reader.readOffsetField(reader.rootTablePos, 1) ?: return null

            // MacroResult.status（field 1）= uint8
            val status = reader.readByteField(macroResultPos, 1) ?: STATUS_FAIL

            // MacroResult.tks（field 2）= [Token]，转换为 token 文本列表
            val tokens = reader.readVectorOfTables(macroResultPos, 2)
                .mapNotNull { tokenPos -> parseToken(reader, tokenPos) }

            // MacroResult.diags（field 5）= [Diagnostic]
            val diagnostics = reader.readVectorOfTables(macroResultPos, 5)
                .mapNotNull { diagPos -> parseDiagnostic(reader, diagPos) }

            ParsedMacroResult(
                status = status,
                tokens = tokens,
                diagnostics = diagnostics,
            )
        }.getOrNull()
    }

    private fun parseToken(reader: FlatBufferReader, tokenPos: Int): ParsedToken? {
        // Token: kind(0:uint8), value(1:string), ...
        val kind = reader.readByteField(tokenPos, 0) ?: return null
        val value = reader.readStringField(tokenPos, 1) ?: ""
        return ParsedToken(kind = kind.toInt() and 0xFF, value = value)
    }

    private fun parseDiagnostic(reader: FlatBufferReader, diagPos: Int): ParsedDiagnostic? {
        // Diagnostic: diagSeverity(0:int32), begin(1:Position struct), end(2:Position struct),
        //             errorMessage(3:string), mainHint(4:string)
        val severity = reader.readIntField(diagPos, 0) ?: 2
        val message = reader.readStringField(diagPos, 3) ?: return null
        val hint = reader.readStringField(diagPos, 4) ?: ""
        return ParsedDiagnostic(severity = severity, message = message, hint = hint)
    }

    // ─── 解析结果数据类 ───────────────────────────────────────────────────────

    data class ParsedMacroResult(
        val status: Byte,
        val tokens: List<ParsedToken>,
        val diagnostics: List<ParsedDiagnostic>,
    ) {
        val isSuccess: Boolean get() = status == STATUS_SUCCESS || status == STATUS_FINISH
        val isFailed: Boolean get() = status == STATUS_FAIL

        /** 将 Token 列表重新拼接为展开后的源码文本 */
        fun toExpandedText(): String = tokens.joinToString("") { it.value }
    }

    data class ParsedToken(val kind: Int, val value: String)

    data class ParsedDiagnostic(val severity: Int, val message: String, val hint: String)
}

// ─── 内部 FlatBuffers 工具类 ──────────────────────────────────────────────────

/**
 * 轻量级 FlatBuffers 构建器
 *
 * 实现 FlatBuffers 二进制格式（从末尾向前构建）。
 * 所有偏移量均为相对偏移（从引用位置到目标位置的有符号差值）。
 *
 * 时间复杂度：O(n)，其中 n 为消息字节数。
 */
internal class FlatBufferBuilder(initialCapacity: Int = 256) {
    // buf[head..] 为已写入的有效数据（从末尾向前构建）
    private var buf = ByteArray(initialCapacity)
    private var head = initialCapacity
    private var minAlign = 1

    // 当前 table 的字段信息：voffset → fromEnd（字段写入时的已写字节数）
    private val fieldPositions = HashMap<Int, Int>()
    private var tableStart = 0 // startTable 调用时的 fromEnd

    /** 已写字节数 = final buffer 中，从末尾算起到当前位置 */
    val fromEnd: Int get() = buf.size - head

    /** 扩容，保证 head >= extraNeeded */
    private fun grow(extraNeeded: Int) {
        val dataSize = buf.size - head
        val newSize = maxOf(buf.size * 2, dataSize + extraNeeded + 64)
        val newBuf = ByteArray(newSize)
        buf.copyInto(newBuf, destinationOffset = newSize - dataSize, startIndex = head)
        head = newSize - dataSize
        buf = newBuf
    }

    /** 对齐并预留空间（FlatBuffers 标准对齐算法） */
    private fun prep(alignSize: Int, extraBytes: Int) {
        if (alignSize > minAlign) minAlign = alignSize
        // 计算为使写入后的起始地址对齐 alignSize 需要填充的字节数
        val paddingNeeded = (-(fromEnd + extraBytes)) and (alignSize - 1)
        val totalNeeded = paddingNeeded + extraBytes
        if (head < totalNeeded) grow(totalNeeded)
        repeat(paddingNeeded) { head--; buf[head] = 0 }
    }

    private fun putByte(b: Byte) {
        head--
        buf[head] = b
    }

    private fun putShortLE(s: Int) {
        head -= 2
        buf[head] = (s and 0xFF).toByte()
        buf[head + 1] = (s shr 8 and 0xFF).toByte()
    }

    private fun putIntLE(i: Int) {
        head -= 4
        buf[head] = (i and 0xFF).toByte()
        buf[head + 1] = (i shr 8 and 0xFF).toByte()
        buf[head + 2] = (i shr 16 and 0xFF).toByte()
        buf[head + 3] = (i shr 24 and 0xFF).toByte()
    }

    /**
     * 创建字符串，返回其 fromEnd 偏移量
     *
     * 布局（低地址→高地址）：`[length:int32][bytes][null:byte]`
     */
    fun createString(s: String): Int {
        val bytes = s.toByteArray(Charsets.UTF_8)
        prep(4, bytes.size + 1)
        putByte(0) // null terminator
        if (head < bytes.size) grow(bytes.size)
        for (i in bytes.indices.reversed()) { head--; buf[head] = bytes[i] }
        prep(4, 4)
        putIntLE(bytes.size) // string length
        return fromEnd
    }

    /**
     * 创建偏移量向量（vector of offsets），返回其 fromEnd 偏移量
     *
     * 布局：`[count:int32][offset0:int32][offset1:int32]...`
     */
    fun createVectorOfOffsets(offsets: IntArray): Int {
        prep(4, offsets.size * 4)
        for (i in offsets.indices.reversed()) {
            prep(4, 4)
            // 相对偏移：reference_fromEnd - target_fromEnd
            // reference_fromEnd = fromEnd + 4（写入 4 字节后）
            putIntLE((fromEnd + 4) - offsets[i])
        }
        prep(4, 4)
        putIntLE(offsets.size)
        return fromEnd
    }

    /** 开始构建 table，[numFields] 为字段数量 */
    fun startTable(numFields: Int) {
        fieldPositions.clear()
        tableStart = fromEnd
    }

    /**
     * 添加偏移量字段到当前 table
     *
     * @param slot 字段索引（0-based）
     * @param off 目标对象的 fromEnd 值
     */
    fun addOffsetField(slot: Int, off: Int) {
        prep(4, 4)
        putIntLE((fromEnd + 4) - off) // 相对偏移
        fieldPositions[slot * 2 + 4] = fromEnd // voffset → 写入后的 fromEnd
    }

    /**
     * 添加 byte 字段到当前 table
     *
     * @param slot 字段索引（0-based）
     * @param b 字节值
     */
    fun addByteField(slot: Int, b: Byte) {
        prep(1, 1)
        putByte(b)
        fieldPositions[slot * 2 + 4] = fromEnd
    }

    /**
     * 结束 table 构建，写入 vtable，返回 table 的 fromEnd 值
     *
     * FlatBuffers table 布局（低→高）：
     * `[vtable_size:u16][obj_size:u16][f0_off:u16]...[soffset:i32][field_data...]`
     */
    fun endTable(): Int {
        // 写 soffset 占位符（4 字节），稍后修正
        prep(4, 4)
        putIntLE(0)
        val tableFromEnd = fromEnd // table 对象的 fromEnd（soffset 所在位置）

        // 计算 vtable 中使用的最大 voffset
        val maxVOffset = (fieldPositions.keys.maxOrNull() ?: 2) + 2
        // 写 vtable 字段偏移（从最高 voffset 到最低，即 4 开始）
        for (voff in (maxVOffset - 2) downTo 4 step 2) {
            val fieldFromEnd = fieldPositions[voff]
            if (fieldFromEnd == null) {
                putShortLE(0) // 字段缺失
            } else {
                // 字段相对于 table 起始（soffset 位置）的偏移
                putShortLE(tableFromEnd - fieldFromEnd)
            }
        }
        // vtable_obj_size = table 数据区大小（从 soffset 到最后一个字段）
        putShortLE(tableFromEnd - tableStart)
        // vtable_size = vtable 自身大小（字节数）
        putShortLE(maxVOffset)

        val vtableFromEnd = fromEnd

        // 修正 soffset：vtable_abs - table_abs = tableFromEnd - vtableFromEnd（负值）
        val soffset = tableFromEnd - vtableFromEnd
        val fixupIdx = buf.size - tableFromEnd
        buf[fixupIdx] = (soffset and 0xFF).toByte()
        buf[fixupIdx + 1] = (soffset shr 8 and 0xFF).toByte()
        buf[fixupIdx + 2] = (soffset shr 16 and 0xFF).toByte()
        buf[fixupIdx + 3] = (soffset shr 24 and 0xFF).toByte()

        return tableFromEnd
    }

    /** 完成构建，写入 root 偏移量 */
    fun finish(rootTableFromEnd: Int) {
        prep(minAlign, 0)
        prep(4, 4)
        // root offset = 相对于自身位置的相对偏移 = target_fromEnd - ref_fromEnd
        putIntLE((fromEnd + 4) - rootTableFromEnd)
    }

    /** 返回最终 FlatBuffers buffer 字节数组 */
    fun toByteArray(): ByteArray = buf.copyOfRange(head, buf.size)
}

/**
 * 轻量级 FlatBuffers 读取器
 *
 * 用于解析服务端返回的 FlatBuffers 消息（不依赖生成代码）。
 */
internal class FlatBufferReader(private val buf: ByteBuffer) {

    /** root table 在 buffer 中的绝对位置 */
    val rootTablePos: Int
        get() {
            val rootOffset = buf.getInt(0)
            return rootOffset // root offset 是从位置 0 开始的相对偏移（= 绝对位置）
        }

    /** 获取 table 的 vtable 绝对位置 */
    private fun getVTablePos(tablePos: Int): Int {
        val soffset = buf.getInt(tablePos)
        return tablePos + soffset
    }

    /** 获取字段在 buffer 中的绝对位置（0 表示字段不存在） */
    private fun getFieldAbsPos(tablePos: Int, fieldIndex: Int): Int {
        val vtablePos = getVTablePos(tablePos)
        val vtableSize = buf.getShort(vtablePos).toInt() and 0xFFFF
        val voffset = 4 + fieldIndex * 2
        if (voffset >= vtableSize) return 0
        val fieldRelOffset = buf.getShort(vtablePos + voffset).toInt() and 0xFFFF
        return if (fieldRelOffset == 0) 0 else tablePos + fieldRelOffset
    }

    /** 读取 byte 字段 */
    fun readByteField(tablePos: Int, fieldIndex: Int): Byte? {
        val pos = getFieldAbsPos(tablePos, fieldIndex)
        return if (pos == 0) null else buf.get(pos)
    }

    /** 读取 int32 字段 */
    fun readIntField(tablePos: Int, fieldIndex: Int): Int? {
        val pos = getFieldAbsPos(tablePos, fieldIndex)
        return if (pos == 0) null else buf.getInt(pos)
    }

    /** 读取 offset 字段，返回目标 table 的绝对位置 */
    fun readOffsetField(tablePos: Int, fieldIndex: Int): Int? {
        val pos = getFieldAbsPos(tablePos, fieldIndex)
        if (pos == 0) return null
        val relOffset = buf.getInt(pos)
        return pos + relOffset
    }

    /** 读取字符串字段 */
    fun readStringField(tablePos: Int, fieldIndex: Int): String? {
        val strRefPos = getFieldAbsPos(tablePos, fieldIndex)
        if (strRefPos == 0) return null
        val strRelOffset = buf.getInt(strRefPos)
        val strPos = strRefPos + strRelOffset
        val len = buf.getInt(strPos)
        val bytes = ByteArray(len)
        buf.position(strPos + 4)
        buf.get(bytes)
        return String(bytes, Charsets.UTF_8)
    }

    /** 读取 table 向量，返回各 table 的绝对位置列表 */
    fun readVectorOfTables(tablePos: Int, fieldIndex: Int): List<Int> {
        val vecRefPos = getFieldAbsPos(tablePos, fieldIndex)
        if (vecRefPos == 0) return emptyList()
        val vecRelOffset = buf.getInt(vecRefPos)
        val vecPos = vecRefPos + vecRelOffset
        val count = buf.getInt(vecPos)
        return (0 until count).map { i ->
            val elemRefPos = vecPos + 4 + i * 4
            val elemRelOffset = buf.getInt(elemRefPos)
            elemRefPos + elemRelOffset
        }
    }
}
