package com.huawei.cangjie.parsing


import com.google.common.collect.ImmutableMap
import com.huawei.cangjie.CjNodeTypes.*
import com.huawei.cangjie.lexer.CjToken
import com.huawei.cangjie.lexer.CjTokens
import com.huawei.cangjie.lexer.CjTokens.*
import com.huawei.cangjie.parsing.CangJieParsing.PARAMETER_NAME_RECOVERY_SET
import com.intellij.lang.PsiBuilder
import com.intellij.openapi.diagnostic.Logger
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


        PREFIX(MINUS, PLUS, MINUSMINUS, PLUSPLUS, EXCL) {

            override fun parseHigherPrecedence(parser: CangJieExpressionParsing) {
                throw IllegalStateException("Don't call this method")
            }

        },

        AS(AS_KEYWORD) {

            override fun parseRightHandSide(operation: IElementType, parser: CangJieExpressionParsing): IElementType {
                logger.info("AS")
                parser.cangJieParsing.parseTypeRefWithoutIntersections()
                return BINARY_WITH_TYPE
            }

            override fun parseHigherPrecedence(parser: CangJieExpressionParsing) {

                parser.parsePrefixExpression()
            }
        },

        MULTIPLICATIVE(MUL, DIV, PERC),
        ADDITIVE(PLUS, MINUS),
        RANGE(CjTokens.RANGE),
        SIMPLE_NAME(IDENTIFIER),
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


        ASSIGNMENT(EQ, PLUSEQ, MINUSEQ, MULTEQ, DIVEQ, PERCEQ);

        private var higher: Precedence? = null
        private val operations: TokenSet

        @OptIn(ExperimentalStdlibApi::class)
        companion object {
            val logger = Logger.getInstance(
                Precedence::class.java
            )

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


    override fun create(builder: SemanticWhitespaceAwarePsiBuilder?): CangJieParsing = cangJieParsing.create(builder)


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
     * (SimpleName "=")? "*"? element
     */
    private fun parseValueArgument() {
        val argument = mark()
        if (at(IDENTIFIER) && lookahead(1) === EQ) {
            val argName = mark()
            val reference = mark()
            advance() // IDENTIFIER
            reference.done(REFERENCE_EXPRESSION)
            argName.done(VALUE_ARGUMENT_NAME)
            advance() // EQ
        }
        if (at(MUL)) {
            advance() // MUL
        }
        parseExpression()
        argument.done(VALUE_ARGUMENT)
    }

    /*
     * valueArguments
     *   : "(" (SimpleName "=")? "*"? element{","} ")"
     *   ;
     */
    fun parseValueArgumentList() {
        val list = mark()
        myBuilder.disableNewlines()
        if (expect(LPAR, "Expecting an argument list", EXPRESSION_FOLLOW)) {
            if (!at(RPAR)) {
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
            expect(RPAR, "Expecting ')'", EXPRESSION_FOLLOW)
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
        var firstExpressionParsed = parseAtomicExpression()



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
            while (!eof() && !at(RBRACE)) {

                if(!at(CASE_KEYWORD)){
                    error("Expecting 'case'")
                }

                parseMatchEntry()
            }
            expect(RBRACE, "Expecting '}'")
        }
        myBuilder.restoreNewlinesState()
        match.done(MATCH)
    }

    /**
     * caseBody
     *
     */
    private fun parseCaseBody() {
        val body = mark()
        if (!at(SEMICOLON)) {
           parseStatements()
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

            parseCaseCondition()
            while (at(OR)) {
                advance() // OR
                parseCaseCondition()
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
    private fun parseCaseCondition() {
        val condition = mark()

        //是否常量模式
        fun isConstantPattern(): Boolean {
            return at(INTEGER_LITERAL) || at(CHARACTER_LITERAL) || at(TRUE_KEYWORD) || at(FALSE_KEYWORD) || at(
                OPEN_QUOTE)
        }

        //是否通配符模式
        fun isWildcardPattern(): Boolean {
            return  myBuilder.tokenText == "_"
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

        when{
            isTypePattern() -> {
              expect(IDENTIFIER,"Expecting a variable name")
                expect(COLON,"Expecting ':'")
                cangJieParsing.parseTypeRef()
            }


            else  -> {
                error("Expecting a pattern")
            }
        }

        condition.done(CASE_CONDITION)
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
        parseCondition()
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


    /*
     * if
     *   : "if" "(" element ")" element SEMI? ("else" element)?
     *   ;
     */
    private fun parseIf() {
        assert(_at(IF_KEYWORD))
        val marker = mark()
        advance() //IF_KEYWORD
        parseCondition()


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
     *   : "[" element{","}? "]"
     *   ;
     */
    private fun parseCollectionLiteralExpression() {
        parseAsCollectionLiteralExpression(COLLECTION_LITERAL_EXPRESSION, true, "Expecting an element")
    }

    /*
     * "(" expression ")"
     */
    private fun parseParenthesizedExpression() {
        assert(_at(LPAR))
        val mark = mark()
        myBuilder.disableNewlines()
        advance() // LPAR
        if (at(RPAR)) {
            error("Expecting an expression")
        } else {
            parseExpression()
        }
        expect(RPAR, "Expecting ')'")
        myBuilder.restoreNewlinesState()
        mark.done(PARENTHESIZED)
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
            parseExpression()
            if (!at(COMMA)) break
            advance() // COMMA
        }
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
                parseExpression()
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

    /*
     * blockLevelExpression
     *  :  expression
     *  ;
     */
    private fun parseBlockLevelExpression() {
        parseExpression()
    }

    fun parseExpression() {

        //TODO 不知道为什么会被插入一个虚拟标识符，暂时先这样处理
//        if(at(IDENTIFIER ) ){
//            error("Expecting an expression")
//            return
//        }
        if (!atSet(EXPRESSION_FIRST)) {
            error("Expecting an expression")
            return
        }
        parseBinaryExpression(Precedence.ASSIGNMENT)
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

            val operation = tt()
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
        advance() // operation
        operationReference.done(OPERATION_REFERENCE)
    }

    /*
     * modifiers declarationRest
     */
    private fun parseLocalDeclaration(rollbackIfDefinitelyNotExpression: Boolean): Boolean {
        val decl: PsiBuilder.Marker = mark()
        val detector: CangJieParsing.ModifierDetector = CangJieParsing.ModifierDetector()

//        修饰符
        cangJieParsing.parseModifierList(detector, TokenSet.EMPTY)
        val declType: IElementType? = parseLocalDeclarationRest(detector, rollbackIfDefinitelyNotExpression)

        return if (declType != null) {
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
            return cangJieParsing.parseFunction(true)
        }


        return cangJieParsing.parseCommonDeclaration(
            detector, CangJieParsing.NameParsingMode.REQUIRED, CangJieParsing.DeclarationParsingMode.LOCAL
        )
    }


    @OptIn(ExperimentalStdlibApi::class)
    companion object {
        var ALL_OPERATIONS: TokenSet? = null
        val EXPRESSION_FOLLOW = TokenSet.create(
            EOL_OR_SEMICOLON, ARROW, COMMA, RBRACE, RPAR, RBRACKET
        )

        val ALLOW_NEWLINE_OPERATIONS = TokenSet.create(
            DOT, COLON, AS_KEYWORD, ANDAND, OROR
        )

        val EXPRESSION_FIRST = TokenSet.create( // Prefix
            MINUS, PLUS, MINUSMINUS, PLUSPLUS, EXCL, LPAR,  // parenthesized
            // literal constant
            TRUE_KEYWORD, FALSE_KEYWORD, OPEN_QUOTE, INTEGER_LITERAL, CHARACTER_LITERAL, FLOAT_LITERAL,

//            LBRACE,  // functionLiteral
            FUNC_KEYWORD,  // expression function
            THIS_KEYWORD,  // this
            SUPER_KEYWORD,  // super
            IF_KEYWORD,  // if
            MATCH_KEYWORD,  // when
            TRY_KEYWORD,  // try

            // jump
            THROW_KEYWORD, RETURN_KEYWORD, CONTINUE_KEYWORD, BREAK_KEYWORD,  // loop
            FOR_KEYWORD, WHILE_KEYWORD, DO_KEYWORD, IDENTIFIER,  // SimpleName
            LBRACKET // Collection literal expression
        )
        private val TYPE_ARGUMENT_LIST_STOPPERS = TokenSet.create(
            INTEGER_LITERAL,
            FLOAT_LITERAL,
            CHARACTER_LITERAL,
            OPEN_QUOTE,
            PACKAGE_KEYWORD,
            AS_KEYWORD,

            INTERFACE_KEYWORD,
            CLASS_KEYWORD,
            THIS_KEYWORD,
            LET_KEYWORD,
            VAR_KEYWORD,
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

            COLON
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
                FUNC_KEYWORD, LET_KEYWORD, VAR_KEYWORD, INTERFACE_KEYWORD, CLASS_KEYWORD

            ), MODIFIER_KEYWORDS
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
            for (token in tokens.getTypes()) {
                builder.put(token.toString(), token as CjToken)
            }
            return builder.build()
        }

        init {
            val operations: MutableSet<IElementType> = HashSet()
            val values: Array<Precedence> = Precedence.values()
            for (precedence in values) {
                operations.addAll(listOf(*precedence.getOperations().getTypes()))
            }
            ALL_OPERATIONS = TokenSet.create(*operations.toTypedArray<IElementType>())


        }

        init {
            val operations: Array<IElementType> = OPERATIONS.getTypes()
            val opSet: MutableSet<IElementType> = HashSet(listOf(*operations))
            val usedOperations: Array<IElementType> = ALL_OPERATIONS?.getTypes() ?: emptyArray()
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
