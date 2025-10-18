# IntelliJ PSI 解析器 vs C++ 编译器解析器 - 缺失功能对比

本文档列出了 C++ 编译器解析器 (`external/cangjie_compiler/src/Parse/`) 中存在但在 IntelliJ PSI 解析器 (`psi/src/main/kotlin/org/cangnova/cangjie/parsing/`) 中**缺失或不完整**的语法功能。

> **注意**: 本对比基于源代码分析,而非 ANTLR 语法文件。

---

## 1. Effect 处理系统 ❌ (实验性功能 - 当前已禁用)

C++ 解析器实现了完整的代数效应系统,包括 `perform`、`resume` 和 `handle` 结构。这在 IntelliJ PSI 解析器中**完全缺失**。

> **重要说明**: Effect Handling 是一个**可选的实验性功能**,在当前编译器构建版本中**已被完全禁用**。
>
> **编译时禁用**:
> - 编译器源码在构建时定义了 `DISABLE_EFFECT_HANDLERS` 宏
> - 这导致 `--enable-eh` 命令行选项和所有相关功能都被移除
> - `perform`、`resume`、`throwing`、`handle` 关键字在当前版本中**完全不可用**
>
> **错误示例**:
> ```bash
> $ cjc --enable-eh your_file.cj
> error: invalid option: '--enable-eh'.
> Invalid options. Try: 'cjc --help' for more information.
> ```
>
> **源码位置**: `external/cangjie_compiler/include/cangjie/Option/Options.inc` (第 332-336 行)
> ```cpp
> #ifndef DISABLE_EFFECT_HANDLERS
> OPTION("--enable-eh", ENABLE_EFFECTS, FLAG, { BACKEND(CJNATIVE) },
>     { GROUP(GLOBAL) COMMA GROUP(VISIBLE) }, nullptr, {}, MULTIPLE_OCCURRENCE,
>     "Enables experimental support for effect handlers")
> #endif
> ```
>
> **解析器设计** (来自 `external/cangjie_compiler/src/Parse/ParserImpl.cpp` 第 123-126 行):
> ```cpp
> // Effect handlers break backwards compatibility by introducing new
> // keywords, so we disable them from the parser unless the user
> // explicitly asks to compile with effect handler support
> SetEHEnabled(opts.enableEH);
> ```
>
> **结论**: Effect Handling 功能在理论上存在于编译器源码中,但在实际发行版本中已被编译时禁用,无法通过任何命令行选项启用。

### 1.1 Perform 表达式

**位置**: `external/cangjie_compiler/src/Parse/ParseAtom.cpp` (第 921-931 行)

**语法**:
```cangjie
perform someEffect(arg1, arg2)
```

**C++ 实现**:
```cpp
OwnedPtr<PerformExpr> ParserImpl::ParsePerformExpr()
{
    OwnedPtr<PerformExpr> ret = MakeOwned<PerformExpr>();
    ret->performPos = lastToken.Begin();
    ret->begin = lookahead.Begin();
    ChainScope cs(*this, ret.get());
    ret->expr = ParseExpr();
    if (ret->expr) {
        ret->end = ret->expr->end;
    }
    return ret;
}
```

**PSI 状态**: ❌ 未实现

---

### 1.2 Resume 表达式

**位置**: `external/cangjie_compiler/src/Parse/ParseAtom.cpp` (第 933-954 行)

**语法**:
```cangjie
// 恢复并返回值
resume with value

// 恢复并抛出异常
resume throwing exception
```

**C++ 实现**:
```cpp
OwnedPtr<ResumeExpr> ParserImpl::ParseResumeExpr()
{
    OwnedPtr<ResumeExpr> ret = MakeOwned<ResumeExpr>();
    ret->resumePos = lastToken.Begin();
    ret->begin = lookahead.Begin();
    ret->end = lookahead.End();
    ChainScope cs(*this, ret.get());
    if (Skip(TokenKind::WITH)) {
        ret->withPos = lastToken.Begin();
        ret->withExpr = ParseExpr();
        if (ret->withExpr) {
            ret->end = ret->withExpr->end;
        }
    } else if (Skip(TokenKind::THROWING)) {
        ret->throwingPos = lastToken.Begin();
        ret->throwingExpr = ParseExpr();
        if (ret->throwingExpr) {
            ret->end = ret->throwingExpr->end;
        }
    }
    return ret;
}
```

**PSI 状态**: ❌ 未实现

---

### 1.3 Try 表达式中的 Handle 块

**位置**: `external/cangjie_compiler/src/Parse/ParseAtom.cpp` (第 505-524 行)

**语法**:
```cangjie
try {
    // 可能执行效应的代码
} handle (command: SomeEffect) {
    // 处理效应
    resume with result
}
```

> **注意**: `handle` 关键字在默认情况下不可用,会报错:
> - `expected ';' or '<NL>', found 'handle'`
> - `expected 'catch' or 'finally' after try block, found 'handle'`
>
> 必须通过编译器选项 `enableEH` 启用 Effect Handling 功能后才能使用。

**C++ 实现**:
```cpp
void ParserImpl::ParseHandleBlock(TryExpr& tryExpr)
{
    auto handler = Handler();
    handler.pos = lastToken.Begin();
    tryExpr.handlePos = lastToken.Begin();
    ChainScope cs(*this, &tryExpr);
    if (!Skip(TokenKind::LPAREN)) {
        DiagExpectCharacter("'('", "handle block must have pattern");
        return;
    }
    handler.leftParenPos = lastToken.Begin();
    handler.commandPattern = ParseCommandTypePattern();
    if (!Skip(TokenKind::RPAREN)) {
        DiagExpectedRightDelimiter("(", handler.leftParenPos);
        return;
    }
    handler.rightParenPos = lastToken.Begin();
    handler.block = ParseBlock(ScopeKind::FUNC_BODY);
    tryExpr.handlers.emplace_back(std::move(handler));
}
```

**PSI 状态**: ❌ 未实现

---

 

## 6. Inout 参数传递修饰符 ❌

**位置**: `external/cangjie_compiler/src/Parse/ParseAtom.cpp` (ParseFuncArg 函数)

**语法**:
```cangjie
// 在函数调用时使用 inout 标记参数
func swap(a: Int64, b: Int64) {
    let temp = a
    // ...
}

let x = 1
let y = 2
swap(inout x, inout y)  // 调用时使用 inout 标记
```

**C++ 实现**:
```cpp
OwnedPtr<FuncArg> ParserImpl::ParseFuncArg()
{
    OwnedPtr<FuncArg> ret = MakeOwned<FuncArg>();
    ret->begin = lookahead.Begin();

    if (SeeingNamedFuncArgs()) {
        ret->name = ExpectIdentifierWithPos(*ret);
        Next();
        ret->colonPos = lookahead.Begin();
    } else if (Skip(TokenKind::INOUT)) {
        ret->withInout = true;
        ret->inoutPos = lastToken.Begin();
    }
    auto tmpExpr = ParseExpr(ExprKind::EXPR_IN_CALLSUFFIX);
    ret->expr = std::move(tmpExpr);
    if (ret->expr) {
        ret->end = ret->expr->end;
    }
    return ret;
}
```

**PSI 状态**: ❌ 未实现 - 函数调用参数解析中不支持 `inout` 修饰符

---

 
 

 

## 10. 带常量类型参数的 VArray ⚠️

**位置**: `external/cangjie_compiler/src/Parse/ParseType.cpp` (第 58-111 行)

**语法**:
```cangjie
// 具有常量大小的固定大小值数组
let arr: VArray<Int64, $10> = VArray<Int64, $10>(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
```

**C++ 实现**:
```cpp
OwnedPtr<AST::Type> ParserImpl::ParseVarrayType()
{
    OwnedPtr<VArrayType> ret = MakeOwned<VArrayType>();
    ret->varrayPos = lastToken.Begin();
    ret->begin = lastToken.Begin();
    ChainScope cs(*this, ret.get());

    if (!Skip(TokenKind::LT)) {
        ParseDiagnoseRefactor(DiagKindRefactor::parse_varray_type_parameter, lastToken.End());
        return MakeOwned<InvalidType>(lookahead.Begin());
    }
    ret->leftAnglePos = lastToken.Begin();

    // <T, $N>
    //  ^ 解析类型参数
    ret->typeArgument = ParseType();

    if (!Skip(TokenKind::COMMA)) {
        DiagVArrayTypeArgMismatch(MakeRange(ret->leftAnglePos, lookahead.End()),
                                  "a type argument and size literal");
        return MakeOwned<InvalidType>(lookahead.Begin());
    }
    ret->typeArgument->commaPos = lastToken.Begin();

    // <T, $N>
    //     ^ 解析 $ 前缀
    skipNL = false;
    if (!Skip(TokenKind::DOLLAR)) {
        DiagVArrayTypeArgMismatch(MakeRange(lookahead.Begin(), lookahead.End()),
            "a '$' follows an integer literal as the second generic argument");
        return MakeOwned<InvalidType>(lookahead.Begin());
    }

    OwnedPtr<ConstantType> constType = MakeOwned<ConstantType>();
    constType->dollarPos = lastToken.Begin();
    constType->begin = lastToken.Begin();

    // <T, $N>
    //      ^ 解析常量值
    if (!Seeing(TokenKind::INTEGER_LITERAL)) {
        ParseDiagnoseRefactor(DiagKindRefactor::parse_expect_integer_literal_varray,
                              constType->dollarPos);
        ConsumeUntil(TokenKind::NL);
        return MakeOwned<InvalidType>(lookahead.Begin());
    }
    skipNL = true;
    constType->constantExpr = ParseLitConst();
    constType->end = lookahead.End();
    ret->constantType = std::move(constType);

    if (!Skip(TokenKind::GT)) {
        DiagExpectedRightDelimiter("<", ret->leftAnglePos);
        return MakeOwned<InvalidType>(lookahead.Begin());
    }
    ret->rightAnglePos = lookahead.Begin();
    ret->end = lookahead.End();

    return ret;
}
```

**PSI 状态**: ⚠️ 不明确 - PSI 支持 VArray 但需要验证常量类型参数处理

---

## 11. 宏系统功能 ⚠️

**位置**: `external/cangjie_compiler/src/Parse/ParseMacro.cpp`

### 11.1 带转义序列的宏属性和参数

**语法**:
```cangjie
// 在属性 [] 中转义方括号
@MyMacro[\[](arg1, arg2)   // 属性中包含字面的 [ 字符
@MyMacro[\]](arg1, arg2)   // 属性中包含字面的 ] 字符

// 在参数 () 中转义圆括号
@Foo[attr](\()             // 参数中包含字面的 ( 字符
@Foo[attr](\))             // 参数中包含字面的 ) 字符

// 嵌套宏调用 (在属性或参数中都可以)
@Bar(\@Nested(123))        // 在参数中嵌套调用 Nested 宏
@Baz[\@Inner](x)           // 在属性中嵌套调用 Inner 宏
```

**重要规则**:
- 在 `[]` 属性中,**只能**转义 `\[` 和 `\]`,不能转义圆括号
- 在 `()` 参数中,**只能**转义 `\(` 和 `\)`,不能转义方括号
- `\@` 可以在属性和参数中使用,用于嵌套宏调用
- 其他转义序列会导致解析错误

**C++ 实现** (第 276-302 行):
```cpp
bool ParserImpl::ParseMacroCallEscapeTokens(const TokenKind& left,
                                           std::vector<Token>& tokens, bool isAttr)
{
    Skip(TokenKind::ILLEGAL);

    if ((Seeing(TokenKind::LSQUARE, TokenKind::RSQUARE) && (left == TokenKind::LSQUARE)) ||
        (Seeing(TokenKind::LPAREN, TokenKind::RPAREN) && (left == TokenKind::LPAREN))) {
        auto tok = Peek();
        (void)tokens.emplace_back(tok.kind, tok.Value(), tok.Begin(), tok.End());
    } else if ((Seeing(TokenKind::AT) && (left == TokenKind::LPAREN)) ||
               (Seeing(TokenKind::AT) && (left == TokenKind::LSQUARE))) {
        // \@ 将被保存到 tokens 中以供进一步分析 (嵌套宏)
        auto skipIll = lastToken;
        auto tok = Peek();
        (void)tokens.emplace_back(skipIll.kind, skipIll.Value(), skipIll.Begin(), skipIll.End());
        (void)tokens.emplace_back(tok.kind, tok.Value(), tok.Begin(), tok.End());
    } else {
        auto tokenPos = lookahead;
        while (SeeingAny({TokenKind::LSQUARE, TokenKind::LPAREN, TokenKind::DOT})) {
            Next();
        }
        auto diagKind = isAttr ? DiagKindRefactor::parse_illegal_macro_expand_attr_args
                               : DiagKindRefactor::parse_illegal_macro_expand_input_args;
        ParseDiagnoseRefactor(diagKind, tokenPos);
        return false;
    }
    return true;
}
```

**PSI 状态**: ⚠️ 不明确 - 需要验证宏解析中的转义序列支持

---

### 11.2 宏参数验证

**C++ 实现** (第 249-274 行):
```cpp
OwnedPtr<AST::FuncParamList> ParserImpl::ParseMacroParameterList()
{
    auto paramList = ParseParameterList();

    // 宏必须有 1-2 个参数
    if ((paramList->params.empty() || paramList->params.size() > G_LIMITED_PARAM_NUM) &&
        !paramList->TestAttr(Attribute::IS_BROKEN)) {
        auto pos = paramList->params.empty() ? lookahead.Begin() : paramList->params[0].get()->begin;
        auto diagKind = paramList->params.empty()
            ? DiagKindRefactor::parse_macro_unexpected_empty_parameter
            : DiagKindRefactor::parse_macro_expected_right_parameter_nums;
        ParseDiagnoseRefactor(diagKind, pos);
    } else {
        for (auto& param : paramList->params) {
            // 所有宏参数必须是 Tokens 类型
            if (param->type == nullptr) {
                ParseDiagnoseRefactor(DiagKindRefactor::parse_macro_illegal_param_type, lookahead);
                chainedAST.back()->EnableAttr(Attribute::HAS_BROKEN);
            } else {
                CheckMacroParamType(*param->type);
            }

            // 宏中不允许命名参数
            if (param->isNamedParam) {
                DiagMacroUnexpectNamedParam(param);
                chainedAST.back()->EnableAttr(Attribute::HAS_BROKEN);
            }
        }
    }
    return paramList;
}
```

**PSI 状态**: ⚠️ 不明确 - 需要验证 PSI 中的宏参数验证

---

## 12. `@` 符号的歧义性:注解 vs 宏调用

### C++ 编译器的处理方式

在 C++ 编译器中,`@` 符号后面跟标识符的含义取决于**标识符名称**和**是否启用自定义注解**:

**判断逻辑** (`external/cangjie_compiler/src/Parse/ParseAnnotations.cpp` 第 198-210 行):

```cpp
bool ParserImpl::SeeingMacroCallDecl()
{
    if (!SeeingAny({TokenKind::AT, TokenKind::AT_EXCL})) {
        return false;
    }
    // Get annotation identifier.
    auto tokens = lexer->LookAheadSkipNL(1);
    if (tokens.begin()->kind != TokenKind::IDENTIFIER &&
        (tokens.begin()->kind < TokenKind::PUBLIC || tokens.begin()->kind > TokenKind::OPEN)) {
        return false;
    }
    return !IsBuiltinAnnotation(moduleName, tokens.begin()->Value());
}
```

**ParseAnnotation 逻辑** (`external/cangjie_compiler/src/Parse/ParseAnnotations.cpp` 第 296-301 行):

```cpp
OwnedPtr<Annotation> ParserImpl::ParseAnnotation()
{
    if (this->enableCustomAnno && SeeingMacroCallDecl()) {
        // Reparse as a custom annotation after macro expansion if one macrocall can't find it's macrodef.
        return ParseCustomAnnotation();
    }
    // parse builtin annotation
    // ...
}
```

**规则**:
1. **内置注解名称** (`@Attribute`, `@Overflow`, `@When`, `@Deprecated`, `@ConstSafe`) → 始终解析为注解
2. **非内置注解名称**:
   - 如果 `enableCustomAnno == true` → 解析为**自定义注解**
   - 如果 `enableCustomAnno == false` → 解析为**宏调用**

### PSI 解析器的处理方式

在 PSI 解析器中,`@` 符号的含义取决于**后续语法结构**:

**判断逻辑** (`psi/src/main/kotlin/org/cangnova/cangjie/parsing/CangJieParsing.kt` 第 1020-1074 行):

```kotlin
context(context: ErrorReportContext) fun parseAnnotation(detector: ModifierDetector?): IElementType? {
    assert(_at(AT))
    val nextRawToken = lookahead(1)

    if (nextRawToken == IDENTIFIER) {
        advance() // consume AT '@'

        val reference = mark()
        val typeReference = mark()
        parseUserType()
        typeReference.done(TYPE_REFERENCE)
        reference.done(CONSTRUCTOR_CALLEE)

        if (at(LBRACKET)) {
            expressionParsing.parseValueArgumentList(LBRACKET, RBRACKET)
        }
    }

    if (at(LPAR)) {
        // TODO 处理宏调用
        // ... 解析宏参数 ...
        return MACRO_EXPRESSION
    } else {
        return if (modifierSize > 0) {
            error("...macro call...")
            MACRO_EXPRESSION
        } else {
            ANNOTATION_ENTRY
        }
    }
}
```

**规则**:
1. `@identifier(...)` → 解析为**宏调用** (`MACRO_EXPRESSION`)
2. `@identifier[...]` → 解析为**注解**(带参数) (`ANNOTATION_ENTRY`)
3. `@identifier` → 解析为**注解**(无参数) (`ANNOTATION_ENTRY`)

### 关键差异总结

| 判断依据 | C++ 编译器 | PSI 解析器 |
|---------|----------|----------|
| **区分方式** | 基于**标识符名称** + `enableCustomAnno` 标志 | 基于**后续语法** (是否有 `()`) |
| **内置注解** | 识别内置注解名称列表 | 不区分内置/自定义 |
| **自定义注解** | 需要 `enableCustomAnno=true` | 始终支持 |
| **宏调用** | 非内置名称 + `enableCustomAnno=false` | 后面跟 `(...)` |
| **编译时可见** | 支持 `@!` 语法 | ❌ 未实现 |

### 示例对比

```cangjie
@Deprecated                  // 两者都解析为注解
@Deprecated[message: "xxx"]  // 两者都解析为注解(带参数)
@MyCustomAnno                // C++: 自定义注解(enableCustomAnno=true) 或 宏调用(false)
                             // PSI: 注解
@MyMacro(...)                // C++: 宏调用(enableCustomAnno=false) 或 自定义注解(true)
                             // PSI: 宏调用
@!CompileTimeAnno            // C++: ✅ 编译时可见注解
                             // PSI: ❌ 不支持
```

**结论**: 在 func 声明中,`@a` **不一定是注解**:
- C++ 编译器根据标识符名称和编译器设置决定
- PSI 解析器根据后续是否有 `()` 决定
- 两者的判断逻辑不同,可能导致解析结果不一致

---

## 13. 注解处理系统 ⚠️

**位置**: `external/cangjie_compiler/src/Parse/ParseAnnotations.cpp`

### 12.1 内置注解与自定义注解区分

C++ 解析器能够区分**内置注解**和**自定义注解**,并进行不同的处理:

**C++ 实现** (第 152-172 行):
```cpp
bool IsBuiltinAnnotation(const std::string& moduleName, const std::string& identifier)
{
    if (STD_ONLY_ANNO.find(identifier) != STD_ONLY_ANNO.end()) {
        return moduleName == "std";
    }
    return NAME_TO_ANNO_KIND.find(identifier) != NAME_TO_ANNO_KIND.end();
}

bool ParserImpl::SeeingBuiltinAnnotation()
{
    if (!Seeing(TokenKind::AT)) {
        return false;
    }
    // Get annotation identifier.
    auto tokens = lexer->LookAheadSkipNL(1);
    if (tokens.begin()->kind != TokenKind::IDENTIFIER) {
        return false;
    }
    return IsBuiltinAnnotation(moduleName, tokens.begin()->Value());
}
```

**内置注解类型**:
- `@Attribute` - 属性注解
- `@Overflow` - 溢出处理注解
- `@When` - 条件注解
- `@Deprecated` - 废弃标记注解
- `@ConstSafe` - 常量安全注解(仅限std模块)

**PSI 状态**: ⚠️ 部分实现 - PSI 的 `parseAnnotation()` 函数将所有注解统一处理,没有明确区分内置和自定义注解

---

### 12.2 编译时可见注解 (@! 语法)

C++ 解析器支持使用 `@!` 前缀标记编译时可见的自定义注解:

**语法**:
```cangjie
@CustomAnno       // 运行时注解
@!CustomAnno      // 编译时可见注解
```

**C++ 实现** (ParseCustomAnnotation 函数,第 226-236 行):
```cpp
OwnedPtr<Annotation> ParserImpl::ParseCustomAnnotation()
{
    bool isCompileTimeVisible{false};
    if (!Skip(TokenKind::AT)) {
        Skip(TokenKind::AT_EXCL);  // @!
        isCompileTimeVisible = true;
    }
    auto atPos = lastToken.Begin();
    (void)Peek();
    OwnedPtr<AST::Expr> expr = ParseRefExpr();
    // ...
    annotation->isCompileTimeVisible = isCompileTimeVisible;
    // ...
}
```

**PSI 状态**: ❌ 未实现 - PSI 解析器不支持 `@!` 语法,仅识别 `@` 符号

---

### 12.3 限定名称的自定义注解

C++ 解析器支持使用完全限定名称的自定义注解:

**语法**:
```cangjie
@some.package.CustomAnno
@another.module.TestAnno[args]
```

**C++ 实现** (ParseCustomAnnotation 函数,第 235-242 行):
```cpp
OwnedPtr<AST::Expr> expr = ParseRefExpr();
while (Skip(TokenKind::DOT)) {
    auto ret = ParseMemberAccess(std::move(expr));
    expr = std::move(ret);
}
auto ident = expr->ToString();
auto annotation = MakeOwned<Annotation>(ident, AnnotationKind::CUSTOM, atPos);
annotation->baseExpr = std::move(expr);
```

**PSI 状态**: ✅ 已实现 - PSI 的 `parseAnnotation()` 使用 `parseUserType()` 解析类型引用,支持限定名称

---

### 12.4 @Deprecated 注解参数验证

C++ 解析器对 `@Deprecated` 注解的参数进行特殊验证:

**C++ 实现** (CheckDeprecatedAnnotation 函数,第 273-294 行):
```cpp
void ParserImpl::CheckDeprecatedAnnotation(Annotation& anno)
{
    for (const auto& arg : anno.args) {
        if (!arg->name.empty()) {
            if (arg->name == "message") {
                // message 参数必须是字符串字面量
                if (arg->value->astKind != ASTKind::LIT_CONST_EXPR ||
                    StaticAs<ASTKind::LIT_CONST_EXPR>(arg->value)->literalKind != LiteralKind::STRING) {
                    DiagDeprecatedMessageNotStringLiteral(*arg);
                }
            } else if (arg->name == "replaceWith") {
                // replaceWith 参数必须是字符串字面量
                if (arg->value->astKind != ASTKind::LIT_CONST_EXPR ||
                    StaticAs<ASTKind::LIT_CONST_EXPR>(arg->value)->literalKind != LiteralKind::STRING) {
                    DiagDeprecatedReplaceWithNotStringLiteral(*arg);
                }
            } else {
                DiagDeprecatedUnknownArgument(*arg, arg->name);
            }
        }
    }
}
```

**允许的参数**:
- `message: String` - 废弃说明消息
- `replaceWith: String` - 替代方案说明

**PSI 状态**: ❌ 未实现 - PSI 没有特殊的 `@Deprecated` 参数验证逻辑

---

### 12.5 注解参数解析

C++ 解析器在 `[]` 中解析注解参数:

**语法**:
```cangjie
@Overflow[Panic]
@Attribute[name: "test", value: 42]
```

**C++ 实现** (ParseAnnotationArguments 函数):
```cpp
void ParserImpl::ParseAnnotationArguments(Annotation& anno)
{
    if (!Skip(TokenKind::LSQUARE)) {
        return;
    }
    anno.leftSquarePos = lastToken.Begin();

    ParseZeroOrMoreSepTrailing(
        [&anno](const Position commaPos) {
            if (!anno.args.empty()) {
                anno.args.back()->commaPos = commaPos;
            }
        },
        [this, &anno]() {
            auto arg = MakeOwned<AnnotationArgument>();
            arg->begin = lookahead.Begin();
            // 解析命名参数 (name: value)
            if (SeeingCombinator({TokenKind::IDENTIFIER, TokenKind::COLON})) {
                arg->name = lastToken.Value();
                arg->namePos = lastToken.Begin();
                Skip(TokenKind::COLON);
                arg->colonPos = lastToken.Begin();
            }
            arg->value = ParseExpr();  // 解析参数值表达式
            arg->end = arg->value ? arg->value->end : arg->begin;
            anno.args.emplace_back(std::move(arg));
        },
        TokenKind::RSQUARE);

    anno.rightSquarePos = lastToken.Begin();
}
```

**特点**:
- 支持命名参数和位置参数
- 参数值是表达式(允许常量、字面量等)
- 使用逗号分隔多个参数

**PSI 状态**: ✅ 部分实现 - PSI 使用 `expressionParsing.parseValueArgumentList(LBRACKET, RBRACKET)` 解析参数,但可能缺少特定注解的验证

---
