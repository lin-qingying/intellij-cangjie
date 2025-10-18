# IntelliJ PSI 中 AT (@) 符号的使用场景分析

本文档详细分析了 IntelliJ 平台仓颉语言插件 PSI 实现中 AT (@) 符号的所有使用场景。

## 1. 词法层面的 AT Token 定义

### 1.1 基本 Token 定义

**文件位置**: `psi/src/main/kotlin/org/cangnova/cangjie/lexer/CjTokens.java`

```java
// 基本 AT 符号
CjSingleValueToken AT = new CjSingleValueToken("AT", "@", AT_Id);
int AT_Id = 95;

// 编译时可见注解符号 (未在 PSI 中实现)
CjSingleValueToken ATEXCL = new CjSingleValueToken("ATEXCL", "@!", ATEXCL_Id);
int ATEXCL_Id = 107;
```

**说明**:
- `AT` - 基本的 @ 符号，用于注解和宏调用
- `ATEXCL` - @! 符号，用于编译时可见注解（C++ 编译器支持，PSI 未实现）

---

## 2. 语法层面的 AT 使用场景

### 2.1 注解声明 (Annotation)

**位置**: `CangJieParsing.kt` (第 1020-1074 行)

**语法结构**:
```cangjie
@AnnotationName
@AnnotationName[param1, param2]
@AnnotationName[name: "value"]
```

**解析逻辑**:
```kotlin
context(context: ErrorReportContext, parseContext: ParsingContext)
fun parseAnnotation(detector: ModifierDetector?): IElementType? {
    assert(_at(AT))

    if (nextRawToken == IDENTIFIER) {
        advance() // consume AT '@'

        // 解析类型引用
        parseUserType()

        // 解析注解参数 []
        if (at(LBRACKET)) {
            expressionParsing.parseValueArgumentList(LBRACKET, RBRACKET)
        }
    }

    // 判断是宏调用还是注解
    if (at(LPAR)) {
        // 有 () 则为宏调用
        return MACRO_EXPRESSION
    } else {
        // 否则为注解
        return ANNOTATION_ENTRY
    }
}
```

**使用场景**:
- 类、函数、变量等声明上的元数据标记
- 支持带参数和不带参数的注解

---

### 2.2 宏调用 (Macro Expression)

**位置**: `CangJieExpressionParsing.kt` (多处)

#### 2.2.1 作为表达式的宏调用

**位置**: `parseExpression()` 函数

**语法结构**:
```cangjie
@macroName(arg1, arg2)
```

**解析逻辑**:
```kotlin
context(context: ExpressionParseContext, errContext: ErrorReportContext)
fun parseExpression() {
    if (at(AT)) {
        parseMacroExpression()
        return
    }
    // ...
}
```

#### 2.2.2 在 Quote 表达式中的宏调用

**位置**: `parseQuoteExpression()` 函数

**语法结构**:
```cangjie
quote {
    @macroCall(params)
}
```

**解析逻辑**:
```kotlin
private fun parseQuoteExpression() {
    // ...
    if (at(AT) && lookahead(1) == IDENTIFIER) {
        parseMacroExpressionByQuoteParameters()
    }
    // ...
}
```

#### 2.2.3 作为声明的宏调用

**位置**: `parseMacroInputExprWithoutParensDeclaration()` 函数

**语法结构**:
```cangjie
@macroThatExpandsToDeclaration()
```

**解析逻辑**:
```kotlin
private fun parseMacroInputExprWithoutParensDeclaration(): IElementType? {
    return when (getTokenId()) {
        AT_Id -> with(ExpressionParseContext.MACRO_BACK_TOKEN) {
            parseMacroExpression()
        }
        // ...
    }
}
```

---

### 2.3 标签引用 (Label Reference)

**位置**: `CangJieExpressionParsing.kt` - `parseLabelReference()` 函数

**语法结构**:
```cangjie
break@labelName
continue@labelName
```

**解析逻辑**:
```kotlin
context(errContext: ErrorReportContext)
private fun parseLabelReference() {
    assert(_at(AT))
    val labelWrap = mark()
    val mark = mark()

    advance() // AT
    expect(IDENTIFIER, "Expecting label name")

    mark.done(LABEL)
    labelWrap.done(LABEL_REFERENCE)
}
```

**特殊规则**:
```kotlin
private fun parseLabelReferenceWithNoWhitespace() {
    if (at(AT) && !builder.newlineBeforeCurrentToken()) {
        if (WHITE_SPACE_OR_COMMENT_BIT_SET.contains(builder.rawLookup(-1))) {
            error("There should be no space or comments before '@' in label reference")
        }
        // ...
    }
}
```

**使用场景**:
- `break@` 和 `continue@` 语句中的标签引用
- @ 前不允许有空格或注释

---

### 2.4 在 Quote Token 中的使用

**位置**: `parseQuoteTokens()` 函数

**语法结构**:
```cangjie
quote {
    // ... tokens ...
    @embeddedMacro()
    // ... more tokens ...
}
```

**解析逻辑**:
```kotlin
private fun parseQuoteTokens() {
    val tokens = mark()
    while (atSet(QUOTE_TOKENS)) {
        if (at(AT) && lookahead(1) == IDENTIFIER) {
            with(ExpressionParseContext.DEFAULT) {
                parseMacroExpression()
            }
        } else {
            advance()
        }
    }
    tokens.done(QUOTE_TOKENS)
}
```

---

## 3. AT 符号的解析判断逻辑

### 3.1 区分注解和宏调用的逻辑

**当前 PSI 实现的判断规则**:

| 语法形式 | 解析结果 | 说明 |
|---------|---------|------|
| `@identifier` | `ANNOTATION_ENTRY` | 无参注解 |
| `@identifier[...]` | `ANNOTATION_ENTRY` | 带参数的注解 |
| `@identifier(...)` | `MACRO_EXPRESSION` | 宏调用 |

**关键判断代码**:
```kotlin
if (at(LPAR)) {
    // 有圆括号 → 宏调用
    return MACRO_EXPRESSION
} else {
    // 无圆括号 → 注解
    return ANNOTATION_ENTRY
}
```

### 3.2 ParsingContext 的影响

根据新增的 `ParsingContext`，可以控制 @ 符号的解析行为：

```kotlin
data class ParsingContext(
    val disableMacroParsing: Boolean = false,  // 禁用宏解析，全部解析为注解
    val allowMacroCallEverywhere: Boolean = false  // 允许所有位置的宏调用
)
```

**待实现的逻辑**:
```kotlin
fun parseAnnotation(detector: ModifierDetector?): IElementType? {
    // ...
    if (parseContext.disableMacroParsing) {
        // 强制解析为注解，即使有 ()
        return ANNOTATION_ENTRY
    }
    // 原有的判断逻辑
}
```

---

## 4. AT 符号使用场景汇总

| 使用场景 | 语法示例 | 解析节点类型 | 文件位置 |
|---------|---------|-------------|---------|
| **注解声明** | `@Deprecated` | `ANNOTATION_ENTRY` | CangJieParsing.kt:1020 |
| **带参数注解** | `@Test[name: "test"]` | `ANNOTATION_ENTRY` | CangJieParsing.kt:1035 |
| **宏调用** | `@macro(arg)` | `MACRO_EXPRESSION` | CangJieExpressionParsing.kt:parseMacroExpression |
| **标签引用** | `break@loop` | `LABEL_REFERENCE` | CangJieExpressionParsing.kt:parseLabelReference |
| **Quote中的宏** | `quote { @m() }` | `MACRO_EXPRESSION` | CangJieExpressionParsing.kt:parseQuoteExpression |
| **修饰符检测** | 解析修饰符时 | - | CangJieParsing.kt:956-969 |
| **声明解析映射** | AT_Id 映射 | `MacroExpressionParser` | CangJieParsing.kt:1486 |

---

## 5. 与 C++ 编译器的差异

### 5.1 PSI 未实现的 AT 相关功能

1. **编译时可见注解 (`@!`)**
   - C++ 支持: `@!CustomAnno`
   - PSI 状态: ❌ Token 定义存在但未实现解析

2. **Effect Handling 中的 perform**
   - C++ 支持: `perform someEffect()`（禁用状态）
   - PSI 状态: ❌ 未实现

3. **判断逻辑差异**
   - C++: 基于标识符名称判断（内置注解列表）
   - PSI: 基于后续语法结构判断（是否有圆括号）

### 5.2 建议改进

1. 实现 `ParsingContext.disableMacroParsing` 的完整逻辑
2. 添加对 `@!` 编译时可见注解的支持
3. 考虑添加内置注解名称列表，提高解析准确性
4. 完善标签引用的空格检查逻辑

---

## 6. 总结

IntelliJ PSI 中的 AT (@) 符号主要用于：

1. **注解声明** - 最常见的用法，用于元数据标记
2. **宏调用** - 带圆括号的宏展开调用
3. **标签引用** - break/continue 语句的标签
4. **Quote 表达式** - 在元编程中嵌入宏调用

PSI 实现基于**语法结构**（是否有圆括号）来区分注解和宏调用，这与 C++ 编译器基于**标识符名称**的判断方式不同。新增的 `ParsingContext` 提供了更灵活的控制机制，但需要完善实现以充分利用其功能。