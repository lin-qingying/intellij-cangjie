/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package org.cangnova.cangjie.macro.server.protocol

import MacroMsgFormat.*
import com.google.flatbuffers.FlatBufferBuilder
import java.nio.ByteBuffer
import java.nio.ByteOrder

object MacroMsgCodec {

    const val TYPE_DEF_LIB: UByte = 1u
    const val TYPE_MULTI_CALLS: UByte = 2u
    const val TYPE_MACRO_RESULT: UByte = 3u
    const val TYPE_EXIT_TASK: UByte = 4u

    // MacroEvalStatus
    const val STATUS_SUCCESS: UByte = 3u
    const val STATUS_FAIL: UByte = 4u
    const val STATUS_FINISH: UByte = 6u

    // ─── 消息构建 ─────────────────────────────────────────────────────────────

    fun buildDefLib(libPaths: List<String>): ByteArray {
        val b = FlatBufferBuilder(256)
        val pathOffsets = libPaths.map { b.createString(it) }.toIntArray()
        val pathsVec = DefLib.createPathsVector(b, pathOffsets)
        val defLib = DefLib.createDefLib(b, pathsVec)
        val msg = MacroMsg.createMacroMsg(b, TYPE_DEF_LIB, defLib)
        b.finish(msg)
        return b.sizedByteArray()
    }

    /** ExitTask(flag=true)：终止服务端进程 */
    fun buildExitTask(): ByteArray {
        val b = FlatBufferBuilder(64)
        val exitTask = ExitTask.createExitTask(b, true)
        val msg = MacroMsg.createMacroMsg(b, TYPE_EXIT_TASK, exitTask)
        b.finish(msg)
        return b.sizedByteArray()
    }

    /** ExitTask(flag=false)：重置服务端状态（清空宏声明/调用/诊断），进程继续运行 */
    fun buildResetStageTask(): ByteArray {
        val b = FlatBufferBuilder(64)
        val exitTask = ExitTask.createExitTask(b, false)
        val msg = MacroMsg.createMacroMsg(b, TYPE_EXIT_TASK, exitTask)
        b.finish(msg)
        return b.sizedByteArray()
    }
    fun buildMultiMacroCalls(calls: List<MacroCallInfo>): ByteArray {
        val b = FlatBufferBuilder(1024)

        val callOffsets = calls.map { call ->
            // ① 所有字符串/向量必须在 startXxx 之前创建
            val libPathOff    = b.createString(call.libPath)
            val pkgNameOff    = b.createString(call.packageName)
            val methodNameOff = b.createString(call.methodName)
            val idNameOff     = b.createString(call.idName)

            // ② parentNames vector
            val parentOffsets = call.parentNames.map { b.createString(it) }.toIntArray()
            val parentNamesVec = MacroCall.createParentNamesVector(b, parentOffsets)

            // ③ args tokens
            val argOffsets = call.args.map { buildToken(b, it) }.toIntArray()
            val argsVec = MacroCall.createArgsVector(b, argOffsets)

            // ④ attrs tokens
            val attrOffsets = call.attrs.map { buildToken(b, it) }.toIntArray()
            val attrsVec = MacroCall.createAttrsVector(b, attrOffsets)

            // ⑤ childMsges 空向量
            // C++ DeSerializeChildMsgesFromCall 直接调用 callFmt.childMsges()->size()，
            // 若字段缺失则 childMsges() 返回 nullptr，导致空指针崩溃 → 管道关闭 → EOFException。
            // IDE 侧不产生 childMsges，固定传空向量保持协议兼容性。
            val childMsgesVec = MacroCall.createChildMsgesVector(b, IntArray(0))

            // ⑥ IdInfo
            IdInfo.startIdInfo(b)
            IdInfo.addName(b, idNameOff)
            IdInfo.addPos(b, Position.createPosition(
                b,
                call.idPos.fileId.toUInt(),
                call.idPos.line,
                call.idPos.column
            ))
            val idInfo = IdInfo.endIdInfo(b)

            // ⑦ MacroCall
            MacroCall.startMacroCall(b)
            MacroCall.addId(b, idInfo)
            MacroCall.addHasAttrs(b, call.hasAttrs)
            MacroCall.addArgs(b, argsVec)
            MacroCall.addAttrs(b, attrsVec)
            MacroCall.addParentNames(b, parentNamesVec)
            MacroCall.addChildMsges(b, childMsgesVec)
            MacroCall.addMethodName(b, methodNameOff)
            MacroCall.addPackageName(b, pkgNameOff)
            MacroCall.addLibPath(b, libPathOff)
            MacroCall.addBegin(b, Position.createPosition(
                b,
                call.begin.fileId.toUInt(),
                call.begin.line,
                call.begin.column
            ))
            MacroCall.addEnd(b, Position.createPosition(
                b,
                call.end.fileId.toUInt(),
                call.end.line,
                call.end.column
            ))
            MacroCall.endMacroCall(b)
        }.toIntArray()

        val callsVec = MultiMacroCalls.createCallsVector(b, callOffsets)
        val multiCalls = MultiMacroCalls.createMultiMacroCalls(b, callsVec)
        val msg = MacroMsg.createMacroMsg(b, TYPE_MULTI_CALLS, multiCalls)
        b.finish(msg)
        return b.sizedByteArray()
    }

    private fun buildToken(b: FlatBufferBuilder, tok: TokenInfo): Int {
        val valOff = b.createString(tok.value)
        Token.startToken(b)
        Token.addKind(b, tok.kind)
        Token.addValue(b, valOff)
        Token.addBegin(b, Position.createPosition(
            b,
            tok.begin.fileId.toUInt(),
            tok.begin.line,
            tok.begin.column
        ))
        Token.addEnd(b, Position.createPosition(
            b,
            tok.end.fileId.toUInt(),
            tok.end.line,
            tok.end.column
        ))
        Token.addDelimiterNum(b, tok.delimiterNum.toUInt())
        return Token.endToken(b)
    }

    // ─── 消息解析 ─────────────────────────────────────────────────────────────

    fun getMsgType(payload: ByteArray): UByte {
        val buf = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN)
        return MacroMsg.getRootAsMacroMsg(buf).contentType
    }

    fun parseMacroResult(payload: ByteArray): ParsedMacroResult? {
        return runCatching {
            val buf = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN)
            val msg = MacroMsg.getRootAsMacroMsg(buf)

            // content union 取出 MacroResult
            val result = msg.content(MacroResult()) as? MacroResult ?: return null

            val status = result.status
            val tokens = (0 until result.tksLength).mapNotNull { i ->
                val tok = result.tks(Token(), i) ?: return@mapNotNull null
                val begin = tok.begin
                val end = tok.end
                ParsedToken(
                    kind  = tok.kind.toInt() and 0xFF,
                    value = tok.value ?: "",
                    beginLine = begin?.line ?: 0,
                    beginCol = begin?.column ?: 0,
                    endLine = end?.line ?: 0,
                    endCol = end?.column ?: 0,
                )
            }
            val diagnostics = (0 until result.diagsLength).mapNotNull { i ->
                val diag = result.diags(Diagnostic(), i) ?: return@mapNotNull null
                ParsedDiagnostic(
                    severity = diag.diagSeverity,
                    message  = diag.errorMessage ?: return@mapNotNull null,
                    hint     = diag.mainHint ?: ""
                )
            }
            ParsedMacroResult(status = status, tokens = tokens, diagnostics = diagnostics)
        }.getOrNull()
    }

    // ─── 数据类 ───────────────────────────────────────────────────────────────

    data class PositionInfo(val fileId: Int = 0, val line: Int = 0, val column: Int = 0)

    data class TokenInfo(
        val kind: UByte,
        val value: String,
        val begin: PositionInfo = PositionInfo(),
        val end: PositionInfo = PositionInfo(),
        val delimiterNum: Int = 1,
    )

    data class MacroCallInfo(
        val idName: String,
        val idPos: PositionInfo = PositionInfo(),
        val hasAttrs: Boolean = false,
        val args: List<TokenInfo> = emptyList(),
        val attrs: List<TokenInfo> = emptyList(),
        val parentNames: List<String> = emptyList(),
        val methodName: String,
        val packageName: String = "",
        val libPath: String = "",
        val begin: PositionInfo = PositionInfo(),
        val end: PositionInfo = PositionInfo(),
    )

    data class ParsedMacroResult(
        val status: UByte,
        val tokens: List<ParsedToken>,
        val diagnostics: List<ParsedDiagnostic>,
    ) {
        val isSuccess: Boolean get() = status == STATUS_SUCCESS || status == STATUS_FINISH
        val isFailed:  Boolean get() = status == STATUS_FAIL

        /**
         * 根据 token 类型启发式重建展开文本
         *
         * C++ 宏服务返回的展开 token 位置信息指向原始宏调用位置，不代表展开后的布局，
         * 因此无法通过位置重建空白。改用基于 token 值的启发式规则在 token 间插入空格：
         * - 换行 token（值含 `\n`/`\r`）直接输出 `\n`
         * - 定界符（括号、逗号等）前后不加空格
         * - 其余 token 间加空格（关键字、标识符等）
         */
        fun toExpandedText(): String {
            if (tokens.isEmpty()) return ""
            val sb = StringBuilder()
            var prevToken: ParsedToken? = null
            for (tok in tokens) {
                if (tok.value.contains('\n') || tok.value.contains('\r')) {
                    sb.append('\n')
                    prevToken = tok
                    continue
                }
                val prev = prevToken
                if (prev != null && needsSpaceBetween(prev, tok)) {
                    sb.append(' ')
                }
                sb.append(tok.value)
                prevToken = tok
            }
            return sb.toString()
        }

        companion object {
            private val noSpaceAfter = setOf("(", "[", ".", "@", "#", "!")
            private val noSpaceBefore = setOf(")", "]", ",", ";", ".", "(")

            private fun needsSpaceBetween(prev: ParsedToken, curr: ParsedToken): Boolean {
                if (prev.value.contains('\n') || prev.value.contains('\r')) return false
                if (prev.value in noSpaceAfter) return false
                if (curr.value in noSpaceBefore) return false
                return true
            }
        }
    }

    data class ParsedToken(
        val kind: Int,
        val value: String,
        val beginLine: Int = 0,
        val beginCol: Int = 0,
        val endLine: Int = 0,
        val endCol: Int = 0,
    )
    data class ParsedDiagnostic(val severity: Int, val message: String, val hint: String)
}