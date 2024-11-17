/*
 * Copyright 2024 LinQingYing. and contributors.
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

package com.linqingying.cangjie.parsing

import com.linqingying.cangjie.lexer.CjKeywordToken
import com.linqingying.cangjie.lexer.CjToken
import com.linqingying.cangjie.lexer.CjTokens
import com.linqingying.cangjie.utils.substringWithContext
import com.intellij.lang.PsiBuilder
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import com.intellij.util.containers.Stack
import org.jetbrains.annotations.TestOnly

abstract class AbstractCangJieParsing1(val builder: SemanticWhitespaceAwarePsiBuilder, val isLazy: Boolean = true) {


    /**
     * 获取标记流中的最后一个标记类型
     *
     * @return
     */
    protected fun getLastToken(): IElementType? {
        var i = 1
        val currentOffset: Int = builder.currentOffset
        while (i <= currentOffset && CjTokens.WHITE_SPACE_OR_COMMENT_BIT_SET.contains(builder.rawLookup(-i))) {
            i++
        }
        return builder.rawLookup(-i)
    }

    /**
     * 检查当前标记是否为指定的 CjToken 类型，并在标记不匹配时报告错误
     *
     * @param expectation
     * @param message
     * @return
     */
    protected fun expect(expectation: CjToken, message: String): Boolean {
        return expect(expectation, message, null)
    }

    /**
     * 在标记流中创建一个标记，并返回该标记的 PsiBuilder.Marker 对象
     *
     * @return
     */
    protected fun mark(): PsiBuilder.Marker {
        return builder.mark()
    }

    /**
     * 报告解析错误并停止解析过程
     *
     * @param message
     */
    protected fun error(message: String) {
        builder.error(message)
    }

    protected fun errorBefore(message: String?, marker: PsiBuilder.Marker?) {
////        PsiBuilder.Marker err = marker.precede();
//
//        marker.error(message);
//
////        err.done(ERROR_ELEMENT);
    }

    /**
     * 检查当前标记是否为指定的复杂标记类型，并在标记不匹配时报告错误
     *
     * @param expectation
     * @param message
     * @param recoverySet
     * @return
     */
    protected fun expect(expectation: CjToken, message: String, recoverySet: TokenSet?): Boolean {
        if (expect(expectation)) {
            return true
        }

        errorWithRecovery(message, recoverySet)

        return false
    }

    /**
     * 检查当前标记是否为指定的 CjToken 类型，并在标记不匹配时报告错误
     *
     * @param expectation
     * @return
     */
    protected fun expect(expectation: CjToken): Boolean {
        if (at(expectation)) {
            advance()
            return true
        }

        if (expectation === CjTokens.IDENTIFIER && "`" == builder.tokenText) {
            advance()
        }

        return false
    }

    /**
     * 检查当前标记是否为指定的 CjToken 类型，但不会消耗标记流中的标记
     *
     * @param expectation
     * @param message
     */
    protected fun expectNoAdvance(expectation: CjToken, message: String) {
        if (at(expectation)) {
            advance()
            return
        }

        error(message)
    }

    /**
     * 报告解析错误并尝试恢复解析过程
     *
     * @param message
     * @param recoverySet
     */
    protected fun errorWithRecovery(message: String, recoverySet: TokenSet?) {
        val tt: IElementType? = tt()
        if (recoverySet == null ||
            recoverySet.contains(tt) || tt === CjTokens.LBRACE || tt === CjTokens.RBRACE ||
            (recoverySet.contains(CjTokens.EOL_OR_SEMICOLON) && (eof() || tt === CjTokens.SEMICOLON || builder.newlineBeforeCurrentToken()))
        ) {
            error(message)
        } else {
            errorAndAdvance(message)
        }
    }

    /**
     * 报告解析错误并消耗当前标记
     *
     * @param message
     */
    protected fun errorAndAdvance(message: String) {
        errorAndAdvance(message, 1)
    }

    /**
     * 报告解析错误并消耗指定数量的标记
     *
     * @param message
     * @param advanceTokenCount
     */
    protected fun errorAndAdvance(message: String, advanceTokenCount: Int) {
        val err = mark()
        advance(advanceTokenCount)
        err.error(message)
    }


    /**
     * 报告错误，但不消耗标记
     */
    protected fun errorWithoutAdvancing(message: String) {
        mark().error(message)
    }


    /**
     * 是否到文件结尾
     *
     * @return
     */
    protected fun eof(): Boolean {
        return builder.eof()
    }

    /**
     * 推进标记流并返回下一个标记
     */
    protected fun advance() {
        // TODO: 如何在错误字符上报告错误(除突出显示外)
        builder.advanceLexer()
    }

    /**
     * 推进标记流并返回下  advanceTokenCount 个标记
     *
     * @param advanceTokenCount
     */
    protected fun advance(advanceTokenCount: Int) {
        for (i in 0 until advanceTokenCount) {
            advance() // 错误的令牌
        }
    }

    /**
     * 推进标记流并返回下一个指定类型的标记
     *
     * @param current
     */
    protected fun advanceAt(current: IElementType) {
        assert(_at(current))
        builder.advanceLexer()
    }

    /**
     * 获取当前标记的类型id
     *
     * @return
     */
    protected fun getTokenId(): Int {
        val elementType = tt()
        return if ((elementType is CjToken)) elementType.tokenId else CjTokens.INVALID_Id
    }


    /**
     * 获取当前标记的类型
     *
     * @return
     */
    protected fun tt(): IElementType? {
        return builder.tokenType
    }


    protected fun rawLookup(steps: Int): IElementType? {
        return builder.rawLookup(steps)
    }

    /**
     * 无副作用版本的at()
     */
    protected fun _at(expectation: IElementType): Boolean {
        val token = tt()
        return token?.let { tokenMatches(it, expectation) } == true
    }

    /**
     * 检查当前标记是否与预期标记匹配
     *
     * @param token
     * @param expectation
     * @return
     */
    private fun tokenMatches(token: IElementType, expectation: IElementType): Boolean {
        if (token === expectation) return true
        if (expectation === CjTokens.EOL_OR_SEMICOLON) {
            if (eof()) return true
            if (token === CjTokens.SEMICOLON) return true
            return builder.newlineBeforeCurrentToken()
        }
        return false
    }

    /**
     * 检查当前标记是否与预期标记匹配
     *
     * @param expectation
     * @return
     */
    protected fun at(expectation: IElementType): Boolean {
        if (_at(expectation)) return true
        val token = tt()
        if (token === CjTokens.IDENTIFIER && expectation is CjKeywordToken) {
            if (expectation.isSoft && expectation.value == builder.tokenText) {
                builder.remapCurrentToken(expectation)
                return true
            }
        }
        if (expectation === CjTokens.IDENTIFIER && token is CjKeywordToken) {
            if (token.isSoft) {
                builder.remapCurrentToken(CjTokens.IDENTIFIER)
                return true
            }
        }
        return false
    }

    /**
     * 检查当前标记是否与预期标记匹配
     *
     * @param set
     * @return
     */
    protected fun _atSet(set: TokenSet): Boolean {
        val token = tt()
        if (set.contains(token)) return true
        if (set.contains(CjTokens.EOL_OR_SEMICOLON)) {
            if (eof()) return true
            if (token === CjTokens.SEMICOLON) return true
            return builder.newlineBeforeCurrentToken()
        }
        return false
    }

    /**
     * 检查当前标记是否与预期标记匹配
     *
     * @param set
     * @return
     */
    protected fun atSet(set: TokenSet): Boolean {
        if (_atSet(set)) return true
        val token = tt()
        if (token === CjTokens.IDENTIFIER) {
            val keywordToken = SOFT_KEYWORD_TEXTS[builder.tokenText]
            if (set.contains(keywordToken)) {
                keywordToken?.let { builder.remapCurrentToken(it) }
                return true
            }
        } else {
            if (set.contains(CjTokens.IDENTIFIER) && token is CjKeywordToken) {
                if (token.isSoft) {
                    builder.remapCurrentToken(CjTokens.IDENTIFIER)
                    return true
                }
            }
        }
        return false
    }

    /**
     * 查看标记流中的下 k 个标记，而不会推进标记流
     *
     * @param k
     * @return
     */
    protected fun lookahead(k: Int): IElementType? {
        return builder.lookAhead(k)
    }



    /**
     * 如果当前标记与指定的标记匹配，则该方法将消耗该标记并返回true。否则，该方法将不会消耗标记并返回false
     *
     * @param token
     * @return
     */
    protected fun consumeIf(token: CjToken): Boolean {
        if (at(token)) {
            advance() // token
            return true
        }
        return false
    }

    /**
     * 如果当前标记与复杂标记匹配，则该方法将消耗该标记并返回true。否则，该方法将不会消耗标记并返回false
     */
    protected fun consumeIfSet(tokenSet: TokenSet): Boolean {
        if (atSet(tokenSet)) {
            advance() // token
            return true
        }
        return false
    }

    /**
     * 根据传入的复杂类型顺序匹配标记流中的标记
     */
    protected fun match(vararg types: IElementType): Boolean {
        var i = 0
        while (i < types.size && !eof()) {
            if (!at(types[i])) return false
            advance()
            i++
        }
        return i == types.size
    }

    /**
     * 跳过标记流中的标记，直到找到指定的标记集合中的任何一个标记
     *
     * @param tokenSet
     */
    protected fun skipUntil(tokenSet: TokenSet) {
        val stopAtEolOrSemi = tokenSet.contains(CjTokens.EOL_OR_SEMICOLON)
        while (!eof() && !tokenSet.contains(tt()) && !(stopAtEolOrSemi && at(CjTokens.EOL_OR_SEMICOLON))) {
            advance()
        }
    }


    protected fun errorUntil(message: String?, tokenSet: TokenSet) {
        assert(tokenSet.contains(CjTokens.LBRACE)) { "Cannot include LBRACE into error element!" }
        assert(tokenSet.contains(CjTokens.RBRACE)) { "Cannot include RBRACE into error element!" }
        val error = mark()
        skipUntil(tokenSet)
        error.error(message!!)
    }

    /**
     * 表示一个可选的标记
     */
    protected inner class OptionalMarker(actuallyMark: Boolean) {
        private val marker: PsiBuilder.Marker? = if (actuallyMark) mark() else null
        private val offset: Int = builder.currentOffset

        /**
         * 标记可选的语法单元已经解析完成，并将其转换为指定类型的语法单元
         *
         * @param elementType
         */
        fun done(elementType: IElementType?) {
            if (marker == null) return
            marker.done(elementType!!)
        }

        /**
         * 报告解析错误
         *
         * @param message
         */
        fun error(message: String?) {
            if (marker == null) return
            if (offset == builder.currentOffset) {
                marker.drop() // 没有空错误
            } else {
                marker.error(message!!)
            }
        }

        /**
         * 用于删除当前位置的标记
         */
        fun drop() {
            if (marker == null) return
            marker.drop()
        }
    }

    /**
     * 匹配指定的标记流模式
     *
     * @param pattern
     * @return
     */
    protected fun matchTokenStreamPredicate(pattern: TokenStreamPattern): Int {
        val currentPosition = mark()
        val opens = Stack<IElementType>()
        var openAngleBrackets = 0
        var openBraces = 0
        var openParentheses = 0
        var openBrackets = 0
        while (!eof()) {
            if (pattern.processToken(
                    builder.currentOffset,
                    pattern.isTopLevel(openAngleBrackets, openBrackets, openBraces, openParentheses)
                )
            ) {
                break
            }
            when (getTokenId()) {
                CjTokens.LPAR_Id -> {
                    openParentheses++
                    opens.push(CjTokens.LPAR)
                }

                CjTokens.LT_Id -> {
                    openAngleBrackets++
                    opens.push(CjTokens.LT)
                }

                CjTokens.LBRACE_Id -> {
                    openBraces++
                    opens.push(CjTokens.LBRACE)
                }

                CjTokens.LBRACKET_Id -> {
                    openBrackets++
                    opens.push(CjTokens.LBRACKET)
                }

                CjTokens.RPAR_Id -> {
                    openParentheses--
                    if (opens.isEmpty() || opens.pop() !== CjTokens.LPAR) {
                        if (pattern.handleUnmatchedClosing(CjTokens.RPAR)) {
                            break
                        }
                    }
                }

                CjTokens.GT_Id -> openAngleBrackets--
                CjTokens.RBRACE_Id -> openBraces--
                CjTokens.RBRACKET_Id -> openBrackets--
            }
            advance()
        }

        currentPosition.rollbackTo()

        return pattern.result()
    }

    /**
     * 检查当前标记是否位于行末
     *
     * @return
     */
    protected fun eol(): Boolean {
        return builder.newlineBeforeCurrentToken() || eof()
    }

    protected abstract fun create(builder: SemanticWhitespaceAwarePsiBuilder?): CangJieParsing

    protected fun createTruncatedBuilder(eofPosition: Int): CangJieParsing {
        return create(TruncatedSemanticWhitespaceAwarePsiBuilder(builder, eofPosition))
    }

    protected inner class At @JvmOverloads constructor(
        private val lookFor: IElementType,
        private val topLevelOnly: Boolean = true
    ) :
        AbstractTokenStreamPredicate() {
        override fun matching(topLevel: Boolean): Boolean {
            return (topLevel || !topLevelOnly) && at(lookFor)
        }
    }

    protected inner class AtSet @JvmOverloads constructor(
        private val lookFor: TokenSet,
        private val topLevelOnly: TokenSet = lookFor
    ) :
        AbstractTokenStreamPredicate() {
        override fun matching(topLevel: Boolean): Boolean {
            return (topLevel || !atSet(topLevelOnly)) && atSet(lookFor)
        }
    }

    /**
     * 获取当前解析上下文的字符串表示
     *
     * @return
     */
    @TestOnly
    fun currentContext(): String {
        return builder.originalText
            .substringWithContext(builder.currentOffset, builder.currentOffset, 20)
    }

    companion object {
        private val SOFT_KEYWORD_TEXTS: MutableMap<String, CjKeywordToken> = HashMap()

        init {
            for (type in CjTokens.SOFT_KEYWORDS.types) {
                val keywordToken = type as CjKeywordToken
                assert(keywordToken.isSoft)
                SOFT_KEYWORD_TEXTS[keywordToken.value] = keywordToken
            }
        }

        init {
            for (token in CjTokens.KEYWORDS.types) {
                assert(token is CjKeywordToken) { "Must be CjKeywordToken: $token" }
                assert(!(token as CjKeywordToken).isSoft) { "Must not be soft: $token" }
            }
        }

        /**
         * 关闭声明并绑定注释
         *
         * @param marker
         * @param elementType
         * @param precedingNonDocComments
         */
        protected fun closeDeclarationWithCommentBinders(
            marker: PsiBuilder.Marker,
            elementType: IElementType,
            precedingNonDocComments: Boolean
        ) {
            marker.done(elementType)
            marker.setCustomEdgeTokenBinders(
                if (precedingNonDocComments) PrecedingCommentsBinder else PrecedingDocCommentsBinder,
                TrailingCommentsBinder
            )
        }

        /**
         * 在满足指定条件时报告解析错误
         *
         * @param marker
         * @param condition
         * @param message
         */
        protected fun errorIf(marker: PsiBuilder.Marker, condition: Boolean, message: String?) {
            if (condition) {
                marker.error(message!!)
            } else {
                marker.drop()
            }
        }

    }
}
