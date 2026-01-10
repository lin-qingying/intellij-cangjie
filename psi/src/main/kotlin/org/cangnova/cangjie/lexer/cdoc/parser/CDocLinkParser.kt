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

package org.cangnova.cangjie.lexer.cdoc.parser

import org.cangnova.cangjie.lexer.CangJieLexer
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.lexer.CjTokens.UNDERLINE
import com.intellij.lang.ASTNode
import com.intellij.lang.PsiBuilder
import com.intellij.lang.PsiBuilderFactory
import com.intellij.lang.PsiParser
import com.intellij.psi.tree.IElementType

/**
 * 分析 CDOC 中的 Markdown 链接的内容。使用标准的 CangJie 词法分析器。
 */
class CDocLinkParser : PsiParser {
    companion object {
        @JvmStatic
        fun parseMarkdownLink(root: IElementType, chameleon: ASTNode): ASTNode {
            val parentElement = chameleon.treeParent.psi
            val project = parentElement.project
            val builder = PsiBuilderFactory.getInstance().createBuilder(
                project,
                chameleon,
                CangJieLexer(),
                root.language,
                chameleon.text,
            )
            val parser = CDocLinkParser()

            return parser.parse(root, builder).firstChildNode
        }
    }

    override fun parse(root: IElementType, builder: PsiBuilder): ASTNode {
        val rootMarker = builder.mark()
        val hasLBracket = builder.tokenType == CjTokens.LBRACKET
        if (hasLBracket) {
            builder.advanceLexer()
        }
        parseQualifiedName(builder)
        if (hasLBracket) {
            if (!builder.eof() && builder.tokenType != CjTokens.RBRACKET) {
                builder.error("Closing bracket expected")
                while (!builder.eof() && builder.tokenType != CjTokens.RBRACKET) {
                    builder.advanceLexer()
                }
            }
            if (builder.tokenType == CjTokens.RBRACKET) {
                builder.advanceLexer()
            }
        } else {
            if (!builder.eof()) {
                builder.error("Expression expected")
                while (!builder.eof()) {
                    builder.advanceLexer()
                }
            }
        }
        rootMarker.done(root)
        return builder.treeBuilt
    }

    private fun parseQualifiedName(builder: PsiBuilder) {
        var marker = builder.mark()
        while (true) {
            // 如果链接中的某个单词恰好是CangJie关键字，则不要将其突出显示为错误
            if (!isName(builder.tokenType) && builder.tokenType != UNDERLINE) {
                marker.drop()
                builder.error("Identifier expected")
                break
            }
            builder.advanceLexer()
            marker.done(CDocElementTypes.CDOC_NAME)
            if (builder.tokenType != CjTokens.DOT) {
                break
            }
            marker = marker.precede()
            builder.advanceLexer()
        }
    }

    private fun isName(tokenType: IElementType?) = tokenType == CjTokens.IDENTIFIER || tokenType in CjTokens.KEYWORDS
}
