package com.huawei.cangjie.parsing


import com.huawei.cangjie.CjNodeTypes.*
import com.huawei.cangjie.lexer.CjTokens.*
import com.intellij.lang.PsiBuilder
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import java.util.*

open class CangJieExpressionParsing(
    builder: SemanticWhitespaceAwarePsiBuilder, private val cangJieParsing: CangJieParsing, isLazy: Boolean
) : AbstractCangJieParsing(
    builder, isLazy
) {


    enum class Precedence(vararg operations: IElementType) {
        POSTFIX(
            PLUSPLUS, MINUSMINUS, DOT
        ),


        PREFIX(MINUS, PLUS, MINUSMINUS, PLUSPLUS, EXCL) {
            // annotations
//            override fun parseHigherPrecedence(parser: CangJieExpressionParsing?) {
//                throw IllegalStateException("Don't call this method")
//            }

        },

        AS(AS_KEYWORD) {

            override fun parseRightHandSide(operation: IElementType?, parser: CangJieExpressionParsing?): IElementType {
                parser?.cangJieParsing?.parseTypeRefWithoutIntersections()
                return BINARY_WITH_TYPE
            }

        },

        MULTIPLICATIVE(MUL, DIV, PERC), ADDITIVE(PLUS, MINUS),

        SIMPLE_NAME(IDENTIFIER), IN_OR_IS(IN_KEYWORD, IS_KEYWORD) {
            override fun parseRightHandSide(operation: IElementType?, parser: CangJieExpressionParsing?): IElementType {
                if (operation === IS_KEYWORD) {
                    parser?.cangJieParsing?.parseTypeRefWithoutIntersections()
                    return IS_EXPRESSION
                }
                return super.parseRightHandSide(operation, parser)
            }


        },

        COMPARISON(LT, GT, LTEQ, GTEQ), EQUALITY(EQEQ, EXCLEQ), CONJUNCTION(ANDAND), DISJUNCTION(OROR),


        RANGE_TO(RANGE), ASSIGNMENT(EQ, PLUSEQ, MINUSEQ, MULTEQ, DIVEQ, PERCEQ);

        private var higher: Precedence? = null
        private val operations: TokenSet

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
        open fun parseRightHandSide(operation: IElementType?, parser: CangJieExpressionParsing?): IElementType {
            parseHigherPrecedence(parser)
            return BINARY_EXPRESSION
        }

        open fun parseHigherPrecedence(parser: CangJieExpressionParsing?) {
            assert(higher != null)
            if (parser != null) {
                higher?.let { parser.parseBinaryExpression(it) }
            }
        }

    }


    override fun create(builder: SemanticWhitespaceAwarePsiBuilder?): CangJieParsing = cangJieParsing.create(builder)

    /*package*/
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

    val STATEMENT_FIRST = TokenSet.orSet(
        EXPRESSION_FIRST, TokenSet.create( // declaration
            FUNC_KEYWORD, LET_KEYWORD, VAR_KEYWORD, INTERFACE_KEYWORD, CLASS_KEYWORD

        ), MODIFIER_KEYWORDS
    )

    private val STATEMENT_NEW_LINE_QUICK_RECOVERY_SET = TokenSet.orSet(
        TokenSet.andSet(
            STATEMENT_FIRST, TokenSet.andNot(KEYWORDS, TokenSet.create(IN_KEYWORD))
        ), TokenSet.create(EOL_OR_SEMICOLON)
    )

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

    private fun parseExpression() {
        if (!atSet(EXPRESSION_FIRST)) {
            kotlin.error("Expecting an expression")
            return
        }
        parseBinaryExpression(Precedence.ASSIGNMENT)
    }

    private val ALLOW_NEWLINE_OPERATIONS = TokenSet.create(
        DOT, COLON, AS_KEYWORD, ANDAND, OROR
    )


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


        init {
            val operations: MutableSet<IElementType> = HashSet()
            val values: Array<Precedence> = Precedence.entries.toTypedArray()
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
