package com.huawei.cangjie.lang.core.parser
//


import com.huawei.cangjie.lang.CjLanguage
import com.huawei.cangjie.lang.core.lexer.CangJieLexer
import com.huawei.cangjie.lang.core.psi.*
import com.huawei.cangjie.lang.doc.psi.CjDocCommentElementType

import com.intellij.lang.ASTNode
import com.intellij.lang.ParserDefinition
import com.intellij.lang.PsiParser
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.lexer.Lexer
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
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

    override fun createFile(viewProvider: FileViewProvider): PsiFile {
        val default = { CjFile(viewProvider) }

//        val project = viewProvider.manager.project
//        val injectionHost = InjectedLanguageManager.getInstance(project).getInjectionHost(viewProvider)

//        if (injectionHost != null) {
//            // this class is contained in clion.jar, so it cannot be used inside `is` type check
//            if (injectionHost.javaClass.simpleName != "GDBExpressionPlaceholder") {
//                return default()
//            }

//            val injectionListener = project.messageBus.syncPublisher(CjDebugInjectionListener.INJECTION_TOPIC)
//            val contextResult = CjDebugInjectionListener.DebugContext()
//            injectionListener.evalDebugContext(injectionHost, contextResult)
//            val context = contextResult.element ?: return default()

//            val fragment = CjDebuggerExpressionCodeFragment(viewProvider, context)
//            injectionListener.didInject(injectionHost)
//
//            return fragment
//        } else if (viewProvider.virtualFile.name == CjConsoleView.VIRTUAL_FILE_NAME) {
//            val context = CjConsoleCodeFragmentContext.createContext(project, null)
//            return CjReplCodeFragment(viewProvider, context)
//        }
        return default()
    }

    companion object {
        @JvmField  val BLOCK_COMMENT = CjTokenType("<BLOCK_COMMENT>")

        @JvmField  val EOL_COMMENT = CjTokenType("<EOL_COMMENT>")

        @JvmField val INNER_BLOCK_DOC_COMMENT = CjDocCommentElementType("<INNER_BLOCK_DOC_COMMENT>")
        @JvmField val OUTER_BLOCK_DOC_COMMENT = CjDocCommentElementType("<OUTER_BLOCK_DOC_COMMENT>")
        @JvmField val INNER_EOL_DOC_COMMENT = CjDocCommentElementType("<INNER_EOL_DOC_COMMENT>")
        @JvmField val OUTER_EOL_DOC_COMMENT = CjDocCommentElementType("<OUTER_EOL_DOC_COMMENT>")

        const val LEXER_VERSION: Int = 6



        const val PARSER_VERSION: Int = LEXER_VERSION + 51
    }
}
