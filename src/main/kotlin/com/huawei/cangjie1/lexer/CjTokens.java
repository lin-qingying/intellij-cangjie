/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.huawei.cangjie1.lexer;

import com.huawei.cangjie1.doc.lexer.CDocTokens;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;

public interface CjTokens {
    int INVALID_Id = 0;
    int EOF_Id = 1;
    int RESERVED_Id = 2;
    int BLOCK_COMMENT_Id = 3;
    int EOL_COMMENT_Id = 4;
    int SHEBANG_COMMENT_Id = 5;
    int INTEGER_LITERAL_Id = 6;
    int FLOAT_LITERAL_Id = 7;
    int CHARACTER_LITERAL_Id = 8;
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

    int CLASS_KEYWORD_Id = 20;
    int THIS_KEYWORD_Id = 21;
    int SUPER_KEYWORD_Id = 22;
    int LET_KEYWORD_Id = 23;
    int VAR_KEYWORD_Id = 24;
    int FUNC_KEYWORD_Id = 25;
    int FOR_KEYWORD_Id = 26;

    int TRUE_KEYWORD_Id = 28;
    int FALSE_KEYWORD_Id = 29;
    int IS_KEYWORD_Id = 30;
    int IN_KEYWORD_Id = 31;
    int THROW_KEYWORD_Id = 32;
    int RETURN_KEYWORD_Id = 33;
    int BREAK_KEYWORD_Id = 34;
    int CONTINUE_KEYWORD_Id = 35;

    int IF_KEYWORD_Id = 37;
    int TRY_KEYWORD_Id = 38;
    int ELSE_KEYWORD_Id = 39;
    int WHILE_KEYWORD_Id = 40;
    int DO_KEYWORD_Id = 41;
    int MATCH_KEYWORD_Id = 42;
    int INTERFACE_KEYWORD_Id = 43;
    int TYPEOF_KEYWORD_Id = 44;
    int AS_SAFE_Id = 45;
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

    int EQEQ_Id = 71;
    int EXCLEQ_Id = 72;

    int ANDAND_Id = 74;
    int AND_Id = 75;
    int OROR_Id = 76;


    int QUEST_Id = 79;

    int COLON_Id = 81;
    int SEMICOLON_Id = 82;
    int DOUBLE_SEMICOLON_Id = 83;
    int RANGE_Id = 84;

    int EQ_Id = 86;
    int MULTEQ_Id = 87;
    int DIVEQ_Id = 88;
    int PERCEQ_Id = 89;
    int PLUSEQ_Id = 90;
    int MINUSEQ_Id = 91;


    int COMMA_Id = 96;
    int EOL_OR_SEMICOLON_Id = 97;
    int FILE_KEYWORD_Id = 98;
    int FIELD_KEYWORD_Id = 99;
    int PROPERTY_KEYWORD_Id = 100;
    int RECEIVER_KEYWORD_Id = 101;
    int PARAM_KEYWORD_Id = 102;
    int SETPARAM_KEYWORD_Id = 103;
    int DELEGATE_KEYWORD_Id = 104;
    int IMPORT_KEYWORD_Id = 105;
    int WHERE_KEYWORD_Id = 106;
    int BY_KEYWORD_Id = 107;
    int GET_KEYWORD_Id = 108;
    int SET_KEYWORD_Id = 109;
    int CONSTRUCTOR_KEYWORD_Id = 110;
    int INIT_KEYWORD_Id = 111;

    int ABSTRACT_KEYWORD_Id = 113;
    int ENUM_KEYWORD_Id = 114;

    int OPEN_KEYWORD_Id = 116;

    int OVERRIDE_KEYWORD_Id = 118;
    int PRIVATE_KEYWORD_Id = 119;
    int PUBLIC_KEYWORD_Id = 120;

    int PROTECTED_KEYWORD_Id = 122;
    int CATCH_KEYWORD_Id = 123;


    int FINALLY_KEYWORD_Id = 130;


    int OPERATOR_KEYWORD_Id = 141;

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


    CjSingleValueToken HASH = new CjSingleValueToken("HASH", "#", HASH_Id);

    CjToken EOF = new CjToken("EOF", EOF_Id);

    CjToken RESERVED = new CjToken("RESERVED", RESERVED_Id);

    CjToken BLOCK_COMMENT = new CjToken("BLOCK_COMMENT", BLOCK_COMMENT_Id);
    CjToken EOL_COMMENT = new CjToken("EOL_COMMENT", EOL_COMMENT_Id);
    CjToken SHEBANG_COMMENT = new CjToken("SHEBANG_COMMENT", SHEBANG_COMMENT_Id);

    IElementType DOC_COMMENT = CDocTokens.Companion.getCDOC();

    IElementType WHITE_SPACE = TokenType.WHITE_SPACE;

    CjToken INTEGER_LITERAL = new CjToken("INTEGER_LITERAL", INTEGER_LITERAL_Id);
    CjToken FLOAT_LITERAL = new CjToken("FLOAT_CONSTANT", FLOAT_LITERAL_Id);
    CjToken CHARACTER_LITERAL = new CjToken("CHARACTER_LITERAL", CHARACTER_LITERAL_Id);

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
    CjKeywordToken THIS_KEYWORD = CjKeywordToken.keyword("this", THIS_KEYWORD_Id);
    CjKeywordToken SUPER_KEYWORD = CjKeywordToken.keyword("super", SUPER_KEYWORD_Id);
    CjKeywordToken LET_KEYWORD = CjKeywordToken.keyword("let", LET_KEYWORD_Id);
    CjKeywordToken VAR_KEYWORD = CjKeywordToken.keyword("var", VAR_KEYWORD_Id);

    CjModifierKeywordToken MAIN_KEYWORD = CjModifierKeywordToken.keywordModifier("main", MAIN_KEYWORD_Id);
    CjModifierKeywordToken FUNC_KEYWORD = CjModifierKeywordToken.keywordModifier("func", FUNC_KEYWORD_Id);
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
    CjKeywordToken INTERFACE_KEYWORD = CjKeywordToken.keyword("interface", INTERFACE_KEYWORD_Id);

    // Reserved for future use:
    CjKeywordToken TYPEOF_KEYWORD = CjKeywordToken.keyword("typeof", TYPEOF_KEYWORD_Id);

    CjToken AS_SAFE = CjKeywordToken.keyword("AS_SAFE", AS_SAFE_Id);

    CjToken IDENTIFIER = new CjToken("IDENTIFIER", IDENTIFIER_Id);

    CjToken FIELD_IDENTIFIER = new CjToken("FIELD_IDENTIFIER", FIELD_IDENTIFIER_Id);
    CjSingleValueToken LBRACKET = new CjSingleValueToken("LBRACKET", "[", LBRACKET_Id);
    CjSingleValueToken RBRACKET = new CjSingleValueToken("RBRACKET", "]", RBRACKET_Id);
    CjSingleValueToken LBRACE = new CjSingleValueToken("LBRACE", "{", LBRACE_Id);
    CjSingleValueToken RBRACE = new CjSingleValueToken("RBRACE", "}", RBRACE_Id);
    CjSingleValueToken LPAR = new CjSingleValueToken("LPAR", "(", LPAR_Id);
    CjSingleValueToken RPAR = new CjSingleValueToken("RPAR", ")", RPAR_Id);
    CjSingleValueToken DOT = new CjSingleValueToken("DOT", ".", DOT_Id);
    CjSingleValueToken PLUSPLUS = new CjSingleValueToken("PLUSPLUS", "++", PLUSPLUS_Id);
    CjSingleValueToken MINUSMINUS = new CjSingleValueToken("MINUSMINUS", "--", MINUSMINUS_Id);
    CjSingleValueToken MUL = new CjSingleValueToken("MUL", "*", MUL_Id);
    CjSingleValueToken PLUS = new CjSingleValueToken("PLUS", "+", PLUS_Id);
    CjSingleValueToken MINUS = new CjSingleValueToken("MINUS", "-", MINUS_Id);
    CjSingleValueToken EXCL = new CjSingleValueToken("EXCL", "!", EXCL_Id);
    CjSingleValueToken DIV = new CjSingleValueToken("DIV", "/", DIV_Id);
    CjSingleValueToken PERC = new CjSingleValueToken("PERC", "%", PERC_Id);
    CjSingleValueToken LT = new CjSingleValueToken("LT", "<", LT_Id);
    CjSingleValueToken GT = new CjSingleValueToken("GT", ">", GT_Id);
    CjSingleValueToken LTEQ = new CjSingleValueToken("LTEQ", "<=", LTEQ_Id);
    CjSingleValueToken GTEQ = new CjSingleValueToken("GTEQ", ">=", GTEQ_Id);
    CjSingleValueToken ARROW = new CjSingleValueToken("ARROW", "->", ARROW_Id);
    CjSingleValueToken DOUBLE_ARROW = new CjSingleValueToken("DOUBLE_ARROW", "=>", DOUBLE_ARROW_Id);
    CjSingleValueToken EQEQ = new CjSingleValueToken("EQEQ", "==", EQEQ_Id);
    CjSingleValueToken EXCLEQ = new CjSingleValueToken("EXCLEQ", "!=", EXCLEQ_Id);
    CjSingleValueToken ANDAND = new CjSingleValueToken("ANDAND", "&&", ANDAND_Id);
    CjSingleValueToken AND = new CjSingleValueToken("AND", "&", AND_Id);
    CjSingleValueToken OROR = new CjSingleValueToken("OROR", "||", OROR_Id);
    CjSingleValueToken QUEST = new CjSingleValueToken("QUEST", "?", QUEST_Id);
    CjSingleValueToken COLON = new CjSingleValueToken("COLON", ":", COLON_Id);
    CjSingleValueToken SEMICOLON = new CjSingleValueToken("SEMICOLON", ";", SEMICOLON_Id);
    CjSingleValueToken DOUBLE_SEMICOLON = new CjSingleValueToken("DOUBLE_SEMICOLON", ";;", DOUBLE_SEMICOLON_Id);
    CjSingleValueToken RANGE = new CjSingleValueToken("RANGE", "..", RANGE_Id);
    CjSingleValueToken EQ = new CjSingleValueToken("EQ", "=", EQ_Id);
    CjSingleValueToken MULTEQ = new CjSingleValueToken("MULTEQ", "*=", MULTEQ_Id);
    CjSingleValueToken DIVEQ = new CjSingleValueToken("DIVEQ", "/=", DIVEQ_Id);
    CjSingleValueToken PERCEQ = new CjSingleValueToken("PERCEQ", "%=", PERCEQ_Id);
    CjSingleValueToken PLUSEQ = new CjSingleValueToken("PLUSEQ", "+=", PLUSEQ_Id);
    CjSingleValueToken MINUSEQ = new CjSingleValueToken("MINUSEQ", "-=", MINUSEQ_Id);

    CjSingleValueToken COMMA = new CjSingleValueToken("COMMA", ",", COMMA_Id);

    CjToken EOL_OR_SEMICOLON = new CjToken("EOL_OR_SEMICOLON", EOL_OR_SEMICOLON_Id);
    CjKeywordToken FILE_KEYWORD = CjKeywordToken.softKeyword("file", FILE_KEYWORD_Id);
    CjKeywordToken FIELD_KEYWORD = CjKeywordToken.softKeyword("field", FIELD_KEYWORD_Id);
    CjKeywordToken PROPERTY_KEYWORD = CjKeywordToken.softKeyword("property", PROPERTY_KEYWORD_Id);
    CjKeywordToken RECEIVER_KEYWORD = CjKeywordToken.softKeyword("receiver", RECEIVER_KEYWORD_Id);
    CjKeywordToken PARAM_KEYWORD = CjKeywordToken.softKeyword("param", PARAM_KEYWORD_Id);
    CjKeywordToken SETPARAM_KEYWORD = CjKeywordToken.softKeyword("setparam", SETPARAM_KEYWORD_Id);
    CjKeywordToken DELEGATE_KEYWORD = CjKeywordToken.softKeyword("delegate", DELEGATE_KEYWORD_Id);
    CjKeywordToken IMPORT_KEYWORD = CjKeywordToken.softKeyword("import", IMPORT_KEYWORD_Id);
    CjKeywordToken WHERE_KEYWORD = CjKeywordToken.softKeyword("where", WHERE_KEYWORD_Id);
    CjKeywordToken BY_KEYWORD = CjKeywordToken.softKeyword("by", BY_KEYWORD_Id);
    CjKeywordToken GET_KEYWORD = CjKeywordToken.softKeyword("get", GET_KEYWORD_Id);
    CjKeywordToken SET_KEYWORD = CjKeywordToken.softKeyword("set", SET_KEYWORD_Id);
    CjKeywordToken CONSTRUCTOR_KEYWORD = CjKeywordToken.softKeyword("constructor", CONSTRUCTOR_KEYWORD_Id);
    CjKeywordToken INIT_KEYWORD = CjKeywordToken.softKeyword("init", INIT_KEYWORD_Id);


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
    CjKeywordToken BOOL_KEYWORD = CjKeywordToken.keyword("Bool", BOOL_Id);
    CjKeywordToken CHAR_KEYWORD = CjKeywordToken.keyword("Char", CHAR_Id);
    CjKeywordToken UNIT_KEYWORD = CjKeywordToken.keyword("Unit", UNIT_Id);


    CjModifierKeywordToken ABSTRACT_KEYWORD = CjModifierKeywordToken.softKeywordModifier("abstract", ABSTRACT_KEYWORD_Id);
    CjModifierKeywordToken ENUM_KEYWORD = CjModifierKeywordToken.softKeywordModifier("enum", ENUM_KEYWORD_Id);
    CjModifierKeywordToken OPEN_KEYWORD = CjModifierKeywordToken.softKeywordModifier("opwen", OPEN_KEYWORD_Id);

    CjModifierKeywordToken OVERRIDE_KEYWORD = CjModifierKeywordToken.softKeywordModifier("override", OVERRIDE_KEYWORD_Id);
    CjModifierKeywordToken PRIVATE_KEYWORD = CjModifierKeywordToken.softKeywordModifier("private", PRIVATE_KEYWORD_Id);
    CjModifierKeywordToken PUBLIC_KEYWORD = CjModifierKeywordToken.softKeywordModifier("public", PUBLIC_KEYWORD_Id);

    CjModifierKeywordToken PROTECTED_KEYWORD = CjModifierKeywordToken.softKeywordModifier("protected", PROTECTED_KEYWORD_Id);
    CjKeywordToken CATCH_KEYWORD = CjKeywordToken.softKeyword("catch", CATCH_KEYWORD_Id);


    CjModifierKeywordToken DEFAULT_VISIBILITY_KEYWORD = PUBLIC_KEYWORD;

    CjKeywordToken FINALLY_KEYWORD = CjKeywordToken.softKeyword("finally", FINALLY_KEYWORD_Id);


    CjModifierKeywordToken OPERATOR_KEYWORD = CjModifierKeywordToken.softKeywordModifier("operator", OPERATOR_KEYWORD_Id);


    TokenSet KEYWORDS = TokenSet.create(PACKAGE_KEYWORD, AS_KEYWORD, CLASS_KEYWORD, INTERFACE_KEYWORD,
            THIS_KEYWORD, SUPER_KEYWORD, LET_KEYWORD, VAR_KEYWORD, FUNC_KEYWORD, FOR_KEYWORD,
            MAIN_KEYWORD,
            TRUE_KEYWORD, FALSE_KEYWORD, IS_KEYWORD,
            IN_KEYWORD, THROW_KEYWORD, RETURN_KEYWORD, BREAK_KEYWORD, CONTINUE_KEYWORD, IF_KEYWORD,
            ELSE_KEYWORD, WHILE_KEYWORD, DO_KEYWORD, TRY_KEYWORD, MATCH_KEYWORD, AS_SAFE,
            TYPEOF_KEYWORD,
            INT8_KEYWORD, INT16_KEYWORD, INT32_KEYWORD, INT64_KEYWORD, UINT8_KEYWORD, UINT16_KEYWORD, UINT32_KEYWORD, UINT64_KEYWORD, FLOAT32_KEYWORD, FLOAT64_KEYWORD, BOOL_KEYWORD, CHAR_KEYWORD, UNIT_KEYWORD
    );

    TokenSet SOFT_KEYWORDS = TokenSet.create(FILE_KEYWORD, IMPORT_KEYWORD, WHERE_KEYWORD, BY_KEYWORD, GET_KEYWORD,

            SET_KEYWORD, ABSTRACT_KEYWORD, ENUM_KEYWORD, OPEN_KEYWORD,
            OVERRIDE_KEYWORD, PRIVATE_KEYWORD, PUBLIC_KEYWORD, PROTECTED_KEYWORD,
            CATCH_KEYWORD, FINALLY_KEYWORD,
            CONSTRUCTOR_KEYWORD, INIT_KEYWORD,
            FIELD_KEYWORD, PROPERTY_KEYWORD, RECEIVER_KEYWORD, PARAM_KEYWORD, SETPARAM_KEYWORD,
            DELEGATE_KEYWORD,

            OPERATOR_KEYWORD
    );


    /*
        This array is used in stub serialization:
        1. Do not change order.
        2. If you add an entry or change order, increase stub version.
     */
    CjModifierKeywordToken[] MODIFIER_KEYWORDS_ARRAY =
            new CjModifierKeywordToken[]{
                    ABSTRACT_KEYWORD, ENUM_KEYWORD, OPEN_KEYWORD, OVERRIDE_KEYWORD, PRIVATE_KEYWORD,
                    PUBLIC_KEYWORD, PROTECTED_KEYWORD, IN_KEYWORD,


                    OPERATOR_KEYWORD,
                    FUNC_KEYWORD, MAIN_KEYWORD
            };

    TokenSet MODIFIER_KEYWORDS = TokenSet.create(MODIFIER_KEYWORDS_ARRAY);


    TokenSet TYPE_ARGUMENT_MODIFIER_KEYWORDS = TokenSet.create(IN_KEYWORD);

    TokenSet VISIBILITY_MODIFIERS = TokenSet.create(PRIVATE_KEYWORD, PUBLIC_KEYWORD, PROTECTED_KEYWORD);
    TokenSet MODALITY_MODIFIERS = TokenSet.create(ABSTRACT_KEYWORD, OPEN_KEYWORD);

    TokenSet WHITESPACES = TokenSet.create(TokenType.WHITE_SPACE);


    TokenSet COMMENTS = TokenSet.create(EOL_COMMENT, BLOCK_COMMENT, DOC_COMMENT, SHEBANG_COMMENT);
    TokenSet WHITE_SPACE_OR_COMMENT_BIT_SET = TokenSet.orSet(COMMENTS, WHITESPACES);

    TokenSet STRINGS = TokenSet.create(CHARACTER_LITERAL, REGULAR_STRING_PART);
    TokenSet OPERATIONS = TokenSet.create(AS_KEYWORD, AS_SAFE, IS_KEYWORD, IN_KEYWORD, DOT, PLUSPLUS, MINUSMINUS, MUL, PLUS,
            MINUS, EXCL, DIV, PERC, LT, GT, LTEQ, GTEQ, EQEQ, EXCLEQ, ANDAND, OROR,

            RANGE, EQ, MULTEQ, DIVEQ, PERCEQ, PLUSEQ, MINUSEQ,

            IDENTIFIER);

    TokenSet AUGMENTED_ASSIGNMENTS = TokenSet.create(PLUSEQ, MINUSEQ, MULTEQ, PERCEQ, DIVEQ);
    TokenSet ALL_ASSIGNMENTS = TokenSet.create(EQ, PLUSEQ, MINUSEQ, MULTEQ, PERCEQ, DIVEQ);
    TokenSet INCREMENT_AND_DECREMENT = TokenSet.create(PLUSPLUS, MINUSMINUS);


    //基本类型
    TokenSet BASICTYPES = TokenSet.create(
            INT8_KEYWORD, INT16_KEYWORD, INT32_KEYWORD, INT64_KEYWORD, UINT8_KEYWORD, UINT16_KEYWORD, UINT32_KEYWORD, UINT64_KEYWORD, FLOAT32_KEYWORD, FLOAT64_KEYWORD, BOOL_KEYWORD, CHAR_KEYWORD, UNIT_KEYWORD
    );
}
