package com.huawei.cangjie

import com.huawei.cangjie.lang.CangJieLanguage
import com.huawei.cangjie.lexer.CangJieLexer
import com.huawei.cangjie.lexer.CjTokens
import com.huawei.cangjie.parsing.CangJieParser
import com.huawei.cangjie.psi.CjBlockExpression
import com.intellij.lang.ASTNode
import com.intellij.lang.Language
import com.intellij.lang.PsiBuilderFactory
import com.intellij.openapi.project.Project
import com.intellij.psi.tree.ICompositeElementType
import com.intellij.psi.tree.IErrorCounterReparseableElementType


class BlockExpressionElementType : IErrorCounterReparseableElementType("BLOCK", CangJieLanguage),
    ICompositeElementType {

    override fun createCompositeNode() = CjBlockExpression(null)

    override fun createNode(text: CharSequence?) = CjBlockExpression(text)

    override fun isParsable(parent: ASTNode?, buffer: CharSequence, fileLanguage: Language, project: Project) =
        fileLanguage == CangJieLanguage &&
                isAllowedParentNode(parent) &&
                isReparseableBlock(buffer) &&
                super.isParsable(buffer, fileLanguage, project)

    override fun getErrorsCount(seq: CharSequence, fileLanguage: Language, project: Project) =
        ElementTypeUtils.getCangJieBlockImbalanceCount(seq)

    override fun parseContents(chameleon: ASTNode): ASTNode {
        val project = chameleon.psi.project
        val builder = PsiBuilderFactory.getInstance().createBuilder(
            project, chameleon, null, CangJieLanguage, chameleon.chars
        )

        return CangJieParser.parseBlockExpression(builder).firstChildNode
    }

    companion object {

        private fun isAllowedParentNode(node: ASTNode?) =
            node != null



        fun isReparseableBlock(blockText: CharSequence): Boolean {

            fun advanceWhitespacesCheckIsEndOrArrow(lexer: CangJieLexer): Boolean {
                lexer.advance()
                while (lexer.tokenType != null && lexer.tokenType != CjTokens.EOF) {
                    if (lexer.tokenType == CjTokens.ARROW) return true
                    if (lexer.tokenType != CjTokens.WHITE_SPACE) return false
                    lexer.advance()
                }
                return true
            }

            val lexer = CangJieLexer()
            lexer.start(blockText)

            //尝试解析后面跟一个箭头的简单名称列表
            //   {a -> ...}
            //   {a, b -> ...}
            //   {(a, b) -> ... }
            if (lexer.tokenType != CjTokens.LBRACE) return false

            if (advanceWhitespacesCheckIsEndOrArrow(lexer)) return false

            if (lexer.tokenType != CjTokens.COLON &&
                lexer.tokenType != CjTokens.IDENTIFIER &&
                lexer.tokenType != CjTokens.LPAR
            ) return true

            val searchForRPAR = lexer.tokenType == CjTokens.LPAR

            if (advanceWhitespacesCheckIsEndOrArrow(lexer)) return false

            val preferParamsToExpressions = lexer.tokenType == CjTokens.COMMA || lexer.tokenType == CjTokens.COLON

            while (true) {

                if (lexer.tokenType == CjTokens.LBRACE) return true
                if (lexer.tokenType == CjTokens.RBRACE) return !preferParamsToExpressions

                if (searchForRPAR && lexer.tokenType == CjTokens.RPAR) {
                    return !advanceWhitespacesCheckIsEndOrArrow(lexer)
                }

                if (advanceWhitespacesCheckIsEndOrArrow(lexer)) return false
            }
        }
    }
}
