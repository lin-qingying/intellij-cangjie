package com.huawei.cangjie.lang.core.lexer;

import com.intellij.lexer.*;
import com.intellij.psi.tree.IElementType;


import static  com.huawei.cangjie.lang.core.psi.CjElementTypes.*;

import  static  com.huawei.cangjie.lang.core.parser.CangJieParserDefinition.*;
import static com.intellij.psi.TokenType.*;

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

%s IN_SHEBANG

%s IN_BLOCK_COMMENT
%s IN_OUTER_EOL_COMMENT

%s IN_LIFETIME_OR_CHAR

%s IN_RAW_LITERAL
%s IN_RAW_LITERAL_SUFFIX

%unicode

///////////////////////////////////////////////////////////////////////////////////////////////////
// Whitespaces
///////////////////////////////////////////////////////////////////////////////////////////////////

EOL_WS           = \n | \r | \r\n
LINE_WS          = [\ \t]
WHITE_SPACE_CHAR = {EOL_WS} | {LINE_WS}
WHITE_SPACE      = {WHITE_SPACE_CHAR}+

///////////////////////////////////////////////////////////////////////////////////////////////////
// Identifier
///////////////////////////////////////////////////////////////////////////////////////////////////

IDENTIFIER = ("r#")?[_\p{xidstart}][\p{xidcontinue}]*
SUFFIX     = {IDENTIFIER}

///////////////////////////////////////////////////////////////////////////////////////////////////
// Literals
///////////////////////////////////////////////////////////////////////////////////////////////////

EXPONENT      = [eE] [-+]? [0-9_]+

INT_LITERAL = ( {DEC_LITERAL}
              | {HEX_LITERAL}
              | {OCT_LITERAL}
              | {BIN_LITERAL} ) {EXPONENT}? {SUFFIX}?

DEC_LITERAL = [0-9] [0-9_]*
HEX_LITERAL = "0x" [a-fA-F0-9_]*
OCT_LITERAL = "0o" [0-7_]*
BIN_LITERAL = "0b" [01_]*


CHAR_LITERAL   = ( \' ( [^\\\'\r\n] | \\[^\r\n] | "\\x" [a-fA-F0-9]+ | "\\u{" [a-fA-F0-9][a-fA-F0-9_]* "}"? )? ( \' {SUFFIX}? | \\ )? )
               | ( \' [\p{xidcontinue}]* \' {SUFFIX}? )
STRING_LITERAL = \" ( [^\\\"] | \\[^] )* ( \" {SUFFIX}? | \\ )?

INNER_EOL_DOC = ({LINE_WS}*"//!".*{EOL_WS})*({LINE_WS}*"//!".*)
// !(!a|b) is a (set) difference between a and b.
EOL_DOC_LINE  = {LINE_WS}*!(!("///".*)|("////".*))

%%
<YYINITIAL> {


  \'                              { yybegin(IN_LIFETIME_OR_CHAR); yypushback(1); }

  "{"                             { return LBRACE; }
  "}"                             { return RBRACE; }
  "["                             { return LBRACK; }
  "]"                             { return RBRACK; }
  "("                             { return LPAREN; }
  ")"                             { return RPAREN; }


  "as"                            { return AS; }

  "break"                         { return BREAK; }

  "continue"                      { return CONTINUE; }

  "else"                          { return ELSE; }
  "enum"                          { return ENUM; }

  "func"                            { return FUNC; }
  "for"                           { return FOR; }
  "if"                            { return IF; }

  "in"                            { return IN; }
  "let"                           { return LET; }
 "var"  { return VAR; }

  "match"                         { return MATCH; }


  "mut"                           { return MUT; }


  "return"                        { return RETURN; }

  "static"                        { return STATIC; }
  "struct"                        { return STRUCT; }
  "super"                         { return SUPER; }





  "while"                         { return WHILE; }


  {WHITE_SPACE}                   { return WHITE_SPACE; }
}






///////////////////////////////////////////////////////////////////////////////////////////////////
// Catch All
///////////////////////////////////////////////////////////////////////////////////////////////////

[^] { return BAD_CHARACTER; }
