//package cn.cangnova.cangjie.lexer.cjtokens
//
//import cn.cangnova.cangjie.lexer.CjKeywordToken
//import cn.cangnova.cangjie.lexer.CjModifierKeywordToken
//import cn.cangnova.cangjie.lexer.CjSingleValueToken
//import cn.cangnova.cangjie.lexer.CjToken
//import com.intellij.lang.BracePair;
//import com.intellij.psi.TokenType;
//import com.intellij.psi.tree.TokenSet;
//import cn.cangnova.cangjie.psi.CjNodeTypes;
//
//const val INVALID_Id: Int = 0
//const val EOF_Id: Int = 1
//const val UNIT_LITERAL_Id: Int = 2
//const val BLOCK_COMMENT_Id: Int = 3
//const val EOL_COMMENT_Id: Int = 4
//const val SHEBANG_COMMENT_Id: Int = 5
//const val INTEGER_LITERAL_Id: Int = 6
//const val FLOAT_LITERAL_Id: Int = 7
//const val RUNE_LITERAL_Id: Int = 8
//const val CLOSING_QUOTE_Id: Int = 9
//const val OPEN_QUOTE_Id: Int = 10
//const val REGULAR_STRING_PART_Id: Int = 11
//const val ESCAPE_SEQUENCE_Id: Int = 12
//const val SHORT_TEMPLATE_ENTRY_START_Id: Int = 13
//const val LONG_TEMPLATE_ENTRY_START_Id: Int = 14
//const val LONG_TEMPLATE_ENTRY_END_Id: Int = 15
//const val DANGLING_NEWLINE_Id: Int = 16
//const val PACKAGE_KEYWORD_Id: Int = 17
//const val AS_KEYWORD_Id: Int = 18
//const val SHOP_STRING_Id: Int = 19
//const val TUPLE_LITERAL_Id: Int = 19
//const val CLASS_KEYWORD_Id: Int = 20
//const val THIS_KEYWORD_Id: Int = 21
//const val SUPER_KEYWORD_Id: Int = 22
//const val LET_KEYWORD_Id: Int = 23
//const val VAR_KEYWORD_Id: Int = 24
//const val FUNC_KEYWORD_Id: Int = 25
//const val FOR_KEYWORD_Id: Int = 26
//
//const val EXTEND_KEYWORD_Id: Int = 27
//
//const val TRUE_KEYWORD_Id: Int = 28
//const val FALSE_KEYWORD_Id: Int = 29
//const val IS_KEYWORD_Id: Int = 30
//const val IN_KEYWORD_Id: Int = 31
//const val THROW_KEYWORD_Id: Int = 32
//const val RETURN_KEYWORD_Id: Int = 33
//const val BREAK_KEYWORD_Id: Int = 34
//const val CONTINUE_KEYWORD_Id: Int = 35
//const val CONST_KEYWORD_Id: Int = 36
//
//const val IF_KEYWORD_Id: Int = 37
//const val TRY_KEYWORD_Id: Int = 38
//const val ELSE_KEYWORD_Id: Int = 39
//const val WHILE_KEYWORD_Id: Int = 40
//const val DO_KEYWORD_Id: Int = 41
//const val MATCH_KEYWORD_Id: Int = 42
//const val INTERFACE_KEYWORD_Id: Int = 43
//const val TYPEOF_KEYWORD_Id: Int = 44
//const val IMPORT_KEYWORD_Id: Int = 45
//const val IDENTIFIER_Id: Int = 46
//const val FIELD_IDENTIFIER_Id: Int = 47
//const val LBRACKET_Id: Int = 48
//const val RBRACKET_Id: Int = 49
//const val LBRACE_Id: Int = 50
//const val RBRACE_Id: Int = 51
//const val LPAR_Id: Int = 52
//const val RPAR_Id: Int = 53
//const val DOT_Id: Int = 54
//const val PLUSPLUS_Id: Int = 55
//const val MINUSMINUS_Id: Int = 56
//const val MUL_Id: Int = 57
//const val PLUS_Id: Int = 58
//const val MINUS_Id: Int = 59
//const val EXCL_Id: Int = 60
//const val DIV_Id: Int = 61
//const val PERC_Id: Int = 62
//const val LT_Id: Int = 63
//const val GT_Id: Int = 64
//const val LTEQ_Id: Int = 65
//const val GTEQ_Id: Int = 66
//const val EQEQEQ_Id: Int = 67
//const val ARROW_Id: Int = 68
//const val DOUBLE_ARROW_Id: Int = 69
//const val CASE_KEYWORD_Id: Int = 70
//const val EQEQ_Id: Int = 71
//const val EXCLEQ_Id: Int = 72
//
//const val LEFT_ARROW_Id: Int = 73
//const val ANDAND_Id: Int = 74
//const val AND_Id: Int = 75
//const val OROR_Id: Int = 76
//const val XOR_Id: Int = 77
//const val OR_Id: Int = 78
//
//const val QUEST_Id: Int = 79
//
//const val CHARACTER_BYTE_LITERAL_Id: Int = 80
//
//const val COLON_Id: Int = 81
//const val SEMICOLON_Id: Int = 82
//const val DOUBLE_SEMICOLON_Id: Int = 83
//const val RANGE_Id: Int = 84
//
//const val RANGEEQ_Id: Int = 85
//
//
//const val EQ_Id: Int = 86
//const val MULTEQ_Id: Int = 87
//const val DIVEQ_Id: Int = 88
//const val PERCEQ_Id: Int = 89
//const val PLUSEQ_Id: Int = 90
//const val MINUSEQ_Id: Int = 91
//const val TILED_Id: Int = 92
//const val ANDANDEQ_Id: Int = 93
//const val AT_Id: Int = 95
//const val COMMA_Id: Int = 96
//const val EOL_OR_SEMICOLON_Id: Int = 97
//const val ELVIS_Id: Int = 98
//const val PIPELINE_Id: Int = 99
//const val WHERE_KEYWORD_Id: Int = 106
//
////    int BY_KEYWORD_Id = 107;
//const val GET_KEYWORD_Id: Int = 108
//const val SET_KEYWORD_Id: Int = 109
//const val COALESCING_Id: Int = 110
//const val INIT_KEYWORD_Id: Int = 111
//const val SEALED_KEYWORD_Id: Int = 112
//const val ABSTRACT_KEYWORD_Id: Int = 113
//const val ENUM_KEYWORD_Id: Int = 114
//const val SPAWN_KEYWORD_Id: Int = 115
//const val OPEN_KEYWORD_Id: Int = 116
//const val SYNCHRONIZED_KEYWORD_Id: Int = 130
//const val STRUCT_KEYWORD_Id: Int = 117
//
//const val OVERRIDE_KEYWORD_Id: Int = 118
//const val PRIVATE_KEYWORD_Id: Int = 119
//const val PUBLIC_KEYWORD_Id: Int = 120
//const val STATIC_KEYWORD_Id: Int = 121
//
//const val PROTECTED_KEYWORD_Id: Int = 122
//const val CATCH_KEYWORD_Id: Int = 123
//
//
//const val TYPE_KEYWORD_Id: Int = 125
//
//const val COMPOSITION_Id: Int = 126
//
//const val SAFE_ACCESS_Id: Int = 127
//const val SAFE_INDEXEX_Id: Int = 128
//const val SAFE_CALL_Id: Int = 129
//const val SAFE_LAMBDA_Id: Int = 142
//
//const val FINALLY_KEYWORD_Id: Int = 130
//const val REDEF_KEYWORD_Id: Int = 131
//const val FOREIGN_KEYWORD_Id: Int = 132
//const val UNSAFE_KEYWORD_Id: Int = 133
//const val OPERATOR_KEYWORD_Id: Int = 141
//const val INTERNAL_KEYWORD_Id: Int = 142
//const val QUOTE_KEYWORD_Id: Int = 143
//const val HASH_Id: Int = 94
//const val MAIN_KEYWORD_Id: Int = 149
//
//const val INT8_Id: Int = 150
//const val INT16_Id: Int = 151
//const val INT32_Id: Int = 152
//const val INT64_Id: Int = 153
//const val UINT8_Id: Int = 154
//const val UINT16_Id: Int = 155
//const val UINT32_Id: Int = 156
//const val UINT64_Id: Int = 157
//const val FLOAT32_Id: Int = 158
//const val FLOAT64_Id: Int = 159
//const val BOOL_Id: Int = 160
//const val CHAR_Id: Int = 161
//const val UNIT_Id: Int = 162
//
//const val LTCOLON_Id: Int = 163
//
//
//const val MUT_KEYWORD_Id: Int = 165
//const val PROP_KEYWORD_Id: Int = 166
//
//const val UNDERLINE_Id: Int = 167
//
//const val OREQ_Id: Int = 168
//const val ANDEQ_Id: Int = 169
//const val XOREQ_Id: Int = 170
//const val LTLT_Id: Int = 171
//const val GTGT_Id: Int = 172
//const val LTLTEQ_Id: Int = 173
//const val GTGTEQ_Id: Int = 174
//
//const val MULMUL_Id: Int = 175
//const val MUL_MUL_EQ_Id: Int = 176
//const val OROREQ_Id: Int = 177
//const val ELLIPSIS_Id: Int = 178
//const val BACKSLASH_Id: Int = 179
//const val QUOTESYMBOL_Id: Int = 180
//const val DOLLAR_Id: Int = 181
//const val FLOAT16_Id: Int = 182
//const val INTNATIVE_Id: Int = 183
//const val UINTNATIVE_Id: Int = 184
//const val NOTHING_Id: Int = 185
//
//const val MACRO_KEYWORD_Id: Int = 186
//
//
//const val ESCAPE_LPAR_Id: Int = 187
//const val ESCAPE_RPAR_Id: Int = 187
//const val ESCAPE_DOLLAR_Id: Int = 187
//const val ESCAPE_LBRACKET_Id: Int = 188
//const val ESCAPE_RBRACKET_Id: Int = 189
//
//const val VARRAY_Id: Int = 190
//
//const val OPERATION_INVOKE_Id: Int = 191
//const val OPERATION_GET_Id: Int = 192
//
//const val OPERATION_EQUALS_Id: Int = 193
//const val OPERATION_TIMES_Id: Int = 194
//const val OPERATION_DIV_Id: Int = 195
//const val OPERATION_REM_Id: Int = 196
//const val OPERATION_MINUS_Id: Int = 197
//const val OPERATION_PLUS_Id: Int = 198
//const val OPERATION_LEFT_SHIFT_Id: Int = 199
//const val OPERATION_RIGHT_SHIFT_Id: Int = 200
//const val OPERATION_COMPARE_GT_Id: Int = 201
//const val OPERATION_COMPARE_LTEQ_Id: Int = 202
//const val OPERATION_COMPARE_LT_Id: Int = 203
//const val OPERATION_COMPARE_GTEQ_Id: Int = 204
//const val OPERATION_AND_Id: Int = 205
//const val OPERATION_XOR_Id: Int = 206
//const val OPERATION_OR_Id: Int = 207
//const val OPERATION_NOT_Id: Int = 208
//const val OPERATION_NOT_EQUALS_Id: Int = 209
//const val OPERATION_EXPONENTIATION_Id: Int = 210
//
//const val THIS_KEYWORD_UPPER_Id: Int = 211
//
//const val FILE_KEYWORD_Id: Int = 212
//
//const val VARARG_KEYWORD_Id: Int = 213
//
//
//val DOC_COMMENT: com.intellij.psi.tree.IElementType = cn.cangnova.cangjie.psi.cdoc.lexer.CDocTokens.CDOC
//val WHITE_SPACE: com.intellij.psi.tree.IElementType = com.intellij.psi.TokenType.WHITE_SPACE
//
//
//val HASH: cn.cangnova.cangjie.lexer.CjSingleValueToken =
//    cn.cangnova.cangjie.lexer.CjSingleValueToken("HASH", "#", HASH_Id)
//val QUOTESYMBOL: cn.cangnova.cangjie.lexer.CjSingleValueToken =
//    cn.cangnova.cangjie.lexer.CjSingleValueToken("QUOTESYMBOL", "`", QUOTESYMBOL_Id)
//val DOLLAR: cn.cangnova.cangjie.lexer.CjSingleValueToken =
//    cn.cangnova.cangjie.lexer.CjSingleValueToken("DOLLAR", "$", DOLLAR_Id)
//val FILE_KEYWORD: cn.cangnova.cangjie.lexer.CjKeywordToken =
//    cn.cangnova.cangjie.lexer.CjKeywordToken.softKeyword("file", FILE_KEYWORD_Id)
//
//
//val BLOCK_COMMENT: cn.cangnova.cangjie.lexer.CjToken =
//    cn.cangnova.cangjie.lexer.CjToken("BLOCK_COMMENT", BLOCK_COMMENT_Id)
//val EOL_COMMENT: cn.cangnova.cangjie.lexer.CjToken =
//    cn.cangnova.cangjie.lexer.CjToken("EOL_COMMENT", EOL_COMMENT_Id)
//val SHEBANG_COMMENT: cn.cangnova.cangjie.lexer.CjToken =
//    cn.cangnova.cangjie.lexer.CjToken("SHEBANG_COMMENT", SHEBANG_COMMENT_Id)
//val VARARG_KEYWORD: cn.cangnova.cangjie.lexer.CjModifierKeywordToken =
//    cn.cangnova.cangjie.lexer.CjModifierKeywordToken.softKeywordModifier("vararg", VARARG_KEYWORD_Id)
//
//val INTEGER_LITERAL: cn.cangnova.cangjie.lexer.CjToken =
//    cn.cangnova.cangjie.lexer.CjToken("INTEGER_LITERAL", INTEGER_LITERAL_Id)
//val FLOAT_LITERAL: cn.cangnova.cangjie.lexer.CjToken =
//    cn.cangnova.cangjie.lexer.CjToken("FLOAT_LITERAL", FLOAT_LITERAL_Id)
//val UNIT_LITERAL: cn.cangnova.cangjie.lexer.CjToken =
//    cn.cangnova.cangjie.lexer.CjToken("UNIT_LITERAL", UNIT_LITERAL_Id)
//val TUPLE_LITERAL: cn.cangnova.cangjie.lexer.CjToken =
//    cn.cangnova.cangjie.lexer.CjToken("TUPLE_LITERAL", TUPLE_LITERAL_Id)
//val RUNE_LITERAL: cn.cangnova.cangjie.lexer.CjToken =
//    cn.cangnova.cangjie.lexer.CjToken("RUNE_LITERAL", RUNE_LITERAL_Id)
//val CHARACTER_BYTE_LITERAL: cn.cangnova.cangjie.lexer.CjToken =
//    cn.cangnova.cangjie.lexer.CjToken("CHARACTER_BYTE_LITERAL", CHARACTER_BYTE_LITERAL_Id)
//val SHOP_STRING: cn.cangnova.cangjie.lexer.CjToken =
//    cn.cangnova.cangjie.lexer.CjToken("SHOP_STRING", SHOP_STRING_Id)
//val CLOSING_QUOTE: cn.cangnova.cangjie.lexer.CjToken =
//    cn.cangnova.cangjie.lexer.CjToken("CLOSING_QUOTE", CLOSING_QUOTE_Id)
//val OPEN_QUOTE: CjToken = CjToken("OPEN_QUOTE", OPEN_QUOTE_Id)
//val REGULAR_STRING_PART: CjToken = CjToken("REGULAR_STRING_PART", REGULAR_STRING_PART_Id)
//
//
//val ESCAPE_SEQUENCE: CjToken = CjToken("ESCAPE_SEQUENCE", ESCAPE_SEQUENCE_Id)
//val SHORT_TEMPLATE_ENTRY_START: CjToken = CjToken("SHORT_TEMPLATE_ENTRY_START", SHORT_TEMPLATE_ENTRY_START_Id)
//val LONG_TEMPLATE_ENTRY_START: CjToken = CjToken("LONG_TEMPLATE_ENTRY_START", LONG_TEMPLATE_ENTRY_START_Id)
//val LONG_TEMPLATE_ENTRY_END: CjToken = CjToken("LONG_TEMPLATE_ENTRY_END", LONG_TEMPLATE_ENTRY_END_Id)
//val DANGLING_NEWLINE: CjToken = CjToken("DANGLING_NEWLINE", DANGLING_NEWLINE_Id)
//val PACKAGE_KEYWORD: CjKeywordToken = CjKeywordToken.keyword("package", PACKAGE_KEYWORD_Id)
//val AS_KEYWORD: CjKeywordToken = CjKeywordToken.keyword("as", AS_KEYWORD_Id)
//val CLASS_KEYWORD: CjKeywordToken = CjKeywordToken.keyword("class", CLASS_KEYWORD_Id)
//val EXTEND_KEYWORD: CjKeywordToken = CjKeywordToken.keyword("extend", EXTEND_KEYWORD_Id)
//val ENUM_KEYWORD: CjKeywordToken = CjKeywordToken.keyword("enum", ENUM_KEYWORD_Id)
//val STRUCT_KEYWORD: CjKeywordToken = CjKeywordToken.keyword("struct", STRUCT_KEYWORD_Id)
//val THIS_KEYWORD: CjKeywordToken = CjKeywordToken.keyword("this", THIS_KEYWORD_Id)
//val THIS_KEYWORD_UPPER: CjKeywordToken = CjKeywordToken.keyword("This", THIS_KEYWORD_UPPER_Id)
//
//val SUPER_KEYWORD: CjKeywordToken = CjKeywordToken.keyword("super", SUPER_KEYWORD_Id)
//val LET_KEYWORD: CjKeywordToken = CjKeywordToken.keyword("let", LET_KEYWORD_Id)
//val VAR_KEYWORD: CjKeywordToken = CjKeywordToken.keyword("var", VAR_KEYWORD_Id)
//val CONST_KEYWORD: CjKeywordToken = CjKeywordToken.keyword("const", CONST_KEYWORD_Id)
//val SPAWN_KEYWORD: CjKeywordToken = CjKeywordToken.keyword("spawn", SPAWN_KEYWORD_Id)
//val SYNCHRONIZED_KEYWORD: CjKeywordToken = CjKeywordToken.keyword("synchronized", SYNCHRONIZED_KEYWORD_Id)
//val MAIN_KEYWORD: CjKeywordToken = CjKeywordToken.keyword("main", MAIN_KEYWORD_Id)
//val FUNC_KEYWORD: CjKeywordToken = CjKeywordToken.keyword("func", FUNC_KEYWORD_Id)
//val FOR_KEYWORD: CjKeywordToken = CjKeywordToken.keyword("for", FOR_KEYWORD_Id)
//val TRUE_KEYWORD: CjKeywordToken = CjKeywordToken.keyword("true", TRUE_KEYWORD_Id)
//val FALSE_KEYWORD: CjKeywordToken = CjKeywordToken.keyword("false", FALSE_KEYWORD_Id)
//val IS_KEYWORD: CjKeywordToken = CjKeywordToken.keyword("is", IS_KEYWORD_Id)
//val IN_KEYWORD: CjModifierKeywordToken = CjModifierKeywordToken.keywordModifier("in", IN_KEYWORD_Id)
//val THROW_KEYWORD: CjKeywordToken = CjKeywordToken.keyword("throw", THROW_KEYWORD_Id)
//val RETURN_KEYWORD: CjKeywordToken = CjKeywordToken.keyword("return", RETURN_KEYWORD_Id)
//val BREAK_KEYWORD: CjKeywordToken = CjKeywordToken.keyword("break", BREAK_KEYWORD_Id)
//val CONTINUE_KEYWORD: CjKeywordToken = CjKeywordToken.keyword("continue", CONTINUE_KEYWORD_Id)
//val IF_KEYWORD: CjKeywordToken = CjKeywordToken.keyword("if", IF_KEYWORD_Id)
//val TRY_KEYWORD: CjKeywordToken = CjKeywordToken.keyword("try", TRY_KEYWORD_Id)
//val ELSE_KEYWORD: CjKeywordToken = CjKeywordToken.keyword("else", ELSE_KEYWORD_Id)
//val WHILE_KEYWORD: CjKeywordToken = CjKeywordToken.keyword("while", WHILE_KEYWORD_Id)
//val DO_KEYWORD: CjKeywordToken = CjKeywordToken.keyword("do", DO_KEYWORD_Id)
//val MATCH_KEYWORD: CjKeywordToken = CjKeywordToken.keyword("match", MATCH_KEYWORD_Id)
//val CASE_KEYWORD: CjKeywordToken = CjKeywordToken.keyword("case", CASE_KEYWORD_Id)
//val INTERFACE_KEYWORD: CjKeywordToken = CjKeywordToken.keyword("interface", INTERFACE_KEYWORD_Id)
//
////    CjToken AS_SAFE = CjKeywordToken.keyword("AS_SAFE", IMPORT_KEYWORD_Id);
//val UNDERLINE: CjSingleValueToken = CjSingleValueToken("UNDERLINE", "_", UNDERLINE_Id)
//val TYPEOF_KEYWORD: CjKeywordToken = CjKeywordToken.keyword("typeof", TYPEOF_KEYWORD_Id)
//val IDENTIFIER: CjToken = CjToken("IDENTIFIER", IDENTIFIER_Id)
//val FIELD_IDENTIFIER: CjToken = CjToken("FIELD_IDENTIFIER", FIELD_IDENTIFIER_Id)
//val LBRACKET: CjSingleValueToken = CjSingleValueToken("LBRACKET", "[", LBRACKET_Id)
//val RBRACKET: CjSingleValueToken = CjSingleValueToken("RBRACKET", "]", RBRACKET_Id)
//val LBRACE: CjSingleValueToken = CjSingleValueToken("LBRACE", "{", LBRACE_Id)
//val RBRACE: CjSingleValueToken = CjSingleValueToken("RBRACE", "}", RBRACE_Id)
//val LPAR: CjSingleValueToken = CjSingleValueToken("LPAR", "(", LPAR_Id)
//val RPAR: CjSingleValueToken = CjSingleValueToken("RPAR", ")", RPAR_Id)
//val ESCAPE_LPAR: CjSingleValueToken = CjSingleValueToken("ESCAPE_LPAR", "\\(", ESCAPE_LPAR_Id)
//val ESCAPE_RPAR: CjSingleValueToken = CjSingleValueToken("ESCAPE_RPAR", "\\)", ESCAPE_RPAR_Id)
//val ESCAPE_DOLLAR: CjSingleValueToken = CjSingleValueToken("ESCAPE_DOLLAR", "\\$", ESCAPE_DOLLAR_Id)
//val ESCAPE_LBRACKET: CjSingleValueToken = CjSingleValueToken("ESCAPE_LBRACKET", "\\]", ESCAPE_LBRACKET_Id)
//val ESCAPE_RBRACKET: CjSingleValueToken = CjSingleValueToken("ESCAPE_RBRACKET", "\\[", ESCAPE_RBRACKET_Id)
//val DOT: CjSingleValueToken = CjSingleValueToken("DOT", ".", DOT_Id)
//val PLUSPLUS: CjSingleValueToken = CjSingleValueToken("PLUSPLUS", "++", PLUSPLUS_Id)
//val MINUSMINUS: CjSingleValueToken = CjSingleValueToken("MINUSMINUS", "--", MINUSMINUS_Id)
//val MUL: CjSingleValueToken = CjSingleValueToken("MUL", "*", MUL_Id)
//val MULMUL: CjSingleValueToken = CjSingleValueToken("MULMUL", "**", MULMUL_Id)
//val MULMULEQ: CjSingleValueToken = CjSingleValueToken("MULMULEQ", "**=", MUL_MUL_EQ_Id)
//val PLUS: CjSingleValueToken = CjSingleValueToken("PLUS", "+", PLUS_Id)
//val MINUS: CjSingleValueToken = CjSingleValueToken("MINUS", "-", MINUS_Id)
//val EXCL: CjSingleValueToken = CjSingleValueToken("EXCL", "!", EXCL_Id)
//val DIV: CjSingleValueToken = CjSingleValueToken("DIV", "/", DIV_Id)
//val PERC: CjSingleValueToken = CjSingleValueToken("PERC", "%", PERC_Id)
//val LT: CjSingleValueToken = CjSingleValueToken("LT", "<", LT_Id)
//val GT: CjSingleValueToken = CjSingleValueToken("GT", ">", GT_Id)
//val BRACE_PAIRS = arrayOf<BracePair>(
//    BracePair(LPAR, RPAR, true),
//    BracePair(LBRACE, RBRACE, true),
//    BracePair(LBRACKET, RBRACKET, true),
//    BracePair(LT, GT, true),
//
//    BracePair(LONG_TEMPLATE_ENTRY_START, LONG_TEMPLATE_ENTRY_END, false),
//
//    )
//val LTEQ: CjSingleValueToken = CjSingleValueToken("LTEQ", "<=", LTEQ_Id)
//val AT: CjSingleValueToken = CjSingleValueToken("AT", "@", AT_Id)
//val GTEQ: CjSingleValueToken = CjSingleValueToken("GTEQ", ">=", GTEQ_Id)
//
//val LTCOLON: CjSingleValueToken = CjSingleValueToken("LT_COLON", "<:", LTCOLON_Id)
//val ARROW: CjSingleValueToken = CjSingleValueToken("ARROW", "->", ARROW_Id)
//val COMPOSITION: CjSingleValueToken = CjSingleValueToken("COMPOSITION", "~>", COMPOSITION_Id)
//val PIPELINE: CjSingleValueToken = CjSingleValueToken("PIPELINE", "|>", PIPELINE_Id)
//
////反向箭头composition
//val SAFE_ACCESS: CjSingleValueToken = CjSingleValueToken("SAFE_ACCESS", "?.", SAFE_ACCESS_Id)
//val SAFE_INDEXEX: CjSingleValueToken = CjSingleValueToken("SAFE_INDEXEX", "?[", SAFE_INDEXEX_Id)
//val SAFE_CALL: CjSingleValueToken = CjSingleValueToken("SAFE_CALL", "?(", SAFE_CALL_Id)
//val SAFE_LAMBDA: CjSingleValueToken = CjSingleValueToken("SAFE_LAMBDA", "?{", SAFE_LAMBDA_Id)
//val BACKSLASH: CjSingleValueToken = CjSingleValueToken("BACKSLASH", "\\", BACKSLASH_Id)
//val LEFT_ARROW: CjSingleValueToken = CjSingleValueToken("LEFT_ARROW", "<-", LEFT_ARROW_Id)
//val DOUBLE_ARROW: CjSingleValueToken = CjSingleValueToken("DOUBLE_ARROW", "=>", DOUBLE_ARROW_Id)
//val EQEQ: CjSingleValueToken = CjSingleValueToken("EQEQ", "==", EQEQ_Id)
//val EXCLEQ: CjSingleValueToken = CjSingleValueToken("EXCLEQ", "!=", EXCLEQ_Id)
//val ANDAND: CjSingleValueToken = CjSingleValueToken("ANDAND", "&&", ANDAND_Id)
//val AND: CjSingleValueToken = CjSingleValueToken("AND", "&", AND_Id)
//val TILDE: CjSingleValueToken = CjSingleValueToken("TILDE", "~", TILED_Id)
//
//val XOR: CjSingleValueToken = CjSingleValueToken("XOR", "^", XOR_Id)
//val OROR: CjSingleValueToken = CjSingleValueToken("OROR", "||", OROR_Id)
//val OR: CjSingleValueToken = CjSingleValueToken("OR", "|", OR_Id)
//val QUEST: CjSingleValueToken = CjSingleValueToken("QUEST", "?", QUEST_Id)
//val ELVIS: CjSingleValueToken = CjSingleValueToken("ELVIS", "?:", ELVIS_Id)
//val COALESCING: CjSingleValueToken = CjSingleValueToken("COALESCING", "??", COALESCING_Id)
//
//val COLON: CjSingleValueToken = CjSingleValueToken("COLON", ":", COLON_Id)
//val SEMICOLON: CjSingleValueToken = CjSingleValueToken("SEMICOLON", ";", SEMICOLON_Id)
//val DOUBLE_SEMICOLON: CjSingleValueToken = CjSingleValueToken("DOUBLE_SEMICOLON", ";;", DOUBLE_SEMICOLON_Id)
//val RANGE: CjSingleValueToken = CjSingleValueToken("RANGE", "..", RANGE_Id)
//val ELLIPSIS: CjSingleValueToken = CjSingleValueToken("ELLIPSIS", "...", ELLIPSIS_Id)
//val RANGEEQ: CjSingleValueToken = CjSingleValueToken("RANGEEQ", "..=", RANGEEQ_Id)
//val EQ: CjSingleValueToken = CjSingleValueToken("EQ", "=", EQ_Id)
//val MULTEQ: CjSingleValueToken = CjSingleValueToken("MULTEQ", "*=", MULTEQ_Id)
//val DIVEQ: CjSingleValueToken = CjSingleValueToken("DIVEQ", "/=", DIVEQ_Id)
//val PERCEQ: CjSingleValueToken = CjSingleValueToken("PERCEQ", "%=", PERCEQ_Id)
//val PLUSEQ: CjSingleValueToken = CjSingleValueToken("PLUSEQ", "+=", PLUSEQ_Id)
//val MINUSEQ: CjSingleValueToken = CjSingleValueToken("MINUSEQ", "-=", MINUSEQ_Id)
//
//val ANDANDEQ: CjSingleValueToken = CjSingleValueToken("ANDANDEQ", "&&=", ANDANDEQ_Id)
//val OREQ: CjSingleValueToken = CjSingleValueToken("OREQ", "|=", OREQ_Id)
//val OROREQ: CjSingleValueToken = CjSingleValueToken("OROREQ", "||=", OROREQ_Id)
//val ANDEQ: CjSingleValueToken = CjSingleValueToken("ANDEQ", "&=", ANDEQ_Id)
//val XOREQ: CjSingleValueToken = CjSingleValueToken("XOREQ", "^=", XOREQ_Id)
//
////    CjKeywordToken FILE_KEYWORD = CjKeywordToken.softKeyword("file", FILE_KEYWORD_Id);
//val LTLT: CjSingleValueToken = CjSingleValueToken("LTLT", "<<", LTLT_Id)
//val GTGT: CjSingleValueToken = CjSingleValueToken("GTGT", ">>", GTGT_Id)
//
////    操作重载
//val OPERATION_INVOKE: CjSingleValueToken = CjSingleValueToken("OPERATION_INVOKE", "()", OPERATION_INVOKE_Id)
//val OPERATION_GET: CjSingleValueToken = CjSingleValueToken("OPERATION_GET", "[]", OPERATION_GET_Id)
//val OPERATION_NOT: CjSingleValueToken = CjSingleValueToken("OPERATION_NOT", "!", OPERATION_NOT_Id)
//val OPERATION_NOT_EQUALS: CjSingleValueToken =
//    CjSingleValueToken("OPERATION_NOT_EQUALS", "!=", OPERATION_NOT_EQUALS_Id)
//
//// Exponentiation
//val OPERATION_EXPONENTIATION: CjSingleValueToken =
//    CjSingleValueToken("OPERATION_EXPONENTIATION", "**", OPERATION_EXPONENTIATION_Id)
//val OPERATION_EQUALS: CjSingleValueToken = CjSingleValueToken("OPERATION_EQUALS", "==", OPERATION_EQUALS_Id)
//val OPERATION_TIMES: CjSingleValueToken = CjSingleValueToken("OPERATION_TIMES", "*", OPERATION_TIMES_Id)
//val OPERATION_DIV: CjSingleValueToken = CjSingleValueToken("OPERATION_DIV", "/", OPERATION_DIV_Id)
//val OPERATION_REM: CjSingleValueToken = CjSingleValueToken("OPERATION_REM", "%", OPERATION_REM_Id)
//val OPERATION_MINUS: CjSingleValueToken = CjSingleValueToken("OPERATION_MINUS", "-", OPERATION_MINUS_Id)
//val OPERATION_PLUS: CjSingleValueToken = CjSingleValueToken("OPERATION_PLUS", "+", OPERATION_PLUS_Id)
//val OPERATION_LEFT_SHIFT: CjSingleValueToken =
//    CjSingleValueToken("OPERATION_LEFT_SHIFT", "<<", OPERATION_LEFT_SHIFT_Id)
//val OPERATION_RIGHT_SHIFT: CjSingleValueToken =
//    CjSingleValueToken("OPERATION_RIGHT_SHIFT", ">>", OPERATION_RIGHT_SHIFT_Id)
//val OPERATION_COMPARE_GT: CjSingleValueToken =
//    CjSingleValueToken("OPERATION_COMPARE_GT", ">", OPERATION_COMPARE_GT_Id)
//val OPERATION_COMPARE_LTEQ: CjSingleValueToken =
//    CjSingleValueToken("OPERATION_COMPARE_LTEQ", "<=", OPERATION_COMPARE_LTEQ_Id)
//val OPERATION_COMPARE_LT: CjSingleValueToken =
//    CjSingleValueToken("OPERATION_COMPARE_LT", "<", OPERATION_COMPARE_LT_Id)
//val OPERATION_COMPARE_GTEQ: CjSingleValueToken =
//    CjSingleValueToken("OPERATION_COMPARE_GTEQ", ">=", OPERATION_COMPARE_GTEQ_Id)
//val OPERATION_AND: CjSingleValueToken = CjSingleValueToken("OPERATION_AND", "&", OPERATION_AND_Id)
//val OPERATION_XOR: CjSingleValueToken = CjSingleValueToken("OPERATION_XOR", "^", OPERATION_XOR_Id)
//val OPERATION_OR: CjSingleValueToken = CjSingleValueToken("OPERATION_OR", "|", OPERATION_OR_Id)
//
//
//val LTLTEQ: CjSingleValueToken = CjSingleValueToken("LTLTEQ", "<<=", LTLTEQ_Id)
//val GTGTEQ: CjSingleValueToken = CjSingleValueToken("GTGTEQ", ">>=", GTGTEQ_Id)
//
//val COMMA: CjSingleValueToken = CjSingleValueToken("COMMA", ",", COMMA_Id)
//val EOL_OR_SEMICOLON: CjToken = CjToken("EOL_OR_SEMICOLON", EOL_OR_SEMICOLON_Id)
//val IMPORT_KEYWORD: CjKeywordToken = CjKeywordToken.keyword("import", IMPORT_KEYWORD_Id)
//val WHERE_KEYWORD: CjKeywordToken = CjKeywordToken.keyword("where", WHERE_KEYWORD_Id)
//
////    CjKeywordToken BY_KEYWORD = CjKeywordToken.softKeyword("by", BY_KEYWORD_Id);
//val GET_KEYWORD: CjKeywordToken = CjKeywordToken.softKeyword("get", GET_KEYWORD_Id)
//val SET_KEYWORD: CjKeywordToken = CjKeywordToken.softKeyword("set", SET_KEYWORD_Id)
//val INIT_KEYWORD: CjKeywordToken = CjKeywordToken.keyword("init", INIT_KEYWORD_Id)
//val PROP_KEYWORD: CjKeywordToken = CjKeywordToken.keyword("prop", PROP_KEYWORD_Id)
//val INT8_KEYWORD: CjKeywordToken = CjKeywordToken.keyword("Int8", INT8_Id)
//val INT16_KEYWORD: CjKeywordToken = CjKeywordToken.keyword("Int16", INT16_Id)
//val INT32_KEYWORD: CjKeywordToken = CjKeywordToken.keyword("Int32", INT32_Id)
//val INT64_KEYWORD: CjKeywordToken = CjKeywordToken.keyword("Int64", INT64_Id)
//val UINT8_KEYWORD: CjKeywordToken = CjKeywordToken.keyword("UInt8", UINT8_Id)
//val UINT16_KEYWORD: CjKeywordToken = CjKeywordToken.keyword("UInt16", UINT16_Id)
//val UINT32_KEYWORD: CjKeywordToken = CjKeywordToken.keyword("UInt32", UINT32_Id)
//val UINT64_KEYWORD: CjKeywordToken = CjKeywordToken.keyword("UInt64", UINT64_Id)
//val FLOAT32_KEYWORD: CjKeywordToken = CjKeywordToken.keyword("Float32", FLOAT32_Id)
//val FLOAT64_KEYWORD: CjKeywordToken = CjKeywordToken.keyword("Float64", FLOAT64_Id)
//val FLOAT16_KEYWORD: CjKeywordToken = CjKeywordToken.keyword("Float16", FLOAT16_Id)
//val INTNATIVE_KEYWORD: CjKeywordToken = CjKeywordToken.keyword("IntNative", INTNATIVE_Id)
//val UINTNATIVE_KEYWORD: CjKeywordToken = CjKeywordToken.keyword("UIntNative", UINTNATIVE_Id)
//val NOTHING_KEYWORD: CjKeywordToken = CjKeywordToken.keyword("Nothing", NOTHING_Id)
//val BOOL_KEYWORD: CjKeywordToken = CjKeywordToken.keyword("Bool", BOOL_Id)
//val RUNE_KEYWORD: CjKeywordToken = CjKeywordToken.keyword("Rune", CHAR_Id)
//val UNIT_KEYWORD: CjKeywordToken = CjKeywordToken.keyword("Unit", UNIT_Id)
//val VARRAY_KEYWORD: CjKeywordToken = CjKeywordToken.keyword("VArray", VARRAY_Id)
//
//val SEALED_KEYWORD: CjModifierKeywordToken =
//    CjModifierKeywordToken.softKeywordModifier("sealed", SEALED_KEYWORD_Id)
//val ABSTRACT_KEYWORD: CjModifierKeywordToken =
//    CjModifierKeywordToken.softKeywordModifier("abstract", ABSTRACT_KEYWORD_Id)
//val OPEN_KEYWORD: CjModifierKeywordToken = CjModifierKeywordToken.softKeywordModifier("open", OPEN_KEYWORD_Id)
//
//val OVERRIDE_KEYWORD: CjModifierKeywordToken =
//    CjModifierKeywordToken.keywordModifier("override", OVERRIDE_KEYWORD_Id)
//val PRIVATE_KEYWORD: CjModifierKeywordToken =
//    CjModifierKeywordToken.keywordModifier("private", PRIVATE_KEYWORD_Id)
//val PUBLIC_KEYWORD: CjModifierKeywordToken = CjModifierKeywordToken.keywordModifier("public", PUBLIC_KEYWORD_Id)
//val STATIC_KEYWORD: CjModifierKeywordToken = CjModifierKeywordToken.keywordModifier("static", STATIC_KEYWORD_Id)
//val INTERNAL_KEYWORD: CjModifierKeywordToken =
//    CjModifierKeywordToken.keywordModifier("internal", INTERNAL_KEYWORD_Id)
//val PROTECTED_KEYWORD: CjModifierKeywordToken =
//    CjModifierKeywordToken.keywordModifier("protected", PROTECTED_KEYWORD_Id)
//
////    Class修饰符
//val CLASS_MODIFIER_KEYWORDS_ARRAY = arrayOf<CjKeywordToken>(
//    OPEN_KEYWORD,
//    SEALED_KEYWORD,
//    ABSTRACT_KEYWORD,
//    PUBLIC_KEYWORD,
//    PRIVATE_KEYWORD,
//    PROTECTED_KEYWORD
//)
//
////    接口修饰符
//val INTERFACE_MODIFIER_KEYWORDS_ARRAY = arrayOf<CjKeywordToken>(
//    OPEN_KEYWORD,
//    SEALED_KEYWORD,
//    PUBLIC_KEYWORD,
//    PRIVATE_KEYWORD,
//    PROTECTED_KEYWORD
//)
//
////    结构体修饰符
//val STRUCT_MODIFIER_KEYWORDS_ARRAY = arrayOf<CjKeywordToken>(
//    PUBLIC_KEYWORD,
//    PRIVATE_KEYWORD,
//    PROTECTED_KEYWORD
//)
//val MACRO_KEYWORD: CjKeywordToken = CjKeywordToken.keyword("macro", MACRO_KEYWORD_Id)
//val CATCH_KEYWORD: CjKeywordToken = CjKeywordToken.keyword("catch", CATCH_KEYWORD_Id)
//val FINALLY_KEYWORD: CjKeywordToken = CjKeywordToken.keyword("finally", FINALLY_KEYWORD_Id)
//val REDEF_KEYWORD: CjModifierKeywordToken = CjModifierKeywordToken.keywordModifier("redef", REDEF_KEYWORD_Id)
//val QUOTE_KEYWORD: CjKeywordToken = CjKeywordToken.keyword("quote", QUOTE_KEYWORD_Id)
//
////    特殊修饰符
//val FOREIGN_KEYWORD: CjKeywordToken = CjKeywordToken.keyword("foreign", FOREIGN_KEYWORD_Id)
//val TYPE_KEYWORD: CjKeywordToken = CjKeywordToken.keyword("type", TYPE_KEYWORD_Id)
//val UNSAFE_KEYWORD: CjKeywordToken = CjKeywordToken.keyword("unsafe", UNSAFE_KEYWORD_Id)
//val SPECIAL_MODIFIER_KEYWORDS_ARRAY = arrayOf<CjKeywordToken>(
//    CONST_KEYWORD,
//    FOREIGN_KEYWORD,
//    UNSAFE_KEYWORD,
//)
//
//
//val SPECIAL_MODIFIER_KEYWORDS: TokenSet = TokenSet.create(*SPECIAL_MODIFIER_KEYWORDS_ARRAY)
//
////   全局函数修饰符
//val FUNC_GLOBAL_MODIFIER_KEYWORDS_ARRAY = arrayOf<CjKeywordToken>(
//    PUBLIC_KEYWORD,
//    FOREIGN_KEYWORD,
//    UNSAFE_KEYWORD,
//    CONST_KEYWORD
//)
//val MUT_KEYWORD: CjModifierKeywordToken = CjModifierKeywordToken.keywordModifier("mut", MUT_KEYWORD_Id)
//val OPERATOR_KEYWORD: CjModifierKeywordToken =
//    CjModifierKeywordToken.keywordModifier("operator", OPERATOR_KEYWORD_Id)
//
///*。
//此数组用于存根序列化：
//1.请勿更改顺序。
//2.如果添加条目或变更单，请增加存根版本。
//*/
//val MODIFIER_KEYWORDS_ARRAY = arrayOf<CjModifierKeywordToken>(
//    ABSTRACT_KEYWORD, OPEN_KEYWORD, OVERRIDE_KEYWORD, PRIVATE_KEYWORD,
//    PUBLIC_KEYWORD, PROTECTED_KEYWORD, INTERNAL_KEYWORD,
//    STATIC_KEYWORD,
//    MUT_KEYWORD,
//    OPERATOR_KEYWORD,
//    SEALED_KEYWORD,  //                    CONST_KEYWORD,
//    //                    FOREIGN_KEYWORD,
//    //                    UNSAFE_KEYWORD,
//    REDEF_KEYWORD
//)
//
//val MODIFIER_KEYWORDS: TokenSet = TokenSet.create(*MODIFIER_KEYWORDS_ARRAY)
//
////    TokenSet MODIFIER_KEYWORDS = TokenSet.andSet(
////            TokenSet.create(MODIFIER_KEYWORDS_ARRAY  ),TokenSet.create(CONST_KEYWORD)
////    );
////    类成员函数修饰符
//val FUNC_CLASSMEMBER_MODIFIER_KEYWORDS_ARRAY = arrayOf<CjKeywordToken>(
//    OVERRIDE_KEYWORD,
//
//    OPERATOR_KEYWORD,
//    STATIC_KEYWORD,
//    ABSTRACT_KEYWORD,
//
//    PUBLIC_KEYWORD,
//    PRIVATE_KEYWORD,
//    PROTECTED_KEYWORD,
//    CONST_KEYWORD,
//    OPEN_KEYWORD,
//    UNSAFE_KEYWORD,
//
//    REDEF_KEYWORD
//)
//
////    enum成员函数
//val FUNC_ENUMMEMBER_MODIFIER_KEYWORDS_ARRAY = arrayOf<CjKeywordToken>(
//    PUBLIC_KEYWORD,
//    PRIVATE_KEYWORD,
//    STATIC_KEYWORD,
//    OVERRIDE_KEYWORD,
//    UNSAFE_KEYWORD,
//    OPERATOR_KEYWORD,
//    REDEF_KEYWORD
//)
//
////    结构体成员函数
//val FUNC_STRUCTMEMBER_MODIFIER_KEYWORDS_ARRAY = arrayOf<CjKeywordToken>(
//    OVERRIDE_KEYWORD,
//    OPERATOR_KEYWORD,
//    STATIC_KEYWORD,
//
//    PUBLIC_KEYWORD,
//    PRIVATE_KEYWORD,
//
//
//    CONST_KEYWORD,
//
//    UNSAFE_KEYWORD,
//    REDEF_KEYWORD,
//    MUT_KEYWORD
//
//)
//
////    扩展修饰符
//val EXTEND_MODIFIER_KEYWORDS_ARRAY = arrayOf<CjKeywordToken?>()
//
////    List<BracePair>  BRACE_PAIR_LIST = Arrays.asList(
////            new BracePair(LPAR, RPAR,true),
////            new BracePair(LBRACE, RBRACE,true),
////            new BracePair(LBRACKET, RBRACKET,true)
////    );
////字面常量规则，包括整型、浮点型、字符型、布尔型、字符串等类型的基本值
//val LITERAL_CONSTANT: TokenSet = TokenSet.create(
//    CjNodeTypes.STRING_TEMPLATE,
//    CjNodeTypes.RUNE_CONSTANT,
//    CjNodeTypes.UNIT_CONSTANT,
//    CjNodeTypes.BOOLEAN_CONSTANT,
//    CjNodeTypes.FLOAT_CONSTANT,
//    CjNodeTypes.INTEGER_CONSTANT,
//    CjNodeTypes.CHARACTER_BYTE_CONSTANT
//)
//
//
//val SOFT_KEYWORDS: TokenSet = TokenSet.create(
//    GET_KEYWORD,
//    SET_KEYWORD, OPEN_KEYWORD,
//    ABSTRACT_KEYWORD,
//    SEALED_KEYWORD
//
//)
//val MODALITY_MODIFIERS: TokenSet = TokenSet.create(ABSTRACT_KEYWORD, SEALED_KEYWORD, OPEN_KEYWORD)
//
////基本类型
//val BASICTYPES: TokenSet = TokenSet.create(
//    INTNATIVE_KEYWORD,
//    INT8_KEYWORD,
//    INT16_KEYWORD,
//    INT32_KEYWORD,
//    INT64_KEYWORD,
//    UINTNATIVE_KEYWORD,
//    UINT8_KEYWORD,
//    UINT16_KEYWORD,
//    UINT32_KEYWORD,
//    UINT64_KEYWORD,
//    FLOAT16_KEYWORD,
//    FLOAT32_KEYWORD,
//    FLOAT64_KEYWORD,
//    NOTHING_KEYWORD,
//
//    VARRAY_KEYWORD,
//
//    BOOL_KEYWORD,
//    RUNE_KEYWORD,
//    UNIT_KEYWORD
//)
//val KEYWORDS: TokenSet = TokenSet.orSet(
//    TokenSet.create(
//        PACKAGE_KEYWORD,
//        AS_KEYWORD,
//        CLASS_KEYWORD,
//        INTERFACE_KEYWORD,
//        THIS_KEYWORD_UPPER,
//        THIS_KEYWORD,
//        SUPER_KEYWORD,
//        LET_KEYWORD,
//        VAR_KEYWORD,
//        CONST_KEYWORD,
//        FUNC_KEYWORD,
//        FOR_KEYWORD,
//        MAIN_KEYWORD,
//        STRUCT_KEYWORD,
//        EXTEND_KEYWORD,
//        TRUE_KEYWORD,
//        FALSE_KEYWORD,
//        IS_KEYWORD,
//        IN_KEYWORD,
//        THROW_KEYWORD,
//        RETURN_KEYWORD,
//        BREAK_KEYWORD,
//        CONTINUE_KEYWORD,
//        IF_KEYWORD,
//        ELSE_KEYWORD,
//        WHILE_KEYWORD,
//        DO_KEYWORD,
//        TRY_KEYWORD,
//        MATCH_KEYWORD,
//        CASE_KEYWORD,
//        TYPEOF_KEYWORD,
//        MACRO_KEYWORD,
//        PROP_KEYWORD,
//        ENUM_KEYWORD,
//        WHERE_KEYWORD,
//
//
//        IMPORT_KEYWORD,
//        OVERRIDE_KEYWORD,
//        PRIVATE_KEYWORD,
//        PUBLIC_KEYWORD,
//        PROTECTED_KEYWORD,
//        INTERNAL_KEYWORD,
//        CATCH_KEYWORD,
//        FINALLY_KEYWORD,
//        INIT_KEYWORD,
//        STATIC_KEYWORD,
//        REDEF_KEYWORD,
//
//        MUT_KEYWORD,
//        OPERATOR_KEYWORD,  //            INT8_KEYWORD, INT16_KEYWORD, INT32_KEYWORD, INT64_KEYWORD, UINT8_KEYWORD, UINT16_KEYWORD, UINT32_KEYWORD, UINT64_KEYWORD, FLOAT32_KEYWORD, FLOAT64_KEYWORD, BOOL_KEYWORD, CHAR_KEYWORD, UNIT_KEYWORD
//
//        TYPE_KEYWORD,
//        SPAWN_KEYWORD,
//        SYNCHRONIZED_KEYWORD,
//        FOREIGN_KEYWORD,
//        UNSAFE_KEYWORD
//
//
//    ),
//    BASICTYPES
//)
//val KEYWORDALL: TokenSet = TokenSet.orSet(KEYWORDS, SOFT_KEYWORDS)
//val OPERATIONS: TokenSet = TokenSet.create(
//    AS_KEYWORD, IS_KEYWORD, DOT, PLUSPLUS, MINUSMINUS, MUL, MULMUL, PLUS,
//    MINUS, EXCL, DIV, PERC, LT, GT, LTEQ, GTEQ, EQEQ, EXCLEQ, ANDAND, OROR, MULMULEQ,
//
//    RANGE, RANGEEQ, EQ, MULTEQ, DIVEQ, PERCEQ, PLUSEQ, MINUSEQ,
//    COALESCING, SAFE_ACCESS,
//    AND, OR, XOR,
//    ANDEQ, OREQ, XOREQ, ANDANDEQ, OROREQ,
//    LTLT, GTGT, LTLTEQ, GTGTEQ,
//
//    COMPOSITION, PIPELINE
//)
//
//
//val VISIBILITY_MODIFIERS: TokenSet =
//    TokenSet.create(PRIVATE_KEYWORD, PUBLIC_KEYWORD, INTERNAL_KEYWORD, PROTECTED_KEYWORD)
//
////可以被重载的运算符
//val OPERATIONS_CAN_BE_OVERLOADED: TokenSet = TokenSet.create(
//    OPERATION_INVOKE,
//    OPERATION_GET,
//    OPERATION_NOT,
//    OPERATION_NOT_EQUALS,
//    OPERATION_EXPONENTIATION,
//    OPERATION_EQUALS,
//    OPERATION_TIMES,
//    OPERATION_DIV,
//    OPERATION_REM,
//    OPERATION_MINUS,
//    OPERATION_PLUS,
//    OPERATION_LEFT_SHIFT,
//    OPERATION_RIGHT_SHIFT,
//    OPERATION_COMPARE_GT,
//    OPERATION_COMPARE_LTEQ,
//    OPERATION_COMPARE_LT,
//    OPERATION_COMPARE_GTEQ,
//    OPERATION_AND,
//    OPERATION_XOR,
//    OPERATION_OR
//)
//
////    Int类型默认支持的操作符
//val INT_SUPPORT_OPERATOR: TokenSet = TokenSet.create(
//    PLUS, DIV, MUL, MULMUL, MINUS, PERC
//)
//
////Float类型默认支持的操作符
//val FLOAT_SUPPORT_OPERATOR: TokenSet = TokenSet.create(
//    PLUS, DIV, MUL, MULMUL, MINUS
//)
//
////比较运算符  返回值Bool
//val COMPARISON_OPERATIONS: TokenSet = TokenSet.create(
//    LT, GT, LTEQ, GTEQ, EXCLEQ, EQEQ
//)
//
////    二进制运算符 返回值需要推断
//val BINARY_OPERATIONS: TokenSet = TokenSet.create(
//    PLUS, DIV, MUL, MULMUL, MINUS, PERC, GTGT, LTLT
//)
//
////    逻辑运算符
//val LOGICAL_OPERATORS: TokenSet = TokenSet.create(
//    ANDAND, OROR
//)
//
////    赋值运算符
//val ALL_ASSIGNMENTS: TokenSet = TokenSet.create(EQ, PLUSEQ, MINUSEQ, MULTEQ, PERCEQ, DIVEQ)
//
////复合赋值
//val AUGMENTED_ASSIGNMENTS: TokenSet = TokenSet.create(PLUSEQ, MINUSEQ, MULTEQ, PERCEQ, DIVEQ)
//
//val STRINGS: TokenSet = TokenSet.create(RUNE_LITERAL, REGULAR_STRING_PART)
//val COMMENTS: TokenSet = TokenSet.create(EOL_COMMENT, BLOCK_COMMENT, DOC_COMMENT, SHEBANG_COMMENT)
//
//val WHITESPACES: TokenSet = TokenSet.create(TokenType.WHITE_SPACE)
//val DEFAULT_VISIBILITY_KEYWORD: CjModifierKeywordToken = INTERNAL_KEYWORD
//
//val WHITE_SPACE_OR_COMMENT_BIT_SET: TokenSet = TokenSet.orSet(COMMENTS, WHITESPACES)
//val EOF: CjToken = CjToken("EOF", EOF_Id)
//
//
//
