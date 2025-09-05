package org.cangnova.cangjie.lexer;
import com.intellij.psi.*;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.containers.Stack;

%%

%unicode
%class _CangJieLexer
%implements FlexLexer


%{
 public _CangJieLexer() {
    this((java.io.Reader)null);}
%}



%{
    private static final class State {
            final int lBraceCount;
            final int requiredInterpolationPrefix;
            final int state;

            public State(int state, int lBraceCount, int requiredInterpolationPrefix) {
                this.state = state;
                this.lBraceCount = lBraceCount;
                this.requiredInterpolationPrefix = requiredInterpolationPrefix;
            }

            @Override
            public String toString() {
                return "yystate = " + state
                    + (lBraceCount == 0 ? "" : "lBraceCount = " + lBraceCount)
                    + (requiredInterpolationPrefix == -1 ? "" : "requiredInterpolationPrefix = " + requiredInterpolationPrefix);
            }
        }
        private final Stack<State> states = new Stack<State>();
        private int lBraceCount;
    private int requiredInterpolationPrefix;

        private int commentStart;
        private int commentDepth;

         private void pushState(int state) {
             states.push(new State(yystate(), lBraceCount, requiredInterpolationPrefix));
             lBraceCount = 0;
             requiredInterpolationPrefix = -1;
             yybegin(state);
         }
   private void pushInterpolationPrefix(int interpolationPrefix) {
        states.push(new State(yystate(), lBraceCount, requiredInterpolationPrefix));
        lBraceCount = 0;
        requiredInterpolationPrefix = interpolationPrefix;
        yybegin(STRING_PREFIX);
    }
      private void popState() {
              State state = states.pop();
              lBraceCount = state.lBraceCount;
              requiredInterpolationPrefix = state.requiredInterpolationPrefix;
              yybegin(state.state);
          }

        private State getState(){

        int size = states.size();
        if(size == 0){
            return null;
        }

           return  states.get(states.size() - 1);


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
%{
    private boolean isGenerics = false;
%}
%scanerror CangJieLexerException

%function advance
%type IElementType
%eof{
  return;
%eof}

%xstate STRING_PREFIX  HSAH_STRING_SINGLE  HSAH_STRING_DOUBLE  STRING_SINGLE  STRING_DOUBLE  RAW_STRING_SINGLE RAW_STRING_DOUBLE SHORT_TEMPLATE_ENTRY BLOCK_COMMENT DOC_COMMENT
%state LONG_TEMPLATE_ENTRY UNMATCHED_BACKTICK


DIGIT=[0-9]
DIGIT_OR_UNDERSCORE = [0-9_]
DIGITS = {DIGIT} {DIGIT_OR_UNDERSCORE}*
HEX_DIGIT=[0-9A-Fa-f]
HEX_DIGIT_OR_UNDERSCORE = [0-9A-Fa-f_]
BIN_DIGIT=[0-1]
BIN_DIGIT_OR_UNDERSCORE = [01_]
OCT_DIGIT=[0-7]
OCT_DIGIT_OR_UNDERSCORE = [0-7_]



WHITE_SPACE_CHAR=[\ \n\t\f]

// TODO: 是否禁止标识符中的“$”？
LETTER = [:letter:]|_
IDENTIFIER_PART=[:digit:]|{LETTER}
PLAIN_IDENTIFIER={LETTER} {IDENTIFIER_PART}*
BOOLEAN_LITERAL= true | false

//TODO：这必须允许运行库接受的所有内容。
//TODO：将反号替换为开头的一个反斜杠
ESCAPED_IDENTIFIER = `[^`\n]+`
IDENTIFIER  =    {ESCAPED_IDENTIFIER } |  {PLAIN_IDENTIFIER}

FIELD_IDENTIFIER = \${IDENTIFIER}

EOL_COMMENT="/""/"[^\n]*


INTEGER_LITERAL=({DECIMAL_INTEGER_LITERAL}|{HEX_INTEGER_LITERAL}|{BIN_INTEGER_LITERAL} |{OCT_INTEGER_LITERAL})(u8|u16|u32|u64|i8|i16|i32|i64)?
DECIMAL_INTEGER_LITERAL=(0|([1-9]({DIGIT_OR_UNDERSCORE})*))
HEX_INTEGER_LITERAL=0[Xx]({HEX_DIGIT_OR_UNDERSCORE})*
BIN_INTEGER_LITERAL=0[Bb]({BIN_DIGIT_OR_UNDERSCORE})*
//八进制
OCT_INTEGER_LITERAL=0[Oo]({OCT_DIGIT_OR_UNDERSCORE})*

FLOAT_LITERAL={ FLOAT_LITERAL_POINT} |{HEX_FLOATING_POINT_LITERAL}
FLOAT_LITERAL_POINT = ({FLOATING_POINT_LITERAL1}|{FLOATING_POINT_LITERAL2}|{FLOATING_POINT_LITERAL3})(f32|f16|f64)?
FLOATING_POINT_LITERAL1=({DIGITS})"."({DIGITS})+({EXPONENT_PART})?
FLOATING_POINT_LITERAL2="."({DIGITS})({EXPONENT_PART})?
FLOATING_POINT_LITERAL3=({DIGITS})({EXPONENT_PART})



HEX_FLOATING_POINT_LITERAL= {HEX_FLOATING_POINT_LITERAL1} |{HEX_FLOATING_POINT_LITERAL2} |{HEX_FLOATING_POINT_LITERAL3}
HEX_FLOATING_POINT_LITERAL1= 0[xX]({HEX_DIGIT_OR_UNDERSCORE})+"."({HEX_DIGIT_OR_UNDERSCORE})+ "p"({DIGIT})+
HEX_FLOATING_POINT_LITERAL2= 0[xX]"."({HEX_DIGIT_OR_UNDERSCORE})+ "p"({DIGIT})+
HEX_FLOATING_POINT_LITERAL3= 0[xX]({HEX_DIGIT_OR_UNDERSCORE})+ "p"({DIGIT})+


//FLOATING_POINT_LITERAL_SUFFIX=[Ff]
EXPONENT_PART=[Ee]["-"]?({DIGIT_OR_UNDERSCORE})*

// TODO: 引入符号(例如‘foo)作为编写字符串文字的另一种方式
ESCAPE_SEQUENCE=\\(u\{ {HEX_DIGIT}{1,8} \} | [^\n])  //\n 是特殊匹配



CHARACTER_BYTE_LITERAL = {CHARACTER_BYTE_SINGLE_LITERAL} /*| {CHARACTER_BYTE_DOUBLE_LITERAL}*/
CHARACTER_BYTE_SINGLE_LITERAL = b \'  ({SINGLE_CHAR}  | {ESCAPE_SEQUENCE})*  \'
//CHARACTER_BYTE_DOUBLE_LITERAL = b \" ({SINGLE_CHAR}  | {ESCAPE_SEQUENCE})* \"

//单字符
SINGLE_CHAR=[^'\\\r\n]

// 定义转义序列
ESCAPE_SEQ  = ({ESCAPE_SEQUENCE} | {ESCAPED_IDENTIFIER})
//字符
RUNE_LITERAL = {RUNE_SINGLE_LITERAL} | {RUNE_DOUBLE_LITERAL}
RUNE_SINGLE_LITERAL = r \'  ({SINGLE_CHAR} | {ESCAPE_SEQ})*  \'
RUNE_DOUBLE_LITERAL = r \" ({SINGLE_CHAR} | {ESCAPE_SEQ})* \"



//单引号
THREE_QUO_SINGLE =   (\'\'\')
THREE_OR_SINGLE_MORE_QUO = ({THREE_QUO_SINGLE}\'*)
//双引号
THREE_QUO_DOUBLE =   (\"\"\")
THREE_OR_DOUBLE_MORE_QUO = ({THREE_QUO_DOUBLE}\"*)

// 单引号
HASH_QUO_SINGLE = (#+\')
HSAH_OR_SINGLE_MORE_QUO = (\'#+)
//双引号
HASH_QUO_DOUBLE = (#+\")
HSAH_OR_DOUBLE_MORE_QUO = (\"#+)


//单引号
SINGLE_QUO = \'
//双引号
DOUBLE_QUO = \"


REGULAR_STRING_PART_DOUBLE=[^\\\"\n\$]+
REGULAR_STRING_PART_SINGLE=[^\\\'\n\$]+
INTERPOLATION = \$+

SHORT_TEMPLATE_ENTRY={INTERPOLATION}{IDENTIFIER}

LONELY_DOLLAR=\$
LONG_TEMPLATE_ENTRY_START=\$\{
LONELY_BACKTICK=`

%%

// String 模板
//
//
{HASH_QUO_SINGLE}                      {
          lBraceCount = yytext().length() - 1;
                                      pushState(HSAH_STRING_SINGLE);
                                      return CjTokens.OPEN_QUOTE; }

<HSAH_STRING_SINGLE> \n                  { return CjTokens.REGULAR_STRING_PART; }
<HSAH_STRING_SINGLE> \"  | \'               { return CjTokens.REGULAR_STRING_PART; }
<HSAH_STRING_SINGLE> \\                  { return CjTokens.REGULAR_STRING_PART; }
<HSAH_STRING_SINGLE>  {HSAH_OR_SINGLE_MORE_QUO}      {

//                                       popState();
                                       int lenght = yytext().length() - 1;
                                       int lBraceCount1 = getState() != null ? getState().lBraceCount : lBraceCount;
                                       if(lBraceCount1 == lenght){
                                           popState();

                                                 return CjTokens.CLOSING_QUOTE;
                                       }


                                       else if(lBraceCount1 < lenght){
//
                                           popState();

                                           yypushback(lenght - lBraceCount1);
                                             return CjTokens.CLOSING_QUOTE;
                                       }else {

//                                            yypushback(yylength()); // return the closing quotes (""") to the stream
                                                                               return CjTokens.REGULAR_STRING_PART;
                                       }
//


      }


{HASH_QUO_DOUBLE}                      {
          lBraceCount = yytext().length() - 1;
                                      pushState(HSAH_STRING_DOUBLE);
                                      return CjTokens.OPEN_QUOTE; }
<HSAH_STRING_DOUBLE> \n                          { return CjTokens.REGULAR_STRING_PART; }
<HSAH_STRING_DOUBLE>  \"  | \'             { return CjTokens.REGULAR_STRING_PART; }
<HSAH_STRING_DOUBLE> \\                  { return CjTokens.REGULAR_STRING_PART; }
<HSAH_STRING_DOUBLE>  {HSAH_OR_DOUBLE_MORE_QUO}      {

//                                       popState();
                                       int lenght = yytext().length() - 1;
                                       int lBraceCount1 = getState() != null ? getState().lBraceCount : lBraceCount;
                                       if(lBraceCount1 == lenght){
                                           popState();

                                                 return CjTokens.CLOSING_QUOTE;
                                       }


                                       else if(lBraceCount1 < lenght){
//
                                           popState();

                                           yypushback(lenght - lBraceCount1);
                                             return CjTokens.CLOSING_QUOTE;
                                       }else {

//                                            yypushback(yylength()); // return the closing quotes (""") to the stream
                                                                               return CjTokens.REGULAR_STRING_PART;
                                       }
//


      }


{THREE_QUO_SINGLE} /*\n*/                      { pushState(RAW_STRING_SINGLE); return CjTokens.OPEN_QUOTE; }
<RAW_STRING_SINGLE> \n                  {
               System.out.println("RAW_STRING_SINGLE1");

          return CjTokens.REGULAR_STRING_PART; }
<RAW_STRING_SINGLE> \" | \'                 {
               System.out.println("RAW_STRING_SINGLE2");
          return CjTokens.REGULAR_STRING_PART; }
<RAW_STRING_SINGLE> \\                  {
               System.out.println("RAW_STRING_SINGLE3");

          return CjTokens.REGULAR_STRING_PART; }
<RAW_STRING_SINGLE> {THREE_OR_SINGLE_MORE_QUO} {
                                    int length = yytext().length();
                                    if (length <= 3) { // closing '''
                                        popState();
                                        return CjTokens.CLOSING_QUOTE;
                                    }
                                    else { // some quotes at the end of a string, e.g. """ "foo""""
                                        yypushback(3); // return the closing quotes (""") to the stream
                                        return CjTokens.REGULAR_STRING_PART;
                                    }
                                 }

{THREE_QUO_DOUBLE}  /*\n*/                     { pushState(RAW_STRING_DOUBLE); return CjTokens.OPEN_QUOTE; }
<RAW_STRING_DOUBLE> \n                  {
          System.out.println("RAW_STRING_DOUBLE1");

          return CjTokens.REGULAR_STRING_PART; }
<RAW_STRING_DOUBLE> \" | \'               {
          System.out.println("RAW_STRING_DOUBLE2");
          return CjTokens.REGULAR_STRING_PART; }
<RAW_STRING_DOUBLE> \\                  {
          System.out.println("RAW_STRING_DOUBLE3");

          return CjTokens.REGULAR_STRING_PART; }
<RAW_STRING_DOUBLE> {THREE_OR_DOUBLE_MORE_QUO} {
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

//b\"                           { pushState(STRING); return CjTokens.OPEN_QUOTE; }


 {SINGLE_QUO}                  { pushState(STRING_SINGLE); return CjTokens.OPEN_QUOTE; }
<STRING_SINGLE> \n                 { popState(); yypushback(1); return CjTokens.DANGLING_NEWLINE; }
<STRING_SINGLE>     {SINGLE_QUO}             { popState(); return CjTokens.CLOSING_QUOTE; }
<STRING_SINGLE> {ESCAPE_SEQUENCE}  { return CjTokens.ESCAPE_SEQUENCE; }

{DOUBLE_QUO}                  { pushState(STRING_DOUBLE); return CjTokens.OPEN_QUOTE; }
<STRING_DOUBLE> \n                 { popState(); yypushback(1); return CjTokens.DANGLING_NEWLINE; }
<STRING_DOUBLE>     {DOUBLE_QUO}             { popState(); return CjTokens.CLOSING_QUOTE; }
<STRING_DOUBLE> {ESCAPE_SEQUENCE}  { return CjTokens.ESCAPE_SEQUENCE; }


<STRING_DOUBLE,STRING_SINGLE> {ESCAPE_SEQUENCE}  { return CjTokens.ESCAPE_SEQUENCE; }

<STRING_SINGLE, RAW_STRING_SINGLE ,HSAH_STRING_SINGLE> {REGULAR_STRING_PART_SINGLE}           {
//           popState();
//          System.out.println("STRING, RAW_STRING_DOUBLE,RAW_STRING_SINGLE ,HSAH_STRING_DOUBLE,HSAH_STRING_SINGLE");
          return CjTokens.REGULAR_STRING_PART; }
<STRING_DOUBLE, RAW_STRING_DOUBLE ,HSAH_STRING_DOUBLE> {REGULAR_STRING_PART_DOUBLE}           {
//           popState();
//          System.out.println("STRING, RAW_STRING_DOUBLE,RAW_STRING_SINGLE ,HSAH_STRING_DOUBLE,HSAH_STRING_SINGLE");
          return CjTokens.REGULAR_STRING_PART; }

<STRING_SINGLE,STRING_DOUBLE, RAW_STRING_DOUBLE,RAW_STRING_SINGLE> {SHORT_TEMPLATE_ENTRY}        {
                          int interpolationPrefix = 0;
                       for (int i = 0; i < yylength(); i++) {
                           if (yycharat(i) == '$') { interpolationPrefix++; }
                           else { break; }
                       }
                       int rest = yylength() - interpolationPrefix;
                       if (interpolationPrefix == requiredInterpolationPrefix) {
                           pushState(SHORT_TEMPLATE_ENTRY);
                           yypushback(rest);
                           return CjTokens.SHORT_TEMPLATE_ENTRY_START;
                       } else if (interpolationPrefix < requiredInterpolationPrefix) {
                           yypushback(rest);
                           return CjTokens.REGULAR_STRING_PART;
                       } else {
                           yypushback(requiredInterpolationPrefix + rest);
                           return CjTokens.REGULAR_STRING_PART;
                       }
                }


// Only *this* keyword is itself an expression valid in this position
// *null*, *true* and *false* are also keywords and expression, but it does not make sense to put them
// in a string template for it'd be easier to just type them in without a dollar
<SHORT_TEMPLATE_ENTRY> "this"          { popState(); return CjTokens.THIS_KEYWORD; }

<SHORT_TEMPLATE_ENTRY> {IDENTIFIER}    { popState(); return CjTokens.IDENTIFIER; }

<STRING_DOUBLE,STRING_SINGLE, RAW_STRING_DOUBLE,RAW_STRING_SINGLE > {LONELY_DOLLAR}               {

          return CjTokens.REGULAR_STRING_PART; }
<STRING_DOUBLE,STRING_SINGLE, RAW_STRING_DOUBLE,RAW_STRING_SINGLE> {LONG_TEMPLATE_ENTRY_START}   { pushState(LONG_TEMPLATE_ENTRY); return CjTokens.LONG_TEMPLATE_ENTRY_START; }

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

//{SHEBANG_COMMENT} {
//            if (zzCurrentPos == 0) {
//                return CjTokens.SHEBANG_COMMENT;
//            }
//            else {
//                yypushback(yylength() - 1);
//                return CjTokens.HASH;
//            }
//          }

{FLOAT_LITERAL}     { return CjTokens.FLOAT_LITERAL; }

//{INTEGER_LITERAL}\.\. { yypushback(2); return CjTokens.INTEGER_LITERAL; }
{INTEGER_LITERAL} { return CjTokens.INTEGER_LITERAL; }


{RUNE_LITERAL}   { return CjTokens.RUNE_LITERAL; }

{CHARACTER_BYTE_LITERAL}   { return CjTokens.CHARACTER_BYTE_LITERAL; }


"package"    { return CjTokens.PACKAGE_KEYWORD ;}


"import"   { return CjTokens.IMPORT_KEYWORD ;}


"prop"       { return CjTokens.PROP_KEYWORD ;}
"mut"        { return CjTokens.MUT_KEYWORD ;}

"interface"  { return CjTokens.INTERFACE_KEYWORD ;}
"continue"   { return CjTokens.CONTINUE_KEYWORD ;}

"return"     { return CjTokens.RETURN_KEYWORD ;}

"while"      { return CjTokens.WHILE_KEYWORD ;}
"break"      { return CjTokens.BREAK_KEYWORD ;}
"class"      { return CjTokens.CLASS_KEYWORD ;}
"extend"     { return CjTokens.EXTEND_KEYWORD ;}
"throw"      { return CjTokens.THROW_KEYWORD ;}
"false"      { return CjTokens.FALSE_KEYWORD ;}
"super"      { return CjTokens.SUPER_KEYWORD ;}


"init"       { return CjTokens.INIT_KEYWORD ;}
"synchronized"     { return CjTokens.SYNCHRONIZED_KEYWORD ;}
"spawn"       { return CjTokens.SPAWN_KEYWORD ;}
"unsafe"       { return CjTokens.UNSAFE_KEYWORD ;}
"foreign"       { return CjTokens.FOREIGN_KEYWORD ;}


"quote"         { return CjTokens.QUOTE_KEYWORD ;}



//"abc"      { return CjTokens.ABC_KEYWORD ;}
"macro"     { return CjTokens.MACRO_KEYWORD ;}
"public"     { return CjTokens.PUBLIC_KEYWORD ;}
"private"    { return CjTokens.PRIVATE_KEYWORD ;}
"protected"  { return CjTokens.PROTECTED_KEYWORD ;}
"static"    { return CjTokens.STATIC_KEYWORD ;}
  "internal"    { return CjTokens.INTERNAL_KEYWORD ;}
"redef"  { return CjTokens.REDEF_KEYWORD ;}
/*"open"       { return CjTokens.OPEN_KEYWORD ;}*/
"abstract"  { return CjTokens.ABSTRACT_KEYWORD ;}
"override"  { return CjTokens.OVERRIDE_KEYWORD ;}
"operator"  { return CjTokens.OPERATOR_KEYWORD ;}
"match"       { return CjTokens.MATCH_KEYWORD ;}
 "case"         { return CjTokens.CASE_KEYWORD ;}
"true"       { return CjTokens.TRUE_KEYWORD ;}
"this"       { return CjTokens.THIS_KEYWORD ;}
"enum"       { return CjTokens.ENUM_KEYWORD ;}
 "VArray"   { return CjTokens.VARRAY_KEYWORD ;}

"else"       { return CjTokens.ELSE_KEYWORD ;}
"try"        { return CjTokens.TRY_KEYWORD ;}
"catch"      { return CjTokens.CATCH_KEYWORD ;}
 "finally"   { return CjTokens.FINALLY_KEYWORD ;}
"let"        { return CjTokens.LET_KEYWORD ;}
"var"        { return CjTokens.VAR_KEYWORD ;}
"const"       { return CjTokens.CONST_KEYWORD ;}
"func"        { return CjTokens.FUNC_KEYWORD ;}
"for"        { return CjTokens.FOR_KEYWORD ;}
"is"         { return CjTokens.IS_KEYWORD ;}
"in"         { return CjTokens.IN_KEYWORD ;}
"if"         { return CjTokens.IF_KEYWORD ;}
"do"         { return CjTokens.DO_KEYWORD ;}
"as"         { return CjTokens.AS_KEYWORD ;}
"main"       { return CjTokens.MAIN_KEYWORD ;}
"struct"     { return CjTokens.STRUCT_KEYWORD ;}
"where"      { return CjTokens.WHERE_KEYWORD ;}
"type"         { return CjTokens.TYPE_KEYWORD ;}

"Int8"       { return CjTokens.INT8_KEYWORD ;}
"Int16"      { return CjTokens.INT16_KEYWORD ;}
"Int32"      { return CjTokens.INT32_KEYWORD ;}
"Int64"      { return CjTokens.INT64_KEYWORD ;}
"UInt8"      { return CjTokens.UINT8_KEYWORD ;}
"UInt16"     { return CjTokens.UINT16_KEYWORD ;}
"UInt32"     { return CjTokens.UINT32_KEYWORD ;}
"UInt64"     { return CjTokens.UINT64_KEYWORD ;}
"Float32"    { return CjTokens.FLOAT32_KEYWORD ;}
"Float16"    { return CjTokens.FLOAT16_KEYWORD ;}
"IntNative"  { return CjTokens.INTNATIVE_KEYWORD ;}
"UIntNative" { return CjTokens.UINTNATIVE_KEYWORD ;}
"Float64"    { return CjTokens.FLOAT64_KEYWORD ;}
"Bool"       { return CjTokens.BOOL_KEYWORD ;}
"Unit"       { return CjTokens.UNIT_KEYWORD ;}
"Rune"       { return CjTokens.RUNE_KEYWORD ;}
"Nothing"    { return CjTokens.NOTHING_KEYWORD; }
"This"    { return CjTokens.THIS_KEYWORD_UPPER; }

"_"            { return CjTokens.UNDERLINE ;}

{FIELD_IDENTIFIER} { return CjTokens.FIELD_IDENTIFIER; }
 {IDENTIFIER}     { return CjTokens.IDENTIFIER; }
\!in{IDENTIFIER_PART}        { yypushback(3); return CjTokens.EXCL; }
\!is{IDENTIFIER_PART}        { yypushback(3); return CjTokens.EXCL; }


"~"        { return CjTokens.TILDE  ; }
"<:"        { return CjTokens.LTCOLON  ; }


"++"         { return CjTokens.PLUSPLUS  ; }
"--"         { return CjTokens.MINUSMINUS; }
"<="         { return CjTokens.LTEQ      ; }
//">="         { return CjTokens.GT_EQ      ; }
"=="         { return CjTokens.EQEQ      ; }
"!="         { return CjTokens.EXCLEQ    ; }
"&&"         { return CjTokens.ANDAND    ; }
"&"          { return CjTokens.AND       ; }
"||"         { return CjTokens.OROR      ; }
"|"          { return CjTokens.OR        ; }
"^"          { return CjTokens.XOR       ; }
"*="         { return CjTokens.MULTEQ    ; }
"/="         { return CjTokens.DIVEQ     ; }
"%="         { return CjTokens.PERCEQ    ; }
"+="         { return CjTokens.PLUSEQ    ; }
"-="         { return CjTokens.MINUSEQ   ; }
"->"         { return CjTokens.ARROW     ; }
"~>"         { return CjTokens.COMPOSITION     ; }
"|>"         { return CjTokens.PIPELINE     ; }
"=>"         { return CjTokens.DOUBLE_ARROW; }
"<-"     { return CjTokens.LEFT_ARROW; }
".."         { return CjTokens.RANGE     ; }
"..="         { return CjTokens.RANGEEQ     ; }
"**="        { return CjTokens.MULMULEQ  ; }
"&&="        { return CjTokens.ANDANDEQ  ; }
 "||="        { return CjTokens.OROREQ  ; }
"["          { return CjTokens.LBRACKET  ; }
"]"          { return CjTokens.RBRACKET  ; }
"{"          { return CjTokens.LBRACE    ; }
"}"          { return CjTokens.RBRACE    ; }
"("          { return CjTokens.LPAR      ; }
")"          { return CjTokens.RPAR      ; }
"."          { return CjTokens.DOT       ; }
"*"          { return CjTokens.MUL       ; }
"**"        { return CjTokens.MULMUL    ; }
"+"          { return CjTokens.PLUS      ; }
"-"          { return CjTokens.MINUS     ; }
"!"          { return CjTokens.EXCL      ; }
"/"          { return CjTokens.DIV       ; }
"%"          { return CjTokens.PERC      ; }
"<"          { return CjTokens.LT        ; }
">"          { return CjTokens.GT        ; }
"$"          { return CjTokens.DOLLAR        ; }

"@"          { return CjTokens.AT; }
//"??"          { return CjTokens.COALESCING     ; }
"?"           { return CjTokens.QUEST     ; }
"?."          { return CjTokens.SAFE_ACCESS     ; }
"?["          { return CjTokens.SAFE_INDEXEX     ; }
//"?("          { return CjTokens.SAFE_CALL    ; }
"?{"          { return CjTokens.SAFE_LAMBDA    ; }


":"          { return CjTokens.COLON     ; }

";"          { return CjTokens.SEMICOLON ; }
"="          { return CjTokens.EQ        ; }
","          { return CjTokens.COMMA     ; }
"|="       { return CjTokens.OREQ     ; }
"^="       { return CjTokens.XOREQ     ; }
"&="       { return CjTokens.ANDEQ     ; }
"<<="      { return CjTokens.LTLTEQ     ; }
//">>="      { return CjTokens.GTGTEQ     ; }
// ">>"     { return CjTokens.GTGT     ; }
 "<<"     { return CjTokens.LTLT     ; }


{LONELY_BACKTICK} {

          pushState(UNMATCHED_BACKTICK);

          return TokenType.BAD_CHARACTER;
      }

// error fallback
[\s\S]       {


          return TokenType.BAD_CHARACTER;

      }
// error fallback for exclusive states
<STRING_DOUBLE,STRING_SINGLE, RAW_STRING_DOUBLE,RAW_STRING_SINGLE, SHORT_TEMPLATE_ENTRY, BLOCK_COMMENT, DOC_COMMENT , HSAH_STRING_DOUBLE,HSAH_STRING_SINGLE> .
             {


          return TokenType.BAD_CHARACTER;


      }

