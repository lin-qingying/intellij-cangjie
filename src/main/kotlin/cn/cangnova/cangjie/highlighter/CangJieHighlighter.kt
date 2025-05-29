/*
 * Copyright 2024 LinQingYing. and contributors.
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

package cn.cangnova.cangjie.highlighter

import cn.cangnova.cangjie.psi.cdoc.lexer.CDocTokens
import cn.cangnova.cangjie.psi.cdoc.lexer.CDocTokens.CDOC_HIGHLIGHT_TOKENS
import cn.cangnova.cangjie.lexer.CjTokens
import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet

/**
 * 仓颉语言的语法高亮器实现
 * 
 * 这个类负责为仓颉语言的源代码提供语法高亮功能。它继承自 IntelliJ Platform 的 SyntaxHighlighterBase 类，
 * 实现了词法分析和高亮颜色映射的功能。主要职责包括：
 * 
 * 1. 提供词法分析器，用于将源代码分解成词法单元（Token）
 * 2. 为每个词法单元分配对应的高亮颜色属性
 * 3. 管理不同类型的语法元素（关键字、标识符、字面量等）的高亮样式
 *
 * 与其他高亮相关类的区别：
 * 
 * 1. 处理层次不同：
 *    - CangJieHighlighter: 工作在词法层面，处理最基本的文本标记
 *    - AbstractHighlightingPassBase/Visitor: 工作在语义层面，处理更复杂的语言结构
 * 
 * 2. 处理时机不同：
 *    - CangJieHighlighter: 在最早的词法分析阶段工作
 *    - 其他高亮类：在后续的语法分析和语义分析阶段工作
 * 
 * 3. 功能复杂度不同：
 *    - CangJieHighlighter: 进行简单的一对一映射（词法标记→颜色）
 *    - 其他高亮类：执行复杂的语义分析和上下文相关的高亮处理
 * 
 * 4. 协作关系：
 *    - CangJieHighlighter 是基础设施，提供最基本的高亮支持
 *    - 其他高亮类在此基础上构建更智能的高亮系统，处理更复杂的语言特性
 * 
 * 可以将它们的关系比喻为：
 * - CangJieHighlighter 就像是给文本"上色"的画笔
 * - 而其他高亮类则构成了一个"智能绘画系统"，能够理解画作的内容并进行更专业的着色处理
 */
class CangJieHighlighter : SyntaxHighlighterBase() {
    /**
     * 获取用于语法高亮的词法分析器
     * 
     * @return 返回仓颉语言的词法分析器实例
     */
    override fun getHighlightingLexer(): Lexer {
        return CangJieHighlightingLexer()
    }

    /**
     * 获取指定词法单元类型的高亮属性
     * 
     * @param tokenType 词法单元类型
     * @return 返回该类型对应的文本属性数组，包含主要和次要高亮样式
     */
    override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey> {
        return pack(
            keys1[tokenType],
            keys2[tokenType],
        )
    }

    companion object {
        // 主要高亮属性映射表
        private val keys1: MutableMap<IElementType, TextAttributesKey> = HashMap()
        // 次要高亮属性映射表（用于叠加效果）
        private val keys2: MutableMap<IElementType, TextAttributesKey> = HashMap()

        init {
            // 初始化各类词法单元的高亮属性映射

            // 关键字高亮
            fillMap(keys1, CjTokens.KEYWORDS, CangJieHighlightingColors.KEYWORD)
            // 基本类型高亮
            fillMap(keys1, CjTokens.BASICTYPES, CangJieHighlightingColors.KEYWORD)

            // 特殊关键字高亮
            keys1[CjTokens.LET_KEYWORD] = CangJieHighlightingColors.LET_KEYWORD
            keys1[CjTokens.VAR_KEYWORD] = CangJieHighlightingColors.VAR_KEYWORD
            keys1[CjTokens.CONST_KEYWORD] = CangJieHighlightingColors.CONST_KEYWORD
            keys1[CjTokens.QUOTE_KEYWORD] = CangJieHighlightingColors.QUOTE_KEYWORD

            // 字面量高亮
            keys1[CjTokens.INTEGER_LITERAL] = CangJieHighlightingColors.NUMBER
            keys1[CjTokens.FLOAT_LITERAL] = CangJieHighlightingColors.NUMBER

            // 运算符高亮
            fillMap(
                keys1,
                TokenSet.andNot(
                    CjTokens.OPERATIONS,
                    TokenSet.orSet(
                        TokenSet.create(
                            CjTokens.IDENTIFIER,
                            CjTokens.AT,
                        ),
                        CjTokens.KEYWORDS,
                    ),
                ),
                CangJieHighlightingColors.OPERATOR_SIGN,
            )

            // 括号和标点符号高亮
            keys1[CjTokens.LPAR] = CangJieHighlightingColors.PARENTHESIS
            keys1[CjTokens.RPAR] = CangJieHighlightingColors.PARENTHESIS
            keys1[CjTokens.LBRACE] = CangJieHighlightingColors.BRACES
            keys1[CjTokens.RBRACE] = CangJieHighlightingColors.BRACES
            keys1[CjTokens.LBRACKET] = CangJieHighlightingColors.BRACKETS
            keys1[CjTokens.RBRACKET] = CangJieHighlightingColors.BRACKETS
            keys1[CjTokens.COMMA] = CangJieHighlightingColors.COMMA
            keys1[CjTokens.SEMICOLON] = CangJieHighlightingColors.SEMICOLON
            keys1[CjTokens.COLON] = CangJieHighlightingColors.COLON

            // 其他操作符和符号高亮
            keys1[CjTokens.QUEST] = CangJieHighlightingColors.QUEST
            keys1[CjTokens.DOT] = CangJieHighlightingColors.DOT
            keys1[CjTokens.ARROW] = CangJieHighlightingColors.ARROW

            // 字符串相关高亮
            keys1[CjTokens.OPEN_QUOTE] = CangJieHighlightingColors.STRING
            keys1[CjTokens.CLOSING_QUOTE] = CangJieHighlightingColors.STRING
            keys1[CjTokens.REGULAR_STRING_PART] = CangJieHighlightingColors.STRING
            keys1[CjTokens.LONG_TEMPLATE_ENTRY_END] = CangJieHighlightingColors.STRING_ESCAPE
            keys1[CjTokens.LONG_TEMPLATE_ENTRY_START] = CangJieHighlightingColors.STRING_ESCAPE
            keys1[CjTokens.SHORT_TEMPLATE_ENTRY_START] = CangJieHighlightingColors.STRING_ESCAPE
            keys1[CjTokens.ESCAPE_SEQUENCE] = CangJieHighlightingColors.STRING_ESCAPE
            keys1[CjTokens.RUNE_LITERAL] = CangJieHighlightingColors.STRING

            // 注释高亮
            keys1[CjTokens.EOL_COMMENT] = CangJieHighlightingColors.LINE_COMMENT
            keys1[CjTokens.SHEBANG_COMMENT] = CangJieHighlightingColors.LINE_COMMENT
            keys1[CjTokens.BLOCK_COMMENT] = CangJieHighlightingColors.BLOCK_COMMENT
            keys1[CjTokens.DOC_COMMENT] = CangJieHighlightingColors.DOC_COMMENT

            // CDoc 文档注释高亮
            fillMap(
                keys1,
                CDOC_HIGHLIGHT_TOKENS,
                CangJieHighlightingColors.DOC_COMMENT,
            )
            keys1[CDocTokens.TAG_NAME] = CangJieHighlightingColors.DOC_COMMENT
            keys2[CDocTokens.TAG_NAME] = CangJieHighlightingColors.CDOC_TAG

            // 错误字符高亮
            keys1[TokenType.BAD_CHARACTER] = CangJieHighlightingColors.BAD_CHARACTER
        }
    }
}
