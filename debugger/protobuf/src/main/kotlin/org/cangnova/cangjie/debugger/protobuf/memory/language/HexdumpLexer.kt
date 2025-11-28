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

package org.cangnova.cangjie.debugger.protobuf.memory.language

import com.intellij.lexer.LexerBase
import com.intellij.psi.tree.IElementType

/**
 * Hexdump词法分析器
 *
 * 将Hexdump文本流分解为Token序列。
 *
 * 状态机：
 * - INITIAL: 行首，期待地址
 * - AFTER_ADDRESS: 地址后，期待冒号
 * - HEX_SECTION: 十六进制部分
 * - ASCII_SECTION: ASCII部分
 * - COMMENT: 注释部分
 *
 * 示例输入：
 * ```
 * 0000000000001000: 48 89 E5 48 83 EC 20  |H..H.. |
 * ```
 *
 * Token序列：
 * ```
 * ADDRESS(0000000000001000)
 * COLON(:)
 * WHITESPACE( )
 * HEX_BYTE(48)
 * WHITESPACE( )
 * HEX_BYTE(89)
 * ...
 * ASCII_PIPE(|)
 * ASCII_CHAR(H)
 * ...
 * ASCII_PIPE(|)
 * NEWLINE(\n)
 * ```
 */
class HexdumpLexer : LexerBase() {

    private var buffer: CharSequence = ""
    private var startOffset = 0
    private var endOffset = 0
    private var bufferEnd = 0
    private var state = State.INITIAL
    private var tokenType: IElementType? = null

    private enum class State {
        INITIAL,          // 行首
        AFTER_ADDRESS,    // 地址后
        HEX_SECTION,      // 十六进制区域
        ASCII_SECTION,    // ASCII区域
        COMMENT           // 注释
    }

    override fun start(buffer: CharSequence, startOffset: Int, endOffset: Int, initialState: Int) {
        this.buffer = buffer
        this.startOffset = startOffset
        this.endOffset = startOffset
        this.bufferEnd = endOffset
        this.state = State.values()[initialState]
        this.tokenType = null
        advance()
    }

    override fun getState(): Int = state.ordinal

    override fun getTokenType(): IElementType? = tokenType

    override fun getTokenStart(): Int = startOffset

    override fun getTokenEnd(): Int = endOffset

    override fun advance() {
        if (endOffset >= bufferEnd) {
            tokenType = null
            return
        }

        startOffset = endOffset

        when (state) {
            State.INITIAL -> advanceInitial()
            State.AFTER_ADDRESS -> advanceAfterAddress()
            State.HEX_SECTION -> advanceHexSection()
            State.ASCII_SECTION -> advanceAsciiSection()
            State.COMMENT -> advanceComment()
        }
    }

    /**
     * 行首状态：期待地址或注释
     */
    private fun advanceInitial() {
        val char = buffer[endOffset]

        when {
            // 注释
            char == ';' -> {
                state = State.COMMENT
                advanceComment()
            }
            // 空白
            char.isWhitespace() -> {
                advanceWhitespace()
            }
            // 地址（十六进制数字）
            char.isHexDigit() -> {
                advanceAddress()
                state = State.AFTER_ADDRESS
            }

            else -> {
                // 错误字符
                endOffset++
                tokenType = HexdumpTokenTypes.BAD_CHARACTER
            }
        }
    }

    /**
     * 地址后状态：期待冒号
     */
    private fun advanceAfterAddress() {
        val char = buffer[endOffset]

        when {
            char == ':' -> {
                endOffset++
                tokenType = HexdumpTokenTypes.COLON
                state = State.HEX_SECTION
            }

            char.isWhitespace() -> {
                advanceWhitespace()
            }

            else -> {
                endOffset++
                tokenType = HexdumpTokenTypes.BAD_CHARACTER
            }
        }
    }

    /**
     * 十六进制区域：字节或ASCII边界
     */
    private fun advanceHexSection() {
        val char = buffer[endOffset]

        when {
            // ASCII边界
            char == '|' -> {
                endOffset++
                tokenType = HexdumpTokenTypes.ASCII_PIPE
                state = State.ASCII_SECTION
            }
            // 注释
            char == ';' -> {
                state = State.COMMENT
                advanceComment()
            }
            // 换行（结束行）
            char == '\n' -> {
                endOffset++
                tokenType = HexdumpTokenTypes.NEWLINE
                state = State.INITIAL
            }
            // 空白
            char.isWhitespace() -> {
                advanceWhitespace()
            }
            // 占位符
            char == '?' -> {
                advancePlaceholder()
            }
            // 十六进制字节
            char.isHexDigit() -> {
                advanceHexByte()
            }

            else -> {
                endOffset++
                tokenType = HexdumpTokenTypes.BAD_CHARACTER
            }
        }
    }

    /**
     * ASCII区域：ASCII字符或结束边界
     */
    private fun advanceAsciiSection() {
        val char = buffer[endOffset]

        when {
            // ASCII结束边界
            char == '|' -> {
                endOffset++
                tokenType = HexdumpTokenTypes.ASCII_PIPE
                state = State.HEX_SECTION  // 可能有多个ASCII区域
            }
            // 换行（结束行）
            char == '\n' -> {
                endOffset++
                tokenType = HexdumpTokenTypes.NEWLINE
                state = State.INITIAL
            }
            // ASCII字符
            else -> {
                endOffset++
                tokenType = HexdumpTokenTypes.ASCII_CHAR
            }
        }
    }

    /**
     * 注释状态
     */
    private fun advanceComment() {
        // 读取到行尾
        while (endOffset < bufferEnd && buffer[endOffset] != '\n') {
            endOffset++
        }
        tokenType = HexdumpTokenTypes.COMMENT
    }

    /**
     * 读取地址
     */
    private fun advanceAddress() {
        while (endOffset < bufferEnd && buffer[endOffset].isHexDigit()) {
            endOffset++
        }
        tokenType = HexdumpTokenTypes.ADDRESS
    }

    /**
     * 读取十六进制字节
     */
    private fun advanceHexByte() {
        // 最多2个十六进制字符
        var count = 0
        while (endOffset < bufferEnd && buffer[endOffset].isHexDigit() && count < 2) {
            endOffset++
            count++
        }
        tokenType = HexdumpTokenTypes.HEX_BYTE
    }

    /**
     * 读取占位符
     */
    private fun advancePlaceholder() {
        // ?? 表示未加载的字节
        if (endOffset + 1 < bufferEnd && buffer[endOffset + 1] == '?') {
            endOffset += 2
        } else {
            endOffset++
        }
        tokenType = HexdumpTokenTypes.PLACEHOLDER
    }

    /**
     * 读取空白
     */
    private fun advanceWhitespace() {
        while (endOffset < bufferEnd && buffer[endOffset].isWhitespace() && buffer[endOffset] != '\n') {
            endOffset++
        }
        tokenType = HexdumpTokenTypes.WHITESPACE
    }

    override fun getBufferSequence(): CharSequence = buffer

    override fun getBufferEnd(): Int = bufferEnd

    private fun Char.isHexDigit(): Boolean {
        return this in '0'..'9' || this in 'a'..'f' || this in 'A'..'F'
    }
}