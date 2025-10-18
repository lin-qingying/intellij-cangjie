/*
 * CangJie语言词法分析器定义
 * =======================
 * 
 * 这是CangJie编程语言的JFlex词法分析器定义文件。
 * 词法分析器负责将源代码分解成一系列标记(tokens)，供解析器进行处理。
 *
 * JFlex文件结构规则：
 * ----------------
 * JFlex文件由三个部分组成，用 % % 分隔：
 * 
 * 1. 用户代码部分（第一部分）
 *    - 包声明和导入语句
 *    - 用户自定义的类和方法
 *    - 在 %{ 和 %} 之间的代码会被直接复制到生成的词法分析器类中
 * 
 * 2. 选项和声明部分（第二部分）
 *    - JFlex选项（如 %unicode, %class 等）
 *    - 状态声明（%state, %xstate）
 *    - 宏定义
 * 
 * 3. 词法规则部分（第三部分）
 *    - 模式和动作对
 *    - 每个规则格式：pattern { java code }
 *
 * JFlex选项说明：
 * -------------
 * %unicode     - 启用完整的Unicode支持
 * %class       - 指定生成的词法分析器类名
 * %implements  - 指定要实现的接口
 * %function    - 指定词法分析的主方法名
 * %type        - 指定返回的标记类型
 * %state       - 声明包容性状态
 * %xstate      - 声明排他性状态
 * %eof{        - 指定文件结束时的操作
 *
 * 状态系统说明：
 * -----------
 * 词法分析器使用状态机来处理复杂的标记：
 * 1. 初始状态（YYINITIAL）- 默认状态
 * 2. 排他性状态（%xstate）：
 *    - STRING_PREFIX: 字符串前缀处理
 *    - STRING_SINGLE: 单引号字符串处理
 *    - STRING_DOUBLE: 双引号字符串处理
 *    - RAW_STRING_SINGLE: 原始单引号字符串
 *    - RAW_STRING_DOUBLE: 原始双引号字符串
 *    - HSAH_STRING_SINGLE: 哈希单引号字符串
 *    - HSAH_STRING_DOUBLE: 哈希双引号字符串
 *    - SHORT_TEMPLATE_ENTRY: 简单字符串插值
 *    - BLOCK_COMMENT: 块注释
 *    - DOC_COMMENT: 文档注释
 * 3. 状态转换：
 *    - pushState(): 压入新状态
 *    - popState(): 恢复之前的状态
 *    - yybegin(): 直接切换到指定状态
 *
 * 宏定义规则：
 * ----------
 * 1. 字符类：
 *    DIGIT               = [0-9]                    数字字符
 *    DIGIT_OR_UNDERSCORE = [0-9_]                  数字或下划线
 *    LETTER             = [:letter:]|_             字母或下划线
 *    WHITE_SPACE_CHAR   = [\ \n\t\f]              空白字符
 * 
 * 2. 标识符：
 *    IDENTIFIER_START    = {LETTER}                标识符起始字符
 *    IDENTIFIER_PART    = [:digit:]|{LETTER}      标识符组成部分
 *    PLAIN_IDENTIFIER   = {IDENTIFIER_START}{IDENTIFIER_PART}*  普通标识符
 *    ESCAPED_IDENTIFIER = `[^`\n]+`               转义标识符
 *    FIELD_IDENTIFIER   = \${IDENTIFIER}          字段标识符
 * 
 * 3. 数值字面量：
 *    INTEGER_LITERAL    = 支持四种进制（十进制、十六进制、二进制、八进制）
 *    FLOAT_LITERAL     = 支持小数点和科学计数法
 *    
 * 4. 字符串相关：
 *    ESCAPE_SEQUENCE    = \\(u\{HEX_DIGIT{1,8}\}|[^\n])  转义序列
 *    STRING_CONTENT    = [^\\\"'\n\$]+            字符串内容
 *    INTERPOLATION     = \$+                      字符串插值标记
 *
 * 错误处理策略：
 * -----------
 * 1. 词法错误抛出CangJieLexerException异常
 * 2. 未匹配字符返回BAD_CHARACTER标记
 * 3. 状态相关的错误提供详细的错误信息
 * 4. 文件结束检查确保所有状态正确结束
 *
 * 标记类型系统：
 * -----------
 * 1. 关键字标记：各种语言关键字
 * 2. 标识符标记：普通标识符、字段标识符等
 * 3. 字面量标记：数值、字符串、字符等
 * 4. 运算符标记：算术、逻辑、位运算等
 * 5. 分隔符标记：括号、逗号、分号等
 * 6. 注释标记：行注释、块注释、文档注释
 * 7. 特殊标记：空白字符、错误字符等
 */

package org.cangnova.cangjie.lexer;
import com.intellij.psi.*;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.containers.Stack;
import com.intellij.lexer.FlexLexer;

%%

%unicode
%class _CangJieLexer
%implements FlexLexer

%{
 public _CangJieLexer() {
    this((java.io.Reader)null);
 }
%}

%{
    private static final class State {
        final int lBraceCount;
        final int requiredInterpolationPrefix;
        final int state;
        final String name;

        public State(int state, int lBraceCount, int requiredInterpolationPrefix) {
            this.name = "无";
            this.state = state;
            this.lBraceCount = lBraceCount;
            this.requiredInterpolationPrefix = requiredInterpolationPrefix;
        }

        public State(String name, int state, int lBraceCount, int requiredInterpolationPrefix) {
            this.name = name;
            this.state = state;
            this.lBraceCount = lBraceCount;
            this.requiredInterpolationPrefix = requiredInterpolationPrefix;
        }

        @Override
        public String toString() {
            return "yystate = " + state +
                (lBraceCount == 0 ? "" : ", lBraceCount = " + lBraceCount) +
                (requiredInterpolationPrefix == -1 ? "" : ", requiredInterpolationPrefix = " + requiredInterpolationPrefix);
        }
    }

    private final Stack<State> states = new Stack<State>();
    private int lBraceCount;
    private int requiredInterpolationPrefix;
    private int commentStart;
    private int commentDepth;

    private void pushState(int state) {
        pushState("无",state);
    }

    private void pushState(String name, int state) {
        states.push(new State(name,yystate(), lBraceCount, requiredInterpolationPrefix));
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

    private State getState() {
        return states.isEmpty() ? null : states.peek();
    }

    private IElementType commentStateToTokenType(int state) {
        switch (state) {
            case BLOCK_COMMENT:
                return CjTokens.BLOCK_COMMENT;
            case DOC_COMMENT:
                return CjTokens.DOC_COMMENT;
            default:
                throw new IllegalArgumentException("Unexpected comment state: " + state);
        }
    }
%}

%scanerror CangJieLexerException

%function advance
%type IElementType
%eof{
  return;
%eof}

%xstate STRING_PREFIX  HSAH_STRING_SINGLE  HSAH_STRING_DOUBLE  STRING_SINGLE  STRING_DOUBLE  RAW_STRING_SINGLE RAW_STRING_DOUBLE SHORT_TEMPLATE_ENTRY BLOCK_COMMENT DOC_COMMENT
%state LONG_TEMPLATE_ENTRY UNMATCHED_BACKTICK

/* 数值字面量定义
 * ===========
 * 支持以下类型的数值：
 * 1. 整数字面量：
 *    - 十进制：123, 1_000_000
 *    - 十六进制：0xFF, 0x1A_2B
 *    - 二进制：0b1010, 0b1010_0101
 *    - 八进制：0o777, 0o77_77
 *    - 可选类型后缀：u8, u16, u32, u64, i8, i16, i32, i64
 * 
 * 2. 浮点数字面量：
 *    - 十进制：123.45, .45, 1e10, 1.2e-10
 *    - 十六进制：0x1.8p1, 0x1.0p-1
 *    - 可选类型后缀：f32, f16, f64
 */

/* 数字相关定义
 * ==========
 * 支持以下数字格式：
 * 1. 整数：十进制、十六进制、二进制、八进制
 * 2. 浮点数：小数点表示、科学计数法
 * 3. 数字可以用下划线分隔以提高可读性
 */

/* 空白字符
 * =======
 */
WHITE_SPACE_CHAR=[\ \n\t\f]          // 空格、换行、制表符、换页符

/* 标识符相关定义
 * ===========
 * 支持以下类型的标识符：
 * 1. 普通标识符：字母或下划线开头，后跟字母、数字、下划线
 * 2. 转义标识符：反引号包围的任意字符序列
 * 3. 字段标识符：以$开头的标识符
 */

// 标识符基本组成
LETTER = [:letter:]|_                 // 字母或下划线
IDENTIFIER_PART=[:digit:]|{LETTER}    // 标识符组成部分：数字、字母或下划线
IDENTIFIER_START={LETTER}             // 标识符起始字符：字母或下划线

// 标识符类型

PLAIN_IDENTIFIER={IDENTIFIER_START}{IDENTIFIER_PART}*  // 普通标识符
BOOLEAN_LITERAL= true | false         // 布尔字面量
ESCAPED_IDENTIFIER = `[^`\n]+`       // 反引号包围的标识符
IDENTIFIER={ESCAPED_IDENTIFIER}|{PLAIN_IDENTIFIER}  // 完整标识符定义
FIELD_IDENTIFIER = \${IDENTIFIER}     // 字段标识符（以$开头）

/* 注释
 * ====
 */
EOL_COMMENT="/""/"[^\n]*             // 单行注释（到行尾）

/* 字符和字符串相关定义
 * ================
 * 支持以下特性：
 * 1. 转义序列
 * 2. Unicode转义
 * 3. 原始字符串
 * 4. 字符串插值
 */

// 转义序列
ESCAPE_SEQUENCE=\\(u\{ {HEX_DIGIT}{1,8} \} | [^\n])  // 支持Unicode转义和普通转义

// 字节字符字面量
CHARACTER_BYTE_LITERAL = {CHARACTER_BYTE_SINGLE_LITERAL}  // 字节字符（b前缀）
CHARACTER_BYTE_SINGLE_LITERAL = b \'  ({SINGLE_CHAR}  | {ESCAPE_SEQUENCE})*  \'

// 基本字符定义
SINGLE_CHAR=[^'\\\r\n]               // 单个字符（非引号、反斜杠、回车换行）

// 转义序列组合
ESCAPE_SEQ  = ({ESCAPE_SEQUENCE} | {ESCAPED_IDENTIFIER})  // 转义序列或转义标识符

// 字符字面量
RUNE_LITERAL = {RUNE_SINGLE_LITERAL} | {RUNE_DOUBLE_LITERAL}  // 字符字面量（支持单双引号）
RUNE_SINGLE_LITERAL = r \'  ({SINGLE_CHAR} | {ESCAPE_SEQ})*  \'  // 单引号字符
RUNE_DOUBLE_LITERAL = r \" ({SINGLE_CHAR} | {ESCAPE_SEQ})* \"   // 双引号字符

/* 字符串引号定义
 * ===========
 * 支持多种字符串界定符：
 * 1. 三引号字符串（原始字符串）
 * 2. 哈希字符串（可变长度分隔符）
 * 3. 普通字符串（单引号或双引号）
 */

// 三引号字符串
THREE_QUO_SINGLE =   (\'\'\')        // 三个单引号
THREE_OR_SINGLE_MORE_QUO = ({THREE_QUO_SINGLE}\'*)  // 三个或更多单引号
THREE_QUO_DOUBLE =   (\"\"\")        // 三个双引号
THREE_OR_DOUBLE_MORE_QUO = ({THREE_QUO_DOUBLE}\"*)  // 三个或更多双引号

// 哈希字符串
HASH_QUO_SINGLE = (#+\')             // 哈希加单引号（开始）
HSAH_OR_SINGLE_MORE_QUO = (\'#+)     // 单引号加哈希（结束）
HASH_QUO_DOUBLE = (#+\")             // 哈希加双引号（开始）
HSAH_OR_DOUBLE_MORE_QUO = (\"#+)     // 双引号加哈希（结束）

// 基本引号
SINGLE_QUO = \'                      // 单引号
DOUBLE_QUO = \"                      // 双引号

/* 字符串内容和插值
 * =============
 */

// 字符串内容
REGULAR_STRING_PART_DOUBLE=[^\\\"\n\$]+  // 双引号字符串内容（非转义字符）
REGULAR_STRING_PART_SINGLE=[^\\\'\n\$]+  // 单引号字符串内容（非转义字符）
INTERPOLATION = \$+                      // 字符串插值标记

// 字符串模板
SHORT_TEMPLATE_ENTRY={INTERPOLATION}{IDENTIFIER}  // 简单模板插值（$标识符）

// 特殊标记
LONELY_DOLLAR=\$                     // 单独的美元符号
LONG_TEMPLATE_ENTRY_START=\$\{       // 长模板插值开始标记
LONELY_BACKTICK=`                    // 单独的反引号

/* 数字相关定义
 * ==========
 * 支持以下数字格式：
 * 1. 整数：十进制、十六进制、二进制、八进制
 * 2. 浮点数：小数点表示、科学计数法
 * 3. 数字可以用下划线分隔以提高可读性
 */

// 基本数字字符

DIGIT=[0-9]                           // 单个数字字符
DIGIT_OR_UNDERSCORE=[0-9_]           // 数字或下划线（用于分隔数字）

DIGITS_WITH_TRAILING_UNDERSCORE={DIGIT}{DIGIT_OR_UNDERSCORE}*  // 数字序列，允许尾随下划线

// 不同进制的数字
HEX_DIGIT=[0-9A-Fa-f]                // 十六进制数字（0-9和A-F）
HEX_DIGIT_OR_UNDERSCORE=[0-9A-Fa-f_] // 十六进制数字或下划线
HEX_DIGITS_WITH_TRAILING_UNDERSCORE = {HEX_DIGIT}{HEX_DIGIT_OR_UNDERSCORE}*
BIN_DIGIT=[0-1]                      // 二进制数字（0和1）
BIN_DIGIT_OR_UNDERSCORE=[01_]        // 二进制数字或下划线
BIN_DIGITS_WITH_TRAILING_UNDERSCORE = {BIN_DIGIT}{BIN_DIGIT_OR_UNDERSCORE}*

OCT_DIGIT=[0-7]                      // 八进制数字（0-7）
OCT_DIGIT_OR_UNDERSCORE=[0-7_]       // 八进制数字或下划线
OCT_DIGITS_WITH_TRAILING_UNDERSCORE = {OCT_DIGIT}{OCT_DIGIT_OR_UNDERSCORE}*

// 整数类型后缀
INTEGER_SUFFIX=(u8|u16|u32|u64|i8|i16|i32|i64)  // 整数类型后缀

// 整数字面量（支持所有进制）
INTEGER_LITERAL=({DECIMAL_INTEGER_LITERAL}|{HEX_INTEGER_LITERAL}|{BIN_INTEGER_LITERAL}|{OCT_INTEGER_LITERAL}){INTEGER_SUFFIX}?

// 各种进制的整数表示
DECIMAL_INTEGER_LITERAL=0(_*)?|[1-9]{DIGIT_OR_UNDERSCORE}*  // 十进制整数
HEX_INTEGER_LITERAL=0[Xx]{HEX_DIGITS_WITH_TRAILING_UNDERSCORE}  // 十六进制整数
BIN_INTEGER_LITERAL=0[Bb]{BIN_DIGITS_WITH_TRAILING_UNDERSCORE} // 二进制整数
OCT_INTEGER_LITERAL=0[Oo]{OCT_DIGITS_WITH_TRAILING_UNDERSCORE} // 八进制整数

// 浮点数类型后缀
FLOAT_SUFFIX=(f32|f16|f64)           // 浮点数类型后缀

// 浮点数字面量（支持十进制和十六进制）

FLOAT_LITERAL={ FLOAT_LITERAL_POINT} |{HEX_FLOATING_POINT_LITERAL}

// 十进制浮点数
FLOAT_LITERAL_POINT = ({FLOATING_POINT_LITERAL1}|{FLOATING_POINT_LITERAL2}|{FLOATING_POINT_LITERAL3}){FLOAT_SUFFIX}?
FLOATING_POINT_LITERAL1=({DIGITS_WITH_TRAILING_UNDERSCORE})"."({DIGITS_WITH_TRAILING_UNDERSCORE})({EXPONENT_PART})? // 123__.45__, 8__.1__
FLOATING_POINT_LITERAL2="."({DIGITS_WITH_TRAILING_UNDERSCORE})({EXPONENT_PART})?           // .45__, .45__e10__
FLOATING_POINT_LITERAL3=({DIGITS_WITH_TRAILING_UNDERSCORE})({EXPONENT_PART})               // 123__e10__
// 十六进制浮点数
HEX_FLOATING_POINT_LITERAL= {HEX_FLOATING_POINT_LITERAL1} |{HEX_FLOATING_POINT_LITERAL2} |{HEX_FLOATING_POINT_LITERAL3}
HEX_FLOATING_POINT_LITERAL1= 0[xX]{HEX_DIGITS_WITH_TRAILING_UNDERSCORE}"."({HEX_DIGITS_WITH_TRAILING_UNDERSCORE})[pP][-]?{DIGITS_WITH_TRAILING_UNDERSCORE}
HEX_FLOATING_POINT_LITERAL2= 0[xX]"."({HEX_DIGITS_WITH_TRAILING_UNDERSCORE})[pP][-]?{DIGITS_WITH_TRAILING_UNDERSCORE}
HEX_FLOATING_POINT_LITERAL3= 0[xX]{HEX_DIGITS_WITH_TRAILING_UNDERSCORE}[pP][-]?{DIGITS_WITH_TRAILING_UNDERSCORE}

// 指数部分
EXPONENT_PART=[Ee][-]?{DIGITS_WITH_TRAILING_UNDERSCORE}      // 科学计数法的指数部分，如e10__, e+10__, e-10__

%%

/* 词法规则部分
 * ==========
 * 本部分定义了词法分析器的所有规则，按以下类别组织：
 * 1. 字符串处理规则：包括各种字符串字面量和字符串插值
 * 2. 注释处理规则：包括单行注释、块注释和文档注释
 * 3. 基本词法单元：包括空白字符、标识符、关键字等
 * 4. 错误处理规则：处理未匹配字符和特殊状态错误
 */

/* 字符串处理规则
 * ============
 * 支持以下类型的字符串：
 * 1. 哈希字符串：#"..."# 或 #'...'#，支持可变数量的#作为分隔符
 * 2. 原始字符串："""...""" 或 '''...'''，不处理转义序列
 * 3. 普通字符串："..." 或 '...'，支持转义序列
 * 4. 字符串插值：${...}
 */

// 哈希字符串（单引号）处理
{HASH_QUO_SINGLE} {
    lBraceCount = yytext().length() - 1;  // 记录哈希字符数量
    pushState("多行字符串字面量",HSAH_STRING_SINGLE);
    return CjTokens.OPEN_QUOTE;
}

// 哈希字符串内容处理
<HSAH_STRING_SINGLE> {
    \n                { return CjTokens.REGULAR_STRING_PART; }  // 换行符处理
    \" | \'          { return CjTokens.REGULAR_STRING_PART; }  // 引号处理
    \\               { return CjTokens.REGULAR_STRING_PART; }  // 反斜杠处理
    {LONELY_DOLLAR}  { return CjTokens.REGULAR_STRING_PART; }
    {HSAH_OR_SINGLE_MORE_QUO} {  // 结束引号处理
        int lenght = yytext().length() - 1;
        int lBraceCount1 = getState() != null ? getState().lBraceCount : lBraceCount;
        if(lBraceCount1 == lenght){  // 哈希数量匹配
            popState();
            return CjTokens.CLOSING_QUOTE;
        }
        else if(lBraceCount1 < lenght){  // 哈希数量过多
            popState();
            yypushback(lenght - lBraceCount1);
            return CjTokens.CLOSING_QUOTE;
        }else {  // 哈希数量不足
            return CjTokens.REGULAR_STRING_PART;
        }
    }
}

// 哈希字符串（双引号）处理
{HASH_QUO_DOUBLE} {
    lBraceCount = yytext().length() - 1;  // 记录哈希字符数量
    pushState("HSAH_STRING_DOUBLE",HSAH_STRING_DOUBLE);
    return CjTokens.OPEN_QUOTE;
}

// 哈希字符串（双引号）内容处理
<HSAH_STRING_DOUBLE> {
    \n                { return CjTokens.REGULAR_STRING_PART; }  // 换行符处理
    \" | \'          { return CjTokens.REGULAR_STRING_PART; }  // 引号处理
    \\               { return CjTokens.REGULAR_STRING_PART; }  // 反斜杠处理
    {LONELY_DOLLAR}  { return CjTokens.REGULAR_STRING_PART; }

    {HSAH_OR_DOUBLE_MORE_QUO} {  // 结束引号处理
        int lenght = yytext().length() - 1;
        int lBraceCount1 = getState() != null ? getState().lBraceCount : lBraceCount;
        if(lBraceCount1 == lenght){  // 哈希数量匹配
            popState();
            return CjTokens.CLOSING_QUOTE;
        }
        else if(lBraceCount1 < lenght){  // 哈希数量过多
            popState();
            yypushback(lenght - lBraceCount1);
            return CjTokens.CLOSING_QUOTE;
        }else {  // 哈希数量不足
            return CjTokens.REGULAR_STRING_PART;
        }
    }
}

/* 原始字符串处理
 * ============
 * 原始字符串不处理转义序列，所有字符都按原样处理
 * 支持单引号和双引号两种形式：
 * 1. 单引号：'''...'''
 * 2. 双引号："""..."""
 */

// 原始字符串（单引号）处理
{THREE_QUO_SINGLE} {
    pushState("RAW_STRING_SINGLE",RAW_STRING_SINGLE);
    return CjTokens.OPEN_QUOTE;
}

// 原始字符串（单引号）内容处理
<RAW_STRING_SINGLE> {
    \n                { return CjTokens.REGULAR_STRING_PART; }  // 换行符处理
    \" | \'          { return CjTokens.REGULAR_STRING_PART; }  // 引号处理
    \\               { return CjTokens.REGULAR_STRING_PART; }  // 反斜杠处理
    {THREE_OR_SINGLE_MORE_QUO} {  // 结束引号处理
        int length = yytext().length();
        if (length <= 3) {  // 正好三个引号
            popState();
            return CjTokens.CLOSING_QUOTE;
        } else {  // 超过三个引号，回退多余部分
            yypushback(3);
            return CjTokens.REGULAR_STRING_PART;
        }
    }
}

// 原始字符串（双引号）处理
{THREE_QUO_DOUBLE} {
    pushState("RAW_STRING_DOUBLE",RAW_STRING_DOUBLE);
    return CjTokens.OPEN_QUOTE;
}

// 原始字符串（双引号）内容处理
<RAW_STRING_DOUBLE> {
    \n                { return CjTokens.REGULAR_STRING_PART; }  // 换行符处理
    \" | \'          { return CjTokens.REGULAR_STRING_PART; }  // 引号处理
    \\               { return CjTokens.REGULAR_STRING_PART; }  // 反斜杠处理
    {THREE_OR_DOUBLE_MORE_QUO} {  // 结束引号处理
        int length = yytext().length();
        if (length <= 3) {  // 正好三个引号
            popState();
            return CjTokens.CLOSING_QUOTE;
        } else {  // 超过三个引号，回退多余部分
            yypushback(3);
            return CjTokens.REGULAR_STRING_PART;
        }
    }
}

//b\" {
//    pushState(STRING);
//    return CjTokens.OPEN_QUOTE;
//}


 {SINGLE_QUO} {
    pushState("STRING_SINGLE",STRING_SINGLE);
    return CjTokens.OPEN_QUOTE;
}
<STRING_SINGLE> \n {
    popState();
    yypushback(1);
    return CjTokens.DANGLING_NEWLINE;
}
<STRING_SINGLE>     {SINGLE_QUO} {
    popState();
    return CjTokens.CLOSING_QUOTE;
}
<STRING_SINGLE> {ESCAPE_SEQUENCE} {
    return CjTokens.ESCAPE_SEQUENCE;
}

{DOUBLE_QUO} {
    pushState("STRING_DOUBLE",STRING_DOUBLE);
    return CjTokens.OPEN_QUOTE;
}
<STRING_DOUBLE> \n {
    popState();
    yypushback(1);
    return CjTokens.DANGLING_NEWLINE;
}
<STRING_DOUBLE>     {DOUBLE_QUO} {
    popState();
    return CjTokens.CLOSING_QUOTE;
}
<STRING_DOUBLE> {ESCAPE_SEQUENCE} {
    return CjTokens.ESCAPE_SEQUENCE;
}


<STRING_DOUBLE,STRING_SINGLE> {ESCAPE_SEQUENCE} {
    return CjTokens.ESCAPE_SEQUENCE;
}

<STRING_SINGLE, RAW_STRING_SINGLE ,HSAH_STRING_SINGLE> {REGULAR_STRING_PART_SINGLE} {
    return CjTokens.REGULAR_STRING_PART;
}
<STRING_DOUBLE, RAW_STRING_DOUBLE ,HSAH_STRING_DOUBLE> {REGULAR_STRING_PART_DOUBLE} {
    return CjTokens.REGULAR_STRING_PART;
}

<STRING_SINGLE,STRING_DOUBLE, RAW_STRING_DOUBLE,RAW_STRING_SINGLE> {SHORT_TEMPLATE_ENTRY} {
    int interpolationPrefix = 0;
    for (int i = 0; i < yylength(); i++) {
        if (yycharat(i) == '$') { interpolationPrefix++; }
        else { break; }
    }
    int rest = yylength() - interpolationPrefix;
    if (interpolationPrefix == requiredInterpolationPrefix) {
        pushState("SHORT_TEMPLATE_ENTRY",SHORT_TEMPLATE_ENTRY);
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
<SHORT_TEMPLATE_ENTRY> "this" {
    popState();
    return CjTokens.THIS_KEYWORD;
}

<SHORT_TEMPLATE_ENTRY> {IDENTIFIER} {
    popState();
    return CjTokens.IDENTIFIER;
}

<STRING_DOUBLE,STRING_SINGLE, RAW_STRING_DOUBLE,RAW_STRING_SINGLE > {LONELY_DOLLAR} {
    return CjTokens.REGULAR_STRING_PART;
}
<STRING_DOUBLE,STRING_SINGLE, RAW_STRING_DOUBLE,RAW_STRING_SINGLE> {LONG_TEMPLATE_ENTRY_START} {
    pushState( "字符串模板", LONG_TEMPLATE_ENTRY);
    return CjTokens.LONG_TEMPLATE_ENTRY_START;
}

<LONG_TEMPLATE_ENTRY> "{" {
    lBraceCount++;
    return CjTokens.LBRACE;
}
<LONG_TEMPLATE_ENTRY> "}" {
    if (lBraceCount == 0) {
        popState();
        return CjTokens.LONG_TEMPLATE_ENTRY_END;
    }
    lBraceCount--;
    return CjTokens.RBRACE;
}



/* 注释处理规则
 * ==========
 * 支持三种类型的注释：
 * 1. 空块注释：/**/
 * 2. 文档注释：/** ... */
 * 3. 块注释：/* ... */
 */

// 空块注释
"/**/" {
    return CjTokens.BLOCK_COMMENT;
}

// 文档注释
"/**" {
    pushState("DOC_COMMENT",DOC_COMMENT);
    commentDepth = 0;
    commentStart = getTokenStart();
}

// 块注释
"/*" {
    pushState("BLOCK_COMMENT",BLOCK_COMMENT);
    commentDepth = 0;
    commentStart = getTokenStart();
}

// 注释内容处理
<BLOCK_COMMENT, DOC_COMMENT> {
    "/*"     { commentDepth++; }  // 嵌套注释处理
    "*/"     {  // 注释结束处理
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
    <<EOF>>  {  // 文件结束处理
        int state = yystate();
        popState();
        zzStartRead = commentStart;
        return commentStateToTokenType(state);
    }
    [\s\S]   {}  // 其他字符处理
}

/* 基本词法单元
 * ==========
 */

// 空白字符
({WHITE_SPACE_CHAR})+ {
    return CjTokens.WHITE_SPACE;
}

// 行注释
{EOL_COMMENT} {
    return CjTokens.EOL_COMMENT;
}

// 数值字面量
{FLOAT_LITERAL} {
    return CjTokens.FLOAT_LITERAL;
}

{INTEGER_LITERAL} {
    return CjTokens.INTEGER_LITERAL;
}

// 字符字面量
{RUNE_LITERAL} {
    return CjTokens.RUNE_LITERAL;
}

{CHARACTER_BYTE_LITERAL} {
    return CjTokens.CHARACTER_BYTE_LITERAL;
}

/* 关键字
 * ======
 * 按功能分类：
 * 1. 包和模块相关
 * 2. 类型声明相关
 * 3. 控制流相关
 * 4. 修饰符
 * 5. 基本类型
 * 6. 变量声明
 * 7. 特殊关键字
 */

// 包和模块相关关键字
"package"    { return CjTokens.PACKAGE_KEYWORD; }
"import"     { return CjTokens.IMPORT_KEYWORD; }
"foreign"    { return CjTokens.FOREIGN_KEYWORD; }

// 类型声明相关关键字
"interface"  { return CjTokens.INTERFACE_KEYWORD; }
"class"      { return CjTokens.CLASS_KEYWORD; }
"enum"       { return CjTokens.ENUM_KEYWORD; }
"struct"     { return CjTokens.STRUCT_KEYWORD; }
"type"       { return CjTokens.TYPE_KEYWORD; }
"extend"     { return CjTokens.EXTEND_KEYWORD; }

// 控制流相关关键字
"if"         { return CjTokens.IF_KEYWORD; }
"else"       { return CjTokens.ELSE_KEYWORD; }
"for"        { return CjTokens.FOR_KEYWORD; }
"while"      { return CjTokens.WHILE_KEYWORD; }
"do"         { return CjTokens.DO_KEYWORD; }
"break"      { return CjTokens.BREAK_KEYWORD; }
"continue"   { return CjTokens.CONTINUE_KEYWORD; }
"return"     { return CjTokens.RETURN_KEYWORD; }
"throw"      { return CjTokens.THROW_KEYWORD; }
"try"        { return CjTokens.TRY_KEYWORD; }
"catch"      { return CjTokens.CATCH_KEYWORD; }
"finally"    { return CjTokens.FINALLY_KEYWORD; }
"match"      { return CjTokens.MATCH_KEYWORD; }
"case"       { return CjTokens.CASE_KEYWORD; }

// 修饰符关键字
"public"     { return CjTokens.PUBLIC_KEYWORD; }
"private"    { return CjTokens.PRIVATE_KEYWORD; }
"protected"  { return CjTokens.PROTECTED_KEYWORD; }
"internal"   { return CjTokens.INTERNAL_KEYWORD; }
"static"     { return CjTokens.STATIC_KEYWORD; }
"abstract"   { return CjTokens.ABSTRACT_KEYWORD; }
"override"   { return CjTokens.OVERRIDE_KEYWORD; }
"redef"      { return CjTokens.REDEF_KEYWORD; }
"operator"   { return CjTokens.OPERATOR_KEYWORD; }
"synchronized" { return CjTokens.SYNCHRONIZED_KEYWORD; }
"unsafe"     { return CjTokens.UNSAFE_KEYWORD; }
"mut"        { return CjTokens.MUT_KEYWORD; }

// 基本类型关键字
"Int8"       { return CjTokens.INT8_KEYWORD; }
"Int16"      { return CjTokens.INT16_KEYWORD; }
"Int32"      { return CjTokens.INT32_KEYWORD; }
"Int64"      { return CjTokens.INT64_KEYWORD; }
"UInt8"      { return CjTokens.UINT8_KEYWORD; }
"UInt16"     { return CjTokens.UINT16_KEYWORD; }
"UInt32"     { return CjTokens.UINT32_KEYWORD; }
"UInt64"     { return CjTokens.UINT64_KEYWORD; }
"Float16"    { return CjTokens.FLOAT16_KEYWORD; }
"Float32"    { return CjTokens.FLOAT32_KEYWORD; }
"Float64"    { return CjTokens.FLOAT64_KEYWORD; }
"IntNative"  { return CjTokens.INTNATIVE_KEYWORD; }
"UIntNative" { return CjTokens.UINTNATIVE_KEYWORD; }
"Bool"       { return CjTokens.BOOL_KEYWORD; }
"Unit"       { return CjTokens.UNIT_KEYWORD; }
"Rune"       { return CjTokens.RUNE_KEYWORD; }
"Nothing"    { return CjTokens.NOTHING_KEYWORD; }

// 变量声明关键字
"let"        { return CjTokens.LET_KEYWORD; }
"var"        { return CjTokens.VAR_KEYWORD; }
"const"      { return CjTokens.CONST_KEYWORD; }
"prop"       { return CjTokens.PROP_KEYWORD; }

// 特殊关键字
"func"       { return CjTokens.FUNC_KEYWORD; }
"init"       { return CjTokens.INIT_KEYWORD; }
"main"       { return CjTokens.MAIN_KEYWORD; }
"this"       { return CjTokens.THIS_KEYWORD; }
"This"       { return CjTokens.THIS_KEYWORD_UPPER; }
"super"      { return CjTokens.SUPER_KEYWORD; }
"true"       { return CjTokens.TRUE_KEYWORD; }
"false"      { return CjTokens.FALSE_KEYWORD; }
"is"         { return CjTokens.IS_KEYWORD; }
"in"         { return CjTokens.IN_KEYWORD; }
"as"         { return CjTokens.AS_KEYWORD; }
"where"      { return CjTokens.WHERE_KEYWORD; }
"spawn"      { return CjTokens.SPAWN_KEYWORD; }
"quote"      { return CjTokens.QUOTE_KEYWORD; }
"macro"      { return CjTokens.MACRO_KEYWORD; }
"VArray"     { return CjTokens.VARRAY_KEYWORD; }
"_"          { return CjTokens.UNDERLINE; }

// 标识符
{FIELD_IDENTIFIER} { return CjTokens.FIELD_IDENTIFIER; }
{IDENTIFIER}       { return CjTokens.IDENTIFIER; }

// 特殊标识符处理
\!in{IDENTIFIER_PART}  { yypushback(3); return CjTokens.EXCL; }
\!is{IDENTIFIER_PART}  { yypushback(3); return CjTokens.EXCL; }

/* 运算符和分隔符
 * ============
 * 按类型分类：
 * 1. 算术运算符
 * 2. 比较运算符
 * 3. 逻辑运算符
 * 4. 位运算符
 * 5. 赋值运算符
 * 6. 特殊运算符
 * 7. 分隔符
 */

// 算术运算符
"++"         { return CjTokens.PLUSPLUS; }
"--"         { return CjTokens.MINUSMINUS; }
"+"          { return CjTokens.PLUS; }
"-"          { return CjTokens.MINUS; }
"*"          { return CjTokens.MUL; }
"**"         { return CjTokens.MULMUL; }
"/"          { return CjTokens.DIV; }
"%"          { return CjTokens.PERC; }

// 比较运算符
"<="         { return CjTokens.LTEQ; }
"=="         { return CjTokens.EQEQ; }
"!="         { return CjTokens.EXCLEQ; }
"<"          { return CjTokens.LT; }
">"          { return CjTokens.GT; }
"<:"         { return CjTokens.LTCOLON; }

// 逻辑运算符
"&&"         { return CjTokens.ANDAND; }
"||"         { return CjTokens.OROR; }
"!"          { return CjTokens.EXCL; }

// 位运算符
"&"          { return CjTokens.AND; }
"|"          { return CjTokens.OR; }
"^"          { return CjTokens.XOR; }
"~"          { return CjTokens.TILDE; }
"<<"         { return CjTokens.LTLT; }

// 赋值运算符
"="          { return CjTokens.EQ; }
"+="         { return CjTokens.PLUSEQ; }
"-="         { return CjTokens.MINUSEQ; }
"*="         { return CjTokens.MULTEQ; }
"**="        { return CjTokens.MULMULEQ; }
"/="         { return CjTokens.DIVEQ; }
"%="         { return CjTokens.PERCEQ; }
"&="         { return CjTokens.ANDEQ; }
"|="         { return CjTokens.OREQ; }
"^="         { return CjTokens.XOREQ; }
"&&="        { return CjTokens.ANDANDEQ; }
"||="        { return CjTokens.OROREQ; }
"<<="        { return CjTokens.LTLTEQ; }

// 特殊运算符
"->"         { return CjTokens.ARROW; }
"=>"         { return CjTokens.DOUBLE_ARROW; }
"<-"         { return CjTokens.LEFT_ARROW; }
"~>"         { return CjTokens.COMPOSITION; }
"|>"         { return CjTokens.PIPELINE; }
".."         { return CjTokens.RANGE; }
"..="        { return CjTokens.RANGEEQ; }
"?."         { return CjTokens.SAFE_ACCESS; }
"?["         { return CjTokens.SAFE_INDEXEX; }
"?{"         { return CjTokens.SAFE_LAMBDA; }
"?"          { return CjTokens.QUEST; }
"..."        { return CjTokens.ELLIPSIS; }
// 分隔符
"["          { return CjTokens.LBRACKET; }
"]"          { return CjTokens.RBRACKET; }
"{"          { return CjTokens.LBRACE; }
"}"          { return CjTokens.RBRACE; }
"("          { return CjTokens.LPAR; }
")"          { return CjTokens.RPAR; }
"."          { return CjTokens.DOT; }
":"          { return CjTokens.COLON; }
";"          { return CjTokens.SEMICOLON; }
","          { return CjTokens.COMMA; }
"@"          { return CjTokens.AT; }
"@!"          { return CjTokens.ATEXCL; }
"$"          { return CjTokens.DOLLAR; }

/* 错误处理规则
 * ==========
 */

// 未匹配的反引号
{LONELY_BACKTICK} {
    pushState(UNMATCHED_BACKTICK);
    return TokenType.BAD_CHARACTER;
}

// 通用错误处理
[\s\S] {

    return TokenType.BAD_CHARACTER;
//    String errorMessage = String.format("Unexpected character '%s' at position %d", yytext(), getTokenStart());
//    throw new CangJieLexerException(errorMessage, getTokenStart());
}

// 特定状态的错误处理
<STRING_DOUBLE,STRING_SINGLE,RAW_STRING_DOUBLE,RAW_STRING_SINGLE,SHORT_TEMPLATE_ENTRY,BLOCK_COMMENT,DOC_COMMENT,HSAH_STRING_DOUBLE,HSAH_STRING_SINGLE> . {
//    String state = yystate() == STRING_DOUBLE ? "string" :
//                  yystate() == STRING_SINGLE ? "string" :
//                  yystate() == RAW_STRING_DOUBLE ? "raw string" :
//                  yystate() == RAW_STRING_SINGLE ? "raw string" :
//                  yystate() == SHORT_TEMPLATE_ENTRY ? "string template" :
//                  yystate() == BLOCK_COMMENT ? "block comment" :
//                  yystate() == DOC_COMMENT ? "doc comment" :
//                  yystate() == HSAH_STRING_DOUBLE ? "hash string" :
//                  yystate() == HSAH_STRING_SINGLE ? "hash string" : "unknown";
//    String errorMessage = String.format("Unexpected character '%s' in %s at position %d", yytext(), state, getTokenStart());
//    throw new CangJieLexerException(errorMessage, getTokenStart());
    return TokenType.BAD_CHARACTER;
}

