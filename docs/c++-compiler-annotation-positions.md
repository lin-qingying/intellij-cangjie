# C++ 编译器中注解可以出现的位置

本文档分析了仓颉语言 C++ 编译器实现中注解（Annotation）可以出现的所有位置，以及内置注解与自定义注解的使用差异。

## 1. 注解类型概览

### 1.1 内置注解 (Built-in Annotations)

| 注解名称 | 用途 | 参数 |
|---------|------|------|
| `@Attribute` | 属性标记 | `[string1, string2, ...]` |
| `@Overflow` | 数值溢出处理策略 | `[Panic/Wrap/Saturate]` |
| `@When` | 条件编译 | `[condition_expr]` |
| `@Deprecated` | 废弃标记 | `[message: "string", since: "version", strict: bool]` |
| `@ConstSafe` | 常量安全标记（仅std库） | 无参数 |

### 1.2 自定义注解 (Custom Annotations)

- 语法：`@CustomName` 或 `@!CustomName`（编译时可见）
- 支持限定名：`@package.module.CustomAnno`
- 参数：`[arg1, arg2, name: value]`

---

## 2. 注解可以出现的位置

基于 C++ 编译器源码分析，注解可以出现在以下位置：

### 2.1 顶层声明前 (Top-Level Declarations)

**位置**: `Parser.cpp` - `ParseTopLevelDecls()` 函数

```cpp
void ParserImpl::ParseTopLevelDecls(AST::File& file, std::vector<OwnedPtr<Annotation>>& annos)
{
    while (!eof()) {
        ParseAnnotations(annos);  // 解析注解
        ParseTopLevelDecl(file, annos);  // 解析顶层声明
        annos.clear();
    }
}
```

**可用于**:
- 函数声明 (`func`)
- 类声明 (`class`, `interface`, `struct`, `enum`)
- 类型别名 (`type`)
- 变量声明 (`let`, `var`, `const`)
- 主函数 (`main`)
- 扩展声明 (`extend`)
- 外部函数声明 (`foreign`)
- 宏声明 (`macro`)

**示例**:
```cangjie
@Deprecated[message: "Use newFunction instead"]
func oldFunction() {}

@Attribute["Serializable"]
class MyClass {}

@ConstSafe
let MAX_SIZE = 1000
```

---

### 2.2 导入语句前 (Import Statements)

**位置**: `ParseImports.cpp` - `ParseImportSpecs()` 函数

```cpp
while (SeeingImport2() || SeeingBuiltinAnnotation()) {
    if (SeeingBuiltinAnnotation()) {
        ParseAnnotations(annos);  // 导入前的注解
    }
    ParseImportSpec(imports, annos);
}
```

**示例**:
```cangjie
@Deprecated
import std.io

@When[DEBUG]
import debug.tools
```

---

### 2.3 函数参数前 (Function Parameters)

**位置**: `ParseDecl.cpp` - `ParseFuncParam()` 函数

```cpp
OwnedPtr<FuncParam> ParserImpl::ParseFuncParam(ScopeKind scopeKind)
{
    OwnedPtr<FuncParam> param = MakeOwned<FuncParam>();
    ParseAnnotations(param->annotations);  // 参数注解
    ParseModifiers(param->modifiers);
    ParseParameter(scopeKind, *param);
}
```

**示例**:
```cangjie
func process(
    @Deprecated data: String,
    @ConstSafe count: Int64
) {}
```

---

### 2.4 属性访问器前 (Property Accessors)

**位置**: `ParseDecl.cpp` - `ParsePropDecl()` 函数

```cpp
void ParserImpl::ParsePropDecl(OwnedPtr<PropDecl>& propDecl)
{
    while (true) {
        std::vector<OwnedPtr<Annotation>> annos;
        ParseAnnotations(annos);  // getter/setter 前的注解
        std::set<Modifier> modis;
        ParseModifiers(modis);

        if (SeeingPropMember()) {
            // 解析 get/set
        }
    }
}
```

**示例**:
```cangjie
prop value: Int64 {
    @Deprecated
    get() { return _value }

    @ConstSafe
    set(v) { _value = v }
}
```

---

### 2.5 Lambda 表达式前

**位置**: `ParseAtom.cpp` 和 `ParseExpr.cpp`

#### 5.1 普通 Lambda
```cpp
OwnedPtr<Expr> ParserImpl::ParseAtomExprWithLBrace(bool isTailClosure)
{
    std::vector<OwnedPtr<Annotation>> annos;
    ParseAnnotations(annos);  // Lambda 前的注解
    auto ret = isTailClosure ? ParseLambdaExprWithTrailingClosure() : ParseLambdaExpr();
}
```

#### 5.2 尾闭包 Lambda
```cpp
OwnedPtr<TrailingClosureExpr> ParserImpl::ParseTrailingClosure()
{
    std::vector<OwnedPtr<Annotation>> annos;
    ParseAnnotations(annos);  // 尾闭包前的注解
    ret->lambda = ParseLambdaExprWithTrailingClosure();
}
```

**示例**:
```cangjie
let handler = @Overflow[Panic] { x, y -> x + y }

list.map(@ConstSafe { item -> item * 2 })
```

---

### 2.6 类成员前 (Class Members)

**位置**: `ParseDecl.cpp` - `ParseDeclByModifiers()` 函数

```cpp
OwnedPtr<Decl> ParserImpl::ParseDeclByModifiers(...)
{
    ParseAnnotations(annos);  // 成员前的注解
    ParseModifiers(modifiers);
    // 解析成员声明
}
```

**可用于**:
- 成员函数
- 成员变量
- 成员属性
- 嵌套类型

**示例**:
```cangjie
class Example {
    @Deprecated
    var oldField: String

    @Attribute["ThreadSafe"]
    func method() {}

    @When[ENABLE_FEATURE]
    class NestedClass {}
}
```

---

## 3. 内置注解 vs 自定义注解的位置差异

### 3.1 解析条件的关键差异

**内置注解** 和 **自定义注解** 在解析时有不同的条件判断：

```cpp
void ParserImpl::ParseAnnotations(PtrVector<Annotation>& annos)
{
    while (SeeingBuiltinAnnotation() ||                          // 内置注解：总是解析
           (this->enableCustomAnno && SeeingMacroCallDecl())) {  // 自定义注解：需要启用标志
        // ...
    }
}
```

### 3.2 位置限制对比

| 位置 | 内置注解 | 自定义注解 | 关键代码 |
|------|---------|-----------|----------|
| **顶层声明** | ✅ 总是可用 | ⚠️ 需要 `enableCustomAnno=true` | `ParseTopLevelDecls()` |
| **导入语句** | ✅ 总是可用 | ❌ 不可用 | `while (SeeingImport2() \|\| SeeingBuiltinAnnotation())` |
| **函数参数** | ✅ 总是可用 | ⚠️ 需要 `enableCustomAnno=true` | `ParseFuncParam()` |
| **Lambda表达式** | ✅ 总是可用 | ❌ 通常不可用 | `ParseAnnotationLambdaExpr()` 只处理内置注解 |
| **类成员** | ✅ 总是可用 | ⚠️ 需要 `enableCustomAnno=true` | `ParseDeclByModifiers()` |
| **属性访问器** | ✅ 总是可用 | ⚠️ 需要 `enableCustomAnno=true` | `ParsePropDecl()` |

### 3.3 特殊位置的限制

#### 导入语句前 - 只允许内置注解
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

#### Lambda 表达式 - 主要用于内置注解
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

### 3.4 自定义注解的特殊处理

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

### 3.5 编译时可见注解 (`@!`) 的限制

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

## 4. 注解使用规则

### 4.1 解析顺序

注解总是在修饰符（modifiers）之前解析：

```cpp
// 标准解析顺序
ParseAnnotations(annos);      // 1. 先解析注解
ParseModifiers(modifiers);    // 2. 再解析修饰符
// 3. 最后解析声明本体
```

### 3.2 重复检查

编译器会检查同一个内置注解是否重复出现：

```cpp
void ParserImpl::ParseAnnotations(PtrVector<Annotation>& annos)
{
    while (SeeingBuiltinAnnotation() || SeeingMacroCallDecl()) {
        auto annotation = ParseAnnotation();
        // 检查是否重复（仅限非自定义注解）
        auto anno = std::find_if(annos.begin(), annos.end(), [&annotation](const auto& anno) {
            return anno->kind != AnnotationKind::CUSTOM &&
                   anno->identifier == annotation->identifier;
        });
        if (anno != annos.end()) {
            DiagDuplicatedAnno(*annotation, **anno);  // 报告重复错误
        }
        annos.emplace_back(std::move(annotation));
    }
}
```

### 3.3 位置限制

某些注解有特定的使用位置限制：

| 注解 | 允许的位置 | 限制说明 |
|------|-----------|----------|
| `@ConstSafe` | 仅std模块中的声明 | 其他模块不可用 |
| `@Overflow` | 函数、Lambda、算术表达式 | 仅用于数值运算相关 |
| `@When` | 所有声明位置 | 条件表达式有限制 |
| `@Deprecated` | 所有可公开访问的声明 | 私有成员使用无意义 |

### 3.4 自定义注解的启用条件

自定义注解需要满足以下条件之一：

1. 启用了 `enableCustomAnno` 标志
2. 标识符不是内置注解名称
3. 在宏展开后重新解析为自定义注解

```cpp
bool ParserImpl::SeeingMacroCallDecl()
{
    if (!SeeingAny({TokenKind::AT, TokenKind::AT_EXCL})) {
        return false;
    }
    // 检查是否为内置注解
    return !IsBuiltinAnnotation(moduleName, tokens.begin()->Value());
}
```

---

## 4. 与 IntelliJ PSI 实现的对比

### 4.1 位置支持对比

| 位置 | C++ 编译器 | IntelliJ PSI | 说明 |
|------|-----------|--------------|------|
| 顶层声明 | ✅ | ✅ | 两者都支持 |
| 导入语句 | ✅ | ❓ | PSI 未明确实现 |
| 函数参数 | ✅ | ❌ | PSI 不支持参数注解 |
| 属性访问器 | ✅ | ❌ | PSI 不支持 getter/setter 注解 |
| Lambda 表达式 | ✅ | ❌ | PSI 不支持 Lambda 注解 |
| 类成员 | ✅ | ✅ | 两者都支持 |

### 4.2 关键差异

1. **参数注解**: C++ 支持在函数参数前使用注解，PSI 未实现
2. **Lambda 注解**: C++ 支持 Lambda 表达式的注解，PSI 未实现
3. **导入注解**: C++ 明确支持导入语句的条件编译注解，PSI 实现不明确
4. **编译时可见注解**: C++ 支持 `@!` 语法，PSI 未实现

---

## 5. 建议的 PSI 改进

基于 C++ 编译器的完整实现，建议 IntelliJ PSI 添加以下支持：

1. **函数参数注解**
   - 在 `parseValueParameter()` 中添加注解解析
   - 支持参数级别的 `@Deprecated` 等注解

2. **Lambda 注解**
   - 在 `parseLambdaExpression()` 前添加注解解析
   - 支持 `@Overflow` 等运算相关注解

3. **属性访问器注解**
   - 在 `parsePropertyGet/Set()` 中添加注解支持
   - 允许 getter/setter 独立注解

4. **导入语句注解**
   - 在 `parseImportDirective()` 中添加注解解析
   - 支持条件导入 (`@When`)

5. **编译时可见注解**
   - 添加 `ATEXCL` (`@!`) 的完整支持
   - 区分运行时和编译时注解

这些改进将使 PSI 的注解支持与 C++ 编译器保持一致，提供更完整的语言特性支持。