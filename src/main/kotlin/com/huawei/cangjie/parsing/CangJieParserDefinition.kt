package com.huawei.cangjie.parsing

import com.huawei.cangjie.doc.lexer.CDocTokens
import com.huawei.cangjie.CjNodeType
import com.huawei.cangjie.lexer.CangJieLexer
import com.huawei.cangjie.psi.stubs.elements.CjStubElementType
import com.huawei.cangjie.lang.CangJieLanguage
import com.huawei.cangjie.psi.CjFile
import com.huawei.cangjie.lexer.CjToken
import com.huawei.cangjie.lexer.CjTokens
import com.huawei.cangjie.psi.stubs.elements.CjFileElementType
import com.intellij.lang.ASTNode
import com.intellij.lang.LanguageParserDefinitions
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

class CangJieParserDefinition : ParserDefinition {
    override fun createLexer(project: Project?): Lexer = CangJieLexer()

    override fun createParser(project: Project?): PsiParser = CangJieParser(project!!)

    override fun getFileNodeType(): IFileElementType = CjFileElementType.INSTANCE


    val EOL_COMMENT = CjToken("EOL_COMMENT", 4)
    val BLOCK_COMMENT = CjToken("BLOCK_COMMENT", 3)
    val DOC_COMMENT: IElementType = CDocTokens.CDOC

    val SHEBANG_COMMENT = CjToken("SHEBANG_COMMENT", 5)
    val COMMENTS = TokenSet.create(
        EOL_COMMENT,
        BLOCK_COMMENT,
        DOC_COMMENT,
        SHEBANG_COMMENT
    )

    override fun getCommentTokens(): TokenSet = CjTokens.COMMENTS


    override fun getWhitespaceTokens(): TokenSet = CjTokens.WHITESPACES

    override fun getStringLiteralElements(): TokenSet = TokenSet.EMPTY


    override fun createElement(node: ASTNode): PsiElement {
        val elementType = node.elementType


        return when (elementType) {
            is CjStubElementType<*, *> ->
                elementType.createPsiFromAst(node)
            //TODO 代码片段类型元素
            //TODO 文档类型元素

            //关键字

            else -> (elementType as CjNodeType).createPsi(node)
        }
    }

    override fun createFile(viewProvider: FileViewProvider): PsiFile = CjFile(viewProvider, false)




    object Util {
        @JvmField
        val STD_SCRIPT_SUFFIX = "cj"

        @JvmField
        val STD_SCRIPT_EXT = "." + STD_SCRIPT_SUFFIX
        val instance: CangJieParserDefinition
            get() = LanguageParserDefinitions.INSTANCE.forLanguage(CangJieLanguage) as CangJieParserDefinition
    }

}
