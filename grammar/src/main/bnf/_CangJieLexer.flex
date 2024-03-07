package cangjie.bnf;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;

import static com.intellij.psi.TokenType.BAD_CHARACTER;
import static com.intellij.psi.TokenType.WHITE_SPACE;
import static cangjie.bnf.psi.CjElementTypes.*;

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
  {WHITE_SPACE}        { return WHITE_SPACE; }

  "Int8"               { return INT8; }
  "Int16"              { return INT16; }
  "Int32"              { return INT32; }
  "Int64"              { return INT64; }
  "IntNative"          { return INTNATIVE; }
  "UInt8"              { return UINT8; }
  "UInt16"             { return UINT16; }
  "UInt32"             { return UINT32; }
  "UInt64"             { return UINT64; }
  "UIntNative"         { return UINTNATIVE; }
  "Float16"            { return FLOAT16; }
  "Float32"            { return FLOAT32; }
  "Float64"            { return FLOAT64; }
  "Char"               { return CHAR; }
  "Bool"               { return BOOLEAN; }
  "Unit"               { return UNIT; }
  "Nothing"            { return NOTHING; }
  "struct"             { return STRUCT; }
  "enum"               { return ENUM; }
  "This"               { return THISTYPE; }
  "package"            { return PACKAGE; }
  "import"             { return IMPORT; }
  "class"              { return CLASS; }
  "interface"          { return INTERFACE; }
  "func"               { return FUNC; }
  "main"               { return MAIN; }
  "let"                { return LET; }
  "var"                { return VAR; }
  "const"              { return CONST; }
  "type"               { return TYPE_ALIAS; }
  "init"               { return INIT; }
  "this"               { return THIS; }
  "super"              { return SUPER; }
  "if"                 { return IF; }
  "else"               { return ELSE; }
  "case"               { return CASE; }
  "try"                { return TRY; }
  "catch"              { return CATCH; }
  "finally"            { return FINALLY; }
  "for"                { return FOR; }
  "do"                 { return DO; }
  "while"              { return WHILE; }
  "throw"              { return THROW; }
  "return"             { return RETURN; }
  "continue"           { return CONTINUE; }
  "break"              { return BREAK; }
  "is"                 { return IS; }
  "as"                 { return AS; }
  "in"                 { return IN; }
  "match"              { return MATCH; }
  "from"               { return FROM; }
  "where"              { return WHERE; }
  "extend"             { return EXTEND; }
  "spawn"              { return SPAWN; }
  "synchronized"       { return SYNCHRONIZED; }
  "macro"              { return MACRO; }
  "quote"              { return QUOTE; }
  "true"               { return TRUE; }
  "false"              { return FALSE; }
  "static"             { return STATIC; }
  "public"             { return PUBLIC; }
  "private"            { return PRIVATE; }
  "protected"          { return PROTECTED; }
  "override"           { return OVERRIDE; }
  "redef"              { return REDEF; }
  "abstract"           { return ABSTRACT; }
  "open"               { return OPEN; }
  "operator"           { return OPERATOR; }
  "foreign"            { return FOREIGN; }
  "inout"              { return INOUT; }
  "prop"               { return PROP; }
  "mut"                { return MUT; }
  "unsafe"             { return UNSAFE; }
  "get"                { return GET; }
  "set"                { return SET; }
  "."                  { return DOT; }
  ","                  { return COMMA; }
  "("                  { return LPAREN; }
  ")"                  { return RPAREN; }
  "["                  { return LSQUARE; }
  "]"                  { return RSQUARE; }
  "{"                  { return LCURL; }
  "}"                  { return RCURL; }
  "**"                 { return EXP; }
  "*"                  { return MUL; }
  "%"                  { return MOD; }
  "/"                  { return DIV; }
  "+"                  { return ADD; }
  "-"                  { return SUB; }
  "|>"                 { return PIPELINE; }
  "~>"                 { return COMPOSITION; }
  "++"                 { return INC; }
  "--"                 { return DEC; }
  "&&"                 { return AND; }
  "||"                 { return OR; }
  "!"                  { return NOT; }
  "&"                  { return BITAND; }
  "|"                  { return BITOR; }
  "^"                  { return BITXOR; }
  "<<"                 { return LSHIFT; }
  ">>"                 { return RSHIFT; }
  ":"                  { return COLON; }
  ";"                  { return SEMI; }
  "="                  { return ASSIGN; }
  "+="                 { return ADD_ASSIGN; }
  "-="                 { return SUB_ASSIGN; }
  "*="                 { return MUL_ASSIGN; }
  "**="                { return EXP_ASSIGN; }
  "/="                 { return DIV_ASSIGN; }
  "%="                 { return MOD_ASSIGN; }
  "&&="                { return AND_ASSIGN; }
  "||="                { return OR_ASSIGN; }
  "&="                 { return BITAND_ASSIGN; }
  "|="                 { return BITOR_ASSIGN; }
  "^="                 { return BITXOR_ASSIGN; }
  "<<="                { return LSHIFT_ASSIGN; }
  ">>="                { return RSHIFT_ASSIGN; }
  "->"                 { return ARROW; }
  "<-"                 { return BACKARROW; }
  "=>"                 { return DOUBLE_ARROW; }
  "..."                { return ELLIPSIS; }
  "..="                { return CLOSEDRANGEOP; }
  ".."                 { return RANGEOP; }
  "#"                  { return HASH; }
  "@"                  { return AT; }
  "?"                  { return QUEST; }
  "<:"                 { return UPPERBOUND; }
  "<"                  { return LT; }
  ">"                  { return GT; }
  "<="                 { return LE; }
  ">="                 { return GE; }
  "!="                 { return NOTEQUAL; }
  "=="                 { return EQUAL; }
  "_"                  { return WILDCARD; }
  "\\\\"               { return BACKSLASH; }
  "`"                  { return QUOTESYMBOL; }
  "$"                  { return DOLLAR; }
  "\""                 { return QUOTE_OPEN; }
  "\"\"\""             { return TRIPLE_QUOTE_OPEN; }
  "\""                 { return QUOTE_CLOSE; }
  "${"                 { return LINESTREXPRSTART; }
  "${"                 { return MULTILINESTREXPRSTART; }
  "identifier"         { return IDENTIFIER; }
  "NL"                 { return NL; }


}

[^] { return BAD_CHARACTER; }
