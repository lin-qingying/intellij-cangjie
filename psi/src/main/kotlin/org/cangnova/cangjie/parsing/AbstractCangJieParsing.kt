/*
 * Copyright 2026 LinQingYing. and contributors.
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

package org.cangnova.cangjie.parsing

import com.intellij.lang.*
import com.intellij.lang.impl.PsiBuilderImpl
import com.intellij.openapi.util.NlsContexts
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import com.intellij.util.containers.Stack
import org.cangnova.cangjie.lexer.CjKeywordToken
import org.cangnova.cangjie.lexer.CjToken
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.lexer.CjTokens.*
import org.cangnova.cangjie.messages.CangJieParsingBundle
import org.cangnova.cangjie.psi.CjNodeTypes
import org.cangnova.cangjie.utils.substringWithContext
import org.jetbrains.annotations.Contract
import org.jetbrains.annotations.TestOnly

abstract class AbstractCangJieParsing(
    protected val builder: SemanticWhitespaceAwarePsiBuilder,
    protected val isLazy: Boolean = true,

    ) {

    var isDeclarationsFile: Boolean = false

    /**
     * 统一的解析上下文
     *
     * 用于控制解析器的行为，包含解析控制、错误报告和表达式解析三个方面的配置
     *
     * ## 解析控制相关属性
     * @property disableMacroParsing 是否禁用宏解析，如果为true，所有@符号后的内容都将被解析为注解而不是宏
     * @property enableCustomAnnotation 是否启用自定义注解
     * @property parseAnnotations 是否解析注解，默认为true。如果为false，则跳过注解的解析
     *
     * ## 错误报告相关属性
     * @property shouldReportError 是否应该报告错误。true表示报告错误，false表示只标记token不报告错误
     *
     * ## 表达式解析相关属性
     * @property allowLetExpression 是否允许let表达式
     * @property parseTypeArguments 是否解析类型参数
     * @property isParseOperator 是否解析操作符
     * @property isExpression 是否为表达式模式
     * @property preferBlock 是否优先解析为代码块
     * @property collapse 是否折叠
     * @property isDoubleArrow 是否使用双箭头
     * @property backToken 宏表达式是否返回token
     * @property processStringInterpolation 是否处理字符串插值
     */
    data class ParsingContext(
        // ==================== 解析控制相关 ====================
        val disableMacroParsing: Boolean = false,

        val enableCustomAnnotation: Boolean = false,

        val allowParseAnnotationsInValueParameter: Boolean = true,
//        val strictMode: Boolean = false

        // ==================== 错误报告相关 ====================
        val shouldReportError: Boolean = true,

        // ==================== 表达式解析相关 ====================
        val allowLetExpression: Boolean = false,
        val parseTypeArguments: Boolean = true,
        val isParseOperator: Boolean = true,
        val isExpression: Boolean = false,
        val preferBlock: Boolean = false,
        val collapse: Boolean = true,
        val isDoubleArrow: Boolean = true,
        val backToken: Boolean = false,
        val processStringInterpolation: Boolean = true,
    ) {
        companion object {
            // ==================== 通用上下文 ====================
            /** 默认上下文 */
            val DEFAULT = ParsingContext()

            /** 仅注解模式（禁用宏解析） */
            val ANNOTATION_ONLY = ParsingContext(disableMacroParsing = true, enableCustomAnnotation = true)


//            /** 严格模式 */
//            val STRICT = ParsingContext(strictMode = true)
//
//            /** 遗留模式（禁用自定义注解） */
            val LEGACY = ParsingContext(enableCustomAnnotation = false)


            // ==================== 错误报告相关 ====================
            /** 报告错误 */
            val REPORT = ParsingContext(shouldReportError = true)

            /** 静默模式（不报告错误） */
            val SILENT = ParsingContext(shouldReportError = false)

            // ==================== 表达式解析相关 ====================
            /** if/while条件上下文 */
            val IF_WHILE_CONDITION = ParsingContext(allowLetExpression = true)

            /** match表达式模式 */
            val MATCH_EXPRESSION_MODE = ParsingContext(isExpression = true)

            /** 函数字面量块模式 */
            val FUNCTION_LITERAL_BLOCK = ParsingContext(preferBlock = true)

            /** 函数字面量折叠模式 */
            val FUNCTION_LITERAL_COLLAPSED = ParsingContext(collapse = true, isDoubleArrow = false)

            /** 宏返回token模式 */
            val MACRO_BACK_TOKEN = ParsingContext(backToken = true)

            /** 不处理字符串插值 */
            val NO_STRING_INTERPOLATION = ParsingContext(processStringInterpolation = false)
        }

        val expressionFirst: TokenSet
            get() = if (allowLetExpression) {
                CangJieExpressionParsing.EXPRESSION_FIRST_WITH_LET
            } else {
                CangJieExpressionParsing.EXPRESSION_FIRST
            }
    }



    fun interface Parser {
        fun parse(builder: PsiBuilder, level: Int): Boolean
    }

    fun interface Hook<T> {
        @Contract("_,null,_->null")
        fun run(builder: PsiBuilder, marker: PsiBuilder.Marker?, param: T?): PsiBuilder.Marker?
    }

    protected inner class At(
        private val lookFor: IElementType,
        private val topLevelOnly: Boolean = true
    ) : AbstractTokenStreamPredicate() {
        override fun matching(topLevel: Boolean): Boolean =
            (topLevel || !topLevelOnly) && at(lookFor)
    }

    protected inner class AtSet(
        private val lookFor: TokenSet,
        private val topLevelOnly: TokenSet = lookFor
    ) : AbstractTokenStreamPredicate() {
        override fun matching(topLevel: Boolean): Boolean =
            (topLevel || !atSet(topLevelOnly)) && atSet(lookFor)
    }

    /**
     * 在标记流中创建一个标记，并返回该标记的 PsiBuilder.Marker 对象
     *
     * @return
     */
    protected fun mark(): PsiBuilder.Marker = builder.mark()

    /**
     * 可选标记类,用于在解析过程中创建可选的语法树标记
     *
     * 该类封装了标记的创建和管理,支持条件性地创建标记。当actuallyMark为true时创建实际的标记,
     * 否则不创建标记但仍提供相同的API接口。这对于在某些条件下需要跳过标记创建的场景非常有用。
     *
     * @param actuallyMark 是否实际创建标记。true表示创建真实标记,false表示跳过标记创建
     *
     * 示例:
     * ```kotlin
     * // 创建一个实际的标记
     * val marker = OptionalMarker(true)
     * parseExpression()
     * marker.done(CjElementTypes.EXPRESSION)
     *
     * // 创建一个不实际标记的实例(用于跳过某些条件下的标记)
     * val noMarker = OptionalMarker(false)
     * parseOptionalElement()
     * noMarker.drop() // 不会有任何效果
     * ```
     */
    protected inner class OptionalMarker(actuallyMark: Boolean) {
        val marker: PsiBuilder.Marker? = if (actuallyMark) mark() else null
        private val offset: Int = builder.currentOffset

        /**
         * 标记可选的语法单元已经解析完成，并将其转换为指定类型的语法单元
         *
         * @param elementType
         */
        fun done(elementType: IElementType) {
            marker?.done(elementType)
        }

        /**
         * 报告解析错误
         *
         * @param message 错误消息
         */
        context(context: ParsingContext)
        fun error(message: String) {
            if (context.shouldReportError) {
                marker?.let {
                    if (offset == builder.currentOffset) {
                        it.drop()
                    } else {
                        it.error(message)
                    }
                }
            }
        }

        /**
         * 用于删除当前位置的标记
         */
        fun drop() {
            marker?.drop()
        }
    }


////////////////////////////////静态类/////////////////////////////////////////////////////////////////


    companion object {


        private
        const val MAX_VARIANTS_SIZE: Int = 10000

        private
        const val MAX_VARIANTS_TO_DISPLAY: Int = 50

        private
        const val MAX_ERROR_TOKEN_TEXT: Int = 20

        private
        const val INITIAL_VARIANTS_SIZE: Int = 1000

        private
        const val VARIANTS_POOL_SIZE: Int = 10000

        private
        const val FRAMES_POOL_SIZE: Int = 500

        // Property delegate for lazy initialization
        private val SOFT_KEYWORD_TEXTS by lazy {
            buildMap<String, CjKeywordToken> {
                CjTokens.SOFT_KEYWORDS.types.forEach { type ->
                    val keywordToken = type as CjKeywordToken
                    require(keywordToken.isSoft) { "Must be soft keyword: $keywordToken" }
                    put(keywordToken.value, keywordToken)
                }
            }
        }
//        init {
//            for (type in CjTokens.SOFT_KEYWORDS.getTypes()) {
//                val keywordToken = type as CjKeywordToken
//                assert(keywordToken.isSoft)
//                SOFT_KEYWORD_TEXTS.put(keywordToken.value, keywordToken)
//            }
//        }

        init {

            for (token in CjTokens.KEYWORDS.getTypes()) {
                assert(token is CjKeywordToken) { "Must be CjKeywordToken: " + token }
                assert(!(token as CjKeywordToken).isSoft) { "Must not be soft: " + token }
            }
        }


        ///////////////////////////////////////////////
        fun isWhitespaceOrComment(builder: PsiBuilder, type: IElementType?): Boolean {
            return (builder as PsiBuilderImpl).whitespaceOrComment(type)
        }

        /**
         * 关闭声明并绑定注释
         *
         * @param marker
         * @param elementType
         * @param precedingNonDocComments
         */
        @JvmStatic
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
        @JvmStatic
        protected fun errorIf(marker: PsiBuilder.Marker, condition: Boolean, message: String) {
            if (condition) {
                marker.error(message)
            } else {
                marker.drop()
            }
        }
    }

    fun advanceBalancedBlock() {
        var braceCount = 1
        while (!eof()) {
            when {
                _at(LBRACE) -> braceCount++
                _at(RBRACE) -> braceCount--
            }

            advance()

            if (braceCount == 0) {
                break
            }
        }
    }

    /**
     * 无副作用版本的at()
     */
    protected fun _at(expectation: IElementType): Boolean {
        val token = tt()
        return tokenMatches(token, expectation)
    }

    /**
     * 检查当前标记是否与预期标记匹配
     *
     * @param token
     * @param expectation
     * @return
     */
    private fun tokenMatches(token: IElementType?, expectation: IElementType): Boolean {
        if (token === expectation) return true
        if (expectation === CjTokens.EOL_OR_SEMICOLON) {
            if (eof()) return true
            if (token === CjTokens.SEMICOLON) return true
            return builder.newlineBeforeCurrentToken()
        }
        return false
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
     * 获取当前标记的类型
     *
     * @return
     */
    protected fun tt(): IElementType? {
        return builder.getTokenType()
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

    protected fun _atSet(vararg set: IElementType): Boolean {
        return _atSet(TokenSet.create(*set))
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
            val keywordToken = SOFT_KEYWORD_TEXTS.get(builder.getTokenText()) ?: return false
            if (set.contains(keywordToken)) {
                builder.remapCurrentToken(keywordToken)
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
     * 获取当前解析上下文的字符串表示
     *
     * @return
     */
    @TestOnly
    fun currentContext(): String {
        return builder.getOriginalText()
            .substringWithContext(builder.getCurrentOffset(), builder.getCurrentOffset(), 20)
    }

    /**
     * 推进标记流并返回下一个标记
     */
    protected fun advance() {
        builder.advanceLexer()
    }

    /**
     * 推进标记流并返回下一个指定类型的标记
     *
     * @param current
     */
    protected fun advanceAt(current: IElementType?) {
        assert(_at(current!!))
        builder.advanceLexer()
    }


    /**
     * 推进标记流并返回下  advanceTokenCount 个标记
     *
     * @param advanceTokenCount
     */
    protected fun advance(advanceTokenCount: Int) {
        for (i in 0..<advanceTokenCount) {
            advance() // 错误的令牌
        }
    }

    /**
     * 如果当前标记与指定的标记匹配，则该方法将消耗该标记并返回true。否则，该方法将不会消耗标记并返回false
     *
     * @param token
     * @return
     */
    protected fun consumeIf(token: CjToken?): Boolean {
        if (at(token!!)) {
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
    protected fun match(vararg types: IElementType?): Boolean {
        var i = 0
        while (i < types.size && !eof()) {
            if (!at(types[i]!!)) return false
            advance()
            i++
        }
        return i == types.size
    }

    /**
     * 报告解析错误并停止解析过程
     *
     * @param message 错误消息
     */
    context(context: ParsingContext)
    protected fun error(message: String) {
        if (context.shouldReportError) {
            builder.error(message)
        }
    }

    /**
     * 在指定标记之前报告错误
     *
     * @param message 错误消息
     * @param marker 标记位置
     */
    context(context: ParsingContext)
    protected fun errorBefore(message: String, marker: PsiBuilder.Marker) {
        if (context.shouldReportError) {
            val err = marker.precede()
            marker.error(message)
        }
    }


    /**
     * 报告解析错误并消耗当前标记
     *
     * @param message 错误消息
     */
    context(context: ParsingContext)
    protected fun errorAndAdvance(message: String) {
        errorAndAdvance(message, 1)
    }

    /**
     * 报告解析错误并消耗指定数量的标记
     *
     * @param message 错误消息
     * @param advanceTokenCount 要消耗的标记数量
     */
    context(context: ParsingContext)
    protected fun errorAndAdvance(message: String, advanceTokenCount: Int) {
        if (context.shouldReportError) {
            val err = mark()
            advance(advanceTokenCount)
            err.error(message)
        } else {
            advance(advanceTokenCount)
        }
    }


    /**
     * 检查当前标记是否为指定的 CjToken 类型，并在标记不匹配时报告错误
     *
     * @param expectation 期望的标记类型
     * @param message 错误消息
     * @return 是否匹配成功
     */
    context(context: ParsingContext)
    protected fun expect(expectation: IElementType, message: String): Boolean {
        return expect(expectation, message, null)
    }

    /**
     * 检查当前标记是否为指定的复杂标记类型，并在标记不匹配时报告错误
     *
     * @param expectation 期望的标记类型
     * @param message 错误消息
     * @param recoverySet 恢复标记集合
     * @return 是否匹配成功
     */
    context(context: ParsingContext)
    protected fun expect(expectation: IElementType, message: String, recoverySet: TokenSet?): Boolean {
        if (expect(expectation)) {
            return true
        }

        errorWithRecovery(message, recoverySet)

        return false
    }

    protected fun expect(expectationSet: TokenSet): Boolean {
        if (atSet(expectationSet)) {
            advance()
            return true
        }


        return false
    }

    /**
     * 检查当前标记是否在指定的标记集合中，并在不匹配时报告错误
     *
     * @param expectationSet 期望的标记集合
     * @param message 错误消息
     * @param recoverySet 恢复标记集合
     * @return 是否匹配成功
     */
    context(context: ParsingContext)
    protected fun expect(expectationSet: TokenSet, message: String, recoverySet: TokenSet?): Boolean {
        if (expect(expectationSet)) {
            return true
        }

        errorWithRecovery(message, recoverySet)

        return false
    }

    /**
     * 报告解析错误并尝试恢复解析过程
     *
     * @param message 错误消息
     * @param recoverySet 恢复标记集合
     */
    context(context: ParsingContext)
    protected fun errorWithRecovery(message: String, recoverySet: TokenSet?) {
        val tt = tt()
        if (null == recoverySet ||
            recoverySet.contains(tt) ||
            (recoverySet.contains(CjTokens.EOL_OR_SEMICOLON) && (eof() || tt === CjTokens.SEMICOLON || builder.newlineBeforeCurrentToken()))
        ) {
            error(message)
        } else {
            errorAndAdvance(message)
        }
    }

    /**
     * 检查当前标记是否为指定的 CjToken 类型，并在标记不匹配时报告错误
     *
     * @param expectation
     * @return
     */
    protected fun expect(expectation: IElementType): Boolean {
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

    /**
     * 报告错误直到遇到指定的标记集合
     *
     * @param message 错误消息
     * @param tokenSet 停止标记集合
     */
    context(context: ParsingContext)
    protected fun errorUntil(message: String, tokenSet: TokenSet) {
        assert(tokenSet.contains(CjTokens.LBRACE)) { "Cannot include LBRACE into error element!" }
        assert(tokenSet.contains(CjTokens.RBRACE)) { "Cannot include RBRACE into error element!" }
        if (context.shouldReportError) {
            val error = mark()
            skipUntil(tokenSet)
            error.error(message)
        } else {
            skipUntil(tokenSet)
        }
    }

    /**
     * 获取当前标记的类型id
     *
     * @return
     */
    protected fun getTokenId(): Int {
        val elementType = tt()
        return if (elementType is CjToken) elementType.tokenId else CjTokens.INVALID_Id
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
     * 报告错误，但不消耗标记
     *
     * @param message 错误消息
     */
    context(context: ParsingContext)
    protected fun errorWithoutAdvancing(message: String) {
        if (context.shouldReportError) {
            mark().error(message)
        }
    }

    protected fun rawLookup(steps: Int): IElementType? {
        return builder.rawLookup(steps)
    }

    protected fun getGtTokenType(): IElementType? =
        when (val tokenType = tt()) {
            CjTokens.QUEST -> if (rawLookup(1) === CjTokens.QUEST) CjTokens.COALESCING else tokenType
            CjTokens.GT -> when {
                rawLookup(1) === CjTokens.GT -> if (rawLookup(2) === CjTokens.EQ) CjTokens.GTGTEQ else CjTokens.GTGT
                rawLookup(1) === CjTokens.EQ -> CjTokens.GTEQ
                else -> tokenType
            }

            else -> tokenType
        }

    //    获取可以被重载的操作符标记 OPERATIONS
    protected fun getOperationTokenType(): IElementType? =
        when (val tokenType = getGtTokenType()) {
            CjTokens.LPAR -> if (rawLookup(1) === CjTokens.RPAR) CjTokens.OPERATION_INVOKE else tokenType
            CjTokens.LBRACKET -> if (rawLookup(1) === CjTokens.RBRACKET) CjTokens.OPERATION_GET else tokenType
            CjTokens.EXCL -> CjTokens.OPERATION_NOT
            CjTokens.EXCLEQ -> CjTokens.OPERATION_NOT_EQUALS
            CjTokens.MULMUL -> CjTokens.OPERATION_EXPONENTIATION
            CjTokens.EQEQ -> CjTokens.OPERATION_EQUALS
            CjTokens.MUL -> CjTokens.OPERATION_TIMES
            CjTokens.DIV -> CjTokens.OPERATION_DIV
            CjTokens.PERC -> CjTokens.OPERATION_REM
            CjTokens.MINUS -> CjTokens.OPERATION_MINUS
            CjTokens.PLUS -> CjTokens.OPERATION_PLUS
            CjTokens.LTLT -> CjTokens.OPERATION_LEFT_SHIFT
            CjTokens.GTGT -> CjTokens.OPERATION_RIGHT_SHIFT
            CjTokens.GT -> CjTokens.OPERATION_COMPARE_GT
            CjTokens.LT -> CjTokens.OPERATION_COMPARE_LT
            CjTokens.LTEQ -> CjTokens.OPERATION_COMPARE_LTEQ
            CjTokens.GTEQ -> CjTokens.OPERATION_COMPARE_GTEQ
            CjTokens.AND -> CjTokens.OPERATION_AND
            CjTokens.XOR -> CjTokens.OPERATION_XOR
            CjTokens.OR -> CjTokens.OPERATION_OR
            else -> tokenType
        }

    protected fun createTruncatedBuilder(eofPosition: Int): CangJieParsing {
        return create(TruncatedSemanticWhitespaceAwarePsiBuilder(builder, eofPosition))
    }

    context(context: ParsingContext)

    protected fun expectSafeCall(expectationSet: TokenSet, message: String, recoverySet: TokenSet?): Boolean {
        val tokenType = getSafeTokenType()
        if (expectationSet.contains(tokenType)) {
            advanceSafeToken(tokenType)
            return true
        }

        errorWithRecovery(message, recoverySet)

        return false
    }

    /**
     * 根据标记类型推进GT标记
     *
     * @param type 要处理的标记类型
     */
    protected fun advanceGtToken(type: IElementType) {
        mark().apply {
            when (type) {
                CjTokens.COALESCING -> {
                    PsiBuilderUtil.advance(builder, 2)
                    collapse(type)
                }

                CjTokens.GTGTEQ -> {
                    PsiBuilderUtil.advance(builder, 3)
                    collapse(type)
                }

                CjTokens.GTGT, CjTokens.GTEQ -> {
                    PsiBuilderUtil.advance(builder, 2)
                    collapse(type)
                }

                else -> {
                    drop()
                    advance()
                }
            }
        }
    }

    protected abstract fun create(builder: SemanticWhitespaceAwarePsiBuilder): CangJieParsing

    /**
     * 匹配指定的标记流模式
     *
     * @param pattern
     * @return
     */
    protected fun matchTokenStreamPredicate(pattern: TokenStreamPattern): Int {
        val currentPosition = mark()
        val opens = Stack<IElementType?>()
        var openAngleBrackets = 0
        var openBraces = 0
        var openParentheses = 0
        var openBrackets = 0
        while (!eof()) {
            if (pattern.processToken(
                    builder.getCurrentOffset(),
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

    // Extension functions for token operations
    fun PsiBuilder.isAtEnd() = eof()

    fun PsiBuilder.currentTokenMatches(expectation: IElementType): Boolean =
        getTokenType() === expectation

    fun PsiBuilder.currentTokenMatchesSet(set: TokenSet): Boolean =
        set.contains(getTokenType())


    /**
     * 当前标记类型的扩展属性
     *
     * 提供对底层构建器当前标记类型的直接访问
     */
    protected val currentTokenType: IElementType?
        get() = builder.tokenType

    /**
     * 带上下文信息的错误报告扩展函数
     *
     * @param message 错误消息
     * 在错误消息中添加当前解析上下文信息
     */
    protected fun PsiBuilder.errorWithContext(message: String) {
        val context = currentContext()
        error("$message (at $context)")
    }

    // DSL for marking and parsing
    /**
     * 用于标记和解析的DSL风格函数
     *
     * @param block 要在标记上执行的代码块
     * @return 创建的标记对象
     * 创建一个新的标记并在其上执行提供的代码块
     */
    protected fun mark(block: PsiBuilder.Marker.() -> Unit): PsiBuilder.Marker {
        val marker = builder.mark()
        marker.block()
        return marker
    }

    protected fun parsing(block: AbstractCangJieParsing.() -> Unit) {
        this.block()
    }

    context(context: ParsingContext)

    protected fun withRecoveryContext(recoverySet: TokenSet, block: () -> Unit) {
        try {
            block()
        } catch (e: ParsingError) {
            errorWithRecovery(e.message ?: "Parsing error", recoverySet)
        }
    }

    protected fun PsiBuilder.Marker.withErrorReporting(message: String, block: PsiBuilder.Marker.() -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            error(message)
        }
    }

    /**
     * 解析错误工具类
     *
     * 用于表示不同类型的解析错误的密封类层次结构
     * @param message 错误消息
     *
     * 示例:
     * ```kotlin
     * // 创建一个意外标记错误
     * val unexpectedError = ParsingError.UnexpectedToken(
     *     found = CjTokens.SEMICOLON,
     *     expected = CjTokens.RPAR
     * )
     *
     * // 创建一个缺失标记错误
     * val missingError = ParsingError.MissingToken(
     *     expected = CjTokens.RBRACE
     * )
     * ```
     */
    sealed class ParsingError(message: String) : Exception(message) {
        /**
         * 表示遇到了意外的标记
         *
         * @param found 实际找到的标记
         * @param expected 期望的标记
         */
        class UnexpectedToken(val found: IElementType?, val expected: IElementType) :
            ParsingError("Expected $expected but found $found")

        /**
         * 表示缺少必需的标记
         *
         * @param expected 缺少的标记
         */
        class MissingToken(val expected: IElementType) :
            ParsingError("Missing required token: $expected")
    }

    /**
     * 安全的期望标记检查函数
     *
     * @param expectation 期望的标记
     * @return 包含操作结果的Result对象
     * 检查当前标记是否为期望的标记，如果不是则抛出异常
     *
     * 示例:
     * ```kotlin
     * // 检查下一个标记是否为右括号
     * expectSafely(CjTokens.RPAR)
     *     .onSuccess {
     *         // 标记匹配成功，继续解析
     *         advance()
     *     }
     *     .onFailure { error ->
     *         // 处理解析错误
     *         when (error) {
     *             is ParsingError.UnexpectedToken -> {
     *                 // 处理意外标记错误
     *                 errorWithRecovery("Expected right parenthesis", RECOVERY_SET)
     *             }
     *         }
     *     }
     * ```
     */
    protected fun expectSafely(expectation: CjToken): Result<Unit> = runCatching {
        if (!expect(expectation)) {
            throw ParsingError.UnexpectedToken(tt(), expectation)
        }
    }

    /**
     * 带恢复机制的期望标记检查函数
     *
     * @param expectation 期望的标记
     * @param recoverySet 用于错误恢复的标记集合
     * @return 包含操作结果的Result对象
     * 检查当前标记是否为期望的标记，如果不是则进行错误恢复并抛出异常
     *
     * 示例:
     * ```kotlin
     * // 定义恢复标记集合
     * val recoverySet = TokenSet.create(
     *     CjTokens.RBRACE,
     *     CjTokens.SEMICOLON,
     *     CjTokens.EOL_OR_SEMICOLON
     * )
     *
     * // 检查并尝试恢复
     * expectWithRecovery(CjTokens.RPAR, recoverySet)
     *     .onSuccess {
     *         // 解析成功，继续处理
     *         parseNextElement()
     *     }
     *     .onFailure { error ->
     *         // 错误已经被处理和恢复
     *         // 可以添加额外的错误处理逻辑
     *         logger.debug("Recovered from parsing error: $error")
     *     }
     * ```
     */
    context(context: ParsingContext)

    protected fun expectWithRecovery(expectation: CjToken, recoverySet: TokenSet?): Result<Unit> = runCatching {
        if (!expect(expectation)) {
            val message = "Expected ${expectation.toString()}"
            errorWithRecovery(message, recoverySet)
            throw ParsingError.UnexpectedToken(tt(), expectation)
        }
    }

    /**
     * 安全的标记完成函数
     *
     * @param elementType 要完成的元素类型
     * @return 包含操作结果的Result对象
     * 安全地将标记完成为指定的元素类型
     *
     * 示例:
     * ```kotlin
     * // 创建一个标记
     * val marker = mark()
     *
     * // 解析一些元素
     * parseExpression()
     *
     * // 安全地完成标记
     * marker.completeSafely(CjElementTypes.EXPRESSION)
     *     .onSuccess {
     *         // 标记成功完成
     *         processExpression()
     *     }
     *     .onFailure { error ->
     *         // 处理完成标记时的错误
     *         errorWithContext("Failed to complete expression marker")
     *     }
     * ```
     */
    protected fun PsiBuilder.Marker.completeSafely(elementType: IElementType): Result<Unit> = runCatching {
        done(elementType)
    }

    /**
     * 获取标记流中的最后一个标记类型
     *
     * @return
     */
    protected fun getLastToken(): IElementType? {
        val currentOffset = builder.currentOffset

        return generateSequence(1) { it + 1 }
            .takeWhile { it <= currentOffset }
            .find { !CjTokens.WHITE_SPACE_OR_COMMENT_BIT_SET.contains(builder.rawLookup(-it)) }
            ?.let { rawLookup(-it) }
    }

    protected fun advanceSafeToken(type: IElementType?) {
        mark().apply {
            when (type) {
                CjTokens.COALESCING, CjTokens.SAFE_CALL -> {
                    PsiBuilderUtil.advance(builder, 2)
                    collapse(type)
                }

                else -> {
                    drop()
                    advance()
                }
            }
        }
    }

    /**
     * 检查当前标记是否为指定的 CjToken 类型，但不会消耗标记流中的标记
     *
     * @param expectation
     * @param message
     */
    context(context: ParsingContext)

    protected fun expectNoAdvance(expectation: CjToken, message: String) {

        if (at(expectation)) {
            advance()
            return
        }
        if (context.shouldReportError) {
            error(message)
        }


    }

    protected fun advanceOperationToken(type: IElementType?) {
        val gtToken = mark()

        when (type) {
            OPERATION_INVOKE,
            OPERATION_GET,
            OPERATION_COMPARE_GTEQ,
            OPERATION_RIGHT_SHIFT -> {
                PsiBuilderUtil.advance(builder, 2)
                gtToken.collapse(type)
            }

            else -> {
                gtToken.drop()
                advance()
            }
        }
    }

    val productions: MutableList<out SyntaxTreeBuilder.Production> get() = builder.getProductions()
    protected fun getSafeTokenType(): IElementType? {
        var tokenType = tt()
        if (tokenType !== CjTokens.QUEST) return tokenType
        if (rawLookup(1) === CjTokens.QUEST) {
            tokenType = CjTokens.COALESCING
        } else if (rawLookup(1) === CjTokens.LPAR) {
            tokenType = CjTokens.SAFE_CALL
        }
        return tokenType
    }

    ///////////////////////////////////////////////////////////////////////////////
    /**
     * DSL风格的解析上下文作用域
     *
     * 提供一个作用域来执行一系列解析操作
     *
     * 示例:
     * ```kotlin
     * parsingContext {
     *     // 在这个作用域内执行解析操作
     *     matchToken(CjTokens.LBRACE)
     *     parseStatements()
     *     matchToken(CjTokens.RBRACE)
     * }
     * ```
     */
    protected fun parsingContext(block: AbstractCangJieParsing.() -> Unit) {
        this.block()
    }

    /**
     * DSL风格的标记匹配函数
     *
     * 匹配指定的标记，如果匹配失败则报告错误
     *
     * 示例:
     * ```kotlin
     * matchToken(CjTokens.LPAR) {
     *     // 匹配成功后执行的代码
     *     parseExpression()
     * }
     * ```
     */
    protected fun matchToken(token: CjToken, block: (() -> Unit)? = null): Boolean {
        val matched = expect(token)
        if (matched && block != null) {
            block()
        }
        return matched
    }

    /**
     * DSL风格的可选标记匹配函数
     *
     * 尝试匹配指定的标记，但不会在匹配失败时报告错误
     *
     * 示例:
     * ```kotlin
     * optionalToken(CjTokens.SEMICOLON) {
     *     // 只在匹配到分号时执行
     *     handleSemicolon()
     * }
     * ```
     */
    protected fun optionalToken(token: CjToken, block: (() -> Unit)? = null): Boolean {
        val matched = consumeIf(token)
        if (matched && block != null) {
            block()
        }
        return matched
    }

    /**
     * DSL风格的标记集合匹配函数
     *
     * 匹配指定标记集合中的任意标记
     *
     * 示例:
     * ```kotlin
     * matchTokenSet(CjTokens.OPERATORS) {
     *     // 匹配到任意操作符时执行
     *     parseOperator()
     * }
     * ```
     */
    protected fun matchTokenSet(tokenSet: TokenSet, block: (() -> Unit)? = null): Boolean {
        val matched = consumeIfSet(tokenSet)
        if (matched && block != null) {
            block()
        }
        return matched
    }

    /**
     * DSL风格的错误恢复作用域
     *
     * 提供一个带有错误恢复机制的作用域
     *
     * 示例:
     * ```kotlin
     * withRecovery(CjTokens.RECOVERY_SET) {
     *     parseStatement()
     * } onError { error ->
     *     // 处理解析错误
     *     handleError(error)
     * }
     * ```
     */
    protected class RecoveryScope(private val error: Throwable?) {
        fun onError(handler: (Throwable) -> Unit) {
            error?.let(handler)
        }
    }

    context(context: ParsingContext)

    protected fun withRecovery(recoverySet: TokenSet, block: () -> Unit): RecoveryScope {
        var error: Throwable? = null
        try {
            block()
        } catch (e: Throwable) {
            error = e
            errorWithRecovery(e.message ?: "Parsing error", recoverySet)
        }
        return RecoveryScope(error)
    }

    /**
     * DSL风格的标记作用域
     *
     * 创建一个新的标记并在其作用域内执行操作
     *
     * 示例:
     * ```kotlin
     * marker {
     *     parseExpression()
     * }.done(CjElementTypes.EXPRESSION)
     * ```
     */
    protected fun marker(block: () -> Unit): PsiBuilder.Marker {
        val marker = mark()
        block()
        return marker
    }

    /**
     * DSL风格的块解析作用域
     *
     * 提供一个作用域来解析代码块，自动处理大括号
     *
     * 示例:
     * ```kotlin
     * parseBlock {
     *     // 解析块内的语句
     *     parseStatements()
     * } onError { error ->
     *     // 处理解析错误
     *     handleBlockError(error)
     * }
     * ```
     */
    context(context: ParsingContext)

    protected fun parseBlock(block: () -> Unit): RecoveryScope {
        val blockMarker = mark()
        var error: Throwable? = null
        try {
            expect(CjTokens.LBRACE)
            block()
            expect(CjTokens.RBRACE)
        } catch (e: Throwable) {
            error = e
            errorWithRecovery(e.message ?: "Block parsing error", TokenSet.create(CjTokens.RBRACE))
        }
        blockMarker.done(CjNodeTypes.BLOCK)
        return RecoveryScope(error)
    }

    /**
     * DSL风格的表达式解析作用域
     *
     * 提供一个作用域来解析表达式，自动处理优先级
     *
     * 示例:
     * ```kotlin
     * parseExpression(priority = ExpressionPriority.ASSIGNMENT) {
     *     // 解析表达式
     *     parseAssignmentExpression()
     * }
     * ```
     */
    context(context: ParsingContext)

    protected fun parseExpression(priority: Int = 0, block: () -> Unit): PsiBuilder.Marker {
        val expressionMarker = mark()
        try {
            block()
        } catch (e: ParsingError) {
            errorWithRecovery(e.message ?: "Expression parsing error", null)
        }
        return expressionMarker
    }

    /**
     * DSL风格的列表解析作用域
     *
     * 提供一个作用域来解析以逗号分隔的列表
     *
     * 示例:
     * ```kotlin
     * parseList(
     *     leftBrace = CjTokens.LPAR,
     *     rightBrace = CjTokens.RPAR,
     *     separator = CjTokens.COMMA
     * ) {
     *     // 解析每个列表项
     *     parseExpression()
     * }
     * ```
     */
    protected fun parseList(
        leftBrace: IElementType,
        rightBrace: IElementType,
        separator: IElementType,
        itemParser: () -> Unit
    ) {
        expect(leftBrace)

        if (!at(rightBrace)) {
            while (true) {
                itemParser()

                if (!at(separator)) break
                advance()

                if (at(rightBrace)) {
                    error(CangJieParsingBundle.message("parsing.error.unexpected.closing.brace.after.separator"))
                    break
                }
            }
        }

        expect(rightBrace)
    }

    /**
     * DSL风格的修饰符解析作用域
     *
     * 提供一个作用域来解析修饰符列表
     *
     * 示例:
     * ```kotlin
     * parseModifiers(CjTokens.MODIFIERS) { modifiers ->
     *     // 使用收集到的修饰符
     *     processModifiers(modifiers)
     * }
     * ```
     */
    protected fun parseModifiers(
        modifierSet: TokenSet,
        block: (Set<IElementType>) -> Unit
    ) {
        val modifiers = mutableSetOf<IElementType>()
        while (atSet(modifierSet)) {
            modifiers.add(tt()!!)
            advance()
        }
        block(modifiers)
    }

    context(context: ParsingContext)

    protected fun expect(tokenType: IElementType, message: String = "", block: () -> Unit) {
        if (expect(tokenType, message)) {
            block()
        }

    }

    /**
     * DSL风格的条件解析作用域
     *
     * 提供一个作用域来根据条件解析不同的分支
     *
     * 示例:
     * ```kotlin
     * parseConditional(
     *     condition = { at(CjTokens.VAR) || at(CjTokens.VAL) },
     *     onTrue = { parseVariableDeclaration() },
     *     onFalse = { parseFunctionDeclaration() }
     * )
     * ```
     */
    protected fun parseConditional(
        condition: () -> Boolean,
        onTrue: () -> Unit,
        onFalse: () -> Unit = {}
    ) {
        if (condition()) {
            onTrue()
        } else {
            onFalse()
        }
    }

    /**
     * DSL风格的标记作用域构建器
     *
     * 提供一个流式的API来构建和操作标记
     *
     * 示例:
     * ```kotlin
     * markers {
     *     mark("expression") {
     *         parseExpression()
     *     }.then {
     *         mark("operator") {
     *             parseOperator()
     *         }
     *     }.done(CjElementTypes.BINARY_EXPRESSION)
     * }
     * ```
     */
    protected class MarkerBuilder(private val marker: PsiBuilder.Marker) {
        fun then(block: () -> Unit): MarkerBuilder {
            block()
            return this
        }

        fun done(elementType: IElementType): MarkerBuilder {
            marker.done(elementType)
            return this
        }

        fun drop(): MarkerBuilder {
            marker.drop()
            return this
        }

        fun rollback(): MarkerBuilder {
            marker.rollbackTo()
            return this
        }
    }

    protected fun markers(block: () -> Unit) {
        block()
    }

    /**
     * DSL风格的前瞻解析作用域
     *
     * 提供一个作用域来进行前瞻性解析而不影响当前位置
     *
     * 示例:
     * ```kotlin
     * lookAhead {
     *     // 尝试解析一些内容
     *     parseExpression()
     * } onSuccess {
     *     // 前瞻成功的处理
     *     handleSuccessfulLookahead()
     * } onFailure {
     *     // 前瞻失败的处理
     *     handleFailedLookahead()
     * }
     * ```
     */
    protected class LookaheadResult(
        private val success: Boolean,
        private val marker: PsiBuilder.Marker
    ) {
        fun onSuccess(block: () -> Unit): LookaheadResult {
            if (success) block()
            return this
        }

        fun onFailure(block: () -> Unit): LookaheadResult {
            if (!success) block()
            return this
        }

        fun rollback() {
            marker.rollbackTo()
        }
    }

    protected fun lookAhead(block: () -> Boolean): LookaheadResult {
        val marker = mark()
        val result = try {
            block()
        } catch (e: Throwable) {
            false
        }
        return LookaheadResult(result, marker)
    }

    /**
     * DSL风格的错误恢复作用域构建器
     *
     * 提供一个流式的API来处理错误恢复
     *
     * 示例:
     * ```kotlin
     * errorRecovery {
     *     // 尝试解析一些内容
     *     parseStatement()
     * } recover { error ->
     *     // 错误恢复逻辑
     *     skipUntil(RECOVERY_SET)
     * } finally {
     *     // 最终清理工作
     *     cleanupAfterError()
     * }
     * ```
     */
    protected class ErrorRecoveryBuilder {
        private var recoveryBlock: ((Throwable) -> Unit)? = null
        private var finallyBlock: (() -> Unit)? = null

        fun recover(block: (Throwable) -> Unit): ErrorRecoveryBuilder {
            recoveryBlock = block
            return this
        }

        fun finally(block: () -> Unit): ErrorRecoveryBuilder {
            finallyBlock = block
            return this
        }

        internal fun handleError(error: Throwable) {
            recoveryBlock?.invoke(error)
        }

        internal fun handleFinally() {
            finallyBlock?.invoke()
        }
    }

    context(context: ParsingContext)
    fun error(mark: PsiBuilder.Marker, @NlsContexts.ParsingError message: String) {
        if (context.shouldReportError) {
            mark.error(message)
        }
    }

    protected fun errorRecovery(block: () -> Unit): ErrorRecoveryBuilder {
        val builder = ErrorRecoveryBuilder()
        try {
            block()
        } catch (e: Throwable) {
            builder.handleError(e)
        } finally {
            builder.handleFinally()
        }
        return builder
    }
}

