package com.huawei.cangjie.parsing


import com.huawei.cangjie.CjNodeTypes.*
import com.huawei.cangjie.lexer.CjTokens
import com.huawei.cangjie.lexer.CjTokens.*
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
                logger.info("AS")
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
                    kotlin.error(severalStatementsError)
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
            }
            else if (at(LBRACKET)) {
                parseArrayAccess()
                expression.done(ARRAY_ACCESS_EXPRESSION)
            }
            else if (parseCallSuffix()) {
                expression.done(CALL_EXPRESSION)
            }
            else if (at(DOT)) {
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
            }else{
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
    fun parseDoubleColonSuffix(expression:PsiBuilder.Marker):Boolean{

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
//            LPAR_Id -> parseParenthesizedExpression()
//            //索引
//            LBRACKET_Id -> parseCollectionLiteralExpression()
//            //this
//            THIS_KEYWORD_Id -> parseThisExpression()
//            //super
//            SUPER_KEYWORD_Id -> parseSuperExpression()
////        throw
//            THROW_KEYWORD_Id -> parseThrow()
//            //return
//            RETURN_KEYWORD_Id -> parseReturn()
//            //continue
//            CONTINUE_KEYWORD_Id -> parseJump(CjNodeTypes.CONTINUE)
//            //break
//            BREAK_KEYWORD_Id -> parseJump(CjNodeTypes.BREAK)
//            //if
//            IF_KEYWORD_Id -> parseIf()
//            //match
//            MATCH_KEYWORD_Id -> parseMatch()
//            //try
//            TRY_KEYWORD_Id -> parseTry()
//            //for
//            FOR_KEYWORD_Id -> parseFor()
//            //while
//            WHILE_KEYWORD_Id -> parseWhile()
//            //do while
//            DO_KEYWORD_Id -> parseDoWhile()
            //标识符
            IDENTIFIER_Id -> parseSimpleNameExpression()
            //lambda
//            LBRACE_Id -> parseFunctionLiteral()
            //字符串模板
//       OPEN_QUOTE_Id -> parseStringTemplate()
            //true false
            TRUE_KEYWORD_Id, FALSE_KEYWORD_Id -> parseOneTokenExpression(BOOLEAN_CONSTANT)
            //整数
            INTEGER_LITERAL_Id -> parseOneTokenExpression(INTEGER_CONSTANT)
//            //字符
            CHARACTER_LITERAL_Id -> parseOneTokenExpression(CHARACTER_CONSTANT)
            //浮点数
          FLOAT_LITERAL_Id -> parseOneTokenExpression(FLOAT_CONSTANT)
//class interface func let var
            CLASS_KEYWORD_Id, INTERFACE_KEYWORD_Id, FUNC_KEYWORD_Id, LET_KEYWORD_Id, VAR_KEYWORD_Id -> if (!parseLocalDeclaration(
                    myBuilder.newlineBeforeCurrentToken(),

                    )
            ) {
                ok = false
            }

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
            kotlin.error(missingElementErrorMessage)
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

        logger.info("parseBinaryExpression ${489}")
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

            LBRACE,  // functionLiteral
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
