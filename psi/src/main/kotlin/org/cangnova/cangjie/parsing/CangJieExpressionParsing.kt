/*
 * Copyright 2025 LinQingYing. and contributors.
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

import com.google.common.collect.ImmutableMap
import com.intellij.lang.PsiBuilder
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.Pair
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import org.cangnova.cangjie.lexer.CjToken
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.lexer.CjTokens.*
import org.cangnova.cangjie.messages.CangJieParsingBundle
import org.cangnova.cangjie.parsing.CangJieParsing.Companion.PARAMETER_NAME_RECOVERY_SET
import org.cangnova.cangjie.psi.CjNodeTypes
import org.cangnova.cangjie.psi.CjNodeTypes.*
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes.BASIC_REFERENCE_EXPRESSION

open class CangJieExpressionParsing(
    builder: SemanticWhitespaceAwarePsiBuilder,
    private val cangJieParsing: CangJieParsing,
    isLazy: Boolean,
) : AbstractCangJieParsing(
    builder,
    isLazy,
) {
    private val ARROW_SET = TokenSet.create(DOUBLE_ARROW)
    private val ARROW_COMMA_SET = TokenSet.create(DOUBLE_ARROW, COMMA)

    val quoteExpressionParsing = CangJieQuoteExpressionParsing(
        builder,
        this,
        true
    )

    public override fun create(builder: SemanticWhitespaceAwarePsiBuilder): CangJieParsing =
        cangJieParsing.create(builder)


    // ==================== 运算符优先级定义 ====================

    /**
     * 运算符优先级枚举
     *
     * 定义了仓颉语言中各种运算符的优先级和结合性。枚举值的声明顺序从高到低，
     * 即先声明的优先级更高。每个优先级包含一组具有相同优先级的运算符。
     *
     * 优先级顺序（从高到低）：
     * 1. POSTFIX - 后缀运算符（++, --, ., ?.）
     * 2. PREFIX - 前缀运算符（-, !）
     * 3. AS - 类型转换运算符（as）
     * 4. MULTIPLICATIVE - 乘法运算符（*, /, %, **）
     * 5. ADDITIVE - 加法运算符（+, -）
     * 6. RANGE - 区间运算符（.., ..=）
     * 7. COALESCING - 合并运算符（??）
     * 8. IS - 类型检查运算符（is）
     * 9. COMPARISON - 比较运算符（<, >, <=, >=）
     * 10. EQUALITY - 相等运算符（==, !=）
     * 11. CONJUNCTION - 逻辑与运算符（&&）
     * 12. DISJUNCTION - 逻辑或运算符（||）
     * 13. BITWISE - 位运算符（&, |, ^, <<, >>, <<=, >>=）
     * 14. FLOW - 流运算符（|>, >>）
     * 15. ASSIGNMENT - 赋值运算符（=, +=, -=, *=, /=, %=, &=, &&=, ||=, |=, ^=, <<=, >>=, **=）
     *
     * @param operations 该优先级包含的运算符token类型
     */
    @SuppressWarnings("UnusedDeclaration")
    enum class Precedence(vararg operations: IElementType) {
        /**
         * 后缀运算符优先级
         *
         * 包含：
         * - `++` - 后缀自增
         * - `--` - 后缀自减
         * - `.` - 成员访问
         * - `?.` - 安全访问
         */
        POSTFIX(
            PLUSPLUS,
            MINUSMINUS,
            DOT,
            SAFE_ACCESS,
        ),

        /**
         * 前缀运算符优先级
         *
         * 包含：
         * - `-` - 负号
         * - `!` - 逻辑非
         *
         * 注意：此优先级不调用标准的parseHigherPrecedence方法，
         * 而是直接在parsePrefixExpression中处理。
         */
        PREFIX(MINUS, /*PLUS,*/ EXCL) {
            context(context: ParsingContext)
            override fun parseHigherPrecedence(parser: CangJieExpressionParsing) {
                throw IllegalStateException("Don't call this method")
            }
        },

        /**
         * 类型转换运算符优先级
         *
         * 包含：
         * - `as` - 类型转换
         *
         * 右侧解析类型引用而非表达式。
         */
        AS(AS_KEYWORD) {
            context(context: ParsingContext)


            override fun parseRightHandSide(
                operation: IElementType,
                parser: CangJieExpressionParsing,

                ): IElementType {
                parser.mark().drop()
                parser.cangJieParsing.parseTypeRefWithoutIntersections()
                return BINARY_WITH_TYPE
            }

            context(context: ParsingContext)
            override fun parseHigherPrecedence(parser: CangJieExpressionParsing) {
                with(context) {
                    parser.parsePrefixExpression()
                }
            }
        },

        /**
         * 乘法运算符优先级
         *
         * 包含：
         * - `*` - 乘法
         * - `/` - 除法
         * - `%` - 取模
         * - `**` - 幂运算
         */
        MULTIPLICATIVE(MUL, DIV, PERC, MULMUL),

        /**
         * 加法运算符优先级
         *
         * 包含：
         * - `+` - 加法
         * - `-` - 减法
         */
        ADDITIVE(PLUS, MINUS),

        /**
         * 区间运算符优先级
         *
         * 包含：
         * - `..` - 开区间
         * - `..=` - 闭区间
         *
         * 解析为RANGE_EXPRESSION节点。
         */
        RANGE(CjTokens.RANGE, RANGEEQ) {
            context(context: ParsingContext)
            override fun parseRightHandSide(
                operation: IElementType,
                parser: CangJieExpressionParsing,

                ): IElementType {
                if (operation == CjTokens.RANGE || operation == RANGEEQ) {
                    with(context) {
                        parser.parseRangeExpression()
                    }
                    return RANGE_EXPRESSION
                }
                return super.parseRightHandSide(operation, parser)
            }
        },

        /**
         * 合并运算符优先级
         *
         * 包含：
         * - `??` - 空值合并运算符
         *
         * 用于处理可选类型，当左侧为None时使用右侧值。
         */
        COALESCING(CjTokens.COALESCING) {
            context(context: ParsingContext)
            override fun parseRightHandSide(
                operation: IElementType,
                parser: CangJieExpressionParsing,

                ): IElementType {
                if (operation == CjTokens.COALESCING) {

                    parser.parseExpression()

                    return BINARY_EXPRESSION
                }
                return super.parseRightHandSide(operation, parser)
            }
        },

        /**
         * 类型检查运算符优先级
         *
         * 包含：
         * - `is` - 类型检查
         *
         * 右侧解析类型引用，生成IS_EXPRESSION节点。
         */
        IS(IS_KEYWORD) {
            context(context: ParsingContext)
            override fun parseRightHandSide(
                operation: IElementType,
                parser: CangJieExpressionParsing,

                ): IElementType {
                if (operation === IS_KEYWORD) {
                    parser.mark().drop()
                    parser.cangJieParsing.parseTypeRefWithoutIntersections()
                    return IS_EXPRESSION
                }
                return super.parseRightHandSide(operation, parser)
            }
        },

        /**
         * 比较运算符优先级
         *
         * 包含：
         * - `<` - 小于
         * - `>` - 大于
         * - `<=` - 小于等于
         * - `>=` - 大于等于
         */
        COMPARISON(LT, GT, LTEQ, GTEQ),

        /**
         * 相等运算符优先级
         *
         * 包含：
         * - `==` - 等于
         * - `!=` - 不等于
         */
        EQUALITY(EQEQ, EXCLEQ),

        /**
         * 逻辑与运算符优先级
         *
         * 包含：
         * - `&&` - 逻辑与（短路求值）
         */
        CONJUNCTION(ANDAND),

        /**
         * 逻辑或运算符优先级
         *
         * 包含：
         * - `||` - 逻辑或（短路求值）
         */
        DISJUNCTION(OROR),

        /**
         * 位运算符优先级
         *
         * 包含：
         * - `&` - 按位与
         * - `|` - 按位或
         * - `^` - 按位异或
         * - `<<` - 左移
         * - `>>` - 右移
         * - `<<=` - 左移赋值
         * - `>>=` - 右移赋值
         */
        BITWISE(AND, OR, XOR, LTLT, GTGT, LTLTEQ, GTGTEQ),

        /**
         * 流运算符优先级
         *
         * 包含：
         * - `|>` - 管道运算符
         * - `>>` - 组合运算符
         */
        FLOW(PIPELINE, COMPOSITION),

        /**
         * 赋值运算符优先级（最低优先级）
         *
         * 包含：
         * - `=` - 赋值
         * - `+=` - 加法赋值
         * - `-=` - 减法赋值
         * - `*=` - 乘法赋值
         * - `/=` - 除法赋值
         * - `%=` - 取模赋值
         * - `&=` - 按位与赋值
         * - `&&=` - 逻辑与赋值
         * - `||=` - 逻辑或赋值
         * - `|=` - 按位或赋值
         * - `^=` - 按位异或赋值
         * - `<<=` - 左移赋值
         * - `>>=` - 右移赋值
         * - `**=` - 幂运算赋值
         */
        ASSIGNMENT(
            EQ,
            PLUSEQ,
            MINUSEQ,
            MULTEQ,
            DIVEQ,
            PERCEQ,
            ANDEQ,
            ANDANDEQ,
            OROREQ,
            OREQ,
            XOREQ,
            LTLTEQ,
            GTGTEQ,
            MULMULEQ,
        ),
        ;

        private var higher: Precedence? = null
        private val operations: TokenSet

        @OptIn(ExperimentalStdlibApi::class)
        companion object {
            init {
                val values: Array<Precedence> = entries.toTypedArray()
                for (precedence in values) {
                    val ordinal: Int = precedence.ordinal
                    precedence.higher = if (ordinal > 0) values[ordinal - 1] else null
                }
            }
        }

        init {
            this.operations = TokenSet.create(*operations)
        }

        fun getOperations(): TokenSet {
            return operations
        }

        context(context: ParsingContext)
        open fun parseRightHandSide(
            operation: IElementType,
            parser: CangJieExpressionParsing,

            ): IElementType {
            parseHigherPrecedence(parser)
            return BINARY_EXPRESSION
        }

        context(context: ParsingContext)
        open fun parseHigherPrecedence(parser: CangJieExpressionParsing) {
            assert(higher != null)
            higher?.let {
                with(context) {
                    parser.parseBinaryExpression(it)
                }
            }
        }
    }

    // ==================== 表达式解析核心方法 ====================

    /**
     * 解析表达式
     *
     * Grammar:
     * ```
     * expression
     *   : macroExpression
     *   | synchronizedExpression
     *   | binaryExpression
     *   ;
     * ```
     */
    context(context: ParsingContext)
    fun parseExpression() {
        if (at(AT)) {
            parseMacroExpression()
            return
        } else if (at(SYNCHRONIZED_KEYWORD)) {
            cangJieParsing.parseSynchronizedExpression()
            return
        } else if (!atSet(context.expressionFirst)) {
            error(CangJieParsingBundle.message("parsing.error.expecting.expression"))
            return
        }
        parseBinaryExpression(Precedence.ASSIGNMENT)
    }

    /**
     * 解析二元表达式
     *
     * Grammar:
     * ```
     * binaryExpression
     *   : prefixExpression (operator prefixExpression)*
     *   ;
     * ```
     *
     * @param precedence 运算符优先级
     */
    context(context: ParsingContext)
    private fun parseBinaryExpression(precedence: Precedence) {
        var expression = mark()
        precedence.parseHigherPrecedence(this)

        while (!interruptedWithNewLine() && precedence.getOperations()
                .contains(getGtTokenType())
        ) {
            getGtTokenType()?.let {
                parseOperationReference(it)
                val resultType = precedence.parseRightHandSide(it, this)
                expression.done(resultType)
                expression = expression.precede()
            }
        }
        expression.drop()
    }

    /**
     * 解析前缀表达式
     *
     * Grammar:
     * ```
     * prefixExpression
     *   : prefixOperator* postfixExpression
     *   ;
     * ```
     */
    context(context: ParsingContext)
    fun parsePrefixExpression() {
        builder.disableJoiningComplexTokens()
        if (atSet(Precedence.PREFIX.getOperations())) {
            val expression = mark()
            parseOperationReference()
            builder.restoreJoiningComplexTokensState()
            parsePrefixExpression()
            expression.done(PREFIX_EXPRESSION)
        } else {
            if (at(MINUSMINUS) || at(PLUSPLUS) || at(PLUS)) {
                errorAndAdvance(
                    CangJieParsingBundle.message(
                        "parsing.error.expected.expression.or.declaration",
                        builder.tokenText ?: "<unknown>"
                    )
                )
            }
            builder.restoreJoiningComplexTokensState()
            parsePostfixExpression()
        }
    }

    /**
     * 解析后缀表达式
     *
     * Grammar:
     * ```
     * postfixExpression
     *   : atomicExpression postfixOperation*
     *   ;
     *
     * postfixOperation
     *   : postfixOperator
     *   | arrayAccess
     *   | callSuffix
     *   | memberAccessOperation
     *   ;
     * ```
     */
    context(context: ParsingContext)
    private fun parsePostfixExpression() {
        var expression = mark()

        var firstExpressionParsed =
            if (atSet(BASICTYPES)) {
                val mark = mark()
                advance()
                mark.done(BASIC_REFERENCE_EXPRESSION)
                true
            } else {
                if (at(VARRAY_KEYWORD)) {
                    val mark = mark()
                    advance()

                    val typeArgumentList = mark()
                    expect(LT, CangJieParsingBundle.message("parsing.error.expecting.symbol", "<"))
                    val projection = mark()

                    cangJieParsing.parseTypeRef(TYPE_ARGUMENT_LIST_STOPPERS)

                    projection.done(TYPE_PROJECTION)

                    expect(COMMA, CangJieParsingBundle.message("parsing.error.expecting.symbol", ","))
                    expect(DOLLAR, CangJieParsingBundle.message("parsing.error.expecting.symbol", "$"))

                    expect(INTEGER_LITERAL, CangJieParsingBundle.message("parsing.error.expecting", "integer literal"))
                    expect(GT, CangJieParsingBundle.message("parsing.error.expecting.symbol", ">"))

                    typeArgumentList.done(TYPE_ARGUMENT_LIST)

                    mark.done(REFERENCE_EXPRESSION)
                    if (!at(LPAR)) {
                        error(CangJieParsingBundle.message("parsing.error.should.be.left.paren"))
                    }

                    false
                } else if (atSet(BASICTYPES)) {
                    errorAndAdvance(
                        CangJieParsingBundle.message(
                            "parsing.error.expected.expression.or.declaration.keyword",
                            builder.tokenText ?: "<unknown>"
                        )
                    )
                    false
                } else {
                    parseAtomicExpression()
                }
            }

        while (true) {
            if (interruptedWithNewLine()) {
                break
            } else if (at(LBRACKET) || at(SAFE_INDEXEX)) {
                parseArrayAccess()
                expression.done(ARRAY_ACCESS_EXPRESSION)
            } else if (parseCallSuffix()) {
                expression.done(CALL_EXPRESSION)
            } else if (at(DOT) || at(SAFE_ACCESS)) {
                val expressionType: IElementType = DOT_QUALIFIED_EXPRESSION
                advance()
                if (!firstExpressionParsed) {
                    expression.drop()
                    expression = mark()
                    firstExpressionParsed = parseAtomicExpression()
                    continue
                }
                parseSelectorCallExpression()
                try {
                    expression.done(expressionType)
                } catch (_: Throwable) {
                }
            } else if (atSet(Precedence.POSTFIX.getOperations())) {
                parseOperationReference()
                expression.done(POSTFIX_EXPRESSION)
            } else if (at(RANGE) && lookahead(1) === RBRACKET) {
                advance()
                expression.done(SLICE_EXPRESSION)
            } else {
                break
            }

            expression = expression.precede()
        }

        expression.drop()
    }

    /**
     * 解析区间表达式
     *
     * Grammar:
     * ```
     * rangeExpression
     *   : expression (":" expression)?
     *   ;
     * ```
     */
    context(context: ParsingContext)
    private fun parseRangeExpression() {
        parseExpression()
        if (at(COLON)) {
            advance()
            parseExpression()
        }
    }


    /**
     * 解析块级表达式
     *
     * Grammar:
     * ```
     * blockLevelExpression
     *   : expression
     *   ;
     * ```
     */
    context(context: ParsingContext)
    private fun parseBlockLevelExpression() {
        parseExpression()
    }

    /**
     * 解析操作符引用
     *
     * @param type 操作符类型
     */
    context(context: ParsingContext)
    private fun parseOperationReference(type: IElementType) {
        val operationReference = mark()
        getGtTokenType()?.let { advanceGtToken(it) }
        operationReference.done(OPERATION_REFERENCE)
    }

    /**
     * 解析操作符引用
     */
    context(context: ParsingContext)
    private fun parseOperationReference() {
        val operationReference = mark()
        getGtTokenType()?.let { advanceGtToken(it) }
        operationReference.done(OPERATION_REFERENCE)
    }

    /**
     * 检查是否被换行中断
     *
     * @return 如果当前操作被换行中断则返回true
     */
    context(context: ParsingContext)
    private fun interruptedWithNewLine(): Boolean {
        return !ALLOW_NEWLINE_OPERATIONS.contains(tt()) && builder.newlineBeforeCurrentToken()
    }

    // ==================== 原子表达式解析 ====================

    /**
     * 解析原子表达式
     *
     * Grammar:
     * ```
     * atomicExpression
     *   : quoteExpression
     *   | unsafeExpression
     *   | spawnExpression
     *   | parenthesizedExpression
     *   | collectionLiteralExpression
     *   | thisExpression
     *   | superExpression
     *   | throwExpression
     *   | returnExpression
     *   | continueExpression
     *   | breakExpression
     *   | ifExpression
     *   | matchExpression
     *   | tryExpression
     *   | forExpression
     *   | whileExpression
     *   | doWhileExpression
     *   | letExpression
     *   | simpleNameExpression
     *   | functionLiteral
     *   | stringTemplate
     *   | literalConstant
     *   ;
     * ```
     *
     * @return 是否成功解析了原子表达式
     */
    context(context: ParsingContext)
    private fun parseAtomicExpression(): Boolean {
        var ok = true

        when (getTokenId()) {
            QUOTE_KEYWORD_Id -> parseQuoteExpression()
            UNSAFE_KEYWORD_Id -> parseUnsafeExpression()
            SPAWN_KEYWORD_Id -> parseSpawnExpression()
            LPAR_Id -> parseParenthesizedExpression()
            LBRACKET_Id -> parseCollectionLiteralExpression()
            THIS_KEYWORD_Id -> parseThisExpression()
            SUPER_KEYWORD_Id -> parseSuperExpression()
            THROW_KEYWORD_Id -> parseThrow()
            RETURN_KEYWORD_Id -> parseReturn()
            CONTINUE_KEYWORD_Id -> parseJump(CONTINUE)
            BREAK_KEYWORD_Id -> parseJump(BREAK)
            IF_KEYWORD_Id -> parseIf()
            MATCH_KEYWORD_Id -> parseMatch()
            TRY_KEYWORD_Id -> parseTry()
            FOR_KEYWORD_Id -> parseFor()
            WHILE_KEYWORD_Id -> parseWhile()
            DO_KEYWORD_Id -> parseDoWhile()
            LET_KEYWORD_Id -> if (context.allowLetExpression) {
                parseLetExpression()
            } else {
                error(CangJieParsingBundle.message("parsing.error.let.expression.only.in.conditions"))
                ok = false
            }

            IDENTIFIER_Id -> parseSimpleNameExpression()
            LBRACE_Id -> parseFunctionLiteral()
            OPEN_QUOTE_Id -> parseStringTemplate()
            TRUE_KEYWORD_Id, FALSE_KEYWORD_Id -> parseOneTokenExpression(BOOLEAN_CONSTANT)
            INTEGER_LITERAL_Id -> parseOneTokenExpression(INTEGER_CONSTANT)
            RUNE_LITERAL_Id -> parseOneTokenExpression(RUNE_CONSTANT)
            CHARACTER_BYTE_LITERAL_Id -> parseOneTokenExpression(CHARACTER_BYTE_CONSTANT)
            FLOAT_LITERAL_Id -> parseOneTokenExpression(FLOAT_CONSTANT)
            else -> ok = false
        }
        if (!ok) {
            errorWithRecovery(
                "Expecting an element",
                TokenSet.orSet(
                    EXPRESSION_FOLLOW,
                    TokenSet.create(LONG_TEMPLATE_ENTRY_END),
                ),
            )
        }
        return ok
    }

    /**
     * 解析单token表达式
     *
     * @param type 表达式节点类型
     */
    context(context: ParsingContext)
    private fun parseOneTokenExpression(type: IElementType) {
        val mark = mark()
        advance()
        mark.done(type)
    }

    /**
     * 解析简单名称表达式
     *
     * Grammar:
     * ```
     * simpleNameExpression
     *   : IDENTIFIER typeArgumentList?
     *   ;
     * ```
     */
    context(context: ParsingContext)
    fun parseSimpleNameExpression() {
        val simpleName = mark()
        expect(IDENTIFIER, CangJieParsingBundle.message("parsing.error.expecting", "an identifier"))

        if (at(LT) && context.parseTypeArguments) {
            val typeArgumentList = mark()
            if (cangJieParsing.tryParseTypeArgumentList(TYPE_ARGUMENT_LIST_STOPPERS)) {
                typeArgumentList.done(TYPE_ARGUMENT_LIST)
            } else {
                typeArgumentList.rollbackTo()
            }
        }

        simpleName.done(REFERENCE_EXPRESSION)
    }

    /**
     * 解析引用表达式
     *
     * Grammar:
     * ```
     * referenceExpression
     *   : simpleNameExpression ("." simpleNameExpression)*
     *   ;
     * ```
     */
    context(context: ParsingContext)
    fun parseReferenceExpression() {
        var reference = mark()

        parseSimpleNameExpression()

        while (at(DOT)) {
            advance()
            parseSimpleNameExpression()
            reference.done(DOT_QUALIFIED_EXPRESSION)
            reference = reference.precede()
        }
        reference.drop()
    }

    // ==================== 括号和元组表达式 ====================

    /**
     * 解析括号表达式
     *
     * Grammar:
     * ```
     * parenthesizedExpression
     *   : "(" ")"                              // unit
     *   | "(" expression ("," expression)+ ")"  // tuple
     *   | "(" expression ")"                    // parenthesized
     *   ;
     * ```
     */
    context(context: ParsingContext)
    private fun parseParenthesizedExpression() {
        assert(_at(LPAR))

        var isUnit = false
        var isTuple = false

        val mark = mark()
        builder.disableNewlines()
        advance()
        if (!at(RPAR) && context.isParseOperator && !at(COMMA)) {

            parseExpression()

        } else {
            isUnit = true
        }
        while (at(COMMA)) {
            isTuple = true
            advance()

            parseExpression()

        }
        expect(RPAR, CangJieParsingBundle.message("parsing.error.expecting.symbol", ")"))
        builder.restoreNewlinesState()

        when {
            isUnit -> mark.done(UNIT_CONSTANT)
            isTuple -> mark.done(TUPLE_EXPRESSION)
            else -> mark.done(PARENTHESIZED)
        }
    }

    /**
     * 解析元组字面量表达式
     *
     * Grammar:
     * ```
     * tupleLiteralExpression
     *   : TUPLE_LITERAL ")"
     *   | TUPLE_LITERAL expression ("," expression)* ")"
     *   ;
     * ```
     */
    context(context: ParsingContext)
    fun parseTupleLiteralExpression() {
        assert(_at(TUPLE_LITERAL))
        val tuple = mark()
        advance()
        if (at(RPAR)) {
            advance()
        } else {
            while (true) {
                if (at(RPAR)) {
                    break
                }

                parseExpression()

                if (at(RPAR)) {
                    break
                }
                expect(COMMA, CangJieParsingBundle.message("parsing.error.expecting.symbol", ","))
            }
            expect(RPAR, CangJieParsingBundle.message("parsing.error.expecting.symbol", ")"))
        }
        tuple.done(TUPLE_EXPRESSION)
    }

    // ==================== this 和 super 表达式 ====================

    /**
     * 解析this表达式
     *
     * Grammar:
     * ```
     * thisExpression
     *   : "this"
     *   ;
     * ```
     */
    context(context: ParsingContext)
    private fun parseThisExpression() {
        assert(_at(THIS_KEYWORD))
        val mark = mark()
        val thisReference = mark()
        advance()
        thisReference.done(REFERENCE_EXPRESSION)
        mark.done(THIS_EXPRESSION)
    }

    /**
     * 解析super表达式
     *
     * Grammar:
     * ```
     * superExpression
     *   : "super" ("<" type ">")? labelReference?
     *   ;
     * ```
     */
    context(context: ParsingContext)
    private fun parseSuperExpression() {
        assert(_at(SUPER_KEYWORD))
        val mark = mark()
        val superReference = mark()
        advance()
        superReference.done(REFERENCE_EXPRESSION)
        if (at(LT)) {
            val supertype = mark()
            builder.disableNewlines()
            advance()
            cangJieParsing.parseTypeRef()
            if (at(GT)) {
                advance()
                supertype.drop()
            } else {
                supertype.rollbackTo()
            }
            builder.restoreNewlinesState()
        }
        parseLabelReferenceWithNoWhitespace()
        mark.done(SUPER_EXPRESSION)
    }

    // ==================== 集合字面量和数组访问 ====================

    /**
     * 解析集合字面量表达式
     *
     * Grammar:
     * ```
     * collectionLiteralExpression
     *   : "[" (expression ("," expression)*)? "]"
     *   ;
     * ```
     */
    context(context: ParsingContext)
    private fun parseCollectionLiteralExpression() {
        parseAsCollectionLiteralExpression(COLLECTION_LITERAL_EXPRESSION, true, "Expecting an element")
    }

    /**
     * 解析为集合字面量表达式
     *
     * @param nodeType 节点类型
     * @param canBeEmpty 是否可以为空
     * @param missingElementErrorMessage 缺失元素时的错误消息
     */
    context(context: ParsingContext)
    private fun parseAsCollectionLiteralExpression(
        nodeType: IElementType,
        canBeEmpty: Boolean,
        missingElementErrorMessage: String,
    ) {
        assert(_at(LBRACKET) || _at(SAFE_INDEXEX))
        val innerExpressions = mark()
        builder.disableNewlines()
        advance()
        if (!canBeEmpty && at(RBRACKET)) {
            error(missingElementErrorMessage)
        } else {
            parseInnerExpressions(missingElementErrorMessage)
        }
        expect(RBRACKET, CangJieParsingBundle.message("parsing.error.expecting.symbol", "]"))
        builder.restoreNewlinesState()
        innerExpressions.done(nodeType)
    }

    /**
     * 解析内部表达式列表
     *
     * @param missingElementErrorMessage 缺失元素时的错误消息
     */
    context(context: ParsingContext)
    private fun parseInnerExpressions(
        missingElementErrorMessage: String
    ) {
        while (true) {
            if (at(COMMA)) errorAndAdvance(missingElementErrorMessage)
            if (at(RBRACKET)) {
                break
            }

            if (at(RANGE)) {
                parsePefixSliceExpression()
            } else {

                parseExpression()

            }

            if (!at(COMMA)) break
            advance()
        }
    }

    /**
     * 解析前缀切片表达式
     *
     * Grammar:
     * ```
     * prefixSliceExpression
     *   : ".." expression?
     *   ;
     * ```
     */
    context(context: ParsingContext)
    private fun parsePefixSliceExpression() {
        val mark = mark()
        advance()
        if (!at(RBRACKET)) {
            parseExpression()
        }
        mark.done(SLICE_EXPRESSION)
    }

    /**
     * 解析数组访问
     *
     * Grammar:
     * ```
     * arrayAccess
     *   : "[" expression ("," expression)* "]"
     *   ;
     * ```
     */
    context(context: ParsingContext)
    private fun parseArrayAccess() {
        parseAsCollectionLiteralExpression(INDICES, false, "Expecting an index element")
    }

    // ==================== 字符串模板解析 ====================

    /**
     * 解析字符串模板
     *
     * Grammar:
     * ```
     * stringTemplate
     *   : OPEN_QUOTE stringTemplateElement* CLOSING_QUOTE
     *   ;
     * ```
     */
    context(context: ParsingContext)
    fun parseStringTemplate() {
        assert(_at(OPEN_QUOTE))
        val template = mark()
        advance()
        while (!eof()) {
            if (at(CLOSING_QUOTE) || at(DANGLING_NEWLINE)) {
                break
            }

            parseStringTemplateElement()

        }
        if (at(DANGLING_NEWLINE)) {
            errorAndAdvance(CangJieParsingBundle.message("parsing.error.expecting.symbol", "\""))
        } else {
            expect(CLOSING_QUOTE, CangJieParsingBundle.message("parsing.error.expecting.symbol", "\""))
        }
        template.done(STRING_TEMPLATE)
    }

    /**
     * 解析字符串模板元素
     *
     * Grammar:
     * ```
     * stringTemplateElement
     *   : REGULAR_STRING_PART
     *   | ESCAPE_SEQUENCE
     *   | SHORT_TEMPLATE_ENTRY_START (IDENTIFIER | "this")
     *   | LONG_TEMPLATE_ENTRY_START statements LONG_TEMPLATE_ENTRY_END
     *   ;
     * ```
     */
    context(context: ParsingContext)
    private fun parseStringTemplateElement() {
        if (at(REGULAR_STRING_PART)) {
            val mark = mark()
            advance()
            mark.done(LITERAL_STRING_TEMPLATE_ENTRY)
        } else if (at(ESCAPE_SEQUENCE)) {
            val mark = mark()
            advance()
            mark.done(ESCAPE_STRING_TEMPLATE_ENTRY)
        } else if (at(SHORT_TEMPLATE_ENTRY_START)) {
            if (context.processStringInterpolation) {
                val entry = mark()
                advance()
                if (at(THIS_KEYWORD)) {
                    val thisExpression = mark()
                    val reference = mark()
                    advance()
                    reference.done(REFERENCE_EXPRESSION)
                    thisExpression.done(THIS_EXPRESSION)
                } else {
                    val keyword: CjToken? = KEYWORD_TEXTS.get(builder.tokenText)
                    if (keyword != null) {
                        builder.remapCurrentToken(keyword)
                        errorAndAdvance(CangJieParsingBundle.message("parsing.error.keyword.cannot.be.reference"))
                    } else {
                        val reference = mark()
                        expect(IDENTIFIER, CangJieParsingBundle.message("parsing.error.expecting", "a name"))
                        reference.done(REFERENCE_EXPRESSION)
                    }
                }
                entry.done(SHORT_STRING_TEMPLATE_ENTRY)
            } else {
                // 当不处理插值时，将其作为普通字符串部分消耗
                val mark = mark()
                advance()
                // 跳过标识符或 this
                if (at(THIS_KEYWORD) || at(IDENTIFIER)) {
                    advance()
                }
                mark.done(LITERAL_STRING_TEMPLATE_ENTRY)
            }
        } else if (at(LONG_TEMPLATE_ENTRY_START)) {
            if (context.processStringInterpolation) {
                val longTemplateEntry = mark()
                advance()
                while (!eof()) {
                    val offset = builder.currentOffset
                    parseStatementsByStringTemplate()
                    if (_at(LONG_TEMPLATE_ENTRY_END)) {
                        advance()
                        break
                    } else {
                        error(CangJieParsingBundle.message("parsing.error.expecting.symbol", "}"))
                        if (offset == builder.currentOffset) {
                            advance()
                        }
                    }
                }
                longTemplateEntry.done(LONG_STRING_TEMPLATE_ENTRY)
            } else {
                // 当不处理插值时，将其作为普通字符串部分消耗
                val mark = mark()
                advance()
                // 跳过所有内容直到遇到结束符
                var braceCount = 1
                while (!eof() && braceCount > 0) {
                    if (at(LBRACE)) {
                        braceCount++
                    } else if (at(LONG_TEMPLATE_ENTRY_END)) {
                        braceCount--
                    }
                    if (braceCount > 0) {
                        advance()
                    }
                }
                if (at(LONG_TEMPLATE_ENTRY_END)) {
                    advance()
                }
                mark.done(LITERAL_STRING_TEMPLATE_ENTRY)
            }
        } else {
            errorAndAdvance(CangJieParsingBundle.message("parsing.error.unexpected.token.string.template"))
        }
    }

    // ==================== 控制流表达式 ====================

    /**
     * 解析if表达式
     *
     * Grammar:
     * ```
     * ifExpression
     *   : "if" condition thenBranch (SEMI? "else" elseBranch)?
     *   ;
     *
     * thenBranch
     *   : block
     *   ;
     *
     * elseBranch
     *   : block
     *   | ifExpression
     *   ;
     * ```
     */
    context(context: ParsingContext)
    private fun parseIf() {
        assert(_at(IF_KEYWORD))
        val marker = mark()
        advance()

        parseCondition()

        val thenBranch = mark()
        if (!at(ELSE_KEYWORD) && !at(SEMICOLON)) {
            parseControlStructureBody()
        }
        if (at(SEMICOLON) && lookahead(1) === ELSE_KEYWORD) {
            advance()
        }
        thenBranch.done(THEN)

        if (at(ELSE_KEYWORD) && lookahead(1) !== ARROW) {
            advance()
            val elseBranch = mark()
            if (!at(SEMICOLON)) {
                if (!at(LBRACE) && !at(IF_KEYWORD)) {
                    error(CangJieParsingBundle.message("parsing.error.code.block.or.if"))
                } else if (at(IF_KEYWORD)) {

                    parseBlockLevelExpression()

                } else {
                    parseControlStructureBody()
                }
            }
            elseBranch.done(ELSE)
        }

        marker.done(IF)
    }

    /**
     * 解析match表达式
     *
     * Grammar:
     * ```
     * matchExpression
     *   : "match" condition? "{" matchEntry* "}"
     *   ;
     * ```
     */
    context(context: ParsingContext)
    private fun parseMatch() {
        assert(_at(MATCH_KEYWORD))
        val match = mark()
        advance()

        builder.disableNewlines()
        val matchContext = if (at(LPAR)) {
            parseCondition()
            context
        } else {
            context.copy(isExpression = true)
        }

        builder.restoreNewlinesState()

        builder.enableNewlines()
        if (expect(LBRACE, CangJieParsingBundle.message("parsing.error.expecting.symbol", "{"))) {
            while (!eof() && !at(RBRACE)) {
                if (!at(CASE_KEYWORD)) {
                    errorAndAdvance(CangJieParsingBundle.message("parsing.error.expecting.keyword", "case"))
                } else {
                    with(matchContext) {
                        parseMatchEntry()
                    }
                }
            }
            expect(RBRACE, CangJieParsingBundle.message("parsing.error.expecting.symbol", "}"))
        }
        builder.restoreNewlinesState()
        match.done(MATCH)
    }

    /**
     * 解析match条目
     *
     * Grammar:
     * ```
     * matchEntry
     *   : "case" casePattern ("|" casePattern)* "=>" caseBody
     *   ;
     * ```
     */
    context(context: ParsingContext)
    private fun parseMatchEntry() {
        val entry = mark()

        if (at(CASE_KEYWORD)) {
            advance()

            parseCasePattern()
            while (at(OR)) {
                advance()
                parseCasePattern()
            }
            expect(DOUBLE_ARROW, CangJieParsingBundle.message("parsing.error.expecting.symbol", "=>"))
            parseCaseBody()
        } else {
            error(CangJieParsingBundle.message("parsing.error.expecting.keyword", "case"))
        }

        entry.done(MATCH_ENTRY)
    }

    /**
     * 解析case体
     *
     * Grammar:
     * ```
     * caseBody
     *   : statements
     *   ;
     * ```
     */
    context(context: ParsingContext)
    private fun parseCaseBody() {
        val body = mark()
        if (!at(SEMICOLON)) {
            if (at(RBRACE) || at(CASE_KEYWORD)) {
                error(CangJieParsingBundle.message("parsing.error.match.case.cannot.be.empty"))
                body.drop()
                return
            } else {
                parseStatements(CASE_KEYWORD)
            }
        }
        body.done(CASE_BLOCK)
    }

    /**
     * 解析try表达式
     *
     * Grammar:
     * ```
     * tryExpression
     *   : "try" ("(" tryResourceList ")")? block catchBlock* finallyBlock?
     *   ;
     *
     * tryResourceList
     *   : tryResource ("," tryResource)*
     *   ;
     *
     * tryResource
     *   : IDENTIFIER "=" expression
     *   ;
     *
     * catchBlock
     *   : "catch" "(" ("_" | IDENTIFIER) ":" type ("|" type)* ")" block
     *   ;
     *
     * finallyBlock
     *   : "finally" block
     *   ;
     * ```
     */
    context(context: ParsingContext)
    private fun parseTry() {
        assert(_at(TRY_KEYWORD))
        val tryExpression = mark()
        advance()

        var isTryWithResources = false
        if (at(LPAR)) {
            isTryWithResources = true
            advance()

            val resourceList = mark()
            do {
                if (at(COMMA)) advance()
                val resource = mark()

                val valueParameter = mark()
                expect(IDENTIFIER, CangJieParsingBundle.message("parsing.error.expecting", "resource name"))
                valueParameter.done(VALUE_PARAMETER)

                if (expect(
                        EQ,
                        CangJieParsingBundle.message("parsing.error.expecting.symbol", "="),
                        TRY_CATCH_RECOVERY_TOKEN_SET
                    )
                ) {

                    parseExpression()

                }

                resource.done(TRY_RESOURCE)
            } while (at(COMMA))

            resourceList.done(TRY_RESOURCE_LIST)
            expect(RPAR, CangJieParsingBundle.message("parsing.error.expecting.symbol", ")"))
        }

        cangJieParsing.parseBlock()
        var catchOrFinally = false
        while (at(CATCH_KEYWORD)) {
            catchOrFinally = true
            val catchBlock = mark()
            advance()
            val parameter = mark()
            if (expect(
                    LPAR,
                    CangJieParsingBundle.message("parsing.error.expecting.symbol", "("),
                    TRY_CATCH_RECOVERY_TOKEN_SET
                )
            ) {
                if (!atSet(TRY_CATCH_RECOVERY_TOKEN_SET)) {
                    if (at(UNDERLINE)) {
                        advance()
                        if (at(COLON)) {
                            advance()
                            parseTypeReferencesByOr()
                        }
                    } else {
                        expect(
                            IDENTIFIER,
                            CangJieParsingBundle.message("parsing.error.expecting", "exception variable name")
                        )

                        if (expect(COLON, CangJieParsingBundle.message("parsing.error.expecting.symbol", ":"))) {
                            parseTypeReferencesByOr()
                        }
                    }

                    expect(
                        RPAR,
                        CangJieParsingBundle.message("parsing.error.expecting.symbol", ")"),
                        TRY_CATCH_RECOVERY_TOKEN_SET
                    )
                } else {
                    error(CangJieParsingBundle.message("parsing.error.exception.variable.declaration"))
                }
            }

            parameter.done(CATCH_PARAMETER)
            if (at(LBRACE)) {
                cangJieParsing.parseBlock()
            } else {
                error(CangJieParsingBundle.message("parsing.error.block"))
            }
            catchBlock.done(CATCH)
        }
        if (at(FINALLY_KEYWORD)) {
            catchOrFinally = true
            val finallyBlock = mark()
            advance()
            cangJieParsing.parseBlock()
            finallyBlock.done(FINALLY)
        }
        if (!catchOrFinally && !isTryWithResources) {
            error(CangJieParsingBundle.message("parsing.error.catch.or.finally"))
        }
        tryExpression.done(TRY)
    }

    /**
     * 解析由|分隔的类型引用列表
     *
     * Grammar:
     * ```
     * typeReferencesByOr
     *   : type ("|" type)*
     *   ;
     * ```
     */
    context(context: ParsingContext)
    private fun parseTypeReferencesByOr() {
        do {
            if (at(OR)) advance()
            cangJieParsing.parseTypeRef()
        } while (at(OR))
    }

    // ==================== 循环语句 ====================

    /**
     * 解析for循环表达式
     *
     * Grammar:
     * ```
     * forExpression
     *   : "for" "(" modifiers? pattern "in" expression patternGuard? ")" loopBody
     *   ;
     * ```
     */
    context(context: ParsingContext)
    private fun parseFor() {
        assert(_at(FOR_KEYWORD))
        val loop = mark()
        advance()
        if (expect(LPAR, CangJieParsingBundle.message("parsing.error.loop.range"), EXPRESSION_FIRST)) {
            builder.disableNewlines()
            if (!at(RPAR)) {
                if (!at(IN_KEYWORD)) {
                    cangJieParsing.parseModifierList(IN_KEYWORD_R_PAR_COLON_SET)
                }

                PatternParser(PatternParseContext.DEFAULT).parsePattern()

                if (expect(
                        IN_KEYWORD,
                        CangJieParsingBundle.message("parsing.error.expecting.in"),
                        L_PAR_L_BRACE_R_PAR_SET
                    )
                ) {
                    val range = mark()

                    parseExpression()

                    range.done(LOOP_RANGE)
                }

                if (at(WHERE_KEYWORD)) {
                    parsePatternGuard()
                }
            } else {
                error(CangJieParsingBundle.message("parsing.error.variable.name"))
            }
            expectNoAdvance(RPAR, CangJieParsingBundle.message("parsing.error.expecting.symbol", ")"))
            builder.restoreNewlinesState()
        }
        parseLoopBody()
        loop.done(FOR)
    }

    /**
     * 解析while循环表达式
     *
     * Grammar:
     * ```
     * whileExpression
     *   : "while" condition loopBody
     *   ;
     * ```
     */
    context(context: ParsingContext)
    private fun parseWhile() {
        assert(_at(WHILE_KEYWORD))
        val loop = mark()
        advance()
        parseCondition()
        parseLoopBody()
        loop.done(WHILE)
    }

    /**
     * 解析do-while循环表达式
     *
     * Grammar:
     * ```
     * doWhileExpression
     *   : "do" loopBody "while" condition
     *   ;
     * ```
     */
    context(context: ParsingContext)
    private fun parseDoWhile() {
        assert(_at(DO_KEYWORD))
        val loop = mark()
        advance()
        if (!at(WHILE_KEYWORD)) {
            parseLoopBody()
        }
        if (expect(WHILE_KEYWORD, CangJieParsingBundle.message("parsing.error.while.post.condition"))) {
            parseCondition()
        }
        loop.done(DO_WHILE)
    }

    /**
     * 解析循环体
     *
     * Grammar:
     * ```
     * loopBody
     *   : block
     *   ;
     * ```
     */
    context(context: ParsingContext)
    private fun parseLoopBody() {
        val body = mark()
        if (!at(SEMICOLON)) {
            parseControlStructureBody()
        }
        body.done(BODY)
    }

    /**
     * 解析条件表达式
     *
     * Grammar:
     * ```
     * condition
     *   : "(" expression ")"
     *   ;
     * ```
     */
    context(context: ParsingContext)
    private fun parseCondition() {
        builder.disableNewlines()
        if (expect(
                LPAR,
                "Expecting a condition in parentheses '(...)'",
                EXPRESSION_FIRST,
            )
        ) {
            val condition = mark()

            with(context.copy(allowLetExpression = true)) {
                parseExpression()
            }

            condition.done(CONDITION)
            expect(RPAR, CangJieParsingBundle.message("parsing.error.expecting.symbol", ")"))
        }
        builder.restoreNewlinesState()
    }

    // ==================== 跳转语句 ====================

    /**
     * 解析throw表达式
     *
     * Grammar:
     * ```
     * throwExpression
     *   : "throw" expression
     *   ;
     * ```
     */
    context(context: ParsingContext)
    private fun parseThrow() {
        assert(_at(THROW_KEYWORD))
        val marker = mark()
        advance()
        parseExpression()
        marker.done(THROW)
    }

    /**
     * 解析return表达式
     *
     * Grammar:
     * ```
     * returnExpression
     *   : "return" expression?
     *   ;
     * ```
     */
    context(context: ParsingContext)
    private fun parseReturn() {
        assert(_at(RETURN_KEYWORD))
        val returnExpression = mark()
        advance()
        if (atSet(EXPRESSION_FIRST) && !at(EOL_OR_SEMICOLON)) {
            parseExpression()
        }
        returnExpression.done(RETURN)
    }

    /**
     * 解析跳转表达式
     *
     * Grammar:
     * ```
     * jumpExpression
     *   : "break"
     *   | "continue"
     *   ;
     * ```
     *
     * @param type 跳转类型(BREAK或CONTINUE)
     */
    context(context: ParsingContext)
    private fun parseJump(type: IElementType) {
        assert(_at(BREAK_KEYWORD) || _at(CONTINUE_KEYWORD))
        val marker = mark()
        advance()
        marker.done(type)
    }

    // ==================== let 表达式 ====================

    /**
     * 解析let表达式
     *
     * Grammar:
     * ```
     * letExpression
     *   : "let" deconstructPattern "<-" expression
     *   ;
     * ```
     */
    context(context: ParsingContext)
    private fun parseLetExpression() {
        assert(_at(LET_KEYWORD))
        val expr = mark()

        advance()

        parseDeconstructPattern()

        if (at(LEFT_ARROW)) {
            advance()
        } else {
            error(CangJieParsingBundle.message("parsing.error.left.arrow.in.let"))
        }

        parseExpression()

        expr.done(LET_EXPRESSION)
    }

    /**
     * 解析解构模式
     */
    context(context: ParsingContext)
    private fun parseDeconstructPattern() {

        do {
            parseCasePattern()
        } while (expect(OR))

    }

    // ==================== 模式解析 ====================

    /**
     * 模式解析上下文
     *
     * 用于控制在不同场景下允许解析哪些类型的模式
     *
     * Grammar:
     * ```
     * pattern
     *   : constantPattern
     *   | wildcardPattern
     *   | bindingPattern
     *   | tuplePattern
     *   | typePattern
     *   | enumPattern
     *   ;
     *
     * constantPattern
     *   : INTEGER_LITERAL
     *   | RUNE_LITERAL
     *   | CHARACTER_BYTE_LITERAL
     *   | FLOAT_LITERAL
     *   | TRUE_KEYWORD
     *   | FALSE_KEYWORD
     *   | stringTemplate
     *   ;
     *
     * wildcardPattern
     *   : "_"
     *   ;
     *
     * bindingPattern
     *   : IDENTIFIER
     *   ;
     *
     * tuplePattern
     *   : "(" pattern ("," pattern)+ ")"
     *   ;
     *
     * typePattern
     *   : (IDENTIFIER | "_") ":" type
     *   ;
     *
     * enumPattern
     *   : qualifiedName ("(" expression ("," expression)* ")")?
     *   ;
     * ```
     *
     * @property allowedPatterns 允许的模式类型集合
     * @property isVariableDeclaration 是否为变量声明
     * @property nestingLevel 嵌套层级
     */
    data class PatternParseContext(
        val allowedPatterns: Set<PatternType> = PatternType.ALL_PATTERNS,
        val isVariableDeclaration: Boolean = false,
        val nestingLevel: Int = 0,
    ) {
        companion object {
            val DEFAULT = PatternParseContext()
            val VARIABLE_DECL = PatternParseContext(
                allowedPatterns = setOf(
                    PatternType.WILDCARD,
                    PatternType.BINDING,
                    PatternType.TUPLE,
                    PatternType.ENUM
                ),
                isVariableDeclaration = true
            )
        }

        fun withNestingLevel(level: Int): PatternParseContext {
            return copy(nestingLevel = level)
        }

        fun isPatternAllowed(pattern: PatternType): Boolean {
            return allowedPatterns.contains(pattern)
        }
    }

    inner class PatternParser(
        private val context: PatternParseContext
    ) {
        context(context: ParsingContext)
        fun parsePattern() {
            val constantPattern = mark()

            when (getTokenId()) {
                UNDERLINE_Id -> {
                    constantPattern.drop()
                    parseUnderlinePattern()
                }

                LPAR_Id -> {
                    constantPattern.drop()
                    parseParenthesizedPattern()
                }

                INTEGER_LITERAL_Id -> {
                    parseOneTokenExpression(INTEGER_CONSTANT)
                    doneConstantPattern(constantPattern)
                }

                RUNE_LITERAL_Id -> {
                    parseOneTokenExpression(RUNE_CONSTANT)
                    doneConstantPattern(constantPattern)
                }

                CHARACTER_BYTE_LITERAL_Id -> {
                    parseOneTokenExpression(CHARACTER_BYTE_CONSTANT)
                    doneConstantPattern(constantPattern)
                }

                TRUE_KEYWORD_Id, FALSE_KEYWORD_Id -> {
                    parseOneTokenExpression(BOOLEAN_CONSTANT)
                    doneConstantPattern(constantPattern)
                }

                FLOAT_LITERAL_Id -> {
                    parseOneTokenExpression(FLOAT_CONSTANT)
                    doneConstantPattern(constantPattern)
                }

                OPEN_QUOTE_Id -> {
                    parseStringTemplate()
                    doneConstantPattern(constantPattern)
                }

                IDENTIFIER_Id -> {
                    constantPattern.drop()
                    parseIdentifierPattern()
                }

                else -> {
                    error(CangJieParsingBundle.message("parsing.error.pattern.expression"))
                    constantPattern.drop()
                }
            }
        }

        context(parsingcontext: ParsingContext)
        fun parseIdentifierPattern() {
            assert(_at(IDENTIFIER))
            var mark = mark()
            var recognized = RecognizedPattern.BINDING

            if (lookahead(1) == DOT || lookahead(1) == LT) {

                parseReferenceExpression()

                recognized = RecognizedPattern.ENUM

                if (at(LPAR)) {
                    parseEnumPatternArguments()
                }
            } else {

                parseReferenceExpression()


                if (at(COLON) && (!context.isVariableDeclaration || context.nestingLevel > 0)) {
                    advance()
                    recognized = RecognizedPattern.TYPE
                    cangJieParsing.parseTypeRef()
                } else if (at(LPAR)) {
                    mark.rollbackTo()
                    mark = mark()

                    parseReferenceExpression()

                    recognized = RecognizedPattern.ENUM
                    parseEnumPatternArguments()
                }
            }

            validateAndDonePattern(mark, recognized.toPatternType(), recognized.toNodeType())
        }

        context(parsingcontext: ParsingContext)
        fun parseUnderlinePattern() {
            val pattern = mark()
            assert(_at(UNDERLINE))
            advance()

            if (at(COLON) && !context.isVariableDeclaration) {
                advance()
                cangJieParsing.parseTypeRef()
                validateAndDonePattern(pattern, PatternType.TYPE, TYPE_PATTERN)
            } else {
                validateAndDonePattern(pattern, PatternType.WILDCARD, WILDCARD_PATTERN)
            }
        }

        context(parsingcontext: ParsingContext)
        fun parseParenthesizedPattern() {
            assert(_at(LPAR))

            var isUnit = false
            var isTuple = false

            val mark = mark()
            builder.disableNewlines()
            advance()

            if (at(RPAR)) {
                isUnit = true
            } else {
                isTuple = true
                val nestedContext = context.withNestingLevel(context.nestingLevel + 1)
                val nestedParser = PatternParser(nestedContext)
                nestedParser.parseTuplePatternElements()
            }
            expect(RPAR, CangJieParsingBundle.message("parsing.error.expecting.symbol", ")"))

            when {
                isUnit -> {
                    mark().done(UNIT_CONSTANT)
                    doneConstantPattern(mark)
                }

                isTuple -> {
                    validateAndDonePattern(mark, PatternType.TUPLE, TUPLE_PATTERN)
                }

                else -> mark.drop()
            }

            builder.restoreNewlinesState()
        }

        context(parsingcontext: ParsingContext)
        private fun parseTuplePatternElements() {
            parsePattern()
            if (at(COMMA)) {
                advance()
                parsePattern()
                while (at(COMMA)) {
                    advance()
                    parsePattern()
                }
            } else {
                error(CangJieParsingBundle.message("parsing.error.tuple.expression"))
            }
        }

        context(parsingcontext: ParsingContext)
        private fun parseEnumPatternArguments() {
            advance()
            parseExpression()
            while (at(COMMA)) {
                advance()
                parseExpression()
            }
            expect(RPAR, CangJieParsingBundle.message("parsing.error.expecting.symbol", ")"))
        }

        private fun validateAndDonePattern(
            marker: PsiBuilder.Marker,
            patternType: PatternType,
            nodeType: IElementType
        ) {
            if (context.isPatternAllowed(patternType) || context.nestingLevel > 0) {
                marker.done(nodeType)
            } else {
                marker.error(
                    CangJieParsingBundle.message(
                        "parsing.error.pattern.not.supported",
                        patternType.displayName
                    )
                )
            }
        }

        private fun doneConstantPattern(constantPattern: PsiBuilder.Marker) {
            validateAndDonePattern(constantPattern, PatternType.CONSTANT, CONSTANT_PATTERN)
        }
    }

    /**
     * 解析模式(入口方法)
     *
     * @param context 模式解析上下文
     */
    context(parsingcontext: ParsingContext)
    fun parsePattern(context: PatternParseContext = PatternParseContext.DEFAULT) {
        val parser = PatternParser(context)
        parser.parsePattern()
    }

    /**
     * 解析case模式
     */
    context(context: ParsingContext)
    private fun parseCasePattern() {
        val parser = PatternParser(PatternParseContext.DEFAULT)

        if (context.isExpression) {
            if (at(UNDERLINE)) {
                parser.parseUnderlinePattern()
            } else {
                val expr = mark()
                parseExpression()
                expr.done(MATCH_CONDITION_EXPRESSION)
            }
        } else {
            parser.parsePattern()
        }

        if (at(WHERE_KEYWORD)) {
            parsePatternGuard()
        }
    }

    /**
     * 解析模式守卫
     *
     * Grammar:
     * ```
     * patternGuard
     *   : "where" expression
     *   ;
     * ```
     */
    context(context: ParsingContext)
    fun parsePatternGuard() {
        assert(_at(WHERE_KEYWORD))
        val marker = mark()

        advance()

        parseExpression()

        marker.done(PATTERN_GUARD)
    }

    // ==================== 函数字面量和 Lambda ====================

    /**
     * 解析函数字面量
     *
     * Grammar:
     * ```
     * functionLiteral
     *   : "{" "=>" statements "}"
     *   | "{" parameterList "=>" statements "}"
     *   | "{" statements "}"  // block
     *   ;
     * ```
     */
    context(context: ParsingContext)
    fun parseFunctionLiteral() {
        assert(_at(LBRACE))
        val literalExpression = mark()
        val literal = mark()
        builder.enableNewlines()
        advance()
        var paramsFound = false
        val token = tt()
        if (token === DOUBLE_ARROW) {
            mark().done(VALUE_PARAMETER_LIST)
            advance()
            paramsFound = true
        } else if (token?.equal(IDENTIFIER_RECOVERY_SET) == true || token === COLON || token === LPAR) {
            val rollbackMarker = mark()
            val nextToken = lookahead(1)
            val preferParamsToExpressions =
                (nextToken === COMMA || nextToken === COLON)
            parseFunctionLiteralParameterList()

            paramsFound = if (preferParamsToExpressions) {
                rollbackOrDrop(
                    rollbackMarker,
                    DOUBLE_ARROW,
                    "An -> is expected",
                    RBRACE,
                )
            } else {
                rollbackOrDropAt(rollbackMarker, DOUBLE_ARROW)
            }
        } else if (context.isDoubleArrow) {
            error(CangJieParsingBundle.message("parsing.error.lambda.arrow.expected", builder.tokenText ?: ""))
        }

        if (!paramsFound && context.preferBlock) {
            literal.drop()
            parseStatements()
            expect(RBRACE, CangJieParsingBundle.message("parsing.error.expecting.symbol", "}"))
            literalExpression.done(BLOCK)
            builder.restoreNewlinesState()
            return
        }

        if (context.collapse && isLazy) {
            cangJieParsing.advanceBalancedBlock()
            literal.done(FUNCTION_LITERAL)
            literalExpression.collapse(LAMBDA_EXPRESSION)
        } else {
            val body = mark()
            parseStatements()

            body.done(BLOCK)
            body.setCustomEdgeTokenBinders(PRECEDING_ALL_COMMENTS_BINDER, TRAILING_ALL_COMMENTS_BINDER)

            expect(RBRACE, CangJieParsingBundle.message("parsing.error.expecting.symbol", "}"))
            literal.done(FUNCTION_LITERAL)
            literalExpression.done(LAMBDA_EXPRESSION)
        }
        builder.restoreNewlinesState()
    }

    /**
     * 解析函数字面量参数列表
     *
     * Grammar:
     * ```
     * functionLiteralParameterList
     *   : parameter ("," parameter)*
     *   ;
     *
     * parameter
     *   : IDENTIFIER (":" type)?
     *   ;
     * ```
     */
    context(context: ParsingContext)
    private fun parseFunctionLiteralParameterList() {
        val parameterList = mark()

        while (!eof()) {
            if (at(DOUBLE_ARROW)) {
                break
            }
            val parameter = mark()

            if (at(COLON)) {
                error(CangJieParsingBundle.message("parsing.error.parameter.name"))
            } else {
                expect(
                    IDENTIFIER_RECOVERY_SET,
                    "Expecting parameter name",
                    ARROW_SET,
                )
            }

            if (at(COLON)) {
                advance()
                cangJieParsing.parseTypeRef(ARROW_COMMA_SET)
            }
            parameter.done(VALUE_PARAMETER)

            if (at(DOUBLE_ARROW)) {
                break
            } else if (at(COMMA)) {
                advance()
            } else {
                error(CangJieParsingBundle.message("parsing.error.arrow.or.comma"))
                break
            }
        }

        parameterList.done(VALUE_PARAMETER_LIST)
    }

    /**
     * 解析带注解的Lambda表达式
     *
     * @return 是否成功解析
     */
    context(context: ParsingContext)
    private fun parseAnnotatedLambda(): Boolean {
        if (!at(LBRACE)) {
            return false
        }

        parseFunctionLiteral()

        return true
    }

    /**
     * 解析带闭包的调用
     *
     * Grammar:
     * ```
     * callWithClosure
     *   : lambdaArgument+
     *   ;
     * ```
     *
     * @return 是否成功解析
     */
    context(context: ParsingContext)
    protected fun parseCallWithClosure(): Boolean {
        var success = false

        while (true) {
            val argument = mark()

            with(context.copy(collapse = true, isDoubleArrow = false)) {
                if (!parseAnnotatedLambda()) {
                    argument.drop()
                    break
                }
            }

            argument.done(LAMBDA_ARGUMENT)
            success = true
        }

        return success
    }

    // ==================== 函数调用和后缀表达式 ====================

    /**
     * 解析调用后缀
     *
     * Grammar:
     * ```
     * callSuffix
     *   : valueArgumentList
     *   | lambdaArgument+
     *   ;
     * ```
     *
     * @return 是否成功解析了调用后缀
     */
    context(context: ParsingContext)
    private fun parseCallSuffix(): Boolean {
        val tokenType = getSafeTokenType()

        if (parseCallWithClosure()) {
        } else if (at(LPAR) || tokenType == SAFE_CALL) {
            parseValueArgumentList()
        } else {
            return false
        }
        return true
    }

    /**
     * 解析选择器调用表达式
     *
     * Grammar:
     * ```
     * selectorCallExpression
     *   : atomicExpression callSuffix?
     *   ;
     * ```
     */
    context(context: ParsingContext)
    private fun parseSelectorCallExpression() {
        val mark = mark()

        parseAtomicExpression()

        if (!builder.newlineBeforeCurrentToken() && parseCallSuffix()) {
            mark.done(CALL_EXPRESSION)
        } else {
            mark.drop()
        }
    }

    /**
     * 解析双冒号后缀(目前未实现)
     *
     * @param expression 表达式标记
     * @return 是否成功解析
     */
    fun parseDoubleColonSuffix(expression: PsiBuilder.Marker): Boolean {
        return false
    }

    // ==================== 值参数列表 ====================

    /**
     * 解析值参数
     *
     * Grammar:
     * ```
     * valueArgument
     *   : (IDENTIFIER ":")? expression
     *   ;
     * ```
     */
    context(context: ParsingContext)
    private fun parseValueArgument() {
        val argument = mark()
        if (at(IDENTIFIER) && lookahead(1) === COLON) {
            val argName = mark()
            val reference = mark()
            advance()
            reference.done(REFERENCE_EXPRESSION)
            argName.done(VALUE_ARGUMENT_NAME)
            advance()
        }
        parseExpression()
        argument.done(VALUE_ARGUMENT)
    }

    /**
     * 解析值参数列表(使用默认括号)
     */
    context(context: ParsingContext)
    fun parseValueArgumentList() {
        parseValueArgumentList(LPAR, RPAR)
    }

    /**
     * 解析值参数列表
     *
     * @param start 开始token
     * @param end 结束token
     */
    context(context: ParsingContext)
    fun parseValueArgumentList(
        start: CjToken = LPAR,
        end: CjToken = RPAR
    ) {
        parseValueArgumentList(Pair.create(TokenSet.create(start), end))
    }

    /**
     * 解析值参数列表
     *
     * Grammar:
     * ```
     * valueArgumentList
     *   : "(" (valueArgument ("," valueArgument)*)? ")"
     *   ;
     * ```
     *
     * @param struct 参数列表的起止符号对
     */
    context(context: ParsingContext)
    fun parseValueArgumentList(
        struct: Pair<TokenSet, CjToken> = Pair(
            TokenSet.create(
                LPAR,
                SAFE_CALL
            ), RPAR
        )
    ) {
        val list = mark()

        val sturctStart = struct.first
        val sturctEnd = struct.second

        builder.disableNewlines()
        if (expectSafeCall(sturctStart, "Expecting an argument list", EXPRESSION_FOLLOW)) {
            if (!at(sturctEnd)) {
                while (true) {
                    while (at(COMMA)) errorAndAdvance(CangJieParsingBundle.message("parsing.error.expecting.element.argument"))
                    parseValueArgument()
                    if (at(COLON) && lookahead(1) === IDENTIFIER) {
                        errorAndAdvance(CangJieParsingBundle.message("parsing.error.unexpected.type.specification"), 2)
                    }
                    if (!at(COMMA)) {
                        if (atSet(EXPRESSION_FIRST)) {
                            error(CangJieParsingBundle.message("parsing.error.expecting.symbol", ","))
                            continue
                        } else {
                            break
                        }
                    }
                    advance()
                    if (at(RPAR)) {
                        break
                    }
                }
            }
            expect(
                sturctEnd,
                CangJieParsingBundle.message("parsing.error.expecting.symbol", sturctEnd.name),
                EXPRESSION_FOLLOW
            )
        }
        builder.restoreNewlinesState()
        list.done(VALUE_ARGUMENT_LIST)
    }

    // ==================== 语句解析 ====================

    /**
     * 解析语句序列(带结束符)
     *
     * Grammar:
     * ```
     * statements
     *   : SEMI* statement (SEMI+ statement)* SEMI*
     *   ;
     * ```
     *
     * @param type 结束符类型
     */
    context(context: ParsingContext)
    fun parseStatements(type: IElementType) {
        while (at(SEMICOLON)) advance()
        while (!eof() && !at(RBRACE) && !at(type)) {
            if (!atSet(STATEMENT_FIRST)) {
                errorAndAdvance(CangJieParsingBundle.message("parsing.error.expecting.element.element"))
            }
            if (atSet(STATEMENT_FIRST)) {

                parseStatement()

            }
            if (at(SEMICOLON)) {
                while (at(SEMICOLON)) advance()
            } else if (at(RBRACE)) {
                break
            } else if (at(type)) {
                break
            } else if (!builder.newlineBeforeCurrentToken()) {
                val severalStatementsError = CangJieParsingBundle.message("parsing.error.unexpected.tokens.same.line")
                if (atSet(STATEMENT_NEW_LINE_QUICK_RECOVERY_SET)) {
                    error(severalStatementsError)
                } else {
                    errorUntil(
                        severalStatementsError,
                        TokenSet.create(EOL_OR_SEMICOLON, LBRACE, RBRACE, type),
                    )
                }
            }
        }
    }

    /**
     * 解析语句序列(默认)
     *
     * Grammar:
     * ```
     * statements
     *   : SEMI* statement (SEMI+ statement)* SEMI*
     *   ;
     * ```
     */
    context(context: ParsingContext)
    fun parseStatements() {
        while (at(SEMICOLON)) advance()
        while (!eof() && !at(RBRACE)) {
            if (!atSet(STATEMENT_FIRST)) {
                errorAndAdvance(CangJieParsingBundle.message("parsing.error.expecting.element.element"))
            }

            if (atSet(STATEMENT_FIRST)) {

                parseStatement()

            }

            if (at(SEMICOLON)) {
                while (at(SEMICOLON)) advance()
            } else if (at(RBRACE)) {
                break
            } else if (!builder.newlineBeforeCurrentToken()) {
                val severalStatementsError = CangJieParsingBundle.message("parsing.error.unexpected.tokens.same.line")
                if (atSet(STATEMENT_NEW_LINE_QUICK_RECOVERY_SET)) {
                    error(severalStatementsError)
                } else {
                    errorUntil(
                        severalStatementsError,
                        TokenSet.create(EOL_OR_SEMICOLON, LBRACE, RBRACE),
                    )
                }
            }
        }
    }

    /**
     * 解析字符串模板中的语句序列
     */
    context(context: ParsingContext)
    private fun parseStatementsByStringTemplate() {
        while (at(SEMICOLON)) advance()
        while (!eof() && !at(LONG_TEMPLATE_ENTRY_END)) {
            if (!atSet(STATEMENT_FIRST)) {
                errorAndAdvance(CangJieParsingBundle.message("parsing.error.expecting.element.element"))
            }

            if (atSet(STATEMENT_FIRST)) {

                parseStatement()

            }

            if (at(SEMICOLON)) {
                while (at(SEMICOLON)) advance()
            } else if (at(LONG_TEMPLATE_ENTRY_END)) {
                break
            } else if (!builder.newlineBeforeCurrentToken()) {
                val severalStatementsError = CangJieParsingBundle.message("parsing.error.unexpected.tokens.same.line")
                if (atSet(STATEMENT_NEW_LINE_QUICK_RECOVERY_SET)) {
                    error(severalStatementsError)
                } else {
                    val errorMarker = mark()
                    errorMarker.error(severalStatementsError)
                }
            }
        }
    }

    /**
     * 解析单个语句
     *
     * Grammar:
     * ```
     * statement
     *   : declaration
     *   | expression
     *   ;
     * ```
     */
    context(context: ParsingContext)
    fun parseStatement() {
        if (!parseLocalDeclaration(false)) {
            if (!atSet(context.expressionFirst)) {
                errorAndAdvance(CangJieParsingBundle.message("parsing.error.expecting.element.statement"))
            } else {
                parseBlockLevelExpression()
            }
        }
    }

    /**
     * 根据作用域解析语句
     *
     * @param scope 声明解析模式
     */
    context(context: ParsingContext)
    fun parseStatementByScope(scope: DeclarationParsingMode) {
        if (!parseDeclaration(scope, false)) {
            if (!atSet(EXPRESSION_FIRST)) {
                errorAndAdvance(CangJieParsingBundle.message("parsing.error.expecting.element.statement"))
            } else {

                parseBlockLevelExpression()

            }
        }
    }

    // ==================== 代码块解析 ====================

    /**
     * 解析代码块(带结束符)
     *
     * Grammar:
     * ```
     * block
     *   : "{" statements "}"
     *   ;
     * ```
     *
     * @param type 结束符类型
     */
    context(context: ParsingContext)
    fun parseBlock(type: IElementType) {
        while (!at(type) && !eof() && !at(RBRACE)) {

            parseBlockLevelExpression()

        }
    }

    /**
     * 解析代码块
     *
     * Grammar:
     * ```
     * block
     *   : "{" statements "}"
     *   ;
     * ```
     */
    context(context: ParsingContext)
    fun parseBlock() {
        assert(_at(LBRACE))
        advance()
        val block = mark()

        parseStatements()

        expect(RBRACE, CangJieParsingBundle.message("parsing.error.expecting.symbol", "}"))

        block.done(BLOCK)
    }

    /**
     * 解析控制结构或主体
     */
    context(context: ParsingContext)
    private fun parseControlStructureOrBody() {
        with(context.copy(preferBlock = true)) {
            if (!parseAnnotatedLambda()) {

                parseBlockLevelExpression()

            }
        }
    }

    /**
     * 解析控制结构主体
     *
     * Grammar:
     * ```
     * controlStructureBody
     *   : block
     *   ;
     * ```
     */
    context(context: ParsingContext)
    private fun parseControlStructureBody() {
        if (at(LBRACE)) {
            val body = mark()
            advance()
            parseStatements()

            expect(RBRACE, CangJieParsingBundle.message("parsing.error.expecting.symbol", "}"))

            body.done(BLOCK)
        } else {
            error(CangJieParsingBundle.message("parsing.error.expecting.symbol", "{"))
        }
    }

    // ==================== 声明解析 ====================

    /**
     * 解析声明
     *
     * Grammar:
     * ```
     * declaration
     *   : modifiers? declarationRest
     *   ;
     * ```
     *
     * @param scope 声明解析模式
     * @param rollbackIfDefinitelyNotExpression 如果明确不是表达式是否回滚
     * @param rollbackMacro 是否回滚宏
     * @return 是否成功解析声明
     */
    context(context: ParsingContext)
    fun parseDeclaration(
        scope: DeclarationParsingMode,
        rollbackIfDefinitelyNotExpression: Boolean,
        rollbackMacro: Boolean = false,
    ): Boolean {
        val decl: PsiBuilder.Marker = mark()
        val detector: CangJieParsing.ModifierDetector = CangJieParsing.ModifierDetector()

        cangJieParsing.parseModifierList(detector, TokenSet.EMPTY, rollbackMacro)
        val declType: IElementType? = parseDeclarationRest(detector, rollbackIfDefinitelyNotExpression, scope)

        return if (declType == ANNOTATION) {
            decl.rollbackTo()
            parseLocalDeclaration(rollbackIfDefinitelyNotExpression, true)
        } else if (declType == INVALID_DECLARATION) {
            decl.error(CangJieParsingBundle.message("parsing.error.invalid.declaration.scope"))

//            errorAndAdvance(
//                CangJieParsingBundle.message("parsing.error.invalid.declaration.scope")
//            )
//            decl.drop()


            true
        } else if (declType != null) {
            closeDeclarationWithCommentBinders(
                decl,
                declType,
                declType !== VARIABLE,
            )
            true
        } else {
            decl.rollbackTo()
            false
        }
    }

    /**
     * 解析局部声明
     *
     * @param rollbackIfDefinitelyNotExpression 如果明确不是表达式是否回滚
     * @param rollbackMacro 是否回滚宏
     * @return 是否成功解析声明
     */
    context(context: ParsingContext)
    private fun parseLocalDeclaration(
        rollbackIfDefinitelyNotExpression: Boolean,
        rollbackMacro: Boolean = false,
    ): Boolean {
        return parseDeclaration(DeclarationParsingMode.LOCAL, rollbackIfDefinitelyNotExpression, rollbackMacro)
    }

    /**
     * 解析声明的剩余部分
     *
     * @param detector 修饰符检测器
     * @param failIfDefinitelyNotExpression 如果明确不是表达式是否失败
     * @param scope 声明解析模式
     * @return 声明类型,如果解析失败则返回null
     */
    context(context: ParsingContext)
    private fun parseDeclarationRest(
        detector: CangJieParsing.ModifierDetector,
        failIfDefinitelyNotExpression: Boolean,
        scope: DeclarationParsingMode,
    ): IElementType? {
        val keyword = tt()
        if (failIfDefinitelyNotExpression) {
            if (keyword != FUNC_KEYWORD) return null
            return cangJieParsing.parseFunction()
        }

        return cangJieParsing.parseCommonDeclaration(
            detector,
            NameParsingMode.REQUIRED,
            scope,
        )
    }

    // ==================== 特殊表达式 ====================

    /**
     * 解析unsafe表达式
     *
     * Grammar:
     * ```
     * unsafeExpression
     *   : "unsafe" block
     *   ;
     * ```
     */
    context(context: ParsingContext)
    fun parseUnsafeExpression() {
        assert(_at(UNSAFE_KEYWORD))
        val unsafe = mark()

        advance()

        cangJieParsing.parseBlock()
        unsafe.done(UNSAFE_EXPRESSION)
    }

    /**
     * 解析spawn表达式
     *
     * Grammar:
     * ```
     * spawnExpression
     *   : "spawn" lambdaArgument+
     *   ;
     * ```
     */
    context(context: ParsingContext)
    fun parseSpawnExpression() {
        assert(_at(SPAWN_KEYWORD))
        val spawn = mark()
        advance()

        parseCallWithClosure()

        spawn.done(SPAWN_EXPRESSION)
    }

    // ==================== 引用(Quote)表达式 ====================

    /**
     * 解析quote表达式
     *
     * Grammar:
     * ```
     * quoteExpression
     *   : "quote" "(" quoteParameters ")" quoteBody
     *   ;
     * ```
     */
    context(context: ParsingContext)
    fun parseQuoteExpression() {
        assert(_at(QUOTE_KEYWORD))

        val quoteExpression = mark()
        advance()

        expect(LPAR, CangJieParsingBundle.message("parsing.error.expecting.symbol", "(")) {
            parseQuoteParameters()
        }
        expect(RPAR, CangJieParsingBundle.message("parsing.error.expecting.symbol", ")"))
        quoteExpression.done(QUOTE_EXPRESSION)

//        quoteExpressionParsing.parseQuoteExpression()
    }

    /**
     * 解析quote参数
     *
     * Grammar:
     * ```
     * quoteParameters
     *   : (quoteToken | quoteInterpolate | macroExpression)+
     *   ;
     * ```
     */
    context(context: ParsingContext)
    private fun parseQuoteParameters() {
//        TODO 后期可能要根据实际代码解析为对应结构
        val quoteParameters = mark()

        var lparCount = 0

        do {
            if (at(DOLLAR) && lookahead(1) == LPAR) {
                parseQuoteInterpolate()
            } else if (at(AT) && lookahead(1) == IDENTIFIER) {
                parseMacroExpressionByQuoteParameters()
            } else if (atSet(QUOTE_TOKENS)) {
                if (at(LPAR)) {
                    lparCount++
                } else if (at(RPAR)) {
                    lparCount--
                    if (lparCount < 0) {
                        break
                    }
                } else if (at(DOLLAR)) {
                    errorAndAdvance(CangJieParsingBundle.message("parsing.error.expected.identifier.after.dollar"))
                } else if (at(ESCAPE_LBRACKET) || at(ESCAPE_RBRACKET)) {
                    errorAndAdvance("Illegal Token")
                }
                advance()
            }
        } while (atSet(QUOTE_TOKENS))
        quoteParameters.done(QUOTE_PARAMETERS)
    }

    /**
     * 解析quote插值
     *
     * Grammar:
     * ```
     * quoteInterpolate
     *   : "$" "(" expression ")"
     *   ;
     * ```
     */
    context(context: ParsingContext)
    private fun parseQuoteInterpolate() {
        assert(_at(DOLLAR) && lookahead(1) == LPAR)
        val mark = mark()

        advance()
        advance()
        parseExpression()

        if (at(RPAR)) {
            advance()
        } else {
            error("expected ')' after '$'")
        }

        mark.done(QUOTE_INTERPOLATE)
    }

    // ==================== 宏表达式 ====================

    /**
     * 解析宏表达式(用于quote参数中)
     *
     * Grammar:
     * ```
     * macroExpression
     *   : "@" IDENTIFIER macroAttr? macroInput
     *   ;
     * ```
     */
    context(context: ParsingContext)
    fun parseMacroExpressionByQuoteParameters() {
        assert(_at(AT))

        val macroExpression = mark()
        advance()

        val simpleName = mark()
        if (at(IDENTIFIER)) {
            advance()
            simpleName.done(REFERENCE_EXPRESSION)
        } else {
            simpleName.drop()
            macroExpression.drop()
        }

        if (at(LBRACKET)) {
            parseMacroAttrExpression()
        }
        val input = mark()
        if (at(LPAR)) {
            parseMacroInputExprWithParens()
        } else {
            val decl = mark()

            val productionsSize = productions.size
            val modifiterDetector = CangJieParsing.ModifierDetector()

            cangJieParsing.parseModifierList(modifiterDetector, TokenSet.EMPTY)

            val declType = parseMacroInputExprWithoutParens(modifiterDetector)
            productions.subList(
                productionsSize,
                productions.size,
            ).any {
                it.tokenType == TokenType.ERROR_ELEMENT
            }
            if (declType == null || productions.subList(
                    productionsSize, productions.size,
                ).any {
                    it.tokenType == TokenType.ERROR_ELEMENT
                }
            ) {
                decl.drop()
                input.drop()
                macroExpression.rollbackTo()
                advance()
                return
            } else {
                closeDeclarationWithCommentBinders(decl, declType, true)
            }
        }
        input.done(MACRO_INPUT)

        macroExpression.done(MACRO_EXPRESSION)
    }

    /**
     * 解析宏表达式
     *
     * Grammar:
     * ```
     * macroExpression
     *   : "@" IDENTIFIER macroAttr? macroInput
     *   ;
     * ```
     *
     * @return 宏表达式类型,如果解析失败则返回null
     */
    context(context: ParsingContext)
    fun parseMacroExpression(): IElementType? {


        assert(_at(AT))

        val macroExpression = mark()
        advance()

        val simpleName = mark()
        if (at(IDENTIFIER)) {
            advance()
            simpleName.done(REFERENCE_EXPRESSION)
        } else {
            simpleName.drop()
            error("expected identifier after '@'")
            macroExpression.drop()

            return null
        }

        if (at(LBRACKET)) {
            parseMacroAttrExpression()
        }

        val input = mark()
        if (at(LPAR)) {
            parseMacroInputExprWithParens()
        } else {
            val decl = mark()

            val modifiterDetector = CangJieParsing.ModifierDetector()

            cangJieParsing.parseModifierList(modifiterDetector, TokenSet.EMPTY)

            val declType = parseMacroInputExprWithoutParens(modifiterDetector)

            if (declType == null) {
                error("Macro call has no input")
                decl.drop()
            } else {
                closeDeclarationWithCommentBinders(decl, declType, true)
            }
        }
        input.done(MACRO_INPUT)
        if (context.backToken) {
            macroExpression.drop()
            return MACRO_EXPRESSION
        } else {
            macroExpression.done(MACRO_EXPRESSION)
            return null
        }
    }

    /**
     * 解析宏输入表达式(不带括号)
     *
     * @param modifiterDetector 修饰符检测器
     * @return 声明类型,如果解析失败则返回null
     */
    context(context: ParsingContext)
    private fun parseMacroInputExprWithoutParens(
        modifiterDetector: CangJieParsing.ModifierDetector
    ): IElementType? {
        var declType = parseMacroInputExprWithoutParensDeclaration(modifiterDetector)

        if (declType == null) {
            if (at(IDENTIFIER) && modifiterDetector.count <= 0) {
                advance()
                if (at(LPAR)) {
                    if (lookahead(1) == IDENTIFIER && lookahead(2) === COLON || lookahead(1) == RPAR) {
                        cangJieParsing.parsePrimaryInitFuncValueParameterList()

                        if (at(LBRACE)) {
                            cangJieParsing.parseFunctionBody()
                        } else {
                            error("Expecting '{' ")
                        }

                        declType = PRIMARY_CONSTRUCTOR
                    }
                }
            } else if (at(IDENTIFIER) && lookahead(1) === LPAR) {
                cangJieParsing.parsePrimaryInitFunc()
                declType = PRIMARY_CONSTRUCTOR
            }
        }

        return declType
    }

    /**
     * 解析宏输入表达式(不带括号的声明)
     *
     * Grammar:
     * ```
     * macroInputExprWithoutParensDeclaration
     *   : macroExpression
     *   | functionDefinition
     *   | classDefinition
     *   | variableDeclaration
     *   | propertyDefinition
     *   | initDefinition
     *   ;
     * ```
     *
     * @param modifiterDetector 修饰符检测器
     * @return 声明类型,如果解析失败则返回null
     */
    context(context: ParsingContext)
    private fun parseMacroInputExprWithoutParensDeclaration(
        modifiterDetector: CangJieParsing.ModifierDetector
    ): IElementType? {
        return when (getTokenId()) {
            AT_Id -> with(context.copy(backToken = true)) { parseMacroExpression() }
            FUNC_KEYWORD_Id -> cangJieParsing.parseFunction(detector = modifiterDetector)
            EXTEND_KEYWORD_Id, ENUM_KEYWORD_Id, STRUCT_KEYWORD_Id, INTERFACE_KEYWORD_Id, CLASS_KEYWORD_Id ->
                cangJieParsing.parseClass(modifiterDetector)

            LET_KEYWORD_Id, VAR_KEYWORD_Id, CONST_KEYWORD_Id -> cangJieParsing.parseVariable(modifiterDetector)

            PROP_KEYWORD_Id -> cangJieParsing.parseProperty(detector = modifiterDetector)
            INIT_KEYWORD_Id -> {
                cangJieParsing.parseInitFunc()
                SECONDARY_CONSTRUCTOR
            }

            else -> null
        }
    }

    /**
     * 解析宏输入表达式(带括号)
     *
     * Grammar:
     * ```
     * macroInputExprWithParens
     *   : "(" quoteTokens ")"
     *   ;
     * ```
     */
    context(context: ParsingContext)
    private fun parseMacroInputExprWithParens() {
        assert(_at(LPAR))
        advance()

        var lparCount = 0
        val tokens = mark()
        while (atSet(QUOTE_TOKENS)) {
            if (at(AT) && lookahead(1) == IDENTIFIER) {

                parseMacroExpression()

            } else {
                if (at(LPAR)) {
                    lparCount++
                } else if (at(RPAR)) {
                    lparCount--
                    if (lparCount < 0) {
                        break
                    }
                } else if (at(DOLLAR)) {
                    errorAndAdvance(CangJieParsingBundle.message("parsing.error.expected.identifier.after.dollar"))
                } else if (at(ESCAPE_LBRACKET) || at(ESCAPE_RBRACKET)) {
                    errorAndAdvance("Illegal Token")
                }
                advance()
            }
        }
        tokens.done(CjNodeTypes.QUOTE_TOKENS)

        expect(RPAR, CangJieParsingBundle.message("parsing.error.expecting.symbol", ")"))
    }

    /**
     * 解析宏属性表达式
     *
     * Grammar:
     * ```
     * macroAttrExpression
     *   : "[" quoteTokens "]"
     *   ;
     * ```
     */
    context(context: ParsingContext)
    private fun parseMacroAttrExpression() {
        val attr = mark()
        if (at(LBRACKET)) {
            advance()
            val tokens = mark()
            var lparCount = 0
            while (atSet(QUOTE_TOKENS)) {
                if (at(LBRACKET)) {
                    lparCount++
                } else if (at(RBRACKET)) {
                    lparCount--
                    if (lparCount < 0) {
                        break
                    }
                } else if (at(OPEN_QUOTE)) {
                    parseStringTemplate()
                    continue
                } else if (at(DOLLAR)) {
                    errorAndAdvance(CangJieParsingBundle.message("parsing.error.expected.identifier.after.dollar"))
                } else if (at(ESCAPE_LPAR) || at(ESCAPE_RPAR)) {
                    errorAndAdvance("Illegal Token")
                }
                advance()
            }
            tokens.done(CjNodeTypes.QUOTE_TOKENS)
            expect(RBRACKET, CangJieParsingBundle.message("parsing.error.expecting.symbol", "]"))
        } else {
            error("expected '['")
        }
        attr.done(MACRO_ATTR)
    }

    // ==================== 标签引用 ====================

    /**
     * 解析标签引用(不允许@前有空格)
     *
     * Grammar:
     * ```
     * labelReference
     *   : "@" IDENTIFIER  // no whitespace before @
     *   ;
     * ```
     */
    context(context: ParsingContext)
    private fun parseLabelReferenceWithNoWhitespace() {
        if (at(AT) && !builder.newlineBeforeCurrentToken()) {
            if (WHITE_SPACE_OR_COMMENT_BIT_SET.contains(builder.rawLookup(-1))) {
                error("There should be no space or comments before '@' in label reference")
            }
            parseLabelReference()
        }
    }

    /**
     * 解析标签引用
     *
     * Grammar:
     * ```
     * labelReference
     *   : "@" IDENTIFIER
     *   ;
     * ```
     */
    context(context: ParsingContext)
    private fun parseLabelReference() {
        assert(_at(AT))
        val labelWrap = mark()
        val mark = mark()
        if (builder.rawLookup(1) !== IDENTIFIER) {
            errorAndAdvance("Label must be named")
            labelWrap.drop()
            mark.drop()
            return
        }
        advance()
        advance()
        mark.done(LABEL)
        labelWrap.done(LABEL_QUALIFIER)
    }

    // ==================== 辅助方法 ====================

    /**
     * 回滚或丢弃标记
     *
     * @param rollbackMarker 标记
     * @param expected 期望的token
     * @param expectMessage 期望消息
     * @param validForDrop 有效丢弃类型
     * @return 是否成功
     */
    context(context: ParsingContext)
    private fun rollbackOrDrop(
        rollbackMarker: PsiBuilder.Marker,
        expected: CjToken,
        expectMessage: String,
        validForDrop: IElementType,
    ): Boolean {
        if (at(expected)) {
            advance()
            rollbackMarker.drop()
            return true
        } else if (at(validForDrop)) {
            rollbackMarker.drop()
            expect(expected, expectMessage)
            return true
        }

        rollbackMarker.rollbackTo()
        return false
    }

    /**
     * 在指定位置回滚或丢弃标记
     *
     * @param rollbackMarker 标记
     * @param dropAt 丢弃位置类型
     * @return 是否成功
     */
    context(context: ParsingContext)
    private fun rollbackOrDropAt(
        rollbackMarker: PsiBuilder.Marker,
        dropAt: IElementType
    ): Boolean {
        if (at(dropAt)) {
            advance()
            rollbackMarker.drop()
            return true
        }
        rollbackMarker.rollbackTo()
        return false
    }

    // ==================== 伴生对象和常量 ====================

    @OptIn(ExperimentalStdlibApi::class)
    companion object {
        var ALL_OPERATIONS: TokenSet? = null

        @JvmField
        val IDENTIFIER_RECOVERY_SET = TokenSet.create(
            IDENTIFIER,
            UNDERLINE,
        )

        private fun doneOrDrop(
            marker: PsiBuilder.Marker,
            type: IElementType,
            condition: Boolean,
        ) {
            if (condition) {
                marker.done(type)
            } else {
                marker.drop()
            }
        }

        @JvmStatic
        val EXPRESSION_FOLLOW = TokenSet.create(
            EOL_OR_SEMICOLON,
            ARROW,
            DOUBLE_ARROW,
            COMPOSITION,
            COMMA,
            RBRACE,
            RPAR,
            RBRACKET,
        )

        val ALLOW_NEWLINE_OPERATIONS = TokenSet.create(
            DOT,
            COLON,
            AS_KEYWORD,
            ANDAND,
            OROR,
            COALESCING,
            SAFE_ACCESS,
        )

        val QUOTE_TOKENS = TokenSet.orSet(
            TokenSet.create(
                ESCAPE_SEQUENCE,
                STRING_TEMPLATE,
                REGULAR_STRING_PART,
                OPEN_QUOTE,
                LITERAL_STRING_TEMPLATE_ENTRY,
                CLOSING_QUOTE,
                DOT,
                COMMA,
                LPAR,
                RPAR,
                LBRACKET,
                RBRACKET,
                LBRACE,
                RBRACE,
                MULMUL,
                MUL,
                PERC,
                DIV,
                PLUS,
                MINUS,
                PIPELINE,
                COMPOSITION,
                PLUSPLUS,
                MINUSMINUS,
                ANDAND,
                OROR,
                EXCL,
                AND,
                OR,
                XOREQ,
                LTLTEQ,
                GTGTEQ,
                COLON,
                SEMICOLON,
                EQ,
                PLUSEQ,
                MINUSEQ,
                MULTEQ,
                PLUS,
                MINUSEQ,
                PERCEQ,
                ANDEQ,
                OROREQ,
                ANDEQ,
                OREQ,
                XOREQ,
                LTLTEQ,
                GTGTEQ,
                ARROW,
                LEFT_ARROW,
                DOUBLE_ARROW,
                ELLIPSIS,
                RANGEEQ,
                RANGE,
                HASH,
                AT,
                QUEST,
                LTCOLON,
                LT,
                GT,
                LTEQ,
                GTEQ,
                EXCLEQ,
                EQEQ,
                UNDERLINE,
                BACKSLASH,
                QUOTESYMBOL,
                DOLLAR,
                INT8_KEYWORD,
                INT16_KEYWORD,
                INT32_KEYWORD,
                INT64_KEYWORD,
                INTNATIVE_KEYWORD,
                UINT8_KEYWORD,
                UINT16_KEYWORD,
                UINT32_KEYWORD,
                UINT64_KEYWORD,
                UINTNATIVE_KEYWORD,
                FLOAT16_KEYWORD,
                FLOAT32_KEYWORD,
                FLOAT64_KEYWORD,
                RUNE_KEYWORD,
                BOOL_KEYWORD,
                UNIT_KEYWORD,
                NOTHING_KEYWORD,
                STRUCT_KEYWORD,
                ENUM_KEYWORD,
                THIS_KEYWORD,
                PACKAGE_KEYWORD,
                IMPORT_KEYWORD,
                CLASS_KEYWORD,
                INTERFACE_KEYWORD,
                FUNC_KEYWORD,
                LET_KEYWORD,
                VAR_KEYWORD,
                CONST_KEYWORD,
                INIT_KEYWORD,
                SUPER_KEYWORD,
                IF_KEYWORD,
                ELSE_KEYWORD,
                CASE_KEYWORD,
                TRY_KEYWORD,
                CATCH_KEYWORD,
                FINALLY_KEYWORD,
                FOR_KEYWORD,
                DO_KEYWORD,
                WHILE_KEYWORD,
                THROW_KEYWORD,
                RETURN_KEYWORD,
                CONTINUE_KEYWORD,
                BREAK_KEYWORD,
                AS_KEYWORD,
                IN_KEYWORD,
                MATCH_KEYWORD,
                WHERE_KEYWORD,
                EXTEND_KEYWORD,
                SPAWN_KEYWORD,
                SYNCHRONIZED_KEYWORD,
                MACRO_KEYWORD,
                QUOTE_KEYWORD,
                TRUE_KEYWORD,
                FALSE_KEYWORD,
                STATIC_KEYWORD,
                PUBLIC_KEYWORD,
                PRIVATE_KEYWORD,
                PROTECTED_KEYWORD,
                OVERRIDE_KEYWORD,
                ABSTRACT_KEYWORD,
                OPEN_KEYWORD,
                OPERATOR_KEYWORD,
                FOREIGN_KEYWORD,
                USER_TYPE,
                IDENTIFIER,
                FIELD_IDENTIFIER,
                ESCAPE_LPAR,
                ESCAPE_RPAR,
                ESCAPE_DOLLAR,
                ESCAPE_LBRACKET,
                ESCAPE_RBRACKET,
                INTEGER_LITERAL,
                FLOAT_LITERAL,
                RUNE_LITERAL,
                LONG_TEMPLATE_ENTRY_START,
                LONG_TEMPLATE_ENTRY_END
            ),
            CjTokens.KEYWORDALL,
            LITERAL_CONSTANT,
        )

        val EXPRESSION_FIRST = TokenSet.orSet(
            TokenSet.create(
                MINUS,
                PLUS,
                MINUSMINUS,
                PLUSPLUS,
                EXCL,
                LPAR,
                TRUE_KEYWORD,
                FALSE_KEYWORD,
                OPEN_QUOTE,
                INTEGER_LITERAL,
                RUNE_LITERAL,
                CHARACTER_BYTE_LITERAL,
                FLOAT_LITERAL,
                LBRACE,
                FUNC_KEYWORD,
                THIS_KEYWORD,
                SUPER_KEYWORD,
                IF_KEYWORD,
                MATCH_KEYWORD,
                TRY_KEYWORD,
                THROW_KEYWORD,
                RETURN_KEYWORD,
                CONTINUE_KEYWORD,
                BREAK_KEYWORD,
                FOR_KEYWORD,
                WHILE_KEYWORD,
                DO_KEYWORD,
                IDENTIFIER,
                LBRACKET,
                UNSAFE_KEYWORD,
                SPAWN_KEYWORD,
                SYNCHRONIZED_KEYWORD,
                QUOTE_KEYWORD,
            ),
            BASICTYPES,
        )

        val EXPRESSION_FIRST_WITH_LET = TokenSet.orSet(
            EXPRESSION_FIRST,
            TokenSet.create(LET_KEYWORD)
        )

        private val TYPE_ARGUMENT_LIST_STOPPERS = TokenSet.create(
            INTEGER_LITERAL,
            FLOAT_LITERAL,
            RUNE_LITERAL,
            CHARACTER_BYTE_LITERAL,
            OPEN_QUOTE,
            PACKAGE_KEYWORD,
            AS_KEYWORD,
            COALESCING, SAFE_ACCESS,
            INTERFACE_KEYWORD,
            CLASS_KEYWORD,
            THIS_KEYWORD,
            LET_KEYWORD,
            VAR_KEYWORD,
            CONST_KEYWORD,
            FUNC_KEYWORD,
            FOR_KEYWORD,
            TRUE_KEYWORD,
            FALSE_KEYWORD,
            IS_KEYWORD,
            THROW_KEYWORD,
            RETURN_KEYWORD,
            BREAK_KEYWORD,
            CONTINUE_KEYWORD,
            IF_KEYWORD,
            TRY_KEYWORD,
            ELSE_KEYWORD,
            WHILE_KEYWORD,
            DO_KEYWORD,
            MATCH_KEYWORD,
            RBRACKET,
            RBRACE,
            RPAR,
            PLUSPLUS,
            MINUSMINUS,
            PLUS,
            MINUS,
            EXCL,
            DIV,
            PERC,
            LTEQ,
            EQEQ,
            EXCLEQ,
            ANDAND,
            OROR,
            SEMICOLON,
            RANGE,
            EQ,
            MULTEQ,
            DIVEQ,
            PERCEQ,
            PLUSEQ,
            MINUSEQ,
            COLON,
        )

        private val TRY_CATCH_RECOVERY_TOKEN_SET =
            TokenSet.create(LBRACE, RBRACE, FINALLY_KEYWORD, CATCH_KEYWORD)

        private val IN_KEYWORD_R_PAR_COLON_SET = TokenSet.create(IN_KEYWORD, RPAR, COLON)

        private val IN_KEYWORD_L_BRACE_SET = TokenSet.create(IN_KEYWORD, LBRACE)

        private val IN_KEYWORD_L_BRACE_RECOVERY_SET =
            TokenSet.orSet(IN_KEYWORD_L_BRACE_SET, PARAMETER_NAME_RECOVERY_SET)

        private val COLON_IN_KEYWORD_SET = TokenSet.create(COLON, IN_KEYWORD)
        private val IN_KEYWORD_SET = TokenSet.create(IN_KEYWORD)

        private val L_PAR_L_BRACE_R_PAR_SET = TokenSet.create(LPAR, LBRACE, RPAR)

        private val MATCH_CONDITION_RECOVERY_SET = TokenSet.create(
            RBRACE,
            IN_KEYWORD,
            IS_KEYWORD,
        )

        @SuppressWarnings("WeakerAccess")
        val STATEMENT_FIRST = TokenSet.orSet(
            EXPRESSION_FIRST,
            TokenSet.create(
                FUNC_KEYWORD,
                LET_KEYWORD,
                CONST_KEYWORD,
                VAR_KEYWORD,
                INTERFACE_KEYWORD,
                CLASS_KEYWORD,
                SPAWN_KEYWORD,
                SYNCHRONIZED_KEYWORD,
            ),
            MODIFIER_KEYWORDS,
            BASICTYPES,
            SPECIAL_MODIFIER_KEYWORDS,
            TokenSet.create(AT),
        )

        val STATEMENT_NEW_LINE_QUICK_RECOVERY_SET = TokenSet.orSet(
            TokenSet.andSet(
                STATEMENT_FIRST,
                TokenSet.andNot(KEYWORDS, TokenSet.create(IN_KEYWORD)),
            ),
            TokenSet.create(EOL_OR_SEMICOLON),
        )

        val logger = Logger.getInstance(
            CangJieExpressionParsing::class.java,
        )

        private val KEYWORD_TEXTS: ImmutableMap<String, CjToken> =
            tokenSetToMap(KEYWORDS)

        private fun tokenSetToMap(tokens: TokenSet): ImmutableMap<String, CjToken> {
            val builder: ImmutableMap.Builder<String, CjToken> = ImmutableMap.builder<String, CjToken>()
            for (token in tokens.types) {
                builder.put(token.toString(), token as CjToken)
            }
            return builder.build()
        }

        init {
            val operations: MutableSet<IElementType> = HashSet()
            val values: Array<Precedence> = Precedence.entries.toTypedArray()
            for (precedence in values) {
                operations.addAll(listOf(*precedence.getOperations().types))
            }
            ALL_OPERATIONS = TokenSet.create(*operations.toTypedArray<IElementType>())
        }

        init {
            val operations: Array<IElementType> = OPERATIONS.types
            val opSet: MutableSet<IElementType> = HashSet(listOf(*operations))
            val usedOperations: Array<IElementType> = ALL_OPERATIONS?.types ?: emptyArray()
            val usedSet: MutableSet<IElementType> = HashSet(listOf(*usedOperations))

            if (opSet.size > usedSet.size) {
                opSet.removeAll(usedSet)
                assert(false) { opSet }
            }
            assert(usedSet.size == opSet.size) { "Either some ops are unused, or something a non-op is used" }

            usedSet.removeAll(opSet)

            assert(usedSet.isEmpty()) { usedSet.toString() }
        }
    }
}

private fun IElementType.equal(token: IElementType): Boolean {
    return token === this
}

private fun IElementType.equal(tokenSet: TokenSet): Boolean {
    return tokenSet.contains(this)
}

private enum class RecognizedPattern {
    BINDING,
    TYPE,
    ENUM;

    fun toPatternType(): PatternType = when (this) {
        BINDING -> PatternType.BINDING
        TYPE -> PatternType.TYPE
        ENUM -> PatternType.ENUM
    }

    fun toNodeType(): IElementType = when (this) {
        BINDING -> BINDING_PATTERN
        TYPE -> TYPE_PATTERN
        ENUM -> ENUM_PATTERN
    }
}

enum class PatternType(val displayName: String) {
    WILDCARD("Wildcard patterns"),
    BINDING("Binding patterns"),
    TYPE("Type patterns"),
    TUPLE("Tuple patterns"),
    ENUM("Enum patterns"),
    CONSTANT("Constant patterns");

    companion object {
        val ALL_PATTERNS = PatternType.entries.toSet()
    }
}

operator fun TokenSet.plus(set: TokenSet): TokenSet {
    return TokenSet.orSet(this, set)
}