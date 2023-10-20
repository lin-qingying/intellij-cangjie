package com.huawei.cangjie.parsing;


import com.google.common.collect.ImmutableMap;
import com.huawei.cangjie.lexer.CjToken;
import com.huawei.cangjie.lexer.CjTokens;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static com.huawei.cangjie.lexer.CjTokens.*;
import static com.huawei.cangjie.parsing.CangJieParsing.PARAMETER_NAME_RECOVERY_SET;

public class CangJieExpressionParsing extends AbstractCangJieParsing {
    private static final TokenSet MATCH_CONDITION_RECOVERY_SET = TokenSet.create(RBRACE, IN_KEYWORD, IS_KEYWORD, ELSE_KEYWORD);
    private static final TokenSet MATCH_CONDITION_RECOVERY_SET_WITH_ARROW = TokenSet.create(RBRACE, IN_KEYWORD, IS_KEYWORD, ELSE_KEYWORD, ARROW, DOT);
    private static final ImmutableMap<String, CjToken> KEYWORD_TEXTS = tokenSetToMap(KEYWORDS);

    private static final TokenSet TOKEN_SET_TO_FOLLOW_AFTER_DESTRUCTURING_DECLARATION_IN_LAMBDA = TokenSet.create(ARROW, COMMA, COLON);
    private static final TokenSet TOKEN_SET_TO_FOLLOW_AFTER_DESTRUCTURING_DECLARATION_IN_LAMBDA_RECOVERY =
            TokenSet.orSet(TOKEN_SET_TO_FOLLOW_AFTER_DESTRUCTURING_DECLARATION_IN_LAMBDA, PARAMETER_NAME_RECOVERY_SET);
    private static final TokenSet EQ_RPAR_SET = TokenSet.create(EQ, RPAR);
    private static final TokenSet ARROW_SET = TokenSet.create(ARROW);
    private static final TokenSet ARROW_COMMA_SET = TokenSet.create(ARROW, COMMA);
    private static final TokenSet IN_KEYWORD_R_PAR_COLON_SET = TokenSet.create(IN_KEYWORD, RPAR, COLON);
    private static final TokenSet IN_KEYWORD_L_BRACE_SET = TokenSet.create(IN_KEYWORD, LBRACE);
    private static final TokenSet IN_KEYWORD_L_BRACE_RECOVERY_SET = TokenSet.orSet(IN_KEYWORD_L_BRACE_SET, PARAMETER_NAME_RECOVERY_SET);
    private static final TokenSet COLON_IN_KEYWORD_SET = TokenSet.create(COLON, IN_KEYWORD);
    private static final TokenSet L_PAR_L_BRACE_R_PAR_SET = TokenSet.create(LPAR, LBRACE, RPAR);
    private static final TokenSet IN_KEYWORD_SET = TokenSet.create(IN_KEYWORD);
    private static final TokenSet TRY_CATCH_RECOVERY_TOKEN_SET = TokenSet.create(LBRACE, RBRACE, FINALLY_KEYWORD, CATCH_KEYWORD);


    public CangJieExpressionParsing(SemanticWhitespaceAwarePsiBuilder builder, CangJieParsing CangJieParsing, boolean isLazy) {
        super(builder, isLazy);
        myCangJieParsing = CangJieParsing;
    }



    public void parseStatements() {
        parseStatements(false);
    }

    public void parseStatements(boolean isScriptTopLevel) {
        while (at(SEMICOLON)) advance(); // SEMICOLON
        while (!eof() && !at(RBRACE)) {
            if (!atSet(STATEMENT_FIRST)) {
                errorAndAdvance("Expecting an element");
            }

            if (at(SEMICOLON)) {
                while (at(SEMICOLON)) advance(); // SEMICOLON
            }
            else if (at(RBRACE)) {
                break;
            }
            else if (!isScriptTopLevel && !myBuilder.newlineBeforeCurrentToken()) {
                String severalStatementsError = "Unexpected tokens (use ';' to separate expressions on the same line)";

                if (atSet(STATEMENT_NEW_LINE_QUICK_RECOVERY_SET)) {
                    error(severalStatementsError);
                }
                else {
                    errorUntil(severalStatementsError, TokenSet.create(EOL_OR_SEMICOLON, LBRACE, RBRACE));
                }
            }
        }
    }



    protected boolean parseCallWithClosure() {
        boolean success = false;

        return success;
    }

    private final CangJieParsing myCangJieParsing;
    public static final TokenSet ALL_OPERATIONS;

    static {
        Set<IElementType> operations = new HashSet<>();
        Precedence[] values = Precedence.values();
        for (Precedence precedence : values) {
            operations.addAll(Arrays.asList(precedence.getOperations().getTypes()));
        }
        ALL_OPERATIONS = TokenSet.create(operations.toArray(new IElementType[0]));
    }

    public enum Precedence {
        POSTFIX(PLUSPLUS, MINUSMINUS,
                DOT),

        PREFIX(MINUS, PLUS, MINUSMINUS, PLUSPLUS, EXCL) { // annotations


        },

        MULTIPLICATIVE(MUL, DIV, PERC),
        ADDITIVE(PLUS, MINUS),
        RANGE(CjTokens.RANGE),
        SIMPLE_NAME(IDENTIFIER),

        IN_OR_IS(IN_KEYWORD, IS_KEYWORD) {

        },
        COMPARISON(LT, GT, LTEQ, GTEQ),
        EQUALITY(EQEQ, EXCLEQ),
        CONJUNCTION(ANDAND),
        DISJUNCTION(OROR),

        ASSIGNMENT(EQ, PLUSEQ, MINUSEQ, MULTEQ, DIVEQ, PERCEQ),
        ;

        static {
            Precedence[] values = values();
            for (Precedence precedence : values) {
                int ordinal = precedence.ordinal();
                precedence.higher = ordinal > 0 ? values[ordinal - 1] : null;
            }
        }

        private Precedence higher;
        private final TokenSet operations;

        Precedence(IElementType... operations) {
            this.operations = TokenSet.create(operations);
        }




        @NotNull
        public final TokenSet getOperations() {
            return operations;
        }
    }

    private static ImmutableMap<String, CjToken> tokenSetToMap(TokenSet tokens) {
        ImmutableMap.Builder<String, CjToken> builder = ImmutableMap.builder();
        for (IElementType token : tokens.getTypes()) {
            builder.put(token.toString(), (CjToken) token);
        }
        return builder.build();
    }

    private static final TokenSet TYPE_ARGUMENT_LIST_STOPPERS = TokenSet.create(
            INTEGER_LITERAL, FLOAT_LITERAL, CHARACTER_LITERAL, OPEN_QUOTE,
            PACKAGE_KEYWORD, AS_KEYWORD, INTERFACE_KEYWORD, CLASS_KEYWORD, THIS_KEYWORD, LET_KEYWORD, VAR_KEYWORD,
            FUNC_KEYWORD, FOR_KEYWORD,
            TRUE_KEYWORD, FALSE_KEYWORD, IS_KEYWORD, THROW_KEYWORD, RETURN_KEYWORD, BREAK_KEYWORD,
            CONTINUE_KEYWORD, IF_KEYWORD, TRY_KEYWORD, ELSE_KEYWORD, WHILE_KEYWORD, DO_KEYWORD,
            MATCH_KEYWORD, RBRACKET, RBRACE, RPAR, PLUSPLUS, MINUSMINUS,

            PLUS, MINUS, EXCL, DIV, PERC, LTEQ,

             EQEQ, EXCLEQ, ANDAND, OROR,
            SEMICOLON, RANGE, EQ, MULTEQ, DIVEQ, PERCEQ, PLUSEQ, MINUSEQ,

            COLON
    );

    static final TokenSet EXPRESSION_FIRST = TokenSet.create(

            MINUS, PLUS, MINUSMINUS, PLUSPLUS,
            EXCL,




            LPAR,


            TRUE_KEYWORD, FALSE_KEYWORD,
            OPEN_QUOTE,
            INTEGER_LITERAL, CHARACTER_LITERAL, FLOAT_LITERAL,


            LBRACE, // functionLiteral
            FUNC_KEYWORD, // expression function

            THIS_KEYWORD, // this
            SUPER_KEYWORD, // super

            IF_KEYWORD, // if
            MATCH_KEYWORD, // match
            TRY_KEYWORD, // try


            // jump
            THROW_KEYWORD,
            RETURN_KEYWORD,
            CONTINUE_KEYWORD,
            BREAK_KEYWORD,

            // loop
            FOR_KEYWORD,
            WHILE_KEYWORD,
            DO_KEYWORD,

            IDENTIFIER, // SimpleName



            LBRACKET // Collection literal expression
    );

    @SuppressWarnings("WeakerAccess")
    public static final TokenSet STATEMENT_FIRST = TokenSet.orSet(
            EXPRESSION_FIRST,
            TokenSet.create(
                    // declaration
                    FUNC_KEYWORD,
                    LET_KEYWORD, VAR_KEYWORD,
                    INTERFACE_KEYWORD,
                    CLASS_KEYWORD

            ),
            MODIFIER_KEYWORDS
    );

    private static final TokenSet STATEMENT_NEW_LINE_QUICK_RECOVERY_SET =
            TokenSet.orSet(
                    TokenSet.andSet(STATEMENT_FIRST, TokenSet.andNot(KEYWORDS, TokenSet.create(IN_KEYWORD))),
                    TokenSet.create(EOL_OR_SEMICOLON));

     static final TokenSet EXPRESSION_FOLLOW = TokenSet.create(
            EOL_OR_SEMICOLON, ARROW, COMMA, RBRACE, RPAR, RBRACKET
    );

    @Override
    protected CangJieParsing create(SemanticWhitespaceAwarePsiBuilder builder) {
        return null;
    }
}
