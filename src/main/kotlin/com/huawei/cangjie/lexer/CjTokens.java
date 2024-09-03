package com.huawei.cangjie.lexer;

import com.huawei.cangjie.CjNodeTypes;
import com.huawei.cangjie.doc.lexer.CDocTokens;
import com.intellij.lang.BracePair;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;

public interface CjTokens {
    int INVALID_Id = 0;
    int EOF_Id = 1;
    int UNIT_LTIERAL_Id = 2;
    int BLOCK_COMMENT_Id = 3;
    int EOL_COMMENT_Id = 4;
    int SHEBANG_COMMENT_Id = 5;
    int INTEGER_LITERAL_Id = 6;
    int FLOAT_LITERAL_Id = 7;
    int RUNE_LITERAL_Id = 8;
    int CLOSING_QUOTE_Id = 9;
    int OPEN_QUOTE_Id = 10;
    int REGULAR_STRING_PART_Id = 11;
    int ESCAPE_SEQUENCE_Id = 12;
    int SHORT_TEMPLATE_ENTRY_START_Id = 13;
    int LONG_TEMPLATE_ENTRY_START_Id = 14;
    int LONG_TEMPLATE_ENTRY_END_Id = 15;
    int DANGLING_NEWLINE_Id = 16;
    int PACKAGE_KEYWORD_Id = 17;
    int AS_KEYWORD_Id = 18;
    int SHOP_STRING_Id = 19;
    int TUPLE_LTIERAL_Id = 19;
    int CLASS_KEYWORD_Id = 20;
    int THIS_KEYWORD_Id = 21;
    int SUPER_KEYWORD_Id = 22;
    int LET_KEYWORD_Id = 23;
    int VAR_KEYWORD_Id = 24;
    int FUNC_KEYWORD_Id = 25;
    int FOR_KEYWORD_Id = 26;

    int EXTEND_KEYWORD_Id = 27;

    int TRUE_KEYWORD_Id = 28;
    int FALSE_KEYWORD_Id = 29;
    int IS_KEYWORD_Id = 30;
    int IN_KEYWORD_Id = 31;
    int THROW_KEYWORD_Id = 32;
    int RETURN_KEYWORD_Id = 33;
    int BREAK_KEYWORD_Id = 34;
    int CONTINUE_KEYWORD_Id = 35;
    int CONST_KEYWORD_Id = 36;

    int IF_KEYWORD_Id = 37;
    int TRY_KEYWORD_Id = 38;
    int ELSE_KEYWORD_Id = 39;
    int WHILE_KEYWORD_Id = 40;
    int DO_KEYWORD_Id = 41;
    int MATCH_KEYWORD_Id = 42;
    int INTERFACE_KEYWORD_Id = 43;
    int TYPEOF_KEYWORD_Id = 44;
    int IMPORT_KEYWORD_Id = 45;
    int IDENTIFIER_Id = 46;
    int FIELD_IDENTIFIER_Id = 47;
    int LBRACKET_Id = 48;
    int RBRACKET_Id = 49;
    int LBRACE_Id = 50;
    int RBRACE_Id = 51;
    int LPAR_Id = 52;
    int RPAR_Id = 53;
    int DOT_Id = 54;
    int PLUSPLUS_Id = 55;
    int MINUSMINUS_Id = 56;
    int MUL_Id = 57;
    int PLUS_Id = 58;
    int MINUS_Id = 59;
    int EXCL_Id = 60;
    int DIV_Id = 61;
    int PERC_Id = 62;
    int LT_Id = 63;
    int GT_Id = 64;
    int LTEQ_Id = 65;
    int GTEQ_Id = 66;
    int EQEQEQ_Id = 67;
    int ARROW_Id = 68;
    int DOUBLE_ARROW_Id = 69;
    int CASE_KEYWORD_Id = 70;
    int EQEQ_Id = 71;
    int EXCLEQ_Id = 72;

    int LEFT_ARROW_Id = 73;
    int ANDAND_Id = 74;
    int AND_Id = 75;
    int OROR_Id = 76;
    int XOR_Id = 77;
    int OR_Id = 78;

    int QUEST_Id = 79;

    int CHARACTER_BYTE_LITERAL_Id = 80;

    int COLON_Id = 81;
    int SEMICOLON_Id = 82;
    int DOUBLE_SEMICOLON_Id = 83;
    int RANGE_Id = 84;

    int RANGEEQ_Id = 85;


    int EQ_Id = 86;
    int MULTEQ_Id = 87;
    int DIVEQ_Id = 88;
    int PERCEQ_Id = 89;
    int PLUSEQ_Id = 90;
    int MINUSEQ_Id = 91;
    int TILED_Id = 92;
    int ANDANDEQ_Id = 93;
    int AT_Id = 95;
    int COMMA_Id = 96;
    int EOL_OR_SEMICOLON_Id = 97;
    int ELVIS_Id = 98;
    int PIPELINE_Id = 99;
    int WHERE_KEYWORD_Id = 106;
    //    int BY_KEYWORD_Id = 107;
    int GET_KEYWORD_Id = 108;
    int SET_KEYWORD_Id = 109;

    int INIT_KEYWORD_Id = 111;
    int SEALED_KEYWORD_Id = 112;
    int ABSTRACT_KEYWORD_Id = 113;
    int ENUM_KEYWORD_Id = 114;
    int SPAWN_KEYWORD_Id = 115;
    int OPEN_KEYWORD_Id = 116;
    int SYNCHRONIZED_KEYWORD_Id = 130;
    int STRUCT_KEYWORD_Id = 117;

    int OVERRIDE_KEYWORD_Id = 118;
    int PRIVATE_KEYWORD_Id = 119;
    int PUBLIC_KEYWORD_Id = 120;
    int STATIC_KEYWORD_Id = 121;

    int PROTECTED_KEYWORD_Id = 122;
    int CATCH_KEYWORD_Id = 123;


    int TYPE_KEYWORD_Id = 125;

    int COMPOSITION_Id = 126;

    int SAFE_ACCESS_Id = 127;
    int SAFE_INDEXEX_Id = 128;
    int SAFE_CALL_Id = 129;
    int SAFE_LAMBDA_Id = 142;

    int FINALLY_KEYWORD_Id = 130;
    int REDEF_KEYWORD_Id = 131;
    int FOREIGN_KEYWORD_Id = 132;
    int UNSAFE_KEYWORD_Id = 133;
    int OPERATOR_KEYWORD_Id = 141;
    int INTERNAL_KEYWORD_Id = 142;
    int QUOTE_KEYWORD_Id = 143;
    int HASH_Id = 94;
    int MAIN_KEYWORD_Id = 149;

    int INT8_Id = 150;
    int INT16_Id = 151;
    int INT32_Id = 152;
    int INT64_Id = 153;
    int UINT8_Id = 154;
    int UINT16_Id = 155;
    int UINT32_Id = 156;
    int UINT64_Id = 157;
    int FLOAT32_Id = 158;
    int FLOAT64_Id = 159;
    int BOOL_Id = 160;
    int CHAR_Id = 161;
    int UNIT_Id = 162;

    int LTCOLON_Id = 163;


    int MUT_KEYWORD_Id = 165;
    int PROP_KEYWORD_Id = 166;

    int UNDERLINE_Id = 167;

    int OREQ_Id = 168;
    int ANDEQ_Id = 169;
    int XOREQ_Id = 170;
    int LTLT_Id = 171;
    int GTGT_Id = 172;
    int LTLTEQ_Id = 173;
    int GTGTEQ_Id = 174;

    int MULMUL_Id = 175;
    int MUL_MUL_EQ_Id = 176;
    int OROREQ_Id = 177;
    int ELLIPSIS_Id = 178;
    int BACKSLASH_Id = 179;
    int QUOTESYMBOL_Id = 180;
    int DOLLAR_Id = 181;
    int FLOAT16_Id = 182;
    int INTNATIVE_Id = 183;
    int UINTNATIVE_Id = 184;
    int NOTHING_Id = 185;

    int MACRO_KEYWORD_Id = 186;


    int ESCAPE_LPAR_Id = 187;
    int ESCAPE_RPAR_Id = 187;
    int ESCAPE_DOLLAR_Id = 187;
    int ESCAPE_LBRACKET_Id = 188;
    int ESCAPE_RBRACKET_Id = 189;

    int VARRAY_Id = 190;


    CjSingleValueToken HASH = new CjSingleValueToken("HASH", "#", HASH_Id);
    CjSingleValueToken QUOTESYMBOL = new CjSingleValueToken("QUOTESYMBOL", "`", QUOTESYMBOL_Id);
    CjSingleValueToken DOLLAR = new CjSingleValueToken("DOLLAR", "$", DOLLAR_Id);

    CjToken EOF = new CjToken("EOF", EOF_Id);


    CjToken BLOCK_COMMENT = new CjToken("BLOCK_COMMENT", BLOCK_COMMENT_Id);
    CjToken EOL_COMMENT = new CjToken("EOL_COMMENT", EOL_COMMENT_Id);
    CjToken SHEBANG_COMMENT = new CjToken("SHEBANG_COMMENT", SHEBANG_COMMENT_Id);

    CjToken INTEGER_LITERAL = new CjToken("INTEGER_LITERAL", INTEGER_LITERAL_Id);
    CjToken FLOAT_LITERAL = new CjToken("FLOAT_CONSTANT", FLOAT_LITERAL_Id);
    CjToken UNIT_LTIERAL = new CjToken("UNIT_LTIERAL", UNIT_LTIERAL_Id);
    CjToken TUPLE_LTIERAL = new CjToken("TUPLE_LTIERAL", TUPLE_LTIERAL_Id);
    CjToken RUNE_LITERAL = new CjToken("RUNE_LITERAL", RUNE_LITERAL_Id);
    CjToken CHARACTER_BYTE_LITERAL = new CjToken("CHARACTER_BYTE_LITERAL", CHARACTER_BYTE_LITERAL_Id);
    CjToken SHOP_STRING = new CjToken("SHOP_STRING", SHOP_STRING_Id);
    CjToken CLOSING_QUOTE = new CjToken("CLOSING_QUOTE", CLOSING_QUOTE_Id);
    CjToken OPEN_QUOTE = new CjToken("OPEN_QUOTE", OPEN_QUOTE_Id);
    CjToken REGULAR_STRING_PART = new CjToken("REGULAR_STRING_PART", REGULAR_STRING_PART_Id);
    CjToken ESCAPE_SEQUENCE = new CjToken("ESCAPE_SEQUENCE", ESCAPE_SEQUENCE_Id);
    CjToken SHORT_TEMPLATE_ENTRY_START = new CjToken("SHORT_TEMPLATE_ENTRY_START", SHORT_TEMPLATE_ENTRY_START_Id);
    CjToken LONG_TEMPLATE_ENTRY_START = new CjToken("LONG_TEMPLATE_ENTRY_START", LONG_TEMPLATE_ENTRY_START_Id);
    CjToken LONG_TEMPLATE_ENTRY_END = new CjToken("LONG_TEMPLATE_ENTRY_END", LONG_TEMPLATE_ENTRY_END_Id);
    CjToken DANGLING_NEWLINE = new CjToken("DANGLING_NEWLINE", DANGLING_NEWLINE_Id);
    CjKeywordToken PACKAGE_KEYWORD = CjKeywordToken.keyword("package", PACKAGE_KEYWORD_Id);
    CjKeywordToken AS_KEYWORD = CjKeywordToken.keyword("as", AS_KEYWORD_Id);
    CjKeywordToken CLASS_KEYWORD = CjKeywordToken.keyword("class", CLASS_KEYWORD_Id);
    CjKeywordToken EXTEND_KEYWORD = CjKeywordToken.keyword("extend", EXTEND_KEYWORD_Id);
    CjKeywordToken ENUM_KEYWORD = CjKeywordToken.keyword("enum", ENUM_KEYWORD_Id);
    CjKeywordToken STRUCT_KEYWORD = CjKeywordToken.keyword("struct", STRUCT_KEYWORD_Id);
    CjKeywordToken THIS_KEYWORD = CjKeywordToken.keyword("this", THIS_KEYWORD_Id);
    CjKeywordToken SUPER_KEYWORD = CjKeywordToken.keyword("super", SUPER_KEYWORD_Id);
    CjKeywordToken LET_KEYWORD = CjKeywordToken.keyword("let", LET_KEYWORD_Id);
    CjKeywordToken VAR_KEYWORD = CjKeywordToken.keyword("var", VAR_KEYWORD_Id);
    CjKeywordToken CONST_KEYWORD = CjKeywordToken.keyword("const", CONST_KEYWORD_Id);
    CjKeywordToken SPAWN_KEYWORD = CjKeywordToken.keyword("spawn", SPAWN_KEYWORD_Id);
    CjKeywordToken SYNCHRONIZED_KEYWORD = CjKeywordToken.keyword("synchronized", SYNCHRONIZED_KEYWORD_Id);
    CjKeywordToken MAIN_KEYWORD = CjKeywordToken.keyword("main", MAIN_KEYWORD_Id);
    CjKeywordToken FUNC_KEYWORD = CjKeywordToken.keyword("func", FUNC_KEYWORD_Id);
    CjKeywordToken FOR_KEYWORD = CjKeywordToken.keyword("for", FOR_KEYWORD_Id);
    CjKeywordToken TRUE_KEYWORD = CjKeywordToken.keyword("true", TRUE_KEYWORD_Id);
    CjKeywordToken FALSE_KEYWORD = CjKeywordToken.keyword("false", FALSE_KEYWORD_Id);
    CjKeywordToken IS_KEYWORD = CjKeywordToken.keyword("is", IS_KEYWORD_Id);
    CjModifierKeywordToken IN_KEYWORD = CjModifierKeywordToken.keywordModifier("in", IN_KEYWORD_Id);
    CjKeywordToken THROW_KEYWORD = CjKeywordToken.keyword("throw", THROW_KEYWORD_Id);
    CjKeywordToken RETURN_KEYWORD = CjKeywordToken.keyword("return", RETURN_KEYWORD_Id);
    CjKeywordToken BREAK_KEYWORD = CjKeywordToken.keyword("break", BREAK_KEYWORD_Id);
    CjKeywordToken CONTINUE_KEYWORD = CjKeywordToken.keyword("continue", CONTINUE_KEYWORD_Id);
    CjKeywordToken IF_KEYWORD = CjKeywordToken.keyword("if", IF_KEYWORD_Id);
    CjKeywordToken TRY_KEYWORD = CjKeywordToken.keyword("try", TRY_KEYWORD_Id);
    CjKeywordToken ELSE_KEYWORD = CjKeywordToken.keyword("else", ELSE_KEYWORD_Id);
    CjKeywordToken WHILE_KEYWORD = CjKeywordToken.keyword("while", WHILE_KEYWORD_Id);
    CjKeywordToken DO_KEYWORD = CjKeywordToken.keyword("do", DO_KEYWORD_Id);
    CjKeywordToken MATCH_KEYWORD = CjKeywordToken.keyword("match", MATCH_KEYWORD_Id);
    CjKeywordToken CASE_KEYWORD = CjKeywordToken.keyword("case", CASE_KEYWORD_Id);
    CjKeywordToken INTERFACE_KEYWORD = CjKeywordToken.keyword("interface", INTERFACE_KEYWORD_Id);
    //    CjToken AS_SAFE = CjKeywordToken.keyword("AS_SAFE", IMPORT_KEYWORD_Id);
    CjSingleValueToken UNDERLINE = new CjSingleValueToken("UNDERLINE", "_", UNDERLINE_Id);
    CjKeywordToken TYPEOF_KEYWORD = CjKeywordToken.keyword("typeof", TYPEOF_KEYWORD_Id);
    CjToken IDENTIFIER = new CjToken("IDENTIFIER", IDENTIFIER_Id);
    CjToken FIELD_IDENTIFIER = new CjToken("FIELD_IDENTIFIER", FIELD_IDENTIFIER_Id);
    CjSingleValueToken LBRACKET = new CjSingleValueToken("LBRACKET", "[", LBRACKET_Id);
    CjSingleValueToken RBRACKET = new CjSingleValueToken("RBRACKET", "]", RBRACKET_Id);
    CjSingleValueToken LBRACE = new CjSingleValueToken("LBRACE", "{", LBRACE_Id);
    CjSingleValueToken RBRACE = new CjSingleValueToken("RBRACE", "}", RBRACE_Id);
    CjSingleValueToken LPAR = new CjSingleValueToken("LPAR", "(", LPAR_Id);
    CjSingleValueToken RPAR = new CjSingleValueToken("RPAR", ")", RPAR_Id);
    CjSingleValueToken ESCAPE_LPAR = new CjSingleValueToken("ESCAPE_LPAR", "\\(", ESCAPE_LPAR_Id);
    CjSingleValueToken ESCAPE_RPAR = new CjSingleValueToken("ESCAPE_RPAR", "\\)", ESCAPE_RPAR_Id);
    CjSingleValueToken ESCAPE_DOLLAR = new CjSingleValueToken("ESCAPE_DOLLAR", "\\$", ESCAPE_DOLLAR_Id);
    CjSingleValueToken ESCAPE_LBRACKET = new CjSingleValueToken("ESCAPE_LBRACKET", "\\]", ESCAPE_LBRACKET_Id);
    CjSingleValueToken ESCAPE_RBRACKET = new CjSingleValueToken("ESCAPE_RBRACKET", "\\[", ESCAPE_RBRACKET_Id);
    CjSingleValueToken DOT = new CjSingleValueToken("DOT", ".", DOT_Id);
    CjSingleValueToken PLUSPLUS = new CjSingleValueToken("PLUSPLUS", "++", PLUSPLUS_Id);
    CjSingleValueToken MINUSMINUS = new CjSingleValueToken("MINUSMINUS", "--", MINUSMINUS_Id);
    CjSingleValueToken MUL = new CjSingleValueToken("MUL", "*", MUL_Id);
    CjSingleValueToken MULMUL = new CjSingleValueToken("MULMUL", "**", MULMUL_Id);
    CjSingleValueToken MULMULEQ = new CjSingleValueToken("MULMULEQ", "**=", MUL_MUL_EQ_Id);
    CjSingleValueToken PLUS = new CjSingleValueToken("PLUS", "+", PLUS_Id);
    CjSingleValueToken MINUS = new CjSingleValueToken("MINUS", "-", MINUS_Id);
    CjSingleValueToken EXCL = new CjSingleValueToken("EXCL", "!", EXCL_Id);
    CjSingleValueToken DIV = new CjSingleValueToken("DIV", "/", DIV_Id);
    CjSingleValueToken PERC = new CjSingleValueToken("PERC", "%", PERC_Id);
    CjSingleValueToken LT = new CjSingleValueToken("LT", "<", LT_Id);
    CjSingleValueToken GT = new CjSingleValueToken("GT", ">", GT_Id);
    BracePair[] BRACE_PAIRS = new BracePair[]{
            new BracePair(LPAR, RPAR, true),
            new BracePair(LBRACE, RBRACE, true),
            new BracePair(LBRACKET, RBRACKET, true),
            new BracePair(LT, GT, true),

            new BracePair(LONG_TEMPLATE_ENTRY_START, LONG_TEMPLATE_ENTRY_END, false),

    };
    CjSingleValueToken LTEQ = new CjSingleValueToken("LTEQ", "<=", LTEQ_Id);
    CjSingleValueToken AT = new CjSingleValueToken("AT", "@", AT_Id);
    CjSingleValueToken GTEQ = new CjSingleValueToken("GT_EQ", ">=", GTEQ_Id);
    CjSingleValueToken LTCOLON = new CjSingleValueToken("LT_COLON", "<:", LTCOLON_Id);
    CjSingleValueToken ARROW = new CjSingleValueToken("ARROW", "->", ARROW_Id);
    CjSingleValueToken COMPOSITION = new CjSingleValueToken("COMPOSITION", "~>", COMPOSITION_Id);
    //反向箭头composition
    CjSingleValueToken SAFE_ACCESS = new CjSingleValueToken("SAFE_ACCESS", "?.", SAFE_ACCESS_Id);
    CjSingleValueToken SAFE_INDEXEX = new CjSingleValueToken("SAFE_INDEXEX", "?[", SAFE_INDEXEX_Id);
    CjSingleValueToken SAFE_CALL = new CjSingleValueToken("SAFE_CALL", "?(", SAFE_CALL_Id);
    CjSingleValueToken SAFE_LAMBDA = new CjSingleValueToken("SAFE_LAMBDA", "?{", SAFE_LAMBDA_Id);
    CjSingleValueToken BACKSLASH = new CjSingleValueToken("BACKSLASH", "\\", BACKSLASH_Id);
    CjSingleValueToken LEFT_ARROW = new CjSingleValueToken("LEFT_ARROW", "<-", LEFT_ARROW_Id);
    CjSingleValueToken DOUBLE_ARROW = new CjSingleValueToken("DOUBLE_ARROW", "=>", DOUBLE_ARROW_Id);
    CjSingleValueToken EQEQ = new CjSingleValueToken("EQEQ", "==", EQEQ_Id);
    CjSingleValueToken EXCLEQ = new CjSingleValueToken("EXCLEQ", "!=", EXCLEQ_Id);
    CjSingleValueToken ANDAND = new CjSingleValueToken("ANDAND", "&&", ANDAND_Id);
    CjSingleValueToken AND = new CjSingleValueToken("AND", "&", AND_Id);
    CjSingleValueToken TILDE = new CjSingleValueToken("TILDE", "~", TILED_Id);
    CjSingleValueToken PIPELINE = new CjSingleValueToken("PIPELINE", "|>", PIPELINE_Id);
    CjSingleValueToken XOR = new CjSingleValueToken("XOR", "^", XOR_Id);
    CjSingleValueToken OROR = new CjSingleValueToken("OROR", "||", OROR_Id);
    CjSingleValueToken OR = new CjSingleValueToken("OR", "|", OR_Id);
    CjSingleValueToken QUEST = new CjSingleValueToken("QUEST", "?", QUEST_Id);

    CjSingleValueToken ELVIS = new CjSingleValueToken("ELVIS", "??", ELVIS_Id);
    CjSingleValueToken COLON = new CjSingleValueToken("COLON", ":", COLON_Id);
    CjSingleValueToken SEMICOLON = new CjSingleValueToken("SEMICOLON", ";", SEMICOLON_Id);
    CjSingleValueToken DOUBLE_SEMICOLON = new CjSingleValueToken("DOUBLE_SEMICOLON", ";;", DOUBLE_SEMICOLON_Id);
    CjSingleValueToken RANGE = new CjSingleValueToken("RANGE", "..", RANGE_Id);
    CjSingleValueToken ELLIPSIS = new CjSingleValueToken("ELLIPSIS", "...", ELLIPSIS_Id);
    CjSingleValueToken RANGEEQ = new CjSingleValueToken("RANGEEQ", "..=", RANGEEQ_Id);
    CjSingleValueToken EQ = new CjSingleValueToken("EQ", "=", EQ_Id);
    CjSingleValueToken MULTEQ = new CjSingleValueToken("MULTEQ", "*=", MULTEQ_Id);
    CjSingleValueToken DIVEQ = new CjSingleValueToken("DIVEQ", "/=", DIVEQ_Id);
    CjSingleValueToken PERCEQ = new CjSingleValueToken("PERCEQ", "%=", PERCEQ_Id);
    CjSingleValueToken PLUSEQ = new CjSingleValueToken("PLUSEQ", "+=", PLUSEQ_Id);
    CjSingleValueToken MINUSEQ = new CjSingleValueToken("MINUSEQ", "-=", MINUSEQ_Id);
    TokenSet ALL_ASSIGNMENTS = TokenSet.create(EQ, PLUSEQ, MINUSEQ, MULTEQ, PERCEQ, DIVEQ);
    CjSingleValueToken ANDANDEQ = new CjSingleValueToken("ANDANDEQ", "&&=", ANDANDEQ_Id);
    CjSingleValueToken OREQ = new CjSingleValueToken("OREQ", "|=", OREQ_Id);
    CjSingleValueToken OROREQ = new CjSingleValueToken("OROREQ", "||=", OROREQ_Id);
    CjSingleValueToken ANDEQ = new CjSingleValueToken("ANDEQ", "&=", ANDEQ_Id);
    CjSingleValueToken XOREQ = new CjSingleValueToken("XOREQ", "^=", XOREQ_Id);
    //    CjKeywordToken FILE_KEYWORD = CjKeywordToken.softKeyword("file", FILE_KEYWORD_Id);
    CjSingleValueToken LTLT = new CjSingleValueToken("LTLT", "<<", LTLT_Id);
    CjSingleValueToken GTGT = new CjSingleValueToken("GTGT", ">>", GTGT_Id);
    //可以被重载的运算符
    TokenSet OPERATIONS_CAN_BE_OVERLOADED = TokenSet.orSet(


            TokenSet.create(
                    LBRACKET, RBRACKET
            ),
            TokenSet.create(
                    LPAR, RPAR
            ),

            TokenSet.create(


                    OR,
                    MULMUL,
                    MUL,
                    DIV,
                    PERC,

                    PLUS,
                    MINUS,
                    GT,
                    GTGT,
                    LT,
                    LTLT,
                    GTEQ,
                    LTEQ,
                    EQEQ,
                    EXCLEQ,
                    AND,
                    OROR,
                    XOR,
                    ANDAND,
                    RANGE,
                    OREQ,
                    EXCL

            )
    );
    IElementType DOC_COMMENT = CDocTokens.CDOC;
    IElementType WHITE_SPACE = TokenType.WHITE_SPACE;
    TokenSet WHITESPACES = TokenSet.create(TokenType.WHITE_SPACE);
    CjSingleValueToken LTLTEQ = new CjSingleValueToken("LTLTEQ", "<<=", LTLTEQ_Id);
    CjSingleValueToken GTGTEQ = new CjSingleValueToken("GTGTEQ", ">>=", GTGTEQ_Id);
    TokenSet OPERATIONS = TokenSet.create(AS_KEYWORD, IS_KEYWORD, DOT, PLUSPLUS, MINUSMINUS, MUL, MULMUL, PLUS,
            MINUS, EXCL, DIV, PERC, LT, GT, LTEQ, GTEQ, EQEQ, EXCLEQ, ANDAND, OROR, MULMULEQ,

            RANGE, RANGEEQ, EQ, MULTEQ, DIVEQ, PERCEQ, PLUSEQ, MINUSEQ,
            ELVIS, SAFE_ACCESS,
            AND, OR, XOR,
            ANDEQ, OREQ, XOREQ, ANDANDEQ,
            LTLT, GTGT, LTLTEQ, GTGTEQ,

            COMPOSITION,PIPELINE
    );
    CjSingleValueToken COMMA = new CjSingleValueToken("COMMA", ",", COMMA_Id);
    CjToken EOL_OR_SEMICOLON = new CjToken("EOL_OR_SEMICOLON", EOL_OR_SEMICOLON_Id);
    CjKeywordToken IMPORT_KEYWORD = CjKeywordToken.keyword("import", IMPORT_KEYWORD_Id);
    CjKeywordToken WHERE_KEYWORD = CjKeywordToken.keyword("where", WHERE_KEYWORD_Id);
    //    CjKeywordToken BY_KEYWORD = CjKeywordToken.softKeyword("by", BY_KEYWORD_Id);
    CjKeywordToken GET_KEYWORD = CjKeywordToken.softKeyword("get", GET_KEYWORD_Id);
    CjKeywordToken SET_KEYWORD = CjKeywordToken.softKeyword("set", SET_KEYWORD_Id);
    CjKeywordToken INIT_KEYWORD = CjKeywordToken.keyword("init", INIT_KEYWORD_Id);
    CjKeywordToken PROP_KEYWORD = CjKeywordToken.keyword("prop", PROP_KEYWORD_Id);
    CjKeywordToken INT8_KEYWORD = CjKeywordToken.keyword("Int8", INT8_Id);
    CjKeywordToken INT16_KEYWORD = CjKeywordToken.keyword("Int16", INT16_Id);
    CjKeywordToken INT32_KEYWORD = CjKeywordToken.keyword("Int32", INT32_Id);
    CjKeywordToken INT64_KEYWORD = CjKeywordToken.keyword("Int64", INT64_Id);
    CjKeywordToken UINT8_KEYWORD = CjKeywordToken.keyword("UInt8", UINT8_Id);
    CjKeywordToken UINT16_KEYWORD = CjKeywordToken.keyword("UInt16", UINT16_Id);
    CjKeywordToken UINT32_KEYWORD = CjKeywordToken.keyword("UInt32", UINT32_Id);
    CjKeywordToken UINT64_KEYWORD = CjKeywordToken.keyword("UInt64", UINT64_Id);
    CjKeywordToken FLOAT32_KEYWORD = CjKeywordToken.keyword("Float32", FLOAT32_Id);
    CjKeywordToken FLOAT64_KEYWORD = CjKeywordToken.keyword("Float64", FLOAT64_Id);
    CjKeywordToken FLOAT16_KEYWORD = CjKeywordToken.keyword("Float16", FLOAT16_Id);
    CjKeywordToken INTNATIVE_KEYWORD = CjKeywordToken.keyword("IntNative", INTNATIVE_Id);
    CjKeywordToken UINTNATIVE_KEYWORD = CjKeywordToken.keyword("UIntNative", UINTNATIVE_Id);
    CjKeywordToken NOTHING_KEYWORD = CjKeywordToken.keyword("Nothing", NOTHING_Id);
    CjKeywordToken BOOL_KEYWORD = CjKeywordToken.keyword("Bool", BOOL_Id);
    CjKeywordToken RUNE_KEYWORD = CjKeywordToken.keyword("Rune", CHAR_Id);
    CjKeywordToken UNIT_KEYWORD = CjKeywordToken.keyword("Unit", UNIT_Id);
    CjKeywordToken VARRAY_KEYWORD = CjKeywordToken.keyword("VArray", VARRAY_Id);
    TokenSet STRINGS = TokenSet.create(RUNE_LITERAL, REGULAR_STRING_PART);

    //基本类型
    TokenSet BASICTYPES = TokenSet.create(
            INTNATIVE_KEYWORD,
            INT8_KEYWORD,
            INT16_KEYWORD,
            INT32_KEYWORD,
            INT64_KEYWORD,
            UINTNATIVE_KEYWORD,
            UINT8_KEYWORD,
            UINT16_KEYWORD,
            UINT32_KEYWORD,
            UINT64_KEYWORD,
            FLOAT16_KEYWORD,
            FLOAT32_KEYWORD,
            FLOAT64_KEYWORD,
            NOTHING_KEYWORD,

            VARRAY_KEYWORD,

            BOOL_KEYWORD,
            RUNE_KEYWORD,
            UNIT_KEYWORD
    );

    CjModifierKeywordToken SEALED_KEYWORD = CjModifierKeywordToken.softKeywordModifier("sealed", SEALED_KEYWORD_Id);
    CjModifierKeywordToken ABSTRACT_KEYWORD = CjModifierKeywordToken.softKeywordModifier("abstract", ABSTRACT_KEYWORD_Id);
    CjModifierKeywordToken OPEN_KEYWORD = CjModifierKeywordToken.softKeywordModifier("open", OPEN_KEYWORD_Id);
    TokenSet SOFT_KEYWORDS = TokenSet.create(GET_KEYWORD,
            SET_KEYWORD, OPEN_KEYWORD,
            ABSTRACT_KEYWORD,
            SEALED_KEYWORD

    );
    CjModifierKeywordToken OVERRIDE_KEYWORD = CjModifierKeywordToken.keywordModifier("override", OVERRIDE_KEYWORD_Id);
    CjModifierKeywordToken PRIVATE_KEYWORD = CjModifierKeywordToken.keywordModifier("private", PRIVATE_KEYWORD_Id);
    CjModifierKeywordToken PUBLIC_KEYWORD = CjModifierKeywordToken.keywordModifier("public", PUBLIC_KEYWORD_Id);
    CjModifierKeywordToken STATIC_KEYWORD = CjModifierKeywordToken.keywordModifier("static", STATIC_KEYWORD_Id);
    CjModifierKeywordToken INTERNAL_KEYWORD = CjModifierKeywordToken.keywordModifier("internal", INTERNAL_KEYWORD_Id);
    CjModifierKeywordToken PROTECTED_KEYWORD = CjModifierKeywordToken.keywordModifier("protected", PROTECTED_KEYWORD_Id);
    CjKeywordToken MACRO_KEYWORD = CjKeywordToken.keyword("macro", MACRO_KEYWORD_Id);

    //    Class修饰符
    CjKeywordToken[] CLASS_MODIFIER_KEYWORDS_ARRAY = new CjKeywordToken[]{
            OPEN_KEYWORD,
            SEALED_KEYWORD,
            ABSTRACT_KEYWORD,
            PUBLIC_KEYWORD,
            PRIVATE_KEYWORD,
            PROTECTED_KEYWORD
    };
    //    接口修饰符
    CjKeywordToken[] INTERFACE_MODIFIER_KEYWORDS_ARRAY = new CjKeywordToken[]{
            OPEN_KEYWORD,
            SEALED_KEYWORD,
            PUBLIC_KEYWORD,
            PRIVATE_KEYWORD,
            PROTECTED_KEYWORD
    };
    //    结构体修饰符
    CjKeywordToken[] STRUCT_MODIFIER_KEYWORDS_ARRAY = new CjKeywordToken[]{
            PUBLIC_KEYWORD,
            PRIVATE_KEYWORD,
            PROTECTED_KEYWORD
    };
    CjKeywordToken CATCH_KEYWORD = CjKeywordToken.keyword("catch", CATCH_KEYWORD_Id);
    CjKeywordToken FINALLY_KEYWORD = CjKeywordToken.keyword("finally", FINALLY_KEYWORD_Id);
    CjModifierKeywordToken REDEF_KEYWORD = CjModifierKeywordToken.keywordModifier("redef", REDEF_KEYWORD_Id);
    CjKeywordToken QUOTE_KEYWORD = CjKeywordToken.keyword("quote", QUOTE_KEYWORD_Id);

    //    特殊修饰符
    CjKeywordToken FOREIGN_KEYWORD = CjKeywordToken.keyword("foreign", FOREIGN_KEYWORD_Id);
    CjKeywordToken TYPE_KEYWORD = CjKeywordToken.keyword("type", TYPE_KEYWORD_Id);
    CjKeywordToken UNSAFE_KEYWORD = CjKeywordToken.keyword("unsafe", UNSAFE_KEYWORD_Id);
    CjKeywordToken[] SPECIAL_MODIFIER_KEYWORDS_ARRAY = new CjKeywordToken[]{
            CONST_KEYWORD,
            FOREIGN_KEYWORD,
            UNSAFE_KEYWORD,
    };
    TokenSet SPECIAL_MODIFIER_KEYWORDS = TokenSet.create(SPECIAL_MODIFIER_KEYWORDS_ARRAY);
    //   全局函数修饰符
    CjKeywordToken[] FUNC_GLOBAL_MODIFIER_KEYWORDS_ARRAY = new CjKeywordToken[]{
            PUBLIC_KEYWORD,
            FOREIGN_KEYWORD,
            UNSAFE_KEYWORD,
            CONST_KEYWORD
    };
    CjModifierKeywordToken MUT_KEYWORD = CjModifierKeywordToken.keywordModifier("mut", MUT_KEYWORD_Id);
    CjModifierKeywordToken OPERATOR_KEYWORD = CjModifierKeywordToken.keywordModifier("operator", OPERATOR_KEYWORD_Id);
    TokenSet KEYWORDS = TokenSet.orSet(
            TokenSet.create(PACKAGE_KEYWORD, AS_KEYWORD, CLASS_KEYWORD, INTERFACE_KEYWORD,
                    THIS_KEYWORD, SUPER_KEYWORD, LET_KEYWORD, VAR_KEYWORD, CONST_KEYWORD, FUNC_KEYWORD, FOR_KEYWORD,
                    MAIN_KEYWORD, STRUCT_KEYWORD, EXTEND_KEYWORD,
                    TRUE_KEYWORD, FALSE_KEYWORD, IS_KEYWORD,
                    IN_KEYWORD, THROW_KEYWORD, RETURN_KEYWORD, BREAK_KEYWORD, CONTINUE_KEYWORD, IF_KEYWORD,
                    ELSE_KEYWORD, WHILE_KEYWORD, DO_KEYWORD, TRY_KEYWORD, MATCH_KEYWORD, CASE_KEYWORD,
                    TYPEOF_KEYWORD,
                    MACRO_KEYWORD,
                    PROP_KEYWORD, ENUM_KEYWORD,
                    WHERE_KEYWORD,


                    IMPORT_KEYWORD,
                    OVERRIDE_KEYWORD, PRIVATE_KEYWORD, PUBLIC_KEYWORD, PROTECTED_KEYWORD,INTERNAL_KEYWORD,
                    CATCH_KEYWORD, FINALLY_KEYWORD,
                    INIT_KEYWORD,
                    STATIC_KEYWORD,
                    MUT_KEYWORD,
                    OPERATOR_KEYWORD
//            INT8_KEYWORD, INT16_KEYWORD, INT32_KEYWORD, INT64_KEYWORD, UINT8_KEYWORD, UINT16_KEYWORD, UINT32_KEYWORD, UINT64_KEYWORD, FLOAT32_KEYWORD, FLOAT64_KEYWORD, BOOL_KEYWORD, CHAR_KEYWORD, UNIT_KEYWORD

                    , TYPE_KEYWORD
                    , SPAWN_KEYWORD,
                    SYNCHRONIZED_KEYWORD,
                    FOREIGN_KEYWORD,
                    UNSAFE_KEYWORD


            ),
            BASICTYPES
    );
    TokenSet KEYWORDALL = TokenSet.orSet(KEYWORDS, SOFT_KEYWORDS);
    /*。
    此数组用于存根序列化：
    1.请勿更改顺序。
    2.如果添加条目或变更单，请增加存根版本。
    */
    CjModifierKeywordToken[] MODIFIER_KEYWORDS_ARRAY =
            new CjModifierKeywordToken[]{
                    ABSTRACT_KEYWORD, OPEN_KEYWORD, OVERRIDE_KEYWORD, PRIVATE_KEYWORD,
                    PUBLIC_KEYWORD, PROTECTED_KEYWORD,INTERNAL_KEYWORD,
                    STATIC_KEYWORD,
                    MUT_KEYWORD,
                    OPERATOR_KEYWORD,
                    SEALED_KEYWORD,
//                    CONST_KEYWORD,
//                    FOREIGN_KEYWORD,
//                    UNSAFE_KEYWORD,
                    REDEF_KEYWORD
            };
    TokenSet MODIFIER_KEYWORDS = TokenSet.create(MODIFIER_KEYWORDS_ARRAY);
    //    类成员函数修饰符
    CjKeywordToken[] FUNC_CLASSMEMBER_MODIFIER_KEYWORDS_ARRAY = new CjKeywordToken[]{
            OVERRIDE_KEYWORD,

            OPERATOR_KEYWORD,
            STATIC_KEYWORD,
            ABSTRACT_KEYWORD,

            PUBLIC_KEYWORD,
            PRIVATE_KEYWORD,
            PROTECTED_KEYWORD,
            CONST_KEYWORD,
            OPEN_KEYWORD,
            UNSAFE_KEYWORD,

            REDEF_KEYWORD
    };
    //    enum成员函数
    CjKeywordToken[] FUNC_ENUMMEMBER_MODIFIER_KEYWORDS_ARRAY = new CjKeywordToken[]{
            PUBLIC_KEYWORD,
            PRIVATE_KEYWORD,
            STATIC_KEYWORD,
            OVERRIDE_KEYWORD,
            UNSAFE_KEYWORD,
            OPERATOR_KEYWORD,
            REDEF_KEYWORD
    };
    //    结构体成员函数
    CjKeywordToken[] FUNC_STRUCTMEMBER_MODIFIER_KEYWORDS_ARRAY = new CjKeywordToken[]{
            OVERRIDE_KEYWORD,
            OPERATOR_KEYWORD,
            STATIC_KEYWORD,

            PUBLIC_KEYWORD,
            PRIVATE_KEYWORD,


            CONST_KEYWORD,

            UNSAFE_KEYWORD,
            REDEF_KEYWORD,
            MUT_KEYWORD

    };
    //    扩展修饰符
    CjKeywordToken[] EXTEND_MODIFIER_KEYWORDS_ARRAY = new CjKeywordToken[]{

    };
    TokenSet MODALITY_MODIFIERS = TokenSet.create(ABSTRACT_KEYWORD,   SEALED_KEYWORD, OPEN_KEYWORD);

    TokenSet VISIBILITY_MODIFIERS = TokenSet.create(PRIVATE_KEYWORD, PUBLIC_KEYWORD, INTERNAL_KEYWORD, PROTECTED_KEYWORD);

    TokenSet COMMENTS = TokenSet.create(EOL_COMMENT, BLOCK_COMMENT, DOC_COMMENT, SHEBANG_COMMENT);
    TokenSet WHITE_SPACE_OR_COMMENT_BIT_SET = TokenSet.orSet(COMMENTS, WHITESPACES);
    //    List<BracePair>  BRACE_PAIR_LIST = Arrays.asList(
//            new BracePair(LPAR, RPAR,true),
//            new BracePair(LBRACE, RBRACE,true),
//            new BracePair(LBRACKET, RBRACKET,true)
//    );
    //字面常量规则，包括整型、浮点型、字符型、布尔型、字符串等类型的基本值
    TokenSet LITERAL_CONSTANT = TokenSet.create(
            CjNodeTypes.STRING_TEMPLATE,
            CjNodeTypes.RUNE_CONSTANT,
            CjNodeTypes.UNIT_CONSTANT,
            CjNodeTypes.BOOLEAN_CONSTANT,
            CjNodeTypes.FLOAT_CONSTANT,
            CjNodeTypes.INTEGER_CONSTANT,
            CjNodeTypes.CHARACTER_BYTE_CONSTANT
    );


}






