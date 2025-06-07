# CangJieLexer.flex 文件文档

## 文件概述

`CangJieLexer.flex` 是仓颉编程语言的词法分析器定义文件，使用 JFlex 工具生成 Java 词法分析器。该文件定义了如何将源代码文本分解为标记（tokens），是编译器前端的重要组成部分。

## 文件结构

JFlex 文件由三个主要部分组成，以 `%%` 分隔：

1. **用户代码段**：包含包声明、导入和辅助类/方法定义
2. **选项和声明段**：包含 JFlex 选项和正则表达式定义
3. **词法规则段**：定义具体的词法分析规则

## 用户代码段

### 包和导入声明

```java
package cn.cangnova.cangjie.lexer;
import com.intellij.psi.*;
import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.containers.Stack;
import cn.cangnova.cangjie.lexer.CjTokens;
```

### State 类

`State` 类用于管理词法分析器的状态，存储：
- 当前状态 ID
- 左大括号计数（用于嵌套结构）
- 所需插值前缀
- 状态名称（调试用）

### 状态管理方法

- `pushState()`：将当前状态压入栈，并切换到新状态
- `popState()`：从栈中弹出状态并恢复
- `getState()`：获取当前状态
- `commentStateToTokenType()`：将注释状态转换为相应的标记类型

## 选项和声明段

### JFlex 选项

- `%unicode`：支持 Unicode 字符
- `%class _CangJieLexer`：生成的词法分析器类名
- `%implements FlexLexer`：实现的接口
- `%function advance`：主扫描方法名
- `%type IElementType`：返回的标记类型
- `%scanerror CangJieLexerException`：扫描错误类

### 状态声明

#### 排他状态 (%xstate)

排他状态下，只有特定为该状态定义的规则才会生效：

- `STRING_PREFIX`：字符串前缀状态
- `STRING_SINGLE/DOUBLE`：单/双引号字符串状态
- `RAW_STRING_SINGLE/DOUBLE`：原始字符串状态
- `HSAH_STRING_SINGLE/DOUBLE`：带井号的字符串状态
- `SHORT_TEMPLATE_ENTRY`：短字符串模板状态
- `BLOCK_COMMENT/DOC_COMMENT`：注释状态

#### 包含状态 (%state)

包含状态下，未指定状态的规则（初始状态规则）仍然有效：

- `LONG_TEMPLATE_ENTRY`：长字符串模板状态
- `UNMATCHED_BACKTICK`：未匹配的反引号状态

### 正则表达式定义

#### 数字相关

```
DIGIT=[0-9]                            // 基本数字
DIGIT_OR_UNDERSCORE=[0-9_]             // 数字或下划线（用于分隔）
DIGITS={DIGIT}({DIGIT_OR_UNDERSCORE}*{DIGIT})?  // 完整数字序列
HEX_DIGIT=[0-9A-Fa-f]                  // 十六进制数字
BIN_DIGIT=[0-1]                        // 二进制数字
OCT_DIGIT=[0-7]                        // 八进制数字
```

#### 标识符相关

```
LETTER = [:letter:]|_                  // 字母或下划线
IDENTIFIER_PART=[:digit:]|{LETTER}     // 标识符组成部分
IDENTIFIER_START={LETTER}              // 标识符起始字符
PLAIN_IDENTIFIER={IDENTIFIER_START}{IDENTIFIER_PART}*  // 普通标识符
ESCAPED_IDENTIFIER = `[^`\n]+`         // 反引号包围的标识符
IDENTIFIER={ESCAPED_IDENTIFIER}|{PLAIN_IDENTIFIER}  // 完整标识符
FIELD_IDENTIFIER = \${IDENTIFIER}      // 字段标识符
```

#### 字符串相关

```
ESCAPE_SEQUENCE=\\(u\{ {HEX_DIGIT}{1,8} \} | [^\n])  // 转义序列
SINGLE_CHAR=[^'\\\r\n]                 // 单个字符（非引号和转义）
THREE_QUO_SINGLE = (\'\'\')            // 三个单引号（多行字符串）
THREE_QUO_DOUBLE = (\"\"\")            // 三个双引号（多行字符串）
HASH_QUO_SINGLE = (#+\')               // 井号加单引号（原始字符串）
HASH_QUO_DOUBLE = (#+\")               // 井号加双引号（原始字符串）
```

#### 字符串插值相关

```
REGULAR_STRING_PART_DOUBLE=[^\\\"\n\$]+  // 双引号字符串内容
REGULAR_STRING_PART_SINGLE=[^\\\'\n\$]+  // 单引号字符串内容
INTERPOLATION = \$+                     // 字符串插值标记
SHORT_TEMPLATE_ENTRY={INTERPOLATION}{IDENTIFIER}  // 短模板形式
LONG_TEMPLATE_ENTRY_START=\$\{         // 长模板开始
```

## 词法规则段

### 字符串处理规则

#### 原始字符串（带井号）

```
{HASH_QUO_SINGLE} {
    lBraceCount = yytext().length() - 1;  // 记录井号数量
    pushState("多行字符串字面量", HSAH_STRING_SINGLE);
    return CjTokens.OPEN_QUOTE;
}
```

原始字符串中的特殊字符处理：
- 换行符保留为字符串的一部分
- 引号和反斜杠作为普通字符处理
- 结束标记必须与开始标记有相同数量的井号

#### 多行字符串

```
{THREE_QUO_SINGLE} {
    pushState("RAW_STRING_SINGLE", RAW_STRING_SINGLE);
    return CjTokens.OPEN_QUOTE;
}
```

多行字符串特性：
- 允许包含换行符
- 单个引号不会结束字符串
- 必须使用三个引号结束

#### 单行字符串

```
{SINGLE_QUO} {
    pushState("STRING_SINGLE", STRING_SINGLE);
    return CjTokens.OPEN_QUOTE;
}
```

单行字符串特性：
- 不允许包含未转义的换行符
- 支持转义序列
- 单个引号结束字符串

### 字符串插值处理

#### 短模板形式 ($identifier)

```
<STRING_SINGLE,STRING_DOUBLE,RAW_STRING_DOUBLE,RAW_STRING_SINGLE> {SHORT_TEMPLATE_ENTRY} {
    // 计算$符号数量并处理
    ...
}
```

#### 长模板形式 (${expression})

```
<STRING_DOUBLE,STRING_SINGLE,RAW_STRING_DOUBLE,RAW_STRING_SINGLE> {LONG_TEMPLATE_ENTRY_START} {
    pushState("字符串模板", LONG_TEMPLATE_ENTRY);
    return CjTokens.LONG_TEMPLATE_ENTRY_START;
}
```

### 注释处理

```
"/**/" {  // 空文档注释
    return CjTokens.BLOCK_COMMENT;
}

"/**" {  // 文档注释开始
    pushState("DOC_COMMENT", DOC_COMMENT);
    commentDepth = 0;
    commentStart = getTokenStart();
}

"/*" {  // 块注释开始
    pushState("BLOCK_COMMENT", BLOCK_COMMENT);
    commentDepth = 0;
    commentStart = getTokenStart();
}
```

注释处理特性：
- 支持嵌套注释
- 区分普通块注释和文档注释
- 处理文件结束情况

### 关键字和标识符

文件包含大量关键字定义，例如：

```
"package"    { return CjTokens.PACKAGE_KEYWORD; }
"import"     { return CjTokens.IMPORT_KEYWORD; }
"class"      { return CjTokens.CLASS_KEYWORD; }
```

以及标识符处理：

```
{FIELD_IDENTIFIER} { return CjTokens.FIELD_IDENTIFIER; }
{IDENTIFIER}       { return CjTokens.IDENTIFIER; }
```

### 错误处理

```
// 未匹配的反引号
{LONELY_BACKTICK} {
    pushState(UNMATCHED_BACKTICK);
    return TokenType.BAD_CHARACTER;
}

// 通用错误处理
[\s\S] {
    String errorMessage = String.format("Unexpected character '%s' at position %d", yytext(), getTokenStart());
    throw new CangJieLexerException(errorMessage, getTokenStart());
}
```

状态特定的错误处理提供更详细的错误信息，有助于调试。

## JFlex 规则总结

1. **规则优先级**：
   - 最长匹配优先
   - 同等长度时，先定义的规则优先

2. **状态管理**：
   - 使用状态栈管理嵌套结构
   - 区分排他状态和包含状态

3. **动作执行**：
   - 每个规则匹配后执行相应的 Java 代码
   - 通常返回对应的标记类型

4. **错误处理**：
   - 提供详细的错误信息
   - 区分不同状态下的错误 