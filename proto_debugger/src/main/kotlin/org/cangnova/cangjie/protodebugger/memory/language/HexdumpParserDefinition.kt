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

import com.intellij.lang.ASTNode
import com.intellij.lang.ParserDefinition
import com.intellij.lang.PsiParser
import com.intellij.lexer.Lexer
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet

/**
 * Hexdump语法分析器定义
 *
 * 定义如何将Token序列解析为PSI树。
 *
 * PSI树结构：
 * ```
 * FILE
 *   ├── LINE
 *   │   ├── ADDRESS_PART
 *   │   │   └── ADDRESS
 *   │   ├── COLON
 *   │   ├── HEX_PART
 *   │   │   ├── HEX_BYTE
 *   │   │   ├── HEX_BYTE
 *   │   │   └── ...
 *   │   └── ASCII_PART
 *   │       ├── ASCII_PIPE
 *   │       ├── ASCII_CHAR*
 *   │       └── ASCII_PIPE
 *   ├── LINE
 *   └── ...
 * ```
 */
class HexdumpParserDefinition : ParserDefinition {

    override fun createLexer(project: Project?): Lexer {
        return HexdumpLexer()
    }

    override fun createParser(project: Project?): PsiParser {
        return HexdumpParser()
    }

    override fun getFileNodeType(): IFileElementType {
        return FILE
    }

    override fun getCommentTokens(): TokenSet {
        return COMMENTS
    }

    override fun getStringLiteralElements(): TokenSet {
        return TokenSet.EMPTY
    }

    override fun createElement(node: ASTNode): PsiElement {
        return HexdumpPsiElement(node)
    }

    override fun createFile(viewProvider: FileViewProvider): PsiFile {
        return HexdumpFile(viewProvider)
    }

    companion object {
        val FILE = IFileElementType(HexdumpLanguage.INSTANCE)

        val WHITESPACES = TokenSet.create(HexdumpTokenTypes.WHITESPACE, HexdumpTokenTypes.NEWLINE)
        val COMMENTS = TokenSet.create(HexdumpTokenTypes.COMMENT)
    }
}

/**
 * Hexdump语法分析器
 *
 * 简单的递归下降解析器。
 */
class HexdumpParser : PsiParser {

    override fun parse(root: IElementType, builder: com.intellij.lang.PsiBuilder): ASTNode {
        val rootMarker = builder.mark()

        // 解析所有行
        while (!builder.eof()) {
            parseLine(builder)
        }

        rootMarker.done(root)
        return builder.treeBuilt
    }

    /**
     * 解析一行
     *
     * LINE := ADDRESS_PART HEX_PART [ASCII_PART] [COMMENT] NEWLINE
     */
    private fun parseLine(builder: com.intellij.lang.PsiBuilder) {
        // 跳过空行
        if (builder.tokenType == HexdumpTokenTypes.NEWLINE) {
            builder.advanceLexer()
            return
        }

        // 注释行
        if (builder.tokenType == HexdumpTokenTypes.COMMENT) {
            builder.advanceLexer()
            if (builder.tokenType == HexdumpTokenTypes.NEWLINE) {
                builder.advanceLexer()
            }
            return
        }

        val lineMarker = builder.mark()

        // 地址部分
        parseAddressPart(builder)

        // 十六进制部分
        parseHexPart(builder)

        // ASCII部分（可选）
        if (builder.tokenType == HexdumpTokenTypes.ASCII_PIPE) {
            parseAsciiPart(builder)
        }

        // 注释（可选）
        if (builder.tokenType == HexdumpTokenTypes.COMMENT) {
            builder.advanceLexer()
        }

        // 换行
        if (builder.tokenType == HexdumpTokenTypes.NEWLINE) {
            builder.advanceLexer()
        }

        lineMarker.done(HexdumpElementTypes.LINE)
    }

    /**
     * 解析地址部分
     *
     * ADDRESS_PART := ADDRESS COLON
     */
    private fun parseAddressPart(builder: com.intellij.lang.PsiBuilder) {
        val marker = builder.mark()

        // 跳过空白
        while (builder.tokenType == HexdumpTokenTypes.WHITESPACE) {
            builder.advanceLexer()
        }

        // 地址
        if (builder.tokenType == HexdumpTokenTypes.ADDRESS) {
            builder.advanceLexer()
        } else {
            marker.error("Expected address")
            return
        }

        // 跳过空白
        while (builder.tokenType == HexdumpTokenTypes.WHITESPACE) {
            builder.advanceLexer()
        }

        // 冒号
        if (builder.tokenType == HexdumpTokenTypes.COLON) {
            builder.advanceLexer()
        } else {
            marker.error("Expected colon after address")
            return
        }

        marker.done(HexdumpElementTypes.ADDRESS_PART)
    }

    /**
     * 解析十六进制部分
     *
     * HEX_PART := (HEX_BYTE | PLACEHOLDER | WHITESPACE)*
     */
    private fun parseHexPart(builder: com.intellij.lang.PsiBuilder) {
        val marker = builder.mark()

        while (builder.tokenType == HexdumpTokenTypes.HEX_BYTE ||
               builder.tokenType == HexdumpTokenTypes.PLACEHOLDER ||
               builder.tokenType == HexdumpTokenTypes.WHITESPACE) {
            builder.advanceLexer()
        }

        marker.done(HexdumpElementTypes.HEX_PART)
    }

    /**
     * 解析ASCII部分
     *
     * ASCII_PART := ASCII_PIPE ASCII_CHAR* ASCII_PIPE
     */
    private fun parseAsciiPart(builder: com.intellij.lang.PsiBuilder) {
        val marker = builder.mark()

        // 左边界
        if (builder.tokenType == HexdumpTokenTypes.ASCII_PIPE) {
            builder.advanceLexer()
        } else {
            marker.error("Expected ASCII pipe")
            return
        }

        // ASCII字符
        while (builder.tokenType == HexdumpTokenTypes.ASCII_CHAR) {
            builder.advanceLexer()
        }

        // 右边界
        if (builder.tokenType == HexdumpTokenTypes.ASCII_PIPE) {
            builder.advanceLexer()
        }

        marker.done(HexdumpElementTypes.ASCII_PART)
    }
}

/**
 * Hexdump PSI元素基类
 */
open class HexdumpPsiElement(node: ASTNode) : com.intellij.extapi.psi.ASTWrapperPsiElement(node)

/**
 * Hexdump文件
 */
class HexdumpFile(viewProvider: FileViewProvider) : com.intellij.extapi.psi.PsiFileBase(viewProvider, HexdumpLanguage.INSTANCE) {
    override fun getFileType(): com.intellij.openapi.fileTypes.FileType {
        return org.cangnova.cangjie.protodebugger.memory.vfs.HexdumpFileType.INSTANCE
    }

    override fun toString(): String = "Hexdump File"
}