package com.huawei.cangjie1.lexer
//
//import com.huawei.cangjie1.doc.lexer.CDocTokens.Companion.CDOC
//import com.intellij.psi.TokenType
//import com.intellij.psi.tree.IElementType
//import com.intellij.psi.tree.TokenSet
//
//
//object CjTokens {
//
//
//
//    const val INVALID_Id = 0
//    const val EOF_Id = 1
//    const val RESERVED_Id = 2
//    const val BLOCK_COMMENT_Id = 3
//    const val EOL_COMMENT_Id = 4
//    const val SHEBANG_COMMENT_Id = 5
//    const val INTEGER_LITERAL_Id = 6
//    const val FLOAT_LITERAL_Id = 7
//    const val CHARACTER_LITERAL_Id = 8
//    const val CLOSING_QUOTE_Id = 9
//    const val OPEN_QUOTE_Id = 10
//    const val REGULAR_STRING_PART_Id = 11
//    const val ESCAPE_SEQUENCE_Id = 12
//    const val SHORT_TEMPLATE_ENTRY_START_Id = 13
//    const val LONG_TEMPLATE_ENTRY_START_Id = 14
//    const val LONG_TEMPLATE_ENTRY_END_Id = 15
//    const val DANGLING_NEWLINE_Id = 16
//    const val PACKAGE_KEYWORD_Id = 17
//    const val AS_KEYWORD_Id = 18
//    const val TYPE_ALIAS_KEYWORD_Id = 19
//    const val CLASS_KEYWORD_Id = 20
//    const val THIS_KEYWORD_Id = 21
//    const val SUPER_KEYWORD_Id = 22
//    const val LET_KEYWORD_Id = 23
//    const val VAR_KEYWORD_Id = 24
//    const val FUNC_KEYWORD_Id = 25
//    const val FOR_KEYWORD_Id = 26
//    const val NULL_KEYWORD_Id = 27
//    const val TRUE_KEYWORD_Id = 28
//    const val FALSE_KEYWORD_Id = 29
//    const val IS_KEYWORD_Id = 30
//    const val IN_KEYWORD_Id = 31
//    const val THROW_KEYWORD_Id = 32
//    const val RETURN_KEYWORD_Id = 33
//    const val BREAK_KEYWORD_Id = 34
//    const val CONTINUE_KEYWORD_Id = 35
//    const val OBJECT_KEYWORD_Id = 36
//    const val IF_KEYWORD_Id = 37
//    const val TRY_KEYWORD_Id = 38
//    const val ELSE_KEYWORD_Id = 39
//    const val WHILE_KEYWORD_Id = 40
//    const val DO_KEYWORD_Id = 41
//    const val MATCH_KEYWORD_Id = 42
//    const val INTERFACE_KEYWORD_Id = 43
//    const val TYPEOF_KEYWORD_Id = 44
//    const val AS_SAFE_Id = 45
//    const val IDENTIFIER_Id = 46
//    const val FIELD_IDENTIFIER_Id = 47
//    const val LBRACKET_Id = 48
//    const val RBRACKET_Id = 49
//    const val LBRACE_Id = 50
//    const val RBRACE_Id = 51
//    const val LPAR_Id = 52
//    const val RPAR_Id = 53
//    const val DOT_Id = 54
//    const val PLUSPLUS_Id = 55
//    const val MINUSMINUS_Id = 56
//    const val MUL_Id = 57
//    const val PLUS_Id = 58
//    const val MINUS_Id = 59
//    const val EXCL_Id = 60
//    const val DIV_Id = 61
//    const val PERC_Id = 62
//    const val LT_Id = 63
//    const val GT_Id = 64
//    const val LTEQ_Id = 65
//    const val GTEQ_Id = 66
//    const val EQEQEQ_Id = 67
//    const val ARROW_Id = 68
//    const val DOUBLE_ARROW_Id = 69
//    const val EXCLEQEQEQ_Id = 70
//    const val EQEQ_Id = 71
//    const val EXCLEQ_Id = 72
//    const val EXCLEXCL_Id = 73
//    const val ANDAND_Id = 74
//    const val AND_Id = 75
//    const val OROR_Id = 76
//    const val SAFE_ACCESS_Id = 77
//    const val ELVIS_Id = 78
//    const val QUEST_Id = 79
//    const val COLONCOLON_Id = 80
//    const val COLON_Id = 81
//    const val SEMICOLON_Id = 82
//    const val DOUBLE_SEMICOLON_Id = 83
//    const val RANGE_Id = 84
//    const val RANGE_UNTIL_Id = 85
//    const val EQ_Id = 86
//    const val MULTEQ_Id = 87
//    const val DIVEQ_Id = 88
//    const val PERCEQ_Id = 89
//    const val PLUSEQ_Id = 90
//    const val MINUSEQ_Id = 91
//    const val NOT_IN_Id = 92
//    const val NOT_IS_Id = 93
//    const val HASH_Id = 94
//    const val AT_Id = 95
//    const val COMMA_Id = 96
//    const val EOL_OR_SEMICOLON_Id = 97
//    const val FILE_KEYWORD_Id = 98
//    const val FIELD_KEYWORD_Id = 99
//    const val PROPERTY_KEYWORD_Id = 100
//    const val RECEIVER_KEYWORD_Id = 101
//    const val PARAM_KEYWORD_Id = 102
//    const val SETPARAM_KEYWORD_Id = 103
//    const val DELEGATE_KEYWORD_Id = 104
//    const val IMPORT_KEYWORD_Id = 105
//    const val WHERE_KEYWORD_Id = 106
//    const val BY_KEYWORD_Id = 107
//    const val GET_KEYWORD_Id = 108
//    const val SET_KEYWORD_Id = 109
//    const val CONSTRUCTOR_KEYWORD_Id = 110
//    const val INIT_KEYWORD_Id = 111
//    const val CONTEXT_KEYWORD_Id = 112
//    const val ABSTRACT_KEYWORD_Id = 113
//    const val ENUM_KEYWORD_Id = 114
//    const val CONTRACT_KEYWORD_Id = 115
//    const val OPEN_KEYWORD_Id = 116
//    const val INNER_KEYWORD_Id = 117
//    const val OVERRIDE_KEYWORD_Id = 118
//    const val PRIVATE_KEYWORD_Id = 119
//    const val PUBLIC_KEYWORD_Id = 120
//    const val INTERNAL_KEYWORD_Id = 121
//    const val PROTECTED_KEYWORD_Id = 122
//    const val CATCH_KEYWORD_Id = 123
//    const val OUT_KEYWORD_Id = 124
//    const val VARARG_KEYWORD_Id = 125
//    const val REIFIED_KEYWORD_Id = 126
//    const val DYNAMIC_KEYWORD_Id = 127
//    const val COMPANION_KEYWORD_Id = 128
//    const val SEALED_KEYWORD_Id = 129
//    const val FINALLY_KEYWORD_Id = 130
//    const val FINAL_KEYWORD_Id = 131
//    const val LATEINIT_KEYWORD_Id = 132
//    const val DATA_KEYWORD_Id = 133
//    const val VALUE_KEYWORD_Id = 134
//    const val INLINE_KEYWORD_Id = 135
//    const val NOINLINE_KEYWORD_Id = 136
//    const val TAILREC_KEYWORD_Id = 137
//    const val EXTERNAL_KEYWORD_Id = 138
//    const val ANNOTATION_KEYWORD_Id = 139
//    const val CROSSINLINE_KEYWORD_Id = 140
//    const val OPERATOR_KEYWORD_Id = 141
//    const val INFIX_KEYWORD_Id = 142
//    const val CONST_KEYWORD_Id = 143
//    const val SUSPEND_KEYWORD_Id = 144
//    const val HEADER_KEYWORD_Id = 145
//    const val IMPL_KEYWORD_Id = 146
//    const val EXPECT_KEYWORD_Id = 147
//    const val ACTUAL_KEYWORD_Id = 148
//    val EOF = CjToken("EOF", EOF_Id)
//    val RESERVED = CjToken("RESERVED", RESERVED_Id)
//    val BLOCK_COMMENT = CjToken("BLOCK_COMMENT", BLOCK_COMMENT_Id)
//    val EOL_COMMENT = CjToken("EOL_COMMENT", EOL_COMMENT_Id)
//    val SHEBANG_COMMENT = CjToken("SHEBANG_COMMENT", SHEBANG_COMMENT_Id)
//    val DOC_COMMENT: IElementType = CDOC
//    val WHITE_SPACE = TokenType.WHITE_SPACE
//    val INTEGER_LITERAL = CjToken("INTEGER_LITERAL", INTEGER_LITERAL_Id)
//    val FLOAT_LITERAL = CjToken("FLOAT_CONSTANT", FLOAT_LITERAL_Id)
//    val CHARACTER_LITERAL = CjToken("CHARACTER_LITERAL", CHARACTER_LITERAL_Id)
//    val CLOSING_QUOTE = CjToken("CLOSING_QUOTE", CLOSING_QUOTE_Id)
//    val OPEN_QUOTE = CjToken("OPEN_QUOTE", OPEN_QUOTE_Id)
//    val ESCAPE_SEQUENCE = CjToken("ESCAPE_SEQUENCE", ESCAPE_SEQUENCE_Id)
//    val SHORT_TEMPLATE_ENTRY_START = CjToken("SHORT_TEMPLATE_ENTRY_START", SHORT_TEMPLATE_ENTRY_START_Id)
//    val LONG_TEMPLATE_ENTRY_START = CjToken("LONG_TEMPLATE_ENTRY_START", LONG_TEMPLATE_ENTRY_START_Id)
//    val LONG_TEMPLATE_ENTRY_END = CjToken("LONG_TEMPLATE_ENTRY_END", LONG_TEMPLATE_ENTRY_END_Id)
//    val DANGLING_NEWLINE = CjToken("DANGLING_NEWLINE", DANGLING_NEWLINE_Id)
//    val PACKAGE_KEYWORD: CjKeywordToken = CjKeywordToken.keyword("package", PACKAGE_KEYWORD_Id)
//    val AS_KEYWORD: CjKeywordToken = CjKeywordToken.keyword("as", AS_KEYWORD_Id)
//    val TYPE_ALIAS_KEYWORD: CjKeywordToken = CjKeywordToken.keyword("typealias", TYPE_ALIAS_KEYWORD_Id)
//    val CLASS_KEYWORD: CjKeywordToken = CjKeywordToken.keyword("class", CLASS_KEYWORD_Id)
//    val THIS_KEYWORD: CjKeywordToken = CjKeywordToken.keyword("this", THIS_KEYWORD_Id)
//    val SUPER_KEYWORD: CjKeywordToken = CjKeywordToken.keyword("super", SUPER_KEYWORD_Id)
//    val LET_KEYWORD: CjKeywordToken = CjKeywordToken.keyword("val", LET_KEYWORD_Id)
//    val VAR_KEYWORD: CjKeywordToken = CjKeywordToken.keyword("var", VAR_KEYWORD_Id)
//    val FUNC_KEYWORD: CjKeywordToken = CjModifierKeywordToken.keywordModifier("fun", FUNC_KEYWORD_Id)
//    val FOR_KEYWORD: CjKeywordToken = CjKeywordToken.keyword("for", FOR_KEYWORD_Id)
//    val NULL_KEYWORD: CjKeywordToken = CjKeywordToken.keyword("null", NULL_KEYWORD_Id)
//    val TRUE_KEYWORD: CjKeywordToken = CjKeywordToken.keyword("true", TRUE_KEYWORD_Id)
//    val FALSE_KEYWORD: CjKeywordToken = CjKeywordToken.keyword("false", FALSE_KEYWORD_Id)
//    val IS_KEYWORD: CjKeywordToken = CjKeywordToken.keyword("is", IS_KEYWORD_Id)
//    val IN_KEYWORD: CjKeywordToken = CjModifierKeywordToken.keywordModifier("in", IN_KEYWORD_Id)
//    val THROW_KEYWORD: CjKeywordToken = CjKeywordToken.keyword("throw", THROW_KEYWORD_Id)
//    val RETURN_KEYWORD: CjKeywordToken = CjKeywordToken.keyword("return", RETURN_KEYWORD_Id)
//    val BREAK_KEYWORD: CjKeywordToken = CjKeywordToken.keyword("break", BREAK_KEYWORD_Id)
//    val CONTINUE_KEYWORD: CjKeywordToken = CjKeywordToken.keyword("continue", CONTINUE_KEYWORD_Id)
//    val OBJECT_KEYWORD: CjKeywordToken = CjKeywordToken.keyword("object", OBJECT_KEYWORD_Id)
//    val IF_KEYWORD: CjKeywordToken = CjKeywordToken.keyword("if", IF_KEYWORD_Id)
//    val TRY_KEYWORD: CjKeywordToken = CjKeywordToken.keyword("try", TRY_KEYWORD_Id)
//    val ELSE_KEYWORD: CjKeywordToken = CjKeywordToken.keyword("else", ELSE_KEYWORD_Id)
//    val WHILE_KEYWORD: CjKeywordToken = CjKeywordToken.keyword("while", WHILE_KEYWORD_Id)
//    val DO_KEYWORD: CjKeywordToken = CjKeywordToken.keyword("do", DO_KEYWORD_Id)
//    val WHEN_KEYWORD: CjKeywordToken = CjKeywordToken.keyword("when", MATCH_KEYWORD_Id)
//    val INTERFACE_KEYWORD: CjKeywordToken = CjKeywordToken.keyword("interface", INTERFACE_KEYWORD_Id)
//
//    // Reserved for future use:
//    val TYPEOF_KEYWORD: CjKeywordToken = CjKeywordToken.keyword("typeof", TYPEOF_KEYWORD_Id)
//    val `AS_SAFE`: CjToken = CjKeywordToken.keyword("AS_SAFE", AS_SAFE_Id)
//    val IDENTIFIER = CjToken("IDENTIFIER", IDENTIFIER_Id)
//    val FIELD_IDENTIFIER = CjToken("FIELD_IDENTIFIER", FIELD_IDENTIFIER_Id)
//    val LBRACKET: CjSingleValueToken = CjSingleValueToken("LBRACKET", "[", LBRACKET_Id)
//    val RBRACKET: CjSingleValueToken = CjSingleValueToken("RBRACKET", "]", RBRACKET_Id)
//    val LBRACE: CjSingleValueToken = CjSingleValueToken("LBRACE", "{", LBRACE_Id)
//    val RBRACE: CjSingleValueToken = CjSingleValueToken("RBRACE", "}", RBRACE_Id)
//    val LPAR: CjSingleValueToken = CjSingleValueToken("LPAR", "(", LPAR_Id)
//    val RPAR: CjSingleValueToken = CjSingleValueToken("RPAR", ")", RPAR_Id)
//    val DOT: CjSingleValueToken = CjSingleValueToken("DOT", ".", DOT_Id)
//    val PLUSPLUS: CjSingleValueToken = CjSingleValueToken("PLUSPLUS", "++", PLUSPLUS_Id)
//    val MINUSMINUS: CjSingleValueToken = CjSingleValueToken("MINUSMINUS", "--", MINUSMINUS_Id)
//    val MUL: CjSingleValueToken = CjSingleValueToken("MUL", "*", MUL_Id)
//    val PLUS: CjSingleValueToken = CjSingleValueToken("PLUS", "+", PLUS_Id)
//    val MINUS: CjSingleValueToken = CjSingleValueToken("MINUS", "-", MINUS_Id)
//    val EXCL: CjSingleValueToken = CjSingleValueToken("EXCL", "!", EXCL_Id)
//    val DIV: CjSingleValueToken = CjSingleValueToken("DIV", "/", DIV_Id)
//    val PERC: CjSingleValueToken = CjSingleValueToken("PERC", "%", PERC_Id)
//    val LT: CjSingleValueToken = CjSingleValueToken("LT", "<", LT_Id)
//    val GT: CjSingleValueToken = CjSingleValueToken("GT", ">", GT_Id)
//    val LTEQ: CjSingleValueToken = CjSingleValueToken("LTEQ", "<=", LTEQ_Id)
//    val GTEQ: CjSingleValueToken = CjSingleValueToken("GTEQ", ">=", GTEQ_Id)
//    val EQEQEQ: CjSingleValueToken = CjSingleValueToken("EQEQEQ", "===", EQEQEQ_Id)
//    val ARROW: CjSingleValueToken = CjSingleValueToken("ARROW", "->", ARROW_Id)
//    val DOUBLE_ARROW: CjSingleValueToken = CjSingleValueToken("DOUBLE_ARROW", "=>", DOUBLE_ARROW_Id)
//    val EXCLEQEQEQ: CjSingleValueToken = CjSingleValueToken("EXCLEQEQEQ", "!==", EXCLEQEQEQ_Id)
//    val EQEQ: CjSingleValueToken = CjSingleValueToken("EQEQ", "==", EQEQ_Id)
//    val EXCLEQ: CjSingleValueToken = CjSingleValueToken("EXCLEQ", "!=", EXCLEQ_Id)
//    val EXCLEXCL: CjSingleValueToken = CjSingleValueToken("EXCLEXCL", "!!", EXCLEXCL_Id)
//    val ANDAND: CjSingleValueToken = CjSingleValueToken("ANDAND", "&&", ANDAND_Id)
//    val AND: CjSingleValueToken = CjSingleValueToken("AND", "&", AND_Id)
//    val OROR: CjSingleValueToken = CjSingleValueToken("OROR", "||", OROR_Id)
//    val SAFE_ACCESS: CjSingleValueToken = CjSingleValueToken("SAFE_ACCESS", "?.", SAFE_ACCESS_Id)
//    val ELVIS: CjSingleValueToken = CjSingleValueToken("ELVIS", "?:", ELVIS_Id)
//    val QUEST: CjSingleValueToken = CjSingleValueToken("QUEST", "?", QUEST_Id)
//    val COLONCOLON: CjSingleValueToken = CjSingleValueToken("COLONCOLON", "::", COLONCOLON_Id)
//
//
//    val COLON: CjSingleValueToken = CjSingleValueToken("COLON", ":", COLON_Id)
//    val SEMICOLON: CjSingleValueToken = CjSingleValueToken("SEMICOLON", ";", SEMICOLON_Id)
//    val DOUBLE_SEMICOLON: CjSingleValueToken = CjSingleValueToken("DOUBLE_SEMICOLON", ";;", DOUBLE_SEMICOLON_Id)
//    val RANGE: CjSingleValueToken = CjSingleValueToken("RANGE", "..", RANGE_Id)
//    val RANGE_UNTIL: CjSingleValueToken = CjSingleValueToken("RANGE_UNTIL", "..<", RANGE_UNTIL_Id)
//    val EQ: CjSingleValueToken = CjSingleValueToken("EQ", "=", EQ_Id)
//    val MULTEQ: CjSingleValueToken = CjSingleValueToken("MULTEQ", "*=", MULTEQ_Id)
//    val DIVEQ: CjSingleValueToken = CjSingleValueToken("DIVEQ", "/=", DIVEQ_Id)
//    val PERCEQ: CjSingleValueToken = CjSingleValueToken("PERCEQ", "%=", PERCEQ_Id)
//    val PLUSEQ: CjSingleValueToken = CjSingleValueToken("PLUSEQ", "+=", PLUSEQ_Id)
//    val MINUSEQ: CjSingleValueToken = CjSingleValueToken("MINUSEQ", "-=", MINUSEQ_Id)
//    val NOT_IN: CjKeywordToken = CjKeywordToken.keyword("NOT_IN", "!in", NOT_IN_Id)
//    val NOT_IS: CjKeywordToken = CjKeywordToken.keyword("NOT_IS", "!is", NOT_IS_Id)
//    val HASH: CjSingleValueToken = CjSingleValueToken("HASH", "#", HASH_Id)
//    val AT: CjSingleValueToken = CjSingleValueToken("AT", "@", AT_Id)
//    val COMMA: CjSingleValueToken = CjSingleValueToken("COMMA", ",", COMMA_Id)
//    val EOL_OR_SEMICOLON = CjToken("EOL_OR_SEMICOLON", EOL_OR_SEMICOLON_Id)
//    val FILE_KEYWORD: CjKeywordToken = CjKeywordToken.softKeyword("file", FILE_KEYWORD_Id)
//    val FIELD_KEYWORD: CjKeywordToken = CjKeywordToken.softKeyword("field", FIELD_KEYWORD_Id)
//    val PROPERTY_KEYWORD: CjKeywordToken = CjKeywordToken.softKeyword("property", PROPERTY_KEYWORD_Id)
//    val RECEIVER_KEYWORD: CjKeywordToken = CjKeywordToken.softKeyword("receiver", RECEIVER_KEYWORD_Id)
//    val PARAM_KEYWORD: CjKeywordToken = CjKeywordToken.softKeyword("param", PARAM_KEYWORD_Id)
//    val SETPARAM_KEYWORD: CjKeywordToken = CjKeywordToken.softKeyword("setparam", SETPARAM_KEYWORD_Id)
//    val DELEGATE_KEYWORD: CjKeywordToken = CjKeywordToken.softKeyword("delegate", DELEGATE_KEYWORD_Id)
//    val IMPORT_KEYWORD: CjKeywordToken = CjKeywordToken.softKeyword("import", IMPORT_KEYWORD_Id)
//    val WHERE_KEYWORD: CjKeywordToken = CjKeywordToken.softKeyword("where", WHERE_KEYWORD_Id)
//    val BY_KEYWORD: CjKeywordToken = CjKeywordToken.softKeyword("by", BY_KEYWORD_Id)
//    val GET_KEYWORD: CjKeywordToken = CjKeywordToken.softKeyword("get", GET_KEYWORD_Id)
//    val SET_KEYWORD: CjKeywordToken = CjKeywordToken.softKeyword("set", SET_KEYWORD_Id)
//    val CONSTRUCTOR_KEYWORD: CjKeywordToken = CjKeywordToken.softKeyword("constructor", CONSTRUCTOR_KEYWORD_Id)
//    val INIT_KEYWORD: CjKeywordToken = CjKeywordToken.softKeyword("init", INIT_KEYWORD_Id)
//    val CONTEXT_KEYWORD: CjKeywordToken = CjKeywordToken.softKeyword("context", CONTEXT_KEYWORD_Id)
//    val ABSTRACT_KEYWORD: CjModifierKeywordToken =
//        CjModifierKeywordToken.softKeywordModifier("abstract", ABSTRACT_KEYWORD_Id)
//    val ENUM_KEYWORD: CjModifierKeywordToken = CjModifierKeywordToken.softKeywordModifier("enum", ENUM_KEYWORD_Id)
//    val CONTRACT_KEYWORD: CjModifierKeywordToken =
//        CjModifierKeywordToken.softKeywordModifier("contract", CONTRACT_KEYWORD_Id)
//    val OPEN_KEYWORD: CjModifierKeywordToken = CjModifierKeywordToken.softKeywordModifier("open", OPEN_KEYWORD_Id)
//    val INNER_KEYWORD: CjModifierKeywordToken = CjModifierKeywordToken.softKeywordModifier("inner", INNER_KEYWORD_Id)
//    val OVERRIDE_KEYWORD: CjModifierKeywordToken =
//        CjModifierKeywordToken.softKeywordModifier("override", OVERRIDE_KEYWORD_Id)
//    val PRIVATE_KEYWORD: CjModifierKeywordToken =
//        CjModifierKeywordToken.softKeywordModifier("private", PRIVATE_KEYWORD_Id)
//    val PUBLIC_KEYWORD: CjModifierKeywordToken = CjModifierKeywordToken.softKeywordModifier("public", PUBLIC_KEYWORD_Id)
//    val INTERNAL_KEYWORD = CjModifierKeywordToken.softKeywordModifier("internal", INTERNAL_KEYWORD_Id)
//    val PROTECTED_KEYWORD = CjModifierKeywordToken.softKeywordModifier("protected", PROTECTED_KEYWORD_Id)
//    val CATCH_KEYWORD: CjKeywordToken = CjKeywordToken.softKeyword("catch", CATCH_KEYWORD_Id)
//    val OUT_KEYWORD = CjModifierKeywordToken.softKeywordModifier("out", OUT_KEYWORD_Id)
//    val VARARG_KEYWORD = CjModifierKeywordToken.softKeywordModifier("vararg", VARARG_KEYWORD_Id)
//    val REIFIED_KEYWORD = CjModifierKeywordToken.softKeywordModifier("reified", REIFIED_KEYWORD_Id)
//    val DYNAMIC_KEYWORD: CjKeywordToken = CjKeywordToken.softKeyword("dynamic", DYNAMIC_KEYWORD_Id)
//    val COMPANION_KEYWORD = CjModifierKeywordToken.softKeywordModifier("companion", COMPANION_KEYWORD_Id)
//    val SEALED_KEYWORD = CjModifierKeywordToken.softKeywordModifier("sealed", SEALED_KEYWORD_Id)
//    val DEFAULT_VISIBILITY_KEYWORD = PUBLIC_KEYWORD
//    val FINALLY_KEYWORD: CjKeywordToken = CjKeywordToken.softKeyword("finally", FINALLY_KEYWORD_Id)
//    val FINAL_KEYWORD = CjModifierKeywordToken.softKeywordModifier("final", FINAL_KEYWORD_Id)
//    val LATEINIT_KEYWORD = CjModifierKeywordToken.softKeywordModifier("lateinit", LATEINIT_KEYWORD_Id)
//    val DATA_KEYWORD = CjModifierKeywordToken.softKeywordModifier("data", DATA_KEYWORD_Id)
//    val VALUE_KEYWORD = CjModifierKeywordToken.softKeywordModifier("value", VALUE_KEYWORD_Id)
//    val INLINE_KEYWORD = CjModifierKeywordToken.softKeywordModifier("inline", INLINE_KEYWORD_Id)
//    val NOINLINE_KEYWORD = CjModifierKeywordToken.softKeywordModifier("noinline", NOINLINE_KEYWORD_Id)
//    val TAILREC_KEYWORD = CjModifierKeywordToken.softKeywordModifier("tailrec", TAILREC_KEYWORD_Id)
//    val EXTERNAL_KEYWORD = CjModifierKeywordToken.softKeywordModifier("external", EXTERNAL_KEYWORD_Id)
//    val ANNOTATION_KEYWORD = CjModifierKeywordToken.softKeywordModifier("annotation", ANNOTATION_KEYWORD_Id)
//    val CROSSINLINE_KEYWORD = CjModifierKeywordToken.softKeywordModifier("crossinline", CROSSINLINE_KEYWORD_Id)
//    val OPERATOR_KEYWORD = CjModifierKeywordToken.softKeywordModifier("operator", OPERATOR_KEYWORD_Id)
//    val INFIX_KEYWORD = CjModifierKeywordToken.softKeywordModifier("infix", INFIX_KEYWORD_Id)
//    val CONST_KEYWORD = CjModifierKeywordToken.softKeywordModifier("const", CONST_KEYWORD_Id)
//    val SUSPEND_KEYWORD = CjModifierKeywordToken.softKeywordModifier("suspend", SUSPEND_KEYWORD_Id)
//    val HEADER_KEYWORD = CjModifierKeywordToken.softKeywordModifier("header", HEADER_KEYWORD_Id)
//    val IMPL_KEYWORD = CjModifierKeywordToken.softKeywordModifier("impl", IMPL_KEYWORD_Id)
//    val EXPECT_KEYWORD = CjModifierKeywordToken.softKeywordModifier("expect", EXPECT_KEYWORD_Id)
//    val ACTUAL_KEYWORD = CjModifierKeywordToken.softKeywordModifier("actual", ACTUAL_KEYWORD_Id)
//    val KEYWORDS = TokenSet.create(
//        PACKAGE_KEYWORD,
//        AS_KEYWORD,
//        TYPE_ALIAS_KEYWORD,
//        CLASS_KEYWORD,
//        INTERFACE_KEYWORD,
//        THIS_KEYWORD,
//        SUPER_KEYWORD,
//        LET_KEYWORD,
//        VAR_KEYWORD,
//        FUNC_KEYWORD,
//        FOR_KEYWORD,
//        NULL_KEYWORD,
//        TRUE_KEYWORD,
//        FALSE_KEYWORD,
//        IS_KEYWORD,
//        IN_KEYWORD,
//        THROW_KEYWORD,
//        RETURN_KEYWORD,
//        BREAK_KEYWORD,
//        CONTINUE_KEYWORD,
//        OBJECT_KEYWORD,
//        IF_KEYWORD,
//        ELSE_KEYWORD,
//        WHILE_KEYWORD,
//        DO_KEYWORD,
//        TRY_KEYWORD,
//        WHEN_KEYWORD,
//        NOT_IN,
//        NOT_IS,
//        `AS_SAFE`,
//        TYPEOF_KEYWORD
//    )
//    val SOFT_KEYWORDS = TokenSet.create(
//        FILE_KEYWORD,
//        IMPORT_KEYWORD,
//        WHERE_KEYWORD,
//        BY_KEYWORD,
//        GET_KEYWORD,
//        SET_KEYWORD,
//        ABSTRACT_KEYWORD,
//        ENUM_KEYWORD,
//        CONTRACT_KEYWORD,
//        OPEN_KEYWORD,
//        INNER_KEYWORD,
//        OVERRIDE_KEYWORD,
//        PRIVATE_KEYWORD,
//        PUBLIC_KEYWORD,
//        INTERNAL_KEYWORD,
//        PROTECTED_KEYWORD,
//        CATCH_KEYWORD,
//        FINALLY_KEYWORD,
//        OUT_KEYWORD,
//        FINAL_KEYWORD,
//        VARARG_KEYWORD,
//        REIFIED_KEYWORD,
//        DYNAMIC_KEYWORD,
//        COMPANION_KEYWORD,
//        CONSTRUCTOR_KEYWORD,
//        INIT_KEYWORD,
//        SEALED_KEYWORD,
//        FIELD_KEYWORD,
//        PROPERTY_KEYWORD,
//        RECEIVER_KEYWORD,
//        PARAM_KEYWORD,
//        SETPARAM_KEYWORD,
//        DELEGATE_KEYWORD,
//        LATEINIT_KEYWORD,
//        DATA_KEYWORD,
//        INLINE_KEYWORD,
//        NOINLINE_KEYWORD,
//        TAILREC_KEYWORD,
//        EXTERNAL_KEYWORD,
//        ANNOTATION_KEYWORD,
//        CROSSINLINE_KEYWORD,
//        CONST_KEYWORD,
//        OPERATOR_KEYWORD,
//        INFIX_KEYWORD,
//        SUSPEND_KEYWORD,
//        HEADER_KEYWORD,
//        IMPL_KEYWORD,
//        EXPECT_KEYWORD,
//        ACTUAL_KEYWORD,
//        VALUE_KEYWORD,
//        CONTEXT_KEYWORD
//    )
//
//    /*。
//此数组用于存根序列化：
//1.请勿更改订单。
//2.如果添加条目或变更单，请增加存根版本。
//*/
//    val MODIFIER_KEYWORDS_ARRAY = arrayOf(
//        ABSTRACT_KEYWORD,
//        ENUM_KEYWORD,
//        CONTRACT_KEYWORD,
//        OPEN_KEYWORD,
//        INNER_KEYWORD,
//        OVERRIDE_KEYWORD,
//        PRIVATE_KEYWORD,
//        PUBLIC_KEYWORD,
//        INTERNAL_KEYWORD,
//        PROTECTED_KEYWORD,
//        OUT_KEYWORD,
//        IN_KEYWORD,
//        FINAL_KEYWORD,
//        VARARG_KEYWORD,
//        REIFIED_KEYWORD,
//        COMPANION_KEYWORD,
//        SEALED_KEYWORD,
//        LATEINIT_KEYWORD,
//        DATA_KEYWORD,
//        INLINE_KEYWORD,
//        NOINLINE_KEYWORD,
//        TAILREC_KEYWORD,
//        EXTERNAL_KEYWORD,
//        ANNOTATION_KEYWORD,
//        CROSSINLINE_KEYWORD,
//        CONST_KEYWORD,
//        OPERATOR_KEYWORD,
//        INFIX_KEYWORD,
//        SUSPEND_KEYWORD,
//        HEADER_KEYWORD,
//        IMPL_KEYWORD,
//        EXPECT_KEYWORD,
//        ACTUAL_KEYWORD,
//        FUNC_KEYWORD,
//        VALUE_KEYWORD
//    )
//    val MODIFIER_KEYWORDS = TokenSet.create(*MODIFIER_KEYWORDS_ARRAY)
//    val TYPE_MODIFIER_KEYWORDS = TokenSet.create(SUSPEND_KEYWORD)
//    val TYPE_ARGUMENT_MODIFIER_KEYWORDS = TokenSet.create(IN_KEYWORD, OUT_KEYWORD)
//    val RESERVED_VALUE_PARAMETER_MODIFIER_KEYWORDS = TokenSet.create(OUT_KEYWORD, VARARG_KEYWORD)
//    val VISIBILITY_MODIFIERS = TokenSet.create(PRIVATE_KEYWORD, PUBLIC_KEYWORD, INTERNAL_KEYWORD, PROTECTED_KEYWORD)
//    val MODALITY_MODIFIERS = TokenSet.create(ABSTRACT_KEYWORD, FINAL_KEYWORD, SEALED_KEYWORD, OPEN_KEYWORD)
//    val WHITESPACES = TokenSet.create(TokenType.WHITE_SPACE)
//    val REGULAR_STRING_PART = CjToken("REGULAR_STRING_PART", REGULAR_STRING_PART_Id)
//    val COMMENTS = TokenSet.create(EOL_COMMENT, BLOCK_COMMENT, DOC_COMMENT, SHEBANG_COMMENT)
//    val WHITE_SPACE_OR_COMMENT_BIT_SET = TokenSet.orSet(COMMENTS, WHITESPACES)
//    val STRINGS = TokenSet.create(CHARACTER_LITERAL, REGULAR_STRING_PART)
//    val OPERATIONS = TokenSet.create(
//        AS_KEYWORD,
//        `AS_SAFE`,
//        IS_KEYWORD,
//        IN_KEYWORD,
//        DOT,
//        PLUSPLUS,
//        MINUSMINUS,
//        EXCLEXCL,
//        MUL,
//        PLUS,
//        MINUS,
//        EXCL,
//        DIV,
//        PERC,
//        LT,
//        GT,
//        LTEQ,
//        GTEQ,
//        EQEQEQ,
//        EXCLEQEQEQ,
//        EQEQ,
//        EXCLEQ,
//        ANDAND,
//        OROR,
//        SAFE_ACCESS,
//        ELVIS,
//        RANGE,
//        RANGE_UNTIL,
//        EQ,
//        MULTEQ,
//        DIVEQ,
//        PERCEQ,
//        PLUSEQ,
//        MINUSEQ,
//        NOT_IN,
//        NOT_IS,
//        IDENTIFIER
//    )
//    val AUGMENTED_ASSIGNMENTS = TokenSet.create(PLUSEQ, MINUSEQ, MULTEQ, PERCEQ, DIVEQ)
//    val ALL_ASSIGNMENTS = TokenSet.create(EQ, PLUSEQ, MINUSEQ, MULTEQ, PERCEQ, DIVEQ)
//    val INCREMENT_AND_DECREMENT = TokenSet.create(PLUSPLUS, MINUSMINUS)
//}
//
