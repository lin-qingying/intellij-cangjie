package com.huawei.cangjie1.doc.lexer

import com.huawei.cangjie1.doc.parser.CDocParser
import com.huawei.cangjie1.doc.psi.impl.CDocImpl
import com.huawei.cangjie1.lang.CangJieLanguage
import com.intellij.lang.ASTNode
import com.intellij.lang.PsiBuilderFactory
import com.intellij.lang.PsiParser
import com.intellij.psi.tree.ILazyParseableElementType


interface CDocTokens {
    companion object {
        val CDOC: ILazyParseableElementType = object : ILazyParseableElementType("CDoc", CangJieLanguage) {
            override fun parseContents(chameleon: ASTNode): ASTNode {
                val parentElement = chameleon.treeParent.psi
                val project = parentElement.project
                val builder = PsiBuilderFactory.getInstance().createBuilder(
                    project, chameleon, CDocLexer(),
                    language,
                    chameleon.text
                )
                val parser: PsiParser = CDocParser()
                return parser.parse(this, builder).firstChildNode
            }

            override fun createNode(text: CharSequence): ASTNode {
                return CDocImpl(text)
            }
        }
        const val START_Id = 0
        const val END_Id = 1
        const val LEADING_ASTERISK_Id = 2
        const val TEXT_ID = 3
        const val CODE_BLOCK_TEXT_ID = 4
        const val TAG_NAME_Id = 5
        const val MARKDOWN_ESCAPED_CHAR_Id = 6
        const val MARKDOWN_INLINE_LINK_Id = 7
        val CODE_BLOCK_TEXT: CDocToken = CDocToken("KDOC_CODE_BLOCK_TEXT", CODE_BLOCK_TEXT_ID)

        val TEXT: CDocToken = CDocToken("KDOC_TEXT", TEXT_ID)
    }
}
