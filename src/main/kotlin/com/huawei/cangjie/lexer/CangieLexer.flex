package com.huawei.cangjie.lexer;
import com.intellij.lexer.FlexLexer;
import com.intellij.psi.*;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.containers.Stack;

%%

%unicode
%class _JetLexer
%implements FlexLexer


%{
 public _JetLexer() {
    this((java.io.Reader)null);}
%}


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

        private IElementType commentStateToTokenType(int state) {
            switch (state) {
                case BLOCK_COMMENT:
                    return CjTokens.BLOCK_COMMENT;
                case DOC_COMMENT:
                    return CjTokens.DOC_COMMENT;
                default:
                    throw new IllegalArgumentException("Unexpected state: " + state);
            }
        }
%}

%scanerror CangJieLexerException

%function advance
%type IElementType
%eof{
  return;
%eof}

%xstate STRING RAW_STRING SHORT_TEMPLATE_ENTRY BLOCK_COMMENT DOC_COMMENT
%state LONG_TEMPLATE_ENTRY UNMATCHED_BACKTICK

DIGIT=[0-9]
DIGIT_OR_UNDERSCORE = [_0-9]
DIGITS = {DIGIT} {DIGIT_OR_UNDERSCORE}*
HEX_DIGIT=[0-9A-Fa-f]
HEX_DIGIT_OR_UNDERSCORE = [_0-9A-Fa-f]
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
SHEBANG_COMMENT="#!"[^\n]*

INTEGER_LITERAL={DECIMAL_INTEGER_LITERAL}|{HEX_INTEGER_LITERAL}|{BIN_INTEGER_LITERAL}
DECIMAL_INTEGER_LITERAL=(0|([1-9]({DIGIT_OR_UNDERSCORE})*)){TYPED_INTEGER_SUFFIX}
HEX_INTEGER_LITERAL=0[Xx]({HEX_DIGIT_OR_UNDERSCORE})*{TYPED_INTEGER_SUFFIX}
BIN_INTEGER_LITERAL=0[Bb]({DIGIT_OR_UNDERSCORE})*{TYPED_INTEGER_SUFFIX}
LONG_SUFFIX=[Ll]
UNSIGNED_SUFFIX=[Uu]
TYPED_INTEGER_SUFFIX = {UNSIGNED_SUFFIX}?{LONG_SUFFIX}?

//FLOAT_LITERAL=(({FLOATING_POINT_LITERAL1})[Ff])|(({FLOATING_POINT_LITERAL2})[Ff])|(({FLOATING_POINT_LITERAL3})[Ff])|(({FLOATING_POINT_LITERAL4})[Ff])
//DOUBLE_LITERAL=(({FLOATING_POINT_LITERAL1})[Dd]?)|(({FLOATING_POINT_LITERAL2})[Dd]?)|(({FLOATING_POINT_LITERAL3})[Dd]?)|(({FLOATING_POINT_LITERAL4})[Dd])
DOUBLE_LITERAL={FLOATING_POINT_LITERAL1}|{FLOATING_POINT_LITERAL2}|{FLOATING_POINT_LITERAL3}|{FLOATING_POINT_LITERAL4}
FLOATING_POINT_LITERAL1=({DIGITS})"."({DIGITS})+({EXPONENT_PART})?({FLOATING_POINT_LITERAL_SUFFIX})?
FLOATING_POINT_LITERAL2="."({DIGITS})({EXPONENT_PART})?({FLOATING_POINT_LITERAL_SUFFIX})?
FLOATING_POINT_LITERAL3=({DIGITS})({EXPONENT_PART})({FLOATING_POINT_LITERAL_SUFFIX})?
FLOATING_POINT_LITERAL4=({DIGITS})({FLOATING_POINT_LITERAL_SUFFIX})
FLOATING_POINT_LITERAL_SUFFIX=[Ff]
EXPONENT_PART=[Ee]["+""-"]?({DIGIT_OR_UNDERSCORE})*

CHARACTER_LITERAL="'"([^\\\'\n]|{ESCAPE_SEQUENCE})*("'"|\\)?
// TODO: 引入符号(例如‘foo)作为编写字符串文字的另一种方式
ESCAPE_SEQUENCE=\\(u{HEX_DIGIT}{HEX_DIGIT}{HEX_DIGIT}{HEX_DIGIT}|[^\n])

// ANY_ESCAPE_SEQUENCE = \\[^]
THREE_QUO = (\"\"\")
THREE_OR_MORE_QUO = ({THREE_QUO}\"*)

REGULAR_STRING_PART=[^\\\"\n\$]+
SHORT_TEMPLATE_ENTRY=\${IDENTIFIER}
LONELY_DOLLAR=\$
LONG_TEMPLATE_ENTRY_START=\$\{
LONELY_BACKTICK=`

%%

// String 模板

{THREE_QUO}                      { pushState(RAW_STRING); return CjTokens.OPEN_QUOTE; }
<RAW_STRING> \n                  { return CjTokens.REGULAR_STRING_PART; }
<RAW_STRING> \"                  { return CjTokens.REGULAR_STRING_PART; }
<RAW_STRING> \\                  { return CjTokens.REGULAR_STRING_PART; }
<RAW_STRING> {THREE_OR_MORE_QUO} {
                                    int length = yytext().length();
                                    if (length <= 3) { // closing """
                                        popState();
                                        return CjTokens.CLOSING_QUOTE;
                                    }
                                    else { // some quotes at the end of a string, e.g. """ "foo""""
                                        yypushback(3); // return the closing quotes (""") to the stream
                                        return CjTokens.REGULAR_STRING_PART;
                                    }
                                 }

\"                          { pushState(STRING); return CjTokens.OPEN_QUOTE; }
<STRING> \n                 { popState(); yypushback(1); return CjTokens.DANGLING_NEWLINE; }
<STRING> \"                 { popState(); return CjTokens.CLOSING_QUOTE; }
<STRING> {ESCAPE_SEQUENCE}  { return CjTokens.ESCAPE_SEQUENCE; }

<STRING, RAW_STRING> {REGULAR_STRING_PART}         { return CjTokens.REGULAR_STRING_PART; }
<STRING, RAW_STRING> {SHORT_TEMPLATE_ENTRY}        {
                                                        pushState(SHORT_TEMPLATE_ENTRY);
                                                        yypushback(yylength() - 1);
                                                        return CjTokens.SHORT_TEMPLATE_ENTRY_START;
                                                   }
// Only *this* keyword is itself an expression valid in this position
// *null*, *true* and *false* are also keywords and expression, but it does not make sense to put them
// in a string template for it'd be easier to just type them in without a dollar
<SHORT_TEMPLATE_ENTRY> "this"          { popState(); return CjTokens.THIS_KEYWORD; }
<SHORT_TEMPLATE_ENTRY> {IDENTIFIER}    { popState(); return CjTokens.IDENTIFIER; }

<STRING, RAW_STRING> {LONELY_DOLLAR}               { return CjTokens.REGULAR_STRING_PART; }
<STRING, RAW_STRING> {LONG_TEMPLATE_ENTRY_START}   { pushState(LONG_TEMPLATE_ENTRY); return CjTokens.LONG_TEMPLATE_ENTRY_START; }

<LONG_TEMPLATE_ENTRY> "{"              { lBraceCount++; return CjTokens.LBRACE; }
<LONG_TEMPLATE_ENTRY> "}"              {
                                           if (lBraceCount == 0) {
                                             popState();
                                             return CjTokens.LONG_TEMPLATE_ENTRY_END;
                                           }
                                           lBraceCount--;
                                           return CjTokens.RBRACE;
                                       }

// (Nested) comments

"/**/" {
    return CjTokens.BLOCK_COMMENT;
}

"/**" {
    pushState(DOC_COMMENT);
    commentDepth = 0;
    commentStart = getTokenStart();
}

"/*" {
    pushState(BLOCK_COMMENT);
    commentDepth = 0;
    commentStart = getTokenStart();
}

<BLOCK_COMMENT, DOC_COMMENT> {
    "/*" {
         commentDepth++;
    }

    <<EOF>> {
        int state = yystate();
        popState();
        zzStartRead = commentStart;
        return commentStateToTokenType(state);
    }

    "*/" {
        if (commentDepth > 0) {
            commentDepth--;
        }
        else {
             int state = yystate();
             popState();
             zzStartRead = commentStart;
             return commentStateToTokenType(state);
        }
    }

    [\s\S] {}
}

// Mere mortals

({WHITE_SPACE_CHAR})+ { return CjTokens.WHITE_SPACE; }

{EOL_COMMENT} { return CjTokens.EOL_COMMENT; }
{SHEBANG_COMMENT} {
            if (zzCurrentPos == 0) {
                return CjTokens.SHEBANG_COMMENT;
            }
            else {
                yypushback(yylength() - 1);
                return CjTokens.HASH;
            }
          }

{INTEGER_LITERAL}\.\. { yypushback(2); return CjTokens.INTEGER_LITERAL; }
{INTEGER_LITERAL} { return CjTokens.INTEGER_LITERAL; }

{DOUBLE_LITERAL}     { return CjTokens.FLOAT_LITERAL; }

{CHARACTER_LITERAL} { return CjTokens.CHARACTER_LITERAL; }


"interface"  { return CjTokens.INTERFACE_KEYWORD ;}
"continue"   { return CjTokens.CONTINUE_KEYWORD ;}
"package"    { return CjTokens.PACKAGE_KEYWORD ;}
"return"     { return CjTokens.RETURN_KEYWORD ;}

"while"      { return CjTokens.WHILE_KEYWORD ;}
"break"      { return CjTokens.BREAK_KEYWORD ;}
"class"      { return CjTokens.CLASS_KEYWORD ;}
"throw"      { return CjTokens.THROW_KEYWORD ;}
"false"      { return CjTokens.FALSE_KEYWORD ;}
"super"      { return CjTokens.SUPER_KEYWORD ;}

"match"       { return CjTokens.MATCH_KEYWORD ;}
"true"       { return CjTokens.TRUE_KEYWORD ;}
"this"       { return CjTokens.THIS_KEYWORD ;}

"else"       { return CjTokens.ELSE_KEYWORD ;}
"try"        { return CjTokens.TRY_KEYWORD ;}
"let"        { return CjTokens.LET_KEYWORD ;}
"var"        { return CjTokens.VAR_KEYWORD ;}
"func"        { return CjTokens.FUNC_KEYWORD ;}
"for"        { return CjTokens.FOR_KEYWORD ;}
"is"         { return CjTokens.IS_KEYWORD ;}
"in"         { return CjTokens.IN_KEYWORD ;}
"if"         { return CjTokens.IF_KEYWORD ;}
"do"         { return CjTokens.DO_KEYWORD ;}
"as"         { return CjTokens.AS_KEYWORD ;}
"main"       { return CjTokens.MAIN_KEYWORD ;}
"struct"     { return CjTokens.STRUCT_KEYWORD ;}
"from"       { return CjTokens.FROM_KEYWORD ;}

"Int8"       { return CjTokens.INT8_KEYWORD ;}
"Int16"      { return CjTokens.INT16_KEYWORD ;}
"Int32"      { return CjTokens.INT32_KEYWORD ;}
"Int64"      { return CjTokens.INT64_KEYWORD ;}
"UInt8"      { return CjTokens.UINT8_KEYWORD ;}
"UInt16"     { return CjTokens.UINT16_KEYWORD ;}
"UInt32"     { return CjTokens.UINT32_KEYWORD ;}
"UInt64"     { return CjTokens.UINT64_KEYWORD ;}
"Float32"    { return CjTokens.FLOAT32_KEYWORD ;}
"Float64"    { return CjTokens.FLOAT64_KEYWORD ;}
"Bool"       { return CjTokens.BOOL_KEYWORD ;}
"Unit"       { return CjTokens.UNIT_KEYWORD ;}
"Char"       { return CjTokens.CHAR_KEYWORD ;}

{FIELD_IDENTIFIER} { return CjTokens.FIELD_IDENTIFIER; }
{IDENTIFIER} { return CjTokens.IDENTIFIER; }
\!in{IDENTIFIER_PART}        { yypushback(3); return CjTokens.EXCL; }
\!is{IDENTIFIER_PART}        { yypushback(3); return CjTokens.EXCL; }



"<:"        { return CjTokens.LTCOLON  ; }


"++"         { return CjTokens.PLUSPLUS  ; }
"--"         { return CjTokens.MINUSMINUS; }
"<="         { return CjTokens.LTEQ      ; }
">="         { return CjTokens.GTEQ      ; }
"=="         { return CjTokens.EQEQ      ; }
"!="         { return CjTokens.EXCLEQ    ; }
"&&"         { return CjTokens.ANDAND    ; }
"&"          { return CjTokens.AND       ; }
"||"         { return CjTokens.OROR      ; }
"*="         { return CjTokens.MULTEQ    ; }
"/="         { return CjTokens.DIVEQ     ; }
"%="         { return CjTokens.PERCEQ    ; }
"+="         { return CjTokens.PLUSEQ    ; }
"-="         { return CjTokens.MINUSEQ   ; }
"->"         { return CjTokens.ARROW     ; }
"=>"         { return CjTokens.DOUBLE_ARROW; }
".."         { return CjTokens.RANGE     ; }

"["          { return CjTokens.LBRACKET  ; }
"]"          { return CjTokens.RBRACKET  ; }
"{"          { return CjTokens.LBRACE    ; }
"}"          { return CjTokens.RBRACE    ; }
"("          { return CjTokens.LPAR      ; }
")"          { return CjTokens.RPAR      ; }
"."          { return CjTokens.DOT       ; }
"*"          { return CjTokens.MUL       ; }
"+"          { return CjTokens.PLUS      ; }
"-"          { return CjTokens.MINUS     ; }
"!"          { return CjTokens.EXCL      ; }
"/"          { return CjTokens.DIV       ; }
"%"          { return CjTokens.PERC      ; }
"<"          { return CjTokens.LT        ; }
">"          { return CjTokens.GT        ; }
"?"          { return CjTokens.QUEST     ; }
":"          { return CjTokens.COLON     ; }
";;"         { return CjTokens.DOUBLE_SEMICOLON;}
";"          { return CjTokens.SEMICOLON ; }
"="          { return CjTokens.EQ        ; }
","          { return CjTokens.COMMA     ; }


{LONELY_BACKTICK} { pushState(UNMATCHED_BACKTICK); return TokenType.BAD_CHARACTER; }

// error fallback
[\s\S]       { return TokenType.BAD_CHARACTER; }
// error fallback for exclusive states
<STRING, RAW_STRING, SHORT_TEMPLATE_ENTRY, BLOCK_COMMENT, DOC_COMMENT> .
             { return TokenType.BAD_CHARACTER; }

