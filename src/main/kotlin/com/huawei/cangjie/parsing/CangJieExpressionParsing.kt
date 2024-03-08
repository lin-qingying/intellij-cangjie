package com.huawei.cangjie.parsing


import com.google.common.collect.ImmutableMap
import com.huawei.cangjie.CjNodeTypes.*
import com.huawei.cangjie.lexer.CjToken
import com.huawei.cangjie.lexer.CjTokens
import com.huawei.cangjie.lexer.CjTokens.*
import com.huawei.cangjie.parsing.CangJieParsing.PARAMETER_NAME_RECOVERY_SET
import com.intellij.lang.PsiBuilder
import com.intellij.lang.PsiBuilderUtil
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.Pair
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet

open class CangJieExpressionParsing(
    builder: SemanticWhitespaceAwarePsiBuilder, private val cangJieParsing: CangJieParsing, isLazy: Boolean
) : AbstractCangJieParsing(
    builder, isLazy
) {


    @SuppressWarnings("UnusedDeclaration")

    enum class Precedence(vararg operations: IElementType) {
        POSTFIX(
            PLUSPLUS, MINUSMINUS, DOT
        ),


        PREFIX(MINUS, PLUS, EXCL) {

            override fun parseHigherPrecedence(parser: CangJieExpressionParsing) {
                throw IllegalStateException("Don't call this method")
            }

        },

        AS(AS_KEYWORD) {

            override fun parseRightHandSide(operation: IElementType, parser: CangJieExpressionParsing): IElementType {

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

        //        SIMPLE_NAME(IDENTIFIER),
        IN_OR_IS(IN_KEYWORD, IS_KEYWORD) {
            override fun parseRightHandSide(operation: IElementType, parser: CangJieExpressionParsing): IElementType {
                if (operation === IS_KEYWORD) {
                    parser.cangJieParsing.parseTypeRefWithoutIntersections()
                    return IS_EXPRESSION
                }
                return super.parseRightHandSide(operation, parser)
            }
        },

        COMPARISON(LT, GT, LTEQ, GTEQ),
        EQUALITY(EQEQ, EXCLEQ),
        CONJUNCTION(ANDAND),
        DISJUNCTION(OROR),

        //位运算
        BITWISE(AND, OR, XOR, LTLT, GTGT, LTLTEQ, GTGTEQ),


        ASSIGNMENT(EQ, PLUSEQ, MINUSEQ, MULTEQ, DIVEQ, PERCEQ, ANDEQ, OREQ, XOREQ, LTLTEQ, GTGTEQ, MULMULEQ);

        private var higher: Precedence? = null
        private val operations: TokenSet

        @OptIn(ExperimentalStdlibApi::class)
        companion object {


            init {
                val values: Array<Precedence> = Precedence.values()
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
        if(at(COLON)){
            advance()
            if(at(INTEGER_LITERAL)){
                advance()
            }else{

                error("Lack of step frequency")
//                errorAndAdvance("Expecting an integer literal")
            }
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

    /*
     * valueArguments
     *   : "(" (SimpleName "=")? "*"? element{","} ")"
     *   ;
     */
    fun parseValueArgumentList(struct: Pair<CjToken, CjToken> = Pair(LPAR, RPAR)) {
        val list = mark()

        val sturctStart = struct.first
        val sturctEnd = struct.second

        myBuilder.disableNewlines()
        if (expect(sturctStart, "Expecting an argument list", EXPRESSION_FOLLOW)) {
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

    /*
     * callSuffix
     *   : typeArguments? valueArguments annotatedLambda
     *   : typeArguments annotatedLambda
     *   ;
     */
    private fun parseCallSuffix(): Boolean {
        if (at(LPAR)) {
            parseValueArgumentList()

        } else if (at(LT)) {
            val typeArgumentList = mark()
            if (cangJieParsing.tryParseTypeArgumentList(TYPE_ARGUMENT_LIST_STOPPERS)) {
                typeArgumentList.done(TYPE_ARGUMENT_LIST)
                if (!myBuilder.newlineBeforeCurrentToken() && at(LPAR)) parseValueArgumentList()

            } else {
                typeArgumentList.rollbackTo()
                return false
            }
        } else {
            return false
        }
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


//        if (atSet(BASICTYPES)) {
//            if (lookahead(1) === LPAR) {
//                cangJieParsing.parseTypeRef()//BASICTYPES
//                if (parseCallSuffix()) {
//                    expression.done(CALL_EXPRESSION)
//
//                }
//
//            } else if (lookahead(1) === DOT) {
//                cangJieParsing.parseTypeRef()//BASICTYPES
//                while (at(DOT)) {
//
//                    val expressionType: IElementType = DOT_QUALIFIED_EXPRESSION
//                    advance() // DOT
//                    if (!parseAtomicExpression()) {
//                        expression.drop()
//                        expression = mark()
//                        continue
//                    }
////                    parseSelectorCallExpression()
//                    expression.done(expressionType)
//
//                    expression = expression.precede()
//                }
//            } else {
//                error("expected expression or declaration, found keyword ${myBuilder.tokenText}")
//            }
//            expression.drop()
//            return
//        }


        var firstExpressionParsed =
            if ((atSet(BASICTYPES) && lookahead(1) === LPAR) || (atSet(BASICTYPES) && lookahead(1) === DOT)) {
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
            } else if (at(LBRACKET)) {
                parseArrayAccess()
                expression.done(ARRAY_ACCESS_EXPRESSION)
            } else if (parseCallSuffix()) {
                expression.done(CALL_EXPRESSION)
            } else if (at(DOT)) {
                val expressionType: IElementType = DOT_QUALIFIED_EXPRESSION
                advance() // DOT
                if (!firstExpressionParsed) {
                    expression.drop()
                    expression = mark()
                    firstExpressionParsed = parseAtomicExpression()
                    continue
                }
                parseSelectorCallExpression()
                expression.done(expressionType)
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

    /*
     * atomicExpression typeParameters? valueParameters? functionLiteral*
     */
    private fun parseSelectorCallExpression() {
        val mark = mark()
        parseAtomicExpression()
        if (!myBuilder.newlineBeforeCurrentToken() && parseCallSuffix()) {
            mark.done(CALL_EXPRESSION)
        } else {
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
//            LBRACE_Id -> parseFunctionLiteral()
            //字符串模板
            OPEN_QUOTE_Id -> parseStringTemplate()
            //true false
            TRUE_KEYWORD_Id, FALSE_KEYWORD_Id -> parseOneTokenExpression(BOOLEAN_CONSTANT)
            //整数
            INTEGER_LITERAL_Id -> parseOneTokenExpression(INTEGER_CONSTANT)
//            //字符
            CHARACTER_LITERAL_Id -> parseOneTokenExpression(CHARACTER_CONSTANT)
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
        assert(_at(TUPLE_LTIERAL))
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
        tuple.done(TUPLE_LITERAL_EXPRESSION)

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

        parseCondition()


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
                    parseMatchEntry()

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
                val error = body.precede()

                error.error("match case cannot be empty")

                body.drop()
                return


            } else {
                parseStatements(CASE_KEYWORD)
            }


        }
        body.done(BODY)
    }

    /*
     * matchEntry
     *   : case caseCondition{|} "=>" caseBody
     *   ;
     */
    private fun parseMatchEntry() {
        val entry = mark()

        if (at(CASE_KEYWORD)) {
            advance() // CASE_KEYWORD

            parseCasePattern()
            while (at(OR)) {
                advance() // OR
                parseCasePattern()
            }
            expect(DOUBLE_ARROW, "Expecting '=>'")
            parseCaseBody()

        } else {
            error("Expecting 'case'")
        }

        entry.done(MATCH_ENTRY)

    }

    /**
     * case condition
     * (常量 | 通配符(_) | 绑定模式(SimpleName) | Tuple模式( (p1,p2,...,pn) ) | 类型模式(SimpleName : type) | Enum模式(SimpleName(SimpleName)? | Tuple与Enum嵌套  )  )
     */
    private fun parseCasePattern() {
        val condition = mark()


        class CasePattern {
            /**
             * 绑定模式(id) | 类型模式(id:type) | 枚举模式(id(expression{,})?)
             */
            fun parseSimpleNameExpression() {
                assert(_at(IDENTIFIER))
                val mark = mark()
                var type = 1

                if (lookahead(1) == DOT) {
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
                    advance() // IDENTIFIER

                    //1.绑定模式
                    //2.类型模式
                    //3.枚举模式
                    if (at(COLON)) {
                        advance() // COLON
                        type = 2
                        cangJieParsing.parseTypeRef()

                    } else if (at(LPAR)) {
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
                    1 -> mark.done(REFERENCE_EXPRESSION)
                    2 -> mark.done(TYPE_PATTERN)
                    3 -> mark.done(ENUM_PATTERN)
                }


            }


            fun parseUnderline() {
                assert(_at(UNDERLINE))
                advance()


                if (at(COLON)) {
                    advance() // COLON
                    //处理类型
                    cangJieParsing.parseTypeRef()
                }


            }

            fun parseExpression() {
                when (tokenId) {

                    UNDERLINE_Id -> parseUnderline()
                    LPAR_Id -> parseParenthesizedExpression()
                    INTEGER_LITERAL_Id -> parseOneTokenExpression(INTEGER_CONSTANT)
                    CHARACTER_LITERAL_Id -> parseOneTokenExpression(CHARACTER_CONSTANT)
                    TRUE_KEYWORD_Id, FALSE_KEYWORD_Id -> parseOneTokenExpression(BOOLEAN_CONSTANT)
                    FLOAT_LITERAL_Id -> parseOneTokenExpression(FLOAT_CONSTANT)
                    OPEN_QUOTE_Id -> parseStringTemplate()
                    IDENTIFIER_Id -> parseSimpleNameExpression()
                    else -> error("Expecting a pattern expression")

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
                    isUnit -> mark.done(UNIT_CONSTANT)
                    isTuple -> mark.done(TUPLE_LITERAL_EXPRESSION)

                    else -> mark.drop()
                }

            }

        }


        val casePattern = CasePattern()

        casePattern.parseExpression()



        //是否常量模式
        fun isConstantPattern(): Boolean {
            return at(INTEGER_LITERAL) || at(CHARACTER_LITERAL) || at(TRUE_KEYWORD) || at(FALSE_KEYWORD) || at(
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


        condition.done(CASE_PATTERN)
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

                if (at(LPAR)) {
                    val destructuringDeclaration = mark()
                    cangJieParsing.parseMultiDeclarationName(
                        IN_KEYWORD_L_BRACE_SET,
                        IN_KEYWORD_L_BRACE_RECOVERY_SET
                    )
                    destructuringDeclaration.done(DESTRUCTURING_DECLARATION)
                } else {
                    expect(
                        IDENTIFIER,
                        "Expecting a variable name",
                        COLON_IN_KEYWORD_SET
                    )
                    if (at(COLON)) {
                        errorAndAdvance("the pattern in for-in expression must be irrefutable")
                    }
                }
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
                    cangJieParsing.parseValueParameter( /*typeRequired = */true)
                    if (at(COMMA)) {
                        advance() // trailing comma
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


    private fun parseControlStructureBody() {


        if (at(LBRACE)) {
            parseFunctionLiteral()
            return
        } else {
//            parseBlockLevelExpression()
            error("Expecting '{'")
        }


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


    /**
     * 函数式
     */
    fun parseFunctionLiteral() {
        assert(_at(LBRACE))
        val literalExpression = mark()
        val literal = mark()
        myBuilder.enableNewlines()
        advance() // LBRACE
//        var paramsFound = false
//        val token = tt()
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
        literal.drop()
        parseStatements()
        expect(RBRACE, "Expecting '}'")
        literalExpression.done(BLOCK)
        myBuilder.restoreNewlinesState()
        return
//        }
//        if (collapse && isLazy) {
//            cangJieParsing.advanceBalancedBlock()
//            literal.done(FUNCTION_LITERAL)
//            literalExpression.collapse(LAMBDA_EXPRESSION)
//        } else {
//            val body = mark()
//            parseStatements()
//            body.done(BLOCK)
//            body.setCustomEdgeTokenBinders(PRECEDING_ALL_COMMENTS_BINDER, TRAILING_ALL_COMMENTS_BINDER)
//            expect(RBRACE, "Expecting '}'")
//            literal.done(FUNCTION_LITERAL)
//            literalExpression.done(LAMBDA_EXPRESSION)
//        }
//        myBuilder.restoreNewlinesState()
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
                errorAndAdvance("Expecting '<-'")
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
//        parseLabelReferenceWithNoWhitespace()
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
            isTuple -> mark.done(TUPLE_LITERAL_EXPRESSION)

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
    private fun parseStringTemplate() {
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
        if(!at(RBRACKET)){
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
     */
    fun parseSimpleNameExpression() {
        val simpleName = mark()
        expect(IDENTIFIER, "Expecting an identifier")
        simpleName.done(REFERENCE_EXPRESSION)
    }

    private fun parseAsCollectionLiteralExpression(
        nodeType: IElementType, canBeEmpty: Boolean, missingElementErrorMessage: String
    ) {
        assert(_at(LBRACKET))
        val innerExpressions = mark()
        myBuilder.disableNewlines()
        advance() // LBRACKET
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
            myBuilder.restoreJoiningComplexTokensState()
            parsePostfixExpression()
        }
    }

    /*
     * statement
     *  : declaration
     *  : blockLevelExpression
     *  ;
     */
    private fun parseStatement() {
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

    fun parseExpression() {

        if (at(AT)) {
            val macroMark = mark()
            val type = cangJieParsing.parseAnnotation(null)
            if (type == ANNOTATION_ENTRY) {
                error("Should call (..) for macros")


            }

            macroMark.done(MACRO_EXPRESSION)

            return
        } else if (at(UNSAFE_KEYWORD)) {
            cangJieParsing.parseUnsafeExpression()
            return
        } else if (!atSet(EXPRESSION_FIRST)) {
            error("Expecting an expression")
            return
        }
        parseBinaryExpression(Precedence.ASSIGNMENT)
    }

    private fun getGtTokenType(): IElementType {
        var tokenType = tt()
        if (tokenType !== GT) return tokenType

        if (rawLookup(1) === GT) {
            tokenType = if (rawLookup(2) === EQ) {
                GTGTEQ
            } else {
                GTGT
            }
        } else if (rawLookup(1) === EQ) {
            tokenType = GTEQ
        }
        return tokenType
    }

    private fun advanceGtToken(type: IElementType) {
        val gtToken = mark()
        if (type === GTGTEQ) {
            PsiBuilderUtil.advance(myBuilder, 3)
        } else if (type === GTGT || type === GTEQ) {
            PsiBuilderUtil.advance(myBuilder, 2)
        } else {
            gtToken.drop()
            myBuilder.advanceLexer()
            return
        }
        gtToken.collapse(type)
    }

    /*
     * element (operation element)*
     *
     * 请查看排序表
     */
    private fun parseBinaryExpression(precedence: Precedence) {


        var expression = mark()
        precedence.parseHigherPrecedence(this)



        while (!interruptedWithNewLine() && atSet(precedence.getOperations())) {

            val operation = getGtTokenType()
            parseOperationReference()
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

    private fun parseOperationReference() {
        val operationReference = mark()
//        advance() // operation
        advanceGtToken(getGtTokenType())
        operationReference.done(OPERATION_REFERENCE)
    }

    /*
     * modifiers declarationRest
     */
    private fun parseLocalDeclaration(
        rollbackIfDefinitelyNotExpression: Boolean,
        rollbackMacro: Boolean = false
    ): Boolean {
        val decl: PsiBuilder.Marker = mark()
        val detector: CangJieParsing.ModifierDetector = CangJieParsing.ModifierDetector()

//        修饰符
        cangJieParsing.parseModifierList(detector, TokenSet.EMPTY, rollbackMacro)
        val declType: IElementType? = parseLocalDeclarationRest(detector, rollbackIfDefinitelyNotExpression)

        return if (declType == ANNOTATION_ENTRY) {
            decl.rollbackTo()


            return parseLocalDeclaration(rollbackIfDefinitelyNotExpression, true)
        } else if (declType != null) {
            // 不将前面的注释(非文档)附加到局部变量，因为它们可能会注释下面的几个语句
            closeDeclarationWithCommentBinders(
                decl, declType, declType !== VARIABLE && declType !== DESTRUCTURING_DECLARATION
            )
            true
        } else {
            decl.rollbackTo()
            false
        }
    }

    private fun parseLocalDeclarationRest(
        detector: CangJieParsing.ModifierDetector, failIfDefinitelyNotExpression: Boolean
    ): IElementType? {


        val keyword = tt()
        if (failIfDefinitelyNotExpression) {
            if (keyword != FUNC_KEYWORD) return null
            return cangJieParsing.parseFunction()


        }


        return cangJieParsing.parseCommonDeclaration(
            detector, CangJieParsing.NameParsingMode.REQUIRED, CangJieParsing.DeclarationParsingMode.LOCAL
        )
    }


    @OptIn(ExperimentalStdlibApi::class)
    companion object {
        var ALL_OPERATIONS: TokenSet? = null

        @JvmStatic
        val EXPRESSION_FOLLOW = TokenSet.create(
            EOL_OR_SEMICOLON, ARROW, DOUBLE_ARROW, COMMA, RBRACE, RPAR, RBRACKET
        )

        val ALLOW_NEWLINE_OPERATIONS = TokenSet.create(
            DOT, COLON, AS_KEYWORD, ANDAND, OROR
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
                CHARACTER_LITERAL,
                CHARACTER_BYTE_LITERAL,
                FLOAT_LITERAL,

//            LBRACE,  // functionLiteral
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
            ),
            BASICTYPES,
        )
        private val TYPE_ARGUMENT_LIST_STOPPERS = TokenSet.create(
            INTEGER_LITERAL,
            FLOAT_LITERAL,
            CHARACTER_LITERAL,
            CHARACTER_BYTE_LITERAL,
            OPEN_QUOTE,
            PACKAGE_KEYWORD,
            AS_KEYWORD,

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
                FUNC_KEYWORD, LET_KEYWORD, CONST_KEYWORD, VAR_KEYWORD, INTERFACE_KEYWORD, CLASS_KEYWORD

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
            val values: Array<Precedence> = Precedence.values()
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
