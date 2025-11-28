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

package org.cangnova.cangjie.debugger.protobuf.memory.input

import com.intellij.codeInsight.editorActions.TypedHandlerDelegate
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import org.cangnova.cangjie.debugger.protobuf.memory.language.HexdumpFile

/**
 * Hexdump智能输入处理器
 *
 * 为十六进制编辑提供智能输入支持：
 * 1. 自动格式化十六进制输入（强制大写、过滤非法字符）
 * 2. 自动空格分隔（每2个字符）
 * 3. 自动换行（每16字节）
 * 4. 同步更新ASCII区域
 * 5. 防止编辑地址和分隔符部分
 *
 * 示例行为：
 * - 用户输入 "4" → 光标保持，等待第二个字符
 * - 用户输入 "8" → 插入 "48 "，光标移到下一个字节位置
 * - 用户输入 "g" → 忽略（非十六进制字符）
 * - 用户输入 "a" → 转换为 "A"
 */
class HexdumpTypedHandler : TypedHandlerDelegate() {

    override fun charTyped(c: Char, project: Project, editor: Editor, file: PsiFile): Result {
        // 只处理Hexdump文件
        if (file !is HexdumpFile) {
            return Result.CONTINUE
        }

        val document = editor.document
        val caretModel = editor.caretModel
        val offset = caretModel.offset

        // 检查是否在可编辑区域（十六进制部分）
        if (!isInHexSection(editor, offset)) {
            return Result.STOP  // 阻止在非编辑区域输入
        }

        // 过滤非十六进制字符
        if (!c.isHexDigit()) {
            return Result.STOP
        }

        // 转换为大写
        val hexChar = c.uppercaseChar()

        // 检查当前字节是否完整
        val currentByteStart = findByteStart(document.text, offset)
        val currentByteEnd = findByteEnd(document.text, offset)

        if (currentByteEnd - currentByteStart >= 2) {
            // 当前字节已完整，移动到下一个字节位置
            val nextByteStart = findNextByteStart(document.text, currentByteEnd)
            if (nextByteStart > currentByteEnd) {
                caretModel.moveToOffset(nextByteStart)
            }
        }

        // 插入十六进制字符
        document.insertString(offset, hexChar.toString())
        caretModel.moveToOffset(offset + 1)

        // 检查是否需要添加空格（字节完整）
        val newByteEnd = findByteEnd(document.text, offset + 1)
        if (newByteEnd - currentByteStart == 2) {
            // 字节完整，添加空格
            document.insertString(offset + 1, " ")
            caretModel.moveToOffset(offset + 2)

            // 同步更新ASCII区域
            updateAsciiSection(editor, currentByteStart, offset + 1)
        }

        return Result.STOP
    }

    /**
     * 检查偏移是否在十六进制区域
     */
    private fun isInHexSection(editor: Editor, offset: Int): Boolean {
        val document = editor.document
        val lineNumber = document.getLineNumber(offset)
        val lineStartOffset = document.getLineStartOffset(lineNumber)
        val lineText = document.getText(
            com.intellij.openapi.util.TextRange(
                lineStartOffset,
                document.getLineEndOffset(lineNumber)
            )
        )

        // 查找冒号（地址结束）
        val colonIndex = lineText.indexOf(':')
        if (colonIndex < 0) return false

        // 查找第一个ASCII边界
        val pipeIndex = lineText.indexOf('|', colonIndex)
        if (pipeIndex < 0) {
            // 没有ASCII部分，十六进制区域到行尾
            return offset > lineStartOffset + colonIndex
        }

        // 十六进制区域在冒号和管道之间
        val hexStart = lineStartOffset + colonIndex + 1
        val hexEnd = lineStartOffset + pipeIndex
        return offset in hexStart until hexEnd
    }

    /**
     * 查找当前字节的起始位置
     */
    private fun findByteStart(text: String, offset: Int): Int {
        var pos = offset
        while (pos > 0 && text[pos - 1].isHexDigit()) {
            pos--
        }
        return pos
    }

    /**
     * 查找当前字节的结束位置
     */
    private fun findByteEnd(text: String, offset: Int): Int {
        var pos = offset
        while (pos < text.length && text[pos].isHexDigit()) {
            pos++
        }
        return pos
    }

    /**
     * 查找下一个字节的起始位置
     */
    private fun findNextByteStart(text: String, currentEnd: Int): Int {
        var pos = currentEnd
        // 跳过空格
        while (pos < text.length && text[pos].isWhitespace()) {
            pos++
        }
        return pos
    }

    /**
     * 同步更新ASCII区域
     */
    private fun updateAsciiSection(editor: Editor, byteStart: Int, byteEnd: Int) {
        val document = editor.document
        val lineNumber = document.getLineNumber(byteStart)
        val lineStartOffset = document.getLineStartOffset(lineNumber)
        val lineText = document.getText(
            com.intellij.openapi.util.TextRange(
                lineStartOffset,
                document.getLineEndOffset(lineNumber)
            )
        )

        // 查找ASCII区域
        val firstPipe = lineText.indexOf('|')
        val secondPipe = lineText.indexOf('|', firstPipe + 1)
        if (firstPipe < 0 || secondPipe < 0) return

        // 解析字节值
        val byteText = document.getText(
            com.intellij.openapi.util.TextRange(byteStart, byteEnd)
        )
        val byteValue = try {
            byteText.toInt(16)
        } catch (e: NumberFormatException) {
            return
        }

        // 计算ASCII位置
        val byteIndex = calculateByteIndex(lineText, byteStart - lineStartOffset)
        val asciiOffset = lineStartOffset + firstPipe + 1 + byteIndex

        // 更新ASCII字符
        val asciiChar = if (byteValue in 32..126) {
            byteValue.toChar()
        } else {
            '·'
        }

        if (asciiOffset < lineStartOffset + secondPipe) {
            document.replaceString(asciiOffset, asciiOffset + 1, asciiChar.toString())
        }
    }

    /**
     * 计算字节在行中的索引（0-15）
     */
    private fun calculateByteIndex(lineText: String, offsetInLine: Int): Int {
        val colonIndex = lineText.indexOf(':')
        if (colonIndex < 0) return 0

        val hexSection = lineText.substring(colonIndex + 1, offsetInLine)
        return hexSection.count { it.isHexDigit() } / 2
    }

    private fun Char.isHexDigit(): Boolean {
        return this in '0'..'9' || this in 'a'..'f' || this in 'A'..'F'
    }
}

/**
 * 原始字节输入处理器
 *
 * 用于直接输入原始字节值（ASCII字符）：
 * - 在ASCII区域输入字符
 * - 自动同步更新十六进制区域
 * - 处理不可打印字符
 */
class MemoryFileRawTypedHandler : TypedHandlerDelegate() {

    override fun charTyped(c: Char, project: Project, editor: Editor, file: PsiFile): Result {
        // 只处理Hexdump文件
        if (file !is HexdumpFile) {
            return Result.CONTINUE
        }

        val document = editor.document
        val caretModel = editor.caretModel
        val offset = caretModel.offset

        // 检查是否在ASCII区域
        if (!isInAsciiSection(editor, offset)) {
            return Result.CONTINUE
        }

        // 插入字符
        val displayChar = if (c.code in 32..126) c else '·'
        document.replaceString(offset, offset + 1, displayChar.toString())
        caretModel.moveToOffset(offset + 1)

        // 同步更新十六进制区域
        updateHexSection(editor, offset, c)

        return Result.STOP
    }

    /**
     * 检查偏移是否在ASCII区域
     */
    private fun isInAsciiSection(editor: Editor, offset: Int): Boolean {
        val document = editor.document
        val lineNumber = document.getLineNumber(offset)
        val lineStartOffset = document.getLineStartOffset(lineNumber)
        val lineText = document.getText(
            com.intellij.openapi.util.TextRange(
                lineStartOffset,
                document.getLineEndOffset(lineNumber)
            )
        )

        // 查找ASCII边界
        val firstPipe = lineText.indexOf('|')
        val secondPipe = lineText.indexOf('|', firstPipe + 1)
        if (firstPipe < 0 || secondPipe < 0) return false

        // ASCII区域在两个管道之间
        val asciiStart = lineStartOffset + firstPipe + 1
        val asciiEnd = lineStartOffset + secondPipe
        return offset in asciiStart until asciiEnd
    }

    /**
     * 同步更新十六进制区域
     */
    private fun updateHexSection(editor: Editor, asciiOffset: Int, char: Char) {
        val document = editor.document
        val lineNumber = document.getLineNumber(asciiOffset)
        val lineStartOffset = document.getLineStartOffset(lineNumber)
        val lineText = document.getText(
            com.intellij.openapi.util.TextRange(
                lineStartOffset,
                document.getLineEndOffset(lineNumber)
            )
        )

        // 计算字节索引
        val firstPipe = lineText.indexOf('|')
        if (firstPipe < 0) return
        val byteIndex = asciiOffset - lineStartOffset - firstPipe - 1

        // 查找对应的十六进制位置
        val colonIndex = lineText.indexOf(':')
        if (colonIndex < 0) return

        val hexOffset = findHexOffsetForByte(lineText, colonIndex, byteIndex)
        if (hexOffset < 0) return

        // 更新十六进制值
        val hexValue = String.format("%02X", char.code)
        document.replaceString(
            lineStartOffset + hexOffset,
            lineStartOffset + hexOffset + 2,
            hexValue
        )
    }

    /**
     * 查找字节索引对应的十六进制偏移
     */
    private fun findHexOffsetForByte(lineText: String, colonIndex: Int, byteIndex: Int): Int {
        var currentByte = 0
        var offset = colonIndex + 1

        while (offset < lineText.length && currentByte < byteIndex) {
            // 跳过空格
            while (offset < lineText.length && lineText[offset].isWhitespace()) {
                offset++
            }

            // 跳过当前字节（2个十六进制字符）
            if (offset + 1 < lineText.length &&
                lineText[offset].isHexDigit() &&
                lineText[offset + 1].isHexDigit()
            ) {
                offset += 2
                currentByte++
            } else {
                break
            }
        }

        // 跳过空格到目标字节
        while (offset < lineText.length && lineText[offset].isWhitespace()) {
            offset++
        }

        return if (currentByte == byteIndex) offset else -1
    }

    private fun Char.isHexDigit(): Boolean {
        return this in '0'..'9' || this in 'a'..'f' || this in 'A'..'F'
    }
}