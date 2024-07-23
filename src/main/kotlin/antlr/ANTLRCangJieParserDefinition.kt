//package antlr
//
//import antlr.parser.CangJieLexer
//import antlr.parser.CangJieParser
//import antlr.psi.CangJiePSIFileRoot
//import com.huawei.cangjie.lang.CangJieLanguage
//import com.intellij.lang.ASTNode
//import com.intellij.lang.ParserDefinition
//import com.intellij.lang.PsiParser
//import com.intellij.lexer.Lexer
//import com.intellij.openapi.project.Project
//import com.intellij.psi.FileViewProvider
//import com.intellij.psi.PsiElement
//import com.intellij.psi.PsiFile
//import com.intellij.psi.tree.IElementType
//import com.intellij.psi.tree.IFileElementType
//import com.intellij.psi.tree.TokenSet
//import org.antlr.intellij.adaptor.lexer.ANTLRLexerAdaptor
//import org.antlr.intellij.adaptor.lexer.PSIElementTypeFactory
//import org.antlr.intellij.adaptor.lexer.RuleIElementType
//import org.antlr.intellij.adaptor.lexer.TokenIElementType
//import org.antlr.intellij.adaptor.parser.ANTLRParserAdaptor
//import org.antlr.intellij.adaptor.psi.ANTLRPsiNode
//import org.antlr.v4.runtime.Parser
//import org.antlr.v4.runtime.tree.ParseTree
//
//class ANTLRCangJieParserDefinition : ParserDefinition {
//
//    init {
//
//        PSIElementTypeFactory.defineLanguageIElementTypes(
//            CangJieLanguage,
//            CangJieParser.tokenNames,
//            CangJieParser.ruleNames
//        )
//
//    }
//
//    val Identifier: TokenIElementType =
//        PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage)[CangJieLexer.Identifier]
//
//    val COMMENTS: TokenSet = PSIElementTypeFactory.createTokenSet(
//        CangJieLanguage,
//        CangJieLexer.DelimitedComment,
//        CangJieLexer.LineComment
//    )
//
//    val WHITESPACE: TokenSet = PSIElementTypeFactory.createTokenSet(
//        CangJieLanguage,
//        CangJieLexer.WS
//    )
//
//    val STRING: TokenSet = PSIElementTypeFactory.createTokenSet(
//        CangJieLanguage,
//        CangJieLexer.JStringLiteral
//    )
//
//    override fun createLexer(p0: Project?): Lexer {
//        val lexer = CangJieLexer(null)
//        return ANTLRLexerAdaptor(CangJieLanguage, lexer)
//    }
//
//    override fun spaceExistenceTypeBetweenTokens(left: ASTNode?, right: ASTNode?): ParserDefinition.SpaceRequirements =
//        ParserDefinition.SpaceRequirements.MAY
//    override fun getWhitespaceTokens(): TokenSet  = WHITESPACE
//    override fun createParser(p0: Project?): PsiParser {
//        val parser = CangJieParser(null)
//        return object : ANTLRParserAdaptor(CangJieLanguage, parser) {
//            override fun parse(parser: Parser, root: IElementType): ParseTree {
//                // start rule depends on root passed in; sometimes we want to create an ID node etc...
//
//                return (parser as CangJieParser).translationUnit()
//
//                // let's hope it's an ID as needed by "rename function"
//
//            }
//        }
//    }
//
//    override fun getFileNodeType(): IFileElementType {
//        return IFileElementType(CangJieLanguage)
//
//    }
//
//    override fun getCommentTokens(): TokenSet {
//        return COMMENTS
//    }
//
//    override fun getStringLiteralElements(): TokenSet {
//        return STRING
//
//    }
//
//    override fun createElement(node: ASTNode): PsiElement {
//        val elType: IElementType = node.elementType
//        if (elType is TokenIElementType) {
//            return ANTLRPsiNode(node)
//        }
//        if (elType !is RuleIElementType) {
//            return ANTLRPsiNode(node)
//        }
//
//        val ruleElType = elType
//
//        return when (ruleElType.ruleIndex) {
//            else -> ANTLRPsiNode(node)
//        }
//
//    }
//
//    override fun createFile(viewProvider: FileViewProvider): PsiFile {
//        return CangJiePSIFileRoot(viewProvider)
//
//    }
//}
