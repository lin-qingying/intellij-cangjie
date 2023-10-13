package com.huawei.cangjie.lang.core.psi


import com.huawei.cangjie.lang.CjLanguage
import com.huawei.cangjie.lang.core.parser.CangJieParserDefinition.Companion.BLOCK_COMMENT
import com.huawei.cangjie.lang.core.parser.CangJieParserDefinition.Companion.EOL_COMMENT
import com.huawei.cangjie.lang.core.psi.CjElementTypes.FUNCTION

import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet

//import com.huawei.cangjie.lang.core.stubs.CjFileStub

open class CjTokenType(debugName: String) : IElementType(debugName, CjLanguage)

fun tokenSetOf(vararg tokens: IElementType) = TokenSet.create(*tokens)

val CJ_REGULAR_COMMENTS = tokenSetOf(BLOCK_COMMENT, EOL_COMMENT)

val CJ_COMMENTS = TokenSet.orSet(CJ_REGULAR_COMMENTS)


val CJ_ITEMS = tokenSetOf(

    FUNCTION,

)

//
//fun tokenSetOf(vararg tokens: IElementType) = TokenSet.create(*tokens)
//
//val CJ_KEYWORDS = tokenSetOf(
//    AS, ASYNC, AUTO,
//    BOX, BREAK,
//    CONST, CONTINUE, CRATE, CSELF,
//    DEFAULT, DYN,
//    ELSE, ENUM, EXTERN,
//    FN, FOR,
//    IF, IMPL, IN,
//    MACRO_KW,
//    LET, LOOP,
//    MATCH, MOD, MOVE, MUT,
//    PUB,
//    RAW, REF, RETURN,
//    SELF, STATIC, STRUCT, SUPER,
//    TRAIT, TYPE_KW,
//    UNION, UNSAFE, USE,
//    WHERE, WHILE,
//    YIELD
//)
//
//val CJ_OPERATOCJ = tokenSetOf(
//    AND, ANDEQ, ARROW, FAT_ARROW, SHA, COLON, COLONCOLON, COMMA, DIV, DIVEQ, DOT, DOTDOT, DOTDOTDOT, DOTDOTEQ, EQ, EQEQ, EXCL,
//    EXCLEQ, GT, LT, MINUS, MINUSEQ, MUL, MULEQ, OR, OREQ, PLUS, PLUSEQ, REM, REMEQ, SEMICOLON, XOR, XOREQ, Q, AT,
//    DOLLAR, GTGTEQ, GTGT, GTEQ, LTLTEQ, LTLT, LTEQ, OROR, ANDAND
//)
//
//val CJ_BINARY_OPS = tokenSetOf(
//    AND, ANDEQ, ANDAND,
//    DIV, DIVEQ,
//    EQ, EQEQ, EXCLEQ,
//    GT, GTGT, GTEQ, GTGTEQ,
//    LT, LTLT, LTEQ, LTLTEQ,
//    MINUS, MINUSEQ, MUL, MULEQ,
//    OR, OREQ, OROR,
//    PLUS, PLUSEQ,
//    REM, REMEQ,
//    XOR, XOREQ
//)
//
//val CJ_INNER_DOC_COMMENTS = tokenSetOf(INNER_BLOCK_DOC_COMMENT, INNER_EOL_DOC_COMMENT)
//
//val CJ_OUTER_DOC_COMMENTS = tokenSetOf(OUTER_BLOCK_DOC_COMMENT, OUTER_EOL_DOC_COMMENT)
//
//val CJ_DOC_COMMENTS = TokenSet.orSet(CJ_INNER_DOC_COMMENTS, CJ_OUTER_DOC_COMMENTS)
//
//val CJ_REGULAR_COMMENTS = tokenSetOf(BLOCK_COMMENT, EOL_COMMENT)
//
//val CJ_COMMENTS = TokenSet.orSet(CJ_REGULAR_COMMENTS, CJ_DOC_COMMENTS)
//
//val CJ_BLOCK_COMMENTS = tokenSetOf(BLOCK_COMMENT, INNER_BLOCK_DOC_COMMENT, OUTER_BLOCK_DOC_COMMENT)
//
//val CJ_STRING_LITERALS = tokenSetOf(STRING_LITERAL, BYTE_STRING_LITERAL)
//
//val CJ_RAW_LITERALS = tokenSetOf(RAW_STRING_LITERAL, RAW_BYTE_STRING_LITERAL, RAW_CSTRING_LITERAL)
//
//val CJ_BYTE_STRING_LITERALS = tokenSetOf(BYTE_STRING_LITERAL, RAW_BYTE_STRING_LITERAL)
//
//val CJ_CSTRING_LITERALS = tokenSetOf(CSTRING_LITERAL, RAW_CSTRING_LITERAL)
//
//val CJ_ALL_STRING_LITERALS = tokenSetOf(
//    STRING_LITERAL, BYTE_STRING_LITERAL, CSTRING_LITERAL,
//    RAW_STRING_LITERAL, RAW_BYTE_STRING_LITERAL, RAW_CSTRING_LITERAL
//)
//
//val CJ_LITERALS = tokenSetOf(
//    STRING_LITERAL, BYTE_STRING_LITERAL, CSTRING_LITERAL,
//    RAW_STRING_LITERAL, RAW_BYTE_STRING_LITERAL, RAW_CSTRING_LITERAL,
//    CHAR_LITERAL, BYTE_LITERAL, INTEGER_LITERAL, FLOAT_LITERAL, BOOL_LITERAL
//)
//
//val CJ_CONTEXTUAL_KEYWORDS = tokenSetOf(DEFAULT, UNION, AUTO, DYN, RAW)
//val CJ_EDITION_2018_KEYWORDS = tokenSetOf(ASYNC, TRY)
//
//val CJ_LIST_OPEN_SYMBOLS = tokenSetOf(LPAREN, LT)
//val CJ_LIST_CLOSE_SYMBOLS = tokenSetOf(RPAREN, GT)
//
//val CJ_BLOCK_LIKE_EXPRESSIONS = tokenSetOf(WHILE_EXPR, IF_EXPR, FOR_EXPR, LOOP_EXPR, MATCH_EXPR, BLOCK_EXPR)
//
///** Successors of [com.huawei.cangjie.lang.core.psi.ext.CjItemElement] */
//val CJ_ITEMS = tokenSetOf(
//    CONSTANT,
//    ENUM_ITEM,
//    EXTERN_CRATE_ITEM,
//    FOREIGN_MOD_ITEM,
//    FUNCTION,
//    IMPL_ITEM,
//    MACRO_2,
//    MOD_DECL_ITEM,
//    MOD_ITEM,
//    STRUCT_ITEM,
//    TRAIT_ALIAS,
//    TRAIT_ITEM,
//    TYPE_ALIAS,
//    USE_ITEM
//)
//
///** Successors of [com.huawei.cangjie.lang.core.psi.CjTypeReference] */
//val CJ_TYPES = tokenSetOf(
//    ARRAY_TYPE,
//    REF_LIKE_TYPE,
//    FN_POINTER_TYPE,
//    TUPLE_TYPE,
//    PAREN_TYPE,
//    TRAIT_TYPE,
//    UNIT_TYPE,
//    NEVER_TYPE,
//    INFER_TYPE,
//    PATH_TYPE,
//    MACRO_TYPE,
//    FOR_IN_TYPE,
//)
//
//val CJ_MOD_OR_FILE = tokenSetOf(MOD_ITEM, CjFileStub.Type)
//
///**
// * Some tokens that treated as keywords by our lexer,
// * but cangjiec's macro parser treats them as identifiers
// */
//val CJ_IDENTIFIER_TOKENS = TokenSet.orSet(
//    tokenSetOf(IDENTIFIER, BOOL_LITERAL),
//    CJ_KEYWORDS
//)
