package com.linqingying.cangjie.parsing

import com.linqingying.cangjie.CjNodeType
import com.linqingying.cangjie.doc.lexer.CDocTokens
import com.linqingying.cangjie.doc.parser.CDocElementType
import com.linqingying.cangjie.doc.psi.impl.CDocLink
import com.linqingying.cangjie.lang.CangJieLanguage
import com.linqingying.cangjie.lang.declarations.CangJieDeclarationsFileType
import com.linqingying.cangjie.lang.declarations.CjDeclarationsFile
import com.linqingying.cangjie.lexer.CangJieLexer
import com.linqingying.cangjie.lexer.CjToken
import com.linqingying.cangjie.lexer.CjTokens
import com.linqingying.cangjie.psi.CjFile
import com.linqingying.cangjie.psi.stubs.elements.CjFileElementType
import com.linqingying.cangjie.psi.stubs.elements.CjStubElementType
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

class CangJieParserDefinitionByBnf : ParserDefinition {
    override fun createLexer(project: Project?): Lexer = CangJieLexer()

    override fun createParser(project: Project?): PsiParser {
        return CangJieParserByBnf()
    }


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
        return when (val elementType = node.elementType) {
            is CjStubElementType<*, *> ->
                elementType.createPsiFromAst(node)


            is CDocElementType -> elementType.createPsi(node)
            CDocTokens.MARKDOWN_LINK -> CDocLink(node)
            else -> (elementType as CjNodeType).createPsi(node)
        }
    }


    override fun createFile(viewProvider: FileViewProvider): PsiFile {




        return CjFile(viewProvider, false)
    }


    object Util {
        @JvmField
        val STD_SCRIPT_SUFFIX = "cj1"

        @JvmField
        val STD_SCRIPT_EXT = ".$STD_SCRIPT_SUFFIX"
        val instance: CangJieParserDefinition
            get() = LanguageParserDefinitions.INSTANCE.forLanguage(CangJieLanguage) as CangJieParserDefinition
    }
}

class CangJieParserDefinition : ParserDefinition {
    override fun createLexer(project: Project?): Lexer = CangJieLexer()

    override fun createParser(project: Project?): PsiParser = CangJieParser(project!!)


    /**
     *  如果使用bnf 需要把 [com.linqingying.cangjie.psi.stubs.elements.CjFileElementType]的[doParseContents]方法注释掉
     */
//    override fun createLexer(p0: Project?): Lexer {
//     return FlexAdapter(_CangJieLexer())
//    }
//    override fun createParser(p0: Project?): PsiParser {
//        return CangJieParser()
//    }

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
        return when (val elementType = node.elementType) {
            is CjStubElementType<*, *> ->
                elementType.createPsiFromAst(node)


            is CDocElementType -> elementType.createPsi(node)
            CDocTokens.MARKDOWN_LINK -> CDocLink(node)
            else -> (elementType as CjNodeType).createPsi(node)
        }
    }


    override fun createFile(viewProvider: FileViewProvider): PsiFile {

//        if(viewProvider is CangJieFileViewProvider){
//
//            return CjDeclarationsFile(viewProvider)
//        }

        if (viewProvider.fileType is CangJieDeclarationsFileType) {
            return CjDeclarationsFile(viewProvider)
        }



        return CjFile(viewProvider, false)
    }


    object Util {
        @JvmField
        val STD_SCRIPT_SUFFIX = "cj"

        @JvmField
        val STD_SCRIPT_EXT = ".$STD_SCRIPT_SUFFIX"
        val instance: CangJieParserDefinition
            get() = LanguageParserDefinitions.INSTANCE.forLanguage(CangJieLanguage) as CangJieParserDefinition
    }

}
