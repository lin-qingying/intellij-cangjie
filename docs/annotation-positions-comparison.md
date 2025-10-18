# 内置注解 vs 自定义注解位置差异分析

本文档详细分析了仓颉语言 C++ 编译器和 IntelliJ PSI 实现中，内置注解与自定义注解在允许出现位置上的差异。

## 1. C++ 编译器中的注解位置差异

### 1.1 解析条件的关键差异

**内置注解** 和 **自定义注解** 在解析时有不同的条件判断：

```cpp
// ParseAnnotations.cpp
void ParserImpl::ParseAnnotations(PtrVector<Annotation>& annos)
{
    while (SeeingBuiltinAnnotation() ||                          // 内置注解：总是解析
           (this->enableCustomAnno && SeeingMacroCallDecl())) {  // 自定义注解：需要启用标志
        // ...
    }
}
```

### 1.2 位置限制对比表

| 位置 | 内置注解 | 自定义注解 | 关键代码位置 |
|------|---------|-----------|-------------|
| **顶层声明** | ✅ 总是可用 | ⚠️ 需要 `enableCustomAnno=true` | `ParseTopLevelDecls()` |
| **导入语句** | ✅ 总是可用 | ❌ **不可用** | `ParseImports.cpp` - 只检查内置注解 |
| **函数参数** | ✅ 总是可用 | ⚠️ 需要 `enableCustomAnno=true` | `ParseFuncParam()` |
| **Lambda表达式** | ✅ 总是可用 | ❌ **通常不可用** | `ParseAnnotationLambdaExpr()` 只处理内置 |
| **类成员** | ✅ 总是可用 | ⚠️ 需要 `enableCustomAnno=true` | `ParseDeclByModifiers()` |
| **属性访问器** | ✅ 总是可用 | ⚠️ 需要 `enableCustomAnno=true` | `ParsePropDecl()` |

### 1.3 特殊位置的限制详解

#### 1.3.1 导入语句前 - 只允许内置注解

```cpp
// ParseImports.cpp
void ParserImpl::ParseCommonImportSpec(PtrVector<ImportSpec>& imports, PtrVector<Annotation>& annos)
{
    while (SeeingImport2() || SeeingBuiltinAnnotation()) {  // 注意：只检查内置注解
        if (SeeingBuiltinAnnotation()) {
            ParseAnnotations(annos);
        }
        // ...
    }
}
```

**示例**：
```cangjie
// ✅ 内置注解可用
@Deprecated
import std.io

@When[DEBUG]
import debug.tools

// ❌ 自定义注解不可用
@CustomAnno  // 错误：不会被解析为注解
import my.module
```

#### 1.3.2 Lambda 表达式 - 主要用于内置注解

```cpp
// ParseAtom.cpp - Lambda注解只处理特定的内置注解
OwnedPtr<Expr> ParserImpl::ParseAnnotationLambdaExpr(bool isTailClosure)
{
    std::vector<OwnedPtr<Annotation>> annos;
    ParseAnnotations(annos);
    // ...
    for (auto& it : annos) {
        if (it->kind == AnnotationKind::NUMERIC_OVERFLOW) {  // 只处理 @Overflow
            ret->EnableAttr(Attribute::NUMERIC_OVERFLOW);
        } else if (it->kind == AnnotationKind::ENSURE_PREPARED_TO_MOCK) {  // 和 @Mock
            ret->EnableAttr(Attribute::MOCK_SUPPORTED);
        } else {
            DiagExpectedDeclaration(ret->begin, "lambda expression");  // 其他注解报错
        }
    }
}
```

**示例**：
```cangjie
// ✅ 内置注解可用
let handler = @Overflow[Panic] { x, y -> x + y }

// ❌ 自定义注解通常不可用
let processor = @CustomAnno { data -> process(data) }  // 错误：不支持的注解
```

### 1.4 自定义注解的特殊处理

当 `enableCustomAnno=false` 时，`@` 符号后的非内置标识符会被解析为**宏调用**而不是注解：

```cpp
// ParseDecl.cpp
OwnedPtr<FuncParam> ParserImpl::ParseFuncParam(ScopeKind scopeKind)
{
    // 如果看到 @ 符号但 enableCustomAnno=false，则解析为宏调用
    if (SeeingMacroCallDecl() && !this->enableCustomAnno) {
        return ParseMacroCall<MacroExpandParam>(scopeKind);
    }
    // 否则解析为注解
    OwnedPtr<FuncParam> param = MakeOwned<FuncParam>();
    ParseAnnotations(param->annotations);
    // ...
}
```

### 1.5 编译时可见注解 (`@!`) 的限制

`@!` 语法只能用于**自定义注解**，不能用于内置注解：

```cpp
// ParseCustomAnnotation - 只有自定义注解支持 @!
OwnedPtr<Annotation> ParserImpl::ParseCustomAnnotation()
{
    bool isCompileTimeVisible{false};
    if (!Skip(TokenKind::AT)) {
        Skip(TokenKind::AT_EXCL);  // @! 语法
        isCompileTimeVisible = true;
    }
    // ...
    annotation->isCompileTimeVisible = isCompileTimeVisible;
}

// ParseAnnotation - 内置注解只支持 @
OwnedPtr<Annotation> ParserImpl::ParseAnnotation()
{
    Skip(TokenKind::AT);  // 只处理 @，不处理 @!
    // ...
}
```

## 2. 内置注解列表

C++ 编译器识别的内置注解：

| 注解名称 | 用途 | 参数格式 | 特殊限制 |
|---------|------|----------|---------|
| `@Attribute` | 属性标记 | `[string1, string2, ...]` | - |
| `@Overflow` | 数值溢出处理策略 | `[Panic/Wrap/Saturate]` | 可用于Lambda |
| `@When` | 条件编译 | `[condition_expr]` | 可用于导入 |
| `@Deprecated` | 废弃标记 | `[message: "string", since: "version", strict: bool]` | 参数必须是字符串字面量 |
| `@ConstSafe` | 常量安全标记 | 无参数 | **仅限std模块** |

## 3. IntelliJ PSI 实现的差异

### 3.1 PSI 不区分内置/自定义注解

PSI 的 `parseAnnotation()` 函数将所有注解统一处理：

```kotlin
context(context: ErrorReportContext, parseContext: ParsingContext)
fun parseAnnotation() {
    // PSI 根据语法结构判断，不区分内置/自定义
    if (at(LPAR)) {
        // 有圆括号 → 宏调用
        return MACRO_EXPRESSION
    } else {
        // 无圆括号 → 注解（不管是内置还是自定义）
        return ANNOTATION_ENTRY
    }
}
```

### 3.2 PSI 缺失的功能

| 功能 | C++ 编译器 | IntelliJ PSI |
|------|-----------|--------------|
| 区分内置/自定义注解 | ✅ 有内置注解列表 | ❌ 统一处理 |
| 导入语句注解限制 | ✅ 只允许内置注解 | ❓ 未明确实现 |
| Lambda 注解限制 | ✅ 只允许特定内置注解 | ❌ 未实现 |
| 编译时可见注解 `@!` | ✅ 支持 | ❌ 未实现 |
| `@ConstSafe` 模块限制 | ✅ 仅限std模块 | ❌ 未实现 |
| `@Deprecated` 参数验证 | ✅ 验证参数类型 | ❌ 未实现 |

## 4. 实际使用示例

### 4.1 导入语句

```cangjie
// C++ 编译器行为
@When[DEBUG]           // ✅ 内置注解可用
import debug.tools

@CustomDebug          // ❌ 自定义注解被忽略或解析为宏
import my.debug

// IntelliJ PSI 行为
@When[DEBUG]          // ✅ 解析为注解
import debug.tools

@CustomDebug          // ✅ 也解析为注解（PSI 不区分）
import my.debug
```

### 4.2 Lambda 表达式

```cangjie
// C++ 编译器行为
list.map(@Overflow[Panic] { x -> x * 2 })     // ✅ 内置注解可用
list.map(@CustomMap { x -> transform(x) })    // ❌ 自定义注解报错

// IntelliJ PSI 行为
list.map(@Overflow[Panic] { x -> x * 2 })     // ❌ PSI 未实现 Lambda 注解
list.map(@CustomMap { x -> transform(x) })    // ❌ PSI 未实现 Lambda 注解
```

### 4.3 函数参数

```cangjie
// 当 enableCustomAnno=true 时
func process(
    @Deprecated data: String,      // ✅ 内置注解
    @Validated input: Data         // ✅ 自定义注解
) {}

// 当 enableCustomAnno=false 时
func process(
    @Deprecated data: String,      // ✅ 内置注解
    @Validated input: Data         // ❌ 被解析为宏调用
) {}
```

## 5. 建议的 PSI 改进

基于 C++ 编译器的实现，建议 IntelliJ PSI 进行以下改进：

### 5.1 添加内置注解识别

```kotlin
companion object {
    private val BUILTIN_ANNOTATIONS = setOf(
        "Attribute", "Overflow", "When", "Deprecated", "ConstSafe"
    )

    fun isBuiltinAnnotation(name: String, moduleName: String): Boolean {
        if (name == "ConstSafe") {
            return moduleName == "std"
        }
        return name in BUILTIN_ANNOTATIONS
    }
}
```

### 5.2 实现位置限制

```kotlin
fun parseImportDirective() {
    // 在导入语句前只允许内置注解
    if (at(AT) && lookahead(1) == IDENTIFIER) {
        val annotationName = getLookaheadText(1)
        if (!isBuiltinAnnotation(annotationName, currentModule)) {
            error("Only built-in annotations are allowed before import statements")
        }
    }
    // ...
}
```

### 5.3 支持编译时可见注解

```kotlin
fun parseAnnotation() {
    val isCompileTimeVisible = when {
        at(AT) -> false
        at(ATEXCL) -> true  // @! 语法
        else -> return null
    }
    // ...
}
```

## 6. 总结

**关键差异**：

1. **内置注解** 在 C++ 编译器中享有特权：
   - 可以出现在导入语句前
   - 可以用于 Lambda 表达式
   - 不需要 `enableCustomAnno` 标志

2. **自定义注解** 受到更多限制：
   - 不能出现在导入语句前
   - 通常不能用于 Lambda 表达式
   - 需要 `enableCustomAnno=true` 才能解析
   - 支持 `@!` 编译时可见语法

3. **IntelliJ PSI** 当前实现较为简化：
   - 不区分内置和自定义注解
   - 缺少位置限制检查
   - 不支持 `@!` 语法
   - 缺少特定注解的参数验证

这些差异可能导致在 IntelliJ 中编写的代码在实际编译时出现错误，建议 PSI 实现增加相应的检查和限制。