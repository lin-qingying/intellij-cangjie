package com.huawei.cangjie.lang.core.parser
//


import com.huawei.cangjie.lang.CjLanguage
import com.huawei.cangjie.lang.core.lexer.CangJieLexer
import com.huawei.cangjie.lang.core.psi.CJ_COMMENTS

import com.huawei.cangjie.lang.core.psi.CjElementTypes
import com.huawei.cangjie.lang.core.psi.CjFile
import com.huawei.cangjie.lang.core.psi.CjTokenType
import com.intellij.lang.ASTNode
import com.intellij.lang.ParserDefinition
import com.intellij.lang.PsiParser
import com.intellij.lexer.Lexer
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet


class CangJieParserDefinition : ParserDefinition {
    override fun createLexer(project: Project?): Lexer = CangJieLexer()

    override fun createParser(project: Project?): PsiParser = CangJieParser()

    override fun getFileNodeType(): IFileElementType = IFileElementType(CjLanguage)

    override fun getCommentTokens(): TokenSet = CJ_COMMENTS


    override fun getWhitespaceTokens(): TokenSet =
        TokenSet.create(TokenType.WHITE_SPACE)

    override fun getStringLiteralElements(): TokenSet = TokenSet.EMPTY


    override fun createElement(node: ASTNode): PsiElement {
        return CjElementTypes.Factory.createElement(node)
    }

    override fun createFile(viewProvider: FileViewProvider): PsiFile = CjFile(viewProvider)

    companion object {
        @JvmField
        val BLOCK_COMMENT = CjTokenType("<BLOCK_COMMENT>")

        @JvmField
        val EOL_COMMENT = CjTokenType("<EOL_COMMENT>")


        /**
         * Should be increased after any change of lexer rules
         */
        const val LEXER_VERSION: Int = 6

        /**
         * Should be increased after any change of parser rules
         */
        const val PARSER_VERSION: Int = LEXER_VERSION + 51
    }
}
