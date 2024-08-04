package com.linqingying.cangjie.grammar.parser;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.containers.Stack;
import com.linqingying.cangjie.lexer.CangJieLexerException;
import static com.intellij.psi.TokenType.BAD_CHARACTER;
import static com.intellij.psi.TokenType.WHITE_SPACE;

import static com.intellij.psi.TokenType.BAD_CHARACTER;

import static com.linqingying.cangjie.grammar.psi.CjElementTypes.*;

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

%{
    private static final class State {
            final int lBraceCount;
            final int state;

            public State(int state, int lBraceCount) {
                this.state = state;
                this.lBraceCount = lBraceCount;
            }

            @Override
            public String toString() {
                return "yystate = " + state + (lBraceCount == 0 ? "" : "lBraceCount = " + lBraceCount);
            }
        }

        private final Stack<State> states = new Stack<State>();
        private int lBraceCount;

        private int commentStart;
        private int commentDepth;

        private void pushState(int state) {
            states.push(new State(yystate(), lBraceCount));
            lBraceCount = 0;
            yybegin(state);
        }

        private void popState() {
            State state = states.pop();
            lBraceCount = state.lBraceCount;
            yybegin(state.state);
        }

        private State getState(){

        int size = states.size();
        if(size == 0){
            return null;
        }

           return  states.get(states.size() - 1);


        }


%}
%{
    private boolean isGenerics = false;
%}
%scanerror CangJieLexerException



%function advance
%type IElementType
%eof{
  return;
%eof}

%xstate SHOP_STRING STRING RAW_STRING SHORT_TEMPLATE_ENTRY BLOCK_COMMENT DOC_COMMENT
%state LONG_TEMPLATE_ENTRY UNMATCHED_BACKTICK


DIGIT=[0-9]
DIGIT_OR_UNDERSCORE = [0-9_]
DIGITS = {DIGIT} {DIGIT_OR_UNDERSCORE}*
HEX_DIGIT=[0-9A-Fa-f]
HEX_DIGIT_OR_UNDERSCORE = [0-9A-Fa-f_]
BIN_DIGIT=[0-1]
BIN_DIGIT_OR_UNDERSCORE = [01_]
OCT_DIGIT=[0-7]
OCT_DIGIT_OR_UNDERSCORE = [07_]



WHITE_SPACE_CHAR=[\ \n\t\f]

// TODO: 是否禁止标识符中的“$”？
LETTER = [:letter:]|_
IDENTIFIER_PART=[:digit:]|{LETTER}
PLAIN_IDENTIFIER={LETTER} {IDENTIFIER_PART}*


//TODO：这必须允许运行库接受的所有内容。
//TODO：将反号替换为开头的一个反斜杠
ESCAPED_IDENTIFIER = `[^`\n]+`
IDENTIFIER = {PLAIN_IDENTIFIER}|{ESCAPED_IDENTIFIER}
FIELD_IDENTIFIER = \${IDENTIFIER}

EOL_COMMENT="/""/"[^\n]*
//SHEBANG_COMMENT="#!"[^\n]*

//UNIT_LTIERAL="()"
//元素，两个以上表达式 (p1,p2,...,pn)
//TUPLE_LTIERAL="("[^)]*")"

INTEGER_LITERAL=({DECIMAL_INTEGER_LITERAL}|{HEX_INTEGER_LITERAL}|{BIN_INTEGER_LITERAL} |{OCT_INTEGER_LITERAL})(u8|u16|u32|u64|i8|i16|i32|i64)?
DECIMAL_INTEGER_LITERAL=(0|([1-9]({DIGIT_OR_UNDERSCORE})*))
HEX_INTEGER_LITERAL=0[Xx]({HEX_DIGIT_OR_UNDERSCORE})*
BIN_INTEGER_LITERAL=0[Bb]({BIN_DIGIT_OR_UNDERSCORE})*
//八进制
OCT_INTEGER_LITERAL=0[Oo]({OCT_DIGIT_OR_UNDERSCORE})*

//LONG_SUFFIX=[Ll]
//UNSIGNED_SUFFIX=[Uu]
//TYPED_INTEGER_SUFFIX = {UNSIGNED_SUFFIX}?{LONG_SUFFIX}?

//FLOAT_LITERAL=(({FLOATING_POINT_LITERAL1})[Ff])|(({FLOATING_POINT_LITERAL2})[Ff])|(({FLOATING_POINT_LITERAL3})[Ff])|(({FLOATING_POINT_LITERAL4})[Ff])
//DOUBLE_LITERAL=(({FLOATING_POINT_LITERAL1})[Dd]?)|(({FLOATING_POINT_LITERAL2})[Dd]?)|(({FLOATING_POINT_LITERAL3})[Dd]?)|(({FLOATING_POINT_LITERAL4})[Dd])
FLOAT_LITERAL=({ FLOAT_LITERAL_POINT} |{HEX_FLOATING_POINT_LITERAL} )
FLOAT_LITERAL_POINT = ({FLOATING_POINT_LITERAL1}|{FLOATING_POINT_LITERAL2}|{FLOATING_POINT_LITERAL3})(f32|f16|f64)?
FLOATING_POINT_LITERAL1=({DIGITS})"."({DIGITS})+({EXPONENT_PART})?
FLOATING_POINT_LITERAL2="."({DIGITS})({EXPONENT_PART})?
FLOATING_POINT_LITERAL3=({DIGITS})({EXPONENT_PART})?



HEX_FLOATING_POINT_LITERAL= {HEX_FLOATING_POINT_LITERAL1} |{HEX_FLOATING_POINT_LITERAL2} |{HEX_FLOATING_POINT_LITERAL3}
HEX_FLOATING_POINT_LITERAL1= 0[xX]({DIGITS})"."({DIGITS})+({EXPONENT_PART})?"p"({DIGIT})
HEX_FLOATING_POINT_LITERAL2= 0[xX]"."({DIGITS})({EXPONENT_PART})?"p"({DIGIT})
HEX_FLOATING_POINT_LITERAL3= 0[xX]({DIGITS})({EXPONENT_PART})?"p"({DIGIT})


//FLOATING_POINT_LITERAL_SUFFIX=[Ff]
EXPONENT_PART=[Ee]["+""-"]?({DIGIT_OR_UNDERSCORE})*




CHARACTER_LITERAL="b"?"'"([^\\\'\n]|{ESCAPE_SEQUENCE})*("'"|\\)?
// TODO: 引入符号(例如‘foo)作为编写字符串文字的另一种方式
ESCAPE_SEQUENCE=\\(u{HEX_DIGIT}{HEX_DIGIT}{HEX_DIGIT}{HEX_DIGIT}|[^\n])

// ANY_ESCAPE_SEQUENCE = \\[^]
THREE_QUO = (\"\"\")
THREE_OR_MORE_QUO = ({THREE_QUO}\"*)

//#[n*]""#[n*]
//SHOP_STRING =(\#\{\1\,\})(\?:\"[^\"]\*\"\1)
SHOP_QUO = (#+\")
SHOP_OR_MORE_QUO = (\"#+)`

REGULAR_STRING_PART=[^\\\"\n\$]+
SHORT_TEMPLATE_ENTRY=\${IDENTIFIER}
LONELY_DOLLAR=\$
LONG_TEMPLATE_ENTRY_START=\$\{
LONELY_BACKTICK=`

WHITE_SPACE=\s+


%%
<YYINITIAL> {
//({WHITE_SPACE_CHAR})+  { return WHITE_SPACE; }

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

    "${"                 { return LINESTREXPRSTART; }


  {IDENTIFIER}           { return IDENTIFIER; }


}

[^] { return BAD_CHARACTER; }
