package com.huawei.cangjie.lang.core.parser;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;

import static com.intellij.psi.TokenType.BAD_CHARACTER;
import static com.intellij.psi.TokenType.WHITE_SPACE;
import static com.huawei.cangjie.lang.core.psi.CjElementTypes.*;

%%

%{
  public _CangJieLexer() {
    this((java.io.Reader)null);
  }
%}

%public
%class _CangJieLexer
%implements FlexLexer
%function advance
%type IElementType
%unicode

EOL=\R
WHITE_SPACE=\s+


%%
<YYINITIAL> {
  {WHITE_SPACE}                 { return WHITE_SPACE; }

  "main"                        { return MAIN; }
  "func"                        { return FUNC; }
  "class"                       { return CLASS; }
  "var"                         { return VAR; }
  "let"                         { return LET; }
  "mut"                         { return MUT; }
  "prop"                        { return PROP; }
  "init"                        { return INIT; }
  "open"                        { return OPEN; }
  "from"                        { return FROM; }
  "import"                      { return IMPORT; }
  "as"                          { return AS; }
  "enum"                        { return ENUM; }
  "super"                       { return SUPER; }
  "this"                        { return THIS; }
  "interface"                   { return INTERFACE; }
  "static"                      { return STATIC; }
  "struct"                      { return STRUCT; }
  "return"                      { return RETURN; }
  "extend"                      { return EXTEND; }
  "true"                        { return TRUE; }
  "false"                       { return FALSE; }
  "Int8"                        { return INT8; }
  "Int16"                       { return INT16; }
  "Int32"                       { return INT32; }
  "Int64"                       { return INT64; }
  "Float32"                     { return FLOAT32; }
  "Float64"                     { return FLOAT64; }
  "Char"                        { return CHAE; }
  "UInt8"                       { return UINT8; }
  "UInt16"                      { return UINT16; }
  "UInt32"                      { return UINT32; }
  "UInt64"                      { return UINT64; }
  "Bool"                        { return BOOL; }
  "Unit"                        { return UNIT; }
  "if"                          { return IF; }
  "else"                        { return ELSE; }
  "while"                       { return WHILE; }
  "for"                         { return FOR; }
  "in"                          { return IN; }
  "do"                          { return DO; }
  "break"                       { return BREAK; }
  "continue"                    { return CONTINUE; }
  "match"                       { return MATCH; }
  "case"                        { return CASE; }
  "try"                         { return TRY; }
  "catch"                       { return CATCH; }
  "finally"                     { return FINALLY; }
  "throw"                       { return THROW; }
  "{"                           { return LBRACE; }
  "}"                           { return RBRACE; }
  "["                           { return LBRACK; }
  "]"                           { return RBRACK; }
  "("                           { return LPAREN; }
  ")"                           { return RPAREN; }
  "="                           { return EQ; }
  ":"                           { return COLON; }
  "=="                          { return EQEQ; }
  "!="                          { return NOTEQ; }
  ">"                           { return GT; }
  ">>"                          { return GTGT; }
  "<"                           { return LT; }
  "<<"                          { return LTLT; }
  ">="                          { return GTEQ; }
  "<="                          { return LTEQ; }
  "&&"                          { return ANDAND; }
  "&"                           { return AND; }
  "||"                          { return OROR; }
  "|"                           { return OR; }
  ","                           { return COMMA; }
  "."                           { return DOT; }
  ".."                          { return DOTDOT; }
  ";"                           { return SEMICOLON; }
  "shebang_line"                { return SHEBANG_LINE; }
  "identifier"                  { return IDENTIFIER; }
  "QUOTE_IDENTIFIER"            { return QUOTE_IDENTIFIER; }
  "unsafe"                      { return UNSAFE; }
  "STRING_LITERAL"              { return STRING_LITERAL; }
  "BYTE_STRING_LITERAL"         { return BYTE_STRING_LITERAL; }
  "CSTRING_LITERAL"             { return CSTRING_LITERAL; }
  "RAW_STRING_LITERAL"          { return RAW_STRING_LITERAL; }
  "RAW_BYTE_STRING_LITERAL"     { return RAW_BYTE_STRING_LITERAL; }
  "RAW_CSTRING_LITERAL"         { return RAW_CSTRING_LITERAL; }
  "CHAR_LITERAL"                { return CHAR_LITERAL; }
  "BYTE_LITERAL"                { return BYTE_LITERAL; }
  "FLOAT_LITERAL"               { return FLOAT_LITERAL; }
  "INTEGER_LITERAL"             { return INTEGER_LITERAL; }
  "BOOL_LITERAL"                { return BOOL_LITERAL; }


}

[^] { return BAD_CHARACTER; }
