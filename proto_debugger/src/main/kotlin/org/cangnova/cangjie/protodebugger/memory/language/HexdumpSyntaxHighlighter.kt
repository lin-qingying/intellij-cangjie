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

package org.cangnova.cangjie.protodebugger.memory.language

import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors as Default
import com.intellij.psi.tree.IElementType

/**
 * Hexdump语法高亮工厂
 */
class HexdumpSyntaxHighlighterFactory : SyntaxHighlighterFactory() {
    override fun getSyntaxHighlighter(project: Project?, virtualFile: VirtualFile?): SyntaxHighlighter {
        return HexdumpSyntaxHighlighter()
    }
}

/**
 * Hexdump语法高亮器
 *
 * 为不同的Token类型分配颜色和样式。
 *
 * 配色方案：
 * - ADDRESS: 深灰色粗体
 * - HEX_BYTE: 蓝色
 * - ASCII_CHAR: 绿色
 * - PLACEHOLDER: 灰色
 * - COMMENT: 斜体灰色
 */
class HexdumpSyntaxHighlighter : SyntaxHighlighter {

    override fun getHighlightingLexer(): com.intellij.lexer.Lexer {
        return HexdumpLexer()
    }

    override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey> {
        return when (tokenType) {
            HexdumpTokenTypes.ADDRESS -> ADDRESS_KEYS
            HexdumpTokenTypes.COLON -> SEPARATOR_KEYS
            HexdumpTokenTypes.HEX_BYTE -> HEX_BYTE_KEYS
            HexdumpTokenTypes.ASCII_PIPE -> SEPARATOR_KEYS
            HexdumpTokenTypes.ASCII_CHAR -> ASCII_CHAR_KEYS
            HexdumpTokenTypes.PLACEHOLDER -> PLACEHOLDER_KEYS
            HexdumpTokenTypes.COMMENT -> COMMENT_KEYS
            HexdumpTokenTypes.BAD_CHARACTER -> BAD_CHAR_KEYS
            else -> EMPTY_KEYS
        }
    }

    companion object {
        // 定义颜色属性
        val ADDRESS = TextAttributesKey.createTextAttributesKey(
            "HEXDUMP_ADDRESS",
            Default.KEYWORD
        )

        val HEX_BYTE = TextAttributesKey.createTextAttributesKey(
            "HEXDUMP_HEX_BYTE",
            Default.NUMBER
        )

        val ASCII_CHAR = TextAttributesKey.createTextAttributesKey(
            "HEXDUMP_ASCII_CHAR",
            Default.STRING
        )

        val SEPARATOR = TextAttributesKey.createTextAttributesKey(
            "HEXDUMP_SEPARATOR",
            Default.OPERATION_SIGN
        )

        val PLACEHOLDER = TextAttributesKey.createTextAttributesKey(
            "HEXDUMP_PLACEHOLDER",
            Default.LINE_COMMENT
        )

        val COMMENT = TextAttributesKey.createTextAttributesKey(
            "HEXDUMP_COMMENT",
            Default.LINE_COMMENT
        )

        val BAD_CHARACTER = TextAttributesKey.createTextAttributesKey(
            "HEXDUMP_BAD_CHARACTER",
            Default.INVALID_STRING_ESCAPE
        )

        private val ADDRESS_KEYS = arrayOf(ADDRESS)
        private val HEX_BYTE_KEYS = arrayOf(HEX_BYTE)
        private val ASCII_CHAR_KEYS = arrayOf(ASCII_CHAR)
        private val SEPARATOR_KEYS = arrayOf(SEPARATOR)
        private val PLACEHOLDER_KEYS = arrayOf(PLACEHOLDER)
        private val COMMENT_KEYS = arrayOf(COMMENT)
        private val BAD_CHAR_KEYS = arrayOf(BAD_CHARACTER)
        private val EMPTY_KEYS = emptyArray<TextAttributesKey>()
    }
}

/**
 * 高亮颜色配置提供者
 *
 * 允许用户在设置中自定义颜色。
 */
class HexdumpColorSettingsPage : com.intellij.openapi.options.colors.ColorSettingsPage {

    override fun getIcon(): javax.swing.Icon? = null

    override fun getHighlighter(): SyntaxHighlighter {
        return HexdumpSyntaxHighlighter()
    }

    override fun getDemoText(): String {
        return """
            ; Memory dump at 0x1000
            0000000000001000: 48 89 E5 48 83 EC 20 48  89 7D F8 89 75 F4 8B 45  |H..H.. H.}..u..E|
            0000000000001010: F4 0F AF 45 F8 89 45 FC  8B 45 FC 48 83 C4 20 5D  |...E..E..E.H.. ]|
            0000000000001020: C3 ?? ?? ??              |...            |

            ; End of function
        """.trimIndent()
    }

    override fun getAdditionalHighlightingTagToDescriptorMap(): Map<String, TextAttributesKey>? {
        return null
    }

    override fun getAttributeDescriptors(): Array<com.intellij.openapi.options.colors.AttributesDescriptor> {
        return DESCRIPTORS
    }

    override fun getColorDescriptors(): Array<com.intellij.openapi.options.colors.ColorDescriptor> {
        return com.intellij.openapi.options.colors.ColorDescriptor.EMPTY_ARRAY
    }

    override fun getDisplayName(): String = "Hexdump"

    companion object {
        private val DESCRIPTORS = arrayOf(
            com.intellij.openapi.options.colors.AttributesDescriptor("Address", HexdumpSyntaxHighlighter.ADDRESS),
            com.intellij.openapi.options.colors.AttributesDescriptor("Hex Byte", HexdumpSyntaxHighlighter.HEX_BYTE),
            com.intellij.openapi.options.colors.AttributesDescriptor("ASCII Character", HexdumpSyntaxHighlighter.ASCII_CHAR),
            com.intellij.openapi.options.colors.AttributesDescriptor("Separator", HexdumpSyntaxHighlighter.SEPARATOR),
            com.intellij.openapi.options.colors.AttributesDescriptor("Placeholder", HexdumpSyntaxHighlighter.PLACEHOLDER),
            com.intellij.openapi.options.colors.AttributesDescriptor("Comment", HexdumpSyntaxHighlighter.COMMENT),
            com.intellij.openapi.options.colors.AttributesDescriptor("Bad Character", HexdumpSyntaxHighlighter.BAD_CHARACTER)
        )
    }
}