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
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.cangjie.lexer.cdoc.psi.impl

import org.cangnova.cangjie.lexer.cdoc.lexer.CDocTokens
import org.cangnova.cangjie.lexer.cdoc.parser.CDocElementTypes
import org.cangnova.cangjie.lexer.cdoc.parser.CDocKnownTag
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.TokenType

open class CDocTag(node: ASTNode) : CDocElementImpl(node) {

    /**
     * 返回此标记的名称，不包括前导@字符。
     *
     * @return 如果此标记表示文档注释标记名称的默认部分，则为NULL；如果此标记表示文档注释的默认部分，则为NULL
     * 或者代码有语法错误。
     */
    override fun getName(): String? {
        val tagName: PsiElement? = findChildByType(CDocTokens.TAG_NAME)
        if (tagName != null) {
            return tagName.text.substring(1)
        }
        return null
    }

    /**
     * 返回此标记记录的实体的名称(例如，@param标记的参数名称)，如果此标记不记录任何特定实体，则返回NULL。
     */
    open fun getSubjectName(): String? = getSubjectLink()?.getLinkText()

    fun getSubjectLink(): CDocLink? {
        val children = childrenAfterTagName()
        if (hasSubject(children)) {
            return children.firstOrNull()?.psi as? CDocLink
        }
        return null
    }

    val knownTag: CDocKnownTag?
        get() {
            return name?.let { CDocKnownTag.findByTagName(it) }
        }

    private fun hasSubject(contentChildren: List<ASTNode>): Boolean {
        if (knownTag?.isReferenceRequired == true) {
            return contentChildren.firstOrNull()?.elementType == CDocTokens.MARKDOWN_LINK
        }
        return false
    }

    private fun childrenAfterTagName(): List<ASTNode> =
        node.getChildren(null)
            .dropWhile { it.elementType == CDocTokens.TAG_NAME }
            .dropWhile { it.elementType == TokenType.WHITE_SPACE }

    /**
     * 返回此标记的内容(标记名称和主题后面的所有文本，如果存在，则删除*)。
     */
    open fun getContent(): String {
        val builder = StringBuilder()
        val codeBlockBuilder = StringBuilder()
        var targetBuilder = builder

        var contentStarted = false
        var afterAsterisk = false
        var indentedCodeBlock = false

        fun isCodeBlock() = targetBuilder == codeBlockBuilder

        fun startCodeBlock() {
            targetBuilder = codeBlockBuilder
        }

        fun flushCodeBlock() {
            if (isCodeBlock()) {
                builder.append(trimCommonIndent(codeBlockBuilder, indentedCodeBlock))
                codeBlockBuilder.setLength(0)
                targetBuilder = builder
            }
        }

        var children = childrenAfterTagName()
        if (hasSubject(children)) {
            children = children.drop(1)
        }
        for (node in children) {
            val type = node.elementType
            if (type == CDocTokens.CODE_BLOCK_TEXT) {
                // If first line of code block
                if (!isCodeBlock()) {
                    indentedCodeBlock =
                        indentedCodeBlock || node.text.startsWith(indentationWhiteSpaces) || node.text.startsWith("\t")
                }
                startCodeBlock()
            } else if (CDocTokens.CONTENT_TOKENS.contains(type)) {
                flushCodeBlock()
                indentedCodeBlock = false
            }

            if (CDocTokens.CONTENT_TOKENS.contains(type)) {
                val isPlainContent = afterAsterisk && !isCodeBlock()
// 如果内容尚未开始且不是缩进代码块的一部分
// 并且不在带围栏的代码块内， 应该去掉前面空格
                val trimLeadingSpaces = !(contentStarted || indentedCodeBlock) || isPlainContent

                targetBuilder.append(if (trimLeadingSpaces) node.text.trimStart() else node.text)
                contentStarted = true
                afterAsterisk = false
            }
            if (type == CDocTokens.LEADING_ASTERISK) {
                afterAsterisk = true
            }
            if (type == TokenType.WHITE_SPACE && contentStarted) {
                targetBuilder.append("\n".repeat(StringUtil.countNewLines(node.text)))
            }
            if (type == CDocElementTypes.CDOC_TAG) {
                break
            }
        }

        flushCodeBlock()

        return builder.toString().trimEnd(' ', '\t')
    }

    private fun trimCommonIndent(builder: StringBuilder, prepend4WhiteSpaces: Boolean = false): String {
        val lines = builder.toString().split('\n')
        val minIndent = lines.filter { it.trim().isNotEmpty() }.minOfOrNull { it.calcIndent() } ?: 0
        var processedLines = lines.map { it.drop(minIndent) }
        if (prepend4WhiteSpaces) {
            processedLines =
                processedLines.map { if (it.isNotBlank()) it.prependIndent(indentationWhiteSpaces) else it }
        }
        return processedLines.joinToString("\n")
    }

    private fun String.calcIndent() = indexOfFirst { !it.isWhitespace() }

    companion object {
        val indentationWhiteSpaces = " ".repeat(4)
    }
}
