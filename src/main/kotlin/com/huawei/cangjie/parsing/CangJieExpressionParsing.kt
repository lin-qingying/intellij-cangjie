package com.huawei.cangjie.parsing


import com.google.common.collect.ImmutableMap
import com.huawei.cangjie.CjNodeTypes.*
import com.huawei.cangjie.lexer.CjToken
import com.huawei.cangjie.lexer.CjTokens
import com.huawei.cangjie.lexer.CjTokens.*
import com.huawei.cangjie.parsing.CangJieParsing.DeclarationParsingMode
import com.huawei.cangjie.parsing.CangJieParsing.PARAMETER_NAME_RECOVERY_SET
import com.intellij.lang.PsiBuilder
import com.intellij.lang.parser.GeneratedParserUtilBase
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.Pair
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet

open class CangJieExpressionParsing(
    builder: SemanticWhitespaceAwarePsiBuilder, private val cangJieParsing: CangJieParsing, isLazy: Boolean
) : AbstractCangJieParsing(
    builder, isLazy
) {
    private val ARROW_SET = TokenSet.create(DOUBLE_ARROW)
    private val ARROW_COMMA_SET =
        TokenSet.create(DOUBLE_ARROW, COMMA)


    @SuppressWarnings("UnusedDeclaration")

    enum class Precedence(vararg operations: IElementType) {
        POSTFIX(
            PLUSPLUS, MINUSMINUS, DOT, SAFE_ACCESS,
        ),


        PREFIX(MINUS, PLUS, EXCL) {

            override fun parseHigherPrecedence(parser: CangJieExpressionParsing) {
                throw IllegalStateException("Don't call this method")
            }

        },

        AS(AS_KEYWORD) {

            override fun parseRightHandSide(operation: IElementType, parser: CangJieExpressionParsing): IElementType {
                parser.mark().drop()

                parser.cangJieParsing.parseTypeRefWithoutIntersections()
                return BINARY_WITH_TYPE
            }

            override fun parseHigherPrecedence(parser: CangJieExpressionParsing) {

                parser.parsePrefixExpression()
            }
        },

        MULTIPLICATIVE(MUL, DIV, PERC, MULMUL),
        ADDITIVE(PLUS, MINUS),


        RANGE(CjTokens.RANGE, RANGEEQ) {

            override fun parseRightHandSide(operation: IElementType, parser: CangJieExpressionParsing): IElementType {
                if (operation == CjTokens.RANGE || operation == RANGEEQ) {
                    parser.parseRangeExpression()
                    return RANGE_EXPRESSION
                }
                return super.parseRightHandSide(operation, parser)


            }

        },
        ELVIS(CjTokens.ELVIS),

        IS(IS_KEYWORD) {
            override fun parseRightHandSide(operation: IElementType, parser: CangJieExpressionParsing): IElementType {
                if (operation === IS_KEYWORD) {
//                    TODO Marker already done.  未知原因，只能先这样解决，包括as 也有这个问题
                    parser.mark().drop()


//parser.advance()
//              parser. parseTest()
//mark.done(IS_EXPRESSION)
                    parser.cangJieParsing.parseTypeRefWithoutIntersections()

//                    parser.parseStatement()
//                    parser.advance()
                    return IS_EXPRESSION
                }
                return super.parseRightHandSide(operation, parser)
            }
        },

        COMPARISON(LT, GT, LTEQ, GTEQ),
        EQUALITY(EQEQ, EXCLEQ),
        CONJUNCTION(ANDAND),
        DISJUNCTION(OROR),
//        COMPOSITION(CjTokens.COMPOSITION),

        //位运算
        BITWISE(AND, OR, XOR, LTLT, GTGT, LTLTEQ, GTGTEQ),

        //flow
        FLOW(PIPELINE, COMPOSITION),

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
            MULMULEQ
        );

        private var higher: Precedence? = null
        private val operations: TokenSet

        @OptIn(ExperimentalStdlibApi::class)
        companion object {
            //        标识符，通配符


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

        /**
         * @param operation  操作操作符号(例如+)
         * @param parser   解析器对象
         * @return 结果的节点类型
         */
        open fun parseRightHandSide(operation: IElementType, parser: CangJieExpressionParsing): IElementType {
            parseHigherPrecedence(parser)
            return BINARY_EXPRESSION
        }

        open fun parseHigherPrecedence(parser: CangJieExpressionParsing) {
            assert(higher != null)

            higher?.let { parser.parseBinaryExpression(it) }
        }

    }



    private fun parseRangeExpression() {
        parseExpression()
        if (at(COLON)) {
            advance()

            parseExpression()

//            if (at(INTEGER_LITERAL)) {
//                advance()
//            } else {
//
//                error("Lack of step frequency")
////                errorAndAdvance("Expecting an integer literal")
//            }
        }
    }


    override fun create(builder: SemanticWhitespaceAwarePsiBuilder?): CangJieParsing = cangJieParsing.create(builder)


    fun parseStatements(type: IElementType) {
        while (at(SEMICOLON)) advance() // SEMICOLON
        while (!eof() && !at(RBRACE) && !at(type)) {
            if (!atSet(STATEMENT_FIRST)) {
                errorAndAdvance("Expecting an element")
            }
            if (atSet(STATEMENT_FIRST)) {
                parseStatement()
            }
            if (at(SEMICOLON)) {
                while (at(SEMICOLON)) advance() // SEMICOLON
            } else if (at(RBRACE)) {
                break
            } else if (at(type)) {
                break
            } else if (!myBuilder.newlineBeforeCurrentToken()) {
                val severalStatementsError = "Unexpected tokens (use ';' to separate expressions on the same line)"
                if (atSet(STATEMENT_NEW_LINE_QUICK_RECOVERY_SET)) {
                    error(severalStatementsError)
                } else {
                    errorUntil(
                        severalStatementsError, TokenSet.create(EOL_OR_SEMICOLON, LBRACE, RBRACE, type)
                    )
                }
            }
        }
    }


    /*
        * expressions
        *   : SEMI* statement{SEMI+} SEMI*
        */
    fun parseStatements() {

        while (at(SEMICOLON)) advance() // SEMICOLON
        while (!eof() && !at(RBRACE)) {
            if (!atSet(STATEMENT_FIRST)) {
                errorAndAdvance("Expecting an element")
            }

            if (atSet(STATEMENT_FIRST)) {
                parseStatement()
            }

            if (at(SEMICOLON)) {
                while (at(SEMICOLON)) advance() // SEMICOLON
            } else if (at(RBRACE)) {
                break
            } else if (!myBuilder.newlineBeforeCurrentToken()) {
                val severalStatementsError = "Unexpected tokens (use ';' to separate expressions on the same line)"
                if (atSet(STATEMENT_NEW_LINE_QUICK_RECOVERY_SET)) {
                    error(severalStatementsError)
                } else {
                    errorUntil(
                        severalStatementsError, TokenSet.create(EOL_OR_SEMICOLON, LBRACE, RBRACE)
                    )
                }
            }
        }
    }

    /*
     * (SimpleName ":")?  element
     */
    private fun parseValueArgument() {
        val argument = mark()
        if (at(IDENTIFIER) && lookahead(1) === COLON) {
            val argName = mark()
            val reference = mark()
            advance() // IDENTIFIER
            reference.done(REFERENCE_EXPRESSION)
            argName.done(VALUE_ARGUMENT_NAME)
            advance() // COLON
        }
//        if (at(MUL)) {
//            advance() // MUL
//        }
        parseExpression()
        argument.done(VALUE_ARGUMENT)
    }

    fun parseValueArgumentList() {

        parseValueArgumentList(LPAR, RPAR)
    }

    /*
     * valueArguments
     *   : "(" (SimpleName "=")? "*"? element{","} ")"
     *   ;
     */
    fun parseValueArgumentList(start: CjToken = LPAR, end: CjToken = RPAR) {

        parseValueArgumentList(Pair.create(TokenSet.create(start), end))
    }

    fun parseValueArgumentList(struct: Pair<TokenSet, CjToken> = Pair(TokenSet.create(LPAR, SAFE_CALL), RPAR)) {
        val list = mark()

        val sturctStart = struct.first
        val sturctEnd = struct.second

        myBuilder.disableNewlines()
        if (expectSafeCall(sturctStart, "Expecting an argument list", EXPRESSION_FOLLOW)) {
            if (!at(sturctEnd)) {
                while (true) {
                    while (at(COMMA)) errorAndAdvance("Expecting an argument")
                    parseValueArgument()
                    if (at(COLON) && lookahead(1) === IDENTIFIER) {
                        errorAndAdvance("Unexpected type specification", 2)
                    }
                    if (!at(COMMA)) {
                        if (atSet(EXPRESSION_FIRST)) {
                            error("Expecting ','")
                            continue
                        } else {
                            break
                        }
                    }
                    advance() // COMMA
                    if (at(RPAR)) {
                        break
                    }
                }
            }
            expect(sturctEnd, "Expecting '${sturctEnd.debugName}'", EXPRESSION_FOLLOW)
        }
        myBuilder.restoreNewlinesState()
        list.done(VALUE_ARGUMENT_LIST)
    }


    /**
     * 解析调用后缀（callSuffix）
     * 该函数尝试解析紧跟在函数调用之前的类型参数列表（可选）、值参数列表或带有注释的Lambda表达式
     * callSuffix
     *   : typeArguments? valueArguments annotatedLambda
     *   : typeArguments annotatedLambda
     *   ;
     * @return Boolean 表示是否成功解析了调用后缀
     */
    private fun parseCallSuffix(): Boolean {
        // 获取当前安全的token类型，用于后续的解析判断
        val tokenType = safeTokenType

        // 尝试解析带有闭包的调用，如果成功，则什么都不做
        if (parseCallWithClosure()) {
            // do nothing
        } else if (at(LPAR) || tokenType == SAFE_CALL) {
            // 如果当前token是左圆括号或安全调用符号，解析值参数列表
            parseValueArgumentList()

        } /*else if (at(LT)) {
            // 如果当前token是左尖括号，尝试解析类型参数列表
            val typeArgumentList = mark()
            if (cangJieParsing.tryParseTypeArgumentList(TYPE_ARGUMENT_LIST_STOPPERS)) {
                // 成功解析类型参数列表后，标记为完成
                typeArgumentList.done(TYPE_ARGUMENT_LIST)
                // 如果当前token不是左圆括号或没有换行，尝试解析值参数列表
                if (!myBuilder.newlineBeforeCurrentToken() && at(LPAR)) parseValueArgumentList()
                // 尝试解析带有闭包的调用
                parseCallWithClosure()
            } else {
                // 如果解析类型参数列表失败，回滚到标记位置，并返回false
                typeArgumentList.rollbackTo()
                return false
            }
        } */else {
            // 如果以上条件都不满足，返回false
            return false
        }
        // 如果成功解析了调用后缀，返回true
        return true
    }


    /*
    * 后缀表达式
     * postfixUnaryExpression
     *   : atomicExpression postfixUnaryOperation*
     *   ;
     *
     * postfixUnaryOperation
     *   : "++" : "--"
     *   : typeArguments? valueArguments (getEntryPoint? functionLiteral)
     *   : typeArguments (getEntryPoint? functionLiteral)
     *   : arrayAccess
     *   : memberAccessOperation postfixUnaryExpression
     *   ;
     */
    private fun parsePostfixExpression() {
        var expression = mark()


        var firstExpressionParsed =
//            TODO 是否应该处理安全访问 ?.
            if ((atSet(BASICTYPES) && lookahead(1) === LPAR) || (atSet(BASICTYPES) && (lookahead(1) === DOT || lookahead(
                    1
                ) === SAFE_ACCESS))
            ) {
                cangJieParsing.parseTypeRef()
                true
            } else if (atSet(BASICTYPES)) {


                errorAndAdvance("expected expression or declaration, found keyword ${myBuilder.tokenText}")
                false
            } else {
                parseAtomicExpression()

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
                advance() // DOT or SAFE_ACCESS
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
//                后缀切片或者区间
                advance()


//                TODO 更改为区间或者切片
                expression.done(SLICE_EXPRESSION)


            } else {

                break
            }

            expression = expression.precede()
        }


        expression.drop()
    }


    /**
     * 解析选择器调用表达式
     * atomicExpression typeParameters? valueParameters? functionLiteral*
     * 选择器调用表达式是编程语言中的一种表达式，它通常紧跟在原子表达式之后
     * 可能包含类型参数、值参数和函数字面量此函数尝试解析这样的表达式，并根据解析结果
     * 标记为调用表达式或放弃标记
     */
    private fun parseSelectorCallExpression() {
        // 设置标记，以便在解析过程中决定是否应用更改
        val mark = mark()
        // 解析原子表达式，这是选择器调用表达式的起始部分
        parseAtomicExpression()
        // 如果当前标记之前没有换行，并且成功解析了调用后缀，则认为解析成功
        if (!myBuilder.newlineBeforeCurrentToken() && parseCallSuffix()) {
            // 成功解析后，将标记范围内的内容标记为调用表达式
            mark.done(CALL_EXPRESSION)
        } else {
            // 如果解析失败或不满足条件，则放弃之前设置的标记
            mark.drop()
        }
    }

    fun parseDoubleColonSuffix(expression: PsiBuilder.Marker): Boolean {

        return false
    }

    /*
     * atomicExpression
     *   : "this" label?
     *   : "super" ("<" type ">")? label?
     *   : jump
     *   : if
     *   : match
     *   : try
     *   : loop
     *   : functionLiteral
     *   : declaration
     *   : SimpleName
     *   ;
     */
    private fun parseAtomicExpression(): Boolean {
        var ok = true



        when (getTokenId()) {

//            宏表达式


            //元组
//            TUPLE_LTIERAL_Id -> parseTupleLiteralExpression()

            //字面量
            LPAR_Id -> parseParenthesizedExpression()
//            //索引
            LBRACKET_Id -> parseCollectionLiteralExpression()
//            //this
            THIS_KEYWORD_Id -> parseThisExpression()
//            //super
            SUPER_KEYWORD_Id -> parseSuperExpression()
//        throw
            THROW_KEYWORD_Id -> parseThrow()
//            //return
            RETURN_KEYWORD_Id -> parseReturn()
//            //continue
            CONTINUE_KEYWORD_Id -> parseJump(CONTINUE)
//            //break
            BREAK_KEYWORD_Id -> parseJump(BREAK)
//            //if
            IF_KEYWORD_Id -> parseIf()
//            //match
            MATCH_KEYWORD_Id -> parseMatch()
//            //try
            TRY_KEYWORD_Id -> parseTry()
//            //for
            FOR_KEYWORD_Id -> parseFor()
//            //while
            WHILE_KEYWORD_Id -> parseWhile()
//            //do while
            DO_KEYWORD_Id -> parseDoWhile()
            //标识符
            IDENTIFIER_Id -> parseSimpleNameExpression()
            //lambda
            LBRACE_Id -> parseFunctionLiteral()
            //字符串模板
            OPEN_QUOTE_Id -> parseStringTemplate()
            //true false
            TRUE_KEYWORD_Id, FALSE_KEYWORD_Id -> parseOneTokenExpression(BOOLEAN_CONSTANT)
            //整数
            INTEGER_LITERAL_Id -> parseOneTokenExpression(INTEGER_CONSTANT)
//            //字符
            RUNE_LITERAL_Id -> parseOneTokenExpression(RUNE_CONSTANT)
//            字符字节字面量
            CHARACTER_BYTE_LITERAL_Id -> parseOneTokenExpression(CHARACTER_BYTE_CONSTANT)

            //浮点数
            FLOAT_LITERAL_Id -> parseOneTokenExpression(FLOAT_CONSTANT)
            // Unit
//            UNIT_LTIERAL_Id -> parseOneTokenExpression(UNIT_CONSTANT)
//class interface func let var
//            CLASS_KEYWORD_Id, INTERFACE_KEYWORD_Id, FUNC_KEYWORD_Id, LET_KEYWORD_Id, VAR_KEYWORD_Id -> if (!parseLocalDeclaration(
//                    myBuilder.newlineBeforeCurrentToken(),
//
//                    )
//            ) {
//                ok = false
//            }

            else -> ok = false
        }
        if (!ok) {

            errorWithRecovery(
                "Expecting an element", TokenSet.orSet(
                    EXPRESSION_FOLLOW, TokenSet.create(LONG_TEMPLATE_ENTRY_END)
                )
            )
        }
        return ok
    }

    fun parseTupleLiteralExpression() {
        assert(_at(TUPLE_LITERAL))
        val tuple = mark()
        advance() // TUPLE_LTIERAL
        if (at(RPAR)) {
            advance() // RPAR
        } else {
            while (true) {
                if (at(RPAR)) {
                    break
                }
                parseExpression()
                if (at(RPAR)) {
                    break
                }
                expect(COMMA, "Expecting ','")
            }
            expect(RPAR, "Expecting ')'")
        }
        tuple.done(TUPLE_EXPRESSION)

    }

    /*
     * match
     *   : "match" condition "{"
     *         matchEntry*
     *     "}"
     *   ;
     */
    private fun parseMatch() {


        assert(_at(MATCH_KEYWORD))
        val match = mark()
        advance() // MATCH_KEYWORD


        myBuilder.disableNewlines()
        var isExpression = true
        if (at(LPAR)) {


//            val atWhenStart = mark()
//            cangJieParsing.parseAnnotationsList(  EQ_RPAR_SET)
//            if (at( LET_KEYWORD) || at( VAR_KEYWORD)) {
//                val declType: IElementType =
//                    cangJieParsing.parseVariable( DeclarationParsingMode.LOCAL)
//
//                atWhenStart.done(declType)
//                atWhenStart.setCustomEdgeTokenBinders(
//                     PrecedingDocCommentsBinder,
//                     TrailingCommentsBinder
//                )
//            } else {
//                atWhenStart.drop()
//                parseExpression()
//            }
            parseCondition()
            isExpression = false
        }


        myBuilder.restoreNewlinesState()


        myBuilder.enableNewlines()
        if (expect(LBRACE, "Expecting '{'")) {

            if (!at(CASE_KEYWORD)) {
                error("Expecting 'case'")
            }

            while (!eof() && !at(RBRACE)) {

                if (!at(CASE_KEYWORD)) {
                    errorAndAdvance("Expecting 'case'")
//                    break
                } else {
                    parseMatchEntry(isExpression)

                }

            }
            expect(RBRACE, "Expecting '}'")
        }
        myBuilder.restoreNewlinesState()
        match.done(MATCH)
    }

    /**
     * 根据结束符处理代码块
     * @param type 结束符
     */
    fun parseBlock(type: IElementType) {
        while (!at(type) && !eof() && !at(RBRACE)) {
            parseBlockLevelExpression()
        }
    }


    /**
     * caseBody
     *
     */
    private fun parseCaseBody() {
        val body = mark()
        if (!at(SEMICOLON)) {


            if (at(RBRACE) || at(CASE_KEYWORD)) {

//             errorBefore("match case cannot be empty")
//                val error = body.precede()

                error("match case cannot be empty")

                body.drop()
                return


            } else {
                parseStatements(CASE_KEYWORD)
            }


        }
        body.done(CASE_BLOCK)
    }

    /*
     * matchEntry
     *   : case caseCondition{|} "=>" caseBody
     *   ;
     */
    private fun parseMatchEntry(isExpression: Boolean = false) {
        val entry = mark()

        if (at(CASE_KEYWORD)) {
            advance() // CASE_KEYWORD

            parseCasePattern(isExpression)
            while (at(OR)) {
                advance() // OR
                parseCasePattern(isExpression)
            }
            expect(DOUBLE_ARROW, "Expecting '=>'")
            parseCaseBody()

//            parseControlStructureOrBody()
        } else {
            error("Expecting 'case'")
        }

        entry.done(MATCH_ENTRY)

    }

    private fun parseReferenceExpression() {


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

    inner class CasePattern {
        /**
         * 绑定模式(id) | 类型模式(id:type) | 枚举模式(id(expression{,})?)
         */
        fun parseSimpleNameExpression() {
            assert(_at(IDENTIFIER))
            var mark = mark()
            var type = 1

            if (lookahead(1) == DOT || lookahead(1) == LT) {
//                parseReferenceExpression()
                cangJieParsing.parseTypeRef()
                type = 3

                if (at(LPAR)) {
                    //枚举模式
                    advance() // LPAR
                    type = 3
                    parseExpression()
                    while (at(COMMA)) {
                        advance() // COMMA
                        parseExpression()
                    }
                    expect(RPAR, "Expecting ')'")
                }
            } else {
//                var reference = mark()

//                advance() // IDENTIFIER
                parseReferenceExpression()

                //1.绑定模式
                //2.类型模式
                //3.枚举模式
                if (at(COLON)) {
                    advance() // COLON
                    type = 2
                    cangJieParsing.parseTypeRef()

                } else if (at(LPAR)) {
                    mark.rollbackTo()
                    mark = mark()
                    cangJieParsing.parseTypeRef()
//                    parseReferenceExpression()

                    //枚举模式
                    advance() // LPAR
                    type = 3
                    parseExpression()
                    while (at(COMMA)) {
                        advance() // COMMA
                        parseExpression()
                    }
                    expect(RPAR, "Expecting ')'")
                }
            }


            when (type) {
                1 -> mark.done(BINDING_PATTERN)
//                1 -> mark.done(REFERENCE_EXPRESSION)
                2 -> mark.done(TYPE_PATTERN)
                3 -> mark.done(ENUM_PATTERN)
            }


        }


        fun parseUnderline() {
            val pattern = mark()

            assert(_at(UNDERLINE))
            advance()

            if (at(COLON)) {
                advance() // COLON
                //处理类型
                cangJieParsing.parseTypeRef()
                pattern.done(TYPE_PATTERN)
            } else {
                pattern.done(WILDCARD_PATTERN)
            }


        }

        fun parseExpression() {
            val constantPattern = mark()

            when (tokenId) {

                UNDERLINE_Id -> {
                    constantPattern.drop()
                    parseUnderline()
                }

                LPAR_Id -> {
                    constantPattern.drop()

                    parseParenthesizedExpression()
                }

                INTEGER_LITERAL_Id -> {
                    parseOneTokenExpression(INTEGER_CONSTANT)
                    constantPattern.done(CONSTANT_PATTERN)
                }

                RUNE_LITERAL_Id -> {
                    parseOneTokenExpression(RUNE_CONSTANT)
                    constantPattern.done(CONSTANT_PATTERN)

                }

                CHARACTER_BYTE_LITERAL_Id -> {
                    parseOneTokenExpression(CHARACTER_BYTE_CONSTANT)
                    constantPattern.done(CONSTANT_PATTERN)

                }

                TRUE_KEYWORD_Id, FALSE_KEYWORD_Id -> {
                    parseOneTokenExpression(BOOLEAN_CONSTANT)
                    constantPattern.done(CONSTANT_PATTERN)

                }

                FLOAT_LITERAL_Id -> {
                    parseOneTokenExpression(FLOAT_CONSTANT)
                    constantPattern.done(CONSTANT_PATTERN)

                }

                OPEN_QUOTE_Id -> {
                    parseStringTemplate()
                    constantPattern.done(CONSTANT_PATTERN)

                }

                IDENTIFIER_Id -> {
                    constantPattern.drop()

                    parseSimpleNameExpression()

                }

                else -> {
                    error("Expecting a pattern expression")
                    constantPattern.drop()
                }

            }
        }

        fun parseTupleExpression() {
            parseExpression()
            if (at(COMMA)) {
                advance() // COMMA
                parseExpression()
                while (at(COMMA)) {
                    advance() // COMMA
                    parseExpression()
                }
            } else {
                error("Expecting Tuple expression")
            }


        }

        fun parseParenthesizedExpression() {
            assert(_at(LPAR))


            var isUnit = false
            var isTuple = false

            val mark = mark()
            myBuilder.disableNewlines()
            advance() // LPAR


            if (at(RPAR)) {
                isUnit = true
//                    mark.done(UNIT_CONSTANT)
//                    return
            } else {
//                    元组
                isTuple = true
                parseTupleExpression()

            }
            expect(RPAR, "Expecting ')'")



            when {
                isUnit -> {
                    mark().done(UNIT_CONSTANT)
                    mark.done(CONSTANT_PATTERN)
                }

                isTuple -> mark.done(TUPLE_PATTERN)

                else -> mark.drop()
            }

            myBuilder.restoreNewlinesState()


        }

    }

    private fun callBnf(call: (PsiBuilder) -> Boolean) {
        val bnfBuilder = GeneratedParserUtilBase.adapt_builder_(BLOCK, myBuilder, CangJieParserByBnf())
        val m = GeneratedParserUtilBase.enter_section_(bnfBuilder, 0, 1, null, null)
        val r = call(bnfBuilder)
        GeneratedParserUtilBase.exit_section_(bnfBuilder, m, null, r)


    }

    private fun parseCasePattern() {

    }

    /**
     * case condition
     * (常量 | 通配符(_)
     * | 绑定模式(SimpleName)
     * | Tuple模式( (p1,p2,...,pn) )
     * | 类型模式(SimpleName : type)
     * | Enum模式(SimpleName(SimpleName)?
     * | Tuple与Enum嵌套  )
     * | 逻辑表达式
     * )
     */
    private fun parseCasePattern(isExpression: Boolean = false) {
//        val condition = mark()

//        callBnf {
//            val r = CangJieParserByBnf.pattern(it, 0)
//            if (at(WHERE_KEYWORD)) {
//                val caseWhere = mark()
//                advance()
//
//                parseExpression()
//                caseWhere.done(CASE_WHERE)
//            }
//            r
//        }
//        return
        val casePattern = CasePattern()

        if (isExpression) {
            if (at(UNDERLINE)) {
                casePattern.parseUnderline()
            } else {
                val expr = mark()
                parseExpression()
                expr.done(MATCH_CONDITION_EXPRESSION)
            }

        } else {
            casePattern.parseExpression()

        }




        //是否常量模式
        fun isConstantPattern(): Boolean {
            return at(INTEGER_LITERAL) || at(RUNE_LITERAL) || at(TRUE_KEYWORD) || at(FALSE_KEYWORD) || at(
                OPEN_QUOTE
            )
        }

        //是否通配符模式
        fun isWildcardPattern(): Boolean {
            return myBuilder.tokenText == "_"
        }

        //是否绑定模式
        fun isBindingPattern(): Boolean {
            return at(IDENTIFIER)
        }

        //是否元组模式
        fun isTuplePattern(): Boolean {
            return at(LPAR)
        }

        //是否类型模式
        fun isTypePattern(): Boolean {
            return at(IDENTIFIER) && lookahead(1) == COLON
        }
//是否枚举模式
//        fun isEnumPattern(): Boolean {
//            return at(IDENTIFIER) && lookahead(1) == LPAR
//        }


        if (at(WHERE_KEYWORD)) {
            val caseWhere = mark()
            advance()

            parseExpression()
            caseWhere.done(CASE_WHERE)
        }

//        condition.done(CASE_PATTERN)
    }

    /*
     * for
     *   : "for" "(" (multipleVariableDeclarations | variableDeclarationEntry) "in" expression ")" expression
     *   ;
     *
     */
    private fun parseFor() {


        assert(_at(FOR_KEYWORD))
        val loop = mark()
        advance() // FOR_KEYWORD
        if (expect(LPAR, "Expecting '(' to open a loop range", EXPRESSION_FIRST)) {
            myBuilder.disableNewlines()
            if (!at(RPAR)) {


                val parameter = mark()
                if (!at(IN_KEYWORD)) {
                    cangJieParsing.parseModifierList(IN_KEYWORD_R_PAR_COLON_SET)
                }

//                if (at(LPAR)) {
//                    val destructuringDeclaration = mark()
//                    cangJieParsing.parseMultiDeclarationName(
//                        IN_KEYWORD_L_BRACE_SET,
//                        IN_KEYWORD_L_BRACE_RECOVERY_SET
//                    )
//                    destructuringDeclaration.done(DESTRUCTURING_DECLARATION)
//                } else {
//                    expect(
//                        IDENTIFIER_RECOVERY_SET,
//                        "Expecting a variable name",
//                        COLON_IN_KEYWORD_SET
//                    )
//                    if (at(COLON)) {
//                        errorAndAdvance("the pattern in for-in expression must be irrefutable")
//                    }
//                }

                CasePattern().parseExpression()
                parameter.done(VALUE_PARAMETER)


                if (expect(IN_KEYWORD, "Expecting 'in'", L_PAR_L_BRACE_R_PAR_SET)) {
                    val range = mark()
                    parseExpression()
                    range.done(LOOP_RANGE)
                }
            } else {
                error("Expecting a variable name")
            }
            expectNoAdvance(RPAR, "Expecting ')'")
            myBuilder.restoreNewlinesState()
        }
        parseLoopBody()
        loop.done(FOR)
    }

    /*
     * doWhile
     *   : "do" element "while" "(" element ")"
     *   ;
     */
    private fun parseDoWhile() {
        assert(_at(DO_KEYWORD))
        val loop = mark()
        advance() // DO_KEYWORD
        if (!at(WHILE_KEYWORD)) {
            parseLoopBody()
        }
        if (expect(WHILE_KEYWORD, "Expecting 'while' followed by a post-condition")) {
            parseCondition()
        }
        loop.done(DO_WHILE)
    }

    /*
     * element
     */
    private fun parseLoopBody() {
        val body = mark()
        if (!at(SEMICOLON)) {
            parseControlStructureBody()
//            parseBlockLevelExpression()
        }
        body.done(BODY)
    }

    /*
     * while
     *   : "while" "(" element ")" element
     *   ;
     */
    private fun parseWhile() {
        assert(_at(WHILE_KEYWORD))
        val loop = mark()
        advance() // WHILE_KEYWORD
//        parseCondition()
        if (at(LPAR) && lookahead(1) == LET_KEYWORD) {
            parseLetExpression()
        } else {
            parseCondition()
        }
        parseLoopBody()
        loop.done(WHILE)


    }

    /*
     * try
     *   : "try" block catchBlock* finallyBlock?
     *   ;
     * catchBlock
     *   : "catch" "(" annotations SimpleName ":" userType ")" block
     *   ;
     *
     * finallyBlock
     *   : "finally" block
     *   ;
     */
    private fun parseTry() {
        assert(_at(TRY_KEYWORD))
        val tryExpression = mark()
        advance() // TRY_KEYWORD

        if (at(LPAR)) {
            advance()
            do {
                if (at(COMMA)) advance()

                if (expect(IDENTIFIER, "Expecting resource name")) {
//                    advance()
                    if (expect(EQ, "Expecting '='", TRY_CATCH_RECOVERY_TOKEN_SET)) {
                        parseExpression()

                    }


                }
//
//                expect(IDENTIFIER, "Expecting resource name")
//
//                expect(EQ, "Expecting '='", TRY_CATCH_RECOVERY_TOKEN_SET)
//
//                parseExpression()


            } while (at(COMMA))


            expect(RPAR, "Expecting ')'")

        }



        cangJieParsing.parseBlock()
        var catchOrFinally = false
        while (at(CATCH_KEYWORD)) {
            catchOrFinally = true
            val catchBlock = mark()
            advance() // CATCH_KEYWORD
            if (atSet(TRY_CATCH_RECOVERY_TOKEN_SET)) {
                error("Expecting exception variable declaration")
            } else {
                val parameters = mark()
                expect(LPAR, "Expecting '('", TRY_CATCH_RECOVERY_TOKEN_SET)
                if (!atSet(TRY_CATCH_RECOVERY_TOKEN_SET)) {

                    if (at(UNDERLINE)) {
//                        所有匹配项
                        advance()
                    } else {
                        cangJieParsing.parseValueParameter( /*typeRequired = */true)
                        if (at(COMMA)) {
                            advance() // trailing comma
                        }
                    }


                    expect(RPAR, "Expecting ')'", TRY_CATCH_RECOVERY_TOKEN_SET)
                } else {
                    error("Expecting exception variable declaration")
                }
                parameters.done(VALUE_PARAMETER_LIST)
            }
            if (at(LBRACE)) {
                cangJieParsing.parseBlock()
            } else {
                error("Expecting a block: { ... }")
            }
            catchBlock.done(CATCH)
        }
        if (at(FINALLY_KEYWORD)) {
            catchOrFinally = true
            val finallyBlock = mark()
            advance() // FINALLY_KEYWORD
            cangJieParsing.parseBlock()
            finallyBlock.done(FINALLY)
        }
        if (!catchOrFinally) {
            error("Expecting 'catch' or 'finally'")
        }
        tryExpression.done(TRY)
    }

    /*
     * "(" element ")"
     */
    private fun parseCondition() {

        myBuilder.disableNewlines()
        if (expect(
                LPAR,
                "Expecting a condition in parentheses '(...)'",
                EXPRESSION_FIRST
            )
        ) {
            val condition = mark()

            parseExpression()

            condition.done(CONDITION)
            expect(RPAR, "Expecting ')")
        }
        myBuilder.restoreNewlinesState()

    }

    private fun parseControlStructureOrBody() {
        if (!parseAnnotatedLambda( /* preferBlock = */true)) {
            parseBlockLevelExpression()
        }

    }

    private fun parseControlStructureBody() {

        if (at(LBRACE)) {

            val body = mark()
            advance()
            parseStatements()

            expect(RBRACE, "Expecting '}'")



            body.done(BLOCK)

        } else {
//            parseBlockLevelExpression()
            error("Expecting '{'")
        }


    }

    private fun rollbackOrDrop(
        rollbackMarker: PsiBuilder.Marker,
        expected: CjToken, expectMessage: String,
        validForDrop: IElementType
    ): Boolean {
        if (at(expected)) {
            advance() // dropAt
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

    private fun rollbackOrDropAt(rollbackMarker: PsiBuilder.Marker, dropAt: IElementType): Boolean {
        if (at(dropAt)) {
            advance() // dropAt
            rollbackMarker.drop()
            return true
        }
        rollbackMarker.rollbackTo()
        return false
    }


    /**
     * parseBlock
     * : "{" statements "}"
     */

    fun parseBlock() {
        assert(_at(LBRACE))
        advance()
        val block = mark()

        parseStatements()

        expect(RBRACE, "Expecting '}'")

        block.done(BLOCK)
    }

    /*
     * lambdaParameter{","}
     *
     * lambdaParameter
     *   : variableDeclarationEntry
     *   : multipleVariableDeclarations (":" type)?
     */
    private fun parseFunctionLiteralParameterList() {
        val parameterList = mark()

        while (!eof()) {
            if (at(DOUBLE_ARROW)) {
                break
            }
            val parameter = mark()

            if (at(COLON)) {
                error("Expecting parameter name")
            } else {
                expect(
                    IDENTIFIER_RECOVERY_SET,
                    "Expecting parameter name",
                    ARROW_SET
                )
            }

            if (at(COLON)) {
                advance() // COLON
                cangJieParsing.parseTypeRef(ARROW_COMMA_SET)
            }
            parameter.done(VALUE_PARAMETER)

            if (at(DOUBLE_ARROW)) {
                break
            } else if (at(COMMA)) {
                advance() // COMMA
            } else {
                error("Expecting '->' or ','")
                break
            }
        }

        parameterList.done(VALUE_PARAMETER_LIST)
    }


    /**
     * If it has no ->, it's a block, otherwise a function literal
     *
     * Please update {@link com.huawei.cangjie.BlockExpressionElementType#isParsable(ASTNode, CharSequence, Language, Project)} if any changes occurs!
     *
     *
     * 函数式
     */
    fun parseFunctionLiteral(preferBlock: Boolean = false, collapse: Boolean = true, isDoubleArrow: Boolean = true) {
        assert(_at(LBRACE))
        val literalExpression = mark()
        val literal = mark()
        myBuilder.enableNewlines()
        advance() // LBRACE
        var paramsFound = false
        val token = tt()
        if (token === DOUBLE_ARROW) {
            //   { => ...}
            mark().done(VALUE_PARAMETER_LIST)
            advance() // ARROW
            paramsFound = true
        } else if (token.equal(IDENTIFIER_RECOVERY_SET) || token === COLON || token === LPAR) {
            // Try to parse a simple name list followed by an ARROW
            //   {a => ...}
            //   {a, b => ...}
            //   {(a, b) => ... }
            val rollbackMarker = mark()
            val nextToken = lookahead(1)
            val preferParamsToExpressions =
                (nextToken === COMMA || nextToken === COLON)
            parseFunctionLiteralParameterList()

            paramsFound = if (preferParamsToExpressions) rollbackOrDrop(
                rollbackMarker,
                DOUBLE_ARROW,
                "An -> is expected",
                RBRACE
            ) else rollbackOrDropAt(rollbackMarker, DOUBLE_ARROW)
        } else if (isDoubleArrow) {
            error("expected '=>' in lambda expression, found '${myBuilder.tokenText}'")
        }

//      if (token === IDENTIFIER || token === COLON || token === LPAR) {
//            // 尝试解析一个后面跟着一个箭头的简单姓名列表
//            //   {a -> ...}
//            //   {a, b -> ...}
//            //   {(a, b) -> ... }
//            val rollbackMarker = mark()
//            val nextToken = lookahead(1)
//            val preferParamsToExpressions = nextToken === COMMA || nextToken === COLON
//            parseFunctionLiteralParameterList()
//            paramsFound = if (preferParamsToExpressions) rollbackOrDrop(
//                rollbackMarker,
//                ARROW,
//                "An -> is expected",
//                RBRACE
//            ) else rollbackOrDropAt(rollbackMarker, ARROW)
//        }
//        if (!paramsFound && preferBlock) {
        /*
                literal.drop()
                parseStatements()
                expect(RBRACE, "Expecting '}'")
                literalExpression.done(BLOCK)
                myBuilder.restoreNewlinesState()
                return
        */

        if (!paramsFound && preferBlock) {
            literal.drop()
            parseStatements()
            expect(RBRACE, "Expecting '}'")
            literalExpression.done(BLOCK)
            myBuilder.restoreNewlinesState()

            return
        }


        if (collapse && isLazy) {
            cangJieParsing.advanceBalancedBlock()
            literal.done(FUNCTION_LITERAL)
            literalExpression.collapse(LAMBDA_EXPRESSION)
        } else {
            val body = mark()
            parseStatements()

            body.done(BLOCK)
            body.setCustomEdgeTokenBinders(PRECEDING_ALL_COMMENTS_BINDER, TRAILING_ALL_COMMENTS_BINDER)

            expect(RBRACE, "Expecting '}'")
            literal.done(FUNCTION_LITERAL)
            literalExpression.done(LAMBDA_EXPRESSION)
        }
        myBuilder.restoreNewlinesState()

    }

    /**
     * let expression
     * ; let
     */
    fun parseLetExpression() {

        myBuilder.disableNewlines()
        val let = mark()
        if (expect(
                LPAR,
                "Expecting a condition in parentheses '(...)'",
                EXPRESSION_FIRST
            )
        ) {

//


            expect(LET_KEYWORD, "Expecting 'let'")


            parseCasePattern()


            if (at(LEFT_ARROW)) {
                advance()
            } else {
                error("Expecting '<-'")
            }


//            expect(LEFT_ARROW, "Expecting '<-'")


            parseExpression()



            expect(RPAR, "Expecting ')")


        }
        let.done(LET_EXPRESSION)
        myBuilder.restoreNewlinesState()


    }


    /*
     * if
     *   : "if" "(" element ")" element SEMI? ("else" element)?
     *   ;
     */
    private fun parseIf() {
        assert(_at(IF_KEYWORD))
        val marker = mark()
        advance() //IF_KEYWORD

        if (at(LPAR) && lookahead(1) == LET_KEYWORD) {
            parseLetExpression()
        } else {
            parseCondition()
        }


        val thenBranch = mark()
        if (!at(ELSE_KEYWORD) && !at(SEMICOLON)) {

//            if(at(LBRACE)){
            parseControlStructureBody()
//            }else{
//                 error("Expecting '{'")
//            }

        }
        if (at(SEMICOLON) && lookahead(1) === ELSE_KEYWORD) {
            advance() // SEMICOLON
        }
        thenBranch.done(THEN)



        if (at(ELSE_KEYWORD) && lookahead(1) !== ARROW) {
            advance() // ELSE_KEYWORD
            val elseBranch = mark()
            if (!at(SEMICOLON)) {
//                如歌else 后面不是代码块或者if 则报错
                if (!at(LBRACE) && !at(IF_KEYWORD)) {
                    error("Expecting code block or if")
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

    /*
     * : "throw" element
     */
    private fun parseThrow() {
        assert(_at(THROW_KEYWORD))
        val marker = mark()
        advance() // THROW_KEYWORD
        parseExpression()
        marker.done(THROW)
    }

    /*
     * : "continue"
     * : "break"
     */
    private fun parseJump(type: IElementType) {
        assert(_at(BREAK_KEYWORD) || _at(CONTINUE_KEYWORD))
        val marker = mark()
        advance() // BREAK_KEYWORD or CONTINUE_KEYWORD

        marker.done(type)
    }

    /*
     * "return"   element?
     */
    private fun parseReturn() {
        assert(_at(RETURN_KEYWORD))
        val returnExpression = mark()
        advance() // RETURN_KEYWORD
//        parseLabelReferenceWithNoWhitespace()
        if (atSet(EXPRESSION_FIRST) && !at(EOL_OR_SEMICOLON)) parseExpression()
        returnExpression.done(RETURN)
    }

    /*
     * collectionLiteral
     *   : "[" element{","} "]"
     *   ;
     */
    private fun parseCollectionLiteralExpression() {
        parseAsCollectionLiteralExpression(COLLECTION_LITERAL_EXPRESSION, true, "Expecting an element")
    }

    /*
     * "(" expression? ")"
     */
    private fun parseParenthesizedExpression(isParseOperator: Boolean = true) {


        assert(_at(LPAR))

        var isUnit = false
        var isTuple = false

        val mark = mark()
        myBuilder.disableNewlines()
        advance() // LPAR
        if (!at(RPAR) && isParseOperator && !at(COMMA)) {

            parseExpression()
        } else {
            isUnit = true
        }
        // , 为元组
        while (at(COMMA)) {
            isTuple = true
            advance() // COMMA
            parseExpression()
        }
        expect(RPAR, "Expecting ')'")
        myBuilder.restoreNewlinesState()

        when {
            isUnit -> mark.done(UNIT_CONSTANT)
            isTuple -> mark.done(TUPLE_EXPRESSION)

            else -> mark.done(PARENTHESIZED)
        }
    }

    /*
     * "this" ("<" type ">")? label?
     */
    private fun parseSuperExpression() {
        assert(_at(SUPER_KEYWORD))
        val mark = mark()
        val superReference = mark()
        advance() // SUPER_KEYWORD
        superReference.done(REFERENCE_EXPRESSION)
        if (at(LT)) {
            // This may be "super < foo" or "super<foo>", thus the backtracking
            val supertype = mark()
            myBuilder.disableNewlines()
            advance() // LT
            cangJieParsing.parseTypeRef()
            if (at(GT)) {
                advance() // GT
                supertype.drop()
            } else {
                supertype.rollbackTo()
            }
            myBuilder.restoreNewlinesState()
        }
        parseLabelReferenceWithNoWhitespace()
        mark.done(SUPER_EXPRESSION)
    }

    /*
     * "this" label?
     */
    private fun parseThisExpression() {
        assert(_at(THIS_KEYWORD))
        val mark = mark()
        val thisReference = mark()
        advance() // THIS_KEYWORD
        thisReference.done(REFERENCE_EXPRESSION)
//        parseLabelReferenceWithNoWhitespace()
        mark.done(THIS_EXPRESSION)
    }

    /*
     * labelReference?
     */
    private fun parseLabelReferenceWithNoWhitespace() {
        if (at(AT) && !myBuilder.newlineBeforeCurrentToken()) {
            if (WHITE_SPACE_OR_COMMENT_BIT_SET.contains(myBuilder.rawLookup(-1))) {
                error("There should be no space or comments before '@' in label reference")
            }
            parseLabelReference()
        }
    }

    /*
     * "@" IDENTIFIER
     */
    private fun parseLabelReference() {
        assert(_at(AT))
        val labelWrap = mark()
        val mark = mark()
        if (myBuilder.rawLookup(1) !== IDENTIFIER) {
            errorAndAdvance("Label must be named") // AT
            labelWrap.drop()
            mark.drop()
            return
        }
        advance() // AT
        advance() // IDENTIFIER
        mark.done(LABEL)
        labelWrap.done(LABEL_QUALIFIER)
    }

    /*
     * stringTemplate
     *   : OPEN_QUOTE stringTemplateElement* CLOSING_QUOTE
     *   ;
     */
    fun parseStringTemplate() {
        assert(_at(OPEN_QUOTE))
        val template = mark()
        advance() // OPEN_QUOTE
        while (!eof()) {
            if (at(CLOSING_QUOTE) || at(DANGLING_NEWLINE)) {
                break
            }
            parseStringTemplateElement()
        }
        if (at(DANGLING_NEWLINE)) {
            errorAndAdvance("Expecting '\"'")
        } else {
            expect(CLOSING_QUOTE, "Expecting '\"'")
        }
        template.done(STRING_TEMPLATE)
    }

    private fun parseInnerExpressions(missingElementErrorMessage: String) {
        while (true) {
            if (at(COMMA)) errorAndAdvance(missingElementErrorMessage)
            if (at(RBRACKET)) {
                break
            }

            if (at(RANGE)) {
//                前缀切片
                parsePefixSliceExpression()
            } else {
                parseExpression()

            }

            if (!at(COMMA)) break
            advance() // COMMA
        }
    }


    private fun parsePefixSliceExpression() {
        val mark = mark()

//        TODO 切片结构
        advance() // RANGE
        if (!at(RBRACKET)) {
            parseExpression()

        }
        mark.done(SLICE_EXPRESSION)
    }

    private fun parseOneTokenExpression(type: IElementType) {
        val mark = mark()
        advance()
        mark.done(type)
    }

    /*
     * stringTemplateElement
     *   : RegularStringPart
     *   : ShortTemplateEntrySTART (SimpleName | "this")
     *   : EscapeSequence
     *   : longTemplate
     *   ;
     *
     * longTemplate
     *   : "${" expression "}"
     *   ;
     */
    private fun parseStringTemplateElement() {
        if (at(REGULAR_STRING_PART)) {
            val mark = mark()
            advance() // REGULAR_STRING_PART
            mark.done(LITERAL_STRING_TEMPLATE_ENTRY)
        } else if (at(ESCAPE_SEQUENCE)) {
            val mark = mark()
            advance() // ESCAPE_SEQUENCE
            mark.done(ESCAPE_STRING_TEMPLATE_ENTRY)
        } else if (at(SHORT_TEMPLATE_ENTRY_START)) {
            val entry = mark()
            advance() // SHORT_TEMPLATE_ENTRY_START
            if (at(THIS_KEYWORD)) {
                val thisExpression = mark()
                val reference = mark()
                advance() // THIS_KEYWORD
                reference.done(REFERENCE_EXPRESSION)
                thisExpression.done(THIS_EXPRESSION)
            } else {
                val keyword: CjToken? = KEYWORD_TEXTS.get(myBuilder.tokenText)
                if (keyword != null) {
                    myBuilder.remapCurrentToken(keyword)
                    errorAndAdvance("Keyword cannot be used as a reference")
                } else {
                    val reference = mark()
                    expect(IDENTIFIER, "Expecting a name")
                    reference.done(REFERENCE_EXPRESSION)
                }
            }
            entry.done(SHORT_STRING_TEMPLATE_ENTRY)
        } else if (at(LONG_TEMPLATE_ENTRY_START)) {
            val longTemplateEntry = mark()
            advance() // LONG_TEMPLATE_ENTRY_START
            while (!eof()) {
                val offset = myBuilder.currentOffset
//                parseExpression()
//                cangJieParsing.parseBlock()
//                parseStatement()
                parseStatementsByStringTemplate()
                if (_at(LONG_TEMPLATE_ENTRY_END)) {
                    advance()
                    break
                } else {
                    error("Expecting '}'")
                    if (offset == myBuilder.currentOffset) {
                        // 如果无法使用parseExpression()前进，则防止挂起
                        advance()
                    }
                }
            }
            longTemplateEntry.done(LONG_STRING_TEMPLATE_ENTRY)
        } else {
            errorAndAdvance("Unexpected token in a string template")
        }
    }

    /*
     * SimpleName
     * 简单名称表达式附带类型参数
     */
    fun parseSimpleNameExpression() {
        val simpleName = mark()
        expect(IDENTIFIER, "Expecting an identifier")


          if (at(LT)) {
            // 如果当前token是左尖括号，尝试解析类型参数列表
            val typeArgumentList = mark()
            if (cangJieParsing.tryParseTypeArgumentList(TYPE_ARGUMENT_LIST_STOPPERS)) {
                // 成功解析类型参数列表后，标记为完成
                typeArgumentList.done(TYPE_ARGUMENT_LIST)
            } else {
                // 如果解析类型参数列表失败，回滚到标记位置，并返回false
                typeArgumentList.rollbackTo()
            }
        }

        simpleName.done(REFERENCE_EXPRESSION)
    }

    private fun parseAsCollectionLiteralExpression(
        nodeType: IElementType, canBeEmpty: Boolean, missingElementErrorMessage: String
    ) {
        assert(_at(LBRACKET) || _at(SAFE_INDEXEX))
        val innerExpressions = mark()
        myBuilder.disableNewlines()
        advance() // LBRACKET or SAFE_INDEXEX
        if (!canBeEmpty && at(RBRACKET)) {
            error(missingElementErrorMessage)
        } else {
            parseInnerExpressions(missingElementErrorMessage)
        }
        expect(RBRACKET, "Expecting ']'")
        myBuilder.restoreNewlinesState()
        innerExpressions.done(nodeType)
    }

    /*
     * arrayAccess
     *   : "[" element{","} "]"
     *   ;
     */
    private fun parseArrayAccess() {
        parseAsCollectionLiteralExpression(INDICES, false, "Expecting an index element")
    }

    /*
    * operation? prefixExpression
    */
    fun parsePrefixExpression() {
        myBuilder.disableJoiningComplexTokens()
        if (atSet(Precedence.PREFIX.getOperations())) {
            val expression = mark()
            parseOperationReference()
            myBuilder.restoreJoiningComplexTokensState()
            parsePrefixExpression()
            expression.done(PREFIX_EXPRESSION)
        } else {

            if (at(MINUSMINUS) || at(PLUSPLUS)) {
                errorAndAdvance("expected expression or declaration, found '${myBuilder.tokenText}'")
            }
            myBuilder.restoreJoiningComplexTokensState()
            parsePostfixExpression()
        }
    }


    /**
     * 根据作用域解析语句
     */
    fun parseStatementByScope(scope: DeclarationParsingMode) {

        if (!parseDeclaration(scope, false)) {
            if (!atSet(EXPRESSION_FIRST)) {
                errorAndAdvance("Expecting a statement")
            } else {
                parseBlockLevelExpression()
            }
        }


    }

    /*
     * statement
     *  : declaration
     *  : blockLevelExpression
     *  ;
     */
    fun parseStatement() {
        if (!parseLocalDeclaration(false)) {
            if (!atSet(EXPRESSION_FIRST)) {
                errorAndAdvance("Expecting a statement")
            } else {
                parseBlockLevelExpression()
            }
        }
    }

    //    处理字符串模板语句
    private fun parseStatementsByStringTemplate() {
        while (at(SEMICOLON)) advance() // SEMICOLON
        while (!eof() && !at(LONG_TEMPLATE_ENTRY_END)) {
            if (!atSet(STATEMENT_FIRST)) {
                errorAndAdvance("Expecting an element")
            }

            if (atSet(STATEMENT_FIRST)) {
                parseStatement()
            }

            if (at(SEMICOLON)) {
                while (at(SEMICOLON)) advance() // SEMICOLON
            } else if (at(LONG_TEMPLATE_ENTRY_END)) {
                break
            } else if (!myBuilder.newlineBeforeCurrentToken()) {
                val severalStatementsError = "Unexpected tokens (use ';' to separate expressions on the same line)"
                if (atSet(STATEMENT_NEW_LINE_QUICK_RECOVERY_SET)) {
                    error(severalStatementsError)
                } else {

                    val errorMarker = mark()
                    errorMarker.error(severalStatementsError)

//                    errorUntil(
//                        severalStatementsError,
//                        TokenSet.create(EOL_OR_SEMICOLON, LONG_TEMPLATE_ENTRY_START, LONG_TEMPLATE_ENTRY_END)
//                    )
                }
            }
        }
    }

    /*
     * blockLevelExpression
     *  :  expression
     *  ;
     */
    private fun parseBlockLevelExpression() {
        parseExpression()
    }


    private val QUOTE_TOKENS = TokenSet.orSet(
        TokenSet.create(
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
            AND,
            OR,
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
            ESCAPE_RBRACKET
        ), LITERAL_CONSTANT
    )


    /**
     *     quoteExpr
     *     : LPAREN NL* quoteParameters NL* RPAREN
     *     ;
     */
    fun parseQuoteExpression() {
        assert(_at(QUOTE_KEYWORD))

        val quoteExpression = mark()
        advance()



        if (at(LPAR)) {
            advance()
            parseQuoteParameters()
        } else {

            error("expected '(' after 'quote'")
        }
        expect(RPAR, "expected ')' ")
        quoteExpression.done(QUOTE_EXPRESSION)
    }


    /**
     *  插值引用表达式规则，允许在引用中嵌入可计算表达式
     * quoteInterpolate
     * : DOLLAR LPAREN NL* expression NL* RPAREN
     * ;
     */
    private fun parseQuoteInterpolate() {
        assert(_at(DOLLAR) && lookahead(1) == LPAR)

        advance()
        advance()
        parseExpression()


        if (at(RPAR)) {
            advance()
        } else {
            error("expected ')' after '$'")
        }

    }


    /**
     *  引用参数列表规则，由一个或多个引用标记、插值表达式或宏表达式组成
     * quoteParameters
     *     : (NL* quoteToken | NL* quoteInterpolate | NL* macroExpression)+
     *     ;
     */
    private fun parseQuoteParameters() {
//TODO 在没有参数的情况下是否报告错误
//        if (at(RPAR)){
//            error("expected quote token ")
//        }


//        解析中出现的 ( 标记数量
        var lparCount = 0


        do {


            if (at(DOLLAR) && lookahead(1) == LPAR) {
                parseQuoteInterpolate()
            } else if (at(AT) && lookahead(1) == IDENTIFIER) {
                parseMacroExpression()
            } else if (atSet(QUOTE_TOKENS)) {
                if (at(LPAR)) {
                    lparCount++
                } else if (at(RPAR)) {
                    lparCount--
                    if (lparCount < 0) {
                        break
                    }
                } else if (at(DOLLAR)) {

                    errorAndAdvance("expected identifier or '(' after '$'")
                } else if (at(ESCAPE_LBRACKET) || at(ESCAPE_RBRACKET)) {
                    errorAndAdvance("Illegal Token")
                }
                advance()
            }
        } while (atSet(QUOTE_TOKENS))

    }

    fun parseMacroExpression(backToken: Boolean = false): IElementType? {
        assert(_at(AT))

        val macroExpression = mark()
        advance()

        if (at(IDENTIFIER)) {
            advance()
        } else {
            error("expected identifier after '@'")
            macroExpression.drop()
            return null
        }


//        带属性的宏
        if (at(LBRACKET)) {
            parseMacroAttrExpression()
        }


//        宏的输入
        if (at(LPAR)) {
            parseMacroInputExprWithParens()


        } else {

            val decl = mark()

            val modifiterDetector = CangJieParsing.ModifierDetector()

            cangJieParsing.parseModifierList(modifiterDetector, TokenSet.EMPTY)

            val declType = parseMacroInputExprWithoutParens(modifiterDetector)

            if (declType == null) {

                error("Macro call has no input")
                //            decl.error("Expecting a top level declaration");
                decl.drop()

            } else {
                closeDeclarationWithCommentBinders(decl, declType, true)

            }
        }
        if (backToken) {
            macroExpression.drop()
            return MACRO_EXPRESSION
        } else {
            macroExpression.done(MACRO_EXPRESSION)
            return null
        }

    }

    private fun parseMacroInputExprWithoutParens(modifiterDetector: CangJieParsing.ModifierDetector): IElementType? {

        var declType = parseMacroInputExprWithoutParensDeclaration(modifiterDetector)

        if (declType == null) {


            if (at(IDENTIFIER) && modifiterDetector.size <= 0) {
//                尝试解析为enum entry
//                val mark  = mark()
//                if (cangJieParsing.parseEnumEntry(false)) {
//
//                }

                advance()
                if (at(LPAR)) {

                    if (lookahead(1) == IDENTIFIER && lookahead(2) === COLON || lookahead(1) == RPAR) {
//                        解析为主构造方法
                        cangJieParsing.parseInitFuncValueParameterList()

                        if (at(LBRACE)) {
                            cangJieParsing.parseFunctionBody()
                        } else {
                            error("Expecting '{' ") //应该为'{'
                        }


                        declType = CLASS_MAIN_INIT

                    } else if ((lookahead(1) == IDENTIFIER && lookahead(2) === COMMA) || (lookahead(1) == IDENTIFIER && lookahead(
                            2
                        ) === LT) || (lookahead(1) == IDENTIFIER && lookahead(2) === RPAR)
                    ) {
                        advance() // LPAR
                        cangJieParsing.parseTypeList()

                        expect(RPAR, "Expecting ')'")
                        declType = ENUM_ENTRY

                    }

                } else {
                    declType = ENUM_ENTRY


                }


            } else
                if (at(IDENTIFIER) && lookahead(1) === LPAR) {


//                主构造函数
                    cangJieParsing.parseMainInitFunc()
                    declType = CLASS_MAIN_INIT
                }

        }




        return declType
    }

    private fun parseMacroInputExprWithoutParensDeclaration(modifiterDetector: CangJieParsing.ModifierDetector): IElementType? {


        return when (tokenId) {
            AT_Id -> parseMacroExpression(true)
            FUNC_KEYWORD_Id -> cangJieParsing.parseFunction(modifiterDetector)
            EXTEND_KEYWORD_Id, ENUM_KEYWORD_Id, STRUCT_KEYWORD_Id, INTERFACE_KEYWORD_Id, CLASS_KEYWORD_Id ->

                cangJieParsing.parseClass(modifiterDetector)

            LET_KEYWORD_Id, VAR_KEYWORD_Id, CONST_KEYWORD_Id -> cangJieParsing.parseVariable(modifiterDetector)

            PROP_KEYWORD_Id -> cangJieParsing.parseProperty(null, modifiterDetector)
            INIT_KEYWORD_Id -> {
                cangJieParsing.parseInitFunc()
                CLASS_INIT
            }

            else -> null
        }


    }

    private fun parseMacroInputExprWithParens() {
        assert(_at(LPAR))
        advance()

        var lparCount = 0

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

                    errorAndAdvance("expected identifier or '(' after '$'")
                } else if (at(ESCAPE_LBRACKET) || at(ESCAPE_RBRACKET)) {
                    errorAndAdvance("Illegal Token")
                }
                advance()
            }

        }
        expect(RPAR, "expected ')'")

    }


    /**
     *  宏属性表达式规则，由方括号包围的一系列引用标记组成
     * parseMacroAttrExpression
     * : LBRACKET NL* quoteToken* NL* RBRACKET
     * ;
     */

    private fun parseMacroAttrExpression() {

        if (at(LBRACKET)) {
            advance()
            var lparCount = 0
            while (atSet(QUOTE_TOKENS)) {
                if (at(LBRACKET)) {

                    lparCount++
                } else if (at(RBRACKET)) {
                    lparCount--
                    if (lparCount < 0) {
                        break
                    }
                } else if (at(DOLLAR)) {

                    errorAndAdvance("expected identifier or '(' after '$'")
                } else if (at(ESCAPE_LPAR) || at(ESCAPE_RPAR)) {
                    errorAndAdvance("Illegal Token")
                }
                advance()
            }
            expect(RBRACKET, "expected ']'")
        } else {
            error("expected '['")
        }

    }

    fun parseExpression() {

        if (at(AT)) {
//            val macroMark = mark()
//            val type = cangJieParsing.parseAnnotation(null)
//            if (type == ANNOTATION_ENTRY) {
//                error("Should call (..) for macros")
//
//
//            }
//
//            macroMark.done(MACRO_EXPRESSION)
            parseMacroExpression()
            return

        } else if (at(SPAWN_KEYWORD)) {
            parseSpawnExpression()
            return
        } else if (at(SYNCHRONIZED_KEYWORD)) {
            cangJieParsing.parseSynchronizedExpression()
            return
        } else if (at(UNSAFE_KEYWORD)) {
            parseUnsafeExpression()
            return
        } else if (at(QUOTE_KEYWORD)) {
            parseQuoteExpression()
            return
        } else if (!atSet(EXPRESSION_FIRST)) {
            error("Expecting an expression")
            return
        }
        parseBinaryExpression(Precedence.ASSIGNMENT)
    }

    //    public void parseSpawnExpression() {
    //        assert _at(SPAWN_KEYWORD);
    //        SyntaxTreeBuilder.Marker spawn = mark();
    //        advance();
    //
    //
    //        if (at(LBRACE)) {
    //            parseBlock();
    //        } else {
    //            error("Expecting '{' ");  //应该为'{'
    //        }
    //
    //
    //        spawn.done(SPAWN_EXPRESSION);
    //    }
    fun parseUnsafeExpression() {
        assert(_at(UNSAFE_KEYWORD))
        val unsafe = mark()


        advance()

        parseCallWithClosure()


        unsafe.done(UNSAFE_EXPRESSION)
    }

    fun parseSpawnExpression() {
        assert(_at(SPAWN_KEYWORD))
        val spawn = mark()
        advance()

        parseCallWithClosure()

//        if (at(LBRACE)) {
//            expect(DOUBLE_ARROW)
//            parseBlock()
//        } else {
//            error("Expecting '{' ") //应该为'{'
//        }


        spawn.done(SPAWN_EXPRESSION)
    }

    /*
     * annotatedLambda
     *  : ("@" annotationEntry)* labelDefinition? functionLiteral
     */
    private fun parseAnnotatedLambda(preferBlock: Boolean): Boolean {


        if (!at(LBRACE)) {

            return false
        }

        parseFunctionLiteral(preferBlock,  /* collapse = */true, false)



        return true
    }

    /*
         * annotatedLambda*
         */
    protected fun parseCallWithClosure(): Boolean {
        var success = false

        while (true) {
            val argument = mark()

            if (!parseAnnotatedLambda( /* preferBlock = */false)) {
                argument.drop()
                break
            }

            argument.done(LAMBDA_ARGUMENT)
            success = true
        }

        return success
    }

    /*
     * element (operation element)*
     *
     * 请查看排序表
     */
    private fun parseBinaryExpression(precedence: Precedence) {


        var expression = mark()
        precedence.parseHigherPrecedence(this)




        while (!interruptedWithNewLine() && /*atSet(precedence.getOperations())*/ precedence.getOperations()
                .contains(gtTokenType)
        ) {
//            val operation = tt()
            val operation = getGtTokenType()

            parseOperationReference(operation)
            val resultType: IElementType = precedence.parseRightHandSide(operation, this)
            expression.done(resultType)
            expression = expression.precede()
        }
        expression.drop()
    }

    private fun interruptedWithNewLine(): Boolean {
//        var a = !ALLOW_NEWLINE_OPERATIONS.contains(tt())
//        var b = myBuilder.newlineBeforeCurrentToken()
//return a && b
        return !ALLOW_NEWLINE_OPERATIONS.contains(tt()) && myBuilder.newlineBeforeCurrentToken()
    }

    private fun parseOperationReference(type: IElementType) {
        val operationReference = mark()
//        advance() // operation
        advanceGtToken(gtTokenType)
        operationReference.done(OPERATION_REFERENCE)
    }

    private fun parseOperationReference() {
        val operationReference = mark()
//        advance() // operation
        advanceGtToken(getGtTokenType())
        operationReference.done(OPERATION_REFERENCE)
    }


    fun parseDeclaration(
        scope: DeclarationParsingMode, rollbackIfDefinitelyNotExpression: Boolean,
        rollbackMacro: Boolean = false
    ): Boolean {
        val decl: PsiBuilder.Marker = mark()
        val detector: CangJieParsing.ModifierDetector = CangJieParsing.ModifierDetector()

//        修饰符
        cangJieParsing.parseModifierList(detector, TokenSet.EMPTY, rollbackMacro)
        val declType: IElementType? = parseDeclarationRest(detector, rollbackIfDefinitelyNotExpression, scope)

        return if (declType == ANNOTATION_ENTRY) {
            decl.rollbackTo()


            return parseLocalDeclaration(rollbackIfDefinitelyNotExpression, true)
        } else if (declType == INVALID_DECLARATION) {
            decl.error("Invalid declaration in scope")
            true
        } else if (declType != null) {
            // 不将前面的注释(非文档)附加到局部变量，因为它们可能会注释下面的几个语句
            closeDeclarationWithCommentBinders(
                decl, declType, declType !== VARIABLE && declType !== DESTRUCTURING_DECLARATION
            )
            true
        } else {
//            decl.drop()
            decl.rollbackTo()
//
            false
        }
    }

    /*
     * modifiers declarationRest
     */
    private fun parseLocalDeclaration(
        rollbackIfDefinitelyNotExpression: Boolean,
        rollbackMacro: Boolean = false
    ): Boolean {
        return parseDeclaration(DeclarationParsingMode.LOCAL, rollbackIfDefinitelyNotExpression, rollbackMacro)
    }

    private fun parseDeclarationRest(
        detector: CangJieParsing.ModifierDetector, failIfDefinitelyNotExpression: Boolean, scope: DeclarationParsingMode
    ): IElementType? {


        val keyword = tt()
        if (failIfDefinitelyNotExpression) {
            if (keyword != FUNC_KEYWORD) return null
            return cangJieParsing.parseFunction()


        }


        return cangJieParsing.parseCommonDeclaration(
            detector, CangJieParsing.NameParsingMode.REQUIRED, scope
        )
    }


    @OptIn(ExperimentalStdlibApi::class)
    companion object {
        var ALL_OPERATIONS: TokenSet? = null

        @JvmField
        val IDENTIFIER_RECOVERY_SET = TokenSet.create(
            IDENTIFIER, UNDERLINE
        )

        private fun doneOrDrop(
            marker: PsiBuilder.Marker,
            type: IElementType,
            condition: Boolean
        ) {
            if (condition) {
                marker.done(type)
            } else {
                marker.drop()
            }
        }

        @JvmStatic
        val EXPRESSION_FOLLOW = TokenSet.create(
            EOL_OR_SEMICOLON, ARROW, DOUBLE_ARROW, COMPOSITION, COMMA, RBRACE, RPAR, RBRACKET
        )

        val ALLOW_NEWLINE_OPERATIONS = TokenSet.create(
            DOT, COLON, AS_KEYWORD, ANDAND, OROR, ELVIS, SAFE_ACCESS
        )

        @JvmStatic
        val EXPRESSION_FIRST = TokenSet.orSet(
            TokenSet.create(
                // Prefix
                MINUS,
                PLUS,
                MINUSMINUS,
                PLUSPLUS,
                EXCL,
                LPAR,  // parenthesized
                // literal constant
                TRUE_KEYWORD,
                FALSE_KEYWORD,
                OPEN_QUOTE,
                INTEGER_LITERAL,
                RUNE_LITERAL,
                CHARACTER_BYTE_LITERAL,
                FLOAT_LITERAL,

                LBRACE,  // functionLiteral
                FUNC_KEYWORD,  // expression function
                THIS_KEYWORD,  // this
                SUPER_KEYWORD,  // super
                IF_KEYWORD,  // if
                MATCH_KEYWORD,  // when
                TRY_KEYWORD,  // try

                // jump
                THROW_KEYWORD,
                RETURN_KEYWORD,
                CONTINUE_KEYWORD,
                BREAK_KEYWORD,  // loop
                FOR_KEYWORD,
                WHILE_KEYWORD,
                DO_KEYWORD,
                IDENTIFIER,  // SimpleName
                LBRACKET,// Collection literal expression
//                UNSAFE_EXPRESSION
//                UNSAFE_KEYWORD


                //线程
                SPAWN_KEYWORD,
                SYNCHRONIZED_KEYWORD
            ),
            BASICTYPES,
        )
        private val TYPE_ARGUMENT_LIST_STOPPERS = TokenSet.create(
            INTEGER_LITERAL,
            FLOAT_LITERAL,
            RUNE_LITERAL,
            CHARACTER_BYTE_LITERAL,
            OPEN_QUOTE,
            PACKAGE_KEYWORD,
            AS_KEYWORD,
            ELVIS, SAFE_ACCESS,
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
//            COMMA
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

//            CASE_KEYWORD
        )

        @SuppressWarnings("WeakerAccess")
        val STATEMENT_FIRST = TokenSet.orSet(
            EXPRESSION_FIRST, TokenSet.create( // declaration
                FUNC_KEYWORD, LET_KEYWORD, CONST_KEYWORD, VAR_KEYWORD, INTERFACE_KEYWORD, CLASS_KEYWORD,
                SPAWN_KEYWORD, SYNCHRONIZED_KEYWORD
            ), MODIFIER_KEYWORDS, BASICTYPES, SPECIAL_MODIFIER_KEYWORDS, TokenSet.create(AT)
        )
        val STATEMENT_NEW_LINE_QUICK_RECOVERY_SET = TokenSet.orSet(
            TokenSet.andSet(
                STATEMENT_FIRST, TokenSet.andNot(KEYWORDS, TokenSet.create(IN_KEYWORD))
            ), TokenSet.create(EOL_OR_SEMICOLON)
        )
        val logger = Logger.getInstance(
            CangJieExpressionParsing::class.java
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
