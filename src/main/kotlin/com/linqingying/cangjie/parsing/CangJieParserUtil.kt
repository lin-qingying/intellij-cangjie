package com.linqingying.cangjie.parsing

import com.linqingying.cangjie.CjNodeTypes.DOT_QUALIFIED_EXPRESSION
import com.linqingying.cangjie.CjNodeTypes.REFERENCE_EXPRESSION
import com.linqingying.cangjie.lexer.CjKeywordToken
import com.linqingying.cangjie.lexer.CjToken
import com.linqingying.cangjie.lexer.CjTokens.*
import com.linqingying.cangjie.parsing.CangJieParserUtil.parseStringTemplate
import com.intellij.lang.PsiBuilder
import com.intellij.lang.parser.GeneratedParserUtilBase
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import com.intellij.util.containers.Stack

object CangJieParserUtil : GeneratedParserUtilBase() {

    val PACKAGE_NAME_RECOVERY_SET: TokenSet = TokenSet.create(DOT, EOL_OR_SEMICOLON)

    private val SOFT_KEYWORD_TEXTS: MutableMap<String, CjKeywordToken> = HashMap()

    init {
        for (type in SOFT_KEYWORDS.types) {
            val keywordToken = type as CjKeywordToken
            assert(keywordToken.isSoft)
            SOFT_KEYWORD_TEXTS[keywordToken.value] = keywordToken
        }
    }

    /**
     * 查看标记流中的下 k 个标记，而不会推进标记流
     *
     * @param k
     * @return
     */
    fun PsiBuilder.lookahead(k: Int): IElementType? {
        return lookAhead(k)
    }

    private fun PsiBuilder.tokenMatches(token: IElementType?, expectation: IElementType): Boolean {
        if (token === expectation) return true
        if (expectation === EOL_OR_SEMICOLON) {
            if (eof()) return true
            if (token === SEMICOLON) return true
            return newlineBeforeCurrentToken()
        }
        return false
    }

    /**
     * 无副作用版本的at()
     */
    fun PsiBuilder._at(expectation: IElementType): Boolean {
        val token = tt()
        return tokenMatches(token, expectation)
    }

    /**
     * 获取当前标记的类型
     *
     * @return
     */
    fun PsiBuilder.tt(): IElementType? {
        return tokenType
    }

    fun PsiBuilder.at(expectation: IElementType): Boolean {
        if (_at(expectation)) return true
        val token = tt()
        if (token === IDENTIFIER && expectation is CjKeywordToken) {
            if (expectation.isSoft && expectation.value == tokenText) {
                remapCurrentToken(expectation)
                return true
            }
        }
        if (expectation === IDENTIFIER && token is CjKeywordToken) {
            if (token.isSoft) {
                remapCurrentToken(IDENTIFIER)
                return true
            }
        }
        return false
    }

    /**
     * 检查当前标记是否为指定的复杂标记类型，并在标记不匹配时报告错误
     *
     * @param expectation
     * @param message
     * @param recoverySet
     * @return
     */
    fun PsiBuilder.expect(expectation: CjToken, message: String, recoverySet: TokenSet?): Boolean {
        if (expect(expectation)) {
            return true
        }

        errorWithRecovery(message, recoverySet)

        return false
    }

    fun PsiBuilder.errorAndAdvance(message: String) {
        errorAndAdvance(message, 1)
    }

    fun PsiBuilder.advance(advanceTokenCount: Int) {
        for (i in 0 until advanceTokenCount) {
            advance() // 错误的令牌
        }
    }

    fun PsiBuilder.errorAndAdvance(message: String, advanceTokenCount: Int) {
        val err: PsiBuilder.Marker = mark()
        advance(advanceTokenCount)
        err.error(message)
    }

    fun PsiBuilder.errorWithRecovery(message: String, recoverySet: TokenSet?) {
        val tt = tt()
        if (recoverySet == null ||
            recoverySet.contains(tt) ||  //                tt == LBRACE || tt == RBRACE ||
            (recoverySet.contains(EOL_OR_SEMICOLON) && (eof() || tt === SEMICOLON || newlineBeforeCurrentToken()))
        ) {
            error(message)
        } else {
            errorAndAdvance(message)
        }
    }

    fun PsiBuilder.expect(expectation: CjToken): Boolean {
        if (at(expectation)) {
            advance()
            return true
        }

        if (expectation === IDENTIFIER && "`" == tokenText) {
            advance()
        }

        return false
    }


    private val joinComplexTokens = Stack<Boolean>()
    private val newlinesEnabled = Stack<Boolean>()


    init {
        newlinesEnabled.push(true)
        joinComplexTokens.push(true)

    }

    fun PsiBuilder.newlineBeforeCurrentToken(): Boolean {


        if (!newlinesEnabled.peek()) return false

        if (eof()) return true


        for (i in 1..currentOffset) {
            val previousToken = rawLookup(-i)





            if (previousToken === BLOCK_COMMENT || previousToken === DOC_COMMENT || previousToken === EOL_COMMENT || previousToken === SHEBANG_COMMENT) {
                continue
            }
            if (previousToken !== TokenType.WHITE_SPACE) {
                break
            }
            val previousTokenStart = rawTokenTypeStart(-i)
            val previousTokenEnd = rawTokenTypeStart(-i + 1)
            assert(previousTokenStart >= 0)
            assert(previousTokenEnd < originalText.length)
            for (j in previousTokenStart until previousTokenEnd) {
                if (originalText[j] == '\n') {
                    return true
                }
            }
        }
        return false
    }

    fun PsiBuilder.disableNewlines() {
        newlinesEnabled.push(false)
    }

    fun PsiBuilder.enableNewlines() {
        newlinesEnabled.push(true)
    }

    fun PsiBuilder.restoreNewlinesState() {
        assert(newlinesEnabled.size > 1)
        newlinesEnabled.pop()
    }

    private fun PsiBuilder.joinComplexTokens(): Boolean {
        return joinComplexTokens.peek()
    }

    /**
     * 推进标记流并返回下一个标记
     */
    fun PsiBuilder.advance() {
        advanceLexer()
    }

    @JvmStatic
    fun PsiBuilder.packageName(level: Int): Boolean {
        var qualifiedExpression = mark()
        var simpleName = true
        while (true) {
            if (newlineBeforeCurrentToken()) {
                errorWithRecovery(
                    "Package name must be a '.'-separated identifier list placed on a single line",
                    PACKAGE_NAME_RECOVERY_SET
                )
                break
            }

            if (at(DOT)) {
                advance() // DOT
                qualifiedExpression.error("Package name must be a '.'-separated identifier list")
                qualifiedExpression = mark()
                continue
            }

            val nsName: PsiBuilder.Marker = mark()
            val simpleNameFound: Boolean = expect(
                IDENTIFIER,
                "Package name must be a '.'-separated identifier list",
                PACKAGE_NAME_RECOVERY_SET
            )
            if (simpleNameFound) {
                nsName.done(REFERENCE_EXPRESSION)
            } else {
                nsName.drop()
            }

            if (!simpleName) {
                val precedingMarker = qualifiedExpression.precede()
                qualifiedExpression.done(DOT_QUALIFIED_EXPRESSION)
                qualifiedExpression = precedingMarker
            }

            if (at(DOT)) {
                advance() // DOT

                if (simpleName && !simpleNameFound) {
                    qualifiedExpression.drop()
                    qualifiedExpression = mark()
                } else {
                    simpleName = false
                }
            } else {
                break
            }
        }
        qualifiedExpression.drop()



        return true
    }

    @JvmStatic
    fun PsiBuilder.parseStringTemplate(level: Int): Boolean {
    if(this.tokenType != OPEN_QUOTE ) return false

        val cp = CangJieParsing.createForTopLevelNonLazy(SemanticWhitespaceAwarePsiBuilderImpl(this) )
        cp.expressionParsing.parseStringTemplate()
        return true
    }

    @JvmStatic
    fun constKeyword(builder: PsiBuilder, level: Int): Boolean {
        val token = builder.tokenType
        if (token == CONST_KEYWORD &&  /* lookahead(1) != IDENTIFIER && */builder.lookahead(2) !== EQ && builder.lookahead(
                2
            ) !== COLON
        ) {
            builder.advanceLexer()
            return true
        }
        return false
    }

    @JvmStatic
    fun sealedKeyword(b: PsiBuilder, level: Int): Boolean =
        softKeyword(b, SEALED_KEYWORD)

    @JvmStatic
    fun abstractKeyword(b: PsiBuilder, level: Int): Boolean =
        softKeyword(b, ABSTRACT_KEYWORD)

    @JvmStatic
    fun openKeyword(b: PsiBuilder, level: Int): Boolean =
        softKeyword(b, OPEN_KEYWORD)

    private fun softKeyword(
        builder: PsiBuilder,

        elementType: IElementType,

        ): Boolean {

        val token = builder.tokenType
        if (token === IDENTIFIER) {
            val keywordToken = SOFT_KEYWORD_TEXTS[builder.tokenText]
            if (elementType == keywordToken) {
                keywordToken.let { builder.remapCurrentToken(it) }

                builder.advanceLexer()
                return true
            }
        }
        return false
    }
}
