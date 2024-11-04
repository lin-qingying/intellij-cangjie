package com.linqingying.cangjie.doc.parser

import com.linqingying.cangjie.lexer.CangJieLexer
import com.linqingying.cangjie.lexer.CjTokens
import com.linqingying.cangjie.lexer.CjTokens.UNDERLINE
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
                chameleon.text
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
