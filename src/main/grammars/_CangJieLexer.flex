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
  {WHITE_SPACE}       { return WHITE_SPACE; }

  "main"              { return MAIN; }
  "func"              { return FUNC; }
  "class"             { return CLASS; }
  "var"               { return VAR; }
  "let"               { return LET; }
  "mut"               { return MUT; }
  "prop"              { return PROP; }
  "init"              { return INIT; }
  "open"              { return OPEN; }
  "from"              { return FROM; }
  "import"            { return IMPORT; }
  "as"                { return AS; }
  "enum"              { return ENUM; }
  "super"             { return SUPER; }
  "this"              { return THIS; }
  "interface"         { return INTERFACE; }
  "static"            { return STATIC; }
  "struct"            { return STRUCT; }
  "return"            { return RETURN; }
  "extend"            { return EXTEND; }
  "true"              { return TRUE; }
  "false"             { return FALSE; }
  "Int8"              { return INT8; }
  "Int16"             { return INT16; }
  "Int32"             { return INT32; }
  "Int64"             { return INT64; }
  "Float32"           { return FLOAT32; }
  "Float64"           { return FLOAT64; }
  "Char"              { return CHAE; }
  "UInt8"             { return UINT8; }
  "UInt16"            { return UINT16; }
  "UInt32"            { return UINT32; }
  "UInt64"            { return UINT64; }
  "Bool"              { return BOOL; }
  "Unit"              { return UNIT; }
  "if"                { return IF; }
  "else"              { return ELSE; }
  "while"             { return WHILE; }
  "for"               { return FOR; }
  "in"                { return IN; }
  "do"                { return DO; }
  "break"             { return BREAK; }
  "continue"          { return CONTINUE; }
  "match"             { return MATCH; }
  "case"              { return CASE; }
  "try"               { return TRY; }
  "catch"             { return CATCH; }
  "finally"           { return FINALLY; }
  "throw"             { return THROW; }
  "{"                 { return LBRACE; }
  "}"                 { return RBRACE; }
  "["                 { return LBRACK; }
  "]"                 { return RBRACK; }
  "("                 { return LPAREN; }
  ")"                 { return RPAREN; }
  "="                 { return EQ; }
  "=="                { return EQEQ; }
  "!="                { return NOTEQ; }
  ">"                 { return GT; }
  ">>"                { return GTGT; }
  "<"                 { return LT; }
  "<<"                { return LTLT; }
  ">="                { return GTEQ; }
  "<="                { return LTEQ; }
  "&&"                { return ANDAND; }
  "&"                 { return AND; }
  "||"                { return OROR; }
  "|"                 { return OR; }
  "identifier"        { return IDENTIFIER; }
  "unsafe"            { return UNSAFE; }


}

[^] { return BAD_CHARACTER; }
